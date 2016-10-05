package edu.tamu.tcat.trc.resolver;

import static java.text.MessageFormat.format;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A basic implementation of the {@link EntryResolverRegistry}. Intended to be registered as
 * a declarative service or otherwise accessed through dependency injection in order to ensure
 * that only one instance is present in the application.
 */
public class BasicResolverRegistry implements EntryResolverRegistry
{

   private final Map<UUID, EntryResolver<?>> resolvers = new ConcurrentHashMap<>();

   @Override
   public <T> Registration register(EntryResolver<T> resolver)
   {
      UUID registrationId = UUID.randomUUID();
      resolvers.put(registrationId, resolver);
      return () -> resolvers.remove(registrationId);
   }

   @Override
   @SuppressWarnings({ "unchecked", "rawtypes" })  // HACK: NOT TYPE SAFE
   public <T> EntryResolver<T> getResolver(EntryReference ref) throws InvalidReferenceException
   {
      return (EntryResolver)resolvers.values().parallelStream()
         .filter(resolver -> resolver.accepts(ref))
         .findAny()
         .orElseThrow(() -> new InvalidReferenceException(ref, "No registered resolver accpets this reference"));
   }

   @Override
   @SuppressWarnings("unchecked")  // Type safety enforced by resolver's #accepts method
   public <T> EntryResolver<T> getResolver(T entry)
   {
      return (EntryResolver<T>)resolvers.values().parallelStream()
            .filter(resolver -> resolver.accepts(entry))
            .findAny()
            .orElseThrow(() -> new InvalidReferenceException((Object)null, "No registered resolver accpets this entry"));
   }

   @Override
   public String tokenize(EntryReference ref)
   {
      // ensure that a resolver exists for this reference.
      this.getResolver(ref);

      // HACK this is an arbitrary restriction on ids and may not be robust
      //      to future changes. Need a better tokenization strategy.
      if (ref.id.contains("::"))
         throw new IllegalStateException("Cannot tokenize reference with id " + ref.id);
      String key = ref.id + "::" + ref.type;
      return Base64.getEncoder().encodeToString(key.getBytes());
   }

   @Override
   public EntryReference decodeToken(String token)
   {
      EntryReference ref;
      byte[] bytes = Base64.getDecoder().decode(token);
      try
      {
         String key = new String(bytes, "UTF-8");
         int ix = key.indexOf("::");
         if (ix < 0 || ix >= key.length() - 3)
            throw new IllegalArgumentException(format("Invalid entry reference token {0}", token));

         ref = new EntryReference();
         ref.id = key.substring(0, ix);
         ref.type = key.substring(ix + 2);
      }
      catch (Exception e)
      {
         throw new IllegalArgumentException(format("Invalid entry reference token {0}", token));
      }

      // ensure that a resolver exists for this reference.
      this.getResolver(ref);
      return ref;
   }

}