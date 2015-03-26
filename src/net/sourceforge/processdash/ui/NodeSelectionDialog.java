// Copyright (C) 2001-2003 Tuma Solutions, LLC
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


package net.sourceforge.processdash.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.hier.ui.PropTreeModel;
import net.sourceforge.processdash.i18n.Resources;


public class NodeSelectionDialog extends JDialog
    implements TreeSelectionListener, ActionListener
{
    /** Class Attributes */
    protected JTree           tree;
    protected PropTreeModel   treeModel;
    protected JButton         okayButton = null;

    protected SelectionApprover approver = null;
    protected String currentlySelectedPath = null;
    protected PropertyKey currentlySelectedKey = null;

    protected String selectedPath = null;
    protected PropertyKey selectedKey = null;

    public interface SelectionApprover {
        boolean selectionIsAcceptable(PropertyKey key, String path);
    }

    public NodeSelectionDialog(Frame owner, DashHierarchy hierarchy,
                               String title, String message, String okayLabel,
                               SelectionApprover approver) {
        super(owner, title, true);
        if (message != null)
            getContentPane().add(new JLabel(message), BorderLayout.NORTH);

        /* Create the JTreeModel. */
        treeModel= new PropTreeModel(new DefaultMutableTreeNode("root"), null);

        /* Create the tree. */
        tree = new JTree(treeModel);
        treeModel.fill(hierarchy);
        tree.setShowsRootHandles(true);
        tree.setEditable(false);
        tree.getSelectionModel().setSelectionMode
            (TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeSelectionListener(this);
        tree.expandRow (0);
        tree.setRootVisible(false);
        tree.setRowHeight(-1);     // make tree ask for the height of each row.

        /* Put the Tree in a scroller. */
        JScrollPane sp = new JScrollPane
            (ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
             ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        sp.setPreferredSize(new Dimension(300, 300));
        sp.getViewport().add(tree);
        getContentPane().add(sp, BorderLayout.CENTER);

        getContentPane().add(buildButtonPanel(okayLabel), BorderLayout.SOUTH);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        this.approver = approver;

        pack();
        show();      // this will block until the user dismisses the dialog
    }

    private Component buildButtonPanel(String okayLabel) {
        JPanel result = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));

        JButton cancelButton = new JButton(Resources.getGlobalBundle().getString("Cancel"));
        cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dispose(); }});
        result.add(cancelButton);

        if (okayLabel == null) okayLabel = Resources.getGlobalBundle().getString("OK");
        okayButton = new JButton(okayLabel);
        okayButton.addActionListener(this);
        okayButton.setEnabled(false);
        result.add(okayButton);

        return result;
    }

    public PropertyKey getSelectedKey() { return selectedKey; }
    public String getSelectedPath() { return selectedPath; }


    // This routine is called when the okay button is pressed.
    public void actionPerformed(ActionEvent e) {
        selectedKey  = currentlySelectedKey;
        selectedPath = currentlySelectedPath;
        dispose();
    }


    // Returns the TreeNode instance that is selected in the tree.
    // If nothing is selected, null is returned.
    protected DefaultMutableTreeNode getSelectedNode() {
        TreePath   selPath = tree.getSelectionPath();

        if (selPath != null)
            return (DefaultMutableTreeNode)selPath.getLastPathComponent();
        return null;
    }

    /**
     * The next method implement the TreeSelectionListener interface
     * to deal with changes to the tree selection.
     */
    public void valueChanged (TreeSelectionEvent e) {
        TreePath selPath = e.getNewLeadSelectionPath();

        if (selPath == null) {           // deselection
            okayButton.setEnabled(false);
            currentlySelectedPath = null;
            currentlySelectedKey = null;
        } else {
            // calculate the path and PropertyKey for this selection
            Object[] pathItems = selPath.getPath();
            StringBuffer path = new StringBuffer();
            for (int i = 1;  i < pathItems.length;  i++)
                path.append("/").append(pathItems[i]);
            currentlySelectedPath = path.toString();
            currentlySelectedKey = PropertyKey.fromPath(currentlySelectedPath);

            // possibly enable/disable the okay button.
            okayButton.setEnabled
                (approver == null ||
                 approver.selectionIsAcceptable(currentlySelectedKey,
                                                currentlySelectedPath));
        }
    }

}
