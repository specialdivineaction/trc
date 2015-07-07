package edu.tamu.tcat.trc.entries.types.reln.rest.v1;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import edu.tamu.tcat.trc.entries.types.reln.repo.EditRelationshipCommand;
import edu.tamu.tcat.trc.entries.types.reln.repo.RelationshipRepository;
import edu.tamu.tcat.trc.entries.types.reln.search.RelationshipDirection;
import edu.tamu.tcat.trc.entries.types.reln.search.RelationshipQueryCommand;
import edu.tamu.tcat.trc.entries.types.reln.search.RelationshipSearchResult;
import edu.tamu.tcat.trc.entries.types.reln.search.RelationshipSearchService;
import edu.tamu.tcat.trc.search.SearchException;

@Path("/relationships")
public class RelationshipsResource
{
   private static final Logger debug = Logger.getLogger(RelationshipsResource.class.getName());

   private RelationshipRepository repo;
   private RelationshipSearchService relnSearchService;

   public void setRepository(RelationshipRepository repo)
   {
      this.repo = repo;
   }

   public void setRelationshipService(RelationshipSearchService service)
   {
      this.relnSearchService = service;
   }

   public void activate()
   {
   }

   public void dispose()
   {
   }

   // /relationships?entity=<uri>      return all entities related to the supplied entity
   // /relationships?entity=<uri>[&type=<type_id>][&direction=from|to|any]
   @GET
   @Produces(MediaType.APPLICATION_JSON)
//   public RestApiV1.RelationshipSearchResultSet
   public List<RestApiV1.RelationshipSearchResult>
   searchRelationships(@QueryParam(value="entity") URI entity,
                       @QueryParam(value="type") String type,
                       @QueryParam(value="direction") RestApiV1.RelDirection direction,
                       @QueryParam(value = "off") @DefaultValue("0")   int offset,
                       @QueryParam(value = "max") @DefaultValue("100") int numResults)
   throws Exception
   {
      if (entity == null)
         throw new BadRequestException("No \"entity\" parameter value was provided.");
      Objects.requireNonNull(relnSearchService, "relationship search service is not available");

      try
      {
         RelationshipQueryCommand cmd = relnSearchService.createQueryCommand();
         RelationshipDirection dir = RelationshipDirection.any;
         if (direction != null)
            dir = direction.dir;
         cmd.forEntity(entity, dir);

         if (type != null) {
            cmd.byType(type);
         }

         cmd.setOffset(offset);
         cmd.setMaxResults(numResults);

         RelationshipSearchResult results = cmd.execute();
         RestApiV1.RelationshipSearchResultSet rs = new RestApiV1.RelationshipSearchResultSet();
         rs.items = SearchAdapter.toDTO(results.get());

         StringBuilder sb = new StringBuilder();
         try
         {
            app(sb, "entity", entity.toString());
            app(sb, "type", type);
            if (direction != null)
               app(sb, "direction", direction.toValue());
         }
         catch (Exception e)
         {
            throw new SearchException("Failed building querystring", e);
         }

         rs.qs = "off="+offset+"&max="+numResults+"&"+sb.toString();
         //TODO: does this depend on the number of results returned (i.e. whether < numResults), or do we assume there are infinite results?
         rs.qsNext = "off="+(offset + numResults)+"&max="+numResults+"&"+sb.toString();
         if (offset >= numResults)
            rs.qsPrev = "off="+(offset - numResults)+"&max="+numResults+"&"+sb.toString();
         // first page got off; reset to zero offset
         else if (offset > 0 && offset < numResults)
            rs.qsPrev = "off="+(0)+"&max="+numResults+"&"+sb.toString();

         //HACK: until the JS is ready to accept this data vehicle, just send the list of results
//         return rs;
         return rs.items;
      }
      catch (Exception e)
      {
         debug.log(Level.SEVERE, "Error", e);
         throw new SearchException(e);
      }
   }

   private static void app(StringBuilder sb, String p, String v)
   {
      if (v == null)
         return;
      if (sb.length() > 0)
         sb.append("&");
      try {
         sb.append(p).append("=").append(URLEncoder.encode(v, "UTF-8"));
         // suppress exception so this method can be used in lambdas
      } catch (UnsupportedEncodingException e) {
         throw new IllegalArgumentException("Failed encoding ["+v+"]", e);
      }
   }

   @POST
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   public RestApiV1.RelationshipId createRelationship(RestApiV1.Relationship relationship)
   {
      EditRelationshipCommand createCommand;
      try
      {
         createCommand = repo.create();
         createCommand.setAll(RepoAdapter.toRepo(relationship));

         RestApiV1.RelationshipId result = new RestApiV1.RelationshipId();
         result.id = createCommand.execute().get();
         return result;
      }
      catch (Exception e)
      {
         debug.severe("An error occured during the creating relationship process. Exception: " + e);
         throw new WebApplicationException("Failed to create a new relationship:", e.getCause(), 500);
      }
   }
}