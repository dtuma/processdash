
package teamdash.wbs;

import java.awt.*;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.event.MouseEvent;


public class WBSNodeRenderer extends DefaultTableCellRenderer {

    private WBSModel wbsModel;
    private Map iconMap;
    private String iconError, nameError;
    private Font regular = null, bold = null;

    public WBSNodeRenderer(WBSModel wbsModel, Map iconMap) {
        this.wbsModel = wbsModel;
        this.iconMap = iconMap;
        setIconTextGap(4);
    }


    public Component getTableCellRendererComponent
        (JTable table, Object value, boolean isSelected, boolean hasFocus,
         int row, int column)
    {
        WBSNode node = (WBSNode) value;

        expansionIcon.indentationLevel = node.getIndentLevel();
        expansionIcon.isExpanded = node.isExpanded();
        expansionIcon.isLeaf = wbsModel.isLeaf(node);
        Icon icon = (Icon) iconMap.get(node.getType());
        if (icon == null) icon =  (Icon) iconMap.get(null);

        iconError = (String) node.getAttribute
            (WBSModelValidator.NODE_TYPE_ERROR_ATTR_NAME);
        nameError = (String) node.getAttribute
            (WBSModelValidator.NODE_NAME_ERROR_ATTR_NAME);

        int modFlags = 0;
        if (iconError != null)
            modFlags |= IconFactory.ERROR_ICON;
        if (table instanceof WBSJTable && ((WBSJTable) table).isCutNode(node))
            modFlags |= IconFactory.PHANTOM_ICON;

        if (modFlags != 0)
            icon = IconFactory.getModifiedIcon(icon, modFlags);

        expansionIcon.realIcon = icon;

        Component result = super.getTableCellRendererComponent
            (table, node.getName(), isSelected, hasFocus, row, column);

        setIcon(null);
        setIcon(expansionIcon);

        if (nameError == null) {
            result.setFont(getFont(false, result));
            result.setForeground(Color.black);
        } else {
            result.setFont(getFont(true, result));
            result.setForeground(Color.red);
        }

        return result;
    }


    public String getToolTipText(MouseEvent event) {
        int delta = event.getX() - expansionIcon.getIconWidth();
        if (delta > 0) return nameError;
        else if (delta > -ICON_HORIZ_SPACING) return iconError;
        return null;
    }


    protected Font getFont(boolean bold, Component c) {
        if (this.regular == null) {
            Font base = c.getFont();
            if (base == null) return null;
            this.regular = base.deriveFont(Font.PLAIN);
            this.bold    = base.deriveFont(Font.BOLD);
        }
        return (bold ? this.bold : this.regular);
    }


    static final int ICON_SIZE   = 16;
    static final int ICON_MARGIN = 1;
    static final int ICON_HORIZ_SPACING = ICON_SIZE + ICON_MARGIN;

    private ExpansionIcon expansionIcon = new ExpansionIcon();
    private class ExpansionIcon implements Icon {

        int indentationLevel;
        boolean isExpanded, isLeaf;
        Icon realIcon;

        public ExpansionIcon() {}

        public void paintIcon(Component c, Graphics g, int x, int y) {
            int realIconHeight = realIcon.getIconHeight();

            if (indentationLevel > 0 && isLeaf == false) {
                int topMargin = (realIconHeight - 9 + 1) / 2;
                Icon i = (isExpanded ? MINUS_ICON : PLUS_ICON);
                i.paintIcon(c, g,
                            x + (indentationLevel-1)*ICON_HORIZ_SPACING + 4,
                            y+topMargin);
            }

            /*
            Icon realIcon = this.realIcon;
            if ((indentationLevel % 3) == 0)
                realIcon = IconFactory.getErrorIcon(realIcon);
            if ((indentationLevel & 1) == 0)
                realIcon = IconFactory.getPhantomIcon(realIcon);
            */

            realIcon.paintIcon(c, g, x+indentationLevel*ICON_HORIZ_SPACING, y);
        }

        public int getIconWidth() {
            return realIcon.getIconWidth() +
                indentationLevel * ICON_HORIZ_SPACING;
        }

        public int getIconHeight() {
            return realIcon.getIconHeight();
        }
        /*
        private void paintIconRectangle(Graphics g, int x, int y, Color c) {
            g.setColor(c);
            g.fillRect(x, y, ICON_HORIZ_SPACING, ICON_HORIZ_SPACING);
        }
        public void paintPhantomRectangle(Graphics g, int x, int y) {
            paintIconRectangle(g, x, y, phantomColor);
        }
        public void paintErrorRectangle(Graphics g, int x, int y) {
            paintIconRectangle(g, x, y, Color.red);
        }
        */
    }

    static Icon MINUS_ICON = new MinusIcon();
    private static class MinusIcon implements Icon {
        public int getIconWidth()  { return 9; }
        public int getIconHeight() { return 9; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Color.black);
            g.drawLine(x,   y,   x,   y+8);  // left side
            g.drawLine(x+1, y+8, x+8, y+8);  // bottom side
            g.drawLine(x+8, y+7, x+8, y);    // right side
            g.drawLine(x+1, y,   x+7, y);    // top side

            g.drawLine(x+2, y+4, x+6, y+4);  // minus symbol
        }
    }

    static Icon PLUS_ICON = new PlusIcon();
    private static class PlusIcon extends MinusIcon {
        public void paintIcon(Component c, Graphics g, int x, int y) {
            super.paintIcon(c, g, x, y);
            g.drawLine(x+4, y+2, x+4, y+6);  // vertical bar of plus symbol
        }
    }

}
