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


package net.sourceforge.processdash.log.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.PrintJob;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import net.sourceforge.processdash.ApplicationEventListener;
import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.Filter;
import net.sourceforge.processdash.hier.Prop;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.hier.ui.PropTreeModel;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.log.defects.Defect;
import net.sourceforge.processdash.log.defects.DefectLog;
import net.sourceforge.processdash.log.defects.DefectLogID;
import net.sourceforge.processdash.process.DefectTypeStandard;
import net.sourceforge.processdash.templates.ExtensionManager;
import net.sourceforge.processdash.ui.Browser;
import net.sourceforge.processdash.ui.ConfigureButton;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.ui.lib.DropDownButton;
import net.sourceforge.processdash.util.FormatUtil;


public class DefectLogEditor extends Component implements
        TreeSelectionListener, ListSelectionListener, ActionListener,
        DashHierarchy.Listener, DefectLog.Listener, ApplicationEventListener {

    public static final String IMPORT_ACTION_INVALID = "invalid";
    public static final String IMPORT_ACTION_SEL_PATH = "selectedPath";
    public static final String IMPORT_ACTION_DEF_PATH = "defectLogPath";


    /** Class Attributes */
    protected JFrame          frame;
    protected JTree           tree;
    protected PropTreeModel   treeModel;
    protected DashHierarchy   useProps;
    protected ProcessDashboard    dashboard = null;
    protected Hashtable       defectLogs = null;
    protected List forbiddenPaths;
    protected ValidatingTable table;
    protected JSplitPane      splitPane;
    protected DataRepository  data;
    protected Vector          currentLog   = new Vector();
    protected DropDownButton  importButton;
    protected JButton editButton, deleteButton, closeButton, dtsEditButton;
    protected JComboBox dtsSelector;
//  protected UserWarning     warnUser;

    Resources resources = Resources.getDashBundle("Defects");
    String inheritTypeSelection, typeSelectionTooltip;
    boolean buildingDtsSelector = false;


    static final String DTS_EDIT_URL = "/dash/dtsEdit.class";


                                  // constructor
    public DefectLogEditor(ProcessDashboard dash,
                        ConfigureButton button,
                        DashHierarchy props,
                        PropertyKey currentPhase) {
        dashboard        = dash;
        JPanel   panel   = new JPanel(true);

        useProps = props;
        props.addHierarchyListener(this);
        data     = dashboard.getDataRepository();

        defectLogs = new Hashtable ();
        forbiddenPaths = dashboard.getBrokenDataPaths();
        reload ();

        frame = new JFrame(resources.getString("Log.Window_Title"));
        DashboardIconFactory.setWindowIcon(frame);
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

        setSelectedPhase(currentPhase);
        loadCustomDimensions();
        splitPane.setDividerLocation(dividerLocation);

        DefectLog.addDefectLogListener(this);

        applyFilter();
        //frame.pack();
        frame.setSize(new Dimension(frameWidth, frameHeight));
        frame.setVisible(true);
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
        inheritTypeSelection = resources.getString("Log.Inherit_Type");
        typeSelectionTooltip = resources.getString("Log.Type_Selector_Tooltip");

        JPanel selectorPanel = new JPanel(false);
        selectorPanel.setLayout(new BorderLayout(5, 5));
        selectorPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        JLabel typeLabel = new JLabel
            (resources.getString("Log.Type_Selector_Label"));
        typeLabel.setToolTipText(typeSelectionTooltip);
        selectorPanel.add(typeLabel, BorderLayout.WEST);

        dtsSelector = new JComboBox();
        dtsSelector.setRenderer(new DtsListCellRenderer());
        dtsSelector.setActionCommand ("dts");
        dtsSelector.addActionListener (this);
        dtsSelector.setMinimumSize(dtsSelector.getPreferredSize());
        selectorPanel.add(dtsSelector, BorderLayout.CENTER);

        dtsEditButton = new JButton(resources.getString("Log.Edit_Types_Button"));
        dtsEditButton.setToolTipText
            (resources.getString("Log.Edit_Types_Tooltip"));
        dtsEditButton.setActionCommand ("dtsEdit");
        dtsEditButton.addActionListener (this);
        selectorPanel.add(dtsEditButton, BorderLayout.EAST);

        JPanel result = new JPanel();
        result.setLayout(new BorderLayout());
        result.setPreferredSize(new Dimension(300, 300));
        result.add("Center", sp);
        if (Settings.isReadWrite())
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
        boolean isInlineType = (selectedType != null && selectedType.startsWith(" "));
        int selectedItem = 0;

        String[] standards = DefectTypeStandard.getDefinedStandards(data);
        dtsSelector.removeAllItems();
        dtsSelector.addItem(isInlineType ? selectedType : inheritTypeSelection);

        for (int i = 0;   i < standards.length;   i++) {
            dtsSelector.addItem(standards[i]);
            if (selectedType != null && selectedType.equals(standards[i]))
                selectedItem = i+1;
        }

        if (selectedItem == 0 && selectedType != null && !isInlineType) {
            selectedItem = dtsSelector.getItemCount();
            dtsSelector.addItem("\t" + selectedType);
            dtsSelector.setForeground(Color.red);
            dtsSelector.setToolTipText
                (resources.getString("Log.Nonexistent_Type_Tooltip"));
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
            if (value instanceof String) {
                String s = (String) value;
                if (s.startsWith("\t")) {
                    hasError = true;
                    value = s.substring(1);
                } else if (s.startsWith(" ")) {
                    value = s.substring(1);
                }
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
        DefectListID dlid;
        Enumeration values = defectLogs.elements();
        while (values.hasMoreElements()) {
            dlid = (DefectListID)values.nextElement();
            if (areEqual(dlid.dl.getDefectLogFilename(), dl.getDefectLogFilename()) &&
                areEqual(dlid.dl.getDataPrefix(), dl.getDataPrefix())) {
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

    public void defectUpdated(DefectLog log, Defect d) {
        updateDefectLog(log, d);
    }

    public void hierarchyChanged(DashHierarchy.Event e) { reloadAll(null); }

    public void reloadAll(DashHierarchy newProps) {
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
                                    data);
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
            frame.setVisible(true);
        }
    }

    public void quit() {
        saveCustomDimensions();
    }

    public void handleApplicationEvent(ActionEvent e) {
        if (APP_EVENT_SAVE_ALL_DATA.equals(e.getActionCommand())) {
            saveCustomDimensions();
        }
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
        PropertyKey selectedKey = null, defectLogKey = null;
        PropertyKey key = null;
        DefaultMutableTreeNode selected = getSelectedNode();
        String extraPathFilter = null;
        if (selected != null) {
            key = selectedKey = treeModel.getPropKey (useProps, selected.getPath());
            if (key != null) {
                String selectedPath = key.path();
                DefectLogID logid = useProps.defectLog(key, "unimportant");
                if (logid != null) {
                    defectLogKey = logid.path;
                    if (logid.path != key) {
                        key = logid.path;
                        String defectLogPath = key.path();
                        extraPathFilter = selectedPath.substring(defectLogPath.length()+1);
                    } else {
                        PropertyKey currPhase = dashboard.getCurrentPhase();
                        if (currPhase != null && currPhase.getParent().equals(selectedKey))
                            selectedKey = currPhase;
                    }
                }
            }
        }
        updateImportActions(selectedKey, defectLogKey);
        // apply the filter and load the vector (and the table)
        VTableModel model = (VTableModel)table.table.getModel();
        Object[] row = new Object [11];
        DefectListID dli;
        DefectListEntry dle;
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
                row [3] = dle.defect.injected.phaseName;
                row [4] = dle.defect.removed.phaseName;
                row [5] = dle.defect.getLocalizedFixTime();
                row [6] = Integer.toString(dle.defect.fix_count);
                row [7] = dle.defect.fix_defect;
                row [8] = dle.defect.fix_pending ? "*" : "";
                row [9] = dle.defect.description;
                row [10] = FormatUtil.formatDate (dle.defect.date);
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

    private DefectDialog getDialogForDefect(DefectListEntry dle, boolean create) {
        return DefectDialog.getDialogForDefect
            (dashboard,
             dashboard.getDirectory() + useProps.pget(dle.pk).getDefectLog(),
             dle.pk,
             dle.defect,
             create);
    }


    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (cmd == null) return;

        DefectDialog dlg;
        DefectListEntry dle;

        if (cmd.equals("edit")) {
            if (Settings.isReadOnly()) return;
            dle = getSelectedDefect();
            if (dle != null) {
                dlg = getDialogForDefect(dle, true);
                dlg.setTitle(dle.pk.path());
                dlg.toFront();
            }

        } else if (cmd.equals("delete")) {
            if (Settings.isReadOnly()) return;
            dle = getSelectedDefect();
            String number = dle.defect.number;
            if (number != null) {
                // display a confirmation dialog
                if (JOptionPane.showConfirmDialog
                    (frame,
                     resources.format("Log.Confirm_Delete_Message_FMT", number),
                     resources.getString("Log.Confirm_Delete_Title"),
                     JOptionPane.YES_NO_OPTION) ==
                    JOptionPane.YES_OPTION)
                    dle.dl.deleteDefect(number);
                dlg = getDialogForDefect(dle, false);
                if (dlg != null) dlg.dispose();
            }

        } else if (cmd.equals("close")) {
            frame.setVisible(false);

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
            if (Settings.isReadOnly()) return;
            if (buildingDtsSelector) return;
            String type = (String) dtsSelector.getSelectedItem();
            if (type == inheritTypeSelection || type.startsWith(" ")) type = null;
            if (type == null || !type.startsWith("\t"))
                DefectTypeStandard.saveDefault(data, getSelectedPath(), type);
            refreshDefectTypeSelector();

        } else if (cmd.equals("dtsEdit")) {
            Browser.launch(DTS_EDIT_URL);
        }
    }


    private JPanel constructEditPanel () {
        JPanel  retPanel = new JPanel(false);

        String[] columns = new String[] {
            "Project", "ID", "Type", "Injected", "Removed",
            "Time", "Count", "Fix", "Pending", "Description", "Date" };

        retPanel.setLayout(new BorderLayout());
        table = new ValidatingTable
            (resources.getStrings("Columns.", columns, ".Name"),
             null,
             resources.getInts("Columns.", columns, ".Width_"),
             resources.getStrings("Columns.", columns, ".Tooltip"),
             null, null, 0, true, null,
             new boolean[] {false, false, false, // no columns editable
                            false, false, false, false, false,
                            false, false, false});
        DefectCellRenderer rend = new DefectCellRenderer();
        for (int col = 2;  col < 5;  col++)
            table.table.getColumnModel().getColumn(col).setCellRenderer(rend);
        table.table.setRowSelectionAllowed (false);
        table.table.getSelectionModel().addListSelectionListener(this);
        table.table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)
                    editButton.doClick();
            } } );

        retPanel.add ("Center", table);

        JPanel btnPanel = new JPanel(false);

        importButton = createImportButton();
        if (importButton != null && Settings.isReadWrite()) {
            btnPanel.add(importButton);
            btnPanel.add(Box.createHorizontalStrut(100));
        }

                                    // Should only be available if one
                                    // entry is selected
        editButton = new JButton (resources.getString("Edit"));
        editButton.setActionCommand ("edit");
        editButton.addActionListener (this);
        editButton.setEnabled (false);
        if (Settings.isReadWrite())
            btnPanel.add (editButton);

                                    // Should only be available if one
                                    // entry is selected
        deleteButton = new JButton (resources.getString("Delete"));
        deleteButton.setActionCommand ("delete");
        deleteButton.addActionListener (this);
        deleteButton.setEnabled (false);
        if (Settings.isReadWrite())
            btnPanel.add (deleteButton);

        closeButton = new JButton (resources.getString("Close"));
        closeButton.setActionCommand ("close");
        closeButton.addActionListener (this);
        btnPanel.add (closeButton);

        retPanel.add ("South", btnPanel);

        return retPanel;
    }

    private class DefectCellRenderer extends DefaultTableCellRenderer {
        private Font boldFont = null;

        public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus, int row,
                    int column) {
            if (value == null || value.toString().trim().length() == 0)
                value = Defect.UNSPECIFIED;

            Component result = super.getTableCellRendererComponent(table,
                        value, isSelected, hasFocus, row, column);

            if (Defect.UNSPECIFIED.equals(value)) {
                result.setForeground(Color.RED);
                if (boldFont == null)
                    boldFont = table.getFont().deriveFont(Font.BOLD);
                result.setFont(boldFont);
            } else {
                result.setForeground(Color.BLACK);
                result.setFont(table.getFont());
            }

            return result;
        }

    }

    private DropDownButton createImportButton() {
        List importActions = ExtensionManager.getExecutableExtensions(
                "defect-importer", dashboard);
        if (importActions.isEmpty())
            return null;

        DropDownButton result = new DropDownButton(resources
                .getString("Log.Import_Button"));
        result.setMainButtonBehavior(DropDownButton.OPEN_DROP_DOWN_MENU);
        result.getMenu().getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent e) {}
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                reupdateImportActions();
            }});

        for (Iterator i = importActions.iterator(); i.hasNext();) {
            Action a = (Action) i.next();
            if (a.getValue(IMPORT_ACTION_INVALID) != null)
                i.remove();
            else
                result.getMenu().add(a);
        }

        if (result.isEmpty())
            return null;
        else
            return result;
    }

    private void reupdateImportActions() {
        DefaultMutableTreeNode selectedTreeNode = getSelectedNode();
        if (selectedTreeNode == null)
            return;
        PropertyKey selectedKey = treeModel.getPropKey(useProps,
                  selectedTreeNode.getPath());
        if (selectedKey == null || hasDefLog(selectedKey) == false)
            return;

        PropertyKey currPhase = dashboard.getCurrentPhase();
        if (currPhase == null)
            return;

        if (currPhase.path().startsWith(selectedKey.path()+"/"))
            updateImportActions(currPhase, selectedKey);
    }

    private void updateImportActions(PropertyKey selectedKey,
            PropertyKey defectLogKey) {
        if (importButton == null)
            return;

        boolean enable = false;
        JMenu importMenu = importButton.getMenu();
        for (int i = 0;  i < importMenu.getItemCount();  i++) {
            JMenuItem item = importMenu.getItem(i);
            if (item == null) continue;
            Action a = item.getAction();
            if (a == null) continue;
            updateImportAction(a, selectedKey, defectLogKey);
            if (a.isEnabled())
                enable = true;
        }
        if (selectedKey != null
                && Filter.matchesFilter(forbiddenPaths, selectedKey.path()))
            enable = false;
        importButton.setEnabled(enable);
    }
    private void updateImportAction(Action a, PropertyKey selectedKey,
              PropertyKey defectLogKey) {
        a.putValue(IMPORT_ACTION_SEL_PATH,
                selectedKey == null ? null : selectedKey.path());
        a.putValue(IMPORT_ACTION_DEF_PATH,
                defectLogKey == null ? null : defectLogKey.path());
    }

    private void enableButtons(boolean enable)
    {
        editButton.setEnabled (enable);
        deleteButton.setEnabled (enable);
    }

    private void maybeEnableButtons() {
        enableButtons(selectedDefectIsEditable());
    }

    private boolean selectedDefectIsEditable() {
        if (table.table.getSelectedRowCount() != 1)
            return false;
        DefectListEntry dle = getSelectedDefect();
        if (dle == null)
            return false;
        if (Filter.matchesFilter(forbiddenPaths, dle.pk.path()))
            return false;
        return true;
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

        applyFilter ();
        refreshDefectTypeSelector();
    }

    public void valueChanged(ListSelectionEvent e) {
        maybeEnableButtons();
    }

    public static void rename(DashHierarchy oldProps, DashHierarchy newProps,
                              String oldPrefix, String newPrefix,
                              ProcessDashboard dashboard) {
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

        DefectLog log = new DefectLog(newLog.filename, newLogPath, null);
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
