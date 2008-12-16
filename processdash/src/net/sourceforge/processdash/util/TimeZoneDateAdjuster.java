// Copyright (C) 2008 Tuma Solutions, LLC
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

import java.util.Date;
import java.util.TimeZone;

public class TimeZoneDateAdjuster implements DateAdjuster {

    private TimeZone from;

    private TimeZone to;

    public TimeZoneDateAdjuster(TimeZone from, TimeZone to) {
        this.from = from;
        this.to = to;
    }

    public Date adjust(Date d) {
        // don't adjust null dates
        if (d == null)
            return null;
        // don't adjust zero dates or dates very near Long.MAX_VALUE
        if (d.getTime() <= 0 || d.getTime() > CUTOFF_TIME)
            return d;

        // get the original timestamp
        long time = d.getTime();
        // convert to UTC
        time += from.getOffset(time);
        // convert from UTC to the destination timezone
        time -= to.getRawOffset();
        Date result = new Date(time);
        // if the final date is observing daylight savings time in the
        // destination timezone, subtract again to account for DST
        if (to.inDaylightTime(result)) {
            time -= to.getDSTSavings();
            result = new Date(time);
        }

        return result;
    }

    private static final long ONE_DAY = 24 * 60 * 60 * 1000;

    private static final long CUTOFF_TIME = Long.MAX_VALUE - ONE_DAY;

}
