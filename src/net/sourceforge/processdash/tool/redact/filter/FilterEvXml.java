// Copyright (C) 2012-2019 Tuma Solutions, LLC
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
import net.sourceforge.processdash.tool.redact.TaskListMapper;
import net.sourceforge.processdash.util.PatternList;
import net.sourceforge.processdash.util.StringUtils;

public class FilterEvXml extends RedactFilterUtils {

    @EnabledFor(RedactFilterIDs.PEOPLE)
    private boolean hashPeople;

    @EnabledFor(RedactFilterIDs.TASK_NAMES)
    private boolean hashTaskNames;

    private TaskListMapper taskListMapper;

    private HierarchyPathMapper pathMapper;

    public void filterXml(String[] tags) {
        for (int i = 0; i < tags.length; i++) {
            String tag = tags[i].trim();

            if (tag.startsWith("<evSchedule "))
                tag = hashEvScheduleTag(tags[i]);

            else if (tag.startsWith("<evSnapshot "))
                tag = hashSnapshotTag(tags[i]);

            else if (tag.startsWith("<task "))
                tag = hashTaskTag(tags[i]);

            else if (tag.startsWith("<dependency "))
                tag = hashDependencyTag(tags[i]);

            tags[i] = tag;
        }
    }

    private String hashEvScheduleTag(String tag) {
        if (!hashTaskNames)
            return tag;
        else
            return replaceXmlAttr(tag, "name", taskListMapper);
    }

    private String hashSnapshotTag(String tag) {
        if (hashTaskNames)
            tag = replaceXmlAttr(tag, "name", "EV Snapshot");
        tag = replaceXmlAttr(tag, "comment", null);
        return tag;
    }

    private String hashTaskTag(String tag) {
        if (hashTaskNames) {
            Object nameReplacement = pathMapper;
            if (getXmlAttr(tag, "flag") != null)
                nameReplacement = taskListMapper;
            tag = replaceXmlAttr(tag, "name", nameReplacement);
        }

        if (hashPeople) {
            tag = replaceXmlAttr(tag, "who", PersonMapper.HASH_PERSON_LIST);
        }

        return tag;
    }

    private String hashDependencyTag(String tag) {
        if (hashTaskNames) {
            tag = replaceXmlAttr(tag, "name", pathMapper);
            tag = replaceXmlAttr(tag, "taskList", taskListMapper);
        }
        if (hashPeople) {
            tag = replaceXmlAttr(tag, "who", PersonMapper.HASH_PERSON_LIST);
        }
        return tag;
    }

    static FilterEvXml getEvXmlFilter(FilterEvXml fex, RedactFilterData data) {
        if (fex != null)
            return fex;

        FilterEvXml result = new FilterEvXml();
        RedactFilterUtils.setFields(data, result);
        return result;
    }


    @EnabledFor({RedactFilterIDs.TASK_NAMES, RedactFilterIDs.PEOPLE})
    public static class InPdash implements RedactFilter {

        private FilterEvXml xmlFilter;

        public Reader filter(RedactFilterData data, String filename,
                Reader contents) throws IOException {
            if (!filename.endsWith("!ev.xml"))
                return contents;

            xmlFilter = getEvXmlFilter(xmlFilter, data);

            String[] lines = RedactFilterUtils.slurpLines(contents);
            xmlFilter.filterXml(lines);
            String filtered = StringUtils.join(Arrays.asList(lines), "\n");
            return new StringReader(filtered);
        }

    }


    @EnabledFor({RedactFilterIDs.TASK_NAMES, RedactFilterIDs.PEOPLE})
    public static class InGlobalDat extends AbstractDataStringFilter {

        private RedactFilterData data;

        private FilterEvXml xmlFilter;

        public InGlobalDat() {
            filenamePatterns = new PatternList("global.dat", "ev-.*.dat");
        }

        @EnabledFor({ "^Task-Schedule/.*/Snapshot/", "^Snapshot/" })
        public String hashEvBaselines(String xml) {
            xmlFilter = getEvXmlFilter(xmlFilter, data);

            String[] tags = xml.split(">", -1);
            xmlFilter.filterXml(tags);
            return StringUtils.join(Arrays.asList(tags), ">");
        }

    }

}
