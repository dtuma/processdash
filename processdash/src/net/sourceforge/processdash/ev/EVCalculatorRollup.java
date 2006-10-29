// Copyright (C) 2003-2006 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import net.sourceforge.processdash.ev.ci.ConfidenceInterval;
import net.sourceforge.processdash.ev.ci.ConfidenceIntervalSum;
import net.sourceforge.processdash.ev.ci.EVScheduleConfidenceIntervals;
import net.sourceforge.processdash.ev.ci.EVTimeErrConfidenceInterval;

public class EVCalculatorRollup extends EVCalculator {

    private EVTask taskRoot;
    private Vector evTaskLists;
    private EVScheduleRollup schedule;

    public EVCalculatorRollup(EVTask root, Vector evTaskLists,
                              EVScheduleRollup schedule) {
        this.taskRoot = root;
        this.evTaskLists = evTaskLists;
        this.schedule = schedule;
    }

    public void recalculate() {
        EVTaskList taskList;

        // Recalculate all the subschedules.
        for (int i = evTaskLists.size();   i-- > 0; ) {
            taskList = (EVTaskList) evTaskLists.get(i);
            taskList.recalc();

            // Some types of task lists perform a recalc by completely
            // replacing their root task and schedule. Give them the
            // benefit of the doubt and make certain that we are using
            // the correct root and schedule
            taskRoot.replace(i, (EVTask) taskList.getRoot());
            schedule.replaceSchedule(i, taskList);
        }

        // Recalculate the root node.
        recalcRollupNode();

        // adjust level of effort percentages
        calculateLevelOfEffort();

        // Calculate confidence intervals, if possible.
        createConfidenceIntervals();

        // Recalculate the rollup schedule.
        schedule.recalc();

        // check for duplicate schedules.
        taskRoot.checkForNodeErrors(schedule.getMetrics(), 0,
                new ArrayList(), new ArrayList(), true);
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
            // recenter the time error interval - wide intervals are
            // of little consequence for rollups.  Bias causes bigger
            // problems.
            s.getMetrics().setTimeErrConfidenceInterval
                (new EVTimeErrConfidenceInterval(s, RECENTER));

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

            // next, sort tasks by planned completion date.
            return compareDates(t1.planDate, t2.planDate);
        }
        private int compareDates(Date a, Date b) {
            if (a == b) return 0;
            if (a == null) return 1;
            if (b == null) return -1;
            return a.compareTo(b);
        }
        public boolean equals(Object obj) {
            return this == obj;
        }
    }
    private static final Comparator EV_LEAF_DATE_COMPARATOR =
        new EVLeafDateComparator();
}
