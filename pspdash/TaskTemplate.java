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
import javax.swing.tree.*;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import pspdash.data.DataRepository;
import pspdash.data.NumberData;
import pspdash.data.DateData;
import pspdash.data.DoubleData;


public class TaskTemplate extends Object implements TreeSelectionListener, TableValidator
{
    /** Class Attributes */
    protected JFrame          frame;
    protected JTree           tree;
    protected PropTreeModel   treeModel;
    protected PSPProperties   useProps;
    protected PSPDashboard    dashboard = null;
    protected ValidatingTable taskTable;
    protected ValidatingTable scheduleTable;
    protected JCheckBox       autoCalc;
    protected JComboBox       taskNames;

    protected Vector          listOfNodes = new Vector();
    protected boolean         autoCalculateOn = false;
    protected JTextField      startDate = null;
    protected String          taskFile;
    protected String          taskSheetName;
    protected Properties      taskProps = null;

    Vector currentTaskLog     = new Vector();
    Vector currentScheduleLog = new Vector();

    DataRepository data         = null;
    TimeLog        tl           = new TimeLog();
    String         validateCell = null;

    static final long   DAY_IN_MILLIS  = 24 * 60 * 60 * 1000;
    static final int    TASK_TABLE     = 0;
    static final int    SCHEDULE_TABLE = 1;
    static final int    EV_DECIMALS    = 1;

    static final String COMPLETED_TIME = "Completed";
    static final String PLANNED_TIME   = "Estimated Time";

    static final String NEW_TOKEN      = "<new>"; // pull menu options
    static final String DELETE_TOKEN   = "<delete>";
    static final String NO_SELECTION   = "<none>";

    static final String ID_SEP         = "|"; // id field divider

    static final String TAB            = "        "; // prop field divider
    static final String TASK_KEY       = "<tasks>";  // top prop for task sheet
    static final String PT_START       = ".start";   // Props for task sheet
    static final String PT_EXTERNAL    = ".external";
    static final String PT_WEEKS       = ".weeks";


    //
    // member functions
    //
    private void debug(String msg) {
        System.out.println("TaskTemplate:" + msg);
    }



                                // constructor
    public TaskTemplate(PSPDashboard dash,
                        ConfigureButton button,
                        PSPProperties props) {
        dashboard    = dash;
        JPanel panel = new JPanel();

        useProps = props;
        data     = dashboard.getDataRepository();

        try {
            tl.read (dashboard.getTimeLog());
        } catch (IOException e) {}

        frame = new JFrame("TaskTemplate");
        frame.setTitle("TaskTemplate");
        frame.setBackground(Color.lightGray);

        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints g = new GridBagConstraints();
        panel.setLayout(layout);
        JPanel p;
        p = constructHeaderPanel();
        g.fill  = GridBagConstraints.HORIZONTAL;
        g.gridx = 0;
        layout.setConstraints (p, g);
        panel.add (p);
        g.fill = GridBagConstraints.BOTH;
        g.weighty = 1.0;
        g.weightx = 1.0;
        Insets dInsets = g.insets;
        g.insets = new Insets (0, 3, 0, 3);
        JSplitPane jsp = new JSplitPane (JSplitPane.VERTICAL_SPLIT,
                                         constructTaskPanel(),
                                         constructSchedulePanel());
        layout.setConstraints (jsp, g);
        panel.add (jsp);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weighty = 0.0;
        g.weightx = 0.0;
        g.insets = dInsets;
        p = constructControlPanel();
        layout.setConstraints (p, g);
        panel.add (p);

        frame.addWindowListener( new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                frame.setVisible (false);
            }
        });

        calculate();
        frame.getContentPane().add("Center", panel);
        frame.pack();
        frame.show();

        getListOfNodes(listOfNodes, useProps);

        taskFile = Settings.getVal ("taskFile");
        taskProps = new Properties ();
        try {
            taskProps.load (new FileInputStream(taskFile));
        } catch (Exception e) {}
    }



    public void save() {
        if (taskProps != null) {
            try {
                FileOutputStream out = new FileOutputStream (taskFile);
                taskProps.store(out, "Task & Schedule Template Data");
            } catch (Exception e2) {}
        }

        Date d = DateFormatter.parseDate (startDate.getText());
        Object a;
        if (d == null)
            a = taskProps.remove (taskSheetName + PT_START);
        else
            taskProps.put (taskSheetName + PT_START, "" + d.getTime());
    }



    protected void addChildNodeNames (Vector v,
                                      PSPProperties props,
                                      PropertyKey pk) {
        v.addElement (pk.path());
        for (int ii = 0; ii < props.getNumChildren(pk); ii++) {
            addChildNodeNames (v, props, props.getChildKey(pk, ii));
        }
    }



    protected void getListOfNodes (Vector v, PSPProperties props) {
        addChildNodeNames (v, props, PropertyKey.ROOT);
    }



    protected void addTask (String s) {
        PropertyKey pk;
        String      node;
        for (int ii = 0; ii < listOfNodes.size(); ii++) {
            node = (String)listOfNodes.elementAt (ii);
            if (node.startsWith (s)) {
                pk = PropertyKey.fromPath (node);
                if (useProps.getNumChildren (pk) == 0)
                    addTaskRow (pk);
                listOfNodes.removeElementAt (ii--);
            }
        }
        calculate();
    }



    public long scanTask (TaskEntry te) { // fill in 'slurp'able data fields
        String thePath = te.taskName.path();

        NumberData pt = (NumberData)data.getValue (thePath + PLANNED_TIME);
        te.plannedTime = ((pt == null) ? 0 : pt.getInteger());
        DateData ad = (DateData)data.getValue (thePath + COMPLETED_TIME);
        te.actualDate = ((ad == null) ? null : ad.getValue());
        return te.plannedTime;
    }



    protected String stringDouble (double x, int afterDot) {
        double factor = Math.pow (10.0, (double)afterDot);
        return String.valueOf (Math.round (x * factor) / factor);
    }



    protected Object [] getScheduleRow (long weekNumber,
                                        Date startDate,
                                        ScheduleEntry se) {
        return new Object[]
            {String.valueOf (weekNumber + 1),
             DateFormatter.formatDate (ScheduleEntry.dateOf (startDate, weekNumber)),
             String.valueOf (se.plannedTime / 60),
             String.valueOf (se.plannedCumTime / 60),
             stringDouble (se.plannedCumValue, EV_DECIMALS),
             formatTime (se.actualTime),
             formatTime (se.actualCumTime),
             stringDouble (se.actualCumEarnedValue, EV_DECIMALS)};
    }



    protected Object [] getTaskRow (TaskEntry te,
                                    int       myNumber) {
        return new Object[]
            {String.valueOf (myNumber),
             ((te.taskName == null) ? "" : te.taskName.path()),
             formatTime (te.plannedTime),
             stringDouble (te.plannedValue, EV_DECIMALS),
             formatTime (te.plannedCumTime),
             stringDouble (te.plannedCumValue, EV_DECIMALS),
             DateFormatter.formatDate (te.plannedDate),
             DateFormatter.formatDate (te.actualDate),
             stringDouble (te.actualEarnedValue, EV_DECIMALS)};
    }



    // Scan time log(s) for all time entered for the listed nodes during the
    // week that starts at time d.
    protected long getTimeFromWeekOf (TimeLog tl, Date d, Vector nodeList) {
        TimeLogEntry tle;
        long         totalTime = 0;
        Enumeration filt = tl.filter (PropertyKey.ROOT,
                                      d,
                                      ScheduleEntry.dateOf (d, 1));
        while (filt.hasMoreElements()) {
            tle = (TimeLogEntry)filt.nextElement();
            if (nodeList.contains (tle.key))
                totalTime += tle.minutesElapsed;
        }
        return totalTime;
    }



    // Scan data log(s) for all (listed) tasks marked complete for the week
    // specified.
    protected double getEarnedValueFromWeek (Date d, Vector taskLog) {
        TaskEntry te;
        Date      d2          = ScheduleEntry.dateOf (d, 1);
        double    earnedValue = 0.0;

        for (int i = 0; i < taskLog.size(); i++) {
            te = (TaskEntry)taskLog.elementAt (i);
            if (te.actualDate != null)
                if ((te.actualDate.equals (d) || te.actualDate.after (d)) &&
                    te.actualDate.before (d2))
                    earnedValue += te.actualEarnedValue;
        }
        return earnedValue;
    }



    protected Vector getNodeList () {
        Vector v = new Vector ();
        for (int ii = 0; ii < currentTaskLog.size(); ii++)
            v.addElement (((TaskEntry)currentTaskLog.elementAt (ii)).taskName);
        return v;
    }



    // Update the tables per current logs
    protected void updateTables()
    {
        JTable        aTable;
        VTableModel   model;
        ScheduleEntry se;
        Object        a[];
        int           rowCount;

        aTable = taskTable.table;
        model = (VTableModel)aTable.getModel();
        rowCount = model.getRowCount();
        for (int row = 0; row < currentTaskLog.size(); row++) {
            a = getTaskRow ((TaskEntry)currentTaskLog.elementAt (row), row + 1);
            if (row < rowCount)
                for (int col = 0; col < a.length; col++)
                    model.setValueAt (a [col], row, col);
            else
                model.addRow(a);
        }
        aTable.validate();

        aTable = scheduleTable.table;
        model = (VTableModel)aTable.getModel();
        rowCount = model.getRowCount();
        Date sd = DateFormatter.parseDate (startDate.getText());
        for (int row = 0; row < currentScheduleLog.size(); row++) {
            a = getScheduleRow
                (row, sd,
                 (ScheduleEntry)currentScheduleLog.elementAt (row));
            if (row < rowCount)
                for (int col = 0; col < a.length; col++)
                    model.setValueAt (a [col], row, col);
            else
                model.addRow(a);
        }
        aTable.validate();
    }



    public void calculate () {
        long cumTime = 0;
        TaskEntry     te;
        ScheduleEntry se;

        //end any ongoing edit(s)
        if (taskTable.table.isEditing())
            taskTable.table.editingStopped
                (new ChangeEvent("Calculating: Stop edit"));
        if (scheduleTable.table.isEditing())
            scheduleTable.table.editingStopped
                (new ChangeEvent("Calculating: Stop edit"));

        for (int ii = 0; ii < currentTaskLog.size(); ii++) {
            te = (TaskEntry)currentTaskLog.elementAt (ii);
            cumTime += scanTask (te);
            te.plannedCumTime = cumTime;
        }

        double cpv = 0.0;
        for (int ii = 0; ii < currentTaskLog.size(); ii++) {
            te = (TaskEntry)currentTaskLog.elementAt (ii);
            if (cumTime != 0)
                te.plannedValue = (double)(te.plannedTime * 100) / (double)cumTime;
            else
                te.plannedValue = 0.0;
            te.plannedCumValue = cpv = cpv + te.plannedValue;
            te.actualEarnedValue = ((te.actualDate != null) ? te.plannedValue : 0.0);
        }

        long pct = 0;
        long actualCumTime = 0;
        int taskIndex = 0;
        double acev = 0.0, pcv = 0.0;
        Vector nodeList = getNodeList ();
        Date sd = DateFormatter.parseDate (startDate.getText());
        for (int ii = 0; ii < currentScheduleLog.size(); ii++) {
            se = (ScheduleEntry)currentScheduleLog.elementAt (ii);
            se.date = ScheduleEntry.dateOf (sd, ii);
            if (se.plannedTime != 0) {
                pct += se.plannedTime;
                se.plannedCumTime = pct;
                se.plannedCumValue = pcv;
                while ((taskIndex < currentTaskLog.size()) &&
                       ((te = (TaskEntry)currentTaskLog.elementAt
                           (taskIndex)).plannedCumTime) <= pct) {
                    te.plannedDate = se.date;
                    se.plannedCumValue = pcv = te.plannedCumValue;
                    taskIndex++;
                }
                //get actual time worked during the week
                actualCumTime += (se.actualTime = getTimeFromWeekOf (tl, se.date,
                                                                     nodeList));
                se.actualCumTime = actualCumTime;
                acev += getEarnedValueFromWeek (se.date, currentTaskLog);
                se.actualCumEarnedValue = acev;
            }
        }
        updateTables();
    }



    protected long parseTime (String s) {
        int colon = s.indexOf (":");
        long lv = 0;
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



    public String hasID (String idList, String id, String sep) {
        StringTokenizer st = new StringTokenizer (idList, sep, false);
        String s, test = id + ".";
        while (st.hasMoreTokens()) {
            s = st.nextToken();
            if (s.startsWith (test))
                return s;
        }
        return null;
    }




    public void addID (Vector v, String id, PropertyKey key) {
        if (id == null)
            return;
        int idx = Integer.valueOf (id.substring
                                   (id.lastIndexOf (".") + 1)).intValue();
        if (idx >= v.size())
            v.setSize (idx + 1);
        v.setElementAt (key, idx);
    }



    //does NOT check for replacement
    public void addID (PropertyKey pk, String id, int index) {
        Prop p = useProps.pget (pk);
        String s = p.getID();
        String t = ((s == null) ? "" : s + ID_SEP) + (id + "." + index);
        p.setID (t);
        useProps.put (pk, p);
    }



    public void deleteID (PropertyKey pk, String id, int index) {
        String  s, t  = "";
        boolean first = true;
        int     count = 0;
        Prop    p     = useProps.pget (pk);
        StringTokenizer st = new StringTokenizer (p.getID(), ID_SEP, false);
        while (st.hasMoreTokens()) {
            s = st.nextToken();
            if (count++ != index) {
                t = (first) ? s : t + ID_SEP + s;
                first = false;
            }
        }
        useProps.put (pk, p);
    }



    public void updateWeek (String id,
                            int    index,
                            long   plannedTime) {
        String wks = (String)taskProps.get (id + PT_WEEKS);
        Vector v = new Vector();
        if (wks != null) {
            StringTokenizer st = new StringTokenizer (wks, TAB, false);
            while (st.hasMoreTokens())
                v.addElement (st.nextToken());
        }
        if (v.size() <= index)
            v.setSize (index + 1);
        v.setElementAt ((String) ("" + plannedTime), index);
        wks = null;
        boolean first = true;
        for (int i = 0; i < v.size(); i++) {
            wks = ((first) ? "" : wks + TAB) + (String)v.elementAt (i);
            first = false;
        }
        taskProps.put (id + PT_WEEKS, wks);
    }



    public void deleteWeek (String id, int index) {
        String wks = (String)taskProps.get (id + PT_WEEKS);
        StringTokenizer st = new StringTokenizer (wks, TAB, false);
        Vector v = new Vector();
        while (st.hasMoreTokens())
            v.addElement (st.nextToken());
        if (v.size() > index) {
            v.removeElementAt (index);
            wks = null;
            boolean first = true;
            for (int i = 0; i < v.size(); i++) {
                wks = ((first) ? "" : wks + TAB) + (String)v.elementAt (i);
                first = false;
            }
            taskProps.put (id + PT_WEEKS, wks);
        }
    }



    // This method implements utilTableValidator
    public boolean validate (int        id,
                             int        row,
                             int        col,
                             String     newValue) {
        if (validateCell != null)
            return false;
        boolean rv = true;
        validateCell = "" + row + "," + col;
        switch (id) {
        case TASK_TABLE:
            TaskEntry te;
            try {
                te = (TaskEntry)currentTaskLog.elementAt (row);
            } catch (Exception e) { validateCell = null; return false; }
            switch (col) {
            case 2:                   // Planned Time
                try {
                    String thePath = te.taskName.path();
                    DoubleData pt = (DoubleData)data.getValue (thePath + PLANNED_TIME);
                    if ((pt == null) || (pt.isEditable())) {
                        long lv = parseTime (newValue);
                        if (lv >= 0) {
                            te.plannedTime = lv;
                        } else {
                            te.plannedTime = 0;
                            rv = false;
                        }
                        data.putValue (thePath + PLANNED_TIME,
                                       new DoubleData (te.plannedTime));
                    }
                    taskTable.table.setValueAt (formatTime (te.plannedTime), col, row);
                } catch (Exception e) { rv = false; }
                break;
            case 7:                   // Actual Date
                Date d = DateFormatter.parseDate (newValue);
                if (d == null) {
                    rv = false;
                    if (te.actualDate != null)
                        taskTable.table.setValueAt (DateFormatter.formatDateTime
                                                    (te.actualDate), col, row);
                    else
                        taskTable.table.setValueAt ("", col, row);
                } else {
                    te.actualDate = d;
                    DateData ad = new DateData (d, true);
                    data.putValue (te.taskName.path() + COMPLETED_TIME, ad);
                }
                break;
            }
            break;

        case SCHEDULE_TABLE:
            ScheduleEntry se;
            try {
                se = (ScheduleEntry)currentScheduleLog.elementAt (row);
            } catch (Exception e) { validateCell = null; return false; }
            switch (col) {
            case 2:                   // Planned Time
                try {
                    long lv = Long.valueOf (newValue).longValue();
                    if (lv >= 0)
                        se.plannedTime = lv * 60;
                    else {
                        se.plannedTime = 0;
                        rv = false;
                    }
                    scheduleTable.table.setValueAt (String.valueOf (se.plannedTime / 60),
                                                    col, row);
                    updateWeek (taskSheetName, row, se.plannedTime);
                } catch (Exception e) { rv = false; }
                break;
            } // switch (col)
        } // switch (id)
        if (autoCalculateOn)
            calculate();
        validateCell = null;
        return rv;
    }



    public void clearTables () {
        JTable aTable = taskTable.table;
        VTableModel model = (VTableModel)aTable.getModel();
        if (aTable.getEditingRow() != -1)
            aTable.editingStopped (new ChangeEvent("Clearing table: Stop edit"));
        for (int i = model.getRowCount() - 1; i >= 0; i--)
            model.removeRow (i);
        currentTaskLog.setSize (0);

        aTable = scheduleTable.table;
        model = (VTableModel)aTable.getModel();
        if (aTable.getEditingRow() != -1)
            aTable.editingStopped (new ChangeEvent("Clearing table: Stop edit"));
        for (int i = model.getRowCount() - 1; i >= 0; i--)
            model.removeRow (i);
        currentScheduleLog.setSize (0);
    }




    public void loadSheet (String sheet) {
        clearTables();
        String taskList = (String)taskProps.get (TASK_KEY);

        Vector v = new Vector();
        String s;
        StringTokenizer st;
        s = (String)taskProps.get (sheet + PT_START);
        startDate.setText (s);
        s = (String)taskProps.get (sheet + PT_EXTERNAL);
        //Here we handle externally defined/imported sheets
        s = (String)taskProps.get (sheet + PT_WEEKS);

        Enumeration keys = useProps.keys();
        Prop        value;
        String      id;
        PropertyKey key;
        while (keys.hasMoreElements()) {
            key = (PropertyKey)keys.nextElement();
            value = (Prop)useProps.get (key);
            if (value != null)
                addID (v, hasID (value.getID(), sheet, ID_SEP), key);
        }
        PropertyKey pk;
        for (int i = 0; i < v.size(); i++) {
            pk = (PropertyKey)v.elementAt (i);
            if (pk != null)
                addTaskRow (pk);
            else {                    // check here for externals
            }
        }

        st = new StringTokenizer (s, TAB, false);
        while (st.hasMoreTokens())
            addScheduleRow (Long.valueOf (st.nextToken()).longValue());
    }



    public void createSheet (String s) {
        String taskList = (String)taskProps.get (TASK_KEY);
        if (taskList != null) {
            StringTokenizer st = new StringTokenizer (taskList, TAB, false);
            while (st.hasMoreTokens())
                if (st.nextToken().equals (s)) {
                                        //Message to user - Can't create existing sheet
                    JOptionPane.showMessageDialog
                        (frame,
                         "Cannot Create New Task Sheet With Existing Name.",
                         "Task Sheet Creation Error",
                         JOptionPane.PLAIN_MESSAGE);
                    return;
                }
        }

        taskList = ((taskList == null) ? "" : taskList + TAB) + s;
        taskProps.put (TASK_KEY, taskList);
        taskNames.insertItemAt (s, 1);
        Date sd = ((startDate != null) ?
                   DateFormatter.parseDate (startDate.getText()) : null);
        taskProps.put (s + PT_START, (sd != null) ? "" + sd.getTime() : "");
        taskProps.put (s + PT_EXTERNAL, "");
        taskProps.put (s + PT_WEEKS, "");
        loadSheet (s);
    }



    public void deleteSheet (String s) {
        String oldTaskList = (String)taskProps.get (TASK_KEY);
        String taskList = "";
        String tok;
        Object a;
        boolean first = true;
        boolean rv = true;

        if (oldTaskList != null) {
            StringTokenizer st = new StringTokenizer (oldTaskList, TAB, false);
            while (st.hasMoreTokens())
                if (!s.equals (tok = st.nextToken())) {
                    taskList = (first) ? tok : taskList + TAB + tok;
                    first = false;
                }
        }

        taskProps.put (TASK_KEY, taskList);
        a = taskProps.remove (s + PT_START);
        a = taskProps.remove (s + PT_EXTERNAL);
        a = taskProps.remove (s + PT_WEEKS);
        taskNames.removeItem (s);
        clearTables();
    }



    // return false if combo box should reset to "<none>"
    protected boolean selectTaskSheet () {
        int selIndex = taskNames.getSelectedIndex();
        int newIndex = taskNames.getItemCount() - 2;
        taskSheetName = null;
        boolean rv = false;

        if (selIndex == 0) {        // NO_SELECTION
            System.err.println ("Clearing ... ");
            clearTables();

        } else if (selIndex == newIndex) { // NEW_TOKEN
            String s = JOptionPane.showInputDialog
                (frame,
                 "Enter New Task Sheet Name.",
                 "Create Task Sheet",
                 JOptionPane.PLAIN_MESSAGE);
            if (s != null) {
                System.err.println ("Creating " + s);
                createSheet (s);
            }

        } else if (selIndex == newIndex + 1) { // DELETE_TOKEN
            int nItems = taskNames.getItemCount() - 3;
            Object ar[] = new Object [nItems];
            for (int i = 0; i < nItems; i++)
                ar[i] = taskNames.getItemAt (i + 1);
            String s = (String)JOptionPane.showInputDialog
                (frame,
                 "Select Task Sheet to Delete.",
                 "Delete Task Sheet",
                 JOptionPane.PLAIN_MESSAGE,
                 null,
                 ar,
                 null);
            if (s != null) {
                System.err.println ("Deleting " + s);
                deleteSheet (s);
            }

        } else {                    // an actual selection - load it
            //TBD
            taskSheetName = (String)taskNames.getSelectedItem();
            System.err.println ("Loading " + taskSheetName);
            loadSheet (taskSheetName);
            rv = true;
        }
        calculate();
        return rv;
    }



    private JPanel constructHeaderPanel () {
        JPanel  retPanel = new JPanel(false);
        JButton button;
        JLabel  label;

        label = new JLabel ("Task Sheet:");
        retPanel.add (label);

        taskNames = new JComboBox ();

        taskNames.addItem (NO_SELECTION);
        if (taskProps != null) {    //Add list of available sheets
            String taskList = (String)taskProps.get (TASK_KEY);
            if (taskList != null) {
                StringTokenizer st = new StringTokenizer (taskList,
                                                          TAB,
                                                          false);
                while (st.hasMoreTokens())
                    taskNames.addItem (st.nextToken());
            }
        }
        taskNames.addItem (NEW_TOKEN);
        taskNames.addItem (DELETE_TOKEN);
        taskNames.setSelectedIndex (0);
        //add the listener here
        taskNames.addActionListener (new ActionListener () {
            boolean mine = false;
            public void actionPerformed(ActionEvent e) {
                if (!mine) {
                    if (!selectTaskSheet ()) {
                        mine = true;
                        taskNames.setSelectedIndex (0);
                        mine = false;
                    }
                }
            }
        });
        retPanel.add (taskNames);

        label = new JLabel (" Start Date:");
        retPanel.add (label);

        startDate = new JTextField ("", 10);
        DateChangeAction l = new DateChangeAction (startDate);
        startDate.addActionListener (l);
        startDate.addFocusListener (l);
        retPanel.add (startDate);

        autoCalc = new JCheckBox ("Auto Calculate");
        autoCalc.addActionListener (new ActionListener () {
            public void actionPerformed(ActionEvent e) {
                autoCalculateOn = autoCalc.isSelected();
            }
        });
        retPanel.add (autoCalc);

        button = new JButton ("Calculate!");
        button.addActionListener (new ActionListener () {
            public void actionPerformed(ActionEvent e) { calculate (); }
        });
        retPanel.add (button);

        return retPanel;
    }



    public void addTaskRow (PropertyKey pk) {
        JTable aTable = taskTable.table;
        VTableModel model = (VTableModel)aTable.getModel();

        if (aTable.getEditingRow() != -1)
            aTable.editingStopped (new ChangeEvent("Adding row: Stop edit"));

        Object aRow[];
        TaskEntry te = new TaskEntry ();
        te.taskName = pk;
        scanTask (te);

        aRow = getTaskRow (te, currentTaskLog.size() + 1);
        model.addRow(aRow);
        currentTaskLog.addElement (te);
        addID (pk, taskSheetName, currentTaskLog.size() - 1);
    }



    public void deleteSelectedTaskRow () {
        JTable aTable = taskTable.table;
        VTableModel model = (VTableModel)aTable.getModel();
        int rowBasedOn = aTable.getSelectedRow();
        if (rowBasedOn == -1) {
            if ((rowBasedOn = aTable.getEditingRow()) == -1)
                return;
            else
                aTable.editingStopped (new ChangeEvent("Deleting row: Stop edit"));
        }
        TaskEntry te = (TaskEntry)currentTaskLog.elementAt (rowBasedOn);
        model.removeRow (rowBasedOn);
        try {
            currentTaskLog.removeElementAt (rowBasedOn);
        } catch (Exception e) {}
        if (autoCalculateOn)
            calculate();
        deleteID (te.taskName, taskSheetName, rowBasedOn);
    }



    public void addScheduleRow (long hours) {
        JTable aTable = scheduleTable.table;
        VTableModel model = (VTableModel)aTable.getModel();

        // first try for selected row, else try for editing row, else try last row
        if (aTable.getEditingRow() != -1)
            aTable.editingStopped (new ChangeEvent("Adding row: Stop edit"));

        Object aRow[];
        ScheduleEntry se = new ScheduleEntry (hours * 60);
        long weekNumber = currentScheduleLog.size() + 1;
        Date sd = DateFormatter.parseDate (startDate.getText());
        aRow = getScheduleRow (weekNumber, sd, se);
        model.addRow(aRow);
        currentScheduleLog.addElement (se);
        updateWeek (taskSheetName, (int)weekNumber, hours);
    }



    public void addScheduleRow () {
        JTable aTable = scheduleTable.table;
        VTableModel model = (VTableModel)aTable.getModel();

        // first try for selected row, else try for editing row, else try last row
        int rowBasedOn = currentScheduleLog.size() - 1;
        if (aTable.getEditingRow() != -1)
            aTable.editingStopped (new ChangeEvent("Adding row: Stop edit"));

        Object aRow[];
        ScheduleEntry se;
        long hours = ((rowBasedOn == -1) ? 40 * 60 :
                      ((ScheduleEntry)currentScheduleLog.elementAt
                       (rowBasedOn)).plannedTime);
        se = new ScheduleEntry (hours);

        long weekNumber = currentScheduleLog.size() + 1;
        Date sd = DateFormatter.parseDate (startDate.getText());
        aRow = getScheduleRow (weekNumber, sd, se);
        model.addRow(aRow);
        currentScheduleLog.addElement (se);
        if (autoCalculateOn)
            calculate();
        updateWeek (taskSheetName, (int)weekNumber, hours);
    }



    public void deleteLastScheduleRow () {
        JTable aTable = scheduleTable.table;
        VTableModel model = (VTableModel)aTable.getModel();

        int lastRow = currentScheduleLog.size() - 1;
        if (lastRow == -1)
            return;
        if ((aTable.getEditingRow()) != -1)
            aTable.editingStopped (new ChangeEvent("Deleting row: Stop edit"));

        ScheduleEntry se = (ScheduleEntry)currentScheduleLog.elementAt (lastRow);
        model.removeRow (lastRow);
        try {
            currentScheduleLog.removeElementAt (lastRow);
        } catch (Exception e) {}
        if (autoCalculateOn)
            calculate();
        aTable.validate();
        deleteWeek (taskSheetName, lastRow);
    }



    public void queryAddExternalTask () {
        // (TBD) xxx
        // Bring up file selection dialog to allow selection of a "state" base
        // file.  after selection, read in file to get a list of allowable nodes
        // to select.  Then query as in queryAddTask(?).
    }



    // Note:  It would be nice if long lists were dropdown or at least
    // larger / resizeable, but that will have to wait until other
    // things are taken care of. (TBD)
    public void queryAddTask () {
        if (listOfNodes == null) {
            System.err.println("queryAddTask: node vector is null");
            return;
        }
        Object ar[] = new Object [listOfNodes.size()];
        try {
            listOfNodes.copyInto (ar);
        } catch (Exception ex) {System.err.println("queryAddTask: copy error");}
        Object o = JOptionPane.showInputDialog
            (frame,
             "Select Node to add.",
             "Add Tasking Node(s)",
             JOptionPane.PLAIN_MESSAGE,
             null,
             ar,
             null);
        if (o != null) {
            addTask ((String)o);
        }
    }



    private JPanel constructTaskPanel () {
        JButton button;

        taskTable = new ValidatingTable
            (new Object[] {" # ", "Task",
                           "PT", "PV", "PCT", "PCV", "PDate",
                           "Date", "EV"},
             null,
             new int[] {30, 200,
                        45, 35, 50, 35, 75,
                        75, 35},
             new String[] {"Task Number",                                  // Task #
                           "Task Name - The node the task is attached to", // Task
                           "Planned Time (hours)",                         // PT
                           "Planned Value",                                // PV
                           "Planned Cumulative Time (hours)",              // PCT
                           "Planned Cumulative Value",                     // PCV
                           "Planned Date",                                 // PDate
                           "Actual Date",                                  // Date
                           "Actual Earned Value"},                         // EV
             null, this, TASK_TABLE, true, null,
             new boolean[] {false, false,
                            true, false, false, false, false,
                            true, false});

        JPanel btnPanel = new JPanel(false);
        button = new JButton ("Add External...");
        button.addActionListener (new ActionListener () {
            public void actionPerformed(ActionEvent e) { queryAddExternalTask (); }
        });
        btnPanel.add (button);

        button = new JButton ("Add...");
        button.addActionListener(new ActionListener () {
            public void actionPerformed(ActionEvent e) { queryAddTask (); }
        });
        btnPanel.add (button);

        button = new JButton ("Delete");
        button.addActionListener (new ActionListener () {
            public void actionPerformed(ActionEvent e) { deleteSelectedTaskRow(); }
        });
        btnPanel.add (button);

        JPanel retPanel = new JPanel(false);
        retPanel.setLayout(new BorderLayout());
        retPanel.add ("Center", taskTable);
        retPanel.add ("South", btnPanel);

        Dimension d = new Dimension (taskTable.getPreferredSize());
        d.setSize (d.width,
                   d.height + (2 * taskTable.table.getRowHeight()) +
                   btnPanel.getPreferredSize().height + 5);
        retPanel.setMinimumSize (d);
        d.setSize (d.width,
                   d.height + (Math.max (taskTable.table.getRowCount() - 2, 0) *
                               taskTable.table.getRowHeight()));
        retPanel.setPreferredSize (d);

        return retPanel;
    }



    private JPanel constructSchedulePanel () {
        JButton button;

        scheduleTable = new ValidatingTable
            (new Object[] {"Week #", "Date",
                           "PT", "PCT", "PCV",
                           "Time", "CT", "CEV"},
             null,
             new int[] {50, 100,
                        45, 50, 35,
                        60, 50, 35},
             new String[] {"Week Number",                        // Week #
                           "Date of first day in week (Monday)", // Task
                           "Planned Time (hours)",               // PT
                           "Planned Cumulative Time (hours)",    // PCT
                           "Planned Cumulative Value",           // PCV
                           "Actual Time (hours)",                // PDate
                           "Actual Cumulative Time (hours)",     // Date
                           "Actual Cumulative Earned Value"},    // EV
             null, this, SCHEDULE_TABLE, true, null,
             new boolean[] {false, false,
                            true, false, false,
                            false, false, false});

        JPanel btnPanel = new JPanel(false);
        button = new JButton ("Add");
        button.addActionListener (new ActionListener () {
            public void actionPerformed(ActionEvent e) { addScheduleRow(); }
        });
        btnPanel.add (button);

        button = new JButton ("Delete Last");
        button.addActionListener (new ActionListener () {
            public void actionPerformed(ActionEvent e) {
                deleteLastScheduleRow();
            }
        });
        btnPanel.add (button);

        JPanel  retPanel = new JPanel(false);
        retPanel.setLayout(new BorderLayout());
        retPanel.add ("Center", scheduleTable);
        retPanel.add ("South", btnPanel);

        Dimension d = new Dimension (scheduleTable.getPreferredSize());
        d.setSize (d.width,
                   d.height + (2 * scheduleTable.table.getRowHeight()) +
                   btnPanel.getPreferredSize().height + 5);
        retPanel.setMinimumSize (d);
        d.setSize(d.width,
                  d.height + (Math.max (scheduleTable.table.getRowCount() - 2, 0) *
                              scheduleTable.table.getRowHeight()));
        retPanel.setPreferredSize (d);

        return retPanel;
    }



    private JPanel constructControlPanel () {
        JPanel  retPanel = new JPanel(false);
        JButton button;

        button = new JButton ("Print");
        retPanel.add (button);
//    button.addActionListener (new PrintAction ());

        button = new JButton ("Chart");
        retPanel.add (button);
//    button.addActionListener (new ChartAction ());

        button = new JButton ("Close");
        retPanel.add (button);
        button.addActionListener(new CloseAction());

        return retPanel;
    }



    public void show() {
        try {
            tl.read (dashboard.getTimeLog());
        } catch (IOException e) {}
        frame.show();
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
            return;
        }
        Object [] path = tp.getPath();
        PropertyKey key = treeModel.getPropKey (useProps, path);

        Prop val = useProps.pget (key);
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
                    if (autoCalculateOn)
                        calculate();
                }
            } else {
                widget.setBackground (background);
                text = DateFormatter.formatDate (d);
                widget.setText (text);
                if (autoCalculateOn)
                    calculate();
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

    } // End of TaskTemplate.DateChangeAction


    //
    // CloseAction responds to user clicking Close button.
    //
    class CloseAction extends Object implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            //if editing, stop edits
            if (taskTable.table.isEditing())
                taskTable.table.editingStopped
                    (new ChangeEvent("Saving Data: Stop edit"));
            if (scheduleTable.table.isEditing())
                scheduleTable.table.editingStopped
                    (new ChangeEvent("Saving Data: Stop edit"));
                                          // save the task & schedule logs
            save();
            frame.setVisible (false);
        }
    } // End of TaskTemplate.CloseAction


}
