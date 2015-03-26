// Copyright (C) 2003 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ev.ci;

import java.util.Arrays;

/** Certain earned value calculations require frequent creation and
 * manipulations of lists of double values.  Rather than using
 * ArrayList and allocating Double objects on the heap, this class
 * provides a simple implementation of a list of primitive double
 * values.
 *
 * This class does not attempt to provide all of the methods defined
 * in the java.util.List interface - it only provides what is needed
 * by the earned value logic.
 */
public class DoubleList {

    private double[] contents;
    private int length;


    /** Create a new, empty <code>double</code> list with a default capacity.
     */
    public DoubleList() { this(10); }


    /** Create a new, empty <code>double</code> list with the specified
     * capacity. */
    public DoubleList(int maxLen) {
        contents = new double[maxLen];
        length = 0;
    }


    /** Return the number of <code>double</code> values in this list
     */
    public int size() { return length; }


    /** Add a single <code>double</code> value to the end of this list
     */
    public synchronized void add(double i) {
        ensureCapacity(length+1);
        contents[length++] = i;
    }


    /** Add all of the <code>double</code> values from the other list to
     * the end of this list
     */
    public synchronized void addAll(DoubleList other) {
        for (int i = 0;   i < other.size();   i++)
            add(other.get(i));
    }


    /** ensure that this list has the capacity to hold a certain
     * number of <code>double</code> values, expanding the internal array
     * structure if necessary.
     */
    public synchronized void ensureCapacity(int totalCapacity) {
        if (totalCapacity <= contents.length) return;

        // grow the size in factors of two.
        int doubleSize = contents.length * 2;
        if (totalCapacity < doubleSize) totalCapacity = doubleSize;

        double[] newContents = new double[totalCapacity];
        System.arraycopy(contents, 0, newContents, 0, length);
        contents = newContents;
    }


    /** Get a single <code>double</code> value from this list.
     * @return the <code>double</code> in position <code>pos</code>,
     * or <code>Double.NaN</code> if that position is invalid.
     */
    public double get(int pos) {
        if (pos >= 0  && pos < length) return contents[pos];
        return Double.NaN;
    }

    public void sort() {
        Arrays.sort(contents, 0, length);
    }


    /** Return true if this list contains the given <code>double</code> value.
     */
    public boolean contains(double num) {
        for (int i=size();   i-- > 0; )
            if (contents[i] == num)
                return true;
        return false;
    }


    /** Search for a value in the list.
     */
    public int find(double value) {
        int left = 0;
        int right = size() - 1;
        int mid;
        double mval;
        while (left < right) {
            mid = (left + right) / 2;
            mval = contents[mid];
            if (value == mval)              return mid;
            else if (value < contents[mid]) right = mid - 1;
            else                            left  = mid + 1;
        }

        return left;
    }


    /** Return an array containing the <code>double</code> values in this
     * list.  If this list has size 0, so will the resulting array.
     */
    public double[] getAsArray() {
        double[] result = new double[length];
        System.arraycopy(contents, 0, result, 0, length);
        return result;
    }


    /** Return a string respresentation of this list, useful for
     * debugging purposes only.
     */
    public String toString() {
        if (size() == 0) return "";
        StringBuffer b = new StringBuffer();
        for (int i=0;   i < size();   i++)
            b.append(get(i)).append(",");
        String result = b.toString();
        return result.substring(0, result.length()-1);
    }

}
