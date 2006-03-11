// Process Dashboard - Data Automation Tool for high-maturity processes
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
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net


package net.sourceforge.processdash.ev;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.Timer;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.table.TableModel;
import javax.swing.tree.TreePath;

import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.cache.ObjectCache;
import net.sourceforge.processdash.ui.lib.AbstractTreeTableModel;
import net.sourceforge.processdash.ui.lib.TreeTableModel;


public class EVTaskList extends AbstractTreeTableModel
    implements EVTask.Listener, ActionListener
{

    public static final String EV_TASK_LIST_ELEMENT_NAME = "EVModel";
    public static final String MAIN_DATA_PREFIX = "/Task-Schedule/";
    static Resources resources = Resources.getDashBundle("EV");


    protected String taskListName;
    protected String taskListID = null;
    protected EVSchedule schedule;
    protected EVCalculator calculator;
    protected EVDependencyCalculator dependencyCalculator = null;

    /** timer for triggering recalculations */
    protected Timer recalcTimer = null;

    protected double totalPlanValue;
    protected double totalActualTime;
    protected boolean showDirectTimeColumns;


    protected EVTaskList(String taskListName,
                         String displayName,
                         boolean willNeedChangeNotification) {
        super(null);

        this.taskListName = taskListName;

        if (willNeedChangeNotification) {
            recalcListeners = Collections.synchronizedSet(new HashSet());

            recalcTimer = new Timer(Integer.MAX_VALUE, this);
            recalcTimer.setInitialDelay(1000);
            recalcTimer.setRepeats(false);
        }

        if (displayName != null)
            root = new EVTask(taskListName);
    }

    public EVTaskList(String taskListName,
                      String displayName,
                      String errorMessage) {
        this(taskListName, null, false);
        createErrorRootNode(displayName, errorMessage);
    }


    protected void createErrorRootNode(String displayName,
                                       String errorMessage) {
        if (displayName == null || displayName.length() == 0)
            displayName = resources.getString
                ("TaskList.Default_Error_Root_Node_Name");
        root = new EVTask(displayName);
        schedule = new EVSchedule(0.0);
        ((EVTask) root).setTaskError(errorMessage);
        schedule.getMetrics().addError(errorMessage, (EVTask) root);
    }


    public static String[] findTaskLists(DataRepository data) {
        return findTaskLists(data, false, false);
    }
    public static String[] findTaskLists(DataRepository data,
                                         boolean excludeRollups,
                                         boolean includeImports) {
        /*
            let findTaskLists return a full path to imported schedules.
            save that full path in rollup task list names.
            make the name pretty when displaying it to the user.

         */
        TreeSet result = new TreeSet();
        Iterator i = data.getKeys();
        String dataName, taskListName;
        while (i.hasNext()) {
            dataName = (String) i.next();

            // see if this data name defines a regular task list.
            taskListName =
                EVTaskListData.taskListNameFromDataElement(dataName);

            // if that failed, maybe see if this defines a rollup.
            if (taskListName == null && !excludeRollups)
                taskListName =
                    EVTaskListRollup.taskListNameFromDataElement(dataName);

            // if that failed, maybe see if this defines an imported list.
            if (taskListName == null && includeImports)
                taskListName =
                    EVTaskListXML.taskListNameFromDataElement(data, dataName);

            // if any of the tests succeeded, add the name to the list.
            if (taskListName != null)
                result.add(taskListName);
        }

        String[] ret = new String[result.size()];
        i = result.iterator();
        int j = 0;
        while (i.hasNext())
            ret[j++] = (String) i.next();
        return ret;
    }

    /** Finds and opens an existing task list.
     *  @param taskListName the symbolic name of the task list.
     *  @param data a reference to the data repository.
     *  @param hierarchy a reference to the user's hierarchy.
     *  @param willNeedChangeNotification should the returned task list
     *     support change notifications?
     *  @return the named task list, or null if no list by that name is found.
     */
    public static EVTaskList openExisting(String taskListName,
                                          DataRepository data,
                                          DashHierarchy hierarchy,
                                          ObjectCache cache,
                                          boolean willNeedChangeNotification)
    {
        // most common case: open a regular task list
        if (EVTaskListData.validName(taskListName) &&
            EVTaskListData.exists(data, taskListName))
            return new EVTaskListData(taskListName, data, hierarchy,
                                      willNeedChangeNotification);

        // next most common case: open a rollup task list
        if (EVTaskListRollup.validName(taskListName) &&
            EVTaskListRollup.exists(data, taskListName))
            return new EVTaskListRollup(taskListName, data, hierarchy, cache,
                                        willNeedChangeNotification);

        // open an cached imported XML task list.
        if (EVTaskListCached.validName(taskListName) &&
            EVTaskListCached.exists(taskListName, cache))
            return new EVTaskListCached(taskListName, cache);

        // open an imported XML task list
        if (EVTaskListXML.validName(taskListName) &&
            EVTaskListXML.exists(data, taskListName))
            return new EVTaskListXML(taskListName, data);

        // for testing purposes, return a cached list
        if (taskListName.equals(TESTING_TASK_LIST_NAME))
            return TESTING_TASK_LIST;

        // no task list was found.
        return null;
    }

    public static EVTaskList open(String taskListName,
                                  DataRepository data,
                                  DashHierarchy hierarchy,
                                  ObjectCache cache,
                                  boolean willNeedChangeNotification)
    {
        EVTaskList result = openExisting
            (taskListName, data, hierarchy, cache, willNeedChangeNotification);

        if (result == null && EVTaskListData.validName(taskListName))
            result = new EVTaskListData
                (taskListName, data, hierarchy, willNeedChangeNotification);

        if (result == null && EVTaskListXML.validName(taskListName))
            result = new EVTaskListXML(taskListName, data);

        if (result == null)
            result = new EVTaskList
                (taskListName, getDisplayName(taskListName),
                 resources.getString
                 ("TaskList.Invalid_Schedule_Error_Message"));

        return result;
    }

    private static String TESTING_TASK_LIST_NAME = null;
    private static EVTaskList TESTING_TASK_LIST = null;
    public static void setTestingTaskList(String name, EVTaskList taskList) {
        TESTING_TASK_LIST_NAME = name;
        TESTING_TASK_LIST = taskList;
    }


    public String getDisplayName() {
        return getDisplayName(taskListName);
    }

    public static String getDisplayName(String taskListName) {
        int slashPos = taskListName.lastIndexOf('/');
        if (slashPos == -1)
            return taskListName;
        else
            return taskListName.substring(slashPos+1);
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

    public boolean isEmpty() { return ((EVTask) root).isLeaf(); }
    public boolean isEditable() { return false; }
    public String getRootName() { return ((EVTask) root).name; }
    public String getID() { return taskListID; }


    public void save() { save(taskListName); }
    public void save(String newName) {}

    public String getAsXML() {
        return getAsXML(false);
    }
    public String getAsXML(boolean whitespace) {
        String newline = (whitespace ? "\n" : "");
        String indent = (whitespace ? "  " : "");
        StringBuffer result = new StringBuffer();
        result.append("<").append(EV_TASK_LIST_ELEMENT_NAME).append(" rct='")
            .append(calculator.reorderCompletedTasks);
        if (getID() != null)
            result.append("' tlid='").append(getID());
        result.append("'>").append(newline);
        ((EVTask) root).saveToXML(result, whitespace);
        schedule.saveToXML(result, whitespace);
        result.append("</").append(EV_TASK_LIST_ELEMENT_NAME).append(">");

        return result.toString();
    }


    public boolean addTask(String path,
                           DataRepository data,
                           DashHierarchy hierarchy,
                           ObjectCache cache,
                           boolean willNeedChangeNotification) {
        if (path == null || path.length() == 0 || !isEditable()) return false;

        EVTask newTask = createAndAddTask(path, data, hierarchy, cache,
                                          willNeedChangeNotification);
        if (newTask == null)
            return false;

        // send the appropriate TreeModel event.
        int[] childIndices = new int[] { ((EVTask)root).getNumChildren() - 1 };
        Object[] children = new Object[] { newTask };
        fireTreeNodesInserted
            (this, ((EVTask) root).getPath(), childIndices, children);
        return true;
    }
    public EVTask createAndAddTask(String path,
                                   DataRepository data,
                                   DashHierarchy hierarchy,
                                   ObjectCache cache,
                                   boolean willNeedChangeNotification) {
        return null;
    }

    public boolean removeTask(TreePath path) {
        if (!isEditable()) return false;

        if (checkRemovable(path) == false) return false;

        int pathLen = path.getPathCount();
        EVTask parent = (EVTask) path.getPathComponent(pathLen-2);
        EVTask child  = (EVTask) path.getPathComponent(pathLen-1);
        int pos = doRemoveTask(parent, child);
        if (pos == -1) return true;

        finishRemovingTask(pos);
        child.destroy();

        // send the appropriate TreeModel event.
        int[] childIndices = new int[] { pos };
        Object[] children = new Object[] { child };
        fireTreeNodesRemoved
            (this, ((EVTask) parent).getPath(), childIndices, children);
        return true;
    }
    protected boolean checkRemovable(TreePath path) {
        // default behavior: only remove tasks which are children of the root.
        return path.getPathCount() == 2;
    }
    protected int doRemoveTask(EVTask parent, EVTask child) {
        return parent.remove(child);
    }
    protected void finishRemovingTask(int pos) {}

    public boolean explodeTask(TreePath path) { return false; }

    public boolean moveTaskUp(int pos) {
        if (!isEditable()) return false;

        EVTask r = (EVTask) root;
        if (pos < 1 || pos >= r.getNumChildren()) return false;

        // make the change
        r.moveUp(pos);
        finishMovingTaskUp(pos);

        // send the appropriate TreeModel event.
        int[] childIndices = new int[] { pos-1, pos };
        Object[] children = new Object[]{ r.getChild(pos-1), r.getChild(pos) };
        fireTreeStructureChanged(this, r.getPath(), childIndices, children);
        return true;
    }
    protected void finishMovingTaskUp(int pos) {}


    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////
    /// Change notification support
    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////


    /*
     * Pass along evNodeChanged events to up to one additional EVTask.Listener
     */

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
    protected boolean someoneCares() {
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
        }
    }
    protected void dispose() {
        //System.out.println("disposing!");
        ((EVTask) root).destroy();
    }

    public void finalize() throws Throwable {
        //System.out.println("finalizing EVTaskList " + taskListName);
        super.finalize();
    }


    public void recalc() {
        if (calculator != null)
            calculator.recalculate();
        if (dependencyCalculator != null)
            dependencyCalculator.recalculate();
        totalPlanValue = schedule.getMetrics().totalPlan();
        EVTask taskRoot = (EVTask) root;
        totalActualTime = taskRoot.actualCurrentTime;
        showDirectTimeColumns = (taskRoot.planTime != taskRoot.planValue);
        fireEvRecalculated();
    }

    public EVSchedule getSchedule() { return schedule; }

    public void setDependencyCalculator(EVDependencyCalculator d) {
        this.dependencyCalculator = d;
    }



    //////////////////////////////////////////////////////////////////////
    /// TreeTableModel support
    //////////////////////////////////////////////////////////////////////


    private static final String[] COLUMN_KEYS = {
        "Task", "PT", "PDT", "Time", "DTime", "PV", "CPT", "CPV",
        "Who", "Plan_Date", "Date", "Depn", "PctC", "PctS", "EV" };

    /** Names of the columns in the TreeTableModel. */
    protected static String[] colNames =
        resources.getStrings("TaskList.Columns.", COLUMN_KEYS, ".Name");
    public static int[] colWidths =
        resources.getInts("TaskList.Columns.", COLUMN_KEYS, ".Width_");
    public static String[] toolTips =
        resources.getStrings("TaskList.Columns.", COLUMN_KEYS, ".Tooltip");

    public static final int TASK_COLUMN           = 0;
    public static final int PLAN_TIME_COLUMN      = 1;
    public static final int PLAN_DTIME_COLUMN     = 2;
    public static final int ACT_TIME_COLUMN       = 3;
    public static final int ACT_DTIME_COLUMN      = 4;
    public static final int PLAN_VALUE_COLUMN     = 5;
    public static final int PLAN_CUM_TIME_COLUMN  = 6;
    public static final int PLAN_CUM_VALUE_COLUMN = 7;
    public static final int ASSIGNED_TO_COLUMN    = 8;
    public static final int PLAN_DATE_COLUMN      = 9;
    public static final int DATE_COMPLETE_COLUMN  = 10;
    public static final int DEPENDENCIES_COLUMN   = 11;
    public static final int PCT_COMPLETE_COLUMN   = 12;
    public static final int PCT_SPENT_COLUMN      = 13;
    public static final int VALUE_EARNED_COLUMN   = 14;

    public static final int[] DIRECT_COLUMN_LIST = {
        PLAN_DTIME_COLUMN, ACT_DTIME_COLUMN };
    public static final String ID_DATA_NAME = "Task List ID";

    /** Types of the columns in the TreeTableModel. */
    static protected Class[]  colTypes = {
        TreeTableModel.class,   // project/task
        String.class,           // planned time
        String.class,           // planned direct time
        String.class,           // actual time
        String.class,           // actual direct time
        String.class,           // planned value
        String.class,           // planned cumulative time
        String.class,           // planned cumulative value
        String.class,           // assigned to
        Date.class,             // planned date
        Date.class,             // date
        Collection.class,       // task dependencies
        String.class,           // percent complete
        String.class,           // percent spent
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

    /** Returns true if the direct time columns in this schedule should be
     * displayed.
     */
    public boolean showDirectTimeColumns() {
        return showDirectTimeColumns;
    }

    /** Returns true if the value in column <code>column</code> of object
     *  <code>node</code> is editable. */
    public boolean isCellEditable(Object node, int column) {
        if (node == null) return false;
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

        case DEPENDENCIES_COLUMN:
            return (this instanceof EVTaskListData && node != root);
        }
        return false;
    }


    //
    //  The TreeTableNode interface.
    //

    /** Returns the number of columns. */
    public int getColumnCount() { return colNames.length; }

    /** Returns the name for a particular column. */
    public String getColumnName(int column) {
        if (!showDirectTimeColumns &&
            (column == PLAN_DTIME_COLUMN || column == ACT_DTIME_COLUMN))
            return " " + colNames[column] + " ";
        return colNames[column];
    }

    /** Returns the class for the particular column. */
    public Class getColumnClass(int column) { return colTypes[column]; }

    /** Returns the value of the particular column. */
    public Object getValueAt(Object node, int column) {
        if (node == null) return null;
        EVTask n = (EVTask) node;
        switch (column) {
        case TASK_COLUMN:           return n.getName();
        case PLAN_TIME_COLUMN:      return n.getPlanTime();
        case PLAN_DTIME_COLUMN:     return n.getPlanDirectTime();
        case ACT_TIME_COLUMN:       return n.getActualTime(totalActualTime);
        case ACT_DTIME_COLUMN:      return n.getActualDirectTime(totalActualTime);
        case PLAN_VALUE_COLUMN:     return n.getPlanValue(totalPlanValue);
        case PLAN_CUM_TIME_COLUMN:  return n.getCumPlanTime();
        case PLAN_CUM_VALUE_COLUMN: return n.getCumPlanValue(totalPlanValue);
        case ASSIGNED_TO_COLUMN:    return n.getAssignedToText();
        case PLAN_DATE_COLUMN:      return n.getPlanDate();
        case DATE_COMPLETE_COLUMN:  return n.getActualDate();
        case DEPENDENCIES_COLUMN:   return n.getDependencies();
        case PCT_COMPLETE_COLUMN:   return n.getPercentComplete();
        case PCT_SPENT_COLUMN:      return n.getPercentSpent();
        case VALUE_EARNED_COLUMN:   return n.getValueEarned(totalPlanValue);
        }
        return null;
    }

    /** Set the value at a particular row/column */
    public void setValueAt(Object aValue, Object node, int column) {
        //System.out.println("setValueAt("+aValue+","+node+","+column+")");
        if (node == null) return;
        EVTask n = (EVTask) node;
        switch (column) {
        case PLAN_TIME_COLUMN:      n.userSetPlanTime(aValue);       break;
        case DATE_COMPLETE_COLUMN:  n.userSetActualDate(aValue);     break;
        }
    }

    /** If the given cell has an error, return it.  Otherwise return null */
    public String getErrorStringAt(Object node, int column) {
        if (node == null) return null;
        EVTask n = (EVTask) node;
        switch (column) {
        case TASK_COLUMN: return ((EVTask) node).getTaskError();
        case PLAN_TIME_COLUMN: return ((EVTask) node).getPlanTimeError();
        default: return null;
        }
    }


    /** Load the unique ID for this task list.
     * @param taskListName the name of this task list.
     * @param data the DataRepository
     * @param savedDataName the name of a data element that is used for the
     *   persistence of this task list. (It will be used to check and see if
     *   this task list is new, or is an existing task list.)
     */
    protected void loadID(String taskListName, DataRepository data,
            String savedDataName) {
        String globalPrefix = MAIN_DATA_PREFIX + taskListName;
        String dataName = DataRepository.createDataName
            (globalPrefix, ID_DATA_NAME);

        SimpleData d = data.getSimpleValue(dataName);
        if (d != null) {
            taskListID = d.format();
        } else {
            // This task list doesn't have a unique ID yet.  Generate one.
            // It should be a value that needs no special handling to
            // appear as an XML attribute.
            int i = Math.abs((new Random()).nextInt());
            taskListID =
                Integer.toString(i, Character.MAX_RADIX) + "." +
                Long.toString(System.currentTimeMillis(), Character.MAX_RADIX);

            // Since unique task list IDs were introduced after version 1.5
            // of the dashboard, we may be in this branch of code not
            // because this is a new task list, but because this is the
            // first time the list has been opened.  In the latter case,
            // we need to save the unique ID.
            String savedDataFullName = DataRepository.createDataName
                (globalPrefix, savedDataName);
            if (data.getValue(savedDataFullName) != null) {
                // getting to this point indicates that this task list is
                // not new - it has been previously saved to the repository.
                data.putValue(dataName, StringData.create(taskListID));
            }
        }
    }


    public Object getAllDependenciesFor(EVTask node) {
        Set result = null;
        while (node != null) {
            if (node.getDependencies() != null) {
                if (result == null)
                    result = new HashSet();
                result.addAll(node.getDependencies());
            }
            node = node.getParent();
        }
        return result;
    }

    public TableModel getSimpleTableModel() {
        return new SimpleTableModel();
    }

    private class SimpleTableModel implements TableModel {
        private List rowList = null;

        public SimpleTableModel() {
            if (calculator != null)
                rowList = calculator.getEVLeaves();
            if (rowList == null || rowList.isEmpty())
                rowList = ((EVTask) root).getLeafTasks();
            rowList = filterRowList(rowList);
        }

        private List filterRowList(List rowList) {
            List result = new ArrayList(rowList);
            Iterator i = result.iterator();
            while (i.hasNext()) {
                EVTask task = (EVTask) i.next();
                if (task.isChronologicallyPruned() &&
                    task.actualDirectTime == 0)
                    i.remove();
            }
            return result;
        }

        public int getRowCount() { return rowList.size(); }
        public int getColumnCount() {
            return EVTaskList.this.getColumnCount(); }
        public String getColumnName(int columnIndex) {
            return EVTaskList.this.getColumnName(columnIndex); }
        public Class getColumnClass(int columnIndex) {
            return EVTaskList.this.getColumnClass(columnIndex); }
        public boolean isCellEditable(int row, int columnIndex) {
            return EVTaskList.this.isCellEditable(getRow(row), columnIndex); }
        public Object getValueAt(int row, int columnIndex) {
            if (columnIndex == 0)
                return getRow(row).getFullName();
            else if (columnIndex == DEPENDENCIES_COLUMN)
                return getAllDependenciesFor(getRow(row));
            else
                return EVTaskList.this.getValueAt(getRow(row), columnIndex);
        }
        public void setValueAt(Object aValue, int row, int columnIndex) {
            EVTaskList.this.setValueAt(aValue, getRow(row), columnIndex); }
        public void addTableModelListener(TableModelListener l) { }
        public void removeTableModelListener(TableModelListener l) { }

        private EVTask getRow(int row) {
            return (EVTask) rowList.get(row);
        }
    }

    public FlatTreeModel getFlatModel() { return new FlatTreeModel(); }

    public class FlatTreeModel extends AbstractTreeTableModel
        implements TreeModelListener, RecalcListener
    {

        private List evLeaves;

        public FlatTreeModel() {
            super(EVTaskList.this.root);
            evLeaves = getEVLeaves();
            EVTaskList.this.addTreeModelListener(this);
            EVTaskList.this.addRecalcListener(this);
        }

        private List getEVLeaves() {
            if (calculator instanceof EVCalculatorData)
                return new ArrayList(((EVCalculatorData) calculator).getReorderableEVLeaves());
            else
                return new ArrayList(calculator.getEVLeaves());
        }

        public Class getColumnClass(int column) {
            return EVTaskList.this.getColumnClass(column);
        }

        public boolean isCellEditable(Object node, int column) {
            return EVTaskList.this.isCellEditable(node, column);
        }

        public void setValueAt(Object aValue, Object node, int column) {
            EVTaskList.this.setValueAt(aValue, node, column);
        }

        public int getColumnCount() {
            return EVTaskList.this.getColumnCount();
        }

        public String getColumnName(int column) {
            return EVTaskList.this.getColumnName(column);
        }

        public Object getValueAt(Object node, int column) {
            if (column == TASK_COLUMN && node != root)
                return ((EVTask) node).getFullName();
            else if (column == DEPENDENCIES_COLUMN)
                return getAllDependenciesFor((EVTask) node);
            else
                return EVTaskList.this.getValueAt(node, column);
        }

        public int getChildCount(Object parent) {
            if (parent == root)
                return evLeaves.size();
            return 0;
        }

        public Object getChild(Object parent, int index) {
            if (parent == root && index >= 0 && index < evLeaves.size())
                return evLeaves.get(index);
            return null;
        }

        public int getIndexOfChild(Object parent, Object child) {
            if (parent == root && child instanceof EVTask)
                return EVTask.indexOfNode(evLeaves, (EVTask) child);
            return -1;
        }

        public void treeNodesChanged(TreeModelEvent e) { recalcLeafList(); }
        public void treeNodesInserted(TreeModelEvent e) { recalcLeafList(); }
        public void treeNodesRemoved(TreeModelEvent e) { recalcLeafList(); }
        public void treeStructureChanged(TreeModelEvent e) { recalcLeafList(); }
        public void evRecalculated(EventObject e) { recalcLeafList(); }

        private synchronized void recalcLeafList() {
            // get the new leaf list.
            List oldLeaves = evLeaves;
            List newLeaves = getEVLeaves();

            // compare the new list to the existing list.  If they are the
            // same, nothing needs to be done.
            if (newLeaves.equals(evLeaves)) return;

            // fire appropriate events based upon the changes to the list.
            int oldLen = oldLeaves.size();
            int newLen = newLeaves.size();
            int changedRows = Math.min(newLen, oldLen);

            evLeaves = newLeaves;

            int[] childIndices = new int[changedRows];
            Object[] children = new Object[changedRows];
            for (int i = 0;   i < changedRows;   i++) {
                childIndices[i] = i;
                children[i] = newLeaves.get(i);
            }
            fireTreeNodesChanged(this, ((EVTask) root).getPath(),
                                 childIndices, children);

            if (oldLen < newLen) {
                int insertedRows = newLen-oldLen;
                childIndices = new int[insertedRows];
                children = new Object[insertedRows];
                for (int i = 0;   i < insertedRows;   i++) {
                    childIndices[i] = i+changedRows;
                    children[i] = newLeaves.get(i+changedRows);
                }
                fireTreeNodesInserted(this, ((EVTask) root).getPath(),
                                      childIndices, children);

            } else if (oldLen > newLen) {
                int deletedRows = oldLen-newLen;
                childIndices = new int[deletedRows];
                children = new Object[deletedRows];
                for (int i = 0;   i < deletedRows;   i++) {
                    childIndices[i] = i+changedRows;
                    children[i] = oldLeaves.get(i+changedRows);
                }
                fireTreeNodesRemoved(this, ((EVTask) root).getPath(),
                                     childIndices, children);
            }
        }

        private void enumerateOrdinals() {
            Iterator allLeaves = evLeaves.iterator();
            int pos = 1;
            while (allLeaves.hasNext()) {
                EVTask task = (EVTask) allLeaves.next();
                task.taskOrdinal = pos++;
            }
        }

        public boolean moveTasksUp(int firstTaskPos, int lastTaskPos) {
            if (lastTaskPos < firstTaskPos
                    || firstTaskPos < 1
                    || lastTaskPos >= evLeaves.size())
                return false;

            EVTask predecessor = (EVTask) evLeaves.remove(firstTaskPos - 1);
            evLeaves.add(lastTaskPos, predecessor);
            enumerateOrdinals();

            fireTreeNodesRemoved(this, ((EVTask) root).getPath(),
                    new int[] { firstTaskPos - 1 },
                    new Object[] { predecessor });
            fireTreeNodesInserted(this, ((EVTask) root).getPath(),
                    new int[] { lastTaskPos },
                    new Object[] { predecessor });
            fireTreeNodesChanged(this, ((EVTask) root).getPath(),
                    new int[] { lastTaskPos },
                    new Object[] { predecessor });

            if (recalcTimer != null) recalcTimer.restart();

            return true;
        }

        public boolean moveTasksDown(int firstTaskPos, int lastTaskPos) {
            if (lastTaskPos < firstTaskPos
                    || firstTaskPos < 0
                    || lastTaskPos >= evLeaves.size() - 1)
                return false;

            EVTask successor = (EVTask) evLeaves.remove(lastTaskPos + 1);
            evLeaves.add(firstTaskPos, successor);
            enumerateOrdinals();

            fireTreeNodesRemoved(this, ((EVTask) root).getPath(),
                    new int[] { lastTaskPos + 1 },
                    new Object[] { successor });
            fireTreeNodesInserted(this, ((EVTask) root).getPath(),
                    new int[] { firstTaskPos },
                    new Object[] { successor });
            fireTreeNodesChanged(this, ((EVTask) root).getPath(),
                    new int[] { firstTaskPos },
                    new Object[] { successor });

            if (recalcTimer != null) recalcTimer.restart();

            return true;
        }

    }

    public TreeTableModel getMergedModel() {
        EVTaskListMerger merger = new EVTaskListMerger(EVTaskList.this);
        MergedTreeModel result = new MergedTreeModel(merger);
        addRecalcListener(result);
        return result;
    }

    public class MergedTreeModel extends AbstractTreeTableModel implements
            RecalcListener {

        private EVTaskListMerger merger;

        public MergedTreeModel(EVTaskListMerger merger) {
            super(merger.getMergedTaskRoot());
            this.merger = merger;
        }

        public Object getChild(Object parent, int index) {
            return EVTaskList.this.getChild(parent, index);
        }

        public int getChildCount(Object parent) {
            return EVTaskList.this.getChildCount(parent);
        }

        public Class getColumnClass(int column) {
            return EVTaskList.this.getColumnClass(column);
        }

        public int getColumnCount() {
            return EVTaskList.this.getColumnCount();
        }

        public String getColumnName(int column) {
            return EVTaskList.this.getColumnName(column);
        }

        public int getIndexOfChild(Object parent, Object child) {
            return EVTaskList.this.getIndexOfChild(parent, child);
        }

        public Object getValueAt(Object node, int column) {
            return EVTaskList.this.getValueAt(node, column);
        }

        public boolean isCellEditable(Object node, int column) {
            return EVTaskList.this.isCellEditable(node, column);
        }

        public boolean isLeaf(Object node) {
            return EVTaskList.this.isLeaf(node);
        }

        public void evRecalculated(EventObject e) {
            merger.recalculate();
            fireTreeStructureChanged(this, new Object[] { root }, new int[0],
                    null);
        }

    }
}
