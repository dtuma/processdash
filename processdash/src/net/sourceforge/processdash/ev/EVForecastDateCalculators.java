// Copyright (C) 2006-2010 Tuma Solutions, LLC
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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.Settings;

public class EVForecastDateCalculators {


    /** Produces a forecast date by extrapolating a straight line between the
     * start and end points of the "actual earned value" curve,  extending
     * this straight line until it reaches the total planned earned value, and
     * observing the date at which that occurs.
     * 
     * This forecast is very simple, because it assumes constant staffing.  As
     * a result, it is rarely the first choice for project planning.  However,
     * it is useful as a fallback when other forecasting methods fail.
     */
    public static class SimpleExtrapolation implements EVForecastDateCalculator {

        private static final Logger logger = Logger
                .getLogger(SimpleExtrapolation.class.getName());

        public void calculateForecastDates(EVTask taskRoot,
                EVSchedule schedule, EVMetrics metrics, List evLeaves) {
            Date forecast = getSimpleExtrapolatedForecast(metrics);
            logger.log(Level.FINEST, "Simple extrapolation = {0}", forecast);
            metrics.setForecastDate(forecast);
        }

    }
    public static final EVForecastDateCalculator SIMPLE_EXTRAPOLATION =
        new SimpleExtrapolation();


    /** Perform the calculation behind the simple extrapolation forecast.
     * 
     * @param metrics the metrics object for an EV schedule
     * @return an extrapolated forecast date, or null if one cannot be
     *     calculated
     */
    private static Date getSimpleExtrapolatedForecast(EVMetrics metrics) {
        Date startDate = metrics.startDate();
        if (startDate == null)
            return null;

        double duration = metrics.elapsed() / metrics.percentComplete();
        if (EVMetrics.badDouble(duration))
            return null;

        return new Date(startDate.getTime()
                + (long) (duration * EVMetrics.MINUTE_MILLIS));
    }


    /** Validates a forecast date for a schedule.
     * 
     * @param forecastDate a tentatively calculated forecast date for the
     *     schedule, might be null
     * @param metrics a metrics object associated with the schedule
     * @return true if forecastDate is null or is nonsensical.
     */
    private static boolean isForecastInvalid(Date forecastDate,
            EVMetrics metrics) {
        // is the date one of a number of known invalid values?
        if (forecastDate == null || forecastDate == EVSchedule.A_LONG_TIME_AGO)
            return true;

        // if the schedule is not 100% complete, does the forecast date
        // precede the current date?
        if (metrics.earnedValue() < metrics.totalPlan()
                && forecastDate.before(metrics.currentDate()))
            return true;

        // forecast date seems OK
        return false;
    }




    /** Taking both CPI and DTPI into account, and looking at future staffing
     * plans, produce an estimate of when the entire project will be complete.
     * 
     * This forecast calculator assumes that the current CPI will apply to
     * future tasks, and calculates a total forecast cost for the entire task
     * list.  Then, it assumes that the current DTPI will apply to future time
     * periods, and asks the schedule when the overall project will complete.
     * 
     * The forecast completion date is only produced for the overall schedule,
     * not for each individual task in the task list.
     */
    public static class ScheduleExtrapolation implements
            EVForecastDateCalculator {

        private static final Logger logger = Logger
                .getLogger(ScheduleExtrapolation.class.getName());

        private boolean shouldFallbackToExtrapolation;

        public ScheduleExtrapolation(boolean shouldFallbackToExtrapolation) {
            this.shouldFallbackToExtrapolation = shouldFallbackToExtrapolation;
        }

        public void calculateForecastDates(EVTask taskRoot,
                EVSchedule schedule, EVMetrics metrics, List evLeaves) {

            double forecastCost = metrics.independentForecastCostEff();
            Date forecastDate = schedule.getHypotheticalDate(forecastCost, true);
            if (forecastDate == EVSchedule.NEVER)
                forecastDate = null;

            logger.log(Level.FINEST, "Schedule extrapolation = {0}",
                    forecastDate);

            if (isForecastInvalid(forecastDate, metrics)) {
                if (shouldFallbackToExtrapolation)
                    forecastDate = getSimpleExtrapolatedForecast(metrics);
                else
                    forecastDate = null;
            }

            metrics.setForecastDate(forecastDate);
        }

    }
    public static final EVForecastDateCalculator SCHEDULE_EXTRAPOLATION =
        new ScheduleExtrapolation(true);
    public static final EVForecastDateCalculator SCHEDULE_EXTRAPOLATION_2 =
        new ScheduleExtrapolation(false);




    /**
     * Without taking CPI or DTPI into account, but only examining the cost and
     * schedule variances to date, calculate a date that the schedule might
     * finish if it were replanned today.
     */
    public static class ReplanDateExtrapolation implements
            EVForecastDateCalculator {

        private double almostDonePercentage;

        public ReplanDateExtrapolation() {
            this.almostDonePercentage = getPercentageSetting("ev.forecast.task.almostDonePct");
        }

        public void calculateForecastDates(EVTask taskRoot,
                EVSchedule schedule, EVMetrics metrics, List evLeaves) {
            // calculate the original planned time of the tasks that have
            // not yet been completed. This is equal to the amount of time
            // in the plan, minus the value earned so far for completed tasks.
            double totalPlannedTimeOfRemainingTasks = metrics.totalPlan()
                    - metrics.earnedValue();

            // calculate the amount of time actually spent in these remaining
            // tasks. This is equal to the total actual time spent so far,
            // minus the actual time spent on completed work.
            double timeSpentOnRemainingTasks = metrics.totalScheduleActualTime
                    - metrics.actual();

            // the planned time remaining is equal to the difference between
            // the two numbers calculated above.
            double plannedTimeRemaining = totalPlannedTimeOfRemainingTasks
                    - timeSpentOnRemainingTasks;

            // if the schedule is significantly overspent, the planned time
            // remaining could be negative. In that case, use the "almost
            // done" percentage setting to assume that we're almost done
            // with the remaining tasks.
            if (plannedTimeRemaining < 0)
                plannedTimeRemaining = timeSpentOnRemainingTasks
                        / almostDonePercentage;

            // get the amount of planned time that has elapsed in the schedule
            // as of the current date. Then add the planned remaining time
            // to produce an amount of time that should represent a replanned
            // total. Find the time the schedule would reach this date.
            double artificialPlannedTimeTotal = metrics.totalSchedulePlanTime
                    + plannedTimeRemaining;
            Date result = schedule.getHypotheticalDate(
                artificialPlannedTimeTotal, false);
            if (isForecastInvalid(result, metrics))
                result = null;

            if (metrics instanceof EVMetricsRollup) {
                ((EVMetricsRollup) metrics).optimizedReplanDate = result;
            } else {
                metrics.replanDate = result;
            }
        }

    }
    public static final EVForecastDateCalculator REPLAN_EXTRAPOLATION =
        new ReplanDateExtrapolation();




    /** Taking both CPI and DTPI into account, looking at future staffing
     * plans, and both planned and actual time spent on tasks in progress,
     * produce estimated completion dates for each task in the plan.
     * 
     * This forecast calculator assumes that the current CPI will apply to
     * future tasks, and the current DTPI will apply to future time periods.
     * It then assumes that tasks will be completed in the order they appear
     * in the evLeaves list, and asks the schedule when each task will be
     * completed.
     * 
     * In-progress tasks require additional handling.  According to the
     * assumptions above, their forecast total cost should be their planned
     * cost divided by CPI.   Forecast cost remaining is then calculated by
     * subtracting the actual time spent on the task so far. Unfortunately,
     * for significantly overspent tasks, this will produce a negative estimate.
     * In this case, a new forecast total cost is produced for the overspent
     * task by presuming that it is "90%" complete. (The exact percentage is
     * determined by the user setting <tt>ev.forecast.task.almostDonePct</tt>.)
     * This change causes the task to use more hours than CPI would have
     * indicated.  Since our original assumption is that the historical CPI will
     * hold for future tasks, this would imply that underspent future tasks
     * will use slightly less time than the CPI would indicate.  So the
     * overspent time is removed from the underspent tasks, in an attempt to
     * make the aggregate CPI of the future tasks match the CPI so far.
     * 
     * Of course, in situations where very few tasks remain in the task list,
     * or where certain in-progress tasks are extremely overspent, this approach
     * could cause underspent tasks to be adjusted down to ridiculously low
     * cost estimates, including 0.  This is not acceptable, so another user
     * setting (<tt>ev.forecast.task.maxCpiCorrectionPct</tt>) configures the
     * maximum percent adjustment that will be applied to underspent tasks.
     * If the adjustment exceeds this max percentage, then the underspent
     * tasks will not be adjusted past this percentage, and the resulting
     * forecast will be based on an effective aggregate CPI that exceeds the
     * historical CPI.  (This is a wise alternative, since the excessively
     * overspent tasks provide ample evidence that the historical CPI is not
     * holding true.)
     * 
     * This calculator works hard to preserve the aggregate CPI, so that the
     * resulting forecast date for the overall schedule is identical to the one
     * that would be produced by standard EV forecast calculations.  (In
     * particular, this makes it compatible with the dashboard's EV forecast
     * range calculations.)  However, a user can completely disable these
     * adjustment steps by setting the <tt>almostDonePct</tt> to 100, and the
     * <tt>maxCpiCorrectionPct</tt> to 0.
     */
    public static class ScheduleTaskExtrapolation implements
            EVForecastDateCalculator {

        private static final Logger logger = Logger
                .getLogger(ScheduleTaskExtrapolation.class.getName());

        public ScheduleTaskExtrapolation() {
            this.almostDonePercentage = getPercentageSetting(
                    "ev.forecast.task.almostDonePct");
            this.maxAdjustmentRatio = getPercentageSetting(
                    "ev.forecast.task.maxCpiCorrectionPct");
        }

        public void calculateForecastDates(EVTask taskRoot,
                EVSchedule schedule, EVMetrics metrics, List evLeaves) {

            logger.log(Level.FINEST, "Schedule task extrapolation running");

            Date finalDate = getFinalDate(schedule, metrics, evLeaves);
            if (finalDate == EVSchedule.NEVER)
                finalDate = null;

            logger.log(Level.FINEST, "Schedule task extrapolation = {0}",
                    finalDate);

            setFinalDate(metrics, finalDate);
        }

        private double underspentTime;
        private double overspentTime;

        private double almostDonePercentage;
        private double maxAdjustmentRatio;

        private Date getFinalDate(EVSchedule schedule, EVMetrics metrics,
                List evLeaves) {
            if (evLeaves.isEmpty())
                return null;

            boolean usePerformanceIndexes = usePerformanceIndexes();
            double cpi = 1.0;
            double dtpi = 1.0;
            if (usePerformanceIndexes) {
                cpi = metrics.costPerformanceIndexEff();
                dtpi = metrics.directTimePerformanceIndexEff();
            }
            if (isBadRatio(cpi) || isBadRatio(dtpi))
                return null;

            underspentTime = overspentTime = 0;
            Date finalDate = EVSchedule.A_LONG_TIME_AGO;

            List tasks = new ArrayList(evLeaves.size());
            for (Iterator i = evLeaves.iterator(); i.hasNext();) {
                EVTask task = (EVTask) i.next();
                Date actualDate = task.getActualDate();
                setProjectedDate(task, actualDate);
                if (actualDate == null)
                    tasks.add(new TaskData(task, cpi));
                else
                    finalDate = EVCalculator.maxPlanDate(finalDate, actualDate);
            }

            double adjustmentRatio = - overspentTime / underspentTime;
            adjustmentRatio = Math.min(adjustmentRatio, maxAdjustmentRatio);
            if (logger.isLoggable(Level.FINEST))
                logger.log(Level.FINEST, "Overspent time = " + overspentTime
                        + ", underspent = " + underspentTime
                        + ", adjustment ratio = " + adjustmentRatio);

            EVScheduleSplit s = new EVScheduleSplit(schedule);

            double cumForecastTime = schedule.getLast().cumActualDirectTime;
            for (Iterator i = tasks.iterator(); i.hasNext();) {
                TaskData td = (TaskData) i.next();
                EVTask task = td.task;
                cumForecastTime += td.getTimeRemaining(adjustmentRatio);
                Date projDate = s.getHypotheticalDate(cumForecastTime,
                        usePerformanceIndexes);
                setProjectedDate(task, projDate);
                finalDate = EVCalculator.maxForecastDate(finalDate, projDate);
            }

            setStartDates(schedule, evLeaves);

            if (finalDate == EVSchedule.A_LONG_TIME_AGO)
                finalDate = null;

            return finalDate;
        }

        protected boolean usePerformanceIndexes() {
            return true;
        }

        protected void setProjectedDate(EVTask task, Date date) {
            task.forecastDate = date;
        }

        protected void setFinalDate(EVMetrics metrics, Date finalDate) {
            metrics.setForecastDate(finalDate);
        }

        protected void setStartDates(EVSchedule schedule, List evLeaves) {
            FORECAST_START_DATE_SETTER.setStartDates(schedule, evLeaves);
        }

        private boolean isBadRatio(double ratio) {
            return Double.isInfinite(ratio) || Double.isNaN(ratio)
                    || ratio <= 0;
        }

        private class TaskData {
            EVTask task;

            /** The projected cost for this task, calculated by adjusting the
             * plan time with the CPI. */
            double cpiCost;

            /** The projected cost for this task, calculated by assuming that
             * the task is "almost done" */
            double almostDoneCost;
            double delta;

            public TaskData(EVTask task, double cpi) {
                this.task = task;

                cpiCost = task.planValue / cpi;
                almostDoneCost = task.actualNodeTime / almostDonePercentage;
                delta = cpiCost - almostDoneCost;
                if (delta > 0)
                    underspentTime += delta;
                else
                    overspentTime += delta;
            }

            public double getTimeRemaining(double deltaRatio) {
                if (delta < 0)
                    return almostDoneCost - task.actualNodeTime;
                else
                    return cpiCost - (delta * deltaRatio) - task.actualNodeTime;
            }

        }

    }

    /** This calculator performs a calculation similar to the
     * ScheduleForecastTaskExtrapolation, but does not use CPI or DTPI, and
     * stores values into the "replanDate" fields instead of "forecastDate"
     */
    public static class ScheduleTaskReplanner extends
            ScheduleTaskExtrapolation {

        @Override
        protected void setFinalDate(EVMetrics metrics, Date finalDate) {
            metrics.setReplanDate(finalDate);
        }

        @Override
        protected void setProjectedDate(EVTask task, Date date) {
            task.replanDate = date;
        }

        @Override
        protected void setStartDates(EVSchedule schedule, List evLeaves) {
            REPLAN_START_DATE_SETTER.setStartDates(schedule, evLeaves);
        }

        @Override
        protected boolean usePerformanceIndexes() {
            return false;
        }

    }




    /** This calculator extract the forecast date from an exported/imported XML
     * schedule.
     * 
     * It assumes that the forecast was calculated elsewhere, and exported
     * as part of the XML data for this schedule.
     * 
     * If no date can be extracted from the XML, this calculator will defer
     * to the ScheduleExtrapolation method.
     */
    public static class XmlForecastDate implements EVForecastDateCalculator {

        public void calculateForecastDates(EVTask taskRoot,
                EVSchedule schedule, EVMetrics metrics, List evLeaves) {
            metrics.setReplanDate(taskRoot.getReplanDate());

            FORECAST_START_DATE_SETTER.setStartDates(schedule, evLeaves);
            REPLAN_START_DATE_SETTER.setStartDates(schedule, evLeaves);
            rollupStartDates(taskRoot);

            Date forecastDate = taskRoot.getForecastDate();
            if (forecastDate != null)
                metrics.setForecastDate(forecastDate);
            else
                SCHEDULE_EXTRAPOLATION.calculateForecastDates(taskRoot,
                        schedule, metrics, evLeaves);
        }

        private void rollupStartDates(EVTask task) {
            for (int i = task.getNumChildren(); i-- > 0;) {
                EVTask child = task.getChild(i);
                rollupStartDates(child);

                task.replanStartDate = EVCalculator.minStartDate(
                    task.replanStartDate, child.replanStartDate);
                task.forecastStartDate = EVCalculator.minStartDate(
                    task.forecastStartDate, child.forecastStartDate);
            }
        }

    }
    public static final EVForecastDateCalculator XML_FORECAST =
        new XmlForecastDate();




    /** This calculator is capable of calculating forecast dates for all the
     * leaf tasks in a schedule.
     * 
     * This calculator determines the amount of value that has been earned per
     * hour in the past, and assumes that this rate will hold true in the
     * future.  It then determines when tasks will be completed based on the
     * pattern of cumulative future earned value.
     * 
     * This approach produces safe estimates which generally make sense.
     * However, it tends to unfairly penalize you for "in progress" work.
     * As a result, the forecast dates tend to be more pessimistic than the
     * ScheduleExtrapolation method.
     */
    public static class HourlyEVRateExtrapolation implements
            EVForecastDateCalculator {

        private static final Logger logger = Logger
                .getLogger(HourlyEVRateExtrapolation.class.getName());

        public void calculateForecastDates(EVTask taskRoot,
                EVSchedule schedule, EVMetrics metrics, List evLeaves) {

            Date finalDate = null;

            if (!evLeaves.isEmpty()) {
                totalActualEV = totalPlannedHistTime = 0;
                HourlySplitSched s = new HourlySplitSched(schedule);

                if (totalActualEV > 0 && totalPlannedHistTime > 0) {
                    for (Iterator i = evLeaves.iterator(); i.hasNext();) {
                        EVTask task = (EVTask) i.next();
                        task.forecastDate = task.getActualDate();
                        if (task.forecastDate == null) {
                            task.forecastDate = s.getHypotheticalDate(
                                    task.cumPlanValue, false);
                            finalDate = task.forecastDate;
                        }
                    }
                }

                FORECAST_START_DATE_SETTER.setStartDates(schedule, evLeaves);
            }

            if (finalDate == EVSchedule.NEVER)
                finalDate = null;
            logger.log(Level.FINEST, "EV rate extrapolation = {0}", finalDate);
            metrics.setForecastDate(finalDate);
        }

        private double totalActualEV;
        private double totalPlannedHistTime;

        private class HourlySplitSched extends EVScheduleSplit {

            public HourlySplitSched(EVSchedule s) {
                super(s);
                double evRate = totalActualEV / totalPlannedHistTime;
                rewriteFuture(evRate);
            }

            protected void rewriteHistoricalPeriod(Period p, Period h) {
                p.planDirectTime = h.cumEarnedValue - totalActualEV;
                p.cumPlanDirectTime = h.cumEarnedValue;
                totalActualEV = h.cumEarnedValue;
                totalPlannedHistTime = h.cumPlanDirectTime;
            }

        }

    }


    /**
     * This class sets the forecast start date for each task in a list. It
     * does this by first checking for an actual start date.  If none is
     * present, it assumes that the task will start when the previous task
     * finishes.
     */
    private static class ForecastStartDateProjection {

        public void setStartDates(EVSchedule schedule, List evLeaves) {
            Date nextStart = schedule.getStartDate();
            for (int i = 0;  i < evLeaves.size();  i++) {
                EVTask task = (EVTask) evLeaves.get(i);
                Date actualStart = task.getActualStartDate();
                Date endDate = getTaskEndDate(task);
                Date effStart;
                // In a normal scenario, we would ALWAYS want to use the
                // actual start date of a task instead of some arbitrary
                // calculation.  But in the real world, people sometimes do
                // dumb things, like mark a task complete and then manually
                // enter its time in the time log (resulting in a start date
                // that follows the completion date).  To account for those
                // real-world misuse cases, we ignore the start date if it
                // precedes the end date.
                if (actualStart != null && endDate != null
                        && actualStart.before(endDate)) {
                    effStart = actualStart;
                } else {
                    effStart = EVCalculator.minStartDate(endDate, nextStart);
                    effStart = EVCalculator.minStartDate(effStart, actualStart);
                }
                setTaskStartDate(task, effStart);
                if (endDate != null)
                    nextStart = endDate;
            }
        }

        protected Date getTaskEndDate(EVTask task) {
            return task.getForecastDate();
        }

        protected void setTaskStartDate(EVTask task, Date d) {
            task.forecastStartDate = d;
        }
    }
    private static final ForecastStartDateProjection FORECAST_START_DATE_SETTER =
        new ForecastStartDateProjection();


    /** This class sets the replan start date for each task in a list.
     */
    private static class ReplanStartDateProjection extends
            ForecastStartDateProjection {
        @Override
        protected Date getTaskEndDate(EVTask task) {
            return task.getReplanDate();
        }

        @Override
        protected void setTaskStartDate(EVTask task, Date d) {
            task.replanStartDate = d;
        }
    }
    private static final ReplanStartDateProjection REPLAN_START_DATE_SETTER =
        new ReplanStartDateProjection();


    /**
     * Convenience method to retrieve the known forecast dates for a task.
     * 
     * @param ctx the dashboard context
     * @param taskPath the full path to a project/task in the dashboard
     * @return null if no task lists contain the given task. Otherwise, returns
     *         a map whose keys are task list names containing the task, and
     *         whose values are the forecast date for the task in that schedule.
     *         If a particular schedule does not calculate a forecast date for
     *         the task, it will not be included in the result. Hence, the Map
     *         could be empty.
     */
    public static Map getForecastDates(DashboardContext ctx, String taskPath) {
        List taskLists = EVTaskList.getTaskListNamesForPath(
                ctx.getData(), taskPath);
        if (taskLists == null || taskLists.isEmpty())
            return null;

        Map result = new HashMap();
        for (Iterator i = taskLists.iterator(); i.hasNext();) {
            String taskListName = (String) i.next();
            Date forecast = getForecastForSchedule(ctx, taskListName, taskPath);
            if (forecast != null)
                result.put(taskListName, forecast);
        }
        return result;

    }
    private static Date getForecastForSchedule(DashboardContext ctx,
            String taskListName, String taskPath) {
        EVTaskList tl = EVTaskList.openExisting(taskListName, ctx.getData(),
                ctx.getHierarchy(), ctx.getCache(), false);
        if (tl == null)
            return null;
        tl.recalc();
        List tasks = tl.findTasksByFullName(taskPath);
        if (tasks == null || tasks.isEmpty())
            return null;

        Iterator i = tasks.iterator();
        Date result = ((EVTask) i.next()).getForecastDate();
        while (i.hasNext()) {
            Date oneDate = ((EVTask) i.next()).getForecastDate();
            result = EVCalculator.maxPlanDate(result, oneDate);
        }
        return result;
    }

    private static double getPercentageSetting(String settingName) {
        int num = Settings.getInt(settingName, 0);
        num = Math.max(0, num);
        num = Math.min(100, num);
        return num / 100.0;
    }

}
