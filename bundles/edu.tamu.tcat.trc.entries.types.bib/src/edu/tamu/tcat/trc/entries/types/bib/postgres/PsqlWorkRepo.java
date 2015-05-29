package edu.tamu.tcat.trc.entries.types.bib.postgres;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.postgresql.util.PGobject;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.tamu.tcat.db.exec.sql.SqlExecutor;
import edu.tamu.tcat.db.exec.sql.SqlExecutor.ExecutorTask;
import edu.tamu.tcat.trc.entries.core.IdFactory;
import edu.tamu.tcat.trc.entries.notification.DataUpdateObserverAdapter;
import edu.tamu.tcat.trc.entries.notification.ObservableTaskWrapper;
import edu.tamu.tcat.trc.entries.repo.NoSuchCatalogRecordException;
import edu.tamu.tcat.trc.entries.types.bib.AuthorReference;
import edu.tamu.tcat.trc.entries.types.bib.Edition;
import edu.tamu.tcat.trc.entries.types.bib.Title;
import edu.tamu.tcat.trc.entries.types.bib.Volume;
import edu.tamu.tcat.trc.entries.types.bib.Work;
import edu.tamu.tcat.trc.entries.types.bib.dto.EditionDV;
import edu.tamu.tcat.trc.entries.types.bib.dto.WorkDV;
import edu.tamu.tcat.trc.entries.types.bib.repo.EditWorkCommand;
import edu.tamu.tcat.trc.entries.types.bib.repo.WorkRepository;
import edu.tamu.tcat.trc.entries.types.bib.repo.WorkChangeEvent;
import edu.tamu.tcat.trc.entries.types.bib.repo.WorkNotAvailableException;
import edu.tamu.tcat.trc.entries.types.bib.repo.WorkChangeEvent.ChangeType;
import edu.tamu.tcat.trc.entries.types.bio.Person;
import edu.tamu.tcat.trc.entries.types.bio.repo.PeopleRepository;

public class PsqlWorkRepo implements WorkRepository
{
   private static final Logger logger = Logger.getLogger(PsqlWorkRepo.class.getName());

   private final static String GET_SQL = "SELECT work FROM works WHERE id=?";
   private final static String LIST_WORKS_SQL = "SELECT work FROM works WHERE active = true";
   private final static String UPDATE_WORK_SQL = "UPDATE works SET work = ?, modified = now() WHERE id = ?";
   private final static String CREATE_WORK_SQL = "INSERT INTO works (work, id) VALUES(?, ?)";
   private final static String DELETE_SQL = "UPDATE works SET active = false WHERE id = ?";


   public static final String WORK_CONTEXT = "works";

   private final WorkUpdateNotifier deleteNotifier = new WorkUpdateNotifier(ChangeType.DELETED);

   private SqlExecutor exec;
   private ObjectMapper mapper;
   private PeopleRepository peopleRepo;

   // FIXME replace with listener service
   private ExecutorService notifications;

   private final CopyOnWriteArrayList<Consumer<WorkChangeEvent>> listeners = new CopyOnWriteArrayList<>();

   private IdFactory idFactory;

   public PsqlWorkRepo()
   {
   }

   public void setDatabaseExecutor(SqlExecutor exec)
   {
      this.exec = exec;
   }

   public void setPeopleRepo(PeopleRepository repo)
   {
      this.peopleRepo = repo;
   }

   public void setIdFactory(IdFactory idFactory)
   {
      this.idFactory = idFactory;
   }

   public void activate()
   {
      Objects.requireNonNull(exec);

      mapper = new ObjectMapper();
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

      notifications = Executors.newCachedThreadPool();
   }

   public void dispose()
   {
      this.exec = null;
      this.mapper = null;
      shutdownNotificationsExec();
   }

   private void shutdownNotificationsExec()
   {
      try
      {
         notifications.shutdown();
         notifications.awaitTermination(10, TimeUnit.SECONDS);    // HACK: make this configurable
      }
      catch (Exception ex)
      {
         logger.log(Level.WARNING, "Failed to shut down event notifications executor in a timely fashion.", ex);
         try {
            List<Runnable> pendingTasks = notifications.shutdownNow();
            logger.info("Forcibly shutdown notifications executor. [" + pendingTasks.size() + "] pending tasks were aborted.");
         } catch (Exception e) {
            logger.log(Level.SEVERE, "An error occurred attempting to forcibly shutdown executor service", e);
         }
      }
   }

   @Override
   public Person getAuthor(AuthorReference ref)
   {
      String id = ref.getId();
      try {
         return peopleRepo.get(id);
      }
      catch (Exception ex)
      {
         throw new IllegalStateException("Could not retrieve person instance (" + id + ").", ex);
      }
   }

   @Override
   public Iterable<Work> listWorks()
   {
      Future<Iterable<Work>> submit = exec.submit(makeListWorksTask());
      try
      {
         return submit.get();
      }
      catch (ExecutionException e)
      {
         Throwable cause = e.getCause();
         if (cause instanceof RuntimeException)
            throw (RuntimeException)cause;

         throw new IllegalStateException("Unexpected problems while attempting to retrieve work records " , e);
      }
      catch (InterruptedException e)
      {
         throw new IllegalStateException("Failed to retrieve work records", e);
      }
   }


   @Override
   public Iterable<Work> listWorks(String titleName)
   {
      List<Work> workResults = new ArrayList<>();
      Iterable<Work> listWorks = listWorks();
      titleName = titleName.toLowerCase();

      for (Work w : listWorks)
      {
         if (hasTitle(w, titleName))
            workResults.add(w);
      }

      return workResults;
   }

   private boolean hasTitle(Work w, String name)
   {
      for (Title t : w.getTitle().getAlternateTitles())
      {
         if (hasTitleName(t, name)) {
            return true;
         }
      }

      return false;
   }

   private boolean hasTitleName(Title title, String titleName)
   {
      String test = title.getFullTitle();
      if (test != null && test.toLowerCase().contains(titleName))
         return true;

      test = title.getTitle();
      if (test != null && test.toLowerCase().contains(titleName))
         return true;

      return false;
   }

   @Override
   public Work getWork(String workId) throws NoSuchCatalogRecordException
   {
      try
      {
         return exec.submit(makeGetWorkTask(workId)).get();
      }
      catch (ExecutionException e)
      {
         Throwable cause = e.getCause();
         if (cause instanceof NoSuchCatalogRecordException)
            throw (NoSuchCatalogRecordException)cause;
         if (cause instanceof RuntimeException)
            throw (RuntimeException)cause;

         throw new IllegalStateException("Unexpected problems while attempting to retrieve bibliographic entry [" + workId +"]" , e);
      }
      catch (InterruptedException e) {
         throw new IllegalStateException("Failed to retrieve bibliographic entry [" + workId +"]", e);
      }
   }

   @Override
   public Edition getEdition(String workId, String editionId) throws NoSuchCatalogRecordException
   {
      Work work = getWork(workId);
      return work.getEdition(editionId);
   }

   @Override
   public Volume getVolume(String workId, String editionId, String volumeId) throws NoSuchCatalogRecordException
   {
      // TODO pull from DB directly
      Work work = getWork(workId);
      Edition edition = work.getEdition(editionId);
      return edition.getVolume(volumeId);
   }

   @Override
   public EditWorkCommand create()
   {
      WorkDV work = new WorkDV();
      work.id = idFactory.getNextId(WORK_CONTEXT);
      EditWorkCommandImpl command = new EditWorkCommandImpl(work, idFactory);
      command.setCommitHook(dto -> updateWork(dto, ChangeType.CREATED));
      return command;
   }

   @Override
   public EditWorkCommand edit(String id) throws NoSuchCatalogRecordException
   {
      Work work = getWork(id);
      EditWorkCommandImpl command = new EditWorkCommandImpl(WorkDV.create(work), idFactory);
      command.setCommitHook(dto -> updateWork(dto, ChangeType.MODIFIED));
      return command;
   }

   public Future<String> updateWork(WorkDV workDv, ChangeType changeType)
   {
      String sql = getUpdateSql(changeType);

      try
      {
         String json = mapper.writeValueAsString(workDv);
         SqlExecutor.ExecutorTask<String> task = makeUpdateTask(workDv.id, json, sql);

         WorkUpdateNotifier updateNotifier = new WorkUpdateNotifier(changeType);
         ObservableTaskWrapper<String> wrapTask = new ObservableTaskWrapper<String>(task, updateNotifier);

         Future<String> workId = exec.submit(wrapTask);
         return workId;
      }
      catch (Exception e)
      {
         if (e instanceof RuntimeException)
            throw (RuntimeException)e;

         throw new IllegalStateException("Failed to update work [" + workDv.id + "]", e);
      }
   }

   @Override
   public void delete(String id)
   {
      ExecutorTask<String> task = makeDeleteTask(id);
      exec.submit(new ObservableTaskWrapper<String>(task, deleteNotifier));
   }

   private String getUpdateSql(ChangeType changeType)
   {
      String sql;
      switch (changeType)
      {
         case MODIFIED:
            sql = UPDATE_WORK_SQL;
            break;
         case CREATED:
            sql = CREATE_WORK_SQL;
            break;
         default:
            throw new IllegalArgumentException("Change type must be created or modified [" + changeType + "]");
      }
      return sql;
   }

   private SqlExecutor.ExecutorTask<Work> makeGetWorkTask(String workId)
   {
      return (conn) -> {
         try (PreparedStatement ps = conn.prepareStatement(GET_SQL))
         {
            ps.setString(1, workId);
            try (ResultSet rs = ps.executeQuery())
            {
               if (!rs.next())
                  throw new NoSuchCatalogRecordException("No catalog record exists for work id=" + workId);

               PGobject pgo = (PGobject)rs.getObject("work");
               String workJson = pgo.toString();
               try
               {
                  WorkDV dv = mapper.readValue(workJson, WorkDV.class);
                  return WorkDV.instantiate(dv);
               }
               catch (IOException e)
               {
                  throw new IllegalStateException("Failed to parse bibliographic record\n" + workJson, e);
               }
            }
         }
         catch (SQLException e)
         {
            throw new IllegalStateException("Failed to retrieve bibliographic entry [entry id = " + workId + "]", e);
         }
      };
   }

   private SqlExecutor.ExecutorTask<Iterable<Work>> makeListWorksTask()
   {
      return (conn) -> {
         List<Work> works = new ArrayList<>();
         try (PreparedStatement ps = conn.prepareStatement(LIST_WORKS_SQL);
              ResultSet rs = ps.executeQuery())
         {
            while(rs.next())
            {
               PGobject pgo = (PGobject)rs.getObject("work");
               String workJson = pgo.toString();
               try
               {
                  WorkDV dv = mapper.readValue(workJson, WorkDV.class);
                  works.add(WorkDV.instantiate(dv));
               }
               catch (IOException e)
               {
                  throw new IllegalStateException("Failed to parse bibliographic record\n" + workJson, e);
               }
            }

            return works;
         }
         catch (SQLException e)
         {
            throw new IllegalStateException("Failed to list bibliographic entries", e);
         }
      };
   }

   private SqlExecutor.ExecutorTask<String> makeUpdateTask(String id, String json, String sql)
   {
      return (conn) -> {
         try (PreparedStatement ps = conn.prepareStatement(sql))
         {
            PGobject jsonObject = new PGobject();
            jsonObject.setType("json");
            jsonObject.setValue(json);

            ps.setObject(1, jsonObject);
            ps.setString(2, id);

            int ct = ps.executeUpdate();
            if (ct != 1)
               throw new IllegalStateException("Failed to update work. Unexpected number of rows updates [" + ct + "]");

            return id;
         }
         catch(SQLException e)
         {
            throw new IllegalStateException("Failed to update work: [" + id + "].\n" + json);
         }
      };
   }

   private SqlExecutor.ExecutorTask<String> makeDeleteTask(String id)
   {
      return (conn) -> {
         try (PreparedStatement ps = conn.prepareCall(DELETE_SQL))
         {
            ps.setString(1, id);

            int ct = ps.executeUpdate();
            if (ct != 1)
               throw new IllegalStateException("Failed to de-activate work, id: [" + id + "]. Unexpected number of rows updated [" + ct + "]");

            return id;
         }
         catch(SQLException e)
         {
            throw new IllegalStateException("Failed to de-activate work: [" + id + "]");
         }
      };
   }

   private void notifyRelationshipUpdate(ChangeType type, String relnId)
   {
      WorksChangeEventImpl evt = new WorksChangeEventImpl(type, relnId);
      listeners.forEach(ears -> {
         notifications.submit(() -> {
            try {
               ears.accept(evt);
            } catch (Exception ex) {
               logger.log(Level.WARNING, "Call to update listener failed.", ex);
            }
         });
      });
   }

   @Override
   public AutoCloseable addBeforeUpdateListener(Consumer<WorkChangeEvent> ears)
   {
      throw new UnsupportedOperationException("not impl");
   }

   @Override
   public AutoCloseable addAfterUpdateListener(Consumer<WorkChangeEvent> ears)
   {
      listeners.add(ears);
      return () -> listeners.remove(ears);
   }

   private class WorksChangeEventImpl implements WorkChangeEvent
   {
      private final ChangeType type;
      private final String id;

      public WorksChangeEventImpl(ChangeType type, String id)
      {
         this.type = type;
         this.id = id;
      }

      @Override
      public ChangeType getChangeType()
      {
         return type;
      }

      @Override
      public String getWorkId()
      {
         return id;
      }

      @Override
      public Work getWorkEvt() throws WorkNotAvailableException
      {
         try
         {
            return getWork(id);
         }
         catch (NoSuchCatalogRecordException e)
         {
            throw new WorkNotAvailableException("Internal error occured while retrieving work [" + id + "]");
         }
      }

      @Override
      public String toString()
      {
         return "Relationship Change Event: action = " + type + "; id = " + id;
      }

   }

   private final class WorkUpdateNotifier extends DataUpdateObserverAdapter<String>
   {
      private final ChangeType type;

      public WorkUpdateNotifier(ChangeType type)
      {
         this.type = type;
      }

      @Override
      public void onFinish(String id)
      {
         notifyRelationshipUpdate(type, id);
      }
   }

   /**
    * @param work
    * @return Context for generating IDs for Editions within a Work.
    */
   public static String getContext(WorkDV work)
   {
      return WORK_CONTEXT + "/" + work.id;
   }

   /**
    * @param work
    * @param edition
    * @return Context for generating IDs for Volumes within an Edition (subs. w/in a Work).
    */
   public static String getContext(WorkDV work, EditionDV edition)
   {
      return getContext(work) + "/" + edition.id;
   }
}