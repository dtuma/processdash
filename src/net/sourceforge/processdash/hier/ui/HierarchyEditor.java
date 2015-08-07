// Copyright (C) 1999-2015 Tuma Solutions, LLC
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


package net.sourceforge.processdash.hier.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.JTextComponent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.HierarchyAlterer;
import net.sourceforge.processdash.hier.PendingDataChange;
import net.sourceforge.processdash.hier.Prop;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.log.time.ModifiableTimeLog;
import net.sourceforge.processdash.log.time.PathRenamer;
import net.sourceforge.processdash.log.ui.DefectLogEditor;
import net.sourceforge.processdash.ui.ConfigureButton;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.ui.macosx.MacGUIUtils;


public class HierarchyEditor extends Object implements TreeModelListener, TreeSelectionListener, ItemListener
{

    public interface Listener {
        public void hierarchyEditorClosed(HierarchyEditor editor);
    };

    /** Class Attributes */
    protected JFrame        frame;
    protected JTree         tree;
    protected PropTreeModel treeModel;
    protected DashHierarchy templates;
    protected DashHierarchy readProps;
    protected DashHierarchy useProps;
    protected ProcessDashboard  dashboard = null;
    protected ConfigureButton configureButton;

    static final Resources resource = Resources.getDashBundle("HierarchyEditor");

    static final char NO_MOVE_CHAR    = 'M';
    static final char NO_EDIT_CHAR    = 'E';
    static final char DELETE_OK_CHAR  = 'D';
    static final char ALLOWED_CHILD   = '<';
    static final char REQUIRED_PARENT = '>';

    protected Vector    pendingVector = null;
    protected JMenu     addTemplateMenu;  // Memorable menu[item]s
    protected JMenu     addNodeMenu;
    protected JMenuItem saveMenuItem;
    protected JMenuItem revertMenuItem;
    protected JMenuItem deleteMenuItem;
    protected Action renameAction;
    protected Action moveUpAction;
    protected Action moveDownAction;
    protected Action cutAction;
    protected Action pasteAction;
    protected Action addNodeAboveAction;
    protected Action addNodeBelowAction;
    protected Action addNodeChildAction;

    protected DefaultMutableTreeNode cutNode = null;

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

            else {
                // check to see if this change will result in a new or renamed node
                // whose name matches one of the names originally present in the
                // hierarchy before modifications began.  If so, don't merge this
                // change with any other change, or we might clobber the original
                // data in the process.
                boolean foundPotentialClobber = false;
                if (newChange.newPrefix != null) {
                    for (Iterator i = pendingVector.iterator(); i.hasNext();) {
                        existingChange = (PendingDataChange) i.next();
                        if (newChange.newPrefix.equals(existingChange.oldPrefix)) {
                            foundPotentialClobber = true;
                            break;
                        }
                    }
                }

                // step backward through changes, looking for past changes that might
                // merge with this one to reduce the amount of data churn.  Examples:
                // a node created and then immediately renamed (a typical use case),
                // a node renamed several times (only the final name is relevant),
                // a node created then deleted (an effective no-op)
                if (foundPotentialClobber == false) {
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
                }
                if (newChange != null)
                    pendingVector.addElement(newChange);
            }
        }
    }


                                // constructor
    public HierarchyEditor(ProcessDashboard dash,
                         ConfigureButton button,
                         DashHierarchy props,
                         DashHierarchy templates) {
        dashboard        = dash;
        configureButton  = button;
        frame = new JFrame(resource.getString("HierarchyEditor"));
        JMenuBar menuBar = constructMenuBar();
        JPanel   panel   = new JPanel(true);

        this.templates = templates;
        readProps      = props;
        useProps       = new DashHierarchy (props.dataPath);
        revertProperties ();
        updateTemplateMenu (null, null);

        frame.setTitle(resource.getString("HierarchyEditor"));
        DashboardIconFactory.setWindowIcon(frame);
        JToolBar toolBar = buildToolBar();
        if (Settings.isReadWrite())
            frame.getContentPane().add("North", toolBar);
        frame.getContentPane().add("Center", panel);
        frame.setJMenuBar(menuBar);
        frame.setBackground(Color.lightGray);
        PCSH.enableHelpKey(frame, "UsingHierarchyEditor");

        /* Create the JTreeModel. */
        treeModel = new PropTreeModel (new DefaultMutableTreeNode ("root"), this);

        /* Create the tree. */
        tree = new JTree(treeModel);
        treeModel.fill (useProps);
        tree.expandRow (0);
        tree.setShowsRootHandles (true);
        tree.setEditable(Settings.isReadWrite());
        tree.setInvokesStopCellEditing(true);
        tree.getSelectionModel().setSelectionMode
            (TreeSelectionModel.SINGLE_TREE_SELECTION);
        treeModel.useTreeModelListener (true);
        tree.addTreeSelectionListener (this);
        tree.setRootVisible(false);
        tree.setRowHeight(-1);      // Make tree ask for the height of each row.
        try {
            tree.setCellEditor(new SelectingTreeEditor
                (tree, (DefaultTreeCellRenderer) tree.getCellRenderer()));
        } catch (ClassCastException cce) {}
        adjustMenu (false, true, false, null, null, null); // deselection case
        addTemplateMenu.setEnabled (false);

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
                confirmClose(true);
            }
        });
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        loadCustomDimensions();
        if (frameHeight == -1)
            frame.pack();
        else
            frame.setSize(new Dimension(frameWidth, frameHeight));
        frame.setVisible(true);
        useProps.addItemListener (this);
    }

    DashHierarchy oldProps = null;

    public void saveProperties () {
        if (Settings.isReadOnly())
            return;

        dashboard.getHierarchy().fireHierarchyWillChange();

        // FIXME_TIMELOG: dashboard.releaseTimeLogEntry(null);
        // if the user is running their timer while they perform
        // renaming operations below, could bad things happen?

        oldProps = new DashHierarchy(useProps.dataPath);
        oldProps.copy(readProps);
        readProps.copy (useProps);
        setDirty (false);

        int tasksToPerform = 1;
        if (pendingVector != null)
            tasksToPerform += pendingVector.size();

        createProgressDialog(tasksToPerform);

        // do work in a different thread - otherwise we steal time from
        // the awt event pump and nothing gets redrawn.
        Thread t = new Thread() {
            public void run() {
                try {
                    savePendingVector();
                } catch (Throwable t) { t.printStackTrace(); }
                closeProgressDialog();
            }
            };
        t.start();

        showProgressDialog();     // this call will block until complete.
    }

    protected void savePendingVector() {

        String dataDir = dashboard.getDirectory();
        dashboard.getData().startInconsistency();

        try {
            if (pendingVector != null) {
                for (int i = 0; i < pendingVector.size(); i++) {
                    if (pendingVector.elementAt (i) instanceof PendingDataChange) {
                        PendingDataChange p=(PendingDataChange)pendingVector.elementAt(i);
                        switch (p.changeType) {
                        case PendingDataChange.CREATE:
                            performDataCreate(dataDir, p);
                            break;

                        case PendingDataChange.DELETE:
                            performDataDelete(p);
                            break;

                        case PendingDataChange.CHANGE:
                            performDataRename(p);
                            break;
                        }
                    }
                    incrementProgressDialog();
                }
                pendingVector.removeAllElements();
            }

            HierarchyAlterer.updateNodesAndLeaves
                    (dashboard.getData(), useProps);
            incrementProgressDialog();

        } finally {
            dashboard.getData().finishInconsistency();
        }

        dashboard.getHierarchy().fireHierarchyChanged();
    }


    private void performDataCreate(String dataDir, PendingDataChange p) {
        if (DashHierarchy.EXISTING_DATAFILE.equals(p.srcFile))
            ;
        else if (p.srcFile == null)
            createEmptyFile(dataDir + p.destFile);
        else
            createDataFile(dataDir + p.destFile, p.srcFile, p.extraData);

        if (p.newPrefix != null)
            dashboard.openDatafile(p.newPrefix, p.destFile);
    }


    private void performDataDelete(PendingDataChange p) {
        dashboard.getData().closeDatafile(p.oldPrefix);
    }


    private void performDataRename(PendingDataChange p) {
        DefectLogEditor.rename(oldProps, useProps, p.oldPrefix, p.newPrefix,
                dashboard);
        ModifiableTimeLog timeLog = (ModifiableTimeLog) dashboard.getTimeLog();
        timeLog.addModification(PathRenamer.getRenameModification(p.oldPrefix,
                p.newPrefix));
        dashboard.getData().renameData(p.oldPrefix, p.newPrefix);
    }


    private JDialog progressDialog = null;
    private JProgressBar progressBar = null;

    private void createProgressDialog(int size) {
        progressDialog = new JDialog(frame, resource.getString("SavingChanges"), true);
        progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        progressDialog.getContentPane().add
            (new JLabel(resource.getString("SavingChangesDots")), BorderLayout.NORTH);
        progressDialog.getContentPane().add
            (progressBar = new JProgressBar(0, size), BorderLayout.CENTER);
        progressDialog.pack();
        progressDialog.setLocationRelativeTo(frame);
    }

    private void showProgressDialog() {
        if (progressDialog != null)
                progressDialog.setVisible(true);
    }

    private void incrementProgressDialog() {
        if (progressBar != null)
            // Need to use SwingUtilities.invokeAndWait();
            progressBar.setValue(progressBar.getValue() + 1);
    }

    private void closeProgressDialog() {
        if (progressDialog != null)
            // Need to use SwingUtilities.invokeAndWait();
            progressDialog.dispose();
        progressDialog = null;
        progressBar = null;
    }

    public void revertProperties () {
        useProps.copy (readProps);
        setDirty (false);
        if (pendingVector != null)
            pendingVector.removeAllElements();
        cutNode = null;
    }

    public void show() {
        frame.setVisible(true);
        frame.toFront();
    }

    private static final Object CONFIRM_CLOSE_MSG =
        resource.getString("HierarchyChangeConfirm");
    public void confirmClose(boolean showCancel) {
        if (isDirty() && Settings.isReadWrite())
            switch (JOptionPane.showConfirmDialog
                    (frame, CONFIRM_CLOSE_MSG, resource.getString("SaveChanges"),
                     showCancel ? JOptionPane.YES_NO_CANCEL_OPTION
                                : JOptionPane.YES_NO_OPTION)) {
            case JOptionPane.CLOSED_OPTION:
            case JOptionPane.CANCEL_OPTION:
                return;                 // do nothing and abort.

            case JOptionPane.YES_OPTION:
                saveProperties();       // save changes.
            }

        dispose();                  // close the hierarchy editor window.
    }

    public void dispose() {
        saveCustomDimensions();
        frame.setVisible(false);
        frame.dispose();
        fireHierarchyEditorClosed();
        frame = null;
        tree = null;
        treeModel = null;
        templates = readProps = useProps = null;
        dashboard = null;
        configureButton = null;
        pendingVector = null;
        addTemplateMenu = addNodeMenu = null;
        saveMenuItem = revertMenuItem = deleteMenuItem = null;
        moveUpAction = moveDownAction = cutAction = pasteAction =
            addNodeAboveAction = addNodeBelowAction = addNodeChildAction = null;
    }

    private static final String DIMENSION_SETTING_NAME =
        "hierarchyEditor.dimensions";
    private int frameWidth, frameHeight = -1;
    private void loadCustomDimensions() {
        String setting = Settings.getVal(DIMENSION_SETTING_NAME);
        if (setting != null && setting.length() > 0) try {
            StringTokenizer tok = new StringTokenizer(setting, ",");
            frameWidth = Integer.parseInt(tok.nextToken());
            frameHeight = Integer.parseInt(tok.nextToken());
        } catch (Exception e) {}
        if (frameHeight == -1) {
            frameWidth = frameHeight = -1;
        }
    }
    private void saveCustomDimensions() {
        frameWidth = frame.getSize().width;
        frameHeight = frame.getSize().height;
        InternalSettings.set(DIMENSION_SETTING_NAME,
                             frameWidth + "," + frameHeight);
    }

                                // make sure root is expanded
    public void expandRoot () {
        tree.expandPath (new TreePath(treeModel.getRoot()));
    }

    /** Construct a menu. */
    private JMenuBar constructMenuBar() {
        JMenu            menu;
        JMenuBar         menuBar = new JMenuBar();
        JMenuItem        menuItem;

        /* File Options (close, save, revert). */
        menu = new JMenu(resource.getString("File"));
        menuBar.add(menu);

        menuItem = menu.add(new JMenuItem(resource.getString("Close")));
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                confirmClose(true);
            }});

        saveMenuItem = new JMenuItem(resource.getString("Save"));
        if (Settings.isReadWrite())
            menu.add(saveMenuItem);
        saveMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveProperties ();
            }});

        revertMenuItem = new JMenuItem(resource.getString("Revert"));
        if (Settings.isReadWrite())
            menu.add(revertMenuItem);
        revertMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tree.getSelectionModel().clearSelection();
                revertProperties ();    // reset properties
                                        // remove listener while reloading
                treeModel.useTreeModelListener (false);
                treeModel.reload (useProps); // remove children of root
                expandRoot ();
                treeModel.useTreeModelListener (true); // restore listener
            }});
        setDirty (false);

        /* Tree related stuff. */
        menu = new JMenu(resource.getString("Edit"));
        if (Settings.isReadWrite())
            menuBar.add(menu);

        deleteMenuItem = menu.add(new JMenuItem(resource.getString("Delete")));
        deleteMenuItem.addActionListener(new RemoveAction());
        deleteMenuItem.setEnabled (false);

        menu.add(renameAction = new RenameAction());
        renameAction.setEnabled(false);

        menu.add(moveUpAction = new MoveUpAction());
        moveUpAction.setEnabled(false);

        menu.add(moveDownAction = new MoveDownAction());
        moveDownAction.setEnabled(false);

        menu.add(cutAction = new CutAction());
        cutAction.setEnabled(false);

        menu.add(pasteAction = new PasteAction());
        pasteAction.setEnabled(false);

        menu.addSeparator();

        addNodeMenu = (JMenu) menu.add(new JMenu(resource.getString("HierarchyAddNode")));
        addNodeMenu.setPopupMenuVisible (false);

        addNodeMenu.add(addNodeAboveAction = new InsertAction());
        addNodeMenu.add(addNodeBelowAction = new AddAction());
        addNodeMenu.add(addNodeChildAction = new AddChildAction());

        addTemplateMenu = (JMenu) menu.add(new JMenu(resource.getString("HierarchyAddTemplate")));
        addTemplateMenu.setPopupMenuVisible (false);

        return menuBar;
    }

    private JToolBar buildToolBar() {
        JToolBar result = new JToolBar();
        result.setFloatable(false);
        result.setMargin(new Insets(0,0,0,0));

        addToolbarButton(result, cutAction);
        addToolbarButton(result, pasteAction);
        result.addSeparator();
        addToolbarButton(result, moveUpAction);
        addToolbarButton(result, moveDownAction);
        result.addSeparator();
        addToolbarButton(result, addNodeAboveAction);
        addToolbarButton(result, addNodeBelowAction);
        addToolbarButton(result, addNodeChildAction);

        return result;
    }

    private void addToolbarButton(JToolBar toolBar, Action a) {
        JButton button = new JButton(a);
        button.setFocusPainted(false);
        button.setText(null);
        toolBar.add(button);
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

    protected boolean dirtyFlag = false;
    protected boolean isDirty() { return dirtyFlag; }
    protected void setDirty (boolean isDirty) {
        dirtyFlag = isDirty;
        saveMenuItem.setEnabled (isDirty);
        revertMenuItem.setEnabled (isDirty);
        MacGUIUtils.setDirty(frame, isDirty);
    }

    protected void adjustMenu (boolean siblings,
                               boolean children,
                               boolean editable,
                               Vector  templateChildren,
                               String  myID,
                               String myPath) {
        addNodeMenu.setPopupMenuVisible (children || siblings);
        addNodeMenu.setEnabled(children || siblings);
        addNodeAboveAction.setEnabled (siblings);
        addNodeBelowAction.setEnabled (siblings);
        addNodeChildAction.setEnabled (children);
        tree.setEditable(editable && Settings.isReadWrite());
        if (templateChildren != null && templateChildren.size() == 0)
            addTemplateMenu.setPopupMenuVisible (false);
        else
            addTemplateMenu.setPopupMenuVisible (true);

        updateTemplateMenu (templateChildren, myID);

        renameAction.setEnabled(editable);
        pasteAction.setEnabled(canPaste(myID, myPath, templateChildren, cutNode));
    }

    /**
     * The next method implement the TreeSelectionListener interface
     * to deal with changes to the tree selection.
     */
    public void valueChanged (TreeSelectionEvent e) {
        TreePath tp = (e == null ? tree.getSelectionPath()
                                 : e.getNewLeadSelectionPath());

        if (tp == null) {           // deselection
            deleteMenuItem.setEnabled (false);
            moveUpAction.setEnabled (false);
            moveDownAction.setEnabled (false);
            adjustMenu (false, true, false, null, null, null);
            addTemplateMenu.setEnabled (false);
            return;
        }
        Object [] path = tp.getPath();
        PropertyKey key = treeModel.getPropKey (useProps, path);

        DefaultMutableTreeNode node =
            (DefaultMutableTreeNode)tp.getLastPathComponent();
        moveUpAction.setEnabled(moveUpIsLegal(node));
        moveDownAction.setEnabled(moveDownIsLegal(node));

        // Place code to update selection-sensitive field(s) here.
        Prop val = useProps.pget (key);

        String status = val.getStatus();
        if (status == null)
            status = "";

        int     parseIndex      = 0;
        boolean moveable        = true;
        boolean editable        = true;
        boolean deletable       = true;
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

        if (path.length <= 1)       // top two levels (root & 1st sub) static
            deletable = false;
        else if (!editable && !moveable)
            deletable =
                status.startsWith("" + NO_MOVE_CHAR + NO_EDIT_CHAR + DELETE_OK_CHAR);
        deleteMenuItem.setEnabled (deletable);
        cutAction.setEnabled (deletable);

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
                String sDebug, childID;
                int endIndex;
                while (st.hasMoreElements()) {
                    sDebug = st.nextToken();
                    endIndex = sDebug.indexOf ("(");
                    if (endIndex < 0)
                        endIndex = sDebug.length();
                    childID = sDebug.substring (0, endIndex);
                    PropertyKey childKey = templates.getByID(childID);
                                         // if there isn't already a child with this name
                                         // or if the given template is rename-able,
                    if (childKey == null ||
                        (val.isUniqueChildName(Prop.unqualifiedName(childKey.name())) ||
                         templateIsMalleable(childKey)))
                                         // then it's okay to allow adding this template.
                        allowedChildren.addElement (childID);
//        System.out.println("Allowing Template " +
//                           sDebug.substring (0, endIndex));
                }
            }
        }
        String valID = val.getID();
        if (valID == null) valID = "";
        adjustMenu (allowsSiblings, allowsChildren, editable,
                    allowedChildren, valID, key.path());
    }

    private boolean templateIsMalleable(PropertyKey templateKey) {
        if (templateKey == null) return false;
        String status = templates.pget(templateKey).getStatus();
        if (status == null) return true;
        if (status.startsWith(NO_MOVE_CHAR + "" + NO_EDIT_CHAR) ||
            status.startsWith(NO_EDIT_CHAR + ""))
            return false;
        else
            return true;
    }

    public void updateTemplateMenu(Vector tList,
                                   String id) {
        Prop      p;
        int       idx;
        JMenu     addTemplateMenu = this.addTemplateMenu;
        JMenuItem menuItem;
        String    val;
        PropertyKey tKey = PropertyKey.ROOT;
        boolean enableMenu = false;

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
            String display = Prop.unqualifiedName(val);
//      System.out.println("Update: passed " + val);
            enableMenu = true;
            if (addTemplateMenu.getItemCount() > 18)
                addTemplateMenu = (JMenu) addTemplateMenu.add
                    (new JMenu(resource.getDlgString("More")), 0);
            menuItem = addTemplateMenu.add(new JMenuItem(display));
            menuItem.addActionListener(new AddTemplateAction(val));
        }
        this.addTemplateMenu.setEnabled(enableMenu);
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
        String previousName, newName;
        int index;

        for (int i = 0; i < indices.length; i++) {
            previousName = useProps.getChildName(parent, indices[i]);
            newName = children [i].toString();
            index = indices[i];
            String newNameTrimmed = newName.trim();

            if (!newName.equals(newNameTrimmed)) {
                // Use the trimmed name
                treeModel.useTreeModelListener(false);
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                    treeModel.getChild(e.getTreePath().getLastPathComponent(), index);
                node.setUserObject(newNameTrimmed);
                treeModel.nodeChanged(node);
                treeModel.useTreeModelListener(true);

            } else if (newNameIsAcceptable(parent, newName)) {
                useProps.setChildKey (parent, newName, index);
                setDirty (true);

            } else {
                // Revert back to the old name.
                treeModel.useTreeModelListener(false);
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                    treeModel.getChild(e.getTreePath().getLastPathComponent(), index);
                node.setUserObject(previousName);
                treeModel.nodeChanged(node);
                treeModel.useTreeModelListener(true);
            }
        }
    }

    private boolean newNameIsAcceptable(PropertyKey parent, String newName) {
        if (newName.indexOf('/') != -1) {
            JOptionPane.showMessageDialog
                (frame, resource.getString("HierarchyNameError"),
                 resource.getString("InvalidName"), JOptionPane.ERROR_MESSAGE);
            return false;
        } else if (!useProps.pget(parent).isUniqueChildName(newName)) {
            //JOptionPane.showMessageDialog
            // (frame, "There is already a node with that name.",
            // "Invalid name", JOptionPane.ERROR_MESSAGE);
            return false;
        } else if (newName.trim().length() == 0)
            return false;
        else if (!newName.equals(new String(newName.getBytes()))) {
            JOptionPane.showMessageDialog
                (frame,
                 resource.getStrings("HierarchyUnicodeError"),
                 resource.getString("InvalidName"), JOptionPane.ERROR_MESSAGE);
            return false;
        } else if (isReservedChildName(useProps.pget(parent), newName)) {
            JOptionPane.showMessageDialog
                (frame,
                 resource.format("HierarchyReservedNameError_FMT", newName, parent.path()),
                 resource.getString("InvalidName"), JOptionPane.ERROR_MESSAGE);
                return false;
        } else
            return true;
    }


    private boolean isReservedChildName(Prop parent, String newName) {
        String status = parent.getStatus();
        if (status == null) return false;

        int parseIndex = status.indexOf (ALLOWED_CHILD);
        if (parseIndex == -1) return false;

        int lastChar = status.indexOf (REQUIRED_PARENT);
        if (lastChar == -1)
            lastChar = status.length();
        if (lastChar <= parseIndex + 1) return false;

        status = status.substring (parseIndex + 1, lastChar);
        StringTokenizer st = new StringTokenizer(status,
                String.valueOf (ALLOWED_CHILD));
        while (st.hasMoreElements()) {
            String childID = st.nextToken();
            int endIndex = childID.indexOf ("(");
            if (endIndex != -1) childID = childID.substring(0, endIndex);
            PropertyKey childKey = templates.getByID(childID);
            if (childKey == null) continue;
            if (!templateIsMalleable(childKey)
                    && newName.equals(Prop.unqualifiedName(childKey.name())))
                return true;
        }

        return false;
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


    public static void createDataFile (String dest, String src, String extra) {
        BufferedWriter out;
        try {
            File parentDir = new File(new File(dest).getParent());
            if (!parentDir.isDirectory())
                parentDir.mkdirs();
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dest),
                DataRepository.getDatasetEncoding()));
            out.write("#include <" + src + ">");
            out.newLine();
            if (extra != null)
                out.write(extra);
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


    public TreeNode copyTemplate(DefaultMutableTreeNode destParent,
                                 String                 templateName) {
        //recursive copy of node, children and properties
        PropertyKey parent = treeModel.getPropKey (useProps,
                                                   destParent.getPath ());
        PropertyKey templateKey = templates.getRootChildByName(templateName);
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

        treeModel.nodeStructureChanged(destParent);

        return (TreeNode) treeModel.getChild(destParent, newIndex);
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
    class AddAction extends AbstractAction {

        public AddAction() {
            super(resource.getString("HierarchyBelow"),
                  new ImageIcon(HierarchyEditor.class.getResource("ins-after.gif")));
            putValue(Action.SHORT_DESCRIPTION, resource.getString("HierarchyAddNodeBelow"));
        }

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
            DefaultMutableTreeNode newNode =
                new DefaultMutableTreeNode (newCName (parent));
            treeModel.insertNodeInto(newNode, parent, newIndex);
            startEditingNode(newNode);
            expandRoot();

            setDirty (true);
        }
    } // End of PropertyFrame.AddAction


    /**
     * AddChildAction is used to add a new item as a child of the selected item.
     */
    class AddChildAction extends AbstractAction {
        /** Number of nodes that have been added. */
        public int               addCount;

        public AddChildAction() {
            super(resource.getString("HierarchyAsChild"),
                  new ImageIcon(HierarchyEditor.class.getResource("ins-child.gif")));
            putValue(Action.SHORT_DESCRIPTION, resource.getString("HierarchyAddNodeAsChild"));
        }

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
            DefaultMutableTreeNode newNode =
                new DefaultMutableTreeNode (newCName (parent));
            treeModel.insertNodeInto(newNode, parent, newIndex);
            startEditingNode(newNode);
            expandRoot();

            /* recompute the template menu. */
            valueChanged(null);

            setDirty (true);
        }
    } // End of PropertyFrame.AddChildAction


    /**
     * InsertAction is used to insert a new item before the selected item.
     */
    class InsertAction extends AbstractAction {
        /** Number of nodes that have been added. */
        public int               insertCount;

        public InsertAction() {
            super(resource.getString("HierarchyAbove"),
                  new ImageIcon(HierarchyEditor.class.getResource("ins-before.gif")));
            putValue(Action.SHORT_DESCRIPTION, resource.getString("HierarchyAddNodeAbove"));
        }

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
            DefaultMutableTreeNode newNode =
                new DefaultMutableTreeNode (newCName (parent));
            treeModel.insertNodeInto(newNode, parent, newIndex);
            startEditingNode(newNode);
            expandRoot();

            setDirty (true);
        }
    } // End of PropertyFrame.InsertAction


    /**
     * AddTemplateAction responds to the user selecting a template for
     * insertion as a child of the selected node.
     */
    class AddTemplateAction extends Object implements ActionListener {
        private String templateName;
        public AddTemplateAction(String templateName) {
            this.templateName = templateName;
        }
        // Adds the specified template as a child of the selected item.
        public void actionPerformed(ActionEvent e) {
            DefaultMutableTreeNode          lastItem = getSelectedNode();

            if (lastItem != null) {
                TreeNode newNode = copyTemplate (lastItem, templateName);

                /* recompute the template menu. */
                valueChanged(null);
                setDirty (true);

                startEditingNode(newNode);
            }
        }
    } // End of PropertyFrame.AddTemplateAction


    /**
     * RemoveAction removes the selected node from the tree.  If
     * The root or nothing is selected nothing is removed.
     */
    class RemoveAction extends Object implements ActionListener {
        // Removes the selected item as long as it isn't root.
        public void actionPerformed(ActionEvent e) {
            DefaultMutableTreeNode nodeToRemove = getSelectedNode();
            if (nodeToRemove == null || nodeToRemove == treeModel.getRoot())
                return;

            PropertyKey key = treeModel.getPropKey(useProps, nodeToRemove.getPath());
            if (key == null)
                return;
            String path = key.path();

            String title = resource.getString("HierarchyDeleteWarningTitle");
            String[] message = resource.format("HierarchyDeleteWarning_FMT",
                    path).split("\n");
            int userChoice = JOptionPane.showConfirmDialog(frame, message,
                title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (userChoice == JOptionPane.YES_OPTION)
                treeModel.removeNodeFromParent(nodeToRemove);
        }
    } // End of PropertyFrame.RemoveAction

    class RenameAction extends AbstractAction {
        public RenameAction() {
            super(resource.getString("Rename"));
        }
        public void actionPerformed(ActionEvent e) {
            DefaultMutableTreeNode node = getSelectedNode();
            if (node != null)
                startEditingNode(node);
        }
    }

    /** MoveUpAction swaps the selected node with its preceeding sibling. */
    class MoveUpAction extends AbstractAction {
        public MoveUpAction() {
            super(resource.getString("HierarchyMoveUp"), new ImageIcon
                  (HierarchyEditor.class.getResource("block-up-arrow.gif")));
            putValue(Action.SHORT_DESCRIPTION, resource.getString("HierarchyMoveNodeUp"));
        }

        public void actionPerformed(ActionEvent e) {
            DefaultMutableTreeNode node = getSelectedNode();
            moveUp(node);
            // make certain that the node is still selected when we're done.
            tree.setSelectionPath(new TreePath(treeModel.getPathToRoot(node)));
        }
    }

    /** MoveDownAction swaps the selected node with its following sibling. */
    class MoveDownAction extends AbstractAction {
        public MoveDownAction() {
            super(resource.getString("HierarchyMoveDown"), new ImageIcon(HierarchyEditor.class.getResource
                                             ("block-down-arrow.gif")));
            putValue(Action.SHORT_DESCRIPTION, resource.getString("HierarchyMoveNodeDown"));
        }

        public void actionPerformed(ActionEvent e) {
            DefaultMutableTreeNode node = getSelectedNode();
            if (node == null) return;
            moveUp(node.getNextSibling());
            // make certain that node is still selected when we're done.
            tree.setSelectionPath(new TreePath(treeModel.getPathToRoot(node)));
        }
    }

    private boolean isMoveable(String status) {
        if (status == null || status.length() == 0) return true;
        return !(status.startsWith(NO_MOVE_CHAR + ""));
    }
    private boolean moveDownIsLegal(DefaultMutableTreeNode node) {
        return (node != null && moveUpIsLegal(node.getNextSibling()));
    }
    private boolean moveUpIsLegal(DefaultMutableTreeNode node) {
        if (node == null ||
            node == (DefaultMutableTreeNode)treeModel.getRoot())
            return false;

        return moveUpIsLegal(treeModel.getPropKey (useProps, node.getPath()));
    }
    private boolean moveUpIsLegal(PropertyKey key) {
        Prop prop = useProps.pget(key);
        if (!isMoveable(prop.getStatus())) return false;

        PropertyKey parentKey = key.getParent();
        Prop parentProp = useProps.pget(parentKey);
        int pos = -1;
        for (int i=parentProp.getNumChildren();  i-- > 0; )
            if (key.equals(parentProp.getChild(pos=i))) break;
        if (pos < 1) return false;

        PropertyKey siblingKey = parentProp.getChild(pos-1);
        Prop siblingProp = useProps.pget(siblingKey);
        return isMoveable(siblingProp.getStatus());
    }

    private void moveUp(DefaultMutableTreeNode node) {
        if (node == null || node == (DefaultMutableTreeNode)treeModel.getRoot())
            return;

        // Check to make certain that the move is legal.
        Object [] path = node.getPath();
        PropertyKey key = treeModel.getPropKey (useProps, path);
        if (!moveUpIsLegal(key)) return;

        // First, make the change in the properties object.
        DefaultMutableTreeNode parentNode=(DefaultMutableTreeNode)node.getParent();
        int index = parentNode.getIndex(node);
        PropertyKey parentKey = key.getParent();
        Prop parentProp = useProps.pget(parentKey);
        parentProp.moveChildUp(index);

        // Next, make the change in the tree model.
        treeModel.useTreeModelListener(false);
        parentNode.insert(node, index-1);
        treeModel.useTreeModelListener(true);
        treeModel.nodeStructureChanged(parentNode);

        setDirty(true);
    }


    /** CutAction remembers the selected node for future paste operations. */
    class CutAction extends AbstractAction {
        public CutAction() {
            super(resource.getString("Cut"),
                  new ImageIcon(HierarchyEditor.class.getResource("cut.gif")));
            putValue(Action.SHORT_DESCRIPTION,
                     resource.getString("HierarchyCutNode"));
        }

        public void actionPerformed(ActionEvent e) {
            DefaultMutableTreeNode node = getSelectedNode();
            if (node != null)
                cutNode = node;
        }
    }


    /** PasteAction moves the previously cut node to become a child of the
     *  currently selected node. */
    class PasteAction extends AbstractAction {
        public PasteAction() {
            super(resource.getString("Paste"),
                  new ImageIcon(HierarchyEditor.class.getResource("paste.gif")));
            putValue(Action.SHORT_DESCRIPTION,
                     resource.getString("HierarchyPasteNode"));
        }

        public void actionPerformed(ActionEvent e) {
            if (cutNode == null) return;

            DefaultMutableTreeNode node = getSelectedNode();
            if (node == null) return;

            // get the default index for adding
            PropertyKey parentPKey = treeModel.getPropKey (useProps, node.getPath ());
            Prop val = useProps.pget (parentPKey);
            int newIndex = useProps.getNumChildren (parentPKey);

                                      // See if should be adding at other index...

                                      // if parent specifies allowed children
            String status, allowedChild;
            PropertyKey cutKey = treeModel.getPropKey (useProps, cutNode.getPath());
            Prop cutProp = useProps.pget(cutKey);
            String cutID = cutProp.getID();
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
                        if (allowedChild.startsWith (cutID)) {
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

            // Create an appropriately named node (original + made unique(if needed))
            String newChildName =
                useProps.pget(parentPKey).uniqueChildName(cutKey.name());
            useProps.addChildKey
                (parentPKey,
                 useProps.pget(parentPKey).uniqueChildName(newChildName),
                 newIndex);

                    // Move nodes from the cutKey to the new child key
            useProps.move (cutKey, useProps.getChildKey (parentPKey, newIndex));

                // Now 'delete' the cut node
            treeModel.removeNodeFromParent(cutNode);

                // And refresh the tree...
            treeModel.useTreeModelListener (false);
            treeModel.reload (useProps);
            expandRoot ();
            treeModel.useTreeModelListener (true);

            treeModel.nodeStructureChanged(node);

            // ensure that the newly pasted node is visible, and select it.
            try {
                DefaultMutableTreeNode pastedNode =
                    (DefaultMutableTreeNode) node.getChildAt(newIndex);
                tree.setSelectionPath
                    (new TreePath(treeModel.getPathToRoot(pastedNode)));
            } catch (Exception ee) {
                // We may get an ArrayIndexOutOfBoundsException if the node
                // was pasted into its own parent.  Selecting the pasted node
                // isn't a critical action - just abort.
            }

            cutNode = null;
            pasteAction.setEnabled(false);
            setDirty(true);
        }
    }

    private boolean canPaste(String parentID, String parentPath,
                             Vector templateChildren,
                             DefaultMutableTreeNode cutNode) {
        if (parentID == null || parentPath == null || cutNode == null ||
            (templateChildren != null && templateChildren.size() == 0))
            return false;

        PropertyKey cutKey = treeModel.getPropKey (useProps, cutNode.getPath());

        String cutPath = cutKey.path();
                // disallow pasting a node into itself.
        if (cutPath.equals(parentPath) ||
                // disallow pasting a node into one of its descendants (would
                // create an illegal recursive tree.
            parentPath.startsWith(cutPath+"/") ||
                // disallow pasting a node into its current parent (it
                // already lives there!)
            parentPath.equals(cutKey.getParent().path()))
            return false;

        Prop cutProp = useProps.pget(cutKey);
        String cutID = cutProp.getID();
            /* match child with parent's allowed child list, if any */
        if (templateChildren != null && !templateChildren.contains(cutID)) return false;

            /* match parent with child's required parent list, if any */
        String cutStatus = cutProp.getStatus();
        if (cutStatus == null) return true;         /* no required parent */
        int idx = cutStatus.indexOf(REQUIRED_PARENT);
        if (idx == -1) return true;                         /* no required parent */

        StringTokenizer st = new StringTokenizer
            (cutStatus.substring (idx + 1), String.valueOf (REQUIRED_PARENT));
        while (st.hasMoreElements())                        /* matches required parent */
            if (parentID.equals(st.nextElement())) return true;

        // special case: team project roots in the personal dashboard can be
        // moved under arbitrary plain nodes
        if ((cutID.endsWith("/IndivRoot") || cutID.endsWith("/Indiv2Root"))
                && "".equals(parentID))
            return true;

        return false;
    }


    private void startEditingNode(TreeNode node) {
        final TreePath path = new TreePath(treeModel.getPathToRoot(node));
        tree.setSelectionPath(path);
        if (tree.isEditable())
            SwingUtilities.invokeLater(new Runnable() {
                TreePath thepath = path;
                public void run() { tree.startEditingAtPath(thepath); }
                });
    }

    /** A modified version of DefaultTreeCellEditor that selects all the
     *  text in the component after editing is started. (This makes it
     *  easy for the user to replace all that text.)
     */
    private class SelectingTreeEditor extends DefaultTreeCellEditor {
        public SelectingTreeEditor(JTree tree, DefaultTreeCellRenderer renderer) {
            super(tree, renderer, null);
        }
        public Component getTreeCellEditorComponent(JTree tree, Object value,
                                                    boolean isSelected,
                                                    boolean expanded,
                                                    boolean leaf, int row) {
            Component result = super.getTreeCellEditorComponent
                (tree, value, isSelected, expanded, leaf, row);
            if (editingComponent instanceof JTextComponent)
                ((JTextComponent) editingComponent).selectAll();
            return result;
        }
    }

    private List listeners = new LinkedList();
    public void addHierarchyEditorListener(Listener l) {
        listeners.add(l);
    }
    public void removeHierarchyEditorListener(Listener l) {
        listeners.remove(l);
    }
    private void fireHierarchyEditorClosed() {
        for (Iterator i = new LinkedList(listeners).iterator(); i.hasNext();) {
            Listener l = (Listener) i.next();
            l.hierarchyEditorClosed(this);
        }
    }
}
