// Copyright (C) 2002-2014 Tuma Solutions, LLC
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import net.sourceforge.processdash.util.PatternList;
import net.sourceforge.processdash.util.StringUtils;

import teamdash.wbs.columns.NotesColumn;
import teamdash.wbs.columns.TaskLabelColumn;
import teamdash.wbs.columns.TaskSizeUnitsColumn;
import teamdash.wbs.columns.WorkflowLabelColumn;
import teamdash.wbs.columns.WorkflowNotesColumn;

public class WorkflowUtil {

    /**
     * Modify a WBS by applying the contents of a workflow to a particular row.
     * 
     * @param destWbs
     *            the WBS to modify
     * @param destRow
     *            the row where the workflow should be applied
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
     * @return a list of the nodes that were inserted into the WBS; can be null
     *         or empty
     */
    public static List<WBSNode> insertWorkflow(WBSModel destWbs, int destRow,
            String workflowName, WBSModel workflows, PatternList attrsToKeep,
            Map extraDefaultAttrs) {
        // locate the destination node for insertion.
        WBSNode destNode = destWbs.getNodeForRow(destRow);
        if (destNode == null) return null;
        int destPos = destWbs.getWbsNodes().indexOf(destNode);
        if (destPos == -1) return null;

        // locate the workflow to be inserted.
        WBSNode[] workflowItems = workflows.getChildren(workflows.getRoot());
        WBSNode srcNode = null;
        for (int i = 0;   i < workflowItems.length;   i++)
            if (workflowName.equals(workflowItems[i].getName())) {
                srcNode = workflowItems[i];
                break;
            }
        if (srcNode == null) return null;

        // update the workflow source ID of the destination node.
        addWorkflowSourceID(destNode, srcNode.getUniqueID());

        // calculate the list of nodes to insert.
        List<WBSNode> nodesToInsert =
            calcInsertWorkflow(srcNode, destNode, destWbs, workflows);
        if (nodesToInsert == null || nodesToInsert.isEmpty()) return null;

        // possibly clear extraneous attributes that are undesirable to keep.
        if (attrsToKeep != null) {
            for (WBSNode node : nodesToInsert)
                node.discardAttributesExcept(attrsToKeep, WFLOW_SRC_IDS);
        }

        // insert the nodes after the last descendant of the dest node.
        IntList destDescendants = destWbs.getDescendantIndexes(destNode,
            destPos);
        int insertAfter = destPos;
        if (destDescendants != null && destDescendants.size() > 0)
            insertAfter = destDescendants.get(destDescendants.size() - 1);
        destWbs.insertNodesAtImpl(nodesToInsert, insertAfter + 1);

        // possibly set extra default attrs that were requested
        if (extraDefaultAttrs != null && !extraDefaultAttrs.isEmpty()) {
            for (Iterator i = extraDefaultAttrs.entrySet().iterator(); i
                    .hasNext();) {
                Map.Entry e = (Map.Entry) i.next();
                String attr = (String) e.getKey();
                Object value = e.getValue();
                for (WBSNode node : nodesToInsert) {
                    if (node.getAttribute(attr) == null)
                        node.setAttribute(attr, value);
                }
            }
        }

        // make certain some of the inserted nodes are visible.
        destNode.setExpanded(true);
        destWbs.recalcRows();
        return nodesToInsert;
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

    private static ArrayList calcInsertWorkflow(WBSNode srcNode,
            WBSNode destNode, WBSModel destWbs, WBSModel workflows) {
        ArrayList nodesToInsert = new ArrayList();

        // calculate the difference in indentation level out
        int srcIndentation = srcNode.getIndentLevel();
        int destIndentation = destNode.getIndentLevel();
        int indentDelta = destIndentation - srcIndentation;

        // alter the notes and labels on the destNode if necessary
        String destLabels = appendNotesAndLabelsForWorkflowRoot(srcNode,
            destNode);

        // make a list of the names of the children of destNode.
        WBSNode[] destChildren = destWbs.getChildren(destNode);
        Set destChildNames = new HashSet();
        for (int i = 0;   i < destChildren.length;   i++)
            destChildNames.add(destChildren[i].getName());

        // iterate over each child of srcNode.
        AtomicBoolean sawProbe = new AtomicBoolean();
        WBSNode[] srcChildren = workflows.getChildren(srcNode);
        for (int i = 0;   i < srcChildren.length;   i++) {
            WBSNode srcChild = srcChildren[i];
            // we don't want to clobber any nodes that already exist in
            // the destination, so we'll skip any children whose names
            // already appear underneath destNode
            if (destChildNames.contains(srcChild.getName())) continue;

            // add the child to our insertion list.
            appendWorkflowNode(nodesToInsert, srcChild, indentDelta,
                destLabels, sawProbe);
            // add all the descendants of the child to our insertion list.
            WBSNode[] srcDescendants = workflows.getDescendants(srcChild);
            for (int j = 0;   j < srcDescendants.length;   j++)
                appendWorkflowNode(nodesToInsert, srcDescendants[j],
                    indentDelta, destLabels, sawProbe);
        }

        return nodesToInsert;
    }

    private static String appendNotesAndLabelsForWorkflowRoot(WBSNode srcNode,
            WBSNode destNode) {
        // if a workflow note is present on the srcNode, append it to the
        // WBS note on the destNode.
        String srcNote = (String) srcNode
                .getAttribute(WorkflowNotesColumn.VALUE_ATTR);
        if (srcNote != null)
            NotesColumn.appendNote(destNode, srcNote);

        // determine what labels are currently in effect on the dest nodes.
        String destLabels = TaskLabelColumn.getEffectiveLabelsAt(destNode);

        // if workflow labels are present on the srcNode, compute the effective
        // set of labels that should be present on the destNode.
        String srcLabels = (String) srcNode
                .getAttribute(WorkflowLabelColumn.VALUE_ATTR);
        if (srcLabels != null && srcLabels.trim().length() > 0)
            destNode.setAttribute(TaskLabelColumn.VALUE_ATTR,
                TaskLabelColumn.mergeLabels(srcLabels, destLabels));

        return destLabels;
    }

    private static void appendWorkflowNode(List dest, WBSNode node,
            int indentDelta, String destLabels, AtomicBoolean sawProbe) {
        if (TeamProcess.isProbeTask(node.getType()) && sawProbe.getAndSet(true))
            return;

        node = (WBSNode) node.clone();
        node.setIndentLevel(node.getIndentLevel() + indentDelta);

        // if any workflow notes were set on the workflow item, transfer them
        // to the WBS notes attribute.  If not, clear the WBS note (since it
        // could contain garbage from an earlier copy/paste operation)
        node.setAttribute(NotesColumn.VALUE_ATTR,
            node.removeAttribute(WorkflowNotesColumn.VALUE_ATTR));

        // determine what labels should be set on the newly appended
        // workflow item.
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

        // record the ID of the workflow elements that produced this node
        node.setAttribute(WFLOW_SRC_IDS, Integer.toString(node.getUniqueID()));

        dest.add(node);
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
