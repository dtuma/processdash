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


class IfList extends DataList {

    public static final int LOGICAL_AND = 1;
    public static final int LOGICAL_OR  = 2;

    NumberFunction func;
    double value;
    int combinationOperator;

    public IfList(DataRepository r, String dataName, String prefix,
                   NumberFunction f, String fName, int combinationOperator) {
        super(r, dataName, prefix, fName);
        func = f;
        this.combinationOperator = combinationOperator;

        recalc();
    }

    public void recalc() {
        if (func == null) return;

        value = (combinationOperator == LOGICAL_AND ? 1.0 : 0.0);
        SimpleData d;
        boolean b;

        Enumeration values = dataList.elements();
        while (values.hasMoreElements()) {
            d = ((DataListValue)values.nextElement()).value;

                                      // null data elements are false.
            if (d == null) b = false;

                                      // numerically zero elements are false.
            else if (d instanceof NumberData &&
                     ((NumberData)d).getDouble() == 0.0) b = false;

                                      // empty strings are false.
            else if (d instanceof StringData &&
                     ((StringData)d).getString().length() == 0) b = false;

                else b = true;		// everything else is true.

            if  (combinationOperator == LOGICAL_OR) {
                if (b)  { value = 1.0; break; }
            } else {
                if (!b) { value = 0.0; break; }
            }
        }

        func.recalc();
    }

    public String toString() {
        if (func != null)
            return "IfList["+func.name()+"]";
        else
            return "IfList[?]";
    }
}
