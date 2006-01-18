// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003-2005 Software Process Dashboard Initiative
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
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.log.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.EventHandler;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.JTextComponent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.hier.ui.PropTreeModel;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.log.ChangeFlagged;
import net.sourceforge.processdash.log.time.CommittableModifiableTimeLog;
import net.sourceforge.processdash.log.time.DashboardTimeLog;
import net.sourceforge.processdash.log.time.TimeLogEntry;
import net.sourceforge.processdash.log.time.TimeLogEvent;
import net.sourceforge.processdash.log.time.TimeLogListener;
import net.sourceforge.processdash.log.time.TimeLogTableModel;
import net.sourceforge.processdash.log.time.TimeLoggingApprover;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.ui.lib.DropDownButton;
import net.sourceforge.processdash.ui.lib.TableUtils;
import net.sourceforge.processdash.util.FormatUtil;

public class TimeLogEditor extends Object implements TreeSelectionListener,
        DashHierarchy.Listener, TableModelListener, TimeLogListener {

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

    JTextField toDate = null;

    JTextField fromDate = null;

    JButton revertButton = null;

    JButton saveButton = null;

    JButton addButton = null;

    JComboBox formatChoice = null;

    TimeCardDialog timeCardDialog = null;

    private Timer recalcTimer;

    static final long DAY_IN_MILLIS = 24 * 60 * 60 * 1000;

    boolean tableContainsRows = false;

    boolean selectedNodeLoggingAllowed = false;

    Resources resources = Resources.getDashBundle("Time");


    // constructor
    public TimeLogEditor(DashboardTimeLog timeLog,
            DashHierarchy props, TimeLoggingApprover approver,
            PropertyKey currentPhase) {
        this.dashTimeLog = timeLog;
        this.useProps = props;
        this.useProps.addHierarchyListener(this);
        this.approver = approver;

        constructUserInterface();
        setSelectedNode(currentPhase);
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

    public boolean saveRevertOrCancel(boolean showCancel) {
        if (isDirty()) {
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

    public void tableChanged(TableModelEvent evt) {
        saveButton.setEnabled(isDirty());
        revertButton.setEnabled(isDirty());

        tableContainsRows = (tableModel.getRowCount() > 0);
        addButton.setEnabled(tableContainsRows || selectedNodeLoggingAllowed);

            recalcTimer.restart();
    }

        public void timeLogChanged(TimeLogEvent e) {
            recalcTimer.restart();
    }

    private boolean affectsElapsedTime(TimeLogEvent evt) {
        TimeLogEntry e = evt.getTimeLogEntry();
        if (e == null)
            return true;


        return false;
    }

    private boolean isDeletion(TimeLogEntry tle) {
        if (tle instanceof ChangeFlagged)
            return ((ChangeFlagged) tle).getChangeFlag() == ChangeFlagged.DELETED;
        else
            return false;
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
        Date fd = FormatUtil.parseDate(fromDate.getText());
        Date td = FormatUtil.parseDate(toDate.getText());
        if (td != null) // need to add a day so search is inclusive
            td = new Date(td.getTime() + DAY_IN_MILLIS);

        Hashtable nodeTimes = new Hashtable();
        long totalTime = collectTime(treeModel.getRoot(), getTimes(fd, td),
                nodeTimes);
        setTimes(treeModel.getRoot(), nodeTimes, totalTime, totalTime);

        tree.repaint(tree.getVisibleRect());
        if (timeCardDialog != null)
            timeCardDialog.recalc();
    }

    private Hashtable getTimes(Date fd, Date td) {
        Hashtable result = new Hashtable();
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

    private long collectTime(Object node, Hashtable timesIn, Hashtable timesOut) {
        long t = 0; // time for this node

        // recursively compute total time for each
        // child and add total time for this node.
        for (int i = 0; i < treeModel.getChildCount(node); i++) {
            t += collectTime(treeModel.getChild(node, i), timesIn, timesOut);
        }

        // fetch and add time spent in this node
        Object[] path = treeModel.getPathToRoot((TreeNode) node);
        long[] l = (long[]) timesIn.get(treeModel.getPropKey(useProps, path)
                .path());
        if (l != null)
            t += l[0];

        timesOut.put(node, new Long(t));

        return t;
    }

    private void setTimes(Object node, Hashtable times, long parentTime,
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
        Date from = FormatUtil.parseDate(fromDate.getText());
        Date to = FormatUtil.parseDate(toDate.getText());
        if (to != null) // need to add a day so search is inclusive
            to = new Date(to.getTime() + DAY_IN_MILLIS);

        saveEditInProgress();

        // apply the filter to the table
        tableModel.setFilter(path, from, to);
        tableContainsRows = tableModel.getRowCount() > 0;

        addButton.setEnabled(tableContainsRows || selectedNodeLoggingAllowed);
    }

    private void setFilter(Date from, Date to) {
        fromDate.setText(FormatUtil.formatDate(from));
        toDate.setText(FormatUtil.formatDate(to));
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
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
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
        Date fd = FormatUtil.parseDate(fromDate.getText());
        Date td = FormatUtil.parseDate(toDate.getText());
        if (fd == null)
            return;
        if (td == null)
            td = fd;

        long diff = 1 + (td.getTime() - fd.getTime()) / DAY_IN_MILLIS;
        if (diff == 28 || diff == 29 || diff == 30 || diff == 31) {
            fd = new Date(td.getTime() + DAY_IN_MILLIS * 2);
            filterThisMonth(fd);
        } else {
            fd = new Date(td.getTime() + DAY_IN_MILLIS);
            td = new Date(fd.getTime() + (diff - 1) * DAY_IN_MILLIS);
            setFilter(fd, td);
        }
    }

    public void scrollFilterBackward() {
        Date fd = FormatUtil.parseDate(fromDate.getText());
        Date td = FormatUtil.parseDate(toDate.getText());
        if (fd == null)
            return;
        if (td == null)
            td = fd;

        long diff = 1 + (td.getTime() - fd.getTime()) / DAY_IN_MILLIS;
        td = new Date(fd.getTime() - DAY_IN_MILLIS);
        if (diff == 28 || diff == 29 || diff == 30 || diff == 31) {
            filterThisMonth(td);
        } else {
            fd = new Date(td.getTime() - (diff - 1) * DAY_IN_MILLIS);
            setFilter(fd, td);
        }
    }

    public void clearFilter() {
        fromDate.setText("");
        toDate.setText("");
        applyFilter();
    }

    public void addRow() {
        int rowBasedOn = -1;
        String pathBasedOn = null;
        DefaultMutableTreeNode selected;
        PropertyKey key;

        // try to base new row on the selected table row
        if ((rowBasedOn = table.getSelectedRow()) != -1)
            ; // nothing to do here .. just drop out of "else if" tree

        // else try to base new row on current editing row
        else if ((rowBasedOn = table.getEditingRow()) != -1)
            saveEditInProgress();

        // else try to base new row on current tree selection
        else if ((selected = getSelectedNode()) != null
                && (key = treeModel.getPropKey(useProps, selected.getPath())) != null
                && timeLoggingAllowed(key))
            pathBasedOn = key.path();

        else
            // else try to base new row on last row of table
            rowBasedOn = tableModel.getRowCount() - 1;

        if (rowBasedOn != -1)
            tableModel.duplicateRow(rowBasedOn);
        else if (pathBasedOn != null)
            tableModel.addRow(pathBasedOn);
        else
            tableModel.addRow("/");

        tableContainsRows = true;
        addButton.setEnabled(true);
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
        if (frame.isShowing())
            frame.toFront();
        else {
            setTimeLog(dashTimeLog.getDeferredTimeLogModifications());
            frame.show();
        }
    }

    protected void setTimeLog(CommittableModifiableTimeLog newTimeLog) {
        if (newTimeLog != timeLog) {
                if (timeLog != null)
                        timeLog.removeTimeLogListener(this);

                timeLog = newTimeLog;

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
        return approver.isTimeLoggingAllowed(key.path());
    }

    // DateChangeAction responds to user input in a date field.
    class DateChangeAction implements ActionListener, FocusListener {
        JTextComponent widget;

        String text = null;

        Color background;

        public DateChangeAction(JTextComponent src) {
            widget = src;
            text = widget.getText();
            background = new Color(widget.getBackground().getRGB());
        }

        protected void validate() {
            String newText = widget.getText();

            Date d = FormatUtil.parseDate(newText);
            if (d == null) {
                if ((newText != null) && (newText.length() > 0))
                    widget.setBackground(Color.red);
                else {
                    widget.setBackground(background);
                    text = newText;
                    widget.setText(text);
                }
            } else {
                widget.setBackground(background);
                text = FormatUtil.formatDate(d);
                widget.setText(text);
            }
            widget.repaint(widget.getVisibleRect());
        }

        public void actionPerformed(ActionEvent e) { // hit return
            validate();
        }

        public void focusGained(FocusEvent e) {
            widget.selectAll();
        }

        public void focusLost(FocusEvent e) {
            validate();
        }

    } // End of TimeLogEditor.DateChangeAction

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
        frame.setIconImage(DashboardIconFactory.getWindowIconImage());
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
        JButton button;

        retPanel.setLayout(new BorderLayout());
        tableModel = new TimeLogTableModel();
        tableModel.addTableModelListener(this);
        table = new JTable(tableModel);
        TableUtils.configureTable(table, TimeLogTableModel.COLUMN_WIDTHS,
                TimeLogTableModel.COLUMN_TOOLTIPS);
        retPanel.add("Center", new JScrollPane(table));

        JPanel btnPanel = new JPanel(false);
        addButton = createButton(btnPanel, "Add", "addRow");
        createButton(btnPanel, "Delete", "deleteSelectedRow");
        createButton(btnPanel, "Summarize_Button", "summarizeWarning");

        retPanel.add("South", btnPanel);

        return retPanel;
    }

    private JPanel constructFilterPanel() {
        JPanel retPanel = new JPanel(false);
        retPanel.setLayout(new BoxLayout(retPanel, BoxLayout.X_AXIS));
        retPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        DropDownButton button;
        JButton btn;
        Insets insets = new Insets(0, 2, 0, 2);
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

        revertButton = createButton(retPanel, "Revert", "reload");
        saveButton = createButton(retPanel, "Save", "save");
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

    private JTextField addDateField(JPanel retPanel, String labelKey) {
        JLabel label = new JLabel(getResource(labelKey) + " ");
        retPanel.add(label);
        retPanel.add(Box.createHorizontalStrut(5));

        JTextField result = new JTextField("", 10);
        label.setLabelFor(result);
        result.setMaximumSize(result.getPreferredSize());
        DateChangeAction l = new DateChangeAction(result);
        result.addActionListener(l);
        result.addFocusListener(l);
        retPanel.add(result);
        retPanel.add(Box.createHorizontalStrut(5));

        return result;
    }

    private void addFilterMenuItem(JMenu menu, String resKey, String action) {
        JMenuItem menuItem = menu.add(getResource(resKey));
        menuItem.addActionListener(createActionListener(action));
    }

    private ActionListener createActionListener(String methodName) {
        return (ActionListener) EventHandler.create(ActionListener.class, this,
                methodName);
    }


    private JButton createButton(Container p, String resKey, String action) {
        JButton result = new JButton(getResource(resKey));
        result.addActionListener(createActionListener(action));
        p.add(result);
        return result;
    }

    private String getResource(String key) {
        return resources.getString(key);
    }


}
