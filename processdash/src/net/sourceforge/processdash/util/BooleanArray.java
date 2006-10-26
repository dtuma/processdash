// Copyright (C) 2006 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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
