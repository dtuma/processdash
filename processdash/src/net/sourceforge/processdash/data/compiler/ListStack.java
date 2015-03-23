// Copyright (C) 2001-2003 Tuma Solutions, LLC
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

package net.sourceforge.processdash.data.compiler;

import java.util.LinkedList;

public class ListStack implements Stack {
    private LinkedList list = new LinkedList();
    private LinkedList descriptors = new LinkedList();

    public ListStack() {}

    public Object pop() {
        descriptors.removeFirst();
        return list.removeFirst();
    }
    public Object push(Object o) {
        return push(o, null);
    }
    public Object push(Object o, Object d) {
        list.addFirst(o);
        descriptors.addFirst(d);
        return o;
    }
    public void clear() {
        list.clear();
        descriptors.clear();
    }
    public boolean empty()         { return list.isEmpty();         }
    public Object peek()           { return list.getFirst();        }
    public Object peekDescriptor() { return descriptors.getFirst(); }
}
