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
package edu.tamu.tcat.trc.entries.types.biblio.impl.search;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;

import edu.tamu.tcat.trc.entries.types.biblio.search.BiblioSearchProxy;
import edu.tamu.tcat.trc.entries.types.biblio.search.SearchWorksResult;
import edu.tamu.tcat.trc.entries.types.biblio.search.WorkQueryCommand;
import edu.tamu.tcat.trc.search.solr.SearchException;
import edu.tamu.tcat.trc.search.solr.impl.DateRangeDTO;
import edu.tamu.tcat.trc.search.solr.impl.TrcQueryBuilder;

public class WorkSolrQueryCommand implements WorkQueryCommand
{
   private static final int DEFAULT_MAX_RESULTS = 25;

   private final SolrClient solr;
   private final TrcQueryBuilder qb;

   private Set<String> authorIds;
   private List<DateRangeDTO> dates;

   public WorkSolrQueryCommand(SolrClient solr, TrcQueryBuilder qb)
   {
      this.solr = solr;
      this.qb = qb;
      qb.max(DEFAULT_MAX_RESULTS);
   }

   @Override
   public CompletableFuture<SearchWorksResult> execute() throws SearchException
   {
      CompletableFuture<SearchWorksResult> result = new CompletableFuture<>();
      try
      {
         if (authorIds != null)
            authorIds.stream().forEach(this::addAuthor);

         if (dates != null)
            dates.stream().forEach(this::addDateRange);

         QueryResponse response = solr.query(qb.get());
         SolrDocumentList results = response.getResults();

         List<BiblioSearchProxy> works = qb.unpack(results, BiblioSolrConfig.SEARCH_PROXY);
         result.complete(new SolrWorksResults(this, works));
      }
      catch (SolrServerException | IOException e)
      {
         result.completeExceptionally(new SearchException("An error occurred while querying the works core", e));
      }

      return result;
   }

   private void addDateRange(DateRangeDTO dr)
   {
      qb.filterRange(BiblioSolrConfig.PUBLICATION_DATE,
                     // For a "year" query, search from 1 Jan of start year through 31 Dec of end year (inclusive)
                     LocalDate.of(dr.start.getValue(), Month.JANUARY, 1),
                     LocalDate.of(dr.end.getValue(), Month.DECEMBER, 31));
   }

   private void addAuthor(String id)
   {
      qb.query(BiblioSolrConfig.AUTHOR_IDS, id);
   }

   @Override
   public void query(String q) throws SearchException
   {
      qb.basic(q);
   }

   public void queryAll() throws SearchException
   {
      qb.basic("*:*");
   }

   @Override
   @Deprecated
   public void queryType(String type) throws SearchException
   {
      // Add quotes so each term acts as a literal
//      qb.query(BiblioSolrConfig.TYPE, '"' + type + '"');
   }

   @Override
   public void queryTitle(String title) throws SearchException
   {
      // Add quotes so each term acts as a literal
      qb.query(BiblioSolrConfig.TITLES, '"' + title + '"');
   }

   @Override
   public void queryAuthorName(String authorName) throws SearchException
   {
      // Add quotes so each term acts as a literal
      qb.query(BiblioSolrConfig.AUTHOR_NAMES, '"' + authorName + '"' );
   }

   @Override
   public void addFilterAuthor(Collection<String> ids) throws SearchException
   {
      if (this.authorIds == null)
         this.authorIds = new HashSet<>();
      if (authorIds != null)
         this.authorIds.addAll(ids);
   }

   @Override
   public void clearFilterAuthor() throws SearchException
   {
      this.authorIds = null;
   }

   @Override
   public void addFilterDate(Year start, Year end) throws SearchException
   {
      if (dates == null)
         dates = new ArrayList<>();

      //TODO: validate date range overlaps
      dates.add(new DateRangeDTO(start, end));
   }

   @Override
   public void clearFilterDate() throws SearchException
   {
      dates = null;
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
}
