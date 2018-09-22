// Copyright (C) 2016-2018 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package net.sourceforge.processdash.team.ui;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.ImmutableDoubleData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.tool.db.WorkflowEnactmentHelper;
import net.sourceforge.processdash.tool.db.WorkflowEnactmentHelper.TaskMapType;
import net.sourceforge.processdash.tool.db.WorkflowEnactmentHelper.TaskNodeType;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;


public class WorkflowTaskPlanSummary extends TinyCGIBase {

    @Override
    protected void doGet() throws IOException {

        // retrieve information about the workflow this task is a part of.
        DataContext data = getDataContext();
        WorkflowEnactmentHelper workflow = new WorkflowEnactmentHelper(data,
                getPrefix());
        Map<String, String> tasks = workflow
                .getEnactmentTasks(TaskMapType.PhaseID);
        if (tasks == null || tasks.isEmpty()) {
            out.write("Location: probeSummaryNA.shtm\r\n\r\n");
            return;
        }
        ListData weh = new ListData();
        weh.add(workflow);

        String rootPath = workflow.getRootItemPath();
        String workflowName = workflow.getWorkflowProcessName();
        String workflowID = workflow.getWorkflowProcessID();

        // Gather up information about the tasks in this workflow enactment.
        StringBuffer uri = new StringBuffer().append(env.get("SCRIPT_PATH"))
                .append(".shtm");
        ListData fullPaths = new ListData();
        ListData orphanedTimePaths = new ListData();
        Map<String, ListData> coqLists = new HashMap();
        for (String coqType : COQ_TYPES)
            coqLists.put(coqType, new ListData());
        ListData phaseNums = new ListData();

        int phaseNum = 0, numRoots = 0;
        String lastRootPath = rootPath;
        for (Entry<String, String> e : tasks.entrySet()) {
            String taskPath = e.getKey();
            TaskNodeType nodeType = workflow.getNodeType(taskPath);
            if (nodeType == TaskNodeType.Leaf || nodeType == TaskNodeType.PSP) {
                fullPaths.add(taskPath);
            } else if (nodeType == TaskNodeType.Root) {
                lastRootPath = taskPath;
                HTMLUtils.appendQuery(uri, phaseNum + "_Is_Row_Group", "t");
                HTMLUtils.appendQuery(uri, phaseNum + "_Abs_Path", taskPath);
                phaseNums.add(Integer.toString(phaseNum++));
                numRoots++;
                continue;
            }

            if (nodeType != TaskNodeType.Parent
                    || hasOrphanedTime(data, taskPath)) {
                String shortName = taskPath.substring(lastRootPath.length() + 1);
                HTMLUtils.appendQuery(uri, phaseNum + "_Rel_Path", shortName);
                HTMLUtils.appendQuery(uri, phaseNum + "_Abs_Path", taskPath);
                if (nodeType == TaskNodeType.Parent) {
                    orphanedTimePaths.add(taskPath);
                    HTMLUtils.appendQuery(uri, phaseNum + "_Orphan", "t");
                }
                phaseNums.add(Integer.toString(phaseNum++));
            }

            String phaseID = e.getValue();
            String phaseType = workflow.getWorkflowPhaseTypes().get(phaseID);
            if (phaseType != null && !"PSP".equals(phaseType)) {
                ListData coqList = coqLists.get(phaseType);
                if (coqList == null)
                    coqList = coqLists.get("Failure");
                coqList.add(taskPath);
            }
        }
        HTMLUtils.appendQuery(uri, "Row_Group_Type",
            numRoots > 1 ? "showRowGroups" : "hideRowGroups");

        String projectPath = workflow.getProjectRootPath();
        ListData phaseIDs = new ListData();
        String lastPhaseName = null;
        for (Entry<String, String> e : workflow.getWorkflowPhaseNames()
                .entrySet()) {
            // the phase ID will be of the form "WF:projID:phaseID". Discard
            // the initial portion, so we just have the project-relative ID
            String phaseID = e.getKey();
            int colonPos = phaseID.lastIndexOf(':');
            if (colonPos != -1)
                phaseID = phaseID.substring(colonPos + 1);
            phaseIDs.add(phaseID);

            // store the phase name in the parameters
            String phaseName = lastPhaseName = e.getValue();
            HTMLUtils.appendQuery(uri, phaseID + "_Name", phaseName);

            // copy quality params for this phase from the project root
            copyWorkflowPhaseQualityParams(data, projectPath, phaseID);
        }
        HTMLUtils.appendQuery(uri, "Last_Phase_Name", lastPhaseName);

        // Write data into the repository for use by the plan summary form
        data.putValue("Workflow_Root_Path", StringData.create(rootPath));
        data.putValue("Workflow_Name", StringData.create(workflowName));
        data.putValue("Workflow_ID", StringData.create(workflowID));
        data.putValue("Workflow_Task_Paths", fullPaths);
        data.putValue("Workflow_Orphaned_Time_Paths", orphanedTimePaths);
        for (String coqType : COQ_TYPES)
            data.putValue("Workflow_Task_Paths/" + coqType,
                coqLists.get(coqType));
        data.putValue("Workflow//Enactment_Helper", weh);
        data.putValue("Workflow//Phase_Nums", phaseNums);
        data.putValue("Workflow//Phase_IDs", phaseIDs);

        String html = getRequestAsString(uri.toString());
        writeHeader();
        out.write(html);
    }

    private void copyWorkflowPhaseQualityParams(DataContext data,
            String projectPath, String phaseID) {
        // the workflow quality parameters are copied into the project root by
        // the sync logic. Construct the prefix where the parameters should be
        // stored for this phase.
        String srcPrefix = projectPath + "/"
                + TeamDataConstants.WORKFLOW_PARAM_PREFIX + "/";
        int slashPos = phaseID.indexOf('/');
        if (slashPos == -1)
            srcPrefix += phaseID;
        else
            srcPrefix += "PSP" + phaseID.substring(slashPos);

        // copy each phase parameter over
        for (String param : WORKFLOW_QUALITY_PARAMS) {
            // get the relevant data names for this parameter
            String srcDataName = srcPrefix + "/" + param;
            String destDataName = TeamDataConstants.WORKFLOW_PARAM_PREFIX + "/"
                    + phaseID + "/" + param;
            String syncDataName = destDataName + "_Last_Synced_Val";

            // if this param has never been copied, or if it has not been edited
            // since the last copy, refresh it with data from the project root
            SimpleData newVal = data.getSimpleValue(srcDataName);
            if (newVal == null)
                newVal = ImmutableDoubleData.EDITABLE_ZERO;
            SimpleData destVal = data.getSimpleValue(destDataName);
            SimpleData syncVal = data.getSimpleValue(syncDataName);
            if (!newVal.equals(destVal)
                    && (syncVal == null || syncVal.equals(destVal))) {
                data.putValue(destDataName, newVal);
                data.putValue(syncDataName, newVal);
            }
        }
    }

    private boolean hasOrphanedTime(DataContext data, String path) {
        String dataName = DataRepository.createDataName(path, "Orphaned Time");
        SimpleData sd = data.getSimpleValue(dataName);
        return (sd != null && sd.test());
    }

    private static final String[] COQ_TYPES = { "Overhead", "Construction",
            "Appraisal", "Failure" };

    private static final String[] WORKFLOW_QUALITY_PARAMS = {
            "Estimated Defects Injected per Hour", "Estimated % Phase Yield" };

}
