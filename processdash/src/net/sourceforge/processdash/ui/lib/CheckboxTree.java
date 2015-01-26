// Copyright (C) 2003-2015 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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

package net.sourceforge.processdash.ui.lib;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import net.sourceforge.processdash.ui.macosx.MacGUIUtils;


/**
 * @since 2.1.6
 */
public class CheckboxTree extends JTree {

    protected Set unselectedNodes;
    protected CheckboxNodeRenderer renderer;

    public CheckboxTree(TreeModel model) {
        super(model);
        unselectedNodes = new HashSet();
        setCellRenderer(renderer = new CheckboxNodeRenderer());
        addClickListener();
        expandRow (0);
        setShowsRootHandles (true);
        setEditable(false);
        setRootVisible(false);
    }

    public List<TreeNode> getSelectedNodes() {
        List<TreeNode> result = new ArrayList<TreeNode>();
        getSelectedNodes(getModel().getRoot(), result, false);
        return result;
    }

    public List<TreeNode> getSelectedNodesBrief() {
        List<TreeNode> result = new ArrayList<TreeNode>();
        getSelectedNodes(getModel().getRoot(), result, true);
        return result;
    }


    protected void getSelectedNodes(Object object, List l, boolean brief) {
        TreeNode node = (TreeNode) object;
        if (!unselectedNodes.contains(node)) {
            l.add(node);
            if (brief)
                return;
        }
        for (int i = node.getChildCount();   i-- > 0; )
            getSelectedNodes(node.getChildAt(i), l, brief);
    }

    private void addClickListener() {
        addMouseListener(new MouseAdapter() {
             @Override
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
        boolean leaf = getModel().isLeaf(clickedNode);

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
        TreeNode node = (TreeNode) selPath.getLastPathComponent();

        // toggle current selection status.
        boolean isSelected = unselectedNodes.contains(node);
        setSelectionStatusRecursive(node, isSelected);
        if (!isSelected) {
            while (true) {
                node = node.getParent();
                if (node == null) break;
                setSelectionStatus(node, false);
            }
        }
        fireListSelectionEvent(false);
    }

    protected void setSelectionStatus(TreeNode node, boolean selected) {
        if (selected)
            unselectedNodes.remove(node);
        else
            unselectedNodes.add(node);
        fireNodeChanged(node);
        fireListSelectionEvent(true);
    }

    protected void setSelectionStatusRecursive(TreeNode node, boolean selected) {
        setSelectionStatus(node, selected);
        for (int i = node.getChildCount();   i-- > 0; )
            setSelectionStatusRecursive(node.getChildAt(i), selected);
    }

    protected void fireNodeChanged(TreeNode node) {
        ((DefaultTreeModel) getModel()).nodeChanged(node);
    }

    public void addListSelectionListener(ListSelectionListener l) {
        listenerList.add(ListSelectionListener.class, l);
    }
    public void removeListSelectionListener(ListSelectionListener l) {
        listenerList.remove(ListSelectionListener.class, l);
    }
    protected void fireListSelectionEvent(boolean isAdjusting) {
        EventListener[] listeners = listenerList.getListeners(ListSelectionListener.class);
        if (listeners != null && listeners.length > 0) {
            ListSelectionEvent event = new ListSelectionEvent(this, -1, -1, isAdjusting);
            for (int i = 0; i < listeners.length; i++) {
                ((ListSelectionListener) listeners[i]).valueChanged(event);
            }
        }
    }

    public class CheckboxNodeRenderer extends DefaultTreeCellRenderer
    {
        private JCheckBox cb = new JCheckBox();

        public CheckboxNodeRenderer() {
            changeLeafIcon(getLeafIcon());
            changeClosedIcon(getClosedIcon());
            changeOpenIcon(getOpenIcon());
        }

        public void changeLeafIcon(Icon icon) {
            super.setLeafIcon(new CheckBoxIcon(icon, cb));
        }

        public void changeClosedIcon(Icon icon) {
            super.setClosedIcon(new CheckBoxIcon(icon, cb));
        }

        public void changeOpenIcon(Icon icon) {
            super.setOpenIcon(new CheckBoxIcon(icon, cb));
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
            this.checkBoxIcon = getCheckBoxIcon(cb);
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

        private Icon getCheckBoxIcon(JCheckBox cb) {
            if (MacGUIUtils.isMacOSX())
                return new MacCheckBoxIcon(cb);
            else
                return UIManager.getIcon("CheckBox.icon");
        }
    }

    private class MacCheckBoxIcon implements Icon {

        JCheckBox cb;
        int height;
        int width;

        public MacCheckBoxIcon(JCheckBox cb) {
            this.cb = cb;
            this.height = cb.getMinimumSize().height;
            this.width = cb.getMinimumSize().width;
            cb.setBounds(0, 0, width, height);
        }

        public int getIconHeight() {
            return height;
        }

        public int getIconWidth() {
            return width;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.translate(x, y);
            cb.paint(g);
            g.translate(-x, -y);
        }

    }

    public boolean isPathEditable(TreePath path) {
        return false;
    }

}
