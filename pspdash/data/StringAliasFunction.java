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

class StringAliasFunction extends StringData implements DataListener {

    String myName = null, aliasOf = null;
    String saveValue = null;
    DataRepository data;


    public StringAliasFunction(String name, String params, String saveAs,
                               DataRepository r, String prefix) {
        super();
        editable = false;
        data = r;
        saveValue = saveAs;

        aliasOf = data.createDataName(prefix, params);
        data.addActiveDataListener(aliasOf, this, name);

        myName = name;
    }

    public void dataValuesChanged(Vector v) {
        if (v == null || v.size() == 0) return;
        for (int i = 0;  i < v.size();  i++)
            dataValueChanged((DataEvent) v.elementAt(i));
    }

    public void dataValueChanged(DataEvent e) {
        if (e.getName().equals(aliasOf)) try {
            StringData d = (StringData) e.getValue();
            value = d.getString();
            defined = d.isDefined();
        } catch (ClassCastException cce) {
            value = null;
            defined = true;
        }
        asList = null;

        if (myName != null)
            data.putValue(myName, this);
    }

    public String saveString() { return saveValue; }

    public void dispose() {
        myName = aliasOf = saveValue = null;
        if (data != null) data.deleteDataListener(this);
        data = null;
    }
}
