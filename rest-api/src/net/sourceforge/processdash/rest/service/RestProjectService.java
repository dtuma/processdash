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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.Filter;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.rest.to.RestProject;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.util.StringUtils;

public class RestProjectService {

    private static RestProjectService svc;

    public static RestProjectService get() {
        if (svc == null)
            svc = new RestProjectService();
        return svc;
    }


    private Map<String, RestProject> idMap;

    private Map<String, RestProject> pathMap;

    private RestProjectService() {
        RestDashContext.get().getHierarchy()
                .addHierarchyListener(new DashHierarchy.Listener() {
                    public void hierarchyChanged(DashHierarchy.Event e) {
                        idMap = pathMap = null;
                    }
                });
    }

    public List<RestProject> all() {
        ensureProjects();
        return new ArrayList<RestProject>(idMap.values());
    }

    public RestProject byID(String projectID) {
        ensureProjects();
        return idMap.get(projectID);
    }

    public RestProject byPath(String projectPath) {
        ensureProjects();
        return pathMap.get(projectPath);
    }

    public RestProject containingPath(String taskPath) {
        ensureProjects();
        for (Entry<String, RestProject> e : pathMap.entrySet()) {
            String projectPath = e.getKey();
            if (projectPath.length() > 0
                    && Filter.pathMatches(taskPath, projectPath))
                return e.getValue();
        }
        return ROOT_PROJECT;
    }

    private void ensureProjects() {
        if (idMap == null || pathMap == null)
            buildProjects();
    }

    private void buildProjects() {
        idMap = new LinkedHashMap<String, RestProject>();
        idMap.put(ROOT_PROJECT.getId(), ROOT_PROJECT);
        pathMap = new LinkedHashMap<String, RestProject>();
        pathMap.put(ROOT_PROJECT.getFullName(), ROOT_PROJECT);
        buildProjects(RestDashContext.get().getHierarchy(), PropertyKey.ROOT);
    }

    private void buildProjects(DashHierarchy hier, PropertyKey node) {
        String templateID = hier.pget(node).getID();
        if (!StringUtils.hasValue(templateID)) {
            for (int i = 0, n = hier.getNumChildren(node); i < n; i++)
                buildProjects(hier, hier.getChildKey(node, i));

        } else if (templateID.endsWith("/Indiv2Root")
                || templateID.endsWith("/IndivRoot")) {
            String path = node.path();
            SimpleData sd = RestDashContext.get().getData()
                    .getSimpleValue(path + "/" + TeamDataConstants.PROJECT_ID);
            if (sd == null)
                return; // shouldn't happen

            String projectID = sd.format();
            int slashPos = path.lastIndexOf('/');
            String name = path.substring(slashPos + 1);
            RestProject project = new RestProject(projectID, name, path);
            idMap.put(projectID, project);
            pathMap.put(path, project);
        }
    }

    private static final RestProject ROOT_PROJECT = new RestProject("0",
            "Other Tasks", "");

}

