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

package net.sourceforge.processdash.rest.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.sourceforge.processdash.rest.rs.GET;
import net.sourceforge.processdash.rest.rs.ParamParser;
import net.sourceforge.processdash.rest.rs.Path;
import net.sourceforge.processdash.rest.service.RestTaskService;
import net.sourceforge.processdash.rest.to.JsonMap;
import net.sourceforge.processdash.rest.to.RestTask;


public class RecentTasksApi {

    @GET
    @Path("/tasks/recent/")
    public Map getRecentTasks(HttpServletRequest req) throws IOException {
        // get the list of recent tasks
        int maxResults = ParamParser.INTEGER.parse(req, "maxResults", 10);
        List<RestTask> tasks = RestTaskService.get().recentTasks(maxResults);
        for (RestTask task : tasks)
            RestTaskService.get().loadData(task);

        // build result
        return new JsonMap("recentTasks", tasks, "stat", "ok");
    }

    @GET
    @Path("/recent-tasks/")
    public Map getRecentTasksLegacyUrl(HttpServletRequest req)
            throws IOException {
        return getRecentTasks(req);
    }

}
