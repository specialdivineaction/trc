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
package edu.tamu.tcat.trc.entries.types.biblio.dto;

import edu.tamu.tcat.trc.entries.common.DateDescription;
import edu.tamu.tcat.trc.entries.common.dto.DateDescriptionDTO;
import edu.tamu.tcat.trc.entries.types.biblio.PublicationInfo;

public class PublicationInfoDV
{
   public String publisher;
   public String place;
   public DateDescriptionDTO date;

   public static PublicationInfoDV create(PublicationInfo pubInfo)
   {
      PublicationInfoDV dto = new PublicationInfoDV();
      dto.publisher = pubInfo.getPublisher();
      dto.place = pubInfo.getLocation();
      dto.date = new DateDescriptionDTO(pubInfo.getPublicationDate());

      return dto;
   }

   public static PublicationInfo instantiate(PublicationInfoDV pubInfo)
   {
      return new PublicationImpl(pubInfo.place, pubInfo.publisher, DateDescriptionDTO.convert(pubInfo.date));
   }

   public static class PublicationImpl implements PublicationInfo
   {
      private final String place;
      private final String publisher;
      private final DateDescription date;

      public PublicationImpl(String place, String publisher, DateDescription date)
      {
         this.place = place;
         this.publisher = publisher;
         this.date = date;
      }

      @Override
      public String getLocation()
      {
         return place;
      }

      @Override
      public String getPublisher()
      {
         return publisher;
      }

      @Override
      public DateDescription getPublicationDate()
      {
         return date;
      }

   }
}
