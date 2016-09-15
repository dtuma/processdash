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
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.processdash.data.util.ResultSet;
import net.sourceforge.processdash.tool.db.WorkflowHistDataHelper.Enactment;
import net.sourceforge.processdash.tool.db.WorkflowHistDataHelper.PhaseType;

public class QualityAnalysisPage extends AnalysisPage {

    public QualityAnalysisPage() {
        super("workflowQuality", "Quality.Title");
    }

    @Override
    protected void writeHtmlContent(HttpServletRequest req,
            HttpServletResponse resp, ChartData chartData)
            throws ServletException, IOException {
        writeChart(req, resp, chartData, "COQ");
        writeChart(req, resp, chartData, "appraisalCOQ");
        writeChart(req, resp, chartData, "appraisalPhases");
        writeChart(req, resp, chartData, "failureCOQ");
        writeChart(req, resp, chartData, "failurePhases");
        writeChart(req, resp, chartData, "totalCOQ");
        writeChart(req, resp, chartData, "qualityPhases");
        writeChart(req, resp, chartData, "appraisalVsFailure");
        writeChart(req, resp, chartData, "AFR");
    }


    @Chart(id = "COQ", type = "line", titleKey = "Quality.COQ_Title", //
    format = "units=${Quality.COQ_Label}")
    public ResultSet getCostOfQuality(ChartData chartData) {
        ResultSet data = chartData.getEnactmentResultSet("Total",
            "Quality.Appraisal_COQ_Label", "Quality.Failure_COQ_Label");

        List<String> allQualityPhases = chartData.histData.getPhasesOfType(
            PhaseType.Appraisal, PhaseType.Failure);
        chartData.writePhaseTimePct(data, 1, allQualityPhases,
            PhaseType.Appraisal, PhaseType.Failure);

        return data;
    }


    @Chart(id = "appraisalCOQ", type = "line", //
    titleKey = "Quality.Appraisal_COQ_Title")
    public ResultSet getAppraisalCostOfQuality(ChartData chartData) {
        ResultSet data = chartData.getEnactmentResultSet( //
                "Quality.Appraisal_COQ_Label");
        chartData.writePhaseTimePct(data, 1, PhaseType.Appraisal);
        return data;
    }


    @Chart(id = "appraisalPhases", type = "line", //
    titleKey = "Quality.Appraisal_Phase_Title", //
    format = "units=${Percent_Time}")
    public ResultSet getAppraisalCostByPhase(ChartData chartData) {
        List<String> apprPhases = chartData.histData
                .getPhasesOfType(PhaseType.Appraisal);
        ResultSet data = chartData.getEnactmentResultSet(apprPhases.size());
        chartData.writePhaseTimePct(data, 1, apprPhases.toArray());
        return data;
    }


    @Chart(id = "failureCOQ", type = "line", //
    titleKey = "Quality.Failure_COQ_Title")
    public ResultSet getFailureCostOfQuality(ChartData chartData) {
        ResultSet data = chartData.getEnactmentResultSet( //
                "Quality.Failure_COQ_Label");
        chartData.writePhaseTimePct(data, 1, PhaseType.Failure);
        return data;
    }


    @Chart(id = "failurePhases", type = "line", //
    titleKey = "Quality.Failure_Phase_Title", //
    format = "units=${Percent_Time}")
    public ResultSet getFailureCostByPhase(ChartData chartData) {
        List<String> failurePhases = chartData.histData
                .getPhasesOfType(PhaseType.Failure);
        ResultSet data = chartData.getEnactmentResultSet(failurePhases.size());
        chartData.writePhaseTimePct(data, 1, failurePhases.toArray());
        return data;
    }


    @Chart(id = "totalCOQ", type = "line", titleKey = "Quality.Total_COQ_Title")
    public ResultSet getTotalCostOfQuality(ChartData chartData) {
        ResultSet data = chartData.getEnactmentResultSet( //
                "Quality.Total_COQ_Label");
        Object phases = chartData.histData.getPhasesOfType(PhaseType.Appraisal,
            PhaseType.Failure);
        chartData.writePhaseTimePct(data, 1, phases);
        return data;
    }


    @Chart(id = "qualityPhases", type = "line", //
    titleKey = "Quality.COQ_Phase_Title", //
    format = "units=${Percent_Time}")
    public ResultSet getQualityCostByPhase(ChartData chartData) {
        List<String> qualityPhases = chartData.histData.getPhasesOfType(
            PhaseType.Appraisal, PhaseType.Failure);
        ResultSet data = chartData.getEnactmentResultSet(qualityPhases.size());
        chartData.writePhaseTimePct(data, 1, qualityPhases.toArray());
        return data;
    }


    @Chart(id = "appraisalVsFailure", type = "xy", //
    titleKey = "Quality.Appraisal_vs_Failure_Title")
    public ResultSet getAppraisalVsFailureCost(ChartData chartData) {
        ResultSet data = chartData.getEnactmentResultSet(
            "Quality.Appraisal_COQ_Label", "Quality.Failure_COQ_Label");
        chartData.writePhaseTimePct(data, 1, PhaseType.Appraisal,
            PhaseType.Failure);
        return data;
    }


    @Chart(id = "AFR", type = "line", titleKey = "Quality.AFR_Title")
    public ResultSet getAppraisalToFailureRatio(ChartData chartData) {
        ResultSet data = chartData.getEnactmentResultSet( //
                "Quality.AFR_Label");
        for (int row = data.numRows(); row > 0; row--) {
            Enactment e = (Enactment) data.getRowObj(row);
            double appraisalTime = e.actualTime(PhaseType.Appraisal);
            double failureTime = e.actualTime(PhaseType.Failure);
            data.setData(row, 1, num(appraisalTime / failureTime));
        }
        return data;
    }


}
