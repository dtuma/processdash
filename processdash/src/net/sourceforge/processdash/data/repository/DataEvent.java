// Copyright (C) 2000-2006 Tuma Solutions, LLC
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

package net.sourceforge.processdash.data.repository;

import java.util.EventObject;

import net.sourceforge.processdash.data.SimpleData;

public class DataEvent extends EventObject implements java.io.Serializable {

    public static final int VALUE_CHANGED = 3;

    protected String name;

    protected int id;

    protected SimpleData value;

    public DataEvent(Repository dataRepository, String name, int id,
            SimpleData value) {
        super(dataRepository);
        this.name = name;
        this.id = id;
        this.value = value;
    }

    public int getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public SimpleData getValue() {
        return value;
    }
}
