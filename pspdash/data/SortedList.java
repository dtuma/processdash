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
import java.util.TreeSet;
import java.util.Comparator;
import java.util.Map;
import java.util.Iterator;

class SortedList extends DataList {

    public SortedList(DataRepository r, String dataName, String prefix,
                      String fName) {
        super(r, dataName, prefix, fName);
    }

    private class Sorter implements Comparator {
        public int compare(Object o1, Object o2) {
            Map.Entry e1 = (Map.Entry) o1, e2 = (Map.Entry) o2;
            DataListValue v1 = (DataListValue) e1.getValue(),
                v2 = (DataListValue) e2.getValue();
            int result = DataComparator.instance.compare(v1.value, v2.value);
            if (result == 0)
                result = ((String) e1.getKey()).compareTo(e2.getKey());
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
