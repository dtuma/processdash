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
import java.util.Enumeration;

class AndFunction extends DoubleData implements NumberFunction {

    String name = null;
    String saveValue = null;
    Vector datalists;
    DataRepository data;


    public AndFunction(String name, String params, String saveAs,
                       DataRepository r, String prefix) {
        super(0.0);
        setEditable(false);
        data = r;
        saveValue = saveAs;
        datalists = new Vector();

        ArgumentList args = new ArgumentList(params);
        String parameter;

        while (args.hasMoreElements()) {
            parameter = args.nextElement();
            datalists.addElement(new IfList(data, parameter, prefix, this,
                                            name, IfList.LOGICAL_AND));
        }

        this.name = name;
        recalc();
    }

    public void recalc() {
        if (name == null) return;

        double oldValue = value;
        value = 1.0;

        for (int i = datalists.size();   i-- != 0; )
            if (((IfList)datalists.elementAt(i)).value == 0.0) {
                value = 0.0;
                break;
            }

        // only update our value in the repository if our boolean
        // value really has changed.
        if (value != oldValue) data.putValue(name, this);
    }

    public String saveString() { return saveValue; }

    public void dispose() {
        name = saveValue = null;
        if (datalists != null) {
            for (int i = datalists.size();   i-- > 0; )
                ((DataList)datalists.elementAt(i)).dispose();
            datalists = null;
        }
        data = null;
    }

    public String name() { return name; }
}

