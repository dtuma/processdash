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

import netscape.javascript.JSObject;

abstract public class FormCheckbox extends DataField {

    JSObject checkbox = null;
    boolean editable = true;

    private static final Double TRUE = new Double(1.0);
    private static final Double FALSE = new Double(0.0);

    private void debug(String msg) {
        System.out.println("FormCheckbox: " + msg);
    }

    public FormCheckbox(Repository r, JSObject o, String name,
                        String defaultValue, String prefix, boolean readOnly) {
        super(r, name, defaultValue, prefix, readOnly);
        this.checkbox = o;
    }

    public boolean isEditable() { return editable; }
    public void setEditable(boolean editable) {
        if (this.editable != editable) {
            this.editable = editable;
            if (checkbox != null)
                checkbox.setMember("isEditable", (editable ? TRUE : FALSE));
        }
    }

    public void setChecked(boolean checked) {
        if (checkbox != null)
            checkbox.setMember("checked", new Boolean(checked));
    }

    public boolean getChecked() {
        if (checkbox == null)
            return false;
        else
            return ((Boolean) checkbox.getMember("checked")).booleanValue();
    }
}

