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

import java.io.IOException;
import java.util.*;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.*;
import javax.swing.tree.TreePath;

import pspdash.data.DataRepository;
import pspdash.data.DataComparator;
import pspdash.data.DoubleData;
import pspdash.data.StringData;
import pspdash.data.SimpleData;
import pspdash.data.ListData;

public class EVTaskList extends AbstractTreeTableModel
    implements EVTask.Listener
{

    public static final String MAIN_DATA_PREFIX = "/Task-Schedule/";
    public static final String TASK_ORDINAL_PREFIX = "TST_";
    public static final String EST_HOURS_DATA_NAME = "Planned Hours";


    protected String taskListName;
    protected DataRepository data;
    protected PSPProperties hierarchy;
    protected EVSchedule schedule;

    public EVTaskList(String taskListName,
                      DataRepository data,
                      PSPProperties hierarchy,
                      boolean willNeedChangeNotification) {
        super(null);

        this.taskListName = taskListName;
        this.data = data;
        this.hierarchy = hierarchy;
        if (willNeedChangeNotification)
            listeners = Collections.synchronizedList(new ArrayList());
        else
            listeners = null;

        root = new EVTask(taskListName);
        addTasksFromData(data, taskListName);
        schedule = getSchedule(data, taskListName);
    }

    private void addTasksFromData(DataRepository data, String taskListName) {
        // search for tasks that belong to the named task list.
        SortedMap tasks = new TreeMap(DataComparator.instance);
        String ordinalPrefix = "/" + TASK_ORDINAL_PREFIX + taskListName;
        Iterator i = data.getKeys();
        String dataName, path;
        SimpleData value;
        while (i.hasNext()) {
            dataName = (String) i.next();
            if (!dataName.endsWith(ordinalPrefix)) continue;
            value = data.getSimpleValue(dataName);
            path = dataName.substring
                (0, dataName.length() - ordinalPrefix.length());
            tasks.put(value, path);
        }

        // now add each task found to the task list.
        i = tasks.values().iterator();
        boolean willNeedChangeNotification = (listeners != null);
        while (i.hasNext())
            addTask((String) i.next(), data, hierarchy,
                    willNeedChangeNotification);
    }
    private EVSchedule getSchedule(DataRepository data, String taskListName) {
        String globalPrefix = MAIN_DATA_PREFIX + taskListName;
        String dataName =
            data.createDataName(globalPrefix, EST_HOURS_DATA_NAME);
        SimpleData d = data.getSimpleValue(dataName);
        if (d instanceof StringData) d = ((StringData) d).asList();
        if (d instanceof ListData)
            return new EVSchedule((ListData) d);
        else
            return new EVSchedule();
    }


    public void save() { save(taskListName); }

    public void save(String newName) {
        // First, compile a list of all the elements in the datafile that
        // were previously used to save this task list.  (That way we'll
        // know what we need to delete.)
        String globalPrefix = MAIN_DATA_PREFIX + taskListName;
        String ordinalPrefix = "/" + TASK_ORDINAL_PREFIX + taskListName;
        Iterator i = data.getKeys();
        Set oldNames = new HashSet();
        String dataName;
        while (i.hasNext()) {
            dataName = (String) i.next();
            if (dataName.startsWith(globalPrefix) ||
                dataName.endsWith(ordinalPrefix))
                oldNames.add(dataName);
        }

        // Now, save the data to the repository.
        globalPrefix = MAIN_DATA_PREFIX + newName;
        ordinalPrefix = TASK_ORDINAL_PREFIX + newName;
        EVTask r = (EVTask) root;
        for (int j = r.getNumChildren();  j-- > 0;  ) {
            dataName = data.createDataName(r.getChild(j).getFullName(),
                                           ordinalPrefix);
            data.putValue(dataName, new DoubleData(j, false));
            oldNames.remove(dataName);
        }
        dataName = data.createDataName(globalPrefix, EST_HOURS_DATA_NAME);
        data.putValue(dataName, schedule.getSaveList());
        oldNames.remove(dataName);

        // Finally, delete any old unused data elements.
        i = oldNames.iterator();
        while (i.hasNext())
            data.removeValue((String) i.next());
    }

    public static String[] findTaskLists(DataRepository data) {
        TreeSet result = new TreeSet();
        Iterator i = data.getKeys();
        String dataName;
        while (i.hasNext()) {
            dataName = (String) i.next();
            if (dataName.startsWith(MAIN_DATA_PREFIX)) {
                dataName = dataName.substring(MAIN_DATA_PREFIX.length());
                int slashPos = dataName.indexOf('/');
                dataName = dataName.substring(0, slashPos);
                result.add(dataName);
            }
        }

        String[] ret = new String[result.size()];
        i = result.iterator();
        int j = 0;
        while (i.hasNext())
            ret[j++] = (String) i.next();
        return ret;
    }

    public EVSchedule getSchedule() { return schedule; }

    public boolean addTask(String path,
                        DataRepository data,
                        PSPProperties hierarchy,
                        boolean willNeedChangeNotification) {
        if (path == null || path.length() == 0) return false;

        // create the new task and add it.
        EVTask newTask = new EVTask(path, data, hierarchy,
                                    willNeedChangeNotification ? this : null);
        ((EVTask) root).add(newTask);

        // send the appropriate TreeModel event.
        int[] childIndices = new int[] { ((EVTask)root).getNumChildren() - 1 };
        Object[] children = new Object[] { newTask };
        fireTreeNodesInserted
            (this, ((EVTask) root).getPath(), childIndices, children);
        return true;
    }

    public boolean removeTask(TreePath path) {
        // for now, only remove tasks which are children of the root.
        int pathLen = path.getPathCount();
        if (pathLen != 2) return false;

        EVTask parent = (EVTask) path.getPathComponent(pathLen-2);
        EVTask child  = (EVTask) path.getPathComponent(pathLen-1);
        int pos = parent.remove(child);

        // send the appropriate TreeModel event.
        int[] childIndices = new int[] { pos };
        Object[] children = new Object[] { child };
        fireTreeNodesRemoved
            (this, ((EVTask) parent).getPath(), childIndices, children);
        return true;
    }

    public boolean moveTaskUp(int pos) {
        EVTask r = (EVTask) root;
        if (pos < 1 || pos >= r.getNumChildren()) return false;

        // make the change
        r.moveUp(pos);

        // send the appropriate TreeModel event.
        int[] childIndices = new int[] { pos-1, pos };
        Object[] children = new Object[]{ r.getChild(pos-1), r.getChild(pos) };
        fireTreeStructureChanged(this, r.getPath(), childIndices, children);
        return true;
    }


    //////////////////////////////////////////////////////////////////////
    /// Change notification support
    //////////////////////////////////////////////////////////////////////

    /** Defines the interface for an object that listens to changes in
     * the <b>values</b> in an EVTaskList.
     *
     * Note: this only allows for notification of changes to values in
     * the task list - not to the structure of the task list.  To be
     * notified of such changes, register as a TreeModelListener.
     */
    public interface Listener {
        /** Data has changed for one node in the task list */
        public void evNodeChanged(Event e);
    }

    /** Notification event object */
    public class Event {
        private EVTask node;
        public Event(EVTask node) { this.node = node; }
        public EVTask getNode() { return node; }
        public EVTaskList getList() { return EVTaskList.this; }
    }

    List listeners = null;
    public void addEVTaskListListener(Listener l)    { listeners.add(l);    }
    public void removeEVTaskListListener(Listener l) { listeners.remove(l); }
    public void fireEVNodeChanged(EVTask node) {
        if (listeners != null  && listeners.size() > 0) {
            Event e = new Event(node);
            for (int i = 0;  i < listeners.size();  i++)
                ((Listener) listeners.get(i)).evNodeChanged(e);
        }
    }
    public void recalc() {
        TimeLog log = new TimeLog();
        try { log.readDefault(); } catch (IOException ioe) {}
        ((EVTask) root).recalc(schedule, log);
    }



    //////////////////////////////////////////////////////////////////////
    /// TreeTableModel support
    //////////////////////////////////////////////////////////////////////


    /** Names of the columns in the TreeTableModel. */
    protected static String[] colNames = { "Project/Task",
        "PT", "Time", "PV", "CPT", "CPV", "Plan Date", "Date", "EV" };
    public static int[] colWidths = { 175,
        50,   50,     40,   50,    40,    80,          80,     40 };
    public static String[] toolTips = {
        null,
        "Planned Time (hours:minutes)",
        "Actual Time (hours:minutes)",
        "Planned Value",
        "Cumulative Planned Time (hours:minutes)",
        "Cumulative Planned Value",
        "Planned Completion Date",
        "Actual Completion Date",
        "Actual Earned Value" };

    protected static final int TASK_COLUMN           = 0;
    protected static final int PLAN_TIME_COLUMN      = 1;
    protected static final int ACT_TIME_COLUMN       = 2;
    protected static final int PLAN_VALUE_COLUMN     = 3;
    protected static final int PLAN_CUM_TIME_COLUMN  = 4;
    protected static final int PLAN_CUM_VALUE_COLUMN = 5;
    protected static final int PLAN_DATE_COLUMN      = 6;
    protected static final int DATE_COMPLETE_COLUMN  = 7;
    protected static final int VALUE_EARNED_COLUMN   = 8;

    /** Types of the columns in the TreeTableModel. */
    static protected Class[]  colTypes = {
        TreeTableModel.class,   // project/task
        String.class,           // planned time
        String.class,           // actual time
        String.class,           // planned value
        String.class,           // planned cumulative time
        String.class,           // planned cumulative value
        Date.class,             // planned date
        Date.class,             // date
        String.class };         // earned value


    //
    // The TreeModel interface
    //

    /** Returns the number of children of <code>node</code>. */
    public int getChildCount(Object node) {
        return ((EVTask) node).getNumChildren();
    }

    /** Returns the child of <code>node</code> at index <code>i</code>. */
    public Object getChild(Object node, int i) {
        return ((EVTask) node).getChild(i);
    }

    /** Returns true if the passed in object represents a leaf, false
     *  otherwise. */
    public boolean isLeaf(Object node) {
        return ((EVTask) node).isLeaf();
    }

    /** Returns true if the value in column <code>column</code> of object
     *  <code>node</code> is editable. */
    public boolean isCellEditable(Object node, int column) {
        switch (column) {
        case TASK_COLUMN:
            // The column with the tree in it should be editable; this
            // causes the JTable to forward mouse and keyboard events
            // in the Tree column to the underlying JTree.
            return true;

        case PLAN_TIME_COLUMN:
            return ((EVTask) node).plannedTimeIsEditable();

        case DATE_COMPLETE_COLUMN:
            return ((EVTask) node).completionDateIsEditable();
        }
        return false;
    }


    //
    //  The TreeTableNode interface.
    //

    /** Returns the number of columns. */
    public int getColumnCount() { return colNames.length; }

    /** Returns the name for a particular column. */
    public String getColumnName(int column) { return colNames[column]; }

    /** Returns the class for the particular column. */
    public Class getColumnClass(int column) { return colTypes[column]; }

    /** Returns the value of the particular column. */
    public Object getValueAt(Object node, int column) {
        EVTask n = (EVTask) node;
        switch (column) {
        case TASK_COLUMN:           return n.getName();
        case PLAN_TIME_COLUMN:      return n.getPlanTime();
        case ACT_TIME_COLUMN:       return n.getActualTime();
        case PLAN_VALUE_COLUMN:     return n.getPlanValue();
        case PLAN_CUM_TIME_COLUMN:  return n.getCumPlanTime();
        case PLAN_CUM_VALUE_COLUMN: return n.getCumPlanValue();
        case PLAN_DATE_COLUMN:      return n.getPlanDate();
        case DATE_COMPLETE_COLUMN:  return n.getActualDate();
        case VALUE_EARNED_COLUMN:   return n.getValueEarned();
        }
        return null;
    }

    /** Set the value at a particular row/column */
    public void setValueAt(Object aValue, Object node, int column) {
        System.out.println("setValueAt("+aValue+","+node+","+column+")");
        EVTask n = (EVTask) node;
        switch (column) {
        case PLAN_TIME_COLUMN:      n.setPlanTime(aValue);   break;
        case DATE_COMPLETE_COLUMN:  n.setActualDate(aValue); break;
        }
    }

}
