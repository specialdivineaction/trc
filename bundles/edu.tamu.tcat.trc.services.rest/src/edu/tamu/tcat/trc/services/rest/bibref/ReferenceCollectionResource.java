package edu.tamu.tcat.trc.services.rest.bibref;

import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import edu.tamu.tcat.trc.resolver.EntryId;
import edu.tamu.tcat.trc.services.bibref.ReferenceCollection;
import edu.tamu.tcat.trc.services.bibref.repo.BibliographicItemMetaMutator;
import edu.tamu.tcat.trc.services.bibref.repo.BibliographicItemMutator;
import edu.tamu.tcat.trc.services.bibref.repo.BibliographicItemReferenceMutator;
import edu.tamu.tcat.trc.services.bibref.repo.CitationMutator;
import edu.tamu.tcat.trc.services.bibref.repo.EditBibliographyCommand;
import edu.tamu.tcat.trc.services.bibref.repo.RefCollectionService;
import edu.tamu.tcat.trc.services.rest.ApiUtils;

public class ReferenceCollectionResource
{
   private final RefCollectionService repo;
   private final EntryId targetRef;

   public ReferenceCollectionResource(RefCollectionService repo, EntryId targetRef)
   {
      // TODO refactor to use TrcApplication
      this.repo = repo;
      this.targetRef = targetRef;
   }

   @GET
   @Produces(MediaType.APPLICATION_JSON)
   public BibRefRestApiV1.ReferenceCollection get()
   {
      ReferenceCollection referenceCollection = repo.get(targetRef);
      return BibRefRestAdapter.toDTO(referenceCollection);
   }

   @PUT
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   public BibRefRestApiV1.ReferenceCollection put(BibRefRestApiV1.ReferenceCollection dto)
   {
      return save(dto);
   }

   @POST
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   public BibRefRestApiV1.ReferenceCollection save(BibRefRestApiV1.ReferenceCollection dto)
   {
      EditBibliographyCommand editCommand = repo.edit(targetRef);
      apply(editCommand, dto);
      return execute(editCommand);
   }

   @DELETE
   public void delete()
   {
      repo.delete(targetRef);
   }

   private void apply(EditBibliographyCommand command, BibRefRestApiV1.ReferenceCollection dto)
   {
      if (dto == null)
         return;

      command.removeAllCitations();
      dto.citations.forEach((id, citation) -> {
         if (citation == null)
         {
            String msg = MessageFormat.format("citation {0} is null", id);
            throw ApiUtils.raise(Response.Status.BAD_REQUEST, msg, Level.WARNING, null);
         }

         if (citation.id != null && !id.equals(citation.id))
         {
            String msg = MessageFormat.format("id mismatch for citation {0} ({1})", id, citation.id);
            throw ApiUtils.raise(Response.Status.BAD_REQUEST, msg, Level.WARNING, null);
         }

         CitationMutator citationMutator = command.addCitation(id);
         apply(citationMutator, citation);
      });

      command.removeAllItems();
      dto.bibliography.forEach((id, item) -> {
         if (item == null)
         {
            String msg = MessageFormat.format("citation {0} is null", id);
            throw ApiUtils.raise(Response.Status.BAD_REQUEST, msg, Level.WARNING, null);
         }

         if (item.id != null && !id.equals(item.id))
         {
            String msg = MessageFormat.format("id mismatch for bibliographic item {0} ({1})", id, item.id);
            throw ApiUtils.raise(Response.Status.BAD_REQUEST, msg, Level.WARNING, null);
         }

         BibliographicItemMutator itemMutator = command.addItem(id);
         apply(itemMutator, item);
      });
   }

   private void apply(CitationMutator mutator, BibRefRestApiV1.Citation dto)
   {
      if (dto == null)
         return;

      mutator.removeAllItemRefs();
      dto.items.forEach(ref -> {
         BibliographicItemReferenceMutator bibItemRefMutator = mutator.addItemRef(ref.id);
         apply(bibItemRefMutator, ref);
      });
   }

   private void apply(BibliographicItemReferenceMutator mutator, BibRefRestApiV1.BibliographicItemReference dto)
   {
      if (dto == null)
         return;

      mutator.setLocatorType(dto.locatorType);
      mutator.setLocator(dto.locator);
      mutator.setLabel(dto.label);
      mutator.supppressAuthorName(dto.suppressAuthor);
   }

   private void apply(BibliographicItemMutator mutator, BibRefRestApiV1.BibliographicItem dto)
   {
      if (dto == null)
         return;

      mutator.setType(dto.type);

      BibliographicItemMetaMutator metaMutator = mutator.editMetadata();
      apply(metaMutator, dto.meta);

      List<BibliographicItemMutator.Creator> creators = dto.creators.stream()
            .map(BibRefRestAdapter::toRepo)
            .collect(Collectors.toList());
      mutator.setCreators(creators);

      mutator.setAllFields(dto.fields);
   }

   private void apply(BibliographicItemMetaMutator mutator, BibRefRestApiV1.BibliographicItemMeta dto)
   {
      if (dto == null)
         return;

      mutator.setKey(dto.key);
      mutator.setCreatorSummary(dto.creatorSummary);
      mutator.setParsedDate(dto.parsedDate);
      mutator.setDateAdded(dto.dateAdded);
      mutator.setDateModified(dto.dateModified);
   }

   private BibRefRestApiV1.ReferenceCollection execute(EditBibliographyCommand command)
   {
      try
      {
         command.execute().get(10, TimeUnit.SECONDS);
         ReferenceCollection referenceCollection = repo.get(targetRef);
         return BibRefRestAdapter.toDTO(referenceCollection);
      }
      catch(InterruptedException | TimeoutException e)
      {
         String msg = "The server seems to be busy right now. Please try again later.";
         throw ApiUtils.raise(Response.Status.SERVICE_UNAVAILABLE, msg, Level.WARNING, e);
      }
      catch(ExecutionException e)
      {
         String msg = "Failed to save bibliographic reference collection";
         throw ApiUtils.handleExecutionException(msg, e);
      }
   }
}
