//Copyright (C) 2003-2008 Tuma Solutions, LLC
//Process Dashboard - Data Automation Tool for high-maturity processes
//
//This program is free software; you can redistribute it and/or
//modify it under the terms of the GNU General Public License
//as published by the Free Software Foundation; either version 2
//of the License, or (at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program; if not, write to the Free Software
//Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
//The author(s) may be contacted at:
//Process Dashboard Group
//c/o Ken Raisor
//6137 Wardleigh Road
//Hill AFB, UT 84056-5843
//
//E-Mail POC:  processdash-devel@lists.sourceforge.net


package net.sourceforge.processdash.ev;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public abstract class EVCalculator {

    protected List evLeaves;
    protected Date scheduleStartDate;
    protected EVSnapshot baselineDataSource;
    protected boolean reorderCompletedTasks = true;


    public abstract void recalculate();


    public List getEVLeaves() {
        return evLeaves;
    }

    protected void sortEVLeafList(List evLeaves) {
        Collections.sort(evLeaves, new EVLeafComparator(evLeaves));
    }

    private class EVLeafComparator implements Comparator {
        private List origOrder;
        private EVLeafComparator(List origList) {
            origOrder = new LinkedList(origList);
        }
        public int compare(Object o1, Object o2) {
            EVTask t1 = (EVTask) o1;
            EVTask t2 = (EVTask) o2;

            // put completed tasks at the front of the list, in order of
            // completion
            int result = compareDates(t1.dateCompleted, t2.dateCompleted);
            if (result != 0) return result;

            // next, order by task ordinal
            result = t1.taskOrdinal - t2.taskOrdinal;
            if (result != 0) return result;

            // finally, return items in the order they appeared in the
            // original list.
            result = origOrder.indexOf(t1) - origOrder.indexOf(t2);
            return result;
        }
        private int compareDates(Date a, Date b) {
            if (!reorderCompletedTasks) {
                if (a != null && a.compareTo(scheduleStartDate) > 0) a = null;
                if (b != null && b.compareTo(scheduleStartDate) > 0) b = null;
            }
            if (a == b) return 0;
            if (a == null) return 1;
            if (b == null) return -1;
            return a.compareTo(b);
        }
        public boolean equals(Object obj) {
            return this == obj;
        }
    }


    public void debugEVLeafList(List evLeaves) {
        System.out.println("EV Leaves:");
        Iterator i = evLeaves.iterator();
        while (i.hasNext()) {
            EVTask element = (EVTask) i.next();
            System.out.println(element.fullName);
        }
    }

    protected static void resetRootData(EVTask taskRoot) {
        taskRoot.topDownPlanTime = taskRoot.bottomUpPlanTime = 0;
        taskRoot.dateCompleted = null;
        taskRoot.dateCompletedEditable = false;
        //taskRoot.taskOrdinal = 1;
    }

    protected static void resetNodeData(EVTask task) {
        task.planDate = task.planStartDate = task.actualStartDate =
            task.replanDate = task.forecastDate = null;

        task.planTime = task.planValue = task.cumPlanValue =
            task.actualPreTime = task.actualNodeTime = task.actualDirectTime =
            task.actualTime = task.actualCurrentTime = task.valueEarned = 0;
    }


    protected static void sumUpNodeData(EVTask task) {
        if (!task.isLevelOfEffortTask() && !task.isUserPruned())
            task.actualDirectTime = task.actualNodeTime;

        task.actualTime = task.actualNodeTime + task.actualPreTime;
        task.actualCurrentTime = task.actualNodeTime;

        Date replanDate = EVSchedule.A_LONG_TIME_AGO;
        Date forecastDate = EVSchedule.A_LONG_TIME_AGO;

        for (int i = task.getNumChildren();   i-- > 0;   ) {
            EVTask child = task.getChild(i);

            task.planValue += child.planValue;
            task.cumPlanValue =
                Math.max(task.cumPlanValue, child.cumPlanValue);

            task.actualTime        += child.actualTime;
            task.actualCurrentTime += child.actualCurrentTime;
            task.actualDirectTime  += child.actualDirectTime;
            task.valueEarned       += child.valueEarned;

            task.planStartDate =
                minStartDate(task.planStartDate, child.planStartDate);
            task.actualStartDate =
                minStartDate(task.actualStartDate, child.actualStartDate);

            task.planDate = maxPlanDate(task.planDate, child.planDate);

            if (child.planValue > 0) {
                replanDate = maxForecastDate(replanDate, child.replanDate);
                forecastDate = maxForecastDate(forecastDate, child.forecastDate);
            }
        }

        if (task.replanDate == null && replanDate != EVSchedule.A_LONG_TIME_AGO)
            task.replanDate = replanDate;
        if (task.forecastDate == null
                && forecastDate != EVSchedule.A_LONG_TIME_AGO)
            task.forecastDate = forecastDate;
    }


    public static Date minStartDate(Date a, Date b) {
        if (a == null) return b;
        if (b == null) return a;
        if (a.compareTo(b) < 0) return a;
        return b;
    }

    public static Date minStartDate(Collection dates) {
        Date result = null;
        if (dates != null) {
            for (Iterator i = dates.iterator(); i.hasNext();)
                result = minStartDate(result, (Date) i.next());
        }
        return result;
    }


    protected static Date maxPlanDate(Date a, Date b) {
        if (a == null) return b;
        if (b == null) return a;
        if (a.compareTo(b) > 0) return a;
        return b;
    }


    protected static Date maxForecastDate(Date a, Date b) {
        if (a == null || b == null) return null;
        if (a.compareTo(b) > 0) return a;
        return b;
    }


    protected void pruneNodes(EVTask task, boolean parentIsPruned) {
        // inherit prune value from parent.
        boolean taskIsPruned = parentIsPruned;
        if (task.pruningFlag == EVTask.USER_UNPRUNED) {
            // this task has been explicitly "unpruned"
            taskIsPruned = false;
            if (parentIsPruned == false)
                // use INFER_FROM_CONTEXT if it would have the same effect
                // as an explicit unpruning.
                task.pruningFlag = EVTask.INFER_FROM_CONTEXT;
        } else if (task.pruningFlag == EVTask.USER_PRUNED)
            // this task has been explicitly pruned.
            taskIsPruned = true;
        else {
            task.pruningFlag =
                (taskIsPruned ? EVTask.ANCESTOR_PRUNED
                              : EVTask.INFER_FROM_CONTEXT);
        }

        for (int i = 0;   i < task.getNumChildren();   i++)
            pruneNodes(task.getChild(i), taskIsPruned);
    }


    protected void getEVLeaves(EVTask task) {
        if (task.isEVLeaf()) {
            if (!task.isUserPruned() && !task.isLevelOfEffortTask())
                evLeaves.add(task);
        } else {
            for (int i = 0;   i < task.getNumChildren();   i++)
                getEVLeaves(task.getChild(i));
        }
    }


    public void setBaselineDataSource(EVSnapshot baselineDataSource) {
        this.baselineDataSource = baselineDataSource;
    }

    public EVSnapshot getBaselineDataSource() {
        return baselineDataSource;
    }

    protected void recalcBaselineData(EVTask taskRoot) {
        EVTask baselineTaskRoot = findBaselineTaskRoot(taskRoot);
        calcBaselineTaskData(taskRoot, baselineTaskRoot);
    }

    protected EVTask findBaselineTaskRoot(EVTask taskRoot) {
        if (baselineDataSource == null)
            return null;
        EVTask baselineDataRoot = baselineDataSource.getTaskList()
                .getTaskRoot();
        return baselineDataRoot.findByTaskIDs(taskRoot.getTaskIDs());
    }

    protected static void calcBaselineTaskData(EVTask taskRoot,
            EVTask baselineRoot) {
        if (baselineRoot == null) {
            resetBaselineData(taskRoot);
        } else {
            Map<String, EVTask> baselineIdCache = new HashMap<String, EVTask>();
            buildTaskIdCache(baselineIdCache, baselineRoot);
            calcBaselineTaskData(taskRoot, baselineRoot, baselineIdCache);
        }
    }

    private static void calcBaselineTaskData(EVTask task, EVTask baselineSrc,
            Map<String, EVTask> baselineIdCache) {
        for (int i = task.getNumChildren(); i-- > 0;)
            calcBaselineTaskData(task.getChild(i), baselineIdCache, baselineSrc);

        copyBaselineData(task, baselineSrc, true);
    }

    private static void calcBaselineTaskData(EVTask task,
            Map<String, EVTask> baselineIdCache, EVTask baselineParent) {
        EVTask baselineSrc = findInBaseline(task, baselineParent,
            baselineIdCache);
        calcBaselineTaskData(task, baselineSrc, baselineIdCache);
    }

    private static EVTask findInBaseline(EVTask task, EVTask baselineParent,
            Map<String, EVTask> baselineIdCache) {
        // sanity check on parameters
        if (task == null)
            return null;

        // if the given task has task IDs, use the cache to look them up in the
        // baseline.
        List taskIDs = task.getTaskIDs();
        if (taskIDs != null && baselineIdCache != null) {
            for (Iterator i = taskIDs.iterator(); i.hasNext();) {
                String taskID = (String) i.next();
                EVTask result = baselineIdCache.get(taskID);
                if (result != null)
                    return result;
            }
        }

        // the current task could not be found in the baseline ID cache.
        // see if it can be located by name under the baseline parent.
        if (baselineParent != null) {
            for (int i = baselineParent.getNumChildren();  i-- > 0; ) {
                EVTask baselineChild = baselineParent.getChild(i);
                if (baselineChild.getName().equals(task.getName()))
                    return baselineChild;
            }
        }

        // no luck!  Return null.
        return null;
    }

    /** Discard any baseline data for the given task, and all its children */
    protected static void resetBaselineData(EVTask task) {
        task.baselineDate = null;
        task.baselineTime = 0;
        for (int i = task.getNumChildren();   i-- > 0; )
            resetBaselineData(task.getChild(i));
    }

    /** Initialize the baseline data for a task.
     * 
     * @param task the task whose baseline data should be set.
     * @param baselineSrc the task in the baseline data source that corresponds
     *       to the given task
     * @param sum when baselineSrc is null, if this parameter is true, the
     *       baseline data for the task will be summed from its children.
     *       Otherwise, if this parameter is false, the baseline data for the
     *       task will be reset.
     */
    protected static void copyBaselineData(EVTask task, EVTask baselineSrc,
            boolean sum) {
        if (baselineSrc == null) {
            Date date = null;
            double time = 0;

            if (sum) {
                for (int i = task.getNumChildren();   i-- > 0; ) {
                    EVTask child = task.getChild(i);
                    time += child.baselineTime;
                    date = maxPlanDate(date, child.baselineDate);
                }
            }

            task.baselineDate = date;
            task.baselineTime = time;
        } else {
            task.baselineDate = baselineSrc.planDate;
            task.baselineTime = baselineSrc.planTime;
        }
    }

    /** Populate a Map so it can be used to look up EVTask objects by their
     * task IDs.
     */
    protected static void buildTaskIdCache(Map<String, EVTask> cache,
            EVTask task) {
        List ids = task.getTaskIDs();
        if (ids != null) {
            for (Iterator i = ids.iterator(); i.hasNext();) {
                String id = (String) i.next();
                cache.put(id, task);
            }
        }
        for (int i = task.getNumChildren(); i-- > 0;)
            buildTaskIdCache(cache, task.getChild(i));
    }
}
