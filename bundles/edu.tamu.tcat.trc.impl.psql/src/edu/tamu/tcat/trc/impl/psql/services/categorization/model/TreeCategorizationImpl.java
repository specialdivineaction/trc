package edu.tamu.tcat.trc.impl.psql.services.categorization.model;

import static java.text.MessageFormat.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.tamu.tcat.trc.impl.psql.services.categorization.repo.PersistenceModelV1;
import edu.tamu.tcat.trc.resolver.EntryResolverRegistry;
import edu.tamu.tcat.trc.services.categorization.CategorizationNode;
import edu.tamu.tcat.trc.services.categorization.strategies.tree.TreeCategorization;
import edu.tamu.tcat.trc.services.categorization.strategies.tree.TreeNode;

public class TreeCategorizationImpl extends CategorizationImpl implements TreeCategorization
{

   private final String rootId;
   private final Map<String, TreeNodeImpl> nodeMap;

   public TreeCategorizationImpl(PersistenceModelV1.TreeCategorizationStrategy dto, EntryResolverRegistry registry)
   {
      super(dto);

      nodeMap = dto.nodes.values().stream()
            .map(node -> new TreeNodeImpl(registry, this, node))
            .collect(Collectors.toMap(n -> n.getId(),
                     Function.identity()));
      rootId = dto.root;
   }

   @Override
   public TreeNode getRootNode()
   {
      return nodeMap.get(rootId);
   }

   @Override
   public TreeNode getNode(String id) throws IllegalArgumentException
   {
      String notFoundError = "The node {0} is not defined for categorization scheme {1}.";
      if (!nodeMap.containsKey(id))
         throw new IllegalArgumentException(format(notFoundError, id, this.getId()));

      return nodeMap.get(id);
   }

   @Override
   public Stream<CategorizationNode> getNodes()
   {
      return preorderTraversal(getRootNode());
   }

   /**
    * Performs a preorder traversal of the subtree rooted at the given node.
    * @param node
    * @return All children in preorder.
    */
   private static Stream<CategorizationNode> preorderTraversal(TreeNode node)
   {
      return Stream.concat(
            Stream.of(node),
            node.getChildren().stream().flatMap(TreeCategorizationImpl::preorderTraversal));
   }

   public class TreeNodeImpl extends CategorizationNodeImpl implements TreeNode
   {
      private final String parentId;
      private final ArrayList<String> children;
   
      public TreeNodeImpl(EntryResolverRegistry registry,
                          TreeCategorizationImpl scheme,
                          PersistenceModelV1.TreeNode dto)
      {
         super(registry, scheme, dto);
   
         this.parentId = dto.parentId;
         this.children = new ArrayList<>(dto.children);
      }
   
      @Override
      public String getParentId()
      {
         return parentId;
      }
   
      @Override
      public List<TreeNode> getChildren()
      {
         return children.stream()
               .map(this::getNode)
               .collect(Collectors.toList());
      }
   
      private TreeNode getNode(String id)
      {
         if (!children.contains(id))
            throw new IllegalArgumentException(format("The requested node {0} is not a child of {1}.", id, this.id));
   
         TreeNodeImpl node = ((TreeCategorizationImpl)scheme).nodeMap.get(id);
         if (node == null)
            throw new IllegalArgumentException(format("Cannot find node {0}.", id));
   
         return node;
      }
   }
}