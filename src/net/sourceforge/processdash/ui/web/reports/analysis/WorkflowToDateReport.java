// Copyright (C) 2014-2016 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.reports.analysis;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.util.ResultSet;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.log.defects.Defect;
import net.sourceforge.processdash.tool.db.WorkflowHistDataHelper;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.DataPair;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

public class WorkflowToDateReport extends TinyCGIBase {

    private static final String SELF_URI = "workflowToDate";

    private static final Resources resources = Resources
            .getDashBundle("Analysis");

    @Override
    protected void writeContents() throws IOException {
        String projectID = getProjectID();
        String workflowName = getParameter("workflowName");
        WorkflowHistDataHelper hist = new WorkflowHistDataHelper(
            getDataContext(), projectID, workflowName);
        if (projectID != null && StringUtils.hasValue(workflowName))
            writeReportForWorkflow(hist);
        else
            writeWorkflowSelectionScreen(hist);
    }

    private void writeWorkflowSelectionScreen(WorkflowHistDataHelper hist) {
        String title = esc(res("Workflow.Analysis.Title"));

        out.print("<html><head>\n<title>");
        out.print(title);
        out.print("</title>\n");
        out.print(cssLinkHTML());
        out.print("</head>\n");
        out.print("<body><h1>");
        out.print(title);
        out.print("</h1>\n");

        if (parameters.containsKey("wait")) {
            out.print("<p>");
            out.print(esc(res("Workflow.Analysis.Wait_Message")));
            out.print("</p>\n");
            out.print(HTMLUtils.redirectScriptHtml(SELF_URI, 0));

        } else if (hist.getContextProjectID() != null) {
            writeWorkflowSelections(hist);
        }

        out.print("</body></html>\n");
    }

    private void writeWorkflowSelections(WorkflowHistDataHelper hist) {
        // get the list of workflows in this team project.
        List<String> workflowNames = hist.getWorkflowNamesForProject();
        if (workflowNames.isEmpty()) {
            out.write("<p>");
            out.write(esc(res("Workflow.Analysis.No_Workflows_Message")));
            out.write("</p>\n");
            return;
        }

        // display a prompt inviting the user to choose a workflow
        out.write("<p>");
        out.write(esc(res("Workflow.Analysis.Choose_Workflow_Prompt")));
        out.write("</p><ul>");

        // display hyperlinks for each of the workflows
        for (String oneName : workflowNames) {
            out.write("<li><a href=\"");
            out.write(HTMLUtils.appendQuery(SELF_URI, "workflowName", oneName));
            out.write("\">");
            out.write(esc(oneName));
            out.write("</a></li>\n");
        }

        out.write("</ul>\n");
    }

    private void writeReportForWorkflow(WorkflowHistDataHelper hist)
            throws IOException {

        String title = resources.getString("Workflow.Analysis.Title") + " - "
                + hist.getWorkflowName();

        out.print("<html><head><title>");
        out.print(esc(title));
        out.print("</title>\n");
        out.print(cssLinkHTML());
        out.print(HTMLUtils.scriptLinkHtml("/lib/overlib.js"));
        out.print("<style>\n");
        out.print(" .rowLabel { padding-right: 10px }\n");
        out.print(" th.plan, th.act { width: 70px; }\n");
        out.print(" td.plan, td.act { padding-right: 4px; border: 1px solid gray; text-align: right }\n");
        out.print(" #filter.collapsed .filterItem { display: none }\n");
        out.print(" #filter.expanded .filterLink { display: none }\n");
        out.print(" #defects th.plan, #defects td.plan { display: none }\n");
        out.print("</style>\n");
        out.print("<script>\n");
        out.print("    function showFilter() {\n");
        out.print("      document.getElementById('filter').className = 'expanded';\n");
        out.print("    }\n");
        out.print("</script>\n");
        out.print("</head>\n");

        out.print("<body><h1>");
        out.print(esc(title));
        out.print("</h1>\n");

        out.print("<h2>");
        if ("this".equals(parameters.get("project"))) {
            hist.setOnlyForProject(hist.getContextProjectID());
            out.print(esc(resources.format("Workflow.To_Date.One_Project_FMT",
                getPrefix())));
        } else {
            out.print(resources.getHTML("Workflow.To_Date.All_Projects"));
        }
        out.print("</h2>\n");

        writeFilterDiv(hist);

        Map<String, DataPair> sizes = hist.getAddedAndModifiedSizes();
        Map<String, DataPair> timeInPhase = hist.getTimeInPhase();
        Map<String, DataPair>[] defectsByPhase = hist.getDefectsByPhase();

        writeOverallMetrics(sizes, timeInPhase, hist.getPhaseTypes());
        printTable("Size", "Added_&_Modified", sizes, Format.Number, false);
        printTable("Time_in_Phase", null, timeInPhase, Format.Time, true);
        printTimeInPhaseCharts(timeInPhase);

        if (defectsByPhase[1].get(TOTAL_KEY).actual > 0) {
            out.print("<div id=\"defects\">\n");
            printTable("Defects_Injected", null, defectsByPhase[0], Format.Number, true);
            printTable("Defects_Removed", null, defectsByPhase[1], Format.Number, true);
            printDefectsByPhaseCharts(defectsByPhase);
            out.print("</div>\n");
        }

        if (!isExportingToExcel()) {
            out.print("<hr>\n");
            out.print("<a href=\"excel.iqy?fullPage\">");
            out.print(resources.getHTML("Export_to_Excel"));
            out.print("</a>");
        }

        out.print("</body></html>\n");

        if (parameters.containsKey("debug"))
            hist.debugPrintEnactments();
    }

    private String getProjectID() {
        String prefix = getPrefix();
        if (prefix == null)
            return null;

        SaveableData projectIDVal = getDataRepository().getInheritableValue(
            prefix, "Project_ID");
        if (projectIDVal == null)
            return null;

        SimpleData sd = projectIDVal.getSimpleValue();
        return (sd == null ? null : sd.format());
    }

    protected void writeFilterDiv(WorkflowHistDataHelper hist) {
        out.print("<div id='filter' class='doNotPrint collapsed'>\n");
        String filterLabel = resources.getHTML("Workflow.To_Date.Filter.Label");
        out.print("<a href='#' onclick='showFilter(); return false;' "
                + "class='filterLink'>" + filterLabel + "...</a>");
        out.print("<span class='filterItem'>" + filterLabel + ":</span>");
        out.print("<ul class='filterItem'>\n");
        String selfUri = HTMLUtils.appendQuery(SELF_URI, "workflowName",
            hist.getWorkflowName());
        out.print("<li><a href='" + selfUri + "'>"
                + resources.getHTML("Workflow.To_Date.Filter.All_Projects")
                + "</a></li>");
        out.print("<li><a href='" + selfUri + "&project=this'>"
                + resources.getHTML("Workflow.To_Date.Filter.This_Project")
                + "</a></li>");
        out.print("</ul></div>\n");
    }

    private void writeOverallMetrics(Map<String, DataPair> sizes,
            Map<String, DataPair> timeInPhase, Map<String, String> phaseTypes) {
        DataPair totalTime = timeInPhase.get(TOTAL_KEY);

        out.print("<h2>");
        out.print(res("Overall_Metrics"));
        out.print("</h2>\n<table>\n");
        printTableHeader(null, false);

        // print numbers for productivity
        for (Entry<String, DataPair> e : sizes.entrySet()) {
            String metric = e.getKey();
            String label = resources.format("Productivity_Units_FMT", metric);
            DataPair productivity = new DataPair(e.getValue()).multiply(60)
                    .divide(totalTime);
            printTableRow(label, productivity, Format.Number);
        }

        // print total time
        printTableRow(res("Total_Time"), totalTime, Format.Time);

        // print time estimating error
        DataPair timeEst = new DataPair();
        timeEst.actual = (totalTime.actual - totalTime.plan) / totalTime.plan;
        printTableRow(res("Time_Estimating_Error"), timeEst, Format.Percent,
            true, 0);

        // print CPI
        DataPair cpi = new DataPair();
        cpi.actual = totalTime.plan / totalTime.actual;
        printTableRow(res("CPI"), cpi, Format.Number, true, 0);

        // calculate cost of quality
        DataPair appraisalCOQ = new DataPair();
        DataPair failureCOQ = new DataPair();
        for (Entry<String, DataPair> e : timeInPhase.entrySet()) {
            String phaseName = e.getKey();
            String phaseType = phaseTypes.get(phaseName);
            if ("Appraisal".equals(phaseType))
                appraisalCOQ.add(e.getValue());
            else if ("Failure".equals(phaseType))
                failureCOQ.add(e.getValue());
        }
        appraisalCOQ.divide(totalTime);
        failureCOQ.divide(totalTime);
        DataPair totalCOQ = new DataPair(appraisalCOQ).add(failureCOQ);
        DataPair afr = new DataPair(appraisalCOQ).divide(failureCOQ);
        printTableRow(res("%_Appraisal_COQ"), appraisalCOQ, Format.Percent);
        printTableRow(res("%_Failure_COQ"), failureCOQ, Format.Percent);
        printTableRow(res("%_Total_COQ"), totalCOQ, Format.Percent);
        printTableRow(res("Appraisal_to_Failure_Ratio"), afr, Format.Number);

        out.print("</table>\n");
    }

    private void printTable(String titleRes, String subtitleRes,
            Map<String, DataPair> data, Format fmt, boolean showActualPercent) {

        if (data.isEmpty())
            return;

        double percentOf = 0;
        if (showActualPercent) {
            DataPair d = data.get(TOTAL_KEY);
            if (d != null)
                percentOf = d.actual;
        }
        showActualPercent = percentOf > 0;

        out.print("<h2>" + esc(res(titleRes)) + "</h2>\n");
        out.print("<table>\n");
        printTableHeader(subtitleRes, showActualPercent);

        for (Entry<String, DataPair> e : data.entrySet()) {
            printTableRow(e.getKey(), e.getValue(), fmt, false, percentOf);
        }

        out.print("</table>\n");
    }

    private void printTableHeader(String subtitleRes, boolean showActualPct) {
        out.print("<tr><th class='rowLabel'>");
        if (subtitleRes != null)
            out.print(resources.getHTML(subtitleRes));
        out.print("</th><th class='plan'>");
        out.print(resources.getString("Plan"));
        out.print("</th><th class='act'>");
        out.print(resources.getString("Actual"));
        if (showActualPct) {
            out.print("</th><th class='act'>");
            out.print(resources.getString("Actual_%"));
        }
        out.print("</th></tr>\n");
    }

    private void printTableRow(String rowLabel, DataPair values, Format fmt) {
        printTableRow(rowLabel, values, fmt, false, 0);
    }

    private void printTableRow(String rowLabel, DataPair dataValues,
            Format fmt, boolean skipPlan, double showPercentOf) {

        out.print("<tr><td class='rowLabel'>");
        out.print(esc(maybeTranslateRowLabel(rowLabel)));
        out.print("</td><td class='plan'>");
        if (!skipPlan)
            printNumber(dataValues.plan, fmt);
        out.print("</td><td class='act'>");
        printNumber(dataValues.actual, fmt);
        if (showPercentOf > 0) {
            out.print("</td><td class='act'>");
            printNumber(dataValues.actual / showPercentOf, Format.Percent);
        }
        out.print("</td></tr>\n");
    }

    private String maybeTranslateRowLabel(String rowLabel) {
        if (TOTAL_KEY.equals(rowLabel))
            return res("Total");
        else if (Defect.BEFORE_DEVELOPMENT.equals(rowLabel))
            return res("Before_Development");
        else if (Defect.AFTER_DEVELOPMENT.equals(rowLabel))
            return res("After_Development");
        else
            return rowLabel;
    }

    private enum Format {
        Number, Time, Percent
    }

    private void printNumber(double value, Format fmt) {
        if (fmt == Format.Time)
            out.print(FormatUtil.formatTime(value));
        else if (fmt == Format.Percent)
            out.print(FormatUtil.formatPercent(value));
        else
            out.print(FormatUtil.formatNumber(value));
    }


    private void printTimeInPhaseCharts(Map<String, DataPair> timeInPhase)
            throws IOException {
        if (isExportingToExcel())
            return;

        out.print("<p>\n");
        writePhaseChart(true, "Estimated_Time", timeInPhase);
        writePhaseChart(false, "Time", timeInPhase);
        out.print("</p>\n");
    }

    private void printDefectsByPhaseCharts(
            Map<String, DataPair>[] defectsByPhase) throws IOException {
        if (isExportingToExcel())
            return;

        // shuffle the items in the "injected" list so "Before Development" is
        // at the end. This makes the phase colors consistent across pie charts.
        Map<String, DataPair> injected = new LinkedHashMap(defectsByPhase[0]);
        DataPair before = injected.remove(Defect.BEFORE_DEVELOPMENT);
        DataPair total = injected.remove(WorkflowToDateReport.TOTAL_KEY);
        injected.put(Defect.BEFORE_DEVELOPMENT, before);
        injected.put(WorkflowToDateReport.TOTAL_KEY, total);
        Map removed = defectsByPhase[1];

        out.print("<p>\n");
        writePhaseChart(false, "Defects_Injected", injected);
        writePhaseChart(false, "Defects_Removed", removed);
        out.print("</p>\n");
    }

    private void writePhaseChart(boolean plan, String titleRes,
            Map<String, DataPair> phaseData) throws IOException {
        int numRows = phaseData.size() - 1;
        ResultSet data = new ResultSet(numRows, 1);
        int row = 0;
        for (Entry<String, DataPair> e : phaseData.entrySet()) {
            if (++row > numRows)
                break;
            data.setRowName(row, e.getKey());
            double value = plan ? e.getValue().plan : e.getValue().actual;
            data.setData(row, 1, new DoubleData(value));
        }
        writeChart((plan ? "Plan" : "Actual"), titleRes, data);
    }

    private void writeChart(String type, String titleRes, ResultSet chartData)
            throws IOException {
        chartData.setColName(0, "Phase");
        chartData.setColName(1, "Time");
        String title = resources.getString(titleRes);

        String dataName = "Workflow_Chart///" + type + "_" + titleRes;
        ListData l = new ListData();
        l.add(chartData);
        getDataContext().putValue(dataName, l);

        StringBuffer fullUri = new StringBuffer("full.htm");
        HTMLUtils.appendQuery(fullUri, "chart", "pie");
        HTMLUtils.appendQuery(fullUri, "useData", dataName);
        HTMLUtils.appendQuery(fullUri, "title", title);
        HTMLUtils.appendQuery(fullUri, "colorScheme", "consistent");

        StringBuffer uri = new StringBuffer();
        uri.append(resolveRelativeURI("pie.class"));
        HTMLUtils.appendQuery(uri, "useData", dataName);
        HTMLUtils.appendQuery(uri, "title", title);
        HTMLUtils.appendQuery(uri, "qf", "small.rpt");
        HTMLUtils.appendQuery(uri, "hideLegend", "t");
        HTMLUtils.appendQuery(uri, "colorScheme", "consistent");
        HTMLUtils.appendQuery(uri, "html", "t");
        HTMLUtils.appendQuery(uri, "href", fullUri.toString());

        out.print(getRequestAsString(uri.toString()));
    }

    private String esc(String s) {
        return HTMLUtils.escapeEntities(s);
    }

    private String res(String resKey) {
        return resources.getString(resKey);
    }

    private static final String TOTAL_KEY = WorkflowHistDataHelper.TOTAL_PHASE_KEY;

}
