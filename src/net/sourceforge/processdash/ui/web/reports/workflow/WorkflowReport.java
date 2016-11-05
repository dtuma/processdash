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
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.api.PDashContext;
import net.sourceforge.processdash.net.http.PDashServletUtils;
import net.sourceforge.processdash.net.http.TinyCGI;
import net.sourceforge.processdash.tool.db.DatabasePlugin;
import net.sourceforge.processdash.tool.db.DatabasePluginUtils;
import net.sourceforge.processdash.tool.db.QueryRunner;
import net.sourceforge.processdash.tool.db.QueryUtils;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

public class WorkflowReport extends HttpServlet {

    protected static final String SELF_URI = "workflowToDate";

    protected static final String SUMMARY_URI = "workflowSummary";

    protected static final String PAGE_PARAM = "page";

    protected static final String WORKFLOW_PARAM = "workflow";


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doGet(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String page = req.getParameter(PAGE_PARAM);
        String workflowID = req.getParameter(WORKFLOW_PARAM);

        if (StringUtils.hasValue(page))
            showAnalysisPage(req, resp, page);

        else if (StringUtils.hasValue(workflowID) == false)
            writeWorkflowSelectionScreen(req, resp);

        else if (req.getParameter("toc") != null)
            writeWorkflowTocPage(req, resp, workflowID);

        else
            writeFrameForWorkflow(req, resp, workflowID);
    }



    private void writeWorkflowSelectionScreen(HttpServletRequest req,
            HttpServletResponse resp) throws IOException {
        String title = esc(res("Workflow.Analysis.Title"));

        PrintWriter out = resp.getWriter();
        out.print("<html><head>\n<title>");
        out.print(title);
        out.print("</title>\n");
        out.print("<link rel='stylesheet' type='text/css' href='/style.css'>");
        out.print("</head>\n");
        out.print("<body><h1>");
        out.print(title);
        out.print("</h1>\n");

        if (req.getParameter("wait") != null) {
            out.print("<p>");
            out.print(esc(res("Workflow.Analysis.Wait_Message")));
            out.print("</p>\n");
            out.print(HTMLUtils.redirectScriptHtml(SELF_URI, 0));

        } else {
            writeWorkflowSelections(req, out);
        }

        out.print("</body></html>\n");
    }

    private void writeWorkflowSelections(HttpServletRequest req, PrintWriter out) {
        // get the list of workflows in this team project.
        Map<String, String> workflows = getWorkflowsForCurrentProject(req);
        if (workflows.isEmpty()) {
            out.write("<p>");
            String resKey = "Workflow.Analysis.No_Workflows_Message"
                    + (Settings.isTeamMode() ? "" : "_Personal");
            out.write(esc(res(resKey)));
            out.write("</p>\n");
            return;
        }

        // display a prompt inviting the user to choose a workflow
        out.write("<p>");
        out.write(esc(res("Workflow.Analysis.Choose_Workflow_Prompt")));
        out.write("</p><ul>");

        // display hyperlinks for each of the workflows
        for (Entry<String, String> e : workflows.entrySet()) {
            out.write("<li><a href=\"/reports/");
            out.write(HTMLUtils.appendQuery(SELF_URI, WORKFLOW_PARAM,
                e.getValue()));
            out.write("\">");
            out.write(esc(e.getKey()));
            out.write("</a></li>\n");
        }

        out.write("</ul>\n");
    }

    private Map<String, String> getWorkflowsForCurrentProject(
            HttpServletRequest req) {
        DashboardContext dash = (DashboardContext) PDashServletUtils
                .buildEnvironment(req).get(TinyCGI.DASHBOARD_CONTEXT);
        DatabasePlugin databasePlugin = dash.getDatabasePlugin();
        QueryUtils.waitForAllProjects(databasePlugin);
        QueryRunner queryRunner = databasePlugin.getObject(QueryRunner.class);

        PDashContext ctx = PDashServletUtils.getContext(req);
        String projectID = ctx.getData().getString("Project_ID");
        String workflowProcessIDPattern = DatabasePluginUtils
                .getWorkflowPhaseIdentifier(projectID, "%");
        Map<String, String> result = new TreeMap<String, String>();
        String query = Settings.isTeamMode() ? WORKFLOW_LIST_QUERY
                : WORKFLOW_LIST_QUERY_PERSONAL;
        QueryUtils.mapColumns(result, queryRunner.queryHql(query, //
            workflowProcessIDPattern));
        return result;
    }

    private static final String WORKFLOW_LIST_QUERY = //
    "select p.name, p.identifier from Process p where p.identifier like ?";

    private static final String WORKFLOW_LIST_QUERY_PERSONAL = //
    "select distinct phase.process.name, phase.process.identifier "
            + "from TaskStatusFact as task "
            + "join task.planItem.phase.mapsToPhase phase "
            + "where task.versionInfo.current = 1 "
            + "and task.actualCompletionDate is not null "
            + "and phase.typeName in ('Overhead', 'Construction') "
            + "and phase.process.identifier like ?";



    private void writeFrameForWorkflow(HttpServletRequest req,
            HttpServletResponse resp, String workflowID) throws IOException {

        PDashContext ctx = PDashServletUtils.getContext(req);
        String workflowName = QueryUtils.singleValue(QueryUtils.pluckColumn( //
            ctx.getQuery().query(WORKFLOW_LIST_QUERY, workflowID), 0));
        if (workflowName == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                "The requested workflow was not found.");
            return;
        }

        PrintWriter out = resp.getWriter();
        out.write("<html>\n<head><title>");
        out.write(esc(res("Workflow.Analysis.Title")));
        out.write(" - ");
        out.write(esc(workflowName));
        out.write("</title></head>\n");

        String tocUri = HTMLUtils.appendQuery(SELF_URI, WORKFLOW_PARAM,
            workflowID);
        String ppsUri = HTMLUtils.appendQuery(SUMMARY_URI, WORKFLOW_PARAM,
            workflowID);

        out.write("<frameset cols='140,*'>\n");
        out.write("    <frame name='toc' src='" + tocUri + "&amp;toc'>\n");
        out.write("    <frame name='contents' src='" + ppsUri + "'>\n");
        out.write("</frameset></html>\n");
    }

    private void writeWorkflowTocPage(HttpServletRequest req,
            HttpServletResponse resp, String workflowID) throws IOException {
        String title = esc(res("Workflow.Analysis.Title"));
        PrintWriter out = resp.getWriter();
        out.write("<html>\n<head><title>");
        out.write(title);
        out.write("</title></head>\n<body>\n");
        out.write("<h2>");
        out.write(title);
        out.write("</h2>\n");

        String query = HTMLUtils.appendQuery("x", WORKFLOW_PARAM, workflowID)
                .substring(1);
        writeTocLink(out, SUMMARY_URI, query, "Summary.Title");
        writeTocLink(out, Page.Defects, query, "Defects.Title");
        writeTocLink(out, Page.Plan, query, "Plan.Title");
        writeTocLink(out, Page.Process, query, "Process.Title");
        writeTocLink(out, Page.Quality, query, "Quality.Title");

        if (req.getParameter("EXPORT") == null) {
            out.write("<hr/>\n");
            out.write("<p><a href='../dash/archive.class?uri=/reports/"
                    + SELF_URI + query + "' target='_top'>");
            out.write(esc(res("Archive.Title")));
            out.write("</a></p>\n");
        }

        out.write("</body></html>\n");
    }

    private void writeTocLink(PrintWriter out, Object page, String query,
            String resKey) {
        out.write("<p><a target='contents' href='");
        if (page instanceof Page)
            out.write(SELF_URI + query + "&amp;" + PAGE_PARAM + "=" + page);
        else
            out.write(page + query);
        out.write("'>");
        out.write(esc(res(resKey)));
        out.write("</a></p>\n");
    }



    private void showAnalysisPage(HttpServletRequest req,
            HttpServletResponse resp, String page) throws ServletException,
            IOException {
        if (analysisPages == null) {
            analysisPages = new AnalysisPage[Page.values().length];
            analysisPages[Page.Defects.ordinal()] = new DefectAnalysisPage();
            analysisPages[Page.Plan.ordinal()] = new PlanAnalysisPage();
            analysisPages[Page.Process.ordinal()] = new ProcessAnalysisPage();
            analysisPages[Page.Quality.ordinal()] = new QualityAnalysisPage();
            analysisPages[Page.Filter.ordinal()] = new FilterAnalysisPage();
            analysisPages[Page.Config.ordinal()] = new ConfigureAnalysisPage();
        }

        AnalysisPage p = analysisPages[Page.valueOf(page).ordinal()];
        if ("POST".equalsIgnoreCase(req.getMethod()))
            p.doPost(req, resp);
        else
            p.doGet(req, resp);
    }

    private enum Page {
        Defects, Plan, Process, Quality, Filter, Config
    }

    private AnalysisPage[] analysisPages;



    private String esc(String s) {
        return HTMLUtils.escapeEntities(s);
    }

    private String res(String key) {
        return AnalysisPage.getRes(key);
    }

}
