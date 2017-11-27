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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.sourceforge.processdash.rest.rs.GET;
import net.sourceforge.processdash.rest.rs.HttpException;
import net.sourceforge.processdash.rest.rs.Path;
import net.sourceforge.processdash.rest.service.RestProjectService;
import net.sourceforge.processdash.rest.service.RestTaskService;
import net.sourceforge.processdash.rest.to.JsonMap;
import net.sourceforge.processdash.rest.to.RestProject;
import net.sourceforge.processdash.rest.to.RestTask;

@Path("/projects/")
public class ProjectApi {

    @GET
    public Map getProjects() {
        // retrieve list of projects
        List<RestProject> projects = RestProjectService.get().all();
        Collections.sort(projects);

        // build result
        return new JsonMap("projects", projects, "stat", "ok");
    }


    @GET
    @Path("{id}/")
    public Map getProject(String projectId) {
        // retrieve project
        RestProject project = RestProjectService.get().byID(projectId);
        if (project == null)
            throw HttpException.notFound();

        // build result
        return new JsonMap("project", project, "stat", "ok");
    }


    @GET
    @Path("{id}/tasks/")
    public Map getProjectTasks(String projectId) {
        // retrieve project
        RestProject project = RestProjectService.get().byID(projectId);
        if (project == null)
            throw HttpException.notFound();

        // get the tasks for this project. load completion date for each task,
        // but clear the redundant "project" attr to reduce the size of the
        // resulting JSON document
        List<RestTask> tasks = RestTaskService.get().forProject(project, true);
        for (RestTask t : tasks) {
            RestTaskService.get().loadData(t,
                RestTaskService.TASK_COMPLETION_DATE);
            t.remove("project");
        }

        // build result
        return new JsonMap("projectTasks", tasks, "forProject", project, //
                "stat", "ok");
    }

}
