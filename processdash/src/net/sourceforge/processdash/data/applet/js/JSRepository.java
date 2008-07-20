// Copyright (C) 2003 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 3
// of the License, or (at your option) any later version.
//
// Additional permissions also apply; see the README-license.txt
// file in the project root directory for more information.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.data.applet.js;

import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.applet.DataInterpreter;
import net.sourceforge.processdash.data.repository.DataListener;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.data.repository.Repository;


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
