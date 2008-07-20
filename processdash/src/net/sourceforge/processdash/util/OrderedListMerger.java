// Copyright (C) 2007 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class OrderedListMerger {

    public static List merge(Collection listsToMerge) {
        if (listsToMerge == null)
            return Collections.EMPTY_LIST;

        List lists = new ArrayList(listsToMerge);
        // order the lists so the longest ones appear first.
        Collections.sort(lists, new Comparator() {
            public int compare(Object o1, Object o2) {
                List l1 = (List) o1;
                List l2 = (List) o2;
                return l2.size() - l1.size();
            }
        });

        // remove empty lists and sublists
        Set allElements = new LinkedHashSet();
        for (Iterator i = lists.iterator(); i.hasNext();) {
            List oneList = (List) i.next();
            if (oneList.isEmpty() || allElements.containsAll(oneList))
                i.remove();
            allElements.addAll(oneList);
        }

        // handle the trivial (and most common) cases
        if (lists.isEmpty())
            return Collections.EMPTY_LIST;
        if (lists.size() == 1)
            return (List) lists.get(0);

        List result = new ArrayList(allElements.size());
        Iterator i = lists.iterator();
        result.addAll((List) i.next());

        while (i.hasNext())
            merge(result, (List) i.next());

        return result;
    }

    private static void merge(List a, List b) {
        List shared = new ArrayList(b);
        shared.retainAll(a);

        for (Iterator i = shared.iterator(); i.hasNext();) {
            Object element = i.next();
            int aPos = a.indexOf(element);
            int bPos = b.indexOf(element);
            a.addAll(aPos, b.subList(0, bPos));
            b = b.subList(bPos + 1, b.size());
        }

        a.addAll(b);
    }



}
