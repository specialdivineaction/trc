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
package edu.tamu.tcat.trc.entries.types.biblio.test.mocks;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import edu.tamu.tcat.trc.entries.repo.NoSuchCatalogRecordException;
import edu.tamu.tcat.trc.entries.types.biblio.dto.AuthorReferenceDTO;
import edu.tamu.tcat.trc.entries.types.biblio.dto.EditionDTO;
import edu.tamu.tcat.trc.entries.types.biblio.dto.PublicationInfoDTO;
import edu.tamu.tcat.trc.entries.types.biblio.dto.TitleDTO;
import edu.tamu.tcat.trc.entries.types.biblio.repo.EditionMutator;
import edu.tamu.tcat.trc.entries.types.biblio.repo.VolumeMutator;

public class MockEditionMutator implements EditionMutator
{
   private EditionDTO dto;
   private Supplier<String> volumeIds;

   public MockEditionMutator(EditionDTO edition, Supplier<String> volumeIds)
   {
      this.dto = edition;
      this.volumeIds = volumeIds;
   }

   @Override
   public String getId()
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public void setAll(EditionDTO edition)
   {
      // TODO Auto-generated method stub

   }

   @Override
   public void setAuthors(List<AuthorReferenceDTO> authors)
   {
      // TODO Auto-generated method stub

   }

   @Override
   public void setTitles(Collection<TitleDTO> titles)
   {
      // TODO Auto-generated method stub

   }

   @Override
   public void setOtherAuthors(List<AuthorReferenceDTO> otherAuthors)
   {
      // TODO Auto-generated method stub

   }

   @Override
   public void setEditionName(String editionName)
   {
      // TODO Auto-generated method stub

   }

   @Override
   public void setPublicationInfo(PublicationInfoDTO pubInfo)
   {
      // TODO Auto-generated method stub

   }

   @Override
   public void setSeries(String series)
   {
      // TODO Auto-generated method stub

   }

   @Override
   public void setSummary(String summary)
   {
      // TODO Auto-generated method stub

   }

   @Override
   public VolumeMutator createVolume()
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public VolumeMutator editVolume(String id) throws NoSuchCatalogRecordException
   {
      // TODO Auto-generated method stub
      return null;
   }

}
