// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil

package pspdash.data;

import java.lang.Math;


/* This class is the basis for all numeric data stored in the repository */
public class DoubleData implements SimpleData, NumberData {

    double value;
    boolean defined = true;
    boolean editable = true;

    public DoubleData() {}

    public DoubleData(double v, boolean e) { value = v;   editable = e; }

    public DoubleData(String s) throws MalformedValueException {
        if (s == null || s.length() == 0)
            throw new MalformedValueException();
        try {
            if (s.charAt(0) == '?') {
                defined = false;
                s = s.substring(1);
            }
            // NOTE: we cannot use the Double.parseDouble method here, because
            // it was introduced in Java 1.2;  this class must run inside
            // Internet Explorer, which only provides Java 1.1 support.
            value = Double.valueOf(s).doubleValue();
        } catch (Exception e) {
            // why aren't the Double.toString and the Double.valueOf methods
            // symmetric?
            if      (NAN_STR.equals(s))   value = Double.NaN;
            else if (P_INF_STR.equals(s)) value = Double.POSITIVE_INFINITY;
            else if (N_INF_STR.equals(s)) value = Double.NEGATIVE_INFINITY;
            else
                throw new MalformedValueException();
        }
    }
    static final String NAN_STR   = Double.toString(Double.NaN);
    static final String P_INF_STR = Double.toString(Double.POSITIVE_INFINITY);
    static final String N_INF_STR = Double.toString(Double.NEGATIVE_INFINITY);

    public DoubleData(int i)    { value = (double)i; }
    public DoubleData(double d) { value = d; }
    public String saveString()  {
        return (isDefined() ? "" : "?") + Double.toString(getDouble());
    }
    public double getDouble()   { return value; }
    public int getInteger()     { return (int)getDouble(); }
    public boolean isEditable() { return editable; }
    public void setEditable(boolean e) { editable = e; }
    public boolean isDefined() { return defined; }
    public void setDefined(boolean d) { defined = d; }
    public void dispose() {};

    public SimpleData getSimpleValue() {
        DoubleData result = new DoubleData(getDouble(), isEditable());
        result.defined = defined;
        return result;
    }

    public static final int AUTO_DECIMAL = -1;

    public static final String NAN_STRING = "#VALUE!";
    public static final String INF_STRING = "#DIV/0!";
    public static String formatNumber(double value, int numDecimalPoints) {
        if (Double.isNaN(value))      return NAN_STRING;
        if (Double.isInfinite(value)) return INF_STRING;

        if (numDecimalPoints == AUTO_DECIMAL)
            if (value-((int)value) == 0.0 || value <= -100 || value >= 100)
                numDecimalPoints = 0;
            else if (value > -10 && value < 10)
                numDecimalPoints = 2;
            else
                numDecimalPoints = 1;

        /*
         * round off value appropriately. Numbers are rounded away from zero,
         * to the number of digits specified by numDecimalPoints.
         */
        value += (value>=0 ? 0.5 : -0.5) * Math.pow(0.1, numDecimalPoints);

        if (numDecimalPoints == 0)
            return Integer.toString((int)value);

                                  // First, calculate the fractional part of
                                  // the value, and make a string from it.
        StringBuffer fraction =
            new StringBuffer(Integer.toString
                             ((int)(Math.abs(value - (int)value) *
                                    Math.pow(10.0, numDecimalPoints))));

                                  // pad the front of the string with the
                                  // appropriate number of zeros.
        int numPadZeros = numDecimalPoints - fraction.length();
        while (numPadZeros-- > 0)
            fraction.insert(0, '0');

                                // if the value ends with several zeros, they
                                // are unnecessary, and can be removed.
        int length = fraction.length();
        while (length > 1 && (fraction.charAt(length-1) == '0'))
            fraction.setLength(--length);

        StringBuffer result = new StringBuffer();
        if (value < 0.0) {
            result.append("-");
            value = value * -1.0;
        }
        result.append((int) value).append(".").append(fraction.toString());
        return result.toString();
    }

    public String formatNumber(int numDecimalPoints) {
        return formatNumber(getDouble(), numDecimalPoints);
    }

    public String toString() {
        return Double.toString(getDouble());
    }

    public String format() {
        return formatNumber(getDouble());
    }

    public static String formatNumber(double value) {
        return formatNumber(value, AUTO_DECIMAL);
    }

    public SimpleData parse(String val) throws MalformedValueException {
        return (val == null || val.length() == 0) ? null : new DoubleData(val);
    }
    public boolean equals(SimpleData val) {
        return ((val instanceof DoubleData) &&
                (getDouble() == ((DoubleData)val).getDouble()));
    }
    public boolean lessThan(SimpleData val) {
        return ((val instanceof DoubleData) &&
                (getDouble() < ((DoubleData)val).getDouble()));
    }
    public boolean greaterThan(SimpleData val) {
        return ((val instanceof DoubleData) &&
                (getDouble() > ((DoubleData)val).getDouble()));
    }
    public boolean test() {
        // should NaN and Infinity count as true values?
        return (getDouble() != 0.0 &&
                !Double.isNaN(value) &&
                !Double.isInfinite(value));
    }

    public SaveableData getEditable(boolean editable) {
        SimpleData result = getSimpleValue();
        result.setEditable(editable);
        return result;
    }
}
