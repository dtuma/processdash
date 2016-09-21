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
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.processdash.util.HTMLUtils;

public class ConfigureAnalysisPage extends AnalysisPage {

    public ConfigureAnalysisPage() {
        super("Config", "Workflow.Config.Title");
    }

    @Override
    protected void writePageSubtitle(HttpServletRequest req, PrintWriter out,
            ChartData chartData) {}

    @Override
    protected void writeHtmlContent(HttpServletRequest req,
            HttpServletResponse resp, ChartData chartData)
            throws ServletException, IOException {

        PrintWriter out = resp.getWriter();
        out.write("<form action='" + WorkflowReport.SELF_URI
                + "' method='post'>\n");
        writeHidden(out, WorkflowReport.PAGE_PARAM, "Config");
        writeHidden(out, LAST_PAGE_PARAM, req.getParameter(LAST_PAGE_PARAM));
        writeHidden(out, WorkflowReport.WORKFLOW_PARAM,
            chartData.histData.getWorkflowID());

        out.write("<p style='margin-top:0px'>");
        out.write(resources.getHTML("Workflow.Config.Size_Header"));
        out.write("</p>\n");

        for (String units : chartData.histData.getSizeUnits()) {
            if (!isTimeUnits(units))
                writeUnitsOption(chartData, out, units);
        }
        writeUnitsOption(chartData, out, "Hours");

        out.write("<br/><p>");
        out.write("<input type='submit' name='save' value='"
                + resources.getHTML("Save") + "'/>&nbsp;&nbsp;&nbsp;");
        out.write("<input type='submit' name='cancel' value='"
                + resources.getHTML("Cancel") + "'/>");
        out.write("</p>\n");

        out.write("</form>\n");
    }

    private void writeHidden(PrintWriter out, String name, String value) {
        out.write("<input type='hidden' name='");
        out.write(name);
        out.write("' value='");
        out.write(HTMLUtils.escapeEntities(value));
        out.write("'/>\n");
    }

    public void writeUnitsOption(ChartData chartData, PrintWriter out,
            String units) {
        String unitsHtml = HTMLUtils.escapeEntities(units);
        out.write("<div style='margin-left:1cm'>");
        out.write("<input type='radio' name='sizeUnit' value='");
        out.write(unitsHtml);
        if (units.equals(chartData.primarySizeUnits))
            out.write("' checked='checked");
        out.write("'/> ");
        if (isTimeUnits(units))
            out.write(resources.getString("Workflow.Config.Size_Time_Option"));
        else
            out.write(unitsHtml);
        out.write("</div>\n");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // if the user pressed the "save" button, save their choices
        if (req.getParameter("save") != null) {
            saveSizeUnits(req, req.getParameter("sizeUnit"));
        }

        // construct the URI of the page the user came from
        String page = req.getParameter(LAST_PAGE_PARAM);
        String uri = (page == null ? WorkflowReport.SUMMARY_URI //
                : HTMLUtils.appendQuery(WorkflowReport.SELF_URI,
                    WorkflowReport.PAGE_PARAM, page));
        uri = HTMLUtils.appendQuery(uri, WorkflowReport.WORKFLOW_PARAM,
            req.getParameter(WorkflowReport.WORKFLOW_PARAM));

        // redirect the user's browser back to that page.
        resp.sendRedirect(uri);
    }

}
