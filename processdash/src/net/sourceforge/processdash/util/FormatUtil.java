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
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Date;


// TODO: move all reusable formatting/parsing logic into this class
// (e.g. dates, numbers, percentages, etc.)

public class FormatUtil {

    /*
     * fields and methods for formatting dates
     */

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


    /*
     * fields and methods for formatting decimals
     */

    public static final int AUTO_DECIMAL = AdaptiveNumberFormat.AUTO_DECIMAL;

    private static AdaptiveNumberFormat NUMBER_FORMAT =
        new AdaptiveNumberFormat(NumberFormat.getNumberInstance(), 3);

    public static String formatNumber(double value) {
        return formatNumber(value, AUTO_DECIMAL);
    }

    public static String formatNumber(double value, int numDecimalPoints) {
        return NUMBER_FORMAT.format(value, numDecimalPoints);
    }

    public static double parseNumber(String number) throws ParseException {
        return NUMBER_FORMAT.parse(number.trim()).doubleValue();
    }


    /*
     * fields and methods for formatting percentages
     */

    private static AdaptiveNumberFormat PERCENT_FORMAT =
        new AdaptiveNumberFormat(NumberFormat.getPercentInstance(), 3);

    public static String formatPercent(double percent) {
        return formatPercent(percent, AUTO_DECIMAL);
    }
    public static String formatPercent(double percent, int digits) {
        return PERCENT_FORMAT.format(percent, digits);
    }

    public static double parsePercent(String percent) throws ParseException {
        try {
            return PERCENT_FORMAT.parse(percent.trim()).doubleValue();
        } catch (Exception e) {}
        return NUMBER_FORMAT.parse(percent.trim()).doubleValue() / 100;
    }

}
