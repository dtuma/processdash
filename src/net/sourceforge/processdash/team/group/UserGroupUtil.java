// Copyright (C) 2016 Tuma Solutions, LLC
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

package net.sourceforge.processdash.team.group;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.tool.db.DatabasePlugin;
import net.sourceforge.processdash.tool.db.QueryRunner;
import net.sourceforge.processdash.tool.db.QueryUtils;

public class UserGroupUtil {

    public static Set<String> getProjectIDsForFilter(UserFilter filter,
            DashboardContext ctx) {
        if (filter == null)
            return null;

        Set<String> datasetIDs = filter.getDatasetIDs();
        if (datasetIDs == null)
            return null;
        else if (datasetIDs.isEmpty())
            return Collections.EMPTY_SET;

        DatabasePlugin databasePlugin = ctx.getDatabasePlugin();
        QueryUtils.waitForAllProjects(databasePlugin);
        QueryRunner query = databasePlugin.getObject(QueryRunner.class);
        return new HashSet(query.queryHql(PROJECT_FILTER_QUERY, datasetIDs));
    }

    private static final String PROJECT_FILTER_QUERY = //
    "select distinct p.value.text "
            + "from TaskStatusFact t, PersonAttrFact i, ProjectAttrFact p "
            + "where t.versionInfo.current = 1 "
            + "and t.dataBlock.person.key = i.person.key "
            + "and t.planItem.project.key = p.project.key "
            + "and i.versionInfo.current = 1 "
            + "and i.attribute.identifier = 'person.pdash.dataset_id' "
            + "and i.value.text in (?) " //
            + "and p.versionInfo.current = 1 "
            + "and p.attribute.identifier = 'project.pdash.team_project_id'";

}
