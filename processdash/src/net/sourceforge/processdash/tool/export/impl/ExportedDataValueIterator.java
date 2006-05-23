// Copyright (C) 2005-2006 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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

package net.sourceforge.processdash.tool.export.impl;

import java.util.Collection;

import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.Filter;
import net.sourceforge.processdash.util.IteratorFilter;

public class ExportedDataValueIterator extends IteratorFilter {

    DataRepository data;

    Collection prefixes;

    public ExportedDataValueIterator(DataRepository data, Collection prefixes) {
        this(data, prefixes, true);
    }

    protected ExportedDataValueIterator(DataRepository data,
            Collection prefixes, boolean init) {
        super(data.getKeys(prefixes, null));
        this.data = data;
        this.prefixes = prefixes;
        if (init)
            init();
    }

    protected boolean includeInResults(Object o) {
        return Filter.matchesFilter(prefixes, (String) o);
    }

    public Object next() {
        String name = (String) super.next();
        return new DataValue(name);
    }

    private class DataValue implements ExportedDataValue {

        private String name;

        public DataValue(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public SimpleData getSimpleValue() {
            return data.getSimpleValue(name);
        }

    }

}
