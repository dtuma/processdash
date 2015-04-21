// Copyright (C) 2015 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.redact.filter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.sourceforge.processdash.tool.redact.RedactFilterUtils;
import net.sourceforge.processdash.util.StringMapper;


public class FilterDefectTypes {

    public static String mapDefectType(String type) {
        if (KNOWN_TYPES.contains(type))
            return type;
        else
            return RedactFilterUtils.hash(type);
    }

    private static final Set<String> KNOWN_TYPES = Collections
            .unmodifiableSet(new HashSet(Arrays.asList("10", "20", "30", "40",
                "50", "60", "70", "80", "90", "100", "Documentation", "Syntax",
                "Build, package", "Assignment", "Interface", "Checking",
                "Data", "Function", "System", "Environment")));

    public static final StringMapper TYPE_MAPPER = new StringMapper() {
        public String getString(String str) {
            return mapDefectType(str);
        }
    };

}
