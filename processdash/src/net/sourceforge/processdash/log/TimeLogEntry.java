// Process Dashboard - Data Automation Tool for high-maturity processes
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
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net


package net.sourceforge.processdash.log;

import java.text.DateFormat;
import java.util.Date;

import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.util.FormatUtil;

public class TimeLogEntry {

    // REFACTOR should this be visible
    public PropertyKey key;
    Date        createTime;
    long        minutesElapsed;
    long        minutesInterrupt;
    private String comment;

    private static final DateFormat df =
        DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);

    private static final String TAB        = "\t";
    private static final String START      = "Start Time: ";
    private static final String ELAPSED    = "Elapsed Time: ";
    private static final String INTERRUPT  = "Interruption Time: ";
    private static final String TERMINATOR = "\n";
    public static boolean sortByPhase = false;


    public TimeLogEntry (PropertyKey key,
                         Date        createTime,
                         long        minutesElapsed,
                         long        minutesInterrupt) {
        this.key              = key;
        this.createTime       = createTime;
        this.minutesElapsed   = minutesElapsed;
        this.minutesInterrupt = minutesInterrupt;
    }

    static TimeLogEntry valueOf(String s) throws IllegalArgumentException {
        TimeLogEntry tle  = new TimeLogEntry (null, null, 0, 0);

        int startPosition = 0;
        int endPosition  = s.indexOf (TAB, startPosition);
        if (startPosition < endPosition) {
            tle.key = PropertyKey.valueOf(s.substring(startPosition, endPosition));
            if (tle.key == null)
                tle.key = PropertyKey.fromKey(s.substring(startPosition, endPosition));
        }
        if (tle.key == null)
            throw new IllegalArgumentException("Invalid Key");

        startPosition = s.indexOf (START, endPosition) + START.length();
        endPosition = s.indexOf (TAB, startPosition);
        tle.createTime = FormatUtil.parseDateTime(s.substring (startPosition,
                                                                  endPosition));
        if (tle.createTime == null)
            throw new IllegalArgumentException("Invalid Start Time");

        startPosition = s.indexOf (ELAPSED, endPosition) + ELAPSED.length();
        endPosition = s.indexOf (TAB, startPosition);
        try {
            tle.minutesElapsed = Long.valueOf
                (s.substring (startPosition, endPosition)).longValue();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Elapsed Time");
        }

        startPosition = s.indexOf (INTERRUPT, endPosition) + INTERRUPT.length();
        endPosition = s.indexOf (TAB, startPosition);
        try {
            String minInterrupt;
            if (endPosition == -1)
                minInterrupt = s.substring (startPosition);
            else
                minInterrupt = s.substring (startPosition, endPosition);
            tle.minutesInterrupt = Long.parseLong(minInterrupt);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Interrupt Time");
        }

        if (endPosition == -1)
            tle.comment = null;
        else
            tle.comment = s.substring(endPosition+1);

        return tle;
    }

    public String toString() {
        return (((key == null) ? "" : key.toString()) + TAB +
                START + FormatUtil.formatDateTime(createTime) + TAB +
                ELAPSED + minutesElapsed + TAB +
                INTERRUPT + minutesInterrupt +
                (comment == null ? "" : TAB + comment) + TERMINATOR);
    }

    private String escComment(String s) {
        return s.replace('\n', '');
    }

    private String unescComment(String s) {
        return s.replace('', '\n');
    }

    public String toAbbrevString() {
        return ("!" + FormatUtil.formatDateTime(createTime) + "!," +
                minutesElapsed);
    }

    public boolean lessThan(TimeLogEntry o) {
        int comp = (sortByPhase ? key.key().compareTo (o.key.key()) : 0);
        return (comp == 0 ? createTime.before (o.createTime) : comp < 0);
    }

    public boolean isSimilarTo(TimeLogEntry o) {
        if (o == this) return true;

        long timeDifference = o.createTime.getTime() - createTime.getTime();
        if (-2000 < timeDifference && timeDifference < 2000)
            if (o.getPath().equals(getPath()))
                return true;

        return false;
    }

    public String getPath() { return (key == null) ? "" : key.path(); }
    public Date getStartTime() { return createTime; }
    public long getElapsedTime() { return minutesElapsed; }
    public void setElapsedTime(long minutes) { minutesElapsed = minutes; }
    public long getInterruptTime() { return minutesInterrupt; }
    public void setInterruptTime(long minutes) { minutesInterrupt = minutes; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date d) { createTime = d; }
    public String getComment() {
        if (comment != null)
            return comment.replace('', '\n');
        else
            return null;
    }
    public void setComment(String comment) {
        if (comment == null || comment.trim().length() == 0)
            this.comment = null;
        else
            this.comment = comment.replace('\t', ' ').replace('\n', '');
    }

    public void merge(TimeLogEntry that) {
        this.minutesElapsed += that.minutesElapsed;
        this.minutesInterrupt += that.minutesInterrupt;
        if (that.getComment() != null) {
            if (this.getComment() == null)
                this.setComment(that.getComment());
            else
                this.setComment(this.getComment() + "\n" + that.getComment());
        }
    }

}
