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
package edu.tamu.tcat.trc.entries.types.biblio.repo;

import java.util.Collection;
import java.util.List;

import edu.tamu.tcat.trc.entries.repo.NoSuchCatalogRecordException;
import edu.tamu.tcat.trc.entries.types.biblio.Edition;
import edu.tamu.tcat.trc.entries.types.biblio.dto.AuthorRefDV;
import edu.tamu.tcat.trc.entries.types.biblio.dto.EditionDV;
import edu.tamu.tcat.trc.entries.types.biblio.dto.PublicationInfoDV;
import edu.tamu.tcat.trc.entries.types.biblio.dto.TitleDV;

/**
 * Used to edit the properties of an {@link Edition}. A {@code EditionMutator} is created
 * within the transactional scope of an {@link EditWorkCommand} via either the
 * {@link EditWorkCommand#createEdition()} or the {@link EditWorkCommand#editEdition(String)}
 * method. Changes made to the {@code Edition} modified by this mutator will take effect
 * when the parent {@link EditWorkCommand#execute()} method is invoked. Changes made after this
 * command's {@code execute()} method is called will have indeterminate affects.
 *
 * <p>
 * Implementations are typically not threadsafe.
 */
public interface EditionMutator
{
   /**
   *
   * @return The unique identifier for the edition that this mutator modifies.
   *         Will not be {@code null}. For newly created editions, this identifier
   *         will be assigned when the java object is first created rather than when
   *         the edition is committed to the persistence layer.
   */
  String getId();

   /**
    * Sets all values from the supplied data vehicle into the edition being edited.
    * @param edition
    */
   void setAll(EditionDV edition);

   /**
    * @param authors The list of authors to be set for this edition.
    */
   void setAuthors(List<AuthorRefDV> authors);

   /**
    * @param titles The titles to be set for this edition
    */
   void setTitles(Collection<TitleDV> titles);

   /**
    * @param otherAuthors The other authors for this edition.
    */
   void setOtherAuthors(List<AuthorRefDV> otherAuthors);

   /**
    * @param editionName the name of this edition.
    */
   void setEditionName(String editionName);

   /**
    * @param pubInfo Information about who, where and when this edition was published.
    */
   void setPublicationInfo(PublicationInfoDV pubInfo);

   /**
    * @param series The series name to which this edition belongs.
    */
   void setSeries(String series);

   /**
    * @param summary An editorial summary of this edition.
    */
   void setSummary(String summary);

   /**
    * Creates a new volume within this edition.
    *
    * @return A mutator to be used to edit the newly created volume.
    */
   VolumeMutator createVolume();

   /**
    * Edit a volume associated with this edition.
    * @param id The id of the volume to edit.
    * @return A mutator to be used to edit the newly created volume.
    * @throws NoSuchCatalogRecordException If the identified volume is not associated with this edition.
    */
   VolumeMutator editVolume(String id) throws NoSuchCatalogRecordException;


}
