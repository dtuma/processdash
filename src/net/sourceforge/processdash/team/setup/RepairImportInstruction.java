// Copyright (C) 2002-2020 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package net.sourceforge.processdash.team.setup;

import java.io.File;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.util.StringUtils;


public class RepairImportInstruction {

    public static void maybeRepairForIndividual(DataContext data) {
        maybeRepair(data, true);
    }

    public static void maybeRepairForTeam(DataContext data) {
        maybeRepair(data, false);
    }

    private static void maybeRepair(DataContext data, boolean indiv) {
        String projectID = getString(data, TeamDataConstants.PROJECT_ID);
        String prefix = "Import_" + projectID;
        if (indiv && getString(data, TeamDataConstants.PERSONAL_PROJECT_FLAG) != null)
            prefix = prefix + TeamDataConstants.PERSONAL_PROJECT_IMPORT_SUFFIX;

        String[] locations = new String[2];

        String url = getString(data, TeamDataConstants.TEAM_DATA_DIRECTORY_URL);
        if (StringUtils.hasValue(url)) {
            String loc = url;
            if (indiv)
                loc = loc + "-" + TeamDataConstants.DISSEMINATION_DIRECTORY;
            locations[0] = loc;
        }

        String teamDir = getString(data, TeamDataConstants.TEAM_DATA_DIRECTORY);
        if (StringUtils.hasValue(teamDir)) {
            File loc = new File(teamDir);
            if (indiv)
                loc = new File(loc, TeamDataConstants.DISSEMINATION_DIRECTORY);
            locations[1] = loc.getPath();
        }

        try {
            DashController.repairImportSetting(projectID, prefix, locations);
        } catch (Throwable t) {
            // this method will be undefined prior to Process Dashboard 1.10.5
        }
    }

    private static String getString(DataContext data, String name) {
        SimpleData value = data.getSimpleValue(name);
        return (value == null ? null : value.format());
    }

}
