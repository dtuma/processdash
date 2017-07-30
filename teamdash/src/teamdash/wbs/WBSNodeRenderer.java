// Copyright (C) 2002-2017 Tuma Solutions, LLC
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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import net.sourceforge.processdash.util.HTMLUtils;

import teamdash.wbs.columns.ErrorNotesColumn;
import teamdash.wbs.columns.NotesColumn;
import teamdash.wbs.columns.PercentCompleteColumn;

/** Custom table cell renderer for WBSNodes.
 */
public class WBSNodeRenderer extends DefaultTableCellRenderer {

    /** The wbsModel we're rendering for */
    private WBSModel wbsModel;
    /** A Map translating node types to their designated icons */
    private Map iconMap;
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
    /** Fonts used in the display of the node name */
    private Font regular = null, bold = null;
    /** The icon displayed for this node */
    private ExpansionIcon expansionIcon = new ExpansionIcon();


    /** Create a renderer for the given wbs model and icon map. */
    public WBSNodeRenderer(WBSModel wbsModel, Map iconMap,
            WorkflowWBSModel workflows) {
        this.wbsModel = wbsModel;
        this.iconMap = iconMap;
        this.workflows = workflows;
        setIconTextGap(4);
    }

    public String getRootNodeName() {
        return rootNodeName;
    }

    public void setRootNodeName(String rootNodeName) {
        this.rootNodeName = rootNodeName;
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
                true);
            if (iconToolTip == null)
                iconToolTip = wbsModel.filterNodeType(node);
            expansionIcon.realIcon = (Icon) iconObj;
        }
        // install the expansion icon
        setIcon(null);
        setIcon(expansionIcon);

        // check to see if the node name is in error
        nameError = WBSModelValidator.getNodeNameError(node);
        if (nameError != null) {
            // if the node name is in error, print it in bold red text
            result.setFont(getFont(true, result));
            result.setForeground(Color.red);
            nameToolTip = null;
        } else {
            result.setFont(getFont(false, result));
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
        String nodeType = WorkflowUtil.getWorkflowTaskType(node, workflows);
        if (nodeType == null)
            nodeType = model.filterNodeType(node);
        Icon icon = (Icon) iconMap.get(nodeType);
        if (icon == null) icon = (Icon) iconMap.get(null);

        String iconError = WBSModelValidator.getNodeTypeError(node);

        int modFlags = 0;
        if (iconError != null)
            modFlags |= IconFactory.ERROR_ICON;
        if (table instanceof WBSJTable && ((WBSJTable) table).isCutNode(node))
            modFlags |= IconFactory.PHANTOM_ICON;

        if (modFlags != 0)
            icon = IconFactory.getModifiedIcon(icon, modFlags);

        if (iconError == null)
            return icon;
        else
            return new ErrorValue(icon, iconError);
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


    /** Compute a bold or plain version of component <code>c</code>'s current
     * font */
    protected Font getFont(boolean bold, Component c) {
        if (this.regular == null) {
            Font base = c.getFont();
            if (base == null) return null;
            this.regular = base.deriveFont(Font.PLAIN);
            this.bold    = base.deriveFont(Font.BOLD);
        }
        return (bold ? this.bold : this.regular);
    }



    /** The fixed size (width and height) of icons used by this component */
    static final int ICON_SIZE   = 16;
    /** The amount of padding to add to the icon when displaying. */
    static final int ICON_MARGIN = 1;
    /** The resulting horizontal spacing for each indentation level. */
    static final int ICON_HORIZ_SPACING = ICON_SIZE + ICON_MARGIN;


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
                int topMargin = (realIconHeight - 9 + 1) / 2;
                Icon i = (isExpanded ? MINUS_ICON : PLUS_ICON);
                i.paintIcon(c, g,
                            x + (indentationLevel-1)*ICON_HORIZ_SPACING + 4,
                            y+topMargin);
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
    static Icon MINUS_ICON = IconFactory.getMinusIcon();

    /** static shared instance of a PlusIcon */
    static Icon PLUS_ICON = IconFactory.getPlusIcon();

    private static Color ERROR_NOTE_BACKGROUND = new Color(255, 200, 200);
    private static Color ERROR_NOTE_SELECTED_BACKGROUND = new Color(216, 175, 194);
}
