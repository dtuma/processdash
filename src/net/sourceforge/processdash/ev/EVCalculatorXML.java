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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.sourceforge.processdash.ev.ci.ConfidenceInterval;
import net.sourceforge.processdash.ev.ci.ConfidenceIntervalProvider;
import net.sourceforge.processdash.ev.ci.DelegatingConfidenceInterval;
import net.sourceforge.processdash.ev.ci.EVScheduleConfidenceIntervals;
import net.sourceforge.processdash.ev.ci.TargetedConfidenceInterval;


public class EVCalculatorXML extends EVCalculator {


    private EVTaskList taskList;
    private EVTask taskRoot;
    private EVSchedule schedule;
    private boolean calcForSnapshot;


    public EVCalculatorXML(EVTaskList taskList, boolean reorderCompletedTasks) {
        this.taskList = taskList;
        this.taskRoot = taskList.getTaskRoot();
        this.schedule = taskList.getSchedule();
        this.reorderCompletedTasks = reorderCompletedTasks;
        this.costIntervalProvider = new XmlCostIntervalProvider();
        this.timeErrIntervalProvider = new XmlTimeErrIntervalProvider();
        this.calcForSnapshot = false;
    }

    public void setCalcForSnapshot() {
        this.calcForSnapshot = true;
    }


    public void recalculate() {
        // load the baseline data for the schedule
        recalcBaselineData(taskRoot);

        // mark ANCESTOR_PRUNED nodes appropriately
        pruneNodes(taskRoot, false);

        // calculate top-down and bottom-up plan times
        taskRoot.recalcPlanTimes();

        // create and sort the list of EV leaves
        scheduleStartDate = schedule.getStartDate();
        evLeaves = new LinkedList();
        getEVLeaves(taskRoot);
        sortEVLeafList(evLeaves);

        // calculate cumulative plan value and value earned
        calcTaskValues(evLeaves);
        recalcValueEarned(taskRoot);
        schedule.getMetrics().recalcScheduleTime(schedule);

        // reinitialize the confidence intervals from our providers
        schedule.getMetrics().setCostConfidenceInterval(
            costIntervalProvider.getConfidenceInterval(taskList));
        schedule.getMetrics().setTimeErrConfidenceInterval(
            timeErrIntervalProvider.getConfidenceInterval(taskList));
        schedule.getMetrics().setDateConfidenceInterval(
            new DeferredDateConfidenceInterval());

        EVForecastDateCalculators.XML_FORECAST.calculateForecastDates(taskRoot,
                schedule, schedule.getMetrics(), evLeaves);
        schedule.getMetrics().planDate = taskRoot.getPlanDate();
        schedule.getMetrics().loadBaselineData(taskRoot);
        schedule.getMetrics().recalcComplete(schedule);

        // check for errors in the task list
        if (calcForSnapshot == false) {
            taskList.scanForMilestoneErrors(evLeaves);
            taskRoot.checkForNodeErrors(schedule.getMetrics(), 0,
                new ArrayList(), new ArrayList(), false);
        }
    }


    private double calcTaskValues(List evLeaves) {
        double cumPlanValue = 0;
        Iterator i = evLeaves.iterator();
        while (i.hasNext()) {
            EVTask task = (EVTask) i.next();

            cumPlanValue += task.planValue;
            task.cumPlanValue = cumPlanValue;

            if (task.dateCompleted != null)
                task.valueEarned = task.planValue;

        }
        return cumPlanValue;
    }


    private void recalcValueEarned(EVTask task) {
        if (!task.isLeaf() && !task.isEVLeaf()) {
            task.valueEarned = 0;
            // for nonleaves, ask each of our children to recalc.
            for (int i = 0;   i < task.getNumChildren();   i++) {
                EVTask child = task.getChild(i);
                recalcValueEarned(child);
                task.valueEarned += child.valueEarned;
            }
        }
    }

    private class XmlIntervalProvider implements ConfidenceIntervalProvider {

        private ConfidenceInterval interval;

        XmlIntervalProvider(ConfidenceInterval interval) {
            this.interval = interval;
        }

        public ConfidenceInterval getConfidenceInterval(EVTaskList taskList) {
            return interval;
        }
    }

    private class XmlCostIntervalProvider extends XmlIntervalProvider {
        XmlCostIntervalProvider() {
            super(schedule.getMetrics().getCostConfidenceInterval());
        }
    }

    private class XmlTimeErrIntervalProvider extends XmlIntervalProvider {
        XmlTimeErrIntervalProvider() {
            super(schedule.getMetrics().getTimeErrConfidenceInterval());
        }
    }

    private class DeferredDateConfidenceInterval extends
            DelegatingConfidenceInterval {

        private boolean initialized = false;
        private double viabilityTarget = Double.NaN;
        private double viabilityProb;

        @Override
        protected ConfidenceInterval getDelegate() {
            if (!initialized) {
                delegate = buildDelegate();
                if (delegate != null && delegate.getViability() < ACCEPTABLE)
                    delegate = null;
                initialized = true;
            }

            return super.getDelegate();
        }

        private ConfidenceInterval buildDelegate() {
            EVMetrics metrics = schedule.getMetrics();
            if (metrics.percentComplete() > 0.995)
                return null;

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

                if (completionDate instanceof TargetedConfidenceInterval
                        && !Double.isNaN(viabilityTarget)) {
                    ((TargetedConfidenceInterval) completionDate)
                            .calcViability(viabilityTarget, viabilityProb);
                }

                return completionDate;
            } catch (Exception e) {
                metrics.setDateConfidenceInterval(null);
                System.out.println("Error calculating schedule interval:");
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public void calcViability(double target, double minimumProb) {
            if (initialized)
                super.calcViability(target, minimumProb);
            else {
                viabilityTarget = target;
                viabilityProb = minimumProb;
            }
        }

        @Override
        public double getViability() {
            return (initialized ? super.getViability() : NOMINAL);
        }

    }
}
