// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil

import pspdash.*;
import pspdash.data.DateData;
import pspdash.data.DoubleData;
import pspdash.data.ResultSet;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Date;
import javax.swing.table.TableModel;

import com.jrefinery.chart.*;

/** CGI script for reporting earned value data in HTML.
 */
public class ev extends CGIChartBase {

    public static final String CHART_PARAM = "chart";
    public static final String TIME_CHART = "time";
    public static final String VALUE_CHART = "value";
    public static final String COMBINED_CHART = "combined";

    private static final int MED = EVMetrics.MEDIUM;

    boolean drawingChart;

    /** Write the CGI header.
     */
    protected void writeHeader() {
        drawingChart = (parameters.get(CHART_PARAM) != null);

        out.print("Content-type: ");
        out.print(drawingChart ? "image/jpeg" : "text/html");
        out.print("\r\n\r\n");

        // flush in case writeContents wants to use outStream instead of out.
        out.flush();
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
        if (chartType == null)
            writeHTML();
        else if (TIME_CHART.equals(chartType))
            writeTimeChart();
        else if (VALUE_CHART.equals(chartType))
            writeValueChart();
        else if (COMBINED_CHART.equals(chartType))
            writeCombinedChart();
        else
            ; // FIXME - error handling?
    }


    private void getEVModel() {
        taskListName = getPrefix();
        if (taskListName == null || taskListName.length() < 2) {
            // FIXME: error handling?
            return;
        }
        taskListName = taskListName.substring(1);

        long now = System.currentTimeMillis();

        synchronized (getClass()) {
            if (drawingChart &&
                (now - lastRecalcTime < MAX_DELAY) &&
                taskListName.equals(lastTaskListName))
                evModel = lastEVModel;
            else {
                lastTaskListName = taskListName;
                lastRecalcTime = now;
                lastEVModel = evModel =
                    new EVTaskList(taskListName,
                                   getDataRepository(),
                                   getPSPProperties(),
                                   false); // change notification not required
                evModel.recalc();
            }
        }
    }

    /** Generate a page of HTML displaying the Task and Schedule templates,
     *  and including img tags referencing charts.
     */
    public void writeHTML() {
        String taskListHTML = TinyWebServer.encodeHtmlEntities(taskListName);
        String taskListURL = URLEncoder.encode(taskListName);

        out.print(StringUtils.findAndReplace
                  (HEADER_HTML, TASK_LIST_VAR, taskListHTML));
        out.print(StringUtils.findAndReplace
                  (SEPARATE_CHARTS_HTML, TASK_LIST_VAR, taskListURL));

        EVSchedule s = evModel.getSchedule();
        EVMetrics  m = s.getMetrics();

        out.print("<table>");
        for (int i = 0;   i < m.getRowCount();   i++)
            writeMetric(m, i);
        out.print("</table>");

        out.print("<h2>Task Template</h2>\n");
        writeHTMLTable("TASK", evModel.getSimpleTableModel(),
                       evModel.toolTips);

        out.print("<h2>Schedule Template</h2>\n");
        writeHTMLTable("SCHEDULE", s, s.toolTips);

        out.print(FOOTER_HTML);
    }
    protected void writeMetric(EVMetrics m, int i) {
        String name = (String) m.getValueAt(i, EVMetrics.NAME);
        if (name == null) return;
        String number = (String) m.getValueAt(i, EVMetrics.SHORT);
        String interpretation = (String) m.getValueAt(i, EVMetrics.MEDIUM);
        String explanation = (String) m.getValueAt(i, EVMetrics.FULL);

        out.write("<tr><td><b>");
        out.write(name);
        out.write(":&nbsp;</b></td><td>");
        out.write(number);
        out.write("</td><td colspan='5'><I>(");
        if (!number.equals(interpretation)) {
            out.write(interpretation);
            out.write(" ");
        }
        out.write("<a href='javascript:alert(\"");
        out.write(explanation);
        out.write("\");'>More...</a>)</I></td></tr>\n");
    }

    static final String TASK_LIST_VAR = "%taskListName%";
    static final String HEADER_HTML =
        "<html><head><title>Earned Value - %taskListName%</title>\n" +
        "<link rel=stylesheet type='text/css' href='/style.css'>\n" +
        "</head><body><h1>Earned Value - %taskListName%</h1>\n";
    static final String SEPARATE_CHARTS_HTML =
        "<img src='ev.class?"+CHART_PARAM+"="+VALUE_CHART+"'>&nbsp;" +
        "<img src='ev.class?"+CHART_PARAM+"="+TIME_CHART+"'><br>\n";
    static final String COMBINED_CHARTS_HTML =
        "<img src='ev.class?"+CHART_PARAM+"="+COMBINED_CHART+"'><br>\n";
    static final String FOOTER_HTML =
        "<p class='doNotPrint'><a href=\"../reports/excel.iqy\">" +
        "<i>Export to Excel</i></a></body></html>";

    /** Generate an HTML table based on a TableModel.
     *
     * This is actually a very useful routine, that perhaps should
     * live elsewhere to promote reuse...
     */
    void writeHTMLTable(String name, TableModel t, String[] toolTips) {
        int numCols = t.getColumnCount();
        int numRows = t.getRowCount();

        // print the header row for the table.
        out.print("<table BORDER=1 name='");
        out.print(name);
        out.print("'><tr>");
        for (int c = 0;   c < numCols;   c++) {
            out.print("<td");
            if (toolTips != null) {
                out.print(" title='");
                out.print(encodeHTML(toolTips[c]));
                out.print("'");
            }
            out.print("><b>");
            out.print(encodeHTML(t.getColumnName(c)));
            out.print("</b></td>\n");
        }
        out.print("</tr>\n\n");

        // print out each row in the table.
        for (int r = 0;   r < numRows;   r++) {
            out.print("<tr>");
            for (int c = 0;   c < numCols;   c++) {
                out.print("<td>");
                out.print(encodeHTML(t.getValueAt(r, c)));
                out.print("</td>");
            }
            out.print("</tr>\n\n");
        }

        out.print("</table>\n\n");
    }

    // Override the inherited definition of this function with a no-op.
    protected void buildData() {}

    /** Store a parameter value if that named parameter doesn't already
     * have a value */
    private void maybeWriteParam(String name, String value) {
        if (parameters.get(name) == null)
            parameters.put(name, value);
    }

    XYDataSource xydata;

    /** Generate jpeg data for the plan-vs-actual time chart */
    public void writeTimeChart() throws IOException {
        // Create the data for the chart to draw.
        xydata = evModel.getSchedule().getTimeChartData();

        // Alter the appearance of the chart.
        maybeWriteParam("title", "Direct Hours");

        super.writeContents();
    }

    /** Generate jpeg data for the plan-vs-actual earned value chart */
    public void writeValueChart() throws IOException {
        // Create the data for the chart to draw.
        xydata = evModel.getSchedule().getValueChartData();

        // Alter the appearance of the chart.
        maybeWriteParam("title", "Earned Value (%)");

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
        JFreeChart chart = JFreeChart.createTimeSeriesChart(xydata);
        return chart;
    }

    /** translate an object to appropriate HTML */
    final static String encodeHTML(Object text) {
        if (text == null)
            return "";
        if (text instanceof Date)
            text = EVSchedule.formatDate((Date) text);

        return TinyWebServer.encodeHtmlEntities(text.toString());
    }

    /** encode a snippet of text with appropriate HTML entities */
    final static String encodeHTML(String text) {
        if (text == null)
            return "";
        else
            return TinyWebServer.encodeHtmlEntities(text);
    }
}
