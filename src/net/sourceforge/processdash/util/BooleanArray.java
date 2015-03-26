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


public class BooleanArray {

    private boolean[] data;

    public BooleanArray() {
        this(1);
    }

    public BooleanArray(int capacity) {
        data = new boolean[capacity];
    }

    public boolean get(int pos) {
        if (pos >= 0 && pos < data.length)
            return data[pos];
        else
            return false;
    }

    public void set(int pos, boolean val) {
        if (pos >= 0) {
            ensureCapacity(pos);
            data[pos] = val;
        }
    }

    public void clear() {
        data = new boolean[1];
    }

    public void ensureCapacity(int pos) {
        if (data.length <= pos) {
            int newLen = Math.max(pos+1, data.length * 2);
            boolean[] newData = new boolean[newLen];
            System.arraycopy(data, 0, newData, 0, data.length);
            data = newData;
        }
    }
}
