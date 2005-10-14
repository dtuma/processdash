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

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.ev.ci.ConfidenceInterval;
import net.sourceforge.processdash.ev.ci.EVCostConfidenceInterval;
import net.sourceforge.processdash.ev.ci.EVScheduleConfidenceIntervals;
import net.sourceforge.processdash.ev.ci.EVTimeErrConfidenceInterval;
import net.sourceforge.processdash.log.time.DashboardTimeLog;
import net.sourceforge.processdash.log.time.IONoSuchElementException;
import net.sourceforge.processdash.log.time.TimeLog;
import net.sourceforge.processdash.log.time.TimeLogEntry;


public class EVCalculatorData extends EVCalculator {

    private EVTask taskRoot;
    private EVSchedule schedule;
    private TimeLog timeLog;
    private Date effectiveDate, completionDate;

    public EVCalculatorData(EVTask root, EVSchedule schedule) {
        this.taskRoot = root;
        this.schedule = schedule;
        this.timeLog = DashboardTimeLog.getDefault();
    }

    public void recalculate() {
        resetData();
        scheduleStartDate = schedule.getStartDate();
        reorderCompletedTasks =
            Settings.getBool("ev.sortCompletedTasks", true);

        pruneNodes(taskRoot, false);
        double levelOfEffort = calculateLevelOfEffort(taskRoot);
//      System.out.println("total level of effort = " + (levelOfEffort*100));
        taskRoot.recalcPlanTimes();
        taskRoot.recalcDateCompleted();
        completionDate = taskRoot.dateCompleted;
        effectiveDate = getEffectiveDate();
        evLeaves = new LinkedList();
        getEVLeaves(taskRoot);
        if (containsTaskOrdinals(taskRoot))
            assignTaskOrdinals(taskRoot, 1);
        sortEVLeafList(evLeaves);
//        debugEVLeafList(evLeaves);

        // clean up the schedule
        schedule.setLevelOfEffort(levelOfEffort);
        schedule.cleanUp();
        schedule.recalcCumPlanTimes();

        // find time logged to tasks before the start of the schedule.
        TimeLog log = timeLog;
        try {
            saveTimeBeforeSchedule(log);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            schedule.getMetrics().addError
                ("Unable to retrieve time log data.", taskRoot);
        }

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
        try {
            saveTimeDuringSchedule(log);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            schedule.getMetrics().addError
                ("Unable to retrieve time log data.", taskRoot);
        }
        schedule.recalcCumActualTimes();

        // recalculate the EVMetrics object
        taskRoot.checkForNodeErrors(schedule.getMetrics(), 0,
                                    new LinkedList(), new LinkedList());
        recalcMetrics(taskRoot, schedule.getMetrics());

        recalculateTaskHierarchy(taskRoot);

        // create confidence intervals for cost, time, and schedule
        createCostConfidenceInterval();
        createTimeErrConfidenceInterval();
        createScheduleConfidenceInterval();

        schedule.getMetrics().recalcComplete(schedule);
        schedule.firePreparedEvents();
    }

    private boolean containsTaskOrdinals(EVTask task) {
        if (task.taskOrdinal > 0) return true;
        for (int i = 0;   i < task.getNumChildren();   i++)
            if (containsTaskOrdinals(task.getChild(i)))
                return true;

        return false;
    }

    private int assignTaskOrdinals(EVTask task, int defaultOrdinal) {

        if (task.isLevelOfEffortTask())
            return defaultOrdinal;

        if (task.taskOrdinal != EVTask.INFER_FROM_CONTEXT)
            defaultOrdinal = task.taskOrdinal;
        else if (EVTask.containsNode(evLeaves, task))
            task.taskOrdinal = defaultOrdinal;

        for (int i = 0;   i < task.getNumChildren();   i++)
            defaultOrdinal = assignTaskOrdinals
                (task.getChild(i), defaultOrdinal);

        return defaultOrdinal;
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



    protected void saveTimeBeforeSchedule(TimeLog log) throws IOException {
        try {
            Iterator entries = log.filter(null, null, scheduleStartDate);
            while (entries.hasNext()) {
                TimeLogEntry entry = (TimeLogEntry) entries.next();

                Date d = entry.getStartTime();
                if (d == null || d.compareTo(scheduleStartDate) >= 0) continue;

                EVTask task = taskRoot.getTaskForPath(entry.getPath());
                if (task != null && !task.isLevelOfEffortTask())
                    task.actualPreTime += entry.getElapsedTime();
            }
        } catch (IONoSuchElementException ion) {
            throw ion.getIOException();
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


    private void saveTimeDuringSchedule(TimeLog log) throws IOException {
        try{
            Iterator entries = log.filter(null, scheduleStartDate, null);
            while (entries.hasNext())
                saveTimeDuringSchedule((TimeLogEntry) entries.next());
        } catch (IONoSuchElementException ion) {
            throw ion.getIOException();
        }
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
                task.actualNodeTime += entry.getElapsedTime();
                schedule.getMetrics().addIndirectTime(entry.getElapsedTime());
                schedule.saveActualIndirectTime(d, entry.getElapsedTime());
            }
            return;
        }

        task.actualNodeTime += entry.getElapsedTime();
        if (task.actualStartDate == null ||
            task.actualStartDate.compareTo(d) < 0)
            task.actualStartDate = d;

        if (task.isUserPruned()) return;

        schedule.saveActualTime(d, entry.getElapsedTime());
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
                metrics.addTask(0, task.actualNodeTime, null,
                                metrics.startDate());
        }
    }


    private void recalculateTaskHierarchy(EVTask task) {
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


    public List getReorderableEVLeaves() {
        List result = new LinkedList(evLeaves);
        Iterator i = result.iterator();
        while (i.hasNext()) {
            EVTask task = (EVTask) i.next();
            Date d = task.dateCompleted;
            if (d != null &&
                (reorderCompletedTasks || d.compareTo(scheduleStartDate) < 0))
                i.remove();
        }
        return result;
    }



    private void createCostConfidenceInterval() {
        ConfidenceInterval costInterval = null;
        if (completionDate == null) {
            costInterval = new EVCostConfidenceInterval
                (getConfidenceIntervalTasks());
            double totalPlan = schedule.getMetrics().totalPlan();
            double completedTasks = schedule.getMetrics().earnedValue();
            double incompleteTime = totalPlan - completedTasks;
            costInterval.setInput(incompleteTime);

            if (!(costInterval.getViability() > ConfidenceInterval.ACCEPTABLE))
                costInterval = null;
        }

        schedule.getMetrics().setCostConfidenceInterval(costInterval);
    }

    private List getConfidenceIntervalTasks() {
        List result = new LinkedList(getEVLeaves());
        addHistoricalTaskLiability(taskRoot, result);
        return result;
    }

    private void addHistoricalTaskLiability(EVTask task, List tasks) {
        if (task.actualNodeTime > 0 &&
            !task.isLevelOfEffortTask() && !task.isUserPruned() &&
            !tasks.contains(task))
            tasks.add(task);
        for (int i = 0;   i < task.getNumChildren();   i++)
            addHistoricalTaskLiability(task.getChild(i), tasks);
    }



    private void createTimeErrConfidenceInterval() {
        ConfidenceInterval timeErrInterval =
            new EVTimeErrConfidenceInterval(schedule, false);
        if (!(timeErrInterval.getViability() > ConfidenceInterval.ACCEPTABLE))
            timeErrInterval = null;
        schedule.getMetrics().setTimeErrConfidenceInterval(timeErrInterval);
    }


    private void createScheduleConfidenceInterval() {
        EVMetrics metrics = schedule.getMetrics();
        try {
            ConfidenceInterval costInterval =
                metrics.getCostConfidenceInterval();
            ConfidenceInterval timeErrInterval =
                metrics.getTimeErrConfidenceInterval();

            ConfidenceInterval completionDate = null;
            if (costInterval != null && timeErrInterval != null) {
                EVScheduleRandom sr = new EVScheduleRandom(schedule);
                EVScheduleConfidenceIntervals ci =
                    new EVScheduleConfidenceIntervals
                        (sr, Collections.singletonList(sr));
                completionDate = ci.getForecastDateInterval();
            }

            metrics.setDateConfidenceInterval(completionDate);
        } catch (Exception e) {
            metrics.setDateConfidenceInterval(null);
            System.out.println("Error calculating schedule interval:");
            e.printStackTrace();
        }
    }
}
