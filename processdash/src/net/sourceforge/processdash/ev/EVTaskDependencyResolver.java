// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2006 Software Process Dashboard Initiative
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

import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;

public class EVTaskDependencyResolver {

    private static EVTaskDependencyResolver INSTANCE = null;

    /** Initialize the EVTaskDependencyResolver if needed */
    public static void init(DashboardContext context) {
        if (INSTANCE == null)
            INSTANCE = new EVTaskDependencyResolver(context);
    }

    /** Retrieve the singleton instance of the EVTaskDependencyResolver */
    public static EVTaskDependencyResolver getInstance() {
        return INSTANCE;
    }




    private DashboardContext context;

    private Map nameCache;

    private Map taskCache;

    private long lastRefresh;

    private EVTaskDependencyResolver(DashboardContext context) {
        this.context = context;
        this.nameCache = new Hashtable();
        this.taskCache = new Hashtable();
        this.lastRefresh = -1;
    }

    public List getTaskListsContaining(String taskID) {
        maybeRefreshCache();
        SortedSet infoList = (SortedSet) taskCache.get(taskID);

        if (infoList == null || infoList.isEmpty())
            return null;
        else {
            List result = new LinkedList();
            for (Iterator i = infoList.iterator(); i.hasNext();) {
                TaskListInfo info = (TaskListInfo) i.next();
                result.add(info.getTaskListName());
            }
            return result;
        }
    }

    public String getCanonicalTaskName(Collection taskIDs) {
        if (taskIDs != null) {
            for (Iterator i = taskIDs.iterator(); i.hasNext();) {
                String id = (String) i.next();
                String result = getCanonicalTaskName(id);
                if (result != null)
                    return result;
            }
        }
        return null;
    }

    public String getCanonicalTaskName(String taskID) {
        maybeRefreshCache();
        SortedSet infoList = (SortedSet) nameCache.get(taskID);
        if (infoList == null || infoList.isEmpty())
            return null;
        else {
            TaskNameInfo info = (TaskNameInfo) infoList.first();
            return info.getTaskName();
        }
    }

    private boolean maybeRefreshCache() {
        long now = System.currentTimeMillis();
        if (now - lastRefresh < 5000)
            return false;

        refreshCache();
        return true;
    }

    private void refreshCache() {
        Map newNameCache = new Hashtable();
        findTasksInHierarchy(newNameCache, PropertyKey.ROOT);

        Map newTaskCache = new Hashtable();
        findTasksInTaskLists(newTaskCache);

        this.nameCache = newNameCache;
        this.taskCache = newTaskCache;
        this.lastRefresh = System.currentTimeMillis();
    }

    private void findTasksInHierarchy(Map newCache, PropertyKey key) {
        String path = key.path();
        List taskIDs = EVTaskDependency.getTaskIDs(context.getData(), path);
        if (taskIDs != null) {
            int pref = 100;

            String templateID = context.getHierarchy().getID(key);
            if (isTemplateIDType(templateID, "/Master"))
                pref += 3000;
            else if (isTemplateIDType(templateID, "/Team"))
                pref += 2000;
            else if (isTemplateIDType(templateID, "/Indiv"))
                pref += 1000;

            for (Iterator i = taskIDs.iterator(); i.hasNext();) {
                String taskID = (String) i.next();
                TaskNameInfo info = new TaskNameInfo(path, pref--);
                addToCache(newCache, taskID, info);
            }
        }

        DashHierarchy hierarchy = context.getHierarchy();
        for (int i = hierarchy.getNumChildren(key);  i-- > 0; )
            findTasksInHierarchy(newCache, hierarchy.getChildKey(key, i));
    }
    private boolean isTemplateIDType(String id, String type) {
        return (id != null && id.indexOf(type) != -1);
    }

    private void findTasksInTaskLists(Map newCache) {
        String[] taskListNames = EVTaskList.findTaskLists(context.getData(),
                false, true);
        for (int i = 0; i < taskListNames.length; i++) {
            String taskListName = taskListNames[i];
            EVTaskList tl = EVTaskList.openExisting(taskListName, context
                    .getData(), context.getHierarchy(), context.getCache(),
                    false);
            if (tl != null) {
                TaskListInfo info = new TaskListInfo(taskListName, tl);
                registerTasks(newCache, info, (EVTask) tl.getRoot());
            }
        }
    }

    private void registerTasks(Map cache, TaskListInfo info, EVTask task) {
        List taskIDs = task.getTaskIDs();
        if (taskIDs != null)
            for (Iterator i = taskIDs.iterator(); i.hasNext();) {
                Object taskID = i.next();
                addToCache(cache, taskID, info);
            }

        for (int i = task.getNumChildren(); i-- > 0;)
            registerTasks(cache, info, task.getChild(i));
    }

    private void addToCache(Map cache, Object taskID, TaskInfo value) {
        SortedSet infoList = (SortedSet) cache.get(taskID);
        if (infoList == null) {
            infoList = new TreeSet();
            cache.put(taskID, infoList);
        }
        infoList.add(value);
    }


    private static class TaskInfo implements Comparable {

        public String name;

        public int prefLevel;

        public TaskInfo(String name, int prefLevel) {
            this.name = name;
            this.prefLevel = prefLevel;
        }

        public int compareTo(Object o) {
            TaskInfo that = (TaskInfo) o;
            if (this.name.equals(that.name))
                return 0;
            else
                // if this is better than that, return a negative number
                // to sort best items at the front of lists.
                return that.prefLevel - this.prefLevel;
        }

        public boolean equals(Object obj) {
            if (obj instanceof TaskInfo) {
                TaskInfo that = (TaskInfo) obj;
                return this.name.equals(that.name);
            } else
                return false;
        }

        public int hashCode() {
            return name.hashCode();
        }

    }

    private static class TaskListInfo extends TaskInfo {

        public TaskListInfo(String taskListName, EVTaskList s) {
            super(taskListName, measurePrefLevel(s));
        }

        public String getTaskListName() {
            return name;
        }
    }

    private static class TaskNameInfo extends TaskInfo {

        public TaskNameInfo(String name, int prefLevel) {
            super(name, prefLevel);
        }

        public String getTaskName() {
            return name;
        }

    }

    /**
     * Determine how desirable a given schedule is for extracting dependency
     * information.
     * 
     * @param s
     *            a schedule to examine
     * @return a rating indicating how desirable the schedule is for providing
     *         dependency information. Higher numbers are more desirable.
     */
    private static int measurePrefLevel(EVTaskList s) {
        // schedules are more desirable if they represent work by many people.
        // also, schedules are more desirable if they include many first-class
        // tasks (ones that have been assigned a taskID).
        Set people = new HashSet();
        Set taskIDs = new HashSet();
        accumulatePrefInfo((EVTask) s.getRoot(), people, taskIDs);
        return people.size() * 10000 + taskIDs.size();
    }

    private static void accumulatePrefInfo(EVTask task, Set people, Set taskIDs) {
        if (task.assignedTo != null)
            people.addAll(task.assignedTo);
        if (task.getTaskIDs() != null)
            taskIDs.addAll(task.getTaskIDs());

        for (int i = task.getNumChildren(); i-- > 0;)
            accumulatePrefInfo(task.getChild(i), people, taskIDs);
    }
}
