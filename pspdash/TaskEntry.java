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

public class TaskEntry {

    PropertyKey taskName;		// The node the task is attached to"
    long        plannedTime;	// in hours
    double       plannedValue;
    long	      plannedCumTime;	// hours
    double       plannedCumValue;
    Date        plannedDate;
    Date        actualDate;
    double       actualEarnedValue;

    private static final String TAB        = "	";
    private static final String TERMINATOR = "\n";


    public TaskEntry () {
        this (null, 0, 0.0, 0, 0.0, null, null, 0.0);
    }

    public TaskEntry (TaskEntry te) {
        this (((te.taskName == null) ? null : new PropertyKey (te.taskName)),
              te.plannedTime,
              te.plannedValue,
              te.plannedCumTime,
              te.plannedCumValue,
              ((te.plannedDate == null) ? null :
               new Date (te.plannedDate.getTime())),
              ((te.actualDate == null) ? null :
               new Date (te.actualDate.getTime())),
              te.actualEarnedValue);
    }

    public TaskEntry (PropertyKey taskName,
                      long        plannedTime,
                      double      plannedValue,
                      long	plannedCumTime,
                      double      plannedCumValue,
                      Date        plannedDate,
                      Date        actualDate,
                      double      actualEarnedValue) {
        this.taskName          = taskName;
        this.plannedTime       = plannedTime;
        this.plannedValue      = plannedValue;
        this.plannedCumTime    = plannedCumTime;
        this.plannedCumValue   = plannedCumValue;
        this.plannedDate       = plannedDate;
        this.actualDate        = actualDate;
        this.actualEarnedValue = actualEarnedValue;
    }

    static TaskEntry valueOf(String s) throws IllegalArgumentException {
        TaskEntry te  = new TaskEntry ();

        int startPosition = 0;
        int endPosition  = s.indexOf (TAB, startPosition);
        if (startPosition < endPosition)
            te.taskName = PropertyKey.fromKey (s.substring (startPosition,
                                                            endPosition));
        else
            throw new IllegalArgumentException
                ("Invalid Key:"+ s.substring (startPosition, endPosition + 1));

        startPosition = endPosition + 1;
        endPosition = s.indexOf (TAB, startPosition);
        try {
            te.plannedTime = Long.valueOf
                (s.substring (startPosition, endPosition)).longValue();
        } catch (Exception e) {
            throw new IllegalArgumentException
                ("Invalid Planned Time:"+s.substring (startPosition, endPosition));
        }

        startPosition = endPosition + 1;
        endPosition = s.indexOf (TAB, startPosition);
        try {
            te.plannedValue = Double.valueOf
                (s.substring (startPosition, endPosition)).doubleValue();
        } catch (Exception e) {
            throw new IllegalArgumentException
                ("Invalid Planned Value:"+s.substring (startPosition, endPosition));
        }

        startPosition = endPosition + 1;
        endPosition = s.indexOf (TAB, startPosition);
        try {
            te.plannedCumTime = Long.valueOf
                (s.substring (startPosition, endPosition)).longValue();
        } catch (Exception e) {
            throw new IllegalArgumentException
                ("Invalid Planned Cum Time:"+s.substring (startPosition, endPosition));
        }

        startPosition = endPosition + 1;
        endPosition = s.indexOf (TAB, startPosition);
        try {
            te.plannedCumValue = Double.valueOf
                (s.substring (startPosition, endPosition)).doubleValue();
        } catch (Exception e) {
            throw new IllegalArgumentException
                ("Invalid Planned Cum Value:"+s.substring(startPosition, endPosition));
        }

        startPosition = endPosition + 1;
        endPosition = s.indexOf (TAB, startPosition);
        te.plannedDate = DateFormatter.parseDate(s.substring (startPosition,
                                                              endPosition));

        startPosition = endPosition + 1;
        endPosition = s.indexOf (TAB, startPosition);
        te.actualDate = DateFormatter.parseDate(s.substring (startPosition,
                                                             endPosition));

        startPosition = endPosition + 1;
        endPosition = s.indexOf (TAB, startPosition);
        try {
            te.actualEarnedValue = Double.valueOf
                (s.substring (startPosition, endPosition)).doubleValue();
        } catch (Exception e) {
            throw new IllegalArgumentException
                ("Invalid Actual Earned Value:"+s.substring (startPosition,
                                                             endPosition));
        }

        return te;
    }

    public String toString() {
        return (((taskName == null) ? "" : taskName.key()) + TAB +
                plannedTime + TAB + plannedValue + TAB +
                plannedCumTime + TAB + plannedCumValue + TAB +
                ((plannedDate == null) ? "" :
                 DateFormatter.formatDate(plannedDate)) + TAB +
                ((actualDate == null) ? "" :
                 DateFormatter.formatDate(actualDate)) + TAB +
                actualEarnedValue + TERMINATOR);
    }

}
