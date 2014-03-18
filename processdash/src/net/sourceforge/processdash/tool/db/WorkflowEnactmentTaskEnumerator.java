// Copyright (C) 2014 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.db;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.util.StringUtils;

public class WorkflowEnactmentTaskEnumerator {

    public static final String MATCH_ALL_SUFFIX = "/** ";

    private DataContext data;

    private String onePath;

    private QueryRunner query;

    private DashHierarchy hier;

    private String projectID;

    private Integer workflowProcessKey;

    private String workflowProcessName;

    private Map<String, String> workflowPhases;

    private Set<String> enactmentWbsIDs;

    private String rootItemPath;

    private PropertyKey rootItemKey;

    private String rootItemWbsID;

    private Map<String, String> enactmentTasks;

    public WorkflowEnactmentTaskEnumerator(DataContext data, String onePath) {
        this.data = data;
        this.onePath = onePath;
        lookupData();
    }

    public String getWorkflowProcessName() {
        return workflowProcessName;
    }

    public Map<String, String> getWorkflowPhases() {
        return workflowPhases;
    }

    public String getRootItemPath() {
        return rootItemPath;
    }

    public String getRootItemWbsID() {
        return rootItemWbsID;
    }

    public Map<String, String> getEnactmentTasks() {
        return enactmentTasks;
    }

    private void lookupData() {
        try {
            lookupObjects();
            loadWorkflowInfo();
            findCurrentEnactmentRoot();
            if (rootItemKey != null) {
                enactmentTasks = new HashMap();
                enumerateCurrentTasks(rootItemKey, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void lookupObjects() {
        DatabasePlugin db = require(QueryUtils.getDatabasePlugin(data));
        query = require(db.getObject(QueryRunner.class));

        ListData hierItem = (ListData) data
                .getSimpleValue(DashHierarchy.DATA_REPOSITORY_NAME);
        hier = (DashHierarchy) hierItem.get(0);

        projectID = DataRepository
                .getInheritableValue(data, new StringBuffer(onePath),
                    PROJECT_ID).getSimpleValue().format();
        db.getObject(ProjectLocator.class).getKeyForProject(projectID, null);
    }

    private void loadWorkflowInfo() {
        // identify the workflow process that generated this task
        String workflowID = getPrimaryWorkflowID(onePath);
        String fullWorkflowID = DatabasePluginUtils.getWorkflowPhaseIdentifier(
            projectID, workflowID);
        List rawData = query.queryHql(WORKFLOW_PROCESS_QUERY, fullWorkflowID);
        Object[] row = (Object[]) rawData.get(0);
        workflowProcessKey = (Integer) row[0];
        workflowProcessName = (String) row[1];

        // retrieve the phases in this workflow.
        rawData = query.queryHql(WORKFLOW_PHASE_QUERY, workflowProcessKey);
        workflowPhases = QueryUtils.mapColumns(rawData, 0, 1);
    }

    private static final String WORKFLOW_PROCESS_QUERY = //
    "select phase.process.key, phase.process.name "
            + "from Phase phase where phase.identifier = ?";

    private static final String WORKFLOW_PHASE_QUERY = //
    "select phase.identifier, phase.shortName "
            + "from Phase phase where phase.process.key = ?";


    private void findCurrentEnactmentRoot() {
        // find the WBS IDs of all of the tasks in this enactment
        String wbsID = getString(onePath, WBS_ID);
        String projectWbsID = projectID + ":" + wbsID;
        enactmentWbsIDs = new HashSet(query.queryHql(ENACTMENT_ITEM_QUERY,
            workflowProcessKey, projectWbsID));

        rootItemPath = onePath;
        String path = DataRepository.chopPath(onePath);
        while (path != null) {
            // get the ID of the dashboard node at path. If it is part of
            // the current enactment, record it as a possible root.
            wbsID = getString(path, WBS_ID);
            projectWbsID = projectID + ":" + wbsID;
            if (enactmentWbsIDs.contains(projectWbsID))
                rootItemPath = path;

            path = DataRepository.chopPath(path);
        }

        rootItemKey = require(hier.findExistingKey(rootItemPath));
    }

    private static final String ENACTMENT_ITEM_QUERY = //
    "select pi.includesItem.identifier "
            + "from ProcessEnactment pe, ProcessEnactment pi "
            + "where pe.process.key = ? "
            + "and pe.includesItem.identifier = ? "
            + "and pe.rootItem.key = pi.rootItem.key "
            + "and pe.process.key = pi.process.key";

    private void enumerateCurrentTasks(PropertyKey parentNode, String parentType) {
        int numChildren = hier.getNumChildren(parentNode);
        if (numChildren == 0) {
            if (parentType != null)
                enactmentTasks.put(parentNode.path(), parentType);

        } else {
            for (int i = numChildren; i-- > 0;) {
                PropertyKey child = hier.getChildKey(parentNode, i);
                String childType = getWorkflowType(child.path(), parentType);

                if (childType != null && childType.endsWith(MATCH_ALL_SUFFIX))
                    enactmentTasks.put(child.path(), childType);
                else
                    enumerateCurrentTasks(child, childType);
            }
        }
    }

    private String getWorkflowType(String nodePath, String parentType) {
        // get the workflow source ID of the given node
        String workflowID = getPrimaryWorkflowID(nodePath);
        if (!StringUtils.hasValue(workflowID))
            return parentType;

        // translate this into a database-specific phase ID
        String phaseID = DatabasePluginUtils.getWorkflowPhaseIdentifier(
            projectID, workflowID);

        // if this node is a PSP task, it requires special handling
        if (isPspTask(nodePath)) {
            String planningPhaseID = phaseID + PLANNING;
            String planningPhaseName = workflowPhases.get(planningPhaseID);
            if (planningPhaseName != null) {
                String baseName = planningPhaseName.substring(0,
                    planningPhaseName.length() - PLANNING.length());
                return baseName + MATCH_ALL_SUFFIX;
            } else {
                return parentType;
            }

        } else {
            // look up the name of the phase with that ID
            String phaseName = workflowPhases.get(phaseID);
            return (phaseName == null ? parentType : phaseName);
        }
    }

    private String getPrimaryWorkflowID(String path) {
        // look up the workflow ID for this path.
        String workflowID = getString(path, WORKFLOW_SOURCE_ID);
        if (workflowID == null)
            return null;

        // if this has multiple workflow IDs, only return the first one.
        int commaPos = workflowID.indexOf(',');
        if (commaPos == -1)
            return workflowID;
        else
            return workflowID.substring(0, commaPos);
    }

    private boolean isPspTask(String path) {
        return getValue(path, PSP_TAG) != null;
    }

    private String getString(String prefix, String name) {
        SimpleData sd = getValue(prefix, name);
        return (sd == null ? null : sd.format());
    }

    private SimpleData getValue(String prefix, String name) {
        return data.getSimpleValue(DataRepository.createDataName(prefix, name));
    }

    private <T> T require(Object obj) {
        if (obj == null)
            throw new NullPointerException();
        return (T) obj;
    }

    private static final String PROJECT_ID = "Project_ID";

    private static final String WBS_ID = "WBS_Unique_ID";

    private static final String WORKFLOW_SOURCE_ID = "Workflow_Source_ID";

    private static final String PSP_TAG = "PSP Project";

    private static final String PLANNING = "/Planning";

}
