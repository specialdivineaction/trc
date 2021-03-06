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
package edu.tamu.tcat.trc.digires.books.hathitrust;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import edu.tamu.tcat.osgi.config.ConfigurationProperties;
import edu.tamu.tcat.trc.digires.books.discovery.ContentQuery;
import edu.tamu.tcat.trc.digires.books.discovery.CopySearchResult;
import edu.tamu.tcat.trc.digires.books.discovery.CopySearchResultImpl;
import edu.tamu.tcat.trc.digires.books.discovery.CopySearchService;
import edu.tamu.tcat.trc.digires.books.discovery.DigitalCopyProxy;
import edu.tamu.tcat.trc.digires.books.resolve.ResourceAccessException;

/**
 *  Searches over the local SOLR index of Hathifiles data. Note that this index may be expanded
 *  to incorporate resource that are not hosted by Hathi Trust, depending on future design
 *  decisions.
 *
 *  Intended to be registered and configured as an OSGi service.
 */
public class HTFilesSearchService implements CopySearchService
{
   // FIXME move to hathitrust SDK
   private static final Logger logger = Logger.getLogger(HTFilesSearchService.class.getName());

   private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_INSTANT;

   private Future<HttpSolrClient> solrServerFuture;
   private ConfigurationProperties props;

   public void setConfiguration(ConfigurationProperties props)
   {
      this.props = props;
   }

   public void activate()
   {
      Objects.requireNonNull(props, "Cannot connect to Solr Server. Configuration data is not available.");

      final URI solrEndpoint = props.getPropertyValue("solr.api.endpoint", URI.class);
      final String core = props.getPropertyValue("hathifiles", String.class);
      ExecutorService exec = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("htfiles").build());
      // check to ensure that the requested SolrServer can be contacted. Run this in a background
      // thread to not prevent the service from initializing in a timely manner. Use the Future to
      // determine that the ping succeeded. Each access to the Future will either return the server
      // or re-throw the original exception, allowing it to be noticed when requested as well as
      // on system startup.
      solrServerFuture = exec.submit(() -> {
         try
         {
            HttpSolrClient server = new HttpSolrClient(solrEndpoint.resolve(core).toString());
            SolrPingResponse pingResponse = server.ping();
            if (pingResponse == null || (pingResponse.getStatus() > 299 && pingResponse.getStatus() < 200))
            {
               //TODO: server.shutdown() here? ping failed, but still need to release resources?
               throw new IllegalStateException("Failed to ping configured solr server [" + solrEndpoint.resolve(core) + "]: " + pingResponse);
            }
            return server;
         }
         catch (IOException | SolrServerException ex)
         {
            //TODO: server.shutdown() here? ping failed, but still need to release resources?
            throw new IllegalStateException("Failed to ping configured solr server [" + solrEndpoint.resolve(core) + "]", ex);
         }
         finally
         {
            // only needed the executor for this task, so shut it down and retain the Future
            exec.shutdown();
         }
      });
   }

   public void deactivate()
   {
      if (solrServerFuture != null)
      {
         try
         {
            // don't wait long, it should be either here or not at this point
            solrServerFuture.get(1, TimeUnit.SECONDS).close();
         }
         catch (Exception ex)
         {
            logger.log(Level.SEVERE, "Failed to shut down connection to solr server", ex);
         }

         solrServerFuture = null;
      }
   }

   @Override
   public CopySearchResult find(ContentQuery query) throws ResourceAccessException
   {
      if (solrServerFuture == null)
         throw new IllegalStateException("Service is disposed");

      // HACK hard coded. Should be provided to the service
      try
      {
         HttpSolrClient solrServer = solrServerFuture.get(2, TimeUnit.MINUTES);
         Objects.requireNonNull(solrServer, "No active connection to Solr Server");

         String queryString = formatQueryString(query);

         SolrQuery solrQuery = new SolrQuery(queryString);
         solrQuery.setRows(Integer.valueOf(query.getLimit()));
         solrQuery.setStart(Integer.valueOf(query.getOffset()));
//         solrQuery.addFilterQuery(buildDateFilter(query));
         QueryResponse response = solrServer.query(solrQuery);
         return getSearchResults(response);
      }
      catch (Exception ex)
      {
         throw new ResourceAccessException("", ex);
      }

   }

   private CopySearchResult getSearchResults(QueryResponse response)
   {
      SolrDocumentList documents = response.getResults();
      Collection<DigitalCopyProxy> digitalProxy = new ArrayList<>();
      for(SolrDocument doc : documents)
      {
         String htid = doc.getFieldValue("id").toString();
         String recordId = doc.getFieldValue("recordNumber").toString();

         HTCopyProxy htProxy = new HTCopyProxy();
         htProxy.identifier = buildIdentifier(recordId, htid);
         htProxy.sourceSummary = doc.getFieldValue("source").toString();
         htProxy.title = doc.getFieldValue("title").toString();
         htProxy.rights = doc.getFieldValue("rights").toString();
         if(doc.containsKey("publicationDate"))
            htProxy.publicationDate = doc.getFieldValue("publicationDate").toString();
         else
            htProxy.publicationDate = "";

         digitalProxy.add(htProxy);
      }
      return new CopySearchResultImpl(digitalProxy);
   }

   private String buildIdentifier(String recordId, String htid)
   {
      StringBuilder identifier = new StringBuilder("htid:");
      identifier.append(recordId)
                .append("#")
                .append(htid);
      return identifier.toString();
   }

   private String trimToNull(String str)
   {
      return (str == null || str.trim().isEmpty()) ? null : str.trim();
   }

   private String formatQueryString(ContentQuery query) throws UnsupportedEncodingException
   {
      StringBuilder qBuilder = new StringBuilder();

      buildMainQuery(query, qBuilder);
//      buildDateFilter(query, qBuilder);

      return qBuilder.toString();
   }

   private void buildMainQuery(ContentQuery query, StringBuilder qBuilder) throws UnsupportedEncodingException
   {
      // TODO sanitize filter - remove &'s, etc.

      // append the required query value
      String keyWordQuery = trimToNull(query.getKeyWordQuery());
      if (keyWordQuery == null)
         throw new IllegalArgumentException("Invalid copy query [" + query + "]. No keyword query supplied.");

      // add author query info
      String authorQ = trimToNull(query.getAuthorQuery());
      if (authorQ != null)
      {
         // HathiTrust records this in the title field, if at all, so append to keywords.
         keyWordQuery += " " + authorQ;
      }

      //         ?q=title%3A(essay+hume)&fq=publicationDate%3A%5B1700-01-01T00%3A00%3A00Z+TO+1800-01-01T00%3A00%3A00Z%5D
      keyWordQuery = URLEncoder.encode(keyWordQuery, "UTF-8");    // UTF-8 required by standard
      qBuilder.append("title:").append("(").append(keyWordQuery).append(")");
   }

   private String buildDateFilter(ContentQuery query)
   {
      StringBuilder sb = new StringBuilder();
      // add filter for date range.
      //fq=publicationDate%3A%5B1700-01-01T00%3A00%3A00Z+TO+1800-01-01T00%3A00%3A00Z%5D
      TemporalAccessor rangeStart = query.getDateRangeStart();
      TemporalAccessor rangeEnd = query.getDateRangeEnd();
      if (rangeStart != null || rangeEnd != null)
      {
         // FIXME prevent null pointer access
         ZonedDateTime startDate = ZonedDateTime.of(rangeStart.get(ChronoField.YEAR), 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
         ZonedDateTime endDate = ZonedDateTime.of(rangeEnd.get(ChronoField.YEAR), 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());

         String start = (rangeStart == null) ? "*": dateFormatter.format(startDate);
         String end = (rangeEnd == null) ? "*": dateFormatter.format(endDate);

         sb.append("publicationDate:")
                 .append("[")
                 .append(start)
                 .append(" TO ")
                 .append(end)
                 .append("]");
      }
      return sb.toString();
   }
}
