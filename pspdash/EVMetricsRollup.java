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
import java.util.Date;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.Map;
import java.text.MessageFormat;


public class EVMetricsRollup extends EVMetrics {

    /** The date the project is forecast to complete */
    Date independentForecastDate = null;

    public void reset(Date effectiveDate) {
        totalPlanTime = earnedValueTime = actualTime = planTime = 0.0;
        totalSchedulePlanTime = totalScheduleActualTime = 0.0;
        currentDate = effectiveDate;
        startDate = independentForecastDate = null;
        errors = null;
    }

    public void addMetrics(EVMetrics that) {
        this.totalPlanTime   += that.totalPlanTime;
        this.earnedValueTime += that.earnedValueTime;
        this.actualTime      += that.actualTime;
        this.planTime        += that.planTime;
        this.totalSchedulePlanTime += that.totalSchedulePlanTime;
        this.totalScheduleActualTime += that.totalScheduleActualTime;
        this.startDate =
            EVScheduleRollup.minDate(this.startDate, that.startDate);
        this.independentForecastDate =
            EVScheduleRollup.maxDate(this.independentForecastDate,
                                     that.independentForecastDate());
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
    public void recalcComplete(EVSchedule s) {
        recalcOptimizedPlanDate(s);
        super.recalcComplete(s);
    }
    protected void recalcScheduleTime(EVSchedule s) {
        // do nothing - it is already calculated.
    }
    protected void recalcOptimizedPlanDate(EVSchedule s) {
        optimizedPlanDate = s.getHypotheticalDate(totalPlan());
    }


    public double independentForecastDuration() {
        return calcDuration(startDate(), independentForecastDate());
    }
    public Date independentForecastDate() {
        return independentForecastDate;
    }

    public double optimizedForecastDuration() {
        if (useSimpleForecastDateFormula)
            return super.independentForecastDuration();
        else
            return calcDuration(startDate(), super.independentForecastDate());
    }

    private Date optimizedForecastDate = null;
    public Date optimizedForecastDate() {
        if (useSimpleForecastDateFormula) {
            // use the extrapolation algorithm that EVMetrics uses to
            // calculate the independentForecastDate.
            Date s;
            if ((s = startDate()) == null) return null;
            double duration = optimizedForecastDuration();
            if (badDouble(duration)) return null;
            return new Date(s.getTime() + (long) (duration * MINUTE_MILLIS));
        } else
            return super.independentForecastDate();
    }

    public Date optimizedPlanDate() {
        return optimizedPlanDate;
    }
    private Date optimizedPlanDate = null;
    void setOptimizedPlanDate(Date d) { optimizedPlanDate = d; }


    protected String getResourcePrefix() { return "Rollup_"; }

    protected ArrayList buildFormatters() {
        ArrayList result = super.buildFormatters();
        result.add(new DurationMetricFormatter
            ("Optimized_Forecast_Duration") {
                double val() { return optimizedForecastDuration(); } } );
        result.add(new DateMetricFormatter("Optimized_Forecast_Date") {
                Date val() { return optimizedForecastDate(); } } );
        result.add(new DateMetricFormatter("Optimized_Plan_Date") {
                Date val() { return optimizedPlanDate(); } } );
        return result;
    }

}
