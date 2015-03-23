// Copyright (C) 2006-2014 Tuma Solutions, LLC
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.util.LightweightSet;
import net.sourceforge.processdash.util.ThreadThrottler;
import net.sourceforge.processdash.util.XMLDepthFirstIterator;
import net.sourceforge.processdash.util.XMLUtils;

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

    private long lastNameRefresh;

    private long lastReverseRefresh;

    private long refreshInterval;


    private EVTaskDependencyResolver(DashboardContext context) {
        this.context = context;
        this.nameCache = new Hashtable();
        this.taskCache = new Hashtable();
        this.reverseDependencyCache = new Hashtable();
        this.lastRefresh = this.lastNameRefresh = this.lastReverseRefresh = -1;
        setDynamic(true);

        ImportedEVManager.getInstance().addCalculator(
            ReverseDependencyInfo.class, new ReverseDependencyCollector());
    }

    public void setDynamic(boolean dynamic) {
        this.refreshInterval = (dynamic ? 30000 : 3000000000L);
    }

    public void flushCaches() {
        flushCaches(nameCache, taskCache, listCache, reverseDependencyCache);
        this.lastRefresh = this.lastNameRefresh = this.lastReverseRefresh = -1;
    }
    private void flushCaches(Object... caches) {
        for (Object c : caches) {
            if (c instanceof Map) {
                ((Map) c).clear();
            } else if (c instanceof Set) {
                ((Set) c).clear();
            } else if (c instanceof Collection) {
                ((Collection) c).clear();
            }
        }
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
        maybeRefreshNameCache();
        SortedSet infoList = (SortedSet) nameCache.get(taskID);
        if (infoList == null || infoList.isEmpty())
            return null;
        else {
            TaskNameInfo info = (TaskNameInfo) infoList.first();
            return info.getTaskName();
        }
    }

    public Map getIndividualsWaitingOnTask(Map result, Collection taskIDs,
            String ignoreIndividual) {
        if (taskIDs == null || taskIDs.isEmpty())
            return result;

        maybeRefreshReverseDependencies();

        for (Iterator i = taskIDs.iterator(); i.hasNext();) {
            String id = (String) i.next();
            Map who = (Map) reverseDependencyCache.get(id);
            if (containsMoreThanJust(who, ignoreIndividual)) {
                if (result == null)
                    result = new TreeMap();
                mergeReverseDependencyInfo(result, who);
                if (ignoreIndividual != null)
                    result.remove(ignoreIndividual);
            }
        }

        return result;
    }

    private boolean containsMoreThanJust(Map map, Object key) {
        if (map == null || map.isEmpty())
            return false;
        if (map.size() == 1 && key != null && map.containsKey(key))
            return false;
        return true;
    }


    private boolean maybeRefreshCache() {
        long now = System.currentTimeMillis();
        if (now - lastRefresh < refreshInterval)
            return false;

        refreshCache();
        return true;
    }

    private void refreshCache() {
        Map newTaskCache = new Hashtable();
        SortedSet newListCache = new TreeSet();
        findTasksInTaskLists(newTaskCache, listCache, newListCache, false);

        this.taskCache = newTaskCache;
        this.listCache = newListCache;
        this.lastRefresh = System.currentTimeMillis();
    }

    private boolean maybeRefreshNameCache() {
        long now = System.currentTimeMillis();
        if (now - lastNameRefresh < refreshInterval)
            return false;

        refreshNameCache();
        return true;
    }

    private void refreshNameCache() {
        Map newNameCache = new Hashtable();
        findTasksInHierarchy(newNameCache, PropertyKey.ROOT);

        this.nameCache = newNameCache;
        this.lastNameRefresh = System.currentTimeMillis();
    }

    private boolean maybeRefreshReverseDependencies() {
        long now = System.currentTimeMillis();
        if (now - lastReverseRefresh < refreshInterval)
            return false;

        refreshExternalReverseDependencies();
        return true;
    }

    private void refreshExternalReverseDependencies() {
        Map newReverseCache = new Hashtable();

        Collection allReverseDependencyLists = ImportedEVManager.getInstance()
                .getCachedData(ReverseDependencyInfo.class).values();
        for (Object oneListObj : allReverseDependencyLists) {
            List<ReverseDependencyInfo> oneList = (List) oneListObj;
            for (ReverseDependencyInfo info : oneList) {
                addToReverseCache(newReverseCache, info);
            }
        }

        this.reverseDependencyCache = newReverseCache;
        this.lastReverseRefresh = System.currentTimeMillis();
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
            SortedSet newListCache, boolean externalOnly) {
        String[] taskListNames = EVTaskList.findTaskLists(context.getData(),
                false, true);

        if (listCache != null)
            for (Iterator i = listCache.iterator(); i.hasNext();) {
                TaskListInfo info = (TaskListInfo) i.next();
                registerListName(newCache, newListCache, info.getTaskListName());
            }

        for (int i = 0; i < taskListNames.length; i++)
            if (externalOnly == false
                    || EVTaskListXML.validName(taskListNames[i]))
                registerListName(newCache, newListCache, taskListNames[i]);
    }

    private void registerListName(Map newCache, SortedSet newListCache,
            String taskListName) {
        if (containsTaskInfo(newListCache, taskListName))
            return;

        ThreadThrottler.tick();
        EVTaskList tl = EVTaskList.openExisting(taskListName, context
                .getData(), context.getHierarchy(), context.getCache(),
                false);
        if (tl != null)
            registerList(newCache, newListCache, taskListName, tl);
    }

    private void registerList(Map newCache, SortedSet newListCache,
            String taskListName, EVTaskList tl) {
        TaskListInfo info = new TaskListInfo(taskListName, tl);
        newListCache.add(info);
        registerTasks(newCache, info, (EVTask) tl.getRoot());
        addToCache(newCache, getPseudoTaskIdForTaskList(tl.getID()),
                info);

        if (tl instanceof EVTaskListRollup) {
            EVTaskListRollup rollup = (EVTaskListRollup) tl;
            for (int i = rollup.getChildCount(rollup.getRoot());  i-- > 0; ) {
                EVTaskList cl = rollup.getSubSchedule(i);
                registerList(newCache, newListCache, cl.taskListName, cl);
            }
        }
    }


    private void registerTasks(Map cache, TaskListInfo info, EVTask task) {
        ThreadThrottler.tick();
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

    private void addToReverseCache(Map reverseCache, ReverseDependencyInfo info) {
        String dependsOnId = info.dependsOnTaskId;

        SortedMap reverseWho = (SortedMap) reverseCache.get(dependsOnId);
        if (reverseWho == null) {
            reverseWho = new TreeMap();
            reverseCache.put(dependsOnId, reverseWho);
        }

        for (String personName : info.who) {
            mergeReverseDependencyInfo(reverseWho, personName, info.needDate);
        }
    }

    public static void mergeReverseDependencyInfo(Map dest, Map src) {
        for (Iterator i = src.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            String name = (String) e.getKey();
            Date date = (Date) e.getValue();
            mergeReverseDependencyInfo(dest, name, date);
        }
    }

    private static void mergeReverseDependencyInfo(Map dest, String name,
            Date date) {
        Date oldDate = (Date) dest.get(name);
        Date earlierDate = EVCalculator.minStartDate(date, oldDate);
        dest.put(name, earlierDate);
    }


    public static String getPseudoTaskIdForTaskList(String taskListID) {
        return "TL-" + taskListID;
    }

    public static String getIdForTask(EVTask task, String taskListId) {
        List ids = task.getTaskIDs();
        if (ids != null && !ids.isEmpty())
            return (String) ids.get(ids.size()-1);
        else if (task.getParent() == null || task.getFlag() != null)
            return getPseudoTaskIdForTaskList(taskListId);
        else
            return pathConcat(getIdForTask(task.getParent(), taskListId),
                    task.getName());
    }

    private static String pathConcat(String a, String b) {
        if (b.startsWith("/"))
            return a + b;
        else
            return a + "/" + b;
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


    private static class ReverseDependencyCollector implements
            ImportedEVManager.CachedDataCalculator {

        public Object calculateCachedData(String taskListName, Element xml) {
            ReverseDependencyCollectorWorker w = new ReverseDependencyCollectorWorker();
            w.run(xml);
            if (w.result.isEmpty())
                return Collections.EMPTY_LIST;
            else
                return Collections.unmodifiableList(w.result);
        }

    }

    private static class ReverseDependencyCollectorWorker extends
            XMLDepthFirstIterator {
        private List result = new ArrayList();

        @Override
        public void caseElement(Element xml, List path) {
            if (EVTaskDependency.DEPENDENCY_TAG.equals(xml.getTagName())) {
                ReverseDependencyInfo info = new ReverseDependencyInfo(path, xml);
                if (info.isViable())
                    result.add(info);
            }
        }

        @Override
        public String getPathAttributeName(Element n) { return "who"; }
    }

    private static class ReverseDependencyInfo {
        private String dependsOnTaskId;
        private Date needDate;
        private Set<String> who;

        private ReverseDependencyInfo(List whoPath, Element xml) {
            Element parentTask = (Element) xml.getParentNode();

            this.dependsOnTaskId = xml
                    .getAttribute(EVTaskDependency.TASK_ID_ATTR);
            this.needDate = EVTaskDependency
                    .getDependencyComparisonDate(parentTask);

            this.who = new LightweightSet();
            getWhoFromChildTasks(parentTask);
            if (who.isEmpty())
                getWhoFromPath(whoPath);
        }

        /**
         * When plain task lists are exported by individuals, and when rollup
         * lists are exported in non-merged mode, the "who" attribute is stored
         * on an XML tag near the top of the document tree. All XML tags
         * underneath that element are understood to inherit the given "who"
         * value. This method searches up the tree for such a "who" attribute
         * and adds the resulting individual to the "who" list in this object.
         */
        private void getWhoFromPath(List whoPath) {
            // start at the end of the list (but skip the final element, which
            // is for the <dependency> tag itself) and look for the nearest
            // enclosing value of the "who" attribute.
            for (int i = whoPath.size() - 1;  i-- > 0; ) {
                if (addAssigned((String) whoPath.get(i)))
                    break;
            }
        }

        /**
         * When rollup task lists are exported in merged mode, "who" attributes
         * are stored on leaf tasks. This method scans a portion of the tree to
         * find all of the "who" attributes that are on or underneath a given
         * XML node, and adds the resulting people to the "who" list in this
         * object.
         */
        private void getWhoFromChildTasks(Element task) {
            if ("task".equals(task.getTagName())) {
                String taskWho = task.getAttribute("who");
                addAssigned(taskWho);

                for (Element child : XMLUtils.getChildElements(task))
                    getWhoFromChildTasks(child);
            }
        }

        private boolean addAssigned(String whoAttr) {
            if (XMLUtils.hasValue(whoAttr)) {
                who.addAll(Arrays.asList(whoAttr.split(", ?")));
                return true;
            } else {
                return false;
            }
        }

        private boolean isViable() {
            return XMLUtils.hasValue(dependsOnTaskId) && !who.isEmpty();
        }

    }

}
