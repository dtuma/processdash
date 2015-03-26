// Copyright (C) 2003-2007 Tuma Solutions, LLC
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
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.i18n;

import java.util.Comparator;


public class TranslationSorter implements Comparator {

    String[] PREFERRED_ITEMS = {
            "(Resources)",
            "ProcessDashboard",
            "HierarchyEditor",
            "Time",
            "Defects",
            "EV",
        };

    public int compare(Object o1, Object o2) {
        String s1 = (String) o1;
        String s2 = (String) o2;
        int i1 = indexOf(s1);
        int i2 = indexOf(s2);
        if (i1 != i2) return i1 - i2;
        return String.CASE_INSENSITIVE_ORDER.compare(s1, s2);
    }

    private int indexOf(String s) {
        for (int i = 0; i < PREFERRED_ITEMS.length; i++) {
            if (s.startsWith(PREFERRED_ITEMS[i]))
                return i;
        }
        return PREFERRED_ITEMS.length;
    }

}