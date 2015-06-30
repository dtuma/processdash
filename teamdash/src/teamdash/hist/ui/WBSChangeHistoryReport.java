// Copyright (C) 2015 Tuma Solutions, LLC
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

package teamdash.hist.ui;

import java.io.IOException;
import java.util.Date;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.api.PDashContext;
import net.sourceforge.processdash.api.PDashData;
import net.sourceforge.processdash.net.http.PDashServletUtils;

import teamdash.hist.ProjectChangeList;
import teamdash.hist.ProjectDiff;
import teamdash.hist.ProjectHistory;
import teamdash.hist.ProjectHistoryException;
import teamdash.hist.ProjectHistoryFactory;

public class WBSChangeHistoryReport extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        try {
            loadHistory(req);
        } catch (ProjectHistoryException u) {
            u.printStackTrace();
            req.setAttribute("errorMessage", u.getHtml());
        }

        RequestDispatcher disp = getServletContext().getRequestDispatcher(
            "/WEB-INF/jsp/wbsChangeHistory.jsp");
        disp.forward(req, resp);
    }

    private void loadHistory(HttpServletRequest req)
            throws ProjectHistoryException {

        PDashContext ctx = PDashServletUtils.getContext(req);
        PDashData data = ctx.getData();
        ProjectHistory hist = ProjectHistoryFactory.getProjectHistory(data);
        if (hist == null)
            throw new ProjectHistoryException("Not_Team_Project_HTML_FMT",
                    ctx.getProjectPath());

        String dateParam = req.getParameter("before");
        Date beforeDate = dateParam == null ? null : new Date(
                Long.parseLong(dateParam));

        ProjectChangeList changes;
        try {
            changes = ProjectDiff.getChanges(hist, beforeDate, 10, true, true);
        } catch (IOException ioe) {
            throw hist.wrapException(ioe);
        }

        req.setAttribute("changes", changes);
        req.setAttribute("followupTimestamp", changes.getFollowupTimestamp());

        // compute the URI that will be used to open WBS hyperlinks
        String processID = data.getString("Team_Process_PID");
        String modeIndicator = Settings.isPersonalMode() ? "indiv" : "forteam";
        String openWbsUri = "../" + processID + "/setup/openWBS.shtm?trigger&"
                + modeIndicator + "&showItem=";
        req.setAttribute("openWbsUri", openWbsUri);
    }

}
