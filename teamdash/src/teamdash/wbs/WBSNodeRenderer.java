
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

import teamdash.wbs.columns.NotesColumn;

/** Custom table cell renderer for WBSNodes.
 */
public class WBSNodeRenderer extends DefaultTableCellRenderer {

    /** The wbsModel we're rendering for */
    private WBSModel wbsModel;
    /** A Map translating node types to their designated icons */
    private Map iconMap;
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
    public WBSNodeRenderer(WBSModel wbsModel, Map iconMap) {
        this.wbsModel = wbsModel;
        this.iconMap = iconMap;
        setIconTextGap(4);
    }


    // Implementation of TableCellRenderer interface

    public Component getTableCellRendererComponent
        (JTable table, Object value, boolean isSelected,
         boolean hasFocus, int row, int column)
    {
        WBSNode node = (WBSNode) value;
        String name = node.getName();
        if (name == null || name.trim().length() == 0) name = "( empty )";

        // Call our superclass to perform the default renderer configuration
        Component result = super.getTableCellRendererComponent
            (table, name, isSelected, hasFocus, row, column);

        // set up the expansion icon
        expansionIcon.indentationLevel = node.getIndentLevel();
        expansionIcon.isExpanded = node.isExpanded();
        expansionIcon.isLeaf = wbsModel.isLeaf(node);
        Object iconObj = getIconForNode(table, iconMap, node, wbsModel);
        if (iconObj instanceof ErrorValue) {
            iconToolTip = ((ErrorValue) iconObj).error;
            expansionIcon.realIcon = (Icon) ((ErrorValue) iconObj).value;
        } else {
            iconToolTip = wbsModel.filterNodeType(node);
            expansionIcon.realIcon = (Icon) iconObj;
        }
        // install the expansion icon
        setIcon(null);
        setIcon(expansionIcon);

        // check to see if the node name is in error
        nameError = (String) node.getAttribute
            (WBSModelValidator.NODE_NAME_ERROR_ATTR_NAME);
        if (nameError != null) {
            // if the node name is in error, print it in bold red text
            result.setFont(getFont(true, result));
            result.setForeground(Color.red);
            nameToolTip = null;
        } else {
            result.setFont(getFont(false, result));
            result.setForeground(node.isReadOnly() ? Color.gray : Color.black);
            nameToolTip = NotesColumn.getTooltipAt(node, false);
        }

        return result;
    }

    /** Convenience method - allows sharing of logic with WBSNodeEditor */
    public static Object getIconForNode(JTable table, Map iconMap,
                                        WBSNode node, WBSModel model)
    {
        String nodeType = model.filterNodeType(node);
        Icon icon = (Icon) iconMap.get(nodeType);
        if (icon == null) icon = (Icon) iconMap.get(null);

        String iconError = (String) node.getAttribute
            (WBSModelValidator.NODE_TYPE_ERROR_ATTR_NAME);

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
            return null;
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



    /** Icon class for displaying a minus sign in a box */
    private static class MinusIcon implements Icon {
        public int getIconWidth()  { return 9; }
        public int getIconHeight() { return 9; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Color.black);
            g.drawRect(x, y, 8, 8);          // square box
            g.drawLine(x+2, y+4, x+6, y+4);  // minus symbol
        }
    }
    /** static shared instance of a MinusIcon */
    static Icon MINUS_ICON = new MinusIcon();



    /** Icon class for displaying a plus sign in a box */
    private static class PlusIcon extends MinusIcon {
        public void paintIcon(Component c, Graphics g, int x, int y) {
            super.paintIcon(c, g, x, y);
            g.drawLine(x+4, y+2, x+4, y+6);  // vertical bar of plus symbol
        }
    }
    /** static shared instance of a PlusIcon */
    static Icon PLUS_ICON = new PlusIcon();
}
