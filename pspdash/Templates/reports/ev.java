// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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
// E-Mail POC:  processdash-devel@lists.sourceforge.net

import pspdash.*;
import pspdash.data.DateData;
import pspdash.data.DoubleData;
import pspdash.data.ResultSet;
import pspdash.data.StringData;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import javax.swing.table.TableModel;

import com.jrefinery.chart.*;

/** CGI script for reporting earned value data in HTML.
 */
public class ev extends CGIChartBase {

    public static final String CHART_PARAM = "chart";
    public static final String TABLE_PARAM = "table";
    public static final String XML_PARAM = "xml";
    public static final String TIME_CHART = "time";
    public static final String VALUE_CHART = "value";
    public static final String VALUE_CHART2 = "value2";
    public static final String COMBINED_CHART = "combined";


    private static final int MED = EVMetrics.MEDIUM;

    boolean drawingChart;

    /** Write the CGI header.
     */
    protected void writeHeader() {
        drawingChart = (parameters.get(CHART_PARAM) != null);
        if (parameters.get(XML_PARAM) != null) return;

        out.print("Content-type: ");
        if (drawingChart)
            out.print("image/jpeg");
        else
            out.print("text/html");
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
        if (chartType == null) {
            String tableType = getParameter(TABLE_PARAM);
            if (tableType == null) {
                if (parameters.get(XML_PARAM) != null)
                    writeXML();
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

        } else if (TIME_CHART.equals(chartType))
            writeTimeChart();
        else if (VALUE_CHART.equals(chartType))
            writeValueChart();
        else if (COMBINED_CHART.equals(chartType))
            writeCombinedChart();
        else
            ; // FIXME - error handling?
    }


    private void getEVModel() throws TinyCGIException {
        taskListName = getPrefix();
        if (taskListName == null || taskListName.length() < 2) {
            // FIXME: error handling?
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
            outStream.write(evModel.getAsXML().getBytes("UTF-8"));
            outStream.flush();
        }
    }
    private static final String XML_HEADER =
        "<?xml version='1.0' encoding='UTF-8'?>";



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

        Map errors = m.getErrors();
        if (errors != null && errors.size() > 0) {
            out.print("<table border><tr><td bgcolor='#ff5050'>" +
                      "<h2>Errors</h2><b>There are problems with this " +
                      "earned value schedule:<ul>");
            Iterator i = errors.keySet().iterator();
            while (i.hasNext())
                out.print("\n<li>" +
                          TinyWebServer.encodeHtmlEntities((String) i.next()));
            out.print("\n</ul>Until you correct these problems, calculations" +
                      " may be incorrect.</b></td></tr></table>\n");
        }


        out.print("<table name='STATS'>");
        for (int i = 0;   i < m.getRowCount();   i++)
            writeMetric(m, i);
        out.print("</table>");

        out.print("<h2>Task Template</h2>\n");
        writeHTMLTable("TASK", evModel.getSimpleTableModel(),
                       evModel.toolTips);

        out.print("<h2>Schedule Template</h2>\n");
        writeHTMLTable("SCHEDULE", s, s.getColumnTooltips());

        out.print("<p class='doNotPrint'>");
        out.print(EXPORT_HTML1);
        if (!parameters.containsKey("EXPORT"))
            out.print(EXPORT_HTML2);
        if (getDataRepository().getValue("/Enable_EV_Week_form") != null)
            out.print(OPT_FOOTER_HTML);
        out.print("</p>");
        out.print(FOOTER_HTML2);
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
        out.write("<a class='doNotPrint' href='javascript:alert(\"");
        out.write(explanation);
        out.write("\");'>More...</a>)</I></td></tr>\n");
    }

    static final String TASK_LIST_VAR = "%taskListName%";
    static final String HEADER_HTML =
        "<html><head><title>Earned Value - %taskListName%</title>\n" +
        "<link rel=stylesheet type='text/css' href='/style.css'>\n" +
        "</head><body><h1>Earned Value - %taskListName%</h1>\n";
    static final String COLOR_PARAMS =
        "&initGradColor=%23bebdff&finalGradColor=%23bebdff";
    static final String SEPARATE_CHARTS_HTML =
        "<pre>"+
        "<img src='ev.class?"+CHART_PARAM+"="+VALUE_CHART+COLOR_PARAMS+"'>" +
        "<img src='ev.class?"+CHART_PARAM+"="+TIME_CHART+COLOR_PARAMS+
        "&width=320&hideLegend'></pre>\n";
    static final String COMBINED_CHARTS_HTML =
        "<img src='ev.class?"+CHART_PARAM+"="+COMBINED_CHART+"'><br>\n";
    static final String EXPORT_HTML1 =
        "<a href=\"../reports/excel.iqy\"><i>Export text to Excel</i></a>" +
        "&nbsp; &nbsp; &nbsp; &nbsp;";
    static final String EXPORT_HTML2 =
        "<a href='ev.xls'><i>Export charts to Excel</i></a>" +
        "&nbsp; &nbsp; &nbsp; &nbsp;";
    static final String OPT_FOOTER_HTML =
        "<a href='week.class'><i>Show Weekly View</i></a>";
    static final String FOOTER_HTML2 = "</body></html>";

    /** Generate an HTML table based on a TableModel.
     *
     * This is actually a very useful routine, that perhaps should
     * live elsewhere to promote reuse...
     */
    void writeHTMLTable(String name, TableModel t, String[] toolTips) {
        int numCols = t.getColumnCount();
        int numRows = t.getRowCount();
        boolean[] hide = new boolean[numCols];

        // print the header row for the table.
        out.print("<table BORDER=1 name='");
        out.print(name);
        out.print("'><tr>");
        for (int c = 0;   c < numCols;   c++) {
            String columnName = t.getColumnName(c);
            hide[c] = columnName.endsWith(" ");
            if (hide[c]) continue;
            out.print("<td");
            if (toolTips != null) {
                out.print(" title='");
                out.print(encodeHTML(toolTips[c]));
                out.print("'");
            }
            out.print("><b>");
            out.print(encodeHTML(columnName));
            out.print("</b></td>\n");
        }
        out.print("</tr>\n\n");

        // print out each row in the table.
        for (int r = 0;   r < numRows;   r++) {
            out.print("<tr>");
            for (int c = 0;   c < numCols;   c++) {
                if (hide[c]) continue;
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

    public void writeTimeTable() {
        writeChartData(evModel.getSchedule().getTimeChartData(), 3);
    }

    public void writeValueTable() {
        writeChartData(evModel.getSchedule().getValueChartData(), 3);
    }

    public void writeValueTable2() {
        EVSchedule s = evModel.getSchedule();
        int maxSeries = 3;
        if (s instanceof EVScheduleRollup) maxSeries = 4;
        writeChartData(s.getValueChartData(), maxSeries);
    }

    public void writeCombinedTable() {
        writeChartData(evModel.getSchedule().getCombinedChartData(), 3);
    }

    /** Display excel-based data for drawing a chart */
    protected void writeChartData(XYDataSource xydata, int maxSeries) {
        // First, print the table header.
        out.print("<html><body><table border>\n");
        int seriesCount = xydata.getSeriesCount();
        if (seriesCount > maxSeries) seriesCount = maxSeries;
        if (parameters.get("nohdr") == null) {
            out.print("<tr><td>Date</td>");
            // print out the series names in the data source.
            for (int i = 0;  i < seriesCount;   i++)
                out.print("<td>" + xydata.getSeriesName(i) + "</td>");

            // if the data source came up short, fill in default
            // column headers.
            if (seriesCount < 1) out.print("<td>Plan</td>");
            if (seriesCount < 2) out.print("<td>Actual</td>");
            if (seriesCount < 3) out.print("<td>Forecast</td>");
            if (seriesCount < 4 && maxSeries == 4)
                                 out.print("<td>Optimized</td>");
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
