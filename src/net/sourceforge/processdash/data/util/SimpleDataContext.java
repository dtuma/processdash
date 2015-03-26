// Copyright (C) 2008 Tuma Solutions, LLC
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

package net.sourceforge.processdash.data.util;

import java.util.HashMap;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;

public class SimpleDataContext extends HashMap implements DataContext {

    public SimpleData getSimpleValue(String name) {
        Object result = get(name);
        if (result instanceof SimpleData) {
            return (SimpleData) result;

        } else if (result instanceof SaveableData) {
            SaveableData sd = (SaveableData) result;
            return sd.getSimpleValue();

        } else {
            return null;
        }
    }

    public SaveableData getValue(String name) {
        Object result = get(name);
        if (result instanceof SaveableData)
            return (SaveableData) result;
        else
            return null;
    }

    public void putValue(String name, SaveableData value) {
        put(name, value);
    }

}
