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

package net.sourceforge.processdash.hier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import net.sourceforge.processdash.util.IteratorConcatenator;


public class PropertyKeyIterator implements Iterator {

    PropertyKeyHierarchy hier;

    Stack keyStack;

    public PropertyKeyIterator(PropertyKeyHierarchy hier, PropertyKey start) {
        this.hier = hier;
        this.keyStack = new Stack();
        this.keyStack.push(start);
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public boolean hasNext() {
        return !keyStack.isEmpty();
    }

    public Object next() {
        PropertyKey result = (PropertyKey) keyStack.pop();

        for (int i = hier.getNumChildren(result); i-- > 0;)
            keyStack.push(hier.getChildKey(result, i));

        return result;
    }

    public static Iterator getForPrefixes(DashHierarchy hier,
            Collection prefixes) {

        List results = new ArrayList(prefixes.size());
        for (Iterator i = prefixes.iterator(); i.hasNext();) {
            String onePrefix = (String) i.next();
            PropertyKey oneKey = hier.findExistingKey(onePrefix);
            if (oneKey != null)
                results.add(new PropertyKeyIterator(hier, oneKey));
        }

        if (results.isEmpty())
            return Collections.EMPTY_LIST.iterator();
        else if (results.size() == 1)
            return (Iterator) results.get(0);
        else
            return new IteratorConcatenator(results);
    }
}
