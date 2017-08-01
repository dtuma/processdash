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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.sourceforge.processdash.rest.rs.GET;
import net.sourceforge.processdash.rest.rs.HttpException;
import net.sourceforge.processdash.rest.rs.POST;
import net.sourceforge.processdash.rest.rs.ParamParser;
import net.sourceforge.processdash.rest.rs.Path;
import net.sourceforge.processdash.rest.service.RestTaskService;
import net.sourceforge.processdash.rest.to.JsonDate;
import net.sourceforge.processdash.rest.to.JsonMap;
import net.sourceforge.processdash.rest.to.RestTask;

@Path("/tasks/")
public class TaskApi {

    @GET
    @Path("{taskId}/")
    public Map getTask(HttpServletRequest req, String taskId) {
        // retrieve task
        RestTask task = RestTaskService.get().byID(taskId);
        if (task == null)
            throw HttpException.notFound();
        else
            RestTaskService.get().loadData(task);

        // build result
        return new JsonMap("task", task, "stat", "ok");
    }

    @POST
    @Path("{taskId}/")
    public Map alterTask(HttpServletRequest req, String taskId) {
        // retrieve posted parameters
        Double estTime = ParamParser.DOUBLE.parse(req, "estimatedTime");
        String completionDateParam = req.getParameter("completionDate");
        JsonDate completionDate = ParamParser.DATE.parse(completionDateParam);

        // retrieve task
        RestTask task = RestTaskService.get().byID(taskId);
        RestTaskService.get().ensureLeaf(task);

        // save estimated time, if provided
        if (estTime != null)
            RestTaskService.get().saveEstimatedTime(task, estTime);

        // save completion date, if provided
        if (completionDateParam != null)
            RestTaskService.get().saveCompletionDate(task, completionDate);

        // return the modified task entity
        return getTask(req, taskId);
    }

}
