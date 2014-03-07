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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.event.TableModelEvent;

import org.w3c.dom.Element;

/** Tweak the behavior of a WBSModel for use in editing common workflows.
 */
public class WorkflowWBSModel extends WBSModel {

    private Map<Integer, WBSNode> workflowNodeMap = null;
    private Map<String, String> workflowTypeMap = new HashMap();

    public WorkflowWBSModel()            { super();     }
    public WorkflowWBSModel(String name) { super(name); }
    public WorkflowWBSModel(Element e)   { super(e);    }

    /* Nodes at indentation level 1 are defined workflows. */
    public String filterNodeType(WBSNode node) {
        if (node.getIndentLevel() == 1)
            return TeamProcess.WORKFLOW_TYPE;
        else
            return super.filterNodeType(node);
    }

    /* Nodes at indentation level 1 are defined workflows. */
    public boolean isNodeTypeEditable(WBSNode node) {
        return (!node.isReadOnly() && node.getIndentLevel() > 1);
    }

    /** Get the names of the workflows containing the given rows */
    public List getWorkflowsForRows(int[] rows) {
        LinkedList result = new UniqueLinkedList();

        List nodes = getNodesForRows(rows, true);
        for (Iterator i = nodes.iterator(); i.hasNext();) {
            WBSNode node = (WBSNode) i.next();
            WBSNode workflowNode = getWorkflowParent(node);
            if (workflowNode != null)
                result.add(workflowNode.getName());
        }

        return result;
    }

    private WBSNode getWorkflowParent(WBSNode node) {
        if (node == null)
            return null;
        else if (node.getIndentLevel() == 1)
            return node;
        else
            return getWorkflowParent(getParent(node));
    }

    public List getNodesForWorkflows(Collection selectedWorkflowNames) {
        LinkedList result = new LinkedList();

        if (selectedWorkflowNames != null && !selectedWorkflowNames.isEmpty()) {
            int numRows = getRowCount();
            for (int r = 1;   r < numRows;   r++) {
                WBSNode node = getNodeForRow(r);
                if (node.getIndentLevel() == 1
                        && selectedWorkflowNames.contains(node.getName())) {
                    result.add(node);
                    result.addAll(Arrays.asList(getDescendants(node)));
                }
            }
        }

        return result;
    }

    public void mergeWorkflow(WorkflowWBSModel source, String workflowName, boolean notify) {
        Set nameSet = Collections.singleton(workflowName);
        List currentWorkflowContents = getNodesForWorkflows(nameSet);
        List newWorkflowContents = source.getNodesForWorkflows(nameSet);

        if (newWorkflowContents == null || newWorkflowContents.isEmpty())
            return;
        newWorkflowContents = WBSNode.cloneNodeList(newWorkflowContents);

        int insertBeforeRow = Integer.MAX_VALUE;
        if (currentWorkflowContents != null && !currentWorkflowContents.isEmpty()) {
            WBSNode currentTopNode = (WBSNode) currentWorkflowContents.get(0);
            insertBeforeRow = getRowForNode(currentTopNode);
            deleteNodes(currentWorkflowContents, false);

            int currentWorkflowId = currentTopNode.getUniqueID();
            WBSNode newTopNode = (WBSNode) newWorkflowContents.get(0);
            newTopNode.setUniqueID(currentWorkflowId);
        }

        insertNodes(newWorkflowContents, insertBeforeRow, notify);
    }

    public void mergeWorkflows(WorkflowWBSModel source, Collection workflowNames) {
        for (Iterator i = workflowNames.iterator(); i.hasNext();) {
            String workflowName = (String) i.next();
            mergeWorkflow(source, workflowName, false);
        }

        fireTableDataChanged();
    }

    public List getWorkflowNames() {
        LinkedList result = new UniqueLinkedList();

        int numRows = getRowCount();
        for (int r = 1;   r < numRows;   r++) {
            WBSNode node = getNodeForRow(r);
            if (node.getIndentLevel() == 1)
                result.add(node.getName());
        }

        return result;
    }

    public Map<Integer, WBSNode> getWorkflowNodeMap() {
        if (workflowNodeMap == null)
            workflowNodeMap = getNodeMap();
        return workflowNodeMap;
    }

    @Override
    public void fireTableChanged(TableModelEvent e) {
        workflowNodeMap = null;
        super.fireTableChanged(e);
    }


    /**
     * If the given node represents a task in a workflow, return a node type
     * ending in "Workflow Task" that can be used for icon lookups.  If the
     * node is not a workflow task, returns null.
     */
    public String getWorkflowTaskType(WBSNode workflowNode) {
        // if this is a root node or a workflow parent, it isn't a task.
        if (workflowNode == null || workflowNode.getIndentLevel() < 2)
            return null;

        String plainNodeType = filterNodeType(workflowNode);
        String result = workflowTypeMap.get(plainNodeType);
        if (result == null) {
            result = calcWorkflowType(plainNodeType);
            workflowTypeMap.put(plainNodeType, result);
        }
        return (result.length() == 0 ? null : result);
    }

    private String calcWorkflowType(String nodeType) {
        if (nodeType == null)
            return "";

        else if (TeamProcess.isPSPTask(nodeType)
                || TeamProcess.isProbeTask(nodeType))
            return nodeType;

        else if (nodeType.endsWith(TeamProcess.TASK_SUFFIX))
            return nodeType.substring(0, nodeType.length()
                    - TeamProcess.TASK_SUFFIX.length())
                    + TeamProcess.WORKFLOW_TASK_SUFFIX;
        else
            return "";
    }


    /**
     * Get a descriptive name for a step within a workflow.
     * 
     * @param workflowNode
     *            a step within a workflow inside this model.
     * @param longName
     *            true if the result should include the name of the parent
     *            workflow; false if it should only include the name of the step
     */
    public String getStepFullName(WBSNode workflowNode, boolean longName) {
        if (workflowNode == null || workflowNode.getIndentLevel() < 2)
            return null;

        if (workflowNode.getIndentLevel() == 2 && !longName)
            return workflowNode.getName();

        StringBuilder result = new StringBuilder();
        result.append(workflowNode.getName());
        while (true) {
            workflowNode = getParent(workflowNode);
            if (workflowNode == null) {
                return null;
            } else if (workflowNode.getIndentLevel() < 2) {
                if (longName)
                    result.insert(0, ": ").insert(0, workflowNode.getName());
                return result.toString();
            } else {
                result.insert(0, "/").insert(0, workflowNode.getName());
            }
        }
    }


    private static class UniqueLinkedList extends LinkedList {
        public boolean add(Object o) {
            if (contains(o))
                return false;
            else
                return super.add(o);
        }
    }

}
