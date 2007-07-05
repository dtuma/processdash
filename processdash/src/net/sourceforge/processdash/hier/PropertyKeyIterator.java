// Copyright (C) 2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC: processdash-devel@lists.sourceforge.net

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
