
package teamdash.wbs;

import java.util.Set;
import java.util.HashSet;
import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;

public class WBSModelValidator implements TableModelListener {

    public static final String NODE_TYPE_ERROR_ATTR_NAME = "Node_Type_Error";
    public static final String NODE_NAME_ERROR_ATTR_NAME = "Node_Name_Error";

    protected WBSModel wbsModel;

    public WBSModelValidator(WBSModel wbsModel) {
        this.wbsModel = wbsModel;
        wbsModel.addTableModelListener(this);
    }

    public void tableChanged(TableModelEvent e) { recalc(); }

    public void recalc() {
        System.out.println("WBSModelValidator.recalc()");
        recalc(wbsModel.getRoot());
    }

    protected void recalc(WBSNode node) {

        String type = node.getType();
        WBSNode parent = wbsModel.getParent(node);
        String typeError = null;
        if (parent != null) {
            String parentType = parent.getType();
            typeError = checkParentType(type, parentType);
            if (typeError != null)
                System.out.println("Type error: " + typeError);
        }
        node.setAttribute(NODE_TYPE_ERROR_ATTR_NAME, typeError);

        /* node names cannot be empty.
        String name = node.getName();
        String nameError = checkNodeName(name, type);
        node.setAttribute(NODE_NAME_ERROR_ATTR_NAME, nameError);
        */

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

    /** Check to ensure that the given parent is a valid parent for
     * this type of node.
     *
     * @return <code>null</code> if the node types are compatible,
     * otherwise, returns an appropriate error message.
     */
    protected String checkParentType(String type, String parentType) {
        if (type == null) type = "Task";
        if (parentType == null) parentType = "Task";

        if (isSoftwareComponent(type)) {

            // the parent of a software component must be another
            // software component.

            if (! isSoftwareComponent(parentType))
                return "Software components can only be children of "+
                    "other software components.";

        } else if (isDocument(type)) {

            // the parent of a document must be another document of the
            // same type, or a software component.

            if (!isSoftwareComponent(parentType) && !type.equals(parentType))
                return "This document must be a child either of a software "+
                    "component or of another " + type.toLowerCase() + ".";
        }

        // No problems detected.
        return null;
    }


    protected String checkNodeName(String name, String type) {
        if (name == null || name.trim().length() == 0)
            return "Every " + type.toLowerCase() + " must have a name.";

        return null;
    }

    protected boolean isDocument(String type) {
        return wbsModel.isDocument(type);
    }

    protected boolean isSoftwareComponent(String type) {
        return wbsModel.isSoftwareComponent(type);
    }
}
