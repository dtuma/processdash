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
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.tree.*;
import javax.swing.text.JTextComponent;
import pspdash.data.DataRepository;


public class DefectEditor extends Component
    implements TreeSelectionListener, ListSelectionListener, ActionListener
{
    /** Class Attributes */
    protected JFrame          frame;
    protected JTree           tree;
    protected PropTreeModel   treeModel;
    protected PSPProperties   useProps;
    protected PSPDashboard    dashboard = null;
    protected Hashtable       defectLogs = null;
    protected ValidatingTable table;
    protected JSplitPane      splitPane;
    protected DataRepository  data;
    protected Vector          currentLog   = new Vector();
    protected JButton editButton, deleteButton, closeButton;
//  protected UserWarning     warnUser;



    //
    // member functions
    //
    private static void debug(String msg) {
        System.out.println("DefectEditor:" + msg);
    }


                                // constructor
    public DefectEditor(PSPDashboard dash,
                        ConfigureButton button,
                        PSPProperties props) {
        dashboard        = dash;
        JPanel   panel   = new JPanel(true);

        useProps = props;
        data     = dashboard.getDataRepository();

        defectLogs = new Hashtable ();
        reload ();

        frame = new JFrame("Defect Editor");
        frame.setTitle("Defect Editor");
        frame.setIconImage(java.awt.Toolkit.getDefaultToolkit().createImage
                           (getClass().getResource("icon32.gif")));
        frame.getContentPane().add("Center", panel);
        frame.setBackground(Color.lightGray);
        PCSH.enableHelpKey(frame, "UsingDefectLogEditor");

        /* Create the JTreeModel. */
        treeModel = new PropTreeModel (new DefaultMutableTreeNode ("root"), null);

        /* Create the tree. */
        tree = new JTree(treeModel);
        treeModel.fill (useProps);
        tree.expandRow (0);
        tree.setShowsRootHandles (true);
        tree.setEditable(false);
        tree.addTreeSelectionListener (this);
        tree.setRootVisible(false);
        tree.setRowHeight(-1);      // Make tree ask for the height of each row.

        /* Put the Tree in a scroller. */
        JScrollPane sp = new JScrollPane
            (ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
             ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        sp.setPreferredSize(new Dimension(300, 300));
        sp.getViewport().add(tree);

        /* And show it. */
        panel.setLayout(new BorderLayout());
//    panel.add("North", constructFilterPanel());
        panel.add("Center", splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                                       sp, constructEditPanel()));
//    panel.add("South", constructControlPanel());

        frame.addWindowListener( new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                frame.setVisible (false);
            }
        });

        setSelectedPhase(dash.getCurrentPhase());
        loadCustomDimensions();
        splitPane.setDividerLocation(dividerLocation);

        applyFilter();
        //frame.pack();
        frame.setSize(new Dimension(frameWidth, frameHeight));
        frame.show();
    }


    boolean areEqual(String s1, String s2) {
        return ((s1 == s2) ||       // handle null / identical strings first
                (s1 != null) ? s1.equals (s2) : false);
    }


    public void updateDefectLog (DefectLog dl) {
        //System.out.println("In updateDefectLog");
        Defect[] defects = dl.readDefects();
        PropertyKey pKey;
        DefectListID dlid;
        Enumeration values = defectLogs.elements();
        while (values.hasMoreElements()) {
            dlid = (DefectListID)values.nextElement();
            if (areEqual(dlid.dl.defectLogFilename, dl.defectLogFilename) &&
                areEqual(dlid.dl.dataPrefix, dl.dataPrefix)) {
                //System.out.println(" Found Log:" + dlid.pk.path());
                //for (int ii = 0; ii < defects.length; ii++)
                //System.out.println("  " + ii + ":" + defects[ii].toString());
                defectLogs.put (dlid.pk,
                                new DefectListID (dlid.pk, dl, defects));
                break;
            }
        }
        applyFilter();
    }


    public void updateDefectLog (DefectLog dl, Defect d) {
        updateDefectLog (dl);       // TBD - optimize?
    }


    public void reloadAll(PSPProperties newProps) {
        useProps.copy(newProps);
        treeModel.reload (useProps);
        treeModel.nodeStructureChanged((TreeNode) treeModel.getRoot());
        applyFilter();
    }


    protected void reload () {
        PropertyKey pKey;
        Prop prop;
        DefectLog dl;
        String defLogName;
        Defect[] defects;
        Enumeration pKeys = useProps.keys ();
        while (pKeys.hasMoreElements()) {
            pKey = (PropertyKey)pKeys.nextElement();
            prop = useProps.pget (pKey);
            defLogName = prop.getDefectLog ();
            if (defLogName != null && defLogName.length() != 0) {
                dl = new DefectLog (dashboard.getDirectory() + defLogName,
                                    pKey.path(),
                                    data,
                                    dashboard);
                defects = dl.readDefects();
                defectLogs.put (pKey, new DefectListID (pKey, dl, defects));
            }
        }
    }


    public void showIt() {
        if (frame.isShowing())
            frame.toFront();
        else {
            reload();
            frame.show();
        }
    }

    public void quit() {
        saveCustomDimensions();
    }


    private static final String DIMENSION_SETTING_NAME = "defectlog.dimensions";
    private int frameWidth, frameHeight, dividerLocation = -1;
    private void loadCustomDimensions() {
        String setting = Settings.getVal(DIMENSION_SETTING_NAME);
        if (setting != null && setting.length() > 0) try {
            StringTokenizer tok = new StringTokenizer(setting, ",");
            frameWidth = Integer.parseInt(tok.nextToken());
            frameHeight = Integer.parseInt(tok.nextToken());
            dividerLocation = Integer.parseInt(tok.nextToken());
        } catch (Exception e) {}
        if (dividerLocation == -1) {
            frameWidth = 800; frameHeight = 400; dividerLocation = 300;
        }
    }
    private void saveCustomDimensions() {
        frameWidth = frame.getSize().width;
        frameHeight = frame.getSize().height;
        dividerLocation = splitPane.getDividerLocation();
        InternalSettings.set
            (DIMENSION_SETTING_NAME,
             frameWidth + "," + frameHeight + "," + dividerLocation);
    }

    void applyFilter () {
        PropertyKey key = null;
        DefaultMutableTreeNode selected = getSelectedNode();
        String extraPathFilter = null;
        if (selected != null) {
            key = treeModel.getPropKey (useProps, selected.getPath());
            if (key != null) {
                String selectedPath = key.path();
                DefectLogID logid = useProps.defectLog(key, "unimportant");
                if (logid != null && logid.path != key) {
                    key = logid.path;
                    String defectLogPath = key.path();
                    extraPathFilter = selectedPath.substring(defectLogPath.length()+1);
                }
            }
        }
        // apply the filter and load the vector (and the table)
        VTableModel model = (VTableModel)table.table.getModel();
        Object[] row = new Object [9];
        Defect[] defects;
        DefectListID dli;
        DefectListEntry dle;
        Defect def;
        PropertyKey pk;
        currentLog.removeAllElements();
        model.setNumRows (0);
        Enumeration dlList = defectLogs.keys ();
        while (dlList.hasMoreElements()) {
            pk = (PropertyKey) dlList.nextElement();
            if (key != null) {
                if ( !pk.key().equals(key.key()) &&
                    (!pk.isChildOf (key)))
                    continue;             // this one filtered
            }
            dli = (DefectListID) defectLogs.get (pk);
            for (int ii = 0; ii < dli.defects.length; ii++) {
                dle = new DefectListEntry (dli, ii);
                if (extraPathFilter != null &&
                    !matchesExtraPath(dle.defect.phase_injected, extraPathFilter) &&
                    !matchesExtraPath(dle.defect.phase_removed,  extraPathFilter))
                    continue;
                currentLog.addElement (dle);
                row [0] = dle.pk.path();
                row [1] = dle.defect.number;
                row [2] = dle.defect.defect_type;
                row [3] = dle.defect.phase_injected;
                row [4] = dle.defect.phase_removed;
                row [5] = dle.defect.fix_time;
                row [6] = dle.defect.fix_defect;
                row [7] = dle.defect.description;
                row [8] = DateFormatter.formatDateTime (dle.defect.date);
                model.addRow(row);
            }
        }
        table.doResizeRepaint();
        maybeEnableButtons();
    }

    private boolean matchesExtraPath(String phase, String pathFilter) {
        //if (pathFilter == null) return true;
        if (phase == null) return false;
        return (phase.equals(pathFilter) || phase.startsWith(pathFilter + "/"));
    }

    private DefectListEntry getSelectedDefect() {
        if (table.table.getSelectedRowCount() > 0) {
            int row = table.table.getSelectedRow();
            return (DefectListEntry) currentLog.elementAt (row);
        } else
            return null;
    }

    private DefectDialog getDialogForDefect(DefectListEntry dle) {
        return DefectDialog.getDialogForDefect
            (dashboard,
             dashboard.getDirectory() + useProps.pget(dle.pk).getDefectLog(),
             dle.pk,
             dle.defect);
    }


    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        DefectDialog dlg;
        DefectListEntry dle;

        if (cmd.equals("edit")) {
            dle = getSelectedDefect();
            if (dle != null) {
                dlg = getDialogForDefect(dle);
                dlg.setTitle(dle.pk.path());
                dlg.toFront();
            }

        } else if (cmd.equals("delete")) {
            dle = getSelectedDefect();
            String number = dle.defect.number;
            if (number != null) {
                // display a confirmation dialog
                if (JOptionPane.showConfirmDialog
                    (frame,
                     "Are you certain you want to delete defect #" + number + "?",
                     "Confirm Defect Deletion", JOptionPane.YES_NO_OPTION) ==
                    JOptionPane.YES_OPTION)
                    dle.dl.deleteDefect(number);
                dlg = getDialogForDefect(dle);
                if (dlg != null) dlg.dispose();
            }

        } else if (cmd.equals("close")) {
            frame.hide();

        } else if (cmd.equals("print")) {  // TBD
            PrintJob pjob = getToolkit().getPrintJob(frame, "Defect Log", null);
            if (pjob != null) {
                Graphics pg = pjob.getGraphics();
                if (pg != null) {
                    table.printAll(pg);
                    pg.dispose();
                }
                pjob.end();
            }
        }
    }


    private JPanel constructEditPanel () {
        JPanel  retPanel = new JPanel(false);
        JButton button;

        retPanel.setLayout(new BorderLayout());
        table = new ValidatingTable
            (new Object[] {"Project",  "ID",          "Type",
                           "Injected", "Removed",     "Time",
                           "Fix",      "Description", "Date"},
             null,
             new int[] {200, 25,  80,
                        100, 100, 40,
                        30,  200, 100},
             new String[] {"The project that the defect is logged to",
                           "The defect number of this defect",
                           "The defect Type",
                           "The phase in which the defect was injected",
                           "The phase in which the defect was removed",
                           "The fix time(minutes)",
                           "The number of the related defect",
                           "The (first part of the) defect description",
                           "The date the defect was entered / edited"},
             null, null, 0, true, null,
             new boolean[] {false, false, false, // no columns editable
                            false, false, false,
                            false, false, false});
        table.table.setRowSelectionAllowed (false);
        table.table.getSelectionModel().addListSelectionListener(this);
        table.table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)
                    editButton.doClick();
            } } );

        retPanel.add ("Center", table);

        JPanel btnPanel = new JPanel(false);
                                    // Should only be available if one
                                    // entry is selected
        editButton = new JButton ("Edit");
        editButton.setActionCommand ("edit");
        editButton.addActionListener (this);
        editButton.setEnabled (false);
        btnPanel.add (editButton);

                                    // Should only be available if one
                                    // entry is selected
        deleteButton = new JButton ("Delete");
        deleteButton.setActionCommand ("delete");
        deleteButton.addActionListener (this);
        deleteButton.setEnabled (false);
        btnPanel.add (deleteButton);

        closeButton = new JButton ("Close");
        closeButton.setActionCommand ("close");
        closeButton.addActionListener (this);
        btnPanel.add (closeButton);

        retPanel.add ("South", btnPanel);

        return retPanel;
    }

    private void enableButtons(boolean enable)
    {
        editButton.setEnabled (enable);
        deleteButton.setEnabled (enable);
    }

    private void maybeEnableButtons() {
        enableButtons(table.table.getSelectedRowCount() == 1);
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

        if (tp == null) {           // deselection
            tree.clearSelection();
            applyFilter ();
            return;
        }
        Object [] path = tp.getPath();
        PropertyKey key = treeModel.getPropKey (useProps, path);

        Prop val = useProps.pget (key);
        applyFilter ();
    }

    public void valueChanged(ListSelectionEvent e) {
        maybeEnableButtons();
    }

    public static void rename(PSPProperties oldProps, PSPProperties newProps,
                              String oldPrefix, String newPrefix,
                              PSPDashboard dashboard) {
        PropertyKey oldKey = PropertyKey.fromPath(oldPrefix);
        String oldLogName = oldProps.pget(oldKey).getDefectLog();
        if (oldLogName != null && oldLogName.length() > 0) {
            // if the node addressed by oldProps owns its own defect log, we
            // don't need to worry.
            return;
        }

        DefectLogID oldLog = oldProps.defectLog(oldKey, dashboard.getDirectory());
        if (oldLog == null) {
            // if the node addressed by oldProps doesn't have a defect log,
            // we don't need to worry.
            return;
        }

        String logPath = oldLog.path.path();
        oldPrefix = oldPrefix.substring(logPath.length() + 1);

        PropertyKey newKey = PropertyKey.fromPath(newPrefix);
        DefectLogID newLog = newProps.defectLog(newKey, dashboard.getDirectory());
        if (newLog == null) {
            // Should this ever happen???
            return;
        }
        String newLogPath = newLog.path.path();
        newPrefix = newPrefix.substring(newLogPath.length() + 1);

        if (oldPrefix.equals(newPrefix)) {
            // if the name change was limited to nodes that are ancestors of
            // the node owning the defect log, we don't need to worry.
            return;
        }

        if (!oldLog.filename.equals(newLog.filename)) {
            // Should this ever happen???
            return;
        }

        DefectLog log = new DefectLog(newLog.filename, newLogPath, null, null);
        log.performInternalRename(oldPrefix, newPrefix);
    }

    public class DefectListID {
        public PropertyKey pk;
        public DefectLog   dl;
        public Defect[]    defects;

        public DefectListID (PropertyKey pk, DefectLog dl, Defect[] defects) {
            this.pk = pk;
            this.dl = dl;
            this.defects = defects;
        }
    }

    public class DefectListEntry {
        public PropertyKey pk;
        public DefectLog   dl;
        public Defect      defect;

        public DefectListEntry (DefectListID dli, int index) {
            this (dli.pk, dli.dl, dli.defects [index]);
        }
        public DefectListEntry (PropertyKey pk, DefectLog dl, Defect defect) {
            this.pk = pk;
            this.dl = dl;
            this.defect = defect;
        }
    }

    /** Expand the hierarchy so that the given node is visible and selected.
     */
    public void setSelectedNode(PropertyKey path) {
        if (path == null) return;
        DefaultMutableTreeNode node =
            (DefaultMutableTreeNode) treeModel.getNodeForKey(useProps, path);
        if (node == null) return;

        TreePath tp = new TreePath(node.getPath());
        tree.clearSelection();
        tree.scrollPathToVisible(tp);
        tree.addSelectionPath(tp);
    }
    private boolean hasDefLog(PropertyKey phase) {
        if (phase == null) return false;
        Prop prop = useProps.pget(phase);
        String defLogName = prop.getDefectLog ();
        return (defLogName != null && defLogName.length() > 0);
    }
    public void setSelectedPhase(PropertyKey phase) {
        if (phase == null) return;
        PropertyKey parent = phase.getParent();
        if (!hasDefLog(phase) && hasDefLog(parent))
            setSelectedNode(parent);
        else
            setSelectedNode(phase);
    }

}
