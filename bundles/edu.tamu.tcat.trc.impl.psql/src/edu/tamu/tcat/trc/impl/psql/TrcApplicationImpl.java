package edu.tamu.tcat.trc.impl.psql;

import java.net.URI;
import java.util.Objects;

import edu.tamu.tcat.account.Account;
import edu.tamu.tcat.osgi.config.ConfigurationProperties;
import edu.tamu.tcat.trc.ConfigParams;
import edu.tamu.tcat.trc.EntryFacade;
import edu.tamu.tcat.trc.TrcApplication;
import edu.tamu.tcat.trc.entries.core.repo.EntryRepositoryRegistry;
import edu.tamu.tcat.trc.repo.id.IdFactory;
import edu.tamu.tcat.trc.repo.id.IdFactoryProvider;
import edu.tamu.tcat.trc.resolver.EntryReference;
import edu.tamu.tcat.trc.resolver.EntryResolverRegistry;
import edu.tamu.tcat.trc.services.TrcServiceManager;

public class TrcApplicationImpl implements TrcApplication
{

   private IdFactoryProvider idFactoryProvider;
   private ConfigurationProperties config;
   private EntryRepositoryRegistry repositories;
   private EntryResolverRegistry resolvers;
   private TrcServiceManager svcMgr;

   public TrcApplicationImpl()
   {
      // TODO Auto-generated constructor stub
   }

   public void setIdFactory(IdFactoryProvider idFactoryProvider)
   {
      this.idFactoryProvider = idFactoryProvider;
   }

   public void setConfiguration(ConfigurationProperties config)
   {
      this.config = config;
   }

   public void setEntryRepoRegistry(EntryRepositoryRegistry entryRepo)
   {
      this.repositories = entryRepo;
      this.resolvers = entryRepo.getResolverRegistry();
   }

   public void setServiceManager(TrcServiceManager svcMgr)
   {
      this.svcMgr = svcMgr;
   }

   public void activate()
   {
      Objects.requireNonNull(idFactoryProvider);
      Objects.requireNonNull(config);
      Objects.requireNonNull(repositories);
      Objects.requireNonNull(resolvers);
   }

   public void deactivate()
   {

   }
   @Override
   public URI getApiEndpoint()
   {
      return config.getPropertyValue(ConfigParams.API_ENDPOINT_PARAM, URI.class, URI.create(""));
   }

   @Override
   public ConfigurationProperties getConfig()
   {
      return config;
   }

   @Override
   public EntryRepositoryRegistry getEntryRepositoryManager()
   {
      return repositories;
   }

   @Override
   public EntryResolverRegistry getResolverRegistry()
   {
      return resolvers;
   }

   @Override
   public TrcServiceManager getServiceManager()
   {
      return svcMgr;
   }

   @Override
   public IdFactory getIdFactory(String scope)
   {
      return idFactoryProvider.getIdFactory(scope);
   }

   @Override
   public <T> EntryFacade<T> getEntryFacade(EntryReference ref, Class<T> type, Account account)
   {
      return new EntryFacadeImpl<>(svcMgr, repositories, ref, account, type);
   }

   @Override
   public <T> EntryFacade<T> getEntryFacade(T entry, Class<T> type, Account account)
   {
      EntryReference ref = resolvers.getResolver(entry).makeReference(entry);
      return new EntryFacadeImpl<>(svcMgr, repositories, ref, account, type);
   }

}
