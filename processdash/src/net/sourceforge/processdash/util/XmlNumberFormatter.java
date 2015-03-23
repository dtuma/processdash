// Copyright (C) 2005 Tuma Solutions, LLC
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

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;

/** General purpose formatter for reading/writing double values to xml.
 * 
 * When persisting data to XML, it is important to ensure round-trip
 * capability of the data.  None of the standard mechanisms for reading
 * and writing double values fit this bill very well.  In particular:
 * <ul>
 *   <li>The <tt>Double</tt> class is not symmetric in its handling of
 *       NaN and Infinity.  It will format these numbers to specific
 *       string values, but won't recognize those same string values
 *       when it is time to parse the value back into a double.</li>
 *   <li>The <tt>Double</tt> class generates needlesly verbose strings
 *       for rational numbers, like 1.33333333333333.  Also, due to
 *       floating point round off problems, some numbers which the user
 *       would recognize as an integer instead show up as a number like
 *       41.99999982141.  It would be nice to generate cleaner output.</li>
 *   <li>The NumberFormat classes do a good job of generating clean output,
 *       but handle NaN and Infinity in odd ways.  For example, NaN is
 *       encoded as the unicode "unknown" character.  We need better
 *       round-trip support than that.</li>
 *   <li>In addition, casual use of a number formatter can lead to subtle
 *       bugs, such as when grouping separators appear, or when data is
 *       written in one locale and read back in another.  This class
 *       specifically handles those cases.</li>
 * </ul>
 */
public class XmlNumberFormatter extends NumberFormat {

    NumberFormat delegate;

    public XmlNumberFormatter() {
        this(3);
    }

    public XmlNumberFormatter(int maxNumDigits) {
        delegate = NumberFormat.getNumberInstance(Locale.US);
        delegate.setMaximumFractionDigits(maxNumDigits);
        delegate.setGroupingUsed(false);
    }

    public Number parse(String source, ParsePosition parsePosition) {
        if (source == null || source.length() <= parsePosition.getIndex())
            return null;

        if (source.startsWith(NAN_STR, parsePosition.getIndex())) {
            parsePosition.setIndex(parsePosition.getIndex() + NAN_STR.length());
            return new Double(Double.NaN);
        } else if (source.startsWith(P_INF_STR, parsePosition.getIndex())) {
            parsePosition.setIndex(parsePosition.getIndex()
                    + P_INF_STR.length());
            return new Double(Double.POSITIVE_INFINITY);
        } else if (source.startsWith(N_INF_STR, parsePosition.getIndex())) {
            parsePosition.setIndex(parsePosition.getIndex()
                    + N_INF_STR.length());
            return new Double(Double.NEGATIVE_INFINITY);
        } else
            return delegate.parse(source, parsePosition);
    }

    public StringBuffer format(double number, StringBuffer toAppendTo,
            FieldPosition pos) {
        if (Double.isNaN(number) || Double.isInfinite(number))
            return toAppendTo.append(Double.toString(number));
        else
            return delegate.format(number, toAppendTo, pos);
    }

    public StringBuffer format(long number, StringBuffer toAppendTo,
            FieldPosition pos) {
        return toAppendTo.append(Long.toString(number));
    }

    private static final String NAN_STR = Double.toString(Double.NaN);

    private static final String P_INF_STR = Double
            .toString(Double.POSITIVE_INFINITY);

    private static final String N_INF_STR = Double
            .toString(Double.NEGATIVE_INFINITY);

}
