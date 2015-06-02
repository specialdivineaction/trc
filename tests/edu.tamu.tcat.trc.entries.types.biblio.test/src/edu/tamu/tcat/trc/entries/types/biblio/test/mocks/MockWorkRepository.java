package edu.tamu.tcat.trc.entries.types.biblio.test.mocks;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import edu.tamu.tcat.trc.entries.core.IdFactory;
import edu.tamu.tcat.trc.entries.repo.NoSuchCatalogRecordException;
import edu.tamu.tcat.trc.entries.types.bib.AuthorReference;
import edu.tamu.tcat.trc.entries.types.bib.Edition;
import edu.tamu.tcat.trc.entries.types.bib.Volume;
import edu.tamu.tcat.trc.entries.types.bib.Work;
import edu.tamu.tcat.trc.entries.types.bib.dto.WorkDV;
import edu.tamu.tcat.trc.entries.types.bib.repo.EditWorkCommand;
import edu.tamu.tcat.trc.entries.types.bib.repo.WorkRepository;
import edu.tamu.tcat.trc.entries.types.bib.repo.WorkChangeEvent;
import edu.tamu.tcat.trc.entries.types.bio.Person;

/**
 * In memory implementation of the {@link WorkRepository} for use in testing.
 */
public class MockWorkRepository implements WorkRepository
{
   private final IdFactory idFactory = new MockIdFactory();
   private final Map<String, Work> cache = new HashMap<>();

   @Override
   public Iterable<Work> listWorks()
   {
      return cache.values();
   }

   @Override
   public Iterable<Work> listWorks(String title)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Work getWork(String workId) throws NoSuchCatalogRecordException
   {
      if (!cache.containsKey(workId))
         throw new NoSuchCatalogRecordException("No record found for work [" + workId + "]");

      return cache.get(workId);
   }

   @Override
   public Edition getEdition(String workId, String editionId) throws NoSuchCatalogRecordException
   {
      Work w = getWork(workId);
      Edition edition = w.getEditions().stream()
                              .filter(e -> e.getId().equals(editionId))
                              .findAny()
                              .orElse(null);

      if (edition == null)
         throw new NoSuchCatalogRecordException("No record found for edition [" + editionId + "]");

      return edition;
   }

   @Override
   public Volume getVolume(String workId, String editionId, String volumeId) throws NoSuchCatalogRecordException
   {
      Edition edition = getEdition(workId, editionId);
      Volume volume = edition.getVolumes().stream()
                           .filter(v -> v.getId().equals(volumeId))
                           .findAny()
                           .orElse(null);

      if (volume == null)
         throw new NoSuchCatalogRecordException("No record found for volume [" + volumeId + "]");

      return volume;
   }

   @Override
   public Person getAuthor(AuthorReference ref)
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public EditWorkCommand create()
   {
      return create(idFactory.getNextId("works"));
   }

   @Override
   public EditWorkCommand create(String id)
   {
      // TODO Auto-generated method stub
      WorkDV dto = new WorkDV();
      dto.id = id;
      return new MockEditWorkCommand(dto, idFactory, (update) ->
      {
         // TODO fire notifications
         cache.put(update.id, WorkDV.instantiate(update));
      });
   }

   @Override
   public EditWorkCommand edit(String id) throws NoSuchCatalogRecordException
   {
      WorkDV dto = WorkDV.create(getWork(id));
      return new MockEditWorkCommand(dto, idFactory, (update) ->
      {
         // TODO fire notifications
         cache.put(update.id, WorkDV.instantiate(update));
      });
   }

   @Override
   public void delete(String id)
   {
      cache.remove(id);
   }

   @Override
   public AutoCloseable addBeforeUpdateListener(Consumer<WorkChangeEvent> ears)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public AutoCloseable addAfterUpdateListener(Consumer<WorkChangeEvent> ears)
   {
      // TODO Auto-generated method stub
      return null;
   }
}
