package teamdash.wbs;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

/** Tweak the behavior of a WBSModel for use in editing common workflows.
 */
public class WorkflowWBSModel extends WBSModel {

    public WorkflowWBSModel()            { super();     }
    public WorkflowWBSModel(String name) { super(name); }
    public WorkflowWBSModel(Element e)   { super(e);    }

    /* Nodes at indentation level 1 are defined workflows. */
    public String filterNodeType(WBSNode node) {
        if (node.getIndentLevel() == 1)
            return "Workflow";
        else
            return super.filterNodeType(node);
    }

    /* Nodes at indentation level 1 are defined workflows. */
    public boolean isNodeTypeEditable(WBSNode node) {
        return (node.getIndentLevel() > 1);
    }

    /** Workflows behave like software components for validation purposes. */
    public boolean isSoftwareComponent(String type) {
        return "Workflow".equals(type) || super.isSoftwareComponent(type);
    }

    /** Get the names of the workflows containing the given rows */
    public Set getWorkflowsForRows(int[] rows) {
        HashSet result = new HashSet();

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

    public List getNodesForWorkflows(Set selectedWorkflowNames) {
        LinkedList result = new LinkedList();

        int numRows = getRowCount();
        boolean isIncluded = false;
        for (int r = 1;   r < numRows;   r++) {
            WBSNode node = getNodeForRow(r);
            if (node.getIndentLevel() == 1)
                isIncluded = selectedWorkflowNames.contains(node.getName());
            if (isIncluded)
                result.add(node);
        }

        return result;
    }

//    public static void mergeWorkflows(WBSModel dest, WBSModel src) {
//        LinkedList workflowNames = new LinkedList();
//        HashMap workflows = new HashMap();
//
//        IntList destWorkflowPositions = dest.getChildIndexes(dest.getRoot());
//        for (int i = 0;   i < destWorkflowPositions.size();  i++) {
//            String workflowName = dest.getChildren()
//        }
//
//
//        WBSNode srcNode = null;
//        for (int i = 0;   i < workflowItems.length;   i++)
//            if (workflowName.equals(workflowItems[i].getName())) {
//                srcNode = workflowItems[i];
//                break;
//            }
//
//    }


}
