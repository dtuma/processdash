// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.hier.ui;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import net.sourceforge.processdash.hier.*;
import net.sourceforge.processdash.hier.ui.HierarchyTreeModel.HierarchyTreeNode;



public class SelectableHierarchyTree extends JTree {

    protected Set unselectedNodes;
    protected HierarchyTreeModel model;
    protected CheckboxNodeRenderer renderer;

    public SelectableHierarchyTree(DashHierarchy hierarchy) {
        setModel(model = new HierarchyTreeModel(hierarchy));
        unselectedNodes = new HashSet();
        setCellRenderer(renderer = new CheckboxNodeRenderer());
        addClickListener();
        expandRow (0);
        setShowsRootHandles (true);
        setEditable(false);
        setRootVisible(false);
    }

    public Vector getSelectedPaths() {
        Vector v = new Vector();
        getSelectedPaths(model.getRoot(), v);
        return v;
    }

    private void getSelectedPaths(Object object, Vector v) {
        HierarchyTreeNode node = (HierarchyTreeNode) object;
        if (!unselectedNodes.contains(node))
            v.add(node.getPath());
        for (int i = node.getChildCount();   i-- > 0; )
            getSelectedPaths(node.getChildAt(i), v);
    }

    private void addClickListener() {
        addMouseListener(new MouseAdapter() {
             public void mousePressed(MouseEvent e) {
                 if (e.getClickCount() != 1) return;
                 if (processMouseClick(e.getX(), e.getY()))
                     e.consume();
             }
         });
    }


    protected boolean processMouseClick(int x, int y) {
        int row = getRowForLocation(x, y);
        if (row == -1) return false;

        TreePath treePath = getPathForRow(row);
        Object clickedNode = treePath.getLastPathComponent();
        boolean expanded = isExpanded(row);
        boolean leaf = model.isLeaf(clickedNode);

        renderer.getTreeCellRendererComponent
            (this, clickedNode, false, expanded, leaf, row, true);
        CheckBoxIcon i = (CheckBoxIcon) renderer.getIcon();
        Rectangle bounds = getRowBounds(row);
        if (!i.isInCheckBox(x - bounds.x, y - bounds.y))
            return false;

        toggleSelection(treePath);
        return true;
    }

    protected void toggleSelection(TreePath selPath) {
        HierarchyTreeNode node =
            (HierarchyTreeNode) selPath.getLastPathComponent();

        // toggle current selection status.
        boolean isSelected = unselectedNodes.contains(node);
        setSelectionStatusRecursive(node, isSelected);
        if (!isSelected) {
            while (true) {
                node = (HierarchyTreeNode) node.getParent();
                if (node == null) break;
                setSelectionStatus(node, false);
            }
        }
    }
    protected void setSelectionStatus(TreeNode node, boolean selected) {
        if (selected)
            unselectedNodes.remove(node);
        else
            unselectedNodes.add(node);
        model.nodeChanged(node);
    }
    protected void setSelectionStatusRecursive(TreeNode node, boolean selected) {
        setSelectionStatus(node, selected);
        for (int i = node.getChildCount();   i-- > 0; )
            setSelectionStatusRecursive(node.getChildAt(i), selected);
    }

    public class CheckboxNodeRenderer extends DefaultTreeCellRenderer
    {
        private JCheckBox cb = new JCheckBox();

        public CheckboxNodeRenderer() {
            setLeafIcon(new CheckBoxIcon(getLeafIcon(), cb));
            setClosedIcon(new CheckBoxIcon(getClosedIcon(), cb));
            setOpenIcon(new CheckBoxIcon(getOpenIcon(), cb));
        }

        public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean sel,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus) {

            super.getTreeCellRendererComponent
                (tree, value, sel, expanded, leaf, row, hasFocus);

            cb.setSelected(!unselectedNodes.contains(value));

            return this;
        }

    }

    private class CheckBoxIcon implements Icon {

        private Icon icon, checkBoxIcon;
        private JCheckBox cb;
        private int gap, height, width;
        private int iconHeight,  iconWidth,  iconDiff;
        private int checkHeight, checkWidth, checkDiff;

        public CheckBoxIcon(Icon icon, JCheckBox cb) {
            this.icon = icon;
            this.cb = cb;
            this.checkBoxIcon = UIManager.getIcon("CheckBox.icon");
            this.gap = 4;

            iconHeight = icon.getIconHeight();
            checkHeight = checkBoxIcon.getIconHeight();
            height = Math.max(iconHeight, checkHeight);

            iconWidth = icon.getIconWidth();
            checkWidth = checkBoxIcon.getIconWidth();
            width = iconWidth + gap + checkWidth;

            iconDiff = (height-iconHeight) / 2;
            checkDiff = (height-checkHeight) / 2;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            icon.paintIcon(c, g, x, y+iconDiff);
            checkBoxIcon.paintIcon(cb, g, x+iconWidth+gap, y+checkDiff);
        }

        public int getIconWidth() { return width; }

        public int getIconHeight() { return height; }

        public boolean isInCheckBox(int x, int y) {
            return (x >= iconWidth + gap)
                && (x < width)
                && (y >= checkDiff)
                && (y < checkDiff + checkHeight);
        }
    }

    public boolean isPathEditable(TreePath path) {
        return false;
    }

}
