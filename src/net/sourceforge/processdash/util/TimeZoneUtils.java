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

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class TimeZoneUtils {

    /**
     * This method performs the task analogous to TimeZone.getTimeZone(), but
     * will return null on failure (rather than GMT).
     */
    public static TimeZone getTimeZone(String id) {
        // if no ID was provided, return null.
        if (id == null || id.length() == 0)
            return null;

        // look up the associated time zone. Note that if the timezone
        // ID was not recognized, this call will return the GMT time zone.
        TimeZone result = TimeZone.getTimeZone(id);

        // if we got GMT back from the getTimeZone call, but our
        // original timezone ID did not specify GMT, then we should
        // interpret this as an ID lookup failure.
        if ("GMT".equals(result.getID()) && !"GMT".equals(id))
            return null;

        return result;
    }


    /**
     * Infer the time zone associated with a particular timestamp, assuming that
     * the timestamp reflects midnight someday, somewhere in the world.
     * 
     * Various processes can produce a timestamp aligned to midnight in the
     * current time zone. (For example, when a date-only DateFormat is used to
     * parse a date string, the time information will reflect midnight at the
     * beginning of that day.) When a date was constructed in this way, this
     * method attempts to guess what timezone was "current" when the timestamp
     * was produced.
     * 
     * Note that this method can only detect raw offsets from GMT. After all,
     * historical DST rules cannot be inferred from a single timestamp at a
     * single instant in time. So the time zones returned by this method will
     * never reflect DST awareness; they will always be of the form "GMT+##".
     * 
     * Also, this method only produces time zones with 30 minute increments. If
     * an arbitrary timestamp is passed in (for example, one representing 1:23
     * PM on some day), this method will not produce a useful timezone.
     * 
     * @param midnight
     *                a timestamp that (presumably) reflects midnight on a
     *                particular day in a particular time zone
     * @return a simple (non-DST-aware) time zone in which the given date would
     *         translate to midnight.
     */
    public static TimeZone inferTimeZoneFromDate(Date midnight) {
        // calculate the time relative to GMT
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.setTime(midnight);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        String zone;
        if (hour == 0 && minute == 0) {
            zone = "GMT";
        } else if (hour < 12) {
            zone = "GMT-" + hour;
            if (minute == 30)
                zone += ":30";
        } else if (minute == 30) {
            zone = "GMT+" + (24 - hour - 1) + ":30";
        } else {
            zone = "GMT+" + (24 - hour);
        }
        return TimeZone.getTimeZone(zone);
    }

}
