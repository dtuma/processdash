// Copyright (C) 2012-2013 Tuma Solutions, LLC
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sourceforge.processdash.util.StringMapper;
import net.sourceforge.processdash.util.StringUtils;

public class LabelMapper implements StringMapper {

    public String getString(String str) {
        return hashLabel(str);
    }

    public static String hashLabel(String str) {
        if (str == null)
            return null;

        str = str.trim();
        if (str.length() == 0)
            return str;

        if (SAFE_LABELS.contains(str))
            return str;
        else
            return RedactFilterUtils.hash(str);
    }

    private static final Set<String> SAFE_LABELS = Collections
            .unmodifiableSet(new HashSet(Arrays.asList("none", "Completed")));

    public static String hashLabelList(String labels) {
        return hashLabelList(labels, true);
    }

    public static String hashLabelList(String labels, boolean addSpaceInDelim) {
        if (labels == null)
            return labels;

        if (labels.indexOf(',') == -1)
            return hashLabel(labels);

        List<String> newLabels = new ArrayList<String>();
        for (String label : labels.split(",")) {
            label = hashLabel(label);
            if (label.length() > 0)
                newLabels.add(label);
        }
        String delim = addSpaceInDelim ? ", " : ",";
        return StringUtils.join(newLabels, delim);
    }

    public static final StringMapper LABEL_LIST_MAPPER = new StringMapper() {
        public String getString(String str) {
            return hashLabelList(str);
        }
    };

    public static final StringMapper LABEL_LIST_ATTR_MAPPER = new StringMapper() {
        public String getString(String str) {
            return hashLabelList(str, false);
        }
    };

    public static String convertToLabel(String text) {
        if (text == null || text.length() == 0)
            return null;

        String[] tokens = text.split(NON_LABEL_CHARS_REGEXP);
        return StringUtils.join(Arrays.asList(tokens), "_");
    }

    private static final String NON_LABEL_CHARS_REGEXP = "[,\u0000- |()]+";

}
