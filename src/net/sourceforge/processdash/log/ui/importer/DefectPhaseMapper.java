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

package net.sourceforge.processdash.log.ui.importer;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.log.defects.Defect;
import net.sourceforge.processdash.log.defects.DefectPhase;
import net.sourceforge.processdash.log.defects.DefectPhaseList;
import net.sourceforge.processdash.log.defects.DefectUtil;
import net.sourceforge.processdash.log.defects.PhaseLookup;
import net.sourceforge.processdash.process.WorkflowInfo.Phase;
import net.sourceforge.processdash.process.WorkflowInfo.Workflow;
import net.sourceforge.processdash.ui.lib.binding.BoundMap;

public class DefectPhaseMapper implements PhaseLookup {

    private static final String ID_PREFIX = DefectPhaseMapper.class.getName();

    public static final String PHASE_LOOKUP_ID = ID_PREFIX + ".Phase_Lookup";

    public static final String WORKFLOW_PHASE_LIST_ID = ID_PREFIX
            + ".Workflow_Phase_List";

    public static final String PROCESS_PHASE_LIST_ID = ID_PREFIX
            + ".Process_Phase_List";

    private DefectPhaseList workflowPhases, processPhases, defectPhases;

    public DefectPhaseMapper(BoundMap map, String defectLogPath,
            String selectedPath, DashboardContext dashboardContext) {

        // retrieve any phases associated with the effective workflow
        workflowPhases = DefectUtil.getWorkflowDefectPhases(selectedPath,
            dashboardContext);
        map.put(WORKFLOW_PHASE_LIST_ID, workflowPhases);

        // retrieve the list of phases for the current process
        processPhases = DefectUtil.getDefectPhaseList(defectLogPath,
            selectedPath, dashboardContext);
        map.put(PROCESS_PHASE_LIST_ID, processPhases);

        // pick the list of phases that users will choose from
        if (workflowPhases != null && !workflowPhases.isEmpty())
            defectPhases = workflowPhases;
        else
            defectPhases = processPhases;
        map.put(DefaultPhaseSelector.PHASE_LIST_ID, defectPhases);

        // set the default injection and removal phases
        Object injectionPhase = getPhaseFromList(defectPhases,
            defectPhases.defaultInjectionPhase);
        map.put(DefaultPhaseSelector.INJ_PHASE_ID, injectionPhase);
        Object removalPhase = getPhaseFromList(defectPhases,
            defectPhases.defaultRemovalPhase);
        map.put(DefaultPhaseSelector.REM_PHASE_ID, removalPhase);

        // store this object in the map
        map.put(PHASE_LOOKUP_ID, this);
    }

    private Object getPhaseFromList(DefectPhaseList list, int pos) {
        if (pos == -1)
            return Defect.UNSPECIFIED_PHASE;
        else
            return list.get(pos);
    }


    public DefectPhase getPhaseForName(String phaseName) {
        DefectPhase result = findPhase(workflowPhases, phaseName);
        if (result == null)
            result = findPhase(processPhases, phaseName);
        if (result == null)
            result = findWorkflowInfoPhase(phaseName);
        return result;
    }

    private DefectPhase findPhase(DefectPhaseList list, String phaseName) {
        if (list != null) {
            for (DefectPhase onePhase : list) {
                if (onePhase.phaseName.equalsIgnoreCase(phaseName))
                    return onePhase;
            }
        }
        return null;
    }

    private DefectPhase findWorkflowInfoPhase(String phaseName) {
        if (workflowPhases != null && workflowPhases.workflowInfo != null) {
            for (Workflow workflow : workflowPhases.workflowInfo.getWorkflows()) {
                for (Phase onePhase : workflow.getPhases()) {
                    if (onePhase.getPhaseName().equalsIgnoreCase(phaseName))
                        return new DefectPhase(onePhase);
                }
            }
        }
        return null;
    }

}
