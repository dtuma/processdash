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

package net.sourceforge.processdash.tool.redact.filter;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;

import net.sourceforge.processdash.tool.redact.RedactFilter;
import net.sourceforge.processdash.tool.redact.RedactFilterData;
import net.sourceforge.processdash.tool.redact.RedactFilterIDs;
import net.sourceforge.processdash.tool.redact.RedactFilterUtils;
import net.sourceforge.processdash.tool.redact.EnabledFor;
import net.sourceforge.processdash.tool.redact.HierarchyPathMapper;
import net.sourceforge.processdash.tool.redact.PersonMapper;
import net.sourceforge.processdash.util.StringMapper;
import net.sourceforge.processdash.util.StringUtils;

@EnabledFor({ RedactFilterIDs.TASK_NAMES, RedactFilterIDs.PEOPLE,
        RedactFilterIDs.DEFECT_TYPES })
public class FilterPdashData extends RedactFilterUtils implements RedactFilter {

    @EnabledFor(RedactFilterIDs.TASK_NAMES)
    private boolean hashTaskNames;

    @EnabledFor(RedactFilterIDs.PEOPLE)
    private boolean hashPeople;

    @EnabledFor(RedactFilterIDs.DEFECT_TYPES)
    private boolean filterDefectTypes;

    private HierarchyPathMapper pathMapper;

    private StringMapper projectWbsIdMapper = new HashProjectWbsID();

    private String[] lines;

    public Reader filter(RedactFilterData data, String filename, Reader contents)
            throws IOException {
        if (!filename.endsWith("!data.xml"))
            return contents;

        lines = slurpLines(contents);
        filterLines();
        String filtered = StringUtils.join(Arrays.asList(lines), "\n");
        lines = null;
        return new StringReader(filtered);
    }

    private void filterLines() {
        int i = remapInitialPath();

        boolean lookingForInitials = hashPeople;

        while (++i < lines.length) {
            if (lookingForInitials && isTag(i, STR, "Indiv_Initials")) {
                replaceXmlContents(i, PersonMapper.HASH_INITIALS);
                lookingForInitials = false;

            } else if (hashTaskNames && isTag(i, NODE) && isTag(i + 1, TAG)) {
                replaceXmlAttr(i, NAME, pathMapper);

            } else if (hashTaskNames && isTag(i, STR, "Project_WBS_ID")) {
                replaceXmlContents(i, projectWbsIdMapper);

            } else if (filterDefectTypes && isTag(i, STR, "Defect_Type_Standard")) {
                lines[i] = "";
            }
        }

    }

    private int remapInitialPath() {
        if (!hashTaskNames)
            return 0;

        StringBuilder initialPath = new StringBuilder();
        int i = 0;
        while (!isTag(i, TAG)) {
            if (isTag(i, NODE)) {
                String name = getXmlAttr(i, NAME);
                if (name != null)
                    initialPath.append("/").append(name);
            }
            if (++i >= lines.length)
                return i;
        }

        String newPath = pathMapper.getString(initialPath.toString());
        String[] newPathSegments = newPath.substring(1).split("/");
        for (int j = 0; j < newPathSegments.length; j++) {
            int destPos = i - newPathSegments.length + j;
            replaceXmlAttr(destPos, NAME, newPathSegments[j]);
        }

        return i;
    }

    private boolean isTag(int lineNum, String tag) {
        if (lineNum >= lines.length)
            return false;
        String line = lines[lineNum];
        int pos = line.indexOf('<');
        if (pos == -1)
            return false;
        else
            return line.regionMatches(pos + 1, tag, 0, tag.length());
    }

    private boolean isTag(int lineNum, String tag, String name) {
        return isTag(lineNum, tag) && name.equals(getXmlAttr(lineNum, NAME));
    }

    private String getXmlAttr(int lineNum, String attrName) {
        return getXmlAttr(lines[lineNum], attrName);
    }

    private void replaceXmlAttr(int lineNum, String attrName, Object replacement) {
        lines[lineNum] = replaceXmlAttr(lines[lineNum], attrName, replacement);
    }

    private void replaceXmlContents(int lineNum, StringMapper replacement) {
        String line = lines[lineNum];
        int beg = line.indexOf('>') + 1;
        if (beg == 0)
            return;

        int end = line.indexOf('<', beg);
        if (end == -1)
            return;

        String origValue = line.substring(beg, end);
        String newValue = replacement.getString(origValue);
        String newLine = line.substring(0, beg) + newValue
                + line.substring(end);
        lines[lineNum] = newLine;
    }

    private class HashProjectWbsID implements StringMapper {

        public String getString(String str) {
            if (str == null)
                return str;
            int slashPos = str.indexOf('/');
            if (slashPos == -1)
                return str;

            String projID = str.substring(0, slashPos);
            String origPath = str.substring(slashPos);
            String newPath = pathMapper.getString(" " + origPath).substring(1);
            return projID + newPath;
        }

    }

    private static final String NODE = "node";

    private static final String NAME = "name";

    private static final String TAG = "tag";

    private static final String STR = "str";

}
