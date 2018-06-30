// Copyright (C) 2015-2018 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge.client;

import java.text.DateFormat;
import java.util.Calendar;

public class HistoricalMode {

    // This constant was historically defined in the TeamServerSelector class.
    // For backward compatibility, we must keep the same qualified string.
    public static final String DATA_EFFECTIVE_DATE_PROPERTY = //
            "net.sourceforge.processdash.tool.bridge.client.TeamServerSelector.effectiveDate";

    /**
     * Test whether the current process is operating in "historical mode,"
     * displaying data from some point in the past.
     */
    public static boolean isHistoricalModeEnabled() {
        return System.getProperty(DATA_EFFECTIVE_DATE_PROPERTY) != null;
    }

    /**
     * Return a human-readable string describing the effective date of the
     * historical data being displayed by the current process, or null if this
     * process is not in historical mode.
     */
    public static String getHistoricalDateStr() {
        String ts = System.getProperty(DATA_EFFECTIVE_DATE_PROPERTY);
        if (ts == null)
            return null;
    
        DateFormat fmt;
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(Long.parseLong(ts));
        if (c.get(Calendar.HOUR_OF_DAY) > 22)
            fmt = DateFormat.getDateInstance(DateFormat.MEDIUM);
        else
            fmt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
                DateFormat.SHORT);
        return fmt.format(c.getTime());
    }

}
