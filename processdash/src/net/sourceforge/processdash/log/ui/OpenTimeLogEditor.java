// Copyright (C) 2007 Tuma Solutions, LLC
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

package net.sourceforge.processdash.log.ui;

import java.io.IOException;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.log.time.RolledUpTimeLog;
import net.sourceforge.processdash.log.time.TimeLog;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.ui.web.reports.analysis.AnalysisPage;

public class OpenTimeLogEditor extends TinyCGIBase {

    protected void doPost() throws IOException {
        parseFormData();
        super.doPost();
    }

    protected void writeContents() throws IOException {
        DashController.checkIP(env.get("REMOTE_ADDR"));
        String type = getParameter("type");
        if ("rollup".equals(type))
            showRollupTimeLog();
        else
            DashController.showTimeLogEditor(getPrefix());
        DashController.printNullDocument(out);
    }

    private void showRollupTimeLog() {
        TimeLog tl = new RolledUpTimeLog.FromResultSet(getDashboardContext(),
                getPrefix(), parameters);
        TimeLogEditor e = new TimeLogEditor(tl, getPSPProperties(), null, null);

        if (parameters.containsKey(
                RolledUpTimeLog.FromResultSet.MERGE_PREFIXES_PARAM))
            try {
                e.narrowToPath(getPrefix());
            } catch (IllegalArgumentException iae) {
                // this will be thrown if we attempt to narrow to a prefix that
                // does not exist.  It's best to ignore the error and proceed.
            }

        String displayPrefix = AnalysisPage.localizePrefix(getPrefix());
        String title = TimeLogEditor.resources.format(
                "Time_Log_Viewer_Window_Title_FMT", displayPrefix);
        e.setTitle(title);
    }

}
