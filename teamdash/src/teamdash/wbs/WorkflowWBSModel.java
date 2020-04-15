// Copyright (C) 2002-2020 Tuma Solutions, LLC
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.event.TableModelEvent;

import org.w3c.dom.Element;

import teamdash.merge.ModelType;

/** Tweak the behavior of a WBSModel for use in editing common workflows.
 */
public class WorkflowWBSModel extends WBSModel {

    private Map<Integer, WBSNode> workflowNodeMap = null;
    private Map<String, String> workflowTypeMap = new HashMap();

    public WorkflowWBSModel()            { super();     }
    public WorkflowWBSModel(String name) { super(name); }
    public WorkflowWBSModel(Element e)   { super(e);    }

    public ModelType getModelType() {
        return ModelType.Workflows;
    }

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

    /** Return the nodes that represent non-empty workflows */
    public List<WBSNode> getWorkflowNodes() {
        WBSNode[] rootChildren = getChildren(getRoot());
        List<WBSNode> result = new ArrayList<WBSNode>(rootChildren.length);
        for (WBSNode child : rootChildren) {
            if (!isLeaf(child))
                result.add(child);
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

        else if (TeamProcess.isProbeTask(nodeType))
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
     * @param parentDelim
     *            null if the result should only include the name of the step
     *            within the workflow. If non-null, the result will begin with
     *            the name of the parent workflow, followed by the given
     *            delimiter.
     */
    public String getStepFullName(WBSNode workflowNode, String parentDelim) {
        if (workflowNode == null || workflowNode.getIndentLevel() < 2)
            return null;

        if (workflowNode.getIndentLevel() == 2 && parentDelim == null)
            return workflowNode.getName();

        StringBuilder result = new StringBuilder();
        result.append(workflowNode.getName());
        while (true) {
            workflowNode = getParent(workflowNode);
            if (workflowNode == null) {
                return null;
            } else if (workflowNode.getIndentLevel() < 2) {
                if (parentDelim != null)
                    result.insert(0, parentDelim).insert(0, workflowNode.getName());
                return result.toString();
            } else {
                result.insert(0, "/").insert(0, workflowNode.getName());
            }
        }
    }

}
