// Copyright (C) 2009 Tuma Solutions, LLC
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


public class CaseInsensitiveString implements Comparable<CaseInsensitiveString> {

    private String value;

    private String cmp;

    public CaseInsensitiveString(String value) {
        this.value = value;
        this.cmp = value.toLowerCase();
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj instanceof CaseInsensitiveString) {
            CaseInsensitiveString that = (CaseInsensitiveString) obj;
            return this.cmp.equals(that.cmp);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return cmp.hashCode();
    }

    public int compareTo(CaseInsensitiveString that) {
        return this.cmp.compareTo(that.cmp);
    }

    public static <T extends Collection> T convertTo(Collection<String> src,
            T dest) {
        for (String s : src) {
            dest.add(new CaseInsensitiveString(s));
        }
        return dest;
    }

    public static <T extends Collection> T convertFrom(
            Collection<CaseInsensitiveString> src, T dest) {
        for (CaseInsensitiveString c : src) {
            dest.add(c.toString());
        }
        return dest;
    }

}
