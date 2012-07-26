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
import net.sourceforge.processdash.tool.redact.TeamProjectInfo;

@EnabledFor(RedactFilterIDs.TASK_NAMES)
public class FilterWbsNodeNames extends AbstractLineBasedFilter {

    private TeamProjectInfo teamProjectInfo;

    private HierarchyNameMapper nameMapper;

    private Object projectNameRepl;

    public FilterWbsNodeNames() {
        setFilenamePatterns("/wbs.xml$");
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
        int indentLevel = getIndentLevel(line);
        if (indentLevel == 0)
            line = replaceXmlAttr(line, "name", projectNameRepl);
        else if (indentLevel > 0)
            line = replaceXmlAttr(line, "name", nameMapper);

        return line;
    }

}
