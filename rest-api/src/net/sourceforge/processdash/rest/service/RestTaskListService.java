// Copyright (C) 2017 Tuma Solutions, LLC
// REST API Add-on for the Process Dashboard
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

package net.sourceforge.processdash.rest.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.ev.EVTask;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.EVTaskListData;
import net.sourceforge.processdash.ev.EVTaskListRollup;
import net.sourceforge.processdash.rest.to.RestProject;
import net.sourceforge.processdash.team.TeamDataConstants;

public class RestTaskListService {

    private static RestTaskListService svc;

    public static RestTaskListService get() {
        if (svc == null)
            svc = new RestTaskListService();
        return svc;
    }


    private DashboardContext ctx;

    private RestTaskListService() {
        ctx = RestDashContext.get();
    }

    public List<String> getTaskOrder(RestProject project) {
        if (project == null)
            return Collections.EMPTY_LIST;

        String projectPath = project.getFullName();
        SimpleData sd = ctx.getData().getSimpleValue(
            projectPath + "/" + TeamDataConstants.PROJECT_SCHEDULE_NAME);
        if (sd == null)
            return Collections.EMPTY_LIST;

        String taskListName = sd.format();
        return getTaskOrder(taskListName);
    }

    public List<String> getTaskOrder(String taskListName) {
        EVTaskList tl = EVTaskList.openExisting(taskListName, ctx.getData(),
            ctx.getHierarchy(), ctx.getCache(), false);
        if (tl instanceof EVTaskListData)
            ((EVTaskListData) tl).recalcLeavesOnly();
        else if (tl instanceof EVTaskListRollup)
            ((EVTaskListRollup) tl).recalcLeavesOnly();
        else
            return Collections.EMPTY_LIST;

        tl.recalc();
        List<EVTask> leaves = tl.getFilteredLeaves(null);

        List<String> result = new ArrayList<String>(leaves.size());
        for (EVTask t : leaves)
            result.add(t.getFullName());
        return result;
    }

}
