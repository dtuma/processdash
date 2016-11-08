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

import java.util.List;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.db.DatabasePlugin;
import net.sourceforge.processdash.tool.db.QueryRunner;
import net.sourceforge.processdash.tool.db.QueryUtils;

public class UserGroupManager {

    public static final String EVERYONE_GROUP_ID = "0";

    private static UserGroupManager INSTANCE;

    public static void init(DashboardContext ctx) {
        INSTANCE = new UserGroupManager(ctx);
    }

    public static UserGroupManager getInstance() {
        return INSTANCE;
    }



    private DatabasePlugin databasePlugin;

    private QueryRunner query;

    private static final Resources resources = Resources
            .getDashBundle("ProcessDashboard.Groups");


    private UserGroupManager(DashboardContext ctx) {
        databasePlugin = QueryUtils.getDatabasePlugin(ctx.getData());
        query = databasePlugin.getObject(QueryRunner.class);
    }

    public UserGroup getEveryone() {
        // create a group object to hold the information
        String groupName = resources.getString("Everyone");
        UserGroup result = new UserGroup(groupName, EVERYONE_GROUP_ID, false);

        // query the database for all known people, and add them to the group
        QueryUtils.waitForAllProjects(databasePlugin);
        List<Object[]> rawData = query.queryHql(EVERYONE_QUERY);
        for (Object[] row : rawData) {
            String userName = (String) row[1];
            String datasetID = (String) row[2];
            UserGroupMember m = new UserGroupMember(userName, datasetID);
            result.getMembers().add(m);
        }

        return result;
    }

    private static final String EVERYONE_QUERY = //
    "select p.person.key, p.person.encryptedName, p.value.text "
            + "from PersonAttrFact as p " //
            + "where p.versionInfo.current = 1 "
            + "and p.attribute.identifier = 'person.pdash.dataset_id'";

}
