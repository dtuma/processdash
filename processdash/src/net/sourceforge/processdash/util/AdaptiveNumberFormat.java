// Copyright (C) 2003 Tuma Solutions, LLC
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

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;


/** This class formats a number to display a certain desired number of
 * significant digits.
 *
 */
public class AdaptiveNumberFormat extends NumberFormat {

    private NumberFormat format;
    private int digitsOfPrecision;
    private int multiplier;

    public AdaptiveNumberFormat(NumberFormat format, int digits) {
        this.format = format;
        this.digitsOfPrecision = digits;
        format.setGroupingUsed(false);
        if (format instanceof DecimalFormat)
            multiplier = ((DecimalFormat) format).getMultiplier();
        else
            multiplier = 1;
    }

    public static final int AUTO_DECIMAL = -1;

    public static final String NAN_STRING = "#VALUE!";
    public static final String INF_STRING = "#DIV/0!";

    private static final Double NAN_DOUBLE = new Double(Double.NaN);
    private static final Double INF_DOUBLE =
        new Double(Double.POSITIVE_INFINITY);


    public String format(double number, int numDigits) {
        if (Double.isNaN(number))
            return NAN_STRING;
        if (Double.isInfinite(number))
            return INF_STRING;
        synchronized (format) {
            // treat negative zero like zero
            if (number == 0) number = 0;
            if (numDigits == AUTO_DECIMAL)
                setupDigits(number);
            else {
                format.setMinimumFractionDigits(1);
                format.setMaximumFractionDigits(numDigits);
            }
            if (number == 0) number = 0;
            return format.format(number);
        }
    }

    public StringBuffer format(double number,
                               StringBuffer toAppendTo,
                               FieldPosition pos) {
        // treat negative zero like zero
        if (Double.isNaN(number))
            return toAppendTo.append(NAN_STRING);
        if (Double.isInfinite(number))
            return toAppendTo.append(INF_STRING);
        else synchronized(format) {
            if (number == 0) number = 0;
            setupDigits(number);
            return format.format(number, toAppendTo, pos);
        }
    }

    private static final double LN10 = Math.log(10);
    private void setupDigits(double num) {
        num = Math.abs(num) * multiplier;
        if (num == Math.floor(num)) {
            format.setMinimumFractionDigits(0);
            format.setMaximumFractionDigits(0);
        } else {
            int numIntegerDigits;
            if (num < 10)
                numIntegerDigits = 1;
            else
                numIntegerDigits = 1 + (int) Math.floor(Math.log(num) / LN10);
            int numFractionDigits = digitsOfPrecision - numIntegerDigits;
            if (numFractionDigits < 1) {
                format.setMinimumFractionDigits(0);
                format.setMaximumFractionDigits(0);
            } else {
                format.setMinimumFractionDigits(1);
                format.setMaximumFractionDigits(numFractionDigits);
            }
        }
    }


    public synchronized StringBuffer format(long number,
                                            StringBuffer toAppendTo,
                                            FieldPosition pos) {
        format.setMaximumFractionDigits(0);
        return format.format(number, toAppendTo, pos);
    }


    public synchronized Number parse(String source,
                                     ParsePosition parsePosition) {
        if (NAN_STRING.equals(source)) {
            parsePosition.setIndex(source.length());
            return NAN_DOUBLE;
        } if (INF_STRING.equals(source)) {
            parsePosition.setIndex(source.length());
            return INF_DOUBLE;
        }

        return format.parse(source, parsePosition);
    }


    public synchronized Number parse(String source) throws ParseException {
        if (NAN_STRING.equals(source))
            return NAN_DOUBLE;
        else if (INF_STRING.equals(source))
            return INF_DOUBLE;
        else
            return format.parse(source);
    }

}
