package edu.tamu.tcat.trc.categorization.rest;

import static java.text.MessageFormat.format;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import edu.tamu.tcat.trc.categorization.CategorizationRepo;
import edu.tamu.tcat.trc.categorization.CategorizationRepoFactory;
import edu.tamu.tcat.trc.categorization.CategorizationScope;

/**
 * Point of entry to the REST API for the Categorization system. This class is
 * designed to be configured and registered as an OSGi declarative service.
 *
 */
@Path("/")
public class CategorizationAPIService
{
   private final static Logger logger = Logger.getLogger(CategorizationAPIService.class.getName());

   private CategorizationRepoFactory repoProvider;

   public void bind(CategorizationRepoFactory repoProvider)
   {
      this.repoProvider = repoProvider;
   }

   public void activate()
   {
      logger.info("Activating Categorization REST API service");
   }

   @Path("/categorizations/{scope}")
   public CategorizationSchemesResource get(@PathParam("scope") String scopeId)
   {
      try
      {
         if (scopeId == null)
            throw new BadRequestException("No scope id provided");

         // TODO may adapt scope by translating username into account id

         CategorizationScope scope = repoProvider.createScope(null, scopeId);
         CategorizationRepo repository = repoProvider.getRepository(scope);

         return new CategorizationSchemesResource(repository);
      }
      catch (Exception ex)
      {
         String pattern = "Failed to obtain categorization scheme for scope {0}";
         logger.log(Level.SEVERE, format(pattern, scopeId), ex);
         throw new InternalServerErrorException(ex);
      }
   }
}
