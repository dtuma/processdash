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

import javax.swing.*;
import javax.swing.event.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.util.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.tree.*;


public class PropertyFrame extends Object implements TreeModelListener, TreeSelectionListener, ItemListener
{
    /** Class Attributes */
    protected JFrame        frame;
    protected JTree         tree;
    protected PropTreeModel treeModel;
    protected PSPProperties templates;
    protected PSPProperties readProps;
    protected PSPProperties useProps;
    protected PSPDashboard  dashboard = null;


    static final char NO_MOVE_CHAR    = 'M';
    static final char NO_EDIT_CHAR    = 'E';
    static final char ALLOWED_CHILD   = '<';
    static final char REQUIRED_PARENT = '>';

    protected Vector    pendingVector = null;
    protected JMenu     addTemplateMenu;  // Memorable menu[item]s
    protected JMenu     addNodeMenu;
    protected JMenuItem saveMenuItem;
    protected JMenuItem revertMenuItem;
    protected JMenuItem deleteMenuItem;
    protected JMenuItem addNodeAboveMenuItem;
    protected JMenuItem addNodeBelowMenuItem;
    protected JMenuItem addNodeChildMenuItem;


    //
    // member functions
    //
    private void debug(String msg) {
        System.out.println(msg);
    }


    public void itemStateChanged(ItemEvent e) {
        addPendingChange (e.getItem());
    }


    public void addPendingChange (Object a) {
        if (pendingVector == null)
            pendingVector = new Vector();

        PendingDataChange newChange, existingChange;
        if (a instanceof PendingDataChange) {
            newChange = (PendingDataChange) a;

            if (newChange.changeType == PendingDataChange.CREATE ||
                newChange.oldPrefix == null)
                pendingVector.addElement(newChange);

            else {                    // step backward through changes
                for (int i = pendingVector.size(); i-- > 0; ) {
                    existingChange = (PendingDataChange) pendingVector.elementAt(i);

                                          // if we find an existing change whose
                                          // "final prefix" (newPrefix) matches the
                                          // "initial prefix" (oldPrefix) of this change,
                    if (newChange.oldPrefix.equals(existingChange.newPrefix)) {

                                          // merge the two changes.
                        if (newChange.changeType == PendingDataChange.CHANGE) {
                            existingChange.newPrefix = newChange.newPrefix;
                            if (existingChange.changeType == PendingDataChange.CHANGE &&
                                existingChange.oldPrefix.equals(existingChange.newPrefix))
                                pendingVector.removeElementAt(i);
                            newChange = null;   break;
                        } else              // newChange.changeType is DELETE
                            if (existingChange.changeType == PendingDataChange.CREATE) {
                                pendingVector.removeElementAt(i);
                                newChange = null;   break;
                            } else {          // existingChange.changeType is CHANGE
                                newChange.oldPrefix = existingChange.oldPrefix;
                                pendingVector.setElementAt(newChange, i);
                                newChange = null;   break;
                            }
                    }
                }
                if (newChange != null)
                    pendingVector.addElement(newChange);
            }
        }
    }


                                // constructor
    public PropertyFrame(PSPDashboard dash,
                         ConfigureButton button,
                         PSPProperties props,
                         PSPProperties templates) {
        dashboard        = dash;
        JMenuBar menuBar = constructMenuBar();
        JPanel   panel   = new JPanel(true);

        this.templates = templates;
        readProps      = props;
        useProps       = new PSPProperties (props.dataPath);
        revertProperties ();
        updateTemplateMenu (null, null);

        frame = new JFrame("Hierarchy Editor");
        frame.setTitle("Hierarchy Editor");
        frame.setIconImage(java.awt.Toolkit.getDefaultToolkit().createImage
                           (getClass().getResource("icon32.gif")));
        frame.getContentPane().add("Center", panel);
        frame.setJMenuBar(menuBar);
        frame.setBackground(Color.lightGray);

        /* Create the JTreeModel. */
        treeModel = new PropTreeModel (new DefaultMutableTreeNode ("root"), this);

        /* Create the tree. */
        tree = new JTree(treeModel);
        treeModel.fill (useProps);
        tree.expandRow (0);
        tree.setShowsRootHandles (true);
        tree.setEditable(true);
        treeModel.useTreeModelListener (true);
        tree.addTreeSelectionListener (this);
        tree.setRootVisible(false);
        tree.setRowHeight(-1);      // Make tree ask for the height of each row.
        adjustMenu (false, true, false, null, null); // deselection case

        /* Put the Tree in a scroller. */
        JScrollPane sp = new JScrollPane
            (ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
             ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        sp.setPreferredSize(new Dimension(300, 300));
        sp.getViewport().add(tree);

        /* And show it. */
        panel.setLayout(new BorderLayout());
        panel.add("Center", sp);

        frame.addWindowListener( new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                frame.setVisible (false);
            }
        });

        frame.pack();
        frame.show();
        useProps.addItemListener (this);
    }

    public void saveProperties () {

        readProps.copy (useProps);
        setDirty (false);

        if (pendingVector != null) {
            String dataDir = dashboard.getDirectory();
            for (int i = 0; i < pendingVector.size(); i++)
                if (pendingVector.elementAt (i) instanceof PendingDataChange) {
                    PendingDataChange p = (PendingDataChange)pendingVector.elementAt (i);
                    switch (p.changeType) {
                    case PendingDataChange.CREATE:
                        if (p.srcFile == null)
                            createEmptyFile(dataDir + p.destFile);
                        else
                            createDataFile(dataDir + p.destFile, p.srcFile);

                        if (p.newPrefix != null)
                            dashboard.openDatafile (p.newPrefix, p.destFile);
                        break;

                    case PendingDataChange.DELETE:
                        dashboard.data.closeDatafile (p.oldPrefix);
                        break;

                    case PendingDataChange.CHANGE:
                        dashboard.data.renameData (p.oldPrefix, p.newPrefix);
                        break;
                    }
                }
            pendingVector.removeAllElements();
        }

        dashboard.refreshHierarchy();
    }

    public void revertProperties () {
        useProps.copy (readProps);
        setDirty (false);
        if (pendingVector != null)
            pendingVector.removeAllElements();
    }

    public void show() {
        frame.show();
    }

                                // make sure root is expanded
    public void expandRoot () { tree.expandRow (0); }

    /** Construct a menu. */
    private JMenuBar constructMenuBar() {
        JMenu            menu, subMenu;
        JMenuBar         menuBar = new JMenuBar();
        JMenuItem        menuItem;

        /* File Options (close, save, revert). */
        menu = new JMenu("File");
        menuBar.add(menu);

        menuItem = menu.add(new JMenuItem("Close"));
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                frame.setVisible (false);
            }});

        saveMenuItem = menu.add(new JMenuItem("Save"));
        saveMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveProperties ();
            }});

        revertMenuItem = menu.add(new JMenuItem("Revert"));
        revertMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                revertProperties ();    // reset properties
                                        // remove listener while reloading
                treeModel.useTreeModelListener (false);
                treeModel.reload (useProps); // remove children of root
                expandRoot ();
                treeModel.useTreeModelListener (true); // restore listener
            }});
        setDirty (false);

        /* Tree related stuff. */
        menu = new JMenu("Edit");
        menuBar.add(menu);

        deleteMenuItem = menu.add(new JMenuItem("Delete"));
        deleteMenuItem.addActionListener(new RemoveAction());
        deleteMenuItem.setEnabled (false);

        menu.addSeparator();

        addNodeMenu = (JMenu) menu.add(new JMenu("Add Node"));
        addNodeMenu.setPopupMenuVisible (false);

        addNodeAboveMenuItem = addNodeMenu.add(new JMenuItem("Above"));
        addNodeAboveMenuItem.addActionListener(new InsertAction());

        addNodeBelowMenuItem = addNodeMenu.add(new JMenuItem("Below"));
        addNodeBelowMenuItem.addActionListener(new AddAction());

        addNodeChildMenuItem = addNodeMenu.add(new JMenuItem("As Child"));
        addNodeChildMenuItem.addActionListener(new AddChildAction());

        addTemplateMenu = (JMenu) menu.add(new JMenu("Add Template"));
        addTemplateMenu.setPopupMenuVisible (false);

        return menuBar;
    }

    /**
     * Returns the TreeNode instance that is selected in the tree.
     * If nothing is selected, null is returned.
     */
    protected DefaultMutableTreeNode getSelectedNode() {
        TreePath   selPath = tree.getSelectionPath();

        if(selPath != null)
            return (DefaultMutableTreeNode)selPath.getLastPathComponent();
        return null;
    }

    protected void setDirty (boolean isDirty) {
        saveMenuItem.setEnabled (isDirty);
        revertMenuItem.setEnabled (isDirty);
    }

    protected void adjustMenu (boolean siblings,
                               boolean children,
                               boolean editable,
                               Vector  templateChildren,
                               String  myID) {
        addNodeMenu.setPopupMenuVisible (children || siblings);
        addNodeAboveMenuItem.setEnabled (siblings);
        addNodeBelowMenuItem.setEnabled (siblings);
        addNodeChildMenuItem.setEnabled (children);
        tree.setEditable(editable);
        if (templateChildren != null && templateChildren.size() == 0)
            addTemplateMenu.setPopupMenuVisible (false);
        else
            addTemplateMenu.setPopupMenuVisible (true);

        updateTemplateMenu (templateChildren, myID);
    }

    /**
     * The next method implement the TreeSelectionListener interface
     * to deal with changes to the tree selection.
     */
    public void valueChanged (TreeSelectionEvent e) {
        TreePath tp = e.getNewLeadSelectionPath();

        if (tp == null) {           // deselection
            deleteMenuItem.setEnabled (false);
            adjustMenu (false, true, false, null, null);
            return;
        }
        Object [] path = tp.getPath();
        PropertyKey key = treeModel.getPropKey (useProps, path);

        // Place code to update selection-sensitive field(s) here.
        Prop val = useProps.pget (key);

                                    // enable if deleteable, else...
        if (path.length > 1) {
            deleteMenuItem.setEnabled (true);
        } else {                    // top two levels (root & 1st sub) static
            deleteMenuItem.setEnabled (false);
        }

        String status = val.getStatus();
        if (status == null)
            status = "";

        int     parseIndex      = 0;
        boolean moveable        = true;
        boolean editable        = true;
        boolean allowsSiblings  = true;
        boolean allowsChildren  = true;
        Vector  allowedChildren = null;

        if ((status.length() > 0) && (status.charAt (0) == NO_MOVE_CHAR)) {
            moveable = false;
            parseIndex++;
            if ((status.length() > 1) && (status.charAt (1) == NO_EDIT_CHAR)) {
                editable = false;
                parseIndex++;
            }
        } else if ((status.length() > 0) && (status.charAt (0) == NO_EDIT_CHAR)) {
            editable = false;
            parseIndex++;
        }

        String pStatus = useProps.pget(key.getParent()).getStatus();
        if ((pStatus != null) && (pStatus.indexOf(ALLOWED_CHILD) >= 0))
            allowsSiblings = false;
        if ((parseIndex = status.indexOf (ALLOWED_CHILD)) >= 0) {
            allowsChildren = false;   // can only add specified templates
            allowedChildren = new Vector(); // non-null implies REQUIRED match

            int lastChar = status.indexOf (REQUIRED_PARENT);
            if (lastChar < 0)
                lastChar = status.length();
            if (lastChar > parseIndex + 1) {
                                      // at least one allowed, make list...
                StringTokenizer st = new StringTokenizer
                    (status.substring (parseIndex + 1, lastChar),
                     String.valueOf (ALLOWED_CHILD));
                String sDebug;
                int endIndex;
                while (st.hasMoreElements()) {
                    sDebug = st.nextToken();
                    endIndex = sDebug.indexOf ("(");
                    if (endIndex < 0)
                        endIndex = sDebug.length();
                    allowedChildren.addElement (sDebug.substring (0, endIndex));
//        System.out.println("Allowing Template " +
//                           sDebug.substring (0, endIndex));
                }
            }
        }

        adjustMenu (allowsSiblings, allowsChildren, editable,
                    allowedChildren, val.getID());
    }

    public void updateTemplateMenu(Vector tList,
                                   String id) {
        Prop      p;
        int       idx;
        JMenuItem menuItem;
        String    val;
        PropertyKey tKey = PropertyKey.ROOT;

        addTemplateMenu.removeAll(); // clear the JMenu

//    System.out.println("Update:" + id);
        for (int ii = 0; ii < templates.getNumChildren (tKey); ii++) {
            p = templates.pget (templates.getChildKey (tKey, ii));
                                      // ensure tList includes the current ID
                                      // (current template is an allowed child)
//      System.out.println("Update: testing " +p.getID());
            if ((tList != null) && (! tList.contains (p.getID())))
                continue;

//      System.out.println("Update:  test2 " +p.getStatus());
            val = p.getStatus();
            if ((val != null) && ((idx = val.indexOf (REQUIRED_PARENT)) >= 0)) {
                                      //check for reqd parent
                if (id == null)
                    continue;
                boolean found = false;
                StringTokenizer st = new StringTokenizer
                    (val.substring (idx + 1), String.valueOf (REQUIRED_PARENT));
                while (st.hasMoreElements() && !found)
                    found = id.equals (st.nextElement());
                if (!found)
                    continue;
            }
            val = templates.getChildName (tKey, ii);
//      System.out.println("Update: passed " + val);
            menuItem = addTemplateMenu.add(new JMenuItem(val));
            menuItem.addActionListener(new AddTemplateAction());
        }
    }

    /**
     * The next four methods implement the TreeModelListener interface
     * to deal with changes to the tree.
     */
    public void treeNodesChanged (TreeModelEvent e) { // name change
        Object [] path = e.getPath();
        int [] indices = e.getChildIndices();
        Object [] children = e.getChildren();
        PropertyKey parent = treeModel.getPropKey (useProps, path);

        for (int i = 0; i < indices.length; i++) {
            useProps.setChildKey (parent,
                                  children [i].toString(),
                                  indices[i]);
            // Update node name or selection sensitive data here
        }
        setDirty (true);
    }

    public void treeNodesInserted (TreeModelEvent e) {
        // debug ("treeNodesInserted:"+e.toString());
        Object [] path = e.getPath();
        int [] indices = e.getChildIndices();
        Object [] children = e.getChildren();

        PropertyKey parent = treeModel.getPropKey (useProps, path);
        debug (((path == null) ? "null" : path.toString()) + "=>" +
               ((parent == null) ? "null" : parent.toString()));
        for (int nodeIdx = 0; nodeIdx < indices.length; nodeIdx++) {
            useProps.addChildKey (parent,
                                  children [nodeIdx].toString(),
                                  indices[nodeIdx]);
        }
        setDirty (true);
    }

    public void treeNodesRemoved (TreeModelEvent e) {
        Object [] path = e.getPath();
        int [] indices = e.getChildIndices();
        PropertyKey parent = treeModel.getPropKey (useProps, path);

                                    // does not yet deal WELL with mult nodes
                                    // (haven't seen mult nodes yet, either...)
        for (int nodeIdx = 0; nodeIdx < indices.length; nodeIdx++) {
            useProps.removeChildKey (parent, indices [nodeIdx]);
        }
        setDirty (true);
    }

    public void treeStructureChanged (TreeModelEvent e) {
//    System.out.println ("PropertyTreeModelListener.treeStructureChanged");
//    System.out.println (e.toString());
    }


    public void setStatusRecursive (PropertyKey key, String status) {
        Prop val = useProps.pget (key);
        val.setStatus (status);
        useProps.put (key, val);
        for (int ii = 0; ii < val.getNumChildren(); ii++)
            setStatusRecursive (val.getChild (ii), status);
    }


    public static void copyFile (String dest, String src) {
        BufferedReader in;
        BufferedWriter out;
        try {
            in  = new BufferedReader(new FileReader(src));
        } catch (IOException e) { return; }
        try {
            File parentDir = new File(new File(dest).getParent());
            if (!parentDir.isDirectory())
                parentDir.mkdirs();
            out = new BufferedWriter (new FileWriter (dest));
        } catch (IOException e) {
            try { in.close(); } catch (IOException e2) {}
            return;
        }
        String line;

        try {
            while ((line = in.readLine()) != null) {
                out.write (line);
                out.newLine ();
            }
        } catch (IOException e) {}
        try { in.close();  } catch (IOException e) {}
        try { out.close(); } catch (IOException e) {}
    }


    public static void createDataFile (String dest, String src) {
        BufferedWriter out;
        try {
            File parentDir = new File(new File(dest).getParent());
            if (!parentDir.isDirectory())
                parentDir.mkdirs();
            out = new BufferedWriter (new FileWriter (dest));
            out.write("#include <" + src + ">");
            out.newLine();
            out.close();
        } catch (IOException e) {}
    }


    public static void createEmptyFile (String dest) {
        BufferedWriter out;
        try {
            File parentDir = new File(new File(dest).getParent());
            if (!parentDir.isDirectory())
                parentDir.mkdirs();
            out = new BufferedWriter (new FileWriter (dest));
            // out.newLine();
            // out.flush();
            out.close();
        } catch (IOException e) {}
    }


    public void copyTemplate (DefaultMutableTreeNode destParent,
                              String                 templateName) {
        //recursive copy of node, children and properties
        PropertyKey parent = treeModel.getPropKey (useProps,
                                                   destParent.getPath ());
        PropertyKey templateKey = new PropertyKey (PropertyKey.ROOT,
                                                   templateName);
        int newIndex = useProps.getNumChildren (parent);

                                    // See if should be adding at other index...

                                    // if parent specifies allowed children
        Prop val = useProps.pget (parent);
        String status, allowedChild;
        if ((val != null) && ((status = val.getStatus()) != null)) {
            int idx1 = status.indexOf (ALLOWED_CHILD);
            int idx2 = status.indexOf (REQUIRED_PARENT);
            if (idx1 >= 0) {
                if (idx2 < 0)
                    idx2 = status.length();
                StringTokenizer st = new StringTokenizer
                    (status.substring (idx1 + 1, idx2), String.valueOf (ALLOWED_CHILD));
                while (st.hasMoreTokens()) {
                    allowedChild = st.nextToken();
                                          // if parent specifies THIS child
                    if (allowedChild.startsWith (templateName)) {
                        idx1 = allowedChild.indexOf("(");
                        idx2 = allowedChild.indexOf(")");
                                            // if parent specifies index
                        if (idx1 >= 0 && idx2 >= 0) {
                                            // change index
                            idx1 = Integer.valueOf (allowedChild.substring
                                                    (idx1 + 1, idx2)).intValue();
                            newIndex = ((idx1 < 0) ? (newIndex + idx1) : idx1);
                        }
                        break;              // exit while loop
                    }
                }
            }
        }

                                    // now add it
        useProps.addChildKey (parent,
                              useProps.pget(parent).uniqueChildName(templateName),
                              newIndex);
        useProps.copyFrom (templates,
                           templateKey,
                           useProps.getChildKey (parent, newIndex));
                                    // clear and reload the tree (NEEDS WORK)
        treeModel.useTreeModelListener (false);
        treeModel.reload (useProps);
        expandRoot ();
        treeModel.useTreeModelListener (true);
    }


    private boolean isUniqueChildName (String s, Enumeration kids) {
        while (kids.hasMoreElements())
            if (kids.nextElement().toString().equals (s))
                return false;
        return true;
    }

    public String newCName (DefaultMutableTreeNode parent) {
        String aName = "new";
        Enumeration kids = parent.children();
        while ( !isUniqueChildName (aName, parent.children())) {
            aName = kids.nextElement().toString() + "1";
        }
        return aName;
    }


    /**
     * AddAction is used to add a new item after the selected item.
     */
    class AddAction extends Object implements ActionListener {
        /**
         * Messaged when the user clicks on the "Add node above" menu item.
         * Determines the selection from the Tree and adds an item
         * after that.  If nothing is selected, an item is added to
         * the root.
         */
        public void actionPerformed(ActionEvent e) {
            int                    newIndex;
            DefaultMutableTreeNode lastItem = getSelectedNode();
            DefaultMutableTreeNode parent;

            /* Determine where to create the new node. */
            if(lastItem != null) {
                parent = (DefaultMutableTreeNode)lastItem.getParent();
                newIndex = parent.getIndex(lastItem) + 1;
            } else {
                parent = (DefaultMutableTreeNode)treeModel.getRoot();
                newIndex = treeModel.getChildCount(parent);
            }

            /* Let the treemodel know. */
            treeModel.insertNodeInto(new DefaultMutableTreeNode (newCName (parent)),
                                     parent, newIndex);
            if ( ! tree.isExpanded (0))
                tree.expandRow(0);

            setDirty (true);
        }
    } // End of PropertyFrame.AddAction


    /**
     * AddChildAction is used to add a new item as a child of the selected item.
     */
    class AddChildAction extends Object implements ActionListener {
        /** Number of nodes that have been added. */
        public int               addCount;

        /**
         * Messaged when the user clicks on the "Add node as child" menu item.
         * Determines the selection from the Tree and adds an item
         * after that.  If nothing is selected, an item is added to
         * the root.
         */
        public void actionPerformed(ActionEvent e) {
            int                    newIndex;
            DefaultMutableTreeNode parent;
            try { parent = getSelectedNode();
                } catch (Exception e1) { parent = null; }

            /* Determine where to create the new node. */
            if(parent == null)
                parent = (DefaultMutableTreeNode)treeModel.getRoot();
            newIndex = treeModel.getChildCount(parent);

            /* Let the treemodel know. */
            treeModel.insertNodeInto(new DefaultMutableTreeNode (newCName (parent)),
                                     parent, newIndex);
            if ( ! tree.isExpanded (0))
                tree.expandRow(0);

            setDirty (true);
        }
    } // End of PropertyFrame.AddChildAction


    /**
     * InsertAction is used to insert a new item before the selected item.
     */
    class InsertAction extends Object implements ActionListener {
        /** Number of nodes that have been added. */
        public int               insertCount;

        /**
         * Messaged when the user clicks on the "Add node above" menu item.
         * Determines the selection from the Tree and inserts an item
         * before that.  If nothing is selected, an item is added to
         * the root.
         */
        public void actionPerformed(ActionEvent e) {
            int                    newIndex;
            DefaultMutableTreeNode lastItem = getSelectedNode();
            DefaultMutableTreeNode parent;

            /* Determine where to create the new node. */
            if(lastItem != null) {
                parent = (DefaultMutableTreeNode)lastItem.getParent();
                newIndex = parent.getIndex(lastItem);
            } else {
                parent = (DefaultMutableTreeNode)treeModel.getRoot();
                newIndex = treeModel.getChildCount(parent);
            }

            /* Let the treemodel know. */
            treeModel.insertNodeInto(new DefaultMutableTreeNode (newCName (parent)),
                                     parent, newIndex);
            if ( ! tree.isExpanded (0))
                tree.expandRow(0);

            setDirty (true);
        }
    } // End of PropertyFrame.InsertAction


    /**
     * AddTemplateAction responds to the user selecting a template for
     * insertion as a child of the selected node.
     */
    class AddTemplateAction extends Object implements ActionListener {
        // Adds the specified template as a child of the selected item.
        public void actionPerformed(ActionEvent e) {
            DefaultMutableTreeNode          lastItem = getSelectedNode();

            if (lastItem != null) {
                String param = e.paramString ();
                String templateName = param.substring (param.indexOf ('=')+1);
                copyTemplate (lastItem, templateName);
            }
            if ( ! tree.isExpanded (0))
                tree.expandRow(0);

            setDirty (true);
        }
    } // End of PropertyFrame.AddTemplateAction


    /**
     * RemoveAction removes the selected node from the tree.  If
     * The root or nothing is selected nothing is removed.
     */
    class RemoveAction extends Object implements ActionListener {
        // Removes the selected item as long as it isn't root.
        public void actionPerformed(ActionEvent e) {
            DefaultMutableTreeNode lastItem = getSelectedNode();

            if(lastItem != null &&
                lastItem != (DefaultMutableTreeNode)treeModel.getRoot())
                treeModel.removeNodeFromParent(lastItem);
        }
    } // End of PropertyFrame.RemoveAction

}
