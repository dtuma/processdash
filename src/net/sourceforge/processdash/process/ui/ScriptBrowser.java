// Copyright (C) 2001-2011 Tuma Solutions, LLC
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


package net.sourceforge.processdash.process.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.hier.ui.PropTreeModel;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.process.ScriptEnumerator;
import net.sourceforge.processdash.process.ScriptID;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.ui.lib.MultiWindowCheckboxIcon;


public class ScriptBrowser extends JDialog
    implements TreeSelectionListener, ListSelectionListener, ActionListener
{
    /** Class Attributes */
    //protected JFrame          frame;
    protected JTree           tree;
    protected PropTreeModel   treeModel;
    protected DashHierarchy   useProps;
    protected ProcessDashboard    dashboard = null;
    protected DefaultListModel scriptList = null;
    protected JButton         displayButton = null;
    protected JList           list;
    protected boolean keepDialogOpen = false;
    //  protected JList

    protected JButton editButton, deleteButton, closeButton;

    Resources resources = Resources.getDashBundle("ProcessDashboard");


    //
    // member functions
    //


                                  // constructor
    public ScriptBrowser(ProcessDashboard dash, boolean showCurrent) {
        super(dash);
        setTitle(resources.getString("Script_Browser_Window_Title"));

        dashboard = dash;
        useProps  = dash.getProperties();

        JPanel panel = new JPanel(true);
        getContentPane().add("Center", panel);
        PCSH.enableHelpKey(this, "AccessingScripts.scriptBrowser");

        /* Create the JTreeModel. */
        treeModel = new PropTreeModel (new DefaultMutableTreeNode ("root"), null);

        /* Create the tree. */
        tree = new JTree(treeModel);
        treeModel.fill (useProps);
        tree.expandRow (0);
        tree.setShowsRootHandles (true);
        tree.setEditable(false);
        tree.getSelectionModel().setSelectionMode
            (TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeSelectionListener (this);
        tree.setRootVisible(false);
        tree.setRowHeight(-1);      // Make tree ask for the height of each row.

        /* Put the Tree in a scroller. */
        JScrollPane sp = new JScrollPane
            (ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
             ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        sp.setPreferredSize(new Dimension(300, 300));
        sp.getViewport().add(tree);

        /* Create the list to display scripts and forms */
        scriptList = new DefaultListModel();
        list = new JList(scriptList);
        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)
                    showSelectedScript();
                } } );
        list.getSelectionModel().addListSelectionListener(this);
        JScrollPane sp2 = new JScrollPane();
        sp2.getViewport().add(list);

        /* And show it. */
        panel.setLayout(new BorderLayout());
        panel.add(new MultiLabel(), BorderLayout.NORTH);
        panel.add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sp, sp2),
            BorderLayout.CENTER);

        Box buttonBox = new Box(BoxLayout.X_AXIS);
        buttonBox.add (Box.createGlue());
        displayButton = new JButton (resources.getString("Display"));
        displayButton.setActionCommand("display");
        displayButton.addActionListener(this);
        buttonBox.add (displayButton);
        displayButton.setEnabled(false);
        buttonBox.add (Box.createGlue());
        JButton button = new JButton (resources.getString("Close"));
        button.setActionCommand("close");
        button.addActionListener(this);
        buttonBox.add (button);
        buttonBox.add (Box.createGlue());
        panel.add(buttonBox, BorderLayout.SOUTH);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        if (showCurrent) {
            PropertyKey currentPhase = dash.getCurrentPhase();
            TreePath path = new TreePath
                (treeModel.getPathToKey(useProps, currentPhase));
            tree.makeVisible(path);
            tree.scrollPathToVisible(path);
            tree.getSelectionModel().addSelectionPath(path);
        }

        pack();
        setVisible(true);
    }

    protected void showSelectedScript() {
        int selectedIndex = list.getMinSelectionIndex();
        if (selectedIndex == -1) return;
        ScriptID id = (ScriptID) scriptList.elementAt(selectedIndex);
        id.display();
        if (!keepDialogOpen)
            dispose();
    }


    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();

        if (cmd.equals("close")) {
            dispose();
        } else if (cmd.equals("display")) {
            showSelectedScript();
        }
    }

    public void valueChanged(ListSelectionEvent e) {
        displayButton.setEnabled(!list.getSelectionModel().isSelectionEmpty());
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
        TreePath tp = e.getNewLeadSelectionPath();

        if (tp == null)           // deselection
            tree.clearSelection();

        applyFilter ();
    }

    private void applyFilter() {
        list.getSelectionModel().clearSelection();
        scriptList.clear();

        DefaultMutableTreeNode selected = getSelectedNode();
        if (selected == null) return;

        PropertyKey key = treeModel.getPropKey (useProps, selected.getPath());
        if (key == null) return;

        List<ScriptID> scripts = ScriptEnumerator.getScripts(dashboard, key);
        if (scripts == null || scripts.size() == 0) return;

        ScriptID script, defaultScript = scripts.get(0);

        String dataPath = defaultScript.getDataPath();
        for (int i=1;  i < scripts.size();  i++) {
            script = scripts.get(i);
            if (dataPath != null && !dataPath.equals(script.getDataPath()))
                break;
            scriptList.addElement(script);
            dataPath = script.getDataPath();
            if (defaultScript.scriptEquals(script))
                list.getSelectionModel().addSelectionInterval(i-1, i-1);
        }
    }

    private class MultiLabel extends JLabel implements MouseListener {
        MultiWindowCheckboxIcon icon;
        public MultiLabel() {
            super(" ", SwingConstants.RIGHT);
            setIcon(icon = new MultiWindowCheckboxIcon());
            setHorizontalTextPosition(SwingConstants.RIGHT);
            setToolTipText(resources.getString("Script_Browser_Keep_Open"));
            addMouseListener(this);
        }

        public void mouseClicked(MouseEvent e) {
            keepDialogOpen = !keepDialogOpen;
            icon.setChecked(keepDialogOpen);
            repaint();
        }

        public void mouseEntered(MouseEvent e) {}
        public void mouseExited(MouseEvent e) {}
        public void mousePressed(MouseEvent e) {}
        public void mouseReleased(MouseEvent e) {}
    }

}
