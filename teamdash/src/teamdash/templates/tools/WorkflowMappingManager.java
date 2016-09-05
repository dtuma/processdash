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

package teamdash.templates.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.api.PDashContext;
import net.sourceforge.processdash.api.PDashQuery;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.net.http.TinyCGI;
import net.sourceforge.processdash.tool.db.DataReloader;
import net.sourceforge.processdash.tool.db.DatabasePlugin;
import net.sourceforge.processdash.tool.db.QueryUtils;
import net.sourceforge.processdash.tool.export.DataImporter;

public class WorkflowMappingManager {

    public static final String PHASE_MAPPING_PREFIX = "Phase Mapping ";

    public static final String DELETE_MAPPINGS = "Delete Mappings";

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

    public Map<String, Set<Workflow>> getAllWorkflowsExcept(String workflowID,
            boolean forImport) {
        // get information about all of the other known workflows
        List<Object[]> rawData = query.query("select p.identifier, p.name "
                + "from Process as p " //
                + "where p.identifier <> ? " //
                + "and p.identifier like 'WF:%' " //
                + "order by p.identifier", workflowID);

        // discard mappings that cannot be edited
        for (Iterator i = rawData.iterator(); i.hasNext();) {
            Object[] row = (Object[]) i.next();
            String sourceId = (forImport ? (String) row[0] : workflowID);
            String targetId = (forImport ? workflowID : (String) row[0]);
            if (!canEditMappings(sourceId, targetId))
                i.remove();
        }

        // find workflows from various projects
        String projectID = getProjectIDForWorkflowID(workflowID);
        List<Workflow> sameProjectWorkflows = extractWorkflowsRelativeToProject(
            rawData, projectID, 0);
        List<Workflow> newerProjectWorkflows = extractWorkflowsRelativeToProject(
            rawData, projectID, 1);
        List<Workflow> olderProjectWorkflows = extractWorkflowsRelativeToProject(
            rawData, projectID, -1);
        Collections.reverse(olderProjectWorkflows);

        // add these to a result map, putting the most relevant ones first
        Map<String, Set<Workflow>> result = new LinkedHashMap();
        storeWorkflowsInProjectMap(result, sameProjectWorkflows);
        if (forImport) {
            storeWorkflowsInProjectMap(result, olderProjectWorkflows);
            storeWorkflowsInProjectMap(result, newerProjectWorkflows);
        } else {
            storeWorkflowsInProjectMap(result, newerProjectWorkflows);
            storeWorkflowsInProjectMap(result, olderProjectWorkflows);
        }
        return result;
    }

    private List<Workflow> extractWorkflowsRelativeToProject(
            List<Object[]> rawData, String projectID, int dir) {
        List<Workflow> result = new ArrayList<Workflow>();
        for (Object[] row : rawData) {
            String id = (String) row[0];
            String rowProject = getProjectIDForWorkflowID(id);
            if (rowProject != null
                    && Math.signum(rowProject.compareTo(projectID)) == dir)
                result.add(new Workflow(id, (String) row[1]));
        }
        return result;
    }

    private void storeWorkflowsInProjectMap(Map<String, Set<Workflow>> dest,
            List<Workflow> workflows) {
        for (Workflow oneWorkflow : workflows) {
            String projectName = oneWorkflow.getProject();
            Set<Workflow> projectWorkflowSet = dest.get(projectName);
            if (projectWorkflowSet == null) {
                projectWorkflowSet = new TreeSet<Workflow>();
                dest.put(projectName, projectWorkflowSet);
            }
            projectWorkflowSet.add(oneWorkflow);
        }
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
        return projectNameMap;
    }

    public void loadPhases(Workflow workflow) {
        List<Object[]> rawData = query.query("select "
                + "phase.identifier, phase.name " //
                + "from Phase as phase " //
                + "where phase.process.identifier = ? " //
                + "and phase.ordinal is not null " //
                + "order by phase.ordinal", workflow.id);
        List<Phase> phases = new ArrayList(rawData.size());
        for (Object[] row : rawData)
            phases.add(new Phase((String) row[0], (String) row[1]));
        workflow.phases = phases;
    }

    public boolean loadPhaseMappings(Workflow workflow, Workflow target) {
        // load the mappings from this workflow to the target workflow
        Map<String, String> phaseMap = QueryUtils.mapColumns(query.query(
            "select phase.identifier, mapsTo.identifier "
                    + "from Phase as phase " //
                    + "join phase.mapsToPhase mapsTo " //
                    + "where phase.process.identifier = ? "
                    + "and mapsTo.process.identifier = ?", //
            workflow.id, target.id));
        // make a map of phases in the target workflow, from name to ID
        Map<String, String> targetPhaseIDs = new HashMap<String, String>();
        for (Phase t : target.phases)
            targetPhaseIDs.put(t.name, t.id);
        // record mapping data in the source workflow
        for (Phase p : workflow.phases) {
            p.mapsTo = phaseMap.get(p.id);
            p.nameMatch = targetPhaseIDs.get(p.name);
            if (p.nameMatch != null)
                workflow.hasNameMatch = true;
        }
        return !phaseMap.isEmpty();
    }

    public boolean canEditMappings(String sourceId, String targetId) {
        String sourceScopeId = getProjectIDForWorkflowID(sourceId);
        String targetScopeId = getProjectIDForWorkflowID(targetId);
        if (sourceScopeId == null || targetScopeId == null)
            // if the scope ID is null, this is an MCF. Don't allow editing.
            return false;
        else if (sourceScopeId.startsWith("lib"))
            // if the source is an org standard workflow, don't allow editing.
            return false;
        else
            return true;
    }

    public void saveChangedMappings(Workflow workflow, Workflow target,
            Map<String, String> changes, Map env)
            throws WorkflowMappingException {
        // find the project this workflow is associated with, and get a
        // workflow alterer for that project
        String projectId = getProjectIDForWorkflowID(workflow.getId());
        WorkflowMappingAlterer alterer = WorkflowMappingAltererFactory.get(
            (DashboardContext) env.get(TinyCGI.DASHBOARD_CONTEXT), projectId);
        if (alterer == null)
            throw new NotFound("Could not find project for workflow "
                    + workflow.getId());

        // save the requested changes
        alterer.applyChanges(workflow, target, changes);

        // tell the data importer to reload data from this project. (In bridged
        // mode, this will cause it to refresh the project directory.)
        DataImporter.refreshLocation(projectId);

        // ask the warehouse to reload all data, and wait for it to finish
        DatabasePlugin databasePlugin = QueryUtils.getDatabasePlugin( //
                (DataContext) env.get(TinyCGI.DATA_REPOSITORY));
        DataReloader reloader = databasePlugin.getObject(DataReloader.class);
        reloader.reloadAllData();
    }



    public class Workflow implements Comparable<Workflow> {
        private String id, process, project;

        private List<Phase> phases;

        private boolean hasNameMatch;

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

        public List<Phase> getPhases() {
            return phases;
        }

        public boolean isHasNameMatch() {
            return hasNameMatch;
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


    public class Phase {
        private String id, name, mapsTo, nameMatch;

        Phase(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getMapsTo() {
            return mapsTo;
        }

        public String getNameMatch() {
            return nameMatch;
        }
    }


    public static class NotFound extends RuntimeException {
        private NotFound(String message) {
            super(message);
        }
    }

}
