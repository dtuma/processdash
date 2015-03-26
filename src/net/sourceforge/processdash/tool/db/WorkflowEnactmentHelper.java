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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.util.StringUtils;

/**
 * @since 2.0.9
 */
public class WorkflowEnactmentHelper {

    public enum TaskNodeType { Root, Parent, Leaf, PSP, PspPhase };

    public enum TaskMapType { PhaseID, PhaseName, PhaseType };

    private DataContext data;

    private String onePath;

    private QueryRunner query;

    private DashHierarchy hier;

    private String projectID;

    private Integer workflowProcessKey;

    private String workflowProcessName;

    private Map<String, String> workflowPhaseNames;

    private Map<String, String> workflowPhaseTypes;

    private Set<String> enactmentWbsIDs;

    private String rootItemPath;

    private PropertyKey rootItemKey;

    private String rootItemWbsID;

    private Map<String, String> enactmentTasks;

    private Map<String, TaskNodeType> nodeTypes;

    public WorkflowEnactmentHelper(DataContext data, String onePath) {
        this.data = data;
        this.onePath = onePath;
    }

    public String getWorkflowProcessName() {
        if (workflowProcessName == null)
            tryLoadWorkflowInfo();
        return workflowProcessName;
    }

    public Map<String, String> getWorkflowPhaseNames() {
        if (workflowPhaseNames == null)
            tryLoadWorkflowInfo();
        return workflowPhaseNames;
    }

    public Map<String, String> getWorkflowPhaseTypes() {
        if (workflowPhaseTypes == null)
            tryLoadWorkflowInfo();
        return workflowPhaseTypes;
    }

    public TaskNodeType getNodeType(String taskPath) {
        if (nodeTypes == null)
            return null;
        else
            return nodeTypes.get(taskPath);
    }

    public String getRootItemPath() {
        if (rootItemPath == null)
            tryFindCurrentEnactmentRoot();
        return rootItemPath;
    }

    public String getRootItemWbsID() {
        if (rootItemWbsID == null)
            tryFindCurrentEnactmentRoot();
        return rootItemWbsID;
    }

    public Map<String, String> getEnactmentTasks(TaskMapType mapValueType,
            TaskNodeType... typesToInclude) {
        if (enactmentTasks == null)
            tryEnumerateTasks();

        if (mapValueType == TaskMapType.PhaseName)
            return buildValueMap(typesToInclude, workflowPhaseNames);
        else if (mapValueType == TaskMapType.PhaseType)
            return buildValueMap(typesToInclude, workflowPhaseTypes);
        else
            return buildValueMap(typesToInclude, null);
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


    private void tryLoadWorkflowInfo() {
        try {
            loadWorkflowInfo();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadWorkflowInfo() {
        if (query == null)
            lookupObjects();
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
        workflowPhaseNames = QueryUtils.mapColumns(rawData, 0, 1);
        workflowPhaseTypes = QueryUtils.mapColumns(rawData, 0, 2);
    }

    private static final String WORKFLOW_PROCESS_QUERY = //
    "select phase.process.key, phase.process.name "
            + "from Phase phase where phase.identifier = ?";

    private static final String WORKFLOW_PHASE_QUERY = //
    "select phase.identifier, phase.shortName, phase.typeName "
            + "from Phase phase where phase.process.key = ? "
            + "order by phase.ordinal";


    private void tryFindCurrentEnactmentRoot() {
        try {
            findCurrentEnactmentRoot();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void findCurrentEnactmentRoot() {
        // ensure the workflow info has been loaded
        if (workflowProcessKey == null)
            loadWorkflowInfo();

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


    private void tryEnumerateTasks() {
        try {
            if (enactmentWbsIDs == null)
                findCurrentEnactmentRoot();
            if (rootItemKey != null) {
                enactmentTasks = new LinkedHashMap<String, String>();
                nodeTypes = new HashMap();
                enumerateTasks(rootItemKey, null);
                nodeTypes.put(rootItemPath, TaskNodeType.Root);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean enumerateTasks(PropertyKey node, String inheritedID) {
        String nodePath = node.path();
        boolean nodeIsPSP = isPspTask(nodePath);
        String nodeID = getWorkflowPhaseID(nodePath, inheritedID, nodeIsPSP);
        enactmentTasks.put(nodePath, nodeID);
        boolean isWorkflowPart = (nodeID != null);

        int numChildren = hier.getNumChildren(node);
        if (numChildren == 0) {
            nodeTypes.put(nodePath, TaskNodeType.Leaf);

        } else if (nodeIsPSP) {
            if (nodeID != null) {
                nodeTypes.put(nodePath, TaskNodeType.PSP);
                for (int i = 0; i < numChildren; i++) {
                    PropertyKey phase = hier.getChildKey(node, i);
                    String phasePath = phase.path();
                    nodeTypes.put(phasePath, TaskNodeType.PspPhase);

                    String phaseID;
                    if (nodeID.equals(inheritedID))
                        phaseID = nodeID;
                    else
                        phaseID = nodeID + "/" + phase.name();
                    enactmentTasks.put(phasePath, phaseID);
                }
            }

        } else {
            nodeTypes.put(nodePath, TaskNodeType.Parent);
            for (int i = 0; i < numChildren; i++) {
                PropertyKey child = hier.getChildKey(node, i);
                if (enumerateTasks(child, nodeID))
                    isWorkflowPart = true;
            }
        }

        if (!isWorkflowPart) {
            enactmentTasks.remove(nodePath);
            nodeTypes.remove(nodePath);
        }

        return isWorkflowPart;
    }

    private String getWorkflowPhaseID(String nodePath, String parentID,
            boolean nodeIsPSP) {
        // get the workflow source ID of the given node
        String workflowID = getPrimaryWorkflowID(nodePath);
        if (!StringUtils.hasValue(workflowID))
            return parentID;

        // translate this into a database-specific phase ID
        String phaseID = DatabasePluginUtils.getWorkflowPhaseIdentifier(
            projectID, workflowID);

        // if this node is a PSP task, it requires special handling
        if (nodeIsPSP) {
            String planningPhaseID = phaseID + PLANNING;
            String planningPhaseName = workflowPhaseNames.get(planningPhaseID);
            if (planningPhaseName != null) {
                String pspName = planningPhaseName.substring(0,
                    planningPhaseName.length() - PLANNING.length());
                workflowPhaseNames.put(phaseID, pspName);
                workflowPhaseTypes.put(phaseID, "PSP");
                return phaseID;
            } else {
                return parentID;
            }

        } else {
            // see if we have a phase with that ID
            return (workflowPhaseNames.containsKey(phaseID) ? phaseID
                    : parentID);
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

    private Map buildValueMap(TaskNodeType[] types, Map values) {
        if (enactmentTasks == null)
            return null;

        boolean typeFlags[] = new boolean[TaskNodeType.values().length];
        if (types.length == 0) {
            Arrays.fill(typeFlags, true);
        } else {
            for (TaskNodeType t : types)
                typeFlags[t.ordinal()] = true;
        }

        Map result = new LinkedHashMap();
        for (Entry<String, String> e : enactmentTasks.entrySet()) {
            String nodePath = e.getKey();
            TaskNodeType nodeType = getNodeType(nodePath);
            if (nodeType != null && typeFlags[nodeType.ordinal()]) {
                String nodeID = e.getValue();
                Object value = values == null ? nodeID : values.get(nodeID);
                result.put(nodePath, value);
            }
        }
        return result;
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
