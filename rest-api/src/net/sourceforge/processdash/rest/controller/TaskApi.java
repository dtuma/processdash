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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.sourceforge.processdash.rest.rs.GET;
import net.sourceforge.processdash.rest.rs.HttpException;
import net.sourceforge.processdash.rest.rs.POST;
import net.sourceforge.processdash.rest.rs.PUT;
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

        // load expanded child attributes if requested
        List<String> expand = getListParam(req, "expand");
        if (expand.contains("resources"))
            task.put("resources", RestTaskService.get().scripts(task));

        // load details for the task
        RestTaskService.get().loadData(task);

        // load children if requested
        if (expand.contains("descendants"))
            RestTaskService.get().loadChildren(task, true);
        else if (expand.contains("children"))
            RestTaskService.get().loadChildren(task, false);

        // build result
        return new JsonMap("task", task, "stat", "ok");
    }

    @POST
    @PUT
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

    @GET
    public Map getAllTasks() {
        // retrieve the list of all tasks in this dashboard
        List<RestTask> tasks = RestTaskService.get().allLeaves();
        for (RestTask task : tasks)
            RestTaskService.get().loadData(task,
                RestTaskService.TASK_COMPLETION_DATE);

        // build result
        return new JsonMap("tasks", tasks, "stat", "ok");
    }

    @GET
    @Path("{taskId}/resources/")
    public Map getTaskScripts(String taskId) {
        // retrieve task
        RestTask task = RestTaskService.get().byID(taskId);
        if (task == null)
            throw HttpException.notFound();

        // load the scripts for the given task
        List scripts = RestTaskService.get().scripts(task);

        // build result
        return new JsonMap("resources", scripts, "stat", "ok");
    }


    private static List<String> getListParam(HttpServletRequest req,
            String parameterName) {
        String[] values = req.getParameterValues(parameterName);
        if (values == null || values.length == 0) {
            return Collections.EMPTY_LIST;
        } else if (values.length == 1) {
            return Arrays.asList(values[0].split(","));
        } else {
            List<String> result = new ArrayList<String>();
            for (String oneValue : values)
                result.addAll(Arrays.asList(oneValue.split(",")));
            return result;
        }
    }

}
