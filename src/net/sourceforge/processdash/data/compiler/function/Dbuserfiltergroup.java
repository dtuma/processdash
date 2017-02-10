// Copyright (C) 2016-2017 Tuma Solutions, LLC
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

package net.sourceforge.processdash.data.compiler.function;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.NumberData;
import net.sourceforge.processdash.data.compiler.ExpressionContext;
import net.sourceforge.processdash.data.repository.SubscribingExpressionContext;
import net.sourceforge.processdash.team.group.UserGroupManagerDash;
import net.sourceforge.processdash.tool.db.StudyGroupManager;

public class Dbuserfiltergroup extends DbAbstractFunction {

    /**
     * Perform a procedure call.
     * 
     * This method <b>must</b> be thread-safe.
     * 
     * Arguments: Project key list, dataset ID list
     */
    public Object call(List arguments, ExpressionContext context) {
        // get the name of the data element we are calculating for
        String listenerName = asStringVal(context
                .get(SubscribingExpressionContext.LISTENERVAR_NAME));

        // get the database keys of the projects in question
        ListData projectKeyList = asList(getArg(arguments, 0));
        if (projectKeyList == null)
            return null;
        List<Integer> projectKeys = new ArrayList();
        for (int i = 0; i < projectKeyList.size();  i++) {
            Object oneKeyVal = projectKeyList.get(i);
            if (oneKeyVal instanceof NumberData) {
                int oneKey = ((NumberData) oneKeyVal).getInteger();
                projectKeys.add(oneKey);
            }
        }
        if (projectKeys.isEmpty())
            return null;

        // get the list of dataset IDs we are searching for, and convert them
        // to plain String objects
        Set<String> datasetIds = new HashSet();
        for (Object oneDatasetIdVal : collapseLists(arguments, 1)) {
            String oneDatasetId = asStringVal(oneDatasetIdVal);
            if (oneDatasetId != null)
                datasetIds.add(oneDatasetId);
        }

        // if the list of dataset IDs is empty, one of three things is true:
        // -- no filter is in effect
        // -- the "everyone" group has been selected
        // -- the selected group was deleted, so we should revert to "everyone"
        // either way, return null to indicate "no filtering"
        if (datasetIds.isEmpty())
            return null;

        // if the list of dataset IDs only contains "*empty*", return an
        // error code signifying an empty group
        if (datasetIds.contains(UserGroupManagerDash.EMPTY_GROUP_TOKEN))
            return new DoubleData(-1, false);

        // retrieve the study group manager
        StudyGroupManager mgr = getDbObject(context, StudyGroupManager.class);
        if (mgr == null)
            return null;

        try {
            int result = mgr.getDataBlockGroup(projectKeys, datasetIds,
                listenerName);
            return new DoubleData(result, false);
        } catch (Throwable t) {
            // if we have an older version of the database plugin, the study
            // group manager may not support data block groups. Return null to
            // disable user group filtering.
            return null;
        }
    }

}
