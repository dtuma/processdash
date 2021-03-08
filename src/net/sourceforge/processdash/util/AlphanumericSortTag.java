// Copyright (C) 2021 Tuma Solutions, LLC
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AlphanumericSortTag implements Comparable<AlphanumericSortTag> {

    private List components;

    public AlphanumericSortTag(String text) {
        if (text == null) {
            components = Collections.EMPTY_LIST;
        } else {
            components = new ArrayList();
            Matcher m = COMPONENT_PAT.matcher(text);
            while (m.find()) {
                if (m.group(1) != null)
                    components.add(new Integer(m.group(1)));
                else if (m.group(2) != null)
                    components.add(m.group(2));
            }
        }
    }

    @Override
    public int compareTo(AlphanumericSortTag that) {
        int thisLen = this.components.size();
        int thatLen = that.components.size();
        if (thisLen == 0 && thatLen > 0)
            return +1;
        else if (thisLen > 0 && thatLen == 0)
            return -1;

        int len = Math.max(thisLen, thatLen);
        for (int i = 0; i < len; i++) {
            Object thisComp = this.component(i);
            Object thatComp = that.component(i);
            int thisDiff = compare(thisComp, thatComp);
            if (thisDiff != 0)
                return thisDiff;
        }

        return 0;
    }

    private Object component(int pos) {
        return (pos < components.size() ? components.get(pos) : null);
    }

    private int compare(Object a, Object b) {
        if (a == b)
            return 0;

        ComponentType aType = getType(a);
        ComponentType bType = getType(b);
        if (aType != bType)
            return aType.compareTo(bType);
        else if (aType == ComponentType.Number)
            return ((Integer) a).compareTo((Integer) b);
        else
            return ((String) a).compareToIgnoreCase((String) b);
    }

    private enum ComponentType {
        Null, Number, Word
    }

    private ComponentType getType(Object obj) {
        if (obj == null)
            return ComponentType.Null;
        else if (obj instanceof Integer)
            return ComponentType.Number;
        else
            return ComponentType.Word;
    }

    private static final Pattern COMPONENT_PAT = Pattern
            .compile("(\\d+)|(\\p{L}+)");

}
