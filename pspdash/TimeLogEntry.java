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

import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

public class TimeLogEntry {

    PropertyKey key;
    Date        createTime;
    long        minutesElapsed;
    long        minutesInterrupt;

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
        tle.createTime = DateFormatter.parseDateTime(s.substring (startPosition,
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
        try {
            tle.minutesInterrupt = Long.valueOf
                (s.substring (startPosition)).longValue();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Interrupt Time");
        }
        return tle;
    }

    public String toString() {
        return (((key == null) ? "" : key.toString()) + TAB +
                START + DateFormatter.formatDateTime(createTime) + TAB +
                ELAPSED + minutesElapsed + TAB +
                INTERRUPT + minutesInterrupt + TERMINATOR);
    }

    public String toAbbrevString() {
        return ("!" + DateFormatter.formatDateTime(createTime) + "!," +
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
    public long getInterruptTime() { return minutesInterrupt; }

}
