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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
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
        calculator = new EVCalculatorRollup
            ((EVTask) root, evTaskLists, (EVScheduleRollup)schedule);
        ((EVTask) root).flag = TASK_LIST_FLAG;
    }

    /** For unit testing purposes only! */
    protected EVTaskListRollup(String taskListName, List taskLists) {
        super(taskListName, taskListName, false);
        evTaskLists = new Vector(taskLists);
        Iterator i = taskLists.iterator();
        int count = 0;
        while (i.hasNext()) {
            EVTaskList taskList = (EVTaskList) i.next();
            EVTask taskListRoot = (EVTask) taskList.getRoot();
            //taskListRoot.fullName = "task list " + count++;
            ((EVTask) root).add(taskListRoot);
        }
        schedule = new EVScheduleRollup(evTaskLists);
        calculator = new EVCalculatorRollup
            ((EVTask) root, evTaskLists, (EVScheduleRollup)schedule);
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
                    taskList = new EVTaskListXML(taskListName, data);
                else
                    continue;
            }

            ((EVTask) root).add((EVTask) taskList.getRoot());
            evTaskLists.add(taskList);
        }
    }
    public boolean isEditable() { return true; }

    public void save(String newName) {
        String dataName;

        // First, erase the data element that used to hold the list of
        // task lists.
        if (!taskListName.equals(newName)) {
            dataName = DataRepository.createDataName
                (MAIN_DATA_PREFIX + taskListName, TASK_LISTS_DATA_NAME);
            data.putValue(dataName, null);
        }

        // Now, save the rollup to the repository with the new name.
        if (newName != null) {
            dataName = DataRepository.createDataName
                (MAIN_DATA_PREFIX + newName, TASK_LISTS_DATA_NAME);
            ListData list = new ListData();
            Iterator i = evTaskLists.iterator();
            while (i.hasNext())
                list.add(((EVTaskList) i.next()).taskListName);

            data.putValue(dataName, list);
            taskListName = newName;
        }
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
            taskList = new EVTaskListXML(path, data);
        }
        if (taskList == null) return null;

        EVTask newTask = (EVTask) taskList.getRoot();
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
