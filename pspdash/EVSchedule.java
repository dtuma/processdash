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

import java.util.*;
import java.text.DateFormat;
import javax.swing.table.*;
import javax.swing.event.*;

import com.jrefinery.chart.RangeInfo;
import com.jrefinery.chart.XYDataSource;
import com.jrefinery.chart.event.DataSourceChangeListener;
import com.jrefinery.chart.event.DataSourceChangeEvent;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pspdash.data.ListData;

public class EVSchedule implements TableModel {

    public static final Date NEVER = new Date(Long.MAX_VALUE);
    public static final Date A_LONG_TIME_AGO = new Date(0);

    public interface Listener {
        public void evScheduleChanged();
    }

    private static final boolean ADJUST = true;
    private static int ADJUSTMENT = 1000;  // one second
    private static final long HOUR_MILLIS =
        60L /*minutes*/ * 60L /*seconds*/ * 1000L /*milliseconds*/;
    private static final long DAY_MILLIS = 24L /*hours*/ * HOUR_MILLIS;
    public static final long WEEK_MILLIS = 7 * DAY_MILLIS;
    private static final long MIDNIGHT = DAY_MILLIS - ADJUSTMENT;

    public class Period implements Cloneable {
        Period previous;
        Date endDate;
        /** The total amount of time the user plans to spend in this period,
         * including level of effort tasks */
        double planTotalTime;
        /** The amount of time the user plans to spend in this period on
         * tasks that earn value */
        double planDirectTime;
        /** The amount of time the user plans to spend on tasks that earn
         * value during all periods up to and including this period */
        double cumPlanDirectTime;
        /** The amount of value the user plans to earn by the end of this
         * period */
        double cumPlanValue;

        /** The actual amount of time the user spent during this period on
         * tasks that earn value */
        double actualDirectTime;
        /** The actual amount of time the user spent during this period on
         * level of effort tasks */
        double actualIndirectTime;
        /** The actual amount of time the user spent on tasks that earn value
         * during all periods up to and including this period */
        double cumActualDirectTime;
        /** The actual value the user has earned by the end of this period */
        double cumEarnedValue;

        boolean automatic = false;

        public Period(Date endDate, double planTotalTime) {
            this.endDate = endDate;
            this.planTotalTime = planTotalTime;
            this.planDirectTime = planTotalTime * directPercentage;
            this.cumPlanDirectTime = this.planDirectTime;
            cumPlanValue = actualDirectTime = actualIndirectTime = cumActualDirectTime = cumEarnedValue = 0;
            previous = null;
        }

        public Date getBeginDate() {
            return previous == null ? A_LONG_TIME_AGO : previous.endDate; }
        public Date getEndDate() {
            if (ADJUST) return adjustDate(endDate, -ADJUSTMENT);
            else        return endDate;
        }

        private Date adjustDate(Date d, long adjustment) {
            return new Date(d.getTime() + adjustment);
        }

        public String getPlanTime() { return formatTime(planTotalTime); }
        public String getPlanDirectTime() { return formatTime(planDirectTime); }
        public String getCumPlanTime() { return formatTime(cumPlanDirectTime); }
        public String getCumPlanValue(double totalPlanTime) {
            return formatPercent(cumPlanValue/totalPlanTime); }
        public String getActualTime() {
            return formatTime(actualDirectTime + actualIndirectTime);
        }
        public String getActualDirectTime() {
            return formatTime(actualDirectTime);
        }
        public String getActualIndirectPercentage() {
            double totalTime = actualDirectTime + actualIndirectTime;
            if (totalTime == 0) return "";
            return formatPercent(actualIndirectTime / totalTime);
        }
        public String getCumActualTime() {
            return formatTime(cumActualDirectTime);
        }
        public String getCumEarnedValue(double totalPlanTime) {
            return formatPercent(cumEarnedValue/totalPlanTime);
        }

        public void setBeginDate(Object value) {
            if (previous != null && !previous.endDate.equals(value)) {
                automatic = false;
                previous.setPeriodEnd(value);
            }
        }
        public void setEndDate(Object value) {
            if (ADJUST && value instanceof Date) {
                long timeOfDay = millisecondsInDay((Date) value);
                if (timeOfDay == 0)
                    // the user entered a plain, unadorned date. the time
                    // period really ends on midnight at the end of that day.
                    value = adjustDate((Date) value, DAY_MILLIS);

                else if ((timeOfDay + ADJUSTMENT) % HOUR_MILLIS == 0)
                    // the date entered was ADJUSTMENT short of an hour
                    // boundary. Bump it back up.
                    value = adjustDate((Date) value, ADJUSTMENT);

                setPeriodEnd(value);

            } else
                setPeriodEnd(value);
        }
        private long millisecondsInDay(Date d) {
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            long hours = c.get(c.HOUR_OF_DAY);
            long minutes = hours * 60L + c.get(c.MINUTE);
            long seconds = minutes * 60L + c.get(c.SECOND);
            long ms = seconds * 1000L + c.get(c.MILLISECOND);
            return ms;
        }
        protected void setPeriodEnd(Object value) {
            synchronized (EVSchedule.this) {
                int pos = periods.indexOf(this);
                if (pos < 0) return;

                if (value == null) {
                    // if value is null, then they want to merge this
                    // entry with the following entry.
                    Period next = get(pos+1);
                    if (next != null) {
                        if (pos == 0)
                            remove(1);
                        else {
                            endDate              = next.endDate;
                            planTotalTime       += next.planTotalTime;
                            planDirectTime      += next.planDirectTime;
                            cumPlanDirectTime    = next.cumPlanDirectTime;
                            cumPlanValue         = next.cumPlanValue;
                            actualDirectTime    += next.actualDirectTime;
                            actualIndirectTime  += next.actualIndirectTime;
                            cumActualDirectTime  = next.cumActualDirectTime;
                            cumEarnedValue       = next.cumEarnedValue;
                            remove(pos+1);
                        }
                        clearAutomaticFlag();

                    } else {
                        // If there is no following entry, then they want
                        // to delete this entry.
                        remove(pos);
                    }

                } else if (value instanceof Date) {
                    // if no change has been made, exit.
                    if (endDate.equals(value)) return;

                    // if they are editing the start date of the schedule,
                    // respond simply by sliding the schedule datewise.
                    if (previous == null) {
                        long oldStart = endDate.getTime();
                        long newStart = ((Date) value).getTime();
                        slideScheduleDates(newStart - oldStart);
                        fireNeedsRecalc();
                        return;
                    }

                    // delete any preceeding periods which begin AFTER the
                    // new end date of this period.
                    while (previous != null &&
                           periods.size() > 2 &&
                           getBeginDate().compareTo(value) >= 0)
                        remove(--pos);

                    // delete any following periods which end BEFORE the
                    // new end date of this period.
                    Period next;
                    while ((next = get(pos+1)) != null &&
                           periods.size() > 2 &&
                           next.endDate.compareTo(value) <= 0)
                        remove(pos+1);

                    // we still will not save the change UNLESS this
                    // date is legal - meaning that dates appear in
                    // the list in strictly ascending order.  Thus,
                    // the new end date is legal IF:
                    //   * there is no previous period, OR it ends
                    //     BEFORE this new end date, AND
                    //   * there is no following period, OR it ends
                    //     AFTER this new end date.
                    if ((previous == null ||
                         getBeginDate().compareTo(value) < 0) &&
                        ((next = get(pos+1)) == null ||
                         next.endDate.compareTo(value) > 0))
                        endDate = (Date) value;

                    clearAutomaticFlag();
                }
                fireNeedsRecalc();
            }
        }

        private void setPlanTime(double planTime) {
            if (planTime != -1 && planTime != this.planTotalTime) {
                this.planTotalTime = planTime;
                this.planDirectTime = planTime * directPercentage;
                clearAutomaticFlag();
                recalcCumPlanTimes();
                fireNeedsRecalc();
            }
        }

        public void setPlanTime(Object value) {
            if (value instanceof String) {
                // parse the value to obtain a number of minutes
                setPlanTime(TimeLogEditor.parseTime((String) value));
            }
        }

        public void setPlanDirectTime(Object value) {
            if (value instanceof String) {
                // parse the value to obtain a number of minutes
                long planTime = TimeLogEditor.parseTime((String) value);
                if (planTime != -1 && directPercentage > 0)
                    setPlanTime(planTime / directPercentage);
            }

        }
        void clearAutomaticFlag() {
            synchronized (EVSchedule.this) {
                automatic = false;
                if (previous != null) previous.clearAutomaticFlag();
            }
        }

        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException ncse) { return null; }
        }

        public void saveToXML(StringBuffer result) {
            result.append("<period end='").append(saveDate(endDate))
                .append("' pt='").append(planTotalTime)
                .append("' pdt='").append(planDirectTime)
                .append("' at='").append(actualDirectTime)
                .append("' ait='").append(actualIndirectTime)
                .append("' cpv='").append(cumPlanValue)
                .append("' cev='").append(cumEarnedValue);
            if (automatic) result.append("' auto='true");
            result.append("'/>");
        }
        public Period(Element e) {
            endDate = getXMLDate(e, "end");
            if (XMLUtils.hasValue(e.getAttribute("pdt"))) {
                planTotalTime = getXMLNum(e, "pt");
                planDirectTime = getXMLNum(e, "pdt");
            } else {
                planTotalTime = planDirectTime = getXMLNum(e, "pt");
            }
            actualDirectTime = getXMLNum(e, "at");
            actualIndirectTime = getXMLNum(e, "ait");
            cumPlanValue = getXMLNum(e, "cpv");
            cumEarnedValue = getXMLNum(e, "cev");
            automatic = "true".equals(e.getAttribute("auto"));
        }

        /** Warning - these fields are typically unused, so they
         * rarely contain any real data. Don't expect them to contain
         * anything useful unless you put it there. */
        double planValue = 0, earnedValue = 0;
        public String getPlanValue(double totalPlanTime) {
            return formatPercent(planValue/totalPlanTime); }
        public String getEarnedValue(double totalPlanTime) {
            return formatPercent(earnedValue/totalPlanTime); }
    }

    Vector periods = new Vector();
    EVMetrics metrics = new EVMetrics();

    public EVSchedule() { this(20.0); }
    public EVSchedule(double hours) {
        Calendar c = Calendar.getInstance();
        c.set(c.DAY_OF_WEEK, 1);

        Date begin = truncDate(c.getTime());
        long beginTime = begin.getTime();
        long endTime  = beginTime + WEEK_MILLIS;
        endTime += dstDifference(beginTime, endTime);
        Date end = new Date(endTime);

        add(new Period(begin, 0.0));
        add(new Period(end, hours * 60));
    }
    private static Date truncDate(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(c.HOUR_OF_DAY, 0); c.set(c.MINUTE, 0);
        c.set(c.SECOND, 0); c.set(c.MILLISECOND, 0);
        return c.getTime();
    }


    public EVSchedule(Date start, Date end, double minutes) {
        add(new Period(start, 0.0));
        add(new Period(end, minutes));
    }

    public EVSchedule(ListData saveList) {
        if (saveList == null || saveList.size() < 3 ||
            (saveList.size() & 1) != 1)
            throw new IllegalArgumentException();

        Date d = parseDate((String) saveList.get(0));
        add(new Period(d, 0.0));
        double time;
        for (int i = 1;   i < saveList.size();   i += 2) {
            time = Double.parseDouble((String) saveList.get(i));
            d = parseDate((String) saveList.get(i+1));
            add(new Period(d, time));
        }
        recalcCumPlanTimes();
        // double-check to ensure that dates are increasing?
    }

    public EVSchedule(EVSchedule s) {
        Iterator i = s.periods.iterator();
        Period p, prev = null;
        while (i.hasNext()) {
            p = (Period) ((Period) i.next()).clone();
            p.previous = prev; prev = p;
            periods.add(p);
        }
        directPercentage = s.directPercentage;
    }

    public EVSchedule(Element e) {
        metrics.loadFromXML(e);
        NodeList periodNodes = e.getChildNodes();
        int len = periodNodes.getLength();
        for (int i=0;   i < len;   i++) {
            Node n = periodNodes.item(i);
            if (n instanceof Element &&
                "period".equals(((Element) n).getTagName()))
                add(new Period((Element) n));
        }
        recalcCumPlanTimes();
        recalcCumActualTimes();
        setEffectiveDate(getXMLDate(e, "eff"));
        metrics.recalcComplete(this);
    }

    protected synchronized void add(Period p) {
        p.previous = getLast();
        periods.add(p);
    }

    protected synchronized void remove(int pos) {
        // refuse to delete the singular remaining period.
        if (periods.size() < 3) return;

        // remove the period in position "pos", as requested.
        periods.remove(pos);

        // repair the "previous" field of the period *now* in position pos.
        Period p = get(pos);
        if (p != null)
            p.previous = (pos == 0 ? null : get(pos-1));

        // recalc the cumulative plan times, since they are now messed up.
        recalcCumPlanTimes();
    }

    protected synchronized void slideScheduleDates(long delta) {
        Iterator i = periods.iterator();
        Period p;
        long currentEndDate, newEndDate;
        while (i.hasNext()) {
            p = (Period) i.next();
            currentEndDate = p.endDate.getTime();
            newEndDate = currentEndDate + delta;
            newEndDate += dstDifference(currentEndDate, newEndDate);
            p.endDate = new Date(newEndDate);
        }
    }


    public Period get(int pos) {
        try {
            return (Period) periods.get(pos);
        } catch (Exception e) {
            return null;
        }
    }

    public Period getLast() {
        try {
            return (Period) periods.lastElement();
        } catch (NoSuchElementException nsee) {
            return null;
        }
    }

    public synchronized ListData getSaveList() {
        ListData result = new ListData();
        result.add(saveDate(get(0).endDate));
        Period p;
        for (int i = 1;   i < periods.size();  i++) {
            p = get(i);
            if (p.automatic)
                break;
            result.add(Double.toString(p.planTotalTime));
            result.add(saveDate(p.endDate));
        }
        result.setImmutable();
        return result;
    }

    static String saveDate(Date d) { return "@" + d.getTime(); }
    static Date parseDate(String d) {
        if (!d.startsWith("@")) throw new IllegalArgumentException();
        return new Date(Long.parseLong(d.substring(1)));
    }
    static double getXMLNum(Element e, String attrName) {
        try {
            return Double.parseDouble(e.getAttribute(attrName));
        } catch (Exception exc) { return 0; }
    }
    static Date getXMLDate(Element e, String attrName) {
        String s = e.getAttribute(attrName);
        if (s == null || s.length() == 0) return null;
        try {
            return EVSchedule.parseDate(s);
        } catch (Exception exc) { return null; }
    }

    public synchronized void saveToXML(StringBuffer result) {
        result.append("<schedule");
        metrics.saveToXML(result);
        result.append(">");
        Iterator i = periods.iterator();
        while (i.hasNext())
            ((Period) i.next()).saveToXML(result);
        result.append("</schedule>");
    }

    protected synchronized Period get(Date when) {
        long time = when.getTime();
        Period p;
        for (int i = periods.size();  i-- > 0; ) {
            p = get(i);
            if (p != null && p.getBeginDate().getTime() < time)
                return p;
        }
        return null;
    }

    public synchronized Date getPeriodStart(Date when) {
        Period p = get(when);
        return (p == null ? null : p.getBeginDate());
    }

    public synchronized Date getPeriodEnd(Date when) {
        Period p = get(when);
        return (p == null ? null : p.endDate);
    }

    /** return the total amount of time in the plan for periods ending
     * before the given date. */
    public double getScheduledPlanTime(Date when) {
        double result = 0;
        long time = when.getTime();
        Period p;
        for (int i = 1;   i < periods.size();   i++) {
            p = get(i);
            if (p != null && p.getEndDate().getTime() < time)
                result += p.planDirectTime;
            else break;
        }
        return result;
    }

    /** return the total amount of actual time for periods ending
     * before the given date. */
    public double getScheduledActualTime(Date when) {
        double result = 0;
        long time = when.getTime();
        Period p;
        for (int i = 1;   i < periods.size();   i++) {
            p = get(i);
            if (p != null && p.getEndDate().getTime() < time)
                result += p.actualDirectTime;
            else break;
        }
        return result;
    }

    /** Return the date that the schedule would reach the given cumulative
     * plan time. Perform a "what-if" calculation - don't modify the
     * current schedule.
     */
    public Date getHypotheticalDate(double cumPlanTime) {
        EVSchedule s = new EVSchedule(this);
        s.cleanUp();
        return s.getPlannedCompletionDate(cumPlanTime, cumPlanTime);
    }

    public synchronized Date getPlannedCompletionDate(double cumPlanTime,
                                                      double cumPlanValue) {
        //System.out.println("getPlannedCompletionDate("+cumPlanTime+","+cumPlanValue+")");

        if (Double.isNaN(cumPlanTime) || Double.isInfinite(cumPlanTime))
            return NEVER;

        // scan the list for the first period containing at least
        // cumPlanTime minutes.  When we find it, save the cum Plan
        // value into the appropriate blocks in the schedule.
        Period p;
        Date result = null;
        for (int i = 0;  i < periods.size();  i++) {
            p = get(i);
            if (p.cumPlanDirectTime >= cumPlanTime) {
                p.cumPlanValue = Math.max(p.cumPlanValue, cumPlanValue);
                if (result == null) result = p.getEndDate();
            }
        }
        if (result != null) return result;

        // There isn't enough time in the schedule - we'll attempt
        // to expand the schedule so it contains enough hours.

        if (defaultPlanDirectTime <= 0.0) // if we can't add hours to the schedule,
            return NEVER;           // the task will never get done.

        boolean firstTimeThrough = true;
        while (true) {
            //FIXME: make this more robust, so it can withstand out of order
            // queries against the schedule.
            if (!firstTimeThrough && !grow(true)) return NEVER;
            if (!addHours(cumPlanTime) && !firstTimeThrough) return NEVER;

            p = getLast();          // get the last period in the list.
            if (p.cumPlanDirectTime >= cumPlanTime) {
                p.cumPlanValue = Math.max(p.cumPlanValue, cumPlanValue);
                return p.getEndDate();
            }
            firstTimeThrough = false;
        }
    }

    public synchronized void saveCompletedTask(Date dateCompleted,
                                               double planValue) {
        saveActualTaskInfo(dateCompleted, planValue, 0, true);
    }
    public synchronized void saveActualTime(Date when, double actualTime) {
        saveActualTaskInfo(when, 0, actualTime, true);
    }
    public synchronized void saveActualIndirectTime(Date when, double actualTime) {
        saveActualTaskInfo(when, 0, actualTime, false);
    }
    public synchronized void saveActualTaskInfo(Date when,
                                                double planValue,
                                                double actualTime,
                                                boolean direct) {
        //System.out.println("saveActualTaskInfo("+when+","+planValue+")");
        if (when == null || when == NEVER) return;

        boolean foundDate = false;
        Period p;
        for (int i = periods.size();  i-- > 0; ) {
            p = get(i);
            // if this period ends *after* the task's completion date,
            // add the task's planValue to this period's cumPlanValue.
            if (when.compareTo(p.endDate) < 0) {
                foundDate = true;
                if (direct) {
                    p.cumEarnedValue += planValue;
                    p.cumActualDirectTime += actualTime;
                }
                if (when.compareTo(p.getBeginDate()) >= 0) {
                    if (direct)
                        p.actualDirectTime += actualTime;
                    else
                        p.actualIndirectTime += actualTime;
                }
                //System.out.println("\tadding to period ending "+p.endDate);
            } else
                break;
        }
        if (foundDate) return;

        // this task info falls AFTER the end date of the entire schedule.
        // expand the schedule until it contains the completion date.
        while (true) {
            if (!grow(true)) return;
            p = getLast();      // get the last period in the list.
            if (when.compareTo(p.endDate) < 0) {
                if (direct) {
                    p.cumEarnedValue += planValue;
                    p.cumActualDirectTime  += actualTime;
                }
                if (when.compareTo(p.getBeginDate()) >= 0) {
                    if (direct)
                        p.actualDirectTime += actualTime;
                    else
                        p.actualIndirectTime += actualTime;
                }
                //System.out.println("\tadding to period ending "+p.endDate);
                return;
            }
        }
    }

    private double directPercentage = 1;
    private boolean showDirectColumns = false;
    private String[] toolTips = buildColumnTooltips();
    public void setLevelOfEffort(double percent) {
        directPercentage = 1.0 - percent;
        if (directPercentage < 0)
            directPercentage = 0;
        showDirectColumns = (directPercentage < 1);
        if (showDirectColumns) {
            toolTips[PLAN_TIME_COLUMN] = TOOL_TIPS[PLAN_TIME_COLUMN];
            toolTips[TIME_COLUMN] = TOOL_TIPS[TIME_COLUMN];
        } else {
            toolTips[PLAN_TIME_COLUMN] = TOOL_TIPS[PLAN_DTIME_COLUMN];
            toolTips[TIME_COLUMN] = TOOL_TIPS[DTIME_COLUMN];
        }
    }

    private Date effectiveDate = null;
    private int effectivePeriod = 0;
    public Date getEffectiveDate() { return effectiveDate; }
    public synchronized void setEffectiveDate(Date d) {
        effectiveDate = d;
        effectivePeriod = 0;
        if (effectiveDate == null) return;
        long time = d.getTime();
        Period p;
        for (int i = periods.size();  i-- > 0; ) {
            p = get(i);
            if (p != null && p.endDate.getTime() < time) {
                effectivePeriod = i+1;
                return;
            }
        }
    }
    public Date getStartDate() { return get(0).endDate; }
    public EVMetrics getMetrics() { return metrics; }

    /** look at the final row in the schedule, and maybe bump it up.
     *
     *  The final row will <b>not</b> be bumped up if:<ul>
     *  <li>it is a manual row.
     *  <li>it already contains at defaultPlanTime minutes or more.
     *  </ul>
     *  In these situations, no changes will be made, and this method will
     *  return false.
     *
     *  Otherwise, the final row will be bumped up an amount equal to the
     *  smaller of:<ul>
     *  <li>the difference between its plan time and defaultPlanTime
     *  <li>the difference between its cum plan time and the required
     *      cum plan time.
     *  </ul>
     */
    protected synchronized boolean addHours(double requiredCumPlanTime) {
        Period z = getLast();

        if (!z.automatic) return false;

        double diff = defaultPlanDirectTime - z.planDirectTime;
        if (diff <= 0.0) return false;

        double cumDiff = requiredCumPlanTime - z.cumPlanDirectTime;
        if (cumDiff <= 0) return false;

        if (diff < cumDiff) {
            z.planTotalTime = defaultPlanTotalTime;
            z.planDirectTime = defaultPlanDirectTime;
            z.cumPlanDirectTime = z.previous.cumPlanDirectTime + defaultPlanDirectTime;
        } else {
            z.cumPlanDirectTime = requiredCumPlanTime;
            z.planDirectTime = requiredCumPlanTime - z.previous.cumPlanDirectTime;
            z.planTotalTime = z.planDirectTime *
                (defaultPlanTotalTime / defaultPlanDirectTime);
        }
        return true;
    }

    /** Add a new period (containing 0 planned hours) to the end of
     *  the schedule.
     */
    protected synchronized boolean grow(boolean automatic) {
        int size = periods.size();
        if (size < 2 || size > 300) return false;
        Period x = get(size-2), y = get(size-1), z;

        long xdate = x.endDate.getTime(), ydate = y.endDate.getTime();
        long delta = ydate - xdate - dstDifference(xdate, ydate);
        long zdateTime = ydate + delta;
        zdateTime += dstDifference(ydate, zdateTime);
        Date zdate = new Date(zdateTime);
        z = new Period(zdate, 0.0);
        z.cumPlanDirectTime = y.cumPlanDirectTime;
        z.cumPlanValue = y.cumPlanValue;
        z.cumEarnedValue = y.cumEarnedValue;
        z.automatic = automatic;

        add(z);
        //System.out.println("growing schedule - new task ends: " + zdate);
        return true;
    }

    /** examines two different dates to see if one is in daylight
     * savings time while the other is not.  If they are both in DST
     * or neither is in DST, returns zero.
     *
     * if the first date is in standard time and the second is in
     * daylight savings time, returns a negative number indicating the
     * number of milliseconds that were lost.
     *
     * if the first date is in daylight savings time and the second is in
     * standard time, returns a positive number indicating the
     * number of additional milliseconds that were gained.
     */
    private static long dstDifference(long first, long second) {
        Calendar calendar = Calendar.getInstance();

        long firstDSTOffset, secondDSTOffset;
        calendar.setTime(new Date(first));
        firstDSTOffset = calendar.get(calendar.DST_OFFSET);
        calendar.setTime(new Date(second));
        secondDSTOffset = calendar.get(calendar.DST_OFFSET);

        return firstDSTOffset - secondDSTOffset;
    }

    public synchronized void deleteRow(int row) {
        Period r = get(row+1);
        if (r == null || r.automatic) return;

        remove(row+1);
        // send a "row deleted" event.
        fireTableChanged(new TableModelEvent
            (this, row, row, TableModelEvent.ALL_COLUMNS,
             TableModelEvent.DELETE));

        // signal the need to recalculate all the schedule data.
        fireNeedsRecalc();
    }

    public synchronized void insertRow(int row) {
        Period r = get(row+1);
        if (r == null) return;
        Date midpoint = new Date((r.getEndDate().getTime() +
                                  r.getBeginDate().getTime()) / 2);
        Period newPeriod = new Period(midpoint, 0.0);

        newPeriod.previous = r.previous;
        r.previous = newPeriod;
        periods.add(row+1, newPeriod);
        r.clearAutomaticFlag();

        // fire a "rows added" event.
        fireTableChanged(new TableModelEvent
            (this, row, row, TableModelEvent.ALL_COLUMNS,
             TableModelEvent.INSERT));

        // signal the need to recalculate all the schedule data.
        fireNeedsRecalc();
    }

    public synchronized void addRow() {
        prepForEvents();
        grow(false);
        getLast().clearAutomaticFlag();
        firePreparedEvents();

        // signal the need to recalculate all the schedule data.
        fireNeedsRecalc();
    }

    /** The amount of planTime in the <b>last</b> manual schedule period.
     *  This is the maximum amount of time that will be alloted to any
     *  automatic schedule period.
     */
    double defaultPlanTotalTime;

    /** The amount of planTime in the <b>last</b> manual schedule period.
     *  This is the maximum amount of time that will be alloted to any
     *  automatic schedule period.
     */
    double defaultPlanDirectTime;

    public synchronized void cleanUp() {
        prepForEvents();
        Period p = null;
        int i;
        for (i = 0;  i < periods.size();  i++) {
            p = get(i);
            p.planDirectTime = directPercentage * p.planTotalTime;
            p.cumPlanValue = p.cumEarnedValue = 0;
            p.actualDirectTime = p.actualIndirectTime = p.cumActualDirectTime = 0;
            if (p.automatic)
                break;
        }
        // truncate all the automatic periods from the list - but never
        // make the list contain fewer than two entries (one period).
        if (i < 2) {
            i = 2;
            if (p != null) p.automatic = false;
        }
        periods.setSize(i);
        defaultPlanTotalTime = get(i-1).planTotalTime;
        defaultPlanDirectTime = get(i-1).planDirectTime;
    }

    public synchronized void recalcCumPlanTimes() {
        double cumPlanDirectTime = 0;
        Period p;
        for (int i = 1;   i < periods.size();   i++) {
            p = get(i);
            cumPlanDirectTime += p.planDirectTime;
            p.cumPlanDirectTime = cumPlanDirectTime;
        }
    }

    public synchronized void recalcCumActualTimes() {
        double cumActualDirectTime = 0;
        Period p;
        for (int i = 0;   i < periods.size();   i++) {
            p = get(i);
            cumActualDirectTime += p.actualDirectTime;
            p.cumActualDirectTime = cumActualDirectTime;
        }
    }


    /** the Period.planValue and Period.earnedValue fields generally do not
     * contain any data. This routine calculates the values of these fields
     * for all the periods in the given schedule */
    protected synchronized void calcIndividualValues() {
        Iterator i = periods.iterator();
        Period p = (Period) i.next();
        p.planValue = p.cumPlanValue;
        p.earnedValue = p.cumEarnedValue;
        while (i.hasNext()) {
            p = (Period) i.next();
            p.planValue   = p.cumPlanValue   - p.previous.cumPlanValue;
            p.earnedValue = p.cumEarnedValue - p.previous.cumEarnedValue;
        }
    }


    int prevNumRows = -1;
    public void prepForEvents() {
        if (prevNumRows == -1) prevNumRows = getRowCount();
    }
    synchronized void firePreparedEvents() {
        if (prevNumRows == -1) {
            fireTableChanged(null);
            return;
        }

        int currNumRows = getRowCount();
        int changedRows = (currNumRows < prevNumRows ? currNumRows
                                                     : prevNumRows);

        // fire an event to redraw rows that previously might have been
        // automatic.
        fireTableChanged(new TableModelEvent
            (this, 0, changedRows-1, TableModelEvent.ALL_COLUMNS,
             TableModelEvent.UPDATE));

        if (prevNumRows < currNumRows)
            // fire a "rows added" event.
            fireTableChanged(new TableModelEvent
                (this, prevNumRows, currNumRows-1, TableModelEvent.ALL_COLUMNS,
                 TableModelEvent.INSERT));
        else if (prevNumRows > currNumRows)
            // fire a "rows deleted" event.
            fireTableChanged(new TableModelEvent
                (this, currNumRows, prevNumRows-1, TableModelEvent.ALL_COLUMNS,
                 TableModelEvent.DELETE));
        prevNumRows = -1;
    }

    public static String formatTime(double time) {
        return EVTask.formatTime(time); }
    public static String formatPercent(double p) {
        return EVTask.formatPercent(p); }
    public static String formatDate(Date d) {
        if (d == null)
            return "";
        else if (d == NEVER)
            return "never";
        else
            return dateFormatter.format(d);
    }
    private static DateFormat dateFormatter =
        DateFormat.getDateInstance(DateFormat.MEDIUM);


    ///
    /// Table model
    ///

    protected static final int DATE_W = 80; // width for date columns
    protected static final int TIME_W = 50; // width for time columns
    protected static final int PCT_W  = 40; // width for percentage columns
    public static final String[] colNames = {
        "From", "To",   "PT",   "PDT",  "CPT",  "CPV", "Time", "%I",  "DTime", "CT",   "EV" };
    public static final int[] colWidths = {
         DATE_W, DATE_W, TIME_W, TIME_W, TIME_W, PCT_W, TIME_W, PCT_W, TIME_W,  TIME_W, PCT_W };
    public static final String[] TOOL_TIPS = {
        null,
        null,
        "Planned Time (hours:minutes)",
        "Planned Direct Time (hours:minutes)",
        "Cumulative Planned Direct Time (hours:minutes)",
        "Cumulative Planned Value",
        "Actual Time (hours:minutes)",
        "Actual Percent Indirect Time",
        "Actual Direct Time (hours:minutes)",
        "Cumulative Actual Direct Time (hours:minutes)",
        "Actual Cumulative Earned Value" };

    public static final int FROM_COLUMN           = 0;
    public static final int TO_COLUMN             = 1;
    public static final int PLAN_TIME_COLUMN      = 2;
    public static final int PLAN_DTIME_COLUMN     = 3;
    public static final int PLAN_CUM_TIME_COLUMN  = 4;
    public static final int PLAN_CUM_VALUE_COLUMN = 5;
    public static final int TIME_COLUMN           = 6;
    public static final int IPERCENT_COLUMN       = 7;
    public static final int DTIME_COLUMN          = 8;
    public static final int CUM_TIME_COLUMN       = 9;
    public static final int CUM_VALUE_COLUMN      = 10;


    public static final int[] DIRECT_COLUMN_LIST = {
        PLAN_DTIME_COLUMN, IPERCENT_COLUMN, DTIME_COLUMN };

    public static Class[] colTypes = {
        Date.class,             // From
        Date.class,             // To
        String.class,           // planned time
        String.class,           // planned direct time
        String.class,           // planned cumulative time
        String.class,           // planned cumulative value
        String.class,           // actual time
        String.class,           // percent indirect time
        String.class,           // actual direct time
        String.class,           // actual cumulative time
        String.class };         // cumulative earned value


    public int getRowCount() { return periods.size() - 1; }
    public int getColumnCount() { return colNames.length; }
    public String getColumnName(int i) {
        if (!showDirectColumns &&
            (i == PLAN_DTIME_COLUMN || i == IPERCENT_COLUMN || i == DTIME_COLUMN))
            return " " + colNames[i] + " ";
        else
            return colNames[i];
    }
    public String[] getColumnTooltips() {
        return toolTips;
    }
    private String[] buildColumnTooltips() {
        String [] result = new String[TOOL_TIPS.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = TOOL_TIPS[i];
        }
        if (directPercentage == 1.0) {
            result[PLAN_TIME_COLUMN] = result[PLAN_DTIME_COLUMN];
            result[TIME_COLUMN] = result[DTIME_COLUMN];
        }
        toolTips = result;
        return result;
    }

    public Class getColumnClass(int i) { return colTypes[i]; }
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex < 4;
    }
    public Object getValueAt(int rowIndex, int columnIndex) {
        Period p = get(rowIndex+1);
        if (p == null) return null;
        switch(columnIndex) {
        case FROM_COLUMN:           return p.getBeginDate();
        case TO_COLUMN:             return p.getEndDate();
        case PLAN_TIME_COLUMN:      return p.getPlanTime();
        case PLAN_DTIME_COLUMN:     return p.getPlanDirectTime();
        case PLAN_CUM_TIME_COLUMN:  return p.getCumPlanTime();
        case PLAN_CUM_VALUE_COLUMN: return p.getCumPlanValue(totalPlan());
        case TIME_COLUMN:           return p.getActualTime();
        case IPERCENT_COLUMN:       return p.getActualIndirectPercentage();
        case DTIME_COLUMN:          return p.getActualDirectTime();
        case CUM_TIME_COLUMN:       return p.getCumActualTime();
        case CUM_VALUE_COLUMN:      return p.getCumEarnedValue(totalPlan());
        }
        return null;
    }
    private double totalPlan() { return metrics.totalPlan(); }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        Period p = get(rowIndex+1);
        if (p == null) return;
        switch(columnIndex) {
        case FROM_COLUMN:       p.setBeginDate(aValue);       break;
        case TO_COLUMN:         p.setEndDate(aValue);         break;
        case PLAN_TIME_COLUMN:  p.setPlanTime(aValue);        break;
        case PLAN_DTIME_COLUMN: p.setPlanDirectTime(aValue);  break;
        }
    }
    public boolean rowIsAutomatic(int row) {
        Period p = get(row+1);
        return (p != null && p.automatic);
    }

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

    Listener listener = null;
    public void setListener(Listener l) { listener = l; }
    public void fireNeedsRecalc() {
        if (listener != null)
            listener.evScheduleChanged();
    }


    ///////////////////////////////////////////////////////////////////////
    // The methods/classes below assist in the generation of JFreeCharts
    // based on EVSchedules.
    //////////////////////////////////////////////////////////////////////

    private static final Double ZERO = new Double(0.0);
    private static final Double ONE_HUNDRED = new Double(100.0);

    private interface ChartSeries {
        /** Returns the name of the specified series (zero-based). */
        String getSeriesName();
        /** Returns the number of items in the specified series */
        int getItemCount();
        /** Returns the x-value for the specified series and item */
        Number getXValue(int itemIndex);
        /** Returns the y-value for the specified series and item */
        Number getYValue(int itemIndex);
    }

    private abstract class PlanChartSeries implements ChartSeries {
        public String getSeriesName() { return "Plan"; }
        public int getItemCount() { return getRowCount()+1; }
        public Number getXValue(int itemIndex) {
            return new Long(get(itemIndex).endDate.getTime()); }
    }

    private abstract class ActualChartSeries implements ChartSeries {
        public String getSeriesName() { return "Actual"; }
        public int getItemCount() {
            int result = getRowCount()+1;
            if (effectivePeriod < result) result = effectivePeriod+1;
            return result;
        }
        public Number getXValue(int itemIndex) {
            Date d;
            if (itemIndex < effectivePeriod)
                d = get(itemIndex).endDate;
            else
                d = effectiveDate;
            return new Long(d.getTime());
        }
    }

    private class ForecastChartSeries implements ChartSeries {
        Number currentYVal, forecastYVal;
        Number currentXVal, forecastXVal;
        int itemCount = 2;
        public String getSeriesName() { return "Forecast"; }
        public int getItemCount() { return itemCount; }
        public Number getXValue(int itemIndex) {
            return (itemIndex == 0 ? currentXVal : forecastXVal);
        }
        public Number getYValue(int itemIndex) {
            return (itemIndex == 0 ? currentYVal : forecastYVal);
        }
        public void recalc() {
            itemCount = 2;
            currentXVal = dateToLong(effectiveDate);
            forecastXVal = dateToLong(metrics.independentForecastDate());
            if (itemCount == 2 &&
                (notLessThan(currentXVal, forecastXVal) ||
                 notLessThan(currentYVal, forecastYVal)))
                itemCount = 0;
        }
        private boolean notLessThan(Number a, Number b) {
            if (a == null || b == null) return true;
            if (a.doubleValue() < b.doubleValue()) return false;
            return true;
        }
        private Number dateToLong(Date d) {
            if (d != null) return new Long(d.getTime());
            itemCount = 0;
            return null;
        }
    }


    /** Base class for implementing XYDataSource funtionality.
     */
    private class ChartData implements XYDataSource, TableModelListener {
        ChartSeries [] series;
        boolean needsRecalc = true;
        protected void recalc() {}
        protected void maybeRecalc() {
            if (needsRecalc) { recalc(); needsRecalc = false; } }
        /** Returns the number of series in the data source. */
        public int getSeriesCount() { maybeRecalc(); return series.length; }
        /** Returns the name of the specified series (zero-based). */
        public String getSeriesName(int seriesIndex) { maybeRecalc();
            return series[seriesIndex].getSeriesName(); }
        /** Returns the number of items in the specified series */
        public int getItemCount(int seriesIndex) { maybeRecalc();
            return series[seriesIndex].getItemCount(); }
        /** Returns the x-value for the specified series and item */
        public Number getXValue(int seriesIndex, int itemIndex) {
            maybeRecalc(); return series[seriesIndex].getXValue(itemIndex); }
        /** Returns the y-value for the specified series and item */
        public Number getYValue(int seriesIndex, int itemIndex) {
            maybeRecalc();
            if (itemIndex == -1) return null;
            return series[seriesIndex].getYValue(itemIndex); }

        // support DataSourceChangeListener notification
        private ArrayList listenerList = null;
        public void addChangeListener(DataSourceChangeListener l) {
            if (listenerList == null) listenerList = new ArrayList();
            synchronized (listenerList) {
                if (listenerList.size() == 0) addTableModelListener(this);
                if (!listenerList.contains(l)) listenerList.add(l);
            }
        }
        public void removeChangeListener(DataSourceChangeListener l) {
            if (listenerList == null) return;
            synchronized (listenerList) {
                if (listenerList.remove(l) && listenerList.size() == 0)
                    removeTableModelListener(this);
            }
        }
        public void fireChangeEvent() {
            if (listenerList == null) return;
            DataSourceChangeEvent e = null;
            Object [] listeners = listenerList.toArray();
            // Process the listeners last to first, notifying
            // those that are interested in this event
            for (int i = listeners.length; i-- > 0; ) {
                if (e == null) e = new DataSourceChangeEvent(this);
                ((DataSourceChangeListener)listeners[i]).dataSourceChanged(e);
            }
        }

        // TableModelListener implementation
        public void tableChanged(TableModelEvent e) {
            needsRecalc = true;
            fireChangeEvent();
        }
    }

    private class PlanTimeSeries extends PlanChartSeries {
        public Number getYValue(int itemIndex) {
            return new Double(get(itemIndex).cumPlanDirectTime / 60.0); } }
    private class ActualTimeSeries extends ActualChartSeries {
        public Number getYValue(int itemIndex) {
            return new Double(get(itemIndex).cumActualDirectTime / 60.0); } }

    /** XYDataSource for charting plan vs actual direct hours.
     */
    private class TimeChartData extends ChartData {
        public TimeChartData() {
            series = new ChartSeries[3];
            series[0] = new PlanTimeSeries();
            series[1] = new ActualTimeSeries();
            series[2] = forecast = new ForecastChartSeries();
        }
        ForecastChartSeries forecast;
        public void recalc() {
            forecast.currentYVal = new Double(getLast().cumActualDirectTime / 60.0);
            forecast.forecastYVal = new Double
                (checkDouble(metrics.independentForecastCost() / 60.0));
            forecast.recalc();
            if (forecast.getItemCount() == 0) numSeries = 2;
        }
        private double checkDouble(double d) {
            numSeries = ((Double.isNaN(d) || Double.isInfinite(d)) ? 2 : 3);
            return d;
        }
        int numSeries = 3;
        public int getSeriesCount() { maybeRecalc(); return numSeries; }
    }
    public XYDataSource getTimeChartData() { return new TimeChartData(); }



    private class PlanValueSeries extends PlanChartSeries {
        double mult;
        PlanValueSeries(double m) { mult = m; }
        public Number getYValue(int itemIndex) {
            return new Double(get(itemIndex).cumPlanValue * mult); } }
    private class ActualValueSeries extends ActualChartSeries {
        double mult;
        ActualValueSeries(double m) { mult = m; }
        public Number getYValue(int itemIndex) {
            return new Double(get(itemIndex).cumEarnedValue * mult); } };


    /** XYDataSource for charting plan vs actual earned value.
     */
    private class ValueChartData extends ChartData implements RangeInfo {
        public ValueChartData() {
            double mult = 100.0 / totalPlan();
            series = new ChartSeries[3];
            series[0] = plan = new PlanValueSeries(mult);
            series[1] = actual = new ActualValueSeries(mult);
            series[2] = forecast = new ForecastChartSeries();
        }
        ForecastChartSeries forecast;
        PlanValueSeries plan;
        ActualValueSeries actual;
        public void recalc() {
            double mult = 100.0 / totalPlan();
            if (Double.isInfinite(mult)) mult = 0;
            plan.mult = actual.mult = mult;
            forecast.currentYVal = new Double(getLast().cumEarnedValue * mult);
            forecast.forecastYVal = ONE_HUNDRED;
            forecast.recalc();
            numSeries = (forecast.getItemCount() == 0 ? 2 : 3);
        }
        int numSeries = 3;
        public int getSeriesCount() { maybeRecalc(); return numSeries; }
        public Number getMinimumRangeValue() { return ZERO; }
        public Number getMaximumRangeValue() { return ONE_HUNDRED; }
    }
    public XYDataSource getValueChartData() {
        ValueChartData result = new ValueChartData();
        result.recalc();
        return result;
    }


    /** XYDataSource for charting cost and schedule on one chart.
     */
    private class CombinedChartData extends ChartData {
        public CombinedChartData() {
            series = new ChartSeries[3];
            series[0] = new PlanValueSeries(1.0 / 60.0) {
                    public String getSeriesName() { return "Plan Value"; } };
            series[1] = new ActualValueSeries(1.0 / 60.0) {
                    public String getSeriesName() { return "Actual Value"; } };
            series[2] = new ActualTimeSeries() {
                    public String getSeriesName() { return "Actual Time"; } };
        }
    }
    public XYDataSource getCombinedChartData() {
        return new CombinedChartData(); }
}
