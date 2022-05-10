// Copyright (C) 2022 Tuma Solutions, LLC
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
import java.text.SimpleDateFormat;

import org.w3c.dom.Element;

public class EVDateFormatOverride implements Runnable {

    private static DateFormat DATE_FORMAT_OVERRIDE = null;

    public static DateFormat getDateFormatOverride() {
        return DATE_FORMAT_OVERRIDE;
    }


    public void setConfigElement(Element xml, String attrName) {
        // create a date format that has no year information
        DATE_FORMAT_OVERRIDE = new SimpleDateFormat("d-MMM");

        // register this as the default format for reports and UIs
        EVSchedule.DATE_FORMATTER = DATE_FORMAT_OVERRIDE;
    }

    public void run() {
        // Implementing this allows this class to register itself via the
        // background-task extension point. But we never actually run. Instead,
        // all our initialization work is done in the setConfigElement method
        // (which is executed by the BackgroundTaskManager on startup).
    }

}
