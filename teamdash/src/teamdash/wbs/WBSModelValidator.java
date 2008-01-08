
package teamdash.wbs;

import java.util.Set;
import java.util.StringTokenizer;
import java.util.HashSet;
import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;


/** Checks a WBSModel for errors, and saves those errors as attributes
 * on the erroneous nodes.
 */
public class WBSModelValidator implements TableModelListener {

    /** The node attribute used to record problems with the type of a node.
     */
    public static final String NODE_TYPE_ERROR_ATTR_NAME = "Node_Type_Error";

    /** The node attribute used to record problems with the name of a node.
     */
    public static final String NODE_NAME_ERROR_ATTR_NAME = "Node_Name_Error";

    /** The wbs model to validate */
    protected WBSModel wbsModel;

    /** Create a validator for the given WBSModel. */
    public WBSModelValidator(WBSModel wbsModel) {
        this.wbsModel = wbsModel;
        wbsModel.addTableModelListener(this);
    }

    public void tableChanged(TableModelEvent e) { recalc(); }

    public void recalc() {
        recalc(wbsModel.getRoot());
    }

    protected void recalc(WBSNode node) {

        // check for illegal parent/child node type relationships
        String type = wbsModel.filterNodeType(node);
        WBSNode parent = wbsModel.getParent(node);
        String typeError = null;
        if (parent != null) {
            String parentType = wbsModel.filterNodeType(parent);
            typeError = checkParentType(type, parentType);
        }
        node.setAttribute(NODE_TYPE_ERROR_ATTR_NAME, typeError);

        // check for broken relationships from the master WBS.
        checkMasterWBSRelationships(node, parent);

        // node names cannot be empty.
        String name = node.getName();
        String nameError = checkNodeName(name, type);
        node.setAttribute(NODE_NAME_ERROR_ATTR_NAME, nameError);


        WBSNode[] children = wbsModel.getChildren(node);
        if (children != null && children.length > 0) {
            // siblings must have unique names
            Set childNames = new HashSet();

            for (int i = 0;   i < children.length;   i++) {
                WBSNode child = children[i];
                String childName = child.getName();

                child.setAttribute(NODE_NAME_ERROR_ATTR_NAME, null);

                recalc(child);

                if (childNames.contains(childName) &&
                    child.getAttribute(NODE_NAME_ERROR_ATTR_NAME) == null)
                    child.setAttribute(NODE_NAME_ERROR_ATTR_NAME,
                                      "Duplicate WBS item name.");

                childNames.add(childName);
            }
        }
    }

    private void checkMasterWBSRelationships(WBSNode node, WBSNode parent) {
        String expectedParentID = (String) node
                .getAttribute(MasterWBSUtil.MASTER_PARENT_ID);
        if (expectedParentID != null) {
            String actualParentID = null;
            if (parent != null)
                actualParentID = (String) parent
                        .getAttribute(MasterWBSUtil.MASTER_NODE_ID);
            if (!expectedParentID.equals(actualParentID))
                node.setAttribute(NODE_TYPE_ERROR_ATTR_NAME,
                        "You have altered the hierarchical arrangement of the "
                                + "work items that were copied from the "
                                + "master project.");
        }
    }

    /** Check to ensure that the given parent is a valid parent for
     * this type of node.
     *
     * @return <code>null</code> if the node types are compatible,
     * otherwise, returns an appropriate error message.
     */
    protected String checkParentType(String type, String parentType) {
        if (type == null) type = "Task";
        if (parentType == null) parentType = "Task";

        if (isLOCComponent(type)) {

            // the parent of a software component must be another
            // software component.

            if (! isLOCComponent(parentType))
                return capitalize(type) + "s can only be children "+
                    "of components or software components.";

        } else if (isOtherSizeComponent(type)) {

            // the parent of a non-LOC size object must be another object
            // of the same type, or a software component.

            if (!isLOCComponent(parentType) && !type.equals(parentType))
                return "This " + type.toLowerCase() + " must be a child either"
                        + " of a component, a software component, or "
                        + " another " + type.toLowerCase() + ".";

        }

        if (isPSPTask(parentType)) {
            // PSP Tasks cannot have children.
            return "PSP tasks cannot have subtasks.";
        }

        if (WBSNode.UNKNOWN_TYPE.equals(type)) {
            return "You must define the type of this item.";
        }

        // No problems detected.
        return null;
    }

    /** Check to ensure that the name of this node is valid. */
    protected String checkNodeName(String name, String type) {
        if (name == null || name.trim().length() == 0)
            return "Every " + type.toLowerCase() + " must have a name.";

        return null;
    }

    /** Convenience method to check for components of non-LOC size types */
    protected boolean isOtherSizeComponent(String type) {
        return TeamProcess.isOtherSizeType(type);
    }

    /** Convenience method to check for software component types */
    protected boolean isLOCComponent(String type) {
        return TeamProcess.isLOCNode(type);
    }

    /** Convenience method to check for PSP tasks */
    protected boolean isPSPTask(String type) {
        return TeamProcess.isPSPTask(type);
    }

    /** Alter the capitalization of the given type so it can be used to
     * start a sentence. */
    protected String capitalize(String type) {
        StringTokenizer tok = new StringTokenizer(type);
        String result = tok.nextToken();
        while (tok.hasMoreTokens())
            result += " " + tok.nextToken().toLowerCase();
        return result;
    }

    public static String getNodeNameError(WBSNode node) {
        return (String) node.getAttribute(NODE_NAME_ERROR_ATTR_NAME);
    }

    public static String getNodeTypeError(WBSNode node) {
        return (String) node.getAttribute(NODE_TYPE_ERROR_ATTR_NAME);
    }

    public static boolean hasNodeError(WBSNode node) {
        return getNodeNameError(node) != null || getNodeTypeError(node) != null;
    }

}
