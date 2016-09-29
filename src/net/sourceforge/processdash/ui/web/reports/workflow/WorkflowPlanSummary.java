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

package net.sourceforge.processdash.ui.web.reports.workflow;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.util.ResultSet;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.log.defects.Defect;
import net.sourceforge.processdash.net.http.TinyCGIException;
import net.sourceforge.processdash.tool.db.QueryRunner;
import net.sourceforge.processdash.tool.db.WorkflowHistDataHelper;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.DataPair;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.HTMLUtils;

public class WorkflowPlanSummary extends TinyCGIBase {

    private static final Resources resources = Resources
            .getDashBundle("Analysis");

    @Override
    protected void writeContents() throws IOException {
        String workflowID = getParameter("workflow");
        WorkflowHistDataHelper hist = new WorkflowHistDataHelper(getQuery(),
                workflowID);
        if (hist.getWorkflowName() == null)
            throw new TinyCGIException(404,
                    "The requested workflow was not found.");
        AnalysisPage.doConfigureFilter(hist,
            (HttpServletRequest) env.get(HttpServletRequest.class));

        String title = resources.getString("Workflow.Analysis.Title") + " - "
                + hist.getWorkflowName();

        out.print("<html><head><title>");
        out.print(esc(title));
        out.print("</title>\n");
        out.print(cssLinkHTML());
        if (hist.isFiltering())
            out.write("<link rel='stylesheet' type='text/css' href='filter-style.css'>\n");
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

        out.write("<table><tr>\n<td style='vertical-align:baseline'><h2>");
        out.print(esc(res("Summary.Title")));
        out.write("&nbsp;</td>\n");
        if (!isExporting())
            writePageSubtitle(hist);
        out.write("</tr></table>\n");

        Map<String, DataPair> sizes = hist.getAddedAndModifiedSizes();
        Map<String, DataPair> timeInPhase = hist.getTotalTimeInPhase();
        Map<String, DataPair>[] defectsByPhase = hist.getDefectsByPhase();

        writeOverallMetrics(sizes, timeInPhase, hist.getPhaseTypes());
        printTable("Size", "Added_&_Modified", sizes, Format.Number, false);
        printTable("Time_in_Phase", null, timeInPhase, Format.Time, true);
        printTimeInPhaseCharts(timeInPhase);

        if (defectsByPhase[1].get(TOTAL_KEY).actual > 0) {
            out.print("<div id=\"defects\">\n");
            setBeforeAndAfterRowLabels(timeInPhase);
            printTable("Defects_Injected", null, defectsByPhase[0], Format.Number, true);
            printTable("Defects_Removed", null, defectsByPhase[1], Format.Number, true);
            printDefectsByPhaseCharts(defectsByPhase);
            writeAdvancedDefectMetrics(hist, defectsByPhase, timeInPhase);
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

    protected void writePageSubtitle(WorkflowHistDataHelper hist) {
        out.write("<td class='doNotPrint' style='vertical-align:baseline'><i>");
        String workflowID = HTMLUtils.urlEncode(hist.getWorkflowID());

        out.write("<a href='workflowToDate?page=Filter&amp;workflow=");
        out.write(workflowID);
        out.write("'>" + resources.getHTML("Workflow.Filter.Link_Text"));
        out.write("</a>&nbsp;&nbsp;");

        if (Settings.isTeamMode()) {
            out.write("<a href='workflowToDate?page=Config&amp;workflow=");
            out.write(workflowID);
            out.write("'>" + resources.getHTML("Workflow.Config.Link_Text"));
            out.write("</a>");
        }

        out.write("</i></td>\n");
    }

    private void writeOverallMetrics(Map<String, DataPair> sizes,
            Map<String, DataPair> timeInPhase, Map<String, String> phaseTypes) {
        DataPair totalTime = timeInPhase.get(TOTAL_KEY);

        out.print("<h2 style='margin-top:0px'>");
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

    private void writeAdvancedDefectMetrics(WorkflowHistDataHelper hist,
            Map<String, DataPair>[] defectsByPhase,
            Map<String, DataPair> timeInPhase) {

        // get the yields for the process
        Map<String, DataPair>[] yields = hist.getYields();
        Map<String, DataPair> processYields = yields[0];
        Map<String, DataPair> phaseYields = yields[1];

        // change the display name for the "total" row
        DataPair totalProcessYield = processYields.remove(TOTAL_KEY);
        processYields.put(res("Workflow.Analysis.Workflow_Completion"),
            totalProcessYield);

        // to clean up the report, replace 0/0 yields with #DIV/0!
        replaceNaNs(Double.POSITIVE_INFINITY, processYields, phaseYields);

        printTable("Workflow.Analysis.Phase_Yields", null, phaseYields,
            Format.Percent, false);
        printTable("Workflow.Analysis.Process_Yields",
            "Workflow.Analysis.%_Removed_Before", processYields,
            Format.Percent, false);

        Map<String, DataPair> injRates = divide(defectsByPhase[0], timeInPhase);
        Map<String, DataPair> remRates = divide(defectsByPhase[1], timeInPhase);
        multiply(60, injRates, remRates); // convert minutes to hours
        replaceNaNs(0.0, injRates, remRates); // clean up 0/0 rates
        applyLegacyMultiplier(defectsByPhase, injRates, remRates);
        printTable("Workflow.Analysis.Defect_Injection_Rates",
            "Defects_Injected_per_Hour", injRates, Format.Number, false);
        printTable("Workflow.Analysis.Defect_Removal_Rates",
            "Defects_Removed_per_Hour", remRates, Format.Number, false);
    }

    private Map<String, DataPair> divide(Map<String, DataPair> numerators,
            Map<String, DataPair> denominators) {
        Map<String, DataPair> result = new LinkedHashMap<String, DataPair>();
        for (Entry<String, DataPair> e : denominators.entrySet()) {
            String phaseName = e.getKey();
            DataPair denominator = e.getValue();
            DataPair numerator = numerators.get(phaseName);
            if (numerator == null)
                continue;

            DataPair ratio = new DataPair(numerator).divide(denominator);
            result.put(phaseName, ratio);
        }

        return result;
    }

    private void multiply(double factor, Map<String, DataPair>... dataToFix) {
        for (Map<String, DataPair> oneDataSet : dataToFix) {
            for (DataPair pair : oneDataSet.values()) {
                pair.multiply(factor);
            }
        }
    }

    private void replaceNaNs(double replacementValue,
            Map<String, DataPair>... dataToFix) {
        for (Map<String, DataPair> oneDataSet : dataToFix) {
            for (DataPair pair : oneDataSet.values()) {
                pair.replaceNaN(replacementValue);
            }
        }
    }

    /**
     * Teams that have been using the dashboard for a while will have legacy
     * (MCF-phase) defects in their defect log. Once they begin collecting
     * defect data against workflow phases, they will receive useful numbers for
     * inj/rem % by phase, as well as yield. But unfortunately, the injection
     * and removal rates will be too low, since we are dividing by time in phase
     * (which includes time from legacy project cycles where the defects were
     * collected the old way).
     * 
     * To avoid this problem, we count the number of legacy defects and apply a
     * scaling factor to let defect counts and time in phase relate in an
     * apples-to-apples way. This is an engineering compromise, based on the
     * observation that useful, meaningful defects rates are better than no
     * rates at all.
     * 
     * In the future, when teams collect all data against workflow phases, this
     * method will be a no-op.
     */
    private void applyLegacyMultiplier(Map<String, DataPair>[] defectsByPhase,
            Map<String, DataPair>... dataToFix) {
        DataPair total = defectsByPhase[1].get(TOTAL_KEY);
        DataPair unrecognized = defectsByPhase[2].get(TOTAL_KEY);
        if (total.actual > 0 && unrecognized.actual > 0) {
            double factor = 1 + (unrecognized.actual / total.actual);
            for (Map<String, DataPair> oneDataSet : dataToFix) {
                for (DataPair pair : oneDataSet.values())
                    pair.actual *= factor;
            }
        }
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
            return beforeRowLabel;
        else if (Defect.AFTER_DEVELOPMENT.equals(rowLabel))
            return afterRowLabel;
        else
            return rowLabel;
    }

    private void setBeforeAndAfterRowLabels(Map<String, DataPair> timeInPhase) {
        if (timeInPhase.size() < 2) {
            beforeRowLabel = res("Before_Development");
            afterRowLabel = res("After_Development");
        } else {
            Iterator<String> i = timeInPhase.keySet().iterator();
            String firstPhase = i.next();
            String lastPhase = firstPhase;
            while (i.hasNext()) {
                String onePhase = i.next();
                if (!TOTAL_KEY.equals(onePhase))
                    lastPhase = onePhase;
            }
            beforeRowLabel = resources.format("Workflow.Analysis.Before_FMT",
                firstPhase);
            afterRowLabel = resources.format("Workflow.Analysis.After_FMT",
                lastPhase);
        }
    }

    private String beforeRowLabel, afterRowLabel;


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
        writePhaseChart(true, "Estimated_Time", "Hours", 60, timeInPhase);
        writePhaseChart(false, "Time", "Hours", 60, timeInPhase);
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
        DataPair total = injected.remove(TOTAL_KEY);
        injected.put(Defect.BEFORE_DEVELOPMENT, before);
        injected.put(TOTAL_KEY, total);
        Map removed = defectsByPhase[1];

        out.print("<p>\n");
        writePhaseChart(false, "Defects_Injected", "Defects", 1, injected);
        writePhaseChart(false, "Defects_Removed", "Defects", 1, removed);
        out.print("</p>\n");
    }

    private void writePhaseChart(boolean plan, String titleRes,
            String columnRes, double factor, Map<String, DataPair> phaseData)
            throws IOException {
        int numRows = phaseData.size() - 1;
        ResultSet data = new ResultSet(numRows, 1);
        int row = 0;
        for (Entry<String, DataPair> e : phaseData.entrySet()) {
            if (++row > numRows)
                break;
            data.setRowName(row, e.getKey());
            double value = plan ? e.getValue().plan : e.getValue().actual;
            data.setData(row, 1, new DoubleData(value / factor));
        }
        writeChart((plan ? "Plan" : "Actual"), titleRes, columnRes, data);
    }

    private void writeChart(String type, String titleRes, String columnRes,
            ResultSet chartData) throws IOException {
        chartData.setColName(0, "Phase");
        chartData.setColName(1, resources.getString(columnRes));
        String title = resources.getString(titleRes);

        String dataName = "Workflow_Chart///" + type + "_" + titleRes;
        ListData l = new ListData();
        l.add(chartData);
        getDataContext().putValue(dataName, l);

        StringBuffer fullUri = new StringBuffer(isExporting() ? "table.class"
                : "full.htm");
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

    private QueryRunner getQuery() {
        return getDashboardContext().getDatabasePlugin().getObject(
            QueryRunner.class);
    }

    private String esc(String s) {
        return HTMLUtils.escapeEntities(s);
    }

    private String res(String resKey) {
        return resources.getString(resKey);
    }

    private static final String TOTAL_KEY = WorkflowHistDataHelper.TOTAL_PHASE_KEY;

}
