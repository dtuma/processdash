// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil


package pspdash;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;


public class EVCalculatorData extends EVCalculator {

    private EVTask taskRoot;
    private EVSchedule schedule;
    private Date scheduleStartDate, effectiveDate, completionDate;

    public EVCalculatorData(EVTask root, EVSchedule schedule) {
        this.taskRoot = root;
        this.schedule = schedule;
    }

    public void recalculate() {
        resetData();
        scheduleStartDate = schedule.getStartDate();

        pruneNodes(taskRoot, false);
        double levelOfEffort = calculateLevelOfEffort(taskRoot);
//      System.out.println("total level of effort = " + (levelOfEffort*100));
        taskRoot.recalcPlanTimes();
        taskRoot.recalcDateCompleted();
        completionDate = taskRoot.dateCompleted;
        effectiveDate = getEffectiveDate();
        evLeaves = new LinkedList();
        getEVLeaves(taskRoot);
        sortEVLeafList(evLeaves);
//        debugEVLeafList(evLeaves);

        // clean up the schedule
        schedule.setLevelOfEffort(levelOfEffort);
        schedule.cleanUp();
        schedule.recalcCumPlanTimes();

        // find time logged to tasks before the start of the schedule.
        TimeLog log = readTimeLog();
        saveTimeBeforeSchedule(log);

        // calculate planned and actual earned values, planned dates, and
        // planned and actual cumulative earned values
        double planDirectTime = calcTaskValues(evLeaves);
        saveCompletedTaskValues(evLeaves);

        // reset the EVMetrics object
        schedule.setEffectiveDate(effectiveDate);
        schedule.getMetrics().reset(scheduleStartDate, effectiveDate,
                                    schedule.getPeriodStart(effectiveDate),
                                    schedule.getPeriodEnd(effectiveDate));

        // record actual time spent on tasks.
        saveTimeDuringSchedule(log);
        schedule.recalcCumActualTimes();

        // recalculate the EVMetrics object
        taskRoot.checkForNodeErrors(schedule.getMetrics(), 0,
                                    new LinkedList(), new LinkedList());
        recalcMetrics(taskRoot, schedule.getMetrics());

        recalculateTaskHierarchy(taskRoot);

        schedule.getMetrics().recalcComplete(schedule);
        schedule.firePreparedEvents();
    }


    protected void resetData() {
        resetRootData(taskRoot);
        resetData(taskRoot);
    }

    protected void resetData(EVTask task) {
        resetNodeData(task);

        if (!task.isLeaf()) {
            task.dateCompleted = null;
            task.dateCompletedEditable = false;
            for (int i = 0;   i < task.getNumChildren();   i++)
                resetData(task.getChild(i));
        }
    }

    protected void pruneNodes(EVTask task, boolean parentIsPruned) {
        // inherit prune value from parent.
        boolean taskIsPruned = parentIsPruned;
        if (task.taskOrdinal > 0)
            // this task has been "unpruned"
            taskIsPruned = false;
        else if (task.taskOrdinal == EVTask.USER_PRUNED)
            // this task has been explicitly pruned.
            taskIsPruned = true;
        else {
            task.taskOrdinal =
                (taskIsPruned ? EVTask.ANCESTOR_PRUNED
                              : EVTask.INFER_FROM_CONTEXT);
        }

        for (int i = 0;   i < task.getNumChildren();   i++)
            pruneNodes(task.getChild(i), taskIsPruned);
    }

    protected double calculateLevelOfEffort(EVTask task) {
        // if this task has a level of effort percentage setting,
        if (task.planLevelOfEffort > 0) {
            // let all of our children know they are "level of effort pruned."
            for (int i = 0;   i < task.getNumChildren();   i++)
                setInheritsLevelOfEffort(task.getChild(i));
            // return the level of effort set on this node.
            if (task.isUserPruned())
                return 0;
            else
                return task.planLevelOfEffort;

        } else {
            // clear our level of effort flag (in case it was previously
            // inherited from a parent which no longer has a LOE setting)
            task.planLevelOfEffort = EVTask.NOT_LEVEL_OF_EFFORT;
            // add up the level of effort values from our descendants, and
            // return it.
            double totalLOE = 0;
            for (int i = 0;   i < task.getNumChildren();   i++)
                totalLOE += calculateLevelOfEffort(task.getChild(i));
            return totalLOE;
        }
    }

    protected void setInheritsLevelOfEffort(EVTask task) {
        task.planLevelOfEffort = 0;
        for (int i = 0;   i < task.getNumChildren();   i++)
            setInheritsLevelOfEffort(task.getChild(i));
    }

    public Date getEffectiveDate() {
        Date result = completionDate;
        if (result == null) result = getTestingEffDate();
        if (result == null) result = new Date();
        return result;
    }

    public Date getTestingEffDate() {
        String setting = Settings.getVal("ev.effectiveDate");
        if (setting == null) return null;
        try {
            return new Date(Long.parseLong(setting));
        } catch (Exception e) {
            return null;
        }
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

    protected void sortEVLeafList(List evLeaves) {
        Collections.sort(evLeaves, new EVLeafComparator(evLeaves));
    }

    protected void sortEVLeafListNoDateSort(List evLeaves) {
        List chronologicallyPrunedLeaves = new LinkedList();
        Iterator i = evLeaves.iterator();
        while (i.hasNext()) {
            EVTask t = (EVTask) i.next();
            if (t.dateCompleted != null &&
                t.dateCompleted.compareTo(scheduleStartDate) < 0) {
                chronologicallyPrunedLeaves.add(t);
                i.remove();
            }
        }
        evLeaves.addAll(0, chronologicallyPrunedLeaves);
    }

    private class EVLeafComparator implements Comparator {
        private List origOrder;
        private boolean reorderCompletedTasks = false;
        private EVLeafComparator(List origList) {
            origOrder = new LinkedList(origList);
            reorderCompletedTasks = Settings.getBool("ev.sortCompletedTasks", true);
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

    protected TimeLog readTimeLog() {
        TimeLog log = new TimeLog();
        try { log.readDefault(); } catch (IOException ioe) {}
        return log;
    }

    protected void saveTimeBeforeSchedule(TimeLog log) {
        TimeLogEntry entry;
        for (int i = log.v.size();   i-- > 0;   ) {
            entry = (TimeLogEntry) log.v.get(i);

            Date d = entry.getStartTime();
            if (d == null || d.compareTo(scheduleStartDate) >= 0) continue;

            EVTask task = taskRoot.getTaskForPath(entry.getPath());
            if (task != null && !task.isLevelOfEffortTask())
                task.actualPreTime += entry.minutesElapsed;
        }
    }


    private double calcTaskValues(List evLeaves) {
        double cumPlanValue = 0;
        Date startDate = scheduleStartDate;
        Iterator i = evLeaves.iterator();
        while (i.hasNext()) {
            EVTask task = (EVTask) i.next();

            if (!task.isLeaf())
                task.actualPreTime = getTotalActualPreTime(task);

            if (task.dateCompleted == null ||
                task.dateCompleted.compareTo(scheduleStartDate) >= 0) {
                task.planValue = task.planTime - task.actualPreTime;
                if (task.planValue < 0) task.planValue = 0;
                cumPlanValue += task.planValue;
                task.cumPlanValue = cumPlanValue;
                task.planStartDate = startDate;
                task.planDate = schedule.getPlannedCompletionDate
                    (cumPlanValue, cumPlanValue);
                startDate = task.planDate;

                if (task.dateCompleted != null)
                    task.valueEarned = task.planValue;
            }
        }
        return cumPlanValue;
    }

    private double getTotalActualPreTime(EVTask task) {
        double result = task.actualPreTime;
        for (int i = 0;   i < task.getNumChildren();   i++)
            result += getTotalActualPreTime(task.getChild(i));

        return result;
    }

    private void saveCompletedTaskValues(List evLeaves) {
        Iterator i = evLeaves.iterator();
        while (i.hasNext()) {
            EVTask task = (EVTask) i.next();
            if (task.valueEarned > 0)
                schedule.saveCompletedTask
                    (task.dateCompleted, task.valueEarned);
        }
    }


    private void saveTimeDuringSchedule(TimeLog log) {
        for (int i = log.v.size();   i-- > 0;   )
            saveTimeDuringSchedule((TimeLogEntry) log.v.get(i));
    }

    private void saveTimeDuringSchedule(TimeLogEntry entry) {
        Date d = entry.getStartTime();
        if (d == null || d.compareTo(scheduleStartDate) < 0) return;

        EVTask task = taskRoot.getTaskForPath(entry.getPath());
        if (task == null) return;

        if (task.isLevelOfEffortTask()) {
            // for level of effort tasks, ignore time logged outside the
            // effective date of the schedule.
            if (d.compareTo(effectiveDate) < 0) {
                task.actualNodeTime += entry.minutesElapsed;
                schedule.getMetrics().addIndirectTime(entry.minutesElapsed);
                schedule.saveActualIndirectTime(d, entry.minutesElapsed);
            }
            return;
        }

        task.actualNodeTime += entry.minutesElapsed;
        if (task.actualStartDate == null ||
            task.actualStartDate.compareTo(d) < 0)
            task.actualStartDate = d;

        if (task.isUserPruned()) return;

        schedule.saveActualTime(d, entry.minutesElapsed);
    }



    private void recalcMetrics(EVTask task, EVMetrics metrics) {
        if (task.planDate != null)
            metrics.addTask(task.planValue, task.actualNodeTime,
                            task.planDate, task.dateCompleted);
        else {
            for (int i = task.getNumChildren();   i-- > 0;   )
                recalcMetrics(task.getChild(i), metrics);
            // if they logged time against a non-leaf node, it counts
            // against their metrics right away.  Treat it as an
            // imaginary task with no planned time, which should have
            // been completed instantaneously when the schedule started
            if (task.actualNodeTime > 0 &&
                !task.isLevelOfEffortTask() && !task.isUserPruned())
                metrics.addTask(0, task.actualNodeTime, null, metrics.startDate());
        }
    }


    private void recalculateTaskHierarchy(EVTask task) {
        /*boolean isEVLeaf = EVTask.containsNode(evLeaves, task);
        if (isEVLeaf || task.isLeaf()) {
            task.actualCurrentTime = task.actualNodeTime;
            if (!task.isLeaf())
                sumUpBumChildrenOfEVLeaf(task);
            else if (isEVLeaf)
                task.actualDirectTime = task.actualNodeTime;
            task.actualTime = task.actualNodeTime + task.actualPreTime;
            return;
        }*/

        for (int i = task.getNumChildren();   i-- > 0;   )
            recalculateTaskHierarchy(task.getChild(i));

        sumUpNodeData(task);

        if (EVTask.containsNode(evLeaves, task) && !task.isLeaf())
            updateBumChildrenOfEVLeaf(task);
    }

    private void updateBumChildrenOfEVLeaf(EVTask task) {
        for (int i = task.getNumChildren();   i-- > 0;   ) {
            EVTask child = task.getChild(i);
            child.planDate = task.planDate;
            child.planStartDate = task.planStartDate;
            child.cumPlanValue = task.cumPlanValue;

            updateBumChildrenOfEVLeaf(child);
        }
    }





    /*

Values I won't recalculate:

    EVTask parent = null;
    ArrayList children = new ArrayList();
    Listener listener;
    DataRepository data;
    String name, fullName, taskListName;
    int savedTaskOrdinal = 0;
    boolean planTimeEditable, planTimeNull, planTimeUndefined,
        dateCompletedEditable;
    boolean ignorePlanTimeValue = false;

Value to recalculate

    double planLevelOfEffort = -1;
    int taskOrdinal = 0;


    double planTime,  cumPlanTime,  actualTime;  // expressed in minutes
    double actualNodeTime, valueEarned;          // expressed in minutes
    double topDownPlanTime, bottomUpPlanTime;    // expressed in minutes
    Date planDate, dateCompleted;
    Date planStartDate, actualStartDate;

     *
     */

}
