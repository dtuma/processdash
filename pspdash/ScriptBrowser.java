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

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import java.io.*;
import java.net.URLEncoder;
import javax.swing.tree.*;
import javax.swing.text.JTextComponent;


public class ScriptBrowser extends JDialog
    implements TreeSelectionListener, ListSelectionListener, ActionListener
{
    /** Class Attributes */
    //protected JFrame          frame;
    protected JTree           tree;
    protected PropTreeModel   treeModel;
    protected PSPProperties   useProps;
    protected PSPDashboard    dashboard = null;
    protected DefaultListModel scriptList = null;
    protected JButton         displayButton = null;
    protected JList           list;
    //  protected JList

    protected JButton editButton, deleteButton, closeButton;

    ResourceBundle resources = Resources.getBundle("pspdash.PSPDashboard");


    //
    // member functions
    //
    private static void debug(String msg) {
        System.out.println("ScriptBrowser:" + msg);
    }


                                // constructor
    public ScriptBrowser(PSPDashboard dash, boolean showCurrent) {
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
        panel.add("Center", new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                           sp, sp2));

        Box buttonBox = new Box(BoxLayout.X_AXIS);
        buttonBox.add (Box.createGlue());
        displayButton = new JButton (Resources.getString("Display"));
        displayButton.setActionCommand("display");
        displayButton.addActionListener(this);
        buttonBox.add (displayButton);
        displayButton.setEnabled(false);
        buttonBox.add (Box.createGlue());
        JButton button = new JButton (Resources.getString("Close"));
        button.setActionCommand("close");
        button.addActionListener(this);
        buttonBox.add (button);
        buttonBox.add (Box.createGlue());
        panel.add("South", buttonBox);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        if (showCurrent) {
            PropertyKey currentPhase = dash.getCurrentPhase();
            TreePath path = new TreePath
                (treeModel.getPathToKey(useProps, currentPhase));
            tree.makeVisible(path);
            tree.getSelectionModel().addSelectionPath(path);
        }

        pack();
        show();
    }

    protected void showSelectedScript() {
        int selectedIndex = list.getMinSelectionIndex();
        if (selectedIndex == -1) return;
        ScriptID id = (ScriptID) scriptList.elementAt(selectedIndex);
        id.display();
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

        Vector scripts = useProps.getScriptIDs(key);
        if (scripts == null || scripts.size() == 0) return;

        ScriptID script, defaultScript = (ScriptID) scripts.elementAt(0);

        String dataPath = defaultScript.getDataPath();
        for (int i=1;  i < scripts.size();  i++) {
            script = (ScriptID) scripts.elementAt(i);
            if (dataPath != null && !dataPath.equals(script.getDataPath()))
                break;
            scriptList.addElement(script);
            dataPath = script.getDataPath();
            if (defaultScript.scriptEquals(script))
                list.getSelectionModel().addSelectionInterval(i-1, i-1);
        }
    }

}
