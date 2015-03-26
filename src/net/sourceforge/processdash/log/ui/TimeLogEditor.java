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
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.EventHandler;
import java.io.IOException;
import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;
import javax.swing.TransferHandler;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.text.JTextComponent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.toedter.calendar.JDateChooser;

import net.sourceforge.processdash.ApplicationEventListener;
import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.Filter;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.hier.ui.PropTreeModel;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.log.time.ChangeFlaggedTimeLogEntry;
import net.sourceforge.processdash.log.time.CommittableModifiableTimeLog;
import net.sourceforge.processdash.log.time.DashboardTimeLog;
import net.sourceforge.processdash.log.time.ModifiableTimeLog;
import net.sourceforge.processdash.log.time.TimeLog;
import net.sourceforge.processdash.log.time.TimeLogEntry;
import net.sourceforge.processdash.log.time.TimeLogEvent;
import net.sourceforge.processdash.log.time.TimeLogListener;
import net.sourceforge.processdash.log.time.TimeLogTableModel;
import net.sourceforge.processdash.log.time.TimeLoggingApprover;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.ui.lib.DeferredSelectAllExecutor;
import net.sourceforge.processdash.ui.lib.DropDownButton;
import net.sourceforge.processdash.ui.lib.JDateTimeChooserCellEditor;
import net.sourceforge.processdash.ui.lib.TableUtils;
import net.sourceforge.processdash.ui.macosx.MacGUIUtils;
import net.sourceforge.processdash.util.EnumerIterator;
import net.sourceforge.processdash.util.FallbackObjectFactory;
import net.sourceforge.processdash.util.FormatUtil;

public class TimeLogEditor extends Object implements TreeSelectionListener,
        DashHierarchy.Listener, TableModelListener, TimeLogListener,
        ApplicationEventListener {

    /** Class Attributes */
    protected JFrame frame;

    protected JTree tree;

    protected PropTreeModel treeModel;

    protected DashHierarchy useProps;

    protected JTable table;

    protected JSplitPane splitPane;

    protected DashboardTimeLog dashTimeLog;

    protected CommittableModifiableTimeLog timeLog;

    protected TimeLogTableModel tableModel;

    protected TimeLoggingApprover approver;

    protected boolean forceReadOnly;

    JDateChooser toDate = null;

    JDateChooser fromDate = null;

    JButton revertButton = null;

    JButton saveButton = null;

    JButton addButton = null;

    JComboBox formatChoice = null;

    TimeCardDialog timeCardDialog = null;

    private Timer recalcTimer;

    static final long HOUR_IN_MILLIS = 60 * 60 * 1000;
    static final long DAY_IN_MILLIS = 24 * HOUR_IN_MILLIS;

    /**  The formats of the string used to validate dates entered manually in
          a JDateTimeChooserCellEditor field */
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd h:mm:ss aa";
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    /** The size of the JDateChoosers in the "Filter" section */
    private static final int DATE_CHOOSER_WIDTH = 120;

    boolean tableContainsRows = false;

    boolean selectedNodeLoggingAllowed = false;

    static Resources resources = Resources.getDashBundle("Time");


    // constructor
    public TimeLogEditor(TimeLog timeLog,
            DashHierarchy props, TimeLoggingApprover approver,
            PropertyKey currentPhase) {
        if (timeLog instanceof DashboardTimeLog)
            this.dashTimeLog = (DashboardTimeLog) timeLog;
        else
            this.dashTimeLog = null;

        this.useProps = props;
        this.useProps.addHierarchyListener(this);
        this.approver = approver;
        this.forceReadOnly = !(timeLog instanceof ModifiableTimeLog);

        constructUserInterface();
        setSelectedNode(currentPhase);
        if (this.dashTimeLog == null)
            setTimeLog(timeLog);
        show();
    }

    public void close() {
        confirmClose(true);
    }

    public void confirmClose(boolean showCancel) {
        if (saveRevertOrCancel(showCancel)) {
            saveCustomDimensions();
            frame.setVisible(false); // close the time log window.
            if (timeCardDialog != null)
                timeCardDialog.hide();
            setTimeLog(null);
        }
    }

    public void handleApplicationEvent(ActionEvent e) {
        if (APP_EVENT_SAVE_ALL_DATA.equals(e.getActionCommand())) {
            saveRevertOrCancel(false);
            saveCustomDimensions();
        }
    }

    public boolean saveRevertOrCancel(boolean showCancel) {
        if (isDirty() && Settings.isReadWrite() && !forceReadOnly) {
            int optionType = showCancel ? JOptionPane.YES_NO_CANCEL_OPTION
                    : JOptionPane.YES_NO_OPTION;
            int userChoice = JOptionPane.showConfirmDialog(frame,
                    getResource("Confirm_Close_Prompt"),
                    getResource("Confirm_Close_Title"), optionType);
            switch (userChoice) {
            case JOptionPane.CLOSED_OPTION:
            case JOptionPane.CANCEL_OPTION:
                return false; // do nothing and abort.

            case JOptionPane.YES_OPTION:
                save(); // save changes.
                break;

            case JOptionPane.NO_OPTION:
                reload(); // revert changes.
            }
        }
        return true;
    }

    protected boolean isDirty() {
        return timeLog != null && timeLog.hasUncommittedData();
    }

    public void narrowToPath(String path) {
        PropertyKey key = useProps.findExistingKey(path);
        if (key == null)
            throw new IllegalArgumentException("Path not found");

        treeModel.setRootKey(key);
        treeModel.reload(useProps);
        tree.setRootVisible(!PropertyKey.ROOT.equals(key));
        expandRoot();
    }

    public void setTitle(String title) {
        frame.setTitle(title);
    }

    public void tableChanged(TableModelEvent evt) {
        boolean isDirty = isDirty();
        saveButton.setEnabled(isDirty);
        revertButton.setEnabled(isDirty);
        MacGUIUtils.setDirty(frame, isDirty);

        tableContainsRows = (tableModel.getRowCount() > 0);
        addButton.setEnabled(tableContainsRows || selectedNodeLoggingAllowed);

        recalcTimer.restart();
    }

    public void timeLogChanged(TimeLogEvent e) {
        recalcTimer.restart();
    }

    void cancelPostedChanges() {
        timeLog.clearUncommittedData();
    }

    private static final String[] TIME_FORMAT_KEY_NAMES = { "Hours_Minutes",
            "Hours", "Percent_Parent", "Percent_Total" };

    private static final int FORMAT_HOURS_MINUTES = 0;

    private static final int FORMAT_HOURS = 1;

    private static final int FORMAT_PERCENT_PARENT = 2;

    private static final int FORMAT_PERCENT_TOTAL = 3;

    protected int timeFormat = FORMAT_HOURS_MINUTES;

    private static NumberFormat decimalFormatter = NumberFormat
            .getNumberInstance();
    static {
        decimalFormatter.setMaximumFractionDigits(2);
    }

    protected String formatTime(long t, long parentTime, long totalTime) {
        switch (timeFormat) {
        case FORMAT_HOURS:
            return FormatUtil.formatNumber((double) t / 60.0);

        case FORMAT_PERCENT_PARENT:
            return formatPercent(t, parentTime);

        case FORMAT_PERCENT_TOTAL:
            return formatPercent(t, totalTime);

        case FORMAT_HOURS_MINUTES:
        default:
            return FormatUtil.formatTime(t);
        }
    }

    private String formatPercent(double a, double b) {
        double result = 0;
        if (b != 0)
            result = a / b;
        return FormatUtil.formatPercent(result);
    }

    private static final String DIMENSION_SETTING_NAME = "timelog.dimensions";

    private int frameWidth, frameHeight, dividerLocation = -1;

    private void loadCustomDimensions() {
        String setting = Settings.getVal(DIMENSION_SETTING_NAME);
        if (setting != null && setting.length() > 0)
            try {
                StringTokenizer tok = new StringTokenizer(setting, ",");
                frameWidth = Integer.parseInt(tok.nextToken());
                frameHeight = Integer.parseInt(tok.nextToken());
                dividerLocation = Integer.parseInt(tok.nextToken());
            } catch (Exception e) {
            }
        if (dividerLocation == -1) {
            frameWidth = 800;
            frameHeight = 400;
            dividerLocation = 300;
        }
    }

    private void saveCustomDimensions() {
        frameWidth = frame.getSize().width;
        frameHeight = frame.getSize().height;
        dividerLocation = splitPane.getDividerLocation();
        InternalSettings.set(DIMENSION_SETTING_NAME, frameWidth + ","
                + frameHeight + "," + dividerLocation);
    }

    public void setTimes() {
        Date fd = getFromDate();
        Date td = getToDate(true);

        Map nodeTimes = new HashMap();
        long totalTime = collectTime(treeModel.getRoot(), getTimes(fd, td),
                nodeTimes);
        setTimes(treeModel.getRoot(), nodeTimes, totalTime, totalTime);

        tree.repaint(tree.getVisibleRect());
        if (timeCardDialog != null)
            timeCardDialog.recalc();
    }

    private SortedMap getTimes(Date fd, Date td) {
        SortedMap result = new TreeMap();
        try {
            for (Iterator i = timeLog.filter(null, fd, td); i.hasNext();) {
                TimeLogEntry tle = (TimeLogEntry) i.next();
                String path = tle.getPath();
                long[] t = (long[]) result.get(path);
                if (t == null) {
                    t = new long[] { 0L };
                    result.put(path, t);
                }
                t[0] += tle.getElapsedTime();
            }
        } catch (Exception e) {
        }
        return result;
    }

    private long collectTime(Object node, SortedMap timesIn, Map timesOut) {
        long t = 0; // time for this node

        // recursively compute total time for each
        // child and add total time for this node.
        for (int i = 0; i < treeModel.getChildCount(node); i++) {
            t += collectTime(treeModel.getChild(node, i), timesIn, timesOut);
        }

        // fetch and add time spent in this node
        Object[] treePath = treeModel.getPathToRoot((TreeNode) node);
        PropertyKey key = treeModel.getPropKey(useProps, treePath);
        if (key != null) {
            String pathPrefix = key.path();
            // For efficiency, we only search through the portion of the
            // SortedMap which could contain paths matching our prefix.
            // "Matching our prefix" means that it exactly equals our
            // prefix, or it begins with our prefix followed by a slash.
            // The character after the slash character is the zero '0'.
            // So any string that is greater than that prefix can't match.
            String endPrefix = pathPrefix + '0';
            for (Iterator i = timesIn.subMap(pathPrefix, endPrefix).entrySet()
                    .iterator(); i.hasNext();) {
                Map.Entry e = (Map.Entry) i.next();
                String onePath = (String) e.getKey();
                if (Filter.pathMatches(onePath, pathPrefix)) {
                    long[] l = (long[]) e.getValue();
                    if (l != null)
                        t += l[0];
                    i.remove();
                }
            }
        }

        timesOut.put(node, new Long(t));

        return t;
    }

    private void setTimes(Object node, Map times, long parentTime,
            long totalTime) {

        long t = ((Long) times.get(node)).longValue();

        // display the time next to the node name
        // in the tree display
        String s = (String) ((DefaultMutableTreeNode) node).getUserObject();
        int index = s.lastIndexOf("=");
        if (index > 0)
            s = s.substring(0, index - 1);
        s = s + " = " + formatTime(t, parentTime, totalTime);
        ((DefaultMutableTreeNode) node).setUserObject(s);
        treeModel.nodeChanged((TreeNode) node);

        for (int i = 0; i < treeModel.getChildCount(node); i++) {
            setTimes(treeModel.getChild(node, i), times, t, totalTime);
        }
    }

    public void applyFilter() {
        String path = null;
        DefaultMutableTreeNode selected = getSelectedNode();
        if (selected != null) {
            path = treeModel.getPropKey(useProps, selected.getPath()).path();
        }
        Date from = getFromDate();
        Date to = getToDate(true);

        saveEditInProgress();

        // apply the filter to the table
        tableModel.setFilter(path, from, to);
        tableContainsRows = tableModel.getRowCount() > 0;

        addButton.setEnabled(tableContainsRows || selectedNodeLoggingAllowed);
    }

    private void setFilter(Date from, Date to) {
        fromDate.setDate(from);
        toDate.setDate(to);
        applyFilter();
    }

    public void filterToday() {
        Date now = new Date();
        setFilter(now, now);
    }

    public void filterThisWeek() {
        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.set(Calendar.DAY_OF_WEEK, getWeekFilterStartDay(cal));
        if (cal.getTime().after(now))
            cal.add(Calendar.DATE, -7);
        Date from = cal.getTime();
        cal.add(Calendar.DATE, 6);
        Date to = cal.getTime();
        setFilter(from, to);
    }

    public void filterThisMonth() {
        filterThisMonth(new Date());
    }

    public void filterThisMonth(Date when) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(when);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        Date from = cal.getTime();
        cal.set(Calendar.DAY_OF_MONTH, cal
                .getActualMaximum(Calendar.DAY_OF_MONTH));
        Date to = cal.getTime();
        setFilter(from, to);
    }

    public void scrollFilterForward() {
        Date from = getFromDate();
        Date to = getToDate(false);

        if (from == null || to == null) {
            from = truncDate(from, 1);
            to = truncDate(to, 1);
            setFilter(from, to);
            return;
        }

        int diff = countDaysInSpan(from, to);
        if (diff == 28 || diff == 29 || diff == 30 || diff == 31) {
            Date next = new Date(to.getTime() + DAY_IN_MILLIS * 2);
            filterThisMonth(next);
        } else if (diff > 0) {
            from = truncDate(to, 1);
            to = truncDate(to, diff);
            setFilter(from, to);
        }
    }

    public void scrollFilterBackward() {
        Date from = getFromDate();
        Date to = getToDate(false);

        if (from == null || to == null) {
            from = truncDate(from, -1);
            to = truncDate(to, -1);
            setFilter(from, to);
            return;
        }

        int diff = countDaysInSpan(from, to);
        if (diff == 28 || diff == 29 || diff == 30 || diff == 31) {
            Date prev = new Date(from.getTime() - DAY_IN_MILLIS * 2);
            filterThisMonth(prev);
        } else if (diff > 0) {
            to = truncDate(from, -1);
            from = truncDate(from, -diff);
            setFilter(from, to);
        }
    }

    private int countDaysInSpan(Date from, Date to) {
        // measure the difference between the two times
        long diff = to.getTime() - from.getTime();
        // add a couple of hours to avoid daylight savings boundary issues
        diff += HOUR_IN_MILLIS * 2;
        // calculate the number of days, rounding down
        long days = diff / DAY_IN_MILLIS;
        // add a day, since our timespans are inclusive
        return (int) days + 1;
    }

    private Date getFromDate() {
        return truncDate(fromDate.getDate(), 0);
    }

    private Date getToDate(boolean endOfDay) {
        return truncDate(toDate.getDate(), endOfDay ? 1 : 0);
    }

    private Date truncDate(Date d, int addDays) {
        if (d == null)
            return null;

        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        if (addDays != 0)
            c.add(Calendar.DATE, addDays);
        return c.getTime();
    }

    public void clearFilter() {
        fromDate.setDate(null);
        toDate.setDate(null);
        applyFilter();
    }

    public void addRow() {
        int rowBasedOn = -1;
        String pathBasedOn = null;
        DefaultMutableTreeNode selected;
        PropertyKey key;

        // try to base new row on current tree selection
        if ((selected = getSelectedNode()) != null
                && (key = treeModel.getPropKey(useProps, selected.getPath())) != null
                && timeLoggingAllowed(key))
            pathBasedOn = key.path();

        // else try to base new row on current editing row
        else if ((rowBasedOn = table.getEditingRow()) != -1)
            saveEditInProgress();

        // else try to base new row on the selected table row
        else if ((rowBasedOn = table.getSelectedRow()) != -1)
            ; // nothing to do here .. just drop out of "else if" tree

        else
            // else try to base new row on last row of table
            rowBasedOn = tableModel.getRowCount() - 1;

        if (rowBasedOn != -1)
            pathBasedOn = (String) tableModel.getValueAt(rowBasedOn,
                TimeLogTableModel.COL_PATH);

        if (pathBasedOn != null)
            tableModel.addRow(pathBasedOn);
        else
            tableModel.addRow("/");

        tableContainsRows = true;
        addButton.setEnabled(true);

        int newRow = tableModel.getRowCount() - 1;
        int col = table.convertColumnIndexToView(TimeLogTableModel.COL_ELAPSED);
        table.setRowSelectionInterval(newRow, newRow);
        table.setColumnSelectionInterval(col, col);
        table.scrollRectToVisible(table.getCellRect(newRow, col, true));
        table.editCellAt(newRow, col);
        table.getEditorComponent().requestFocusInWindow();
    }

    public void deleteSelectedRow() {
        int editingRow = table.getEditingRow();
        if (editingRow != -1)
            table.getCellEditor().cancelCellEditing();

        int[] rows = table.getSelectedRows();
        if (rows != null && rows.length > 0) {
            Arrays.sort(rows);
            for (int i = rows.length; i-- > 0;)
                tableModel.deleteRow(rows[i]);
        } else if (editingRow != -1) {
            tableModel.deleteRow(editingRow);
        }

    }


    public void summarizeWarning() {
        saveEditInProgress();
        int userChoice = JOptionPane.showConfirmDialog(frame,
                resources.getStrings("Summarization_Warning_Message"),
                getResource("Summarization_Warning_Title"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (userChoice == JOptionPane.OK_OPTION)
            tableModel.summarizeRows();
    }

    public void showTimeCard() {
        if (timeCardDialog != null)
            timeCardDialog.show();
        else
            timeCardDialog = new TimeCardDialog(useProps, timeLog);
    }

    public void save() {
        saveEditInProgress();
        timeLog.commitData();
    }

    private void saveEditInProgress() {
        // if editing, stop edits
        if (table.isEditing())
            table.getCellEditor().stopCellEditing();
    }

    public void hierarchyChanged(DashHierarchy.Event e) {
        reloadAll(null);
    }

    public void reloadAll(DashHierarchy newProps) {
        if (newProps != null)
            useProps.copy(newProps);
        treeModel.reload(useProps);

        checkSelectedNodeLoggingAllowed();
        applyFilter();
    }

    public void reload() {
        timeLog.clearUncommittedData();
        applyFilter();
    }

    public void show() {
        if (frame.isShowing()) {
            frame.setExtendedState(JFrame.NORMAL);
            frame.toFront();
        } else {
            if (dashTimeLog != null)
                setTimeLog(dashTimeLog.getDeferredTimeLogModifications());
            frame.setVisible(true);
        }
    }

    protected void setTimeLog(TimeLog newTimeLog) {
        if (newTimeLog != timeLog) {
            if (timeLog != null)
                timeLog.removeTimeLogListener(this);

            if (newTimeLog == null)
                timeLog = null;
            else if (newTimeLog instanceof CommittableModifiableTimeLog)
                timeLog = (CommittableModifiableTimeLog) newTimeLog;
            else {
                timeLog = new CMTLWrapper(newTimeLog);
                tableModel.setEditable(false);
            }

            if (timeLog != null)
                timeLog.addTimeLogListener(this);
            tableModel.setTimeLog(timeLog);
        }
    }

    // make sure root is expanded
    public void expandRoot() {
        tree.expandRow(0);
    }

    // Returns the TreeNode instance that is selected in the tree.
    // If nothing is selected, null is returned.
    protected DefaultMutableTreeNode getSelectedNode() {
        TreePath selPath = tree.getSelectionPath();

        if (selPath != null)
            return (DefaultMutableTreeNode) selPath.getLastPathComponent();
        return null;
    }

    protected String getPathIfLoggingAllowed(TreePath path) {
        if (path == null)
            return null;

        PropertyKey key = treeModel.getPropKey(useProps, path.getPath());
        return (timeLoggingAllowed(key) ? key.path() : null);
    }

    /**
     * The next method implement the TreeSelectionListener interface to deal
     * with changes to the tree selection.
     */
    public void valueChanged(TreeSelectionEvent e) {
        TreePath tp = e.getNewLeadSelectionPath();

        if (tp == null) { // deselection
            tree.clearSelection();
            applyFilter();
            return;
        }
        Object[] path = tp.getPath();
        PropertyKey key = treeModel.getPropKey(useProps, path);
        checkSelectedNodeLoggingAllowed(key);
        applyFilter();
    }

    private void checkSelectedNodeLoggingAllowed() {
        TreePath selPath = tree.getSelectionPath();
        PropertyKey key = null;
        if (selPath != null) {
            Object[] path = selPath.getPath();
            key = treeModel.getPropKey(useProps, path);
        }
        checkSelectedNodeLoggingAllowed(key);
    }

    private void checkSelectedNodeLoggingAllowed(PropertyKey key) {
        selectedNodeLoggingAllowed = timeLoggingAllowed(key);
    }

    private boolean timeLoggingAllowed(PropertyKey key) {
        return key != null && approver != null
            && approver.isTimeLoggingAllowed(key.path());
    }

    /**
     * Expand the hierarchy so that the given node is visible and selected.
     */
    public void setSelectedNode(PropertyKey path) {
        if (path == null)
            return;
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) treeModel
                .getNodeForKey(useProps, path);
        if (node == null)
            return;

        TreePath tp = new TreePath(node.getPath());
        tree.clearSelection();
        tree.scrollPathToVisible(tp);
        tree.addSelectionPath(tp);
    }

    public void updateTimeFormat() {
        timeFormat = formatChoice.getSelectedIndex();
        setTimes();
    }

    /*
     * methods for constructing the user interface
     */

    private void constructUserInterface() {
        loadCustomDimensions();

        frame = new JFrame(getResource("Time_Log_Editor_Window_Title"));
        DashboardIconFactory.setWindowIcon(frame);
        frame.setSize(new Dimension(frameWidth, frameHeight));
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                confirmClose(true);
            }});
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        PCSH.enableHelpKey(frame, "UsingTimeLogEditor");

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                constructTreePanel(), constructEditPanel());
        splitPane.setDividerLocation(dividerLocation);

        // Setup drag-and-drop support on Java 6 and above
        new FallbackObjectFactory<TimeLogEditorDragDropSetup>(
                TimeLogEditorDragDropSetup.class)
                .add("TimeLogEditorDragDropSetupJava6") //
                .get(true).setupEditorForDragDrop(this);

        Container panel = frame.getContentPane();
        panel.setLayout(new BorderLayout());
        panel.add(BorderLayout.NORTH, constructFilterPanel());
        panel.add(BorderLayout.CENTER, splitPane);
        panel.add(BorderLayout.SOUTH, constructControlPanel());

        createRecalcTimer();
    }

    private void createRecalcTimer() {
        recalcTimer = new Timer(1000, (ActionListener) EventHandler.create(
                ActionListener.class, this, "setTimes"));
        recalcTimer.setRepeats(false);
        recalcTimer.setInitialDelay(100);
    }

    private JScrollPane constructTreePanel() {
        /* Create the JTreeModel. */
        treeModel = new PropTreeModel(new DefaultMutableTreeNode("root"), null);

        /* Create the tree. */
        tree = new JTree(treeModel);
        treeModel.fill(useProps);
        tree.expandRow(0);
        tree.setShowsRootHandles(true);
        tree.setEditable(false);
        tree.addTreeSelectionListener(this);
        tree.setRootVisible(false);
        tree.setRowHeight(-1); // Make tree ask for the height of each row.

        /* Put the Tree in a scroller. */
        JScrollPane scrollPane = new JScrollPane(tree,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(300, 300));
        return scrollPane;
    }

    private JPanel constructEditPanel() {
        JPanel retPanel = new JPanel(false);

        retPanel.setLayout(new BorderLayout());
        tableModel = new TimeLogTableModel();
        if (Settings.isReadOnly() || forceReadOnly)
            tableModel.setEditable(false);
        tableModel.setApprover(approver);
        tableModel.addTableModelListener(this);
        table = new TimeLogJTable(tableModel);
        TableUtils.configureTable(table, TimeLogTableModel.COLUMN_WIDTHS,
                TimeLogTableModel.COLUMN_TOOLTIPS);
        TableColumn startTimeCol = table.getColumnModel().getColumn(TimeLogTableModel.COL_START_TIME);
        startTimeCol.setCellEditor(new JDateTimeChooserCellEditor(Settings
                .getVal("timelog.dateTimeEditFormat", DATE_TIME_FORMAT)));
        retPanel.add("Center", new JScrollPane(table));

        JPanel btnPanel = new JPanel(false);
        addButton = createButton(btnPanel, "Add", "addRow");
        createButton(btnPanel, "Delete", "deleteSelectedRow");
        createButton(btnPanel, "Summarize_Button", "summarizeWarning");

        if (Settings.isReadWrite() && !forceReadOnly)
            retPanel.add("South", btnPanel);

        return retPanel;
    }

    private JPanel constructFilterPanel() {
        JPanel retPanel = new JPanel(false);
        retPanel.setLayout(new BoxLayout(retPanel, BoxLayout.X_AXIS));
        retPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        DropDownButton button;
        JLabel label;

        retPanel.add(Box.createHorizontalGlue());

        addTimeFormatControl(retPanel);
        retPanel.add(new JLabel("          "));

        label = new JLabel(getResource("Filter.Label") + " ");
        retPanel.add(label);
        retPanel.add(Box.createHorizontalStrut(5));

        addScrollButton(retPanel, "Filter.Scroll_Backward",
                "scrollFilterBackward");
        fromDate = addDateField(retPanel, "Filter.From");
        toDate = addDateField(retPanel, "Filter.To");
        addScrollButton(retPanel, "Filter.Scroll_Forward",
                "scrollFilterForward");

        button = new DropDownButton(getResource("Filter.Apply"));
        button.setRunFirstMenuOption(false);
        button.getButton().addActionListener(
                createActionListener("applyFilter"));

        JMenu menu = button.getMenu();
        addFilterMenuItem(menu, "Filter.Today", "filterToday");
        addFilterMenuItem(menu, "Filter.Week", "filterThisWeek");
        menu.add(createWeekFilterStartDaySubmenu());
        addFilterMenuItem(menu, "Filter.Month", "filterThisMonth");
        menu.addSeparator();
        addFilterMenuItem(menu, "Filter.Remove", "clearFilter");
        retPanel.add(button);

        retPanel.add(Box.createHorizontalGlue());

        return retPanel;
    }

    private JPanel constructControlPanel() {
        JPanel retPanel = new JPanel(false);

        createButton(retPanel, "Time_Card_View_Button", "showTimeCard");
        retPanel.add(Box.createHorizontalStrut(100));

        revertButton = createButton(retPanel, "Revert", "reload", true);
        saveButton = createButton(retPanel, "Save", "save", true);
        createButton(retPanel, "Close", "close");

        return retPanel;
    }

    private void addTimeFormatControl(JPanel retPanel) {
        JLabel label = new JLabel(getResource("Time_Format.Label")
                + " ");
        retPanel.add(label);
        retPanel.add(Box.createHorizontalStrut(5));
        String[] formatChoices = resources.getStrings("Time_Format.",
                TIME_FORMAT_KEY_NAMES);
        formatChoice = new JComboBox(formatChoices);
        formatChoice
                .addActionListener(createActionListener("updateTimeFormat"));
        label.setLabelFor(formatChoice);
        retPanel.add(formatChoice);
        retPanel.add(Box.createHorizontalStrut(5));
    }

    private void addScrollButton(JPanel retPanel, String resPrefix,
            String action) {
        JButton btn = new JButton(getResource(resPrefix + "_Button"));
        btn.setMargin(new Insets(0, 2, 0, 2));
        btn.setToolTipText(getResource(resPrefix + "_Tooltip"));
        btn.addActionListener(createActionListener(action));
        retPanel.add(btn);
        retPanel.add(Box.createHorizontalStrut(5));
    }

    private JDateChooser addDateField(JPanel retPanel, String labelKey) {
        JLabel label = new JLabel(getResource(labelKey) + " ");
        retPanel.add(label);
        retPanel.add(Box.createHorizontalStrut(5));

        JDateChooser result = new JDateChooser(null, DATE_FORMAT);
        result.setPreferredSize(new Dimension(DATE_CHOOSER_WIDTH,
                                              result.getPreferredSize().height));
        label.setLabelFor(result);
        result.setMaximumSize(result.getPreferredSize());
        retPanel.add(result);
        retPanel.add(Box.createHorizontalStrut(5));

        return result;
    }

    private void addFilterMenuItem(JMenu menu, String resKey, String action) {
        JMenuItem menuItem = menu.add(getResource(resKey));
        menuItem.addActionListener(createActionListener(action));
    }

    private static final String WEEKDAY_SETTING_NAME = "timelog.weekFilterStartDay";

    private JMenuItem createWeekFilterStartDaySubmenu() {
        int filterStartDay = getWeekFilterStartDay(Calendar.getInstance());
        String[] dayNames = new DateFormatSymbols().getWeekdays();
        JMenu submenu = new JMenu();
        for (int day = 1; day <= 7; day++)
            submenu.add(new WeekFilterStartDayOption(submenu, dayNames, day,
                    filterStartDay));
        submenu.setFont(submenu.getFont().deriveFont(
            submenu.getFont().getSize2D() * 0.8f));
        return submenu;
    }

    private class WeekFilterStartDayOption extends AbstractAction {
        JMenu parentMenu;
        int day;

        public WeekFilterStartDayOption(JMenu parentMenu, String[] dayNames,
                int day, int chosenDay) {
            this.parentMenu = parentMenu;
            this.day = day;
            int endDay = day - 1;
            if (endDay == 0)
                endDay = 7;
            String text = resources.format("Filter.Weekday_Span_FMT",
                dayNames[day], dayNames[endDay]);
            putValue(NAME, text);
            if (day == chosenDay)
                updateParent();
        }
        private void updateParent() {
            parentMenu.setText("     (" + getValue(NAME) + ")");
        }
        public void actionPerformed(ActionEvent e) {
            InternalSettings.set(WEEKDAY_SETTING_NAME, Integer.toString(day));
            updateParent();
            filterThisWeek();
        }
    }

    private int getWeekFilterStartDay(Calendar cal) {
        int result = Settings.getInt(WEEKDAY_SETTING_NAME,
            cal.getFirstDayOfWeek());
        result = Math.max(result, 1);
        result = Math.min(result, 7);
        return result;
    }

    private ActionListener createActionListener(String methodName) {
        return (ActionListener) EventHandler.create(ActionListener.class, this,
                methodName);
    }


    private JButton createButton(Container p, String resKey, String action) {
        return createButton(p, resKey, action, false);
    }
    private JButton createButton(Container p, String resKey, String action,
            boolean hideIfReadOnly) {
        JButton result = new JButton(getResource(resKey));
        result.addActionListener(createActionListener(action));
        if (hideIfReadOnly == false
                || (Settings.isReadWrite() && !forceReadOnly))
            p.add(result);
        return result;
    }

    private String getResource(String key) {
        return resources.getString(key);
    }

    private class TimeLogJTable extends JTable {

        public TimeLogJTable(TimeLogTableModel tableModel) {
            super(tableModel);
            setTransferHandler(new TransferSupport());
            MacGUIUtils.tweakTable(this);
        }

        public boolean editCellAt(int row, int column, EventObject e) {
            boolean result = super.editCellAt(row, column, e);

            if (result == true
                    && e instanceof MouseEvent
                    && shouldSelectAll(column))
                DeferredSelectAllExecutor.register(getEditorComponent());

            return result;
        }

        public Component prepareEditor(TableCellEditor tce, int row, int col) {
            Component result = super.prepareEditor(tce, row, col);
            if (result instanceof JTextComponent && shouldSelectAll(col))
                ((JTextComponent) result).selectAll();
            return result;
        }

        private boolean shouldSelectAll(int viewColumn) {
            int column = convertColumnIndexToModel(viewColumn);
            return (column == TimeLogTableModel.COL_ELAPSED
                    || column == TimeLogTableModel.COL_INTERRUPT);
        }

        private class TransferSupport extends TransferHandler {

            public int getSourceActions(JComponent c) {
                return COPY_OR_MOVE;
            }

            protected Transferable createTransferable(JComponent c) {
                return tableModel.getTransferrable(getSelectedRows());
            }
        }

    }

    /**
     * The code for the time log editor generally targets modifiable time logs.
     * However, 95% of it is useful for read-only analysis of unmodifiable time
     * logs as well. In fact, when a small number of boolean flags are toggled,
     * the 5% of the code that handles modification is no longer be triggered by
     * user gestures. Rather than cluttering that unreachable code with
     * conditional branches to handle readOnly scenarios that will never happen,
     * this class wraps an unmodifiable time log with the expected interfaces,
     * implementing modification calls as no-ops.
     */
    private class CMTLWrapper implements CommittableModifiableTimeLog {
        TimeLog tl;

        public CMTLWrapper(TimeLog timeLog) {
            this.tl = timeLog;
        }
        public EnumerIterator filter(String path, Date from, Date to)
                throws IOException {
            return tl.filter(path, from, to);
        }
        public void addModification(ChangeFlaggedTimeLogEntry tle) {}
        public void addModifications(Iterator iter) {}
        public void addTimeLogListener(TimeLogListener l) {}
        public void removeTimeLogListener(TimeLogListener l) {}
        public void clearUncommittedData() {}
        public void commitData() {}
        public long getNextID() { return -1; }
        public boolean hasUncommittedData() { return false; }
    }

}
