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

import static net.sourceforge.processdash.util.NullSafeObjectUtils.EQ;

import net.sourceforge.processdash.process.WorkflowInfo;


public class DefectPhase {

    public String processName;

    public String phaseName;

    public String phaseID;

    public String legacyPhase;

    public DefectPhase(String name) {
        this.phaseName = name;
        this.legacyPhase = name;
    }

    public DefectPhase(WorkflowInfo.Phase phase) {
        this.processName = phase.getWorkflow().getWorkflowName();
        this.phaseName = phase.getPhaseName();
        this.phaseID = phase.getPhaseId();
        this.legacyPhase = phase.getMcfPhase();
    }

    public String getTerminalPhaseID() {
        if (phaseID == null)
            return null;

        int commaPos = phaseID.lastIndexOf(',');
        if (commaPos == -1)
            return phaseID;
        else
            return phaseID.substring(commaPos + 1);
    }

    @Override
    public String toString() {
        return phaseName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((legacyPhase == null) ? 0 : legacyPhase.hashCode());
        result = prime * result + ((phaseID == null) ? 0 : phaseID.hashCode());
        result = prime * result
                + ((phaseName == null) ? 0 : phaseName.hashCode());
        result = prime * result
                + ((processName == null) ? 0 : processName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof DefectPhase) {
            DefectPhase that = (DefectPhase) obj;
            return EQ(this.processName, that.processName) //
                    && EQ(this.phaseName, that.phaseName) //
                    && EQ(this.phaseID, that.phaseID) //
                    && EQ(this.legacyPhase, that.legacyPhase);
        } else {
            return false;
        }
    }


}
