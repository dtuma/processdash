// Copyright (C) 2012-2016 Tuma Solutions, LLC
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

import net.sourceforge.processdash.tool.redact.DefectWorkflowPhaseMapper;
import net.sourceforge.processdash.tool.redact.EnabledFor;
import net.sourceforge.processdash.tool.redact.HierarchyPathMapper;
import net.sourceforge.processdash.tool.redact.RedactFilterIDs;

@EnabledFor({ RedactFilterIDs.TASK_NAMES, RedactFilterIDs.NOTES,
        RedactFilterIDs.DEFECT_TYPES, RedactFilterIDs.WORKFLOWS })
public class FilterPdashDefects extends AbstractLineBasedFilter {

    @EnabledFor(RedactFilterIDs.DEFECT_TYPES)
    private boolean filterTypes;

    @EnabledFor(RedactFilterIDs.NOTES)
    private boolean stripDescriptions;

    @EnabledFor(RedactFilterIDs.TASK_NAMES)
    private boolean hashPaths;

    @EnabledFor(RedactFilterIDs.WORKFLOWS)
    private boolean hashWorkflows;

    private HierarchyPathMapper pathMapper;

    private DefectWorkflowPhaseMapper workflowPhaseMapper;

    public FilterPdashDefects() {
        setFilenamePatterns("!defects.xml$");
    }

    public String getString(String line) {
        if (hashPaths)
            line = replaceXmlAttr(line, "path", pathMapper);
        if (filterTypes)
            line = replaceXmlAttr(line, "type", FilterDefectTypes.TYPE_MAPPER);
        if (stripDescriptions)
            line = discardXmlAttr(line, "desc");
        if (hashWorkflows) {
            line = replaceXmlAttr(line, "injName", workflowPhaseMapper);
            line = replaceXmlAttr(line, "remName", workflowPhaseMapper);
        }
        return line;
    }

}
