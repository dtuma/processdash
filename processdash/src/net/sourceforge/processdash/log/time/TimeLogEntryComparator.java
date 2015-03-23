// Copyright (C) 2005 Tuma Solutions, LLC
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

package net.sourceforge.processdash.log.time;

import java.util.Comparator;


public class TimeLogEntryComparator implements Comparator {

    public static final Comparator INSTANCE = new TimeLogEntryComparator();

    public int compare(Object o1, Object o2) {
        if (o1 == o2)
            return 0;

        TimeLogEntry a = (TimeLogEntry) o1;
        TimeLogEntry b = (TimeLogEntry) o2;

        int result = compareObjects(a.getStartTime(), b.getStartTime());
        if (result != 0) return result;

        result = compareObjects(a.getPath(), b.getPath());
        if (result != 0) return result;

        return compareLongs(a.getID(), b.getID());
    }

    private static int compareObjects(Comparable a, Comparable b) {
        if (a == b)
            return 0;
        else if (a == null)
            return -1;
        else if (b == null)
            return +1;
        else
            return a.compareTo(b);
    }

    private static int compareLongs(long a, long b) {
        if (a < b)
            return -1;
        else if (a > b)
            return +1;
        else
            return 0;
    }
}
