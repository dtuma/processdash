// PSP Dashboard - Data Automation Tool for PSP-like processes
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net


package pspdash;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

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
            taskRoot.replace(i, (EVTask) taskList.root);
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
    }

    /** If this node is the root node of an EVTaskList rollup, this will
     *  recalculate it.
     *
     * <b>Important:</b> the children of this node (which are
     * themselves root nodes of other EVTaskLists) should already be
     * recalculated before calling this method.
     */
    public void recalcRollupNode() {
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
//        if (taskRoot.dateCompleted == EVSchedule.A_LONG_TIME_AGO)
//            taskRoot.dateCompleted = null;
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
                evLeaves.addAll(taskList.calculator.getEVLeaves());
            }
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

    private void createConfidenceIntervals() {
        if (!allSchedulesHaveCostInterval())
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
        sum.acceptableError = 5 * 60;
        Iterator i = schedule.subSchedules.iterator();
        while (i.hasNext()) {
            EVSchedule s = (EVSchedule) i.next();
            sum.addInterval(s.getMetrics().getCostConfidenceInterval());
        }
        sum.intervalsComplete();
        // System.out.println("created " + sum.samples.size() + " cost samples");
        sum.debugPrint(5);
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
        sr.metrics = new EVMetricsRollupRandom(sr);
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


}
