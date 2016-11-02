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

package net.sourceforge.processdash.tool.redact;

import java.io.IOException;

public class RedactFilterHelperInit {

    public static void createHelpers(RedactFilterData data) throws IOException {
        createStandardHelpers(data);
        createCustomHelpers(data);
    }

    private static void createStandardHelpers(RedactFilterData data)
            throws IOException {

        // scan the hierarchy and collect relevant information
        HierarchyInfo hierarchyInfo = new HierarchyInfo(data);
        data.putHelper(hierarchyInfo);

        // create an object which can map names and paths of hierarchy nodes
        HierarchyNodeMapper nodeMapper = new HierarchyNodeMapper();
        data.putHelper(nodeMapper);
        data.putHelper(HierarchyNameMapper.class, nodeMapper.getNameMapper());
        data.putHelper(HierarchyPathMapper.class, nodeMapper.getPathMapper());
        data.putHelper(DefectWorkflowPhaseMapper.class,
            nodeMapper.getDefectWorkflowPhaseMapper());

        // register process phases as "safe" and not needing scrambling
        TemplateInfo.addSafeNamesOfProcessPhases(nodeMapper);

        // create an object which gathers information about the team projects
        // that are defined in this dashboard.
        TeamProjectInfo teamProjectInfo = new TeamProjectInfo(data,
                hierarchyInfo);
        data.putHelper(teamProjectInfo);

        // if the user doesn't want to scramble workflow names, register them
        // as "safe"
        if (data.isFiltering(RedactFilterIDs.WORKFLOWS) == false) {
            hierarchyInfo.registerWorkflowNamesAsSafe(nodeMapper);
            TeamProjectInfo.scanForSafeWorkflowNames(data, nodeMapper);
        }

        // use our information about safe/nonsafe node names to compute new
        // names for the nodes in our hierarchy. Also register the paths of
        // nodes that were assigned explicit names based on a pattern.
        hierarchyInfo.remapNamesAndRegisterPatternedPaths(nodeMapper);

        // record other objects that are helpful for the redaction process.
        data.putHelper(new TaskListMapper(data));
        data.putHelper(new LabelMapper());
    }

    private static void createCustomHelpers(RedactFilterData data) {
        for (Object helper : RedactFilterUtils.getExtensions(data,
            "redact-filter-helper"))
            data.putHelper(helper);
    }

}
