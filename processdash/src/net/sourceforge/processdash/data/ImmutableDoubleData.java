// Copyright (C) 2001-2003 Tuma Solutions, LLC
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



public class ImmutableDoubleData extends DoubleData {

    public static final ImmutableDoubleData READ_ONLY_ZERO =
        new ImmutableDoubleData(0.0, false, true);
    public static final ImmutableDoubleData EDITABLE_ZERO =
        (ImmutableDoubleData) READ_ONLY_ZERO.getEditable(true);
    public static final ImmutableDoubleData EDITABLE_UNDEF_NAN =
        new ImmutableDoubleData(Double.NaN, true, false);
    public static final ImmutableDoubleData READ_ONLY_NAN =
        new ImmutableDoubleData(Double.NaN, false, true);

    public static final ImmutableDoubleData TRUE =
        new ImmutableDoubleData(1.0, false, true);
    public static final ImmutableDoubleData FALSE = READ_ONLY_ZERO;

    public static final ImmutableDoubleData BAD_VALUE = READ_ONLY_NAN;
    public static final ImmutableDoubleData DIVIDE_BY_ZERO =
        new ImmutableDoubleData(Double.POSITIVE_INFINITY, false, true);

    private double value;

    public ImmutableDoubleData(double val, boolean editable, boolean defined) {
        super(val, editable);
        super.setDefined(defined);
        value = super.value;
    }

    public ImmutableDoubleData(String s) throws MalformedValueException {
        super(s);
        value = super.value;
    }

    // Overwrite DoubleData's definition of this function, to instead
    // use our "value" member (which is private and hence immutable).
    public double getDouble()   { return value; }

    // Overwrite DoubleData's definition of these functions with no-ops.
    public void setEditable(boolean e) {}
    public void setDefined(boolean d) {}

    public SimpleData getSimpleValue() { return this; }

    public SaveableData getEditable(boolean editable) {
        if (editable == isEditable()) return this;
        if (otherEditable != null) return otherEditable;
        ImmutableDoubleData other =
            new ImmutableDoubleData(value, editable, isDefined());
        other.otherEditable = this;
        otherEditable = other;
        return other;
    }

    private ImmutableDoubleData otherEditable = null;
}
