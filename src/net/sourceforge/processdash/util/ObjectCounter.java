// Copyright (C) 2011 Tuma Solutions, LLC
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjectCounter<T> {

    private Map<T, Count> counts = new HashMap();

    public void add(T obj) {
        Count c = counts.get(obj);
        if (c == null) {
            counts.put(obj, new Count(obj));
        } else {
            c.count++;
        }
    }

    public int getCount(T obj) {
        Count c = counts.get(obj);
        return (c == null ? 0 : c.count);
    }

    public List<T> getMostCommonObjects() {
        int highestCount = 0;
        for (Count c : counts.values()) {
            if (c.count > highestCount) {
                highestCount = c.count;
            }
        }

        if (highestCount == 0)
            return Collections.EMPTY_LIST;

        List<T> result = new ArrayList();
        for (Count c : counts.values())
            if (c.count == highestCount)
                result.add((T) c.obj);
        return result;
    }

    private static class Count {
        Object obj;

        int count;

        Count(Object obj) {
            this.obj = obj;
            this.count = 1;
        }
    }

}
