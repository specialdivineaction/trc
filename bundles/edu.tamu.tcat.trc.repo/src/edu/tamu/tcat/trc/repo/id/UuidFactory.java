package edu.tamu.tcat.trc.repo.id;

import java.util.UUID;

public class UuidFactory implements IdFactory
{
   @Override
   public String get()
   {
      return UUID.randomUUID().toString();
   }
}
