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

//Note: the valueOf & toString member functions only save / restore the
//NON-calculated member element (plannedTime).  In valueOf, the "date"
//field is calculated based on the passed in start date and week number.
//
public class ScheduleEntry {

    Date        date;
    long        plannedTime;	// in hours
    long	      plannedCumTime;	// hours
    double      plannedCumValue;
    long        actualTime;
    long        actualCumTime;
    double      actualCumEarnedValue;

    private static final long   MILLIS_PER_WEEK = 1000 * 60 * 60 * 24 * 7;


    public ScheduleEntry () {
        this (null, 0, 0, 0.0, 0, 0, 0.0);
    }

    public ScheduleEntry (long plannedTime) {
        this (null, plannedTime, 0, 0.0, 0, 0, 0.0);
    }

    public ScheduleEntry (ScheduleEntry se) {
        this (((se.date == null) ? null : new Date (se.date.getTime())),
              se.plannedTime,
              se.plannedCumTime,
              se.plannedCumValue,
              se.actualTime,
              se.actualCumTime,
              se.actualCumEarnedValue);
    }

    public ScheduleEntry (Date   date,
                          long   plannedTime,
                          long   plannedCumTime,
                          double plannedCumValue,
                          long   actualTime,
                          long   actualCumTime,
                          double actualCumEarnedValue) {
        this.date                 = date;
        this.plannedTime          = plannedTime;
        this.plannedCumTime       = plannedCumTime;
        this.plannedCumValue      = plannedCumValue;
        this.actualTime           = actualTime;
        this.actualCumTime        = actualCumTime;
        this.actualCumEarnedValue = actualCumEarnedValue;
    }

    static Date dateOf (Date start, long weekNumber) {
        if (start == null)
            return null;
        return new Date (start.getTime() + (weekNumber * MILLIS_PER_WEEK));
    }

    static ScheduleEntry valueOf(String s,
                                 long   weekNumber,
                                 Date   start) throws IllegalArgumentException {
        ScheduleEntry se;
        long pt;

        try {
            pt = Long.valueOf(s).longValue();
        } catch (Exception e) {
            throw new IllegalArgumentException ("Invalid Planned Time:"+s);
        }

        se      = new ScheduleEntry (pt);
        se.date = ((start == null) ? null : dateOf (start, weekNumber));
        return se;
    }

    public String toString() {
        return ("" + plannedTime);
    }

}
