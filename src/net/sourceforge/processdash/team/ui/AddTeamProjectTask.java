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

package net.sourceforge.processdash.team.ui;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ev.EVTask;
import net.sourceforge.processdash.hier.ui.AddTaskHandler;
import net.sourceforge.processdash.hier.ui.AddTaskTypeOption;
import net.sourceforge.processdash.process.ProcessUtil;
import net.sourceforge.processdash.ui.DashboardIconFactory;

public class AddTeamProjectTask implements AddTaskHandler {

    private DashboardContext ctx;

    public void setDashboardContext(DashboardContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public List<AddTaskTypeOption> getTaskTypes(String targetParent,
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

        // create a list of types representing the phases in our MCF
        List<AddTaskTypeOption> result = new ArrayList<AddTaskTypeOption>();
        for (String phase : phaseNames) {
            AddTaskTypeOption oneType = new AddTaskTypeOption();
            oneType.typeID = oneType.displayName = phase;
            oneType.icon = icons.get(phase);
            oneType.templateID = templateID;
            oneType.isDefault = phase.equalsIgnoreCase(currentPhase);
            result.add(oneType);
        }

        // if any problems occurred, create a single, simple task type
        if (result.isEmpty()) {
            AddTaskTypeOption oneType = new AddTaskTypeOption();
            oneType.icon = DashboardIconFactory.getTaskIcon();
            oneType.templateID = templateID;
            result.add(oneType);
        }

        return result;
    }

    @Override
    public void finalizeAddedTask(String newTaskPath,
            AddTaskTypeOption newTaskType) {
        if (newTaskType.typeID != null) {
            String dataName = DataRepository.createDataName(newTaskPath,
                EVTask.NODE_TYPE_DATA_NAME);
            StringData value = StringData.create(newTaskType.typeID);
            ctx.getData().userPutValue(dataName, value);
        }
    }

}
