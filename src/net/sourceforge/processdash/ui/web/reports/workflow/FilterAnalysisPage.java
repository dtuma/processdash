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
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.db.WorkflowHistDataHelper;
import net.sourceforge.processdash.tool.db.WorkflowHistDataHelper.Enactment;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

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
        req.setAttribute("filt", getFilter(req));
        req.setAttribute("projects", getProjects(histData));
        req.setAttribute("sizeUnits", getSizeUnits(histData));
        req.setAttribute("mappingPromptHtml", getMappingPrompt(histData));
        req.setAttribute("resources", filtRes.asJSTLMap());

        req.getRequestDispatcher("/WEB-INF/jsp/workflowAnalysisFilter.jsp")
                .forward(req, resp);
    }

    private Object getFilter(HttpServletRequest req) {
        Properties p = loadFilter(req);
        expandMulti(p, "projVal");
        return p;
    }

    private void expandMulti(Properties p, String key) {
        String value = p.getProperty(key);
        if (value == null)
            return;

        String[] values = value.split(",");
        for (String oneVal : values)
            p.setProperty(key + oneVal, "checked=\"checked\"");
    }

    private Map<String, String> getProjects(WorkflowHistDataHelper histData) {
        Map<String, String> result = new TreeMap<String, String>();
        for (Enactment e : histData.getEnactments())
            result.put(e.getProjectID(), e.getProjectName());
        return result;
    }

    private Set<String> getSizeUnits(WorkflowHistDataHelper histData) {
        Set<String> sizeUnits = histData.getSizeUnits();
        for (Iterator i = sizeUnits.iterator(); i.hasNext();) {
            if (isTimeUnits((String) i.next()))
                i.remove();
        }
        return sizeUnits;
    }

    private String getMappingPrompt(WorkflowHistDataHelper histData) {
        if (!Settings.isTeamMode())
            return null;

        String uri = HTMLUtils.appendQuery("/team/workflowMap", "list",
            histData.getWorkflowID());
        String link = "<a href='" + uri + "' target='_blank'>";
        String result = filtRes.getHTML("Map_Prompt");
        result = StringUtils.findAndReplace(result, "[[", link);
        result = StringUtils.findAndReplace(result, "]]", "</a>");
        return result;
    }

    @Override
    protected void writeHtmlContent(HttpServletRequest req,
            HttpServletResponse resp, ChartData chartData)
            throws ServletException, IOException {}



    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // see if the user is applying or removing the filter
        boolean isRemoving = req.getParameter("remove") != null;

        // calculate the properties we should save for this filter
        Properties p = new Properties();
        for (String filterID : req.getParameterValues("filterID")) {
            for (Entry<String, String[]> e : req.getParameterMap().entrySet()) {
                if (e.getKey().startsWith(filterID)) {
                    p.setProperty(e.getKey(),
                        StringUtils.join(Arrays.asList(e.getValue()), ","));
                }
            }
            if (isRemoving)
                p.remove(filterID + "Enabled");
        }

        // save these properties into the respository
        saveFilter(req, p);

        // redirect to the previous page the user was viewing
        String lastPage = req.getParameter(LAST_PAGE_PARAM);
        String workflowID = req.getParameter(WorkflowReport.WORKFLOW_PARAM);
        StringBuffer uri = new StringBuffer(WorkflowReport.SELF_URI);
        HTMLUtils.appendQuery(uri, WorkflowReport.PAGE_PARAM, lastPage);
        HTMLUtils.appendQuery(uri, WorkflowReport.WORKFLOW_PARAM, workflowID);
        resp.sendRedirect(uri.toString());
    }

}
