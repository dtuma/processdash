// PSP Dashboard - Data Automation Tool for PSP-like processes
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package pspdash;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JTree;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import pspdash.HierarchyTreeModel.HierarchyTreeNode;


public class SelectableHierarchyTree extends JTree {

    protected Set unselectedNodes;
    protected HierarchyTreeModel model;
    protected CheckboxNodeRenderer renderer;

    public SelectableHierarchyTree(PSPProperties hierarchy) {
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
                 int selRow = getRowForLocation(e.getX(), e.getY());
                 if (selRow == -1) return;
                 TreePath selPath = getPathForRow(selRow);
                 toggleSelection(selPath);
             }
         });
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

        private Icon a, b;
        private Component cb;
        private int gap;

        public CheckBoxIcon(Icon a, Component cb) {
            this.a = a;
            this.cb = cb;
            this.b = UIManager.getIcon("CheckBox.icon");
            this.gap = 4;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            int aHeight = a.getIconHeight();
            int bHeight = b.getIconHeight();
            int height = Math.max(aHeight, bHeight);

            int aDiff = (height-aHeight) / 2;
            int bDiff = (height-bHeight) / 2;

            a.paintIcon(c, g, x, y+aDiff);
            b.paintIcon(cb, g, x+a.getIconWidth()+gap, y+bDiff);
        }

        public int getIconWidth() {
            return a.getIconWidth() + gap + b.getIconWidth();
        }

        public int getIconHeight() {
            return Math.max(a.getIconHeight(), b.getIconHeight());
        }
    }

    public boolean isPathEditable(TreePath path) {
        return false;
    }

}
