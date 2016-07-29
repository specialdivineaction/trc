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
package edu.tamu.tcat.trc.entries.types.bio.search;

import edu.tamu.tcat.trc.entries.types.bio.Person;

/**
 * JSON serializable summary information about a biographical entry (i.e. a person).
 * Intended to be returned when a brief summary of the person is required to save data
 * transfer and parsing resources.
 */
public class BioSearchProxy
{
   /**
    * ID corresponding to the {@link Person} object that this simple data vehicle represents.
    */
   public String id;

   /**
    * Display name for this person (for use when populating fields)
    */
   public PersonNameDTO displayName = new PersonNameDTO();

   /**
    * Formatted name to display e.g. when linking to the underlying {@link Person}.
    *
    * This is essentially a string representation of the display name plus the lifespan of the person.
    */
   public String formattedName;

   /**
    * An excerpt (canonically the first sentence) of the biographical summary to give the search
    * result some context. This value may be null if no summary has been provided yet.
    */
   public String summaryExcerpt;

   public class PersonNameDTO
   {
      public String given;
      public String family;
      public String display;
   }
}
