// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package pspdash.data.js;

import pspdash.data.DataListener;
import pspdash.data.DataRepository;
import pspdash.data.Repository;
import pspdash.data.SaveableData;



public class JSRepository implements Repository {

    DataRepository r;

    public JSRepository(DataRepository data) {
        this.r = data;
    }

    public void addDataListener(String name, DataListener dl) {
        r.addDataListener(name, dl);
    }

    public void maybeCreateValue(String name, String value, String prefix) {
        r.maybeCreateValue(name, value, prefix);
    }

    public void putValue(String name, SaveableData value) {
        if (DataInterpreter.RESTORE_DEFAULT_TOKEN.equals(value))
            r.restoreDefaultValue(name);
        else
            r.userPutValue(name, value);
    }

    public void removeDataListener(String name, DataListener dl) {
        r.removeDataListener(name, dl);
    }

    public void removeValue(String name) {
        r.removeValue(name);
    }

}
