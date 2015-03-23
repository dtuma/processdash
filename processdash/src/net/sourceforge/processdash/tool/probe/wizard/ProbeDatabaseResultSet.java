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

package net.sourceforge.processdash.tool.probe.wizard;

import java.util.HashMap;
import java.util.Map;

import net.sourceforge.processdash.data.util.ResultSet;

public class ProbeDatabaseResultSet extends ResultSet {

    private String workflowName;

    private Map<String, Number> timeInPhase;

    public ProbeDatabaseResultSet(int numRows, String[] columnHeaders,
            String workflowName, Map timeInPhase) {
        super(numRows, ProbeData.NUM_COLUMNS);

        setColName(0, Wizard.resources.getString("Project_Task"));
        for (int i = columnHeaders.length; i-- > 0;)
            setColName(i + 1, columnHeaders[i]);

        this.workflowName = workflowName;
        this.timeInPhase = timeInPhase;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public Map<String, Number> getTimeInPhase() {
        if (timeInPhase == null)
            timeInPhase = new HashMap();
        return timeInPhase;
    }

}
