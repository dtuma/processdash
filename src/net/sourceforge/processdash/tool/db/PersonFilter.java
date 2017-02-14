// Copyright (C) 2017 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.db;

import java.util.Collections;
import java.util.Map;

import net.sourceforge.processdash.api.PDashQuery;
import net.sourceforge.processdash.team.group.GroupPermission;
import net.sourceforge.processdash.team.group.UserFilter;
import net.sourceforge.processdash.team.group.UserGroup;

public class PersonFilter {

    private UserFilter userFilter;

    private Map<Object, String> datasetIDs;

    public PersonFilter(String permissionID, Object queryRunner) {
        this(GroupPermission.getGrantedMembers(permissionID), queryRunner);
    }

    public PersonFilter(UserFilter userFilter, Object queryRunner) {
        this.userFilter = userFilter;

        if (userFilter == null)
            datasetIDs = Collections.EMPTY_MAP;

        else if (UserGroup.isEveryone(userFilter))
            datasetIDs = null;

        else if (queryRunner instanceof PDashQuery)
            datasetIDs = QueryUtils.mapColumns(((PDashQuery) queryRunner)
                    .query(DATASET_ID_QUERY));

        else if (queryRunner instanceof QueryRunner)
            datasetIDs = QueryUtils.mapColumns(((QueryRunner) queryRunner)
                    .queryHql(DATASET_ID_QUERY));

        else
            throw new IllegalArgumentException("Urecognized query object");
    }

    public boolean isBlock() {
        return userFilter == null;
    }

    public boolean isAllow() {
        return datasetIDs == null;
    }

    public boolean include(Object personKey) {
        if (isBlock())
            return false;
        else if (isAllow())
            return true;

        String datasetID = datasetIDs.get(personKey);
        return userFilter.getDatasetIDs().contains(datasetID);
    }

    private static final String DATASET_ID_QUERY = "select " //
            + "p.person.key, p.value.text " //
            + "from PersonAttrFact as p "
            + "where p.attribute.identifier = 'person.pdash.dataset_id'";

}
