// Copyright (C) 2006-2017 Tuma Solutions, LLC
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

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.net.cache.ObjectCache;
import net.sourceforge.processdash.util.StringUtils;

public class EVDependencyCalculator {

    private DataRepository data;

    private DashHierarchy hier;

    private ObjectCache cache;


    public EVDependencyCalculator(DataRepository data, DashHierarchy hier,
            ObjectCache cache) {
        this.data = data;
        this.hier = hier;
        this.cache = cache;
    }

    public void recalculate(Collection dependencies) {
        Map taskLists = new HashMap();
        openTaskListsForDependencies(taskLists, dependencies);
        updateDependencies(taskLists, dependencies);
    }

    public void recalculate(EVTaskList taskList) {
        EVTask root = (EVTask) taskList.getRoot();
        Map taskLists = new HashMap();
        taskLists.put(taskList.taskListName, taskList);

        // scan the tree and open the necessary EV schedules.
        new TaskListCollector(taskLists).visit(root);

        // use them to update dependency information
        new DependencyUpdater(taskLists).visit(root);
    }

    private void openTaskListsForDependencies(Map taskLists,
            Collection dependencies) {
        if (hasValue(dependencies))
            for (Iterator i = dependencies.iterator(); i.hasNext();)
                openTaskListForDependency(taskLists,
                        (EVTaskDependency) i.next());
    }

    private void openTaskListForDependency(Map taskLists, EVTaskDependency d) {
        // first, try to honor the task list named by the dependency.
        // (This policy may need to be revisited.)
        String taskListName = d.getTaskListName();
        if (openTaskList(taskLists, taskListName))
            return;

        // try to find the best task list for this dependency.
        String taskID = d.getTaskID();
        List lists = EVTaskDependencyResolver.getInstance()
                .getTaskListsContaining(taskID);
        if (hasValue(lists)) {
            taskListName = (String) lists.get(0);
            d.setTaskListName(taskListName);
            openTaskList(taskLists, taskListName);
        }
    }

    private boolean openTaskList(Map taskLists, String taskListName) {
        if (taskListName != null) {
            if (taskLists.containsKey(taskListName))
                // we've already opened that task list (or attempted to)
                return taskLists.get(taskListName) != null;

            EVTaskList tl = EVTaskList.openExisting(taskListName,
                    data, hier, cache, false);
            taskLists.put(taskListName, tl);
            if (tl != null) {
                // we were able to successfully open the task list.
                tl.recalc();
                return true;
            }
        }

        return false;
    }


    private void updateDependencies(Map taskLists, Collection dependencies) {
        if (hasValue(dependencies))
            for (Iterator i = dependencies.iterator(); i.hasNext();) {
                EVTaskDependency d = (EVTaskDependency) i.next();
                updateDependency(taskLists, d);
            }
    }

    private void updateDependency(Map taskLists, EVTaskDependency d) {
        String assignedTo = d.getAssignedTo();
        double percentComplete = d.getPercentComplete();
        Date projectedDate = d.getProjectedDate();
        boolean unresolvable = (assignedTo == null && percentComplete == 0);
        String displayName = d.getDisplayName();

        String taskListName = d.getTaskListName();
        if (taskListName != null) {
            EVTaskList taskList = (EVTaskList) taskLists.get(taskListName);
            if (taskList != null) {
                StatusCollector c = new StatusCollector(d.getTaskID(),
                        taskList.getRootName(), taskList.getID());
                c.visit((EVTask) taskList.getRoot());
                if (c.foundTask) {
                    unresolvable = false;
                    assignedTo = c.getAssignedTo();
                    percentComplete = c.getPercentComplete();
                    projectedDate = c.getProjectedDate();
                    displayName = c.getTaskDisplayName();
                }
            }
        }

        d.setResolvedDetails(unresolvable, assignedTo, percentComplete,
                projectedDate, displayName);
    }

    public void addReverseAndCollabDependencies(EVTask t) {
        // first, remove the reverse/collab dependency entries if present.
        if (hasValue(t.getDependencies())) {
            for (Iterator i = t.getDependencies().iterator(); i.hasNext();) {
                EVTaskDependency d = (EVTaskDependency) i.next();
                if (d.isReverse() || d.isCollab())
                    i.remove();
            }
        }

        // now, find any reverse dependencies for this task
        Map waitingPeople = EVTaskDependencyResolver.getInstance()
                .getIndividualsWaitingOnTask(null, t.getTaskIDs(),
                        ProcessDashboard.getOwnerName(data));
        if (hasValue(waitingPeople))
            t.getDependencies(true).add(new EVTaskDependency(waitingPeople));

        // now add a collab dependency if applicable
        if (Settings.isPersonalMode()) {
            EVTaskDependency collab = EVTaskDependency
                    .getTaskCollaborators(data, t.fullName);
            if (collab != null)
                t.getDependencies(true).add(collab);
        }
    }

    public void setParentDateForDependencies(EVTask t) {
        if (hasValue(t.getDependencies())) {
            for (Iterator i = t.getDependencies().iterator(); i.hasNext();) {
                EVTaskDependency d = (EVTaskDependency) i.next();
                d.loadParentDate(t);
            }
        }
    }


    private abstract class EVTaskVistor {

        public void visit(EVTask t) {
            enter(t);
            if (t.isLeaf())
                leaf(t);
            else {
                node(t);
                for (int i = t.getNumChildren(); i-- > 0;)
                    visit(t.getChild(i));
            }
            leave(t);
        }

        protected void enter(EVTask t) {
        }

        protected void node(EVTask t) {
        }

        protected void leaf(EVTask t) {
        }

        protected void leave(EVTask t) {
        }
    }

    private class TaskListCollector extends EVTaskVistor {

        Map taskLists;

        public TaskListCollector(Map taskLists) {
            this.taskLists = taskLists;
        }

        protected void enter(EVTask t) {
            openTaskListsForDependencies(taskLists, t.getDependencies());
        }
    }

    private class DependencyUpdater extends EVTaskVistor {
        Map taskLists;

        public DependencyUpdater(Map taskLists) {
            this.taskLists = taskLists;
        }

        protected void enter(EVTask t) {
            updateDependencies(taskLists, t.getDependencies());
            addReverseAndCollabDependencies(t);
            setParentDateForDependencies(t);
        }

    }

    private class StatusCollector extends EVTaskVistor {
        String taskID;

        String extraPath;

        private String taskListID;

        boolean collecting;

        boolean foundTask;

        String taskDisplayName;

        Set people;

        int taskCount;

        double planValue;

        int completedTaskCount;

        double completedValue;

        Date projectedDate;

        private String rootDisplayName;

        private List canonicalTaskRoots;

        public StatusCollector(String taskID, String rootDisplayName,
                String taskListID) {
            this.taskID = taskID;
            this.taskListID = taskListID;
            int slashPos = taskID.indexOf('/');
            if (slashPos != -1) {
                this.extraPath = taskID.substring(slashPos);
                this.taskID = taskID.substring(0, slashPos);
            } else {
                this.extraPath = null;
            }
            this.rootDisplayName = "/" + rootDisplayName;
            this.collecting = this.foundTask = false;
            this.taskDisplayName = null;
            this.people = new TreeSet();
            this.planValue = this.completedValue = 0;
            this.projectedDate = null;
            this.canonicalTaskRoots = new LinkedList();
        }


        private boolean isIDMatch(EVTask t) {
            if (hasValue(t.getTaskIDs()))
                return t.getTaskIDs().contains(taskID);
            else if (t.getFlag() != null)
                return taskID.equals(EVTaskDependencyResolver
                        .getPseudoTaskIdForTaskList(taskListID));
            else
                return false;
        }

        private boolean isTaskMatch(EVTask t) {
            return extraPathMatches(extraPath, t);
        }

        private boolean extraPathMatches(String extraPath, EVTask t) {
            if (extraPath == null || extraPath.length() == 0)
                return isIDMatch(t);

            if (t.getParent() == null || t.getFlag() != null)
                return false;

            String pathTail = t.getName();
            if (!pathTail.startsWith("/"))
                pathTail = "/" + pathTail;
            if (!extraPath.endsWith(pathTail))
                return false;

            String parentExtraPath =
                extraPath.substring(0, extraPath.length() - pathTail.length());
            return extraPathMatches(parentExtraPath, t.getParent());
        }




        protected void enter(EVTask t) {
            if (isTaskMatch(t)) {
                taskDisplayName = getDisplayNameForTask(t);
                foundTask = collecting = true;
                planValue += t.getPlanValue();
                completedValue += t.getValueEarned();
            }
        }

        protected void leaf(EVTask t) {
            if (collecting && !t.isTotallyPruned()) {
                if (hasValue(t.getAssignedTo()))
                    people.addAll(t.getAssignedTo());

                projectedDate = EVCalculator.maxPlanDate(projectedDate,
                        EVTaskDependency.getDependencyComparisonDate(t));

                taskCount++;
                if (t.getActualDate() != null)
                    completedTaskCount++;
            }
        }

        protected void leave(EVTask t) {
            if (isTaskMatch(t))
                collecting = false;
        }

        private String getDisplayNameForTask(EVTask t) {
            if (t.flag != null)
                return null;
            else {
                String parentName = getDisplayNameForTask(t.parent);
                if (parentName != null)
                    return parentName + "/" + t.getName();

                canonicalTaskRoots.add(t);
                return "";
            }
        }



        public double getPercentComplete() {
            if (planValue > 0) {
                if (planValue == completedValue
                        && taskCount > completedTaskCount)
                    // this would indicate some incomplete tasks with 0 planned
                    // hours. We don't want to report that the task is
                    // complete until ALL tasks are marked complete.
                    return 0.99;
                else
                    return completedValue / planValue;

            } else if (taskCount > 0) {
                return (double) completedTaskCount / (double) taskCount;

            } else
                return 0;
        }

        public String getAssignedTo() {
            if (people.isEmpty())
                return null;
            else
                return StringUtils.join(people, ", ");
        }

        public Date getProjectedDate() {
            return projectedDate;
        }

        public String getTaskDisplayName() {
            String rootName = rootDisplayName;

            if (hasValue(canonicalTaskRoots)) {
                List bestTaskIDs = EVTaskListMerger
                        .getBestTaskIDs(canonicalTaskRoots);
                String canonicalTaskName = EVTaskDependencyResolver
                        .getInstance().getCanonicalTaskName(bestTaskIDs);
                if (canonicalTaskName != null)
                    rootName = canonicalTaskName;
                else {
                    EVTask t = (EVTask) canonicalTaskRoots.get(0);
                    rootName = pathConcat(rootDisplayName, t.getName());
                }
            }

            return pathConcat(rootName, taskDisplayName);
        }

    }

    private static boolean hasValue(Collection c) {
        return c != null && !c.isEmpty();
    }

    private static boolean hasValue(Map m) {
        return m != null && !m.isEmpty();
    }

    private static String pathConcat(String a, String b) {
        if (b == null || b.length() == 0)
            return a;
        else if (a == null || a.length() == 0)
            return b;
        else if (b.startsWith("/"))
            return a + b;
        else
            return a + "/" + b;
    }

}
