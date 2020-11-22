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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import net.sourceforge.processdash.util.HTMLUtils;

import teamdash.sync.ExtSyncUtil;
import teamdash.wbs.columns.ErrorNotesColumn;
import teamdash.wbs.columns.NotesColumn;
import teamdash.wbs.columns.PercentCompleteColumn;
import teamdash.wbs.icons.ExpansionToggleIcon;

/** Custom table cell renderer for WBSNodes.
 */
public class WBSNodeRenderer extends DefaultTableCellRenderer {

    /** The wbsModel we're rendering for */
    private WBSModel wbsModel;
    /** A Map translating node types to their designated icons */
    private Map iconMap;
    /** A Map with special display values for selected node types */
    private Map<String, String> typeNameMap;
    /** A workflow model for resolving workflow type icons */
    private WorkflowWBSModel workflows;
    /** An optional override for the name of the root node */
    private String rootNodeName;
    /** The error message to display as a tooltip for the node's icon */
    private String iconToolTip;
    /** The error message to display as a tooltip for the node name */
    private String nameError;
    /** Text to display as the normal tooltip for the node name */
    private String nameToolTip;
    /** The icon displayed for this node */
    private ExpansionIcon expansionIcon = new ExpansionIcon();


    /** Create a renderer for the given wbs model and icon map. */
    public WBSNodeRenderer(WBSModel wbsModel, Map iconMap,
            WorkflowWBSModel workflows) {
        this.wbsModel = wbsModel;
        this.iconMap = iconMap;
        this.typeNameMap = Collections.EMPTY_MAP;
        this.workflows = workflows;
        setIconTextGap(4);
        updateGeometry();
    }

    public String getRootNodeName() {
        return rootNodeName;
    }

    public void setRootNodeName(String rootNodeName) {
        this.rootNodeName = rootNodeName;
    }

    public final Map<String, String> getTypeNameMap() {
        return typeNameMap;
    }

    public final void setTypeNameMap(Map<String, String> typeNameMap) {
        this.typeNameMap = typeNameMap;
    }


    // Implementation of TableCellRenderer interface

    public Component getTableCellRendererComponent
        (JTable table, Object value, boolean isSelected,
         boolean hasFocus, int row, int column)
    {
        // clear the background color
        setBackground(null);

        WBSNode node = (WBSNode) value;
        if (node == null)
            return super.getTableCellRendererComponent(table, null, isSelected,
                hasFocus, row, column);

        String name;
        if (row == 0 && rootNodeName != null)
            name = rootNodeName;
        else
            name = node.getName();
        if (name == null || name.trim().length() == 0) name = "( empty )";

        // display completed tasks with a strikethrough font
        if (PercentCompleteColumn.isComplete(node))
            name = "<html><strike>" + HTMLUtils.escapeEntities(name)
                    + "</strike></html>";

        // Call our superclass to perform the default renderer configuration
        Component result = super.getTableCellRendererComponent
            (table, name, isSelected, hasFocus, row, column);

        // set up the expansion icon
        expansionIcon.indentationLevel = node.getIndentLevel();
        expansionIcon.isExpanded = node.isExpanded();
        expansionIcon.isLeaf = wbsModel.isLeaf(node);
        Object iconObj = getIconForNode(table, iconMap, node, wbsModel,
            workflows);
        if (iconObj instanceof ErrorValue) {
            iconToolTip = ((ErrorValue) iconObj).error;
            expansionIcon.realIcon = (Icon) ((ErrorValue) iconObj).value;
        } else {
            iconToolTip = WorkflowUtil.getWorkflowStepName(node, workflows,
                ": ");
            if (iconToolTip == null)
                iconToolTip = wbsModel.filterNodeType(node);
            String typeName = typeNameMap.get(iconToolTip);
            if (typeName != null)
                iconToolTip = typeName;
            expansionIcon.realIcon = (Icon) iconObj;
        }
        // install the expansion icon
        setIcon(null);
        setIcon(expansionIcon);

        // check to see if the node name is in error
        nameError = WBSModelValidator.getNodeNameError(node);
        if (nameError != null) {
            // if the node name is in error, print it in bold red text
            result.setFont(TableFontHandler.getBold(table));
            result.setForeground(Color.red);
            nameToolTip = null;
        } else {
            result.setFont(table.getFont());
            result.setForeground(node.isReadOnly() ? Color.gray : Color.black);
            String errorNotesTip = ErrorNotesColumn.getTooltipAt(node, false);
            if (errorNotesTip != null) {
                setBackground(isSelected ? ERROR_NOTE_SELECTED_BACKGROUND
                        : ERROR_NOTE_BACKGROUND);
                nameToolTip = errorNotesTip;
            } else {
                nameToolTip = NotesColumn.getTooltipAt(node, false);
            }
        }

        return result;
    }

    /** Convenience method - allows sharing of logic with WBSNodeEditor */
    public static Object getIconForNode(JTable table, Map iconMap,
            WBSNode node, WBSModel model, WorkflowWBSModel workflows)
    {
        ErrorValue extNodeType = getExtNodeTypeIcon(table, node);

        String nodeType = null;
        if (extNodeType != null)
            nodeType = extNodeType.error;
        if (nodeType == null)
            nodeType = WorkflowUtil.getWorkflowTaskType(node, workflows);
        if (nodeType == null)
            nodeType = model.filterNodeType(node);

        Icon icon = null;
        if (extNodeType != null) icon = (Icon) extNodeType.value;
        if (icon == null) icon = (Icon) iconMap.get(nodeType);
        if (icon == null) icon = (Icon) iconMap.get(null);

        String iconError = WBSModelValidator.getNodeTypeError(node);

        int modFlags = 0;
        if (iconError != null)
            modFlags |= IconFactory.ERROR_ICON;
        if (table instanceof WBSJTable && ((WBSJTable) table).isCutNode(node))
            modFlags |= IconFactory.PHANTOM_ICON;

        if (modFlags != 0)
            icon = IconFactory.getModifiedIcon(icon, modFlags);

        if (iconError != null)
            return new ErrorValue(icon, iconError);
        else if (extNodeType != null)
            return new ErrorValue(icon, extNodeType.error);
        else
            return icon;
    }

    private static ErrorValue getExtNodeTypeIcon(JTable table, WBSNode node) {
        if (!ExtSyncUtil.isExtNode(node))
            return null;

        ExternalSystemManager extSys = getExtSysMgr(table);
        if (extSys == null)
            return null;

        Icon icon = extSys.getExtNodeTypeIcon(node);
        String type = (String) node.getAttribute(ExtSyncUtil.EXT_NODE_TYPE_ATTR);
        return new ErrorValue(icon, type);
    }

    private static ExternalSystemManager getExtSysMgr(JTable table) {
        if (table instanceof WBSJTable) {
            WBSJTable wbsTable = (WBSJTable) table;
            if (wbsTable.dataModel instanceof WBSDataModel) {
                WBSDataModel data = (WBSDataModel) wbsTable.dataModel;
                return data.getExternalSystemManager();
            }
        }
        return null;
    }


    /** Determine the appropriate tool tip based upon mouse location */
    public String getToolTipText(MouseEvent event) {
        int delta = event.getX() - expansionIcon.getIconWidth();
        if (delta > 0)
            return (nameError != null ? nameError : nameToolTip);
        else if (delta > -ICON_HORIZ_SPACING)
            return iconToolTip;
        else
            return nameToolTip;
    }



    /** The amount of padding to add to the icon when displaying. */
    static final int ICON_MARGIN = 1;

    /** The resulting horizontal spacing for each indentation level. */
    private int ICON_HORIZ_SPACING;

    void updateGeometry() {
        Icon i = (Icon) iconMap.get(null);
        ICON_HORIZ_SPACING = i.getIconWidth() + ICON_MARGIN;
    }



    /** An icon for display to the left of a wbs node.
     *
     * This icon contains three independent parts:<ul>
     * <li>initial blank space, whose width is determined by the
     * <code>indentationLevel</code> field
     * <li>an optional "expansion icon" - a plus sign or minus sign
     * used for expanding/collapsing the tree. (This icon does not appear if
     * the given node has no children.)
     * <li>a node-specific icon, whose appearance is controlled by the
     * <code>realIcon</code> field
     * </ul>
         */
    private final class ExpansionIcon implements Icon {

        int indentationLevel;
        boolean isExpanded, isLeaf;
        Icon realIcon;

        public ExpansionIcon() {}

        public void paintIcon(Component c, Graphics g, int x, int y) {
            int realIconHeight = realIcon.getIconHeight();

            // paint the plus/minus sign if appropriate
            if (indentationLevel > 0 && isLeaf == false) {
                Icon i = (isExpanded ? MINUS_ICON : PLUS_ICON);
                int topMargin = (realIconHeight - i.getIconHeight() + 1) / 2;
                int leftMargin = (indentationLevel - 1) * ICON_HORIZ_SPACING
                        + (ICON_HORIZ_SPACING - i.getIconWidth()) / 2;
                i.paintIcon(c, g, x + leftMargin, y + topMargin);
            }

            // paint the real icon
            realIcon.paintIcon(c, g, x+indentationLevel*ICON_HORIZ_SPACING, y);
        }

        public int getIconWidth() {
            return realIcon.getIconWidth() +
                indentationLevel * ICON_HORIZ_SPACING;
        }

        public int getIconHeight() {
            return realIcon.getIconHeight();
        }
    }

    /** static shared instance of a MinusIcon */
    static Icon MINUS_ICON = WBSZoom.icon(new ExpansionToggleIcon(false));

    /** static shared instance of a PlusIcon */
    static Icon PLUS_ICON = WBSZoom.icon(new ExpansionToggleIcon(true));

    private static Color ERROR_NOTE_BACKGROUND = new Color(255, 200, 200);
    private static Color ERROR_NOTE_SELECTED_BACKGROUND = new Color(216, 175, 194);
}
