// Copyright (C) 2012-2015 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.redact;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.processdash.util.StringMapper;
import net.sourceforge.processdash.util.StringUtils;

public class PersonMapper {

    public static String hashPersonName(String name) {
        return "Person " + RedactFilterUtils.hash(name);
    }

    public static final StringMapper HASH_PERSON_NAME = new StringMapper() {
        public String getString(String name) {
            return hashPersonName(name);
        }
    };

    public static String hashPersonList(String list) {
        if (list == null || list.trim().length() == 0)
            return list;

        String[] names = list.split(",");
        for (int i = 0; i < names.length; i++) {
            names[i] = hashPersonName(names[i].trim());
        }
        return StringUtils.join(Arrays.asList(names), ", ");
    }

    public static final StringMapper HASH_PERSON_LIST = new StringMapper() {
        public String getString(String name) {
            return hashPersonList(name);
        }
    };


    public static String hashInitials(String initials) {
        return RedactFilterUtils.hash(initials, true, 4);
    }

    public static final StringMapper HASH_INITIALS = new StringMapper() {
        public String getString(String initials) {
            return hashInitials(initials);
        }
    };


    public static String hashSubteamList(String list) {
        if (list == null || list.trim().length() == 0)
            return list;

        String[] names = list.split(",(\t|&#9;)");
        for (int i = 0; i < names.length; i++) {
            names[i] = "Subteam " + RedactFilterUtils.hash(names[i]);
        }
        return StringUtils.join(Arrays.asList(names), ",&#9;");
    }

    public static final StringMapper HASH_SUBTEAMS = new StringMapper() {
        public String getString(String str) {
            return hashSubteamList(str);
        }
    };


    public static String renameZipEntry(String filename) {
        Matcher m = PDASH_FILENAME_PAT.matcher(filename);
        if (m.matches() && !"team".equals(m.group(2)))
            return m.group(1) + hashInitials(m.group(2))
                    + PDASH_FILENAME_SUFFIX;
        else
            return filename;
    }

    private static final Pattern PDASH_FILENAME_PAT = Pattern.compile(
        "(.*/)([^/]+)-data\\.pdash", Pattern.CASE_INSENSITIVE);

    public static final String PDASH_FILENAME_SUFFIX = "-data.pdash";

}
