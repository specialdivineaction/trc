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
package edu.tamu.tcat.trc.entries.types.bio.dto;

import java.util.Date;

import edu.tamu.tcat.trc.entries.common.DateDescription;
import edu.tamu.tcat.trc.entries.common.HistoricalEvent;

@Deprecated
public class HistoricalEventDTO
{

   public String id;
   public String title;
   public String description;
   public String location;

   /** The date this event took place. */
   public DateDescriptionDTO date;

   /** Replaced by date. */
   @Deprecated
   public Date eventDate;

   public static HistoricalEventDTO create(HistoricalEvent orig)
   {
      HistoricalEventDTO dto = new HistoricalEventDTO();

      dto.id = orig.getId();

      dto.title = orig.getTitle();

      dto.description = orig.getDescription();

      dto.location = orig.getLocation();

      DateDescription date = orig.getDate();
      if (date != null)
      {
         dto.date = DateDescriptionDTO.create(date);
      }

      return dto;
   }
}