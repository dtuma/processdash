// Copyright (C) 2001-2019 Tuma Solutions, LLC
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


package net.sourceforge.processdash.ev.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.AbstractCellEditor;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.TransferHandler;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.w3c.dom.Element;

import com.xduke.xswing.DataTipManager;

import net.sourceforge.processdash.ApplicationEventListener;
import net.sourceforge.processdash.ApplicationEventSource;
import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.ev.DefaultTaskLabeler;
import net.sourceforge.processdash.ev.EVCalculator;
import net.sourceforge.processdash.ev.EVDependencyCalculator;
import net.sourceforge.processdash.ev.EVHierarchicalFilter;
import net.sourceforge.processdash.ev.EVMetadata;
import net.sourceforge.processdash.ev.EVMetrics;
import net.sourceforge.processdash.ev.EVPermissions;
import net.sourceforge.processdash.ev.EVSchedule;
import net.sourceforge.processdash.ev.EVSnapshot;
import net.sourceforge.processdash.ev.EVTask;
import net.sourceforge.processdash.ev.EVTaskFilter;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.EVTaskListCached;
import net.sourceforge.processdash.ev.EVTaskListData;
import net.sourceforge.processdash.ev.EVTaskListFilter;
import net.sourceforge.processdash.ev.EVTaskListGroupFilter;
import net.sourceforge.processdash.ev.EVTaskListRollup;
import net.sourceforge.processdash.ev.Milestone;
import net.sourceforge.processdash.ev.MilestoneList;
import net.sourceforge.processdash.hier.HierarchyNote;
import net.sourceforge.processdash.hier.HierarchyNoteManager;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.hier.ui.HierarchyNoteEditorDialog;
import net.sourceforge.processdash.hier.ui.icons.HierarchyNoteIcon;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.cache.CachedObject;
import net.sourceforge.processdash.net.cache.CachedURLObject;
import net.sourceforge.processdash.team.group.GroupPermission;
import net.sourceforge.processdash.team.group.UserFilter;
import net.sourceforge.processdash.team.group.UserGroup;
import net.sourceforge.processdash.team.group.UserGroupManager;
import net.sourceforge.processdash.team.group.ui.GroupFilterMenu;
import net.sourceforge.processdash.templates.ExtensionManager;
import net.sourceforge.processdash.tool.perm.PermissionsChangeEvent;
import net.sourceforge.processdash.tool.perm.PermissionsChangeListener;
import net.sourceforge.processdash.tool.perm.PermissionsManager;
import net.sourceforge.processdash.ui.Browser;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.NodeSelectionDialog;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.ui.lib.DeferredSelectAllExecutor;
import net.sourceforge.processdash.ui.lib.DropDownButton;
import net.sourceforge.processdash.ui.lib.ErrorReporter;
import net.sourceforge.processdash.ui.lib.GuiPrefs;
import net.sourceforge.processdash.ui.lib.JDateTimeChooserCellEditor;
import net.sourceforge.processdash.ui.lib.JDialogCellEditor;
import net.sourceforge.processdash.ui.lib.JOptionPaneClickHandler;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;
import net.sourceforge.processdash.ui.lib.JTableColumnVisibilityAction;
import net.sourceforge.processdash.ui.lib.JTableColumnVisibilityButton;
import net.sourceforge.processdash.ui.lib.JTreeTable;
import net.sourceforge.processdash.ui.lib.PaddedIcon;
import net.sourceforge.processdash.ui.lib.PaintUtils;
import net.sourceforge.processdash.ui.lib.ScalableImageIcon;
import net.sourceforge.processdash.ui.lib.TableUtils;
import net.sourceforge.processdash.ui.lib.ToolTipTableCellRendererProxy;
import net.sourceforge.processdash.ui.lib.ToolTipTimingCustomizer;
import net.sourceforge.processdash.ui.lib.TreeModelWillChangeListener;
import net.sourceforge.processdash.ui.lib.TreeTableModel;
import net.sourceforge.processdash.ui.lib.WindowUtils;
import net.sourceforge.processdash.ui.lib.WindowsGUIUtils;
import net.sourceforge.processdash.ui.macosx.MacGUIUtils;
import net.sourceforge.processdash.util.BooleanArray;
import net.sourceforge.processdash.util.Disposable;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.PreferencesUtils;
import net.sourceforge.processdash.util.StringUtils;


public class TaskScheduleDialog implements EVTask.Listener,
        EVTaskList.RecalcListener, EVSchedule.Listener,
        ApplicationEventListener {

    /** Model for the JTreeTable */
    protected EVTaskList model;
    /** TreeTable displaying the task list */
    protected JTreeTable treeTable;
    /** table displaying the schedule */
    protected JTable scheduleTable;
    /** Frame containing everything */
    protected JFrame frame;
    /** object managing preferred sizes/states of GUI elements */
    protected GuiPrefs guiPrefs;
    /** the dashboard */
    protected DashboardContext dash;
    protected String taskListName;
    private boolean hasRollupEditPerm;

    private EVTaskList.FlatTreeModel flatModel = null;
    private TreeTableModel mergedModel = null;
    private TableColumnModel treeColumnModel;
    private TableColumnModel flatColumnModel = null;
    private TableColumn sortTagColumn;

    private Milestone activeMilestone = null;
    private Set<Milestone> previousMilestones = null;
    private Date activeMilestoneDate = null;
    private int activeMilestoneDateRow = -1;

    private TSAction addTaskAction, deleteTaskAction, moveUpAction,
            moveDownAction, flatViewAction, mergedViewAction, addPeriodAction,
            insertPeriodAction, deletePeriodAction, chartAction, reportAction,
            closeAction, saveAction, errorAction, filteredChartAction,
            saveBaselineAction, collaborateAction, filteredReportAction,
            weekReportAction, scheduleOptionsAction, expandAllAction,
            showTimeLogAction, showDefectLogAction, copyTaskInfoAction,
            manageBaselinesAction, sortTasksAction;
    private List<TSAction> altReportActions;
    private GroupFilterMenu groupFilterMenu;

    protected boolean disableTaskPruning;

    protected JFrame chartDialog = null;

    protected TaskScheduleOptions optionsDialog = null;

    protected static final Resources resources = Resources.getDashBundle("EV");

    private static Preferences preferences = Preferences.userNodeForPackage(TaskScheduleDialog.class);
    private static final String EXPANDED_NODES_KEY_SUFFIX = "_EXPANDEDNODES";
    private static final String EXPANDED_NODES_DELIMITER = Character.toString('\u0001');

    /**  The format of the string used to validate dates entered manually */
    private static final String DATE_FORMAT = "yyyy-MM-dd h:mm:ss aa";

    public TaskScheduleDialog(DashboardContext dash, String taskListName,
                              boolean createRollup) {
        this(dash, taskListName, createRollup, false, true);
    }

    protected TaskScheduleDialog(DashboardContext dash, String taskListName,
            boolean createRollup, boolean requireExisting, boolean showWindow) {
        this.dash = dash;
        this.taskListName = taskListName;
        this.hasRollupEditPerm = PermissionsManager.getInstance()
                .hasPermission(EVPermissions.ROLLUPS);

        if (dash instanceof ApplicationEventSource) {
            ((ApplicationEventSource) dash).addApplicationEventListener(this);
        }

        // Create the frame and set an appropriate icon
        frame = new JFrame(resources.getString("Window_Title"));
        DashboardIconFactory.setWindowIcon(frame);
        PCSH.enableHelpKey(frame, "UsingTaskSchedule");

        // Try to open an existing earned value model.
        model = EVTaskList.openExisting
            (taskListName, dash.getData(), dash.getHierarchy(),
             dash.getCache(), true);

        // If the earned value model doesn't already exist, create a new one.
        if (model == null) {
            if (requireExisting)
                throw new IllegalArgumentException(
                        "Task list " + taskListName + " not found");
            else if (createRollup)
                model = new EVTaskListRollup
                    (taskListName, dash.getData(), dash.getHierarchy(),
                     dash.getCache(),true);
            else
                model = new EVTaskListData
                    (taskListName, dash.getData(), dash.getHierarchy(), true);
        }

        EVDependencyCalculator depCalc = new EVDependencyCalculator(
                dash.getData(), dash.getHierarchy(), dash.getCache());
        model.setDependencyCalculator(depCalc);
        model.setTaskLabeler(new DefaultTaskLabeler(dash.getHierarchy(), //
                dash.getData(), model));

        model.recalc();
        model.setNodeListener(this);
        model.addRecalcListener(this);
        model.getSchedule().setListener(this);

        // Create the GUI preferences object
        guiPrefs = new GuiPrefs(TaskScheduleDialog.class, model.getID());

        // Create a JTreeTable to display the task list.
        treeTable = new TaskJTreeTable(model);
        treeTable.setShowGrid(true);
        treeTable.setIntercellSpacing(new Dimension(1, 1));
        treeTable.getSelectionModel().setSelectionMode
            (ListSelectionModel.SINGLE_INTERVAL_SELECTION);
                                // add tool tips to the table header.
        ToolTipTableCellRendererProxy.installHeaderToolTips
            (treeTable, EVTaskList.toolTips);
                                // identify columns that shouldn't appear
        Set hiddenCols = new HashSet();
        if (!Settings.getBool("ev.showCumulativeTaskData", false)) {
            hiddenCols.add(new Integer(EVTaskList.PLAN_CUM_TIME_COLUMN));
            hiddenCols.add(new Integer(EVTaskList.PLAN_CUM_VALUE_COLUMN));
        }
        if (!(model instanceof EVTaskListRollup))
            hiddenCols.add(new Integer(EVTaskList.ASSIGNED_TO_COLUMN));
        if (Settings.isReadOnly() || !(model instanceof EVTaskListData))
            hiddenCols.add(new Integer(EVTaskList.SORT_TAG_COLUMN));

                                // set default widths for the columns
        int totalWidth = 0;
        treeColumnModel = treeTable.getColumnModel();
        for (int i = treeColumnModel.getColumnCount();  i-- > 0; ) {
            TableColumn column = treeColumnModel.getColumn(i);
            if (hiddenCols.contains(new Integer(i))) {
                treeColumnModel.removeColumn(column);
            } else {
                column.setIdentifier(EVTaskList.COLUMN_KEYS[i].toLowerCase());
                int width = EVTaskList.colWidths[i];
                column.setPreferredWidth(width);
                if (i != EVTaskList.SORT_TAG_COLUMN)
                    totalWidth += width;
            }
        }
        configureEditor(treeTable);

        if (!Settings.getBool("datatips.disabled", false)) {
            try {
                DataTipManager.get().register(treeTable);
            } catch (Throwable t) {
                // In some runtime environments, this call may fail with a
                // security exception.  Gracefully degrade and display no
                // datatips.
            }
        }

        model.addTreeModelWillChangeListener((TaskJTreeTable)treeTable);
        model.addTreeModelListener((TaskJTreeTable)treeTable);

        // Create a JTable to display the schedule list.
        scheduleTable = new ScheduleJTable(model.getSchedule());
                                // add tool tips to the table header.
        ToolTipTableCellRendererProxy.installHeaderToolTips
            (scheduleTable, model.getSchedule().getColumnTooltips());
                                // set default widths for the columns
        for (int i = 0;  i < EVSchedule.colWidths.length;  i++) {
            TableColumn column = scheduleTable.getColumnModel().getColumn(i);
            column.setIdentifier(EVSchedule.COLUMN_KEYS[i].toLowerCase());
            column.setPreferredWidth(EVSchedule.colWidths[i]);
        }
        configureEditor(scheduleTable);

        // hide columns such as direct time or labels if they aren't needed
        showHideColumns();
        flatColumnModel = createFlatColumnModel();

        // record user preference for pruning enablement
        disableTaskPruning = Settings.getBool("ev.disablePruning", false);

        boolean isRollup = isRollup();
        if (isRollup)
            frame.setTitle(resources.getString("Window_Rollup_Title"));
        maybeReadExpandedNodesPref(model.getID());

        JScrollPane sp;
        sp = new JScrollPane(treeTable,
                             JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                             JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setPreferredSize(new Dimension(10, 10));
        sp.setCorner(JScrollPane.UPPER_RIGHT_CORNER,
            new JTableColumnVisibilityButton(treeTable, resources, " .*",
                    EVTaskList.TASK_COLUMN));
        Box topBox = newVBox
            (sp,
             Box.createVerticalStrut(2),
             buildTaskButtons(isRollup),
             Box.createVerticalStrut(2));

        sp = new JScrollPane(scheduleTable,
                             JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                             JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setPreferredSize(new Dimension(10, 10));
        Box bottomBox = newVBox
            (sp,
             Box.createVerticalStrut(2),
             isRollup || Settings.isReadOnly() ? null : buildScheduleButtons(),
             isRollup || Settings.isReadOnly() ? null : Box.createVerticalStrut(2),
             buildMainButtons(isRollup));

        JSplitPane jsp = new JSplitPane
            (JSplitPane.VERTICAL_SPLIT, true, topBox, bottomBox);
        jsp.setResizeWeight(0.5);
        frame.getContentPane().add(jsp);

        frame.setJMenuBar(buildMenuBar());

        setDirty(false);


        frame.addWindowListener( new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    confirmClose(true); }});
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        frame.setSize(new Dimension(totalWidth + 20, 600));

        // hide the "sort tag" column by default
        if (sortTagColumn != null)
            treeColumnModel.removeColumn(sortTagColumn);

        guiPrefs.load("treeTable", treeTable);
        guiPrefs.load("scheduleTable", scheduleTable);
        guiPrefs.load(frame);
        guiPrefs.load(jsp);
        if (flatViewAction != null) {
            guiPrefs.load("flatView", flatViewAction.buttonModel);
            if (isFlatView()) toggleFlatView();
        }

        int currentWeek = model.getSchedule().getRowForEffectivePeriod();
        if (currentWeek >= 0 && currentWeek < scheduleTable.getRowCount()) {
            scheduleTable.addRowSelectionInterval(currentWeek, currentWeek);
            scheduleTable.scrollRectToVisible(scheduleTable.getCellRect(
                currentWeek, 0, true));
        }

        if (!showWindow)
            return;

        WindowUtils.showWindowToFront(frame);

        // if the task list is empty, open the add task dialog immediately.
        if (((EVTask) model.getRoot()).isLeaf() && canEdit())
            addTask();
        else {
            if (getErrors() != null)
                SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            Map errors = getErrors();
                            highlightErrors(errors);
                            displayErrorDialog(errors);
                        } } );
        }
    }


    private boolean isRollup() {
        return (model instanceof EVTaskListRollup);
    }

    protected boolean isFlatView() {
        return (flatViewAction != null && flatViewAction.isSelected());
    }

    protected boolean isMergedView() {
        return (mergedViewAction != null && mergedViewAction.isSelected());
    }

    protected boolean canEdit() {
        if (Settings.isReadOnly())
            return false;
        else if (!isRollup())
            return true;
        else
            return hasRollupEditPerm;
    }

    protected void stopCellEditing() {
        if (treeTable.isEditing())
            treeTable.getCellEditor().stopCellEditing();
        if (scheduleTable.isEditing())
            scheduleTable.getCellEditor().stopCellEditing();
    }

    private boolean isDirty = false;
    protected void setDirty(boolean dirty) {
        isDirty = dirty;
        saveAction.setEnabled(isDirty);
        closeAction.setText(isDirty ? resources.getString("Cancel")
                                    : resources.getString("Close"));
        MacGUIUtils.setDirty(frame, isDirty);
    }

    private Set<EVTaskList> dirtySubschedules;
    void recordDirtySubschedule(EVTaskList tl) {
        if (dirtySubschedules == null)
            dirtySubschedules = new HashSet();
        dirtySubschedules.add(tl);
        setDirty(true);
    }

    protected Component buildTaskButtons(boolean isRollup) {
        Box result = Box.createHorizontalBox();
        result.add(Box.createHorizontalGlue());
        addTaskAction = new TSAction("Buttons.Add_Schedule", "Buttons.Add_Task") {
            public void actionPerformed(ActionEvent e) {
                addTask(); }};
        if (canEdit())
            result.add(new JButton(addTaskAction));
        result.add(Box.createHorizontalGlue());

        // button margins: 2 pixels top and bottom, 14 left and right.

        deleteTaskAction = new TSAction("Buttons.Remove_Schedule",
                "Buttons.Remove_Task") {
            public void actionPerformed(ActionEvent e) {
                deleteTask(); }};
        deleteTaskAction.setEnabled(false);
        if (canEdit())
            result.add(new JButton(deleteTaskAction));
        result.add(Box.createHorizontalGlue());


        moveUpAction = new TSAction("Buttons.Move_Schedule_Up",
                "Buttons.Move_Task_Up") {
            public void actionPerformed(ActionEvent e) {
                moveTaskUp(); }};
        moveUpAction.setEnabled(false);
        moveUpAction.setMnemonic('U');
        if (canEdit())
            result.add(new JButton(moveUpAction));
        result.add(Box.createHorizontalGlue());

        moveDownAction = new TSAction("Buttons.Move_Schedule_Down",
                "Buttons.Move_Task_Down") {
            public void actionPerformed(ActionEvent e) {
                moveTaskDown(); }};
        moveDownAction.setEnabled(false);
        moveDownAction.setMnemonic('D');
        if (canEdit())
            result.add(new JButton(moveDownAction));
        result.add(Box.createHorizontalGlue());

        sortTasksAction = new TSAction("Buttons.Sort_Tasks.Label") {
            public void actionPerformed(ActionEvent e) {
                sortTasks(); }};
        final JButton sortTasksButton = new JButton(sortTasksAction);
        sortTasksAction.addPropertyChangeListener(new PropertyChangeListener(){
            public void propertyChange(PropertyChangeEvent evt) {
                sortTasksButton.setVisible(sortTasksAction.isEnabled());
            }});
        sortTasksAction.setEnabled(false);
        if (!isRollup() && canEdit()) {
            result.add(sortTasksButton);
            result.add(Box.createHorizontalGlue());
        }

        flatViewAction = mergedViewAction = null;
        if (!isRollup) {
            flatViewAction = new TSAction("Buttons.Flat_View") {
                public void actionPerformed(ActionEvent e) {
                    toggleFlatView(); }};
            JCheckBox flatViewCheckbox = new JCheckBox(flatViewAction);
            flatViewCheckbox.setFocusPainted(false);
            flatViewAction.useButtonModel(flatViewCheckbox);
            flatViewAction.setSelected(false);
            result.add(flatViewCheckbox);
            result.add(Box.createRigidArea(new Dimension(30, 0)));
        } else {
            mergedViewAction = new TSAction("Buttons.Merged_View") {
                public void actionPerformed(ActionEvent e) {
                    toggleMergedView(); }};
            JCheckBox mergedViewCheckbox = new JCheckBox(mergedViewAction);
            mergedViewCheckbox.setFocusPainted(false);
            mergedViewAction.useButtonModel(mergedViewCheckbox);
            mergedViewAction.setSelected(false);
            result.add(mergedViewCheckbox);
            result.add(Box.createHorizontalGlue());
        }

        expandAllAction = new TSAction("Buttons.Expand_All") {
            public void actionPerformed(ActionEvent e) {
                expandAllForSelectedItems(); }};

        treeTable.getSelectionModel().addListSelectionListener
            (new ListSelectionListener() {
                    public void valueChanged(ListSelectionEvent e) {
                        enableTaskButtons(e); }});
        return result;
    }


    protected Component buildScheduleButtons() {
        Box result = Box.createHorizontalBox();
        result.add(Box.createHorizontalGlue());

        addPeriodAction = new TSAction("Buttons.Add_Schedule_Row") {
            public void actionPerformed(ActionEvent e) {
                addScheduleRow(); }};
        result.add(new JButton(addPeriodAction));
        result.add(Box.createHorizontalGlue());

        insertPeriodAction = new TSAction("Buttons.Insert_Schedule_Row") {
            public void actionPerformed(ActionEvent e) {
                insertScheduleRow(); }};
        insertPeriodAction.setEnabled(false);
        result.add(new JButton(insertPeriodAction));
        result.add(Box.createHorizontalGlue());

        deletePeriodAction = new TSAction("Buttons.Delete_Schedule_Row") {
            public void actionPerformed(ActionEvent e) {
                deleteScheduleRows(); }};
        deletePeriodAction.setEnabled(false);
        result.add(new JButton(deletePeriodAction));
        result.add(Box.createHorizontalGlue());

        scheduleTable.getSelectionModel().addListSelectionListener
            (new ListSelectionListener() {
                    public void valueChanged(ListSelectionEvent e) {
                        enableScheduleButtons(); }});

        return result;
    }

    protected Component buildMainButtons(boolean isRollup) {
        JPanel result = new JPanel(new BorderLayout());
        result.setBorder(BorderFactory.createRaisedBevelBorder());

        Box box = Box.createHorizontalBox();
        result.add(newVBox(Box.createVerticalStrut(2),
                           box,
                           Box.createVerticalStrut(2)), BorderLayout.CENTER);
        box.add(Box.createHorizontalGlue());

        JPopupMenu popupMenu = treeTable.getComponentPopupMenu();

        /*
        if (isRollup) {
            recalcButton = new JButton("Refresh");
            recalcButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    recalcAll(); }});
            box.add(recalcButton);
        }
        */

        errorAction = new TSAction("Buttons.Errors") {
            public void actionPerformed(ActionEvent e) {
                displayErrorDialog(getErrors()); }};
        final JButton errorButton = new JButton(errorAction);
        if (WindowsGUIUtils.isWindowsLAF()) {
            errorButton.setIcon(new RedFillIcon());
            errorButton.setIconTextGap(0);
        } else {
            errorButton.setBackground(Color.red);
        }
        errorButton.setFocusPainted(false);
        errorAction.addPropertyChangeListener(new PropertyChangeListener(){
            public void propertyChange(PropertyChangeEvent evt) {
                errorButton.setVisible(errorAction.isEnabled());
            }});
        errorAction.setEnabled(getErrors() != null);
        box.add(errorButton);
        box.add(Box.createHorizontalStrut(2));

        filteredChartAction = new TSAction("Buttons.Filtered_Chart") {
            public void actionPerformed(ActionEvent e) {
                showFilteredChart(); }};
        filteredChartAction.setEnabled(false);
        addRenamedPopupMenuItem(popupMenu, filteredChartAction,
            "Buttons.View_Filtered_Chart");
        chartAction = new TSAction("Buttons.Chart") {
            public void actionPerformed(ActionEvent e) {
                showChart(); }};
        box.add(makeDropDownButton(chartAction, filteredChartAction));
        box.add(Box.createHorizontalStrut(2));

        altReportActions = buildAltReportActions();
        weekReportAction = new TSAction("Buttons.Weekly_Report") {
            public void actionPerformed(ActionEvent e) {
                showWeekReport(); }};
        filteredReportAction = new TSAction("Buttons.Filtered_Report") {
            public void actionPerformed(ActionEvent e) {
                showFilteredHTML(); }};
        filteredReportAction.setEnabled(false);
        addRenamedPopupMenuItem(popupMenu, filteredReportAction,
            "Buttons.View_Filtered_Report");
        reportAction = new TSAction("Buttons.Report") {
            public void actionPerformed(ActionEvent e) {
                showHTML(); }};
        box.add(makeDropDownButton(reportAction, weekReportAction,
            altReportActions, filteredReportAction));
        box.add(Box.createHorizontalStrut(2));

        closeAction = new TSAction("Close") {
            public void actionPerformed(ActionEvent e) {
                confirmClose(true); }};
        box.add(new JButton(closeAction));
        box.add(Box.createHorizontalStrut(2));

        saveAction = new TSAction("Save") {
            public void actionPerformed(ActionEvent e) {
                save(); }};
        if (canEdit()) {
            box.add(new JButton(saveAction));
            box.add(Box.createHorizontalStrut(2));
        }

        Dimension size = result.getMinimumSize();
        size.width = 2000;
        result.setMaximumSize(size);

        return result;
    }
    private void addRenamedPopupMenuItem(JPopupMenu m, Action a, String resKey) {
        JMenuItem menuItem = new JMenuItem(a);
        menuItem.setText(resources.getString(resKey));
        m.insert(menuItem, m.getComponentCount() - 1);
    }

    private List<TSAction> buildAltReportActions() {
        List<TSAction> result = new ArrayList();
        List<Element> altViews = ExtensionManager
                .getXmlConfigurationElements("ev-report-view");
        if (altViews != null)
            for (Element view : altViews)
                result.add(new ShowAltReportAction(view));
        return result;
    }


    private DropDownButton makeDropDownButton(Object... actionItems) {
        List actions = new ArrayList();
        for (Object item : actionItems) {
            if (item instanceof TSAction) {
                actions.add((TSAction) item);
            } else if (item instanceof List) {
                actions.addAll((List) item);
            }
        }
        TSAction[] actionArray = new TSAction[actions.size()];
        actionArray = (TSAction[]) actions.toArray(actionArray);
        return makeDropDownButton(actionArray);
    }

    private DropDownButton makeDropDownButton(TSAction... actions) {
        DropDownButton result = new DropDownButton(actions[0]);
        for (int i = 1;  i < actions.length;  i++)
            result.getMenu().add(actions[i]);
        result.setRunFirstMenuOption(false);
        return result;
    }

    private JMenuBar buildMenuBar() {
        JMenuBar result = new JMenuBar();
        boolean rw = canEdit();

        // create the File menu
        JMenu fileMenu = makeMenu("File");
        fileMenu.add(closeAction);
        if (rw) fileMenu.add(saveAction);
        result.add(fileMenu);

        // If we're in read-write mode, create the Edit menu.
        if (rw) {
            JMenu editMenu = makeMenu("Edit");
            editMenu.add(addTaskAction);
            editMenu.add(deleteTaskAction);
            editMenu.add(moveUpAction);
            editMenu.add(moveDownAction);
            if (!isRollup())
                editMenu.add(sortTasksAction);
            editMenu.add(copyTaskInfoAction);
            if (!isRollup()) {
                editMenu.addSeparator();
                editMenu.add(addPeriodAction);
                editMenu.add(insertPeriodAction);
                editMenu.add(deletePeriodAction);
            }
            result.add(editMenu);
        }

        // create the View menu
        JMenu viewMenu = makeMenu("View");
        viewMenu.add(expandAllAction);
        viewMenu.add(chartAction);
        viewMenu.add(filteredChartAction);
        viewMenu.add(reportAction);
        viewMenu.add(weekReportAction);
        for (TSAction t : altReportActions)
            viewMenu.add(t);
        viewMenu.add(filteredReportAction);
        viewMenu.add(errorAction);
        if (Settings.isPersonalMode()) {
            viewMenu.addSeparator();
            viewMenu.add(showTimeLogAction);
            viewMenu.add(showDefectLogAction);
        }
        viewMenu.addSeparator();
        viewMenu.add(JTableColumnVisibilityAction.getForTable(treeTable));
        viewMenu.add(new TSAction("Column_Chooser.Reset_Columns") {
            public void actionPerformed(ActionEvent e) {
                guiPrefs.reset("treeTable", "flatTable"); }});
        if (flatViewAction != null) {
            viewMenu.addSeparator();
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(flatViewAction);
            flatViewAction.installButtonModel(item);
            viewMenu.add(item);
        } else if (mergedViewAction != null) {
            viewMenu.addSeparator();
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(mergedViewAction);
            mergedViewAction.installButtonModel(item);
            viewMenu.add(item);
        }
        result.add(viewMenu);

        // create the Tools menu
        if (Settings.isReadWrite()) {
            JMenu toolsMenu = makeMenu("Tools");

            saveBaselineAction = new BaselineAction("Buttons.Save_Baseline") {
                public void actionPerformed(ActionEvent e) {
                    saveBaseline(); }};
            toolsMenu.add(saveBaselineAction);

            manageBaselinesAction = new BaselineAction("Buttons.Manage_Baselines") {
                public void actionPerformed(ActionEvent e) {
                    manageBaselines(); }};
            toolsMenu.add(manageBaselinesAction);

            if (rw) {
                if (!isCollaborationBlocked()) {
                    collaborateAction = new TSAction("Buttons.Collaborate") {
                        public void actionPerformed(ActionEvent e) {
                            showCollaborationWizard(); }};
                    toolsMenu.add(collaborateAction);
                }

                scheduleOptionsAction = new TSAction("Buttons.Schedule_Options") {
                    public void actionPerformed(ActionEvent e) {
                        showOptionsDialog();
                    }};
                toolsMenu.add(scheduleOptionsAction);
            }

            result.add(toolsMenu);
        }

        // create the Help menu
        JMenu helpMenu = makeMenu("Help_Menu.Title");
        JMenuItem usingTaskSchedule = new JMenuItem(resources
                .getString("Help_Menu.Using_Item"));
        usingTaskSchedule.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                PCSH.displayHelpTopic("UsingTaskSchedule"); }});
        helpMenu.add(usingTaskSchedule);
        result.add(helpMenu);

        // add a group selector, if applicable
        UserGroupManager groupMgr = UserGroupManager.getInstance();
        if (isRollup() && groupMgr.isEnabled()) {
            UserFilter filter = groupMgr.getGlobalFilter();
            groupFilterMenu = new GroupFilterMenu(filter,
                    groupMgr.isIndivFilteringSupported());
            groupFilterMenu.addGroupChangeListener( //
                new GroupFilterHandler(filter));

            result.add(Box.createHorizontalGlue());
            result.add(groupFilterMenu);
        }

        return result;
    }


    private boolean isCollaborationBlocked() {
        // collaboration feature is no longer supported in 2.0.14 and up
        return true;
    }


    private JMenu makeMenu(String resKey) {
        String text = resources.getString(resKey);
        JMenu result = new JMenu(text);
        result.setMnemonic(text.charAt(0));
        return result;
    }

    private class GroupFilterHandler implements ChangeListener {

        private GroupFilterHandler(UserFilter f) {
            if (!UserGroup.isEveryone(f))
                setGroupFilter(f);
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            GroupFilterMenu menu = (GroupFilterMenu) e.getSource();
            setGroupFilter(menu.getSelectedItem());
        }

        private void setGroupFilter(UserFilter f) {
            if (f != null) {
                EVTaskListRollup rollup = (EVTaskListRollup) model;
                rollup.applyTaskListFilter(new EVTaskListGroupFilter(f));
                chartAction.setEnabled(checkChartPermission(f));
                treeTable.getTree().expandRow(0);
                recalcAll();
                enableTaskButtons();
            }
        }

        private boolean checkChartPermission(UserFilter f) {
            // clear the tooltip for the chart action.
            chartAction.putValue(Action.SHORT_DESCRIPTION, null);

            // if no group filter is in effect, charts are always OK. (This
            // aligns with our strategy of not censoring "teams of one")
            if (UserGroup.isEveryone(f))
                return true;

            // if our current rollup includes more than one person, we're OK
            String personalDataID = model.getPersonalDataID();
            if (personalDataID == null)
                return true;

            // find out which people we have permission to view charts for. If
            // we have permission to view data for the given person, we're OK
            UserFilter pf = GroupPermission
                    .getGrantedMembers(EVPermissions.PERSONAL_CHARTS);
            if (UserGroup.isEveryone(pf))
                return true;
            EVTaskListFilter tlf = new EVTaskListGroupFilter(pf);
            if (tlf.include(personalDataID))
                return true;

            // the filter has narrowed data down to a single individual, whose
            // charts we do not have permission to view. Disable the charts.
            chartAction.putValue(Action.SHORT_DESCRIPTION,
                resources.getString("Buttons.Filtered_Chart_Restricted"));
            if (chartDialog != null)
                chartDialog.dispose();
            return false;
        }

    }

    private UserFilter getUserFilter() {
        if (groupFilterMenu == null)
            return null;
        else
            return groupFilterMenu.getSelectedItem();
    }

    private Box newHBox(Component a, Component b) {
        Box result = Box.createHorizontalBox();
        result.add(a); result.add(b);
        return result;
    }

    private Box newVBox(Component a, Component b, Component c) {
        return newVBox(a, b, c, null, null); }
    private Box newVBox(Component a, Component b, Component c, Component d) {
        return newVBox(a, b, c, d, null); }
    private Box newVBox(Component a, Component b, Component c, Component d,
                        Component e) {
        Box result = Box.createVerticalBox();
        if (a != null) result.add(a);
        if (b != null) result.add(b);
        if (c != null) result.add(c);
        if (d != null) result.add(d);
        if (e != null) result.add(e);
        return result;
    }

    public void show() {
        WindowUtils.showWindowToFront(frame);
    }

    protected Map getErrors() {
        return model.getSchedule().getMetrics().getErrors();
    }

    protected void highlightErrors(Map errors) {
        if (errors == null || errors.size() == 0) return;
        Iterator i = errors.values().iterator();
        while (i.hasNext()) {
            EVTask t = (EVTask) i.next();
            treeTable.getTree().makeVisible(new TreePath(t.getPath()));
        }
    }

    protected void displayErrorDialog(Map errors) {
        if (errors == null || errors.size() == 0) return;
        String[] footer = EVMetrics.isWarningOnly(errors) ? null : resources
                .getStrings("Error_Dialog.Foot");
        ErrorReporter err = new ErrorReporter
            (resources.getString("Error_Dialog.Title"),
             resources.getStrings("Error_Dialog.Head"),
             footer);
        Iterator i = errors.keySet().iterator();
        while (i.hasNext()) {
            err.logError(StringUtils.findAndReplace((String) i.next(), //
                "\n#", "\n#http://ignored/"));
        }
        err.setHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    String url = e.getURL().getFile();
                    int pos = url.lastIndexOf('/');
                    String helpSet = url.substring(0, pos);
                    String topic = url.substring(pos + 1);
                    String helpUri = helpSet + "/frame.html?" + topic;
                    Browser.launch(helpUri);
                }
            }
        });
        err.done();
    }

    public void setActiveMilestone(Milestone newMilestone) {
        if (!isFlatView())
            newMilestone = null;

        activeMilestone = newMilestone;
        activeMilestoneDate = null;
        activeMilestoneDateRow = -1;
        previousMilestones = new HashSet<Milestone>();

        if (newMilestone != null) {
            // gather the set of previous milestones
            Milestone m = newMilestone.getPreviousMilestone();
            while (m != null) {
                previousMilestones.add(m);
                m = m.getPreviousMilestone();
            }
            // find the earliest subsequent commit date
            m = newMilestone;
            while (m != null) {
                Date oneDate = m.getCommitDate();
                activeMilestoneDate = EVCalculator.minStartDate(
                    activeMilestoneDate, oneDate);
                m = m.getNextMilestone();
            }
            // recalculate the effective row for that commit date
            recalcActiveMilestoneDateRow();
        }

        treeTable.repaint();
    }

    private void toggleActiveMilestone(Milestone m) {
        if (activeMilestone != null && activeMilestone.equals(m))
            setActiveMilestone(null);
        else
            setActiveMilestone(m);
    }

    private void recalcActiveMilestoneDateRow() {
        if (flatModel == null || activeMilestoneDate == null)
            activeMilestoneDateRow = -1;
        else
            activeMilestoneDateRow = flatModel
                    .getIndexOfFirstTaskFinishingAfter(activeMilestoneDate);
    }

    abstract class TSAction extends AbstractAction {

        protected TSAction() {}
        public TSAction(String resKey) {
            this(resKey, resKey);
        }
        public TSAction(String rollupKey, String plainKey) {
            super(isRollup() ? resources.getString(rollupKey) : resources
                    .getString(plainKey));
        }
        public void setText(String text) {
            putValue(NAME, text);
        }
        public void setMnemonic(char c) {
            putValue(MNEMONIC_KEY, new Integer(c));
        }
        private ButtonModel buttonModel;
        public void useButtonModel(AbstractButton b) {
            this.buttonModel = b.getModel();
        }
        public void installButtonModel(AbstractButton b) {
            b.setModel(buttonModel);
        }
        public boolean isSelected() {
            return buttonModel.isSelected();
        }
        public void setSelected(boolean selected) {
            buttonModel.setSelected(selected);
        }
    }

    abstract class TSSelectedTaskAction extends TSAction {
        public TSSelectedTaskAction(String resKey) {
            super(resKey);
        }
        public void actionPerformed(ActionEvent e) {
            int selRow = treeTable.getSelectedRow();
            if (selRow == -1)
                return;
            EVTask task = (EVTask) treeTable.getValueAt(selRow,
                EVTaskList.EVTASK_NODE_COLUMN);
            if (task != null)
                actionPerformed(task);
        }
        protected void actionPerformed(EVTask task) {
            String path = task.getFlag() != null ? "" : task.getFullName();
            actionPerformed(path);
        }
        protected void actionPerformed(String path) {}
    }

    abstract class BaselineAction extends TSAction
            implements PermissionsChangeListener {

        public BaselineAction(String resKey) {
            super(resKey);
            PermissionsManager.getInstance().addPermissionsChangeListener(this);
            permissionsChanged(null);
        }

        @Override
        public void permissionsChanged(PermissionsChangeEvent event) {
            boolean hasPerm = PermissionsManager.getInstance()
                    .hasPermission(EVPermissions.BASELINES);
            setEnabled(hasPerm);
            putValue(SHORT_DESCRIPTION, hasPerm ? null
                    : resources.getString("Buttons.No_Baseline_Permission"));
        }
    }

    class ShowTimeLogAction extends TSSelectedTaskAction {
        public ShowTimeLogAction() {
            super("Buttons.Show_Time_Log");
        }
        protected void actionPerformed(String path) {
            DashController.showTimeLogEditor(path);
        }
    }

    class ShowDefectLogAction extends TSSelectedTaskAction {
        public ShowDefectLogAction() {
            super("Buttons.Show_Defect_Log");
        }
        protected void actionPerformed(String path) {
            DashController.showDefectLogEditor(path);
        }
    }

    class CopyTaskInfoAction extends TSAction {
        public boolean running;
        public CopyTaskInfoAction() {
            super("Buttons.Copy_Task_Info");
            setEnabled(false);
        }
        public void actionPerformed(ActionEvent e) {
            Action copyAction = treeTable.getActionMap().get("copy");
            if (copyAction != null) {
                try {
                    running = true;
                    e.setSource(treeTable);
                    copyAction.actionPerformed(e);
                } finally {
                    running = false;
                }
            }
        }
    }

    class ShowAltReportAction extends TSAction {
        String uri;
        public ShowAltReportAction(Element xml) {
            // Get the URI of the report
            String uri = xml.getAttribute("href");
            if (uri.startsWith("/"))
                uri = uri.substring(1);
            this.uri = "//" + uri;

            // Get the resource bundle for messages
            String resourcePrefix = xml.getAttribute("resources");
            Resources res = Resources.getDashBundle(resourcePrefix);
            setText(res.getString("Menu_Text"));
        }

        public void actionPerformed(ActionEvent e) {
            showReport(taskListName, null, getUserFilter(), uri);
        }
    }

    Color editableColor, selectedEditableColor;
    Color expandedColor, expandedSelectedColor;

    class TaskJTreeTable extends JTreeTable implements TreeModelWillChangeListener,
                                                       TreeModelListener {

        DefaultTableCellRenderer editable, readOnly, milestone, notes,
                dependencies;
        TaskDependencyCellEditor dependencyEditor;
        TreeTableModel model;
        BooleanArray cutList;

        public TaskJTreeTable(TreeTableModel m) {
            super(m);
            setRowHeight(Math.max(17, getRowHeight()));
            model = m;
            cutList = new BooleanArray();

            editableColor = PaintUtils.mixColors(
                getBackground(), Color.yellow, 0.6);
            selectedEditableColor = PaintUtils.mixColors(
                getSelectionBackground(), editableColor, 0.5);
            expandedColor = PaintUtils.mixColors(
                getBackground(), getForeground(), 0.45);
            expandedSelectedColor = PaintUtils.mixColors(
                getBackground(), getSelectionForeground(), 0.45);

            editable = new TaskTableRenderer(selectedEditableColor,
                    editableColor, getForeground(), getSelectionForeground());
            readOnly = new TaskTableRenderer(getSelectionBackground(),
                    getBackground(), expandedColor, expandedSelectedColor);
            milestone = new MilestoneCellRenderer(getSelectionBackground(),
                    getBackground(), expandedColor, expandedSelectedColor);
            notes = new NoteCellRenderer(getSelectionBackground(),
                    getBackground(), selectedEditableColor, editableColor);
            dependencies = new DependencyCellRenderer(getSelectionBackground(),
                    getBackground(), selectedEditableColor, editableColor);

            getColumnModel().getColumn(EVTaskList.NODE_TYPE_COLUMN)
                    .setCellEditor(new NodeTypeEditor());

            getColumnModel().getColumn(EVTaskList.NOTES_COLUMN)
                    .setCellEditor(new TaskNoteCellEditor());

            dependencyEditor = new TaskDependencyCellEditor(
                    TaskScheduleDialog.this, dash);
            getColumnModel().getColumn(EVTaskList.DEPENDENCIES_COLUMN)
                    .setCellEditor(dependencyEditor);

            TaskJTreeTableCellRenderer r =
                new TaskJTreeTableCellRenderer(getTree().getCellRenderer(),
                        cutList);
            getTree().setCellRenderer(r);
            ToolTipManager.sharedInstance().registerComponent(getTree());
            new ToolTipTimingCustomizer().install(this);
            setTransferHandler(new TransferSupport());
            setComponentPopupMenu(makePopupMenu());
            setAutoscrolls(true);

            if (!isRollup()) {
                setBorder(new MilestoneBorder());

                MilestoneMouseoverHandler mmh = new MilestoneMouseoverHandler();
                addMouseListener(mmh);
                addMouseMotionListener(mmh);
            }
        }

        private JPopupMenu makePopupMenu() {
            JPopupMenu result = new JPopupMenu();
            if (Settings.isPersonalMode()) {
                result.add(showTimeLogAction = new ShowTimeLogAction());
                result.add(showDefectLogAction = new ShowDefectLogAction());
            }
            result.add(copyTaskInfoAction = new CopyTaskInfoAction());
            return result;
        }

        @Override
        public Point getPopupLocation(MouseEvent event) {
            Point point = (event == null ? null : event.getPoint());
            int row = (point == null ? -1 : rowAtPoint(point));
            if (row != -1)
                // select the clicked-on row, so actions can work against it
                setRowSelectionInterval(row, row);

            JPopupMenu popupMenu = getComponentPopupMenu();
            if (row != -1 && point != null && popupMenu != null) {
                // position the popup menu so it is horizontally centered on
                // the mouse location, and vertically positioned immediately
                // underneath the clicked cell. This location provides us the
                // best chance of avoiding the display of data tips, which would
                // otherwise obscure the popup menu in an ugly way.
                Dimension d = popupMenu.getPreferredSize();
                int x = (int) Math.max(0, point.getX() - d.getWidth() / 2);
                int y = (int) getCellRect(row, 0, false).getMaxY();
                return new Point(x, y);
            } else {
                return super.getPopupLocation(event);
            }
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            if (activeMilestoneDateRow != -1) {
                Point p = event.getPoint();
                int markerY = (activeMilestoneDateRow + 1) * getRowHeight();
                int delta = p.y - markerY;
                if (Math.abs(delta) < 4) {
                    return resources.format(
                        "Task.Milestone_Date.Commit_Line_Tip_FMT",
                        activeMilestoneDate, activeMilestone.getName());
                }
            }

            return super.getToolTipText(event);
        }

        public TableCellRenderer getCellRenderer(int row, int column) {
            int modelCol = convertColumnIndexToModel(column);
            if (modelCol == EVTaskList.DEPENDENCIES_COLUMN)
                return dependencies;
            else if (modelCol == EVTaskList.NOTES_COLUMN)
                return notes;
            else if (modelCol == EVTaskList.MILESTONE_COLUMN)
                return milestone;

            TableCellRenderer result = super.getCellRenderer(row, column);
            if (result instanceof JTreeTable.TreeTableCellRenderer)
                return result;

            if (row < 0) return readOnly;

            TreePath path = getTree().getPathForRow(row);
            if (path != null &&
                model.isCellEditable(path.getLastPathComponent(), modelCol))
                return editable;
            else
                return readOnly;
        }

        class TaskTableRenderer extends ShadedTableCellRenderer {
            private Font regular = null, bold = null;
            public TaskTableRenderer(Color sel, Color desel, Color afg,
                    Color sfg) {
                super(sel, desel, afg, sfg);
            }
            protected boolean useAltForeground(int row) {
                return getTree().isExpanded(row);
            }
            private Font getFont(boolean bold, Component c) {
                if (this.regular == null) {
                    Font base = c.getFont();
                    if (base == null) return null;
                    this.regular = base.deriveFont(Font.PLAIN);
                    this.bold    = base.deriveFont(Font.BOLD);
                }
                return (bold ? this.bold : this.regular);
            }
            public Component getTableCellRendererComponent(JTable table,
                                                           Object value,
                                                           boolean isSelected,
                                                           boolean hasFocus,
                                                           int row,
                                                           int column) {
                Component result = super.getTableCellRendererComponent
                    (table, value, isSelected, hasFocus, row, column);
                String errorStr = null;

                int modelCol = table.convertColumnIndexToModel(column);
                if (modelCol == EVTaskList.SORT_TAG_COLUMN
                        || modelCol == EVTaskList.LABELS_COLUMN)
                    setHorizontalAlignment(SwingConstants.LEFT);

                TreePath path = getTree().getPathForRow(row);
                EVTask node = null;
                if (path != null) {
                    if (path.getLastPathComponent() instanceof EVTask)
                        node = (EVTask) path.getLastPathComponent();
                    errorStr = ((EVTaskList) model).getErrorStringAt
                        (path.getLastPathComponent(), modelCol);
                }

                if (result instanceof JComponent)
                    ((JComponent) result).setToolTipText(errorStr);

                if (errorStr != null)
                    result.setForeground(errorStr.startsWith(" ")
                                         ? Color.orange : Color.red);
                else if (node != null && node.isUserPruned())
                    result.setForeground(PHANTOM_COLOR);
                else if (node != null && node.isChronologicallyPruned())
                    result.setForeground(SEPIA);

                Font f = getFont(errorStr != null, result);
                if (f != null) result.setFont(f);

                return result;
            }

            /*
             * We absolutely have to reduce the preferred height in order
             * for the datatips tooltips to show up correctly.
             */
            public Dimension getPreferredSize() {
                Dimension originalDimension = super.getPreferredSize();

                return new Dimension(originalDimension.width,
                                     originalDimension.height-3);
            }
        }

        class MilestoneCellRenderer extends TaskTableRenderer {

            private Icon checkedIcon, uncheckedIcon;
            private Color activeMilestoneColor, previousMilestoneColor;
            private String checkboxTip;

            public MilestoneCellRenderer(Color sel, Color desel, Color afg,
                    Color sfg) {
                super(sel, desel, afg, sfg);
                checkedIcon = new PaddedIcon(new ScalableImageIcon(12, //
                        getClass(), "box_checked.png"), 0, 1, 0, 0);
                uncheckedIcon = new PaddedIcon(new ScalableImageIcon(12, //
                        getClass(), "box_unchecked.png"), 0, 1, 0, 0);
                activeMilestoneColor = new Color(216, 184, 229);
                previousMilestoneColor = new Color(240, 223, 247);
            }

            @Override
            public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus,
                    int row, int column) {
                Component result = super.getTableCellRendererComponent(table,
                    value, isSelected, hasFocus, row, column);

                MilestoneList mVal = (MilestoneList) value;
                boolean isActiveMilestone = isActiveMilestone(mVal);

                if (row != mouseOverMilestoneRow //
                        || mVal == null || mVal.size() != 1) {
                    setIcon(null);
                    checkboxTip = null;
                } else {
                    setIcon(isActiveMilestone ? checkedIcon : uncheckedIcon);
                    checkboxTip = resources.format(
                        "Task.Milestone_Date.Checkbox_Tip_FMT", //
                        mVal.get(0).toString());
                }

                if (isActiveMilestone) {
                    cancelSelectionForeground(isSelected);
                    setBackground(activeMilestoneColor);
                    setOpaque(true);
                } else if (isPreviousMilestone(mVal)) {
                    cancelSelectionForeground(isSelected);
                    setBackground(previousMilestoneColor);
                    setOpaque(true);
                }

                setHorizontalAlignment(SwingConstants.LEFT);
                return result;
            }

            private void cancelSelectionForeground(boolean isSelected) {
                if (isSelected
                        && getForeground().equals(getSelectionForeground()))
                    setForeground(Color.black);
            }

            private boolean isActiveMilestone(MilestoneList mVal) {
                return mVal != null && mVal.contains(activeMilestone);
            }

            private boolean isPreviousMilestone(MilestoneList mVal) {
                if (mVal != null && previousMilestones != null)
                    for (Milestone m : mVal)
                        if (previousMilestones.contains(m))
                            return true;
                return false;
            }

            @Override
            public String getToolTipText(MouseEvent event) {
                if (checkboxTip != null
                        && event.getPoint().getX() < uncheckedIcon
                                .getIconWidth())
                    return checkboxTip;
                else
                    return super.getToolTipText(event);
            }

        }

        int mouseOverMilestoneRow = -1;

        class MilestoneMouseoverHandler implements MouseListener,
                MouseMotionListener {

            public void mouseMoved(MouseEvent e) {
                int row = -1;
                if (isFlatView()) {
                    Point point = e.getPoint();
                    int col = convertColumnIndexToModel(columnAtPoint(point));
                    if (col == EVTaskList.MILESTONE_COLUMN)
                        row = rowAtPoint(point);
                }
                setHighlightRow(row);
            }

            public void mouseExited(MouseEvent e) {
                setHighlightRow(-1);
            }

            private void setHighlightRow(int newRow) {
                if (newRow == mouseOverMilestoneRow)
                    return;

                int oldRow = mouseOverMilestoneRow;
                mouseOverMilestoneRow = newRow;
                repaintMilestoneCellForRow(oldRow);
                repaintMilestoneCellForRow(newRow);
            }

            private void repaintMilestoneCellForRow(int row) {
                if (row != -1)
                    repaint(getCellRect(row,
                        convertColumnIndexToView(EVTaskList.MILESTONE_COLUMN),
                        true));
            }

            public void mouseClicked(MouseEvent e) {}
            public void mousePressed(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
            public void mouseEntered(MouseEvent e) {}
            public void mouseDragged(MouseEvent e) {}
        }

        private boolean handleMilestoneClick(int row, int col, EventObject e) {
            if (convertColumnIndexToModel(col) == EVTaskList.MILESTONE_COLUMN
                    && e instanceof MouseEvent
                    && isFlatView()) {
                double absX = ((MouseEvent) e).getPoint().getX();
                double relX = absX - getCellRect(row, col, false).getX();
                if (0 <= relX && relX <= 14) {
                    MilestoneList mVal = (MilestoneList) getValueAt(row, col);
                    if (mVal != null && mVal.size() == 1)
                        toggleActiveMilestone(mVal.get(0));
                }
                return true;
            }
            return false;
        }

        class MilestoneBorder implements Border {

            private Insets insets;
            private Stroke stroke;
            private Color color;

            public MilestoneBorder() {
                insets = new Insets(0, 0, 0, 0);
                stroke = new BasicStroke(3f, BasicStroke.CAP_SQUARE,
                        BasicStroke.JOIN_MITER, 10.0f,
                        new float[] { 10f, 10f }, 0.0f);
                color = new Color(155, 0, 217);
            }

            public void paintBorder(Component c, Graphics g, int x, int y,
                    int width, int height) {
                if (activeMilestoneDateRow != -1 && g instanceof Graphics2D) {
                    Graphics2D g2 = (Graphics2D) g;
                    Stroke oldStroke = g2.getStroke();
                    g2.setStroke(stroke);
                    g2.setColor(color);

                    int yy = (activeMilestoneDateRow+1) * getRowHeight() + y-1;
                    g2.drawLine(x, yy, x + width, yy);

                    g2.setStroke(oldStroke);
                }
            }

            public Insets getBorderInsets(Component c) {
                return insets;
            }

            public boolean isBorderOpaque() {
                return true;
            }

        }

        class NoteCellRenderer extends DefaultTableCellRenderer {

            private Color[] colors;
            private Icon noteIcon, noteErrorIcon;

            public NoteCellRenderer(Color selRO, Color deselRO,
                    Color sel, Color desel) {

                colors = new Color[] { sel, desel, selRO, deselRO };
                noteIcon = HierarchyNoteIcon.WHITE;
                noteErrorIcon = HierarchyNoteIcon.RED;
            }

            public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus,
                    int row, int column) {

                Component result = super.getTableCellRendererComponent(table,
                        null, isSelected, hasFocus, row, column);

                boolean isEditable = false;
                TreePath path = getTree().getPathForRow(row);
                if (path != null &&
                    model.isCellEditable(path.getLastPathComponent(),
                            table.convertColumnIndexToModel(column)))
                    isEditable = true;

                setBackground(colors[(isSelected ? 0:1) + (isEditable ? 0:2)]);
                setHorizontalAlignment(SwingConstants.CENTER);

                HierarchyNote note = null;
                boolean hasError = false;
                if (value instanceof Map) {
                    Map noteData = (Map) value;
                    note = (HierarchyNote) noteData.get(
                            HierarchyNoteManager.NOTE_KEY);
                    hasError = noteData.containsKey(
                            HierarchyNoteManager.NOTE_CONFLICT_KEY);
                }

                if (note == null) {
                    setIcon(null);
                    setToolTipText(null);
                } else {
                    setIcon(hasError ? noteErrorIcon : noteIcon);
                    setToolTipText("<html><div width='300'>"
                        + note.getAsHTML() + "</div></html>");
                }

                return result;
            }
        }

        class TaskNoteCellEditor extends AbstractCellEditor implements
                TableCellEditor, Runnable {

            JLabel dummyComponent = new JLabel(HierarchyNoteIcon.WHITE);

            private Object value;
            PropertyKey nodeToEdit;

            public Component getTableCellEditorComponent(JTable table,
                    Object value, boolean isSelected, int row, int column) {
                this.value = value;

                String path = (String) table.getValueAt(row,
                    EVTaskList.TASK_FULLNAME_COLUMN);
                this.nodeToEdit = dash.getHierarchy().findExistingKey(path);

                SwingUtilities.invokeLater(this);
                return dummyComponent;
            }

            public Object getCellEditorValue() {
                return value;
            }

            public void run() {
                cancelCellEditing();
                if (nodeToEdit != null)
                    HierarchyNoteEditorDialog
                            .showGlobalNoteEditor(dash, nodeToEdit);
            }

        }

        class DependencyCellRenderer extends DefaultTableCellRenderer {

            private Color[] colors;

            public DependencyCellRenderer(Color selRO, Color deselRO,
                    Color sel, Color desel) {

                colors = new Color[] { sel, desel, selRO, deselRO };
            }

            public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus,
                    int row, int column) {

                Component result = super.getTableCellRendererComponent(table,
                        null, isSelected, hasFocus, row, column);

                boolean isEditable = false;
                TreePath path = getTree().getPathForRow(row);
                if (path != null &&
                    model.isCellEditable(path.getLastPathComponent(),
                            table.convertColumnIndexToModel(column)))
                    isEditable = true;

                setBackground(colors[(isSelected ? 0:1) + (isEditable ? 0:2)]);
                setHorizontalAlignment(SwingConstants.CENTER);

                TaskDependencyAnalyzer.GUI analyzer =
                    new TaskDependencyAnalyzer.GUI(value);
                analyzer.syncLabel(this);

                return result;
            }
        }

        class NodeTypeEditor extends DefaultCellEditor {
            JComboBox comboBox;
            public NodeTypeEditor() {
                super(new JComboBox());
                this.comboBox = (JComboBox) getComponent();
                Font f = comboBox.getFont();
                comboBox.setFont(f.deriveFont(Font.PLAIN, f.getSize2D()-2));
            }

            public Component getTableCellEditorComponent(JTable table,
                    Object value, boolean isSelected, int row, int column) {

                comboBox.removeAllItems();

                TreePath path = getTree().getPathForRow(row);
                if (path != null) {
                    EVTask task = (EVTask) path.getLastPathComponent();
                    ListData types = task.getAcceptableNodeTypes();
                    if (types != null) {
                        for (int i = 1;  i < types.size();  i++) {
                            String oneType = (String) types.get(i);
                            if (oneType.startsWith("(") && oneType.endsWith(")"))
                                continue;
                            comboBox.addItem(oneType);
                        }
                    }
                }
                if (comboBox.getItemCount() == 0)
                    comboBox.addItem(value);

                return super.getTableCellEditorComponent(table, value,
                        isSelected, row, column);
            }


        }

        public boolean editCellAt(int row, int column, EventObject e) {
            if (handleMilestoneClick(row, column, e))
                return false;

            boolean result = super.editCellAt(row, column, e);

            if (result == true && e instanceof MouseEvent)
                DeferredSelectAllExecutor.register(getEditorComponent());

            return result;
        }

        public Component prepareEditor(TableCellEditor editor,
                                       int row,
                                       int column) {
            Component result = super.prepareEditor(editor, row, column);
            result.setBackground(selectedEditableColor);
            if (result instanceof JTextComponent)
                ((JTextComponent) result).selectAll();
            return result;
        }

        public void setTreeTableModel(TreeTableModel treeTableModel) {
            cutList.clear();

            TreeCellRenderer r = getTree().getCellRenderer();

            super.setTreeTableModel(treeTableModel);

            getTree().setCellRenderer(r);
            ToolTipManager.sharedInstance().registerComponent(getTree());
        }


        private void setCutList(int[] rows) {
            cutList.clear();
            if (rows != null)
                for (int i = rows.length; i-- > 0; )
                    cutList.set(rows[i], true);
            if (treeTable != null)
                tableChanged(new TableModelEvent(treeTable.getModel(), 1,
                        treeTable.getRowCount()-1));
        }

        private class TransferSupport extends TransferHandler implements
                ClipboardOwner {

            public int getSourceActions(JComponent c) {
                return COPY_OR_MOVE;
            }

            public boolean canImport(JComponent comp, DataFlavor[] flavors) {
                for (int i = 0; i < flavors.length; i++) {
                    if (DataFlavor.stringFlavor.equals(flavors[i]) && isFlatView())
                        return true;
                    else if (EV_TASK_FLAVOR.equals(flavors[i]) && !isFlatView())
                        return true;
                }
                return false;
            }

            public void exportToClipboard(JComponent comp, Clipboard clip,
                    int action) {
                // we have to override this method, because we need to register
                // a ClipboardOwner when we set the clipboard contents.
                Transferable t = createTransferable(comp);
                if (t != null) {
                    clip.setContents(t, this);
                    exportDone(comp, t, action);
                } else {
                    exportDone(comp, null, NONE);
                }
            }

            protected Transferable createTransferable(JComponent c) {
                if (isFlatView()
                        && !((CopyTaskInfoAction) copyTaskInfoAction).running)
                    return createFlatViewTransferrable();
                else
                    return createTreeViewTransferrable();
            }

            private Transferable createFlatViewTransferrable() {
                int[] rows = treeTable.getSelectedRows();
                if (rows == null || rows.length == 0
                        || (rows.length == 1 && rows[0] == 0))
                    return null;

                StringBuffer data = new StringBuffer();
                for (int i = 0; i < rows.length; i++) {
                    Object taskName = treeTable.getValueAt(rows[i],
                            EVTaskList.TASK_COLUMN);
                    data.append(taskName).append('\n');
                }

                if (isEditing())
                    removeEditor();
                setCutList(rows);

                return new StringSelection(data.toString());
            }

            private Transferable createTreeViewTransferrable() {
                int[] rows = treeTable.getSelectedRows();
                if (rows == null || rows.length == 0)
                    return null;

                EVTask task = (EVTask) getValueAt(rows[0],
                    EVTaskList.EVTASK_NODE_COLUMN);
                return new TaskSelection(task);
            }

            public void lostOwnership(Clipboard clipboard, Transferable t) {
                setCutList(null);
            }

            public boolean importData(JComponent comp, Transferable t) {
                if (isFlatView())
                    return importDataFlatView(t);
                else
                    return importDataTreeView(t);
            }

            private boolean importDataFlatView(Transferable t) {
                int insertionRow = treeTable.getSelectedRow();
                if (insertionRow == -1) return false;
                if (insertionRow == 0) insertionRow = 1;

                String tasks;
                try {
                    tasks = (String) t.getTransferData(DataFlavor.stringFlavor);
                } catch (Exception ex) {
                    return false;
                }

                cutList.clear();

                int[] newSelection = flatModel.insertTasks(tasks, insertionRow-1);
                if (newSelection == null)
                    return false;

                setDirty(true);
                SwingUtilities.invokeLater(new RowSelectionTask(
                        newSelection[0]+1, newSelection[1]+1, false));
                return true;
            }

            private boolean importDataTreeView(Transferable t) {
                int[] rows = treeTable.getSelectedRows();
                int col = treeTable.getSelectedColumn();
                if (rows == null || rows.length == 0 || col == -1)
                    return false;

                Object value;
                try {
                    EVTask task = (EVTask) t.getTransferData(EV_TASK_FLAVOR);
                    int modelCol = convertColumnIndexToModel(col);
                    value = model.getValueAt(task, modelCol);
                } catch (Exception e) {
                    return false;
                }

                boolean madeChange = false;
                for (int row : rows) {
                    if (isCellEditable(row, col)) {
                        setValueAt(value, row, col);
                        madeChange = true;
                    }
                }
                return madeChange;
            }

        }

        private class TaskSelection implements Transferable {

            private EVTask task;

            protected TaskSelection(EVTask task) {
                this.task = task;
            }

            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[] { EV_TASK_FLAVOR,
                        DataFlavor.stringFlavor };
            }

            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return flavor.equals(EV_TASK_FLAVOR)
                        || flavor.equals(DataFlavor.stringFlavor);
            }

            public Object getTransferData(DataFlavor flavor)
                    throws UnsupportedFlavorException {
                if (flavor.equals(EV_TASK_FLAVOR)) {
                    return task;

                } else if (flavor.equals(DataFlavor.stringFlavor)) {
                    String newLine = System.getProperty("line.separator");
                    StringBuilder data = new StringBuilder();
                    for (int i = 0; i < getColumnCount(); i++) {
                        TableColumn col = getColumnModel().getColumn(i);
                        int pos = convertColumnIndexToModel(i);
                        if (col.getPreferredWidth() == 0
                                || pos == EVTaskList.NOTES_COLUMN
                                || pos == EVTaskList.DEPENDENCIES_COLUMN)
                            // skip hidden and special columns
                            continue;

                        String val = getStringForCol(pos);
                        if (val != null)
                            data.append(model.getColumnName(pos)).append(":\t")
                                    .append(val).append(newLine);
                    }
                    return data.toString();
                }

                throw new UnsupportedFlavorException(flavor);
            }

            private String getStringForCol(int col) {
                if (col == EVTaskList.TASK_COLUMN && task.getFlag() == null)
                    col = EVTaskList.TASK_FULLNAME_COLUMN;
                Object val = model.getValueAt(task, col);
                if (val == null || "".equals(val))
                    return null;
                else if (val instanceof Date)
                    return EVSchedule.formatDate((Date) val);
                else
                    return val.toString();
            }
        }

        public void treeStructureWillChange(TreeModelEvent e) {
            if (shouldUseExpandedNodesPref() && model instanceof EVTaskList) {
                    saveExpandedNodesPref(((EVTaskList)model).getID());
                }
        }

        public void treeStructureChanged(TreeModelEvent e) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (model instanceof EVTaskList)
                        maybeReadExpandedNodesPref(((EVTaskList)model).getID());
                }
            });
        }

        public void treeNodesChanged(TreeModelEvent e) { }

        public void treeNodesInserted(TreeModelEvent e) { }

        public void treeNodesRemoved(TreeModelEvent e) { }
    }

    private static final DataFlavor EV_TASK_FLAVOR = new DataFlavor(
            EVTask.class, "EV Task Data");

    class TaskJTreeTableCellRenderer
        extends javax.swing.tree.DefaultTreeCellRenderer {

        private Font regular = null, bold = null, strike = null;
        private BooleanArray cutList;

        public TaskJTreeTableCellRenderer(TreeCellRenderer r,
                BooleanArray cutList) {
            super();
            this.cutList = cutList;
        }
        private Font getFont(boolean bold, boolean strike, Component c) {
            if (this.regular == null) {
                Font base = c.getFont();
                if (base == null) return null;
                this.regular = base.deriveFont(Font.PLAIN);
                this.bold    = base.deriveFont(Font.BOLD);
                this.strike  = regular.deriveFont(Collections
                        .singletonMap(TextAttribute.STRIKETHROUGH,
                            TextAttribute.STRIKETHROUGH_ON));
            }
            return (bold ? this.bold : (strike ? this.strike : this.regular));
        }

        /**
         * Overrides <code>DefaultTreeCellRenderer.getPreferredSize</code> to
         * return a wider preferred size value.
         *
         * Without this method, nodes displayed in bold text are
         * sometimes truncated (with "..." at the end of the label),
         * presumably because DefaultTreeCellRenderer doesn't expect
         * the font to be changing.
         *
         * Since we display in a JTreeTable table and our preferred
         * width is typically ignored entirely, we could really ask
         * for as large a preferred width as we want and not worry
         * that we'll be given too much horizontal space.
         */
        public Dimension getPreferredSize() {
            Dimension        retDimension = super.getPreferredSize();

            if(retDimension != null)
                retDimension = new Dimension((int) (retDimension.width * 1.2),
                                             retDimension.height);
            return retDimension;
        }

        public Component getTreeCellRendererComponent(JTree tree,
                                                      Object value,
                                                      boolean selected,
                                                      boolean expanded,
                                                      boolean leaf,
                                                      int row,
                                                      boolean hasFocus)
        {
            Component result = super.getTreeCellRendererComponent
                (tree, value, selected, expanded, leaf, row, hasFocus);
            String errorStr = null;

            // find the EVTask for the indicated row
            TreePath path = tree.getPathForRow(row);
            EVTask node = null;
            if (path != null)
                node = (EVTask) path.getLastPathComponent();

            // in flat mode, display the full name of the node rather
            // than the terminal name
            if (node != null && tree.getModel() == flatModel)
                setText((String) flatModel.getValueAt(node,
                    EVTaskList.TASK_COLUMN));

            // retrieve the error string for this node
            if (node != null && model != null)
                errorStr = ((EVTaskList) model).getErrorStringAt
                    (node, EVTaskList.TASK_COLUMN);

            // display the error string if needed
            if (result instanceof JComponent)
                ((JComponent) result).setToolTipText(errorStr);
            if (errorStr != null)
                result.setForeground(errorStr.startsWith(" ")
                                     ? Color.orange : Color.red);
            Font f = getFont(errorStr != null, false, result);
            if (f != null) result.setFont(f);

            // give pruned nodes a special appearance
            if (node != null && node.isUserPruned()) {
                result.setForeground(PHANTOM_COLOR);
                if (errorStr == null && result instanceof JComponent)
                    ((JComponent) result).setToolTipText
                        (resources.getString("Task.Removed_Tooltip"));
                if (result instanceof JLabel)
                    ((JLabel) result).setIcon(getPrunedIcon(expanded, leaf));

            // give chronologically pruned nodes a special appearance
            } else if (node != null && node.isChronologicallyPruned()) {
                result.setForeground(SEPIA);
                if (errorStr == null && result instanceof JComponent) {
                    ((JComponent) result).setToolTipText
                        (resources.getString
                         ("Task.Previously_Completed_Tooltip"));
                    result.setFont(getFont(false, true, result));
                }
                if (result instanceof JLabel)
                    ((JLabel) result).setIcon(getChronIcon(expanded, leaf));

            // give cut nodes a special appearance
            } else if (row > 0 && cutList.get(row)) {
                if (result instanceof JLabel)
                    ((JLabel) result).setIcon(getCutLeafIcon());
                if (errorStr == null && result instanceof JComponent) {
                    ((JComponent) result).setToolTipText
                        (resources.getString("Task.Copied_Tooltip"));
                    result.setForeground(Color.BLUE);
                }

            // use strikethrough for completed (but otherwise normal) tasks
            } else if (node != null && node.getPercentComplete() > 0.99999
                    && errorStr == null) {
                result.setFont(getFont(false, true, result));
            }

            return result;
        }
        private Icon prunedOpenIcon = null;
        private Icon prunedClosedIcon = null;
        private Icon prunedLeafIcon = null;

        private Icon chronOpenIcon = null;
        private Icon chronClosedIcon = null;
        private Icon chronLeafIcon = null;

        private Icon cutLeafIcon = null;

        private Icon getPrunedIcon(boolean expanded, boolean leaf) {
            if (leaf) {
                if (prunedLeafIcon == null)
                    prunedLeafIcon = pruneIcon(getLeafIcon(), getDefaultLeafIcon(), 8);
                return prunedLeafIcon;
            } else if (expanded) {
                if (prunedOpenIcon == null)
                    prunedOpenIcon = pruneIcon(getOpenIcon(), getDefaultOpenIcon(), 8);
                return prunedOpenIcon;
            } else {
                if (prunedClosedIcon == null)
                    prunedClosedIcon = pruneIcon(getClosedIcon(), getDefaultClosedIcon(), 8);
                return prunedClosedIcon;
            }
        }
        private Icon pruneIcon(Icon icon, Icon alt, int y) {
            if (icon == null) icon = alt;
            if (icon == null) return null;
            BufferedIcon result = new BufferedIcon(this, icon);
//            result.applyFilter(new GrayFilter(true, 85));
            result.applyFilter(PHANTOM_FILTER);
            result.setDecorator(new PruneDecorationIcon(), y);
            return result;
        }
        private Icon getChronIcon(boolean expanded, boolean leaf) {
            if (leaf) {
                if (chronLeafIcon == null)
                    chronLeafIcon = chronIcon(getLeafIcon(), getDefaultLeafIcon(), 8);
                return chronLeafIcon;
            } else if (expanded) {
                if (chronOpenIcon == null)
                    chronOpenIcon = chronIcon(getOpenIcon(), getDefaultOpenIcon(), 8);
                return chronOpenIcon;
            } else {
                if (chronClosedIcon == null)
                    chronClosedIcon = chronIcon(getClosedIcon(), getDefaultClosedIcon(), 8);
                return chronClosedIcon;
            }
        }
        private Icon chronIcon(Icon icon, Icon alt, int y) {
            if (icon == null) icon = alt;
            if (icon == null) return null;
            BufferedIcon result = new BufferedIcon(this, icon);
            result.applyFilter(SEPIA_FILTER);
            return result;
        }
        private Icon getCutLeafIcon() {
            if (cutLeafIcon == null) {
                Icon icon = getLeafIcon();
                if (icon == null) icon = getDefaultLeafIcon();
                BufferedIcon buf = new BufferedIcon(this, icon);
                buf.applyFilter(new CutItemFilter(
                        getBackgroundSelectionColor()));
                cutLeafIcon = buf;
            }
            return cutLeafIcon;
        }
    }

    private static class BufferedIcon implements Icon {
        protected Image image = null;
        protected int width = 16, height = 16;
        protected Icon decoration = null;
        private int decorationOffset = 0;

        public BufferedIcon(Component c, Icon originalIcon) {
            width = originalIcon.getIconWidth();
            height = originalIcon.getIconHeight();
            image = new BufferedImage(width, height,
                                      BufferedImage.TYPE_INT_ARGB);
            Graphics imageG = image.getGraphics();
            originalIcon.paintIcon(c, imageG, 0, 0);
            imageG.dispose();
        }

        public int getIconWidth() { return width; }
        public int getIconHeight() { return height; }
        protected void doPaint(Component c, Graphics g) {}

        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (image == null) {
                image = new BufferedImage(getIconWidth(), getIconHeight(),
                                          BufferedImage.TYPE_INT_ARGB);
                Graphics imageG = image.getGraphics();
                doPaint(c,imageG);
                imageG.dispose();
            }
            g.drawImage(image, x, y, null);
            if (decoration != null) {
                decoration.paintIcon(c, g, 0, decorationOffset);
            }
        }

        public void applyFilter(RGBImageFilter filter) {
            ImageProducer prod =
                new FilteredImageSource(image.getSource(), filter);
            this.image = Toolkit.getDefaultToolkit().createImage(prod);
        }

        public void setDecorator(Icon decoration, int y) {
            this.decoration = decoration;
            this.decorationOffset = y;
        }
    }

    // filter for creating "error" icons.  Converts to light monochrome.
    private static Color PHANTOM_COLOR = new Color(255*2/3, 255*2/3, 255*2/3);
    private static PhantomFilter PHANTOM_FILTER = new PhantomFilter();
    private static class PhantomFilter extends RGBImageFilter {
        public PhantomFilter() { canFilterIndexColorModel = true; }

        public int filterRGB(int x, int y, int rgb) {
            // Use NTSC conversion formula.
            int gray = (int)(0.30 * ((rgb >> 16) & 0xff) +
                             0.59 * ((rgb >> 8) & 0xff) +
                             0.11 * (rgb & 0xff));
            // mix one part gray with two parts white to brighten.
            gray = (gray + 255 + 255) / 3;

            if (gray < 0) gray = 0;
            if (gray > 255) gray = 255;
            return (rgb & 0xff000000) | (0x010101 * gray);
        }
    }

    private static Color SEPIA = new Color(159, 141, 114);
    private static SepiaFilter SEPIA_FILTER = new SepiaFilter();
    private static class SepiaFilter extends RGBImageFilter {
        public SepiaFilter() { canFilterIndexColorModel = true; }

        public int filterRGB(int x, int y, int rgb) {
            // Use NTSC conversion formula.
            int gray = (int)(0.30 * ((rgb >> 16) & 0xff) +
                             0.59 * ((rgb >> 8) & 0xff) +
                             0.11 * (rgb & 0xff));

            if (gray < 0) gray = 0;
            if (gray > 255) gray = 255;
            double p = ((double) gray) / 255.0;
            int r = linear(159, 232, p);
            int g = linear(141, 224, p);
            int b = linear(114, 205, p);

            return (rgb & 0xff000000) | (r<<16) | (g<<8) | b;
        }
        private int linear(int zero, int one, double g) {
            return 0xff & ((int) (zero + g * (one - zero)));
        }
    }

    private static class CutItemFilter extends RGBImageFilter {
        private int replacementRgb;
        public CutItemFilter(Color whiteReplacement) {
            canFilterIndexColorModel = true;
            replacementRgb = whiteReplacement.getRGB();
        }

        public int filterRGB(int x, int y, int rgb) {
            int gray = (int)(0.30 * ((rgb >> 16) & 0xff) +
                    0.59 * ((rgb >> 8) & 0xff) +
                    0.11 * (rgb & 0xff));
            if (gray > 200)
                return (rgb & 0xff000000) | replacementRgb;
            else
                return rgb;
        }
    }


    private class PruneDecorationIcon implements Icon {
        public int getIconWidth() { return 8; }
        public int getIconHeight() { return 7; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g;
            Stroke saved = g2.getStroke();
            g2.setStroke(new BasicStroke(3));
            g2.setColor(Color.red);
            g2.drawLine(x+1,   y,   x+7, y+6);
            g2.drawLine(x+1,   y+6, x+7, y);
            g2.setStroke(saved);
        }
    }


    private class RedFillIcon implements Icon {
        Color redFill = new Color(255, 92, 92);
        public int getIconWidth()  { return 1; }
        public int getIconHeight() { return 1; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(redFill);
            g.fillRect(2, 2, c.getWidth() - 4, c.getHeight() - 4);
        }
    }


    class ScheduleJTable extends JTable {
        DefaultTableCellRenderer editable, readOnly;
        Font bold, plain;
        EVSchedule model;

        public ScheduleJTable(EVSchedule model) {
            super(model);
            this.model = model;

            editable = new ScheduleTableRenderer(selectedEditableColor,
                    editableColor, expandedColor, expandedSelectedColor);

            readOnly = new ScheduleTableRenderer(getSelectionBackground(),
                    getBackground(), expandedColor, expandedSelectedColor);

            bold = editable.getFont().deriveFont(Font.BOLD);
            plain = editable.getFont().deriveFont(Font.PLAIN);

            TableColumn notes = getColumnModel().getColumn(
                EVSchedule.NOTES_COLUMN);
            if (model.isShowNotesColumn()) {
                notes.setCellRenderer(new ScheduleNoteCellRenderer());
                notes.setCellEditor(new ScheduleNoteCellEditor());
                new ToolTipTimingCustomizer().install(this);
            } else {
                showHideColumn(notes, null, 0);
            }
            if (isRollup()) {
                ScheduleBalancingDialog d = new ScheduleBalancingDialog(
                        TaskScheduleDialog.this,
                        (EVTaskListRollup) TaskScheduleDialog.this.model);
                getColumnModel().getColumn(EVSchedule.PLAN_TIME_COLUMN)
                        .setCellEditor(d);
                getColumnModel().getColumn(EVSchedule.PLAN_DTIME_COLUMN)
                        .setCellEditor(d);
            }
        }

        public TableCellRenderer getCellRenderer(int row, int column) {
            TableCellRenderer result = super.getCellRenderer(row, column);

            if (result instanceof JTreeTable.TreeTableCellRenderer
                   || result instanceof ScheduleJTable.ScheduleNoteCellRenderer)
                return result;

            if (row < 0) return readOnly;

            if (model.isCellEditable(row, convertColumnIndexToModel(column)))
                return editable;
            else
                return readOnly;
        }

        class ScheduleTableRenderer extends ShadedTableCellRenderer {
            public ScheduleTableRenderer(Color sel, Color desel, Color afg,
                    Color sfg) {
                super(sel, desel, afg, sfg);
            }
            protected boolean useAltForeground(int row) {
                return model.rowIsAutomatic(row);
            }
            @Override
            public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus,
                    int row, int column) {
                Component result = super.getTableCellRendererComponent(table,
                    value, isSelected, hasFocus, row, column);
                result.setFont(model.rowIsEffective(row) ? bold : plain);
                return result;
            }
        }

        class ScheduleNoteCellRenderer extends ShadedTableCellRenderer {
            private Icon noteIcon;

            public ScheduleNoteCellRenderer() {
                super(selectedEditableColor, editableColor, Color.gray,
                        Color.gray);
                noteIcon = HierarchyNoteIcon.WHITE;
            }

            @Override
            public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus,
                    int row, int column) {

                super.getTableCellRendererComponent(table, null, isSelected,
                    hasFocus, row, column);

                setHorizontalAlignment(SwingConstants.CENTER);
                String note = (String) value;
                if (note == null || note.trim().length() == 0) {
                    setIcon(null);
                    setToolTipText(null);
                } else {
                    setIcon(noteIcon);

                    StringBuffer html = new StringBuffer();
                    html.append("<html><div width='300'>")
                        .append(HTMLUtils.escapeEntities(note))
                        .append("</div></html>");
                    StringUtils.findAndReplace(html, "\n", "<br>");
                    StringUtils.findAndReplace(html, "  ", "&nbsp;&nbsp;");
                    setToolTipText(html.toString());
                }

                return this;
            }

        }

        class ScheduleNoteCellEditor extends JDialogCellEditor<String> {

            private Object fromDate, toDate;

            public ScheduleNoteCellEditor() {
                button.setIcon(HierarchyNoteIcon.WHITE);
            }

            @Override
            protected String getButtonText(String value) {
                return null;
            }

            @Override
            public Component getTableCellEditorComponent(JTable table,
                    Object value, boolean isSelected, int row, int column) {
                fromDate = table.getValueAt(row, EVSchedule.FROM_COLUMN);
                toDate = table.getValueAt(row, EVSchedule.TO_COLUMN);

                return super.getTableCellEditorComponent(table, value,
                    isSelected, row, column);
            }

            @Override
            protected String showEditorDialog(String value)
                    throws EditingCancelled {
                // create a text field to hold the note
                JTextArea text = new JTextArea();
                if (value instanceof String)
                    text.setText((String) value);
                text.setFont(scheduleTable.getFont());
                text.setCaretPosition(0);
                text.setLineWrap(true);
                text.setWrapStyleWord(true);

                // wrap the text field in a scroll pane
                JScrollPane sp = new JScrollPane(text,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                sp.setPreferredSize(new Dimension(200, 100));

                // display the scrolled text field in a dialog box
                String title = resources
                        .getString("Schedule.Notes_Dialog.Title");
                String prompt = resources.format(
                        "Schedule.Notes_Dialog.Prompt_FMT", fromDate, toDate);
                Object[] message = new Object[] { prompt, sp,
                        new JOptionPaneTweaker.GrabFocus(text),
                        new JOptionPaneTweaker.MakeResizable() };
                int userChoice = JOptionPane.showConfirmDialog(
                        ScheduleJTable.this, message, title,
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                        null);

                // if the user cancels, abort.
                if (userChoice != JOptionPane.OK_OPTION)
                    throw new EditingCancelled();

                // otherwise, return the newly entered value.
                setDirty(true);
                return text.getText();
            }

        }

        public boolean editCellAt(int row, int column, EventObject e) {
            boolean result = super.editCellAt(row, column, e);

            if (result == true && e instanceof MouseEvent)
                DeferredSelectAllExecutor.register(getEditorComponent());

            return result;
        }

        public Component prepareEditor(TableCellEditor editor,
                int row, int column) {
            Component result = super.prepareEditor(editor, row, column);
            result.setBackground(selectedEditableColor);
            if (result instanceof JTextComponent)
                ((JTextComponent) result).selectAll();
            return result;
        }
    }

    class ShadedTableCellRenderer extends DefaultTableCellRenderer {
        /** The color to use in this renderer if the cell is selected. */
        Color selectedBackgroundColor;
        /** The color to use in this renderer if the cell is not selected. */
        Color backgroundColor;
        /** The alternate foreground color to use when useAlt returns true. */
        Color altForeground, altSelForeground;
        /** The minimum width the cell as to have for all it's content to be visible*/
        double minimumWidth;

        public ShadedTableCellRenderer(Color sel, Color desel, Color altFg,
                Color altSelFg) {
            selectedBackgroundColor = sel;
            backgroundColor = desel;
            altForeground = altFg;
            altSelForeground = altSelFg;
        }

        protected boolean useAltForeground(int row) { return false; }

        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            Component result = super.getTableCellRendererComponent
                (table, value, isSelected, hasFocus, row, column);
            Color bg = null;
            if (isSelected) {
                result.setForeground(useAltForeground(row) ?
                        altSelForeground : table.getSelectionForeground());
                result.setBackground(bg = selectedBackgroundColor);
            } else {
                result.setForeground(useAltForeground(row) ?
                        altForeground : table.getForeground());
                result.setBackground(bg = backgroundColor);
            }

            // This step is necessary because the DefaultTableCellRenderer
            // may have incorrectly set the "opaque" flag.
            if (result instanceof JComponent) {
                boolean colorMatch = (bg != null) &&
                    ( bg.equals(table.getBackground()) ) && table.isOpaque();
                ((JComponent)result).setOpaque(!colorMatch);
            }

            return result;
        }

        public void setValue(Object value) {
            if (value instanceof Date) {
                setHorizontalAlignment(SwingConstants.LEFT);
                setText(EVSchedule.formatDate((Date) value));
            } else {
                setHorizontalAlignment(SwingConstants.RIGHT);
                super.setValue(value);
            }
        }

        public void repaint() {
            super.repaint();
            minimumWidth = getMinimumSize().getWidth();
        }

        public int getHorizontalAlignment() {
            double currentWidth = getBounds().getWidth();

            if (currentWidth < minimumWidth)
                return SwingConstants.LEFT;
            else
                return super.getHorizontalAlignment();
        }
    }


    public void evNodeChanged(final EVTask node, boolean needsRecalc) {
        if (SwingUtilities.isEventDispatchThread())
            handleEvNodeChanged(node);
        else
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { handleEvNodeChanged(node); } } );
    }

    private void handleEvNodeChanged(EVTask node) {
        try {
            TreePath tp = new TreePath(node.getPath());
            int row = treeTable.getTree().getRowForPath(tp);
            if (row != -1) {
                AbstractTableModel model =
                    (AbstractTableModel) treeTable.getModel();
                model.fireTableChanged(new TableModelEvent(model, row));
            }
        } catch (Exception e) {}
    }

    public void evScheduleChanged() {
        setDirty(true);
        recalcAll();
    }

    protected void recalcAll() {
        // recalculate all the data. This will automatically fire
        // appropriate events on the EVSchedule table.
        model.recalc();
    }

    public void evRecalculated(EventObject e) {
        recalcActiveMilestoneDateRow();

        // We have to manually generate events for the JTreeTable,
        // since it has installed some wrapper object to convert the
        // TreeTableModel into a TableModel.
        AbstractTableModel model =
            (AbstractTableModel) treeTable.getModel();
        model.fireTableChanged(new TableModelEvent(model, 0,
                                                   treeTable.getRowCount()-1));

        // Calculating the schedule may mean that direct time columns now
        // need to be displayed or hidden
        showHideColumns();

        // Since rows may have been added or deleted to the schedule, and
        // rows may have changed to or from automatic rows, update the
        // buttons appropriately.
        enableScheduleButtons();

        // Display the error button if necessary.
        maybeDisplayErrorButton();

        // highlight any errors in the EVModel if they exist.
        highlightErrors(getErrors());
    }

    /** Display a dialog allowing the user to choose a hierarchy task,
     *  then add the selected task to the task list as a child of the
     *  task tree root. */
    protected void addTask() {
        if (!canEdit()) return;

        boolean madeChange = false;
        if (isRollup()) {
            String[] taskListNames = chooseTaskLists();
            if (taskListNames != null) {
                for (int i = 0; i < taskListNames.length; i++) {
                    if (taskListNames[i] != null
                            && model.addTask(taskListNames[i], dash.getData(),
                                    dash.getHierarchy(), dash.getCache(), true))
                        madeChange = true;
                }
            }

        } else {
            NodeSelectionDialog dialog = new NodeSelectionDialog
                (frame, dash.getHierarchy(),
                 resources.getString("Add_Task_Dialog.Title"),
                 resources.getString("Add_Task_Dialog.Instructions"),
                 resources.getString("Add_Task_Dialog.Button"),
                 null);
            String path = dialog.getSelectedPath();
            if (path != null)
                madeChange = model.addTask(path, dash.getData(),
                        dash.getHierarchy(), dash.getCache(), true);
        }

        if (madeChange) {
            treeTable.getTree().expandRow(0);
            setDirty(true);
            recalcAll();
            enableTaskButtons();
        }
    }

    /**
     * Certain external processes might add a task to this schedule
     * asynchronously.  When that occurs, this method will be called so we
     * can update our model to match the externally changed state.
     */
    protected void addExternallyAddedTask(String path) {
        boolean madeChange = model.addTask(path, dash.getData(), dash
                .getHierarchy(), dash.getCache(), true);
        if (madeChange) {
            treeTable.getTree().expandRow(0);
            recalcAll();
            enableTaskButtons();
        }
    }


    private String[] chooseTaskLists() {
        boolean includeImports = Settings.isTeamMode()
                || Settings.getBool("ev.addImportsToRollups", false);
        String[] taskListNames =
            EVTaskList.findTaskLists(dash.getData(), false, includeImports);
        taskListNames = insertRemoveElem
            (taskListNames,
             resources.getString("Import_Schedule.New_Schedule_Option"),
             this.taskListName);
        String[] taskListDisplayNames = EVTaskList.getDisplayNames(taskListNames);
        JList taskLists = new JList(taskListDisplayNames);
        new JOptionPaneClickHandler().install(taskLists);
        JScrollPane sp = new JScrollPane(taskLists);
        sp.setPreferredSize(new Dimension(300, 300));
        Object message = new Object[] {
            new JOptionPaneTweaker.MakeResizable(),
            resources.getString("Add_Schedule_Dialog.Instructions"), sp };
        if (JOptionPane.showConfirmDialog
            (frame, message,
             resources.getString("Add_Schedule_Dialog.Title"),
             JOptionPane.OK_CANCEL_OPTION)
            == JOptionPane.OK_OPTION) {

            int[] indexes = taskLists.getSelectedIndices();
            String[] result = new String[indexes.length];
            for (int i = 0; i < result.length; i++) {
                if (indexes[i] == 0)
                    result[i] = importNewSharedSchedule();
                else
                    result[i] = taskListNames[indexes[i]];
            }
            return result;
        }
        return null;
    }
    private String[] insertRemoveElem(String[] array, String elemToInsert,
            String elemToRemove) {
        String result[];
        if (array == null) {
            result = new String[1];
            result[0] = elemToInsert;
        } else {
            LinkedList list = new LinkedList(Arrays.asList(array));
            list.remove(elemToRemove);
            list.add(0, elemToInsert);
            result = (String[]) list.toArray(new String[list.size()]);
        }
        return result;
    }
    private class FocusHighlighter implements FocusListener {
        JTextField f;
        FocusHighlighter(JTextField field) { f = field; }
        public void focusGained(FocusEvent e) { f.selectAll(); }
        public void focusLost(FocusEvent e) { }
    }

    private String importNewSharedSchedule() {
        String urlStr = "http://";
        String passwordStr = "";
        String errorMessage = null;
        URL u = null;

        while (true) {
            // ask the user for the relevant information to locate the
            // schedule.
            JTextField url = new JTextField(urlStr, 40);
            url.addFocusListener(new FocusHighlighter(url));
            JTextField password = new JPasswordField(passwordStr, 10);
            password.addFocusListener(new FocusHighlighter(password));
            String urlLabel = resources.getString("Import_Schedule.URL_Label");
            String passwordLabel =
                resources.getString("Import_Schedule.Password_Label");
            Object message = new Object[] {
                errorMessage,
                resources.getString("Import_Schedule.Instructions"),
                newHBox(new JLabel("  "+urlLabel+" "), url),
                newHBox(new JLabel("  "+passwordLabel+" "), password) };
            if (JOptionPane.showConfirmDialog
                (frame, message,
                 resources.getString("Import_Schedule.Dialog_Title"),
                 JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION)
                // if the user didn't hit the OK button, return null.
                return null;

            urlStr = url.getText();
            passwordStr = password.getText();

            if (urlStr == null || urlStr.trim().length() == 0) {
                errorMessage = resources.getString
                    ("Import_Schedule.URL_Missing");
                continue;
            }
            if (urlStr.indexOf("/ev+/") != -1) {
                errorMessage = resources.getString("Import_Schedule.Pub_URL");
                continue;
            }
            try {
                u = new URL(urlStr.trim() + XML_QUERY_SUFFIX);
            } catch (MalformedURLException mue) {
                errorMessage = resources.getString
                    ("Import_Schedule.URL_Invalid");
                continue;
            }
            break;
        }

        // fetch the specified schedule.
        if (passwordStr != null) {
            passwordStr = passwordStr.trim();
            if (passwordStr.length() == 0) passwordStr = null;
        }
        CachedObject importedSchedule =
            new CachedURLObject(dash.getCache(),
                                EVTaskListCached.CACHED_OBJECT_TYPE,
                                u,
                                passwordStr == null ? null : "EV",
                                passwordStr);


        // check to see if there was an error in fetching the schedule.
        errorMessage = importedSchedule.getErrorMessage();
        String remoteName = null;
        if (errorMessage == null) {
            // if we were able to successfully fetch the schedule, try
            // to interpret its contents as an XML schedule.
            remoteName = EVTaskListCached.getNameFromXML
                (importedSchedule.getString("UTF-8"));

            // if we weren't able to interpret the fetched schedule,
            // record an error message.
            if (remoteName == null)
                errorMessage =
                    resources.getString("Import_Schedule.Invalid_Schedule");
        }

        // if there was any error, ask the user if they want to continue.
        if (errorMessage != null) {
            errorMessage = CachedURLObject.translateMessage
                (resources, "Import_Schedule.", errorMessage);
            Object message = new Object[] {
                resources.getString("Import_Schedule.Error.Header"),
                "    " + errorMessage,
                resources.getString("Import_Schedule.Error.Footer") };
            if (JOptionPane.showConfirmDialog
                (frame, message,
                 resources.getString("Import_Schedule.Error.Title"),
                 JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE)
                != JOptionPane.YES_OPTION)
                return null;
        }

        // get a local name to use for this schedule.
        String localName = remoteName;
        String owner = (String) importedSchedule.getLocalAttr
            (CachedURLObject.OWNER_ATTR);
        if (owner != null)
            localName = resources.format
                ("Import_Schedule.Default_Name_FMT", localName, owner);

        do {
            localName = (String) JOptionPane.showInputDialog
                (frame,
                 resources.getStrings("Import_Schedule.Name_Dialog.Prompt"),
                 resources.getString("Import_Schedule.Name_Dialog.Title"),
                 JOptionPane.PLAIN_MESSAGE, null, null, localName);
            if (localName != null)
                localName = localName.replace('/', ' ').trim();
        } while (localName == null || localName.length() == 0);
        importedSchedule.setLocalAttr(EVTaskListCached.LOCAL_NAME_ATTR,
                                      localName);

        // return the name of the cached schedule object
        return EVTaskListCached.buildTaskListName(importedSchedule.getID(),
                                                  localName);
    }
    private static final String XML_QUERY_SUFFIX = "?xml";

    /** delete the currently selected task.
     */
    protected void deleteTask() {
        if (!canEdit()) return;

        TreePath selPath = treeTable.getTree().getSelectionPath();
        if (selPath == null) return;
        if (isFlatView()) {
            EVTask selectedTask = (EVTask) selPath.getLastPathComponent();
            selPath = new TreePath(selectedTask.getPath());
        }

        int pathLen = selPath.getPathCount();
        if (isRollup()) {
            if (pathLen != 2 || !confirmDelete(selPath)) return;
        } else {
            if (pathLen < 2) return;
            if (pathLen == 2 && !confirmDelete(selPath)) return;
        }

        // make the change.
        if (model.removeTask(selPath)) {
            setDirty(true);
            recalcAll();
            enableTaskButtons();
        }
    }


    protected boolean confirmDelete(TreePath selPath) {
        EVTask task = (EVTask) selPath.getLastPathComponent();
        String fullName = isRollup() ? task.getName() : task.getFullName();
        String[] message =
            resources.formatStrings(isRollup()
                                        ? "Confirm.Delete_Schedule.Prompt_FMT"
                                        : "Confirm.Delete_Task.Prompt_FMT",
                                    fullName);
        return (JOptionPane.showConfirmDialog
                (frame, message,
                 isRollup()
                     ? resources.getString("Confirm.Delete_Schedule.Title")
                     : resources.getString("Confirm.Delete_Task.Title"),
                 JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)
                == JOptionPane.YES_OPTION);
    }

    /** Explode the currently selected task.
     *
     * Will only operate on tasks that are immediate children of the
     * task tree root.
    protected void explodeTask() {
        TreePath selPath = treeTable.getTree().getSelectionPath();
        if (selPath == null || selPath.getPathCount() != 2) return;

        // make the change.
        if (confirmExplode(selPath) && model.explodeTask(selPath)) {
            setDirty(true);
            recalcAll();
            enableTaskButtons();
        }
    }
    protected boolean confirmExplode(TreePath selPath) {
        EVTask task = (EVTask) selPath.getLastPathComponent();
        String fullName = task.getFullName();
        String[] message = new String[] {
            "By default, the task list is displayed hierarchically.",
            "If this prevents you from arranging tasks in the order",
            "they will be performed, you can \"explode\" items in your",
            "task list.",
            " ",
            "\"Exploding\" a task replaces the hierarchically organized",
            "task with a flat list of all its children.  You can",
            "then rearrange the children at will, but you will no",
            "longer be able to view them in the task list hierarchically.",
            "In addition, items added to that hierarchy in the future",
            "will no longer be automatically added to your task list.",
            " ",
            "Are you certain you want to explode the task,",
            "        '" + fullName + "'",
            "in this task list?" };
        return (JOptionPane.showConfirmDialog
                (frame, message, "Explode Task?",
                 JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)
                == JOptionPane.YES_OPTION);
    }
     */

    /** Swap the currently selected task with its previous sibling.
     *
     * Will only operate on tasks that are immediate children of the
     * task tree root.
     */
    protected void moveTaskUp() {
        if (!canEdit())
            return;

        if (isFlatView()) {
            moveTaskUpFlatView();
        } else {
            moveTaskUpTreeView();
        }
    }

    protected void moveTaskUpFlatView() {
        int firstRowNum = treeTable.getSelectionModel().getMinSelectionIndex();
        int lastRowNum = treeTable.getSelectionModel().getMaxSelectionIndex();
        if (treeTable.getSelectedRowCount() < lastRowNum - firstRowNum+1) {
            TransferHandler.getCopyAction().actionPerformed(
                    new ActionEvent(treeTable, ActionEvent.ACTION_PERFORMED,
                            "Copy"));
            treeTable.getSelectionModel().setSelectionInterval(firstRowNum - 1,
                    firstRowNum - 1);
            TransferHandler.getPasteAction().actionPerformed(
                    new ActionEvent(treeTable, ActionEvent.ACTION_PERFORMED,
                            "Paste"));
        } else {
            if (flatModel.moveTasksUp(firstRowNum-1, lastRowNum-1)) {
                setDirty(true);
                SwingUtilities.invokeLater
                (new RowSelectionTask(firstRowNum-1, lastRowNum-1, false));
            }
        }
    }

    private void moveTaskUpTreeView() {
        TreePath selPath = treeTable.getTree().getSelectionPath();

        // make the change.
        if (model.moveTaskUp(selectedTaskPos(selPath, model))) {
            setDirty(true);
            recalcAll();

            // reselect the item that moved.
            int row = treeTable.getTree().getRowForPath(selPath);
            SwingUtilities.invokeLater(new RowSelectionTask(row, row, false));
        }
    }


    /** Swap the currently selected task with its next sibling.
     *
     * Will only operate on tasks that are immediate children of the
     * task tree root.
     */
    protected void moveTaskDown() {
        if (!canEdit())
            return;

        if (isFlatView()) {
            moveTaskDownFlatView();
        } else {
            moveTaskDownTreeView();
        }
    }

    protected void moveTaskDownFlatView() {
        int firstRowNum = treeTable.getSelectionModel().getMinSelectionIndex();
        int lastRowNum = treeTable.getSelectionModel().getMaxSelectionIndex();
        if (treeTable.getSelectedRowCount() < lastRowNum - firstRowNum+1) {
            TransferHandler.getCopyAction().actionPerformed(
                    new ActionEvent(treeTable, ActionEvent.ACTION_PERFORMED,
                            "Copy"));
            treeTable.getSelectionModel().setSelectionInterval(lastRowNum + 2,
                    lastRowNum + 2);
            TransferHandler.getPasteAction().actionPerformed(
                    new ActionEvent(treeTable, ActionEvent.ACTION_PERFORMED,
                            "Paste"));
        } else {
            if (flatModel.moveTasksDown(firstRowNum-1, lastRowNum-1)) {
                setDirty(true);
                SwingUtilities.invokeLater
                (new RowSelectionTask(firstRowNum+1, lastRowNum+1, true));
            }
        }
    }

    private void moveTaskDownTreeView() {
        TreePath selPath = treeTable.getTree().getSelectionPath();

        // make the change.
        if (model.moveTaskUp(selectedTaskPos(selPath, model)+1)) {
            setDirty(true);
            recalcAll();

            // reselect the item that moved.
            int row = treeTable.getTree().getRowForPath(selPath);
            SwingUtilities.invokeLater(new RowSelectionTask(row, row, true));
        }
    }


    /** In Flat View, give the user options to sort tasks in various ways */
    private void sortTasks() {
        // we shouldn't be sorting tasks unless we're in flat view
        if (flatModel == null)
            return;

        // if an editing session is in progress, stop it
        stopCellEditing();

        // determine which sorting options are applicable
        boolean hasMilestones = model.showMilestoneColumn();
        boolean sortColumnVisible = isSortColumnVisible();

        if (hasMilestones) {
            // if milestones are present, present various options to the user
            showSortOptionsPrompt(sortColumnVisible);

        } else if (sortColumnVisible) {
            // if the sort column is visible, ask the user if they want to
            // reorder the tasks based on its contents
            showSortTagsPrompt();

        } else {
            // if neither option is currently applicable, make the sort column
            // visible, and display a message explaining how it is used
            makeSortColumnVisible();
        }
    }

    private void showSortOptionsPrompt(boolean sortColumnVisible) {
        // create radio button and textual description for "sort by tag"
        ButtonGroup group = new ButtonGroup();
        JRadioButton sortColumnOption = new JRadioButton(
                resources.getString("Buttons.Sort_Tasks.Tag.Option"));
        String[] sortColumnDescr = getIndentedText(
            "Buttons.Sort_Tasks.Tag.Description");
        group.add(sortColumnOption);

        // create radio button and textual description for "sort by milestone"
        JRadioButton milestoneOption = new JRadioButton(
                resources.getString("Buttons.Sort_Tasks.Milestone.Option"));
        String[] milestoneDescr = getIndentedText(
            "Buttons.Sort_Tasks.Milestone.Description");
        group.add(milestoneOption);

        // select a reasonable default option
        if (isSortColumnSelected())
            sortColumnOption.setSelected(true);
        else
            milestoneOption.setSelected(true);

        // show a prompt to the user
        String title = resources.getString("Buttons.Sort_Tasks.Title");
        Object message = new Object[] { //
                sortColumnOption, sortColumnDescr, " ", //
                milestoneOption, milestoneDescr, " ", //
        };
        int userChoice = JOptionPane.showConfirmDialog(frame, message, title,
            JOptionPane.OK_CANCEL_OPTION);
        if (userChoice != JOptionPane.OK_OPTION)
            return;

        // take action based on the selected option
        if (milestoneOption.isSelected()) {
            sortByMilestones();

        } else if (sortColumnOption.isSelected()) {
            if (!sortColumnVisible)
                makeSortColumnVisible();
            else
                sortByTags();
        }
    }

    private String[] getIndentedText(String resKey) {
        String[] lines = resources.getStrings(resKey);
        for (int i = lines.length; i-- > 0;)
            lines[i] = "            " + lines[i];
        return lines;
    }


    private boolean isSortColumnVisible() {
        int v = treeTable.convertColumnIndexToView(EVTaskList.SORT_TAG_COLUMN);
        return v != -1;
    }

    private boolean isSortColumnSelected() {
        int selCol = treeTable.getSelectedColumn();
        if (selCol == -1)
            return false;

        int selIndex = treeTable.convertColumnIndexToModel(selCol);
        return selIndex == EVTaskList.SORT_TAG_COLUMN;
    }

    private void makeSortColumnVisible() {
        if (sortTagColumn == null)
            return;

        // insert the column in the desired location
        int appendPos = flatColumnModel.getColumnCount();
        int insertAfter = Math.max(
            treeTable.convertColumnIndexToView(EVTaskList.FORECAST_DATE_COLUMN),
            treeTable.convertColumnIndexToView(EVTaskList.DATE_COMPLETE_COLUMN));
        flatColumnModel.addColumn(TableUtils.cloneTableColumn(sortTagColumn));
        if (insertAfter != -1)
            flatColumnModel.moveColumn(appendPos, insertAfter + 1);

        // display a message explaining what we've done
        JOptionPane.showMessageDialog(frame,
            resources.getStrings("Buttons.Sort_Tasks.Tag.Column_Message"),
            resources.getString("Buttons.Sort_Tasks.Tag.Option"),
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void showSortTagsPrompt() {
        int userChoice = JOptionPane.showConfirmDialog(frame,
            resources.getStrings("Buttons.Sort_Tasks.Tag.Confirmation"),
            resources.getString("Buttons.Sort_Tasks.Title"),
            JOptionPane.OK_CANCEL_OPTION);
        if (userChoice == JOptionPane.OK_OPTION)
            sortByTags();
    }

    private void sortByTags() {
        if (flatModel.sortTasksByTag())
            setDirty(true);
    }


    private void sortByMilestones() {
        if (flatModel.sortTasksByMilestone())
            setDirty(true);
    }


    private class RowSelectionTask implements Runnable {
        private int firstRow;
        private int lastRow;
        private boolean downward;
        public RowSelectionTask(int firstRow, int lastRow, boolean downward) {
            this.firstRow = firstRow;
            this.lastRow = lastRow;
            this.downward = downward;
        }
        public void run() {
            if (firstRow != -1) {
                treeTable.getSelectionModel().setSelectionInterval
                    (firstRow, lastRow);
                enableTaskButtons();

                int rowToShow =
                    (downward
                        ? Math.min(lastRow+1, treeTable.getRowCount()-1)
                        : Math.max(firstRow-1, 0));
                scrollToRow(rowToShow);
            }
        }
    }

    protected int selectedTaskPos(TreePath selPath, TreeModel model) {
        if (selPath == null) return -1;

        int pathLen = selPath.getPathCount();
        if (pathLen != 2) return -1; // only adjust children of the root node.
        EVTask selectedTask = (EVTask) selPath.getPathComponent(pathLen-1);
        EVTask parentNode = (EVTask) selPath.getPathComponent(pathLen-2);
        return model.getIndexOfChild(parentNode, selectedTask);
    }

    protected void enableTaskButtons(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting())
            SwingUtilities.invokeLater(new EnableTaskButtons());
    }
    private class EnableTaskButtons implements Runnable {
        public void run() { enableTaskButtons(); }
    }

    protected void enableTaskButtons() {
        if (treeTable != null) {
            filteredChartAction.putValue(Action.SHORT_DESCRIPTION, null);
            filteredReportAction.putValue(Action.SHORT_DESCRIPTION, null);

            if (isFlatView())
                enableTaskButtonsFlatView();
            else if (isMergedView())
                enableTaskButtonsMergedView();
            else
                enableTaskButtonsTreeView();
        }
    }

    private void enableTaskButtonsFlatView() {
        int firstRowNum = treeTable.getSelectionModel().getMinSelectionIndex();
        int lastRowNum = treeTable.getSelectionModel().getMaxSelectionIndex();

        boolean enableDelete = (disableTaskPruning == false
                && firstRowNum > 0 && firstRowNum == lastRowNum);

        boolean enableUp = (lastRowNum - treeTable.getSelectedRowCount() > 0);
        boolean enableDown = (lastRowNum > 0 && (firstRowNum + treeTable
                .getSelectedRowCount()) < treeTable.getRowCount());

        //addTaskButton    .setEnabled(false);
        deleteTaskAction .setEnabled(enableDelete);
        deleteTaskAction .setText(resources.getString("Buttons.Exclude_Task"));
        moveUpAction     .setEnabled(enableUp);
        moveDownAction   .setEnabled(enableDown);
        expandAllAction  .setEnabled(false);
        sortTasksAction  .setEnabled(true);
        filteredChartAction.setEnabled(false);
        filteredReportAction.setEnabled(false);
        copyTaskInfoAction.setEnabled(firstRowNum >= 0);
    }

    private void enableTaskButtonsMergedView() {
        addTaskAction    .setEnabled(false);
        deleteTaskAction .setEnabled(false);
        moveUpAction     .setEnabled(false);
        moveDownAction   .setEnabled(false);
        expandAllAction  .setEnabled(true);

        int firstRowNum = treeTable.getSelectionModel().getMinSelectionIndex();
        int lastRowNum = treeTable.getSelectionModel().getMaxSelectionIndex();
        boolean canFilter = firstRowNum > 0 && firstRowNum == lastRowNum;
        filteredChartAction.setEnabled(canFilter);
        filteredReportAction.setEnabled(canFilter);
        copyTaskInfoAction.setEnabled(firstRowNum >= 0);
    }


    private void enableTaskButtonsTreeView() {
        TreePath selectionPath = treeTable.getTree().getSelectionPath();
        boolean enableDelete = false,
            enableUp = false, enableDown = false, isPruned = false;
        int pos = selectedTaskPos(selectionPath, model);
        if (pos != -1) {
            int numKids = ((EVTask) model.getRoot()).getNumChildren();

            isPruned = false;
            enableDelete = true;
            enableUp     = (pos > 0);
            enableDown   = (pos < numKids-1);

        } else if (!isRollup() && selectionPath != null &&
                   selectionPath.getPathCount() > 2) {
            isPruned =
                ((EVTask) selectionPath.getLastPathComponent()).isUserPruned();
            enableDelete = isPruned || (disableTaskPruning == false);
        }

        addTaskAction    .setEnabled(true);
        deleteTaskAction .setEnabled(enableDelete);
        if (!isRollup()) {
            String resKey;
            if (isPruned)
                resKey = "Buttons.Restore_Task";
            else if (pos == -1 && enableDelete)
                resKey = "Buttons.Exclude_Task";
            else
                resKey = "Buttons.Remove_Task";
            deleteTaskAction.setText(resources.getString(resKey));
        }
        moveUpAction     .setEnabled(enableUp);
        moveDownAction   .setEnabled(enableDown);
        expandAllAction  .setEnabled(true);
        sortTasksAction  .setEnabled(false);

        int firstRowNum = treeTable.getSelectionModel().getMinSelectionIndex();
        int lastRowNum = treeTable.getSelectionModel().getMaxSelectionIndex();
        boolean canFilter = firstRowNum > 0 && firstRowNum == lastRowNum;
        filteredChartAction.setEnabled(canFilter //
                && checkPermission(filteredChartAction, firstRowNum,
                    EVPermissions.PERSONAL_CHARTS, "Chart"));
        filteredReportAction.setEnabled(canFilter //
                && checkPermission(filteredReportAction, firstRowNum,
                    EVPermissions.PERSONAL_REPORT, "Report"));
        copyTaskInfoAction.setEnabled(firstRowNum >= 0);
    }

    private boolean checkPermission(AbstractAction a, int row,
            String permissionID, String resKey) {
        // if this is not a rollup in a team dashboard, permissions are OK
        if (!isRollup() || !Settings.isTeamMode())
            return true;

        // get the list of people we can view data for. If null, the current
        // user does not have permission to view personal data at all.
        UserFilter userFilter = GroupPermission.getGrantedMembers(permissionID);
        if (userFilter == null) {
            a.putValue(Action.SHORT_DESCRIPTION,
                resources.getString("Buttons.Filtered_" + resKey + "_Blocked"));
            return false;
        }

        // if the user is allowed to view data for everyone, return true
        if (UserGroup.isEveryone(userFilter))
            return true;

        // find the personal schedule we are asking to view data for.
        TreePath path = treeTable.getTree().getPathForRow(row);
        if (path.getPathCount() < 2)
            return false;
        EVTask rootTask = (EVTask) path.getPathComponent(1);
        EVTaskList subSchedule = findTaskListWithRoot(model, rootTask);
        if (subSchedule == null)
            return false;

        // see if the current user has permission to view this schedule
        EVTaskListGroupFilter filter = new EVTaskListGroupFilter(userFilter);
        if (filter.include(subSchedule.getID()))
            return true;

        // if we don't have permission, display an explanatory tooltip
        a.putValue(Action.SHORT_DESCRIPTION,
            resources.getString("Buttons.Filtered_" + resKey + "_Restricted"));
        return false;
    }


    protected void toggleFlatView() {
        boolean isCurrentlyFlat = (treeTable.getColumnModel() == flatColumnModel);
        boolean shouldBeFlat = isFlatView();
        if (shouldBeFlat == isCurrentlyFlat)
            return;

        List<EVTask> selectedTasks = getSelectedTasks();
        if (shouldBeFlat == false) {
            changeTreeTableModel(model, treeColumnModel);
            treeTable.setDragEnabled(false);
            treeTable.setSelectionMode(
                    ListSelectionModel.SINGLE_INTERVAL_SELECTION);
            setActiveMilestone(null);
        } else {
            boolean isFirstTimeForFlatView = false;
            if (flatModel == null) {
                isFirstTimeForFlatView = true;
                flatModel = model.getFlatModel();
                HierarchyNoteManager.addHierarchyNoteListener(flatModel);
            }
            changeTreeTableModel(flatModel, flatColumnModel);
            if (isFirstTimeForFlatView)
                guiPrefs.load("flatTable", treeTable);
            treeTable.setDragEnabled(true);
            treeTable.setSelectionMode(
                    ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        }
        setSelectedTasks(selectedTasks);

        enableTaskButtons();
    }

    protected void expandAllForSelectedItems() {
        List<EVTask> selectedTasks = getSelectedTasksOrRoot();
        for (EVTask task : selectedTasks)
            expandAllForTask(task);
        selectNodesAndDescendants(selectedTasks);
    }

    private void expandAllForTask(EVTask task) {
        for (EVTask child : task.getChildren())
            expandAllForTask(child);
        TreePath path = new TreePath(task.getPath());
        treeTable.getTree().expandPath(path);
    }


    private void selectNodesAndDescendants(List<EVTask> selectedTasks) {
        int[] newSelection = new int[] { Integer.MAX_VALUE, -1 };
        for (EVTask task : selectedTasks)
            getRowRangeForTaskAndDescendants(task, newSelection);
        if (newSelection[1] > -1) {
            treeTable.clearSelection();
            treeTable.getSelectionModel().addSelectionInterval(newSelection[0],
                newSelection[1]);
        }
    }

    private void getRowRangeForTaskAndDescendants(EVTask task, int[] range) {
        TreePath path = new TreePath(task.getPath());
        int row = treeTable.getTree().getRowForPath(path);
        if (row != -1) {
            range[0] = Math.min(range[0], row);
            range[1] = Math.max(range[1], row);

            List<EVTask> children = task.getChildren();
            if (!children.isEmpty())
                getRowRangeForTaskAndDescendants(
                    children.get(children.size() - 1), range);
        }
    }

    private List<EVTask> getSelectedTasksOrRoot() {
        List<EVTask> selectedTasks = getSelectedTasks();
        if (selectedTasks.isEmpty())
            selectedTasks = Collections.singletonList( //
                    (EVTask) treeTable.getTree().getModel().getRoot());
        return selectedTasks;
    }

    private List<EVTask> getSelectedTasks() {
        List<EVTask> result = new ArrayList<EVTask>();
        TreePath[] selectedPaths = treeTable.getTree().getSelectionPaths();
        if (selectedPaths != null) {
            for (TreePath path : selectedPaths)
                result.add((EVTask) path.getLastPathComponent());
        }
        return result;
    }

    private void setSelectedTasks(List<EVTask> tasks) {
        if (tasks != null && !tasks.isEmpty()) {
            if (isFlatView())
                setSelectedTasksFlatView(tasks);
            else
                setSelectedTasksTreeView(tasks);
        }
    }

    private void setSelectedTasksFlatView(List<EVTask> tasks) {
        treeTable.clearSelection();
        for (EVTask task : tasks)
            if (task != model.getRoot())
                addTaskToFlatViewSelection(task);
        scrollToRow(treeTable.getSelectionModel().getMaxSelectionIndex());
        scrollToRow(treeTable.getSelectionModel().getMinSelectionIndex());
    }

    private void addTaskToFlatViewSelection(EVTask task) {
        if (task.isLeaf()) {
            int pos = flatModel.getIndexOfChild(flatModel.getRoot(), task);
            if (pos != -1)
                treeTable.getSelectionModel().addSelectionInterval(pos+1, pos+1);
        } else {
            for (int i = task.getNumChildren();  i-- > 0;)
                addTaskToFlatViewSelection(task.getChild(i));
        }
    }

    private void setSelectedTasksTreeView(List<EVTask> tasks) {
        treeTable.clearSelection();
        EVTask t = tasks.get(0);
        TreePath path = new TreePath(t.getPath());
        treeTable.getTree().makeVisible(path);
        int row = treeTable.getTree().getRowForPath(path);
        if (row != -1) {
            treeTable.getSelectionModel().addSelectionInterval(row, row);
            scrollToRow(row);
        }
    }

    private void scrollToRow(int row) {
        if (row != -1)
            treeTable.scrollRectToVisible(treeTable.getCellRect(row, 0, true));
    }


    private TableColumnModel createFlatColumnModel() {
        DefaultTableColumnModel result = new DefaultTableColumnModel();

        int extraWidth = 0;
        for (int i = 0;   i < treeColumnModel.getColumnCount();  i++) {
            TableColumn c = treeColumnModel.getColumn(i);
            switch (c.getModelIndex()) {
            case EVTaskList.TASK_COLUMN:
            case EVTaskList.NODE_TYPE_COLUMN:
            case EVTaskList.PLAN_TIME_COLUMN:
            case EVTaskList.PLAN_DTIME_COLUMN:
            //case EVTaskList.ACT_TIME_COLUMN:
            //case EVTaskList.ACT_DTIME_COLUMN:
            case EVTaskList.PLAN_DATE_COLUMN:
            case EVTaskList.REPLAN_DATE_COLUMN:
            case EVTaskList.FORECAST_DATE_COLUMN:
            case EVTaskList.MILESTONE_COLUMN:
            case EVTaskList.LABELS_COLUMN:
            case EVTaskList.NOTES_COLUMN:
            case EVTaskList.DEPENDENCIES_COLUMN:
            case EVTaskList.PCT_SPENT_COLUMN:
                result.addColumn(TableUtils.cloneTableColumn(c));
                break;

            case EVTaskList.SORT_TAG_COLUMN:
                sortTagColumn = c;
                break;

            default:
                extraWidth += c.getPreferredWidth();
                break;
            }
        }

        TableColumn c = result.getColumn(0);
        c.setPreferredWidth(c.getWidth() + extraWidth);

        return result;
    }


    protected void toggleMergedView() {
        if (!isMergedView())
            changeTreeTableModel(model, treeColumnModel);
        else {
            boolean needsExpansionSetup = false;
            if (mergedModel == null) {
                mergedModel = model.getMergedModel();
                needsExpansionSetup = true;
            }
            changeTreeTableModel(mergedModel, treeColumnModel);
            if (needsExpansionSetup)
                readExpandedNodesPref(model.getID());
        }

        enableTaskButtons();
    }

    protected void changeTreeTableModel(TreeTableModel m,
            TableColumnModel columnModel) {
        // changing the TreeTableModel below causes our column model to
        // be completely recreated from scratch.  Unfortunately, this
        // loses all information about column width, tooltips, etc.  To
        // avoid this, we temporarily install a discardable column model.
        // The disruptive changes will be made to it, then we reinstall
        // the desired column model when we're done.
        //    Note that we can do this only because we know that we'll
        // be replacing the TreeTableModel with another one that is
        // exactly compatible.
        treeTable.setColumnModel(new DefaultTableColumnModel());
        treeTable.setTreeTableModel(m);
        treeTable.setColumnModel(columnModel);
    }


    protected void addScheduleRow() {
        model.getSchedule().addRow();
        setDirty(true);
    }
    protected void insertScheduleRow() {
        int[] rows = scheduleTable.getSelectedRows();
        if (rows == null || rows.length == 0) return;
        model.getSchedule().insertRow(rows[0]);
        setDirty(true);
    }
    protected void deleteScheduleRows() {
        int[] rows = scheduleTable.getSelectedRows();
        if (rows == null || rows.length == 0) return;
        for (int i = rows.length;  i-- > 0; )
            model.getSchedule().deleteRow(rows[i]);
        setDirty(true);
    }

    protected void showHideColumns() {
        // update the task table
        showHideColumns(treeColumnModel, treeTable.getModel(),
            EVTaskList.HIDABLE_COLUMN_LIST, EVTaskList.colWidths);
        showHideColumns(flatColumnModel, treeTable.getModel(),
            EVTaskList.HIDABLE_COLUMN_LIST, EVTaskList.colWidths);

        // update the schedule table
        showHideColumns(scheduleTable.getColumnModel(), model.getSchedule(),
            EVSchedule.DIRECT_COLUMN_LIST, EVSchedule.colWidths);
    }

    private void showHideColumns(TableColumnModel columnModel,
            TableModel tableModel, int[] hidableColumns, int[] columnWidths) {
        if (columnModel == null)
            return;

        for (int i : hidableColumns) {
            int viewPos = TableUtils.convertColumnIndexToView(columnModel, i);
            if (viewPos != -1)
                showHideColumn(columnModel.getColumn(viewPos), tableModel
                        .getColumnName(i), columnWidths[i]);
        }
    }

    protected void showHideColumn(TableColumn col, String name, int width) {

        boolean hide = (name == null || name.endsWith(" "));
        boolean hidden = (col.getMaxWidth() == 0);
        if (hide != hidden) {
            col.setMinWidth(0);
            col.setMaxWidth(hide ? 0 : width);
            col.setPreferredWidth(hide ? 0 : width);
        }
    }

    protected void enableScheduleButtons() {
        if (isRollup() || Settings.isReadOnly()) return;

        boolean enableDelete = false, enableInsert = false;
        int[] rows = scheduleTable.getSelectedRows();
        if (rows != null && rows.length > 0) {
            if (model.getSchedule().areDatesLocked()) {
                Arrays.sort(rows);
                int minRow = rows[0];
                enableDelete = !model.getSchedule().rowIsAutomatic(minRow);
                for (int i = minRow;  i < model.getSchedule().getRowCount(); i++) {
                    if (model.getSchedule().rowIsAutomatic(i))
                        break;
                    else if (Arrays.binarySearch(rows, i) < 0)
                        enableDelete = false;
                }

            } else {
                enableInsert = true;
                for (int i = rows.length;  i-- > 0; )
                    if (!model.getSchedule().rowIsAutomatic(rows[i])) {
                        enableDelete = true; break;
                    }
            }
        }

        insertPeriodAction.setEnabled(enableInsert);
        deletePeriodAction.setEnabled(enableDelete);
    }

    protected void maybeDisplayErrorButton() {
        errorAction.setEnabled(getErrors() != null);
    }

    protected void close() {
        guiPrefs.saveAll();

        TaskScheduleChooser.close(taskListName);
        if (dash instanceof ApplicationEventSource)
            ((ApplicationEventSource) dash)
                    .removeApplicationEventListener(this);
        frame.dispose();

        removePermissionsListeners(saveBaselineAction, manageBaselinesAction);

        if (shouldUseExpandedNodesPref()) {
            saveExpandedNodesPref(model.getID());
        }
        model.setNodeListener(null);
        model.removeRecalcListener(this);
        model.removeTreeModelWillChangeListener((TaskJTreeTable)treeTable);
        model.removeTreeModelListener((TaskJTreeTable)treeTable);
        model.getSchedule().setListener(null);
        model = null;
        if (flatModel != null)
            HierarchyNoteManager.removeHierarchyNoteListener(flatModel);
        if (mergedModel instanceof Disposable)
            ((Disposable) mergedModel).dispose();
        treeTable.dispose();
        treeTable = null;
        scheduleTable = null;
    }

    private void removePermissionsListeners(Object... listeners) {
        PermissionsManager mgr = PermissionsManager.getInstance();
        for (Object l : listeners) {
            if (l instanceof PermissionsChangeListener) {
                mgr.removePermissionsChangeListener(
                    (PermissionsChangeListener) l);
            }
        }
    }

    protected void save() {
        if (dirtySubschedules != null) {
            for (EVTaskList subschedule : dirtySubschedules)
                subschedule.save();
            dirtySubschedules = null;
        }
        model.save();
        setDirty(false);
        displayErrorDialog(getErrors());
    }

    protected void configureEditor(JTable table) {
        JDateTimeChooserCellEditor editor = new JDateTimeChooserCellEditor(
                DATE_FORMAT);
        editor.setClickCountToStart(2);
        table.setDefaultEditor(Date.class, editor);
    }

    public void saveBaseline() {
        if (groupFilterMenu != null)
            groupFilterMenu.setSelectedItem(UserGroup.EVERYONE);

        String snapshotName = resources.format(
            "Save_Baseline.Snapshot_Name_FMT", new Date());
        String[] userValues = TaskScheduleSnapshotManager.showSnapEditDialog(
            frame, resources.getString("Save_Baseline.Save_Dialog.Title"),
            snapshotName, null, true);
        if (userValues == null)
            return;

        if (isDirty) {
            if (JOptionPane.OK_OPTION != JOptionPane.showConfirmDialog(frame,
                resources.getString("Save_Baseline.Confirm_Save.Prompt"),
                resources.getString("Save_Baseline.Confirm_Save.Title"),
                JOptionPane.OK_CANCEL_OPTION))
                return;
        }

        String userSelectedName = userValues[0];
        if (userSelectedName.length() > 0)
            snapshotName = userSelectedName;
        String comment = userValues[1];
        String snapshotId = model.saveSnapshot(null, snapshotName, comment);
        if ("true".equals(userValues[2]))
            model.setMetadata(EVMetadata.Baseline.SNAPSHOT_ID, snapshotId);
        model.save();
        setDirty(false);

        recalcAll();
    }

    public void manageBaselines() {
        // get a list of the snapshots registered for this task list.  If no
        // snapshots exist, print a message and abort.
        List<EVSnapshot.Metadata> snapshots = model.getSnapshots();
        if (snapshots.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                resources.getString("Manage_Baselines.No_Baselines.Prompt"),
                resources.getString("Manage_Baselines.No_Baselines.Title"),
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (groupFilterMenu != null)
            groupFilterMenu.setSelectedItem(UserGroup.EVERYONE);

        String activeSnapshotId = model.getMetadata(EVMetadata.Baseline.SNAPSHOT_ID);
        TaskScheduleSnapshotManager manager = new TaskScheduleSnapshotManager(
                dash.getData(), model, snapshots, activeSnapshotId);
        JOptionPane.showMessageDialog(this.frame, manager,
            resources.getString("Manage_Baselines.Window_Title"),
            JOptionPane.PLAIN_MESSAGE);
        if (manager.isDirty()) {
            if (canEdit()) {
                setDirty(true);
            } else {
                model.save();
            }
            recalcAll();
        }
    }

    public void showCollaborationWizard() {
        if (saveOrCancel(true))
            new TaskScheduleCollaborationWizard(dash, taskListName);
    }

    protected void showOptionsDialog() {
        if (optionsDialog != null) {
            optionsDialog.show();
        } else {
            optionsDialog = new TaskScheduleOptions(this);
        }
    }

    public void showChart() {
        if (chartDialog != null && chartDialog.isDisplayable()) {
            chartDialog.setVisible(true);
            chartDialog.toFront();
        } else
            chartDialog = new TaskScheduleChart(model, null, groupFilterMenu,
                    dash);
    }

    public void showFilteredChart() {
        TreePath selectionPath = treeTable.getTree().getSelectionPath();
        if (selectionPath == null) return;

        // If the user has selected a node in the tree that has no siblings,
        // we can move up a generation and start the filtering there.
        EVTask task = (EVTask) selectionPath.getLastPathComponent();
        EVTask parent = task.getParent();
        while (parent != null && parent.getNumChildren() == 1) {
            task = parent;
            parent = task.getParent();
        }

        // The "filtered" node selected was really a sole descendant of the
        // main task list.  We can just display the regular, unfiltered chart.
        if (parent == null) {
            showChart();
            return;
        }

        // The selected node happens to be the root of some subschedule. No
        // filtering is needed;  just display the chart for that schedule.
        EVTaskList subSchedule = findTaskListWithRoot(model, task);
        if (subSchedule != null) {
            new TaskScheduleChart(subSchedule, null, groupFilterMenu, dash);
            return;
        }

        // Construct a string describing the filtered path
        StringBuffer filteredPath = new StringBuffer();
        for (int i = 1;  i < selectionPath.getPathCount();  i++) {
            EVTask t = (EVTask) selectionPath.getPathComponent(i);
            filteredPath.append("/").append(t.getName());
            if (t == task) break;
        }

        // Build an appropriate filter, and use it to launch a chart window
        TaskFilter filter = new TaskFilter(filteredPath.substring(1),
                getTaskFilterSet(task));
        new TaskScheduleChart(model, filter, groupFilterMenu, dash);
    }

    private EVTaskList findTaskListWithRoot(EVTaskList tl, EVTask possibleRoot) {
        if (tl.getRoot() == possibleRoot)
            return tl;
        if (tl instanceof EVTaskListRollup) {
            EVTaskListRollup rtl = (EVTaskListRollup) tl;
            for (int i = ((EVTask) rtl.getRoot()).getNumChildren(); i-- > 0; ) {
                EVTaskList result = findTaskListWithRoot(rtl.getSubSchedule(i),
                        possibleRoot);
                if (result != null)
                    return result;
            }
        }
        return null;
    }

    private Set getTaskFilterSet(EVTask task) {
        if (isMergedView())
            return (Set) mergedModel.getValueAt(task,
                    EVTaskList.MERGED_DESCENDANT_NODES);

        HashSet result = new HashSet();
        result.add(task);
        result.addAll(task.getDescendants());
        return result;
    }

    private class TaskFilter extends EVHierarchicalFilter {

        public TaskFilter(String displayName, Set includedTasks) {
            super(displayName, includedTasks);
        }

        public String getAttribute(String name) {
            if (EVTaskFilter.IS_INVALID.equals(name)) {
                Set tasks = new HashSet(includedTasks);
                Set currentTasks = ((EVTask) model.getRoot()).getDescendants();
                tasks.retainAll(currentTasks);
                if (tasks.isEmpty())
                    return "invalid";
            }

            return super.getAttribute(name);
        }

    }

    public static final String WEEK_URL = "//reports/week.class";
    public void showWeekReport() {
        if (saveOrCancel(true))
            showReport(taskListName, null, getUserFilter(), WEEK_URL);
    }


    public static final String REPORT_URL = "//reports/ev.class";
    public void showHTML() {
        if (saveOrCancel(true))
            showReport(taskListName, getUserFilter());
    }

    public void showFilteredHTML() {
        TreePath selectionPath = treeTable.getTree().getSelectionPath();
        if (selectionPath == null) return;

        if (!saveOrCancel(true))
            return;

        // If the user has selected a node in the tree that has no siblings,
        // we can move up a generation and start the filtering there.
        EVTask task = (EVTask) selectionPath.getLastPathComponent();
        EVTask parent = task.getParent();
        while (parent != null && parent.getNumChildren() == 1) {
            task = parent;
            parent = task.getParent();
        }

        // The "filtered" node selected was really a sole descendant of the
        // main task list.  We can just display the regular, unfiltered chart.
        if (parent == null) {
            showReport(taskListName, getUserFilter());
            return;
        }

        // In merged view, calculate the report parameters for the selected
        // node and launch the report.
        if (isMergedView()) {
            String option = EVReportSettings.MERGED_PATH_FILTER_PARAM + "="
                    + HTMLUtils.urlEncode(getMergedPathFilterParam(task));
            showReport(taskListName, option, getUserFilter(), REPORT_URL);
            return;
        }

        // find the root of the task list containing the selected node.
        EVTask root = task;
        String subPath = null;
        while (root != null && root.getFlag() == null) {
            subPath = root.getName() + (subPath == null ? "" : "/" + subPath);
            root = root.getParent();
        }
        EVTaskList subSchedule = findTaskListWithRoot(model, root);
        if (subSchedule == null)
            return;

        String option;
        if (task == root) {
            // The selected node happens to be the root of some subschedule. No
            // filtering is needed;  just display the chart for that schedule.
            option = null;
        } else {
            // if the task isn't the schedule root, send subpath to the report
            option = EVReportSettings.PATH_FILTER_PARAM + "="
                    + HTMLUtils.urlEncode(subPath);
        }
        showReport(subSchedule.getTaskListName(), option, null, REPORT_URL);
    }

    /** Calculate a merged path string to pass to the report logic.
     * 
     * @param task a task node in a merged task list
     * @return a path string that can be used to relocate that node later for
     *     filtering purposes
     */
    private String getMergedPathFilterParam(EVTask task) {
        if (task.getFlag() != null)
            return maybeAppendExtraPath(EVHierarchicalFilter.MERGED_ROOT_ID,
                task);

        List taskIDs = task.getTaskIDs();
        if (taskIDs != null && !taskIDs.isEmpty())
            return maybeAppendExtraPath((String) taskIDs.get(0), task);

        return getMergedPathFilterParam(task.getParent()) + "/"
                + task.getName();
    }

    private String maybeAppendExtraPath(String base, EVTask task) {
        // In merged mode, nodes may be "simplified", containing multiple
        // slash-concatenated path segments. Thus, when we find an identifiable
        // node, we still need to check if it has multiple path segments, and
        // adopt all but the first segment in our identifying path.
        String taskName = task.getName();
        int slashPos = taskName.indexOf('/');
        if (slashPos == -1)
            return base;
        else
            return base + taskName.substring(slashPos);
    }


    public static void showReport(String taskListName, UserFilter f) {
        showReport(taskListName, null, f, REPORT_URL);
    }

    public static void showReport(String taskListName, String options,
            UserFilter f, String reportUri) {
        String uri = "/" + HTMLUtils.urlEncode(taskListName) + reportUri;
        if (StringUtils.hasValue(options))
            uri = uri + "?" + options;
        if (f != null)
            uri = HTMLUtils.appendQuery(uri,
                EVReportSettings.GROUP_FILTER_PARAM, f.getId());
        Browser.launch(uri);
    }


    public void handleApplicationEvent(ActionEvent e) {
        if (APP_EVENT_SAVE_ALL_DATA.equals(e.getActionCommand())) {
            saveOrCancel(false);
        }
    }


    public void confirmClose(boolean showCancel) {
        if (saveOrCancel(showCancel))
            close();
    }
    public boolean saveOrCancel(boolean showCancel) {
        if (isDirty)
            switch (JOptionPane.showConfirmDialog
                    (frame,
                     resources.getString("Confirm.Close.Prompt"),
                     resources.getString("Confirm.Close.Title"),
                     showCancel ? JOptionPane.YES_NO_CANCEL_OPTION
                                : JOptionPane.YES_NO_OPTION)) {
            case JOptionPane.CLOSED_OPTION:
            case JOptionPane.CANCEL_OPTION:
                return false;                 // do nothing and abort.

            case JOptionPane.NO_OPTION:
                return true;

            case JOptionPane.YES_OPTION:
                save();                 // save changes.
            }
        return true;
    }

    private String getExpandedNodesKey(String taskListId) {
        return taskListId + EXPANDED_NODES_KEY_SUFFIX;
    }

    private Set getExpandedNodesPref(String taskListId) {
        String value = PreferencesUtils.getCLOB(preferences,
                getExpandedNodesKey(taskListId), "");
        String[] nodesArray = value.split(EXPANDED_NODES_DELIMITER);
        Set nodesToExpand = new HashSet(Arrays.asList(nodesArray));

        return nodesToExpand;
    }

    private void setExpandedNodesPref(String taskListId, Set value) {
        PreferencesUtils.putCLOB(preferences, getExpandedNodesKey(taskListId),
                StringUtils.join(value, EXPANDED_NODES_DELIMITER));
    }

    private void saveExpandedNodesPref(String taskListId) {
        if (treeTable == null || treeTable.getTree() == null
                || treeTable.getTree().getModel() == null)
            return;

        Set expandedNodes = new HashSet();
        getExpandedNodes(expandedNodes, (EVTask) treeTable.getTree().getModel().getRoot());
        setExpandedNodesPref(taskListId, expandedNodes);
    }

    private void getExpandedNodes(Set expandedNodes, EVTask taskNode) {
        boolean isVisible = treeTable.getTree().isVisible(new TreePath(taskNode.getPath()));
        boolean isExpanded = treeTable.getTree().isExpanded(new TreePath(taskNode.getPath()));

        if (isVisible && isExpanded) {
            List ids = taskNode.getTaskIDs();
            if (null == ids || ids.isEmpty())
                expandedNodes.add(taskNode.getFullName());
            else
                expandedNodes.addAll(ids);

            for (int i = 0; i < taskNode.getNumChildren(); i++)
                getExpandedNodes(expandedNodes, taskNode.getChild(i));
        }
    }

    private void maybeReadExpandedNodesPref(String taskListId) {
        if (shouldUseExpandedNodesPref())
            readExpandedNodesPref(taskListId);
    }

    private void readExpandedNodesPref(String taskListId) {
        Set nodesToExpand = getExpandedNodesPref(taskListId);
        expandNodes(nodesToExpand, (EVTask) treeTable.getTree().getModel().getRoot());
    }

    private void expandNodes(Set nodesToExpand, EVTask taskNode) {
        List ids = taskNode.getTaskIDs();
        if ((null != ids && nodesToExpand.removeAll(ids)) ||
                nodesToExpand.remove(taskNode.getFullName())) {
            treeTable.getTree().makeVisible(new TreePath(taskNode.getPath()));
            treeTable.getTree().expandPath(new TreePath(taskNode.getPath()));
        }
        for (int i = 0; i < taskNode.getNumChildren(); i++) {
            expandNodes(nodesToExpand, taskNode.getChild(i));
        }
    }

    private boolean shouldUseExpandedNodesPref() {
        if (isRollup()) {
            return isMergedView() || Settings.isPersonalMode();
        } else {
            return !isFlatView();
        }
    }

}
