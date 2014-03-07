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
import java.util.Collection;
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

    /** The node attribute used on the WBS root node to record a set of valid
     * task types.
     */
    public static final String VALID_TASK_TYPES_ATTR_NAME = "Valid_Task_Types";

    /** The wbs model to validate */
    protected WBSModel wbsModel;

    /** True if we are validating a workflow WBS */
    protected boolean workflowMode;

    /** Create a validator for the given WBSModel. */
    public WBSModelValidator(WBSModel wbsModel) {
        this.wbsModel = wbsModel;
        this.workflowMode = (wbsModel instanceof WorkflowWBSModel);
        wbsModel.addTableModelListener(this);
    }

    public void tableChanged(TableModelEvent e) { recalc(); }


    private Collection validTaskTypes;

    public Collection getValidTaskTypes() {
        return validTaskTypes;
    }

    public void setValidTaskTypes(Collection validTaskTypes) {
        this.validTaskTypes = new ArrayList(validTaskTypes);
    }

    public void recalc() {
        recalc(wbsModel.getRoot(), new HashSet());
    }

    protected void recalc(WBSNode node, Set probeIDs) {

        // check for illegal parent/child node type relationships
        String type = wbsModel.filterNodeType(node);
        WBSNode parent = wbsModel.getParent(node);
        String typeError = null;
        if (isDuplicateProbeTask(node, probeIDs)) {
            typeError = "A workflow can only contain one PROBE task.";
        } else if (parent != null) {
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

                recalc(child, probeIDs);

                if (childNames.contains(childName) &&
                    child.getAttribute(NODE_NAME_ERROR_ATTR_NAME) == null)
                    child.setAttribute(NODE_NAME_ERROR_ATTR_NAME,
                                      "Duplicate name.");

                childNames.add(childName);

                maybeResetProbeIDs(node, probeIDs);
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
                return "This " + lowerCase(type) + " must be a child either"
                + " of a component, a software component, or "
                + (isCodeTask(type) ? "another" : "a") + " code task.";

        } else if (isOtherSizeComponent(type)) {

            // the parent of a non-LOC size object must be another object
            // of the same type, or a software component.

            if (!isLOCComponent(parentType) && !type.equals(parentType))
                return "This " + lowerCase(type) + " must be a child either"
                        + " of a component, a software component, or "
                        + " another " + lowerCase(type) + ".";

        }

        if (isPSPTask(parentType) || isProbeTask(parentType)) {
            // PSP and PROBE Tasks cannot have children.
            return lowerCase(parentType) + "s cannot have subtasks.";
        }

        if (WBSNode.UNKNOWN_TYPE.equals(type)) {
            return "You must define the type of this item.";
        }

        if (validTaskTypes != null
                && type.endsWith(" Task")
                && !validTaskTypes.contains(type)) {
            String taskType = type.substring(0, type.length() - 5);
            return "'" + taskType + "' is not a valid task type.";
        }

        // No problems detected.
        return null;
    }

    /** Check to ensure that the name of this node is valid. */
    protected String checkNodeName(String name, String type) {
        if (name == null || name.trim().length() == 0)
            return "Every " + lowerCase(type) + " must have a name.";

        return null;
    }

    /** Check to see if this is a duplicate PROBE task for a workflow */
    protected boolean isDuplicateProbeTask(WBSNode node, Set probeIDs) {
        if (!isProbeTask(node.getType()))
            return false;

        String workflowID = workflowMode ? "probe" //
                : WorkflowUtil.getPrimaryWorkflowSrcID(node);
        return !probeIDs.add(workflowID);
    }

    /** Clear out the set of PROBE IDs if appropriate */
    private void maybeResetProbeIDs(WBSNode parentNode, Set probeIDs) {
        if (workflowMode ? parentNode.getIndentLevel() == 0 //
                : WorkflowUtil.getPrimaryWorkflowSrcID(parentNode) == null)
            probeIDs.clear();
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

    /** Convenience method to check for PSP tasks */
    protected boolean isProbeTask(String type) {
        return TeamProcess.isProbeTask(type);
    }

    /** Convenience method to check for Code tasks */
    protected boolean isCodeTask(String type) {
        return TeamProcess.isCodeTask(type);
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

    protected String lowerCase(String type) {
        if (isPSPTask(type))
            return "PSP task";
        else if (isProbeTask(type))
            return "PROBE task";
        else
            return type.toLowerCase();
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
