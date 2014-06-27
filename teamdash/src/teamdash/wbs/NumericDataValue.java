// Copyright (C) 2002-2014 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package teamdash.wbs;

import java.awt.Color;
import java.text.NumberFormat;
import java.text.ParseException;


/** Holds a numeric value and various attributes of that value.
 */
public class NumericDataValue {

    /** The numeric value */
    public double value;

    /** True if this value is editable */
    public boolean isEditable = true;

    /** True if this value should be hidden from the user */
    public boolean isInvisible = false;

    /** An optional error message describing why this value is erroneous.
     * <code>null</code> indicates this value is not erroneous. */
    public String errorMessage;

    /** The color to use when highlighting an error. This value should be
     * ignored if there is no error. */
    public Color errorColor = Color.red;

    /** If this value is erroneous, this is the value the validation
     * logic expected to see instead. */
    public double expectedValue;



    /** Create an editable, visible, non-erroneous numeric value. */
    public NumericDataValue(double v) {
        this(v, true, false, null, v);
    }

    /** Create a visible, non-erroneous numeric value. */
    public NumericDataValue(double v, boolean editable) {
        this(v, editable, false, null, v);
    }

    /** Create a numeric value.  The expected value will equal the value. */
    public NumericDataValue(double v, boolean editable, boolean invisible,
                            String error) {
        this(v, editable, invisible, error, v);
    }

    /** Create a numeric value. */
    public NumericDataValue(double v, boolean editable, boolean invisible,
                            String error, double expected)
    {
        this.value = v;
        this.isEditable = editable;
        this.isInvisible = invisible;
        this.errorMessage = error;
        this.expectedValue = expected;
    }



    /** Format this numeric value for display. */
    public String toString() {
        return format(value);
    }



    /** Format a double value for display. */
    public static String format(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value))
            return "";
        else if (value > 1)
            return FORMATTER.format(value);
        else
            return FORMATTER2.format(value);
    }


    /** Parse an object as a number */
    public static double parse(Object aValue) {

        if (aValue instanceof NumericDataValue)
            return ((NumericDataValue) aValue).value;
        if (aValue instanceof Number)
            return ((Number) aValue).doubleValue();
        if (aValue == null || "".equals(aValue))
            return 0;
        try {
            return FORMATTER.parse(aValue.toString()).doubleValue();
        } catch (ParseException nfe) { }

        return Double.NaN;
    }


    protected static final NumberFormat FORMATTER =
        NumberFormat.getNumberInstance();
    protected static final NumberFormat FORMATTER2 =
            NumberFormat.getNumberInstance();
    static {
        FORMATTER.setMaximumFractionDigits(1);
        FORMATTER2.setMaximumFractionDigits(2);
    }

}
