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

import java.awt.event.*;
import java.io.IOException;
import java.util.*;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;
import javax.swing.tree.TreePath;

import pspdash.data.DataRepository;
import pspdash.data.DataComparator;
import pspdash.data.DoubleData;
import pspdash.data.StringData;
import pspdash.data.SimpleData;
import pspdash.data.ListData;

public class EVTaskList extends AbstractTreeTableModel
    implements EVTask.Listener, ActionListener, PSPProperties.Listener
{

    public static final String MAIN_DATA_PREFIX = "/Task-Schedule/";
    public static final String TASK_ORDINAL_PREFIX = "TST_";
    public static final String EST_HOURS_DATA_NAME = "Planned Hours";
    public static final String TASK_LISTS_DATA_NAME = "Task Lists";


    protected String taskListName;
    protected DataRepository data;
    protected PSPProperties hierarchy;
    protected EVSchedule schedule;
    protected Vector evTaskLists = null;
    /** timer for triggering recalculations */
    protected Timer recalcTimer;

    /**
     * @param taskListName the name of the task list. This will be a simple
     *    string, not containing any '/' characters.
     */
    public EVTaskList(String taskListName,
                      DataRepository data,
                      PSPProperties hierarchy,
                      boolean createRollup,
                      boolean willNeedChangeNotification) {
        super(null);

        this.taskListName = taskListName;
        this.data = data;
        this.hierarchy = hierarchy;
        if (willNeedChangeNotification) {
            recalcListeners = Collections.synchronizedSet(new HashSet());

            recalcTimer = new Timer(Integer.MAX_VALUE, this);
            recalcTimer.setInitialDelay(1000);
            recalcTimer.setRepeats(false);
        }

        root = new EVTask(taskListName);

        if (isRollup(data, taskListName) ||
            (createRollup && !isPlain(data, taskListName))) {
            evTaskLists = new Vector();
            addTaskListsFromData(data, hierarchy, taskListName);
            schedule = new EVScheduleRollup(evTaskLists);
        } else {
            addTasksFromData(data, taskListName);
            schedule = getSchedule(data, taskListName);
        }
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
        boolean willNeedChangeNotification = (recalcListeners != null);
        while (i.hasNext())
            addTask((String) i.next(), data, hierarchy,
                    willNeedChangeNotification);

        hierarchy.addHierarchyListener(this);
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

    private void addTaskListsFromData(DataRepository data,
                                      PSPProperties hierarchy,
                                      String taskListName) {
        String globalPrefix = MAIN_DATA_PREFIX + taskListName;
        String dataName =
            data.createDataName(globalPrefix, TASK_LISTS_DATA_NAME);
        SimpleData listVal = data.getSimpleValue(dataName);
        ListData list = null;
        if (listVal instanceof ListData)
            list = (ListData) listVal;
        else if (listVal instanceof StringData)
            list = ((StringData) listVal).asList();

        if (list == null) return;
        for (int i = 0;   i < list.size();   i++) {
            taskListName = (String) list.get(i);
            EVTaskList taskList =
                new EVTaskList(taskListName, data, hierarchy, false, false);
            ((EVTask) root).add((EVTask) taskList.root);
            evTaskLists.add(taskList);
        }
    }


    public void save() { save(taskListName); }

    public void save(String newName) {
        if (isRollup())
            saveRollup(newName);
        else
            savePlain(newName);
    }

    protected void savePlain(String newName) {
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
        if (newName != null) {
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
            taskListName = newName;
        }

        // Finally, delete any old unused data elements.
        i = oldNames.iterator();
        while (i.hasNext())
            data.removeValue((String) i.next());
    }

    protected void saveRollup(String newName) {
        String dataName;

        // First, erase the data element that used to hold the list of
        // task lists.
        if (!taskListName.equals(newName)) {
            dataName = data.createDataName(MAIN_DATA_PREFIX + taskListName,
                                           TASK_LISTS_DATA_NAME);
            data.putValue(dataName, null);
        }

        // Now, save the rollup to the repository with the new name.
        if (newName != null) {
            dataName = data.createDataName(MAIN_DATA_PREFIX + newName,
                                           TASK_LISTS_DATA_NAME);
            ListData list = new ListData();
            Iterator i = evTaskLists.iterator();
            while (i.hasNext())
                list.add(((EVTaskList) i.next()).taskListName);

            data.putValue(dataName, list);
            taskListName = newName;
        }
    }



    public static String[] findTaskLists(DataRepository data) {
        return findTaskLists(data, false);
    }
    public static String[] findTaskLists(DataRepository data,
                                         boolean excludeRollups) {
        TreeSet result = new TreeSet();
        Iterator i = data.getKeys();
        String dataName;
        while (i.hasNext()) {
            dataName = (String) i.next();
            if (dataName.startsWith(MAIN_DATA_PREFIX)) {
                if (excludeRollups && dataName.endsWith(TASK_LISTS_DATA_NAME))
                    continue;
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



    public boolean isRollup() {
        return evTaskLists != null;
    }

    public static boolean isRollup(DataRepository data, String taskListName) {
        String dataName = data.createDataName(MAIN_DATA_PREFIX + taskListName,
                                              TASK_LISTS_DATA_NAME);
        return data.getSimpleValue(dataName) != null;
    }
    public static boolean isPlain(DataRepository data, String taskListName) {
        String dataName = data.createDataName(MAIN_DATA_PREFIX + taskListName,
                                              EST_HOURS_DATA_NAME);
        return data.getSimpleValue(dataName) != null;
    }

    public static String cleanupName(String taskListDataName) {
        // Strip all initial text up to and including the "main data prefix."
        int pos = taskListDataName.indexOf(MAIN_DATA_PREFIX);
        if (pos != -1)
            taskListDataName = taskListDataName.substring
                (pos + MAIN_DATA_PREFIX.length());

        // Strip all final text following the "/" character.
        pos = taskListDataName.indexOf('/');
        if (pos != -1)
            taskListDataName = taskListDataName.substring(0, pos);

        return taskListDataName;
    }


    public EVSchedule getSchedule() { return schedule; }

    public boolean addTask(String path,
                        DataRepository data,
                        PSPProperties hierarchy,
                        boolean willNeedChangeNotification) {
        if (path == null || path.length() == 0) return false;

        // create the new task and add it.
        EVTask newTask;
        if (isRollup()) {
            EVTaskList taskList = new EVTaskList(path, data, hierarchy,
                                                 false, false);
            evTaskLists.add(taskList);
            ((EVScheduleRollup) schedule).addSchedule(taskList.schedule);
            newTask = (EVTask) taskList.root;
        } else {
            newTask = new EVTask(path, data, hierarchy,
                                 willNeedChangeNotification ? this : null);
        }
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

        if (isRollup()) {
            EVTaskList taskList = (EVTaskList) evTaskLists.remove(pos);
            ((EVScheduleRollup) schedule).removeSchedule(taskList.schedule);
        }
        child.destroy();

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
        if (isRollup()) {
            Object taskList = evTaskLists.remove(pos);
            evTaskLists.insertElementAt(taskList, pos-1);
        }

        // send the appropriate TreeModel event.
        int[] childIndices = new int[] { pos-1, pos };
        Object[] children = new Object[]{ r.getChild(pos-1), r.getChild(pos) };
        fireTreeStructureChanged(this, r.getPath(), childIndices, children);
        return true;
    }


    //////////////////////////////////////////////////////////////////////
    /// Change notification support
    //////////////////////////////////////////////////////////////////////

    EVTask.Listener evNodeListener = null;
    public void setNodeListener(EVTask.Listener l) { evNodeListener = l; }
    public void evNodeChanged(EVTask node) {
        if (evNodeListener != null) evNodeListener.evNodeChanged(node);
        if (recalcTimer    != null) recalcTimer.restart();
    }

    /** Defines the interface for an object that listens for recalculations
     *  that occur in an EVTaskList.
     *
     * Note: this only allows for notification of recalculation
     * events - not to the structure of the task list.  To be
     * notified of such changes, register as a TreeModelListener.
     */
    public interface RecalcListener {
        /** The task list has been recalculated. */
        public void evRecalculated(EventObject e);
    }
    Set recalcListeners = null;
    public synchronized void addRecalcListener(RecalcListener l) {
        if (recalcListeners != null)
            recalcListeners.add(l);
    }
    public void removeRecalcListener(RecalcListener l) {
        if (recalcListeners != null) {
            recalcListeners.remove(l);
            maybeDispose();
        }
    }
    private boolean someoneCares() {
        return (recalcListeners != null && !recalcListeners.isEmpty());
    }
    protected void fireEvRecalculated() {
        if (someoneCares()) {
            EventObject e = new EventObject(this);
            Iterator i = recalcListeners.iterator();
            while (i.hasNext())
                ((RecalcListener) i.next()).evRecalculated(e);
        }
    }
    public void actionPerformed(ActionEvent e) {
        if (recalcTimer != null && e.getSource() == recalcTimer &&
            someoneCares())
            recalc();
    }

    private void maybeDispose() {
        if (recalcListeners == null) return;
        if (recalcListeners.isEmpty()) {
            System.out.println("disposing!");
            hierarchy.removeHierarchyListener(this);
            ((EVTask) root).destroy();
        }
    }
    public void hierarchyChanged(PSPProperties.Event e) {
        if (someoneCares()) {
            EVTask r = (EVTask) root;

            // delete all the previous children.
            int n = r.getNumChildren();
            int[] childIndices = new int[n];
            Object[] children = new Object[n];
            while (n-- > 0)
                children[(childIndices[n] = n)] = r.getChild(n);
            r.destroy();
            fireTreeNodesRemoved
                (this, ((EVTask) r).getPath(), childIndices, children);

            // add the new kids.
            addTasksFromData(data, taskListName);
            fireTreeStructureChanged(this, r.getPath(), null, null);
            recalc();
        }
    }




    private double totalPlanTime;
    public void recalc() {
        System.out.println("recalculating " + taskListName);
        if (isRollup())
            recalcRollup();
        else
            recalcSimple();

        totalPlanTime = schedule.getMetrics().totalPlan();
        fireEvRecalculated();
    }

    protected void recalcSimple() {
        TimeLog log = new TimeLog();
        try { log.readDefault(); } catch (IOException ioe) {}
        ((EVTask) root).recalc(schedule, log);
    }

    protected void recalcRollup() {
        // Recalculate all the subschedules.
        Iterator i = evTaskLists.iterator();
        while (i.hasNext())
            ((EVTaskList) i.next()).recalc();

        // Recalculate the root node.
        ((EVTask) root).recalcRollupNode();

        // Recalculate the rollup schedule.
        ((EVScheduleRollup) schedule).recalc();
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
        case PLAN_VALUE_COLUMN:     return n.getPlanValue(totalPlanTime);
        case PLAN_CUM_TIME_COLUMN:  return n.getCumPlanTime();
        case PLAN_CUM_VALUE_COLUMN: return n.getCumPlanValue(totalPlanTime);
        case PLAN_DATE_COLUMN:      return n.getPlanDate();
        case DATE_COMPLETE_COLUMN:  return n.getActualDate();
        case VALUE_EARNED_COLUMN:   return n.getValueEarned(totalPlanTime);
        }
        return null;
    }

    /** Set the value at a particular row/column */
    public void setValueAt(Object aValue, Object node, int column) {
        //System.out.println("setValueAt("+aValue+","+node+","+column+")");
        EVTask n = (EVTask) node;
        switch (column) {
        case PLAN_TIME_COLUMN:      n.setPlanTime(aValue);   break;
        case DATE_COMPLETE_COLUMN:  n.setActualDate(aValue); break;
        }
    }

    public TableModel getSimpleTableModel() {
        return new SimpleTableModel();
    }

    private class SimpleTableModel implements TableModel {
        public int getRowCount() { return countRows((EVTask) root); }
        public int getColumnCount() {
            return EVTaskList.this.getColumnCount(); }
        public String getColumnName(int columnIndex) {
            return EVTaskList.this.getColumnName(columnIndex); }
        public Class getColumnClass(int columnIndex) {
            return EVTaskList.this.getColumnClass(columnIndex); }
        public boolean isCellEditable(int row, int columnIndex) {
            return EVTaskList.this.isCellEditable(getRow(row), columnIndex); }
        public Object getValueAt(int row, int columnIndex) {
            return (columnIndex == 0 ?
                    getRow(row).getFullName() :
                    EVTaskList.this.getValueAt(getRow(row), columnIndex)); }
        public void setValueAt(Object aValue, int row, int columnIndex) {
            EVTaskList.this.setValueAt(aValue, getRow(row), columnIndex); }
        public void addTableModelListener(TableModelListener l) { }
        public void removeTableModelListener(TableModelListener l) { }

        private int countRows(EVTask node) {
            int result = 0;
            if (node.isLeaf())
                result = 1;
            else
                for (int i = node.getNumChildren();   i-- > 0; )
                    result += countRows(node.getChild(i));
            return result;
        }

        private EVTask getRow(int row) {
            RowFinder f = new RowFinder((EVTask) root, row);
            return f.getResult();
        }

        private class RowFinder {
            int rowToReturn, rowsSeen = 0;
            EVTask result = null;
            public RowFinder(EVTask node, int rowToReturn) {
                this.rowToReturn = rowToReturn;
                find(node);
            }
            public EVTask getResult() { return result; }
            private void find(EVTask node) {
                if (result != null)
                    return;
                else if (node.isLeaf()) {
                    if (rowToReturn == rowsSeen) {
                        result = node;
                        return;
                    } else
                        rowsSeen++;
                } else {
                    for (int i = 0;  i < node.getNumChildren();  i++)
                        if (result == null)
                            find(node.getChild(i));
                }
            }
        }
    }

    public void finalize() throws Throwable {
        System.out.println("finalizing EVTaskList " + taskListName);
        super.finalize();
    }

}
