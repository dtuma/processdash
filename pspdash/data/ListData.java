// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil

package pspdash.data;

import java.util.StringTokenizer;
import java.util.Vector;

public class ListData implements SimpleData {

    private Vector list = new Vector();
    private String stringVersion = null;

    public ListData() {}
    public ListData(ListData l) {
        this.list = (Vector) l.list.clone();
    }

    public ListData(String l) {
        if (l == null) return;

        StringTokenizer tok = null;
        if (l.length() > 1) {
            char first = l.charAt(0);
            char last = l.charAt(l.length() - 1);
            if (first == last)
                tok = new StringTokenizer(l, "" + first);
        }

        if (tok == null)
            add(l);
        else
            while (tok.hasMoreTokens())
                add(tok.nextToken());
    }

    // If you call either of the following methods, it is up to you to
    // call DataRepository.putValue() to notify the data repository
    // that the value has changed.
    synchronized void clear() {
        list.removeAllElements(); stringVersion = null; }
    synchronized void add(Object o) {
        list.addElement(o); stringVersion = null; }
    public boolean contains(Object o) {
        return list.contains(o); }
    public synchronized boolean remove(Object o) {
        // We cannot use the Vector.remove() method because it was
        // introduced in JDK 1.2, and this class must run inside IE
        // which only supports 1.1
        if (o == null) return false;
        for (int i=0;  i<list.size();  i++)
            if (o.equals(list.elementAt(i))) {
                list.removeElementAt(i);
                stringVersion = null;
                return true;
            }
        return false;
    }

    public synchronized boolean setAdd(Object o) {
        if (list.contains(o)) return false;
        add(o); return true;
    }

    public int size()           { return list.size();          }
    public Object get(int item) { return list.elementAt(item); }

    // The following methods implement the SaveableData interface.

                                // editable lists are not yet supported.
    public boolean isEditable() { return false; }
    public void setEditable(boolean e) {}
                                // undefined lists are not yet supported.
    public boolean isDefined() { return true; }
    public void setDefined(boolean d) {}

    public String saveString() {
        return StringData.create(format()).saveString();
    }

    public SimpleData getSimpleValue() {
        return new ListData(this);
    }

    public void dispose() {
        clear();
    }

    // The following methods implement the SimpleData interface.

    private static final char DEFAULT_DELIM = '\u0001';
    public synchronized String format() {
        if (stringVersion != null) return stringVersion;

        StringBuffer result = new StringBuffer();
        result.append(DEFAULT_DELIM);
        for (int i=0;  i < list.size();  i++)
            result.append(list.elementAt(i)).append(DEFAULT_DELIM);
        stringVersion = result.toString();

        return stringVersion;
    }

    public SimpleData parse(String val) {
        return (val == null || val.length() == 0) ? null : new ListData(val);
    }


    public boolean equals(SimpleData val) {
        return (val != null && format().equals(val.format()));
    }
                                // ordering for lists doesn't make sense.
    public boolean lessThan(SimpleData val) { return false; }
    public boolean greaterThan(SimpleData val) { return false; }

    public boolean test() { return !list.isEmpty(); }
}
