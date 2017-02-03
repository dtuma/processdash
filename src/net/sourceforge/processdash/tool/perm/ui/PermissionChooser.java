// Copyright (C) 2017 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.perm.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import net.sourceforge.processdash.tool.perm.PermissionSpec;
import net.sourceforge.processdash.tool.perm.PermissionsManager;
import net.sourceforge.processdash.ui.lib.JOptionPaneClickHandler;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;

public class PermissionChooser {

    private JTree tree;

    private JScrollPane sp;

    public PermissionChooser() {
        // build a tree of all known permissions
        tree = new JTree(buildModel());
        tree.setRootVisible(false);
        tree.setCellRenderer(new PermissionRenderer());
        tree.setToggleClickCount(3);
        new JOptionPaneClickHandler().install(tree);
        tree.getSelectionModel().setSelectionMode(
            TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

        // expand all rows
        for (int i = 0; i < tree.getRowCount(); i++)
            tree.expandRow(i);

        // create a scroll pane to contain the tree
        sp = new JScrollPane(tree);
        Dimension d = tree.getPreferredSize();
        d.width = Math.min(d.width + 20, 300);
        d.height = Math.min(d.height + 20, 500);
        sp.setPreferredSize(d);
    }


    public List<PermissionSpec> promptForPermissions(Component parent) {
        // reset the UI and display the dialog asking to select a permission
        tree.clearSelection();
        String title = RolesEditor.resources.getString("Add_Permissions");
        Object message = new Object[] { sp,
                new JOptionPaneTweaker.MakeResizable() };
        int userChoice = JOptionPane.showConfirmDialog(parent, message, title,
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        // remember the size of the window for the next invocation
        sp.setPreferredSize(sp.getSize());

        // abort if the user pressed cancel, or didn't select any permissions.
        TreePath[] selected = tree.getSelectionPaths();
        if (userChoice != JOptionPane.OK_OPTION || selected == null)
            return Collections.EMPTY_LIST;

        // retrieve the permission specs for each of the selected tree paths
        List<PermissionSpec> result = new ArrayList<PermissionSpec>();
        for (TreePath oneItem : selected) {
            DefaultMutableTreeNode oneNode = (DefaultMutableTreeNode) oneItem
                    .getLastPathComponent();
            result.add((PermissionSpec) oneNode.getUserObject());
        }
        return result;
    }


    private DefaultTreeModel buildModel() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(null);
        addChildPermissions(root, null, new ArrayList());
        return new DefaultTreeModel(root);
    }

    private void addChildPermissions(DefaultMutableTreeNode parentNode,
            PermissionSpec parentSpec, List parents) {
        // add this spec to the list of parents (to avoid infinite recursion)
        parents.add(parentSpec);

        // find permission specs that are children of this one, and create
        // nodes for each one
        List<PermissionSpec> children = PermissionsManager.getInstance()
                .getChildSpecsFor(parentSpec);
        for (PermissionSpec childSpec : children) {
            if (!parents.contains(childSpec)) {
                DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(
                        childSpec);
                parentNode.add(childNode);
                addChildPermissions(childNode, childSpec, parents);
            }
        }

        // remove this spec from the list of parents
        parents.remove(parents.size() - 1);
    }


    private class PermissionRenderer extends DefaultTreeCellRenderer
            implements TreeSelectionListener {

        Color unselectedBackground, inheritedSelectionBackground;

        PermissionRenderer() {
            unselectedBackground = getBackgroundNonSelectionColor();
            inheritedSelectionBackground = new Color(230, 236, 242);
            tree.getSelectionModel().addTreeSelectionListener(this);
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean sel, boolean expanded, boolean leaf, int row,
                boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf,
                row, hasFocus);

            if (sel == false && isChildOfSelection(tree, row)) {
                setBackgroundNonSelectionColor(inheritedSelectionBackground);
            } else {
                setBackgroundNonSelectionColor(unselectedBackground);
            }

            return this;
        }

        private boolean isChildOfSelection(JTree tree, int row) {
            TreePath rowPath = tree.getPathForRow(row);
            TreePath[] selectionPaths = tree.getSelectionPaths();
            if (selectionPaths != null && selectionPaths.length > 0) {
                for (TreePath sel : selectionPaths) {
                    if (sel.isDescendant(rowPath))
                        return true;
                }
            }
            return false;
        }

        @Override
        public void valueChanged(TreeSelectionEvent e) {
            tree.repaint();
        }

    }

}
