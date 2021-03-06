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
package edu.tamu.tcat.trc.entries.types.bio;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import edu.tamu.tcat.trc.entries.common.HistoricalEvent;

/**
 * Records biographical information about a person, historical or fictional.
 */
public interface BiographicalEntry
{
   /**
    * @return A unique, persistent identifier for this person.
    */
   String getId();

   /**
    * @return The canonical name associated with this person. Many people are commonly referenced
    *       by multiple names, for example pen names or titles of nobility. This form of the
    *       represents an editorially determined 'canonical' representation of this person.
    * @see BiographicalEntry#getAlternativeNames()
    */
   PersonName getCanonicalName();

   /**
    * @return a set of alternative names for this person. Note that the canonical name is not a
    *       member of this set.
    * @see #getCanonicalName()
    */
   Set<PersonName> getAlternativeNames();    // FIXME PersonName does not implement notion of equality. Set is not an appropriate data type

   /**
    * @return All names associated with this person, including the person's canonical name.
    */
   default Collection<PersonName> getNames()
   {
      List<PersonName> result = new ArrayList<>();
      PersonName cname = getCanonicalName();
      if (cname != null)
         result.add(cname);

      result.addAll(getAlternativeNames());

      return result;
   }

   /**
    * @return The date of this person's birth. NOTE that this API is provisional and will likely change
    *    either to incorporate the new Java 8 time utilities or to provide more richly structured
    *    information about the person's birth (e.g., including location, fuzzy dates, etc).
    */
   HistoricalEvent getBirth();

   /**
    * @return The date of this person's death. NOTE that this API is provisional and will likely change
    *    either to incorporate the new Java 8 time utilities or to provide more richly structured
    *    information about the person's death (e.g., including location, fuzzy dates, etc).
    */
   HistoricalEvent getDeath();

   /**
    * @return A summary description of this person.
    */
   String getSummary();
}
