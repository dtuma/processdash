// Copyright (C) 2003-2013 Tuma Solutions, LLC
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.ev.ci.AbstractConfidenceInterval;
import net.sourceforge.processdash.ev.ci.ConfidenceInterval;
import net.sourceforge.processdash.ev.ci.ConfidenceIntervalProvider;
import net.sourceforge.processdash.ev.ci.EVCostConfidenceInterval;
import net.sourceforge.processdash.ev.ci.EVScheduleConfidenceIntervals;
import net.sourceforge.processdash.ev.ci.EVTimeErrConfidenceInterval;
import net.sourceforge.processdash.log.time.DashboardTimeLog;
import net.sourceforge.processdash.log.time.IONoSuchElementException;
import net.sourceforge.processdash.log.time.TimeLog;
import net.sourceforge.processdash.log.time.TimeLogEntry;


public class EVCalculatorData extends EVCalculator {

    private EVTaskList taskList;
    private EVTask taskRoot;
    private EVSchedule schedule;
    private TimeLog timeLog;
    private Date effectiveDate, completionDate;
    private boolean rezeroAtStartDate;
    private boolean checkForFutureTimeLogDates;
    private EVForecastDateCalculator replanDateCalculator;
    private EVForecastDateCalculator forecastDateCalculator;

    @SuppressWarnings("deprecation")
    public EVCalculatorData(EVTaskList taskList) {
        this.taskList = taskList;
        this.taskRoot = taskList.getTaskRoot();
        this.schedule = taskList.getSchedule();
        this.metadata = taskList.metaData;
        this.timeLog = DashboardTimeLog.getDefault();
        this.costIntervalProvider = new CostIntervalProvider(
                new CurrentPlanCostIntervalProvider());
        this.timeErrIntervalProvider = new TimeIntervalProvider(
                new CurrentPlanTimeErrIntervalProvider());
        this.replanDateCalculator =
            new EVForecastDateCalculators.ScheduleTaskReplanner();
        this.forecastDateCalculator = createForecastCalculator();
    }

    public void recalculate() {
        resetData();
        recalcBaselineData(taskRoot);
        scheduleStartDate = schedule.getStartDate();
        reorderCompletedTasks =
            Settings.getBool("ev.sortCompletedTasks", true);
        rezeroAtStartDate = getBoolSetting(EVMetadata.REZERO_ON_START_DATE,
            true);

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
            if (rezeroAtStartDate)
                saveActualPreTime(log);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            schedule.getMetrics().addError
                ("Unable to retrieve time log data.", taskRoot);
        }

        // calculate planned and actual earned values, planned dates, and
        // planned and actual cumulative earned values
        calcTaskValues(evLeaves);
        saveCompletedTaskValues(evLeaves);

        // reset the EVMetrics object
        schedule.setEffectiveDate(effectiveDate);
        schedule.getMetrics().reset(scheduleStartDate, effectiveDate,
                                    schedule.getPeriodStart(effectiveDate),
                                    schedule.getPeriodEnd(effectiveDate));
        schedule.getMetrics().loadBaselineData(taskRoot);

        // record actual time spent on tasks.
        try {
            saveActualScheduleTime(log);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            schedule.getMetrics().addError
                ("Unable to retrieve time log data.", taskRoot);
        }
        schedule.recalcCumActualTimes();

        // recalculate the EVMetrics object
        taskRoot.checkForNodeErrors(schedule.getMetrics(), 0,
                                    new ArrayList(), new ArrayList(), false);
        recalcMetrics(taskRoot, schedule.getMetrics());

        schedule.getMetrics().recalcScheduleTime(schedule);

        // create confidence intervals for cost and time
        createCostConfidenceInterval();
        createTimeErrConfidenceInterval();

        replanDateCalculator.calculateForecastDates(taskRoot, schedule,
                schedule.getMetrics(), evLeaves);
        forecastDateCalculator.calculateForecastDates(taskRoot, schedule,
                schedule.getMetrics(), evLeaves);
        taskList.scanForMilestoneErrors(evLeaves);

        recalculateTaskHierarchy(taskRoot);

        saveCompletedTaskCosts(evLeaves);

        // create confidence interval for schedule
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
        checkForFutureTimeLogDates = (result == null);
        if (result == null) result = getFixedEffectiveDate();
        if (result == null) result = new Date();
        return result;
    }

    private void saveActualPreTime(TimeLog log) throws IOException {
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
        Date startDateA = scheduleStartDate;
        Date startDateB = startDateA;
        Iterator i = evLeaves.iterator();
        while (i.hasNext()) {
            EVTask task = (EVTask) i.next();

            if (!task.isLeaf())
                task.actualPreTime = getTotalActualPreTime(task);

            if (task.dateCompleted == null ||
                rezeroAtStartDate == false ||
                task.dateCompleted.compareTo(scheduleStartDate) >= 0) {
                task.planValue = task.planTime - task.actualPreTime;
                if (task.planValue < 0) task.planValue = 0;
                cumPlanValue += task.planValue;
                task.cumPlanValue = cumPlanValue;
                task.planDate = schedule.getPlannedCompletionDate
                    (cumPlanValue, cumPlanValue);

                if (startDateB.before(task.planDate))
                    startDateA = startDateB;
                task.planStartDate = startDateA;
                startDateB = task.planDate;

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

    private void saveCompletedTaskCosts(List evLeaves) {
        for (Iterator i = evLeaves.iterator(); i.hasNext();) {
            EVTask task = (EVTask) i.next();
            if (task.dateCompleted != null)
                schedule.saveCompletedTaskCost(task.dateCompleted,
                        task.actualCurrentTime);
        }
    }


    private void saveActualScheduleTime(TimeLog log) throws IOException {
        try{
            Date start = rezeroAtStartDate ? scheduleStartDate : null;
            Iterator entries = log.filter(null, start, null);
            while (entries.hasNext())
                saveActualScheduleTime((TimeLogEntry) entries.next());
        } catch (IONoSuchElementException ion) {
            throw ion.getIOException();
        }
    }

    private void saveActualScheduleTime(TimeLogEntry entry) {
        Date d = entry.getStartTime();
        if (d == null || beforeZeroDate(d)) return;

        EVTask task = taskRoot.getTaskForPath(entry.getPath());
        if (task == null) return;

        if (task.isLevelOfEffortTask()) {
            // for level of effort tasks, ignore time logged outside the
            // effective period of the schedule.
            if (d.compareTo(scheduleStartDate) > 0
                    && d.compareTo(effectiveDate) < 0) {
                task.actualNodeTime += entry.getElapsedTime();
                schedule.getMetrics().addIndirectTime(entry.getElapsedTime());
                schedule.saveActualIndirectTime(d, entry.getElapsedTime());
            }
            return;
        }

        task.actualNodeTime += entry.getElapsedTime();
        if (task.actualStartDate == null ||
            task.actualStartDate.compareTo(d) > 0)
            task.actualStartDate = d;

        if (task.isUserPruned()) return;

        schedule.saveActualTime(d, entry.getElapsedTime());

        if (checkForFutureTimeLogDates) {
            long delta = d.getTime() - effectiveDate.getTime();
            if (delta > DAY_MILLIS) {
                String errMsg = EVSchedule.resources
                        .getString("Task.Future_Time_Log_Entry.Error");
                schedule.metrics.addError(errMsg, taskList
                        .getTaskRoot());
                checkForFutureTimeLogDates = false;
            }
        }
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
                !task.isLevelOfEffortTask() && !task.isUserPruned()) {
                metrics.addTask(0, task.actualNodeTime, null,
                                metrics.startDate());
                schedule.saveCompletedTaskCost(metrics.startDate(),
                        task.actualNodeTime);
            }
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
            child.replanDate = task.replanDate;
            child.forecastDate = task.forecastDate;
            child.planStartDate = task.planStartDate;
            child.replanStartDate = task.replanStartDate;
            child.forecastStartDate = task.forecastStartDate;
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
            if (d != null && (reorderCompletedTasks || beforeZeroDate(d)))
                i.remove();
        }
        return result;
    }

    private boolean beforeZeroDate(Date d) {
        return rezeroAtStartDate && d.compareTo(scheduleStartDate) < 0;
    }

    private void createCostConfidenceInterval() {
        ConfidenceInterval costInterval = null;
        if (completionDate == null)
            costInterval = costIntervalProvider.getConfidenceInterval(taskList);
        schedule.getMetrics().setCostConfidenceInterval(costInterval);
    }

    private class CurrentPlanCostIntervalProvider implements
            ConfidenceIntervalProvider {

        public ConfidenceInterval getConfidenceInterval(EVTaskList taskList) {
            return new EVCostConfidenceInterval(getConfidenceIntervalTaskData());
        }

        private List getConfidenceIntervalTaskData() {
            List result = new LinkedList();
            List evLeaves = getEVLeaves();
            for (Iterator i = evLeaves.iterator(); i.hasNext();) {
                EVTask task = (EVTask) i.next();
                if (task.getDateCompleted() != null) {
                    result.add(new AbstractConfidenceInterval.DataPoint(
                            task.planValue, task.actualNodeTime));
                }
            }
            addNonLeafTaskLiability(taskRoot, evLeaves, result);
            return result;
        }

        private void addNonLeafTaskLiability(EVTask task, List leaves,
                List results) {
            if (task.actualNodeTime > 0 &&
                !task.isLevelOfEffortTask() && !task.isUserPruned() &&
                !leaves.contains(task))
                results.add(new AbstractConfidenceInterval.DataPoint(0,
                        task.actualNodeTime));

            for (int i = 0; i < task.getNumChildren(); i++)
                addNonLeafTaskLiability(task.getChild(i), leaves, results);
        }

    }



    private void createTimeErrConfidenceInterval() {
        ConfidenceInterval timeErrInterval =
            timeErrIntervalProvider.getConfidenceInterval(taskList);
        schedule.getMetrics().setTimeErrConfidenceInterval(timeErrInterval);
    }

    protected class CurrentPlanTimeErrIntervalProvider implements
            ConfidenceIntervalProvider {

        public ConfidenceInterval getConfidenceInterval(EVTaskList taskList) {
            return new EVTimeErrConfidenceInterval(taskList.getSchedule(),
                    false);
        }

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

    private EVForecastDateCalculator createForecastCalculator() {
        String method = Settings.getVal("ev.forecast.method");
        if ("simple".equalsIgnoreCase(method)
                || Settings.getBool("ev.simpleForecastDate", false))
            return EVForecastDateCalculators.SIMPLE_EXTRAPOLATION;
        if ("schedule".equalsIgnoreCase(method))
            return EVForecastDateCalculators.SCHEDULE_EXTRAPOLATION;
        if ("evRate".equalsIgnoreCase(method))
            return new EVForecastDateCalculators.HourlyEVRateExtrapolation();

        // default approach, also specified as "task"
        return new EVForecastDateCalculators.ScheduleTaskExtrapolation();
    }
}
