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
package edu.tamu.tcat.trc.entries.types.biblio.impl.repo;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import edu.tamu.tcat.trc.entries.types.biblio.dto.AuthorReferenceDTO;
import edu.tamu.tcat.trc.entries.types.biblio.dto.PublicationInfoDTO;
import edu.tamu.tcat.trc.entries.types.biblio.dto.TitleDTO;
import edu.tamu.tcat.trc.entries.types.biblio.repo.BibliographicEntryRepository;
import edu.tamu.tcat.trc.entries.types.biblio.repo.CopyReferenceMutator;
import edu.tamu.tcat.trc.entries.types.biblio.repo.EditionMutator;
import edu.tamu.tcat.trc.entries.types.biblio.repo.VolumeMutator;
import edu.tamu.tcat.trc.repo.ChangeSet;
import edu.tamu.tcat.trc.repo.id.IdFactory;
import edu.tamu.tcat.trc.repo.id.IdFactoryProvider;

public class EditionMutatorImpl implements EditionMutator
{
   // private final EditionDTO edition;
   private final IdFactoryProvider idFactoryProvider;
   private final IdFactory volumeIds;
   private final IdFactory copyRefIds;

   private final ChangeSet<DataModelV1.EditionDTO> changes;
   private String id;

   public EditionMutatorImpl(String id, ChangeSet<DataModelV1.EditionDTO> edChanges, IdFactoryProvider idFactoryProvider)
   {
      this.id = id;
      this.changes = edChanges;

      this.idFactoryProvider = idFactoryProvider;
      this.volumeIds = idFactoryProvider.getIdFactory(BibliographicEntryRepository.ID_CONTEXT_VOLUMES);
      this.copyRefIds = idFactoryProvider.getIdFactory(BibliographicEntryRepository.ID_CONTEXT_COPIES);
   }

   private Function<DataModelV1.EditionDTO, DataModelV1.CopyReferenceDTO> makeCopySelector(String id)
   {
      return (dto) -> dto.copyReferences.stream()
            .filter(ref -> Objects.equals(id, ref.id))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Cannot find copy reference with id {" + id + "}."));
   }

   private Function<DataModelV1.EditionDTO, DataModelV1.VolumeDTO> makeVolumeSelector(String id)
   {
      return (dto) -> dto.volumes.stream()
            .filter(vol -> Objects.equals(id, vol.id))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Cannot find volume with id {" + id + "}."));
   }

   @Override
   public String getId()
   {
      return id;
   }

   @Override
   public void setAuthors(List<AuthorReferenceDTO> authors)
   {
      List<DataModelV1.AuthorReferenceDTO> copied = ModelAdapter.adaptAuthors(authors);
      changes.add("authors", dto -> dto.authors = copied);
   }

   @Override
   public void setTitles(Collection<TitleDTO> titles)
   {
      List<DataModelV1.TitleDTO> copied = ModelAdapter.adaptTitles(titles);
      changes.add("titles", dto -> dto.titles = copied);
   }

   @Override
   public void setOtherAuthors(List<AuthorReferenceDTO> otherAuthors)
   {
      List<DataModelV1.AuthorReferenceDTO> copied = ModelAdapter.adaptAuthors(otherAuthors);
      changes.add("authors", dto -> dto.otherAuthors = copied);
   }

   @Override
   public void setEditionName(String editionName)
   {
      changes.add("name", dto -> dto.editionName = editionName);
   }

   @Override
   public void setPublicationInfo(PublicationInfoDTO pubInfo)
   {
      changes.add("pubInfo", dto -> dto.publicationInfo = ModelAdapter.adapt(pubInfo));
   }

   @Override
   public void setSummary(String summary)
   {
      changes.add("summary", dto -> dto.summary = summary);
   }

   @Override
   public void setSeries(String series)
   {
      changes.add("series", dto -> dto.series = series);
   }

   @Override
   public VolumeMutator editVolume(String id)
   {
      ChangeSet<DataModelV1.VolumeDTO> edChanges = changes.partial("volume." + id, makeVolumeSelector(id));
      return new VolumeMutatorImpl(id, edChanges, idFactoryProvider);
   }

   @Override
   public VolumeMutator createVolume()
   {
      String id = volumeIds.get();
      changes.add("volume." + id + "[create]", dto -> {
         DataModelV1.VolumeDTO volume = new DataModelV1.VolumeDTO();
         volume.id = id;
         dto.volumes.add(volume);
      });

      ChangeSet<DataModelV1.VolumeDTO> volChanges = changes.partial("volume." + id, makeVolumeSelector(id));
      return new VolumeMutatorImpl(id, volChanges, idFactoryProvider);
   }

   @Override
   public void removeVolume(String volumeId)
   {
      changes.add("volume [remove]", dto ->
         dto.volumes.removeIf(volume -> Objects.equals(volume.id, volumeId))
      );
   }

   @Override
   public Set<String> retainAllVolumes(Set<String> editionIds)
   {
      Objects.requireNonNull(editionIds);

      changes.add("volume [retain some]", dto -> {
         dto.volumes.removeIf(edition -> !editionIds.contains(edition.id));
      });

      // FIXME violates contract. Need to rethink API
      return Collections.emptySet();
   }

   @Override
   public CopyReferenceMutator createCopyReference()
   {
      String refId = copyRefIds.get();
      changes.add("copy." + refId + " [create]", dto -> {
         DataModelV1.CopyReferenceDTO ref = new DataModelV1.CopyReferenceDTO();
         ref.id = refId;
         dto.copyReferences.add(ref);
      });

      ChangeSet<DataModelV1.CopyReferenceDTO> refChanges = changes.partial("copy." + refId, makeCopySelector(refId));
      return new CopyReferenceMutatorImpl(refId, refChanges);
   }

   @Override
   public CopyReferenceMutator editCopyReference(String id)
   {
      ChangeSet<DataModelV1.CopyReferenceDTO> refChanges = changes.partial("copy." + id, makeCopySelector(id));
      return new CopyReferenceMutatorImpl(id, refChanges);
   }

   @Override
   public void removeCopyReference(String id)
   {
      Objects.requireNonNull(id, "Must supply non-null id");

      changes.add("copy." + id + " [remove]", dto -> {
         dto.copyReferences.removeIf(ref -> !id.equals(ref.id));
      });
   }

   @Override
   public Set<String> retainAllCopyReferences(Set<String> copyReferenceIds)
   {
      changes.add("copy" + "[retain some]", dto -> {
         dto.copyReferences.removeIf(ref -> !copyReferenceIds.contains(ref.id));
      });

      // TODO remove this and update API
      return Collections.emptySet();
   }

   @Override
   public void setDefaultCopyReference(String refId)
   {
      changes.add("defaultCopy", dto -> {
         dto.defaultCopyReferenceId = refId;
      });
   }
}
