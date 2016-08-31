/*
 * Copyright 2015 Texas A&M Engineering Experiment Station
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.tamu.tcat.trc.entries.types.bio.rest.v1;

import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import edu.tamu.tcat.trc.entries.types.bio.repo.DateDescriptionMutator;
import edu.tamu.tcat.trc.entries.types.bio.repo.EditPersonCommand;
import edu.tamu.tcat.trc.entries.types.bio.repo.HistoricalEventMutator;
import edu.tamu.tcat.trc.entries.types.bio.repo.PeopleRepository;
import edu.tamu.tcat.trc.entries.types.bio.repo.PersonNameMutator;
import edu.tamu.tcat.trc.entries.types.bio.rest.v1.RestApiV1.HistoricalEvent;
import edu.tamu.tcat.trc.entries.types.bio.rest.v1.RestApiV1.PersonName;
import edu.tamu.tcat.trc.entries.types.bio.rest.v1.internal.SearchAdapter;
import edu.tamu.tcat.trc.entries.types.bio.search.PeopleQueryCommand;
import edu.tamu.tcat.trc.entries.types.bio.search.PeopleSearchService;
import edu.tamu.tcat.trc.entries.types.bio.search.PersonSearchResult;
import edu.tamu.tcat.trc.search.SearchException;

public class PeopleResource
{
   // records internal errors accessing the REST
   static final Logger errorLogger = Logger.getLogger(PeopleResource.class.getName());

   private PeopleRepository repo;
   private PeopleSearchService peopleSearchService;

   public PeopleResource(PeopleRepository repo, PeopleSearchService search)
   {
      this.repo = repo;
      peopleSearchService = search;
   }

   @GET
   @Produces(MediaType.APPLICATION_JSON + "; charset=UTF-8")
   public RestApiV1.PersonSearchResultSet
   searchPeople(@QueryParam(value="q") String q,
                @QueryParam(value = "off") @DefaultValue("0")   int offset,
                @QueryParam(value = "max") @DefaultValue("100") int numResults)
   {
      try
      {
         PeopleQueryCommand cmd = peopleSearchService.createQueryCommand();
         if (q != null)
            cmd.query(q);
         cmd.setOffset(offset);
         cmd.setMaxResults(numResults);
         PersonSearchResult results = cmd.execute();

         RestApiV1.PersonSearchResultSet rs = new RestApiV1.PersonSearchResultSet();
         rs.items = SearchAdapter.toDTO(results.get());

         buildQueryLinks(rs, q, offset, numResults);

         return rs;
      }
      catch (SearchException e)
      {
         errorLogger.log(Level.SEVERE, "Error", e);
         throw new InternalServerErrorException("Failed to execute search", e);
      }
   }

   private void buildQueryLinks(RestApiV1.PersonSearchResultSet rs, String q, int offset, int numResults)
   {
      //TODO: does next link depend on the number of results returned (i.e. whether < numResults), or do we assume there are infinite results?
      try
      {
         String linktemplate = "off={0}&max={1}&q={2}";
         String query = q != null ? URLEncoder.encode(q, "UTF-8") : "";

         rs.qs = MessageFormat.format(linktemplate, Integer.valueOf(offset), Integer.valueOf(numResults), query);
         rs.qsNext = MessageFormat.format(linktemplate, Integer.valueOf(offset + numResults), Integer.valueOf(numResults), query);
         if (offset >= numResults)
            rs.qsPrev = MessageFormat.format(linktemplate, Integer.valueOf(offset - numResults), Integer.valueOf(numResults), query);
         // first page got off; reset to zero offset
         else if (offset > 0 && offset < numResults)
            rs.qsPrev = MessageFormat.format(linktemplate, Integer.valueOf(0), Integer.valueOf(numResults), query);
      }
      catch (Exception e)
      {
         throw new InternalServerErrorException("Failed building querystring", e);
      }
   }

   @POST
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   public RestApiV1.PersonId createPerson(RestApiV1.Person person)
   {
      try
      {
         EditPersonCommand createCommand = repo.create();

         PersonNameMutator addName = createCommand.addName();
         addName.setFamilyName(person.name.familyName);
         addName.setGivenName(person.name.givenName);
         addName.setMiddleName(person.name.middleName);
         addName.setSuffix(person.name.suffix);
         addName.setTitle(person.name.title);
         addName.setDisplayName(person.name.label);

         HistoricalEvent birth = person.birth;
         HistoricalEventMutator addBirthEvt = createCommand.addBirthEvt();
         addBirthEvt.setTitle(birth.title);
         addBirthEvt.setLocations(birth.location);
         addBirthEvt.setDescription(birth.description);

         DateDescriptionMutator setBirthDateDescription = addBirthEvt.editDateDescription();
         setBirthDateDescription.setCalendar(birth.date.calendar);
         setBirthDateDescription.setDescription(birth.date.description);

         HistoricalEvent death = person.death;
         HistoricalEventMutator addDeathEvt = createCommand.addDeathEvt();
         addDeathEvt.setTitle(death.title);
         addDeathEvt.setLocations(death.location);
         addDeathEvt.setDescription(death.description);

         DateDescriptionMutator setDeathDateDescription = addDeathEvt.editDateDescription();
         setDeathDateDescription.setCalendar(death.date.calendar);
         setDeathDateDescription.setDescription(death.date.description);

         createCommand.setSummary(person.summary);

         Set<PersonName> altNames = person.altNames;

         RestApiV1.PersonId personId = new RestApiV1.PersonId();
         personId.id = createCommand.execute().get();
         return personId;
      }
      catch (InterruptedException | ExecutionException e)
      {
         throw new InternalServerErrorException();
      }
   }

   @Path("{personId}")
   public PersonResource getPerson(@PathParam(value="personId") String personId)
   {
      return new PersonResource(repo, personId);
   }
}
