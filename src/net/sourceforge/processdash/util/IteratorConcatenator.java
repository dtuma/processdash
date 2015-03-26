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

package net.sourceforge.processdash.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

public class IteratorConcatenator implements EnumerIterator {

    private List contents;

    private Iterator i;

    private Enumeration e;

    protected Object nextObj;

    public IteratorConcatenator(Collection contents) {
        ensureContents(contents);
        this.contents = new LinkedList(contents);
        getNextObj();
    }

    public IteratorConcatenator(Object a, Object b) {
        this(Arrays.asList(new Object[] { a, b }));
    }

    public IteratorConcatenator(Object a, Object b, Object c) {
        this(Arrays.asList(new Object[] { a, b, c }));
    }

    private void ensureContents(Collection c) {
        for (Iterator iter = c.iterator(); iter.hasNext();) {
            Object o = (Object) iter.next();
            if (!(o instanceof Iterator || o instanceof Enumeration)) {
                throw new IllegalArgumentException(
                        "Object is neither an Iterator nor an Enumeration: "
                                + o);
            }
        }
    }

    protected void getNextObj() {
        if (i != null && i.hasNext()) {
            nextObj = i.next();

        } else if (e != null && e.hasMoreElements()) {
            nextObj = e.nextElement();

        } else if (!contents.isEmpty()) {
            i = null;
            e = null;
            Object o = contents.remove(0);
            if (o instanceof Iterator) {
                i = (Iterator) o;
            } else if (o instanceof Enumeration) {
                e = (Enumeration) o;
            }
            getNextObj();

        } else {
            nextObj = null;
        }
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
        throw new UnsupportedOperationException(getClass().getName()
                + " does not support remove()");
    }

    public boolean hasMoreElements() {
        return hasNext();
    }

    public Object nextElement() {
        return next();
    }

}
