package edu.tamu.tcat.trc.services.rest.notes;

import java.util.Objects;

import javax.ws.rs.Path;

import edu.tamu.tcat.trc.entries.core.resolver.EntryResolverRegistry;
import edu.tamu.tcat.trc.impl.psql.services.notes.NotesService;
import edu.tamu.tcat.trc.services.rest.notes.v1.NotesCollectionResource;


@Path("/")
public class NotesApiService
{
   private NotesService notesSvc;

   private EntryResolverRegistry resolvers;

   public void setRepository(NotesService notes)
   {
      this.notesSvc = notes;
   }

   public void setResolvers(EntryResolverRegistry resolvers)
   {
      this.resolvers = resolvers;
   }

   public void activate()
   {
      // FIXME
      Objects.requireNonNull(notesSvc, "Notes Repsoitory was not setup correctly.");
   }

   public void dispose()
   {
      notesSvc = null;
   }

   @Path("services/notes")
   public NotesCollectionResource getNotes()
   {
      return new NotesCollectionResource(notesSvc.getRepository(null), resolvers, null);
   }
}