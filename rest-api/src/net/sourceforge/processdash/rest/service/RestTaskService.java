// Copyright (C) 2017 Tuma Solutions, LLC
// REST API Add-on for the Process Dashboard
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

package net.sourceforge.processdash.rest.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.log.time.TimeLogEntry;
import net.sourceforge.processdash.process.ScriptEnumerator;
import net.sourceforge.processdash.process.ScriptID;
import net.sourceforge.processdash.process.ui.TriggerURI;
import net.sourceforge.processdash.rest.rs.HttpException;
import net.sourceforge.processdash.rest.to.RestProject;
import net.sourceforge.processdash.rest.to.RestTask;
import net.sourceforge.processdash.rest.to.RestTaskScript;
import net.sourceforge.processdash.util.StringUtils;

public class RestTaskService {

    private static RestTaskService svc;

    public static RestTaskService get() {
        if (svc == null)
            svc = new RestTaskService();
        return svc;
    }


    private DashboardContext ctx;

    private DashHierarchy hier;

    private RestProjectService projects;

    private RestTaskService() {
        ctx = RestDashContext.get();
        hier = ctx.getHierarchy();
        projects = RestProjectService.get();
        DashController.assignHierarchyNodeIDs();
    }

    public RestTask byID(String nodeID) {
        return byKey(hier.findKeyByNodeID(nodeID));
    }

    public RestTask byPath(String taskPath) {
        return byKey(hier.findExistingKey(taskPath));
    }

    public RestTask byKey(PropertyKey key) {
        String nodeID = hier.pget(key).getNodeID();
        if (!StringUtils.hasValue(nodeID))
            return null;

        String fullPath = key.path();
        RestProject proj = projects.containingPath(fullPath);
        if (fullPath.equals(proj.getFullName()))
            return new RestTask(nodeID, null, proj);

        String taskName = fullPath.substring(proj.getFullName().length() + 1);
        return new RestTask(nodeID, taskName, proj);
    }

    public List<RestTask> allLeaves() {
        return leavesUnder(PropertyKey.ROOT);
    }

    public List<RestTask> leavesUnder(String parentPath) {
        return leavesUnder(hier.findExistingKey(parentPath));
    }

    public List<RestTask> leavesUnder(PropertyKey parent) {
        return leavesUnder(parent, false);
    }

    public List<RestTask> forProject(RestProject project,
            boolean chronological) {
        PropertyKey projectKey = hier.findExistingKey(project.getFullName());
        List<RestTask> result = leavesUnder(projectKey, true);
        if (chronological)
            result = sortTasksChronologically(result, project);
        return result;
    }

    private List<RestTask> leavesUnder(PropertyKey parent,
            boolean pruneTeamProjects) {
        List<RestTask> result = new ArrayList<RestTask>();
        if (parent != null)
            enumLeafTasks(result, parent, true, pruneTeamProjects);
        return result;
    }

    private void enumLeafTasks(List<RestTask> result, PropertyKey node,
            boolean isStartingNode, boolean pruneTeamProjects) {
        // if pruneTeamProjects is true, and this is a team project, abort
        if (pruneTeamProjects && !isStartingNode
                && projects.byPath(node.path()) != null)
            return;

        int numKids = hier.getNumChildren(node);
        if (numKids == 0) {
            RestTask task = byKey(node);
            if (task != null && task.getFullName() != null)
                result.add(task);
        } else {
            for (int i = 0; i < numKids; i++)
                enumLeafTasks(result, hier.getChildKey(node, i), false,
                    pruneTeamProjects);
        }
    }

    private List<RestTask> sortTasksChronologically(List<RestTask> tasks,
            RestProject project) {
        List<String> taskOrder = RestTaskListService.get()
                .getTaskOrder(project);
        if (taskOrder == null || taskOrder.isEmpty())
            return tasks;

        Map<String, RestTask> tasksByPath = new LinkedHashMap<String, RestTask>();
        for (RestTask t : tasks)
            tasksByPath.put(t.getFullPath(), t);

        List<RestTask> result = new ArrayList<RestTask>(tasks.size());
        for (String oneTaskPath : taskOrder) {
            RestTask task = tasksByPath.remove(oneTaskPath);
            if (task != null)
                result.add(task);
        }
        result.addAll(tasksByPath.values());
        return result;
    }


    public List<RestTask> recentTasks(int maxCount) throws IOException {
        List<TimeLogEntry> entries = RestTimeLogService.get()
                .allTimeLogEntries();
        Map<String, RestTask> results = new LinkedHashMap<String, RestTask>();

        // look through the time log entries, from newest to oldest.
        for (int i = entries.size(); i-- > 0;) {
            // get the task path for this time log entry. if we've already seen
            // this task, skip to the next entry.
            TimeLogEntry tle = entries.get(i);
            String path = tle.getPath();
            if (results.containsKey(path))
                continue;

            // if the named task doesn't exist, or isn't a leaf node, skip it
            PropertyKey key = hier.findExistingKey(path);
            if (key == null || hier.getNumChildren(key) > 0)
                continue;

            // build a task for this object and add it to our result list
            RestTask task = byKey(key);
            if (task != null)
                results.put(path, task);

            // stop if we've found enough tasks
            if (results.size() >= maxCount)
                break;
        }

        return new ArrayList<RestTask>(results.values());
    }


    public static final int TASK_COMPLETION_DATE = 1;

    public static final int TASK_TIMES = 2;

    public RestTask loadData(RestTask task) {
        return loadData(task, TASK_COMPLETION_DATE + TASK_TIMES);
    }

    public RestTask loadData(RestTask task, int fieldMask) {
        if (task == null)
            return null;
        String fullPath = task.getFullPath();
        SimpleData sd;

        if ((fieldMask & TASK_COMPLETION_DATE) > 0) {
            // load the task completion date
            sd = ctx.getData().getSimpleValue(fullPath + COMPLETED);
            if (sd instanceof DateData)
                task.setCompletionDate(((DateData) sd).getValue());
        }

        if ((fieldMask & TASK_TIMES) > 0) {
            // load the estimated time for the task
            sd = ctx.getData().getSimpleValue(fullPath + EST_TIME);
            if (sd instanceof DoubleData)
                task.setEstimatedTime(((DoubleData) sd).getDouble());

            // load the actual time for the task
            sd = ctx.getData().getSimpleValue(fullPath + ACT_TIME);
            if (sd instanceof DoubleData)
                task.setActualTime(((DoubleData) sd).getDouble());
        }

        return task;
    }

    public void saveEstimatedTime(RestTask task, Double estimatedTime) {
        ensureLeaf(task);

        // if this task has a read-only time estimate, don't change it.
        String dataName = task.getFullPath() + EST_TIME;
        SimpleData sd = ctx.getData().getSimpleValue(dataName);
        if (sd != null && !sd.isEditable())
            return;

        // store the new time estimate
        DoubleData dd = (estimatedTime == null ? null
                : new DoubleData(estimatedTime.doubleValue(), true));
        ctx.getData().userPutValue(dataName, dd);
    }

    public void saveCompletionDate(RestTask task, Date completionDate) {
        ensureLeaf(task);
        DateData dd = (completionDate == null ? null
                : new DateData(completionDate, true));
        ctx.getData().userPutValue(task.getFullPath() + COMPLETED, dd);
    }

    public void ensureLeaf(RestTask task) {
        if (task == null)
            throw HttpException.notFound();
        if (task.getFullName() == null)
            throw HttpException.badRequest();
        String path = task.getFullPath();
        PropertyKey key = hier.findExistingKey(path);
        if (key == null || hier.getNumChildren(key) > 0)
            throw HttpException.badRequest();
    }

    public List<RestTaskScript> scripts(RestTask task) {
        List<ScriptID> scripts = ScriptEnumerator.getScripts(ctx,
            task.getFullPath());
        if (scripts == null || scripts.isEmpty())
            return Collections.EMPTY_LIST;

        List<RestTaskScript> result = new ArrayList(scripts.size() - 1);
        for (int i = 1; i < scripts.size(); i++) {
            ScriptID script = scripts.get(i);
            String name = script.getDisplayName();
            String uri = script.getHref();
            String path = script.getDataPath();
            boolean trigger = TriggerURI.isTrigger(uri);
            result.add(new RestTaskScript(name, uri, path, trigger));
        }
        return result;
    }

    public void loadChildren(RestTask task, boolean deep) {
        task.put("children", Collections.EMPTY_LIST);
        String nodeID = task.getId();
        PropertyKey key = hier.findKeyByNodeID(nodeID);
        loadChildren(task, key, deep);
    }

    private void loadChildren(RestTask task, PropertyKey key, boolean deep) {
        int numChildren = hier.getNumChildren(key);
        if (numChildren > 0) {
            List<RestTask> children = new ArrayList<RestTask>(numChildren);
            for (int i = 0; i < numChildren; i++) {
                PropertyKey childKey = hier.getChildKey(key, i);
                String childID = hier.pget(childKey).getNodeID();
                RestTask childTask = new RestTask(childID, childKey.name());
                if (deep)
                    loadChildren(childTask, childKey, true);
                children.add(childTask);
            }
            task.put("children", children);
        }
    }


    private static final String EST_TIME = "/Estimated Time";

    private static final String ACT_TIME = "/Time";

    private static final String COMPLETED = "/Completed";

    /*
     * A map whose keys are the names of JSON attributes in a RestTask, and
     * whose values are the data name suffixes used to store those attributes in
     * the repository
     */
    static final Map<String, String> JSON_ATTR_DATA_NAME_MAP = getAttrNameMap();

    private static Map<String, String> getAttrNameMap() {
        Map<String, String> result = new HashMap<String, String>();
        result.put("estimatedTime", EST_TIME);
        result.put("actualTime", ACT_TIME);
        result.put("completionDate", COMPLETED);
        return Collections.unmodifiableMap(result);
    }

}
