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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/** Implementation of a synchronized {@link java.util.Set} which is optimized
 * for memory rather than performance.
 * 
 * Note that since memory is our primary goal, we do not use the
 * {@link java.util.Collections#synchronizedSet(java.util.Set)} method,
 * because that would add an additional unnecessary Object and Object reference
 * (16 more bytes per Set).
 */
public class LightweightSynchronizedSet extends LightweightSet {

    public LightweightSynchronizedSet() {
    }

    public LightweightSynchronizedSet(Collection c) {
        super(c);
    }

    public synchronized void add(int index, Object o) {
        super.add(index, o);
    }

    public synchronized boolean add(Object o) {
        return super.add(o);
    }

    public synchronized boolean addAll(Collection c) {
        return super.addAll(c);
    }

    public synchronized boolean addAll(int index, Collection c) {
        return super.addAll(index, c);
    }

    public synchronized void clear() {
        super.clear();
    }

    public synchronized Object clone() {
        return super.clone();
    }

    public synchronized boolean contains(Object elem) {
        return super.contains(elem);
    }

    public synchronized boolean containsAll(Collection c) {
        return super.containsAll(c);
    }

    public synchronized void ensureCapacity(int minCapacity) {
        super.ensureCapacity(minCapacity);
    }

    public synchronized boolean equals(Object o) {
        return super.equals(o);
    }

    public synchronized Object get(int index) {
        return super.get(index);
    }

    public synchronized int hashCode() {
        return super.hashCode();
    }

    public synchronized int indexOf(Object elem) {
        return super.indexOf(elem);
    }

    public synchronized boolean isEmpty() {
        return super.isEmpty();
    }

    public synchronized Iterator iterator() {
        return super.iterator();
    }

    public synchronized int lastIndexOf(Object elem) {
        return super.lastIndexOf(elem);
    }

    public synchronized ListIterator listIterator() {
        return super.listIterator();
    }

    public synchronized ListIterator listIterator(int index) {
        return super.listIterator(index);
    }

    public synchronized Object remove(int index) {
        return super.remove(index);
    }

    public synchronized boolean remove(Object o) {
        return super.remove(o);
    }

    public synchronized boolean removeAll(Collection c) {
        return super.removeAll(c);
    }

    public synchronized boolean retainAll(Collection c) {
        return super.retainAll(c);
    }

    public synchronized Object set(int index, Object o) {
        return super.set(index, o);
    }

    public synchronized int size() {
        return super.size();
    }

    public synchronized List subList(int fromIndex, int toIndex) {
        return super.subList(fromIndex, toIndex);
    }

    public synchronized Object[] toArray() {
        return super.toArray();
    }

    public synchronized Object[] toArray(Object[] a) {
        return super.toArray(a);
    }

    public synchronized String toString() {
        return super.toString();
    }

    public synchronized void trimToSize() {
        super.trimToSize();
    }

}
