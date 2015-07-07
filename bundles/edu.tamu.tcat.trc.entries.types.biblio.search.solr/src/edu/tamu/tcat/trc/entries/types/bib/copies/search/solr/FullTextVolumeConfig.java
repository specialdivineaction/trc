package edu.tamu.tcat.trc.entries.types.bib.copies.search.solr;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.solr.client.solrj.SolrQuery;

import edu.tamu.tcat.trc.search.SearchException;
import edu.tamu.tcat.trc.search.solr.SolrIndexConfig;
import edu.tamu.tcat.trc.search.solr.SolrIndexField;
import edu.tamu.tcat.trc.search.solr.impl.BasicFields;

/**
 * Defines the Solr configuration for indexing full text search results on a page level.
 */
public class FullTextVolumeConfig implements SolrIndexConfig
{

   public static final SolrIndexField<String> ID = new BasicFields.BasicString("id");
   public static final SolrIndexField<String> ASSOC_ENTRY = new BasicFields.BasicString("associatedEntry");
   public static final SolrIndexField<String> TEXT = new BasicFields.BasicString("volumeText");

   public FullTextVolumeConfig()
   {
      // TODO Auto-generated constructor stub
   }

   @Override
   public void initialConfiguration(SolrQuery params) throws SearchException
   {
      /*
       * Using eDisMax seemed like a more adventagous way of doing the query. This will allow
       * additional solr Paramaters to be set in order to 'fine tune' the query.
       */
      params.set("defType", "edismax");
   }

   @Override
   public Class<Void> getSearchProxyType()
   {
      return Void.class;
   }

   @Override
   public Class<VolumeTextDocument> getIndexDocumentType()
   {
      return VolumeTextDocument.class;
   }

   @Override
   public Collection<? extends SolrIndexField<?>> getIndexedFields()
   {
      return Arrays.asList(ID, ASSOC_ENTRY, TEXT);
   }

   @Override
   public Collection<? extends SolrIndexField<?>> getStoredFields()
   {
      return Arrays.asList(ID, ASSOC_ENTRY, TEXT);
   }

   @Override
   public Collection<? extends SolrIndexField<?>> getMultiValuedFields()
   {
      return Collections.emptyList();
   }

}
