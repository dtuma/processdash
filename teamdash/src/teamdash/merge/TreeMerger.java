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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import teamdash.merge.MergeWarning.Severity;
import teamdash.merge.TreeNodeChange.Type;

public class TreeMerger<ID, C> {

    public enum Conflict { DeleteParent, TreeCycle };

    /** The base tree */
    private TreeNode<ID, C> base;

    /** The main branch of modifications to the tree */
    private TreeNode<ID, C> main;

    /** A new branch of modifications to the tree */
    private TreeNode<ID, C> incoming;

    /** An object which can perform a 3-way merge of node content */
    private ContentMerger<ID, C> contentMerger;

    /** The list of changes made by the main branch */
    private TreeDiff<ID, C> mainDiff;

    /** The list of changes made by the incoming branch */
    private TreeDiff<ID, C> incomingDiff;

    /** The merged tree */
    private TreeNode<ID, C> merged;

    /** A collection of warnings produced during the merge operation */
    private Set<MergeWarning<ID>> mergeWarnings;

    /** A list of nodes that were deleted in one branch or the other, but
     * that are being retained for the conflict resolution phase */
    private Set<ID> mergedUndeletedNodeIDs;


    public TreeMerger(TreeNode<ID, C> base, TreeNode<ID, C> main,
            TreeNode<ID, C> incoming, ContentMerger<ID, C> contentMerger) {
        this.base = base;
        this.main = main;
        this.incoming = incoming;
        this.contentMerger = contentMerger;
    }

    public TreeNode<ID, C> getBaseTree() {
        return base;
    }

    public TreeNode<ID, C> getMainTree() {
        return main;
    }

    public TreeNode<ID, C> getIncomingTree() {
        return incoming;
    }

    public TreeNode<ID, C> getMergedTree() {
        return merged;
    }

    public Set<MergeWarning<ID>> getMergeWarnings() {
        return mergeWarnings;
    }

    public Set<ID> getMergedUndeletedNodeIDs() {
        return mergedUndeletedNodeIDs;
    }

    /** Merge the data from the two trees */
    public void run() {
        // initialize data structures
        mergeWarnings = new LinkedHashSet<MergeWarning<ID>>();

        // compute diffs of each tree from the base.
        mainDiff = new TreeDiff<ID, C>(base, main, contentMerger);
        incomingDiff = new TreeDiff<ID, C>(base, incoming, contentMerger);

        // to construct the merged tree, start with a copy of main.
        merged = main.copyTree();

        // construct the list of changes, and apply them
        List<TreeNodeChange<ID, C>> incomingChanges = buildIncomingEditList();
        applyChanges(incomingChanges);
    }

    /**
     * Construct a set of effective incoming edits that should be applied
     * to the main tree to create the merged result
     */
    private List<TreeNodeChange<ID, C>> buildIncomingEditList() {
        // start with the edits from the incoming tree
        List<TreeNodeChange<ID, C>> incomingChanges = new LinkedList(
                incomingDiff.getChanges());

        // initialize data structures
        Set<ID> mainNodeIDsToUndelete = new HashSet<ID>();

        // scan the list of incoming changes, looking for conflicts.
        for (Iterator i = incomingChanges.iterator(); i.hasNext();) {
            TreeNodeChange change = (TreeNodeChange) i.next();
            if (checkForIncomingConflicts(change, mainNodeIDsToUndelete))
                i.remove();
        }

        // construct "add" operations for undeleted nodes
        for (ID nodeID : mainNodeIDsToUndelete) {
            TreeNode<ID, C> nodeToUndelete = incoming.findNode(nodeID);
            incomingChanges.add(new TreeNodeChange(Type.Add, nodeToUndelete));
        }
        mergedUndeletedNodeIDs = mainNodeIDsToUndelete;

        // return the list of changes we just built
        return incomingChanges;
    }

    /**
     * Examine an incoming change to see if it conflicts with a change that
     * was made to the main tree.  If so, log appropriate conflicts.
     * @return true if this incoming change should be discarded.
     */
    private boolean checkForIncomingConflicts(TreeNodeChange<ID, C> change,
            Set<ID> nodeIDsToUndelete) {

        boolean ignoreThisChange = false;
        boolean loggedDeleteConflict = false;
        boolean maybeUndeleteParents = false;

        ID changedID = change.getNodeID();
        Type changeType = change.getType();

        if (mainTreeDeletedChangedNode(change)) {
            // the incoming branch edited or moved a node, but that node
            // was deleted from the main branch.  Log a conflict.
            addConflict(changedID, Type.Delete, changeType);
            loggedDeleteConflict = true;

            // the node in question needs to be undeleted so we can apply
            // the change and show the conflict to the user.
            nodeIDsToUndelete.add(changedID);
            maybeUndeleteParents = true;

            // we can delete this change from the list of edits/moves, because
            // we are replacing it with an undeletion operation.
            ignoreThisChange = true;
        }

        if (nodeWasMovedInBothBranches(change)) {
            // this node was moved in both branches. Log a conflict.
            addConflict(changedID, Type.Move, Type.Move);

            // delete this change from the list of edits, prefering the move
            // from the main branch.  (This will make it easier to display the
            // conflict to the user.)
            ignoreThisChange = true;
        }

        if (mainTreeDeletedTargetParent(change)) {
            // the incoming branch inserted or moved a node, but the desired
            // parent of the node was deleted from the main branch.  Log a
            // conflict if we haven't already.
            if (!loggedDeleteConflict)
                addConflict(change.getParentID(), Conflict.DeleteParent,
                    changedID, changeType);

            // we need to restore the parent to the merged tree.
            maybeUndeleteParents = true;
        }

        if (nodeWasAddedInBothBranches(change)) {
            // this node was not present in the base tree, but both branches
            // added it.  Check to see if they placed it in the same location.
            if (nodeHasDifferentTargetParents(change))
                addConflict(changedID, Type.Add, Type.Add);

            // We do not want to add the node to the tree twice, so we don't
            // want to preserve this "add" operation.  However, this incoming
            // operation may have content that we need to preserve.  So we
            // modify this change to become an "edit" operation instead.
            change.setType(Type.Edit);
        }

        if (maybeUndeleteParents) {
            // if we need parents to exist, check that they do.
            TreeNode<ID, C> parent = change.getParent();
            recordMissingMainNodesToUndelete(nodeIDsToUndelete, parent);
        }

        return ignoreThisChange;
    }

    /** Did the main tree delete a node that was edited or moved by the
     * incoming tree? */
    private boolean mainTreeDeletedChangedNode(TreeNodeChange<ID, C>  change) {
        if (change.getType() != Type.Edit && change.getType() != Type.Move)
            // this change does not represent an important modification to
            // an existing tree node
            return false;

        // check to see if the main tree deleted this node.
        ID changedID = change.getNodeID();
        return mainDiff.nodeWasChanged(changedID, Type.Delete);
    }

    /** Did both trees move the same node, to different locations? */
    private boolean nodeWasMovedInBothBranches(TreeNodeChange<ID, C> change) {
        if (change.getType() != Type.Move)
            // the node was not moved in the incoming branch.
            return false;

        return nodeHasDifferentTargetParents(change);
    }

    /** Did both trees perform a move or add operation, but specify
     * different target parents? */
    private boolean nodeHasDifferentTargetParents(TreeNodeChange<ID, C> change) {
        ID changedID = change.getNodeID();
        if (!mainDiff.nodeWasChanged(changedID, Type.Move, Type.Add))
            // the node was not moved/added in the main branch, so the main
            // branch is not specifying a target parent.
            return false;

        // the node was moved or added in both branches.  Check to see if both
        // moves placed the node in the same location.
        ID mainParentID = main.findNode(changedID).getParent().getID();
        ID incParentID = incoming.findNode(changedID).getParent().getID();
        if (mainParentID.equals(incParentID))
            return false;

        // the two branches have apparently put the node in different places.
        return true;
    }

    /** Did the main tree delete a node that served as the parent of an
     * added or moved node in the incoming tree? */
    private boolean mainTreeDeletedTargetParent(TreeNodeChange<ID, C> change) {
        if (change.getType() != Type.Move && change.getType() != Type.Add)
            // this change does not represent an modification that has a
            // target parent
            return false;

        // check whether the main tree has deleted our target parent.
        ID parentID = change.getParentID();
        return mainDiff.nodeWasChanged(parentID, Type.Delete);
    }

    /** Was this node added to the tree by both branches? */
    private boolean nodeWasAddedInBothBranches(TreeNodeChange<ID, C> change) {
        if (change.getType() != Type.Add)
            // the node was not added in the incoming branch.
            return false;

        // check to see if the main tree also added this node.
        ID changedID = change.getNodeID();
        return mainDiff.nodeWasChanged(changedID, Type.Add);
    }

    /**
     * Check to see if a node was deleted from the main tree.  If so, add
     * it to a list of nodes that need to be undeleted.  Repeat as needed
     * to check for missing parents, grandparents, etc.
     */
    private void recordMissingMainNodesToUndelete(Set<ID> nodeIDsToUndelete,
            TreeNode<ID, C> node) {
        Set<ID> incomingAdds = incomingDiff.getChangedNodeIDs(Type.Add);
        while (node != null) {
            ID nodeID = node.getID();

            // if this node is present in the main tree, we're good.
            if (main.getIDMap().containsKey(nodeID))
                break;

            // if we've already made a note to undelete this node, we're good.
            if (nodeIDsToUndelete.contains(nodeID))
                break;

            // if this node is being inserted by the incoming tree, we're good.
            if (incomingAdds.contains(nodeID))
                break;

            // Otherwise, make a note to undelete this node.  Then check its
            // parent to see if it might be missing as well.
            nodeIDsToUndelete.add(nodeID);
            node = node.getParent();
        }
    }


    /**
     * Apply the list of incoming changes to the merged tree.
     */
    private void applyChanges(List<TreeNodeChange<ID, C>> incomingChanges) {
        processEdits(incomingChanges);
        processMovesInsertionsAndReorderings(incomingChanges);
        processDeletions(incomingChanges);
    }


    /**
     * Look through the list of incoming changes, and perform the requested
     * edits to node content in the merged tree.
     */
    private void processEdits(List<TreeNodeChange<ID, C>> incomingChanges) {
        for (Iterator i = incomingChanges.iterator(); i.hasNext();) {
            TreeNodeChange change = (TreeNodeChange) i.next();
            if (change.getType() == Type.Edit) {
                processEdit(change);
                i.remove();
            }
        }
    }

    private void processEdit(TreeNodeChange<ID, C> change) {
        ID changedID = change.getNodeID();
        C incomingContent = change.getNode().getContent();
        TreeNode<ID, C> targetNode = merged.findNode(changedID);

        C mergedContent;
        if (mainDiff.nodeWasChanged(changedID, Type.Edit, Type.Add)) {
            // this node was edited in both branches.  Ask our merger to sort
            // out the new content appropriately
            TreeNode<ID, C> baseNode = base.findNode(changedID);
            C baseContent = baseNode == null ? null : baseNode.getContent();
            C mainContent = main.findNode(changedID).getContent();
            mergedContent = contentMerger.mergeContent(targetNode, baseContent,
                mainContent, incomingContent, editMergeConflictLogger);
        } else {
            // this node was only edited in the incoming branch.  Adopt the
            // incoming content for the merge.
            mergedContent = incomingContent;
        }
        targetNode.setContent(mergedContent);
    }


    /**
     * Look through the list of incoming changes, and perform the requested
     * additions, moves, and reorderings to nodes in the merged tree.
     */
    private void processMovesInsertionsAndReorderings(
            List<TreeNodeChange<ID, C>> incomingChanges) {
        // get the list of the add/move/reorder changes that need to be
        // processed, and the IDs of the affected nodes.
        List<TreeNodeChange<ID, C>> changesToProcess = new LinkedList();
        Set<ID> nodeIDsToProcess = new HashSet();
        extractAllAddMoveAndReorderOperations(incomingChanges,
            changesToProcess, nodeIDsToProcess);

        // make a list of the nodes that were added or moved in the main branch
        Set<ID> mainAdditionsAndMoves = new HashSet<ID>();
        mainAdditionsAndMoves.addAll(mainDiff.getChangedNodeIDs(Type.Add));
        mainAdditionsAndMoves.addAll(mainDiff.getChangedNodeIDs(Type.Move));

        while (true) {
            // get the next addition, insertion, or reordering to process.
            TreeNodeChange<ID, C> change = extractOneAddMoveOrReorderToProcess(
                changesToProcess, nodeIDsToProcess);
            // if there are no more operations to handle, we can stop.
            if (change == null)
                break;

            ID nodeID = change.getNodeID();
            nodeIDsToProcess.remove(nodeID);

            TreeNode<ID, C> node;
            if (change.getType() == Type.Add) {
                // if we are adding a new nodes, construct it from the
                // incoming ID and content.
                node = new TreeNode(nodeID, change.getNode().getContent());
            } else {
                // if we are moving or reordering a node, find that node
                // within the merged tree.
                node = merged.findNode(nodeID);
            }

            ID parentID = change.getParentID();
            TreeNode<ID, C> targetParent = merged.findNode(parentID);

            if (change.getType() == Type.Reorder) {
                if (node == null)
                    // the incoming branch reordered a node that was deleted
                    // in the main branch.  Ignore the incoming reorder.
                    continue;

                else if (node.getParent() != targetParent)
                    // the incoming branch reordered a node that was moved
                    // in the main branch.  Ignore the incoming reorder.
                    continue;

            } else if (change.getType() == Type.Move) {
                // check to see if this move would create a cycle in the
                // tree.  If so, don't allow it; log a conflict instead.
                if (ancestorsMatch(targetParent, Collections.singleton(nodeID))) {
                    addConflict(parentID, Conflict.TreeCycle, nodeID, Type.Move);
                    continue;
                }
            }

            // Calculate the right position to insert the node, and do it
            List<ID> predecessors = change.getNode().getPredecessorIDs();
            int pos = getInsertionPos(targetParent, predecessors,
                mainAdditionsAndMoves);
            targetParent.addChild(node, pos);
        }

        // No changes should remain in our list at this point. But if they do,
        // they will be add/move operations whose target parent is missing.
        // log appropriate conflicts.
        for (TreeNodeChange<ID, C> change : changesToProcess)
            addConflict(change.getParentID(), Conflict.DeleteParent,
                change.getNodeID(), change.getType());
    }

    private void extractAllAddMoveAndReorderOperations(
            List<TreeNodeChange<ID, C>> allIncomingChanges,
            List<TreeNodeChange<ID, C>> resultChanges, Set<ID> resultIDs) {
        for (Iterator i = allIncomingChanges.iterator(); i.hasNext();) {
            TreeNodeChange<ID, C> change = (TreeNodeChange<ID, C>) i.next();
            Type type = change.getType();
            if (type == Type.Add || type == Type.Move || type == Type.Reorder) {
                resultChanges.add(change);
                resultIDs.add(change.getNodeID());
                i.remove();
            }
        }
    }

    /**
     * Find a good/safe addition, move, or reorder to process.
     *
     * @param changes a list of operations to choose from
     * @param nodesInProcessing the IDs of the nodes affected by those changes
     * @return a change that can be safely applied without potentially
     *     affecting any of the other "nodes in processing."
     */
    private TreeNodeChange<ID, C> extractOneAddMoveOrReorderToProcess(
            List<TreeNodeChange<ID, C>> changes, Set<ID> nodesInProcessing) {

        TreeNodeChange<ID, C> fallback = null;

        for (Iterator i = changes.iterator(); i.hasNext();) {
            TreeNodeChange<ID, C> change = (TreeNodeChange<ID, C>) i.next();
            Type changeType = change.getType();
            if (changeType == Type.Add || changeType == Type.Move) {
                // if we are adding or moving a node, its target parent MUST be
                // present in the merged tree. (It might be missing if we need
                // to add it too, and we still haven't gotten to it yet.) If
                // the parent is missing, skip this change.
                ID parentID = change.getParentID();
                if (!merged.getIDMap().containsKey(parentID))
                    continue;
            }

            if (changeType == Type.Move
                    && ancestorsMatch(change.getNode(), nodesInProcessing)) {
                // if we are moving this node, and one of its target ancestors
                // is in the avoid list, only use it as a fallback of last
                // resort.  This is an attempt to avoid creating transient
                // tree cycles that will otherwise resolve themselves if the
                // target ancestor gets moved first.
                if (fallback == null)
                    fallback = change;

            } else if (predecessorsMatch(change.getNode(), nodesInProcessing)) {
                // if the predecessors of this node are in the avoid list,
                // record it as a fallback option with bad predecessors.  This
                // is an attempt to avoid placing tasks in the wrong order.
                fallback = change;

            } else {
                // otherwise, this node meets all of our criteria. return it.
                i.remove();
                return change;
            }
        }

        // we didn't find a change that met our idealized goals. Return the
        // best thing we could find.
        changes.remove(fallback);
        return fallback;
    }

    /** Do any of the predecessor siblings of a node appear in a set of IDs? */
    private boolean predecessorsMatch(TreeNode<ID, C> node, Set<ID> nodeIDs) {
        for (ID id : node.getPredecessorIDs())
            if (nodeIDs.contains(id))
                return true;
        return false;
    }

    /** Do any of the ancestors of a node appear in a set of IDs? */
    private boolean ancestorsMatch(TreeNode<ID, C> node, Set<ID> nodesIDs) {
        TreeNode<ID, C> ancestor = node.getParent();
        while (ancestor != null && ancestor != node) {
            if (nodesIDs.contains(ancestor.getID()))
                return true;
            ancestor = ancestor.getParent();
        }
        return false;
    }

    /** Calculate the appropriate position to insert a node underneath a
     * particular parent, based on a preferred list of predecessors.
     *
     * @param parent the parent where the node will be inserted
     * @param predecessors the preferred list of predecessors
     * @param skipOverNodes if the insertion position points at a node in this
     *     list, skip forward to the next position.
     */
    private int getInsertionPos(TreeNode<ID, C> parent, List<ID> predecessors,
            Set<ID> skipOverNodes) {
        List<TreeNode<ID, C>> children = parent.getChildren();

        int result = getIndexOfLastPredecessor(predecessors, children);

        result = result + 1;
        while (result < children.size()) {
            TreeNode<ID, C> child = children.get(result);
            if (skipOverNodes.contains(child.getID()))
                result++;
            else
                break;
        }
        return result;
    }

    private int getIndexOfLastPredecessor(List<ID> predecessors,
            List<TreeNode<ID, C>> children) {
        for (int i = predecessors.size();  i-- > 0; ) {
            int onePos = findPosOfChildWithID(children, predecessors.get(i));
            if (onePos != -1)
                return onePos;
        }
        return -1;
    }

    public int findPosOfChildWithID(List<TreeNode<ID, C>> children, ID id) {
        for (int i = 0;  i < children.size();  i++)
            if (children.get(i).getID().equals(id))
                return i;
        return -1;
    }


    /**
     * Look through the list of incoming changes, and perform the requested
     * deletions if possible.  Log conflicts if we cannot delete any nodes.
     */
    private void processDeletions(List<TreeNodeChange<ID, C>> incomingChanges) {
        List<Deletion> deletions = new ArrayList<Deletion>();
        for (Iterator i = incomingChanges.iterator(); i.hasNext();) {
            TreeNodeChange<ID, C> change = (TreeNodeChange) i.next();
            if (change.getType() == Type.Delete) {
                deletions.add(new Deletion(change.getNodeID()));
                i.remove();
            }
        }
        // process deletione, starting with the deepest nodes first.
        Collections.sort(deletions);
        for (Deletion d : deletions)
            processDeletion(d);
    }

    private void processDeletion(Deletion d) {
        if (d.nodeToDelete == null)
            // this node was deleted by both branches.  Nothing to do
            return;

        if (checkForMainChangeToIncomingDelete(d.id, Type.Edit, Type.Move))
            // this node was edited or moved by the main branch but deleted by
            // the incoming branch.  Log a conflict and abort.
            return;

        if (checkForMainChildrenOfIncomingDelete(d))
            // this node has children; we can't delete it.
            return;

        // all is well; carry out the deletion.
        d.nodeToDelete.delete();
    }

    private boolean checkForMainChangeToIncomingDelete(ID id, Type... types) {
        for (Type t : types) {
            if (mainDiff.nodeWasChanged(id, t)) {
                // this node was altered by the main branch but deleted by the
                // incoming branch.  Log a conflict.
                addConflict(id, t, Type.Delete);
                mergedUndeletedNodeIDs.add(id);
                return true;
            }
        }

        return false;
    }

    private boolean checkForMainChildrenOfIncomingDelete(Deletion deletion) {
        if (deletion.nodeToDelete.getChildren().isEmpty())
            return false;

        // we are supposed to delete a node, but it has children.  These
        // could be nodes that were "undeleted" (because they were moved or
        // edited), nodes that were added by main, or parent of any such node.

        // scan children and log "deleted parent" conflicts if appropriate
        for (TreeNode<ID, C> child : deletion.nodeToDelete.getChildren()) {
            ID childID = child.getID();
            if (incomingDiff.nodeWasChanged(childID, Type.Delete))
                // we were supposed to delete this child, and we didn't
                // because of some conlict we detected earlier.  We don't
                // need to log a conflict again.
                continue;

            Type conflictingChangeType;
            if (mainDiff.nodeWasChanged(childID, Type.Add))
                conflictingChangeType = Type.Add;
            else if (mainDiff.nodeWasChanged(childID, Type.Move))
                conflictingChangeType = Type.Move;
            else
                // unexpected
                conflictingChangeType = Type.Edit;
            addConflict(childID, conflictingChangeType, deletion.id,
                Conflict.DeleteParent);
        }

        // make a record that this node was "undeleted" due to a conflict
        mergedUndeletedNodeIDs.add(deletion.id);
        return true;
    }

    private class Deletion implements Comparable<Deletion> {
        ID id;
        TreeNode<ID, C> nodeToDelete;
        int depth;
        private Deletion(ID id) {
            this.id = id;
            this.nodeToDelete = merged.findNode(id);
            if (nodeToDelete != null)
                this.depth = nodeToDelete.getDepth();
        }
        public int compareTo(Deletion that) {
            return that.depth - this.depth;
        }
    }

    /**
     * Convenience method to add a conflict to the list of merge warnings.
     *
     * @param id the ID of a node with a conflict
     * @param mainChangeType the change made to the node in the main branch
     * @param incomingChangeType the change made to the node in the incoming
     *     branch
     */
    private void addConflict(ID id, Type mainChangeType, Type incomingChangeType) {
        mergeWarnings.add(new MergeWarning<ID>(Severity.CONFLICT, id,
                mainChangeType, incomingChangeType));
    }

    /**
     * Convenience method to add a conflict to the list of merge warnings.
     *
     * @param id the ID of a node with a conflict
     * @param mainChangeType the change made to the node in the main branch
     * @param incomingChangeType the change made to the node in the incoming
     *     branch
     */
    private void addConflict(ID mainID, Object mainChangeType, ID incomingID,
            Object incomingChangeType) {
        mergeWarnings.add(new MergeWarning<ID>(Severity.CONFLICT, mainID,
                mainChangeType, incomingID, incomingChangeType));
    }

    private class EditMergeConflictLogger implements ContentMerger.ErrorReporter<ID> {
        public void addMergeWarning(MergeWarning<ID> w) {
            mergeWarnings.add(w);
        }
    }
    private EditMergeConflictLogger editMergeConflictLogger = new EditMergeConflictLogger();
}
