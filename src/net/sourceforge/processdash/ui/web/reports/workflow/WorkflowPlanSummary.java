// Copyright (C) 2014-2018 Tuma Solutions, LLC
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

import static net.sourceforge.processdash.tool.db.WorkflowHistDataHelper.INJ;
import static net.sourceforge.processdash.tool.db.WorkflowHistDataHelper.REM;
import static net.sourceforge.processdash.tool.db.WorkflowHistDataHelper.UNK;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.NumberData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.data.util.ResultSet;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.log.defects.Defect;
import net.sourceforge.processdash.net.http.TinyCGIException;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.tool.db.WorkflowHistDataHelper;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.DataPair;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.HTMLUtils;

public class WorkflowPlanSummary extends TinyCGIBase
        implements AnalysisPage.HistDataFilterConfigurer {

    private static final Resources resources = Resources
            .getDashBundle("Analysis");

    @Override
    protected void writeContents() throws IOException {
        // is this request for a specific project, or for "to date" data?
        boolean isProjectReport = parameters.containsKey("project");

        // retrieve a set of chart data for the current report
        ChartData chartData = AnalysisPage.getChartData(
            (HttpServletRequest) env.get(HttpServletRequest.class),
            isProjectReport ? this : Boolean.TRUE);
        if (chartData == null)
            throw new TinyCGIException(404,
                    "The requested workflow was not found.");

        WorkflowHistDataHelper hist = chartData.histData;
        String workflowName = hist.getWorkflowName();
        String title = resources.getString("Workflow.Analysis.Title") + " - "
                    + workflowName;

        out.print("<html><head><title>");
        out.print(esc(title));
        out.print("</title>\n");
        out.print(cssLinkHTML());
        if (!isProjectReport && hist.isFiltering())
            out.write("<link rel='stylesheet' type='text/css' href='filter-style.css'>\n");
        out.print(HTMLUtils.scriptLinkHtml("/lib/overlib.js"));
        out.print("<style>\n");
        out.print(" .rowLabel { padding-right: 10px }\n");
        out.print(" th.plan, th.act { width: 70px; }\n");
        out.print(" td.plan, td.act { padding-right: 4px; border: 1px solid gray; text-align: right }\n");
        out.print(" div.hideQualPlan th.plan, div.hideQualPlan td.plan { display: none }\n");
        out.print("</style>\n");
        out.print("</head>\n");

        out.print("<body><h1>");
        out.print(esc(isProjectReport ? workflowName : title));
        out.print("</h1>\n");

        if (!isProjectReport) {
            out.write("<table><tr>\n<td style='vertical-align:baseline'><h2>");
            out.print(esc(res("Summary.Title")));
            out.write("&nbsp;</td>\n");
            if (!isExporting())
                writePageSubtitle(hist);
            out.write("</tr></table>\n");
        }

        Map<String, DataPair> sizes = hist.getAddedAndModifiedSizes();
        Map<String, DataPair> timeInPhase = hist.getTotalTimeInPhase();
        Map<String, DataPair>[] defectsByPhase = hist.getDefectsByPhase();

        DataContext qualityParams = getQualityParamData(hist);
        Map<String, String> phaseIDs = hist.getPhaseIdentifiers();

        // calculate actual defect injection rates
        Map<String, DataPair> injRates = divide(defectsByPhase[INJ], timeInPhase);
        multiply(60, injRates); // convert minutes to hours
        // load quality parameter values for planned injection rates
        loadPhaseQualityPlanParams(qualityParams, phaseIDs, injRates,
            "Estimated Defects Injected per Hour");

        // get the yields for the process/phases
        Map<String, DataPair>[] yields = hist.getYields();
        Map<String, DataPair> processYields = yields[0];
        Map<String, DataPair> phaseYields = yields[1];
        // load quality parameter values for planned phase yields
        loadPhaseQualityPlanParams(qualityParams, phaseIDs, phaseYields,
            "Estimated % Phase Yield");

        // estimate planned injected/removed defects by phase
        boolean hasQualityPlan = calculatePlannedDefects(hist, defectsByPhase,
            timeInPhase, phaseYields, processYields, injRates);

        // calculate planned and actual defect removal rates
        Map<String, DataPair> remRates = divide(defectsByPhase[REM], timeInPhase);
        multiply(60, remRates); // convert minutes to hours

        for (Iterator<String> i = sizes.keySet().iterator(); i.hasNext();) {
            if (AnalysisPage.isTimeUnits(i.next()))
                i.remove();
        }

        writeOverallMetrics(sizes, timeInPhase, hist.getPhaseTypes());
        printTable("Size", "Added_&_Modified", sizes, Format.Number, false);
        printTable("Time_in_Phase", null, timeInPhase, Format.Time, true);
        printTimeInPhaseCharts(timeInPhase);

        if (hasQualityPlan || defectsByPhase[REM].get(TOTAL_KEY).actual > 0) {
            if (hasQualityPlan == false)
                out.print("<div class=\"hideQualPlan\">\n");
            setBeforeAndAfterRowLabels(timeInPhase);
            printTable("Defects_Injected", null, defectsByPhase[INJ], Format.Number, true);
            printTable("Defects_Removed", null, defectsByPhase[REM], Format.Number, true);
            printDefectsByPhaseCharts(defectsByPhase);
            writeAdvancedDefectMetrics(hist, defectsByPhase, timeInPhase,
                phaseYields, processYields, injRates, remRates, hasQualityPlan);
            if (hasQualityPlan == false)
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

    @Override
    public void applyFilter(WorkflowHistDataHelper histData) {
        // don't exclude data from incomplete enactments
        histData.setOnlyCompleted(false);

        // read the list of included enactment roots from the "root" URI param
        String[] roots = (String[]) parameters.get("root_ALL");
        histData.setIncludedEnactments(new HashSet(Arrays.asList(roots)));
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
            Map<String, DataPair> timeInPhase,
            Map<String, DataPair> phaseYields,
            Map<String, DataPair> processYields,
            Map<String, DataPair> injRates,
            Map<String, DataPair> remRates,
            boolean hasQualityPlan) throws IOException {

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

        // display plan-vs-actual yield charts
        if (hasQualityPlan) {
            out.print("<p>\n");
            writePhasePlanVsActualLineChart("Workflow.Analysis.Phase_Yields",
                "Quality.Phase_Yield_Label", phaseYields, "100%");
            writePhasePlanVsActualLineChart("Workflow.Analysis.Process_Yields",
                "Workflow.Analysis.%_Removed_Before", processYields, "100%");
            out.print("</p>\n");
        }

        injRates.remove(TOTAL_KEY); // don't display overall rates
        remRates.remove(TOTAL_KEY);
        replaceNaNs(0.0, injRates, remRates); // clean up 0/0 rates
        applyLegacyMultiplier(defectsByPhase, injRates, remRates);
        printTable("Workflow.Analysis.Defect_Injection_Rates",
            "Defects_Injected_per_Hour", injRates, Format.Number, false);
        printTable("Workflow.Analysis.Defect_Removal_Rates",
            "Defects_Removed_per_Hour", remRates, Format.Number, false);

        // display injection/removal rate charts
        out.print("<p>\n");
        if (hasQualityPlan) {
            writePhasePlanVsActualLineChart(
                "Workflow.Analysis.Defect_Injection_Rates",
                "Defects_Injected_per_Hour", injRates);
            writePhasePlanVsActualLineChart(
                "Workflow.Analysis.Defect_Removal_Rates",
                "Defects_Removed_per_Hour", remRates);
        }
        writeInjVsRemovedRatesChart(injRates, remRates);
        out.print("</p>\n");
    }

    private DataContext getQualityParamData(WorkflowHistDataHelper hist) {
        DataRepository data = getDataRepository();
        String projectID = hist.getContextProjectID();
        String projectPath = findProjectWithID(data, getPSPProperties(),
            PropertyKey.ROOT, projectID);
        return data.getSubcontext(
            projectPath + "/" + TeamDataConstants.WORKFLOW_PARAM_PREFIX);
    }

    private String findProjectWithID(DataRepository data, DashHierarchy hier,
            PropertyKey key, String projectID) {
        String dataName = key.path() + "/" + TeamDataConstants.PROJECT_ID;
        SimpleData sd = data.getSimpleValue(dataName);
        if (sd == null) {
            for (int i = hier.getNumChildren(key); i-- > 0;) {
                String childResult = findProjectWithID(data, hier,
                    hier.getChildKey(key, i), projectID);
                if (childResult != null)
                    return childResult;
            }
            return null;
        } else if (projectID.equals(sd.format())) {
            return key.path();
        } else {
            return null;
        }
    }

    private void loadPhaseQualityPlanParams(DataContext params,
            Map<String, String> phaseIDs, Map<String, DataPair> phaseData,
            String paramName) {
        for (Entry<String, String> e : phaseIDs.entrySet()) {
            // get the name of this phase and find its data pair
            String phaseName = e.getKey();
            DataPair phase = phaseData.get(phaseName);
            if (phase == null)
                continue;

            // get the phase ID, and use it to construct a param prefix
            String phaseID = e.getValue();
            int colonPos = phaseID.lastIndexOf(':');
            String numericID = phaseID.substring(colonPos + 1);

            // get the parameter value
            SimpleData sd = params.getSimpleValue(numericID + "/" + paramName);
            if (sd instanceof NumberData)
                phase.plan = ((NumberData) sd).getDouble();
            else
                phase.plan = 0;
        }
    }

    private boolean calculatePlannedDefects(WorkflowHistDataHelper hist,
            Map<String, DataPair>[] defectsByPhase,
            Map<String, DataPair> timeInPhase,
            Map<String, DataPair> phaseYields,
            Map<String, DataPair> processYields,
            Map<String, DataPair> injRates) {

        double cumInj = 0, cumRem = 0, runningDefectCount = 0;
        for (String phaseName : hist.getPhasesOfType()) {
            // calculate the process yield for this phase
            DataPair processYield = processYields.get(phaseName);
            if (processYield != null)
                processYield.plan = cumRem / cumInj;

            // estimate the # defects that will be injected in this phase
            double estInjRate = injRates.get(phaseName).plan;
            double estDefInj = estInjRate * timeInPhase.get(phaseName).plan / 60;
            defectsByPhase[INJ].get(phaseName).plan = estDefInj;
            runningDefectCount += estDefInj;
            cumInj += estDefInj;

            // use yield to estimate the defects that will be removed
            double estYield = phaseYields.get(phaseName).plan;
            double estDefRem = runningDefectCount * estYield;
            defectsByPhase[REM].get(phaseName).plan = estDefRem;
            runningDefectCount -= estDefRem;
            cumRem += estDefRem;
        }

        // calculate the process yield for the overall workflow
        processYields.get(TOTAL_KEY).plan = cumRem / cumInj;

        // save the escaped defects into the "after development" bucket
        DataPair remAfter = defectsByPhase[REM].get(Defect.AFTER_DEVELOPMENT);
        remAfter.plan = runningDefectCount;

        // update total planned defect count (inj and removed share a total)
        defectsByPhase[INJ].get(TOTAL_KEY).plan = cumInj;

        // return true if a nonzero quality plan was generated
        return cumInj > 0;
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
        DataPair total = defectsByPhase[REM].get(TOTAL_KEY);
        DataPair unrecognized = defectsByPhase[UNK].get(TOTAL_KEY);
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
        writePhasePieChart(true, "Estimated_Time", "Hours", 60, timeInPhase);
        writePhasePieChart(false, "Time", "Hours", 60, timeInPhase);
        out.print("</p>\n");
    }

    private void printDefectsByPhaseCharts(
            Map<String, DataPair>[] defectsByPhase) throws IOException {
        if (isExportingToExcel())
            return;

        // shuffle the items in the "injected" list so "Before Development" is
        // at the end. This makes the phase colors consistent across pie charts.
        Map<String, DataPair> injected = new LinkedHashMap(defectsByPhase[INJ]);
        DataPair before = injected.remove(Defect.BEFORE_DEVELOPMENT);
        DataPair total = injected.remove(TOTAL_KEY);
        injected.put(Defect.BEFORE_DEVELOPMENT, before);
        injected.put(TOTAL_KEY, total);
        Map removed = defectsByPhase[REM];

        out.print("<p>\n");
        writePhasePieChart(false, "Defects_Injected", "Defects", 1, injected);
        writePhasePieChart(false, "Defects_Removed", "Defects", 1, removed);
        writePhasePlanVsActualLineChart("Defect_Removal_Profile",
            "Workflow.Analysis.#_Defects_Removed", removed);
        writeLatentDefectChart(defectsByPhase);
        out.print("</p>\n");
    }

    private void writePhasePieChart(boolean plan, String titleRes,
            String columnRes, double factor, Map<String, DataPair> phaseData)
            throws IOException {
        ResultSet data = buildPhaseResultSet(factor, phaseData, plan);
        data.setColName(1, resources.getString(columnRes));
        writeChart("pie", (plan ? "Plan" : "Actual"), titleRes, data,
            "colorScheme", "consistent");
    }

    private void writePhasePlanVsActualLineChart(String titleRes,
            String yAxisRes, Map<String, DataPair> phaseData,
            String... format) throws IOException {
        ResultSet data = buildPhaseResultSet(1, phaseData, true, //
            phaseData, false);
        data.setColName(1, "Plan");
        data.setColName(2, "Actual");
        if (format.length == 1) {
            data.setFormat(1, format[0]);
            data.setFormat(2, format[0]);
        }
        String units = resources.getString(yAxisRes);
        writeChart("line", "Line", titleRes, data, //
            "units", units, "headerComment", units);
    }

    private void writeLatentDefectChart(Map<String, DataPair>[] defectsByPhase)
            throws IOException {
        Map<String, DataPair> latent = new LinkedHashMap();
        DataPair running = new DataPair();
        for (String phaseName : defectsByPhase[INJ].keySet()) {
            DataPair injectedInPhase = defectsByPhase[INJ].get(phaseName);
            running.add(injectedInPhase);
            DataPair removedInPhase = defectsByPhase[REM].get(phaseName);
            if (removedInPhase != null)
                running.subtract(removedInPhase);
            latent.put(phaseName, new DataPair(running));
        }

        ResultSet data = buildPhaseResultSet(1, latent, true, latent, false);
        data.setColName(1, resources.getString(LATENT_RES + "Plan"));
        data.setColName(2, resources.getString(LATENT_RES + "Actual"));
        String units = resources.getString(LATENT_RES + "Units");
        writeChart("line", "Line", LATENT_RES + "Title", data, //
            "units", units, "headerComment", units, //
            "footerComment", resources.getString(LATENT_RES + "Footer"));
    }
    private static final String LATENT_RES = "Workflow.Analysis.Latent_Defects.";

    private void writeInjVsRemovedRatesChart(Map<String, DataPair> injRates,
            Map<String, DataPair> remRates) throws IOException {
        injRates.put(TOTAL_KEY, null);
        ResultSet data = buildPhaseResultSet(1, injRates, false, //
            remRates, false);
        data.setColName(1, resources.getString(RATES_RES + "Injected"));
        data.setColName(2, resources.getString(RATES_RES + "Removed"));
        String u = resources.getString(RATES_RES + "Units");
        writeChart("line", "Line", RATES_RES + "Title", data, //
            "units", u, "headerComment", u);
    }
    private static final String RATES_RES = "Workflow.Analysis.Defect_Rates.";

    private ResultSet buildPhaseResultSet(double factor, Object... columns) {
        Map<String, DataPair> phaseData = (Map<String, DataPair>) columns[0];
        int numRows = phaseData.size() - 1;
        int numCols = columns.length / 2;
        ResultSet result = new ResultSet(numRows, numCols);
        result.setColName(0, "Phase");

        int row = 0;
        for (String phaseName : phaseData.keySet()) {
            if (++row > numRows)
                break;
            result.setRowName(row, phaseName);
            for (int col = 0; col < numCols; col++) {
                phaseData = (Map<String, DataPair>) columns[col * 2];
                DataPair pair = phaseData.get(phaseName);
                Boolean plan = (Boolean) columns[col * 2 + 1];
                double value = plan ? pair.plan : pair.actual;
                result.setData(row, col + 1, new DoubleData(value / factor));
            }
        }
        return result;
    }

    private void writeChart(String chartType, String subtype, String titleRes,
            ResultSet chartData, String... extraParams) throws IOException {
        String title = resources.getString(titleRes);

        String dataName = "Workflow_Chart///" + subtype + "_" + titleRes;
        ListData l = new ListData();
        l.add(chartData);
        getDataContext().putValue(dataName, l);

        StringBuffer fullUri = new StringBuffer(isExporting() ? "table.class"
                : "full.htm");
        HTMLUtils.appendQuery(fullUri, "chart", chartType);
        HTMLUtils.appendQuery(fullUri, "useData", dataName);
        HTMLUtils.appendQuery(fullUri, "title", title);
        for (int i = 0; i < extraParams.length; i += 2)
            HTMLUtils.appendQuery(fullUri, extraParams[i], extraParams[i+1]);

        StringBuffer uri = new StringBuffer();
        uri.append(resolveRelativeURI(chartType + ".class"));
        HTMLUtils.appendQuery(uri, "useData", dataName);
        HTMLUtils.appendQuery(uri, "title", title);
        HTMLUtils.appendQuery(uri, "qf", "small.rpt");
        HTMLUtils.appendQuery(uri, "hideLegend", "t");
        for (int i = 0; i < extraParams.length; i += 2)
            HTMLUtils.appendQuery(uri, extraParams[i], extraParams[i+1]);
        HTMLUtils.appendQuery(uri, "html", "t");
        HTMLUtils.appendQuery(uri, "href", fullUri.toString());

        out.print(getRequestAsString(uri.toString()));
        out.print("&nbsp;\n");
    }

    private String esc(String s) {
        return HTMLUtils.escapeEntities(s);
    }

    private String res(String resKey) {
        return resources.getString(resKey);
    }

    private static final String TOTAL_KEY = WorkflowHistDataHelper.TOTAL_PHASE_KEY;

}
