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


import java.util.Enumeration;


/* a datalist that keeps a running value of the product of its element(s)
*/
class ProductList extends DataList {

    NumberFunction func;
    double value;

    public ProductList(DataRepository r, String dataName, String prefix,
                       NumberFunction f, String fName) {
        super(r, dataName, prefix, fName);
        func = f;

        recalc();
    }

    public void recalc() {
        if (func == null) return;

        value = 1.0;
        NumberData d;

        Enumeration values = dataList.elements();
        while (values.hasMoreElements()) {
            try {
                d = (NumberData) ((DataListValue)values.nextElement()).value;
            } catch (ClassCastException cce) { d = null; }

            if (d != null && !Double.isNaN(d.getDouble()))
                value *= d.getDouble();
            else if (re == null) {
                value = Double.NaN;
                break;
            }
        }

        func.recalc();
    }

    public String toString() {
        if (func != null)
            return "ProductList["+func.name()+"]";
        else
            return "ProductList[?]";
    }
}
