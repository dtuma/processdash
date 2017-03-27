// Copyright (C) 2016-2017 Tuma Solutions, LLC
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

package net.sourceforge.processdash.team.ui;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ev.EVTask;
import net.sourceforge.processdash.hier.ui.AddTaskHandler;
import net.sourceforge.processdash.hier.ui.AddTaskTypeOption;
import net.sourceforge.processdash.hier.ui.icons.HierarchyIcons;
import net.sourceforge.processdash.process.ProcessUtil;
import net.sourceforge.processdash.team.TeamDataConstants;

public class AddTeamProjectTask implements AddTaskHandler<String[]> {

    private DashboardContext ctx;

    public void setDashboardContext(DashboardContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public List<AddTaskTypeOption<String[]>> getTaskTypes(String targetParent,
            String activeTask) {
        // find the root node of the enclosing project
        StringBuffer path = new StringBuffer(activeTask);
        ctx.getData().getInheritableValue(path, "Project_ID");
        String projectRoot = path.toString();

        // create a ProcessUtil object to extract process metadata
        ProcessUtil proc = new ProcessUtil(ctx.getData(), projectRoot);
        String templateID = proc.getProcessID() + "/Indiv2Task";
        List<String> phaseNames = proc.getProcessListPlain("Phase_List");
        TeamPhaseIconSet icons = new TeamPhaseIconSet(phaseNames);
        String currentPhase = proc.getEffectivePhase(activeTask, true);
        SimpleData currentWorkflowID = ctx.getData().getSimpleValue(
            DataRepository.createDataName(activeTask,
                TeamDataConstants.WORKFLOW_ID_DATA_NAME));

        // create a list of types representing the phases in our MCF
        List<AddTaskTypeOption<String[]>> result = new ArrayList();
        for (String phase : phaseNames) {
            AddTaskTypeOption<String[]> oneType = new AddTaskTypeOption();
            oneType.typeID = new String[] { phase, null };
            oneType.displayName = phase;
            oneType.icon = icons.get(phase);
            oneType.templateID = templateID;
            oneType.isDefault = phase.equalsIgnoreCase(currentPhase);
            if (oneType.isDefault && currentWorkflowID != null
                    && currentWorkflowID.test()) {
                oneType.typeID[1] = currentWorkflowID.format();
            }
            result.add(oneType);
        }

        // if any problems occurred, create a single, simple task type
        if (result.isEmpty()) {
            AddTaskTypeOption oneType = new AddTaskTypeOption();
            oneType.icon = HierarchyIcons.getTaskIcon();
            oneType.templateID = templateID;
            result.add(oneType);
        }

        return result;
    }

    @Override
    public void finalizeAddedTask(String newTaskPath,
            AddTaskTypeOption<String[]> newTaskType) {
        if (newTaskType.typeID != null) {
            saveString(newTaskPath, EVTask.NODE_TYPE_DATA_NAME,
                newTaskType.typeID[0]);
            saveString(newTaskPath, TeamDataConstants.WORKFLOW_ID_DATA_NAME,
                newTaskType.typeID[1]);
        }
    }

    private void saveString(String path, String name, String value) {
        if (value != null) {
            String dataName = DataRepository.createDataName(path, name);
            ctx.getData().userPutValue(dataName, StringData.create(value));
        }
    }

}
