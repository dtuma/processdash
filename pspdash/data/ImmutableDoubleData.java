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
