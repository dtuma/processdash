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
import javax.swing.tree.*;
import javax.swing.text.JTextComponent;
import pspdash.data.DataRepository;


public class DefectEditor extends Component
    implements TreeSelectionListener, ListSelectionListener, ActionListener,
               PSPProperties.Listener
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
    protected JButton editButton, deleteButton, closeButton, dtsEditButton;
    protected JComboBox dtsSelector;
//  protected UserWarning     warnUser;

    Resources resources = Resources.getDashBundle("pspdash.Defects");
    String inheritTypeSelection, typeSelectionTooltip;
    boolean buildingDtsSelector = false;


    static final String DTS_EDIT_URL = "/dash/dtsEdit.class";

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
        props.addHierarchyListener(this);
        data     = dashboard.getDataRepository();

        defectLogs = new Hashtable ();
        reload ();

        frame = new JFrame(resources.getString("Defect_Log_Window_Title"));
        frame.setIconImage(DashboardIconFactory.getWindowIconImage());
        frame.getContentPane().add("Center", panel);
        frame.setBackground(Color.lightGray);
        PCSH.enableHelpKey(frame, "UsingDefectLogEditor");

        /* Create the JTreeModel. */
        treeModel = new PropTreeModel (new DefaultMutableTreeNode ("root"), null);
        treeModel.fill (useProps);

        /* Create and show the visual components. */
        panel.setLayout(new BorderLayout());
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                   constructTreePanel(), constructEditPanel());
        panel.add("Center", splitPane);

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

    JComponent constructTreePanel() {
        /* Create the tree. */
        tree = new JTree(treeModel);
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
        sp.setPreferredSize(new Dimension(300, 280));
        sp.getViewport().add(tree);

        /* Create the type standard selection widget */
        inheritTypeSelection = resources.getString("Inherit_Type");
        typeSelectionTooltip = resources.getString("Type_Selector_Tooltip");

        JPanel selectorPanel = new JPanel(false);
        JLabel typeLabel = new JLabel(resources.getString("Type_Selector_Label"));
        typeLabel.setToolTipText(typeSelectionTooltip);
        selectorPanel.add(typeLabel);

        dtsSelector = new JComboBox();
        dtsSelector.setRenderer(new DtsListCellRenderer());
        dtsSelector.setActionCommand ("dts");
        dtsSelector.addActionListener (this);
        selectorPanel.add(dtsSelector);

        dtsEditButton = new JButton(resources.getString("Edit_Types_Button"));
        dtsEditButton.setToolTipText(resources.getString("Edit_Types_Tooltip"));
        dtsEditButton.setActionCommand ("dtsEdit");
        dtsEditButton.addActionListener (this);
        selectorPanel.add(dtsEditButton);

        JPanel result = new JPanel();
        result.setLayout(new BorderLayout());
        result.setPreferredSize(new Dimension(300, 300));
        result.add("Center", sp);
        result.add("South", selectorPanel);
        return result;
    }

    void refreshDefectTypeSelector() {
        String selectedPath = getSelectedPath();
        if (selectedPath.length() == 0) {
            dtsSelector.setEnabled(false);
            return;
        }

        buildingDtsSelector = true;
        String selectedType = DefectTypeStandard.getSetting(data, selectedPath);
        int selectedItem = 0;

        String[] standards = DefectTypeStandard.getDefinedStandards(data);
        dtsSelector.removeAllItems();
        dtsSelector.addItem(inheritTypeSelection);

        for (int i = 0;   i < standards.length;   i++) {
            dtsSelector.addItem(standards[i]);
            if (selectedType != null && selectedType.equals(standards[i]))
                selectedItem = i+1;
        }

        if (selectedItem == 0 && selectedType != null) {
            selectedItem = dtsSelector.getItemCount();
            dtsSelector.addItem("\t" + selectedType);
            dtsSelector.setForeground(Color.red);
            dtsSelector.setToolTipText
                (resources.getString("Nonexistent_Type_Tooltip"));
        } else {
            dtsSelector.setForeground(Color.black);
            dtsSelector.setToolTipText(typeSelectionTooltip);
        }

        dtsSelector.setEnabled(true);
        dtsSelector.setSelectedIndex(selectedItem);
        buildingDtsSelector = false;
    }

    private class DtsListCellRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(JList list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            boolean hasError = false;
            if (value instanceof String && ((String) value).startsWith("\t")) {
                hasError = true;
                value = ((String) value).substring(1);
            }

            Component result = super.getListCellRendererComponent
                (list, value, index, isSelected, cellHasFocus);

            setForeground(hasError ? Color.red : Color.black);

            return result;
        }
    }

    String getSelectedPath() {
        DefaultMutableTreeNode selected = getSelectedNode();
        if (selected == null) return "";

        PropertyKey key = treeModel.getPropKey (useProps, selected.getPath());
        return (key == null ? "" : key.path());
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


    public void hierarchyChanged(PSPProperties.Event e) { reloadAll(null); }

    public void reloadAll(PSPProperties newProps) {
        if (newProps != null)
            useProps.copy(newProps);
        treeModel.reload (useProps);
        treeModel.nodeStructureChanged((TreeNode) treeModel.getRoot());
        reload();
        applyFilter();
    }


    protected void reload () {
        PropertyKey pKey;
        Prop prop;
        DefectLog dl;
        String defLogName;
        Defect[] defects;
        defectLogs.clear();
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
        if (cmd == null) return;

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
                     resources.format("Confirm_Delete_Message_FMT", number),
                     resources.getString("Confirm_Delete_Title"),
                     JOptionPane.YES_NO_OPTION) ==
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

        } else if (cmd.equals("dts")) {
            if (buildingDtsSelector) return;
            String type = (String) dtsSelector.getSelectedItem();
            if (type == inheritTypeSelection) type = null;
            if (type == null || !type.startsWith("\t"))
                DefectTypeStandard.saveDefault(data, getSelectedPath(), type);
            refreshDefectTypeSelector();

        } else if (cmd.equals("dtsEdit")) {
            Browser.launch(DTS_EDIT_URL);
        }
    }


    private JPanel constructEditPanel () {
        JPanel  retPanel = new JPanel(false);
        JButton button;

        String[] columns = new String[] {
            "Project", "ID", "Type", "Injected", "Removed",
            "Time", "Fix", "Description", "Date" };

        retPanel.setLayout(new BorderLayout());
        table = new ValidatingTable
            (resources.getStrings("Column_Name_", columns),
             null,
             resources.getInts("Column_Width_", columns),
             resources.getStrings("Column_Tooltip_", columns),
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
        editButton = new JButton (resources.getString("Edit"));
        editButton.setActionCommand ("edit");
        editButton.addActionListener (this);
        editButton.setEnabled (false);
        btnPanel.add (editButton);

                                    // Should only be available if one
                                    // entry is selected
        deleteButton = new JButton (resources.getString("Delete"));
        deleteButton.setActionCommand ("delete");
        deleteButton.addActionListener (this);
        deleteButton.setEnabled (false);
        btnPanel.add (deleteButton);

        closeButton = new JButton (resources.getString("Close"));
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
            refreshDefectTypeSelector();
            return;
        }
        Object [] path = tp.getPath();
        PropertyKey key = treeModel.getPropKey (useProps, path);

        Prop val = useProps.pget (key);
        applyFilter ();
        refreshDefectTypeSelector();
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
