// Copyright (C) 2003-2014 Tuma Solutions, LLC
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import net.sourceforge.processdash.ev.ci.ConfidenceInterval;
import net.sourceforge.processdash.ev.ci.ConfidenceIntervalProvider;
import net.sourceforge.processdash.ev.ci.ConfidenceIntervalSum;
import net.sourceforge.processdash.ev.ci.EVScheduleConfidenceIntervals;
import net.sourceforge.processdash.ev.ci.EVTimeErrConfidenceInterval;
import net.sourceforge.processdash.ev.ci.LinearRatioConfidenceInterval;
import net.sourceforge.processdash.ev.ci.LogCenteredConfidenceInterval;
import net.sourceforge.processdash.ev.ci.SingleValueConfidenceInterval;

public class EVCalculatorRollup extends EVCalculator {

    private EVTaskListRollup rollupTaskList;
    private EVTask taskRoot;
    private Vector<EVTaskList> evTaskLists;
    private EVScheduleRollup schedule;

    public EVCalculatorRollup(EVTaskListRollup rollupTaskList, EVTask root,
            Vector evTaskLists, EVScheduleRollup schedule, Properties metadata) {
        this.rollupTaskList = rollupTaskList;
        this.taskRoot = root;
        this.evTaskLists = evTaskLists;
        this.schedule = schedule;
        this.metadata = metadata;
    }

    public void recalculate() {
        rollupTaskList.fireTreeStructureWillChange();
        evLeaves = null;

        // Recalculate all the subschedules.
        for (int i = evTaskLists.size();   i-- > 0; ) {
            EVTaskList taskList = (EVTaskList) evTaskLists.get(i);

            // install rollup-level confidence interval providers, then ask
            // the task list to recalculate.
            tweakConfidenceIntervalProviders(taskList);
            taskList.recalc();

            // On rare occasions, some task lists might create a new calculator
            // as a result of a recalc operation. If this has occurred,
            // reinstall our interval providers and recalc again.
            if (tweakConfidenceIntervalProviders(taskList))
                taskList.recalc();

            // Some types of task lists perform a recalc by completely
            // replacing their root task and schedule. Give them the
            // benefit of the doubt and make certain that we are using
            // the correct root and schedule
            taskRoot.replace(i, (EVTask) taskList.getRoot());
            schedule.replaceSchedule(i, taskList);
        }

        // Calculate confidence intervals, if possible.
        createConfidenceIntervals();

        // Recalculate the root node.
        recalcRollupNode();

        // adjust level of effort percentages
        calculateLevelOfEffort();

        // set baseline data for the schedule
        schedule.getMetrics().loadBaselineData(taskRoot);

        // Recalculate the rollup schedule.
        schedule.recalc();

        // check for duplicate schedules.
        taskRoot.checkForNodeErrors(schedule.getMetrics(), 0,
                new ArrayList(), new ArrayList(), true);

        // possibly store fallback completion dates for overspent tasks
        writeOverspentFallbackDates();
    }

    @Override
    public void setBaselineDataSource(EVSnapshot baselineDataSource) {
        super.setBaselineDataSource(baselineDataSource);
        for (EVTaskList tl : evTaskLists) {
            if (tl != null && tl.calculator != null)
                tl.calculator.setBaselineDataSource(baselineDataSource);
        }
    }


    /** If this node is the root node of an EVTaskList rollup, this will
     *  recalculate it.
     *
     * <b>Important:</b> the children of this node (which are
     * themselves root nodes of other EVTaskLists) should already be
     * recalculated before calling this method.
     */
    public void recalcRollupNode() {
        recalcRollupNode(taskRoot);
        recalcRollupNodeBaseline(taskRoot);
    }

    private void recalcRollupNodeBaseline(EVTask taskRoot) {
        EVTask baselineRoot = findBaselineTaskRoot(taskRoot);
        copyBaselineData(taskRoot, baselineRoot, true);
    }

    protected static void recalcRollupNode(EVTask taskRoot) {
        resetRootData(taskRoot);
        resetNodeData(taskRoot);
        sumUpNodeData(taskRoot);

        taskRoot.cumPlanValue = taskRoot.planValue;


        EVTask child;

        taskRoot.recalcParentDateCompleted();

        for (int i = taskRoot.children.size();   i-- > 0;  ) {
            child = taskRoot.getChild(i); // For each child,

            // accumulate numeric task data.
            taskRoot.planTime += child.planTime;
            taskRoot.topDownPlanTime += child.topDownPlanTime;
            taskRoot.bottomUpPlanTime += child.bottomUpPlanTime;
        }
    }

    private void calculateLevelOfEffort() {
        double rTotalTime = 0;
        double rIndirectTime = 0;
        Iterator i = evTaskLists.iterator();
        while (i.hasNext()) {
            EVSchedule s = ((EVTaskList) i.next()).getSchedule();
            double sDirectTime = s.getMetrics().totalPlan();
            double sLOE = s.getLevelOfEffort();
            double sTotalTime = sDirectTime / (1 - sLOE);
            if (!Double.isInfinite(sTotalTime) && !Double.isNaN(sTotalTime)) {
                rTotalTime += sTotalTime;
                rIndirectTime += (sTotalTime * sLOE);
            }
        }

        if (rTotalTime == 0 || rIndirectTime == 0)
            schedule.setLevelOfEffort(0);
        else
            schedule.setLevelOfEffort(rIndirectTime / rTotalTime);

        i = evTaskLists.iterator();
        while (i.hasNext()) {
            EVTaskList tl = (EVTaskList) i.next();
            EVSchedule s = tl.getSchedule();
            double sDirectTime = s.getMetrics().totalPlan();
            double sLOE = s.getLevelOfEffort();
            double sTotalTime = sDirectTime / (1 - sLOE);
            double sFraction = sTotalTime / rTotalTime;
            if (!Double.isInfinite(sFraction) && !Double.isNaN(sFraction))
                scaleLevelOfEffort((EVTask) tl.getRoot(), sFraction);
        }
    }

    private void scaleLevelOfEffort(EVTask task, double ratio) {
        if (task.isLevelOfEffortTask())
            task.rollupLevelOfEffort = task.planLevelOfEffort * ratio;
        else
            task.rollupLevelOfEffort = -1;

        for (int i = task.getNumChildren();   i-- > 0;  )
            scaleLevelOfEffort(task.getChild(i), ratio);
    }

    /**
     * When a team member has an END date on their schedule, but they have
     * more work than they can complete, some of their tasks may have an
     * end date of "never."  In that scenario, we still calculate an optimized
     * completion date for the team, based on an assumption that the team
     * will reassign that person's remaining work to other team members.
     * This method finds the tasks with "never" dates, and attaches various
     * fallback date ranges based upon the timeframe when the team might step
     * in and take over the unfinished work.
     */
    private void writeOverspentFallbackDates() {
        DateRange fallbackPlan, fallbackReplan, fallbackForecast;
        if (!someSchedulesAreRollups()) {
            // our fallback date ranges begin when the first team member
            // finishes their work (since that is the date someone could
            // potentially begin working on the unfinished tasks), and end
            // with the team's optimized completion date.
            EVMetricsRollup thatRollup = (EVMetricsRollup) schedule.getMetrics();
            fallbackPlan = new DateRange(thatRollup.earliestPlanDate,
                    thatRollup.optimizedPlanDate());
            fallbackReplan = new DateRange(thatRollup.earliestReplanDate,
                    thatRollup.optimizedReplanDate());
            fallbackForecast = new DateRange(thatRollup.earliestForecastDate,
                    thatRollup.optimizedForecastDate());
        } else {
            // when we perform a rollup of rollups (for example, in a master
            // project), we do NOT assume that we can rebalance work from
            // one subschedule to another.  So in that scenario, we will not
            // assign any fallback dates
            fallbackPlan = fallbackReplan = fallbackForecast = null;
        }

        // now that we've calculated the appropriate ranges, store the
        // values in each plain subschedule.
        for (int i = evTaskLists.size(); i-- > 0;) {
            EVTaskList tl = evTaskLists.get(i);
            if ((tl instanceof EVTaskListData) || (tl instanceof EVTaskListXML))
                writeOverspentFallbackDates(tl, fallbackPlan,
                    fallbackReplan, fallbackForecast);
        }
    }

    private void writeOverspentFallbackDates(EVTaskList tl,
            DateRange fallbackPlan, DateRange fallbackReplan,
            DateRange fallbackForecast) {
        // check to see whether this schedule has an end date set.  Based on
        // the presence or absence of that end date, adjust the ranges.
        Date scheduleEndDate = tl.getSchedule().getEndDate();
        fallbackPlan = resolveRange(fallbackPlan, scheduleEndDate);
        fallbackReplan = resolveRange(fallbackReplan, scheduleEndDate);
        fallbackForecast = resolveRange(fallbackForecast, scheduleEndDate);

        // now store the resulting ranges into each task of the task list.
        writeOverspentFallbackDates(tl.getTaskRoot(), fallbackPlan,
            fallbackReplan, fallbackForecast);
    }

    private DateRange resolveRange(DateRange r, Date scheduleEnd) {
        // if this schedule does not have an end date, then it should be able
        // to calculate its own dates, and does not need fallback ranges
        if (scheduleEnd == null)
            return null;
        // if we were passed a bad date range, don't attempt to use it.
        if (r == null || badDate(r.getEnd()))
            return null;
        // if the start date of our range is bad, try replacing it with the
        // schedule end.  If that doesn't help, don't apply a fallback range.
        Date newStart = r.getStart();
        if (badDate(newStart) || newStart.after(r.getEnd())) {
            newStart = scheduleEnd;
            if (badDate(newStart) || newStart.after(r.getEnd()))
                return null;
        }
        return new DateRange(newStart, r.getEnd());
    }

    private void writeOverspentFallbackDates(EVTask node,
            DateRange fallbackPlan, DateRange fallbackReplan,
            DateRange fallbackForecast) {
        // store ranges into this EVTask, and recurse into children.
        node.overspentPlanDates = fallbackPlan;
        node.overspentReplanDates = fallbackReplan;
        node.overspentForecastDates = fallbackForecast;
        for (int i = node.getNumChildren();  i-- > 0; )
            writeOverspentFallbackDates(node.getChild(i), fallbackPlan,
                fallbackReplan, fallbackForecast);
    }


    public List getEVLeaves() {
        if (evLeaves == null) {
            evLeaves = new ArrayList();
            Iterator i = evTaskLists.iterator();
            while (i.hasNext()) {
                EVTaskList taskList = (EVTaskList) i.next();
                if (taskList != null && taskList.calculator != null)
                    evLeaves.addAll(taskList.calculator.getEVLeaves());
            }
            Collections.sort(evLeaves, EV_LEAF_DATE_COMPARATOR);
        }

        return evLeaves;
    }

    private boolean tweakConfidenceIntervalProviders(EVTaskList taskList) {
        if (taskList instanceof EVTaskListRollup)
            return false;

        EVCalculator c = taskList.calculator;
        if (c == null)
            return false;

        boolean madeChange = false;

        if (!(c.costIntervalProvider instanceof RollupCostIntervalProvider)) {
            ConfidenceIntervalProvider newCIP =
                new RollupCostIntervalProvider(c.costIntervalProvider);
            c.costIntervalProvider = newCIP;
            madeChange = true;
        }

        if (!(c.timeErrIntervalProvider instanceof RollupTimeErrIntervalProvider)) {
            ConfidenceIntervalProvider newCIP =
                new RollupTimeErrIntervalProvider(c.timeErrIntervalProvider);
            c.timeErrIntervalProvider = newCIP;
            madeChange = true;
        }

        return madeChange;
    }

    private class RollupCostIntervalProvider extends CostIntervalProvider {

        protected RollupCostIntervalProvider(
                ConfidenceIntervalProvider current) {
            super(current);
        }

        @Override
        public ConfidenceInterval getConfidenceInterval(EVTaskList taskList) {
            // If this individual is finished with all of their tasks, the
            // "time left" will be 0.  In that case, assign them a cost
            // confidence interval that states "zero time remaining" with
            // 100% certainty.
            Double input = getInput(taskList);
            if (input != null && input.doubleValue() < 0.001)
                return new SingleValueConfidenceInterval(0.0);

            return super.getConfidenceInterval(taskList);
        }

    }

    private class RollupTimeErrIntervalProvider extends TimeIntervalProvider {

        protected RollupTimeErrIntervalProvider(
                ConfidenceIntervalProvider current) {
            super(current);
        }

        @Override
        public ConfidenceInterval getConfidenceInterval(EVTaskList taskList) {
            // a null effective date is generally a sign of a deeply flawed
            // schedule - like a missing task list, for example.  If such a
            // schedule is present, we can't generate any interval for it.
            if (taskList.getSchedule().getEffectiveDate() == null)
                return null;

            ConfidenceInterval result = super.getConfidenceInterval(taskList);

            // always recenter the time error interval - wide intervals are
            // of little consequence for rollups.  Bias causes bigger
            // problems.
            if (isCentered(result) == false)
                result = new EVTimeErrConfidenceInterval(taskList.getSchedule(),
                    RECENTER);

            return result;
        }

        private boolean isCentered(ConfidenceInterval result) {
            if (result instanceof LogCenteredConfidenceInterval)
                return true;

            if (result instanceof LinearRatioConfidenceInterval)
                return true;

            if (result instanceof EVTimeErrConfidenceInterval)
                return ((EVTimeErrConfidenceInterval) result).isCentered();

            return false;
        }

    }

    private boolean allSchedulesHaveCostInterval() {
        if (schedule.subSchedules.isEmpty())
            return false;
        Iterator i = schedule.subSchedules.iterator();
        while (i.hasNext()) {
            EVSchedule s = (EVSchedule) i.next();
            ConfidenceInterval ci = s.getMetrics().getCostConfidenceInterval();
            if (ci == null)
                return false;
            if (!(ci.getViability() > ConfidenceInterval.ACCEPTABLE))
                return false;
        }
        return true;
    }

    private boolean someSchedulesAreRollups() {
        Iterator i = schedule.subSchedules.iterator();
        while (i.hasNext()) {
            if (i.next() instanceof EVScheduleRollup)
                return true;
        }
        return false;
    }

    private void createConfidenceIntervals() {
        if (!allSchedulesHaveCostInterval() || someSchedulesAreRollups())
            setNullIntervals();
        else if (!allSchedulesHaveTimeErrInterval())
            createCostInterval();
        else
            createBothIntervals();
    }

    private void setNullIntervals() {
        schedule.getMetrics().setCostConfidenceInterval(null);
        schedule.getMetrics().setTimeErrConfidenceInterval(null);
        schedule.getMetrics().setDateConfidenceInterval(null);
        ((EVMetricsRollup) schedule.getMetrics())
            .setOptimizedDateConfidenceInterval(null);
    }

    private void createCostInterval() {
        // System.out.println("Creating cost interval");
        ConfidenceIntervalSum sum = new ConfidenceIntervalSum();
        sum.setAcceptableError(5 * 60);
        Iterator i = schedule.subSchedules.iterator();
        while (i.hasNext()) {
            EVSchedule s = (EVSchedule) i.next();
            sum.addInterval(s.getMetrics().getCostConfidenceInterval());
        }
        sum.intervalsComplete();
        // System.out.println("created " + sum.samples.size() + " cost samples");
        // sum.debugPrint(5);
        schedule.getMetrics().setCostConfidenceInterval(sum);
        schedule.getMetrics().setTimeErrConfidenceInterval(null);
        schedule.getMetrics().setDateConfidenceInterval(null);
        ((EVMetricsRollup) schedule.getMetrics())
            .setOptimizedDateConfidenceInterval(null);
    }

    private static final boolean COST_ONLY = false;
    private void createBothIntervals() {
        if (COST_ONLY) { createCostInterval(); return; }

        // System.out.println("Creating both intervals");
        List subs = schedule.subSchedules;
        EVScheduleRandom[] randSchedules = new EVScheduleRandom[subs.size()];
        for (int i = 0;   i < randSchedules.length;   i++)
            randSchedules[i] = new EVScheduleRandom((EVSchedule) subs.get(i));

        EVScheduleRollup sr = new EVScheduleRollup(randSchedules);
        EVScheduleConfidenceIntervals ci = new EVScheduleConfidenceIntervals
            (sr, Arrays.asList(randSchedules));

        EVMetricsRollup metrics = (EVMetricsRollup) schedule.getMetrics();
        metrics.setCostConfidenceInterval(ci.getCostInterval());
        metrics.setTimeErrConfidenceInterval(null);
        metrics.setDateConfidenceInterval(ci.getForecastDateInterval());
        metrics.setOptimizedDateConfidenceInterval
            (ci.getOptimizedForecastDateInterval());
    }



    private static boolean RECENTER = true;
    private boolean allSchedulesHaveTimeErrInterval() {
        if (schedule.subSchedules.isEmpty())
            return false;
        Iterator i = schedule.subSchedules.iterator();
        while (i.hasNext()) {
            EVSchedule s = (EVSchedule) i.next();
            ConfidenceInterval ci =
                s.getMetrics().getTimeErrConfidenceInterval();
            if (ci == null)
                return false;
            if (!(ci.getViability() > ConfidenceInterval.ACCEPTABLE))
                return false;
        }
        return true;
    }


    private static class EVLeafDateComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            EVTask t1 = (EVTask) o1;
            EVTask t2 = (EVTask) o2;

            // put completed tasks at the front of the list, in order of
            // completion
            int result = compareDates(t1.dateCompleted, t2.dateCompleted);
            if (result != 0) return result;

            // next, sort tasks by forecast/planned completion date.
            return compareDates(getProjectedDate(t1), getProjectedDate(t2));
        }
        private int compareDates(Date a, Date b) {
            if (a == b) return 0;
            if (a == null) return 1;
            if (b == null) return -1;
            return a.compareTo(b);
        }
        private Date getProjectedDate(EVTask t) {
            Date result = t.getForecastDate();
            if (result == null) result = t.planDate;
            return result;
        }
        public boolean equals(Object obj) {
            return this == obj;
        }
        public int hashCode() {
            return super.hashCode();
        }
    }
    private static final Comparator EV_LEAF_DATE_COMPARATOR =
        new EVLeafDateComparator();
}
