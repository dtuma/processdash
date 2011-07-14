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

package net.sourceforge.processdash.ui.web.dash;

import java.io.IOException;
import java.util.Calendar;

import net.sourceforge.processdash.ui.web.TinyCGIBase;

/**
 * Localize the display of the popup calendars provided by JACS.
 * 
 * Currently, only the day of the week is being localized.  All textual elements
 * are still being displayed in English.  This could be enhanced in the future.
 */
public class JacsLocalizedCalendar extends TinyCGIBase {

    @Override
    protected void doGet() throws IOException {
        byte[] script = getRequest("/lib/jacs-js.txt", false);

        // get the day of the week, where 0 == Sunday. The checks on the 2nd
        // line below are probably unnecessary, but just be ultra careful.
        int startDay = Calendar.getInstance().getFirstDayOfWeek()
                - Calendar.SUNDAY;
        startDay = Math.min(Math.max(startDay, 0), 6);

        // the script contains a single-character integer literal near the end
        // which defines the start day. Locate that literal and change it.
        if (startDay != 0) {
            for (int pos = script.length; pos-- > 0;) {
                if (script[pos] == '0') {
                    script[pos] = (byte) ('0' + startDay);
                    break;
                }
            }
        }

        outStream.write(script);
        outStream.flush();
    }

}
