package edu.tamu.tcat.trc.services.categorization.strategies.tree;

import edu.tamu.tcat.trc.services.categorization.CategorizationNodeMutator;

/**
 *  Extends the basic node mutator to support structural edits of a {@link TreeNode}.
 */
public interface TreeNodeMutator extends CategorizationNodeMutator
{
   /**
    * Creates a new node and appends it to the end of the list of children.
    *
    * @param label A label for the node to be created.
    * @return A mutator to edit the created node.
    */
   TreeNodeMutator add(String label);

   /**
    * Creates a new child node at the specified index.
    *
    * @param index The index at which the new child should be added.
    * @return A mutator to edit the created node.
    */
   TreeNodeMutator insert(String label, int index);

   /**
    * Edit an existing child of this node. Note that the behavior is undefined if
    * the identified node is not a child of this node.
    *
    * @param id The id of the child node to edit.
    * @return A mutator to edit the indicated node.
    */
   TreeNodeMutator edit(String id);

   /**
    * Removes the child node having the specified id.
    *
    * @param id The id of the child to remove.
    */
   void removeChild(String id);
}
