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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.text.MessageFormat;
import javax.swing.table.*;
import javax.swing.event.*;
import org.w3c.dom.Element;

import pspdash.data.DoubleData;

public class EVMetrics implements TableModel {

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

    /** A list of warnings/errors associated with this EVModel.
     *  the keys are the error messages; they may map to an EVTask
     *  where the error was located. */
    protected Map errors = null;

    public void reset(Date start, Date current,
                      Date periodStart, Date periodEnd) {
        totalPlanTime = earnedValueTime = actualTime = planTime = 0.0;
        startDate = start;
        currentDate = current;
        this.periodEnd = periodEnd;
        errors = null;
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
    public void addError(String message, EVTask node) {
        if (errors == null) errors = new HashMap();
        errors.put(message, node);
    }
    public void recalcComplete(EVSchedule s) {
        fireTableChanged(new TableModelEvent(this, 0, getRowCount()-1));
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

    public Map getErrors() {
        if (errors == null)
            return null;
        else
            return Collections.unmodifiableMap(errors);
    }




    /** display the name of the metric. */
    public static final int NAME = 0;

    /** display a value as a number only, possibly preceeded by a
     * negative sign and possibly followed by a percentage sign or
     * other unit qualifier. Examples: 1.09, -65% */
    public static final int SHORT  = 1;

    /** display a value succintly, but rather than using a numeric sign,
     * interpret the value.  Examples: 3 days behind schedule, 45% over
     * budget */
    public static final int MEDIUM = 2;

    /** return a complete sentence explaining and interpreting the value.
     * Examples: 40% of the total work has been accomplished. */
    public static final int FULL   = 3;



    public String costVariance(int style) {
        double d= costVariance();
        if (badDouble(d)) return null;
        return MessageFormat.format(CV_FORMATS[style], costArgs(d));
    }
    static final String[] CV_FORMATS = {
        "Cost Variance",
        "{0,number}",
        "{1} {0,choice,-1#over|0#under} budget",
        "The tasks completed to date have taken {1} {0,choice,-1#more|0#less} effort than planned." };


    public String scheduleVariance(int style) {
        double d= scheduleVariance();
        if (badDouble(d)) return null;
        return MessageFormat.format(SV_FORMATS[style], costArgs(d));
    }
    static final String[] SV_FORMATS = {
        "Schedule Variance",
        "{0,number}",
        "{1} {0,choice,-1#behind|0#ahead of} schedule",
        "You have completed {1} {0,choice,-1#less|0#more} work than you had planned to complete by this date." };


    public String costVariancePercentage(int style) {
        double d= costVariancePercentage();
        if (badDouble(d)) return null;
        return MessageFormat.format(CVP_FORMATS[style], absArgs(d));
    }
    static final String[] CVP_FORMATS = {
        "Cost Variance %",
        "{0,number,percent}",
        "{1,number,percent} {0,choice,-1#over|0#under} budget",
        "The tasks completed to date have taken {1,number,percent} {0,choice,-1#more|0#less} effort than planned." };

    public String scheduleVariancePercentage(int style) {
        double d= scheduleVariancePercentage();
        if (badDouble(d)) return null;
        return MessageFormat.format(SVP_FORMATS[style], absArgs(d));
    }
    static final String[] SVP_FORMATS = {
        "Schedule Variance %",
        "{0,number,percent}",
        "{1,number,percent} {0,choice,-1#behind|0#ahead of} schedule",
        "You have completed  {1,number,percent} {0,choice,-1#less|0#more} work than you had planned to complete by this date." };


    public String costPerformanceIndex(int style) {
        double d= costPerformanceIndex();
        if (badDouble(d)) return null;
        return MessageFormat.format(CPI_FORMATS[style], args(d));
    }
    static final String[] CPI_FORMATS = {
        "Cost Performance Index",
        "{0,number}",
        "{0,number}",
        "Work is taking {0,choice,0#more|1#less} time than planned.  For every effort hour actually spent, you are accomplishing {0,number} hours worth of planned results.  This usually means that your original effort estimate was too {0,choice,0#small|1#big}." };

    public String schedulePerformanceIndex(int style) {
        double d= schedulePerformanceIndex();
        if (badDouble(d)) return null;
        return MessageFormat.format(SPI_FORMATS[style], args(d));
    }
    static final String[] SPI_FORMATS = {
        "Schedule Performance Index",
        "{0,number}",
        "{0,number}",
        "You are completing tasks more {0,choice,0#slowly|1#quickly} than scheduled.  For every effort hour of work you should have accomplished to date, you are completing {0,number} hours.  This may mean that your original planned schedule was too {0,choice,0#aggressive|1#laid-back}." };


    public String percentComplete(int style) {
        double d= percentComplete();
        if (badDouble(d)) return null;
        return MessageFormat.format(PC_FORMATS[style], args(d));
    }
    static final String[] PC_FORMATS = {
        "Percent Complete",
        "{0,number,percent}",
        "{0,number,percent}",
        "You have completed {0,number,percent} of the total work." };


    public String percentSpent(int style) {
        double d= percentSpent();
        if (badDouble(d)) return null;
        return MessageFormat.format(PS_FORMATS[style], args(d));
    }
    static final String[] PS_FORMATS = {
        "Percent Spent",
        "{0,number,percent}",
        "{0,number,percent}",
        "You have spent {0,number,percent} of the total planned effort hours." };


    public String toCompletePerformanceIndex(int style) {
        double d= toCompletePerformanceIndex();
        if (badDouble(d)) return null;
        return MessageFormat.format(TCPI_FORMATS[style], args(d));
    }
    static final String[] TCPI_FORMATS = {
        "To Complete Performance Index",
        "{0,number}",
        "{0,number}",
        "In order to complete all the work within the original effort estimate, each future hour of effort must accomplish {0,number} hours worth of planned results." };


    public String improvementRatio(int style) {
        double d= improvementRatio();
        if (badDouble(d)) return null;
        return MessageFormat.format(IR_FORMATS[style], absArgs(d));
    }
    static final String[] IR_FORMATS = {
        "Improvement Ratio",
        "{0,number}",
        "{0,number}",
        "In order to complete all the work within the original effort estimate, you {0,choice,-1#can|0#must} be {1,number,percent} {0,choice,-1#less|0#more} productive in the future than you have been in the past." };


    public String independentForecastCost(int style) {
        double d= independentForecastCost();
        if (badDouble(d)) return null;
        return MessageFormat.format(IFC_FORMATS[style], costArgs(d));
    }
    static final String[] IFC_FORMATS = {
        "Forecast Cost",
        "{0}",
        "{1}",
        "Based upon your current CPI, the total work is forecast to require {1} of effort." };


    public String elapsed(int style) {
        double d= elapsed();
        if (badDouble(d)) return null;
        return MessageFormat.format(EL_FORMATS[style], durationArgs(d));
    }
    static final String[] EL_FORMATS = {
        "Elapsed Time",
        "{0}",
        "{1}",
        "The project began {1} ago." };


    public String scheduleVarianceDuration(int style) {
        double d= scheduleVarianceDuration();
        if (badDouble(d)) return null;
        return MessageFormat.format(SVD_FORMATS[style], durationArgs(d));
    }
    static final String[] SVD_FORMATS = {
        "Schedule Variance Duration",
        "{0,number}",
        "{1} {0,choice,-1#behind|0#ahead of} schedule",
        "Based upon your current rate of task completion to date, you are {1} {0,choice,-1#behind|0#ahead of} schedule." };


    public String independentForecastDuration(int style) {
        double d= independentForecastDuration();
        if (badDouble(d)) return null;
        return MessageFormat.format(IFD_FORMATS[style], durationArgs(d));
    }
    static final String[] IFD_FORMATS = {
        "Forecast Duration",
        "{0}",
        "{1}",
        "Based upon your current rate of task completion to date, the total work schedule is forecast to be {1}." };


    public String independentForecastDate(int style) {
        Date d =  independentForecastDate();
        if (d == null) return null;
        return MessageFormat.format(IFDT_FORMATS[style], args(d));
    }
    static final String[] IFDT_FORMATS = {
        "Forecast Completion Date",
        "{0,date}",
        "{0,date}",
        "Based upon your current rate of task completion to date, the total work is forecast to be completed {0,date}." };


    public static String formatDuration(double duration) {
        return formatDuration(duration, YEAR_MINUTES);
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
    public static Object[] durationArgs(double d) {
        return args(d/DAY_MINUTES, formatDuration(d));
    }
    public static Object[] costArgs(double d) {
        return args(d/HOUR_MINUTES, formatDuration(d, HOUR_MINUTES));
    }
    public static Object[] absArgs(double d) {
        Object[] result = { new Double(d), new Double(Math.abs(d)) };
        return result;
    }



    /*
     *TableModel Interface
     */

    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
        case 0: return "Metric";
        case 1: return "Value";
        case 2: return "Interpretation";
        case 3: default: return "Explanation";
        }
    }
    public int getColumnCount() { return 4; }

    public Object getValueAt(int row, int col) {
        switch (row) {
        case 0: return costVariance(col);
        case 1: return costVariancePercentage(col);
        case 2: return costPerformanceIndex(col);

        case 3: return scheduleVariance(col);
        case 4: return scheduleVariancePercentage(col);
        case 5: return scheduleVarianceDuration(col);
        case 6: return schedulePerformanceIndex(col);

        case 7: return percentComplete(col);
        case 8: return percentSpent(col);
        case 9: return toCompletePerformanceIndex(col);
        case 10: return improvementRatio(col);

        case 11: return independentForecastCost(col);
        case 12: return independentForecastDuration(col);
        case 13: default: return independentForecastDate(col);
        }
    }
    public int getRowCount() { return 14; }


    public Class getColumnClass(int columnIndex) { return String.class; }
    public boolean isCellEditable(int row, int col) { return false; }
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {}

    EventListenerList listenerList = new EventListenerList();
    public void addTableModelListener(TableModelListener l) {
        listenerList.add(TableModelListener.class, l);
    }
    public void removeTableModelListener(TableModelListener l) {
        listenerList.remove(TableModelListener.class, l);
    }
    public void fireTableChanged(TableModelEvent e) {
        Object [] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==TableModelListener.class) {
                if (e == null)
                    e = new TableModelEvent(this);
                ((TableModelListener)listeners[i+1]).tableChanged(e);
            }
        }
    }

    public void saveToXML(StringBuffer result) {
        result
            .append( " tpt='").append(totalPlanTime)
            .append("' evt='").append(earnedValueTime)
            .append("' at='").append(actualTime)
            .append("' pt='").append(planTime)
            .append("' start='").append(EVSchedule.saveDate(startDate))
            .append("' eff='").append(EVSchedule.saveDate(currentDate))
            .append("'");
    }
    public void loadFromXML(Element e) {
        totalPlanTime   = EVSchedule.getXMLNum(e, "tpt");
        earnedValueTime = EVSchedule.getXMLNum(e, "evt");
        actualTime      = EVSchedule.getXMLNum(e, "at");
        planTime        = EVSchedule.getXMLNum(e, "pt");
        startDate       = EVSchedule.getXMLDate(e, "start");
        currentDate     = EVSchedule.getXMLDate(e, "eff");
    }
}
