// Copyright (C) 2002-2013 Tuma Solutions, LLC
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

import net.sourceforge.processdash.util.PatternList;
import net.sourceforge.processdash.util.StringUtils;

import teamdash.wbs.columns.NotesColumn;
import teamdash.wbs.columns.TaskLabelColumn;
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
                node.discardAttributesExcept(attrsToKeep,
                    WORKFLOW_SOURCE_IDS_ATTR);
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
        String oldIDs = (String) node.getAttribute(WORKFLOW_SOURCE_IDS_ATTR);
        String newIDs = Integer.toString(sourceID);
        if (oldIDs != null && oldIDs.length() > 0) {
            List<String> oldIdList = Arrays.asList(oldIDs.split(","));
            if (oldIdList.contains(newIDs))
                newIDs = oldIDs;
            else
                newIDs = oldIDs + "," + newIDs;
        }
        node.setAttribute(WORKFLOW_SOURCE_IDS_ATTR, newIDs);
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
        WBSNode[] srcChildren = workflows.getChildren(srcNode);
        for (int i = 0;   i < srcChildren.length;   i++) {
            WBSNode srcChild = srcChildren[i];
            // we don't want to clobber any nodes that already exist in
            // the destination, so we'll skip any children whose names
            // already appear underneath destNode
            if (destChildNames.contains(srcChild.getName())) continue;

            // add the child to our insertion list.
            appendWorkflowNode(nodesToInsert, srcChild, indentDelta, destLabels);
            // add all the descendants of the child to our insertion list.
            WBSNode[] srcDescendants = workflows.getDescendants(srcChild);
            for (int j = 0;   j < srcDescendants.length;   j++)
                appendWorkflowNode(nodesToInsert, srcDescendants[j],
                                   indentDelta, destLabels);
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
            int indentDelta, String destLabels) {
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
        node.setAttribute(WORKFLOW_SOURCE_IDS_ATTR,
            Integer.toString(node.getUniqueID()));

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
            String ids = (String) node.getAttribute(WORKFLOW_SOURCE_IDS_ATTR);
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
                    node.setAttribute(WORKFLOW_SOURCE_IDS_ATTR, newVal);
                }
            }
        }
    }

    private static final String WORKFLOW_SOURCE_IDS_ATTR = //
            WorkflowModel.WORKFLOW_SOURCE_IDS_ATTR;

}
