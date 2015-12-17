/*
 * Copyright 2015 Texas A&M Engineering Experiment Station
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.tamu.tcat.trc.entries.types.article.postgres;

import java.io.IOException;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.postgresql.util.PGobject;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.tamu.tcat.db.exec.sql.SqlExecutor;
import edu.tamu.tcat.trc.entries.notification.BaseUpdateEvent;
import edu.tamu.tcat.trc.entries.notification.DataUpdateObserverAdapter;
import edu.tamu.tcat.trc.entries.notification.EntryUpdateHelper;
import edu.tamu.tcat.trc.entries.notification.ObservableTaskWrapper;
import edu.tamu.tcat.trc.entries.notification.UpdateEvent;
import edu.tamu.tcat.trc.entries.notification.UpdateListener;
import edu.tamu.tcat.trc.entries.repo.NoSuchCatalogRecordException;
import edu.tamu.tcat.trc.entries.types.article.Article;
import edu.tamu.tcat.trc.entries.types.article.ArticleAuthor;
import edu.tamu.tcat.trc.entries.types.article.ArticleLink;
import edu.tamu.tcat.trc.entries.types.article.ArticlePublication;
import edu.tamu.tcat.trc.entries.types.article.Bibliography;
import edu.tamu.tcat.trc.entries.types.article.Citation;
import edu.tamu.tcat.trc.entries.types.article.Footnote;
import edu.tamu.tcat.trc.entries.types.article.Theme;
import edu.tamu.tcat.trc.entries.types.article.dto.ArticleAuthorDTO;
import edu.tamu.tcat.trc.entries.types.article.dto.ArticleDTO;
import edu.tamu.tcat.trc.entries.types.article.dto.BibliographyDTO;
import edu.tamu.tcat.trc.entries.types.article.dto.CitationDTO;
import edu.tamu.tcat.trc.entries.types.article.dto.FootnoteDTO;
import edu.tamu.tcat.trc.entries.types.article.dto.LinkDTO;
import edu.tamu.tcat.trc.entries.types.article.dto.PublicationDTO;
import edu.tamu.tcat.trc.entries.types.article.dto.ThemeDTO;
import edu.tamu.tcat.trc.entries.types.article.repo.ArticleChangeEvent;
import edu.tamu.tcat.trc.entries.types.article.repo.ArticleRepository;
import edu.tamu.tcat.trc.entries.types.article.repo.EditArticleCommand;

public class PsqlArticleRepo implements ArticleRepository
{
   private static final Logger logger = Logger.getLogger(PsqlArticleRepo.class.getName());

   private static final String SQL_GET_ALL =
         "SELECT article "
        +  "FROM articles "
        + "WHERE reference->>'associatedEntry' LIKE ? AND active = true "
        + "ORDER BY reference->>'associatedEntry'";

   private static final String SQL_GET =
         "SELECT article "
               +  "FROM articles "
               + "WHERE article_id = ? AND active = true";

   private static String CREATE_SQL =
         "INSERT INTO articles (article, article_id) VALUES(?, ?)";

   private static String UPDATE_SQL =
         "UPDATE articles "
         + " SET article = ?, "
         +     " modified = now() "
         +"WHERE article_id = ?";

   private static final String SQL_REMOVE =
         "UPDATE articles "
         + " SET active = FALSE, "
         +     " modified = now() "
         +"WHERE article_id = ?";

   //HACK: doesn't really matter for now, but once authz is in place, this will be the user's id
   private static final UUID ACCOUNT_ID_REPO = UUID.randomUUID();

   private EntryUpdateHelper<ArticleChangeEvent> listeners;
   private SqlExecutor exec;
   private ObjectMapper mapper;

   public PsqlArticleRepo()
   {
   }

   public void setDatabaseExecutor(SqlExecutor exec)
   {
      this.exec = exec;
   }

   public void activate()
   {
      listeners = new EntryUpdateHelper<>();

      mapper = new ObjectMapper();
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
   }

   public void dispose()
   {
      try
      {
         if (listeners != null)
            listeners.close();
      }
      catch (Exception ex)
      {
         logger.log(Level.WARNING, "Failed to shut down event notification helper.", ex);
      }

      // managed by supplier. no need to shut down
      this.exec = null;
      listeners = null;
      mapper = null;
   }

   @Override
   public Article get(UUID articleId) throws NoSuchCatalogRecordException
   {
      return adapt(getArticleDTO(SQL_GET, articleId));
   }

   @Override
   public List<Article> getArticles(URI entityURI) throws NoSuchCatalogRecordException
   {
      Future<List<Article>> results = exec.submit(conn -> {
         try (PreparedStatement ps = conn.prepareStatement(SQL_GET_ALL))
         {
            ps.setString(1, entityURI.toString() + "%");
            try (ResultSet rs = ps.executeQuery())
            {
               List<Article> articles = new ArrayList<>();
               while (rs.next())
               {
                  PGobject pgo = (PGobject)rs.getObject("note");
                  ArticleDTO article = parseCopyRefJson(pgo.toString());
                  Article n = adapt(article);
                  articles.add(n);
               }

               return articles;
            }
         }
         catch(SQLException e)
         {
            throw new IllegalStateException("Failed to retrive copy reference [" + entityURI + "]. ", e);
         }
      });

      try
      {
         return unwrapGetResults(results, entityURI.toString());
      }
      catch (NoSuchCatalogRecordException e)
      {
         throw new IllegalStateException("Unexpected internal error", e);
      }
   }

   private static Article adapt(ArticleDTO article)
   {
      return new PsqlArticle(article.id, article.title, article.type,
                             article.authors, article.info, article.articleAbstract,
                             article.body, article.citation, article.footnotes,
                             article.bibliographies, article.links, article.theme );
   }

   @Override
   public EditArticleCommand create()
   {
      ArticleDTO article = new ArticleDTO();
      article.id = UUID.randomUUID();

      PostgresEditArticleCmd cmd = new PostgresEditArticleCmd(article);
      cmd.setCommitHook((n) -> {
         ArticleChangeNotifier notifier = new ArticleChangeNotifier(UpdateEvent.UpdateAction.CREATE);
         return exec.submit(new ObservableTaskWrapper<UUID>(makeSaveTask(n, CREATE_SQL), notifier));
      });

      return cmd;
   }

   @Override
   public EditArticleCommand edit(UUID articleId) throws NoSuchCatalogRecordException
   {
      ArticleDTO article = getArticleDTO(SQL_GET, articleId);

      PostgresEditArticleCmd cmd = new PostgresEditArticleCmd(article);
      cmd.setCommitHook((n) -> {
         ArticleChangeNotifier notifier = new ArticleChangeNotifier(UpdateEvent.UpdateAction.UPDATE);
         return exec.submit(new ObservableTaskWrapper<UUID>(makeSaveTask(n, UPDATE_SQL), notifier));
      });

      return cmd;
   }

   @Override
   public Future<Boolean> remove(UUID articleId)
   {
      ArticleChangeEvent evt = new ArticleChangeEventImpl(articleId, UpdateEvent.UpdateAction.DELETE);
      return exec.submit(new ObservableTaskWrapper<Boolean>(
            makeRemoveTask(articleId),
            new DataUpdateObserverAdapter<Boolean>()
            {
               @Override
               protected void onFinish(Boolean result) {
                  if (result.booleanValue())
                     listeners.after(evt);
               }
            }));
   }

   @Override
   public AutoCloseable register(UpdateListener<ArticleChangeEvent> ears)
   {
      Objects.requireNonNull(listeners, "Registration for updates is not available.");
      return listeners.register(ears);
   }

   private SqlExecutor.ExecutorTask<Boolean> makeRemoveTask(UUID id)
   {
      return (conn) -> {
         try (PreparedStatement ps = conn.prepareStatement(SQL_REMOVE))
         {
            ps.setString(1, id.toString());
            int ct = ps.executeUpdate();
            if (ct == 0)
            {
               logger.log(Level.WARNING, "Failed to remove article  [" + id + "]. Reference may not exist.", id);
               return Boolean.valueOf(false);
            }

            return Boolean.valueOf(true);
         }
         catch(SQLException e)
         {
            throw new IllegalStateException("Failed to remove article [" + id + "]. ", e);
         }
      };
   }

   private SqlExecutor.ExecutorTask<UUID> makeSaveTask(ArticleDTO dto, String sql)
   {
      ObjectMapper mapper = new ObjectMapper();
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

      return (conn) -> {
         try (PreparedStatement ps = conn.prepareStatement(sql))
         {
            PGobject jsonObject = new PGobject();
            jsonObject.setType("json");
            jsonObject.setValue(mapper.writeValueAsString(dto));

            ps.setObject(1, jsonObject);
            ps.setString(2, dto.id.toString());

            int cnt = ps.executeUpdate();
            if (cnt != 1)
               throw new IllegalStateException("Failed to update copy reference [" + dto.id +"]");

            return dto.id;
         }
         catch(SQLException e)
         {
            throw new IllegalStateException("Failed to update note reference [" + dto.id + "]. "
                  + "\n\tEntry [" + dto.title + "]"
                  + "\n\tCopy  [" + dto.id + "]", e);
         }
      };
   }

   private ArticleDTO parseCopyRefJson(String json)
   {
      try
      {
         return mapper.readValue(json, ArticleDTO.class);
      }
      catch (IOException e)
      {
         throw new IllegalStateException("Failed to parse relationship record\n" + json, e);
      }
   }

   private ArticleDTO getArticleDTO(String sql, UUID id) throws NoSuchCatalogRecordException
   {
      Future<ArticleDTO> result = exec.submit((conn) -> executeGetQuery(sql, conn, id));
      return unwrapGetResults(result, id.toString());
   }

   /**
    *
    * @param result The future to unwrap
    * @param id For error messaging purposes
    * @return
    * @throws NoSuchCatalogRecordException
    */
   private <T> T unwrapGetResults(Future<T> result, String id) throws NoSuchCatalogRecordException
   {
      try
      {
         return result.get();
      }
      catch (InterruptedException e)
      {
         throw new IllegalStateException("Failed to retrieve copy reference [" + id + "].", e);
      }
      catch (ExecutionException e)
      {
         // unwrap the execution exception that may be thrown from the executor
         Throwable cause = e.getCause();
         if (cause instanceof NoSuchCatalogRecordException)
            throw (NoSuchCatalogRecordException)cause;         // if not found
         else if (cause instanceof RuntimeException)
            throw (RuntimeException)cause;                     // 'expected' internal errors - json parsing, db access, etc
         else if (cause instanceof Error)
            throw (Error)cause;                                // OoM and other system errors
         else                                                  // unanticipated errors
            throw new IllegalStateException("Unknown error while attempting to retrive copy reference [" + id + "]", cause);
      }
   }

   private ArticleDTO executeGetQuery(String sql, Connection conn, UUID id) throws NoSuchCatalogRecordException
   {
      try (PreparedStatement ps = conn.prepareStatement(sql))
      {
         ps.setString(1, id.toString());
         try (ResultSet rs = ps.executeQuery())
         {
            if (!rs.next())
               throw new NoSuchCatalogRecordException("No catalog record exists for article id=" + id);

            PGobject pgo = (PGobject)rs.getObject("article");
            return parseCopyRefJson(pgo.toString());
         }
      }
      catch(SQLException e)
      {
         throw new IllegalStateException("Failed to retrive copy reference [" + id + "]. ", e);
      }
   }

   private final class ArticleChangeNotifier extends DataUpdateObserverAdapter<UUID>
   {
      private final UpdateEvent.UpdateAction type;

      public ArticleChangeNotifier(UpdateEvent.UpdateAction type)
      {
         this.type = type;
      }

      @Override
      public void onFinish(UUID id)
      {
         listeners.after(new ArticleChangeEventImpl(id, type));
      }
   }

   private class ArticleChangeEventImpl extends BaseUpdateEvent implements ArticleChangeEvent
   {
      public ArticleChangeEventImpl(UUID id, UpdateEvent.UpdateAction type)
      {
         super(id.toString(), type, ACCOUNT_ID_REPO, Instant.now());
      }

      @Override
      public String toString()
      {
         return "Article Change " + super.toString();
      }
   }
   
   private static class PsqlArticle implements Article
   {
      private final UUID id;
      private final String title;
      private final List<ArticleAuthor> authors;
      private final String articleAbstract;
      private final String type;
      private final ArticlePublication info;
      private final String body;
      private final List<Citation> citation;
      private final List<Footnote> footnotes;
      private final List<Bibliography> bibliographies;
      private final List<ArticleLink> links;
      private final Theme theme;

      public PsqlArticle(UUID id, String title, String type, List<ArticleAuthorDTO> authors,
                         PublicationDTO info, String articleAbstract, String body,
                         List<CitationDTO> citation, List<FootnoteDTO> footnotes,
                         List<BibliographyDTO> bibliographies, List<LinkDTO> links, ThemeDTO theme)
      {
         this.id = id;
         this.title = title;
         this.type = type;
         this.authors = getAuthors(authors);
         this.info = getPublication(info);
         this.articleAbstract = articleAbstract;
         this.body = body;
         this.citation = getCitations(citation); //citation;
         this.footnotes = getFootnotes(footnotes); //footnotes;
         this.bibliographies = getBiblios(bibliographies); //bibliographies;
         this.links = getLinks(links);
         this.theme = getTheme(theme);
      }

      private List<Bibliography> getBiblios(List<BibliographyDTO> bibliographies)
      {
         List<Bibliography> biblios = new ArrayList<>();
         
         bibliographies.forEach((bib) ->
         {
            biblios.add(new PsqlBibliography(bib.id, bib.type, bib.title, bib.author, bib.issued, bib.publisher, bib.publisherPlace, bib.translator, bib.url));
         });
         
         return biblios;
      }

      private List<Footnote> getFootnotes(List<FootnoteDTO> footnotes)
      {
         List<Footnote> ftnotes = new ArrayList<>();
         
         footnotes.forEach((fn) ->
         {
            ftnotes.add(new PsqlFootnote(fn.id, fn.text));
         });
         
         return ftnotes;
      }

      private List<Citation> getCitations(List<CitationDTO> citations)
      {
         List<Citation> cites = new ArrayList<>();
         
         citations.forEach((c) ->
         {
            cites.add(new PsqlCitation(c.id, c.properties, c.suppressAuthor, c.citationItems));
         });
         
         return cites;
      }

      private List<ArticleLink> getLinks(List<LinkDTO> links)
      {
         List<ArticleLink> articleLinks = new ArrayList<>();
         
         links.forEach((l) ->
         {
            articleLinks.add(new PsqlArticleLink(l.id, l.title, l.type, l.uri, l.rel));
         });
         
         return articleLinks;
      }

      private Theme getTheme(ThemeDTO theme)
      {
         return new PsqlTheme(theme.title, theme.themeAbstract, theme.treatments);
      }

      private List<ArticleAuthor> getAuthors(List<ArticleAuthorDTO> authors)
      {
         List<ArticleAuthor> auths = new ArrayList<>();
         
         if(authors == null)
            return auths;
         
         authors.forEach((a) ->
         {
            auths.add(new PsqlArticleAuthor(a.id, a.name, a.affiliation, a.contact));
         });
         
         return auths;
      }
      
      private ArticlePublication getPublication(PublicationDTO pub)
      {
         return new BasicPublication(pub.dateCreated, pub.dateModified);
      }

      @Override
      public UUID getId()
      {
         return this.id;
      }

      @Override
      public String getTitle()
      {
         return this.title;
      }

      @Override
      public List<ArticleAuthor> getAuthors()
      {
         return this.authors;
      }

      @Override
      public String getAbstract()
      {
         return this.articleAbstract == null ? "" : this.articleAbstract;
      }

      @Override
      public String getSlug()
      {
         return null;
      }

      @Override
      public String getType()
      {
         return this.type;
      }

      @Override
      public ArticlePublication getPublicationInfo()
      {
         return this.info;
      }

      @Override
      public String getBody()
      {
         return this.body;
      }

      @Override
      public List<Footnote> getFootnotes()
      {
         return this.footnotes;
      }

      @Override
      public List<Citation> getCitations()
      {
         return this.citation;
      }

      @Override
      public List<Bibliography> getBibliographies()
      {
         return this.bibliographies;
      }

      @Override
      public List<ArticleLink> getLinks()
      {
         return this.links;
      }

      @Override
      public Theme getTheme()
      {
         return this.theme;
      }
   }
   
   private static class PsqlArticleAuthor implements ArticleAuthor
   {
      private final String id;
      private final String name;
      private final String affiliation;
      private final ContactInfo contactInfo;
      
      public PsqlArticleAuthor(String id, String name, String affiliation, ArticleAuthorDTO.ContactInfoDTO info)
      {
         this.id = id;
         this.name = name;
         this.affiliation = affiliation;
         this.contactInfo = new PsqlContactInfo(info.email, info.phone);
      }
      
      @Override
      public String getId()
      {
         return this.id;
      }

      @Override
      public String getName()
      {
         return this.name;
      }

      @Override
      public String getAffiliation()
      {
         return this.affiliation;
      }

      @Override
      public ContactInfo getContactInfo()
      {
         // TODO Auto-generated method stub
         return this.contactInfo;
      }
      
      private static class PsqlContactInfo implements ContactInfo
      {
         private String email;
         private String phone;

         private PsqlContactInfo(String email, String phone)
         {
            this.email = email;
            this.phone = phone;
            
         }

         @Override
         public String getEmail()
         {
            return email;
         }

         @Override
         public String getPhone()
         {
            return phone;
         }
         
      }

   }
}
