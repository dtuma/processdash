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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC: processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

// TODO: move all reusable formatting/parsing logic into this class
// (e.g. dates, numbers, percentages, etc.)

public class FormatUtil {

    private static DateFormat DATE_FORMAT = DateFormat.getDateInstance();

    private static DateFormat DATE_TIME_FORMAT =
        DateFormat.getDateTimeInstance();

    public static void setDateFormats(String dateFormats,
                                      String dateTimeFormats) {
        DATE_FORMAT = new FlexibleDateFormat(dateFormats);
        DATE_TIME_FORMAT = new FlexibleDateFormat(dateTimeFormats);
    }


    public static String formatDateTime(Date d) {
        return DATE_TIME_FORMAT.format(d);
    }

    public static String formatDate(Date d) {
        return DATE_FORMAT.format(d);
    }

    public static Date parseDateTime(String s) {
        try {
            return DATE_TIME_FORMAT.parse(s);
        } catch (ParseException e) {
            return null;
        }
    }

    public static Date parseDate(String s) {
        try {
            return DATE_FORMAT.parse(s);
        } catch (ParseException e) {
            return null;
        }
    }

}
