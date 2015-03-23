// Copyright (C) 2006 Tuma Solutions, LLC
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
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/** An implementation of the Set interface that is optimized for memory
 * usage, rather than for performance.
 * 
 * Note that when the number of items in the Set is fairly small, and the
 * equals() method of the contained objects is fairly efficient, this class
 * still provides very good performance.  Yet it can require about 1/4 the
 * memory of a HashSet.
 */
public class LightweightSet extends ArrayList implements Set {

    public LightweightSet() {
        super(1);
    }

    public LightweightSet(Collection c) {
        super(c.size());
        addAll(c);
    }

    // We must modify the semantics of several methods to conform to
    // the contract for java.util.Set

    public boolean add(Object o) {
        if (contains(o))
            return false;
        else
            return super.add(o);
    }

    public boolean addAll(Collection c) {
        boolean result = false;
        for (Iterator i = c.iterator(); i.hasNext();)
            if (add(i.next()))
                result = true;
        return result;
    }

    // Don't support the following optional java.util.List operations, as they
    // could lead to violations of the java.util.Set interface.

    public void add(int index, Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(int index, Collection c) {
        throw new UnsupportedOperationException();
    }

    public Object set(int index, Object o) {
        throw new UnsupportedOperationException();
    }
}

