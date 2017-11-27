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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.log.time.DashboardTimeLog;
import net.sourceforge.processdash.log.time.TimeLoggingModel;
import net.sourceforge.processdash.rest.rs.GET;
import net.sourceforge.processdash.rest.rs.HttpException;
import net.sourceforge.processdash.rest.rs.POST;
import net.sourceforge.processdash.rest.rs.PUT;
import net.sourceforge.processdash.rest.rs.Path;
import net.sourceforge.processdash.rest.service.RestDashContext;
import net.sourceforge.processdash.rest.service.RestDefectLogService;
import net.sourceforge.processdash.rest.service.RestTaskService;
import net.sourceforge.processdash.rest.to.JsonMap;
import net.sourceforge.processdash.rest.to.RestTask;
import net.sourceforge.processdash.util.StringUtils;

@Path("/timer/")
public class TimerApi {

    @GET
    public Map getTimingState() {
        TimeLoggingModel model = getModel();
        Map state = new LinkedHashMap();
        state.put("timing", !model.isPaused());
        state.put("timingAllowed", model.isLoggingAllowed());

        RestTask task = RestTaskService.get().loadData(
            RestTaskService.get().byKey(model.getActiveTaskModel().getNode()));
        state.put("defectsAllowed",
            RestDefectLogService.get().defectsAllowed(task));
        state.put("activeTask", task);

        return new JsonMap("timer", state, "stat", "ok");
    }


    @POST
    @PUT
    public Map setTimingState(HttpServletRequest req) {
        TimeLoggingModel model = getModel();

        // if the client has requested a change to the active task, oblige
        String activeTaskID = req.getParameter("activeTaskId");
        if (StringUtils.hasValue(activeTaskID)) {
            RestTask task = RestTaskService.get().byID(activeTaskID);

            // clients will often pass the ID of a project root as a way of
            // requesting that we switch projects. Unfortunately, this doesn't
            // have the desired effect for the "Other Tasks" pseudo-project.
            // Detect such a request and handle it specially.
            if (task != null && "".equals(task.getFullPath())) {
                List<RestTask> nonProjectTasks = RestTaskService.get()
                        .forProject(task.getProject(), true);
                if (!nonProjectTasks.isEmpty())
                    task = nonProjectTasks.get(0);
            }

            if (task == null)
                throw HttpException.notFound();
            else
                model.getActiveTaskModel().setPath(task.getFullPath());
        }

        // if the client has requested a change to the paused state, oblige
        String timing = req.getParameter("timing");
        if ("true".equals(timing))
            model.setPaused(false);
        else if ("false".equals(timing))
            model.setPaused(true);

        return getTimingState();
    }


    private TimeLoggingModel getModel() {
        DashboardContext ctx = RestDashContext.get();
        DashboardTimeLog timeLog = (DashboardTimeLog) ctx.getTimeLog();
        return timeLog.getTimeLoggingModel();
    }

}
