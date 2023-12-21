// Copyright (C) 2012-2020 Tuma Solutions, LLC
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

import java.util.StringTokenizer;

import net.sourceforge.processdash.tool.redact.HierarchyPathMapper;
import net.sourceforge.processdash.tool.redact.LabelMapper;
import net.sourceforge.processdash.tool.redact.PersonMapper;
import net.sourceforge.processdash.tool.redact.RedactFilterIDs;
import net.sourceforge.processdash.tool.redact.EnabledFor;
import net.sourceforge.processdash.tool.redact.HierarchyNameMapper;
import net.sourceforge.processdash.tool.redact.RedactFilterUtils;
import net.sourceforge.processdash.tool.redact.TeamProjectInfo;
import net.sourceforge.processdash.util.StringMapper;

public class FilterWbsProjDump extends AbstractLineBasedFilter {

    @EnabledFor(RedactFilterIDs.TASK_NAMES)
    private boolean hashTaskNames;

    @EnabledFor(RedactFilterIDs.PEOPLE)
    private boolean hashPersonNames;

    @EnabledFor(RedactFilterIDs.NOTES)
    private boolean filterNotes;

    @EnabledFor(RedactFilterIDs.LABELS)
    private boolean filterLabels;

    @EnabledFor(RedactFilterIDs.EXT_LINKS)
    private boolean filterExtLinks;

    private TeamProjectInfo teamProjectInfo;

    private HierarchyNameMapper nameMapper;

    private HierarchyPathMapper pathMapper;

    private Object projectNameRepl;

    public FilterWbsProjDump() {
        setFilenamePatterns("/projdump.xml$");
    }

    @Override
    public boolean shouldFilter(String filename) {
        if (super.shouldFilter(filename)) {
            projectNameRepl = teamProjectInfo.getTeamProjectName(filename);
            if (projectNameRepl == null)
                projectNameRepl = nameMapper;

            return true;

        } else {
            return false;
        }
    }

    public String getString(String line) {
        String trimmed = line.trim();

        if (startsWith(trimmed, "<?", "</", "<scheduleException"))
            return line;

        else if (startsWith(trimmed, "<project"))
            return filterProjectTag(line);

        else if (startsWith(trimmed, "<teamMember"))
            return filterTeamMemberTag(line);

        else if (startsWith(trimmed, "<milestone"))
            return filterMilestoneTag(line);

        else if (startsWith(trimmed, "<attrType", "<attrValue"))
            return filterAttributeTag(line);

        else if (startsWith(trimmed, "<note"))
            return filterNoteTag(line);

        else if (startsWith(trimmed, "<dependency"))
            return filterDependencyTag(line);

        else if (startsWith(trimmed, "<size "))
            return filterSizeTag(line);

        else if (startsWith(trimmed, "<component", "<document", "<psp", "<task"))
            return filterWbsNodeTag(line);

        else
            return line;
    }

    private boolean startsWith(String trimmedLine, String prefix) {
        return trimmedLine.startsWith(prefix);
    }

    private boolean startsWith(String trimmedLine, String... prefixes) {
        for (String prefix : prefixes)
            if (trimmedLine.startsWith(prefix))
                return true;
        return false;
    }

    private String filterProjectTag(String line) {
        // The name of the project needs special handling
        if (hashTaskNames)
            line = replaceXmlAttr(line, "name", projectNameRepl);
        line = filterLabels(line);

        return line;
    }

    private String filterTeamMemberTag(String line) {
        if (hashPersonNames) {
            line = replaceXmlAttr(line, "name", PersonMapper.HASH_PERSON_NAME);
            line = replaceXmlAttr(line, "initials", PersonMapper.HASH_INITIALS);
            line = replaceXmlAttr(line, "subteams", PersonMapper.HASH_SUBTEAMS);
            line = discardXmlAttr(line, "serverIdentityData");
        }

        return line;
    }

    private String filterMilestoneTag(String line) {
        String name = RedactFilterUtils.getXmlAttr(line, "labelName");
        String scrambled = LabelMapper.hashLabel(name);
        line = RedactFilterUtils.replaceXmlAttr(line, "name", scrambled);
        line = RedactFilterUtils.replaceXmlAttr(line, "labelName", scrambled);
        return line;
    }

    private String filterAttributeTag(String line) {
        // Custom attributes could contain just about anything, and their very
        // presence could indicate the company the data came from. Discard them
        // if the user has asked us to filter "notes/comments/descriptions" or
        // "labels." Otherwise, leave them alone.
        return (filterNotes || filterLabels ? null : line);
    }

    private String filterNoteTag(String line) {
        // if we are filtering notes, discard the entire line.
        if (filterNotes)
            return null;

        // otherwise, possibly hash the name of the author, and embedded URLs
        if (hashPersonNames)
            line = replaceXmlAttr(line, "author", PersonMapper.HASH_PERSON_NAME);
        if (filterExtLinks)
            line = RedactFilterUtils.STRIP_URLS.getString(line);

        return line;
    }

    private String filterDependencyTag(String line) {
        if (hashTaskNames) {
            // this may incorrectly scramble the first segment in the name
            // (which would indicate the owning project). But that is an
            // acceptable over-scramble because dependency path names are not
            // computable data. They are only used as a cached fallback value
            // to display an error message when the target of the dependency
            // cannot be found. In most scenarios, the path will be recalculated
            // automatically when the dependency is displayed.
            line = replaceXmlAttr(line, "name", pathMapper);
        }

        return line;
    }

    private String filterSizeTag(String line) {
        if (hashPersonNames)
            line = replaceXmlAttr(line, "owner", PersonMapper.HASH_INITIALS);
        return line;
    }

    private String filterWbsNodeTag(String line) {
        if (hashTaskNames)
            line = replaceXmlAttr(line, "name", nameMapper);
        if (hashPersonNames) {
            line = replaceXmlAttr(line, "time", timeListMapper);
            line = replaceXmlAttr(line, "syncTime", timeListMapper);
            line = replaceXmlAttr(line, "deferredTime", timeListMapper);
            line = replaceXmlAttr(line, "cid", clientIdMapper);
        }
        if (filterExtLinks)
            line = replaceXmlAttr(line, "url", null);
        line = filterLabels(line);

        return line;
    }

    private String filterLabels(String line) {
        if (filterLabels)
            line = replaceXmlAttr(line, "labels",
                LabelMapper.LABEL_LIST_ATTR_MAPPER);

        return line;
    }

    private StringMapper timeListMapper = new StringMapper() {
        public String getString(String str) {
            return filterTimeList(str);
        }
    };

    protected String filterTimeList(String str) {
        if (str == null || !str.startsWith(","))
            return str;

        StringBuilder result = new StringBuilder();
        StringTokenizer tok = new StringTokenizer(str, ",", true);
        while (tok.hasMoreTokens()) {
            String oneTok = tok.nextToken();
            if (",".equals(oneTok))
                result.append(",");
            else {
                int eqPos = oneTok.indexOf('=');
                if (eqPos != -1) {
                    String initials = oneTok.substring(0, eqPos);
                    String time = oneTok.substring(eqPos);
                    if (!initials.equals("unassigned"))
                        initials = PersonMapper.hashInitials(initials);
                    oneTok = initials + time;
                }
                result.append(oneTok);
            }
        }

        return result.toString();
    }

    private StringMapper clientIdMapper = new StringMapper() {
        public String getString(String str) {
            return PersonMapper.hashClientNodeID(str);
        }
    };

}
