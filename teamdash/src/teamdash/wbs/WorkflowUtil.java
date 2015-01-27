// Copyright (C) 2002-2015 Tuma Solutions, LLC
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


package teamdash.wbs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import net.sourceforge.processdash.util.PatternList;
import net.sourceforge.processdash.util.StringUtils;

import teamdash.wbs.columns.AbstractNotesColumn;
import teamdash.wbs.columns.NotesColumn;
import teamdash.wbs.columns.TaskLabelColumn;
import teamdash.wbs.columns.TaskSizeUnitsColumn;
import teamdash.wbs.columns.TeamActualTimeColumn;
import teamdash.wbs.columns.WorkflowLabelColumn;
import teamdash.wbs.columns.WorkflowNotesColumn;

public class WorkflowUtil {

    /**
     * Modify a WBS by applying the contents of a workflow to a particular row.
     * 
     * @param destWbs
     *            the WBS to modify
     * @param destNode
     *            the node where the workflow should be applied
     * @param workflowName
     *            the name of the workflow to apply
     * @param workflows
     *            the model containing workflow definitions
     * @param attrsToKeep
     *            a list of WBS node attributes that are valid for the newly
     *            created nodes
     * @param extraDefaultAttrs
     *            a list of attribute values that should be added to the newly
     *            created nodes
     * @param expandTargetNode true if the target node should be expanded, to
     *            ensure the display of the newly inserted tasks
     * @return a list of the nodes that were inserted into the WBS; can be null
     *         or empty
     */
    public static List<WBSNode> insertWorkflow(WBSModel destWbs,
            WBSNode destNode, String workflowName, WBSModel workflows,
            PatternList attrsToKeep, Map extraDefaultAttrs,
            boolean expandTargetNode) {
        // check the destination node for insertion.
        if (destNode == null) return null;

        // locate the workflow to be inserted.
        WBSNode[] workflowItems = workflows.getChildren(workflows.getRoot());
        WBSNode srcNode = null;
        for (int i = 0;   i < workflowItems.length;   i++)
            if (workflowName.equals(workflowItems[i].getName())) {
                srcNode = workflowItems[i];
                break;
            }
        if (srcNode == null) return null;

        // alter the notes and labels on the destNode if necessary
        String destLabels = appendWorkflowNotesAndLabels(srcNode,
            destNode);

        // gather data structures we will need for the operation
        List<WBSNode> currentDestNodes = Arrays.asList(destWbs
                .getDescendants(destNode));
        Set<Integer> srcWorkflowIDs = new HashSet();
        for (WBSNode n : workflows.getDescendants(srcNode))
            srcWorkflowIDs.add(n.getUniqueID());
        int destPos = getWorkflowInsertionPos(destWbs, destNode,
            currentDestNodes, srcWorkflowIDs);
        AtomicBoolean sawProbe = new AtomicBoolean();

        // perform the workflow apply operation
        applyWorkflow(destWbs, destNode, destPos, currentDestNodes, destLabels,
            workflows, srcNode, srcWorkflowIDs, attrsToKeep, extraDefaultAttrs,
            sawProbe);

        // update the workflow source ID of the destination node.
        addWorkflowSourceID(destNode, srcNode.getUniqueID());

        // delete extraneous nodes that were created by a past workflow
        // application, but whose source workflow step no longer exists
        purgeDeletedWorkflowSteps(destWbs, destNode, workflows);

        // make the inserted nodes visible if requested.
        if (expandTargetNode)
            destNode.setExpanded(true);
        // recalculate the rows and the data in the WBS
        destWbs.recalcRows(false, false);
        destWbs.fireTableDataChanged();

        // make a list of the nodes that are now in place as a result of the
        // application of this workflow, and return it
        List<WBSNode> finalDestNodes = new ArrayList();
        for (WBSNode n : destWbs.getDescendants(destNode))
            if (hasWorkflowSourceID(n, srcWorkflowIDs))
                finalDestNodes.add(n);
        return finalDestNodes;
    }

    private static int getWorkflowInsertionPos(WBSModel destWbs,
            WBSNode destNode, List<WBSNode> currentDestNodes,
            Set<Integer> srcWorkflowIDs) {
        // if this workflow has ever been applied to this node in the past,
        // start our insertion operation at its current location.
        for (WBSNode n : currentDestNodes)
            if (hasWorkflowSourceID(n, srcWorkflowIDs))
                return destWbs.getIndexOfNode(n);

        // if this workflow has never been applied here before, append the
        // new workflow tasks to the end of the current dest node.
        return destWbs.getIndexOfNode(destNode) + currentDestNodes.size() + 1;
    }

    private static void applyWorkflow(WBSModel destWbs, WBSNode destNode,
            int destPos, List<WBSNode> currentDestNodes, String destLabels,
            WBSModel workflows, WBSNode srcNode, Set<Integer> srcWorkflowIDs,
            PatternList attrsToKeep, Map extraDefaultAttrs,
            AtomicBoolean sawProbe) {

        Map<WBSNode, WBSNode> nodesWithSrcChildren = new LinkedHashMap();
        if (destPos == -1)
            destPos = destWbs.getIndexOfNode(destNode) + 1;
        int targetIndent = destNode.getIndentLevel() + 1;

        // iterate over the immediate children of the workflow source node.
        for (WBSNode srcChild : workflows.getChildren(srcNode)) {

            // if a workflow contains more than one PROBE task, the second one
            // will be flagged in the Workflow editor as an error.  Ignore the
            // extra PROBE task and don't instantiate it.
            if (TeamProcess.isProbeTask(srcChild.getType())
                    && sawProbe.getAndSet(true))
                continue;
            WBSNode destChild = null;

            // see if there is an existing set of nodes we should use to
            // represent this workflow source node
            WBSNode nodeToMove = getSrcNodeToMoveForWorkflowNode(
                destWbs, destNode, currentDestNodes, workflows, srcChild,
                srcWorkflowIDs);
            if (nodeToMove != null) {
                int moveNodeCount = (Integer) nodeToMove
                        .removeAttribute(MOVE_NODE_COUNT_ATTR);
                int currPos = destWbs.getIndexOfNode(nodeToMove);
                if (currPos == destPos) {
                    // the existing nodes are already in the correct place (a
                    // common scenario for reapplying a workflow that hasn't
                    // changed much). No alterations are needed.
                } else {
                    // adjust the indentation of the moved nodes if necessary
                    int delta = targetIndent - nodeToMove.getIndentLevel();
                    if (delta != 0) {
                        for (int i = currPos + moveNodeCount; i-- > currPos; ) {
                            WBSNode n = destWbs.getNodeForPos(i);
                            n.setIndentLevel(n.getIndentLevel() + delta);
                        }
                    }
                    // move the nodes to their new location in the WBS
                    destWbs.moveNodesImpl(currPos, moveNodeCount, destPos);
                }
                destChild = nodeToMove;
                if (currPos >= destPos)
                    destPos += moveNodeCount;

            } else {
                // if we don't find a node to move, create a new one and
                // insert it.
                WBSNode newNode = createNewWorkflowNodeToInsert(srcChild,
                    destLabels, attrsToKeep, extraDefaultAttrs);
                newNode.setIndentLevel(targetIndent);
                destWbs.insertNodesAtImpl(Collections.singletonList(newNode),
                    destPos);

                // possibly set extra default attrs that were requested
                newNode.setAttributes(extraDefaultAttrs, false);

                destChild = newNode;
                destPos += 1;
            }

            if (!workflows.isLeaf(srcChild))
                nodesWithSrcChildren.put(srcChild, destChild);
        }

        // recurse for workflow source nodes that have children
        for (Entry<WBSNode, WBSNode> e : nodesWithSrcChildren.entrySet()) {
            WBSNode srcChild = e.getKey();
            WBSNode destChild = e.getValue();
            applyWorkflow(destWbs, destChild, -1, currentDestNodes, destLabels,
                workflows, srcChild, srcWorkflowIDs, attrsToKeep,
                extraDefaultAttrs, sawProbe);
        }
    }

    private static WBSNode getSrcNodeToMoveForWorkflowNode(
            WBSModel destWbs, WBSNode destParent,
            List<WBSNode> currentDestNodes, WBSModel workflows,
            WBSNode srcNode, Set<Integer> srcWorkflowIDs) {

        // see if the WBS contains an existing node that we should use to
        // represent the source workflow node.
        WBSNode nodeToMove = findWbsNodeForWorkflowNode(destWbs, destParent,
            currentDestNodes, workflows, srcNode, srcWorkflowIDs);
        if (nodeToMove == null)
            return null;

        // make certain the name & type match the workflow step
        nodeToMove.setName(srcNode.getName());
        nodeToMove.setType(srcNode.getType());

        // make a count of the number of nodes we need to move. To begin, we
        // must move the node in question and all its descendants.
        int moveNodeCount = 1 + destWbs.getDescendants(nodeToMove).length;

        // check the sibling nodes that immediately follow this node in the
        // WBS, and possibly move them too, if:
        // (a) they are duplicated workflow steps
        // (b) they are ad-hoc tasks, inserted between our workflow steps
        String workflowSourceID = Integer.toString(srcNode.getUniqueID());
        List<WBSNode> siblings = Arrays.asList(destWbs.getChildren(destWbs
                .getParent(nodeToMove)));
        int pos = siblings.indexOf(nodeToMove) + 1;

        // we only want to move ad-hoc nodes that were *inserted* between our
        // workflow steps. Find the last sibling that's a part of our workflow,
        // and only look that far when choosing nodes to move.
        int maxPos = siblings.size();
        while (maxPos-- > pos) {
            WBSNode sibling = siblings.get(maxPos);
            if (hasWorkflowSourceID(sibling, srcWorkflowIDs))
                break;
        }

        // look over the siblings in question and decide which should be moved.
        while (pos > 0 && pos <= maxPos) {
            WBSNode sibling = siblings.get(pos++);
            List<String> siblingSrcIDs = getWorkflowSourceIDs(sibling);
            if (siblingSrcIDs.contains(workflowSourceID) // duplicated step
                    || siblingSrcIDs.isEmpty()) { // inserted ad-hoc item
                moveNodeCount += 1 + destWbs.getDescendants(sibling).length;
            } else {
                break;
            }
        }

        // store the count of moved items on our result node, and return it
        nodeToMove.setAttribute(MOVE_NODE_COUNT_ATTR, moveNodeCount);
        return nodeToMove;
    }
    private static final String MOVE_NODE_COUNT_ATTR = "_Move_Node_Count";

    private static WBSNode findWbsNodeForWorkflowNode(WBSModel destWbs,
            WBSNode destParent, List<WBSNode> currentDestNodes,
            WBSModel workflows, WBSNode srcNode, Set<Integer> srcWorkflowIDs) {

        // see if any of the current WBS nodes have the workflow source ID
        // matching the node we are looking for
        String workflowSourceID = Integer.toString(srcNode.getUniqueID());
        for (WBSNode node : currentDestNodes) {
            if (hasWorkflowSourceID(node, workflowSourceID)) {
                appendWorkflowNotesAndLabels(srcNode, node);
                return node;
            }
        }

        // no existing node was found with the given workflow source ID. Look
        // for an alternative WBS node that is a potential match
        for (WBSNode altNode : destWbs.getChildren(destParent)) {
            // look for a node that has the same name as the workflow step.
            if (!altNode.getName().equals(srcNode.getName()))
                continue;

            // don't repurpose any node that was produced by this workflow in
            // the past. This "apply workflow" operation will find it by its
            // ID and use it appropriately
            if (hasWorkflowSourceID(altNode, srcWorkflowIDs))
                continue;

            // update notes and labels for this node if needed
            appendWorkflowNotesAndLabels(srcNode, altNode);

            // Update the workflow source IDs of this node so it belongs to
            // this workflow
            StringBuilder newSourceIDs = new StringBuilder();
            newSourceIDs.append(workflowSourceID);
            for (String oldIdStr : getWorkflowSourceIDs(altNode)) {
                // if a nested workflow was applied to this node in the past,
                // retain the source ID of that nested workflow.
                Integer oldId = Integer.valueOf(oldIdStr);
                WBSNode oldSrcNode = workflows.getNodeMap().get(oldId);
                if (oldSrcNode != null && oldSrcNode.getIndentLevel() == 1)
                    newSourceIDs.append(",").append(oldIdStr);
            }
            altNode.setAttribute(WFLOW_SRC_IDS, newSourceIDs.toString());
            return altNode;
        }

        // no node was found to move.
        return null;
    }

    private static void addWorkflowSourceID(WBSNode node, int sourceID) {
        String oldIDs = (String) node.getAttribute(WFLOW_SRC_IDS);
        String newIDs = Integer.toString(sourceID);
        if (oldIDs != null && oldIDs.length() > 0) {
            List<String> oldIdList = Arrays.asList(oldIDs.split(","));
            if (oldIdList.contains(newIDs))
                newIDs = oldIDs;
            else
                newIDs = oldIDs + "," + newIDs;
        }
        node.setAttribute(WFLOW_SRC_IDS, newIDs);
    }

    private static String appendWorkflowNotesAndLabels(WBSNode srcNode,
            WBSNode destNode) {

        // if a workflow note is present on the srcNode, append it to the
        // WBS note on the destNode. Don't append if the destNode was
        // previously created by this workflow step, and it already has a note
        String srcNote = (String) srcNode
                .getAttribute(WorkflowNotesColumn.VALUE_ATTR);
        String destNote = (String) destNode
                .getAttribute(WorkflowNotesColumn.VALUE_ATTR);
        if (srcNote != null)
            if (destNote == null || !hasWorkflowSourceID(srcNode, destNode))
                NotesColumn.appendNote(destNode, srcNote);

        // determine what labels are currently in effect on the dest nodes.
        String destLabels = TaskLabelColumn.getEffectiveLabelsAt(destNode);

        // if workflow labels are present on the srcNode, compute the effective
        // set of labels that should be present on the destNode.
        String srcLabels = (String) srcNode
                .getAttribute(WorkflowLabelColumn.VALUE_ATTR);
        if (srcLabels != null && srcLabels.trim().length() > 0) {
            String merged = TaskLabelColumn.mergeLabels(srcLabels, destLabels);
            if (!merged.equals(destLabels))
                destNode.setAttribute(TaskLabelColumn.VALUE_ATTR, merged);
            if (destLabels != null)
                destLabels = TaskLabelColumn.subtractLabels(merged, srcLabels);
        }

        return destLabels;
    }

    private static WBSNode createNewWorkflowNodeToInsert(WBSNode srcNode,
            String destLabels, PatternList attrsToKeep, Map extraDefaultAttrs) {

        // make a copy of the workflow source node.
        WBSNode node = (WBSNode) srcNode.clone();

        // if any workflow notes were set on the workflow item, transfer them
        // to the WBS notes attribute.  If not, clear the WBS note (since it
        // could contain garbage from an earlier copy/paste operation)
        node.setAttribute(NotesColumn.VALUE_ATTR,
            node.removeAttribute(WorkflowNotesColumn.VALUE_ATTR));
        for (String attr : AbstractNotesColumn
                .getMetadataAttrs(WorkflowNotesColumn.VALUE_ATTR)) {
            String destAttr = StringUtils.findAndReplace(attr,
                WorkflowNotesColumn.VALUE_ATTR, NotesColumn.VALUE_ATTR);
            node.setAttribute(destAttr, node.removeAttribute(attr));
        }

        // determine what labels should be set on the new workflow item.
        String mergedLabels;
        String srcLabels = (String) node
                .removeAttribute(WorkflowLabelColumn.VALUE_ATTR);
        if (srcLabels == null) {
            // if the workflow labels are "null", this means to inherit from
            // the parent node.  Set our merged labels to null as well, so
            // we will inherit from the WBS parent.  (If the workflow node
            // contained extraneous WBS labels, left over from copying and
            // pasting a WBS node into the workflows window, this will clear
            // that garbage away too.)
            mergedLabels = null;
        } else {
            // if labels were set on the workflow node, calculate the merged
            // labels that should be applied to the newly inserted WBS item.
            mergedLabels = TaskLabelColumn.mergeLabels(srcLabels, destLabels);
        }
        node.setAttribute(TaskLabelColumn.VALUE_ATTR, mergedLabels);

        // possibly clear extraneous attributes that are undesirable to keep.
        if (attrsToKeep != null)
            node.discardAttributesExcept(attrsToKeep);

        // record the ID of the workflow elements that produced this node
        node.setAttribute(WFLOW_SRC_IDS, Integer.toString(node.getUniqueID()));

        return node;
    }

    private static void purgeDeletedWorkflowSteps(WBSModel destWbs,
            WBSNode destNode, WBSModel workflows) {
        Set<Integer> allKnownWorkflowIDs = workflows.getNodeMap().keySet();
        List nodesToDelete = null;
        // look at each of the descendants of the dest node
        for (WBSNode node : destWbs.getDescendants(destNode)) {
            // if this node has no workflow source IDs, it is a node that was
            // manually created by the user. Leave it alone.
            List<String> ids = getWorkflowSourceIDs(node);
            if (ids.isEmpty())
                continue;

            // at this point, we know the node in question was (probably)
            // created in the past by the application of a workflow. if the
            // given workflow step still exists, keep the node too.
            if (hasWorkflowSourceID(ids, allKnownWorkflowIDs))
                continue;

            // we've found a node that was created by a workflow, but the source
            // workflow step no longer exists. Check to see if it has any actual
            // data before we delete it.
            if (TeamActualTimeColumn.hasActualTime(node))
                continue;

            // delete this obsolete workflow step.
            if (nodesToDelete == null)
                nodesToDelete = new ArrayList();
            nodesToDelete.add(node);
        }
        if (nodesToDelete != null)
            destWbs.deleteNodesImpl(nodesToDelete);
    }

    private static boolean hasWorkflowSourceID(WBSNode node,
            Collection<Integer> ids) {
        return hasWorkflowSourceID(getWorkflowSourceIDs(node), ids);
    }

    private static boolean hasWorkflowSourceID(Collection<String> a,
            Collection<Integer> b) {
        for (String oneID : a)
            if (b.contains(Integer.valueOf(oneID)))
                return true;
        return false;
    }

    private static boolean hasWorkflowSourceID(WBSNode srcNode, WBSNode destNode) {
        return hasWorkflowSourceID(destNode,
            Integer.toString(srcNode.getUniqueID()));
    }

    private static boolean hasWorkflowSourceID(WBSNode node, String id) {
        return getWorkflowSourceIDs(node).contains(id);
    }

    private static List<String> getWorkflowSourceIDs(WBSNode node) {
        String attr = (String) node.getAttribute(WFLOW_SRC_IDS);
        if (attr == null || attr.length() == 0)
            return Collections.EMPTY_LIST;
        else if (attr.indexOf(',') == -1)
            return Collections.singletonList(attr);
        else
            return Arrays.asList(attr.split(","));
    }


    /**
     * Scan a WBS for all of the workflow enactments that appear in certain
     * branches of the tree
     * 
     * @param wbs
     *            the WBS to scan
     * @param nodes
     *            a set of nodes of interest in that WBS. These nodes, and their
     *            descendants, will be scanned for workflow enactments. The
     *            nodes MUST be presented in WBS-order.
     * @param workflows
     *            the set of defined workflows in this project
     * @return a map whose keys are workflow names, and whose values are
     *         collections of nodes that happen to be roots where the given
     *         workflow was applied.
     */
    public static Map<String, Set<WBSNode>> getWorkflowEnactmentRoots(
            final WBSModel wbs, List<WBSNode> nodes, WorkflowWBSModel workflows) {
        // gather up a set of all of the distinct nodes we should examine
        HashSet<WBSNode> nodesToCheck = new LinkedHashSet<WBSNode>();
        for (WBSNode node : nodes) {
            nodesToCheck.add(node);
            nodesToCheck.addAll(Arrays.asList(wbs.getDescendants(node)));
        }

        // find workflow enactment roots in the given node collection
        Map<String, Set<WBSNode>> result = new TreeMap<String, Set<WBSNode>>();
        Map<Integer, WBSNode> workflowMap = workflows.getNodeMap();
        for (WBSNode node : nodesToCheck) {
            for (String srcId : getWorkflowSourceIDs(node)) {
                WBSNode srcNode = workflowMap.get(Integer.valueOf(srcId));
                if (srcNode != null && srcNode.getIndentLevel() == 2) {
                    WBSNode targetNode = wbs.getParent(node);
                    WBSNode srcWorkflow = workflows.getParent(srcNode);
                    String workflowName = srcWorkflow.getName();
                    Set<WBSNode> enactments = result.get(workflowName);
                    if (enactments == null) {
                        enactments = new LinkedHashSet();
                        enactments.add(targetNode);
                        result.put(workflowName, enactments);
                    } else if (!containsAncestor(wbs, targetNode, enactments)) {
                        enactments.add(targetNode);
                    }
                }
            }
        }
        return result;
    }

    private static boolean containsAncestor(WBSModel wbs, WBSNode node,
            Set<WBSNode> targetSet) {
        while (true) {
            if (node == null)
                return false;
            else if (targetSet.contains(node))
                return true;
            else
                node = wbs.getParent(node);
        }
    }


    /**
     * Alter the "workflow source ID" attributes in a given WBS, replacing IDs
     * as described by a particular map
     * 
     * @param wbsModel
     *            the WBS model to modify
     * @param idMap
     *            a list of workflow source IDs that should be changed. The keys
     *            of this map are old workflow source IDs, and the values are
     *            the desired replacements.
     */
    public static void remapWorkflowSourceIDs(WBSModel wbsModel,
            Map<Integer, Integer> idMap) {

        if (idMap == null || idMap.isEmpty())
            return;

        for (WBSNode node : wbsModel.getWbsNodes()) {
            String ids = (String) node.getAttribute(WFLOW_SRC_IDS);
            if (ids != null && ids.length() > 0) {
                boolean madeChange = false;
                String[] list = ids.split(",");
                for (int i = 0; i < list.length; i++) {
                    Integer oneID = Integer.parseInt(list[i]);
                    Integer newID = idMap.get(oneID);
                    if (newID != null) {
                        list[i] = Integer.toString(newID);
                        madeChange = true;
                    }
                }
                if (madeChange) {
                    String newVal = StringUtils.join(Arrays.asList(list), ",");
                    node.setAttribute(WFLOW_SRC_IDS, newVal);
                }
            }
        }
    }


    /**
     * Update WBS workflow data if it needs any attention or cleanup.
     */
    public static void maybeUpdateWorkflowTypeData(WBSModel wbs,
            WorkflowWBSModel workflows) {
        WBSNode root = wbs.getRoot();
        if (root.getAttribute(WORKFLOW_IDS_SCRUBBED_FLAG) == null) {
            // perform a one-time cleanup of workflow source IDs
            scrubWorkflowSourceIDs(wbs, workflows, root, null);
            root.setAttribute(WORKFLOW_IDS_SCRUBBED_FLAG, "true");
        }
    }

    private static void scrubWorkflowSourceIDs(WBSModel wbs,
            WorkflowWBSModel workflows, WBSNode parent,
            WBSNode parentWorkflowNode) {

        // Look at each of the child nodes of the given parent.
        for (WBSNode child : wbs.getChildren(parent)) {
            // See if this child node came from the instantiation of a workflow
            WBSNode childWorkflowNode = getWorkflowSourceNode(child, workflows);

            if (childWorkflowNode != null) {

                if (childWorkflowNode.getIndentLevel() < 2) {
                    // if the "workflow equivalent" of this node is a workflow
                    // parent root rather than a workflow step, ignore it.
                    childWorkflowNode = null;

                } else if (!child.typeMatches(childWorkflowNode)) {
                    // if the MCF type of this node doesn't match the MCF
                    // type of the workflow source node, break the association.
                    Object wfSrcID = child.getAttribute(WFLOW_SRC_IDS);
                    child.setAttribute(WFLOW_SRC_IDS + "-broken", wfSrcID);
                    child.setAttribute(WFLOW_SRC_IDS, null);
                    childWorkflowNode = null;
                }

            } else if (child.typeMatches(parentWorkflowNode)
                    && child.getAttribute(WFLOW_SRC_IDS) == null) {
                // if this node is a child of a workflow step, and its MCF type
                // matches the MCF type of the step, inherit the workflow type
                // from that parent.
                child.setAttribute(WFLOW_SRC_IDS,
                    Integer.toString(parentWorkflowNode.getUniqueID()));
                childWorkflowNode = parentWorkflowNode;
            }

            // recurse over descendants of this child.
            scrubWorkflowSourceIDs(wbs, workflows, child, childWorkflowNode);
        }
    }


    public static String getTypeViaWorkflow(WBSNode node,
            WorkflowWBSModel workflows, boolean fallback) {
        String result = null;
        if (workflows != null) {
            WBSNode workflowNode = getWorkflowSourceNode(node, workflows);
            if (workflowNode != null && workflowNode.getIndentLevel() > 1)
                result = workflows.filterNodeType(workflowNode);
        }
        if (result == null && fallback)
            result = node.getWbsModel().filterNodeType(node);
        return result;
    }

    public static String getWorkflowTaskType(WBSNode node,
            WorkflowWBSModel workflows) {
        if (node == null || workflows == null)
            return null;
        WBSNode workflowNode = getWorkflowSourceNode(node, workflows);
        return workflows.getWorkflowTaskType(workflowNode);
    }

    public static String getWorkflowStepName(WBSNode node,
            WorkflowWBSModel workflows, boolean longName) {
        if (node == null || workflows == null)
            return null;
        WBSNode workflowNode = getWorkflowSourceNode(node, workflows);
        return workflows.getStepFullName(workflowNode, longName);
    }

    public static String getWorkflowSizeUnits(WBSNode node,
            WorkflowWBSModel workflows, TeamProcess teamProcess) {
        if (node == null || workflows == null)
            return null;
        WBSNode workflowNode = getWorkflowSourceNode(node, workflows);
        return TaskSizeUnitsColumn.getSizeUnitsForTask(workflowNode,
            teamProcess);
    }

    private static WBSNode getWorkflowSourceNode(WBSNode node,
            WorkflowWBSModel workflows) {
        Integer workflowSourceId;
        try {
            // See if this node has a workflow source attribute.
            String attr = getPrimaryWorkflowSrcID(node);
            if (attr == null)
                return null;

            // parse the workflow source ID as an integer.
            workflowSourceId = Integer.parseInt(attr);
        } catch (Exception e) {
            return null;
        }

        // find the workflow node with the given ID.
        WBSNode result = workflows.getWorkflowNodeMap().get(workflowSourceId);

        // compare the types of the original node and the workflow node to
        // see if they are quasi-compatible.
        if (nodeTypesAreCompatible(node, result))
            return result;
        else
            return null;
    }

    private static boolean nodeTypesAreCompatible(WBSNode a, WBSNode b) {
        if (a == null || b == null)
            return false;
        String typeA = a.getType();
        String typeB = b.getType();
        if (typeA.equals(typeB))
            return true;
        else if (typeA.endsWith(TeamProcess.TASK_SUFFIX)
                && typeB.endsWith(TeamProcess.TASK_SUFFIX))
            return true;
        else
            return false;
    }

    public static String getPrimaryWorkflowSrcID(WBSNode node) {
        // See if this node has a workflow source attribute.
        String workflowSourceIDs = (String) node.getAttribute(WFLOW_SRC_IDS);
        if (workflowSourceIDs == null)
            return null;

        // if this node has a list of workflow source IDs, the first one
        // is the primary ID. (Subsequent IDs would indicate nested workflows
        // that had been applied to the node.)
        int commaPos = workflowSourceIDs.indexOf(',');
        if (commaPos == -1)
            return workflowSourceIDs;
        else
            return workflowSourceIDs.substring(0, commaPos);
    }


    private static final String WFLOW_SRC_IDS = //
            WorkflowModel.WORKFLOW_SOURCE_IDS_ATTR;

    private static final String WORKFLOW_IDS_SCRUBBED_FLAG = //
            "workflowIDsScrubbed";

}
