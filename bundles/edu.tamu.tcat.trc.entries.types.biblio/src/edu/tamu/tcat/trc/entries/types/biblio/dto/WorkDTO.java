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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import edu.tamu.tcat.trc.entries.types.biblio.BibliographicEntry;
import edu.tamu.tcat.trc.entries.types.biblio.CopyReference;


/**
 * Represents a work
 */
@Deprecated
public class WorkDTO
{
   public String id;
   @Deprecated // see note on Work#getType()
   public String type;
   public List<AuthorReferenceDTO> authors = new ArrayList<>();
   public Collection<TitleDTO> titles = new ArrayList<>();
   @Deprecated
   public List<AuthorReferenceDTO> otherAuthors = new ArrayList<>();
   public List<EditionDTO> editions = new ArrayList<>();
   public String series;
   public String summary;
   public String defaultCopyReferenceId;
   public Set<CopyReferenceDTO> copyReferences = new HashSet<>();

   public WorkDTO()
   {

   }

   public WorkDTO(WorkDTO orig)
   {
      this.id = orig.id;
      this.type = orig.type;
      this.authors = orig.authors.stream().map(AuthorReferenceDTO::new).collect(Collectors.toList());
      this.titles = orig.titles.stream().map(TitleDTO::new).collect(Collectors.toList());

      // TODO clone editions
      this.editions = orig.editions.stream().map(EditionDTO::new).collect(Collectors.toList());

      this.series = orig.series;
      this.summary = orig.summary;
      this.defaultCopyReferenceId = orig.defaultCopyReferenceId;
      this.copyReferences = orig.copyReferences.stream().map(CopyReferenceDTO::new).collect(Collectors.toSet());
   }

   // TODO move to model adapter
   public static WorkDTO create(BibliographicEntry work)
   {
      WorkDTO dto = new WorkDTO();

      if (work == null)
      {
         return dto;
      }

      dto.id = work.getId();

      dto.type = work.getType();

      dto.authors = StreamSupport.stream(work.getAuthors().spliterator(), false)
            .map(AuthorReferenceDTO::create)
            .collect(Collectors.toList());

      dto.titles = work.getTitle().get().stream()
            .map(TitleDTO::create)
            .collect(Collectors.toSet());

      dto.otherAuthors = StreamSupport.stream(work.getOtherAuthors().spliterator(), false)
            .map(AuthorReferenceDTO::create)
            .collect(Collectors.toList());

      dto.editions = work.getEditions().stream()
            .map(EditionDTO::create)
            .collect(Collectors.toList());

      dto.series = work.getSeries();

      dto.summary = work.getSummary();

      CopyReference defaultCopyReference = work.getDefaultCopyReference();
      if (defaultCopyReference != null)
      {
         dto.defaultCopyReferenceId = defaultCopyReference.getId();
      }

      dto.copyReferences = work.getCopyReferences().stream()
            .map(CopyReferenceDTO::create)
            .collect(Collectors.toSet());

      return dto;
   }
}
