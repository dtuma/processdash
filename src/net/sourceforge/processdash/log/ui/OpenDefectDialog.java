// Copyright (C) 2015-2016 Tuma Solutions, LLC
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
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.Filter;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.log.defects.Defect;
import net.sourceforge.processdash.log.defects.DefectLog;
import net.sourceforge.processdash.log.defects.DefectLogID;
import net.sourceforge.processdash.net.http.TinyCGIException;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.StringUtils;

public class OpenDefectDialog extends TinyCGIBase {

    @Override
    protected void writeContents() throws IOException {
        DashController.checkIP(env.get("REMOTE_ADDR"));

        String path = getPath();
        DefectLogID defectLog = getDefectLog(path);
        if (defectLog == null)
            throw new TinyCGIException(404, "No defect log found for " + path);

        String id = getParameter("id");
        if (StringUtils.hasValue(id))
            openExistingDefectDialog(defectLog, id);
        else
            openNewDefectDialog(defectLog, path);

        DashController.printNullDocument(out);
    }

    private String getPath() {
        String path = getPrefix();
        if (!StringUtils.hasValue(path))
            path = getDash().getCurrentPhase().path();
        return path;
    }

    private DefectLogID getDefectLog(String path) {
        DashHierarchy hier = getPSPProperties();
        PropertyKey key = hier.findExistingKey(path);
        if (key == null)
            return null;

        ProcessDashboard dash = getDash();
        DefectLogID defectLog = hier.defectLog(key, dash.getDirectory());
        if (defectLog == null)
            return null;

        if (Filter.matchesFilter(dash.getBrokenDataPaths(),
            defectLog.path.path()))
            return null;

        return defectLog;
    }

    private void openExistingDefectDialog(DefectLogID defectLog, String id)
            throws TinyCGIException {
        DefectLog log = new DefectLog(defectLog.filename,
                defectLog.path.path(), getDataRepository());
        Defect defect = log.getDefect(id);
        if (defect == null)
            throw new TinyCGIException(404, "No defect found with ID '" + id
                    + "' in " + defectLog.path.path());

        DefectDialog dlg = DefectDialog.getDialogForDefect(getDash(),
            defectLog.filename, defectLog.path, defect, true);
        dlg.setTitle(defectLog.path.path());
        dlg.toFront();
    }

    private void openNewDefectDialog(DefectLogID defectLog, String path) {
        ProcessDashboard dash = getDash();
        PropertyKey task = dash.getHierarchy().findClosestKey(path);
        DefectDialog dlg = new DefectDialog(dash, defectLog.filename,
                defectLog.path, task);
        if (!defectLog.path.equals(dash.getCurrentPhase()))
            dlg.setTitle(defectLog.path.path());
    }

    private ProcessDashboard getDash() {
        return (ProcessDashboard) getDashboardContext();
    }

}
