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

package net.sourceforge.processdash.data;

import java.util.Comparator;

public class DataComparator implements Comparator {

    private DataComparator() {}

    private static final DataComparator INSTANCE = new DataComparator();

    public static DataComparator getInstance() {
        return INSTANCE;
    }


    /** Dates sort before numbers, which sort before strings, which
     *  sort before null values. */
    public int compare(Object o1, Object o2) {
        o1 = scrub(o1);    o2 = scrub(o2);
        if (o1 instanceof DateData) {
            if (o2 instanceof DateData) {
                DateData d1 = (DateData) o1, d2 = (DateData) o2;
                return d1.getValue().compareTo(d2.getValue());
            } else
                return -1;

        } else if (o1 instanceof NumberData) {
            if (o2 instanceof NumberData) {
                double n1 = ((NumberData) o1).getDouble(), n2 = ((NumberData) o2).getDouble();
                if (n1 < n2)
                    return -1;
                else if (n1 > n2)
                    return 1;
                else
                    return 0;
            } else
                return (o2 instanceof DateData) ? 1 : -1;

        } else if (o1 instanceof StringData) {
            if (o2 instanceof StringData) {
                StringData s1 = (StringData) o1, s2 = (StringData) o2;
                return s1.getString().compareTo(s2.getString());
            } else
                return (o2 == null) ? -1 : 1;

        } else
            return (o2 == null) ? 0 : 1;
    }

    private Object scrub(Object obj) {
        if (obj instanceof SaveableData && !(obj instanceof SimpleData))
            obj = ((SaveableData) obj).getSimpleValue();
        if (obj instanceof NumberData) return obj;
        if (obj instanceof DateData) return obj;
        if (obj instanceof StringData) return obj;
        return null;
    }

    public boolean equals(Object obj) {
        return (obj instanceof DataComparator);
    }
}
