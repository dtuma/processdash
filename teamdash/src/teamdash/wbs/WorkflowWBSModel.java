package teamdash.wbs;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
        return (!node.isReadOnly() && node.getIndentLevel() > 1);
    }

    /** Workflows behave like software components for validation purposes. */
    public boolean isSoftwareComponent(String type) {
        return "Workflow".equals(type) || super.isSoftwareComponent(type);
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
        // FIXME - this is not copying collapsed nodes
        Set nameSet = Collections.singleton(workflowName);
        List currentWorkflowContents = getNodesForWorkflows(nameSet);
        List newWorkflowContents = source.getNodesForWorkflows(nameSet);

        if (newWorkflowContents == null || newWorkflowContents.isEmpty())
            return;

        int insertBeforeRow = Integer.MAX_VALUE;
        if (currentWorkflowContents != null && !currentWorkflowContents.isEmpty()) {
            insertBeforeRow = getRowForNode((WBSNode) currentWorkflowContents.get(0));
            deleteNodes(currentWorkflowContents, false);
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

    private static class UniqueLinkedList extends LinkedList {
        public boolean add(Object o) {
            if (contains(o))
                return false;
            else
                return super.add(o);
        }
    }

}
