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

public class PercentDataField extends FormTextField {

    int numberOfDigits;

    PercentDataField(Repository r, JSObject o, String name, String defaultValue,
                     String dataPrefix, int digits, boolean readOnly) {
        super(r, o, name, defaultValue, dataPrefix, readOnly);
        numberOfDigits = digits;
    }

    private void debug(String msg) {
        System.out.println("PercentDataField: " + msg);
    }

    void paint() {
        DoubleData d = (DoubleData)value;
        if (d == null)
            setText("");
        else {
            d = (DoubleData)d.getSimpleValue();
            d.value *= 100.0;
            setText(d.formatNumber(numberOfDigits));
        }
    }

    void parse() throws MalformedValueException {
        DoubleData d = new DoubleData(getText());
        d.value /= 100.0;
        value = d;
    }
}
