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
    boolean editable = true;

    public DoubleData() {}

    public DoubleData(double v, boolean e) { value = v;   editable = e; }

    public DoubleData(String s) throws MalformedValueException {
        try {
            value = Integer.valueOf(s).doubleValue();
        } catch (NumberFormatException e) {
            try {
                value = Double.valueOf(s).doubleValue();
            } catch (NumberFormatException e2) {
                throw new MalformedValueException();
            }
        }
    }

    public DoubleData(int i)	{ value = (double)i; }
    public DoubleData(double d)	{ value = d; }
    public String saveString()	{ return Double.toString(value); }
    public double getDouble()	{ return value; }
    public int getInteger()	{ return (int)value; }
    public boolean isEditable()	{ return editable; }
    public void setEditable(boolean e) { editable = e; }
    public void dispose() {};

    public SimpleData getSimpleValue() {
        return new DoubleData(value, editable);
    }

    public static String formatNumber(double value, int numDecimalPoints) {
        if (Double.isNaN(value) || Double.isInfinite(value))
            return "ERROR";

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

        return (Integer.toString((int)value) + "." + fraction.toString());
    }

    public String formatNumber(int numDecimalPoints) {
        return formatNumber(value, numDecimalPoints);
    }

    public String toString() {
        return Double.toString(value);
    }

    public String format() {
        return formatNumber(value);
    }

    public static String formatNumber(double value) {
        return formatNumber(value, (value-((int)value)==0.0) ? 0 : 2);
    }

    public SimpleData parse(String val) throws MalformedValueException {
        return (val == null || val.length() == 0) ? null : new DoubleData(val);
    }
    public boolean equals(SimpleData val) {
        return ((val instanceof DoubleData) && (value==((DoubleData)val).value));
    }
    public boolean lessThan(SimpleData val) {
        return ((val instanceof DoubleData) && (value<((DoubleData)val).value));
    }
    public boolean greaterThan(SimpleData val) {
        return ((val instanceof DoubleData) && (value>((DoubleData)val).value));
    }
}
