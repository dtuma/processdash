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


public class ImmutableStringData extends StringData {
    private String value;

    public ImmutableStringData(String val, boolean editable, boolean defined) {
        super();
        value = val;
        super.setEditable(editable);
        super.setDefined(defined);
    }

    public ImmutableStringData(String s) {
        super();
        value = s;
    }

    // Overwrite StringData's definition of this function, to instead
    // use our "value" member (which is private and hence immutable).
    public String getString()   { return value; }

    // Overwrite StringData's definition of these functions with no-ops.
    public void setEditable(boolean e) {}
    public void setDefined(boolean d) {}

    public SimpleData getSimpleValue() { return this; }

    public SaveableData getEditable(boolean editable) {
        if (editable == isEditable()) return this;
        if (otherEditable != null) return otherEditable;
        ImmutableStringData other =
            new ImmutableStringData(value, editable, isDefined());
        other.otherEditable = this;
        otherEditable = other;
        return other;
    }

    private ImmutableStringData otherEditable = null;
}
