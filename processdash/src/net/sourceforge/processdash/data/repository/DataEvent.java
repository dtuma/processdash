// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003-2006 Software Process Dashboard Initiative
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
