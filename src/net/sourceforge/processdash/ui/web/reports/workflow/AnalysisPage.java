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

import static net.sourceforge.processdash.ui.web.reports.workflow.WorkflowReport.PAGE_PARAM;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.data.util.ResultSet;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.http.PDashServletUtils;
import net.sourceforge.processdash.net.http.TinyCGI;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.tool.db.DatabasePlugin;
import net.sourceforge.processdash.tool.db.QueryRunner;
import net.sourceforge.processdash.tool.db.QueryUtils;
import net.sourceforge.processdash.tool.db.WorkflowHistDataHelper;
import net.sourceforge.processdash.util.DataPair;
import net.sourceforge.processdash.util.DateUtils;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

public abstract class AnalysisPage {

    protected static final String LAST_PAGE_PARAM = "last" + PAGE_PARAM;

    protected static Resources resources = Resources.getDashBundle("Analysis");

    private String selfUri;

    private String titleKey;

    public AnalysisPage(String page, String titleKey) {
        this.selfUri = HTMLUtils.appendQuery(WorkflowReport.SELF_URI,
            PAGE_PARAM, page);
        this.titleKey = titleKey;
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doGet(req, resp);
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        ChartData chartData = getChartData(req);
        if (chartData == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                "The requested workflow was not found.");
            return;
        }

        if (req.getParameter("type") == null) {
            writeHtmlPage(req, resp, chartData);
        } else {
            serveChartResultSet(req, chartData);
        }
    }



    protected ChartData getChartData(HttpServletRequest req) {
        ChartData result = new ChartData();

        String workflowID = req.getParameter("workflow");
        if (!StringUtils.hasValue(workflowID))
            return null;

        DashboardContext ctx = (DashboardContext) PDashServletUtils
                .buildEnvironment(req).get(TinyCGI.DASHBOARD_CONTEXT);
        DatabasePlugin databasePlugin = ctx.getDatabasePlugin();
        QueryUtils.waitForAllProjects(databasePlugin);
        QueryRunner query = databasePlugin.getObject(QueryRunner.class);

        result.histData = new WorkflowHistDataHelper(query, workflowID);
        if (result.histData.getWorkflowName() == null)
            return null;

        configureFilter(result.histData, req);
        configureSizeUnits(result, ctx);

        return result;
    }

    private void configureSizeUnits(ChartData chartData, DashboardContext ctx) {
        String workflowID = chartData.histData.getWorkflowID();
        String workflowName = chartData.histData.getWorkflowName();
        DataRepository data = ctx.getData();

        // check for an explicit setting for this workflow
        if (setSizeUnits(chartData, data, workflowID + "/Size_Units"))
            return;

        // check for a setting stored for another workflow with this name
        if (setSizeUnits(chartData, data, workflowName + "/Size_Units"))
            return;

        // check for the size unit we guessed during the current session
        if (setSizeUnits(chartData, data, workflowID + "//Size_Units_Guess"))
            return;

        // no luck so far; examine the historical data and make a best guess
        double bestSize = 0;
        String bestUnits = null;
        for (Entry<String, DataPair> e : chartData.histData
                .getAddedAndModifiedSizes().entrySet()) {
            double oneSize = Math.max(e.getValue().plan, e.getValue().actual);
            if (oneSize > bestSize) {
                bestSize = oneSize;
                bestUnits = e.getKey();
            }
        }
        if (bestUnits != null) {
            // if we find a good metric, use it and save our decision for the
            // rest of this session. This ensures that the charts won't silently
            // switch size units (for example, when different filters are in
            // effect), as that erratic behavior could confuse/mislead users.
            String dataName = "/Workflow_Prefs/" + workflowID
                    + "//Size_Units_Guess";
            data.putValue(dataName, StringData.create(bestUnits));
            chartData.setPrimarySizeUnits(bestUnits);
            return;
        }

        // no actual size data is present. Use "Hours" as our guess.
        chartData.setPrimarySizeUnits("Hours");
    }

    private boolean setSizeUnits(ChartData chartData, DataRepository data,
            String dataName) {
        SimpleData sd = data.getSimpleValue("/Workflow_Prefs/" + dataName);
        if (sd == null || !sd.test())
            return false;
        chartData.setPrimarySizeUnits(sd.format());
        return true;
    }

    protected void saveSizeUnits(HttpServletRequest req, String units) {
        WorkflowHistDataHelper histData = getChartData(req).histData;
        DataRepository data = (DataRepository) PDashServletUtils
                .buildEnvironment(req).get(TinyCGI.DATA_REPOSITORY);
        StringData sd = StringData.create(units);
        data.putValue("/Workflow_Prefs/" + histData.getWorkflowID()
                + "/Size_Units", sd);
        data.putValue("/Workflow_Prefs/" + histData.getWorkflowName()
                + "/Size_Units", sd);
    }



    protected void configureFilter(WorkflowHistDataHelper histData,
            HttpServletRequest req) {
        doConfigureFilter(histData, req);
    }

    public static void doConfigureFilter(WorkflowHistDataHelper histData,
            HttpServletRequest req) {
        Properties p = loadFilter(req);
        if (p.isEmpty())
            return;

        // configure the list of included/excluded projects
        if ("true".equals(p.getProperty("projEnabled"))) {
            Set<String> projIDs = getList(p, "projVal");
            String logic = p.getProperty("projLogic");
            if ("include".equals(logic))
                histData.setIncludedProjects(projIDs);
            else
                histData.setExcludedProjects(projIDs);
        }

        // configure before/after dates
        if ("true".equals(p.getProperty("dateEnabled"))) {
            histData.setOnlyCompletedBefore(getDate(p, "dateBefore", 0));
            histData.setOnlyCompletedAfter(getDate(p, "dateAfter", 1));
        }

        // configure min/max times
        if ("true".equals(p.getProperty("timeEnabled"))) {
            histData.setMinTime(getNum(p, "timeMin", 60));
            histData.setMaxTime(getNum(p, "timeMax", 60));
        }
    }

    private static Double getNum(Properties p, String key, int mult) {
        try {
            String val = p.getProperty(key);
            if (val == null)
                return null;
            else if (mult == 1)
                return Double.valueOf(val);
            else
                return Double.parseDouble(val) * mult;
        } catch (Exception e) {
            return null;
        }
    }

    private static Set<String> getList(Properties p, String key) {
        String list = p.getProperty(key);
        if (list == null)
            return Collections.EMPTY_SET;
        else
            return new HashSet<String>(Arrays.asList(list.split(",")));
    }

    private static Date getDate(Properties p, String key, int adjust) {
        try {
            String val = p.getProperty(key);
            if (val == null)
                return null;
            Date result = (Date) DATE_FMT.parseObject(val);
            if (adjust != 0)
                result = new Date(result.getTime() + adjust * DateUtils.DAYS);
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat(
            "yyyy-MM-dd");



    protected static Properties loadFilter(HttpServletRequest req) {
        DataRepository data = (DataRepository) PDashServletUtils
                .buildEnvironment(req).get(TinyCGI.DATA_REPOSITORY);
        SimpleData sd = data.getSimpleValue(getFilterDataName(req));

        Properties p = new Properties();
        if (sd != null && sd.test()) {
            try {
                p.load(new StringReader(sd.format()));
            } catch (IOException e) {
                // can't happen
            }
        }
        return p;
    }

    protected void saveFilter(HttpServletRequest req, Properties filter) {
        StringWriter save = new StringWriter();
        try {
            filter.store(save, null);
        } catch (IOException ioe) {
            // can't happen
        }

        DataRepository data = (DataRepository) PDashServletUtils
                .buildEnvironment(req).get(TinyCGI.DATA_REPOSITORY);
        String dataName = getFilterDataName(req);
        data.putValue(dataName, StringData.create(save.toString()));
    }

    private static String getFilterDataName(HttpServletRequest req) {
        String workflowID = req.getParameter(WorkflowReport.WORKFLOW_PARAM);
        return "/Workflow_Prefs/" + workflowID + "//Filter";
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

        out.write("<table><tr>\n<td style='vertical-align:baseline'><h2>");
        out.write(HTMLUtils.escapeEntities(getRes(titleKey)));
        out.write("&nbsp;</td>\n");
        writePageSubtitle(req, out, chartData);
        out.write("</tr></table>\n");
    }

    protected void writePageSubtitle(HttpServletRequest req, PrintWriter out,
            ChartData chartData) {
        out.write("<td class='doNotPrint' style='vertical-align:baseline'><i>");
        out.write(HTMLUtils.escapeEntities(getRes("More_Detail_Instruction")));

        if (!isExporting(req)) {
            out.write("&nbsp;&nbsp;<a href='" + getSideUri(chartData, "Filter")
                    + "'>");
            out.write(resources.getHTML("Workflow.To_Date.Filter.Label"));
            out.write("...</a>");
        }

        if (!isExporting(req) && chartData.isSizeConfigurable()) {
            out.write("&nbsp;&nbsp;<a href='" + getSideUri(chartData, "Config")
                    + "'>");
            out.write(resources.getHTML("Workflow.Config.Link_Text"));
            out.write("</a>");
        }

        out.write("</i></td>\n");
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
        if (isExporting(req))
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


    protected Map invokeChart(Entry<Chart, Method> chart, ChartData chartData)
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

    public static boolean isTimeUnits(String sizeUnits) {
        // "Hours" are not a standard size metric in the TSP process; but they
        // are a common proxy used by teams when they create custom MCFs. This
        // method tests to see if a team has followed that pattern, even if the
        // authors of the MCF did not use English words.
        return sizeUnits != null
                && HOURS_UNITS.contains(sizeUnits.toLowerCase());
    }

    private static final Set<String> HOURS_UNITS = Collections
            .unmodifiableSet(new HashSet(Arrays.asList("hours", // en
                "stunden", // de
                "horas", // es, pt
                "valandos", // lt
                "\u0447\u0430\u0441\u044B", // ru
                "\u6642\u9593", // ja
                "\u5C0F\u65F6\u6570" // zh
            )));


    protected String getSideUri(ChartData chartData, String sidePage) {
        String uri = selfUri;
        uri = StringUtils.findAndReplace(uri, PAGE_PARAM, LAST_PAGE_PARAM);
        uri = HTMLUtils.appendQuery(uri, PAGE_PARAM, sidePage);
        uri = HTMLUtils.appendQuery(uri, WorkflowReport.WORKFLOW_PARAM,
            chartData.histData.getWorkflowID());
        return uri;
    }

    protected boolean isExporting(HttpServletRequest req) {
        return req.getParameter("EXPORT") != null;
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
