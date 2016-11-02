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

import java.util.Arrays;

import net.sourceforge.processdash.tool.redact.DefectWorkflowPhaseMapper;
import net.sourceforge.processdash.tool.redact.RedactFilterIDs;
import net.sourceforge.processdash.tool.redact.EnabledFor;
import net.sourceforge.processdash.tool.redact.HierarchyInfo;
import net.sourceforge.processdash.tool.redact.HierarchyNodeMapper;
import net.sourceforge.processdash.tool.redact.HierarchyInfo.Node;
import net.sourceforge.processdash.util.StringMapper;
import net.sourceforge.processdash.util.StringUtils;

@EnabledFor({ RedactFilterIDs.TASK_NAMES, RedactFilterIDs.NOTES,
        RedactFilterIDs.DEFECT_TYPES, RedactFilterIDs.WORKFLOWS })
public class FilterLocalDefectLogs extends AbstractLineBasedFilter {

    @EnabledFor(RedactFilterIDs.DEFECT_TYPES)
    private boolean filterTypes;

    @EnabledFor(RedactFilterIDs.NOTES)
    private boolean stripComments;

    private HierarchyInfo hierarchyInfo;

    private HierarchyNodeMapper nameMapper;

    private DefectPhaseFilter phaseFilter;

    private DefectWorkflowPhaseMapper workflowPhaseFilter;

    public FilterLocalDefectLogs() {
        setFilenamePatterns(".def$");
    }

    public void afterPropertiesSet() {
        if (data.isFiltering(RedactFilterIDs.TASK_NAMES))
            phaseFilter = new DefectPhaseFilter();
        if (data.isFiltering(RedactFilterIDs.WORKFLOWS))
            workflowPhaseFilter = nameMapper.getDefectWorkflowPhaseMapper();
    }

    @Override
    public boolean shouldFilter(String filename) {
        boolean result = super.shouldFilter(filename);
        if (result && phaseFilter != null)
            phaseFilter.setCurrentFilename(filename);
        return result;
    }

    public String getString(String line) {
        if (line == null)
            return null;
        else if (line.trim().startsWith("<"))
            return filterXml(line);
        else
            return filterOldStyle(line);
    }

    private String filterXml(String line) {
        if (filterTypes)
            line = replaceXmlAttr(line, "type", FilterDefectTypes.TYPE_MAPPER);

        if (stripComments)
            line = discardXmlAttr(line, "desc");

        if (phaseFilter != null) {
            line = replaceXmlAttr(line, "inj", phaseFilter);
            line = replaceXmlAttr(line, "rem", phaseFilter);
        }

        if (workflowPhaseFilter != null) {
            line = replaceXmlAttr(line, "injName", workflowPhaseFilter);
            line = replaceXmlAttr(line, "remName", workflowPhaseFilter);
        }

        return line;
    }

    private String filterOldStyle(String line) {
        String[] pieces = line.split("\t", -1);

        if (filterTypes)
            pieces[1] = FilterDefectTypes.mapDefectType(pieces[1]);

        if (stripComments)
            pieces[6] = " ";

        if (phaseFilter != null) {
            pieces[2] = phaseFilter.getString(pieces[2]);
            pieces[3] = phaseFilter.getString(pieces[3]);
        }

        return StringUtils.join(Arrays.asList(pieces), "\t");
    }


    private class DefectPhaseFilter implements StringMapper {

        private boolean isGeneric = false;

        public void setCurrentFilename(String name) {
            Node info = hierarchyInfo.findNodeForDefectFile(name);
            isGeneric = (info != null && "Generic".equals(info.templateId));
        }

        public String getString(String phase) {
            // Generic projects have free-text phase names, and those names
            // should generally be scrambled.
            if (isGeneric)
                return nameMapper.mapName(phase);

            int slashPos = phase.lastIndexOf('/');
            if (slashPos == -1)
                // if this phase does not contain a slash, it represents
                // the simple name of a phase in a process. These should not
                // be scrambled, or the process metric analyses will fail.
                return phase;

            else
                // the phase will contain a slash for a PSP3 process. The
                // portion before the slash needs to be scrambled.
                return nameMapper.mapPath(phase.substring(0, slashPos))
                        + phase.substring(slashPos);
        }

    }

}
