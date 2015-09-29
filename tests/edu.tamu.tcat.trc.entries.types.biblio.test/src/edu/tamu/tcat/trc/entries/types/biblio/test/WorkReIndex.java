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
package edu.tamu.tcat.trc.entries.types.biblio.test;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.tamu.tcat.trc.persist.postgres.PsqlDataSourceProvider;

import edu.tamu.tcat.db.core.DataSourceException;
import edu.tamu.tcat.db.postgresql.exec.PostgreSqlExecutor;
import edu.tamu.tcat.osgi.config.file.SimpleFileConfigurationProperties;
import edu.tamu.tcat.trc.entries.types.bib.postgres.PsqlWorkRepo;
import edu.tamu.tcat.trc.entries.types.bib.search.solr.BiblioDocument;
import edu.tamu.tcat.trc.entries.types.biblio.Edition;
import edu.tamu.tcat.trc.entries.types.biblio.Volume;
import edu.tamu.tcat.trc.entries.types.biblio.Work;

public class WorkReIndex
{
   private final static Logger logger = Logger.getLogger(WorkReIndex.class.getName());

   private PostgreSqlExecutor exec;
   private SimpleFileConfigurationProperties config;
   private PsqlDataSourceProvider dsp;
   private PsqlWorkRepo repo;

   private SolrServer solr;
   public static final String SOLR_API_ENDPOINT = "solr.api.endpoint";
   public static final String SOLR_CORE = "catalogentries.works.solr.core";

   @Before
   public void setupTest() throws DataSourceException
   {
      Map<String, Object> params = new HashMap<>();
      params.put(SimpleFileConfigurationProperties.PROP_FILE, "config.path");
      config = new SimpleFileConfigurationProperties();
      config.activate(params);

      dsp = new PsqlDataSourceProvider();
      dsp.bind(config);
      dsp.activate();

      exec = new PostgreSqlExecutor();
      exec.init(dsp);

      repo = new PsqlWorkRepo();
      repo.setDatabaseExecutor(exec);
      repo.activate();

      // construct Solr core
      URI solrBaseUri = config.getPropertyValue(SOLR_API_ENDPOINT, URI.class);
      String solrCore = config.getPropertyValue(SOLR_CORE, String.class);

      URI coreUri = solrBaseUri.resolve(solrCore);
      logger.info("Connecting to Solr Service [" + coreUri + "]");

      solr = new HttpSolrServer(coreUri.toString());
   }

   @After
   public void tearDownTest() throws InterruptedException, ExecutionException
   {

      repo.dispose();
      exec.close();
      dsp.dispose();
      config.dispose();
      solr.shutdown();
   }

   @Test
   @Ignore
   public void testReIndex()
   {
      try
      {
         solr.deleteByQuery("*:*");
         solr.commit();
      }
      catch (SolrServerException | IOException e)
      {
         logger.log(Level.SEVERE, "Failed to remove data from the works core.");
      }

      Iterable<Work> works = repo.listWorks();

      Collection<SolrInputDocument> solrDocs = new ArrayList<>();

      for (Work work : works)
      {
         solrDocs.addAll(addWork(work));
      }

      try
      {
         solr.add(solrDocs);
         solr.commit();
      }
      catch (SolrServerException | IOException e)
      {
         logger.log(Level.SEVERE, "Failed to commit the works to the SOLR server. " + e);
      }
   }

   private static Collection<SolrInputDocument> addWork(Work work)
   {
      Collection<SolrInputDocument> solrDocs = new ArrayList<>();
      try
      {
         BiblioDocument workProxy = BiblioDocument.createWork(work);
         solrDocs.add(workProxy.getDocument());

         for(Edition edition : work.getEditions())
         {
            BiblioDocument editionProxy = BiblioDocument.createEdition(work.getId(), edition);
            solrDocs.add(editionProxy.getDocument());

            for(Volume volume : edition.getVolumes())
            {
               BiblioDocument volumeProxy = BiblioDocument.createVolume(work.getId(), edition, volume);
               solrDocs.add(volumeProxy.getDocument());
            }
         }
      }
      catch (Exception e)
      {
         throw new IllegalStateException("Failed to adapt created Work to indexable data transfer objects for work id: [" + work.getId() + "]", e);
      }

      return solrDocs;
   }

}
