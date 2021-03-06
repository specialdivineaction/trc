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
package edu.tamu.tcat.trc.entries.types.biblio.rest.v1;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.ws.rs.BadRequestException;

import edu.tamu.tcat.trc.entries.common.DateDescription;
import edu.tamu.tcat.trc.entries.types.biblio.AuthorReference;
import edu.tamu.tcat.trc.entries.types.biblio.BibliographicEntry;
import edu.tamu.tcat.trc.entries.types.biblio.CopyReference;
import edu.tamu.tcat.trc.entries.types.biblio.Edition;
import edu.tamu.tcat.trc.entries.types.biblio.PublicationInfo;
import edu.tamu.tcat.trc.entries.types.biblio.Title;
import edu.tamu.tcat.trc.entries.types.biblio.Volume;
import edu.tamu.tcat.trc.entries.types.biblio.dto.AuthorReferenceDTO;
import edu.tamu.tcat.trc.entries.types.biblio.dto.DateDescriptionDTO;
import edu.tamu.tcat.trc.entries.types.biblio.dto.PublicationInfoDTO;
import edu.tamu.tcat.trc.entries.types.biblio.dto.TitleDTO;
import edu.tamu.tcat.trc.entries.types.biblio.repo.CopyReferenceMutator;
import edu.tamu.tcat.trc.entries.types.biblio.repo.EditBibliographicEntryCommand;
import edu.tamu.tcat.trc.entries.types.biblio.repo.EditionMutator;
import edu.tamu.tcat.trc.entries.types.biblio.repo.VolumeMutator;
import edu.tamu.tcat.trc.resolver.EntryIdDto;
import edu.tamu.tcat.trc.resolver.EntryResolverRegistry;

/**
 * An encapsulation of adapter methods to convert between the repository API and
 * the {@link BiblioRestApiV1} schema DTOs.
 */
public class RepoAdapter
{
   public static BiblioRestApiV1.Work toDTO(BibliographicEntry work, EntryResolverRegistry resolvers)
   {
      if (work == null)
      {
         return null;
      }

      BiblioRestApiV1.Work dto = new BiblioRestApiV1.Work();

      dto.id = work.getId();

      dto.ref = EntryIdDto.adapt(resolvers.getResolver(work).makeReference(work), resolvers);

      dto.type = work.getType();

      dto.authors = StreamSupport.stream(work.getAuthors().spliterator(), false)
            .map(RepoAdapter::toDTO)
            .collect(Collectors.toList());

      dto.titles = work.getTitle().get().stream()
            .map(RepoAdapter::toDTO)
            .collect(Collectors.toList());

      dto.otherAuthors = StreamSupport.stream(work.getOtherAuthors().spliterator(), false)
            .map(RepoAdapter::toDTO)
            .collect(Collectors.toList());

      dto.series = work.getSeries();

      dto.summary = work.getSummary();

      dto.editions = work.getEditions().stream()
            .sorted((a, b) -> extractPubDate(a).compareTo(extractPubDate(b)))
            .map(RepoAdapter::toDTO)
            .collect(Collectors.toList());

      CopyReference defaultCopyReference = work.getDefaultCopyReference();
      if (defaultCopyReference != null)
      {
         dto.defaultCopyId = defaultCopyReference.getId();
      }

      dto.copies = work.getCopyReferences().stream()
            .map(edu.tamu.tcat.trc.entries.types.biblio.rest.v1.RepoAdapter::toDTO)
            .collect(Collectors.toList());

      return dto;
   }

   public static BiblioRestApiV1.AuthorRef toDTO(AuthorReferenceDTO orig)
   {
      if (orig == null)
         return null;

      BiblioRestApiV1.AuthorRef dto = new BiblioRestApiV1.AuthorRef();
      dto.authorId = orig.authorId;
      dto.lastName = orig.lastName;
      dto.firstName = orig.firstName;
      dto.role = orig.role;

      return dto;
   }

   public static BiblioRestApiV1.Edition toDTO(Edition ed)
   {
      BiblioRestApiV1.Edition dto = new BiblioRestApiV1.Edition();
      dto.id = ed.getId();

      dto.editionName = ed.getEditionName();

      dto.publicationInfo = toDTO(ed.getPublicationInfo());

      dto.authors = ed.getAuthors().stream()
            .map(RepoAdapter::toDTO)
            .collect(Collectors.toList());

      dto.titles = ed.getTitles().parallelStream()
            .map(RepoAdapter::toDTO)
            .collect(Collectors.toSet());

      dto.otherAuthors = ed.getOtherAuthors().stream()
            .map(RepoAdapter::toDTO)
            .collect(Collectors.toList());

      dto.summary = ed.getSummary();

      dto.series = ed.getSeries();

      dto.volumes = ed.getVolumes().stream()
            .map(RepoAdapter::toDTO)
            .collect(Collectors.toList());

      CopyReference defaultCopyReference = ed.getDefaultCopyReference();
      if (defaultCopyReference != null)
      {
         dto.defaultCopyId = defaultCopyReference.getId();
      }

      dto.copies = ed.getCopyReferences().stream()
            .map(edu.tamu.tcat.trc.entries.types.biblio.rest.v1.RepoAdapter::toDTO)
            .collect(Collectors.toList());

      return dto;
   }

   public static BiblioRestApiV1.PublicationInfo toDTO(PublicationInfo orig)
   {
      if (orig == null)
         return null;

      BiblioRestApiV1.PublicationInfo dto = new BiblioRestApiV1.PublicationInfo();
      dto.publisher = orig.getPublisher();
      dto.place = orig.getLocation();
      dto.date = toDTO(orig.getPublicationDate());
      return dto;
   }

   public static BiblioRestApiV1.DateDescription toDTO(DateDescription orig)
   {
      if (orig == null)
         return null;
      BiblioRestApiV1.DateDescription dto = new BiblioRestApiV1.DateDescription();
      LocalDate d = orig.getCalendar();
      if (d != null)
      {
         dto.calendar = DateTimeFormatter.ISO_LOCAL_DATE.format(d);
      }

      dto.description = orig.getDescription();

      return dto;
   }

   public static BiblioRestApiV1.Volume toDTO(Volume vol)
   {
      if (vol == null)
         return null;

      BiblioRestApiV1.Volume dto = new BiblioRestApiV1.Volume();
      dto.id = vol.getId();

      dto.volumeNumber = vol.getVolumeNumber();

      dto.publicationInfo = toDTO(vol.getPublicationInfo());

      dto.authors = vol.getAuthors().stream()
            .map(RepoAdapter::toDTO)
            .collect(Collectors.toList());

      dto.titles = vol.getTitles().parallelStream()
            .map(RepoAdapter::toDTO)
            .collect(Collectors.toSet());

      dto.otherAuthors = vol.getOtherAuthors().stream()
            .map(RepoAdapter::toDTO)
            .collect(Collectors.toList());

      dto.summary = vol.getSummary();

      dto.series = vol.getSeries();

      CopyReference defaultCopyReference = vol.getDefaultCopyReference();
      if (defaultCopyReference != null)
      {
         dto.defaultCopyId = defaultCopyReference.getId();
      }

      dto.copies = vol.getCopyReferences().stream()
            .map(edu.tamu.tcat.trc.entries.types.biblio.rest.v1.RepoAdapter::toDTO)
            .collect(Collectors.toList());

      return dto;
   }

   public static BiblioRestApiV1.Title toDTO(Title orig)
   {
      if (orig == null)
         return null;

      BiblioRestApiV1.Title dto = new BiblioRestApiV1.Title();
      dto.type = orig.getType();
      dto.lg = orig.getLanguage();
      dto.title = orig.getTitle();
      dto.subtitle = orig.getSubTitle();
      return dto;
   }

   public static BiblioRestApiV1.AuthorRef toDTO(AuthorReference author)
   {
      if (author == null)
         return null;

      BiblioRestApiV1.AuthorRef dto = new BiblioRestApiV1.AuthorRef();
      dto.authorId = author.getId();

      String fName = author.getFirstName();
      String lName = author.getLastName();

      dto.firstName = ((fName != null) && !fName.trim().isEmpty()) ? fName : dto.firstName;
      dto.lastName = ((lName != null) && !lName.trim().isEmpty()) ? lName : dto.lastName;

      dto.role = author.getRole();
      return dto;
   }

   public static BiblioRestApiV1.CopyReference toDTO(CopyReference copyReference)
   {
      if (copyReference == null)
      {
         return null;
      }

      BiblioRestApiV1.CopyReference dto = new BiblioRestApiV1.CopyReference();

      dto.id = copyReference.getId();
      dto.type = copyReference.getType();
      dto.properties = copyReference.getProperties();
      dto.title = copyReference.getTitle();
      dto.summary = copyReference.getSummary();
      dto.rights = copyReference.getRights();

      return dto;
   }

   public static AuthorReferenceDTO toRepo(BiblioRestApiV1.AuthorRef orig)
   {
      if (orig == null)
         return null;

      AuthorReferenceDTO dto = new AuthorReferenceDTO();
      dto.authorId = orig.authorId;
      dto.lastName = orig.lastName;
      dto.firstName = orig.firstName;
      dto.role = orig.role;

      return dto;
   }

   public static PublicationInfoDTO toRepo(BiblioRestApiV1.PublicationInfo orig)
   {
      if (orig == null)
         return null;

      PublicationInfoDTO dto = new PublicationInfoDTO();
      dto.publisher = orig.publisher;
      dto.place = orig.place;
      dto.date = toRepo(orig.date);

      return dto;
   }

   public static DateDescriptionDTO toRepo(BiblioRestApiV1.DateDescription orig)
   {
      if (orig == null)
         return null;

      DateDescriptionDTO dto = new DateDescriptionDTO();
      dto.calendar = orig.calendar;
      dto.description = orig.description;

      return dto;
   }

   public static TitleDTO toRepo(BiblioRestApiV1.Title orig)
   {
      if (orig == null)
         return null;

      TitleDTO dto = new TitleDTO();
      dto.title = orig.title;
      dto.type = orig.type;
      dto.lg = orig.lg;
      dto.subtitle = orig.subtitle;

      return dto;
   }

   public static void apply(BiblioRestApiV1.Work work, EditBibliographicEntryCommand command)
   {
      List<AuthorReferenceDTO> authors = work.authors.stream()
            .map(RepoAdapter::toRepo)
            .collect(Collectors.toList());
      command.setAuthors(authors);

      List<TitleDTO> titles = work.titles.stream()
            .map(RepoAdapter::toRepo)
            .collect(Collectors.toList());
      command.setTitles(titles);

      command.setSeries(work.series);
      command.setSummary(work.summary);

      Set<String> editionIds = work.editions.stream()
            .map(edition -> edition.id)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

      Set<String> extraneousEditionIds = command.retainAllEditions(editionIds);
      if (!extraneousEditionIds.isEmpty())
      {
         StringJoiner sj = new StringJoiner(", ");
         extraneousEditionIds.forEach(sj::add);
         throw new BadRequestException("The following copy reference IDs do not exist: " + sj.toString());
      }

      work.editions.forEach(edition -> {
         EditionMutator editionMutator = edition.id == null ? command.createEdition() : command.editEdition(edition.id);
         RepoAdapter.apply(edition, editionMutator);
      });

      Set<String> copyReferenceIds = work.copies.stream()
            .map(copy -> copy.id)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

      Set<String> extraneousCopyReferenceIds = command.retainAllCopyReferences(copyReferenceIds);
      if (!extraneousCopyReferenceIds.isEmpty())
      {
         StringJoiner sj = new StringJoiner(", ");
         extraneousCopyReferenceIds.forEach(sj::add);
         throw new BadRequestException("The following copy reference IDs do not exist: " + sj.toString());
      }

      work.copies.forEach(copyReference -> {
         CopyReferenceMutator copyReferenceMutator = copyReference.id == null ? command.createCopyReference() : command.editCopyReference(copyReference.id);
         RepoAdapter.apply(copyReference, copyReferenceMutator);
      });

      if (work.defaultCopyId != null)
      {
         try
         {
            command.setDefaultCopyReference(work.defaultCopyId);
         }
         catch (IllegalArgumentException e)
         {
            throw new BadRequestException("Unable to find copy reference with ID {" + work.defaultCopyId + "}.");
         }
      }
   }

   public static void apply(BiblioRestApiV1.Edition edition, EditionMutator mutator)
   {
      mutator.setEditionName(edition.editionName);

      mutator.setPublicationInfo(toRepo(edition.publicationInfo));

      List<AuthorReferenceDTO> authors = edition.authors.stream()
            .map(RepoAdapter::toRepo)
            .collect(Collectors.toList());
      mutator.setAuthors(authors);

      List<TitleDTO> titles = edition.titles.stream()
            .map(RepoAdapter::toRepo)
            .collect(Collectors.toList());
      mutator.setTitles(titles);

      mutator.setSeries(edition.series);

      mutator.setSummary(edition.summary);

      Set<String> volumeIds = edition.volumes.stream()
            .map(volume -> volume.id)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

      Set<String> extraneousVolumeIds = mutator.retainAllVolumes(volumeIds);
      if (!extraneousVolumeIds.isEmpty())
      {
         StringJoiner sj = new StringJoiner(", ");
         extraneousVolumeIds.forEach(sj::add);
         throw new BadRequestException("The following volume IDs do not exist: " + sj.toString());
      }

      edition.volumes.forEach(volume -> {
         VolumeMutator volumeMutator = volume.id == null ? mutator.createVolume() : mutator.editVolume(volume.id);
         RepoAdapter.apply(volume, volumeMutator);
      });

      Set<String> copyReferenceIds = edition.copies.stream()
            .map(copy -> copy.id)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

      Set<String> extraneousCopyReferenceIds = mutator.retainAllCopyReferences(copyReferenceIds);
      if (!extraneousCopyReferenceIds.isEmpty())
      {
         StringJoiner sj = new StringJoiner(", ");
         extraneousCopyReferenceIds.forEach(sj::add);
         throw new BadRequestException("The following copy reference IDs do not exist: " + sj.toString());
      }

      edition.copies.forEach(copyReference -> {
         CopyReferenceMutator copyReferenceMutator = copyReference.id == null ? mutator.createCopyReference() : mutator.editCopyReference(copyReference.id);
         RepoAdapter.apply(copyReference, copyReferenceMutator);
      });

      if (edition.defaultCopyId != null)
      {
         try
         {
            mutator.setDefaultCopyReference(edition.defaultCopyId);
         }
         catch (IllegalArgumentException e)
         {
            throw new BadRequestException("Unable to find copy reference with ID {" + edition.defaultCopyId + "}.");
         }
      }
   }

   public static void apply(BiblioRestApiV1.Volume volume, VolumeMutator mutator)
   {
      mutator.setVolumeNumber(volume.volumeNumber);

      mutator.setPublicationInfo(toRepo(volume.publicationInfo));

      List<AuthorReferenceDTO> authors = volume.authors.stream()
            .map(RepoAdapter::toRepo)
            .collect(Collectors.toList());
      mutator.setAuthors(authors);

      List<TitleDTO> titles = volume.titles.stream()
            .map(RepoAdapter::toRepo)
            .collect(Collectors.toList());
      mutator.setTitles(titles);

      mutator.setSeries(volume.series);

      mutator.setSummary(volume.summary);

      Set<String> copyReferenceIds = volume.copies.stream()
            .map(copy -> copy.id)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

      Set<String> extraneousCopyReferenceIds = mutator.retainAllCopyReferences(copyReferenceIds);
      if (!extraneousCopyReferenceIds.isEmpty())
      {
         StringJoiner sj = new StringJoiner(", ");
         extraneousCopyReferenceIds.forEach(sj::add);
         throw new BadRequestException("The following copy reference IDs do not exist: " + sj.toString());
      }

      volume.copies.forEach(copyReference -> {
         CopyReferenceMutator copyReferenceMutator = copyReference.id == null ? mutator.createCopyReference() : mutator.editCopyReference(copyReference.id);
         RepoAdapter.apply(copyReference, copyReferenceMutator);
      });

      if (volume.defaultCopyId != null)
      {
         try
         {
            mutator.setDefaultCopyReference(volume.defaultCopyId);
         }
         catch (IllegalArgumentException e)
         {
            throw new BadRequestException("Unable to find copy reference with ID {" + volume.defaultCopyId + "}.");
         }
      }
   }

   public static void apply(BiblioRestApiV1.CopyReference dto, CopyReferenceMutator mutator)
   {
      mutator.setType(dto.type);
      mutator.setProperties(dto.properties);
      mutator.setTitle(dto.title);
      mutator.setSummary(dto.summary);
      mutator.setRights(dto.rights);
   }

   /**
    * Finds the publication date of an edition.
    * This method will never return {@code null}.
    *
    * @param edition
    * @return The publication date of the given edition or {@code LocalDate.MIN} if none could be found.
    */
   private static LocalDate extractPubDate(Edition edition)
   {
      return Optional.ofNullable(edition)
            .map(Edition::getPublicationInfo)
            .map(PublicationInfo::getPublicationDate)
            .map(DateDescription::getCalendar)
            .orElse(LocalDate.MIN);
   }
}
