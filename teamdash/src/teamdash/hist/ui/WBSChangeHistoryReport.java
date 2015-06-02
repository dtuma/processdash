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
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import teamdash.hist.ProjectChange;
import teamdash.hist.ProjectDiff;
import teamdash.hist.ProjectHistory;
import teamdash.hist.ProjectHistoryTest;

public class WBSChangeHistoryReport extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // TODO: get history from active project, not mock data.
        // PDashContext dash = PDashServletUtils.getContext(req);
        // String path = dash.getProjectPath();

        ProjectHistory hist = ProjectHistoryTest.getMockHistory(1);

        String dateParam = req.getParameter("before");
        Date beforeDate = dateParam == null ? null : new Date(
                Long.parseLong(dateParam));

        List<ProjectChange> changes = ProjectDiff.getChanges(hist, beforeDate,
            10);
        req.setAttribute("changes", changes);

        RequestDispatcher disp = getServletContext().getRequestDispatcher(
            "/WEB-INF/jsp/wbsChangeHistory.jsp");
        disp.forward(req, resp);
    }

}
