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
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.*;
import java.text.NumberFormat;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.tree.*;
import javax.swing.text.JTextComponent;
import pspdash.data.DataRepository;
import pspdash.data.NumberData;
import pspdash.data.NumberFunction;
import pspdash.data.DoubleData;


public class TimeLogEditor extends Object
    implements TreeSelectionListener, TableValidator, PSPProperties.Listener
{
    /** Class Attributes */
    protected JFrame          frame;
    protected JTree           tree;
    protected PropTreeModel   treeModel;
    protected PSPProperties   useProps;
    protected PSPDashboard    dashboard = null;
    protected ValidatingTable table;
    protected JSplitPane      splitPane;

    protected Hashtable       postedChanges = new Hashtable();

    JTextField toDate       = null;
    JTextField fromDate     = null;
    TimeLog    tl           = new TimeLog();
    Vector     currentLog   = new Vector();
    String     validateCell = null;
    JButton    revertButton = null;
    JButton    saveButton   = null;
    JButton    addButton    = null;
    JComboBox  formatChoice = null;
    TimeCardDialog timeCardDialog = null;

    static final long DAY_IN_MILLIS = 24 * 60 * 60 * 1000;

    boolean tableContainsRows = false;
    boolean selectedNodeLoggingAllowed = false;

    Resources resources = Resources.getDashBundle("pspdash.Time");

    //
    // member functions
    //
    private void debug(String msg) {
        System.out.println("TimeLogEditor:" + msg);
    }


                                // constructor
    public TimeLogEditor(PSPDashboard dash,
                         ConfigureButton button,
                         PSPProperties props) {
        dashboard        = dash;
        JPanel   panel   = new JPanel(true);

        useProps       = props;
        props.addHierarchyListener(this);
        try {
            tl.read (dashboard.getTimeLog());
        } catch (IOException e) {}

        frame = new JFrame(resources.getString("Time_Log_Editor_Window_Title"));
        frame.setIconImage(java.awt.Toolkit.getDefaultToolkit().createImage
                           (getClass().getResource("icon32.gif")));
        frame.getContentPane().add("Center", panel);
        frame.setBackground(Color.lightGray);
        PCSH.enableHelpKey(frame, "UsingTimeLogEditor");

        /* Create the JTreeModel. */
        treeModel = new PropTreeModel (new DefaultMutableTreeNode ("root"), null);

        /* Create the tree. */
        tree = new JTree(treeModel);
        treeModel.fill (useProps);
        setTimes ();
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
        panel.add("North", constructFilterPanel());
        panel.add("Center", splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                                       sp, constructEditPanel()));
        panel.add("South", constructControlPanel());

        frame.addWindowListener( new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                confirmClose(true);
            }
        });
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        setSelectedNode(dash.getCurrentPhase());
        loadCustomDimensions();
        splitPane.setDividerLocation(dividerLocation);

        applyFilter(false);
        cancelPostedChanges();
        setDirty(false);
        frame.setSize(new Dimension(frameWidth, frameHeight));
        frame.show();
    }


    public void confirmClose(boolean showCancel) {
        if (saveRevertOrCancel(showCancel)) {
            saveCustomDimensions();
            frame.setVisible (false);   // close the time log window.
            if (timeCardDialog != null) timeCardDialog.hide();
        }
    }

    public boolean saveRevertOrCancel(boolean showCancel) {
        if (isDirty())
            switch (JOptionPane.showConfirmDialog
                    (frame,
                     resources.getString("Confirm_Close_Prompt"),
                     resources.getString("Confirm_Close_Title"),
                     showCancel ? JOptionPane.YES_NO_CANCEL_OPTION
                                : JOptionPane.YES_NO_OPTION)) {
            case JOptionPane.CLOSED_OPTION:
            case JOptionPane.CANCEL_OPTION:
                return false;                 // do nothing and abort.

            case JOptionPane.YES_OPTION:
                save();                 // save changes.
                break;

            case JOptionPane.NO_OPTION:
                reload();               // revert changes.
            }
        return true;
    }

    protected boolean dirtyFlag = false;
    protected boolean isDirty() { return dirtyFlag; }
    protected void setDirty (boolean isDirty) {
        dirtyFlag = isDirty;
        saveButton.setEnabled(isDirty);
        revertButton.setEnabled(isDirty);
    }


    void postTimeChange (PropertyKey key, long deltaMinutes) {
        setDirty(true);
        //log the change for future action
        if (deltaMinutes == 0) // if no change, return
            return;
        Long delta = (Long)postedChanges.get (key);
        if (delta == null)
            delta = new Long (deltaMinutes);
        else
            delta = new Long (delta.longValue() + deltaMinutes);
        Object a = postedChanges.put (key, delta);
    }


    void cancelPostedChanges () {
        postedChanges.clear ();
    }


    // ApplyPostedChanges takes the changes made to the TimeLogEditor and
    // applies them to the data repository.
    void applyPostedChanges () {
        long l;

        // If there are any changes, apply them.
        if (postedChanges.size() > 0) {
            DataRepository data = dashboard.getDataRepository();
            String thePath;

            // Get the posted changes (keys) and loop through them all
            Enumeration keys = postedChanges.keys();
            while (keys.hasMoreElements ()) {
                // Store the change's key information into k, and data into l.
                PropertyKey k = (PropertyKey)keys.nextElement ();
                l = ((Long)postedChanges.get (k)).longValue();
                if (l != 0) {
                    thePath = k.path() + "/Time";

                    // Extract the data from the data repository that corresponds
                    // to the change we are currently applying.
                    Object pt = data.getValue (thePath);

                    // Are they trying to log time against some node which performs
                    // roll up only?  This is bad - don't allow it.
                    if (pt != null &&
                        (!(pt instanceof DoubleData) || pt instanceof NumberFunction)) {
                        System.err.println("Error in TimeLogEditor: time must be logged " +
                                           "to phases (i.e. leaves of the hierarchy).");
                        continue;
                    }

                    if (pt != null)
                        l += (long)((DoubleData) pt).getInteger ();

                    // Save the new value into the data repository.
                    data.putValue(thePath, new DoubleData(l, false));
                }
            }
        }
        postedChanges.clear ();
    }


    public static long parseTime (String s) {
        int colon = s.indexOf (":");
        long lv = -1;
        if (colon >= 0) {
            try {
                lv = 60 * Long.valueOf (s.substring (0, colon)).longValue();
            } catch (Exception e) { }
            try {
                lv += Long.valueOf (s.substring (colon + 1)).longValue();
            } catch (Exception e) { }
        } else {
            try {
                lv = Long.valueOf (s).longValue();
            } catch (Exception e) { }
        }
        return lv;
    }

    protected String formatTime (long t) {
        if (t < 60)
            return String.valueOf(t);
        int min = (int) (t % 60);
        if (min < 10)
            return String.valueOf (t / 60) + ":0" + String.valueOf (min);
        return String.valueOf (t / 60) + ":" + String.valueOf (min);
    }

    private static final String[] TIME_FORMAT_KEY_NAMES = {
        "Hours_Minutes", "Hours", "Percent_Parent", "Percent_Total" };
    private static final int FORMAT_HOURS_MINUTES = 0;
    private static final int FORMAT_HOURS = 1;
    private static final int FORMAT_PERCENT_PARENT = 2;
    private static final int FORMAT_PERCENT_TOTAL = 3;
    protected int timeFormat = FORMAT_HOURS_MINUTES;

    private static NumberFormat percentFormatter =
        NumberFormat.getPercentInstance();
    private static NumberFormat decimalFormatter =
        NumberFormat.getNumberInstance();
    static { decimalFormatter.setMaximumFractionDigits(2); }

    protected String formatTime(long t, long parentTime, long totalTime) {
        switch (timeFormat) {
        case FORMAT_HOURS:
            return decimalFormatter.format( (double) t / 60.0);

        case FORMAT_PERCENT_PARENT:
            return (parentTime == 0 ?
                    percentFormatter.format(0.0) :
                    percentFormatter.format( (double) t / (double) parentTime));

        case FORMAT_PERCENT_TOTAL:
            return (totalTime == 0 ?
                    percentFormatter.format(0.0) :
                    percentFormatter.format( (double) t / (double) totalTime));

        case FORMAT_HOURS_MINUTES: default:
            return formatTime(t);
        }
    }

    private static final String DIMENSION_SETTING_NAME = "timelog.dimensions";
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

    public long collectTime(Object node, Hashtable timesIn, Hashtable timesOut) {
        long t = 0;                 // time for this node

                                    // recursively compute total time for each
                                    // child and add total time for this node.
        for (int i = 0; i < treeModel.getChildCount (node); i++) {
            t += collectTime(treeModel.getChild (node, i), timesIn, timesOut);
        }

                                    // fetch and add time spent in this node
        Object [] path = treeModel.getPathToRoot((TreeNode) node);
        Long l = (Long) timesIn.get(treeModel.getPropKey (useProps, path));
        if (l != null)
            t += l.longValue();

        timesOut.put(node, new Long(t));

        return t;
    }

    public void setTimes (Object node, Hashtable times,
                          long parentTime, long totalTime) {

        long t = ((Long) times.get(node)).longValue();

                                    // display the time next to the node name
                                    // in the tree display
        String s = (String) ((DefaultMutableTreeNode)node).getUserObject();
        int index = s.lastIndexOf ("=");
        if (index > 0)
            s = s.substring (0, index-1);
        s = s + " = " + formatTime (t, parentTime, totalTime);
        ((DefaultMutableTreeNode)node).setUserObject(s);
        treeModel.nodeChanged((TreeNode) node);

        for (int i = 0; i < treeModel.getChildCount (node); i++) {
            setTimes (treeModel.getChild (node, i), times, t, totalTime);
        }
    }

    public void setTimes () {
        Date fd = ((fromDate == null) ? null :
                   DateFormatter.parseDate (fromDate.getText()));
        Date td = ((toDate == null) ? null :
                   DateFormatter.parseDate (toDate.getText()));
        if (td != null)             // need to add a day so search is inclusive
            td = new Date (td.getTime() + DAY_IN_MILLIS);

        Hashtable nodeTimes = new Hashtable();
        long totalTime =
            collectTime(treeModel.getRoot(), tl.getTimes(fd, td), nodeTimes);
        setTimes(treeModel.getRoot(), nodeTimes, totalTime, totalTime);

        //treeModel.nodeStructureChanged((TreeNode)treeModel.getRoot());
        tree.repaint(tree.getVisibleRect());
        if (timeCardDialog != null) timeCardDialog.recalc();
    }

    void applyFilter (boolean resetTimes) {
        PropertyKey key = null;
        Date fd = DateFormatter.parseDate (fromDate.getText());
        Date td = DateFormatter.parseDate (toDate.getText());
        if (td != null)             // need to add a day so search is inclusive
            td = new Date (td.getTime() + DAY_IN_MILLIS);
        DefaultMutableTreeNode selected = getSelectedNode();
        if (selected != null) {
            key = treeModel.getPropKey (useProps, selected.getPath());
        }

        //if editing, stop edits
        if (table.table.isEditing())
            table.table.editingStopped
                (new ChangeEvent("Applying filter: Stop edit"));

        // apply the filter and load the vector (and the table)
        VTableModel model = (VTableModel)table.table.getModel();
        Object[] row;
        TimeLogEntry tle;
        Enumeration filt = tl.filter (key, fd, td);
        currentLog.removeAllElements();
        model.setNumRows (0);
        tableContainsRows = false;
        while (filt.hasMoreElements()) {
            tle = (TimeLogEntry)filt.nextElement();
            currentLog.addElement (tle);
            row = new Object[]
                {tle.key.path(),
                 DateFormatter.formatDateTime (tle.createTime),
                 formatTime (tle.minutesElapsed),
                 formatTime (tle.minutesInterrupt),
                 tle.getComment()};
            model.addRow(row);
            tableContainsRows = true;
        }
        table.doResizeRepaint();
        if (resetTimes)
            setTimes();

        addButton.setEnabled(tableContainsRows || selectedNodeLoggingAllowed);
    }

    private void setFilter(Date from, Date to) {
        fromDate.setText(DateFormatter.formatDate(from));
        toDate.setText(DateFormatter.formatDate(to));
        applyFilter(true);
    }

    public void filterToday() {
        Date now = new Date();
        setFilter(now, now);
    }
    public void filterThisWeek() {
        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now); cal.set(cal.DAY_OF_WEEK, 1);
        Date from = cal.getTime();
        cal.setTime(now); cal.set(cal.DAY_OF_WEEK, 7);
        Date to = cal.getTime();
        setFilter(from, to);
    }
    public void filterThisMonth(Date when) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(when); cal.set(cal.DAY_OF_MONTH, 1);
        Date from = cal.getTime();
        cal.set(cal.DAY_OF_MONTH, cal.getActualMaximum(cal.DAY_OF_MONTH));
        Date to = cal.getTime();
        setFilter(from, to);
    }
    public void scrollFilterForward() {
        Date fd = DateFormatter.parseDate (fromDate.getText());
        Date td = DateFormatter.parseDate (toDate.getText());
        if (fd == null) return;
        if (td == null) td = fd;

        long diff = 1 + (td.getTime() - fd.getTime()) / DAY_IN_MILLIS;
        if (diff == 28 || diff == 29 || diff == 30 || diff == 31) {
            fd = new Date(td.getTime() + DAY_IN_MILLIS * 2);
            filterThisMonth(fd);
        } else {
            fd = new Date(td.getTime() + DAY_IN_MILLIS);
            td = new Date(fd.getTime() + (diff-1) * DAY_IN_MILLIS);
            setFilter(fd, td);
        }
    }
    public void scrollFilterBackward() {
        Date fd = DateFormatter.parseDate (fromDate.getText());
        Date td = DateFormatter.parseDate (toDate.getText());
        if (fd == null) return;
        if (td == null) td = fd;

        long diff = 1 + (td.getTime() - fd.getTime()) / DAY_IN_MILLIS;
        td = new Date(fd.getTime() - DAY_IN_MILLIS);
        if (diff == 28 || diff == 29 || diff == 30 || diff == 31) {
            filterThisMonth(td);
        } else {
            fd = new Date(td.getTime() - (diff-1) * DAY_IN_MILLIS);
            setFilter(fd, td);
        }
    }
    public void clearFilter() {
        fromDate.setText("");
        toDate.setText("");
        applyFilter(true);
    }

    // This method implements utilTableValidator
    public boolean validate (int        id,
                             int        row,
                             int        col,
                             String     newValue) {
        if (validateCell != null)
            return false;
        boolean rv = true;
        TimeLogEntry tle;
        try {
            tle = (TimeLogEntry)currentLog.elementAt (row);
            validateCell = "" + row + "," + col;

            // tell the PauseButton (via the dashboard) to release this time
            // log entry, if it is currently "holding" it.
            dashboard.releaseTimeLogEntry(tle);

            tle = (TimeLogEntry) currentLog.elementAt (row);
        } catch (Exception e) {
            validateCell = null;
            return false;
        }
        switch (table.table.convertColumnIndexToModel(col)) {
        case 0:                     //Logged To (key) (must exist in hierarchy)
            PropertyKey key = useProps.findExistingKey (newValue);
            if (key == null || key.equals(tle.key) || !timeLoggingAllowed(key)) {
                rv = false;
                table.table.setValueAt (tle.key.path(), row, col);
            } else {
                long deltaMinutes = parseTime((String)table.table.getValueAt (row, 2));
                postTimeChange (key, deltaMinutes);
                postTimeChange (tle.key, - deltaMinutes);
                tle.key = key;
            }
            break;
        case 1:                     //createTime (must be valid date)
            Date d = DateFormatter.parseDateTime (newValue);
            if (d == null || d.equals(tle.createTime)) {
                rv = false;
                table.table.setValueAt (DateFormatter.formatDateTime (tle.createTime),
                                        row, col);
            } else
                tle.createTime = d;
            break;
        case 2:                     //minutesElapsed (must be number >= 0)
            try {
                long lv = parseTime (newValue);//Long.valueOf (newValue).longValue();
                long deltaMinutes = tle.minutesElapsed;
                if (lv >= 0 && lv != deltaMinutes) {
                    tle.minutesElapsed = lv;
                    postTimeChange (tle.key, lv - deltaMinutes);
                } else
                    rv = false;
                table.table.setValueAt (formatTime (tle.minutesElapsed), row, col);
            } catch (Exception e) { rv = false; }
            break;
        case 3:                     //minutesInterrupt (must be number >= 0)
            try {
                long lv = parseTime (newValue);//Long.valueOf (newValue).longValue();
                if (lv >= 0 && lv != tle.minutesInterrupt)
                    tle.minutesInterrupt = lv;
                else
                    rv = false;
                table.table.setValueAt (formatTime (tle.minutesInterrupt), row, col);
            } catch (Exception e) { rv = false; }
            break;
        case 4:                     // comment (any string is fine)
            tle.setComment(newValue);
            break;
        }
        setTimes ();
        validateCell = null;
        if (rv) setDirty(true);
        return rv;
    }


    private JPanel constructFilterPanel () {
        JPanel  retPanel = new JPanel(false);
        retPanel.setLayout(new BoxLayout(retPanel, BoxLayout.X_AXIS));
        retPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        DropDownButton button;
        JButton btn;
        Insets insets = new Insets(0, 2, 0, 2);
        JLabel  label;

        retPanel.add(Box.createHorizontalGlue());

        label = new JLabel (resources.getString("Format_Label")+" ");
        retPanel.add(label);   retPanel.add(Box.createHorizontalStrut(5));
        formatChoice = new JComboBox
            (resources.getStrings("Format_", TIME_FORMAT_KEY_NAMES));
        retPanel.add(formatChoice);   retPanel.add(Box.createHorizontalStrut(5));
        formatChoice.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                timeFormat = formatChoice.getSelectedIndex(); setTimes(); }} );
        retPanel.add(new JLabel("          "));

        label = new JLabel (resources.getString("Filter_Label") + " ");
        retPanel.add (label);   retPanel.add(Box.createHorizontalStrut(5));

        btn = new JButton(resources.getString("Filter_Scroll_Backward_Button"));
        btn.setMargin(insets);
        btn.setToolTipText(resources.getString("Filter_Scroll_Backward_Tooltip"));
        btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { scrollFilterBackward(); }
            });
        retPanel.add(btn);   retPanel.add(Box.createHorizontalStrut(5));

        label = new JLabel (resources.getString("Filter_From") + " ");
        retPanel.add (label);   retPanel.add(Box.createHorizontalStrut(5));

        fromDate = new JTextField ("", 10);
        fromDate.setMaximumSize(fromDate.getPreferredSize());
        DateChangeAction l = new DateChangeAction (fromDate);
        fromDate.addActionListener (l);
        fromDate.addFocusListener (l);
        retPanel.add (fromDate);   retPanel.add(Box.createHorizontalStrut(5));

        label = new JLabel (" " + resources.getString("Filter_To") + " ");
        retPanel.add (label);   retPanel.add(Box.createHorizontalStrut(5));

        toDate = new JTextField ("", 10);
        toDate.setMaximumSize(toDate.getPreferredSize());
        l = new DateChangeAction (toDate);
        toDate.addActionListener (l);
        toDate.addFocusListener (l);
        retPanel.add (toDate);   retPanel.add(Box.createHorizontalStrut(5));


        btn = new JButton(resources.getString("Filter_Scroll_Forward_Button"));
        btn.setMargin(insets);
        btn.setToolTipText(resources.getString("Filter_Scroll_Forward_Tooltip"));
        btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { scrollFilterForward(); }
            });
        retPanel.add(btn);   retPanel.add(Box.createHorizontalStrut(5));

        button = new DropDownButton (resources.getString("Filter_Apply"));
        button.setRunFirstMenuOption(false);
        button.getButton().addActionListener(new ActionListener () {
            public void actionPerformed(ActionEvent e) { applyFilter (true); }
            });
        JMenu menu = button.getMenu();
        menu.add(resources.getString("Filter_Today")).addActionListener
            (new ActionListener() {
                public void actionPerformed(ActionEvent e) { filterToday(); }
                });
        menu.add(resources.getString("Filter_Week")).addActionListener
            (new ActionListener() {
                public void actionPerformed(ActionEvent e) { filterThisWeek(); }
                });
        menu.add(resources.getString("Filter_Month")).addActionListener
            (new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    filterThisMonth(new Date());
                }});
        menu.addSeparator();
        menu.add(resources.getString("Filter_Remove")).addActionListener
            (new ActionListener() {
                public void actionPerformed(ActionEvent e) { clearFilter(); }
                });
        retPanel.add (button);

        retPanel.add(Box.createHorizontalGlue());

        return retPanel;
    }

    /** Add or update a row in the time log.
     * @param tle a time log entry that was created or modified elsewhere.
     */
    public void addRow (TimeLogEntry tle) {
        TimeLogEntry oldEntry = tl.addOrUpdate(tle);
        if (oldEntry != null) {
            int row = currentLog.indexOf(oldEntry);

            if (row == -1)            // oldEntry is not in the list of currently
                return;                 // visible entries. No redraw is necessary.

            currentLog.set(row, tle);
            VTableModel model = (VTableModel)table.table.getModel();
            model.setValueAt(tle.key.path(), row, 0);
            model.setValueAt(DateFormatter.formatDateTime(tle.createTime), row, 1);
            model.setValueAt(String.valueOf (tle.minutesElapsed), row, 2);
            model.setValueAt(String.valueOf (tle.minutesInterrupt), row, 3);
            model.setValueAt(tle.getComment(), row, 4);

        } else {
            applyFilter(false);
        }
        setTimes();

        /*
        Object aRow[] = new Object[]
            {,
             ,
             ,
             };

        ((VTableModel)table.table.getModel()).addRow(aRow);
        tableContainsRows = true;
        addButton.setEnabled(true);
        currentLog.addElement (tl.add (tle));
        setDirty(true);
        */
    }

    public void addRow () {
        JTable aTable = table.table;
        VTableModel model = (VTableModel)aTable.getModel();
        TimeLogEntry tle = null;
        int rowBasedOn;
        DefaultMutableTreeNode selected;
        PropertyKey key;

                            // try to base new row on the selected table row
        if ((rowBasedOn = aTable.getSelectedRow()) != -1)
            ; // nothing to do here .. just drop out of "else if" tree

                              // else try to base new row on current editing row
        else if ((rowBasedOn = aTable.getEditingRow()) != -1)
            aTable.editingStopped (new ChangeEvent("Adding row: Stop edit"));

                              // else try to base new row on current tree selection
        else if ((selected = getSelectedNode()) != null &&
                 (key = treeModel.getPropKey(useProps, selected.getPath())) != null
                 && timeLoggingAllowed(key))
            tle = new TimeLogEntry (key, new Date(), 0, 0);

        else              // else try to base new row on last row of table
            rowBasedOn = currentLog.size() - 1;


        Object aRow[];
        if (tle == null) {
            if (rowBasedOn == -1) {   // create 'blank'
                tle = new TimeLogEntry (new PropertyKey (PropertyKey.ROOT),
                                        new Date(), 0, 0);
            } else {          // base it on rowBasedOn
                TimeLogEntry tle2 = (TimeLogEntry)currentLog.elementAt (rowBasedOn);
                tle = new TimeLogEntry (new PropertyKey (tle2.key),
                                        new Date (tle2.createTime.getTime()),
                                        tle2.minutesElapsed, tle2.minutesInterrupt);
            }
        }
        aRow = new Object[]
            {tle.key.path(),
             DateFormatter.formatDateTime (tle.createTime),
             String.valueOf (tle.minutesElapsed),
             String.valueOf (tle.minutesInterrupt),
             null };                  // don't copy the comment.
        model.addRow(aRow);
        tableContainsRows = true;
        addButton.setEnabled(true);
        currentLog.addElement (tl.add (tle));
        setDirty(true);
        setTimes ();
        postTimeChange (tle.key, tle.minutesElapsed);
    }

    public void deleteSelectedRow () {
        JTable aTable = table.table;
        VTableModel model = (VTableModel)aTable.getModel();
        int selectedRow = aTable.getSelectedRow();
        int editingRow  = aTable.getEditingRow();
        int rowBasedOn = (selectedRow != -1 ? selectedRow : editingRow);
        if (rowBasedOn == -1) return;
        if (editingRow != -1)
            aTable.editingStopped (new ChangeEvent("Deleting row: Stop edit"));

        TimeLogEntry tle = (TimeLogEntry)currentLog.elementAt (rowBasedOn);
        dashboard.releaseTimeLogEntry(tle);
        model.removeRow (rowBasedOn);
        try {
            currentLog.removeElementAt (rowBasedOn);
        } catch (Exception e) {}
        tl.remove (tle);
        setDirty(true);
        setTimes ();
        postTimeChange (tle.key, - tle.minutesElapsed);

        tableContainsRows = (model.getRowCount() > 0);
        addButton.setEnabled(tableContainsRows || selectedNodeLoggingAllowed);
    }

    protected void summarize() {
        TimeLogEntry tle, tle2;
        boolean      merged = false;
        for (int i = 0; i < currentLog.size(); i++) {
            tle = (TimeLogEntry)currentLog.elementAt(i);
            for (int j = currentLog.size() - 1; j > i; j--) {
                tle2 = (TimeLogEntry)currentLog.elementAt(j);
                if (tle.key.key().equals (tle2.key.key())) {
                    //merge into tle and delete tle2
                    merged = true;
                    tle.minutesElapsed   += tle2.minutesElapsed;
                    tle.minutesInterrupt += tle2.minutesInterrupt;
                    if (tle2.getComment() != null) {
                        if (tle.getComment() == null)
                            tle.setComment(tle2.getComment());
                        else
                            tle.setComment(tle.getComment() + "\n" + tle2.getComment());
                    }
                    System.err.println("merging:"+tle+"+"+tle2);
                    tl.remove (tle2);
                    setDirty(true);
                    try {                 // make sure that we only merge once
                        currentLog.removeElementAt (j);
                    } catch (Exception e) {}
                }
            }
        }
        if (merged)
            applyFilter (true);
    }

    protected void summarizeWarning() {
        if (JOptionPane.showConfirmDialog
            (frame,
             resources.getStrings("Summarization_Warning_Message"),
             resources.getString("Summarization_Warning_Title"),
             JOptionPane.OK_CANCEL_OPTION,
             JOptionPane.WARNING_MESSAGE)
            == JOptionPane.OK_OPTION) {
            summarize();
        }
    }

    private static final String[] COLUMN_KEYS = {
        "Logged_To", "Start_Time", "Delta_Time", "Interrupt_Time", "Comment" };
    private JPanel constructEditPanel () {
        JPanel  retPanel = new JPanel(false);
        JButton button;

        retPanel.setLayout(new BorderLayout());
        table = new ValidatingTable
            (resources.getStrings("Column_Name_", COLUMN_KEYS),
             null,
             resources.getInts("Column_Width_", COLUMN_KEYS),
             resources.getStrings("Column_Tooltip_", COLUMN_KEYS),
             null, this, 0, true, null, null);
        retPanel.add ("Center", table);

        JPanel btnPanel = new JPanel(false);
        addButton = button = new JButton (Resources.getGlobalString("Add"));
        button.addActionListener (new ActionListener () {
            public void actionPerformed(ActionEvent e) { addRow(); }
        });
        btnPanel.add (button);

        button = new JButton (Resources.getGlobalString("Delete"));
        button.addActionListener (new ActionListener () {
            public void actionPerformed(ActionEvent e) { deleteSelectedRow(); }
        });
        btnPanel.add (button);

        button = new JButton (resources.getString("Summarize_Button"));
        button.addActionListener (new ActionListener () {
            public void actionPerformed(ActionEvent e) { summarizeWarning(); }
        });
        btnPanel.add (button);

        retPanel.add ("South", btnPanel);

        return retPanel;
    }

    protected void showTimeCard() {
        if (timeCardDialog != null)
            timeCardDialog.show();
        else
            timeCardDialog = new TimeCardDialog(useProps, tl);
    }

    private JPanel constructControlPanel () {
        JPanel  retPanel = new JPanel(false);

        JButton timeCardButton = new JButton
            (resources.getString("Time_Card_View_Button"));
        retPanel.add(timeCardButton);
        retPanel.add(Box.createHorizontalStrut(100));
        timeCardButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showTimeCard(); }});

        revertButton = new JButton (Resources.getGlobalString("Revert"));
        retPanel.add (revertButton);
        revertButton.addActionListener (new ReloadAction ());

        saveButton = new JButton (Resources.getGlobalString("Save"));
        retPanel.add (saveButton);
        saveButton.addActionListener (new SaveAction ());

        JButton closeButton = new JButton(Resources.getGlobalString("Close"));
        retPanel.add(closeButton);
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { confirmClose(true); }
            });

        return retPanel;
    }

    public void save() {
        //if editing, stop edits
        if (table.table.isEditing())
            table.table.editingStopped
                (new ChangeEvent("Saving Data: Stop edit"));
        try {                     // save the time log
            tl.save (dashboard.getTimeLog());
        } catch (IOException ioe) {}
        applyPostedChanges ();
        setDirty(false);
    }


    public void hierarchyChanged(PSPProperties.Event e) { reloadAll(null); }

    public void reloadAll(PSPProperties newProps) {
        if (newProps != null)
            useProps.copy(newProps);
        treeModel.reload (useProps);

        checkSelectedNodeLoggingAllowed();

        reload();
    }

    public void reload() {
        try {                       // re-read time log
            tl.read (dashboard.getTimeLog());
        } catch (IOException ioe) {}
        applyFilter(true);
        cancelPostedChanges ();
        setDirty(false);
    }

    public void show() {
        if (frame.isShowing())
            frame.toFront();
        else {
            reload();
            frame.show();
        }
    }

                                // make sure root is expanded
    public void expandRoot () { tree.expandRow (0); }


    // Returns the TreeNode instance that is selected in the tree.
    // If nothing is selected, null is returned.
    protected DefaultMutableTreeNode getSelectedNode() {
        TreePath   selPath = tree.getSelectionPath();

        if(selPath != null)
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
            applyFilter (false);
            return;
        }
        Object [] path = tp.getPath();
        PropertyKey key = treeModel.getPropKey (useProps, path);
        checkSelectedNodeLoggingAllowed(key);
        applyFilter (false);
    }

    private void checkSelectedNodeLoggingAllowed() {
        TreePath selPath = tree.getSelectionPath();
        PropertyKey key = null;
        if (selPath != null) {
            Object [] path = selPath.getPath();
            key = treeModel.getPropKey (useProps, path);
        }
        checkSelectedNodeLoggingAllowed(key);
    }
    private void checkSelectedNodeLoggingAllowed(PropertyKey key) {
        selectedNodeLoggingAllowed = timeLoggingAllowed(key);
    }
    private boolean timeLoggingAllowed(PropertyKey key) {
        return TimeLog.timeLoggingAllowed(key, useProps, dashboard.data);
    }


    // DateChangeAction responds to user input in a date field.
    class DateChangeAction implements ActionListener, FocusListener {
        JTextComponent widget;
        String         text = null;
        Color          background;

        public DateChangeAction (JTextComponent src) {
            widget = src;
            text = widget.getText();
            background = new Color (widget.getBackground().getRGB());
        }

        protected void validate () {
            String newText = widget.getText();

            Date d = DateFormatter.parseDate (newText);
            if (d == null) {
                if ((newText != null) && (newText.length() > 0))
                    widget.setBackground (Color.red);
                else {
                    widget.setBackground (background);
                    text = newText;
                    widget.setText (text);
                }
            } else {
                widget.setBackground (background);
                text = DateFormatter.formatDate (d);
                widget.setText (text);
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


    //
    // ReloadAction responds to user clicking Reload button.
    //
    class ReloadAction extends Object implements ActionListener {
        public void actionPerformed(ActionEvent e) { reload(); }
    } // End of TimeLogEditor.ReloadAction


    //
    // SaveAction responds to user clicking Save button.
    //
    class SaveAction extends Object implements ActionListener {
        public void actionPerformed(ActionEvent e) { save(); }
    } // End of TimeLogEditor.SaveAction

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

}
