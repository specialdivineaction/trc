package edu.tamu.tcat.trc.entries.types.reln.rest.v1;

import java.util.List;
import java.util.Set;

import javax.ws.rs.BadRequestException;

import com.google.common.base.Joiner;

import edu.tamu.tcat.trc.entries.types.reln.search.RelationshipDirection;

public class RestApiV1
{
   /**
    * Simple data vehicle for reporting relationship identifiers as JSON objects.
    */
   public static class RelationshipId
   {
      public String id;
   }

   /**
    *  A JSON serializable representation of a {@link edu.tamu.tcat.trc.entries.types.reln.RelationshipType} for use in the REST API.
    */
   public static class RelationshipType
   {
      /**
      *  A string that uniquely identifies this relationship.
      *  @see RelationshipType#getIdentifier()
      */
      public String identifier;

      /**
      * The title of this relationship for display.
      * @see RelationshipType#getTitle()
      */
      public String title;

      /**
      * The reverse reading direction title of this relationship for display.
      * @see RelationshipType#getReverseTitle()
      */
      public String reverseTitle;

      /**
      * A textual description of the intended meaning of this relationship type.
      * @see RelationshipType#getDescription()
      */
      public String description;

      /**
      * {@code true} If this relationship is directed, {@code false} otherwise.
      * @see RelationshipType#isDirected()
      */
      public boolean isDirected;
   }

   public static class Relationship
   {
      public String id;
      public String typeId;
      public String description;
      public String descriptionMimeType;
      public Provenance provenance;
      public Set<Anchor> relatedEntities;
      public Set<Anchor> targetEntities;
   }

   public static class Anchor
   {
      public Set<String> entryUris;
   }

   public static class Provenance
   {
      /** The string-valued URIs associated with the creators of the associated annotation. */
      public Set<String> creatorUris;

      /** Date created in ISO 8601 format such as '2011-12-03T10:15:30Z' */
      public String dateCreated;

      /** Date modified in ISO 8601 format such as '2011-12-03T10:15:30Z' */
      public String dateModified;
   }

   /**
    * A DTO to be used as a REST query or path parameter. This class parses the String
    * sent (using the REST API format) as the parameter value into a {@link RelationshipDirection}.
    */
   public static class RelDirection
   {
      public final RelationshipDirection dir;

      public RelDirection(String d)
      {
         if (d == null || d.trim().isEmpty())
         {
            dir = RelationshipDirection.any;
         }
         else
         {
            try
            {
               dir = RelationshipDirection.valueOf(d.toLowerCase());
            }
            catch (Exception iea)
            {
               Joiner joiner = Joiner.on(", ");
               //FIXME: this needs to build a Response to properly report to the client
               throw new BadRequestException("Invalid value for query parameter 'direction' [" + d + "]. Must be one of the following: " + joiner.join(RelationshipDirection.values()));
            }
         }
      }

      public String toValue()
      {
         return dir.toString();
      }
   }


   public static class RelationshipSearchResultSet
   {
      public List<RelationshipSearchResult> items;
      /** The querystring that resulted in this result set */
      public String qs;
      public String qsNext;
      public String qsPrev;
   }

   public static class RelationshipSearchResult
   {
      public String id;
      public String typeId;
      public String description;
      public String descriptionMimeType;
      public Provenance provenance;
      public Set<Anchor> relatedEntities;
      public Set<Anchor> targetEntities;
   }
}