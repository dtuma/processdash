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

class AdditionFunction extends DoubleData implements NumberFunction {

    double offset = 0.0;
    String name = null;
    String saveValue = null;
    Vector datalists;
    DataRepository data;


    public AdditionFunction(String name, String params, String saveAs,
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
            try {
                offset += new Integer(parameter).doubleValue();
            } catch (NumberFormatException ie) { try {
                offset += new Double(parameter).doubleValue();
            } catch (NumberFormatException de) {
                datalists.addElement(new SumList(data, parameter,
                                                 prefix, this, name));
            }}
        }

        this.name = name;
        recalc();
    }

    public void recalc() {
        if (name == null) return;

        value = offset;

        for (int i = datalists.size();   i-- != 0; )
            value += ((SumList)datalists.elementAt(i)).value;

        data.putValue(name, this);
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

