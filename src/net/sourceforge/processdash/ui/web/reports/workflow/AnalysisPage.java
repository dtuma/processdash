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
import java.io.StringReader;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.util.ResultSet;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.http.PDashServletUtils;
import net.sourceforge.processdash.net.http.TinyCGI;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.tool.db.DatabasePlugin;
import net.sourceforge.processdash.tool.db.QueryRunner;
import net.sourceforge.processdash.tool.db.QueryUtils;
import net.sourceforge.processdash.tool.db.WorkflowHistDataHelper;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

public abstract class AnalysisPage {

    protected static Resources resources = Resources.getDashBundle("Analysis");

    private String selfUri;

    private String titleKey;

    public AnalysisPage(String page, String titleKey) {
        this.selfUri = HTMLUtils.appendQuery(WorkflowReport.SELF_URI, 
            WorkflowReport.PAGE_PARAM, page);
        this.titleKey = titleKey;
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doGet(req, resp);
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        ChartData chartData = getChartData(req);

        if (req.getParameter("type") == null) {
            writeHtmlPage(req, resp, chartData);
        } else {
            serveChartResultSet(req, chartData);
        }
    }



    private ChartData getChartData(HttpServletRequest req) {
        ChartData result = new ChartData();

        String workflowID = req.getParameter("workflow");
        if (!StringUtils.hasValue(workflowID))
            throw new IllegalArgumentException("Missing workflow parameter");

        DashboardContext ctx = (DashboardContext) PDashServletUtils
                .buildEnvironment(req).get(TinyCGI.DASHBOARD_CONTEXT);
        DatabasePlugin databasePlugin = ctx.getDatabasePlugin();
        QueryUtils.waitForAllProjects(databasePlugin);
        QueryRunner query = databasePlugin.getObject(QueryRunner.class);

        result.histData = new WorkflowHistDataHelper(query, workflowID);

        result.primarySizeUnits = "LOC";
        result.sizeDensityMultiplier = 1000;

        return result;
    }



    private void writeHtmlPage(HttpServletRequest req,
            HttpServletResponse resp, ChartData chartData) throws IOException,
            ServletException {
        resp.setContentType("text/html; charset=UTF-8");

        writeHtmlHeader(req, resp, chartData);
        writeHtmlContent(req, resp, chartData);
        writeHtmlFooter(req, resp);
    }


    private void writeHtmlHeader(HttpServletRequest req,
            HttpServletResponse resp, ChartData chartData) throws IOException {

        PrintWriter out = resp.getWriter();

        String title = getRes("Workflow.Analysis.Title") + " - "
                + chartData.histData.getWorkflowName();
        out.write("<html><head>\n<title>");
        out.write(HTMLUtils.escapeEntities(title));
        out.write("</title>\n");
        out.write("<link rel='stylesheet' type='text/css' href='/style.css'>\n");
        out.write("<script type='text/javascript' src='/lib/overlib.js'></script>\n");
        out.write("</head>\n<body>\n<h1>");
        out.write(HTMLUtils.escapeEntities(title));
        out.write("</h1>\n");

        out.write("<table><tr><td style='vertical-align:baseline'><h2>");
        out.write(HTMLUtils.escapeEntities(getRes(titleKey)));
        out.write("&nbsp;</td><td style='vertical-align:baseline'><i>");
        out.write(HTMLUtils.escapeEntities(getRes("More_Detail_Instruction")));
        out.write("</i></td></tr></table>\n");
    }


    protected abstract void writeHtmlContent(HttpServletRequest req,
            HttpServletResponse resp, ChartData chartData)
            throws ServletException, IOException;


    private void writeHtmlFooter(HttpServletRequest req,
            HttpServletResponse resp) throws IOException {
        resp.getWriter().write("</body>\n</html>\n");
        resp.getWriter().flush();
    }



    protected void writeChart(HttpServletRequest req, HttpServletResponse resp,
            ChartData chartData, String chartId, String... chartArgs)
            throws ServletException, IOException {

        // retrieve the data to display in the chart. If the data for this chart
        // cannot be calculated for the given dataset, abort.
        Entry<Chart, Method> chart = getChartById(chartId);
        chartData.chartArgs = chartArgs;
        Map chartParams;
        try {
            chartParams = invokeChart(chart, chartData);
            if (chartParams == null)
                return;
        } catch (ServletException se) {
            resp.getWriter().write("<!-- Could not calculate data for chart '" //
                    + chartId + "', encountered error:\n");
            se.printStackTrace(resp.getWriter());
            resp.getWriter().write("\n -->\n");
            return;
        }

        // build query parameters that can be used to request this chart later
        Chart metadata = chart.getKey();
        StringBuffer chartDataArgs = new StringBuffer("x");
        HTMLUtils.appendQuery(chartDataArgs, "dqf", selfUri);
        HTMLUtils.appendQuery(chartDataArgs, "workflow",
            chartData.histData.getWorkflowID());
        HTMLUtils.appendQuery(chartDataArgs, "type", chartId);
        for (int i = 0; i < chartArgs.length; i++) {
            HTMLUtils.appendQuery(chartDataArgs, metadata.params()[i],
                chartArgs[i]);
        }

        // use the query parameters to build a URL for the chart details view
        String fullURL;
        if (req.getParameter("EXPORT") != null)
            fullURL = "table.class";
        else {
            fullURL = "full.htm";
            HTMLUtils.appendQuery(chartDataArgs, "chart", metadata.type());
        }
        String href = fullURL + "?" + chartDataArgs.substring(2);
        chartParams.put("href", href);

        chartParams.putAll(parseFormatParams(chartData, metadata.smallFmt()));

        // make an internal request to the CGI script for the given chart,
        // and write the resulting HTML fragment to the response
        String chartHtmlUri = "/reports/" + metadata.type() + ".class"
                + "?qf=small.rpt&html";
        WebServer webServer = (WebServer) PDashServletUtils.buildEnvironment(
            req).get(TinyCGI.TINY_WEB_SERVER);
        String chartHtml = webServer.getRequestAsString(chartHtmlUri,
            Collections.singletonMap("REQUEST_PARAMS", chartParams));
        resp.getWriter().write(chartHtml);
        resp.getWriter().write(" \n");
    }



    private void serveChartResultSet(HttpServletRequest req, ChartData chartData)
            throws ServletException {
        // decide which type of chart we need to display
        String chartID = req.getParameter("type");
        Entry<Chart, Method> chart = getChartById(chartID);

        // if this chart needs parameters, fetch them from the request
        String[] paramNames = chart.getKey().params();
        String[] args = new String[paramNames.length];
        for (int i = 0; i < args.length; i++)
            args[i] = req.getParameter(paramNames[i]);
        chartData.chartArgs = args;

        // run the chart and store the result in the request
        Map chartResults = invokeChart(chart, chartData);
        req.setAttribute("REQUEST_PARAMS", chartResults);
    }


    private Map invokeChart(Entry<Chart, Method> chart, ChartData chartData)
            throws ServletException {
        ResultSet resultSet;
        try {
            resultSet = (ResultSet) chart.getValue().invoke(this, chartData);
            if (resultSet == null)
                return null;
        } catch (Exception e) {
            throw new ServletException(e);
        }

        Map result = new HashMap();
        result.put("resultSet", resultSet);

        Chart metadata = chart.getKey();
        result.put("title", chartData.getRes(metadata.titleKey()));

        result.putAll(parseFormatParams(chartData, metadata.format()));

        return result;
    }

    private Map parseFormatParams(ChartData chartData, String fmt) {
        if (fmt == null || fmt.length() == 0)
            return Collections.EMPTY_MAP;

        String formatValue = resources.interpolate(fmt);
        if (formatValue.indexOf('{') != -1)
            formatValue = MessageFormat.format(formatValue,
                (Object[]) chartData.chartArgs);
        Properties p = new Properties();
        try {
            p.load(new StringReader(formatValue));
        } catch (IOException e) {
        }
        return p;
    }


    private final Map<Chart, Method> charts = reflectivelyLoadCharts();

    private Map<Chart, Method> reflectivelyLoadCharts() {
        Map<Chart, Method> result = new HashMap<Chart, Method>();
        for (Method m : getClass().getMethods()) {
            Chart annotation = m.getAnnotation(Chart.class);
            if (annotation != null)
                result.put(annotation, m);
        }
        return Collections.unmodifiableMap(result);
    }

    private Map.Entry<Chart, Method> getChartById(String id) {
        for (Entry<Chart, Method> e : charts.entrySet()) {
            if (e.getKey().id().equals(id))
                return e;
        }
        throw new IllegalArgumentException("Unrecognized ID: " + id);
    }

    protected boolean isLegitSize(String units) {
        return units != null && !units.equalsIgnoreCase("hours")
                && !units.equalsIgnoreCase(getRes("Hours"));
    }

    protected static String getRes(String resKey) {
        return resources.getString(resKey);
    }

    protected static DoubleData num(double number) {
        if (Double.isNaN(number))
            number = Double.POSITIVE_INFINITY;
        return new DoubleData(number);
    }

}
