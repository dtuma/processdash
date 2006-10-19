// Copyright (C) 2006 Tuma Solutions, LLC
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

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.processdash.util.PatternList;

public class EVScheduleFiltered extends EVSchedule {


    private EVTaskList taskList;

    private EVTaskFilter filter;

    public EVScheduleFiltered(EVTaskList taskList, EVTaskFilter filter) {
        this.taskList = taskList;
        this.filter = filter;
        metrics.discardMetrics(QUESTIONABLE_METRICS);
        recalculate();
    }

    private Date forecastDate;

    private void recalculate() {
        addAllPeriods(taskList.schedule.periods, this.periods);

        Date effectiveDate = taskList.getSchedule().getEffectiveDate();
        setEffectiveDate(effectiveDate);
        metrics.reset(getStartDate(), effectiveDate,
                getPeriodStart(effectiveDate), getPeriodEnd(effectiveDate));

        cleanUp();

        forecastDate = EVSchedule.A_LONG_TIME_AGO;

        List evLeaves = taskList.calculator.getEVLeaves();
        EVTask taskRoot = (EVTask) taskList.getRoot();
        calculateActualNodeTimes(taskRoot);
        addTaskData(taskRoot, evLeaves);

        setFakePeriodData();

        if (forecastDate == EVSchedule.A_LONG_TIME_AGO)
            forecastDate = null;
        metrics.setForecastDate(forecastDate);
        metrics.recalcComplete(this);

        setEffectiveDate(effectiveDate);
    }

    private void calculateActualNodeTimes(EVTask task) {
        task.actualNodeTime = task.actualCurrentTime;
        for (int i = task.getNumChildren(); i-- > 0;) {
            EVTask child = task.getChild(i);
            calculateActualNodeTimes(child);
            task.actualNodeTime -= child.actualCurrentTime;
        }
    }

    private void addTaskData(EVTask task, List evLeaves) {
        if (filter.include(task)) {
            if (evLeaves.contains(task)) {
                double value = task.planValue;
                double cost = task.actualCurrentTime;
                Date planDate = task.planDate;
                Date actualDate = task.dateCompleted;
                Date taskForecastDate = task.getForecastDate();

                metrics.addTask(value, cost, planDate, actualDate);
                saveActualTaskInfo(planDate, value, 0, 0, 0, true);
                saveActualTaskInfo(actualDate, 0, value, 0, cost, true);
                forecastDate = EVCalculator.maxForecastDate(forecastDate,
                        taskForecastDate);
                return;

            } else if (task.actualNodeTime > 0 && !task.isLevelOfEffortTask()
                    && !task.isUserPruned()) {
                Date startDate = metrics.startDate();
                metrics.addTask(0, task.actualNodeTime, null, startDate);
                saveCompletedTaskCost(startDate, task.actualNodeTime);
            }
        }

        for (int i = task.getNumChildren(); i-- > 0;) {
            EVTask child = task.getChild(i);
            addTaskData(child, evLeaves);
        }
    }

    private void setFakePeriodData() {
        Iterator i = periods.iterator();
        Period p = (Period) i.next();
        while (i.hasNext()) {
            p = (Period) i.next();
            p.cumPlanDirectTime = p.cumPlanValue;
            p.cumActualDirectTime = p.cumActualCost;
            p.planDirectTime = p.cumPlanValue - p.previous.cumPlanValue;
        }
    }



    private static final PatternList QUESTIONABLE_METRICS = new PatternList()
            .addRegexp("_Duration$").addRegexp("_Range$");

    public String getColumnName(int i) {
        switch (i) {
        case FROM_COLUMN:
        case TO_COLUMN:
        case PLAN_CUM_VALUE_COLUMN:
        case CUM_VALUE_COLUMN:
            return super.getColumnName(i);
        }

        return " " + super.getColumnName(i) + " ";
    }

    protected double totalPlan() {
        return taskList.getSchedule().totalPlan();
    }



}
