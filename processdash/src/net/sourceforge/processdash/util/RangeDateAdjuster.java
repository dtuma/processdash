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

package net.sourceforge.processdash.util;

import java.util.Date;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

public class RangeDateAdjuster implements DateAdjuster {

    private SortedSet<Adjustment> adjustments;

    public RangeDateAdjuster() {
        this.adjustments = new TreeSet<Adjustment>();
    }

    public void add(Date adjustFrom, Date adjustTo) {
        long delta = adjustTo.getTime() - adjustFrom.getTime();
        add(adjustFrom, delta);
    }

    public void add(Date d, long adjustment) {
        adjustments.add(new Adjustment(d, adjustment));
    }

    public Date adjust(Date d) {
        if (d == null || adjustments.isEmpty())
            return d;

        Iterator<Adjustment> i = adjustments.iterator();
        Adjustment adj = i.next();

        while (i.hasNext()) {
            Adjustment a = i.next();
            if (a.matches(d))
                adj = a;
            else
                break;
        }

        return adj.adjust(d);
    }



    private static class Adjustment implements Comparable<Adjustment> {

        Date start;

        long offset;

        private Adjustment(Date start, long offset) {
            this.start = start;
            this.offset = offset;
        }

        public boolean matches(Date d) {
            return start.compareTo(d) <= 0;
        }

        public Date adjust(Date d) {
            if (offset == 0)
                return d;

            long time = d.getTime();
            time += offset;
            return new Date(time);
        }

        public int compareTo(Adjustment that) {
            return this.start.compareTo(that.start);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Adjustment) {
                Adjustment that = (Adjustment) obj;
                return this.start.equals(that.start);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return start.hashCode();
        }

    }

}
