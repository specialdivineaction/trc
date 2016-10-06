package edu.tamu.tcat.trc.impl.psql.services.bibref;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import edu.tamu.tcat.account.Account;
import edu.tamu.tcat.trc.impl.psql.entries.DbEntryRepositoryRegistry;
import edu.tamu.tcat.trc.impl.psql.services.ServiceFactory;
import edu.tamu.tcat.trc.impl.psql.services.bibref.model.ReferenceCollectionImpl;
import edu.tamu.tcat.trc.impl.psql.services.bibref.repo.DataModelV1;
import edu.tamu.tcat.trc.impl.psql.services.bibref.repo.EditBibliographyCommandFactory;
import edu.tamu.tcat.trc.repo.DocRepoBuilder;
import edu.tamu.tcat.trc.repo.DocumentRepository;
import edu.tamu.tcat.trc.resolver.EntryReference;
import edu.tamu.tcat.trc.resolver.EntryResolverRegistry;
import edu.tamu.tcat.trc.services.ServiceContext;
import edu.tamu.tcat.trc.services.bibref.ReferenceCollection;
import edu.tamu.tcat.trc.services.bibref.repo.EditBibliographyCommand;
import edu.tamu.tcat.trc.services.bibref.repo.RefCollectionService;

public class RefServiceFactory implements ServiceFactory<RefCollectionService>
{
   private static final Logger logger = Logger.getLogger(RefServiceFactory.class.getName());

   private static final String TABLE_NAME = "bibrefs";
   private static final String SCHEMA_DATA_FIELD = "data";

   private final DbEntryRepositoryRegistry repoRegistry;
   private final DocumentRepository<ReferenceCollection, DataModelV1.ReferenceCollection, EditBibliographyCommand> docRepo;

   public RefServiceFactory(DbEntryRepositoryRegistry repoRegistry)
   {
      this.repoRegistry = repoRegistry;
      this.docRepo = initDocRepo();
   }

   /**
    * @param context
    * @return A document repository for persisting and retrieving {@link ReferenceCollection} instances.
    */
   private DocumentRepository<ReferenceCollection, DataModelV1.ReferenceCollection, EditBibliographyCommand> initDocRepo()
   {
      EntryResolverRegistry resolverRegistry = repoRegistry.getResolverRegistry();
      EditBibliographyCommandFactory editCommandFactory = new EditBibliographyCommandFactory(resolverRegistry);

      DocRepoBuilder<ReferenceCollection, DataModelV1.ReferenceCollection, EditBibliographyCommand> builder =
            repoRegistry.getDocRepoBuilder();
      builder.setTableName(TABLE_NAME);
      builder.setDataColumn(SCHEMA_DATA_FIELD);
      builder.setEditCommandFactory(editCommandFactory);
      builder.setDataAdapter(ReferenceCollectionImpl::new);
      builder.setStorageType(DataModelV1.ReferenceCollection.class);
      builder.setEnableCreation(true);

      return builder.build();
   }

   @Override
   public Class<RefCollectionService> getType()
   {
      return RefCollectionService.class;
   }

   /**
    * Stops the bibliographic reference repository service, releasing allocated resources.
    * Once stopped, the service must be re-initialized and {@link RefServiceFactory#start} must be called in order to restart.
    */
   @Override
   public void shutdown()
   {
      docRepo.dispose();
   }

   @Override
   public RefCollectionService getService(ServiceContext<RefCollectionService> context)
   {
      return new ReferenceRepositoryImpl(context);
   }

   private class ReferenceRepositoryImpl implements RefCollectionService
   {
      private EntryResolverRegistry resolverRegistry;
      private final ServiceContext<RefCollectionService> context;
      private final Account account;

      public ReferenceRepositoryImpl(ServiceContext<RefCollectionService> context)
      {
         this.resolverRegistry = repoRegistry.getResolverRegistry();
         this.context = context;
         this.account = this.context.getAccount().orElse(null);
      }

      @Override
      public ReferenceCollection get(EntryReference ref)
      {
         return get(resolverRegistry.tokenize(ref));
      }

      @Override
      public EditBibliographyCommand edit(EntryReference ref)
      {
         return edit(resolverRegistry.tokenize(ref));
      }

      @Override
      public CompletableFuture<Boolean> delete(EntryReference ref)
      {
         return delete(resolverRegistry.tokenize(ref));
      }

      @Override
      public ReferenceCollection get(String id)
      {
         return docRepo.get(id).orElseGet(ReferenceCollectionImpl::new);
      }

      @Override
      public EditBibliographyCommand edit(String id)
      {
         return docRepo.get(id)
               .map(o -> docRepo.edit(account, id))
               .orElseGet(() -> docRepo.create(account, id));
      }

      @Override
      public CompletableFuture<Boolean> delete(String id)
      {
         return docRepo.delete(account, id);
      }
   }
}
