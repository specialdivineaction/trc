/**
 *
 */
package edu.tamu.tcat.trc.impl.psql.services.bibref.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import edu.tamu.tcat.trc.impl.psql.services.bibref.repo.DataModelV1;
import edu.tamu.tcat.trc.services.bibref.BibliographicItem;
import edu.tamu.tcat.trc.services.bibref.Citation;
import edu.tamu.tcat.trc.services.bibref.ReferenceCollection;

public class ReferenceCollectionImpl implements ReferenceCollection
{
   private final Collection<Citation> citations = new ArrayList<>();
   private final Map<String, BibliographicItem> items = new HashMap<>();

   public ReferenceCollectionImpl()
   {
   }

   public ReferenceCollectionImpl(DataModelV1.ReferenceCollection dto)
   {
      if (dto == null)
         return;

      dto.citations.values().stream()
            .map(CitationImpl::new)
            .forEach(citations::add);

      dto.items.values().stream()
            .map(BibliographicItemImpl::new)
            .forEach(item -> items.put(item.getItemId(), item));
   }

   @Override
   public Collection<Citation> getCitations()
   {
      return Collections.unmodifiableCollection(citations);
   }

   @Override
   public BibliographicItem getItem(String id)
   {
      return items.get(id);
   }

   @Override
   public Collection<BibliographicItem> getItems()
   {
      return items.values();
   }

}
