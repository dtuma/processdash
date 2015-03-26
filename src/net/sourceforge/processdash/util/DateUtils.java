// Copyright (C) 2011 Tuma Solutions, LLC
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

package net.sourceforge.processdash.util;

import java.util.Calendar;
import java.util.Date;

public class DateUtils {

    public static final long SECONDS = 1000;

    public static final long MINUTES = 60 * SECONDS;
    public static final long MINUTE = MINUTES;

    public static final long HOURS = 60 * MINUTES;
    public static final long HOUR = HOURS;

    public static final long DAYS = 24 * HOURS;
    public static final long DAY = DAYS;

    public static final long WEEKS = 7 * DAYS;
    public static final long WEEK = WEEKS;

    public static Date maxDate(Date a, Date b) {
        if (a == null) return b;
        if (b == null) return a;
        if (a.compareTo(b) > 0) return a;
        return b;
    }

    public static Date minDate(Date a, Date b) {
        if (a == null) return b;
        if (b == null) return a;
        if (a.compareTo(b) < 0) return a;
        return b;
    }

    public static Date truncDate(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

}
