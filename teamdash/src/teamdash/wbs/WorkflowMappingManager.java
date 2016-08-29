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

package teamdash.wbs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.sourceforge.processdash.api.PDashContext;
import net.sourceforge.processdash.api.PDashQuery;
import net.sourceforge.processdash.tool.db.QueryUtils;

public class WorkflowMappingManager {

    private PDashQuery query;

    private Map<String, String> projectNameMap;

    public WorkflowMappingManager(PDashContext ctx) {
        this.query = ctx.getQuery();
    }

    public Workflow getWorkflow(String workflowID) {
        String processName = QueryUtils.singleValue(query.query(
            "select name from Process where identifier = ?", workflowID));
        if (processName == null)
            throw new NotFound("No workflow with ID '" + workflowID
                    + "' was found.");

        return new Workflow(workflowID, processName);
    }

    public List<Workflow> getImportedWorkflows(String workflowID) {
        List<Object[]> rawData = query.query("select distinct "
                + "p.process.identifier, p.process.name "
                + "from Phase as p join p.mapsToPhase mapsTo "
                + "where mapsTo.process.identifier = ? "
                + "and p.process.identifier <> ? "
                + "and p.process.identifier like 'WF:%'", //
            workflowID, workflowID);
        List<Workflow> result = new ArrayList();
        for (Object[] row : rawData)
            result.add(new Workflow((String) row[0], (String) row[1]));
        Collections.sort(result);
        return result;
    }

    public List<Workflow> getExportedWorkflows(String workflowID) {
        List<Object[]> rawData = query.query("select distinct "
                + "mapsTo.process.identifier, mapsTo.process.name "
                + "from Phase as p join p.mapsToPhase mapsTo "
                + "where p.process.identifier = ? "
                + "and mapsTo.process.identifier <> ? "
                + "and mapsTo.process.identifier like 'WF:%'", //
            workflowID, workflowID);
        List<Workflow> result = new ArrayList();
        for (Object[] row : rawData)
            result.add(new Workflow((String) row[0], (String) row[1]));
        Collections.sort(result);
        return result;
    }

    public String getProjectNameForWorkflowID(String workflowID) {
        String projectID = getProjectIDForWorkflowID(workflowID);
        if (projectID != null && projectID.startsWith("lib"))
            return WorkflowMappingEditor.resources.getString("Org_Standards");
        else
            return getProjectNameMap().get(projectID);
    }

    private String getProjectIDForWorkflowID(String workflowID) {
        if (!workflowID.startsWith("WF:"))
            return null;
        int colonPos = workflowID.indexOf(':', 3);
        if (colonPos == -1)
            return null;
        return workflowID.substring(3, colonPos);
    }

    private Map<String, String> getProjectNameMap() {
        if (projectNameMap == null) {
            projectNameMap = QueryUtils.mapColumns(query
                    .query("select p.value.text, p.project.name "
                            + "from ProjectAttrFact as p "
                            + "where p.attribute.identifier "
                            + "   = 'project.pdash.team_project_id'"));
        }
        System.out.println(projectNameMap);
        return projectNameMap;
    }



    public class Workflow implements Comparable<Workflow> {
        private String id, process, project;

        Workflow(String id, String process) {
            this.id = id;
            this.process = process;
            this.project = getProjectNameForWorkflowID(id);
        }

        public String getId() {
            return id;
        }

        public String getProcess() {
            return process;
        }

        public String getProject() {
            return project;
        }

        @Override
        public int compareTo(Workflow that) {
            int result = this.process.compareTo(that.process);
            if (result != 0)
                return result;
            else
                return this.id.compareTo(that.id);
        }

    }

    public static class NotFound extends RuntimeException {
        private NotFound(String message) {
            super(message);
        }
    }

}
