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

class DivisionFunction extends DoubleData implements NumberFunction {

    double multiplier = 1.0;
    String name = null;
    String saveValue = null;
    ProductList numerator = null;
    Vector datalists;
    DataRepository data;


    public DivisionFunction(String name, String params, String saveAs,
                            DataRepository r, String prefix) {
        super(1.0);
        setEditable(false);
        data = r;
        saveValue = saveAs;
        datalists = new Vector();

        ArgumentList args = new ArgumentList(params);
        String parameter;

        parameter = args.nextElement();
        try {
            multiplier *= new Integer(parameter).doubleValue();
        } catch (NumberFormatException ie) { try {
            multiplier *= new Double(parameter).doubleValue();
        } catch (NumberFormatException de) {
            numerator = new ProductList(data, parameter, prefix, this, name);
        }}

        while (args.hasMoreElements()) {
            parameter = args.nextElement();
            try {
                multiplier /= new Integer(parameter).doubleValue();
            } catch (NumberFormatException ie) { try {
                multiplier /= new Double(parameter).doubleValue();
            } catch (NumberFormatException de) {
                datalists.addElement(new ProductList(data, parameter,
                                                     prefix, this, name));
            }}
        }

        this.name = name;
        recalc();
    }

    public void recalc() {
        if (name == null) return;

        value = multiplier;

        if (numerator != null)
            value *= numerator.value;

        for (int i = datalists.size();   i-- != 0; )
            value /= ((ProductList)datalists.elementAt(i)).value;

        data.putValue(name, this);
    }

    public String saveString() { return saveValue; }

    public void dispose() {
        name = saveValue = null;
        if (numerator != null) { numerator.dispose();  numerator = null; }
        if (datalists != null) {
            for (int i = datalists.size();   i-- > 0; )
                ((DataList)datalists.elementAt(i)).dispose();
            datalists = null;
        }
        data = null;
    }

    public String name() { return name; }
}
