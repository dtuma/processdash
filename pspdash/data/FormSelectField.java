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

import java.util.Vector;
import netscape.javascript.JSObject;

abstract public class FormSelectField extends DataField {

    JSObject selectField = null;
    boolean editable = true;
    Vector optionList = null;

    private static final Double TRUE = new Double(1.0);
    private static final Double FALSE = new Double(0.0);

    private void debug(String msg) {
        System.out.println("FormSelectField: " + msg);
    }

    public FormSelectField(Repository r, JSObject o, String name,
                           String defaultValue, String prefix,
                           boolean readOnly) {
        super(r, name, defaultValue, prefix, readOnly);
        this.selectField = o;
        optionList = new Vector();

        if (noConnection) {
            setEditable(false);
            return;
        }

                                    // fill the optionList value with the various
                                    // OPTIONs that are a part of this SELECT tag.
        JSObject formOptions = (JSObject) o.getMember("options");
        JSObject option;
        String element;
        int numOptions = ((Double)formOptions.getMember("length")).intValue();
        for (int optIdx = 0;   optIdx < numOptions;   optIdx++)
            optionList.addElement(getOptionValue(formOptions, optIdx));
    }

    public boolean isEditable() { return editable; }
    public void setEditable(boolean editable) {
        if (this.editable != editable) {
            this.editable = editable;
            if (selectField != null)
                selectField.setMember("isEditable", (editable ? TRUE : FALSE));
        }
    }

    public void setSelection(String text) {
        if (selectField != null) {
            for (int idx = optionList.size();   idx-- > 0; )
                if (text.equals((String)optionList.elementAt(idx))) {
                    selectField.setMember("selectedIndex", new Double(idx));
                    return;
                }
        }
    }

    public String getSelection() {
        if (selectField == null)
            return "";

        int idx = ((Double) selectField.getMember("selectedIndex")).intValue();
        if (idx >= optionList.size())
            return "";

        return (String) optionList.elementAt(idx);
    }

    public static String getSelection(JSObject selectElement) {
        if (selectElement == null)
            return "";

        int idx = ((Double) selectElement.getMember("selectedIndex")).intValue();
        JSObject optList = (JSObject) selectElement.getMember("options");
        return getOptionValue(optList, idx);
    }

    private static String getOptionValue(JSObject optionList, int idx) {
        JSObject option = (JSObject) optionList.getSlot(idx);
        String result = (String) option.getMember("value");
        if (result.length() == 0)
            result = (String) option.getMember("text");

        return result;
    }

}

