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
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.processdash.data.util.ResultSet;
import net.sourceforge.processdash.tool.db.WorkflowHistDataHelper.Enactment;
import net.sourceforge.processdash.tool.db.WorkflowHistDataHelper.PhaseType;

public class QualityAnalysisPage extends AnalysisPage {

    public QualityAnalysisPage() {
        super("Quality", "Quality.Title");
    }

    @Override
    protected void writeHtmlContent(HttpServletRequest req,
            HttpServletResponse resp, ChartData chartData)
            throws ServletException, IOException {

        writeChart(req, resp, chartData, "yield");
        writeChart(req, resp, chartData, "phaseYields");
        writeChart(req, resp, chartData, "processYields");
        writeChart(req, resp, chartData, "COQ");
        writeChart(req, resp, chartData, "appraisalCOQ");
        writeChart(req, resp, chartData, "appraisalPhases");
        writeChart(req, resp, chartData, "failureCOQ");
        writeChart(req, resp, chartData, "failurePhases");
        writeChart(req, resp, chartData, "totalCOQ");

        List<String> appraisal = chartData.getPhases(PhaseType.Appraisal);
        List<String> failure = chartData.getPhases(PhaseType.Failure);
        boolean hasAFR = !appraisal.isEmpty() && !failure.isEmpty();
        if (hasAFR) {
            writeChart(req, resp, chartData, "qualityPhases");
            writeChart(req, resp, chartData, "appraisalVsFailure");
            writeChart(req, resp, chartData, "AFR");
            writeChart(req, resp, chartData, "yieldVsAfr");
        }

        if (!failure.isEmpty()) {
            String lastFailPhase = failure.get(failure.size() - 1);
            if (hasAFR)
                writeChart(req, resp, chartData, "defectsVsAfr", lastFailPhase);
            writeChart(req, resp, chartData, "defectsVsYield", lastFailPhase);
        }
    }


    @Chart(id = "yield", type = "line", titleKey = "Quality.Yield_Title", //
    format = "headerComment=${Quality.Yield_Header}")
    public ResultSet getYield(ChartData chartData) {
        ResultSet data = chartData.getEnactmentResultSet(1);
        writeYield(data, 1);
        return data;
    }


    @Chart(id = "phaseYields", type = "line", //
    titleKey = "Quality.Phase_Yield_Title", //
    format = "units=${Quality.Phase_Yield_Label}\n"
            + "headerComment=${Quality.Phase_Yield_Header}")
    public ResultSet getPhaseYields(ChartData chartData) {
        return writeYieldsByPhase(chartData, false);
    }


    @Chart(id = "processYields", type = "line", //
    titleKey = "Quality.Process_Yield_Title", //
    format = "units=${Quality.Process_Yield_Label}\n"
            + "headerComment=${Quality.Process_Yield_Header}")
    public ResultSet getProcessYields(ChartData chartData) {
        return writeYieldsByPhase(chartData, true);
    }


    @Chart(id = "COQ", type = "line", titleKey = "Quality.COQ_Title", //
    format = "units=${Quality.COQ_Label}")
    public ResultSet getCostOfQuality(ChartData chartData) {
        ResultSet data = chartData.getEnactmentResultSet("Total",
            "Quality.Appraisal_COQ_Label", "Quality.Failure_COQ_Label");

        List<String> allQualityPhases = chartData.histData.getPhasesOfType(
            PhaseType.Appraisal, PhaseType.Failure);
        writePhaseTimePct(data, 1, allQualityPhases, PhaseType.Appraisal,
            PhaseType.Failure);

        return data;
    }


    @Chart(id = "appraisalCOQ", type = "line", //
    titleKey = "Quality.Appraisal_COQ_Title")
    public ResultSet getAppraisalCostOfQuality(ChartData chartData) {
        ResultSet data = chartData.getEnactmentResultSet( //
                "Quality.Appraisal_COQ_Label");
        writePhaseTimePct(data, 1, PhaseType.Appraisal);
        return data;
    }


    @Chart(id = "appraisalPhases", type = "line", //
    titleKey = "Quality.Appraisal_Phase_Title", //
    format = "units=${Percent_Time}\nnoSkipLegend=t")
    public ResultSet getAppraisalCostByPhase(ChartData chartData) {
        return ProcessAnalysisPage.getPhaseTimePctSet(chartData,
            PhaseType.Appraisal);
    }


    @Chart(id = "failureCOQ", type = "line", //
    titleKey = "Quality.Failure_COQ_Title")
    public ResultSet getFailureCostOfQuality(ChartData chartData) {
        ResultSet data = chartData.getEnactmentResultSet( //
                "Quality.Failure_COQ_Label");
        writePhaseTimePct(data, 1, PhaseType.Failure);
        return data;
    }


    @Chart(id = "failurePhases", type = "line", //
    titleKey = "Quality.Failure_Phase_Title", //
    format = "units=${Percent_Time}\nnoSkipLegend=t")
    public ResultSet getFailureCostByPhase(ChartData chartData) {
        return ProcessAnalysisPage.getPhaseTimePctSet(chartData,
            PhaseType.Failure);
    }


    @Chart(id = "totalCOQ", type = "line", titleKey = "Quality.Total_COQ_Title")
    public ResultSet getTotalCostOfQuality(ChartData chartData) {
        ResultSet data = chartData.getEnactmentResultSet( //
                "Quality.Total_COQ_Label");
        Object phases = chartData.histData.getPhasesOfType(PhaseType.Appraisal,
            PhaseType.Failure);
        writePhaseTimePct(data, 1, phases);
        return data;
    }


    @Chart(id = "qualityPhases", type = "line", //
    titleKey = "Quality.COQ_Phase_Title", //
    format = "units=${Percent_Time}\nnoSkipLegend=t")
    public ResultSet getQualityCostByPhase(ChartData chartData) {
        List<String> qualityPhases = chartData.histData.getPhasesOfType(
            PhaseType.Appraisal, PhaseType.Failure);
        ResultSet data = chartData.getEnactmentResultSet(qualityPhases.size());
        writePhaseTimePct(data, 1, qualityPhases.toArray());
        return data;
    }


    @Chart(id = "appraisalVsFailure", type = "xy", //
    titleKey = "Quality.Appraisal_vs_Failure_Title")
    public ResultSet getAppraisalVsFailureCost(ChartData chartData) {
        ResultSet data = chartData.getEnactmentResultSet(
            "Quality.Appraisal_COQ_Label", "Quality.Failure_COQ_Label");
        writePhaseTimePct(data, 1, PhaseType.Appraisal, PhaseType.Failure);
        return data;
    }


    @Chart(id = "AFR", type = "line", titleKey = "Quality.AFR_Title")
    public ResultSet getAppraisalToFailureRatio(ChartData chartData) {
        ResultSet data = chartData.getEnactmentResultSet( //
                "Quality.AFR_Label");
        writeAFR(data, 1);
        return data;
    }


    @Chart(id = "yieldVsAfr", type = "xy", //
    titleKey = "Process.Yield_Vs_AFR_Title")
    public ResultSet getYieldVsAFR(ChartData chartData) {
        ResultSet data = chartData.getEnactmentResultSet(2);
        writeAFR(data, 1);
        writeYield(data, 2);
        return data;
    }


    @Chart(id = "defectsVsAfr", type = "xy", params = "phase", //
    titleKey = "Process.Defects_Vs_AFR_Title_FMT")
    public ResultSet getDefectsVsAFR(ChartData chartData) {
        ResultSet data = chartData.getEnactmentResultSet(2);
        writeAFR(data, 1);
        writePhaseDefectDensity(chartData, data, 2, chartData.chartArgs[0]);
        return data;
    }


    @Chart(id = "defectsVsYield", type = "xy", params = "phase", //
    titleKey = "Process.Defects_Vs_Yield_Title_FMT")
    public ResultSet getDefectsVsYield(ChartData chartData) {
        ResultSet data = chartData.getEnactmentResultSet(2);
        writeYield(data, 1);
        writePhaseDefectDensity(chartData, data, 2, chartData.chartArgs[0]);
        return data;
    }



    public static void writeAFR(ResultSet data, int col) {
        if (data.getColName(col) == null)
            data.setColName(col, getRes("Process.AFR_Label"));
        for (int row = data.numRows(); row > 0; row--) {
            Enactment e = (Enactment) data.getRowObj(row);
            double appraisalTime = e.actualTime(PhaseType.Appraisal);
            double failureTime = e.actualTime(PhaseType.Failure);
            data.setData(row, col, num(appraisalTime / failureTime));
        }
    }

    public static void writeYield(ResultSet data, int col) {
        data.setColName(col, getRes("Process.Yield_Label"));
        data.setFormat(col, "100%");
        for (int row = data.numRows(); row > 0; row--) {
            Enactment e = (Enactment) data.getRowObj(row);
            data.setData(row, col, num(e.actualYield(null, true)));
        }
    }

    private ResultSet writeYieldsByPhase(ChartData chartData, boolean process) {
        List<String> phases = chartData.getPhases();
        if (process)
            phases.remove(0);
        Collections.reverse(phases);
        ResultSet data = chartData.getEnactmentResultSet(phases.size());
        for (int col = phases.size(); col > 0; col--) {
            String phase = phases.get(col - 1);
            data.setColName(col, phase);
            data.setFormat(col, "100%");
            for (int row = data.numRows(); row > 0; row--) {
                Enactment e = (Enactment) data.getRowObj(row);
                data.setData(row, col, num(e.actualYield(phase, process)));
            }
        }
        return data;
    }

    private static void writePhaseDefectDensity(ChartData chartData,
            ResultSet data, int col, String phase) {
        DefectAnalysisPage.writePhaseDefectDensity(chartData, data, col, phase);
        data.setColName(col, resources.format(
            "Defects.Density_Scatter.Axis_FMT", phase,
            chartData.getDensityStr()));
    }

    private static void writePhaseTimePct(ResultSet resultSet, int firstCol,
            Object... phases) {
        ProcessAnalysisPage.writePhaseTimePct(resultSet, firstCol, phases);
    }

}
