 // PSP Dashboard - Data Automation Tool for PSP-like processes
 // Copyright (C) 1999  United States Air Force
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
 // E-Mail POC:  ken.raisor@hill.af.mil

package pspdash;

import java.util.Date;
import java.text.MessageFormat;
import javax.swing.table.*;
import javax.swing.event.*;

import pspdash.data.DoubleData;

public class EVMetrics {

    /** The total planned time for all tasks in an EVModel, in minutes. */
    protected double totalPlanTime = 0.0;

    /** The total planned time for all <b>completed</b> tasks in an
     *  EVModel, in minutes.  */
    protected double earnedValueTime = 0.0;

    /** The total actual time for all <b>completed</b> tasks in an
     *  EVModel, in minutes.  */
    protected double actualTime = 0.0;

    /** The total planned time for all tasks that were planned to be
     * completed as of today, in minutes.  */
    protected double planTime = 0.0;

    /** The start date of this EVModel */
    protected Date startDate = null;

    /** The current date (or effective date) */
    protected Date currentDate = null;

    /** What % complete is the current period, in terms of elapsed time? */
    double periodPercent = 0.0;

    /** The end of the current time period in the schedule */
    protected Date periodEnd = null;

    public void reset(Date start, Date current,
                      Date periodStart, Date periodEnd) {
        totalPlanTime = earnedValueTime = actualTime = planTime = 0.0;
        startDate = start;
        currentDate = current;
        this.periodEnd = periodEnd;
        if (periodStart != null && periodEnd != null) {
            long periodElapsed = currentDate.getTime() - periodStart.getTime();
            long periodLength = periodEnd.getTime() - periodStart.getTime();
            periodPercent = (double) periodElapsed / (double) periodLength;
            if (periodPercent < 0.0) periodPercent = 0.0;
            if (periodPercent > 1.0) periodPercent = 1.0;
        } else {
            periodEnd = current;
            periodPercent = 0.0;
        }
    }
    public void addTask(double planTime, double actualTime,
                        Date planDate, Date actualDate) {
        this.totalPlanTime += planTime;

        // has the task been completed?
        if (actualDate != null) {
            this.earnedValueTime += planTime;
            this.actualTime += actualTime;
        }

        // did we plan to have the task completed by now?
        if (planDate != null) {
            if (planDate.before(currentDate))
                this.planTime += planTime;
            else if (!periodEnd.before(planDate))
                this.planTime += planTime * periodPercent;
        }
    }


    public double earnedValue() { return earnedValueTime; }
    public double actual()      { return actualTime;      }
    public double plan()        { return planTime;        }
    public double totalPlan()   { return totalPlanTime;   }
    public Date startDate()     { return startDate;       }
    public Date currentDate()   { return currentDate;     }



    public double costVariance() {
        return earnedValue() - actual();
    }
    public double scheduleVariance() {
        return earnedValue() - plan();
    }
    public double costVariancePercentage() {
        return costVariance() / earnedValue();
    }
    public double timeEstimatingError() {
        return  - costVariancePercentage();
    }
    public double scheduleVariancePercentage() {
        return scheduleVariance() / plan();
    }
    public double costPerformanceIndex() {
        return earnedValue() / actual();
    }
    public double schedulePerformanceIndex() {
        return earnedValue() / plan();
    }
    public double percentComplete() {
        return earnedValue() / totalPlan();
    }
    public double earnedValuePercentage() {
        return percentComplete();
    }
    public double percentSpent() {
        return actual() / totalPlan();
    }
    public double toCompletePerformanceIndex() {
        return (totalPlan() - earnedValue()) / (totalPlan() - actual());
    }
    public double improvementRatio() {
        return (toCompletePerformanceIndex() / costPerformanceIndex()) - 1.0;
    }
    public double independentForecastCost() {
        return totalPlan() / costPerformanceIndex();
    }

    /** the number of minutes that have elapsed since the beginning of
         the project. */
    public double elapsed() {
        Date s, c;
        if ((s = startDate()) == null) return Double.NaN;
        if ((c = currentDate()) == null) return Double.NaN;
        return (c.getTime() - s.getTime()) / (double) MINUTE_MILLIS;
    }

    public double scheduleVarianceDuration() {
        return scheduleVariance() * elapsed() / earnedValue();
    }
    public double independentForecastDuration() {
        return elapsed() / percentComplete();
    }
    public Date independentForecastDate() {
        Date s;
        if ((s = startDate()) == null) return null;
        double duration = independentForecastDuration();
        if (badDouble(duration)) return null;
        return new Date(s.getTime() + (long) (duration * MINUTE_MILLIS));
    }




    /** display a value as a number only, possibly preceeded by a
     * negative sign and possibly followed by a percentage sign or
     * other unit qualifier. Examples: 1.09, -65% */
    public static final int SHORT  = 0;

    /** display a value succintly, but rather than using a numeric sign,
     * interpret the value.  Examples: 3 days behind schedule, 45% over
     * budget */
    public static final int MEDIUM = 1;

    /** return a complete sentence explaining and interpreting the value.
     * Examples: 40% of the total work has been accomplished. */
    public static final int FULL   = 2;



    public String costVariance(int style) {
        double d= costVariance();
        if (badDouble(d)) return null;
        return MessageFormat.format(CV_FORMATS[style], args(d,formatCost(d)));
    }
    static final String[] CV_FORMATS = {
        "{0,number}",
        "{1} {0,choice,-1#over|0#under} budget",
        "The tasks completed to date have taken {1} {0,choice,-1#more|0#less} effort than planned." };


    public String scheduleVariance(int style) {
        double d= scheduleVariance();
        if (badDouble(d)) return null;
        return MessageFormat.format(SV_FORMATS[style], args(d,formatCost(d)));
    }
    static final String[] SV_FORMATS = {
        "{0,number}",
        "{1} {0,choice,-1#behind|0#ahead of} schedule",
        "You have completed {1} {0,choice,-1#less|0#more} work than you had planned to complete by this date." };


    public String costVariancePercentage(int style) {
        double d= costVariancePercentage();
        if (badDouble(d)) return null;
        return MessageFormat.format(CVP_FORMATS[style], absArgs(d));
    }
    static final String[] CVP_FORMATS = {
        "{0,number,percent}",
        "{1,number,percent} {0,choice,-1#over|0#under} budget",
        "The tasks completed to date have taken {1,number,percent} {0,choice,-1#less|0#more} effort than planned." };

    public String scheduleVariancePercentage(int style) {
        double d= scheduleVariancePercentage();
        if (badDouble(d)) return null;
        return MessageFormat.format(SVP_FORMATS[style], absArgs(d));
    }
    static final String[] SVP_FORMATS = {
        "{0,number,percent}",
        "{1,number,percent} {0,choice,-1#behind|0#ahead of} schedule",
        "You have completed  {1,number,percent} {0,choice,-1#less|0#more} work than you had planned to complete by this date." };


    public String costPerformanceIndex(int style) {
        double d= costPerformanceIndex();
        if (badDouble(d)) return null;
        return MessageFormat.format(CPI_FORMATS[style], args(d));
    }
    static final String[] CPI_FORMATS = {
        "{0,number}",
        "{0,number}",
        "Work is taking {0,choice,0#more|1#less} time than planned.  For every effort hour actually spent, you are accomplishing {0,number} hours worth of planned results." };

    public String schedulePerformanceIndex(int style) {
        double d= schedulePerformanceIndex();
        if (badDouble(d)) return null;
        return MessageFormat.format(SPI_FORMATS[style], args(d));
    }
    static final String[] SPI_FORMATS = {
        "{0,number}",
        "{0,number}",
        "You are completing tasks more {0,choice,0#slowly|1#quickly} than scheduled.  For every effort hour of work you should have accomplished to date, you are completing {0,number} hours." };


    public String percentComplete(int style) {
        double d= percentComplete();
        if (badDouble(d)) return null;
        return MessageFormat.format(PC_FORMATS[style], args(d));
    }
    static final String[] PC_FORMATS = {
        "{0,number,percent}",
        "{0,number,percent}",
        "You have completed {0,number,percent} of the total work." };


    public String percentSpent(int style) {
        double d= percentSpent();
        if (badDouble(d)) return null;
        return MessageFormat.format(PS_FORMATS[style], args(d));
    }
    static final String[] PS_FORMATS = {
        "{0,number,percent}",
        "{0,number,percent}",
        "You have spent {0,number,percent} of the total planned effort hours." };


    public String toCompletePerformanceIndex(int style) {
        double d= toCompletePerformanceIndex();
        if (badDouble(d)) return null;
        return MessageFormat.format(TCPI_FORMATS[style], args(d));
    }
    static final String[] TCPI_FORMATS = {
        "{0,number}",
        "{0,number}",
        "In order to complete all the work within the original effort estimate, each future hour of effort must accomplish {0,number} hours worth of planned results." };


    public String improvementRatio(int style) {
        double d= improvementRatio();
        if (badDouble(d)) return null;
        return MessageFormat.format(IR_FORMATS[style], args(d));
    }
    static final String[] IR_FORMATS = {
        "{0,number}",
        "{0,number}",
        "In order to complete all the work within the original effort estimate, you must be {0,number} times as productive in the future as you have been in the past." };


    public String independentForecastCost(int style) {
        double d= independentForecastCost();
        if (badDouble(d)) return null;
        return MessageFormat.format(IFC_FORMATS[style], args(formatCost(d)));
    }
    static final String[] IFC_FORMATS = {
        "{0}",
        "{0}",
        "Based upon your current CPI, the total work is forecast to require {0} of effort." };


    public String elapsed(int style) {
        double d= elapsed();
        if (badDouble(d)) return null;
        return MessageFormat.format(EL_FORMATS[style],
                                    args(formatDuration(d)));
    }
    static final String[] EL_FORMATS = {
        "{0}",
        "{0}",
        "The project began {0} ago." };


    public String scheduleVarianceDuration(int style) {
        double d= scheduleVarianceDuration();
        if (badDouble(d)) return null;
        return MessageFormat.format(SVD_FORMATS[style], durArgs(d));
    }
    static final String[] SVD_FORMATS = {
        "{0,number}",
        "{1} {0,choice,-1#behind|0#ahead of} schedule",
        "Based upon your current rate of task completion to date, you are {1} {0,choice,-1#behind|0#ahead of} schedule." };


    public String independentForecastDuration(int style) {
        double d= independentForecastDuration();
        if (badDouble(d)) return null;
        return MessageFormat.format(IFD_FORMATS[style],
                                    args(formatDuration(d)));
    }
    static final String[] IFD_FORMATS = {
        "{0}",
        "{0}",
        "Based upon your current rate of task completion to date, the total work schedule is forecast to be {1}." };


    public String independentForecastDate(int style) {
        Date d =  independentForecastDate();
        if (d == null) return null;
        return MessageFormat.format(IFDT_FORMATS[style], args(d));
    }
    static final String[] IFDT_FORMATS = {
        "{0,date,}",
        "{0,date}",
        "Based upon your current rate of task completion to date, the total work is forecast to be completed {0,date}." };


    public static String formatDuration(double duration) {
        return formatDuration(duration, YEAR_MINUTES);
    }
    public static String formatCost(double cost) {
        return formatDuration(cost, HOUR_MINUTES);
    }
    public static String formatDuration(double duration, int maxUnits) {
        if (badDouble(duration))
            return null;
        if (duration < 0) duration = -duration;

        switch (maxUnits) {
        case YEAR_MINUTES:
            if (duration > YEAR_MINUTES)
                return formatNumber(duration / YEAR_MINUTES) + " years";
            if (duration == YEAR_MINUTES)
                return "1 year";
        case MONTH_MINUTES:
            if (duration > MONTH_MINUTES)
                return formatNumber(duration / MONTH_MINUTES) + " months";
            if (duration == MONTH_MINUTES)
                return "1 month";
        case WEEK_MINUTES:
            if (duration > WEEK_MINUTES)
                return formatNumber(duration / WEEK_MINUTES) + " weeks";
            if (duration == WEEK_MINUTES)
                return "1 week";
        case DAY_MINUTES:
            if (duration > DAY_MINUTES)
                return formatNumber(duration / DAY_MINUTES) + " days";
            if (duration == DAY_MINUTES)
                return "1 day";
        case HOUR_MINUTES:
        default:
            if (duration > HOUR_MINUTES)
                return formatNumber(duration / HOUR_MINUTES) + " hours";
            if (duration == HOUR_MINUTES)
                return "1 hour";
        case MINUTE:
            if (duration == 1.0)
                return "1 minute";
        }
        return formatNumber(duration) + " minutes";
    }
    public static String formatNumber(double number) {
        return DoubleData.formatNumber(number);
    }

    static final int MINUTE_MILLIS = 60 /*seconds*/ * 1000 /*millis*/;
    static final int MINUTE        = 1;
    static final int HOUR_MINUTES  = 60;
    static final int DAY_MINUTES   = 24 /*hours*/ * HOUR_MINUTES;
    static final int WEEK_MINUTES  = 7  /*days*/  * DAY_MINUTES;
    static final int MONTH_MINUTES = 30 /*days*/  * DAY_MINUTES;
    static final int YEAR_MINUTES  = 365 /*days*/ * DAY_MINUTES;

    public static boolean badDouble(double d) {
        return Double.isNaN(d) || Double.isInfinite(d);
    }
    public static Object[] args(double d) {
        Object[] result = { new Double(d) };
        return result;
    }
    public static Object[] args(Object o) {
        Object[] result = { o };
        return result;
    }
    public static Object[] args(double d, Object o) {
        Object[] result = { new Double(d), o };
        return result;
    }
    public static Object[] durArgs(double d) {
        Object[] result = { new Double(d), formatDuration(d) };
        return result;
    }
    public static Object[] absArgs(double d) {
        Object[] result = { new Double(d), new Double(Math.abs(d)) };
        return result;
    }
}
