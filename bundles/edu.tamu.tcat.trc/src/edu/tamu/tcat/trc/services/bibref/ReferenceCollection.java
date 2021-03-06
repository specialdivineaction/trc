package edu.tamu.tcat.trc.services.bibref;

import java.util.Collection;

import edu.tamu.tcat.account.Account;
import edu.tamu.tcat.trc.services.BasicServiceContext;
import edu.tamu.tcat.trc.services.ServiceContext;

/**
 *  Defines a collection of bibliographic references associated with some entity
 *  (typically, but not necessarily) a TRC Entry). A reference collection is defined
 *  by both a collection of bibliographic items as well as a set of citations that
 *  defines how those items are used within the context of the associated entity.
 *  Note that not all items will be cited.
 */
public interface ReferenceCollection
{
   static ServiceContext<ReferenceCollection> makeContext(Account account)
   {
      return new BasicServiceContext<>(ReferenceCollection.class, account);
   }

   /**
    * @return all citations
    */
   Collection<Citation> getCitations();

   /**
    * Finds an individual bibliographic item. Intended principally to lookup bibliographic
    * items referenced in a citation.
    *
    * @param id The id of the bibliographic item.
    * @return The bibliographic item or {@code null} if not found.
    */
   BibliographicItem getItem(String id);

   /**
    * @return All bibliographic items in this bibliography.
    */
   Collection<BibliographicItem> getItems();
}
