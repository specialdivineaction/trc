package edu.tamu.tcat.trc.entries.types.article.docrepo;

import static java.text.MessageFormat.format;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.tamu.tcat.account.Account;
import edu.tamu.tcat.db.exec.sql.SqlExecutor;
import edu.tamu.tcat.trc.entries.types.article.Article;
import edu.tamu.tcat.trc.entries.types.article.ArticleRepoFacade;
import edu.tamu.tcat.trc.entries.types.article.repo.ArticleRepository;
import edu.tamu.tcat.trc.entries.types.article.repo.EditArticleCommand;
import edu.tamu.tcat.trc.entries.types.article.search.ArticleSearchService;
import edu.tamu.tcat.trc.entries.types.article.search.solr.ArticleDocument;
import edu.tamu.tcat.trc.entries.types.article.search.solr.ArticleIndexManagerService;
import edu.tamu.tcat.trc.repo.BasicSchemaBuilder;
import edu.tamu.tcat.trc.repo.DocumentRepository;
import edu.tamu.tcat.trc.repo.IdFactory;
import edu.tamu.tcat.trc.repo.IdFactoryProvider;
import edu.tamu.tcat.trc.repo.NoSuchEntryException;
import edu.tamu.tcat.trc.repo.RepositoryException;
import edu.tamu.tcat.trc.repo.UpdateContext;
import edu.tamu.tcat.trc.repo.postgres.PsqlJacksonRepoBuilder;
import edu.tamu.tcat.trc.search.solr.impl.TrcDocument;

public class ArticleRepoService implements ArticleRepoFacade
{
   private final static Logger logger = Logger.getLogger(ArticleRepoService.class.getName());

   public static final String PARAM_TABLE_NAME = "trc.entries.articles.tablename";
   public static final String PARAM_ID_CTX = "trc.entries.articles.id_context";

   private static final String ID_CONTEXT_ARTICLES = "trc.articles";
   private static final String TABLE_NAME = "articles";

   private SqlExecutor sqlExecutor;
   private IdFactoryProvider idFactoryProvider;
   private ArticleIndexManagerService indexSvc;


   private IdFactory idFactory;
   private DocumentRepository<Article, DataModelV1.Article, EditArticleCommand> articleBackend;

   /**
    * Bind method for SQL executor service dependency (usually called by dependency injection layer)
    *
    * @param sqlExecutor
    */
   public void setSqlExecutor(SqlExecutor sqlExecutor)
   {
      this.sqlExecutor = sqlExecutor;
   }

   /**
    * Bind method for ID factory provider service dependency (usually called by dependency injection layer)
    *
    * @param idFactory
    */
   public void setIdFactory(IdFactoryProvider idFactoryProvider)
   {
      this.idFactoryProvider = idFactoryProvider;
   }

   public void setIndexService(ArticleIndexManagerService indexSvc)
   {
      this.indexSvc = indexSvc;
   }

   /**
    * Lifecycle management method (usually called by framework service layer)
    * Called when all dependencies have been provided and the service is ready to run.
    */
   public void activate(Map<String, Object> properties)
   {
      try
      {
         Objects.requireNonNull(sqlExecutor, "No SQL Executor provided.");
         Objects.requireNonNull(idFactoryProvider, "No IdFactoryProvider provided.");

         String tablename = (String)properties.getOrDefault(PARAM_TABLE_NAME, TABLE_NAME);
         articleBackend = buildDocumentRepository(tablename);
         configureIndexing(articleBackend);
         configureVersioning(articleBackend);

         String idContext = (String)properties.getOrDefault(PARAM_ID_CTX, ID_CONTEXT_ARTICLES);
         idFactory = idFactoryProvider.getIdFactory(idContext);
      }
      catch (Exception e)
      {
         logger.log(Level.SEVERE, "Failed to construct articles repository instance.", e);
         throw e;
      }
   }

   /**
    * @return A new document repository instance for persisting and retrieving works
    */
   private DocumentRepository<Article, DataModelV1.Article, EditArticleCommand> buildDocumentRepository(String tablename)
   {
      PsqlJacksonRepoBuilder<Article, DataModelV1.Article, EditArticleCommand> repoBuilder = new PsqlJacksonRepoBuilder<>();

      repoBuilder.setDbExecutor(sqlExecutor);
      repoBuilder.setTableName(tablename);
      repoBuilder.setEditCommandFactory(new EditArticleCommandFactory());
      repoBuilder.setDataAdapter(dto -> new ArticleImpl(dto));
      repoBuilder.setSchema(BasicSchemaBuilder.buildDefaultSchema());
      repoBuilder.setStorageType(DataModelV1.Article.class);
      repoBuilder.setEnableCreation(true);

      return repoBuilder.build();
   }

   private void configureIndexing(DocumentRepository<Article, DataModelV1.Article, EditArticleCommand> repo)
   {
      if (indexSvc == null)
         logger.warning("No search index has been configured for articles.");

      repo.afterUpdate(this::index);
   }

   private void index(UpdateContext<DataModelV1.Article> ctx)
   {
      if (indexSvc == null)
      {
         logger.info(() -> format("Not indexing article {0}. No search index is available.", ctx.getId()));
         return;
      }

      try
      {
         // TODO should be able to generalize this.
         TrcDocument doc;
         switch(ctx.getActionType())
         {
            case CREATE:
               doc = ArticleDocument.adapt(ctx.getModified());
               indexSvc.postDocument(doc);
               break;
            case EDIT:
               doc = ArticleDocument.adapt(ctx.getModified());
               indexSvc.postDocument(doc);
               break;
            case REMOVE:
               indexSvc.remove(ctx.getId());
               break;
         }
      }
      catch (Exception ex)
      {
         logger.log(Level.SEVERE, ex, () -> "Failed to index article {0}: " + ex.getMessage());
      }
   }

   private void configureVersioning(DocumentRepository<Article, DataModelV1.Article, EditArticleCommand> repo)
   {
      // TODO Auto-generated method stub
   }


   /**
    * Lifecycle management method (usually called by framework service layer)
    * Called when this service is no longer required.
    */
   public void dispose()
   {
      sqlExecutor = null;
   }

   /**
    * @param account The account to be used with this repository.
    * @return Obtains an {@code ArticleRepository} scoped to a particular user account.
    */
   @Override
   public ArticleRepository getArticleRepo(Account account)
   {
      return new ArticleRepoImpl(account);
   }

   /**
    * @return The article search service associated with this repository.
    *       Note that this may be {@code null} if no search service has been configured.
    */
   @Override
   public ArticleSearchService getSearchService()
   {
      return indexSvc;
   }

   public class ArticleRepoImpl implements ArticleRepository
   {

      @SuppressWarnings("unused")      // placeholder to be used once account creation has been integrated
      private Account account;

      public ArticleRepoImpl(Account account)
      {
         this.account = account;
      }

      @Override
      public Article get(String articleId)
      {
         return articleBackend.get(articleId);
      }

      @Override
      public List<Article> getArticles(URI entityURI) throws NoSuchEntryException
      {
         // This seems like a query rather than part of the article repo impl.
         throw new UnsupportedOperationException();
      }

      @Override
      public EditArticleCommand create()
      {
         String id = idFactory.get();
         return create(id);
      }

      @Override
      public EditArticleCommand create(String id)
      {
         return articleBackend.create(id);
      }

      @Override
      public EditArticleCommand edit(String articleId) throws NoSuchEntryException
      {
         try
         {
            return articleBackend.edit(articleId);
         }
         catch (RepositoryException e)
         {
            throw new IllegalArgumentException("Unable to find article with id {" + articleId + "}.", e);
         }
      }

      @Override
      public Future<Boolean> remove(String articleId)
      {
         CompletableFuture<Boolean> result = articleBackend.delete(articleId);

         result.thenRun(() -> {
               if (indexSvc != null)
                  indexSvc.remove(articleId);
            });

         return result;
      }

      @Override
      public Runnable register(Consumer<Article> ears)
      {
         return articleBackend.afterUpdate((dto) -> {
            // TODO adapt dto to Article
            ears.accept(null);
         });
      }
   }

   private static class ArticleResolver implements EntryResolver<Article>
   {
      private final ArticleRepoService articleSvc;
      private URI apiEndpoint;

      public ArticleResolver(ArticleRepoService articleSvc, ConfigurationProperties config)
      {
         this.articleSvc = articleSvc;
         this.apiEndpoint = config.getPropertyValue("trc.api.endpoint", URI.class, URI.create(""));
      }

      @Override
      public Article resolve(Account account, EntryReference reference) throws InvalidReferenceException
      {
         if (!accepts(reference.type))
            throw new InvalidReferenceException(reference, "Unsupported reference type.");

         ArticleRepository repo = articleSvc.getArticleRepo(account);
         return repo.get(reference.id);
      }

      @Override
      public URI toUri(EntryReference reference) throws InvalidReferenceException
      {
         if (!accepts(reference.type))
            throw new InvalidReferenceException(reference, "Unsupported reference type.");

         // format: <api_endpoint>/entries/articles/{articleId}
         return apiEndpoint.resolve(ArticleRepository.ENTRY_URI_BASE).resolve(reference.id);
      }

      @Override
      public EntryReference makeReference(Article instance) throws InvalidReferenceException
      {
         EntryReference ref = new EntryReference();
         ref.id = instance.getId();
         ref.type = ArticleRepository.ENTRY_TYPE_ID;

         return ref;
      }

      @Override
      public EntryReference makeReference(URI uri) throws InvalidReferenceException
      {
         URI articleId = uri.relativize(apiEndpoint.resolve(ArticleRepository.ENTRY_URI_BASE));
         if (articleId.equals(uri))
            throw new InvalidReferenceException(uri, "The supplied URI does not reference an article.");

         String path = articleId.getPath();
         if (path.contains("/"))
            throw new InvalidReferenceException(uri, "The supplied URI represents a sub-resource of an article.");

         EntryReference ref = new EntryReference();
         ref.id = path;
         ref.type = ArticleRepository.ENTRY_TYPE_ID;

         return ref;
      }

      @Override
      public boolean accepts(Object obj)
      {
         return (ArticleImpl.class.isInstance(obj));
      }

      @Override
      public boolean accepts(EntryReference ref)
      {
         return ArticleRepository.ENTRY_TYPE_ID.equals(ref.type);
      }

      @Override
      public boolean accepts(URI uri)
      {
         URI articleId = uri.relativize(apiEndpoint.resolve(ArticleRepository.ENTRY_URI_BASE));
//         The supplied URI does not reference an article
         if (articleId.equals(uri))
            return false;

         String path = articleId.getPath();
//         The supplied URI represents a sub-resource of an article.
         if (path.contains("/"))
            return false;

         return true;
      }
   }
}
