// Copyright (C) 2001-2016 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ev.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleEdge;
import org.w3c.dom.Element;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.ev.DefaultTaskLabeler;
import net.sourceforge.processdash.ev.EVDependencyCalculator;
import net.sourceforge.processdash.ev.EVHierarchicalFilter;
import net.sourceforge.processdash.ev.EVLabelFilter;
import net.sourceforge.processdash.ev.EVMetadata;
import net.sourceforge.processdash.ev.EVMetrics;
import net.sourceforge.processdash.ev.EVSchedule;
import net.sourceforge.processdash.ev.EVScheduleFiltered;
import net.sourceforge.processdash.ev.EVScheduleRollup;
import net.sourceforge.processdash.ev.EVTask;
import net.sourceforge.processdash.ev.EVTaskDependency;
import net.sourceforge.processdash.ev.EVTaskFilter;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.EVTaskListData;
import net.sourceforge.processdash.ev.EVTaskListGroupFilter;
import net.sourceforge.processdash.ev.EVTaskListMerged;
import net.sourceforge.processdash.ev.EVTaskListRollup;
import net.sourceforge.processdash.ev.MilestoneList;
import net.sourceforge.processdash.ev.ui.TaskScheduleChartUtil.ChartItem;
import net.sourceforge.processdash.ev.ui.TaskScheduleChartUtil.ChartListPurpose;
import net.sourceforge.processdash.ev.ui.chart.AbstractEVChart;
import net.sourceforge.processdash.ev.ui.chart.AbstractEVTimeSeriesChart;
import net.sourceforge.processdash.ev.ui.chart.HelpAwareEvChart;
import net.sourceforge.processdash.ev.ui.chart.HtmlEvChart;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.cache.CachedURLObject;
import net.sourceforge.processdash.net.cms.CMSSnippetEnvironment;
import net.sourceforge.processdash.net.http.TinyCGIException;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.team.group.UserFilter;
import net.sourceforge.processdash.team.group.UserGroup;
import net.sourceforge.processdash.team.group.UserGroupMember;
import net.sourceforge.processdash.team.group.UserGroupPrivacyBlock;
import net.sourceforge.processdash.templates.ExtensionManager;
import net.sourceforge.processdash.ui.lib.HTMLTableWriter;
import net.sourceforge.processdash.ui.lib.HTMLTreeTableWriter;
import net.sourceforge.processdash.ui.lib.TreeTableModel;
import net.sourceforge.processdash.ui.lib.chart.XYDatasetFilter;
import net.sourceforge.processdash.ui.snippet.SnippetEnvironment;
import net.sourceforge.processdash.ui.snippet.SnippetWidget;
import net.sourceforge.processdash.ui.web.CGIChartBase;
import net.sourceforge.processdash.ui.web.reports.ExcelReport;
import net.sourceforge.processdash.util.FastDateFormat;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.HTTPUtils;
import net.sourceforge.processdash.util.OrderedListMerger;
import net.sourceforge.processdash.util.StringUtils;



/** CGI script for reporting earned value data in HTML.
 */
public class EVReport extends CGIChartBase {

    public static final String CHART_PARAM = "chart";
    public static final String CHARTS_PARAM = "charts";
    public static final String SINGLE_CHART_PARAM = "showChart";
    public static final String CHART_OPTIONS_PARAM = "chartOptions";
    public static final String TABLE_PARAM = "table";
    public static final String XML_PARAM = "xml";
    public static final String XLS_PARAM = "xls";
    public static final String CSV_PARAM = "csv";
    static final String MS_PROJ_XML_PARAM = "msProjXml";
    public static final String MERGED_PARAM = "merged";
    public static final String TIME_CHART = "time";
    public static final String VALUE_CHART = "value";
    public static final String VALUE_CHART2 = "value2";
    public static final String COMBINED_CHART = "combined";
    public static final String FAKE_MODEL_NAME = "/  ";
    private static final String CUSTOMIZE_PARAM = "customize";
    static final String CUSTOMIZE_HIDE_BASELINE = "hideBaseline";
    static final String CUSTOMIZE_HIDE_PLAN_LINE = "hidePlanLine";
    static final String CUSTOMIZE_HIDE_REPLAN_LINE = "hideReplanLine";
    static final String CUSTOMIZE_HIDE_FORECAST_LINE = "hideForecastLine";
    static final String CUSTOMIZE_HIDE_NAMES = "hideAssignedTo";
    static final String CUSTOMIZE_LABEL_FILTER =
        EVReportSettings.LABEL_FILTER_PARAM;
    static final String TASK_STYLE_PARAM = "taskStyle";


    private static Resources resources = Resources.getDashBundle("EV");
    private static Logger logger = Logger.getLogger(EVReport.class.getName());

    boolean drawingChart;



    @Override
    protected void doPost() throws IOException {
        parseFormData();
        super.doPost();
    }


    /** Write the CGI header.
     */
    protected void writeHeader() {
        drawingChart = (parameters.get(CHART_PARAM) != null);
        if (parameters.get(XML_PARAM) != null
                || parameters.get(MS_PROJ_XML_PARAM) != null
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
    static WeakReference<EVTaskList> lastEVModel = null;

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
                else if (parameters.get(MS_PROJ_XML_PARAM) != null)
                    writeMSProjXml();
                else if (parameters.get(CHARTS_PARAM) != null)
                    writeChartsPage();
                else if (parameters.get(CHART_OPTIONS_PARAM) != null)
                    writeChartOptions();
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

        synchronized (EVReport.class) {
            if (drawingChart &&
                (now - lastRecalcTime < MAX_DELAY) &&
                taskListName.equals(lastTaskListName)) {
                evModel = lastEVModel.get();
                if (evModel != null)
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

        UserFilter f = settings.getUserGroupFilter();
        if (f != null && !UserGroup.isEveryone(f)
                && evModel instanceof EVTaskListRollup)
            ((EVTaskListRollup) evModel)
                    .applyTaskListFilter(new EVTaskListGroupFilter(f));

        EVDependencyCalculator depCalc = new EVDependencyCalculator(
                getDataRepository(), getPSPProperties(), getObjectCache());
        evModel.setDependencyCalculator(depCalc);
        evModel.setTaskLabeler(new DefaultTaskLabeler(getDashboardContext()));

        if (settings.getBool(CUSTOMIZE_HIDE_BASELINE))
            evModel.disableBaselineData();

        evModel.recalc();

        synchronized (EVReport.class) {
            lastTaskListName = taskListName;
            lastRecalcTime = now;
            lastEVModel = new WeakReference<EVTaskList>(evModel);
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

            if (evModel instanceof EVTaskListRollup
                    && parameters.containsKey(MERGED_PARAM)) {
                evModel = new EVTaskListMerged(evModel, false,
                        settings.shouldMergePreserveLeaves(), null);
            }

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
        String path = (String) env.get("PATH_INFO");
        String newBase = getRequestURLBase() + path;

        // need to escape the result for quoted printable display. Since it's
        // a URL, the only unsafe character it might contain is the '=' sign.
        newBase = StringUtils.findAndReplace(newBase, "=", "=3D");

        // replace the generic host/path information in the URL with the
        // specific information we've built.
        line = StringUtils.findAndReplace(line, "http://localhost:2468/++", newBase);

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

        writeContentDispositionHeader(".csv");
        out.print("Content-type: text/plain\r\n\r\n");

        boolean simpleCsv = Settings.getBool("ev.simpleCsvOutput", false);

        List columns = null;
        if (simpleCsv) {
            columns = createSimpleCsvColumns();
        } else {
            columns = createCsvColumns();
            writeCsvColumnHeaders(columns);
        }

        TreeTableModel merged = evModel.getMergedModel(false, false, null);
        EVTask root = (EVTask) merged.getRoot();
        prepCsvColumns(columns, root, root, 1);
        writeCsvRows(columns, root, 1);
    }

    private void writeContentDispositionHeader(String filenameSuffix)
            throws IOException {
        DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        String displayName = EVTaskList.getDisplayName(taskListName);
        String filename = FileUtils.makeSafe(displayName) + "-"
                + fmt.format(new Date()) + filenameSuffix;

        out.flush();
        outStream.write(("Content-Disposition: attachment; filename=\""
                + filename + "\"\r\n").getBytes(HTTPUtils.DEFAULT_CHARSET));
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
                out.print(cleanupPercentComplete(node.getPercentCompleteText()));
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
                out.print(cleanupPercentComplete(node.getPercentCompleteText()));
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
//        public Date nullToNA(Date d) {
//            return (d == null ? EVSchedule.NEVER : d);
//        }
        public abstract Date getNodeDate(EVTask node);
    }

    /** Generate an XML document in Microsoft Project mspdi format.
     */
    public void writeMSProjXml() throws IOException {
        MSProjectXmlWriter writer = new MSProjectXmlWriter();

        EVTaskFilter taskFilter = settings.getEffectiveFilter(evModel);
        EVTaskListMerged mergedModel = new EVTaskListMerged(evModel, false,
            true, taskFilter);
        writer.setTaskList(mergedModel);

        String taskListID = evModel.getID();
        String metadataPrefix = "/Task-Schedule-MS-Project/" + taskListID;
        writer.setMetadata(getDataRepository().getSubcontext(metadataPrefix));

        if (parameters.containsKey("dateStyle"))
            writer.setDateStyle(getParameter("dateStyle"));
        if (parameters.containsKey("showSaveAs"))
            writeContentDispositionHeader(".xml");
        outStream.write("Content-type: application/xml\r\n\r\n"
                .getBytes(HTTPUtils.DEFAULT_CHARSET));

        writer.write(outStream);
        outStream.flush();
    }

    // handle the storage and retrieval of customization settings.

    public void storeCustomizationSettings() throws IOException {
        out.println("<html><head><script>");
        if (parameters.containsKey("OK")) {
            settings.store(CUSTOMIZE_HIDE_BASELINE, true);
            settings.store(CUSTOMIZE_HIDE_PLAN_LINE, true);
            settings.store(CUSTOMIZE_HIDE_REPLAN_LINE, true);
            settings.store(CUSTOMIZE_HIDE_FORECAST_LINE, true);
            settings.store(CUSTOMIZE_HIDE_NAMES, true);
            settings.store(CUSTOMIZE_LABEL_FILTER, false);
            saveChartOrderingPreference();
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
        String namespace = (isSnippet ? "$$$_" : "");
        String taskListDisplayName = EVTaskList.cleanupName(taskListName);
        String taskListHTML = HTMLUtils.escapeEntities(taskListDisplayName);
        String title = resources.format("Report.Title_FMT", taskListHTML);

        EVTaskFilter taskFilter = settings.getEffectiveFilter(evModel);
        EVSchedule s = getEvSchedule(taskFilter);

        EVTaskDataWriter taskDataWriter = getEffectiveTaskDataWriter();

        StringBuffer header = new StringBuffer(HEADER_HTML);
        StringUtils.findAndReplace(header, TITLE_VAR, title);
        if (taskFilter != null && isSnippet == false)
            header.append(FILTER_HEADER_HTML);
        out.print(header);
        out.print(taskDataWriter.getHeaderItems());
        out.print("</head><body>");

        out.print(isSnippet ? "<h2>" : "<h1>");
        out.print(title);
        if (!exportingToExcel()) {
            interpOutLink(SHOW_WEEK_LINK, EVReportSettings.PURPOSE_WEEK);
            interpOutLink(SHOW_MONTH_LINK, EVReportSettings.PURPOSE_WEEK);
            printAlternateViewLinks();
            interpOutLink(SHOW_CHARTS_LINK, EVReportSettings.PURPOSE_OTHER);
        }
        printCustomizationLink();
        out.print(isSnippet ? "</h2>" : "</h1>");

        if (!isSnippet)
            printFilterInfo(out, taskFilter, settings, isExporting(),
                exportingToExcel());

        if (!exportingToExcel()) {
            writeImageHtml(taskFilter != null);
            out.print("<div style='clear:both'></div>");
            writeCharts(evModel, s, taskFilter,
                settings.getBool(CUSTOMIZE_HIDE_NAMES), 350, 300,
                ChartListPurpose.ReportMain, null, null);
            out.print("<div style='clear:both'>&nbsp;</div>");
            out.print(HTMLTreeTableWriter.TREE_ICON_HEADER);
        }

        EVMetrics m = s.getMetrics();

        printScheduleErrors(out, m.getErrors());

        boolean hidePlan = settings.getBool(CUSTOMIZE_HIDE_PLAN_LINE);
        boolean hideReplan = settings.getBool(CUSTOMIZE_HIDE_REPLAN_LINE);
        boolean hideForecast = settings.getBool(CUSTOMIZE_HIDE_FORECAST_LINE);
        out.print("<table name='STATS'>");
        for (int i = 0;   i < m.getRowCount();   i++)
            writeMetric(m, i, hidePlan, hideReplan, hideForecast);
        out.print("</table>");

        out.print("<h2><a name='" + namespace + "tasks'></a>"
                + getResource("TaskList.Title"));
        printTaskStyleLinks(taskDataWriter, namespace);
        out.print("</h2>\n");
        taskDataWriter.write(out, evModel, taskFilter, settings, namespace);

        out.print("<h2>"+getResource("Schedule.Title")+"</h2>\n");
        writeScheduleTable(s);

        if (isExporting() && !isSnippet)
            writeExportFooter(out);

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

    protected static void printScheduleErrors(PrintWriter out, Map errors) {
        if (errors != null && errors.size() > 0) {
            out.print("<table border><tr>");
            out.print("<td style='text-align: left; background-color: #ff5050'><h2>");
            out.print(getResource("Report.Errors_Heading"));
            out.print("</h2><b>");
            out.print(getResource("Error_Dialog.Head"));
            out.print("<ul>");
            Iterator i = errors.keySet().iterator();
            while (i.hasNext()) {
                String message = (String) i.next();
                String helpSet = null;
                String helpTopic = null;
                String helpText = null;
                Matcher m = ERROR_HELP_PATTERN.matcher(message);
                if (m.matches()) {
                    message = m.group(1);
                    helpSet = m.group(2);
                    helpTopic = m.group(3);
                    helpText = m.group(4).trim();
                }
                out.print("\n<li>" + HTMLUtils.escapeEntities(message));
                if (helpTopic != null) {
                    out.print(" <i>(<a target='_blank' href='/" + helpSet
                            + "/frame.html?" + helpTopic + "'>");
                    out.print(HTMLUtils.escapeEntities(helpText));
                    out.print("</a>)</i>");
                }
                out.print("</li>");
            }
            out.print("\n</ul>");
            if (!EVMetrics.isWarningOnly(errors))
                out.print(getResource("Error_Dialog.Foot"));
            out.print("</b></td></tr></table>\n");
        }
    }

    private static final Pattern ERROR_HELP_PATTERN = Pattern
            .compile("(.+)\\n#(\\S+)/(\\S+) (.+)");


    protected static void writeExportFooter(PrintWriter out) {
        out.print("<p><i>");
        out.print(HTMLUtils.escapeEntities(resources.format(
            "Report.Export_Date_Footer_FMT", new Date())));
        out.print("</i></p>\n");
    }

    private static List<EVTaskDataWriter> taskDataWriters = null;
    private static List<EVTaskDataWriter> getTaskDataWriters() {
        if (taskDataWriters == null) {
            List<EVTaskDataWriter> result = new ArrayList();
            result.add(new TreeViewTaskDataWriter());
            result.add(new FlatViewTaskDataWriter());

            TreeMap<String, EVTaskDataWriter> customWriters = new TreeMap();
            for (Object extObj : ExtensionManager.getExecutableExtensions(
                "ev-task-writer", null)) {
                if (extObj instanceof EVTaskDataWriter) {
                    EVTaskDataWriter custom = (EVTaskDataWriter) extObj;
                    customWriters.put(custom.getID(), custom);
                }
            }
            result.addAll(customWriters.values());

            taskDataWriters = Collections.unmodifiableList(result);
        }
        return taskDataWriters;
    }

    private EVTaskDataWriter getEffectiveTaskDataWriter() {
        List<EVTaskDataWriter> writers = getTaskDataWriters();

        // force flat view in export to excel mode
        if (exportingToExcel())
            return writers.get(1);

        // look for a parameter indicating which writer to use
        String paramStyle = getParameter(TASK_STYLE_PARAM);
        if (paramStyle != null) {
            for (EVTaskDataWriter w : writers)
                if (paramStyle.equals(w.getID()))
                    return w;
        }

        // return the default (tree) writer
        return writers.get(0);
    }


    private void writeImageHtml(boolean showCombined) throws IOException {
        boolean exporting = isExporting();
        Map realParameters = this.parameters;
        Map imgParams = new HashMap();
        this.parameters = imgParams;

        imgParams.put("initGradColor", "#bebdff");
        imgParams.put("finalGradColor", "#bebdff");
        imgParams.put("html", "t");
        imgParams.put("noBorder", "t");
        if (exporting)
            imgParams.put("EXPORT", realParameters.get("EXPORT"));

        out.write("<pre>");
        if (showCombined) {
            imgParams.put("width", "720");
            if (!exporting)
                imgParams.put("href",
                    getChartDrillDownUrl("pdash.ev.cumCombinedChart"));
            writeCombinedChart();

        } else {
            if (!exporting)
                imgParams.put("href",
                    getChartDrillDownUrl("pdash.ev.cumValueChart"));
            writeValueChart();

            imgParams.put("width", "320");
            imgParams.put("hideLegend", "t");
            imgParams.remove("title");
            if (!exporting)
                imgParams.put("href",
                    getChartDrillDownUrl("pdash.ev.cumDirectTimeChart"));
            writeTimeChart();
        }
        out.write("</pre>\n");

        this.parameters = realParameters;
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
        interpOutLink(html, purpose, resources);
    }
    private void interpOutLink(String html, int purpose, Resources resources) {
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





    public static void printFilterInfo(PrintWriter out, EVTaskFilter filter,
            EVReportSettings settings, boolean exporting, boolean textOnly) {

        String labelFilter = (filter == null ? null : filter
                .getAttribute(EVLabelFilter.LABEL_FILTER_ATTR));
        String pathFilter = (filter == null ? null : filter
                .getAttribute(EVHierarchicalFilter.HIER_FILTER_ATTR));
        UserFilter groupFilter = (settings == null ? null : settings
                .getUserGroupFilter());
        if (labelFilter == null && pathFilter == null && groupFilter == null)
            return;

        out.print("<h2 style='position:relative; left:-10px'>");

        if (labelFilter != null) {
            if (!textOnly)
                out.print("<img border=0 src='/Images/filter.png' "
                        + "style='margin: 0px 2px 0px 10px; position:relative; top:3px' "
                        + "width='16' height='23' title=\"");
            out.print(resources.getHTML("Report.Filter_Tooltip"));
            out.print(textOnly ? " - " : "\">");
            out.print(HTMLUtils.escapeEntities(labelFilter));
        }

        if (pathFilter != null) {
            if (!textOnly)
                out.print("<img border=0 src='/Images/hier.png' "
                        + "style='margin: 0px 2px 0px 10px; position:relative; top:3px' "
                        + "width='16' height='23' title=\"");
            out.print(resources.getHTML("Report.Filter_Tooltip"));
            out.print(textOnly ? " - " : "\">");
            out.print(HTMLUtils.escapeEntities(pathFilter));
        }

        if (groupFilter != null) {
            boolean isPrivacyViolation = groupFilter instanceof UserGroupPrivacyBlock;

            // display an icon to represent this group filter
            if (!textOnly) {
                boolean showGroupHyperlink = !exporting
                        && (settings.getParameters().containsKey(
                            EVReportSettings.GROUP_FILTER_PARAM) //
                        || settings.getParameters().containsKey(
                            EVReportSettings.GROUP_FILTER_AUTO_PARAM));
                if (showGroupHyperlink)
                    out.print("<a href='../team/setup/selectGroupFilter'>");
                out.print("<img border=0 src='/Images/userGroup");
                if (isPrivacyViolation)
                    out.print("Privacy");
                else if (groupFilter instanceof UserGroupMember)
                    out.print("Member");
                out.print(".png' ");
                if (isPrivacyViolation)
                    out.print("title='Group filter blocked to protect data privacy' ");
                else if (showGroupHyperlink)
                    out.print("title='Filter to group' ");
                out.print("style='margin: 0px 2px 0px 10px; position:relative; top:3px' "
                        + "width='23' height='23'>");
                if (showGroupHyperlink)
                    out.print("</a>");
            }

            // display the name of the filter
            if (isPrivacyViolation)
                out.print("<span style='color:#888; font-weight:normal; text-decoration:line-through'>");
            out.print(HTMLUtils.escapeEntities(groupFilter.toString()));
            if (isPrivacyViolation)
                out.print("</span>");
        }

        out.println("</h2>");
    }


    private void printAlternateViewLinks() {
        List<Element> altViews = ExtensionManager
                .getXmlConfigurationElements("ev-report-view");
        if (altViews != null)
            for (Element view : altViews)
                printAlternateViewLink(view);
    }

    private void printAlternateViewLink(Element view) {
        // Get the URI of the report
        String uri = view.getAttribute("href");
        if (uri.startsWith("/"))
            uri = uri.substring(1);

        // Get the resource bundle for messages
        String resourcePrefix = view.getAttribute("resources");
        Resources res = Resources.getDashBundle(resourcePrefix);

        // Construct the link HTML and print it
        String linkHtml = StringUtils.findAndReplace(SHOW_ALT_LINK, "[URI]",
            uri);
        interpOutLink(linkHtml, EVReportSettings.PURPOSE_OTHER, res);
    }


    private void printCustomizationLink() {
        if (!parameters.containsKey("EXPORT")) {
            out.print("<span " + HEADER_LINK_STYLE + ">"
                    + "<span class='doNotPrint'><a href='");
            out.print(settings.getEffectivePrefix());
            out.print("ev-customize.shtm?tlid=");
            out.print(HTMLUtils.urlEncode(evModel.getID()));
            if ((evModel instanceof EVTaskListRollup))
                out.print("&isRollup");
            if (!parameters.containsKey(EVReportSettings.LABEL_FILTER_PARAM)
                    && EVLabelFilter.taskListContainsLabelData(evModel,
                            getDataRepository()))
                out.print("&showLabelFilter");
            if (evModel.getMetadata(EVMetadata.Baseline.SNAPSHOT_ID) != null)
                out.print("&hasBaseline");
            out.print("' target='customize' onClick='PdashEV.openCustomizeWindow();'>");
            out.print(HTMLUtils.escapeEntities(resources
                    .getDlgString("Customize")));
            out.print("</a></span></span>");
        }
    }


    private void printTaskStyleLinks(EVTaskDataWriter taskDataWriter, String namespace) {
        if (exportingToExcel())
            return;

        StringBuffer href = new StringBuffer();
        String uri = (String) env.get(CMSSnippetEnvironment.CURRENT_FRAME_URI);
        if (uri == null) {
            href.append("ev.class");
        } else {
            href.append(uri);
            Matcher m = TASK_STYLE_PARAM_PATTERN.matcher(uri);
            if (m.find())
                HTMLUtils.removeParam(href, m.group());
        }
        HTMLUtils.appendQuery(href, settings
            .getQueryString(EVReportSettings.PURPOSE_TASK_STYLE));
        href.append(href.indexOf("?") == -1 ? '?' : '&');
        href.append(namespace).append(TASK_STYLE_PARAM).append('=');

        for (EVTaskDataWriter w : getTaskDataWriters()) {
            if (w == taskDataWriter)
                continue;

            out.print("<span " + HEADER_LINK_STYLE + ">"
                    + "<span class='doNotPrint'>"
                    + "<a id=\"" + namespace + "taskstyle" + w.getID()
                    + "\" href=\"" + href + w.getID()
                    + "#" + namespace + "tasks\">");
            out.print(HTMLUtils.escapeEntities(w.getDisplayName()));
            out.print("</a></span></span>");
        }
    }
    private Pattern TASK_STYLE_PARAM_PATTERN = Pattern.compile("[^&?]*"
            + TASK_STYLE_PARAM);


    protected void writeMetric(EVMetrics m, int i, boolean hidePlan,
            boolean hideReplan, boolean hideForecast) {
        String metricID = (String) m.getValueAt(i, EVMetrics.METRIC_ID);
        if (hidePlan && (metricID.indexOf("Plan_") != -1)) return;
        if (hideReplan && (metricID.indexOf("Replan_") != -1)) return;
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
    static final String SIMPLE_HEADER_HTML =
        HTMLUtils.HTML_TRANSITIONAL_DOCTYPE +
        "<html><head><title>%title%</title>\n" +
        "<link rel=stylesheet type='text/css' href='/style.css'>\n" +
        "<script type='text/javascript' src='/lib/overlib.js'></script>\n" +
        "<script type='text/javascript' src='/reports/ev.js'></script>\n" +
        "<link rel=stylesheet type='text/css' href='/reports/ev.css'>\n";
    static final String HEADER_HTML =
        SIMPLE_HEADER_HTML +
        HTMLTreeTableWriter.TREE_HEADER_ITEMS +
        REDUNDANT_EXCEL_HEADER +
        POPUP_HEADER +
        SORTTABLE_HEADER +
        SORTTREE_HEADER;
    public static final String FILTER_HEADER_HTML =
        "<link rel=stylesheet type='text/css' href='/reports/filter-style.css'>\n";
    static final String HEADER_LINK_STYLE = " style='font-size: medium; " +
        "font-style: italic; font-weight: normal; margin-left: 0.5cm' ";
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
    static final String SHOW_MONTH_LINK = "<span " + HEADER_LINK_STYLE + ">"
            + "<span class='doNotPrint'><a href='month???'>"
            + "${Report.Show_Monthly_View}</a></span></span>";
    static final String SHOW_ALT_LINK = "<span " + HEADER_LINK_STYLE + ">"
            + "<span class='doNotPrint'><a href='../[URI]???'>"
            + "${Link_Text}</a></span></span>";
    static final String SHOW_CHARTS_LINK = "<span " + HEADER_LINK_STYLE + ">"
            + "<span class='doNotPrint'><a href='ev.class??&charts'>"
            + "${Report.Charts_Link}</a></span></span>";
    static final String EXCEL_TIME_TD = "<td class='timeFmt'>";


    private static class FlatViewTaskDataWriter implements EVTaskDataWriter {
        public String getID() { return "flat"; }
        public String getDisplayName() {
            return resources.getString("Report.Flat_View"); }
        public String getHeaderItems() { return ""; }
        public void write(Writer out, EVTaskList taskList, EVTaskFilter filter,
                EVReportSettings settings, String namespace) throws IOException {
            writeTaskTable(out, taskList, filter, settings, namespace);
        }
    }

    private static void writeTaskTable(Writer out, EVTaskList taskList,
            EVTaskFilter filter, EVReportSettings settings, String namespace)
            throws IOException {
        HTMLTableWriter writer = new HTMLTableWriter();
        boolean showTimingIcons = taskList instanceof EVTaskListData
                && !settings.isExporting();
        TableModel table = customizeTaskTableWriter(writer, taskList, filter,
                settings, showTimingIcons);
        writer.setTableAttributes("class='sortable' id='" + namespace
                + "task' border='1'");
        writer.writeTable(out, table);
    }

    private static class TreeViewTaskDataWriter implements EVTaskDataWriter {
        public String getID() { return "tree"; }
        public String getDisplayName() {
            return resources.getString("Report.Tree_View"); }
        public String getHeaderItems() { return ""; }
        public void write(Writer out, EVTaskList taskList, EVTaskFilter filter,
                EVReportSettings settings, String namespace) throws IOException {
            writeTaskTree(out, taskList, filter, settings, namespace);
        }
    }

    private static void writeTaskTree(Writer out, EVTaskList taskList,
            EVTaskFilter filter, EVReportSettings settings, String namespace)
            throws IOException {
        TreeTableModel tree = taskList.getMergedModel(true,
            settings.shouldMergePreserveLeaves(), filter);
        HTMLTreeTableWriter writer = new HTMLTreeTableWriter();
        customizeTaskTableWriter(writer, taskList, null, settings, false);
        writer.setTreeName(namespace + "t");
        writer.setExpandAllTooltip(resources.getHTML("Report.Expand_All_Tooltip"));
        writer.setTableAttributes("class='needsTreeSortLinks' id='" + namespace
                + "task' border='1'");
        writer.setShowDepth(Settings.getInt("ev.showHierarchicalDepth", 3) - 1);
        writer.writeTree(out, tree);
    }

    void writeScheduleTable(EVSchedule s) throws IOException {
        HTMLTableWriter writer = new HTMLTableWriter();
        customizeTableWriter(writer, s, s.getColumnTooltips());
        setupRenderers(writer, EVSchedule.COLUMN_FORMATS);
        writer.setSkipColumn(EVSchedule.NOTES_COLUMN, true);
        writer.setTableName("SCHEDULE");
        writer.writeTable(out, s);
    }

    private static TableModel customizeTaskTableWriter(HTMLTableWriter writer,
            EVTaskList taskList, EVTaskFilter filter, EVReportSettings settings,
            boolean showTimingIcons) {
        TableModel table = taskList.getSimpleTableModel(filter);
        boolean hidePlan = settings.getBool(CUSTOMIZE_HIDE_PLAN_LINE);
        boolean hideReplan = settings.getBool(CUSTOMIZE_HIDE_REPLAN_LINE);
        boolean hideForecast = settings.getBool(CUSTOMIZE_HIDE_FORECAST_LINE);
        boolean hideNames = settings.getBool(CUSTOMIZE_HIDE_NAMES);
        customizeTableWriter(writer, table, EVTaskList.toolTips);
        writer.setTableName("TASK");
        writer.setSkipColumn(EVTaskList.PLAN_CUM_TIME_COLUMN, true);
        writer.setSkipColumn(EVTaskList.PLAN_CUM_VALUE_COLUMN, true);
        writer.setSkipColumn(EVTaskList.NOTES_COLUMN, true);
        setupTaskTableRenderers(writer, showTimingIcons, settings
                .exportingToExcel(), hideNames, taskList.getNodeTypeSpecs());
        if (!(taskList instanceof EVTaskListRollup) || hideNames)
            writer.setSkipColumn(EVTaskList.ASSIGNED_TO_COLUMN, true);
        if (hidePlan)
            writer.setSkipColumn(EVTaskList.PLAN_DATE_COLUMN, true);
        if (hideReplan)
            writer.setSkipColumn(EVTaskList.REPLAN_DATE_COLUMN, true);
        if (hideForecast)
            writer.setSkipColumn(EVTaskList.FORECAST_DATE_COLUMN, true);
        return table;
    }


    /** Install renderers that are appropriate for a task table. */
    static HTMLTableWriter setupTaskTableRenderers(HTMLTableWriter writer,
            boolean showTimingIcons, boolean exportingToExcel, boolean hideNames,
            Set nodeTypeSpecs) {
        setupRenderers(writer, EVTaskList.COLUMN_FORMATS);
        writer.setCellRenderer(EVTaskList.MILESTONE_COLUMN,
                new MilestoneCellRenderer());
        writer.setCellRenderer(EVTaskList.DEPENDENCIES_COLUMN,
                new DependencyCellRenderer(exportingToExcel, hideNames));
        if (showTimingIcons)
            writer.setCellRenderer(EVTaskList.TASK_COLUMN,
                    new TaskNameWithTimingIconRenderer());
        if (nodeTypeSpecs != null && !nodeTypeSpecs.isEmpty())
            writer.setCellRenderer(EVTaskList.NODE_TYPE_COLUMN,
                    new NodeTypeCellRenderer(nodeTypeSpecs));
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

    private static void customizeTableWriter(HTMLTableWriter writer,
            TableModel t, String[] toolTips) {
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
        boolean hideReplan = settings.getBool(CUSTOMIZE_HIDE_REPLAN_LINE);
        boolean hideForecast = settings.getBool(CUSTOMIZE_HIDE_FORECAST_LINE);
        if (hidePlan || hideReplan || hideForecast)
            xydata = new XYDatasetFilter(xydata)
                .setSeriesHidden("Plan", hidePlan)
                .setSeriesHidden("Replan", hideReplan)
                .setSeriesHidden("Forecast", hideForecast)
                .setSeriesHidden("Optimized_Forecast", hideForecast);

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
        boolean hideReplan = settings.getBool(CUSTOMIZE_HIDE_REPLAN_LINE);
        boolean hideForecast = settings.getBool(CUSTOMIZE_HIDE_FORECAST_LINE);
        if (hidePlan || hideReplan || hideForecast)
            xydata = new XYDatasetFilter(xydata)
                .setSeriesHidden("Plan", hidePlan)
                .setSeriesHidden("Replan", hideReplan)
                .setSeriesHidden("Forecast", hideForecast)
                .setSeriesHidden("Optimized_Forecast", hideForecast);

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
        if (hidePlan)
            xydata = new XYDatasetFilter(xydata)
                .setSeriesHidden("Plan_Value", hidePlan);

        // Alter the appearance of the chart.
        maybeWriteParam("title", "Cost & Schedule");

        super.writeContents();
    }

    /** Create a time series chart. */
    public JFreeChart createChart() {
        JFreeChart chart = AbstractEVTimeSeriesChart.createEVReportChart(xydata);
        if (parameters.get("hideLegend") == null)
            chart.getLegend().setPosition(RectangleEdge.RIGHT);
        return chart;
    }

    protected void writeChartsPage() {
        String taskListDisplayName = EVTaskList.cleanupName(taskListName);
        String taskListHTML = HTMLUtils.escapeEntities(taskListDisplayName);
        String title = resources.format("Report.Charts_Title_FMT", taskListHTML);

        EVTaskFilter taskFilter = settings.getEffectiveFilter(evModel);
        boolean hideNames = settings.getBool(CUSTOMIZE_HIDE_NAMES);

        StringBuffer header = new StringBuffer(SIMPLE_HEADER_HTML);
        StringUtils.findAndReplace(header, TITLE_VAR, title);
        if (taskFilter != null)
            header.append(FILTER_HEADER_HTML);
        out.print(header);
        out.print("</head><body>");
        out.print("<h1>");
        out.print(title);
        out.print("</h1>");
        printFilterInfo(out, taskFilter, settings, false, false);

        EVSchedule s = getEvSchedule(taskFilter);

        String singleChartId = getParameter(SINGLE_CHART_PARAM);
        if (singleChartId == null)
            writeChartsGalleryPage(taskFilter, hideNames, s);
        else
            writeSingleChartPage(taskFilter, hideNames, s, singleChartId);

        out.print("</body></html>\n");
    }


    private void writeChartsGalleryPage(EVTaskFilter taskFilter,
            boolean hideNames, EVSchedule s) {

        if (!isExporting()) {
            out.write("<p class='doNotPrint'><i>"
                    + resources.getHTML("Report.Charts_More_Detail")
                    + "</i></p>");
        }

        // display all of the charts that are relevant to this task list.
        writeCharts(evModel, s, taskFilter, hideNames, 400, 300,
            ChartListPurpose.ReportAll, null, null);

        // add space to the bottom of the page so the chart tooltips don't
        // get truncated.
        out.print("<div style='height: 1in; clear:both'>&nbsp;</div>\n");
    }

    protected void writeSingleChartPage(EVTaskFilter taskFilter,
            boolean hideNames, EVSchedule s, String singleChartId) {

        out.write("<div class='singleChartWrapper'>\n");

        // display the single chart that was requested via query parameters
        out.write("<div class='singleChartHolder'>\n");
        Map<String, String> chartHelp = new HashMap<String, String>();
        List<ChartItem> allCharts = writeCharts(evModel, s, taskFilter,
            hideNames, 800, 500, ChartListPurpose.ReportAll, singleChartId,
            chartHelp);
        out.write("</div>\n"); // singleChartHolder

        // display a drop-down selector that can be used to select a
        // different chart to display
        out.write("<div class='singleChartSelector'><form>\n<b>");
        out.write(resources.getHTML("Report.Select_Chart"));
        out.write("</b>&nbsp;<select id='chartSelector' name='chartSelector' "
                + "onchange='PdashEV.selectSingleChart(this)'>\n");
        out.write("<option value='ALL'>"
                + resources.getHTML("Report.Show_Gallery") + "</option>\n");
        out.write("<option value=''> </option>\n");
        for (ChartItem chart : allCharts) {
            out.write("<option value='");
            String chartId = getChartId(chart);
            out.write(HTMLUtils.escapeEntities(chartId));
            if (chartId.equals(singleChartId))
                out.write("' selected='selected");
            out.write("'>");
            out.write(HTMLUtils.escapeEntities(chart.name));
            out.write("</option>\n");
        }
        out.write("</select></form></div>\n"); // singleChartSelector

        out.write("</div>"); // singleChartWrapper

        // write out the help topic for the current chart, if one is found.
        String chartHelpUri = chartHelp.get(singleChartId);
        if (chartHelpUri != null) {
            try {
                String helpContent = getRequestAsString(chartHelpUri);
                helpContent = fixChartHelpContent(helpContent,
                    chartHelpUri, chartHelp);

                out.write("<div style='clear:both'></div>\n");
                out.write("<div class='singleChartHelp'>\n");
                out.write(helpContent);
                out.write("</div>"); // singleChartHelp
            } catch (Exception e) {
            }
        }
    }

    private String fixChartHelpContent(String helpContent,
            String helpBaseUri, Map<String, String> chartHelp) {

        // discard headers and footers from the help content
        int cutStart = helpContent.indexOf("</h1>");
        if (cutStart != -1)
            helpContent = helpContent.substring(cutStart + 5);
        int cutEnd = helpContent.lastIndexOf("</body");
        if (cutEnd != -1)
            helpContent = helpContent.substring(0, cutEnd);

        // create a map of the chart help topics
        Map<String, String> chartUrls = new HashMap<String, String>();
        for (Map.Entry<String, String> e : chartHelp.entrySet()) {
            String chartId = e.getKey();
            String chartUrl = getChartDrillDownUrl(chartId);
            String helpUri = e.getValue();
            String helpName = hrefFileName(helpUri);
            chartUrls.put(helpName, chartUrl);
        }

        // find and fix all the hrefs in this help topic:
        //   * If any hrefs point to the help topic for a different chart,
        //     rewrite the href so it actually loads the "drill-down page"
        //     for that chart instead.
        //   * For links that point to some non-chart help topic, rewrite the
        //     href to be absolute (so the help-relative URI won't break)

        StringBuilder html = new StringBuilder(helpContent);
        int pos = 0;
        while (true) {
            // find the next href in the document.
            pos = html.indexOf("href=", pos);
            if (pos == -1)
                break; // no more hrefs to fix

            pos += 6;
            int beg = pos;  // the first character of the href value itself
            char delim = html.charAt(beg-1);
            int end = html.indexOf(String.valueOf(delim), beg);
            if (end == -1)
                continue; // invalid href syntax.  Skip to the next one.

            // extract the href value
            String oneHref = html.substring(beg, end);
            // extract the final portion of the path name
            String oneName = hrefFileName(oneHref);
            // see if that name refers to one of the charts we can display
            String chartUrl = chartUrls.get(oneName);
            if (chartUrl != null) {
                // replace the href with a chart drill-down URL
                html.replace(beg, end, chartUrl);
                pos = beg + chartUrl.length();
            } else {
                try {
                    // make the URL absolute, and set a "target" attribute
                    // so it will open in another window.
                    URI base = new URI(helpBaseUri);
                    URI target = base.resolve(oneHref);
                    String newUri = target.toString();
                    html.replace(beg, end, newUri);
                    html.insert(beg-6, "target='evHelp' ");
                    pos = beg + newUri.length() + 16;
                } catch (Exception e) {
                    // problems resolving the URI?  Turn the link into an
                    // anchor so it can't be clicked on anymore.
                    html.replace(beg-6, beg-2, "name");
                }
            }
        }

        return html.toString();
    }

    private String hrefFileName(String href) {
        int slashPos = href.lastIndexOf('/');
        return href.substring(slashPos + 1);
    }


    protected List<ChartItem> writeCharts(EVTaskList evModel,
            EVSchedule schedule, EVTaskFilter filter, boolean hideNames,
            int width, int height, ChartListPurpose p,
            String singleChartId, Map<String, String> chartHelpMap) {
        DashboardContext ctx = getDashboardContext();
        Object exportMarker = parameters.get("EXPORT");
        boolean filterInEffect = (filter != null);
        boolean isRollup = (evModel instanceof EVTaskListRollup);
        List<ChartItem> chartList = TaskScheduleChartUtil.getChartsForTaskList(
            evModel.getID(), getDataRepository(), filterInEffect, isRollup,
            hideNames, p);

        for (Iterator i = chartList.iterator(); i.hasNext();) {
            ChartItem chart = (ChartItem) i.next();
            if (chart == null) {
                i.remove();
                continue;
            }
            try {
                SnippetWidget w = chart.snip.getWidget("view", null);
                if (w instanceof HtmlEvChart) {
                    String chartId = getChartId(chart);
                    if (chartHelpMap != null && w instanceof HelpAwareEvChart) {
                        String helpUri = ((HelpAwareEvChart) w).getHelpUri();
                        if (StringUtils.hasValue(helpUri))
                            chartHelpMap.put(chartId, helpUri);
                    }

                    if (singleChartId != null && !singleChartId.equals(chartId))
                        continue;

                    Map environment = TaskScheduleChartUtil.getEnvironment(
                        evModel, schedule, filter, chart.snip, ctx);
                    Map params = TaskScheduleChartUtil
                            .getParameters(chart.settings);
                    params.put("title", chart.name);
                    params.put("width", Integer.toString(width));
                    params.put("height", Integer.toString(height));
                    params.put("noBorder", "t");
                    if (hideNames)
                        params.put(CUSTOMIZE_HIDE_NAMES, "t");
                    if (exportMarker != null)
                        params.put("EXPORT", exportMarker);
                    else if (singleChartId == null)
                        params.put("href", getChartDrillDownUrl(chartId));

                    // write the chart to an in-memory buffer. If any errors
                    // occur, we will fall out to the exception handler.
                    StringWriter buf = new StringWriter();
                    ((HtmlEvChart) w).writeChartAsHtml(buf, environment, params);
                    // The generation of the chart was successful.  Write out
                    // the chart, surrounded by a DIV to control layout.
                    out.write("<div class='evChartItem' style='width:" + width
                            + "px; height:" + height + "px'>");
                    out.write(buf.toString());
                    out.write("</div>");
                } else {
                    i.remove();
                }
                out.write(" ");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Unexpected error when displaying "
                        + "EV snippet widget with id '" + chart.snip.getId()
                        + "'", e);
            }
        }
        return chartList;
    }

    private String getChartDrillDownUrl(String chartId) {
        String result = "ev.class"
            + settings.getQueryString(EVReportSettings.PURPOSE_OTHER);
        result = HTMLUtils.appendQuery(result, CHARTS_PARAM);
        if (chartId != null)
            result = HTMLUtils.appendQuery(result, SINGLE_CHART_PARAM, chartId);
        return result;
    }

    private void writeChartOptions() {
        String taskListId = getParameter("tlid");
        boolean isRollup = parameters.containsKey("isRollup");
        List<ChartItem> chartList = TaskScheduleChartUtil.getChartsForTaskList(
            taskListId, getDataRepository(), false, isRollup, false,
            ChartListPurpose.ReportAll);
        Iterator<ChartItem> i = chartList.iterator();

        out.write("<div id='chartOrderBlock'>\n");

        out.write("<div>");
        out.write(resources.getString("Report.Customize.Show_On_Report_HTML"));
        out.write(" <span class='chartOrderTooltip' title='");
        out.write(resources.getHTML("Report.Customize.Charts_Hidden_Tooltip"));
        out.write("'>*</span></div>\n");
        out.write("<div class='chartOrderItem standardChartItem'>");
        out.write(resources.getHTML("Report.Customize.Standard_Chart_Name"));
        out.write("</div>\n");

        out.write("<div id='chartOrderBlockShow'>\n");
        while (i.hasNext()) {
            if (!writeChartOrderingItem(i.next()))
                break;
        }
        out.write("</div>\n"); // chartOrderBlockShow

        out.write("<div>"
                + resources.getString("Report.Customize.Show_On_More_Charts_HTML")
                + "<input type='hidden' name='chartOrder' " + "value='"
                + TaskScheduleChartSettings.SECONDARY_CHART_MARKER
                + "'></div>\n");

        out.write("<div id='chartOrderBlockHide'>\n");
        while (i.hasNext()) {
            writeChartOrderingItem(i.next());
        }
        out.write("</div>\n");  // chartOrderBlockHide

        out.write("</div>\n");  // chartOrderBlock
    }

    private boolean writeChartOrderingItem(ChartItem chart) {
        if (chart == null)
            return false;
        try {
            SnippetWidget w = chart.snip.getWidget("view", null);
            if (w instanceof HtmlEvChart) {
                out.write("<div class='chartOrderItem'>");
                out.write("<input type='hidden' name='chartOrder' value='");
                out.write(HTMLUtils.escapeEntities(getChartId(chart)));
                out.write("'>");
                out.write(HTMLUtils.escapeEntities(chart.name));
                out.write("</div>\n");
            }
        } catch (Exception e) {}
        return true;
    }

    private String getChartId(ChartItem chart) {
        if (chart.settings != null)
            return chart.settings.getSettingsIdentifier();
        else
            return chart.snip.getId();
    }

    private void saveChartOrderingPreference() {
        String taskListID = getParameter("tlid");
        String[] chartOrder = (String[]) parameters.get("chartOrder_ALL");
        if (taskListID != null && chartOrder != null) {
            TaskScheduleChartSettings.savePreferredChartOrdering(taskListID,
                Arrays.asList(chartOrder), getDataRepository());
        }
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
                out.print("<td>" + AbstractEVChart.getNameForSeries(xydata, i)
                    + "</td>");

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

        FastDateFormat f = FastDateFormat.getInstance("yyyy-MM-dd HH:mm");
        for (int series = 0;  series < seriesCount;   series++) {
            int itemCount = xydata.getItemCount(series);
            for (int item = 0;   item < itemCount;   item++) {
                // print the date for the data item.
                out.print("<tr><td>");
                out.print(f.format(new Date
                    (xydata.getX(series,item).longValue())));
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
                d = new Date(xydata.getX(0,0).longValue());
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
            return getDateSortKey(value);
        }

    }
    static String getDateSortKey(Object value) {
        if (value instanceof Date)
            return Long.toString(((Date) value).getTime());
        else
            // The dates we display are typically completion dates.
            // if a date is missing, that implies not yet completed;
            // such dates should sort after all reasonable date values
            return "9999999999999";
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


    static class NodeTypeCellRenderer extends EVCellRenderer {

        List phaseOrder;

        public NodeTypeCellRenderer(Set nodeTypeSpecs) {
            phaseOrder = new ArrayList();
            phaseOrder.add(EVTask.MISSING_NODE_TYPE);
            phaseOrder.add(null);
            phaseOrder.add("");
            phaseOrder.addAll(OrderedListMerger.merge(nodeTypeSpecs));
        }

        protected String getSortKey(Object value) {
            return Integer.toString(phaseOrder.indexOf(value));
        }
    }

    static class MilestoneCellRenderer implements HTMLTableWriter.CellRenderer {

        public String getInnerHtml(Object value, int row, int column) {
            if (value == null)
                return null;
            else
                return HTMLUtils.escapeEntities(value.toString());
        }

        public String getAttributes(Object value, int row, int column) {
            if (value == null)
                return getSortAttribute("99999");

            MilestoneList l = (MilestoneList) value;
            StringBuilder result = new StringBuilder();
            result.append(getSortAttribute(Integer.toString(l
                    .getMinSortOrdinal())));

            if (l.isMissedMilestone()) {
                String errMsg = l.getMissedMilestoneMessage();
                result.append(" class=\"behindSchedule\" title=\"")
                        .append(HTMLUtils.escapeEntities(errMsg)).append("\"");
            }

            return result.toString();
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
            return HTMLUtils.escapeEntities(text);
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

    private class FakeChartData extends AbstractXYDataset {

        private String[] seriesNames;

        public FakeChartData(String[] seriesNames) {
            this.seriesNames = seriesNames;
        }

        @Override
        public int getSeriesCount() {
            return 4;
        }

        @Override
        public String getSeriesKey(int series) {
            return seriesNames[series];
        }

        public int getItemCount(int series) {
            return 2;
        }

        public Number getX(int series, int item) {
            if (item == 0)
                return new Integer(0);
            else
                return new Integer(24 * 60 * 60 * 1000);
        }

        public Number getY(int series, int item) {
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
