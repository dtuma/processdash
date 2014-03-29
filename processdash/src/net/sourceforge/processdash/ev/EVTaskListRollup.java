// Copyright (C) 2002-2014 Tuma Solutions, LLC
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


package net.sourceforge.processdash.ev;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;

import javax.swing.tree.TreePath;

import org.jfree.data.xy.XYDataset;

import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataNameFilter;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ev.ui.chart.ConfidenceIntervalMemberCompletionDateChartData;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.net.cache.ObjectCache;


public class EVTaskListRollup extends EVTaskList {

    public static final String TASK_LIST_FLAG = "rollup";

    public static final String TASK_LISTS_DATA_NAME = "Task Lists";

    protected DataRepository data;
    protected Vector evTaskLists;

    /** Create a rollup task list.
     *
     * Note: change notification is only <b>partially</b> supported.
     */
    public EVTaskListRollup(String taskListName,
                            DataRepository data,
                            DashHierarchy hierarchy,
                            ObjectCache cache,
                            boolean willNeedChangeNotification) {
        super(taskListName, taskListName, willNeedChangeNotification);
        this.data = data;

        evTaskLists = new Vector();
        addTaskListsFromData(data, hierarchy, cache, taskListName);
        schedule = new EVScheduleRollup(evTaskLists);
        loadID(taskListName, data, TASK_LISTS_DATA_NAME);
        loadMetadata(taskListName, data);
        calculator = new EVCalculatorRollup(this, (EVTask) root, evTaskLists,
                (EVScheduleRollup) schedule, metaData);
        setBaselineDataSource(getBaselineSnapshot());
        ((EVTask) root).flag = TASK_LIST_FLAG;
    }

    /** For unit testing purposes only! */
    protected EVTaskListRollup(String taskListName, List taskLists) {
        super(taskListName, taskListName, false);
        evTaskLists = new Vector(taskLists);
        Iterator i = taskLists.iterator();
        while (i.hasNext()) {
            EVTaskList taskList = (EVTaskList) i.next();
            EVTask taskListRoot = (EVTask) taskList.getRoot();
            //taskListRoot.fullName = "task list " + count++;
            ((EVTask) root).add(taskListRoot);
        }
        schedule = new EVScheduleRollup(evTaskLists);
        calculator = new EVCalculatorRollup(this, (EVTask) root, evTaskLists,
                (EVScheduleRollup) schedule, null);
    }

    private void addTaskListsFromData(DataRepository data,
                                      DashHierarchy hierarchy,
                                      ObjectCache cache,
                                      String taskListName) {
        String globalPrefix = MAIN_DATA_PREFIX + taskListName;
        String dataName =
            DataRepository.createDataName(globalPrefix, TASK_LISTS_DATA_NAME);
        SimpleData listVal = data.getSimpleValue(dataName);
        ListData list = null;
        if (listVal instanceof ListData)
            list = (ListData) listVal;
        else if (listVal instanceof StringData)
            list = ((StringData) listVal).asList();

        if (list == null) return;
        for (int i = 0;   i < list.size();   i++) {
            taskListName = (String) list.get(i);
            EVTaskList taskList = openTaskListToAdd(taskListName, data,
                    hierarchy, cache);
            if (taskList == null) {
                if (EVTaskListXML.validName(taskListName))
                    taskList = new EVTaskListXML(taskListName);
                else
                    continue;
            }

            if (((EVTask) root).add((EVTask) taskList.getRoot()))
                evTaskLists.add(taskList);
        }
    }
    public boolean isEditable() { return true; }

    public void setTaskLabeler(TaskLabeler l) {
        super.setTaskLabeler(l);
        for (Iterator i = evTaskLists.iterator(); i.hasNext();) {
            EVTaskList tl = (EVTaskList) i.next();
            tl.setTaskLabeler(l);
        }
    }

    public void save(String newName) {
        saveTaskLists(newName, data);
        saveID(newName, data);
        saveMetadata(newName, data);
        renameSnapshots(getPotentialDataNames(), newName, data);

        taskListName = newName;

        super.save(newName);
    }

    private Set getPotentialDataNames() {
        String globalPrefix = MAIN_DATA_PREFIX + taskListName + "/";
        Iterator i = data.getKeys(null, DataNameFilter.EXPLICIT_ONLY);
        Set result = new HashSet();
        while (i.hasNext()) {
            String dataName = (String) i.next();
            if (dataName.startsWith(globalPrefix))
                result.add(dataName);
        }
        return result;
    }

    private void saveTaskLists(String newName, DataRepository data) {
        ListData list = null;
        if (newName != null) {
            list = new ListData();
            Iterator i = evTaskLists.iterator();
            while (i.hasNext())
                list.add(((EVTaskList) i.next()).taskListName);
        }

        persistDataValue(newName, data, TASK_LISTS_DATA_NAME, list);
    }

    public List getSubSchedules() {
        return Collections.unmodifiableList(evTaskLists);
    }

    public int getSubScheduleCount() {
        return evTaskLists.size();
    }

    @Override
    public String getTimezoneID() {
        // rollups are typically rolling up data from the default, local time
        // zone.  So we should explicitly state this for our relative time zone
        return TimeZone.getDefault().getID();
    }

    public EVTaskList getSubSchedule(int pos) {
        if (pos < 0 || pos >= evTaskLists.size()) return null;
        return (EVTaskList) evTaskLists.get(pos);
    }

    public EVTask createAndAddTask(String path,
                                   DataRepository data,
                                   DashHierarchy hierarchy,
                                   ObjectCache cache,
                                   boolean willNeedChangeNotification) {

        EVTaskList taskList = openTaskListToAdd(path, data, hierarchy, cache);
        // when adding an XML task list that doesn't appear to exist yet
        // (most likely due to the timing of import/export operations when
        // an individual is joining a team project), give the caller the
        // benefit of the doubt that the task list will be created soon, and
        // add the named schedule anyway.
        if (taskList == null && EVTaskListXML.validName(path)) {
            taskList = new EVTaskListXML(path);
        }
        if (taskList == null) return null;

        // first, remove any existing child with the same name.  (This covers
        // the common scenario where a child task list has been deleted and
        // recreated, and we're attempting to replace the missing task list
        // with the replacement.)
        EVTask newTask = taskList.getTaskRoot();
        int oldTaskIndex = getTaskRoot().getChildIndex(newTask);
        if (oldTaskIndex != -1) {
            EVTask oldTask = getTaskRoot().getChild(oldTaskIndex);
            TreePath tp = new TreePath(new Object[] { getTaskRoot(), oldTask});
            removeTask(tp);
        }

        // now, add the new task list to the end of our list.
        if (((EVTask) root).add(newTask)) {
            evTaskLists.add(taskList);
            ((EVScheduleRollup) schedule).addSchedule(taskList);
            return newTask;
        } else
            return null;
    }

    private EVTaskList openTaskListToAdd(String taskListToAdd,
            DataRepository data, DashHierarchy hierarchy, ObjectCache cache) {
        String myName = this.taskListName;
        Set names = (Set) TASK_LIST_NAMES_CURRENTLY_OPENING.get();
        try {
            names.add(myName);
            if (names.contains(taskListToAdd))
                return new EVTaskList(taskListToAdd, taskListToAdd,
                        resources.getString("Task.Circular_Task.Error"));
            else
                return EVTaskList.openExisting
                    (taskListToAdd, data, hierarchy, cache, false);
        } finally {
            names.remove(myName);
        }
    }

    protected void finishRemovingTask(int pos) {
        EVTaskList taskList = (EVTaskList) evTaskLists.remove(pos);
        ((EVScheduleRollup) schedule).removeSchedule(taskList.schedule);
    }

    protected void finishMovingTaskUp(int pos) {
        Object taskList = evTaskLists.remove(pos);
        evTaskLists.insertElementAt(taskList, pos-1);
    }

    protected void fireEvRecalculated() {
        if (someoneCares()) {
            EVTask rootTask = (EVTask) root;
            int size = rootTask.getNumChildren();
            int[] childIndexes = new int[size];
            Object[] children = new Object[size];
            while (size-- > 0) {
                childIndexes[size] = size;
                children[size] = rootTask.getChild(size);
            }
            fireTreeStructureChanged(this, rootTask.getPath(),
                                     childIndexes, children);
        }
        super.fireEvRecalculated();
    }

    @Override
    public String saveSnapshot(String snapshotId, String snapshotName,
            String snapshotComment) {
        return saveSnapshotToData(data, snapshotId, snapshotName,
            snapshotComment);
    }

    @Override
    public EVSnapshot getSnapshotById(String snapshotId) {
        return getSnapshotFromData(data, snapshotId);
    }

    @Override
    public List<EVSnapshot.Metadata> getSnapshots() {
        return super.getSnapshots(data);
    }

    public XYDataset getTeamMemberCompletionDateData() {
        return new ConfidenceIntervalMemberCompletionDateChartData(
                new EVTaskChartEventAdapter(), this);
    }



    public static boolean validName(String taskListName) {
        return (taskListName != null &&
                taskListName.length() > 0 &&
                taskListName.indexOf('/') == -1);
    }
    public static boolean exists(DataRepository data, String taskListName) {
        String dataName = DataRepository.createDataName
            (MAIN_DATA_PREFIX + taskListName, TASK_LISTS_DATA_NAME);
        return data.getSimpleValue(dataName) != null;
    }
    public static String taskListNameFromDataElement(String dataName) {
        if (dataName == null ||
            !dataName.startsWith(MAIN_DATA_PREFIX) ||
            !dataName.endsWith("/" + TASK_LISTS_DATA_NAME))
            return null;

        return dataName.substring
            (MAIN_DATA_PREFIX.length(),
             dataName.length() - TASK_LISTS_DATA_NAME.length() - 1);
    }

    private static ThreadLocal TASK_LIST_NAMES_CURRENTLY_OPENING =
        new ThreadLocal() {
            protected Object initialValue() {
                return new HashSet();
            }
        };
}
