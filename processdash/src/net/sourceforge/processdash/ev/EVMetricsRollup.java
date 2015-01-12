// Copyright (C) 2002-2015 Tuma Solutions, LLC
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

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.sourceforge.processdash.ev.ci.ConfidenceInterval;
import net.sourceforge.processdash.ev.ci.TargetedConfidenceInterval;


public class EVMetricsRollup extends EVMetrics {

    /** The forecast total cost of the project */
    double independentForecastCost;

    /** The optimized date the project is planned to complete */
    protected Date optimizedPlanDate = null;

    /** The optimized date the project is replanned to complete */
    protected Date optimizedReplanDate = null;

    /** The optimized date the project is forecast to complete */
    protected Date optimizedForecastDate;

    /** A confidence interval predicting the optimized forecast
     * completion date. */
    protected ConfidenceInterval optCompletionDateInterval = null;

    /** A flag indicating whether this metrics object is rolling up
     * other EVMetricsRollup objects. */
    protected boolean isRollupOfRollups = false;

    /** For a rollup of rollups, the chronologically latest opt plan date. */
    protected Date rollupOfOptimizedPlanDates;

    /** For a rollup of rollups, the chronologically latest opt replan date. */
    protected Date rollupOfOptimizedReplanDates;

    /** For a rollup of rollups, the chronologically latest opt fcst date. */
    protected Date rollupOfOptimizedForecastDates;

    /** The earliest date a subschedule plans to finish */
    protected Date earliestPlanDate;

    /** The earliest date a subschedule replans to finish */
    protected Date earliestReplanDate;

    /** The earliest date a subschedule is forecast to finish */
    protected Date earliestForecastDate;


    public EVMetricsRollup() {
        super();
    }

    public EVMetricsRollup(boolean supportFormatting) {
        super(supportFormatting);
    }

    public void reset(Date effectiveDate) {
        totalPlanTime = earnedValueTime = actualTime = planTime = 0.0;
        totalSchedulePlanTime = totalScheduleActualTime = 0.0;
        independentForecastCost = 0;
        currentDate = effectiveDate;
        startDate = planDate = rollupOfOptimizedPlanDates = null;
        replanDate = forecastDate = rollupOfOptimizedForecastDates =
            rollupOfOptimizedReplanDates = EVSchedule.A_LONG_TIME_AGO;
        earliestPlanDate = earliestReplanDate = earliestForecastDate =
            EVSchedule.NEVER;
        errors = null;
        isRollupOfRollups = false;
    }

    public void addMetrics(EVMetrics that) {
        this.totalPlanTime   += that.totalPlanTime;
        this.earnedValueTime += that.earnedValueTime;
        this.actualTime      += that.actualTime;
        this.planTime        += that.planTime;
        this.totalSchedulePlanTime += that.totalSchedulePlanTime;
        this.totalScheduleActualTime += that.totalScheduleActualTime;
        this.independentForecastCost += that.independentForecastCostEff();
        this.startDate =
            EVScheduleRollup.minDate(this.startDate, that.startDate);
        this.planDate =
            EVCalculator.maxPlanDate(this.planDate, that.planDate());
        this.replanDate =
            EVCalculator.maxForecastDate(this.replanDate, that.replanDate());
        this.forecastDate =
            EVCalculator.maxForecastDate(this.forecastDate,
                                         that.independentForecastDate());

        if (that instanceof EVMetricsRollup) {
            this.isRollupOfRollups = true;

            EVMetricsRollup thatRollUp = (EVMetricsRollup) that;

            Date thatDate = thatRollUp.optimizedPlanDate();
            if (thatDate == null) thatDate = that.planDate();
            this.rollupOfOptimizedPlanDates = EVCalculator.maxPlanDate(
                    this.rollupOfOptimizedPlanDates, thatDate);

            thatDate = thatRollUp.optimizedReplanDate();
            if (thatDate == null) thatDate = that.replanDate();
            this.rollupOfOptimizedReplanDates = EVCalculator.maxForecastDate(
                    this.rollupOfOptimizedReplanDates, thatDate);

            thatDate = thatRollUp.optimizedForecastDate();
            if (thatDate == null) thatDate = that.independentForecastDate();
            this.rollupOfOptimizedForecastDates = EVCalculator.maxForecastDate(
                    this.rollupOfOptimizedForecastDates, thatDate);

            this.earliestPlanDate = EVScheduleRollup.minDate(
                this.earliestPlanDate, thatRollUp.earliestPlanDate);
            this.earliestReplanDate = EVScheduleRollup.minDate(
                this.earliestReplanDate, thatRollUp.earliestReplanDate);
            this.earliestForecastDate = EVScheduleRollup.minDate(
                this.earliestForecastDate, thatRollUp.earliestForecastDate);

        } else {
            this.rollupOfOptimizedPlanDates = EVCalculator.maxPlanDate(
                    this.rollupOfOptimizedPlanDates, that.planDate());
            this.rollupOfOptimizedReplanDates = EVCalculator.maxForecastDate(
                    this.rollupOfOptimizedReplanDates, that.replanDate());
            this.rollupOfOptimizedForecastDates = EVCalculator.maxForecastDate(
                    this.rollupOfOptimizedForecastDates,
                    that.independentForecastDate());
            this.earliestPlanDate = EVScheduleRollup.minDate(
                this.earliestPlanDate, that.planDate);
            this.earliestReplanDate = EVScheduleRollup.minDate(
                this.earliestReplanDate, that.replanDate);
            this.earliestForecastDate = EVScheduleRollup.minDate(
                this.earliestForecastDate, that.forecastDate);
        }

        if (that.errors != null) {
            if (this.errors == null)
                this.errors = new TreeMap();
            String qualifier = that.errorQualifier;
            if (qualifier == null || qualifier.length() == 0)
                this.errors.putAll(that.errors);
            else {
                Iterator i = that.errors.entrySet().iterator();
                while (i.hasNext()) {
                    Map.Entry e = (Map.Entry) i.next();
                    this.errors.put(qualifier + e.getKey(),
                                    e.getValue());
                }
            }
        }
    }

    protected void recalcViability(EVSchedule s) {
        super.recalcViability(s);
        if (costInterval == null)
            optCompletionDateInterval = null;

        else {
            if (optCompletionDateInterval instanceof TargetedConfidenceInterval)
                retargetViability
                    (s, (TargetedConfidenceInterval)optCompletionDateInterval,
                     optimizedForecastDate());
            if (unviable(optCompletionDateInterval)) {
                // System.out.println("opt date interval is not viable");
                optCompletionDateInterval = null;
            }
        }
    }
    protected void recalcScheduleTime(EVSchedule s) {
        // do nothing - it is already calculated.
    }
    protected void recalcForecastDate(EVSchedule s) {
        if (replanDate == EVSchedule.A_LONG_TIME_AGO
                || replanDate == EVSchedule.NEVER)
            replanDate = null;
        if (forecastDate == EVSchedule.A_LONG_TIME_AGO
                || forecastDate == EVSchedule.NEVER)
            forecastDate = null;

        if (isRollupOfRollups) {
            optimizedPlanDate = filterNonUniqueDate(rollupOfOptimizedPlanDates,
                    planDate);

            optimizedReplanDate = filterNonUniqueDate(
                    rollupOfOptimizedReplanDates, replanDate);
            if (optimizedReplanDate == EVSchedule.A_LONG_TIME_AGO)
                optimizedReplanDate = null;

            optimizedForecastDate = filterNonUniqueDate(
                    rollupOfOptimizedForecastDates, forecastDate);
            if (optimizedForecastDate == EVSchedule.A_LONG_TIME_AGO)
                optimizedForecastDate = null;
        } else {
            optimizedPlanDate = s.getHypotheticalDate(totalPlan(), false);
            if (optimizedPlanDate == EVSchedule.NEVER)
                optimizedPlanDate = null;

            // calculate the replanned completion date.
            EVForecastDateCalculators.REPLAN_EXTRAPOLATION
                    .calculateForecastDates(null, s, this, null);

            // recalculate the optimized forecast date using a balanced
            // schedule extrapolation.
            //
            // The forecast calculator will write its calculation into the
            // forecastDate field, rather than into the optimized forecast date
            // field where we want the data to go.  So we need to do a few
            // gyrations to preserve the current forecast date and get the
            // optimized calculation moved into the right field.
            Date savedForecastDate = forecastDate;
            EVForecastDateCalculators.SCHEDULE_EXTRAPOLATION_2
                    .calculateForecastDates(null, s, this, null);
            optimizedForecastDate = forecastDate;
            forecastDate = savedForecastDate;
        }
    }
    private Date filterNonUniqueDate(Date a, Date b) {
        if (a == null || a.equals(b))
            return null;
        else
            return a;
    }

    public void setOptimizedDateConfidenceInterval
        (ConfidenceInterval optCompletionDate)
    {
        optCompletionDateInterval = optCompletionDate;
    }

    public ConfidenceInterval getOptimizedDateConfidenceInterval() {
        return optCompletionDateInterval;
    }


    public double independentForecastCost() {
        return independentForecastCost;
    }


    public double optimizedForecastDuration() {
        return calcDuration(startDate(), optimizedForecastDate);
    }

    public Date optimizedForecastDate() {
        return optimizedForecastDate;
    }
    public Date optimizedForecastDateLPI() {
        if (optCompletionDateInterval == null)
            return null;
        else
            return convertToDate(optCompletionDateInterval.getLPI(0.70));
    }
    public Date optimizedForecastDateUPI() {
        if (optCompletionDateInterval == null)
            return null;
        else
            return convertToDate(optCompletionDateInterval.getUPI(0.70));
    }


    public Date optimizedPlanDate() {
        return optimizedPlanDate;
    }

    public Date optimizedReplanDate() {
        return optimizedReplanDate;
    }

    public boolean isRollupOfRollups() {
        return isRollupOfRollups;
    }

    protected String getResourcePrefix() { return "Rollup_"; }

    protected List buildFormatters() {
        List result = super.buildFormatters();
        result.add(new DurationMetricFormatter
            ("Optimized_Forecast_Duration") {
                double val() { return optimizedForecastDuration(); } } );
        result.add(new DateMetricFormatter("Optimized_Forecast_Date") {
                Date val() { return optimizedForecastDate(); } } );
        result.add(new DateRangeMetricFormatter
            ("Optimized_Forecast_Date_Range") {
                Date lpi() { return optimizedForecastDateLPI(); }
                Date upi() { return optimizedForecastDateUPI(); } } );
        result.add(1, new DateMetricFormatter("Optimized_Plan_Date") {
                Date val() { return optimizedPlanDate(); } } );
        result.add(3, new DateMetricFormatter("Optimized_Replan_Date") {
            Date val() { return optimizedReplanDate(); } } );
        return result;
    }

}
