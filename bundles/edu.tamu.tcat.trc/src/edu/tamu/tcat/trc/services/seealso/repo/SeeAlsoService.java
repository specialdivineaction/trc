package edu.tamu.tcat.trc.services.seealso.repo;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.tamu.tcat.account.Account;
import edu.tamu.tcat.trc.services.BasicServiceContext;
import edu.tamu.tcat.trc.services.ServiceContext;
import edu.tamu.tcat.trc.services.seealso.Link;

/**
 * A service that provides a lightweight mechanism for establishing relationships between string-identifiable entries.
 * This service differs from TRC Relationships in that links are untyped and always directed.
 */
public interface SeeAlsoService
{
   static ServiceContext<SeeAlsoService> makeContext(Account account)
   {
      return new BasicServiceContext<>(SeeAlsoService.class, account);
   }

   /**
    * Creates a new link from the given source entry to the given target entry.
    * This operation is idempotent: if a link already exists, then no changes will be made.
    *
    * @param source A token that uniquely identifies the source entry
    * @param target A token that uniquely identifies the target entry
    * @return A new link record after it has been successfully persisted
    */
   Link create(String source, String target);

   /**
    * Creates links to multiple targets.
    *
    * @param source A token that uniquely identifies the source entry
    * @param targets A set of identifiers (preferably a set of them since duplicates will be ignored)
    * @return All created links
    */
   default Collection<Link> createAll(String source, String... targets)
   {
      return Stream.of(targets)
         .map(target -> create(source, target))
         .collect(Collectors.toList());
   }

   /**
    * Tests whether the given link exists.
    *
    * @param source A token that uniquely identifies the source entry
    * @param target A token that uniquely identifies the target entry
    * @return {@code true} if the link exists
    */
   boolean isRelated(String source, String target);

   /**
    * @param id The token for which to find related entries
    * @return All links for which the provided identifier is listed as either the source or the target
    */
   Collection<Link> getFor(String id);

   /**
    * @param source The token for which to find related entries
    * @return All links for which the provided identifier is listed as the source
    */
   Collection<Link> getFrom(String source);

   /**
    * @param target The token for which to find related entries
    * @return All links for which the provided identifier is listed as the target
    */
   Collection<Link> getTo(String target);

   /**
    * Removes the specified link if it exists
    * @param source A token that uniquely identifies the source entry
    * @param target A token that uniquely identifies the target entry
    * @return {@code true} if the persistence layer was changed by the deletion
    */
   boolean delete(String source, String target);

   /**
    * Removes all links for which the identified entry is the source or target
    * @param id A token that uniquely identifies an entry
    * @return {@code true} if the persistence layer was changed by the deletion
    */
   boolean delete(String id);
}
