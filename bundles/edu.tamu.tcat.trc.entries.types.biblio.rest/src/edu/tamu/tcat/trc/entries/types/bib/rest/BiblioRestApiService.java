package edu.tamu.tcat.trc.entries.types.bib.rest;

import java.util.Objects;

import javax.ws.rs.Path;

import edu.tamu.tcat.trc.entries.types.bib.rest.v1.WorkCollectionResource;
import edu.tamu.tcat.trc.entries.types.biblio.repo.WorkRepository;
import edu.tamu.tcat.trc.entries.types.biblio.search.WorkSearchService;

@Path("/")
public class BiblioRestApiService
{
   private WorkRepository repo;
   private WorkSearchService searchSvc;

   /**
    * Bind method for persistence component (usually called by framework dependency injection layer)
    *
    * @param repo
    */
   public void setRepository(WorkRepository repo)
   {
      this.repo = repo;
   }

   /**
    * Bind method for search component (usually called by framework dependency injection layer)
    *
    * @param workSearchService
    */
   public void setWorkService(WorkSearchService workSearchService)
   {
      this.searchSvc = workSearchService;
   }

   /**
    * Lifecycle management callback (usually called by framework service layer)
    * This method should be called once all dependent services have been provided and the resource
    * is ready to start responding to requests.
    */
   public void activate()
   {
      Objects.requireNonNull(repo, "No works repository provided.");
      Objects.requireNonNull(searchSvc, "No work search service provided.");
   }

   @Path("v1/works")
   public WorkCollectionResource getV1Endpoint()
   {
      return new WorkCollectionResource(repo, searchSvc);
   }

   @Path("works")
   public WorkCollectionResource getDefaultEndpoint()
   {
      return new WorkCollectionResource(repo, searchSvc);
   }


}
