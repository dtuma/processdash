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


public class DefectEditor extends Component implements TreeSelectionListener, ActionListener
{
    /** Class Attributes */
    protected JFrame          frame;
    protected JTree           tree;
    protected PropTreeModel   treeModel;
    protected PSPProperties   useProps;
    protected PSPDashboard    dashboard = null;
    protected Hashtable       defectLogs = null;
    protected ValidatingTable table;
    protected DataRepository  data;
    protected Vector          currentLog   = new Vector();
//  protected UserWarning     warnUser;



    //
    // member functions
    //
    private void debug(String msg) {
        System.out.println("TimeLogEditor:" + msg);
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

        frame = new JFrame("DefectEditor");
        frame.setTitle("DefectEditor");
        frame.getContentPane().add("Center", panel);
        frame.setBackground(Color.lightGray);

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
        tree.setRowHeight(-1);	// Make tree ask for the height of each row.

        /* Put the Tree in a scroller. */
        JScrollPane sp = new JScrollPane
            (ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
             ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        sp.setPreferredSize(new Dimension(300, 300));
        sp.getViewport().add(tree);

        /* And show it. */
        panel.setLayout(new BorderLayout());
//    panel.add("North", constructFilterPanel());
        panel.add("West", sp);
        panel.add("Center", constructEditPanel());
//    panel.add("South", constructControlPanel());

        frame.addWindowListener( new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                frame.setVisible (false);
            }
        });

        applyFilter();
        frame.pack();
        frame.show();
    }


    boolean areEqual(String s1, String s2) {
        return ((s1 == s2) ||	// handle null / identical strings first
                (s1 != null) ? s1.equals (s2) : false);
    }


    public void updateDefectLog (DefectLog dl) {
        System.out.println("In updateDefectLog");
        Defect[] defects = dl.readDefects();
        PropertyKey pKey;
        DefectListID dlid;
        Enumeration values = defectLogs.elements();
        while (values.hasMoreElements()) {
            dlid = (DefectListID)values.nextElement();
            if (areEqual(dlid.dl.defectLogFilename, dl.defectLogFilename) &&
                areEqual(dlid.dl.dataPrefix, dl.dataPrefix)) {
                System.out.println(" Found Log:" + dlid.pk.path());
                for (int ii = 0; ii < defects.length; ii++)
                    System.out.println("  " + ii + ":" + defects[ii].toString());
                defectLogs.put (dlid.pk,
                                new DefectListID (dlid.pk, dl, defects));
                break;
            }
        }
        applyFilter();
    }


    public void updateDefectLog (DefectLog dl, Defect d) {
        updateDefectLog (dl);	// TBD - optimize?
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

    void applyFilter () {
        PropertyKey key = null;
        DefaultMutableTreeNode selected = getSelectedNode();
        if (selected != null) {
            key = treeModel.getPropKey (useProps, selected.getPath());
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
                    continue;		// this one filtered
            }
            dli = (DefectListID) defectLogs.get (pk);
            for (int ii = 0; ii < dli.defects.length; ii++) {
                dle = new DefectListEntry (dli, ii);
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
    }


    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        DefectDialog dlg;

        if (cmd.equals("edit")) {
            if (table.table.getSelectedRowCount() > 0) {
                int row = table.table.getSelectedRow();
                DefectListEntry dle = (DefectListEntry)currentLog.elementAt (row);
                if (dle != null) {
                    dlg = new DefectDialog
                        (dashboard,
                         dashboard.getDirectory() + useProps.pget(dle.pk).getDefectLog(),
                         dle.pk,
                         dle.defect);
                    dlg.setTitle(dle.pk.path());
                }
            }
        } else if (cmd.equals("delete")) { // TBD

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

        retPanel.add ("Center", table);

        JPanel btnPanel = new JPanel(false);
                                    // Should only be available if one
                                    // entry is selected
        JButton editButton;
        editButton = new JButton ("Edit");
        editButton.setActionCommand ("edit");
        editButton.addActionListener (this);
        btnPanel.add (editButton);

                                    // Should only be available if one
                                    // entry is selected
        button = new JButton ("Delete");
        button.setActionCommand ("delete");
        button.addActionListener (this);
        button.setEnabled (false);
        btnPanel.add (button);

        button = new JButton ("Print");
        button.setActionCommand ("print");
        button.addActionListener (this);
        button.setEnabled (false);
        btnPanel.add (button);

        retPanel.add ("South", btnPanel);

        return retPanel;
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

        if (tp == null) {		// deselection
            tree.clearSelection();
            applyFilter ();
            return;
        }
        Object [] path = tp.getPath();
        PropertyKey key = treeModel.getPropKey (useProps, path);

        Prop val = useProps.pget (key);
        applyFilter ();
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

}

