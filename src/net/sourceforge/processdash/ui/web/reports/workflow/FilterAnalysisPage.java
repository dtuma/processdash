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

package net.sourceforge.processdash.ui.web.reports.workflow;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.db.WorkflowHistDataHelper;

public class FilterAnalysisPage extends AnalysisPage {

    private static final Resources filtRes = Resources
            .getDashBundle("Analysis.Workflow.Filter");

    public FilterAnalysisPage() {
        super("Filter", "Workflow.Filter.Title");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        ChartData chartData = getChartData(req);
        if (chartData == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                "The requested workflow was not found.");
            return;
        }

        WorkflowHistDataHelper histData = chartData.histData;
        req.setAttribute("hist", histData);

        Set<String> sizeUnits = histData.getSizeUnits();
        for (Iterator i = sizeUnits.iterator(); i.hasNext();) {
            if (isTimeUnits((String) i.next()))
                i.remove();
        }
        req.setAttribute("sizeUnits", sizeUnits);

        req.setAttribute("resources", filtRes.asJSTLMap());
        req.getRequestDispatcher("/WEB-INF/jsp/workflowAnalysisFilter.jsp")
                .forward(req, resp);
    }

    @Override
    protected void writeHtmlContent(HttpServletRequest req,
            HttpServletResponse resp, ChartData chartData)
            throws ServletException, IOException {}

}
