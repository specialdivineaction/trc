package edu.tamu.tcat.trc.categorization.impl;

import edu.tamu.tcat.trc.categorization.CategorizationNodeMutator;
import edu.tamu.tcat.trc.entries.core.EntryReference;
import edu.tamu.tcat.trc.repo.ChangeSet;

public class BaseCategorizationNodeMutator implements CategorizationNodeMutator
{
   protected final ChangeSet<PersistenceModelV1.TreeNode> changes;
   protected final String id;

   public BaseCategorizationNodeMutator(String id, ChangeSet<PersistenceModelV1.TreeNode> changes)
   {
      this.id = id;
      this.changes = changes;
   }

   @Override
   public final void setLabel(String label)
   {
      changes.add("label", dto -> dto.label = label);
   }

   @Override
   public final void setDescription(String description)
   {
      changes.add("description", dto -> dto.description = description);
   }

   @Override
   public final void associateEntityRef(EntryReference ref)
   {
      EntryReference refDto = PersistenceModelV1Adapter.copy(ref);
      changes.add("ref", dto -> dto.ref = refDto);
   }

}