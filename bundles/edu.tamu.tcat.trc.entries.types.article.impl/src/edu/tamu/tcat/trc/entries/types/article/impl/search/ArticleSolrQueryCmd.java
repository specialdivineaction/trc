package edu.tamu.tcat.trc.entries.types.article.impl.search;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;

import edu.tamu.tcat.trc.entries.types.article.search.ArticleQuery;
import edu.tamu.tcat.trc.entries.types.article.search.ArticleQueryCommand;
import edu.tamu.tcat.trc.entries.types.article.search.ArticleSearchProxy;
import edu.tamu.tcat.trc.entries.types.article.search.ArticleSearchResult.FacetValueList;
import edu.tamu.tcat.trc.search.SearchException;
import edu.tamu.tcat.trc.search.solr.impl.TrcQueryBuilder;

/**
 * @since 1.1
 */
public class ArticleSolrQueryCmd implements ArticleQueryCommand
{

   private static final int DEFAULT_MAX_RESULTS = 25;

   private final ArticleQuery query;;
   private final SolrClient solr;
   private final TrcQueryBuilder qb;

   public ArticleSolrQueryCmd(SolrClient solr, ArticleQuery query, TrcQueryBuilder qb)
   {
      this.query = query;
      this.solr = solr;
      this.qb = qb;

      this.query.max = DEFAULT_MAX_RESULTS;

      qb.hitHighlight(query.highlighting);
      qb.max(DEFAULT_MAX_RESULTS);
   }

   @Override
   public void setQuery(String q)
   {
      this.query.q = q;
   }

   @Override
   public void setOffset(int start)
   {
      if (start < 0)
         throw new IllegalArgumentException("Offset ["+start+"] cannot be negative");

      qb.offset(start);
   }

   @Override
   public void setMaxResults(int max)
   {
      qb.max(max);
   }

   @Override
   public SolrArticleResults execute() throws SearchException
   {
      String q = (query.q == null || query.q.trim().isEmpty()) ? "*:*" : query.q;
      qb.basic(q);

      try
      {
         QueryResponse response = solr.query(qb.get());

         SolrDocumentList results = response.getResults();

         // TODO need to parse the highlighted results into something more meaningful.
         //      should add to article proxy.

         // HACK what is this structure - presumably it is field/document id or vice/versa, but this
         //      isn't clear.
         Map<String, Map<String, List<String>>> highlighting = response.getHighlighting();
         List<FacetValueList> facets = response.getFacetFields().stream()
               .map(SolrArticleResults::adapt)
               .collect(Collectors.toList());

         List<ArticleSearchProxy> articles = qb.unpack(results, ArticleSolrConfig.SEARCH_PROXY);
         return new SolrArticleResults(query, articles, highlighting, facets, results.getNumFound());
      }
      catch (Exception e)
      {
         throw new SearchException("An error occurred while querying the article core: " + e, e);
      }
   }





}