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

abstract public class FormTextField extends DataField {

    JSObject textField = null;
    boolean editable = true;

    private static final Double TRUE = new Double(1.0);
    private static final Double FALSE = new Double(0.0);

    private void debug(String msg) {
        System.out.println("FormTextField: " + msg);
    }

    public FormTextField(Repository r, JSObject o, String name,
                         String defaultValue, String prefix, boolean readOnly) {
        super(r, name, defaultValue, prefix, readOnly);
        this.textField = o;
        if (value != null)
            dataValueChanged(null);

        if (noConnection) {
            setEditable(false);
            setText("NO CONNECTION");
        }
    }

    public boolean isEditable() { return editable; }
    public void setEditable(boolean editable) {
        if (this.editable != editable) {
            this.editable = editable;
            if (textField != null)
                textField.setMember("isEditable", (editable ? TRUE : FALSE));
        }
    }

    public void setText(String text) {
        if (textField != null)
            textField.setMember("value", text);
    }

    public String getText() {
        if (textField == null)
            return "";
        else
            return (String) textField.getMember("value");
    }
}

