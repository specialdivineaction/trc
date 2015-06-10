package edu.tamu.tcat.trc.entries.types.reln.search.solr;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;

import org.apache.solr.client.solrj.SolrQuery;

import edu.tamu.tcat.trc.entries.search.SearchException;
import edu.tamu.tcat.trc.entries.search.solr.SolrIndexConfig;
import edu.tamu.tcat.trc.entries.search.solr.SolrIndexField;
import edu.tamu.tcat.trc.entries.search.solr.impl.BasicFields;
import edu.tamu.tcat.trc.entries.types.reln.search.RelnSearchProxy;

public class RelnSolrConfig implements SolrIndexConfig
{
   public static final SolrIndexField<String> ID = new BasicFields.BasicString("id");
   public static final BasicFields.SearchProxyField<RelnSearchProxy> SEARCH_PROXY = new BasicFields.SearchProxyField<RelnSearchProxy>("relationshipModel", RelnSearchProxy.class);
   public static final SolrIndexField<String> DESCRIPTION = new BasicFields.BasicString("description");
   public static final SolrIndexField<String> DESCRIPTION_MIME_TYPE = new BasicFields.BasicString("descriptionMimeType");
   public static final SolrIndexField<String> REL_TYPE = new BasicFields.BasicString("relationshipType");
   public static final SolrIndexField<String> RELATED_ENTITIES = new BasicFields.BasicString("relatedEntities");
   public static final SolrIndexField<String> TARGET_ENTITIES = new BasicFields.BasicString("targetEntities");
   public static final SolrIndexField<String> PROV_CREATORS = new BasicFields.BasicString("provCreator");
   // Using LocalDate for yyyy-MM-dd
   public static final SolrIndexField<LocalDate> PROV_CREATED_DATE = new BasicFields.BasicDate<LocalDate>("provCreateDate", LocalDate.class);
   public static final SolrIndexField<LocalDate> PROV_MODIFIED_DATE = new BasicFields.BasicDate<LocalDate>("provModifiedDate", LocalDate.class);

   @Override
   public void initialConfiguration(SolrQuery params)
   {
      //HACK: relationship query does not use edismax
//      /*
//       * Using eDisMax seemed like a more adventagous way of doing the query. This will allow
//       * additional solr Paramaters to be set in order to 'fine tune' the query.
//       */
//      params.set("defType", "edismax");
   }

   @Override
   public void configureBasic(String q, SolrQuery params) throws SearchException
   {
      //HACK: if no query specified, should this throw and require a call to queryAll() ?
      if (q == null || q.trim().isEmpty())
         q = "*:*";

      // NOTE query against all fields, boosted appropriately, free text
      //      I think that means *:(qBasic)
      // NOTE in general, if this is applied, the other query params are unlikely to be applied
      StringBuilder qBuilder = new StringBuilder(q);

      params.set("q", qBuilder.toString());

      //HACK: relationship query does not use edismax
      // Basic query only searches over these fields
      //params.set("qf", "syntheticName");
   }

   @Override
   public Class<RelnSearchProxy> getSearchProxyType()
   {
      return RelnSearchProxy.class;
   }

   @Override
   public Class<RelnDocument> getIndexDocumentType()
   {
      return RelnDocument.class;
   }

   @Override
   public Collection<? extends SolrIndexField<?>> getIndexedFields()
   {
      return Arrays.asList(ID,
                           DESCRIPTION,
                           DESCRIPTION_MIME_TYPE,
                           REL_TYPE,
                           RELATED_ENTITIES,
                           TARGET_ENTITIES,
                           PROV_CREATORS,
                           PROV_CREATED_DATE,
                           PROV_MODIFIED_DATE
                           );
   }

   @Override
   public Collection<? extends SolrIndexField<?>> getStoredFields()
   {
      return Arrays.asList(ID,
                           SEARCH_PROXY,
                           DESCRIPTION,
                           DESCRIPTION_MIME_TYPE,
                           REL_TYPE,
                           RELATED_ENTITIES,
                           TARGET_ENTITIES,
                           PROV_CREATORS,
                           PROV_CREATED_DATE,
                           PROV_MODIFIED_DATE);
   }

   @Override
   public Collection<? extends SolrIndexField<?>> getMultiValuedFields()
   {
      return Arrays.asList(RELATED_ENTITIES,
                           TARGET_ENTITIES,
                           PROV_CREATORS);
   }
}
