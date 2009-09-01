// Copyright (C) 2006-2009 Tuma Solutions, LLC
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


public class ComparableValue implements Comparable {

    private Object value;
    private int ordinal;

    public ComparableValue(Object value, int ordinal) {
        this.value = value;
        this.ordinal = ordinal;
    }

    public int compareTo(Object o) {
        ComparableValue that = (ComparableValue) o;
        return this.ordinal - that.ordinal;
    }

    public Object getValue() {
        return value;
    }

    public int getOrdinal() {
        return ordinal;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

}
