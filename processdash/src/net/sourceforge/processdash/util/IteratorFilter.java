// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2004 Software Process Dashboard Initiative
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
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.util;

import java.util.Iterator;
import java.util.NoSuchElementException;


/** Iterator that filters the items returned by another iterator.
 *
 */
public abstract class IteratorFilter implements Iterator {

    private Iterator parent;
    private Object nextObj;

    /** Subclasses must make certain to call init() as part of their
     * constructor! */
    protected IteratorFilter(Iterator parent) {
        this.parent = parent;
    }

    protected abstract boolean includeInResults(Object o);

    protected void init() {
        getNextObj();
    }

    private void getNextObj() {
        while (parent.hasNext()) {
            String val = (String) parent.next();
            if (includeInResults(val)) {
                nextObj = val;
                return;
            }
        }
        nextObj = null;
    }

    public boolean hasNext() {
        return nextObj != null;
    }

    public Object next() {
        if (nextObj == null)
            throw new NoSuchElementException();

        Object result = nextObj;
        getNextObj();
        return result;
    }

    /** This operation is not supported by this iterator. */
    public void remove() {
        throw new UnsupportedOperationException
            (getClass().getName() + " does not support remove()");
    }

}
