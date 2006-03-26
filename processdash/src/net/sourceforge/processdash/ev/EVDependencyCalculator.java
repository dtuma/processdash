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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.net.cache.ObjectCache;
import net.sourceforge.processdash.util.StringUtils;

public class EVDependencyCalculator {

    private DataRepository data;

    private DashHierarchy hier;

    private ObjectCache cache;

    private EVTaskList taskList;


    public EVDependencyCalculator(EVTaskList taskList, DataRepository data,
            DashHierarchy hier, ObjectCache cache) {
        this.data = data;
        this.hier = hier;
        this.cache = cache;
        this.taskList = taskList;
    }

    public void recalculate() {
        String myName = taskList.taskListName;
        EVTask root = (EVTask) taskList.getRoot();

        // scan the tree and make a list of the EV schedules we need to open.
        Set taskListNames = new TaskListNameCollector(root).names;
        taskListNames.remove(myName);

        // open those task lists
        Map taskLists = openTaskLists(taskListNames);
        taskLists.put(myName, taskList);

        // use them to update dependency information
        new DependencyUpdater(taskLists).visit(root);
    }

    private Map openTaskLists(Set taskListNames) {
        Map result = new HashMap();
        for (Iterator i = taskListNames.iterator(); i.hasNext();) {
            String name = (String) i.next();
            EVTaskList taskList = EVTaskList.openExisting(name, data, hier,
                    cache, false);
            if (taskList != null) {
                taskList.recalc();
                result.put(name, taskList);
            }
        }
        return result;
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

    private class TaskListNameCollector extends EVTaskVistor {

        Set names;

        public TaskListNameCollector(EVTask root) {
            names = new HashSet();
            visit(root);
        }

        protected void enter(EVTask t) {
            List dependencies = t.getDependencies();
            if (!hasValue(dependencies))
                return;

            for (Iterator i = dependencies.iterator(); i.hasNext();) {
                EVTaskDependency d = (EVTaskDependency) i.next();
                String taskListName = d.getTaskListName();
                if (taskListName == null) {
                    String taskID = d.getTaskID();
                    List taskLists = EVTaskDependencyResolver.getInstance()
                            .getTaskListsContaining(taskID);
                    if (hasValue(taskLists)) {
                        taskListName = (String) taskLists.get(0);
                        d.setTaskListName(taskListName);
                    }
                }
                if (taskListName != null)
                    names.add(taskListName);
            }
        }
    }

    private class DependencyUpdater extends EVTaskVistor {
        Map taskLists;

        public DependencyUpdater(Map taskLists) {
            this.taskLists = taskLists;
        }

        protected void enter(EVTask t) {
            List dependencies = t.getDependencies();
            if (hasValue(dependencies))
                for (Iterator i = dependencies.iterator(); i.hasNext();) {
                    EVTaskDependency d = (EVTaskDependency) i.next();
                    update(d);
                }
        }

        private void update(EVTaskDependency d) {
            String assignedTo = d.getAssignedTo();
            double percentComplete = d.getPercentComplete();
            Date planDate = d.getPlannedDate();
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
                        planDate = c.getPlannedDate();
                        displayName = c.getTaskDisplayName();
                    }
                }
            }

            d.setResolvedDetails(unresolvable, assignedTo, percentComplete,
                    planDate, displayName);
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

        Date plannedDate;

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
            this.plannedDate = null;
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
                plannedDate = EVCalculator.maxPlanDate(plannedDate,
                        t.getPlanDate());
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

        public Date getPlannedDate() {
            return plannedDate;
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
