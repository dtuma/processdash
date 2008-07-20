// Copyright (C) 2000-2003 Tuma Solutions, LLC
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

package net.sourceforge.processdash.data;

import net.sourceforge.processdash.util.FormatUtil;




/* This class is the basis for all numeric data stored in the repository */
public class DoubleData implements SimpleData, NumberData {

    protected double value;
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
    public static final String NAN_STR   = Double.toString(Double.NaN);
    public static final String P_INF_STR = Double.toString(Double.POSITIVE_INFINITY);
    public static final String N_INF_STR = Double.toString(Double.NEGATIVE_INFINITY);

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

    public String formatNumber(int numDecimalPoints) {
        return FormatUtil.formatNumber(getDouble(), numDecimalPoints);
    }

    public String toString() {
        return Double.toString(getDouble());
    }

    public String format() {
        return FormatUtil.formatNumber(getDouble());
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
        double value = getDouble();
        return (value != 0.0 &&
                !Double.isNaN(value) &&
                !Double.isInfinite(value));
    }

    public SaveableData getEditable(boolean editable) {
        SimpleData result = getSimpleValue();
        result.setEditable(editable);
        return result;
    }
}
