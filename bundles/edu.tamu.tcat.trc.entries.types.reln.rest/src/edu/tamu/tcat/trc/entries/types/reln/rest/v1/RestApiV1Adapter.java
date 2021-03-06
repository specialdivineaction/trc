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
package edu.tamu.tcat.trc.entries.types.reln.rest.v1;

import java.util.stream.Collectors;

import edu.tamu.tcat.trc.TrcApplication;
import edu.tamu.tcat.trc.entries.types.reln.Anchor;
import edu.tamu.tcat.trc.entries.types.reln.Relationship;
import edu.tamu.tcat.trc.entries.types.reln.RelationshipType;
import edu.tamu.tcat.trc.entries.types.reln.repo.RelationshipRepository;
import edu.tamu.tcat.trc.resolver.EntryId;
import edu.tamu.tcat.trc.resolver.EntryIdDto;

/**
 * An encapsulation of adapter methods to convert between the repository API and
 * the {@link RestApiV1} schema DTOs.
 */
public class RestApiV1Adapter
{
   private final TrcApplication trcCtx;

   public RestApiV1Adapter(TrcApplication trcCtx)
   {
      this.trcCtx = trcCtx;
   }

   public RestApiV1.Relationship toDTO(Relationship orig)
   {
      if (orig == null)
         return null;

      EntryId entryId = new EntryId(orig.getId(), RelationshipRepository.ENTRY_TYPE_ID);

      RestApiV1.Relationship dto = new RestApiV1.Relationship();

      dto.id = orig.getId();
      dto.ref = EntryIdDto.adapt(entryId, trcCtx.getResolverRegistry());
      dto.typeId = orig.getType().getIdentifier();
      dto.description = orig.getDescription();

      dto.related = orig.getRelatedEntities().stream()
            .map(anchor -> toDto(anchor))
            .collect(Collectors.toSet());

      dto.targets = orig.getTargetEntities().stream()
            .map(anchor -> toDto(anchor))
            .collect(Collectors.toSet());

      return dto;
   }

   public RestApiV1.Anchor toDto(Anchor anchor)
   {
      RestApiV1.Anchor dto = new RestApiV1.Anchor();
      dto.ref = EntryIdDto.adapt(anchor.getTarget(), trcCtx.getResolverRegistry());
      dto.properties.clear();
      anchor.listProperties().stream()
            .forEach(key -> dto.properties.put(key, anchor.getProperty(key)));

      return dto;
   }

   public RestApiV1.RelationshipType toDto(RelationshipType relnType)
   {
      RestApiV1.RelationshipType dto = new RestApiV1.RelationshipType();
      dto.identifier = relnType.getIdentifier();
      dto.title = relnType.getTitle();
      dto.reverseTitle = relnType.getReverseTitle();
      dto.isDirected = relnType.isDirected();
      dto.description = relnType.getDescription();

      // TODO Auto-generated method stub
      return null;
   }
}
