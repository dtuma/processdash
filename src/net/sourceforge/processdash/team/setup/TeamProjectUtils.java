// Copyright (C) 2021 Tuma Solutions, LLC
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

package net.sourceforge.processdash.team.setup;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.team.TeamDataConstants;

public class TeamProjectUtils {

    public enum ProjectType {
        Stub, Team, Master, Indiv, Personal
    }


    public static ProjectType getProjectType(DashboardContext ctx,
            String projectPath) {
        PropertyKey projectKey = ctx.getHierarchy()
                .findExistingKey(projectPath);
        return (projectKey == null ? null : getProjectType(ctx, projectKey));
    }


    public static ProjectType getProjectType(DashboardContext ctx,
            PropertyKey projectKey) {
        String templateID = ctx.getHierarchy().getID(projectKey);
        if (templateID == null)
            return null;

        else if (templateID.equals(TeamStartBootstrap.TEAM_STUB_ID))
            return ProjectType.Stub;

        else if (templateID.endsWith("/TeamRoot"))
            return ProjectType.Team;

        else if (templateID.endsWith("/MasterRoot"))
            return ProjectType.Master;

        else if (templateID.endsWith("/Indiv2Root")) {
            String dataName = DataRepository.createDataName(projectKey.path(),
                TeamDataConstants.PERSONAL_PROJECT_FLAG);
            SimpleData sd = ctx.getData().getSimpleValue(dataName);
            return (sd != null && sd.test() ? ProjectType.Personal
                    : ProjectType.Indiv);

        } else
            return null;
    }

}
