package edu.tamu.tcat.trc.entries.types.reln.search.solr;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.solr.common.SolrInputDocument;

import edu.tamu.tcat.trc.entries.search.SearchException;
import edu.tamu.tcat.trc.entries.search.solr.SolrIndexField;
import edu.tamu.tcat.trc.entries.search.solr.impl.TrcDocument;
import edu.tamu.tcat.trc.entries.types.reln.Relationship;
import edu.tamu.tcat.trc.entries.types.reln.dto.AnchorDTO;
import edu.tamu.tcat.trc.entries.types.reln.dto.ProvenanceDTO;
import edu.tamu.tcat.trc.entries.types.reln.dto.RelationshipDTO;
import edu.tamu.tcat.trc.entries.types.reln.search.RelnSearchProxy;

/**
 *  A data structure for representing the searchable fields associated with a {@link Relationship}.
 */
public class RelnDocument
{
   private final static Logger logger = Logger.getLogger(RelnDocument.class.getName());

   // composed instead of extended to not expose TrcDocument as API to this class
   private TrcDocument indexDocument;

   public RelnDocument()
   {
      indexDocument = new TrcDocument(new RelnSolrConfig());
   }

   public SolrInputDocument getDocument()
   {
      return indexDocument.getSolrDocument();
   }

   public static RelnDocument create(Relationship reln) throws SearchException
   {
      RelnDocument doc = new RelnDocument();
      RelationshipDTO relnDV = RelationshipDTO.create(reln);

      doc.indexDocument.set(RelnSolrConfig.ID, relnDV.id);
      doc.indexDocument.set(RelnSolrConfig.DESCRIPTION, relnDV.description);
      doc.indexDocument.set(RelnSolrConfig.DESCRIPTION_MIME_TYPE, relnDV.descriptionMimeType);
      doc.indexDocument.set(RelnSolrConfig.REL_TYPE, relnDV.typeId);
      doc.addEntities(RelnSolrConfig.RELATED_ENTITIES, relnDV.relatedEntities);
      doc.addEntities(RelnSolrConfig.TARGET_ENTITIES, relnDV.targetEntities);
      doc.addProvenance(relnDV.provenance);

      try
      {
         doc.indexDocument.set(RelnSolrConfig.SEARCH_PROXY, RelnSearchProxy.create(reln));
      }
      catch (Exception e)
      {
         throw new IllegalStateException("Failed to serialize Relationship Search Proxy data", e);
      }

      return doc;
   }

   public static RelnDocument update(Relationship reln) throws SearchException
   {
      RelnDocument doc = new RelnDocument();
      RelationshipDTO relnDV = RelationshipDTO.create(reln);

      doc.indexDocument.update(RelnSolrConfig.ID, relnDV.id);

      doc.indexDocument.update(RelnSolrConfig.DESCRIPTION, relnDV.description);
      doc.indexDocument.update(RelnSolrConfig.DESCRIPTION_MIME_TYPE, relnDV.descriptionMimeType);
      doc.indexDocument.update(RelnSolrConfig.REL_TYPE, relnDV.typeId);
      doc.updateEntities(RelnSolrConfig.RELATED_ENTITIES, relnDV.relatedEntities);
      doc.updateEntities(RelnSolrConfig.TARGET_ENTITIES, relnDV.targetEntities);
      doc.updateProvenance(relnDV.provenance);

      try
      {
         doc.indexDocument.update(RelnSolrConfig.SEARCH_PROXY, RelnSearchProxy.create(reln));
      }
      catch (Exception e)
      {
         throw new IllegalStateException("Failed to serialize Relationship Search Proxy data", e);
      }

      return doc;
   }

   private void addEntities(SolrIndexField<String> field, Set<AnchorDTO> anchors) throws SearchException
   {
      for (AnchorDTO anchor : anchors)
      {
         for (String uri : anchor.entryUris)
         {
            indexDocument.set(field, uri);
         }
      }
   }

   private void updateEntities(SolrIndexField<String> field, Set<AnchorDTO> anchors) throws SearchException
   {
      Set<String> allEntities = new HashSet<>();

      for (AnchorDTO anchor : anchors)
      {
         allEntities.addAll(anchor.entryUris);
      }

      indexDocument.update(field, allEntities);
   }

   private void addProvenance(ProvenanceDTO prov) throws SearchException
   {
      Set<String> uris = new HashSet<>();
      for (String uri : uris)
         indexDocument.set(RelnSolrConfig.PROV_CREATORS, uri);

      if (prov.dateCreated != null)
         indexDocument.set(RelnSolrConfig.PROV_CREATED_DATE, Instant.from(DateTimeFormatter.ISO_INSTANT.parse(prov.dateCreated)));
      if (prov.dateModified != null)
         indexDocument.set(RelnSolrConfig.PROV_MODIFIED_DATE, Instant.from(DateTimeFormatter.ISO_INSTANT.parse(prov.dateModified)));
   }

   private void updateProvenance(ProvenanceDTO prov) throws SearchException
   {
      indexDocument.update(RelnSolrConfig.PROV_CREATORS, prov.creatorUris);
      if (prov.dateCreated != null)
         indexDocument.update(RelnSolrConfig.PROV_CREATED_DATE, Instant.from(DateTimeFormatter.ISO_INSTANT.parse(prov.dateCreated)));
      if (prov.dateModified != null)
         indexDocument.update(RelnSolrConfig.PROV_MODIFIED_DATE, Instant.from(DateTimeFormatter.ISO_INSTANT.parse(prov.dateModified)));
   }
}