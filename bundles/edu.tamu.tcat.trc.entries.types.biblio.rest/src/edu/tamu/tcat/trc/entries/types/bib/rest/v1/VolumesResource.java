package edu.tamu.tcat.trc.entries.types.bib.rest.v1;

import java.util.Collection;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import edu.tamu.tcat.trc.entries.repo.NoSuchCatalogRecordException;
import edu.tamu.tcat.trc.entries.types.bib.Edition;
import edu.tamu.tcat.trc.entries.types.bib.Volume;
import edu.tamu.tcat.trc.entries.types.bib.repo.EditWorkCommand;
import edu.tamu.tcat.trc.entries.types.bib.repo.EditionMutator;
import edu.tamu.tcat.trc.entries.types.bib.repo.VolumeMutator;
import edu.tamu.tcat.trc.entries.types.bib.repo.WorkRepository;

@Path("/works/{workId}/editions/{editionId}/volumes")
public class VolumesResource
{
   private WorkRepository repo;

   public void activate()
   {
   }

   public void dispose()
   {
   }

   public void setRepository(WorkRepository repo)
   {
      this.repo = repo;
   }

   @GET
   @Produces(MediaType.APPLICATION_JSON)
   public Collection<RestApiV1.Volume> listVolumes(@PathParam(value = "workId") String workId,
                                           @PathParam(value = "editionId") String editionId) throws NumberFormatException, NoSuchCatalogRecordException
   {
      Edition edition = repo.getEdition(workId, editionId);
      return edition.getVolumes().stream()
            .map(RepoAdapter::toDTO)
            .collect(Collectors.toSet());
   }

   @GET
   @Path("{volumeId}")
   @Produces(MediaType.APPLICATION_JSON)
   public RestApiV1.Volume getVolume(@PathParam(value = "workId") String workId,
                             @PathParam(value = "editionId") String editionId,
                             @PathParam(value = "volumeId") String volumeId) throws NumberFormatException, NoSuchCatalogRecordException
   {
      Volume volume = repo.getVolume(workId, editionId, volumeId);
      return RepoAdapter.toDTO(volume);
   }

   @PUT
   @Path("{volumeId}")
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   public RestApiV1.VolumeId updateVolume(@PathParam(value = "workId") String workId,
                                       @PathParam(value = "editionId") String editionId,
                                       @PathParam(value = "volumeId") String volumeId,
                                       RestApiV1.Volume volume) throws NoSuchCatalogRecordException
   {
      EditWorkCommand editWorkCommand = repo.edit(workId);
      EditionMutator editionMutator = editWorkCommand.editEdition(editionId);
      VolumeMutator volumeMutator = editionMutator.editVolume(volumeId);
      volumeMutator.setAll(RepoAdapter.toRepo(volume));
      editWorkCommand.execute();
      RestApiV1.VolumeId vid = new RestApiV1.VolumeId();
      vid.id = volumeMutator.getId();
      return vid;
   }

   @POST
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   public RestApiV1.VolumeId createVolume(@PathParam(value = "workId") String workId,
                                       @PathParam(value = "editionId") String editionId,
                                       RestApiV1.Volume volume) throws NoSuchCatalogRecordException
   {
      EditWorkCommand editWorkCommand = repo.edit(workId);
      EditionMutator editionMutator = editWorkCommand.editEdition(editionId);
      VolumeMutator volumeMutator = editionMutator.createVolume();
      volumeMutator.setAll(RepoAdapter.toRepo(volume));
      editWorkCommand.execute();
      RestApiV1.VolumeId vid = new RestApiV1.VolumeId();
      vid.id = volumeMutator.getId();
      return vid;
   }

   @DELETE
   @Path("{volumeId}")
   public void deleteVolume(@PathParam(value = "workId") String workId,
                          @PathParam(value = "volumeId") String volumeId) throws NoSuchCatalogRecordException
   {
      EditWorkCommand command = repo.edit(workId);
      command.removeVolume(volumeId);
      command.execute();
   }
}