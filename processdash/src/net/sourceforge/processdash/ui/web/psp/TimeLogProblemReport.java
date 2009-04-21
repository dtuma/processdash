// Copyright (C) 2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.psp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.process.ProcessUtil;
import net.sourceforge.processdash.util.StringUtils;

public class TimeLogProblemReport extends PspForEngBase {

    @Override
    protected void writeContents() throws IOException {
        out.println(HEADER);

        DataContext data = getDataContext();
        ProcessUtil process = new ProcessUtil(data);
        List phases = process.getProcessListPlain("Phase_List");
        phases = process.filterPhaseList(phases);

        List<String> missingPhases = new ArrayList<String>();
        for (Iterator i = phases.iterator(); i.hasNext();) {
            String phase = (String) i.next();
            if (getBoolParam("Check" + phase)) {
                SimpleData phaseTime = data.getSimpleValue(phase + "/Time");
                if (phaseTime == null || !phaseTime.test())
                    missingPhases.add(phase);
            }
        }

        if (missingPhases.isEmpty()) {
            printStudentDataOK(res().getString("OK_Message"));
        } else {
            String missingPhaseStr = joinList(missingPhases, "or");
            String message = getParameter("Message");
            if (!StringUtils.hasValue(message))
                message = res().getString("Default_Error_Message");
            message = StringUtils.findAndReplace(message, "[PHASES]", missingPhaseStr);
            printStudentDataError(message);
        }

        out.println(FOOTER);
    }



}
