// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil


package pspdash;

import javax.swing.tree.*;
import java.awt.*;
import javax.swing.*;

public class SelectableTreeCellRenderer extends JCheckBox
implements TreeCellRenderer {

    /** Is the value currently selected. */
    protected boolean selected;

    // Icons
    /** Icon used to show non-leaf nodes that aren't expanded. */
    transient protected Icon closedIcon;

    /** Icon used to show leaf nodes. */
    transient protected Icon leafIcon;

    /** Icon used to show non-leaf nodes that are expanded. */
    transient protected Icon openIcon;

    // Colors
    /** Color to use for the foreground for selected nodes. */
    protected Color textSelectionColor;

    /** Color to use for the foreground for non-selected nodes. */
    protected Color textNonSelectionColor;

    /** Color to use for the background when a node is selected. */
    protected Color backgroundSelectionColor;

    /** Color to use for the background when the node isn't selected. */
    protected Color backgroundNonSelectionColor;

    /** Color to use for the background when the node isn't selected. */
    protected Color borderSelectionColor;

    public SelectableTreeCellRenderer() {

        setLeafIcon(UIManager.getIcon("Tree.leafIcon"));
        setClosedIcon(UIManager.getIcon("Tree.closedIcon"));
        setOpenIcon(UIManager.getIcon("Tree.openIcon"));

        setTextSelectionColor(UIManager.getColor("Tree.textSelectionColor"));
        setTextNonSelectionColor(UIManager.getColor("Tree.textNonSelectionColor"));
        setBackgroundSelectionColor(UIManager.getColor("Tree.backgroundSelectionColor"));
        setBackgroundNonSelectionColor(UIManager.getColor("Tree.backgroundNonSelectionColor"));
        setBorderSelectionColor(UIManager.getColor("Tree.borderSelectionColor"));
    }


    /**
     * Returns the default icon used to represent non-leaf nodes that are expanded.
     */
    public Icon getDefaultOpenIcon() {
        return openIcon;
    }

    /**
     * Returns the default icon used to represent non-leaf nodes that are not
     * expanded.
     */
    public Icon getDefaultClosedIcon() {
        return closedIcon;
    }

    /**
     * Returns the default icon used to represent leaf nodes.
     */
    public Icon getDefaultLeafIcon() {
        return leafIcon;
    }

    /**
     * Sets the icon used to represent non-leaf nodes that are expanded.
     */
    public void setOpenIcon(Icon newIcon) {
        openIcon = newIcon;
    }

    /**
     * Returns the icon used to represent non-leaf nodes that are expanded.
     */
    public Icon getOpenIcon() {
        return openIcon;
    }

    /**
     * Sets the icon used to represent non-leaf nodes that are not expanded.
     */
    public void setClosedIcon(Icon newIcon) {
        closedIcon = newIcon;
    }

    /**
     * Returns the icon used to represent non-leaf nodes that are not
     * expanded.
     */
    public Icon getClosedIcon() {
        return closedIcon;
    }

    /**
     * Sets the icon used to represent leaf nodes.
     */
    public void setLeafIcon(Icon newIcon) {
        leafIcon = newIcon;
    }

    /**
     * Returns the icon used to represent leaf nodes.
     */
    public Icon getLeafIcon() {
        return leafIcon;
    }

    /**
     * Sets the color the text is drawn with when the node is selected.
     */
    public void setTextSelectionColor(Color newColor) {
        textSelectionColor = newColor;
    }

    /**
     * Returns the color the text is drawn with when the node is selected.
     */
    public Color getTextSelectionColor() {
        return textSelectionColor;
    }

    /**
     * Sets the color the text is drawn with when the node isn't selected.
     */
    public void setTextNonSelectionColor(Color newColor) {
        textNonSelectionColor = newColor;
    }

    /**
     * Returns the color the text is drawn with when the node isn't selected.
     */
    public Color getTextNonSelectionColor() {
        return textNonSelectionColor;
    }

    /**
     * Sets the color to use for the background if node is selected.
     */
    public void setBackgroundSelectionColor(Color newColor) {
        backgroundSelectionColor = newColor;
    }


    /**
     * Returns the color to use for the background if node is selected.
     */
    public Color getBackgroundSelectionColor() {
        return backgroundSelectionColor;
    }

    /**
     * Sets the background color to be used for non selected nodes.
     */
    public void setBackgroundNonSelectionColor(Color newColor) {
        backgroundNonSelectionColor = newColor;
    }

    /**
     * Returns the background color to be used for non selected nodes.
     */
    public Color getBackgroundNonSelectionColor() {
        return backgroundNonSelectionColor;
    }

    /**
     * Sets the color to use for the border.
     */
    public void setBorderSelectionColor(Color newColor) {
        borderSelectionColor = newColor;
    }

    /**
     * Returns the color the border is drawn.
     */
    public Color getBorderSelectionColor() {
        return borderSelectionColor;
    }


    public Component getTreeCellRendererComponent(JTree tree,
                                                  Object value,
                                                  boolean sel,
                                                  boolean expanded,
                                                  boolean leaf,
                                                  int row,
                                                  boolean hasFocus) {
        /*
         * Sets the value of the current tree cell to value. If selected is true,
         * the cell will be drawn as if selected. If expanded is true the node is
         * currently expanded and if leaf is true the node represets a leaf and if
         * hasFocus is true the node currently has focus. tree is the JTree the
         * receiver is being configured for. Returns the Component that the
         * renderer uses to draw the value.
         * Returns:
         *  Component that the renderer uses to draw the value.
         */
        DefaultMutableTreeNode dmn = (DefaultMutableTreeNode)value;
        JCheckBox jcb = (JCheckBox)(dmn.getUserObject());
        String stringValue = jcb.getText();
        setFont( tree.getFont() );
        setText(stringValue);
        if(sel)
            setForeground(getTextSelectionColor());
        else
            setForeground(getTextNonSelectionColor());
        if (leaf) {
            setIcon(getLeafIcon());
        } else if (expanded) {
            setIcon(getOpenIcon());
        } else {
            setIcon(getClosedIcon());
        }
        setSelected (jcb.isSelected());

        selected = sel;

        return this;
    }

    /**
     * Paints the value.  The background is filled based on selected.
     */
    public void paint(Graphics g) {
        Color bColor;
        int offset = 0;
        Icon currentI = null;

        if(selected) {
            bColor = getBackgroundSelectionColor();
        } else {
            bColor = getBackgroundNonSelectionColor();
            if(bColor == null)
                bColor = getBackground();
        }
        if(bColor != null) {
            currentI = getIcon();

            g.setColor(bColor);
            if(currentI != null && getText() != null) {
                offset += (currentI.getIconWidth() + 5);//getIconTextGap());
            }
            g.fillRect(offset, 0, getWidth() - 1 - offset,
                       getHeight() - 1);
        }
        if (selected) {
            g.setColor(getBorderSelectionColor());
            g.drawRect(0, 0, getWidth()-1, getHeight()-1);
        }
        if (currentI != null)
            currentI.paintIcon (this, g, 0,
                                (getHeight() - currentI.getIconHeight())/2);
        if (selected) {
            g.setColor(getTextSelectionColor());
        } else {
            g.setColor(getTextNonSelectionColor());
        }
        FontMetrics fm = g.getFontMetrics();
        int checkSize = fm.getAscent() + fm.getDescent();
        int yOffset = (getHeight() - checkSize)/2;
        Rectangle check = new Rectangle (offset, yOffset, checkSize, checkSize);
        g.setColor(Color.black);
                                    // checkbox
        g.drawRect(check.x, check.y, check.width, check.height);
        if (isSelected()) {
            g.drawLine (check.x, check.y,
                        check.x + check.width, check.y + check.height);
            g.drawLine (check.x, check.y + check.height,
                        check.x + check.width, check.y);
        }
        offset += checkSize + 5;

        if (getText() != null) {
            yOffset += fm.getAscent();
            g.setColor(Color.black);
            g.drawString (getText(), offset, yOffset);
        }
    }

    public Dimension getPreferredSize() {
        Dimension        retDimension = super.getPreferredSize();

        if(retDimension != null)
            retDimension = new Dimension(retDimension.width + 33,
                                         retDimension.height);
        return retDimension;
    }

}
