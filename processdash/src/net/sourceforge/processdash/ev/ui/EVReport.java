// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003-2006 Software Process Dashboard Initiative
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ev.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.table.TableModel;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.ev.EVDependencyCalculator;
import net.sourceforge.processdash.ev.EVMetrics;
import net.sourceforge.processdash.ev.EVSchedule;
import net.sourceforge.processdash.ev.EVScheduleRollup;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.EVTaskListRollup;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.cache.CachedURLObject;
import net.sourceforge.processdash.net.http.TinyCGIException;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.ui.web.CGIChartBase;
import net.sourceforge.processdash.ui.web.reports.ExcelReport;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.HTMLTableWriter;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.Legend;
import org.jfree.data.AbstractDataset;
import org.jfree.data.XYDataset;



/** CGI script for reporting earned value data in HTML.
 */
public class EVReport extends CGIChartBase {

    public static final String CHART_PARAM = "chart";
    public static final String TABLE_PARAM = "table";
    public static final String XML_PARAM = "xml";
    public static final String XLS_PARAM = "xls";
    public static final String TIME_CHART = "time";
    public static final String VALUE_CHART = "value";
    public static final String VALUE_CHART2 = "value2";
    public static final String COMBINED_CHART = "combined";
    public static final String FAKE_MODEL_NAME = "/  ";


    private static final int MED = EVMetrics.MEDIUM;

    private static Resources resources = Resources.getDashBundle("EV");

    boolean drawingChart;

    /** Write the CGI header.
     */
    protected void writeHeader() {
        drawingChart = (parameters.get(CHART_PARAM) != null);
        if (parameters.get(XML_PARAM) != null
                        || parameters.get(XLS_PARAM) != null)
                return;

        if (drawingChart)
            super.writeHeader();
        else
            writeHtmlHeader();
    }


    /* In its most common use, this script will generate HTML tables
     * of an earned value model, accompanied by one or two charts.
     * These charts (of course) will require additional HTTP requests,
     * yet we do not want to recalculate the evmodel three times.
     * Thus, we calculate it once and save it in the following static
     * fields.  */

    /** The name of the task list we most recently computed */
    static String lastTaskListName = null;

    /** The earned value model for that task list */
    static EVTaskList lastEVModel = null;

    /** The instant in time when that task list was calculated. */
    static long lastRecalcTime = 0;

    /** We'll consider the above cached EV model to be stale (needing
     *  recalculation) if it was calculated more than this many
     *  milliseconds ago.
     */
    public static final long MAX_DELAY = 10000L;

    /** The earned value model we are currently drawing */
    EVTaskList evModel = null;
    /** The name of the task list that creates that earned value model */
    String taskListName = null;

    /** Generate CGI output. */
    protected void writeContents() throws IOException {
        // load the user requested earned value model.
        getEVModel();

        String chartType = getParameter(CHART_PARAM);
        if (chartType == null) {
            String tableType = getParameter(TABLE_PARAM);
            if (tableType == null) {
                if (parameters.get(XML_PARAM) != null)
                    writeXML();
                else if (parameters.get(XLS_PARAM) != null)
                        writeXls();
                else
                    writeHTML();
            } else if (TIME_CHART.equals(tableType))
                writeTimeTable();
            else if (VALUE_CHART2.equals(tableType))
                writeValueTable2();
            else if (VALUE_CHART.equals(tableType))
                writeValueTable();
            else if (COMBINED_CHART.equals(tableType))
                writeCombinedTable();
            else
                throw new TinyCGIException
                    (400, "unrecognized table type parameter");

        } else if (TIME_CHART.equals(chartType))
            writeTimeChart();
        else if (VALUE_CHART.equals(chartType))
            writeValueChart();
        else if (COMBINED_CHART.equals(chartType))
            writeCombinedChart();
        else
            throw new TinyCGIException
                (400, "unrecognized chart type parameter");
    }


    private void getEVModel() throws TinyCGIException {
        taskListName = getPrefix();
        if (taskListName == null || taskListName.length() < 2)
            throw new TinyCGIException(400, "schedule name missing");
        else if (FAKE_MODEL_NAME.equals(taskListName)) {
                evModel = null;
                return;
        }
        taskListName = taskListName.substring(1);

        // strip the "publishing prefix" if it is present.
        if (taskListName.startsWith("ev /"))
            taskListName = taskListName.substring(4);
        else if (taskListName.startsWith("evr /"))
            taskListName = taskListName.substring(5);

        long now = System.currentTimeMillis();

        synchronized (getClass()) {
            if (drawingChart &&
                (now - lastRecalcTime < MAX_DELAY) &&
                taskListName.equals(lastTaskListName)) {
                evModel = lastEVModel;
                return;
            }
        }

        evModel = EVTaskList.openExisting
            (taskListName,
             getDataRepository(),
             getPSPProperties(),
             getObjectCache(),
             false); // change notification not required
        if (evModel == null)
            throw new TinyCGIException(404, "Not Found",
                                       "No such task/schedule");

        EVDependencyCalculator depCalc = new EVDependencyCalculator(
                evModel, getDataRepository(), getPSPProperties(),
                getObjectCache());
        evModel.setDependencyCalculator(depCalc);

        evModel.recalc();

        synchronized (getClass()) {
            lastTaskListName = taskListName;
            lastRecalcTime = now;
            lastEVModel = evModel;
        }
    }


        /** Generate a page of XML data for the Task and Schedule templates.
     */
    public void writeXML() throws IOException {
        if (evModel.isEmpty()) {
            out.print("Status: 404 Not Found\r\n\r\n");
            out.flush();
        } else {
            outStream.write("Content-type: application/xml\r\n".getBytes());
            String owner = getOwner();
            if (owner != null)
                outStream.write((CachedURLObject.OWNER_HEADER_FIELD +
                                 ": " + owner + "\r\n").getBytes());
            outStream.write("\r\n".getBytes());

            outStream.write(XML_HEADER.getBytes("UTF-8"));
            outStream.write(evModel.getAsXML(true).getBytes("UTF-8"));
            outStream.flush();
        }
    }
    private static final String XML_HEADER =
        "<?xml version='1.0' encoding='UTF-8'?>";



    /** Generate a excel spreadsheet to display EV charts.
     */
    public void writeXls() throws IOException {
        if (evModel == null || evModel.isEmpty()) {
            out.print("Status: 404 Not Found\r\n\r\n");
            out.flush();

        } else if ("Excel97".equalsIgnoreCase(Settings
                                .getVal("excel.exportChartsMethod"))) {
            out.print("Content-type: application/vnd.ms-excel\r\n\r\n");
            out.flush();
            FileUtils.copyFile(EVReport.class
                                        .getResourceAsStream("evCharts97.xls"), outStream);

        } else {
            out = new PrintWriter(new OutputStreamWriter(
                        outStream, "us-ascii"));
            out.print("Content-type: application/vnd.ms-excel\r\n\r\n");
                BufferedReader in = new BufferedReader(new InputStreamReader(
                                EVReport.class.getResourceAsStream("evCharts2002.mht"),
                                "us-ascii"));

                scanAndCopyLines(in, "Single File Web Page", true, false);
                out.print("This document, generated by the Process Dashboard,\r\n"
                                + "is designed to work with Excel 2002 and higher.  If\r\n"
                                + "you are using an earlier version of Excel, try\r\n"
                                + "adding the following line to your pspdash.ini file:\r\n"
                                + "excel.exportChartsMethod=Excel97\r\n");

                boolean needsOptimizedLine =
                        (evModel.getSchedule() instanceof EVScheduleRollup);
                if (needsOptimizedLine == false) {
                        // find the data series immediately preceeding the series
                        // describing the optimized line.  Copy it all to output.
                        scanAndCopyLines(in, "'EV Data'!$D$2:$D$10", true, true);
                        scanAndCopyLines(in, "</x:Series>", true, true);
                        // Now skip over and discard the series describing the
                        // optimized line.
                        scanAndCopyLines(in, "</x:Series>", false, false);
                }

                String line;
                while ((line = scanAndCopyLines(in, "http://localhost:2468/++/",
                                true, false)) != null) {
                        writeUrlLine(line);
                }
                out.flush();
        }
    }
        private String scanAndCopyLines(BufferedReader in, String lookFor,
                boolean printLines, boolean printLast) throws IOException {
        String line;
        while ((line = in.readLine()) != null) {
                boolean found = (line.indexOf(lookFor) != -1);
                        if ((!found && printLines) || (found && printLast)) {
                        out.print(line);
                        out.print("\r\n");
                        }
                if (found)
                        return line;
        }
        return null;
    }
    private void writeUrlLine(String line) {
        String host = getTinyWebServer().getHostName(true);
        String port = (String) env.get("SERVER_PORT");
        String path = (String) env.get("PATH_INFO");
        String newBase = host + ":" + port + path;

        // need to escape the result for quoted printable display. Since it's
        // a URL, the only unsafe character it might contain is the '=' sign.
        newBase = StringUtils.findAndReplace(newBase, "=", "=3D");

        // replace the generic host/path information in the URL with the
        // specific information we've built.
        line = StringUtils.findAndReplace(line, "localhost:2468/++", newBase);

        while (line.length() > 76) {
                int pos = line.indexOf('=', 70);
                if (pos == -1 || pos > 73)
                        pos = 73;
                out.print(line.substring(0, pos));
                out.print("=\r\n");
                line = line.substring(pos);
        }
        out.print(line);
        out.print("\r\n");
        }



    /** Generate a page of HTML displaying the Task and Schedule templates,
     *  and including img tags referencing charts.
     */
    public void writeHTML() throws IOException {
        String taskListHTML = WebServer.encodeHtmlEntities(taskListName);
        String taskListURL = HTMLUtils.urlEncode(taskListName);

        out.print(StringUtils.findAndReplace
                  (HEADER_HTML, TITLE_VAR,
                   resources.format("Report.Title_FMT", taskListHTML)));
        if (!exportingToExcel())
            out.print(SEPARATE_CHARTS_HTML);

        EVSchedule s = evModel.getSchedule();
        EVMetrics  m = s.getMetrics();

        Map errors = m.getErrors();
        if (errors != null && errors.size() > 0) {
            out.print("<table border><tr><td bgcolor='#ff5050'><h2>");
            out.print(getResource("Report.Errors_Heading"));
            out.print("</h2><b>");
            out.print(getResource("Error_Dialog.Head"));
            out.print("<ul>");
            Iterator i = errors.keySet().iterator();
            while (i.hasNext())
                out.print("\n<li>" +
                          WebServer.encodeHtmlEntities((String) i.next()));
            out.print("\n</ul>");
            out.print(getResource("Error_Dialog.Foot"));
            out.print("</b></td></tr></table>\n");
        }


        out.print("<table name='STATS'>");
        for (int i = 0;   i < m.getRowCount();   i++)
            writeMetric(m, i);
        out.print("</table>");

        out.print("<h2>"+getResource("TaskList.Title")+"</h2>\n");
        writeTaskTable(evModel);

        out.print("<h2>"+getResource("Schedule.Title")+"</h2>\n");
        writeScheduleTable(s);

        out.print("<p class='doNotPrint'>");
        out.print(EXPORT_HTML1A);
        out.print(getResource("Report.Export_Text"));
        out.print(EXPORT_HTML1B);

        if (!parameters.containsKey("EXPORT")) {
            out.print(EXPORT_HTML2A);
            out.print(getResource("Report.Export_Charts"));
            out.print(EXPORT_HTML2B);
        }

        if (getDataRepository().getValue("/Enable_EV_Week_form") != null) {
            out.print(OPT_FOOTER_HTML1);
            out.print(getResource("Report.Show_Weekly_View"));
            out.print(OPT_FOOTER_HTML2);
        }
        out.print("</p>");
        out.print(FOOTER_HTML2);
    }
    protected void writeMetric(EVMetrics m, int i) {
        String name = (String) m.getValueAt(i, EVMetrics.NAME);
        if (name == null) return;
        String number = (String) m.getValueAt(i, EVMetrics.SHORT);
        String interpretation = (String) m.getValueAt(i, EVMetrics.MEDIUM);
        String explanation = (String) m.getValueAt(i, EVMetrics.FULL);


        boolean writeInterpretation = !number.equals(interpretation);
        boolean writeExplanation = !exportingToExcel();

        out.write("<tr><td><b>");
        out.write(name);
        out.write(":&nbsp;</b></td><td>");
        out.write(number);
        out.write("</td><td colspan='5'><i>");

        if (writeInterpretation || writeExplanation)
            out.write("(");
        if (writeInterpretation)
            out.write(interpretation);
        if (writeInterpretation && writeExplanation)
            out.write(" ");

        if (writeExplanation) {
            out.write("<a class='doNotPrint' href='#' " +
                            "onclick='togglePopupInfo(this); return false;'>");
            out.write(encodeHTML(resources.getDlgString("More")));
            out.write("</a>)<div class='popupInfo'><table width='300' " +
                            "onclick='togglePopupInfo(this.parentNode)'><tr><td>");
            out.write(HTMLUtils.escapeEntities(explanation));
            out.write("</td></tr></table></div>");
        } else if (writeInterpretation) {
            out.write(")");
        }
        out.write("</i></td></tr>\n");
    }


    private boolean exportingToExcel() {
        return ExcelReport.EXPORT_TAG.equals(getParameter("EXPORT"));
    }


    static final String TITLE_VAR = "%title%";
    static final String POPUP_HEADER =
        "<style>\n" +
        "a.noLine { text-decoration: none }\n" +
        ".popupInfo { position: relative; height: 0 }\n" +
        ".popupInfo table { " +
        "position: absolute; right: 0; display: none; " +
        "border: 1px solid black; background-color: #ccccff }\n" +
        "</style>\n" +
        "<script>\n" +
        "function togglePopupInfo(elm) {\n" +
        "   var table = elm.parentNode.getElementsByTagName(\"DIV\")[0].childNodes[0];\n" +
        "   if (table.style.display == \"block\")\n" +
        "      table.style.display = \"none\";\n" +
        "   else\n" +
        "      table.style.display = \"block\";\n" +
        "}\n" +
        "</script>\n";
    static final String HEADER_HTML =
        "<html><head><title>%title%</title>\n" +
        "<link rel=stylesheet type='text/css' href='/style.css'>\n" +
        "<style>td.timefmt { vnd.ms-excel.numberformat: [h]\\:mm }</style>\n" +
        POPUP_HEADER +
        "</head><body><h1>%title%</h1>\n";
    static final String COLOR_PARAMS =
        "&initGradColor=%23bebdff&finalGradColor=%23bebdff";
    static final String SEPARATE_CHARTS_HTML =
        "<pre>"+
        "<img src='ev.class?"+CHART_PARAM+"="+VALUE_CHART+COLOR_PARAMS+"'>" +
        "<img src='ev.class?"+CHART_PARAM+"="+TIME_CHART+COLOR_PARAMS+
        "&width=320&hideLegend'></pre>\n";
    static final String COMBINED_CHARTS_HTML =
        "<img src='ev.class?"+CHART_PARAM+"="+COMBINED_CHART+"'><br>\n";
    static final String EXPORT_HTML1A = "<a href=\"../reports/excel.iqy\"><i>";
    static final String EXPORT_HTML1B = "</i></a>&nbsp; &nbsp; &nbsp; &nbsp;";
    static final String EXPORT_HTML2A = "<a href='ev.xls'><i>";
    static final String EXPORT_HTML2B = "</i></a>&nbsp; &nbsp; &nbsp; &nbsp;";
    static final String OPT_FOOTER_HTML1 = "<a href='week.class'><i>";
    static final String OPT_FOOTER_HTML2 = "</i></a>";
    static final String FOOTER_HTML2 = "</body></html>";
    static final String EXCEL_TIME_TD = "<td class='timefmt'>";


    void writeTaskTable(EVTaskList taskList) throws IOException {
        TableModel table = taskList.getSimpleTableModel();
        HTMLTableWriter writer = getTableWriter(table, EVTaskList.toolTips);
        writer.setTableName("TASK");
        writer.setCellRenderer(EVTaskList.DEPENDENCIES_COLUMN,
                new DependencyCellRenderer(exportingToExcel()));
        if (!(taskList instanceof EVTaskListRollup))
            writer.setSkipColumn(EVTaskList.ASSIGNED_TO_COLUMN, true);
        writer.writeTable(out, table);
    }

    void writeScheduleTable(EVSchedule s) throws IOException {
        HTMLTableWriter writer = getTableWriter(s, s.getColumnTooltips());
        writer.setTableName("SCHEDULE");
        writer.writeTable(out, s);
    }

    private HTMLTableWriter getTableWriter(TableModel t, String[] toolTips) {
        HTMLTableWriter writer = new HTMLTableWriter();
        writer.setTableAttributes("border='1'");
        writer.setHeaderRenderer(
                new HTMLTableWriter.DefaultHTMLHeaderCellRenderer(toolTips));
        writer.setCellRenderer(new EVCellRenderer());

        for (int i = t.getColumnCount();  i-- > 0; )
            if (t.getColumnName(i).endsWith(" "))
                writer.setSkipColumn(i, true);

        return writer;
    }


    // Override the inherited definition of this function with a no-op.
    protected void buildData() {}

    /** Store a parameter value if that named parameter doesn't already
     * have a value */
    private void maybeWriteParam(String name, String value) {
        if (parameters.get(name) == null)
            parameters.put(name, value);
    }

    XYDataset xydata;

    /** Generate jpeg data for the plan-vs-actual time chart */
    public void writeTimeChart() throws IOException {
        // Create the data for the chart to draw.
        xydata = evModel.getSchedule().getTimeChartData();

        // Alter the appearance of the chart.
        maybeWriteParam
            ("title", resources.getString("Report.Time_Chart_Title"));

        super.writeContents();
    }

    /** Generate jpeg data for the plan-vs-actual earned value chart */
    public void writeValueChart() throws IOException {
        // Create the data for the chart to draw.
        xydata = evModel.getSchedule().getValueChartData();

        // Alter the appearance of the chart.
        maybeWriteParam("title", resources.getString("Report.EV_Chart_Title"));

        super.writeContents();
    }

    public void writeCombinedChart() throws IOException {
        // Create the data for the chart to draw.
        xydata = evModel.getSchedule().getCombinedChartData();

        // Alter the appearance of the chart.
        maybeWriteParam("title", "Cost & Schedule");

        super.writeContents();
    }

    /** Create a time series chart. */
    public JFreeChart createChart() {
        JFreeChart chart = TaskScheduleChart.createChart(xydata);
        if (parameters.get("hideLegend") == null)
            chart.getLegend().setAnchor(Legend.EAST);
        return chart;
    }

    public void writeTimeTable() {
        if (evModel != null)
                writeChartData(evModel.getSchedule().getTimeChartData(), 3);
        else
                writeFakeChartData("Plan", "Actual", "Forecast", null);
    }

    public void writeValueTable() {
        if (evModel != null)
                writeChartData(evModel.getSchedule().getValueChartData(), 3);
        else
                writeFakeChartData("Plan", "Actual", "Forecast", null);
    }

    public void writeValueTable2() {
        if (evModel != null) {
                EVSchedule s = evModel.getSchedule();
                int maxSeries = 3;
                if (s instanceof EVScheduleRollup) maxSeries = 4;
                writeChartData(s.getValueChartData(), maxSeries);
        } else
                writeFakeChartData("Plan", "Actual", "Forecast", "Optimized");
    }

    public void writeCombinedTable() {
        if (evModel != null)
                writeChartData(evModel.getSchedule().getCombinedChartData(), 3);
        else
                writeFakeChartData("Plan Value", "Actual Value", "Actual Time", null);
    }

    private void writeFakeChartData(String a, String b, String c, String d) {
                int maxSeries = (d == null ? 3 : 4);
                writeChartData(new FakeChartData(new String[] {a,b,c,d}), maxSeries);
        }


        /** Display excel-based data for drawing a chart */
    protected void writeChartData(XYDataset xydata, int maxSeries) {
        // First, print the table header.
        out.print("<html><body><table border>\n");
        int seriesCount = xydata.getSeriesCount();
        if (seriesCount > maxSeries) seriesCount = maxSeries;
        if (parameters.get("nohdr") == null) {
            out.print("<tr><td>"+getResource("Schedule.Date_Label")+"</td>");
            // print out the series names in the data source.
            for (int i = 0;  i < seriesCount;   i++)
                out.print("<td>" + xydata.getSeriesName(i) + "</td>");

            // if the data source came up short, fill in default
            // column headers.
            if (seriesCount < 1)
                out.print("<td>"+getResource("Schedule.Plan_Label")+"</td>");
            if (seriesCount < 2)
                out.print("<td>"+getResource("Schedule.Actual_Label")+"</td>");
            if (seriesCount < 3)
                out.print("<td>"+getResource("Schedule.Forecast_Label")+
                          "</td>");
            if (seriesCount < 4 && maxSeries == 4)
                out.print("<td>"+getResource("Schedule.Optimized_Label")+
                          "</td>");
            out.println("</tr>");
        }

        DateFormat f = DateFormat.getDateTimeInstance
            (DateFormat.MEDIUM, DateFormat.SHORT);
        for (int series = 0;  series < seriesCount;   series++) {
            int itemCount = xydata.getItemCount(series);
            for (int item = 0;   item < itemCount;   item++) {
                // print the date for the data item.
                out.print("<tr><td>");
                out.print(f.format(new Date
                    (xydata.getXValue(series,item).longValue())));
                out.print("</td>");

                // tab to the appropriate column
                for (int i=0;  i<series;  i++)   out.print("<td></td>");
                // print out the Y value for the data item.
                out.print("<td>");
                out.print(xydata.getYValue(series,item));
                out.print("</td>");
                // finish out the table row.
                for (int i=series+1;  i<seriesCount;  i++)
                    out.print("<td></td>");
                out.println("</tr>");
            }
        }
        if (seriesCount < maxSeries) {
            Date d = new Date();
            if (seriesCount > 0)
                d = new Date(xydata.getXValue(0,0).longValue());
            StringBuffer s = new StringBuffer();
            s.append("<tr><td>").append(f.format(d)).append("</td><td>");
            if (seriesCount < 1) s.append("0");
            s.append("</td><td>");
            if (seriesCount < 2) s.append("0");
            s.append("</td><td>");
            if (seriesCount < 3) s.append("0");
            if (maxSeries == 4) {
                s.append("</td><td>");
                if (seriesCount < 4) s.append("0");
            }
            s.append("</td></tr>\n");
            out.print(s.toString());
            out.print(s.toString());
        }
        out.println("</table></body></html>");
    }

    private class EVCellRenderer extends
            HTMLTableWriter.DefaultHTMLTableCellRenderer {

        public String getInnerHtml(Object value, int row, int column) {
            if (value instanceof Date)
                value = EVSchedule.formatDate((Date) value);

            return super.getInnerHtml(value, row, column);
        }

        public String getAttributes(Object value, int row, int column) {
            if (value instanceof String
                    && HOURS_MINUTES_PATTERN.matcher((String) value).matches())
                return "class='timefmt'";
            else
                return null;
        }

    }
    Pattern HOURS_MINUTES_PATTERN = Pattern.compile("\\d+:\\d\\d");


    static class DependencyCellRenderer implements HTMLTableWriter.CellRenderer {

        private static final String STOP_URI = "/Images/stop.gif";
        private static final String CHECK_URI = "/Images/check.gif";
        private static final String[] indicators = new String[] {
                "<span style='color:red; font-weight:bold'>"
                        + getRes(0, "Text") + "</span>",
                "<img src='" + STOP_URI + "' border='0' width='14' height='14'>",
                "<img src='" + CHECK_URI + "' border='0' width='14' height='14'>",
        };

        boolean plainText;

        public DependencyCellRenderer(boolean plainText) {
            this.plainText = plainText;
        }

        public String getInnerHtml(Object value, int row, int column) {
            TaskDependencyAnalyzer analyzer = new TaskDependencyAnalyzer(value);
            int status = analyzer.getStatus();
            if (status == TaskDependencyAnalyzer.NO_DEPENDENCIES)
                return null;
            else if (plainText)
                return getRes(status, "Text");

            StringBuffer result = new StringBuffer();
            result.append("<a class='noLine' href='#'"
                    + " title='" + getRes(status, "Explanation_All")
                    + "' onclick='togglePopupInfo(this); return false;'>");
            result.append(indicators[status]);
            result.append("</a><div class='popupInfo'>");
            result.append(analyzer.getHtmlTable(
                            "onclick='togglePopupInfo(this.parentNode)'",
                            STOP_URI, CHECK_URI, " &bull; ", false, true));
            result.append("</div>");
            return result.toString();
        }

        public String getAttributes(Object value, int row, int column) {
            return "style='text-align:center'";
        }

        private static String getRes(int status, String type) {
            String subkey = TaskDependencyAnalyzer.RES_KEYS[status];
            return resources.getHTML("Dependency." + subkey + "." + type);
        }

    }

    /** encode a snippet of text with appropriate HTML entities */
    final static String encodeHTML(String text) {
        if (text == null)
            return "";
        else
            return WebServer.encodeHtmlEntities(text);
    }

    final static String getResource(String key) {
        return encodeHTML(resources.getString(key)).replace('\n', ' ');
    }

    private class FakeChartData extends AbstractDataset implements XYDataset {

        private String[] seriesNames;

                public FakeChartData(String[] seriesNames) {
                        this.seriesNames = seriesNames;
                }

                public int getSeriesCount() {
                        return 4;
                }

                public String getSeriesName(int series) {
                        return seriesNames[series];
                }

                public int getItemCount(int series) {
                        return 2;
                }

                public Number getXValue(int series, int item) {
                        if (item == 0)
                                return new Integer(0);
                        else
                                return new Integer(24 * 60 * 60 * 1000);
                }

                public Number getYValue(int series, int item) {
                        if (item == 0)
                                return new Integer(0);
                        else switch (series) {
                        case 0: return new Integer(100);
                        case 1: return new Integer(60);
                        case 2: return new Integer(30);
                        default: return new Integer(5);
                        }
                }

    }
}
