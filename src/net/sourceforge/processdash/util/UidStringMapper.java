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

import java.util.ArrayList;
import java.util.List;

public class UidStringMapper {

    private List<String> strings;

    private boolean hasChanged;

    public UidStringMapper() {
        strings = new ArrayList<String>();
        hasChanged = false;
    }

    public UidStringMapper(String s) {
        this();

        if (s != null && s.length() > 2) {
            char delim = s.charAt(0);
            s = s.substring(1);
            int pos;
            while (s.length() > 0) {
                pos = s.indexOf(delim);
                strings.add(s.substring(0, pos));
                s = s.substring(pos + 1);
            }
        }
    }

    public int map(String s) {
        int pos = strings.indexOf(s) + 1;
        if (pos == 0) {
            strings.add(s);
            hasChanged = true;
            pos = strings.size();
        }
        return pos;
    }

    public boolean hasChanged() {
        return hasChanged;
    }

    public String saveAsString() {
        if (strings.isEmpty())
            return "";
        StringBuilder binary = new StringBuilder();
        binary.append(DEFAULT_DELIM);
        for (String s : strings)
            binary.append(s).append(DEFAULT_DELIM);

        String result = binary.toString();
        for (int i = 0; i < PREFERRED_DELIMITERS.length(); i++) {
            char d = PREFERRED_DELIMITERS.charAt(i);
            if (result.indexOf(d) == -1)
                return result.replace(DEFAULT_DELIM, d);
        }
        return result;
    }

    private static final char DEFAULT_DELIM = '\u0002';

    private static final String PREFERRED_DELIMITERS = ",;|!^~`/+-_@#$%&*";

}
