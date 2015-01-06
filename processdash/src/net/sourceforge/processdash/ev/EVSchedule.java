// Copyright (C) 2001-2015 Tuma Solutions, LLC
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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.jfree.data.Range;
import org.jfree.data.RangeInfo;
import org.jfree.data.xy.XYDataset;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.ev.ui.chart.ChartEventAdapter;
import net.sourceforge.processdash.ev.ui.chart.ConfidenceIntervalCompletionDateChartData;
import net.sourceforge.processdash.ev.ui.chart.ConfidenceIntervalTotalCostChartData;
import net.sourceforge.processdash.ev.ui.chart.XYChartData;
import net.sourceforge.processdash.ev.ui.chart.XYChartSeries;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.DateAdjuster;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.RangeDateAdjuster;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.TimeZoneUtils;


public class EVSchedule implements TableModel {

    public static final Date NEVER = new Date(Long.MAX_VALUE);
    public static final Date A_LONG_TIME_AGO = new Date(0);
    static Resources resources = Resources.getDashBundle("EV");
    static final String END_TOKEN = resources.getString("Schedule.End_Token");

    public interface Listener {
        public void evScheduleChanged();
    }

    private static final boolean ADJUST = true;
    private static int ADJUSTMENT = 1000;  // one second
    private static final long HOUR_MILLIS =
        60L /*minutes*/ * 60L /*seconds*/ * 1000L /*milliseconds*/;
    private static final long DAY_MILLIS = 24L /*hours*/ * HOUR_MILLIS;
    public static final long WEEK_MILLIS = 7 * DAY_MILLIS;
//    private static final long MIDNIGHT = DAY_MILLIS - ADJUSTMENT;

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
        /** The actual cost of tasks completed in or before this period.
         * Also known as ACWP (actual cost of work performed) */
        double cumActualCost;

        /** Free-text notes that the user has associated with this period.
         * EVSchedule.periodNotes is the true source of such data, and a
         * calculation must be performed to find the notes associated with
         * a period.  This field only exists to cache the results of that
         * calculation.  This field will be null if the calculation still
         * needs to be performed. */
        String note;

        boolean automatic = false;

        public Period(Date endDate, double planTotalTime) {
            this.endDate = endDate;
            this.planTotalTime = planTotalTime;
            this.planDirectTime = planTotalTime * directPercentage;
            this.cumPlanDirectTime = this.planDirectTime;
            cumPlanValue = actualDirectTime = actualIndirectTime =
                cumActualDirectTime = cumEarnedValue = cumActualCost = 0;
            previous = null;
        }

        public Date getBeginDate() {
            return previous == null ? A_LONG_TIME_AGO : previous.endDate; }
        public Date getEndDate() {
            return getEndDate(ADJUST);
        }
        public Date getEndDate(boolean adjust) {
            if (adjust) return adjustDate(endDate, -ADJUSTMENT);
            else        return endDate;
        }

        private Date adjustDate(Date d, long adjustment) {
            return new Date(d.getTime() + adjustment);
        }
        public boolean isAutomatic() { return automatic; }

        public String getPlanTotalTimeText() {
            if (isScheduleEnd())
                return END_TOKEN;
            else
                return formatTime(planTotalTime);
        }

        public String getPlanDirectTimeText() {
            if (isScheduleEnd())
                return END_TOKEN;
            else
                return formatTime(planDirectTime);
        }
        public double getPlanDirectTime() {
            return planDirectTime;
        }
        // note: this method must remain for legacy binary compatibility
        public double planDirectTime() { return planDirectTime; }

        public String getCumPlanDirectTimeText() {
            return formatTime(cumPlanDirectTime);
        }
        public double getCumPlanDirectTime() {
            return cumPlanDirectTime;
        }

        public String getCumPlanValueText(double totalPlanTime) {
            return formatPercent(cumPlanValue/totalPlanTime);
        }
        public double getCumPlanValue() {
            return cumPlanValue;
        }

        public String getActualTotalTimeText() {
            return formatTime(actualDirectTime + actualIndirectTime);
        }

        public String getActualDirectTimeText() {
            return formatTime(actualDirectTime);
        }
        public double getActualDirectTime() {
            return actualDirectTime;
        }

        public String getActualIndirectPercentageText() {
            double totalTime = actualDirectTime + actualIndirectTime;
            if (totalTime == 0) return "";
            return formatPercent(actualIndirectTime / totalTime);
        }

        public String getCumActualDirectTimeText() {
            return formatTime(cumActualDirectTime);
        }
        public double getCumActualDirectTime() {
            return cumActualDirectTime;
        }

        public String getCumEarnedValueText(double totalPlanTime) {
            return formatPercent(cumEarnedValue/totalPlanTime);
        }
        public double getCumEarnedValue() {
            return cumEarnedValue;
        }

        public double getCumActualCost() {
            return cumActualCost;
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
            long hours = c.get(Calendar.HOUR_OF_DAY);
            long minutes = hours * 60L + c.get(Calendar.MINUTE);
            long seconds = minutes * 60L + c.get(Calendar.SECOND);
            long ms = seconds * 1000L + c.get(Calendar.MILLISECOND);
            return ms;
        }
        protected void setPeriodEnd(Object value) {
            synchronized (EVSchedule.this) {
                int pos = periods.indexOf(this);
                if (pos < 0) return;

                for (Iterator i = periods.iterator(); i.hasNext();)
                    ((Period) i.next()).note = NOTE_NEEDS_CALC;

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
                            cumActualCost        = next.cumActualCost;
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
                        long delta = newStart - oldStart;
                        delta += dstDifference(newStart, oldStart);
                        slideScheduleDates(delta);
                        fireNeedsRecalc();
                        return;
                    }

                    // delete any preceeding periods which begin AFTER the
                    // new end date of this period.
                    while (previous != null &&
                           periods.size() > 2 &&
                           getBeginDate().compareTo((Date) value) >= 0)
                        remove(--pos);

                    // delete any following periods which end BEFORE the
                    // new end date of this period.
                    Period next;
                    while ((next = get(pos+1)) != null &&
                           periods.size() > 2 &&
                           next.endDate.compareTo((Date) value) <= 0)
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
                         getBeginDate().compareTo((Date) value) < 0) &&
                        ((next = get(pos+1)) == null ||
                         next.endDate.compareTo((Date) value) > 0))
                        endDate = (Date) value;

                    clearAutomaticFlag();
                }
                fireNeedsRecalc();
            }
        }

        private boolean isScheduleEnd() {
            return inScheduleEnd()
                && (previous == null || previous.planTotalTime > 0);
        }
        private boolean inScheduleEnd() {
            return !isAutomatic() && planTotalTime == 0
                && cumPlanDirectTime == getLast().cumPlanDirectTime;
        }
        private void setAsScheduleEnd() {
            for (int i = periods.indexOf(this); i < periods.size(); i++)
                get(i).setPlanTime(0);
        }
        private void prepForException() {
            int pos = periods.indexOf(this);
            if (pos == periods.size()-1) {
                grow(false);
                getLast().setPlanTime(defaultPlanTotalTime);
            } else if (get(pos+1).isAutomatic()) {
                get(pos+1).setPlanTime(defaultPlanTotalTime);
                get(pos+1).clearAutomaticFlag();
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
                if (END_TOKEN.equalsIgnoreCase((String)value))
                    setAsScheduleEnd();
                else {
                    // parse the value to obtain a number of minutes
                    long time = FormatUtil.parseTime((String) value);
                    if (!inScheduleEnd() && (time == 0 || datesLocked))
                        prepForException();
                    setPlanTime(time);
                }
            }
        }

        private void setPlanDirectTime(double planDirectTime) {
            if (planDirectTime != -1 && directPercentage > 0)
                setPlanTime(planDirectTime / directPercentage);
        }

        public void setPlanDirectTime(Object value) {
            setPlanDirectTime(value, false);
        }

        public void setPlanDirectTime(Object value, boolean forceException) {
            if (value instanceof String) {
                if (END_TOKEN.equalsIgnoreCase((String)value))
                    setAsScheduleEnd();
                else {
                    // parse the value to obtain a number of minutes
                    long time = FormatUtil.parseTime((String) value);
                    if (!inScheduleEnd()
                            && (time == 0 || datesLocked || forceException))
                        prepForException();
                    setPlanDirectTime(time);
                }
            }

        }
        void clearAutomaticFlag() {
            synchronized (EVSchedule.this) {
                automatic = false;
                if (previous != null) previous.clearAutomaticFlag();
            }
        }

        /**
         * Return the percentage of time in this period that has elapsed
         * before the given instant in time.
         * 
         * If the given time falls after this period, this will return 1.0.
         * If the given time falls before this period, this will return 0.0.
         * Otherwise, it will return a number between 0 and 1, indicating
         * the percentage of this period's elapsed time that has passed
         * before the given time.
         */
        public double getElapsedPercent(Date when) {
            if (when == null) return 0;

            long periodBegin = getBeginDate().getTime();
            long periodElapsed = when.getTime() - periodBegin;
            if (periodElapsed <= 0)
                return 0;

            long periodLength = endDate.getTime() - periodBegin;
            if (periodElapsed >= periodLength)
                return 1;

            return (double) periodElapsed / (double) periodLength;
        }

        public String getNote() {
            if (note == null)
                note = calculateNote();
            return note;
        }

        private String calculateNote() {
            if (periodNotes == null || periodNotes.isEmpty())
                return "";

            StringBuffer result = new StringBuffer();
            for (Map.Entry<Date, String> e : periodNotes.entrySet()) {
                Date noteTimestamp = e.getKey();
                if (noteTimestamp.compareTo(endDate) >= 0)
                    break;
                if (noteTimestamp.compareTo(getBeginDate()) >= 0)
                    result.append("\n").append(e.getValue());
            }

            return result.length() == 0 ? "" : result.substring(1);
        }

        public void setNote(String note) {
            // create the map for notes if it doesn't already exist
            if (periodNotes == null)
                periodNotes = new TreeMap<Date, String>();

            // look through the existing notes, and delete any that fall
            // within this time period
            for (Iterator i = periodNotes.entrySet().iterator(); i.hasNext();) {
                Map.Entry<Date, String> e = (Map.Entry<Date, String>) i.next();
                Date noteTimestamp = e.getKey();
                if (noteTimestamp.compareTo(endDate) >= 0)
                    break;
                if (noteTimestamp.compareTo(getBeginDate()) >= 0)
                    i.remove();
            }

            // save the new note with a timestamp near the center of this
            // time period.
            note = (note == null ? "" : note.trim());
            this.note = note;
            if (StringUtils.hasValue(note)) {
                long timestamp = (endDate.getTime()
                        + getBeginDate().getTime()) / 2 - 5000;
                periodNotes.put(new Date(timestamp), note);
            }
        }

        @Override
        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException ncse) { return null; }
        }

        public void saveToXML(StringBuffer result) {
            result.append("<period end='").append(saveDate(endDate))
                .append("' pt='").append(planDirectTime)
                .append("' at='").append(actualDirectTime)
                .append("' cpv='").append(cumPlanValue)
                .append("' cev='").append(cumEarnedValue)
                .append("' cac='").append(cumActualCost);
            if (planTotalTime != planDirectTime)
                result.append("' ptt='").append(planTotalTime);
            if (actualIndirectTime > 0)
                result.append("' ait='").append(actualIndirectTime);
            if (automatic) result.append("' auto='true");
            result.append("'/>");
        }
        public Period(Element e) {
            endDate = getXMLDate(e, "end");
            planDirectTime = getXMLNum(e, "pt");
            planTotalTime = getXMLNum(e, "ptt", planDirectTime);
            actualDirectTime = getXMLNum(e, "at");
            actualIndirectTime = getXMLNum(e, "ait");
            cumPlanValue = getXMLNum(e, "cpv");
            cumEarnedValue = getXMLNum(e, "cev");
            cumActualCost = getXMLNum(e, "cac");
            automatic = "true".equals(e.getAttribute("auto"));
        }

        /** Warning - these fields are typically unused, so they
         * rarely contain any real data. Don't expect them to contain
         * anything useful unless you put it there. */
        double planValue = 0, earnedValue = 0, actualCost = 0;
        public String getPlanValue(double totalPlanTime) {
            return formatPercent(planValue/totalPlanTime); }
        public double planValue() { return planValue; }
        public String getEarnedValue(double totalPlanTime) {
            return formatPercent(earnedValue/totalPlanTime); }
        public double earnedValue() { return earnedValue; }
    }

    Vector periods = new Vector();
    boolean datesLocked = false;
    EVMetrics metrics = new EVMetrics();
    EVSnapshot baselineSnapshot = null;
    SortedMap<Date, String> periodNotes;

    private static final String NOTE_NEEDS_CALC = null;

    public EVSchedule() { this(20.0); }
    public EVSchedule(double hours) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_WEEK, 1);

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
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }


    public EVSchedule(Date start, Date end, double minutes) {
        add(new Period(start, 0.0));
        add(new Period(end, minutes));
    }

    public EVSchedule(ListData saveList, boolean datesLocked) {
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
        this.datesLocked = datesLocked;
        recalcCumPlanTimes();
        // double-check to ensure that dates are increasing?
    }

    public EVSchedule(Date startDate, double hoursPerWeek, int endWeek,
            Map exceptions, double levelOfEffort, boolean datesLocked) {
        this();
        double minutesPerWeek = Math.round(hoursPerWeek * 60);
        setLevelOfEffort(levelOfEffort);
        get(1).setBeginDate(startDate);
        get(1).setPlanDirectTime(minutesPerWeek);
        this.datesLocked = datesLocked;
        cleanUp();

        List weeksToTweak = new ArrayList();
        if (endWeek >= 0)
            weeksToTweak.add(new Integer(endWeek));
        if (exceptions != null)
            weeksToTweak.addAll(exceptions.keySet());
        if (weeksToTweak.isEmpty())
            return;

        Collections.sort(weeksToTweak);
        int maxTweakWeek = ((Integer) weeksToTweak.get(weeksToTweak.size() - 1))
                .intValue();
        if (maxTweakWeek != endWeek)
            maxTweakWeek++;
        if (minutesPerWeek > 0) {
            double totalTime = minutesPerWeek * (maxTweakWeek + 1);
            getPlannedCompletionDate(totalTime, totalTime);
            getLast().clearAutomaticFlag();
        } else {
            while (periods.size() < maxTweakWeek+2)
                grow(false);
        }

        if (exceptions != null) {
            for (Iterator i = exceptions.entrySet().iterator(); i
                    .hasNext();) {
                Map.Entry e = (Map.Entry) i.next();
                int week = ((Integer) e.getKey()).intValue();
                double hours = ((Number) e.getValue()).doubleValue();
                get(week+1).setPlanDirectTime(hours * 60);
            }
        }

        if (endWeek >= 0) {
            for (int i = endWeek+1;  i < periods.size(); i++)
                get(i).setPlanTime(0);
        }
    }

    public EVSchedule copy() { return new EVSchedule(this); }
    public EVSchedule(EVSchedule s) {
        addAllPeriods(s.periods, periods);
        directPercentage = s.directPercentage;
        metrics.costInterval = s.metrics.costInterval;
        metrics.timeErrInterval = s.metrics.timeErrInterval;
        metrics.completionDateInterval = s.metrics.completionDateInterval;
        metrics.totalSchedulePlanTime = s.metrics.totalSchedulePlanTime;
        metrics.totalScheduleActualTime = s.metrics.totalScheduleActualTime;
    }
    public void copyFrom(EVSchedule that) {
        addAllPeriods(that.periods, this.periods);
    }
    protected void addAllPeriods(List src, List dest) {
        dest.clear();
        Iterator i = src.iterator();
        Period p, prev = null;
        while (i.hasNext()) {
            p = (Period) ((Period) i.next()).clone();
            p.previous = prev; prev = p;
            p.note = NOTE_NEEDS_CALC;
            dest.add(p);
        }
    }
    public void multiply(double planMultiplier) {
        if (Double.isNaN(planMultiplier) || Double.isInfinite(planMultiplier))
            return;
        Iterator i = periods.iterator();
        while (i.hasNext()) {
            Period p = (Period) i.next();
            p.planDirectTime *= planMultiplier;
            p.cumPlanDirectTime *= planMultiplier;
            p.planTotalTime *= planMultiplier;
        }
        defaultPlanDirectTime *= planMultiplier;
        defaultPlanTotalTime *= planMultiplier;
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
        setLevelOfEffort(getXMLNum(e, "loe"));
    }

    protected synchronized void add(Period p) {
        p.previous = getLast();
        p.note = NOTE_NEEDS_CALC;
        periods.add(p);
    }

    protected synchronized void remove(int pos) {
        // refuse to delete the singular remaining period.
        if (periods.size() < 3) return;

        // remove the period in position "pos", as requested.
        periods.remove(pos);

        // repair the "previous" field of the period *now* in position pos.
        Period p = get(pos);
        if (p != null) {
            p.previous = (pos == 0 ? null : get(pos-1));
            p.note = NOTE_NEEDS_CALC;
        }

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
            p.note = NOTE_NEEDS_CALC;
        }
    }

    public TimeZone guessTimeZone() {
        Date scheduleStart = get(0).getEndDate(false);
        return TimeZoneUtils.inferTimeZoneFromDate(scheduleStart);
    }

    protected void adjustDates(DateAdjuster adj) {
        for (Iterator i = periods.iterator(); i.hasNext();) {
            Period p = (Period) i.next();
            Date origDate = p.endDate;
            Date newDate = adj.adjust(origDate);
            p.endDate = newDate;
            p.note = NOTE_NEEDS_CALC;
        }
        effectiveDate = adj.adjust(effectiveDate);
        metrics.adjustDates(adj);
        adjustPeriodNoteTimestamps(adj);
    }

    protected DateAdjuster normalizeDates(int offset) {
        RangeDateAdjuster result = new RangeDateAdjuster();

        Calendar c = Calendar.getInstance();

        for (Iterator i = periods.iterator(); i.hasNext();) {
            Period p = (Period) i.next();
            Date origDate = p.endDate;

            c.setTime(origDate);
            // add the offset.  This should get us "close to" midnight in the
            // target time zone.
            c.add(Calendar.MILLISECOND, offset);
            // now add four hours, and then truncate the date.  This will
            // account for differences due to daylight savings time.
            c.add(Calendar.HOUR_OF_DAY, 4);
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            Date newDate = c.getTime();

            p.endDate = newDate;
            p.note = NOTE_NEEDS_CALC;
            result.add(origDate, newDate);
        }

        // we are returning a DateAdjuster that alters timestamps for various
        // calendar periods.  But the special date "NEVER" should not be
        // altered, so we add a specific statement not to alter that timestamp.
        result.add(EVSchedule.NEVER, 0);

        effectiveDate = result.adjust(effectiveDate);
        metrics.adjustDates(result);
        adjustPeriodNoteTimestamps(result);

        return result;
    }

    protected void adjustPeriodNoteTimestamps(DateAdjuster adj) {
        if (periodNotes == null || periodNotes.isEmpty())
            return;

        TreeMap<Date, String> newMap = new TreeMap<Date, String>();
        for (Map.Entry<Date, String> e : periodNotes.entrySet())
            newMap.put(adj.adjust(e.getKey()), e.getValue());
        periodNotes = newMap;
    }

    /** Return the average number of days in the periods in this schedule. */
    protected double getAverageDaysPerPeriod() {
        long scheduleStart = get(0).getEndDate(false).getTime();
        long scheduleEnd = getLast().getEndDate(false).getTime();
        long avgPeriodLen = scheduleEnd - scheduleStart / getRowCount();
        double avgPeriodDays = (avgPeriodLen * 7.0) / WEEK_MILLIS;
        return avgPeriodDays;
    }

    public boolean areDatesLocked() {
        return datesLocked;
    }

    public void setDatesLocked(boolean datesLocked) {
        this.datesLocked = datesLocked;
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
            if (!periods.isEmpty())
                return (Period) periods.lastElement();
        } catch (NoSuchElementException nsee) {
        }
        return null;
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

    public boolean isEquivalentTo(EVSchedule that) {
        return this.getSaveList().equals(that.getSaveList());
    }

    public String getPeriodNoteData() {
        if (periodNotes == null || periodNotes.isEmpty())
            return null;
        ListData result = new ListData();
        for (Map.Entry<Date, String> e : periodNotes.entrySet()) {
            result.add(saveDate(e.getKey()));
            result.add(e.getValue());
        }
        return result.format();
    }

    public void setPeriodNoteData(String noteData) {
        periodNotes = new TreeMap<Date, String>();
        if (noteData != null && noteData.length() > 0) {
            ListData data = new ListData(noteData);
            for (int i = 1;  i < data.size(); i += 2) {
                try {
                    Date date = parseDate((String) data.get(i-1));
                    String note = (String) data.get(i);
                    periodNotes.put(date, note);
                } catch (Exception e) {
                    // bad data - try to continue with the next element.
                }
            }
        }
    }

    static String saveDate(Date d) {
        return (d == null ? "" : "@" + d.getTime());
    }
    static Date parseDate(String d) {
        if (!d.startsWith("@")) throw new IllegalArgumentException();
        long when = Long.parseLong(d.substring(1));
        if (when == 0) return A_LONG_TIME_AGO;
        if (when == Long.MAX_VALUE) return NEVER;
        return new Date(when);
    }
    static double getXMLNum(Element e, String attrName, double defVal) {
        if (e.hasAttribute(attrName))
            return getXMLNum(e, attrName);
        else
            return defVal;
    }
    static double getXMLNum(Element e, String attrName) {
        String s = e.getAttribute(attrName);
        if (s == null || s.length() == 0) return 0;
        try {
            return Double.parseDouble(s);
        } catch (Exception exc) { return 0; }
    }
    static Date getXMLDate(Element e, String attrName) {
        String s = e.getAttribute(attrName);
        if (s == null || s.length() == 0) return null;
        try {
            return EVSchedule.parseDate(s);
        } catch (Exception exc) { return null; }
    }

    public void saveToXML(StringBuffer result) {
        saveToXML(result, false);
    }
    public synchronized void saveToXML(StringBuffer result, boolean whitespace) {
        String indent = (whitespace ? "  " : "");
        String newline = (whitespace ? "\n" : "");
        result.append(indent).append("<schedule");
        metrics.saveToXML(result);
        if (directPercentage != 1.0)
            result.append(" loe='").append(1.0-directPercentage).append("'");
        result.append(">").append(newline);
        Iterator i = periods.iterator();
        while (i.hasNext()) {
            result.append(indent).append(indent);
            ((Period) i.next()).saveToXML(result);
            result.append(newline);
        }
        metrics.saveIntervalsToXML(result, whitespace);
        result.append(indent).append("</schedule>").append(newline);
    }

    public synchronized Period get(Date when) {
        long time = when.getTime();
        Period p;
        for (int i = periods.size();  i-- > 0; ) {
            p = get(i);
            if (p != null && p.getBeginDate().getTime() < time)
                return p;
        }
        return null;
    }

    public synchronized List<Period> getPeriods() {
        return new ArrayList<Period>(periods);
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
        return getScheduledPlanTime(when, false);
    }

    public double getScheduledPlanTime(Date when, boolean includePartial) {
        double result = 0;
        double auto = 0;
        Period p;
        for (int i = 0;   i < periods.size();   i++) {
            p = get(i);

            double thisPeriodPercent = 0;
            if (p != null)
                thisPeriodPercent = p.getElapsedPercent(when);
            if (thisPeriodPercent == 0)
                break;
            if (thisPeriodPercent < 1 && !includePartial)
                break;

            double thisPeriodTime;
            if (p.automatic)
                thisPeriodTime = auto;
            else
                thisPeriodTime = (auto = p.planDirectTime);

            result += (thisPeriodTime * thisPeriodPercent);
        }
        return result;
    }

    /** return the total amount of actual time for periods ending
     * before the given date. */
    public double getScheduledActualTime(Date when) {
        return getScheduledActualTime(when, false);
    }

    public double getScheduledActualTime(Date when, boolean includePartial) {
        double result = 0;
        long time = when.getTime();
        Period p;
        for (int i = 0;   i < periods.size();   i++) {
            p = get(i);
            if (p == null || p.getBeginDate().getTime() > time)
                break;
            if (!includePartial && p.getEndDate().getTime() > time)
                break;
            else
                result += p.actualDirectTime;
        }
        return result;
    }

    /** Return the date that the schedule would reach the given cumulative
     * plan time. Perform a "what-if" calculation - don't modify the
     * current schedule.
     */
    public Date getHypotheticalDate(double cumPlanTime, boolean useDTPI) {
        EVSchedule s = new EVSchedule(this);
        s.cleanUp();
        if (useDTPI) s.multiply(1 / metrics.directTimePerformanceIndexEff());
        double extra = cumPlanTime + s.defaultPlanDirectTime;
        s.getPlannedCompletionDate(extra, extra);
        return s.extrapolateWithinSchedule(cumPlanTime);
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

    protected Date extrapolateWithinSchedule(double cumPlanTime) {
        if (cumPlanTime < 0) return A_LONG_TIME_AGO;
        if (Double.isNaN(cumPlanTime) || Double.isInfinite(cumPlanTime))
            return NEVER;

        Iterator i = periods.iterator();
        while (i.hasNext()) {
            Period p = (Period) i.next();
            if (p.cumPlanDirectTime < cumPlanTime || p.previous == null)
                continue;

            double prevCumPlanTime = p.previous.cumPlanDirectTime;
            double percent = (cumPlanTime - prevCumPlanTime) /
                (p.cumPlanDirectTime - prevCumPlanTime);

            long start = p.getBeginDate().getTime();
            long end = p.endDate.getTime();
            long duration = end - start;
            long durationPercent = (long) (duration * percent);
            return new Date(start + durationPercent);
        }

        return NEVER;
    }


    public synchronized void saveCompletedTask(Date dateCompleted,
                                               double earnedValue) {
        saveActualTaskInfo(dateCompleted, 0, earnedValue, 0, 0, true);
    }
    public synchronized void saveCompletedTaskCost(Date dateCompleted,
            double actualCost) {
        saveActualTaskInfo(dateCompleted, 0, 0, 0, actualCost, true);
    }
    public synchronized void saveActualTime(Date when, double actualTime) {
        saveActualTaskInfo(when, 0, 0, actualTime, 0, true);
    }
    public synchronized void saveActualIndirectTime(Date when, double actualTime) {
        saveActualTaskInfo(when, 0, 0, actualTime, 0, false);
    }
    protected synchronized void saveActualTaskInfo(Date when,
                                                double planValue,
                                                double earnedValue,
                                                double actualTime,
                                                double actualCost,
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
                    p.cumPlanValue += planValue;
                    p.cumEarnedValue += earnedValue;
                    p.cumActualDirectTime += actualTime;
                    p.cumActualCost += actualCost;
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
                    p.cumPlanValue += planValue;
                    p.cumEarnedValue += earnedValue;
                    p.cumActualDirectTime  += actualTime;
                    p.cumActualCost += actualCost;
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
    protected boolean showNotesColumn = false;
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
    public double getLevelOfEffort() {
        return 1.0 - directPercentage;
    }
    public boolean isShowNotesColumn() {
        return showNotesColumn;
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

    public Date getEndDate() {
        if (defaultPlanDirectTime > 0
                || getLast().getPlanDirectTime() > 0)
            return null;
        for (int i = periods.size();  i-- > 0; ) {
            Period p = get(i);
            if (p.isScheduleEnd())
                return p.getBeginDate();
        }
        return null;
    }

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
        z.cumActualCost = y.cumActualCost;
        z.cumActualDirectTime = y.cumActualDirectTime;
        z.automatic = automatic;

        add(z);
        //System.out.println("growing schedule - new task ends: " + zdate);
        return true;
    }

    protected int findIndexOfFinalPlanValuePeriod() {
        return Math.min(findIndexOfFinalPeriodWithPlannedTime(),
            findIndexOfPlannedCompletionPeriod());
    }

    protected int findIndexOfFinalPlanTimePeriod() {
        int extendToPeriod = findIndexOfActualCompletionPeriod();
        if (extendToPeriod == -1)
            extendToPeriod = Math.min(periods.size() - 1, effectivePeriod);
        return Math.max(extendToPeriod, findIndexOfFinalPlanValuePeriod());
    }

    protected int findIndexOfFinalPeriodWithPlannedTime() {
        for (int i = periods.size();   i-- > 1; ) {
            Period p = get(i);
            if (p != null && p.getPlanDirectTime() > 0)
                return i;
        }
        return 1;
    }

    protected int findIndexOfPlannedCompletionPeriod() {
        // get the total amount of planned value in the schedule.  Subtract a
        // small "fuzz factor" amount to simplify floating point comparisons
        double totalPlanValueCutoff = metrics.totalPlanTime - 0.05;
        for (int i = 1; i < periods.size(); i++) {
            Period p = get(i);
            if (p != null && p.getCumPlanValue() > totalPlanValueCutoff)
                return i;
        }
        return periods.size() - 1;
    }

    protected int findIndexOfActualCompletionPeriod() {
        // get the total amount of planned value in the schedule.  Subtract a
        // small "fuzz factor" amount to simplify floating point comparisons
        double totalPlanValueCutoff = metrics.totalPlanTime - 0.05;
        for (int i = 1; i < periods.size(); i++) {
            Period p = get(i);
            if (p != null && p.getCumEarnedValue() > totalPlanValueCutoff)
                return i;
        }
        return -1;
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
    public static long dstDifference(long first, long second) {
        Calendar calendar = Calendar.getInstance();

        long firstDSTOffset, secondDSTOffset;
        calendar.setTime(new Date(first));
        firstDSTOffset = calendar.get(Calendar.DST_OFFSET);
        calendar.setTime(new Date(second));
        secondDSTOffset = calendar.get(Calendar.DST_OFFSET);

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
        r.note = NOTE_NEEDS_CALC;
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
        grow(true);
        Period p = getLast();
        while (p != null && p.isAutomatic()) {
            p.planTotalTime = defaultPlanTotalTime;
            p.planDirectTime = defaultPlanDirectTime;
            p.automatic = false;
            p = p.previous;
        }
        recalcCumPlanTimes();
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
            p.cumPlanValue = p.cumEarnedValue = p.cumActualCost = 0;
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
        for (int i = 0;   i < periods.size();   i++) {
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
        p.actualCost = p.cumActualCost;
        while (i.hasNext()) {
            p = (Period) i.next();
            p.planValue   = p.cumPlanValue   - p.previous.cumPlanValue;
            p.earnedValue = p.cumEarnedValue - p.previous.cumEarnedValue;
            p.actualCost  = p.cumActualCost  - p.previous.cumActualCost;
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
        return FormatUtil.formatTime(time); }
    public static String formatPercent(double p) {
        return EVTask.formatPercent(p); }
    public static String formatDate(Date d) {
        if (d == null || d == A_LONG_TIME_AGO)
            return "";
        else if (d == NEVER)
            return NEVER_STRING;
        else
            return DATE_FORMATTER.format(d);
    }
    private static DateFormat DATE_FORMATTER =
        DateFormat.getDateInstance(DateFormat.SHORT);
    private static final String NEVER_STRING =
        resources.getString("Schedule.Date_Never");


    ///
    /// Table model
    ///

    public static final String[] COLUMN_KEYS = {
        "From", "To", "PT", "PDT", "CPT", "CPV", "Time", "PctI",
        "DTime", "CT", "Notes", "EV" };

    public static final String[] colNames =
        resources.getStrings("Schedule.Columns.", COLUMN_KEYS, ".Name");
    public static final int[] colWidths =
        resources.getInts("Schedule.Columns.", COLUMN_KEYS, ".Width_");
    public static final String[] TOOL_TIPS =
        resources.getStrings("Schedule.Columns.", COLUMN_KEYS, ".Tooltip");

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
    public static final int NOTES_COLUMN          = 10;
    public static final int CUM_VALUE_COLUMN      = 11;


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
        String.class,           // notes
        String.class };         // cumulative earned value

    public static final Object[] COLUMN_FORMATS = {
        EVTaskList.COLUMN_FMT_DATE,           // From
        EVTaskList.COLUMN_FMT_DATE,           // To
        EVTaskList.COLUMN_FMT_TIME,           // planned time
        EVTaskList.COLUMN_FMT_TIME,           // planned direct time
        EVTaskList.COLUMN_FMT_TIME,           // planned cumulative time
        EVTaskList.COLUMN_FMT_PERCENT,        // planned cumulative value
        EVTaskList.COLUMN_FMT_TIME,           // actual time
        EVTaskList.COLUMN_FMT_PERCENT,        // percent indirect time
        EVTaskList.COLUMN_FMT_TIME,           // actual direct time
        EVTaskList.COLUMN_FMT_TIME,           // actual cumulative time
        EVTaskList.COLUMN_FMT_OTHER,          // notes
        EVTaskList.COLUMN_FMT_PERCENT,        // cumulative earned
    };


    public int getRowCount() { return periods.size() - 1; }
    public int getColumnCount() { return colNames.length; }
    public String getColumnName(int i) {
        if (!showDirectColumns &&
            (i == PLAN_DTIME_COLUMN || i == IPERCENT_COLUMN || i == DTIME_COLUMN))
            return " " + colNames[i] + " ";
        else if (!showNotesColumn && i == NOTES_COLUMN)
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
        if (Settings.isReadOnly()) return false;
        if (columnIndex == NOTES_COLUMN) return true;
        if (columnIndex < 2) return (datesLocked == false);
        return columnIndex < 4;
    }
    public Object getValueAt(int rowIndex, int columnIndex) {
        Period p = get(rowIndex+1);
        if (p == null) return null;
        switch(columnIndex) {
        case FROM_COLUMN:           return p.getBeginDate();
        case TO_COLUMN:             return p.getEndDate();
        case PLAN_TIME_COLUMN:      return p.getPlanTotalTimeText();
        case PLAN_DTIME_COLUMN:     return p.getPlanDirectTimeText();
        case PLAN_CUM_TIME_COLUMN:  return p.getCumPlanDirectTimeText();
        case PLAN_CUM_VALUE_COLUMN: return p.getCumPlanValueText(totalPlan());
        case TIME_COLUMN:           return p.getActualTotalTimeText();
        case IPERCENT_COLUMN:       return p.getActualIndirectPercentageText();
        case DTIME_COLUMN:          return p.getActualDirectTimeText();
        case CUM_TIME_COLUMN:       return p.getCumActualDirectTimeText();
        case NOTES_COLUMN:          return p.getNote();
        case CUM_VALUE_COLUMN:      return p.getCumEarnedValueText(totalPlan());
        }
        return null;
    }
    protected double totalPlan() { return metrics.totalPlan(); }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        Period p = get(rowIndex+1);
        if (p == null) return;
        switch(columnIndex) {
        case FROM_COLUMN:       p.setBeginDate(aValue);       break;
        case TO_COLUMN:         p.setEndDate(aValue);         break;
        case PLAN_TIME_COLUMN:  p.setPlanTime(aValue);        break;
        case PLAN_DTIME_COLUMN: p.setPlanDirectTime(aValue);  break;
        case NOTES_COLUMN:      p.setNote((String) aValue);   break;
        }
    }
    public boolean rowIsAutomatic(int row) {
        Period p = get(row+1);
        return (p != null && p.automatic);
    }

    public boolean rowIsEffective(int row) {
        return row + 1 == effectivePeriod;
    }
    public int getRowForEffectivePeriod() {
        return effectivePeriod - 1;
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

    protected void setBaseline(EVSnapshot baseline) {
        this.baselineSnapshot = baseline;
    }

    protected static final Double ZERO = new Double(0.0);

    protected static class NumericRange extends Number implements RangeInfo {

        Number value, low, high;

        public NumericRange(Number value, Number low, Number high) {
            this.value = value;
            this.low = low;
            this.high = high;
        }

        @Override
        public int intValue() { return value.intValue(); }
        @Override
        public long longValue() { return value.longValue(); }
        @Override
        public float floatValue() { return value.floatValue(); }
        @Override
        public double doubleValue() { return value.doubleValue(); }

        @Override
        public String toString() { return String.valueOf(value); }

        public Range getRangeBounds(boolean includeInterval) { return null; }

        public double getRangeLowerBound(boolean includeInterval) {
            return low.doubleValue();
        }

        public double getRangeUpperBound(boolean includeInterval) {
            return high.doubleValue();
        }
    }

    private abstract class PlanChartSeries implements XYChartSeries {
        protected boolean isBaseline = false;
        public String getSeriesKey() {
            return (isBaseline ? "Baseline" : "Plan");
        }
        public Number getX(int itemIndex) {
            return new Long(get(itemIndex).endDate.getTime()); }
    }
    protected PlanChartSeries markAsBaseline(PlanChartSeries s) {
        s.isBaseline = true;
        return s;
    }

    private abstract class ActualChartSeries implements XYChartSeries {
        public String getSeriesKey()  { return "Actual"; }
        public int getItemCount() {
            int result = getRowCount()+1;
            if (effectivePeriod < result) result = effectivePeriod+1;
            return result;
        }
        public Number getX(int itemIndex) {
            Date d;
            if (itemIndex < effectivePeriod)
                d = get(itemIndex).endDate;
            else
                d = effectiveDate;
            return new Long(d.getTime());
        }
    }

    protected class ForecastChartSeries implements XYChartSeries {
        Number currentYVal, forecastYVal, forecastYValLow, forecastYValHigh;
        Number currentXVal, forecastXVal;
        int itemCount = 2;
        public String getSeriesKey()  { return "Forecast"; }
        public int getItemCount() { return itemCount; }
        public Number getX(int itemIndex) {
            return (itemIndex == 0 ? currentXVal : forecastXVal);
        }
        public Number getY(int itemIndex) {
            return (itemIndex == 0 ? currentYVal : forecastYVal);
        }
        public void recalc() {
            itemCount = 2;
            currentXVal = dateToLong(effectiveDate);
            forecastXVal = dateToLong(getForecastDate(), getForecastDateLPI(),
                                      getForecastDateUPI());
            if (itemCount == 2 &&
                (notLessThan(currentXVal, forecastXVal) ||
                 notLessThan(currentYVal, forecastYVal)))
                itemCount = 0;
            else if (okayNumber(forecastYValLow) &&
                     okayNumber(forecastYValHigh))
                forecastYVal = new NumericRange(forecastYVal, forecastYValLow,
                                                forecastYValHigh);
        }
        protected Date getForecastDate() {
            return metrics.independentForecastDate();
        }
        protected Date getForecastDateLPI() { return null; }
        protected Date getForecastDateUPI() { return null; }
        private boolean okayNumber(Number n) {
            return (n != null &&
                    !Double.isNaN(n.doubleValue()) &&
                    !Double.isInfinite(n.doubleValue()));
        }
        private boolean notLessThan(Number a, Number b) {
            if (!okayNumber(a) || !okayNumber(b)) return true;
            if (a.doubleValue() < b.doubleValue()) return false;
            return true;
        }
        private Number dateToLong(Date d) {
            return dateToLong(d, null, null);
        }
        private Number dateToLong(Date d, Date l, Date h) {
            if (d == null) {
                itemCount = 0;
                return null;
            }
            Number result = new Long(d.getTime());
            if (l != null && h != null)
                result = new NumericRange(result, new Long(l.getTime()),
                                          new Long(h.getTime()));
            return result;
        }
    }

    protected class EVScheduleChartEventAdapter extends ChartEventAdapter
            implements TableModelListener {

        public void registerForUnderlyingDataEvents() { addTableModelListener(this); }
        public void deregisterForUnderlyingDataEvents() { removeTableModelListener(this); }

        public void tableChanged(TableModelEvent e) { chartDataRecalcHelper.dataChanged(); }
    }

    private class PlanTimeSeries extends PlanChartSeries {
        public int getItemCount() {
            return findIndexOfFinalPlanTimePeriod() + 1; }
        public Number getY(int itemIndex) {
            return new Double(get(itemIndex).cumPlanDirectTime / 60.0); } }
    private XYChartSeries getBaselineTimeSeries() {
        return markAsBaseline(new PlanTimeSeries()); }
    private class ActualTimeSeries extends ActualChartSeries {
        public Number getY(int itemIndex) {
            return new Double(get(itemIndex).cumActualDirectTime / 60.0); } }
    private class ActualCostSeries extends ActualChartSeries {
        public Number getY(int itemIndex) {
            return new Double(get(itemIndex).cumActualCost / 60.0); } }

    /** XYDataSource for charting plan vs actual direct hours.
     */
    private class TimeChartData extends XYChartData {
        public TimeChartData(ChartEventAdapter eventAdapter) {
            super(eventAdapter);
            plan = new PlanTimeSeries();
            actual = new ActualTimeSeries();
            forecast = new ForecastChartSeries();
        }
        XYChartSeries plan, actual;
        ForecastChartSeries forecast;
        @Override
        public void recalc() {
            clearSeries();
            recalcBaseline();
            maybeAddSeries(plan);
            maybeAddSeries(actual);
            recalcForecast();
        }
        private void recalcBaseline() {
            if (baselineSnapshot != null) {
                EVSchedule baselineSchedule = baselineSnapshot.getTaskList()
                        .getSchedule();
                maybeAddSeries(baselineSchedule.getBaselineTimeSeries());
            }
        }
        private void recalcForecast() {
            forecast.currentYVal = makeTime(getLast().cumActualDirectTime);
            forecast.forecastYVal = makeTime(metrics.independentForecastCostEff());
            forecast.forecastYValLow = makeTime
                (metrics.independentForecastCostLPI());
            forecast.forecastYValHigh = makeTime
                (metrics.independentForecastCostUPI());
            forecast.recalc();
            maybeAddSeries(forecast);
        }
        private Number makeTime(double d) { return new Double(d / 60.0); }
    }
    public XYDataset getTimeChartData() {
        return new TimeChartData(new EVScheduleChartEventAdapter());
    }


    private class PlanValueSeries extends PlanChartSeries {
        double mult;
        PlanValueSeries(double m) { mult = m; }
        public int getItemCount() {
            return findIndexOfFinalPlanValuePeriod() + 1; }
        public Number getY(int itemIndex) {
            return new Double(get(itemIndex).cumPlanValue * mult); } }
    private XYChartSeries getBaselineValueSeries(double mult) {
        return markAsBaseline(new PlanValueSeries(mult)); }
    private class ActualValueSeries extends ActualChartSeries {
        double mult;
        ActualValueSeries(double m) { mult = m; }
        public Number getY(int itemIndex) {
            return new Double(get(itemIndex).cumEarnedValue * mult); } };


    /** XYDataSource for charting plan vs actual earned value.
     */
    protected class ValueChartData extends XYChartData {
        public ValueChartData(ChartEventAdapter eventAdapter) {
            super(eventAdapter);
            double mult = 100.0 / totalPlan();
            plan = new PlanValueSeries(mult);
            actual = new ActualValueSeries(mult);
            forecast = new ForecastChartSeries() {
                    @Override
                    protected Date getForecastDateLPI() {
                        return metrics.independentForecastDateLPI(); }
                    @Override
                    protected Date getForecastDateUPI() {
                        return metrics.independentForecastDateUPI(); }
                };
        }
        ForecastChartSeries forecast;
        PlanValueSeries plan;
        ActualValueSeries actual;
        @Override
        public void recalc() {
            clearSeries();
            double mult = 100.0 / totalPlan();
            if (Double.isInfinite(mult)) mult = 0;
            plan.mult = actual.mult = mult;
            recalcBaseline(mult);
            maybeAddSeries(plan);
            maybeAddSeries(actual);
            recalcForecast(new Double(getLast().cumEarnedValue * mult),
                           new Double(getLast().cumPlanValue * mult));
        }
        protected void recalcBaseline(double mult) {
            if (baselineSnapshot != null) {
                EVSchedule baselineSchedule = baselineSnapshot.getTaskList()
                        .getSchedule();
                maybeAddSeries(baselineSchedule.getBaselineValueSeries(mult));
            }
        }
        protected void recalcForecast(Double currentYVal, Double forecastYVal) {
            forecast.currentYVal = currentYVal;
            forecast.forecastYVal = forecastYVal;
            forecast.recalc();
            maybeAddSeries(forecast);
        }
    }
    public XYDataset getValueChartData() {
        ValueChartData result = new ValueChartData(new EVScheduleChartEventAdapter());
        result.recalc();
        return result;
    }


    /** XYDataSource for charting cost and schedule on one chart.
     */
    private class CombinedChartData extends XYChartData {
        public CombinedChartData(ChartEventAdapter eventAdapter) {
            super(eventAdapter);
            maybeAddSeries(new PlanValueSeries(1.0 / 60.0) {
                    @Override
                    public String getSeriesKey() { return "Plan_Value"; }});
            maybeAddSeries(new ActualValueSeries(1.0 / 60.0) {
                    @Override
                    public String getSeriesKey() { return "Actual_Value"; }});
            maybeAddSeries(new ActualCostSeries() {
                    @Override
                    public String getSeriesKey() { return "Actual_Cost"; }});
        }
    }
    public XYDataset getCombinedChartData() {
        return new CombinedChartData(new EVScheduleChartEventAdapter()); }


    public XYDataset getConfidenceIntervalTotalCostChartData() {
        return new ConfidenceIntervalTotalCostChartData(new EVScheduleChartEventAdapter(),
                                                        metrics);
    }
    public XYDataset getConfidenceIntervalCompletionDateChartData() {
        return new ConfidenceIntervalCompletionDateChartData(new EVScheduleChartEventAdapter(),
                                                             metrics);
    }


    /**
     * Abstract chart series for computing the trend of the discrepancy
     * between a planned and actual value.  Extrapolates the plan for the
     * effective time period.
     */
    private abstract class ActualTrendChartSeries extends ActualChartSeries
            implements RangeInfo {
        double mult = 1.0;
        double range;
        protected String seriesKey;
        public abstract double getPlanValue(Period p);
        public abstract double getActualValue(Period p);
        public Number getY(int itemIndex) {
            Period p = get(itemIndex);

            double plan = getPlanValue(p);
            double actual = getActualValue(p);
            double pct = p.getElapsedPercent(getEffectiveDate());

            if (pct < 1) {
                double prevPlan = 0;
                if (p.previous != null)
                    prevPlan = getPlanValue(p.previous);
                double delta = plan - prevPlan;
                plan = prevPlan + delta * pct;
            }

            return (actual - plan) * mult;
        }
        public void recalc() {
            int i = getItemCount() - 1;
            Period p = get(i);
            double max = Math.abs(getPlanValue(p)) * mult * 0.25;
            while (i >= 0) {
                double onePoint = Math.abs(getY(i--).doubleValue());
                max = Math.max(max, onePoint);
            }
            range = max;
        }
        public Range getRangeBounds(boolean includeInterval) {
            return new Range(-range, range);
        }
        public double getRangeLowerBound(boolean includeInterval) {
            return -range;
        }
        public double getRangeUpperBound(boolean includeInterval) {
            return range;
        }
        @Override public String getSeriesKey() {
            return (seriesKey != null ? seriesKey : super.getSeriesKey());
        }
    }
    private class PlanTrendChartSeries extends ActualChartSeries {
        public String getSeriesKey()  { return "Plan"; }
        public Number getY(int itemIndex) {
            return Integer.valueOf(0);
        }
    }
    public XYChartSeries getPlanTrendChartSeries() {
        return new PlanTrendChartSeries();
    }


    /**
     * XYDataSource for charting the earned value trend
     */
    private class ActualValueTrendChartSeries extends ActualTrendChartSeries {
        @Override public double getActualValue(Period p) {
            return p.cumEarnedValue;
        }
        @Override public double getPlanValue(Period p) {
            return p.cumPlanValue;
        }
    };
    public XYChartSeries getActualValueTrendChartSeries(String name, double mult) {
        ActualValueTrendChartSeries result = new ActualValueTrendChartSeries();
        result.seriesKey = name;
        result.mult = mult;
        result.recalc();
        return result;
    }
    protected class ValueTrendChartData extends XYChartData {
        ActualTrendChartSeries actual;
        public ValueTrendChartData(ChartEventAdapter eventAdapter) {
            super(eventAdapter);
            series.add(new PlanTrendChartSeries());
            series.add(actual = new ActualValueTrendChartSeries());
        }
        @Override
        public void recalc() {
            actual.mult = 100.0 / totalPlan();
            actual.recalc();
        }
    }
    public XYDataset getValueTrendChartData() {
        return new ValueTrendChartData(new EVScheduleChartEventAdapter());
    }


    /**
     * XYDataSource for charting the direct time trend
     */
    private class ActualTimeTrendChartSeries extends ActualTrendChartSeries {
        ActualTimeTrendChartSeries() { mult = 1 / 60.0; }
        @Override public double getActualValue(Period p) {
            return p.cumActualDirectTime;
        }
        @Override public double getPlanValue(Period p) {
            return p.cumPlanDirectTime;
        }
    };
    public XYChartSeries getActualTimeTrendChartSeries(String name) {
        ActualTimeTrendChartSeries result = new ActualTimeTrendChartSeries();
        result.seriesKey = name;
        result.recalc();
        return result;
    }
    protected class TimeTrendChartData extends XYChartData {
        ActualTimeTrendChartSeries actual;
        public TimeTrendChartData(ChartEventAdapter eventAdapter) {
            super(eventAdapter);
            series.add(new PlanTrendChartSeries());
            series.add(actual = new ActualTimeTrendChartSeries());
        }
        @Override
        public void recalc() {
            actual.recalc();
        }
    }
    public XYDataset getTimeTrendChartData() {
        return new TimeTrendChartData(new EVScheduleChartEventAdapter());
    }


    /**
     * XYDatasource for charting planned-vs-actual time on completed periods
     */
    private abstract class CompletedPeriodsXYChartSeries implements XYChartSeries {
        protected List<Period> completedPeriods;
        protected String seriesKey;

        public CompletedPeriodsXYChartSeries(String seriesKey) {
            this.seriesKey = seriesKey;
            this.completedPeriods = new ArrayList<Period>();
        }

        public int getItemCount() {
            return completedPeriods.size();
        }

        public String getSeriesKey() {
            return seriesKey;
        }

        public Number getX(int itemIndex) {
            return completedPeriods.get(itemIndex).getEndDate().getTime();
        }

        abstract void recalc();
    }

    private class CompletedPeriodsPlanVsActXYChartSeries extends CompletedPeriodsXYChartSeries {

        private List<Double> ratios;

        public CompletedPeriodsPlanVsActXYChartSeries(String seriesKey) {
            super(seriesKey);
            this.ratios = new ArrayList<Double>();
        }

        /**
         * Iterates through all periods an adds the completed ones to the
         *  completedPeriods List. The periods are discarted if they have an
         *  invalid planned-vs-actual time ratio.
         */
        public void recalc() {
            completedPeriods.clear();
            ratios.clear();
            double ratio = 0;

            for (Object o : periods) {
                Period p = (Period) o;

                ratio = p.getActualDirectTime() / p.getPlanDirectTime();

                if ((p.getEndDate().before(getEffectiveDate())
                    || p.getEndDate().equals(getEffectiveDate()))
                    && (!Double.isNaN(ratio) && !Double.isInfinite(ratio) && ratio > 0)) {
                    ratios.add(ratio);
                    completedPeriods.add(p);
                }
            }
        }

        public Number getY(int itemIndex) {
            return ratios.get(itemIndex);
        }

    }
    private class CompletedPeriodsDirectTimeVarianceXYChartSeries extends CompletedPeriodsXYChartSeries {

        public CompletedPeriodsDirectTimeVarianceXYChartSeries(String seriesKey) {
            super(seriesKey);
        }

        public Number getY(int itemIndex) {
            Period period = completedPeriods.get(itemIndex);

            // The units are in minutes and we want them in hours so we divide by 60
            return (period.getActualDirectTime() - period.getPlanDirectTime()) / 60;
        }

        public void recalc() {
            completedPeriods.clear();

            for (Object o : periods) {
                Period p = (Period) o;

                if ((p.getEndDate().before(getEffectiveDate())
                    || p.getEndDate().equals(getEffectiveDate()))) {
                    completedPeriods.add(p);
                }
            }
        }

    }
    private class CompletedPeriodChartData extends XYChartData {

        private CompletedPeriodsXYChartSeries series;

        public CompletedPeriodChartData(ChartEventAdapter eventAdapter,
                                   CompletedPeriodsXYChartSeries series) {
            super(eventAdapter);
            this.series = series;
        }

        @Override
        public void recalc() {
            clearSeries();
            this.series.recalc();
            maybeAddSeries(series);
        }

    }
    public XYDataset getCompletedPeriodsPlanVsActData() {
        return new CompletedPeriodChartData(new EVScheduleChartEventAdapter(),
                                       new CompletedPeriodsPlanVsActXYChartSeries("Completed_Period"));
    }
    public XYDataset getCompletedPeriodsDirectTimeVarianceData() {
        return new CompletedPeriodChartData(new EVScheduleChartEventAdapter(),
                new CompletedPeriodsDirectTimeVarianceXYChartSeries("Completed_Period"));
    }
}
