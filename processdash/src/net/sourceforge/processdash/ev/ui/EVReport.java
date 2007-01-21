// Copyright (C) 2003-2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.table.TableModel;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.ev.EVDependencyCalculator;
import net.sourceforge.processdash.ev.EVLabelFilter;
import net.sourceforge.processdash.ev.EVMetrics;
import net.sourceforge.processdash.ev.EVSchedule;
import net.sourceforge.processdash.ev.EVScheduleFiltered;
import net.sourceforge.processdash.ev.EVScheduleRollup;
import net.sourceforge.processdash.ev.EVTask;
import net.sourceforge.processdash.ev.EVTaskDependency;
import net.sourceforge.processdash.ev.EVTaskFilter;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.EVTaskListData;
import net.sourceforge.processdash.ev.EVTaskListRollup;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.cache.CachedURLObject;
import net.sourceforge.processdash.net.cms.SnippetEnvironment;
import net.sourceforge.processdash.net.http.TinyCGIException;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.ui.lib.HTMLTableWriter;
import net.sourceforge.processdash.ui.lib.HTMLTreeTableWriter;
import net.sourceforge.processdash.ui.lib.TreeTableModel;
import net.sourceforge.processdash.ui.web.CGIChartBase;
import net.sourceforge.processdash.ui.web.reports.ExcelReport;
import net.sourceforge.processdash.util.FileUtils;
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
    public static final String CSV_PARAM = "csv";
    public static final String TIME_CHART = "time";
    public static final String VALUE_CHART = "value";
    public static final String VALUE_CHART2 = "value2";
    public static final String COMBINED_CHART = "combined";
    public static final String FAKE_MODEL_NAME = "/  ";
    private static final String CUSTOMIZE_PARAM = "customize";
    static final String CUSTOMIZE_HIDE_PLAN_LINE = "hidePlanLine";
    static final String CUSTOMIZE_HIDE_FORECAST_LINE = "hideForecastLine";
    static final String CUSTOMIZE_HIDE_NAMES = "hideAssignedTo";
    static final String CUSTOMIZE_LABEL_FILTER =
        EVReportSettings.LABEL_FILTER_PARAM;


    private static Resources resources = Resources.getDashBundle("EV");

    boolean drawingChart;

    /** Write the CGI header.
     */
    protected void writeHeader() {
        drawingChart = (parameters.get(CHART_PARAM) != null);
        if (parameters.get(XML_PARAM) != null
                || parameters.get(XLS_PARAM) != null
                || parameters.get(CSV_PARAM) != null)
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

    /** Settings information to use for generating the report */
    EVReportSettings settings;
    /** The earned value model we are currently drawing */
    EVTaskList evModel = null;
    /** The name of the task list that creates that earned value model */
    String taskListName = null;
    /** True if we are rendering a snippet, to be embedded on a larger page */
    boolean isSnippet = false;

    /** Generate CGI output. */
    protected void writeContents() throws IOException {
        // load settings information for the report
        settings = new EVReportSettings(getDataRepository(), parameters,
                getPrefix());
        // possibly store settings data if requested.
        if (parameters.get(CUSTOMIZE_PARAM) != null) {
            storeCustomizationSettings();
            return;
        }

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
                else if (parameters.get(CSV_PARAM) != null)
                    writeCsv();
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
        taskListName = settings.getTaskListName();
        if (taskListName == null)
            throw new TinyCGIException(400, "schedule name missing");
        else if (FAKE_MODEL_NAME.equals(taskListName)) {
            evModel = null;
            return;
        }

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
                getDataRepository(), getPSPProperties(), getObjectCache());
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


    /** Generate a file of comma-separated data for use by MS Project.
     */
    private void writeCsv() throws IOException {
        if (evModel == null || evModel.isEmpty()) {
            out.print("Status: 404 Not Found\r\n\r\n");
            out.flush();
            return;
        }

        DateFormat fmt = new SimpleDateFormat("dd-MMM-yyyy");
        String filename = FileUtils.makeSafe(taskListName) + "-"
                + fmt.format(new Date()) + ".csv";

        out.print("Content-type: text/plain\r\n");
        out.print("Content-Disposition: attachment; filename=\""
                + filename + "\"\r\n\r\n");

        boolean simpleCsv = Settings.getBool("ev.simpleCsvOutput", false);

        List columns = null;
        if (simpleCsv) {
            columns = createSimpleCsvColumns();
        } else {
            columns = createCsvColumns();
            writeCsvColumnHeaders(columns);
        }

        TreeTableModel merged = evModel.getMergedModel(false, null);
        EVTask root = (EVTask) merged.getRoot();
        prepCsvColumns(columns, root, root, 1);
        writeCsvRows(columns, root, 1);
    }


    private void writeCsvColumnHeaders(List columns) {
        for (Iterator i = columns.iterator(); i.hasNext();) {
            CsvColumn c = (CsvColumn) i.next();
            out.print(c.header);
            if (i.hasNext())
                out.print(",");
            else
                out.print("\r\n");
        }
    }

    private List createSimpleCsvColumns() {
        List result = new LinkedList();

        result.add(new CsvColumn("Outline_Level") {
            public void write(EVTask node, int depth) {
                out.print(depth);
            }
        });

        result.add(new CsvColumn("Name") {
            public void write(EVTask node, int depth) {
                writeStringCsvField(node.getName());
            }
        });

        result.add(new CsvDateColumn("Finish_Date") {
            public Date getNodeDate(EVTask node) {
                Date result = node.getActualDate();
                if (result == null)
                    result = node.getPlanDate();
                return result;
            }
        });

        result.add(new CsvHoursColumn("Duration") {
            public double getNodeMinutes(EVTask node) {
                return node.getPlanValue();
            }
        });

        result.add(new CsvColumn("Resource_Names") {
            public void writeNode(EVTask node, int depth) {
                writeStringCsvField("");
            }
            public void writeLeaf(EVTask node, int depth) {
                writeStringCsvField(node.getAssignedToText());
            }
        });

        result.add(new CsvHoursColumn("Actual_Duration") {
            public double getNodeMinutes(EVTask node) {
                return node.getActualDirectTime();
            }
        });

        result.add(new CsvColumn("Percent_Complete") {
            public void writeNode(EVTask node, int depth) {
                out.print("0");
            }

            public void writeLeaf(EVTask node, int depth) {
                out.print(cleanupPercentComplete(node.getPercentComplete()));
            }
        });


        return result;
    }

    private List createCsvColumns() {
        List result = new LinkedList();

        final Map idNumbers = new HashMap();
        final Map taskIDs = new HashMap();

        result.add(new CsvColumn("ID") {
            int id = 1;
            public void doPrepWork(EVTask root, EVTask node, int depth) {
                idNumbers.put(node, new Integer(id++));
            }
            public void write(EVTask node, int depth) {
                out.print(idNumbers.get(node));
            }
        });

        result.add(new CsvColumn("Name") {
            public void write(EVTask node, int depth) {
                writeStringCsvField(node.getName());
            }
        });

        result.add(new CsvColumn("Outline_Level") {
            public void write(EVTask node, int depth) {
                out.print(depth);
            }
        });

        result.add(new CsvColumn("Predecessors") {

            public void doPrepWork(EVTask root, EVTask node, int depth) {
                // populate the taskIDs map so we can look up dashboard tasks
                // by their taskID later.
                List nodeIDs = node.getTaskIDs();
                if (nodeIDs != null)
                    for (Iterator i = nodeIDs.iterator(); i.hasNext();) {
                        String id = (String) i.next();
                        taskIDs.put(id, node);
                    }
            }

            public void write(EVTask node, int depth) {
                List dependencies = node.getDependencies();
                if (dependencies == null || dependencies.isEmpty())
                    return;

                List predIDs = new LinkedList();
                for (Iterator i = dependencies.iterator(); i.hasNext();) {
                    // find the dashboard task named by each dependency.
                    EVTaskDependency d = (EVTaskDependency) i.next();
                    String dashTaskID = d.getTaskID();
                    Object predTask = taskIDs.get(dashTaskID);
                    if (predTask == null) continue;
                    // look up the ID number we assigned to it in this CSV
                    // export file, and add that ID number to our list.
                    Object csvIdNumber = idNumbers.get(predTask);
                    if (csvIdNumber == null) continue;
                    predIDs.add(csvIdNumber);
                }

                if (!predIDs.isEmpty())
                    writeStringCsvField(StringUtils.join(predIDs, ","));
            }
        });

//        result.add(new CsvDateColumn("Start_Date") {
//            public Date getNodeDate(EVTask node) {
//                return node.getPlanStartDate();
//            }
//        });

        result.add(new CsvDateColumn("Finish_Date") {
            public Date getNodeDate(EVTask node) {
                Date result = node.getActualDate();
                if (result == null)
                    result = node.getPlanDate();
                return result;
            }
        });

        result.add(new CsvColumn("Percent_Complete") {
            public void writeNode(EVTask node, int depth) {
                out.print("0");
            }

            public void writeLeaf(EVTask node, int depth) {
                out.print(cleanupPercentComplete(node.getPercentComplete()));
            }
        });

//        result.add(new CsvDateColumn("Actual_Start") {
//            public Date getNodeDate(EVTask node) {
//                return nullToNA(node.getActualStartDate());
//            }
//        });
//
//        result.add(new CsvDateColumn("Actual_Finish") {
//            public Date getNodeDate(EVTask node) {
//                return nullToNA(node.getActualDate());
//            }
//        });

        result.add(new CsvHoursColumn("Duration") { // "Scheduled_Work") {
            public double getNodeMinutes(EVTask node) {
                return node.getPlanValue();
            }
        });

        result.add(new CsvHoursColumn("Actual_Duration") { // "Actual_Work") {
            public double getNodeMinutes(EVTask node) {
                return node.getActualDirectTime();
            }
        });

        result.add(new CsvColumn("Resource_Names") {
            public void writeLeaf(EVTask node, int depth) {
                writeStringCsvField(node.getAssignedToText());
            }
        });

        return result;
    }

    private void prepCsvColumns(List columns, EVTask root, EVTask node, int depth) {
        for (Iterator i = columns.iterator(); i.hasNext();) {
            CsvColumn c = (CsvColumn) i.next();
            c.doPrepWork(root, node, depth);
        }
        for (int i = 0;   i < node.getNumChildren();  i++)
            prepCsvColumns(columns, root, node.getChild(i), depth+1);
    }

    private void writeCsvRows(List columns, EVTask node, int depth) {
        for (Iterator i = columns.iterator(); i.hasNext();) {
            CsvColumn c = (CsvColumn) i.next();
            c.write(node, depth);
            if (i.hasNext())
                out.print(",");
            else
                out.print("\r\n");
        }
        for (int i = 0;   i < node.getNumChildren();  i++)
            writeCsvRows(columns, node.getChild(i), depth+1);
    }

    private void writeStringCsvField(String s) {
        out.print("\"");
        if (s != null)
            out.print(StringUtils.findAndReplace(s, "\"", "\"\""));
        out.print("\"");
    }

    private void writeDateCsvField(Date d) {
        if (d == null)
            out.print(" ");
        else if (d == EVSchedule.NEVER)
            out.print("NA");
        else
            out.print(CSV_DATE_FORMAT.format(d));
    }
    private static final DateFormat CSV_DATE_FORMAT = DateFormat
            .getDateInstance(DateFormat.SHORT);

    private class CsvColumn {
        String header;
        public CsvColumn(String header) {
            this.header = header;
        }
        public void doPrepWork(EVTask root, EVTask node, int depth) {}
        public void write(EVTask node, int depth) {
            if (node.isLeaf())
                writeLeaf(node, depth);
            else
                writeNode(node, depth);
        }
        public void writeLeaf(EVTask node, int depth) {}
        public void writeNode(EVTask node, int depth) {}
    }

    private abstract class CsvHoursColumn extends CsvColumn {
        public CsvHoursColumn(String header) {
            super(header);
        }
        public void writeLeaf(EVTask node, int depth) {
            out.print(HOURS_FMT.format(getNodeMinutes(node) / 60));
            out.print("h");
        }
        public void writeNode(EVTask node, int depth) {
            out.print("0h");
        }
        public abstract double getNodeMinutes(EVTask node);
    }
    private NumberFormat HOURS_FMT = NumberFormat.getInstance();

    private abstract class CsvDateColumn extends CsvColumn {
        public CsvDateColumn(String header) {
            super(header);
        }
        public void writeNode(EVTask node, int depth) {
            out.print(" ");
        }
        public void writeLeaf(EVTask node, int depth) {
            writeDateCsvField(getNodeDate(node));
        }
        public Date nullToNA(Date d) {
            return (d == null ? EVSchedule.NEVER : d);
        }
        public abstract Date getNodeDate(EVTask node);
    }


    // handle the storage and retrieval of customization settings.

    public void storeCustomizationSettings() throws IOException {
        out.println("<html><head><script>");
        if (parameters.containsKey("OK")) {
            settings.store(CUSTOMIZE_HIDE_PLAN_LINE, true);
            settings.store(CUSTOMIZE_HIDE_FORECAST_LINE, true);
            settings.store(CUSTOMIZE_HIDE_NAMES, true);
            settings.store(CUSTOMIZE_LABEL_FILTER, false);
            out.println("window.opener.location.reload();");
        }
        out.println("window.close();");
        out.println("</script></head>");
        // the text below generally will never appear to the user (the
        // javascript should close this window immediately)
        out.println("<body>Changes saved.</body></html>");
    }

    /** Generate a page of HTML displaying the Task and Schedule templates,
     *  and including img tags referencing charts.
     */
    public void writeHTML() throws IOException {
        isSnippet = (env.containsKey(SnippetEnvironment.SNIPPET_ID));
        String taskListHTML = WebServer.encodeHtmlEntities(taskListName);
        String title = resources.format("Report.Title_FMT", taskListHTML);

        EVTaskFilter taskFilter = settings.getEffectiveFilter(evModel);

        StringBuffer header = new StringBuffer(HEADER_HTML);
        StringUtils.findAndReplace(header, TITLE_VAR, title);
        if (taskFilter != null && isSnippet == false)
            header.insert(header.indexOf("</head>"), FILTER_HEADER_HTML);
        out.print(header);

        out.print(isSnippet ? "<h2>" : "<h1>");
        out.print(title);
        if (!exportingToExcel())
            interpOutLink(SHOW_WEEK_LINK, EVReportSettings.PURPOSE_WEEK);
        printCustomizationLink();
        out.print(isSnippet ? "</h2>" : "</h1>");

        if (!isSnippet)
            printFilterInfo(out, taskFilter, exportingToExcel());

        if (!exportingToExcel()) {
            String imgHtml = (taskFilter == null
                    ? SEPARATE_CHARTS_HTML
                    : COMBINED_CHARTS_HTML);
            interpOutLink(imgHtml, EVReportSettings.PURPOSE_IMAGE);
            out.print(HTMLTreeTableWriter.TREE_ICON_HEADER);
        }

        EVSchedule s = getEvSchedule(taskFilter);
        EVMetrics m = s.getMetrics();

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

        boolean hidePlan = settings.getBool(CUSTOMIZE_HIDE_PLAN_LINE);
        boolean hideForecast = settings.getBool(CUSTOMIZE_HIDE_FORECAST_LINE);
        out.print("<table name='STATS'>");
        for (int i = 0;   i < m.getRowCount();   i++)
            writeMetric(m, i, hidePlan, hideForecast);
        out.print("</table>");

        out.print("<h2><a name='$$$_tasks'></a>"+getResource("TaskList.Title"));
        printTaskStyleLink();
        out.print("</h2>\n");
        if (isFlatView())
            writeTaskTable(evModel, taskFilter, hidePlan, hideForecast);
        else
            writeTaskTree(evModel, taskFilter, hidePlan, hideForecast);

        out.print("<h2>"+getResource("Schedule.Title")+"</h2>\n");
        writeScheduleTable(s);

        out.print("<p class='doNotPrint'>");
        if (!isSnippet && !exportingToExcel())
            interpOutLink(EXPORT_TEXT_LINK);

        if (!parameters.containsKey("EXPORT")) {
            if (taskFilter == null) {
                interpOutLink(EXPORT_CHARTS_LINK);
                interpOutLink(EXPORT_MSPROJ_LINK);
            }
            if (!isSnippet) {
                String link = EXPORT_ARCHIVE_LINK;
                String filenamePat = HTMLUtils.urlEncode(
                        resources.getString("Report.Archive_Filename"));
                link = StringUtils.findAndReplace(link, "FILENAME", filenamePat);
                interpOutLink(link);
            }
        }

        out.print("</p>");
        out.print("</body></html>");
    }


    private EVSchedule getEvSchedule(EVTaskFilter taskFilter) {
        if (taskFilter == null)
            return evModel.getSchedule();
        else
            return new EVScheduleFiltered(evModel, taskFilter);
    }


    private void interpOutLink(String html) {
        interpOutLink(html, EVReportSettings.PURPOSE_OTHER);
    }
    private void interpOutLink(String html, int purpose) {
        html = StringUtils.findAndReplace(html, "@@@", settings.getEffectivePrefix());
        String query = settings.getQueryString(purpose);
        html = StringUtils.findAndReplace(html, "???", query);
        if (StringUtils.hasValue(query))
            html = StringUtils.findAndReplace(html, "??&", query + "&");
        else
            html = StringUtils.findAndReplace(html, "??&", "?");
        html = resources.interpolate(html, HTMLUtils.ESC_ENTITIES);
        out.print(html);
    }





    static void printFilterInfo(PrintWriter out, EVTaskFilter filter,
            boolean textOnly) {
        if (filter == null)
            return;

        String labelFilter = filter.getAttribute(EVLabelFilter.FILTER_ATTR);
        if (labelFilter != null) {
            out.print("<h2>");
            if (!textOnly)
                out.print("<img border=0 src='/Images/filter.png' "
                        + "style='margin-right:2px' "
                        + "width='16' height='23' title=\"");
            out.print(resources.getHTML("Report.Filter_Tooltip"));
            out.print(textOnly ? " - " : "\">");
            out.print(HTMLUtils.escapeEntities(labelFilter));
            out.println("</h2>");
        }
    }


    private void printCustomizationLink() {
        if (!parameters.containsKey("EXPORT")) {
            out.print("<span " + HEADER_LINK_STYLE + ">"
                    + "<span class='doNotPrint'><a href='");
            out.print(settings.getEffectivePrefix());
            out.print("ev-customize.shtm?a");
            if ((evModel instanceof EVTaskListRollup))
                out.print("&isRollup");
            if (!parameters.containsKey(EVReportSettings.LABEL_FILTER_PARAM)
                    && EVLabelFilter.taskListContainsLabelData(evModel,
                            getDataRepository()))
                out.print("&showLabelFilter");
            out.print("' target='customize' onClick='openCustomizeWindow();'>");
            out.print(HTMLUtils.escapeEntities(resources
                    .getDlgString("Customize")));
            out.print("</a></span></span>");
            out.print("<script>\n" +
                    "function openCustomizeWindow() {\n" +
                    "    var newWind = window.open ('', 'customize',\n" +
                    "        'scrollbars=yes,dependent=yes,resizable=yes,width=420,height=200');\n" +
                    "    newWind.focus();\n" +
                    "}\n" +
                    "</script>\n");
        }
    }


    private void printTaskStyleLink() {
        if (!exportingToExcel()) {
            boolean isFlat = isFlatView();
            out.print("<span " + HEADER_LINK_STYLE + ">"
                    + "<span class='doNotPrint'>"
                    + "<a id=\"$$$_taskstylelink\" href=\"");

            StringBuffer href = new StringBuffer();

            String uri = (String) env.get(SnippetEnvironment.CURRENT_FRAME_URI);
            href.append(uri == null ? "ev.class" : uri);

            if (isFlat) {
                if (uri != null) {
                    Matcher m = FLAT_PARAM_PATTERN.matcher(uri);
                    if (m.find())
                        HTMLUtils.removeParam(href, m.group());
                }
            } else {
                href.append(href.indexOf("?") == -1 ? '?' : '&');
                href.append(isSnippet ? "$$$_flat=t" : "flat=t");
            }

            HTMLUtils.appendQuery(href, settings
                    .getQueryString(EVReportSettings.PURPOSE_TASK_STYLE));

            out.print(href.toString());

            out.print("#$$$_tasks\">");
            out.print(resources.getHTML(isFlat ? "Report.Tree_View"
                    : "Report.Flat_View"));
            out.print("</a></span></span>");
        }
    }
    private Pattern FLAT_PARAM_PATTERN = Pattern.compile("[^&?]*flat");


    protected void writeMetric(EVMetrics m, int i, boolean hidePlan,
            boolean hideForecast) {
        String metricID = (String) m.getValueAt(i, EVMetrics.METRIC_ID);
        if (hidePlan && metricID.indexOf("Plan_") != -1) return;
        if (hideForecast && metricID.indexOf("Forecast_") != -1) return;

        String name = (String) m.getValueAt(i, EVMetrics.NAME);
        if (name == null) return;
        String number = (String) m.getValueAt(i, EVMetrics.SHORT);
        String interpretation = (String) m.getValueAt(i, EVMetrics.MEDIUM);
        String explanation = (String) m.getValueAt(i, EVMetrics.FULL);


        boolean writeInterpretation = !number.equals(interpretation);
        boolean writeExplanation = !exportingToExcel();
        boolean printExplanation = Settings.getBool(
                "ev.printMetricsExplanations", true);

        out.write("<tr><td valign='top'><b>");
        out.write(name);
        out.write(":&nbsp;</b></td><td valign='top'>");
        out.write(number);
        out.write("</td><td colspan='5' valign='top'>");
        if (printExplanation)
            out.write("<i class='doNotPrint'>");
        else
            out.write("<i>");

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
        out.write("</i>");

        if (writeExplanation && printExplanation) {
            out.write("<i class='printOnly' style='margin-bottom:1em'>(");
            out.write(HTMLUtils.escapeEntities(explanation));
            out.write(")</i>");
        }

        out.write("</td></tr>\n");
    }


    private boolean exportingToExcel() {
        return ExcelReport.EXPORT_TAG.equals(getParameter("EXPORT"));
    }

    private boolean isFlatView() {
        return parameters.containsKey("flat") || exportingToExcel();
    }


    static final String TITLE_VAR = "%title%";
    static final String REDUNDANT_EXCEL_HEADER =
        "<style>.timeFmt { vnd.ms-excel.numberformat: [h]\\:mm }</style>\n";
    static final String POPUP_HEADER =
        "<link rel=stylesheet type='text/css' href='/lib/popup.css'>\n" +
        "<script type='text/javascript' src='/lib/popup.js'></script>\n";
    static final String SORTTABLE_HEADER =
        "<link rel=stylesheet type='text/css' href='/lib/sorttable.css'>\n" +
        "<script type='text/javascript' src='/lib/sorttable.js'></script>\n";
    static final String SORTTREE_HEADER =
        "<script type='text/javascript' src='/reports/evTreeSort.js'></script>\n";
    static final String HEADER_HTML =
        "<html><head><title>%title%</title>\n" +
        "<link rel=stylesheet type='text/css' href='/style.css'>\n" +
        HTMLTreeTableWriter.TREE_HEADER_ITEMS +
        REDUNDANT_EXCEL_HEADER +
        POPUP_HEADER +
        SORTTABLE_HEADER +
        SORTTREE_HEADER +
        "</head><body>";
    static final String FILTER_HEADER_HTML =
        "<link rel=stylesheet type='text/css' href='/reports/filter-style.css'>\n";
    static final String HEADER_LINK_STYLE = " style='font-size: medium; " +
        "font-style: italic; font-weight: normal; margin-left: 0.5cm' ";
    static final String COLOR_PARAMS =
        "&initGradColor=%23bebdff&finalGradColor=%23bebdff";
    static final String SEPARATE_CHARTS_HTML =
        "<pre>"+
        "<img src='ev.class??&"+CHART_PARAM+"="+VALUE_CHART+COLOR_PARAMS+"'>" +
        "<img src='ev.class??&"+CHART_PARAM+"="+TIME_CHART+COLOR_PARAMS+
        "&width=320&hideLegend'></pre>\n";
    static final String COMBINED_CHARTS_HTML =
        "<img src='ev.class??&"+CHART_PARAM+"="+COMBINED_CHART+COLOR_PARAMS+
        "&width=720'><br>\n";
    static final String EXPORT_TEXT_LINK = "<a href=\"@@@excel.iqy\"><i>"
            + "${Report.Export_Text}</i></a>&nbsp; &nbsp; &nbsp; &nbsp;";
    static final String EXPORT_CHARTS_LINK = "<a href='@@@ev.xls'><i>"
            + "${Report.Export_Charts}</i></a>&nbsp; &nbsp; &nbsp; &nbsp;";
    static final String EXPORT_MSPROJ_LINK =
            "<a href='@@@ev-project-instr.htm'><i>"
            + "${Report.Export_Project}</i></a>&nbsp; &nbsp; &nbsp; &nbsp;";
    static final String EXPORT_ARCHIVE_LINK =
            "<a href='../dash/archive.class?filename=FILENAME'><i>"
            + "${Report.Export_Archive}</i></a>&nbsp; &nbsp; &nbsp; &nbsp;";
    static final String SHOW_WEEK_LINK = "<span " + HEADER_LINK_STYLE + ">"
            + "<span class='doNotPrint'><a href='week.class???'>"
            + "${Report.Show_Weekly_View}</a></span></span>";
    static final String EXCEL_TIME_TD = "<td class='timeFmt'>";


    void writeTaskTable(EVTaskList taskList, EVTaskFilter filter,
            boolean hidePlan, boolean hideForecast) throws IOException {
        HTMLTableWriter writer = new HTMLTableWriter();
        boolean showTimingIcons = taskList instanceof EVTaskListData
                && parameters.get("EXPORT") == null;
        TableModel table = customizeTaskTableWriter(writer, taskList, filter,
                hidePlan, hideForecast, showTimingIcons);
        writer.setTableAttributes("class='sortable' id='$$$_task' border='1'");
        writer.writeTable(out, table);
    }

    void writeTaskTree(EVTaskList taskList, EVTaskFilter filter,
            boolean hidePlan, boolean hideForecast) throws IOException {
        TreeTableModel tree = taskList.getMergedModel(true, filter);
        HTMLTreeTableWriter writer = new HTMLTreeTableWriter();
        customizeTaskTableWriter(writer, taskList, null, hidePlan,
                hideForecast, false);
        writer.setTreeName("$$$_t");
        writer.setTableAttributes(
                "class='needsTreeSortLinks' id='$$$_task' border='1'");
        writer.writeTree(out, tree);

        int depth = Settings.getInt("ev.showHierarchicalDepth", 3);
        out.write("<script>collapseAllRows(" + depth + ");</script>");
    }

    void writeScheduleTable(EVSchedule s) throws IOException {
        HTMLTableWriter writer = new HTMLTableWriter();
        customizeTableWriter(writer, s, s.getColumnTooltips());
        setupRenderers(writer, EVSchedule.COLUMN_FORMATS);
        writer.setTableName("SCHEDULE");
        writer.writeTable(out, s);
    }

    private TableModel customizeTaskTableWriter(HTMLTableWriter writer,
            EVTaskList taskList, EVTaskFilter filter, boolean hidePlan,
            boolean hideForecast, boolean showTimingIcons) {
        TableModel table = taskList.getSimpleTableModel(filter);
        customizeTableWriter(writer, table, EVTaskList.toolTips);
        writer.setTableName("TASK");
        writer.setSkipColumn(EVTaskList.PLAN_CUM_TIME_COLUMN, true);
        writer.setSkipColumn(EVTaskList.PLAN_CUM_VALUE_COLUMN, true);
        boolean hideNames = settings.getBool(CUSTOMIZE_HIDE_NAMES);
        setupTaskTableRenderers(writer, showTimingIcons, exportingToExcel(),
                hideNames);
        if (!(taskList instanceof EVTaskListRollup) || hideNames)
            writer.setSkipColumn(EVTaskList.ASSIGNED_TO_COLUMN, true);
        if (hidePlan)
            writer.setSkipColumn(EVTaskList.PLAN_DATE_COLUMN, true);
        if (hideForecast)
            writer.setSkipColumn(EVTaskList.FORECAST_DATE_COLUMN, true);
        return table;
    }


    /** Install renderers that are appropriate for a task table. */
    static HTMLTableWriter setupTaskTableRenderers(HTMLTableWriter writer,
            boolean showTimingIcons, boolean exportingToExcel, boolean hideNames) {
        setupRenderers(writer, EVTaskList.COLUMN_FORMATS);
        writer.setCellRenderer(EVTaskList.DEPENDENCIES_COLUMN,
                new DependencyCellRenderer(exportingToExcel, hideNames));
        if (showTimingIcons)
            writer.setCellRenderer(EVTaskList.TASK_COLUMN,
                    new TaskNameWithTimingIconRenderer());
        return writer;
    }

    private static void setupRenderers(HTMLTableWriter w, Object[] formats) {
        w.setCellRenderer(EV_CELL_RENDERER);
        for (int col = 0; col < formats.length; col++) {
            Object cellRenderer = RENDERERS.get(formats[col]);
            if (cellRenderer == null)
                cellRenderer = EV_CELL_RENDERER;
            w.setCellRenderer(col, (HTMLTableWriter.CellRenderer) cellRenderer);
        }
    }

    private void customizeTableWriter(HTMLTableWriter writer, TableModel t,
            String[] toolTips) {
        writer.setTableAttributes("border='1'");
        writer.setHeaderRenderer(
                new HTMLTableWriter.DefaultHTMLHeaderCellRenderer(toolTips));

        for (int i = t.getColumnCount();  i-- > 0; )
            if (t.getColumnName(i).endsWith(" "))
                writer.setSkipColumn(i, true);
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

        // possibly hide lines on the chart, at user request.
        boolean hidePlan = settings.getBool(CUSTOMIZE_HIDE_PLAN_LINE);
        boolean hideForecast = settings.getBool(CUSTOMIZE_HIDE_FORECAST_LINE);
        if (hidePlan || hideForecast)
            xydata = new EVDatasetFilter(xydata, hidePlan, hideForecast, 2);

        // Alter the appearance of the chart.
        maybeWriteParam
            ("title", resources.getString("Report.Time_Chart_Title"));

        super.writeContents();
    }

    /** Generate jpeg data for the plan-vs-actual earned value chart */
    public void writeValueChart() throws IOException {
        // Create the data for the chart to draw.
        xydata = evModel.getSchedule().getValueChartData();

        // possibly hide lines on the chart, at user request.
        boolean hidePlan = settings.getBool(CUSTOMIZE_HIDE_PLAN_LINE);
        boolean hideForecast = settings.getBool(CUSTOMIZE_HIDE_FORECAST_LINE);
        if (hidePlan || hideForecast)
            xydata = new EVDatasetFilter(xydata, hidePlan, hideForecast, 2);

        // Alter the appearance of the chart.
        maybeWriteParam("title", resources.getString("Report.EV_Chart_Title"));

        super.writeContents();
    }

    public void writeCombinedChart() throws IOException {
        // Create the data for the chart to draw.
        EVSchedule s = getEvSchedule(settings.getEffectiveFilter(evModel));
        xydata = s.getCombinedChartData();

        // possibly hide lines on the chart, at user request.
        boolean hidePlan = settings.getBool(CUSTOMIZE_HIDE_PLAN_LINE);
        boolean hideForecast = settings.getBool(CUSTOMIZE_HIDE_FORECAST_LINE);
        if (hidePlan || hideForecast)
            xydata = new EVDatasetFilter(xydata, hidePlan, hideForecast, 3);

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
            writeFakeChartData("Plan Value","Actual Value","Actual Time",null);
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

    private String cleanupPercentComplete(String percent) {
        if (percent == null || percent.length() == 0)
            percent = "0";
        else if (percent.endsWith("%"))
            percent = percent.substring(0, percent.length()-1);
        return percent;
    }

    private static class EVCellRenderer extends
            HTMLTableWriter.DefaultHTMLTableCellRenderer {

        public String getInnerHtml(Object value, int row, int column) {
            if (value instanceof Date)
                value = EVSchedule.formatDate((Date) value);

            return super.getInnerHtml(value, row, column);
        }

        protected String getSortKey(Object value) {
            return null;
        }

        public String getAttributes(Object value, int row, int column) {
            return getSortAttribute(getSortKey(value));
        }

    }

    private static class EVDateCellRenderer extends EVCellRenderer {

        protected String getSortKey(Object value) {
            if (value instanceof Date)
                return Long.toString(((Date) value).getTime());
            else
                // The dates we display are typically completion dates.
                // if a date is missing, that implies not yet completed;
                // such dates should sort after all reasonable date values
                return "9999999999999";
        }

    }

    private static class EVPercentCellRenderer extends EVCellRenderer {

        protected String getSortKey(Object value) {
            if (value == null || "".equals(value))
                return "0";
            else
                return StringUtils.findAndReplace(value.toString(), "%", "");
        }

    }

    private static class EVTimeCellRenderer extends EVCellRenderer {
        public String getAttributes(Object value, int r, int c) {
            return "class='timeFmt' " + super.getAttributes(value, r, c);
        }

        protected String getSortKey(Object value) {
            if (value == null || "".equals(value))
                return "0";
            else
                return value.toString().replace(':', '.');
        }

    }

    static final EVCellRenderer EV_CELL_RENDERER = new EVCellRenderer();
    static final EVTimeCellRenderer TIME_CELL_RENDERER = new EVTimeCellRenderer();
    private static Map RENDERERS;
    static {
        Map r = new HashMap();
        r.put(EVTaskList.COLUMN_FMT_OTHER, EV_CELL_RENDERER);
        r.put(EVTaskList.COLUMN_FMT_TIME, TIME_CELL_RENDERER);
        r.put(EVTaskList.COLUMN_FMT_DATE, new EVDateCellRenderer());
        r.put(EVTaskList.COLUMN_FMT_PERCENT, new EVPercentCellRenderer());
        RENDERERS = Collections.unmodifiableMap(r);
    }

    static class TaskNameWithTimingIconRenderer extends
            HTMLTableWriter.DefaultHTMLTableCellRenderer {

        public String getInnerHtml(Object value, int row, int column) {
            return super.getInnerHtml(value, row, column)
                    + getTimingLink((String) value);
        }
    }


    static class DependencyCellRenderer implements HTMLTableWriter.CellRenderer {

        boolean plainText;
        boolean hideNames;

        public DependencyCellRenderer(boolean plainText, boolean hideNames) {
            this.plainText = plainText;
            this.hideNames = hideNames;
        }

        public String getInnerHtml(Object value, int row, int column) {
            TaskDependencyAnalyzer.HTML analyzer =
                new TaskDependencyAnalyzer.HTML(value, hideNames);
            int status = analyzer.getStatus();
            if (status == TaskDependencyAnalyzer.NO_DEPENDENCIES)
                return null;
            else if (plainText)
                return analyzer.getRes("Text");

            StringBuffer result = new StringBuffer();
            result.append("<a style='text-decoration: none' href='#'"
                    + " onclick='togglePopupInfo(this); return false;'>");
            result.append(analyzer.getHtmlIndicator());
            result.append("</a><div class='popupInfo'>");
            result.append(analyzer.getHtmlTable(
                            "onclick='togglePopupInfo(this.parentNode)'"));
            result.append("</div>");
            return result.toString();
        }

        public String getAttributes(Object value, int row, int column) {
            TaskDependencyAnalyzer.HTML analyzer =
                new TaskDependencyAnalyzer.HTML(value, hideNames);
            return "style='text-align:center' "
                    + getSortAttribute(analyzer.getSortKey());
        }

    }

    static String getSortAttribute(String sortKey) {
        if (sortKey == null)
            return null;
        else
            return SORTKEY_ATTRIBUTE + "='" + HTMLUtils.escapeEntities(sortKey)
                    + "'";
    }
    private static final String SORTKEY_ATTRIBUTE = "sortkey";


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

    static String getTimingLink(String path) {
        if (path == null || path.length() == 0)
            return "";

        return "&nbsp;<a class=doNotPrint href=\""
                + WebServer.urlEncodePath(path)
                + "//control/setPath.class?start\"><img border=\"0\" title=\""
                + resources.getHTML("Start_timing")
                + "\" src=\"/control/startTiming.png\"></A>";
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
