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

public class CheckboxDataField extends FormCheckbox {

    public static final int NUMBER = 1;
    public static final int DATE = 2;

    int type = NUMBER;

    CheckboxDataField(Repository r, JSObject o, String name, String defaultValue,
                      String dataPrefix, String flags, boolean readOnly) {
        super(r, o, name, defaultValue, dataPrefix, readOnly);

        if (flags.indexOf('d') != -1)
            type = DATE;		// This is a date checkbox.
    }

    void paint() {
        switch (type) {
        case NUMBER:
            DoubleData d = (DoubleData)value;
            setChecked(d != null && d.value != 0.0);
            break;

        case DATE:
            setChecked(value != null);
            break;
        }
    }

    void parse() {
        switch (type) {
        case NUMBER:
            value = new DoubleData(getChecked() ? 1.0 : 0.0);
            break;

        case DATE:
            if (!getChecked())	// only set the value to the current date
                value = null;		// if it was previously null (i.e., the
            else if (value == null)	// checkbox used to be unchecked and the user
                value = new DateData();	// just checked it.)
            break;
        }
    }
}



