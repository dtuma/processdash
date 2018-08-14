// Copyright (C) 2018 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ev.ui;

import java.awt.Color;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.EVTaskListXML;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.team.TeamDataConstants;


/**
 * @since 2.4.4
 */
public class TaskListColorSelector {

    private DashboardContext ctx;

    private Map<String, ListData> projectColors;


    public TaskListColorSelector(DashboardContext ctx) {
        this.ctx = ctx;
    }


    /**
     * If the given task list was imported from a PDASH file within a team
     * project, return the color that was assigned to the corresponding team
     * member in the WBS.
     */
    public Color getPreferredColor(EVTaskList tl) {
        // our logic only works for imported XML task lists
        if (!(tl instanceof EVTaskListXML))
            return null;

        // get the PDASH file this task list was imported from, or abort
        EVTaskListXML xml = (EVTaskListXML) tl;
        File source = xml.getImportSourceFile();
        if (source == null)
            return null;

        // extract the team member initials from the PDASH file name
        String filename = source.getName();
        int dashPos = filename.indexOf('-');
        if (dashPos == -1)
            return null;
        String initials = filename.substring(0, dashPos).toLowerCase() + "=";

        // PDASH files live in a directory whose name corresponds to the ID
        // of a team project. Look up team member colors for this project
        String projectID = source.getParentFile().getName();
        ListData colors = getTeamMemberColorsForProject(projectID);
        if (colors == null)
            return null;

        // look through the project colors to find this team member
        for (int i = colors.size(); i-- > 0;) {
            String item = (String) colors.get(i);
            if (item.startsWith(initials)) {
                try {
                    String color = item.substring(initials.length());
                    return Color.decode(color);
                } catch (Exception e) {
                }
            }
        }

        return null;
    }


    /** Return the team member color map for a given project */
    private ListData getTeamMemberColorsForProject(String projectID) {
        if (projectColors == null) {
            projectColors = new HashMap<String, ListData>();
            loadAllProjectTeamMemberColors(ctx.getHierarchy(),
                PropertyKey.ROOT);
        }
        return projectColors.get(projectID);
    }


    /** Scan the hierarchy and load all team member color maps */
    private void loadAllProjectTeamMemberColors(DashHierarchy hier,
            PropertyKey node) {
        // recurse over the hierarchy as needed
        for (int i = hier.getNumChildren(node); i-- > 0;)
            loadAllProjectTeamMemberColors(hier, hier.getChildKey(node, i));

        // check for color settings on the current node, and save if present
        String dataName = DataRepository.createDataName(node.path(),
            TeamDataConstants.PROJECT_ID);
        SimpleData projectID = ctx.getData().getSimpleValue(dataName);
        dataName = DataRepository.createDataName(node.path(),
            TeamDataConstants.TEAM_MEMBER_COLORS);
        SimpleData colors = ctx.getData().getSimpleValue(dataName);
        if (projectID != null && colors != null)
            projectColors.put(projectID.format(), ListData.asListData(colors));
    }

}
