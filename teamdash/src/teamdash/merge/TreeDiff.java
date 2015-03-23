// Copyright (C) 2012 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 3
// of the License, or (at your option) any later version.
//
// Additional permissions also apply; see the README-license.txt
// file in the project root directory for more information.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package teamdash.merge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sourceforge.processdash.util.Diff;

import teamdash.merge.TreeNodeChange.Type;

public class TreeDiff<ID, Content> {

    /** The base tree */
    private TreeNode<ID, Content> baseRoot;

    /** Modifications to the tree */
    private TreeNode<ID, Content> modRoot;

    /** An object which can tell us if two pieces of content are equal */
    private ContentMerger<ID, Content> contentComparator;

    /** A list of the computed changes between the two trees */
    private List<TreeNodeChange> changes;

    /** Collections of nodes that experienced certain types of edits */
    private Map<Type, Set<ID>> changedNodeIDs;


    public TreeDiff(TreeNode<ID, Content> baseRoot,
            TreeNode<ID, Content> modifiedRoot,
            ContentMerger<ID, Content> contentComparator) {
        this.baseRoot = baseRoot;
        this.modRoot = modifiedRoot;
        this.contentComparator = contentComparator;

        computeChanges();
    }

    public TreeNode<ID, Content> getBaseRoot() {
        return baseRoot;
    }

    public TreeNode<ID, Content> getModifiedRoot() {
        return modRoot;
    }

    public List<TreeNodeChange> getChanges() {
        return changes;
    }

    public Set<ID> getChangedNodeIDs(TreeNodeChange.Type type) {
        return changedNodeIDs.get(type);
    }

    public boolean nodeWasChanged(ID id, TreeNodeChange.Type type) {
        return getChangedNodeIDs(type).contains(id);
    }

    public boolean nodeWasChanged(ID id, TreeNodeChange.Type[] types) {
        for (Type t : types)
            if (nodeWasChanged(id, t))
                return true;
        return false;
    }

    public boolean nodeWasChanged(ID id, TreeNodeChange.Type firstType,
            TreeNodeChange.Type... additionalTypes) {
        return (nodeWasChanged(id, firstType)
                || nodeWasChanged(id, additionalTypes));
    }

    private void computeChanges() {
        // initialize data objects
        changes = new ArrayList<TreeNodeChange>();
        changedNodeIDs = new HashMap<Type, Set<ID>>();

        // compute the various types of changes that were made in the tree
        computeNodeDeletions();
        computeNodeAdditions();
        computeNodeModifications();

        // finalize our data objects to avoid external modifications
        changes = Collections.unmodifiableList(changes);
        for (Map.Entry<Type, Set<ID>> e : changedNodeIDs.entrySet()) {
            e.setValue(Collections.unmodifiableSet(e.getValue()));
        }
    }

    private void computeNodeDeletions() {
        // infer the set of deleted nodes by computing the set difference
        Set<ID> deletedNodeIDs = new HashSet<ID>(baseRoot.getIDMap().keySet());
        deletedNodeIDs.removeAll(modRoot.getIDMap().keySet());
        changedNodeIDs.put(Type.Delete, deletedNodeIDs);

        // record change operations representing each deletion
        for (ID id : deletedNodeIDs) {
            TreeNode<ID, Content> deletedNode = baseRoot.findNode(id);
            recordChange(Type.Delete, deletedNode);
        }
    }

    private void computeNodeAdditions() {
        // infer the set of added nodes by computing the set difference
        Set<ID> addedNodeIDs = new HashSet<ID>(modRoot.getIDMap().keySet());
        addedNodeIDs.removeAll(baseRoot.getIDMap().keySet());
        changedNodeIDs.put(Type.Add, addedNodeIDs);

        // record change operations representing each addition
        for (ID id : addedNodeIDs) {
            TreeNode<ID, Content> addedNode = modRoot.findNode(id);
            recordChange(Type.Add, addedNode);
        }
    }

    private void computeNodeModifications() {
        // Infer the set of nodes that are common to both trees
        Set<ID> commonNodeIDs = new HashSet<ID>(baseRoot.getIDMap().keySet());
        commonNodeIDs.retainAll(modRoot.getIDMap().keySet());

        // Determine which of these nodes were moved and/or edited
        computeEditedAndMovedNodes(commonNodeIDs);

        // Finally, determine which nodes have been reordered
        computeReorderedNodes(commonNodeIDs);
    }

    private void computeEditedAndMovedNodes(Set<ID> commonNodeIDs) {
        // build data structures to hold changes
        Set<ID> editedNodeIDs = new HashSet<ID>();
        changedNodeIDs.put(Type.Edit, editedNodeIDs);
        Set<ID> movedNodeIDs = new HashSet<ID>();
        changedNodeIDs.put(Type.Move, movedNodeIDs);

        // iterate over common nodes, looking for edits and far moves
        for (ID id : commonNodeIDs) {
            TreeNode<ID, Content> baseNode = baseRoot.findNode(id);
            TreeNode<ID, Content> modNode = modRoot.findNode(id);

            // if content differs, log an edit operation
            if (wasEdited(baseNode, modNode)) {
                editedNodeIDs.add(id);
                recordChange(Type.Edit, modNode);
            }

            // if the node has been moved, log a move operation
            if (wasMoved(baseNode, modNode)) {
                movedNodeIDs.add(id);
                recordChange(Type.Move, modNode);
            }
        }
    }

    private boolean wasEdited(TreeNode<ID, Content> baseNode,
            TreeNode<ID, Content> modNode) {
        Content baseContent = baseNode.getContent();
        Content modContent = modNode.getContent();
        return !contentComparator.isEqual(baseContent, modContent);
    }

    private boolean wasMoved(TreeNode<ID, Content> baseNode,
            TreeNode<ID, Content> modNode) {
        TreeNode<ID, Content> baseParent = baseNode.getParent();
        TreeNode<ID, Content> modParent = modNode.getParent();
        if (baseParent == null && modParent == null)
            return false;
        else if (baseParent == null || modParent == null)
            return true;

        ID baseID = baseParent.getID();
        ID modID = modParent.getID();
        return !baseID.equals(modID);
    }

    private void computeReorderedNodes(Set<ID> commonNodeIDs) {
        // make a list of the nodes that were added, deleted, or moved within
        // the tree.  Nodes are not considered "reordered" if the only change
        // to their predecessor was the arrival or departure of one of these
        // "volatile" nodes.
        Set<ID> volatileNodeIDs = new HashSet<ID>();
        volatileNodeIDs.addAll(getChangedNodeIDs(Type.Add));
        volatileNodeIDs.addAll(getChangedNodeIDs(Type.Move));
        volatileNodeIDs.addAll(getChangedNodeIDs(Type.Delete));

        // build a data structure to hold changes
        Set<ID> reorderedNodeIDs = new HashSet<ID>();
        changedNodeIDs.put(Type.Reorder, reorderedNodeIDs);

        // Now, scan the tree and find any parent nodes whose nonvolatile
        // children have been reordered. Compute the reordered nodes for each
        // such parent
        for (ID id : commonNodeIDs)
            checkForReorderedChildrenOfNode(id, volatileNodeIDs,
                reorderedNodeIDs);

        // record change operations representing each reordering operation
        for (ID id : reorderedNodeIDs) {
            TreeNode<ID, Content> reorderedNode = modRoot.findNode(id);
            recordChange(Type.Reorder, reorderedNode);
        }
    }

    private void checkForReorderedChildrenOfNode(ID parentID,
            Set<ID> volatileNodeIDs, Set<ID> dest) {
        // look in the base tree to see if this node has nonvolatile kids
        List<ID> baseKids = getIDsOfNonvolatileChildren(baseRoot, parentID,
            volatileNodeIDs);
        if (baseKids.isEmpty())
            // either this node was a leaf in the base tree, or all of its
            // children were deleted or moved away.  We don't have to look for
            // reordering operations.
            return;

        // Get the list of nonvolatile children in the modified tree.
        List<ID> modKids = getIDsOfNonvolatileChildren(modRoot, parentID,
            volatileNodeIDs);
        if (baseKids.equals(modKids))
            // If the list of children is identical; nothing was moved.
            return;

        // Find common subsequences that did not change and compute the smallest
        // possible diff between the two lists.  The nodes that appear in the
        // diff are the ones that moved.
        Diff diff = new Diff(baseKids.toArray(), modKids.toArray());
        Diff.change c = diff.diff_2(false);
        while (c != null) {
            addIDs(dest, baseKids, c.line0, c.deleted);
            addIDs(dest, modKids, c.line1, c.inserted);
            c = c.link;
        }
    }

    private List<ID> getIDsOfNonvolatileChildren(TreeNode<ID, Content> root,
            ID parentID, Set<ID> volatileNodeIDs) {
        TreeNode<ID, Content> parent = root.findNode(parentID);
        if (parent == null || parent.getChildren().isEmpty())
            return Collections.EMPTY_LIST;

        List<ID> result = new ArrayList<ID>(parent.getChildren().size());
        for (TreeNode<ID, Content> child : parent.getChildren()) {
            if (!volatileNodeIDs.contains(child.getID()))
                result.add(child.getID());
        }
        return result;
    }

    private void addIDs(Set<ID> dest, List<ID> list, int start, int count) {
        for (int i = start + count;  i-- > start; )
            dest.add(list.get(i));
    }


    private void recordChange(TreeNodeChange.Type type, TreeNode node) {
        TreeNodeChange change = new TreeNodeChange(type, node);
        changes.add(change);
    }

}
