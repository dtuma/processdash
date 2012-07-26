// Copyright (C) 2012 Tuma Solutions, LLC
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

import net.sourceforge.processdash.tool.redact.RedactFilterIDs;
import net.sourceforge.processdash.tool.redact.EnabledFor;
import net.sourceforge.processdash.tool.redact.HierarchyNameMapper;
import net.sourceforge.processdash.tool.redact.LocationMapper;
import net.sourceforge.processdash.tool.redact.PersonMapper;
import net.sourceforge.processdash.tool.redact.TaskListMapper;
import net.sourceforge.processdash.tool.redact.TeamProjectInfo;

public class FilterWbsSettingsXml extends AbstractLineBasedFilter {

    @EnabledFor(RedactFilterIDs.TASK_NAMES)
    private boolean hashTaskNames;

    private TeamProjectInfo teamProjectInfo;

    private HierarchyNameMapper nameMapper;

    private TaskListMapper taskListMapper;

    private Object projectNameRepl;

    public FilterWbsSettingsXml() {
        setFilenamePatterns("externalresources/.*/settings.xml$");
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
        if (hashTaskNames) {
            line = replaceXmlAttr(line, "projectName", projectNameRepl);
            line = replaceXmlAttr(line, "shortName", PersonMapper.HASH_INITIALS);
            line = replaceXmlAttr(line, "scheduleName", taskListMapper);
        }

        line = replaceXmlAttr(line, "teamDirectory", LocationMapper.FAKE_TEAM_DIR);
        line = replaceXmlAttr(line, "teamDirectoryUNC", LocationMapper.FAKE_TEAM_DIR);
        line = replaceXmlAttr(line, "teamDataURL", LocationMapper.URL_MAPPER);

        return line;
    }

}
