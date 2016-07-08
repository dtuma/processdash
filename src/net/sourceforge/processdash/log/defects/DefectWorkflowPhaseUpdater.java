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

package net.sourceforge.processdash.log.defects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.sourceforge.processdash.process.WorkflowInfo;
import net.sourceforge.processdash.process.WorkflowInfo.Phase;
import net.sourceforge.processdash.util.StringUtils;

public class DefectWorkflowPhaseUpdater implements DefectAnalyzer.Task {

    private WorkflowInfo info;

    private boolean saveInvalidPhases;

    private Set<String> pathsWithNewInvalidPhases;

    public DefectWorkflowPhaseUpdater(WorkflowInfo info,
            boolean saveInvalidPhases) {
        this.info = info;
        this.saveInvalidPhases = saveInvalidPhases;
        this.pathsWithNewInvalidPhases = new LinkedHashSet<String>();
    }

    @Override
    public void analyze(String path, Defect d) {
        boolean needsSave = false;
        if (maybeFixPhase(path, d.injected)) {
            d.phase_injected = d.injected.legacyPhase;
            needsSave = true;
        }
        if (maybeFixPhase(path, d.removed)) {
            d.phase_removed = d.removed.legacyPhase;
            needsSave = true;
        }
        if (needsSave)
            defectNeedsSave(path, d);
    }

    public void defectNeedsSave(String path, Defect d) {
        // no-op implmentation; subclasses can override
    }

    public Set<String> getPathsWithNewInvalidPhases() {
        return pathsWithNewInvalidPhases;
    }

    private boolean maybeFixPhase(String path, DefectPhase p) {
        if (p == null || p.phaseID == null)
            // this is not a workflow phase. Nothing to do
            return false;

        boolean needsSave = false;

        Phase workflowPhase = null;
        if (p.phaseID.indexOf(',') == -1) {
            // Look up the phase specified by this phaseID.
            workflowPhase = info.getPhase(p.phaseID);

        } else {
            // this phaseID field is multivalued, with comma-separated IDs
            // specifying nested workflow phases. Scan these IDs to make
            // sure they are still valid.
            List idList = new ArrayList(Arrays.asList(p.phaseID.split(",")));
            for (int i = idList.size(); i-- > 0;) {
                String oneID = (String) idList.get(i);
                Phase onePhase = info.getPhase(oneID);
                if (onePhase == null) {
                    idList.remove(i);
                    needsSave = true;
                } else if (workflowPhase == null) {
                    workflowPhase = onePhase;
                }
            }
            if (idList.isEmpty()) {
                // if we scanned all of the items in the ID list and came
                // up emptyhanded, don't discard the bad IDs.
                needsSave = false;
            } else if (needsSave) {
                // if we modified the ID list without completely emptying
                // it, save the new value into the DefectPhase object.
                p.phaseID = StringUtils.join(idList, ",");
            }
        }

        if (workflowPhase == null) {
            // the workflow phase with this ID no longer exists in the
            // project. See if we can find a phase with the same name; if
            // so, update the phaseID to match what we found.
            workflowPhase = info.getPhase(p.processName, p.phaseName.trim());
            if (workflowPhase != null) {
                p.phaseID = workflowPhase.getPhaseId();
                needsSave = true;
            }
        }

        if (workflowPhase == null) {
            // The phaseID is invalid, and we couldn't fix it. Erroneous
            // phases like this should be flagged with a terminal space in
            // the phase name.If that space isn't present, add it.
            if (!p.phaseName.endsWith(" ")) {
                p.phaseName += " ";
                needsSave = saveInvalidPhases;
                pathsWithNewInvalidPhases.add(path);
            }

        } else {
            // we found an appropriate workflow phase. Make certain the
            // names and MCF phase match; update them if needed.
            if (p.updateFrom(workflowPhase))
                needsSave = true;
        }

        return needsSave;
    }

}