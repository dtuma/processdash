// Copyright (C) 2001-2003 Tuma Solutions, LLC
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


import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import net.sourceforge.processdash.data.DataComparator;
import net.sourceforge.processdash.data.repository.DataRepository;


class SortedList extends DataList {

    Comparator nodeComparator;

    private static Hashtable cache = new Hashtable();

    public static SortedList getInstance
        (DataRepository r, String dataName, String prefix,
         String fName, Comparator nodeComparator)
    {
        String cacheKey = prefix + dataName;
        SortedList result = (SortedList) cache.get(cacheKey);
        if (result == null)         // no cached list? get a synchronization lock
            synchronized(SortedList.class) {
                // now that we have the lock, check to see if there STILL is no
                // cached list. (The person who had the lock before us might have
                // created and cached a list.)
                result = (SortedList) cache.get(cacheKey);
                if (result == null) {
                    // okay, we *really* do need to create a new list, and then cache it.
                    result = new SortedList(r, dataName, prefix, fName, nodeComparator);
                    cache.put(cacheKey, result);
                }
            }
        return result;
    }

    private SortedList(DataRepository r, String dataName, String prefix,
                       String fName, Comparator nodeComparator) {
        super(r, dataName, prefix, fName);
        this.nodeComparator = nodeComparator;
    }

    private class Sorter implements Comparator {
        public int compare(Object o1, Object o2) {
            Map.Entry e1 = (Map.Entry) o1, e2 = (Map.Entry) o2;
            DataListValue v1 = (DataListValue) e1.getValue(),
                v2 = (DataListValue) e2.getValue();
            int result = DataComparator.getInstance().compare(v1.value, v2.value);
            if (result == 0)
                result = nodeComparator.compare((String) e1.getKey(),
                                                (String) e2.getKey());
            if (result == 0)
                result = ((String) e1.getKey()).compareTo((String) e2.getKey());
            return result;
        }
        public boolean equals(Object obj) { return (obj == this); }
    }

    private final Sorter sorter = new Sorter();

    public String[] getNames() {
        TreeSet t = new TreeSet(sorter);
        t.addAll(dataList.entrySet());
        String [] result = new String[dataList.size()];
        Iterator iter = t.iterator();
        int i = 0;
        while (iter.hasNext())
            result[i++] = (String) ((Map.Entry) iter.next()).getKey();
        return result;
    }

    private static final String[] stringArrayType = new String[1];

    public void recalc() {  }
}
