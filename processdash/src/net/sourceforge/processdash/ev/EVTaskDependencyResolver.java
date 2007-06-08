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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.util.ThreadThrottler;

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

    private SortedSet listCache;

    private Map reverseDependencyCache;

    private long lastRefresh;

    private EVTaskDependencyResolver(DashboardContext context) {
        this.context = context;
        this.nameCache = new Hashtable();
        this.taskCache = new Hashtable();
        this.reverseDependencyCache = new Hashtable();
        this.lastRefresh = -1;
    }

    public List getTaskListsContaining(String taskID) {
        if (taskID == null)
            return null;
        Matcher m = TASK_ID_PATTERN.matcher(taskID);
        if (!m.matches())
            return null;
        else
            // At the moment, this will ignore the extra path info on the
            // task ID, and resolve only based on the initial task ID portion.
            // this should not cause problems, because all dependencies with
            // extra path information should have been created with an
            // explicit task list target, and this routine might never get
            // called.  If it does, we'll return all task lists which contain
            // the base taskID, regardless of whether they actually contain a
            // task descendant named by the extra path.
            taskID = m.group(PAT_GROUP_TASK_ID);

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

    public Set getIndividualsWaitingOnTask(Set result, Collection taskIDs,
            String ignoreIndividual) {
        if (taskIDs == null || taskIDs.isEmpty())
            return result;

        if (lastRefresh == -1)
            refreshCache();

        for (Iterator i = taskIDs.iterator(); i.hasNext();) {
            String id = (String) i.next();
            Set who = (Set) reverseDependencyCache.get(id);
            if (containsMoreThanJust(who, ignoreIndividual)) {
                if (result == null) result = new TreeSet();
                result.addAll(who);
                if (ignoreIndividual != null)
                    result.remove(ignoreIndividual);
            }
        }

        return result;
    }

    private boolean containsMoreThanJust(Set set, Object object) {
        if (set == null || set.isEmpty())
            return false;
        if (set.size() == 1 && object != null && set.contains(object))
            return false;
        return true;
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
        SortedSet newListCache = new TreeSet();
        Map newReverseCache = new Hashtable();
        findTasksInTaskLists(newTaskCache, listCache, newListCache,
                newReverseCache);

        this.nameCache = newNameCache;
        this.taskCache = newTaskCache;
        this.listCache = newListCache;
        this.reverseDependencyCache = newReverseCache;
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

    private void findTasksInTaskLists(Map newCache, SortedSet listCache,
            SortedSet newListCache, Map newReverseCache) {
        String[] taskListNames = EVTaskList.findTaskLists(context.getData(),
                false, true);

        if (listCache != null)
            for (Iterator i = listCache.iterator(); i.hasNext();) {
                TaskListInfo info = (TaskListInfo) i.next();
                registerListName(newCache, newListCache, info.getTaskListName(),
                        newReverseCache);
            }

        for (int i = 0; i < taskListNames.length; i++)
            registerListName(newCache, newListCache, taskListNames[i],
                    newReverseCache);

    }

    private void registerListName(Map newCache, SortedSet newListCache,
            String taskListName, Map newReverseCache) {
        if (containsTaskInfo(newListCache, taskListName))
            return;

        ThreadThrottler.tick();
        EVTaskList tl = EVTaskList.openExisting(taskListName, context
                .getData(), context.getHierarchy(), context.getCache(),
                false);
        if (tl != null)
            registerList(newCache, newListCache, newReverseCache, taskListName,
                    tl);
    }

    private void registerList(Map newCache, SortedSet newListCache,
            Map newReverseCache, String taskListName, EVTaskList tl) {
        TaskListInfo info = new TaskListInfo(taskListName, tl);
        newListCache.add(info);
        registerTasks(newCache, newReverseCache, info, (EVTask) tl.getRoot());
        addToCache(newCache, getPseudoTaskIdForTaskList(tl.getID()),
                info);

        if (tl instanceof EVTaskListRollup) {
            EVTaskListRollup rollup = (EVTaskListRollup) tl;
            for (int i = rollup.getChildCount(rollup.getRoot());  i-- > 0; ) {
                EVTaskList cl = rollup.getSubSchedule(i);
                registerList(newCache, newListCache, newReverseCache,
                        cl.taskListName, cl);
            }
        }
    }


    private void registerTasks(Map cache, Map reverseCache,
            TaskListInfo info, EVTask task) {
        ThreadThrottler.tick();
        List taskIDs = task.getTaskIDs();
        if (taskIDs != null)
            for (Iterator i = taskIDs.iterator(); i.hasNext();) {
                Object taskID = i.next();
                addToCache(cache, taskID, info);
            }

        List taskDeps = task.getDependencies();
        if (taskDeps != null)
            for (Iterator i = taskDeps.iterator(); i.hasNext();) {
                EVTaskDependency d = (EVTaskDependency) i.next();
                addToReverseCache(reverseCache, task, d);
            }

        for (int i = task.getNumChildren(); i-- > 0;)
            registerTasks(cache, reverseCache, info, task.getChild(i));
    }

    private void addToCache(Map cache, Object taskID, TaskInfo value) {
        SortedSet infoList = (SortedSet) cache.get(taskID);
        if (infoList == null) {
            infoList = new TreeSet();
            cache.put(taskID, infoList);
        }
        infoList.add(value);
    }

    private void addToReverseCache(Map reverseCache, EVTask task,
            EVTaskDependency dependsOn) {
        String dependsOnId = dependsOn.getTaskID();
        if (dependsOnId == null)
            return;

        List assignedIndividuals = task.getAssignedTo();
        if (assignedIndividuals == null || assignedIndividuals.isEmpty())
            return;

        SortedSet reverseWho = (SortedSet) reverseCache.get(dependsOnId);
        if (reverseWho == null) {
            reverseWho = new TreeSet();
            reverseCache.put(dependsOnId, reverseWho);
        }
        reverseWho.addAll(assignedIndividuals);
    }


    public static String getPseudoTaskIdForTaskList(String taskListID) {
        return "TL-" + taskListID;
    }


    private static boolean containsTaskInfo(Collection c, String name) {
        for (Iterator i = c.iterator(); i.hasNext();) {
            TaskInfo info = (TaskInfo) i.next();
            if (name.equals(info.name))
                return true;
        }
        return false;
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

            else if (this.prefLevel == that.prefLevel)
                return this.name.compareTo(that.name);

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

    private static final Pattern TASK_ID_PATTERN = Pattern
            .compile("([^/]+)(/.*)?");
    private static final int PAT_GROUP_TASK_ID = 1;
    // private static final int PAT_GROUP_EXTRAPATH = 2;
}
