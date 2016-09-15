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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.processdash.data.util.ResultSet;
import net.sourceforge.processdash.tool.db.WorkflowHistDataHelper;
import net.sourceforge.processdash.tool.db.WorkflowHistDataHelper.PhaseType;
import net.sourceforge.processdash.util.DataPair;

public class ProcessAnalysisPage extends AnalysisPage {

    public ProcessAnalysisPage() {
        super("workflowProcess", "Process.Title");
    }


    @Override
    protected void writeHtmlContent(HttpServletRequest req,
            HttpServletResponse resp, ChartData chartData)
            throws ServletException, IOException {
        writeChart(req, resp, chartData, "overheadTime");
        writeChart(req, resp, chartData, "overheadPhases");
        writeChart(req, resp, chartData, "constrTime");
        writeChart(req, resp, chartData, "constrPhases");
        writeChart(req, resp, chartData, "typeTime");
        writeChart(req, resp, chartData, "typeToDate");
        writeChart(req, resp, chartData, "phaseTime");
        writeChart(req, resp, chartData, "phaseToDate");
    }


    @Chart(id = "overheadTime", type = "line", //
    titleKey = "Plan.Percent_Overhead_Time")
    public ResultSet getOverheadTime(ChartData chartData) {
        ResultSet data = chartData.getEnactmentResultSet("Percent_Units");
        chartData.writePhaseTimePct(data, 1, PhaseType.Overhead);
        return data;
    }


    @Chart(id = "overheadPhases", type = "line", //
    titleKey = "Plan.Overhead_Phase_Time", //
    format = "units=${Percent_Time}")
    public ResultSet getOverheadTimeByPhase(ChartData chartData) {
        List<String> overheadPhases = chartData.histData
                .getPhasesOfType(PhaseType.Overhead);
        ResultSet data = chartData.getEnactmentResultSet(overheadPhases.size());
        chartData.writePhaseTimePct(data, 1, overheadPhases.toArray());
        return data;
    }


    @Chart(id = "constrTime", type = "line", //
    titleKey = "Plan.Percent_Construction_Time")
    public ResultSet getConstructionTime(ChartData chartData) {
        ResultSet data = chartData.getEnactmentResultSet("Percent_Units");
        chartData.writePhaseTimePct(data, 1, PhaseType.Construction);
        return data;
    }


    @Chart(id = "constrPhases", type = "line", //
    titleKey = "Plan.Construction_Phase_Time", //
    format = "units=${Percent_Time}")
    public ResultSet getConstructionTimeByPhase(ChartData chartData) {
        List<String> constrPhases = chartData.histData
                .getPhasesOfType(PhaseType.Construction);
        ResultSet data = chartData.getEnactmentResultSet(constrPhases.size());
        chartData.writePhaseTimePct(data, 1, constrPhases.toArray());
        return data;
    }


    @Chart(id = "typeTime", type = "area", //
    titleKey = "Process.Time_By_Type_Title", //
    format = "stacked=pct")
    public ResultSet getTimeByPhaseType(ChartData chartData) {
        ResultSet data = chartData.getEnactmentResultSet(4);
        for (PhaseType type : PhaseType.values()) {
            int col = 4 - type.ordinal();
            data.setColName(col, getRes("Process.Type_" + type));
            chartData.writePhaseTimePct(data, col, type);
        }
        return data;
    }


    @Chart(id = "typeToDate", type = "pie", //
    titleKey = "Process.Time_By_Type_To_Date_Title")
    public ResultSet getTimeByPhaseTypeToDate(ChartData chartData) {
        ResultSet data = new ResultSet(4, 2);
        data.setColName(0, getRes("Process.Type_Header"));
        data.setColName(1, getRes("Hours"));
        data.setColName(2, getRes("Percent_Units"));
        data.setFormat(2, "100%");

        double totalTime = chartData.histData.getTime(null, null, true);
        for (PhaseType type : PhaseType.values()) {
            int row = 4 - type.ordinal();
            double time = chartData.histData.getTime(null, type, true);
            data.setRowName(row, getRes("Process.Type_" + type));
            data.setData(row, 1, num(time / 60));
            data.setData(row, 2, num(time / totalTime));
        }

        return data;
    }


    @Chart(id = "phaseTime", type = "area", //
    titleKey = "Plan.Phase_Time_Title", smallFmt = "hideLegend=t", //
    format = "stacked=pct\nheaderComment=(${Hours})")
    public ResultSet getTimeByPhase(ChartData chartData) {
        List<String> phases = chartData.histData.getPhasesOfType();
        ResultSet data = chartData.getEnactmentResultSet(phases.size());
        chartData.writePhaseTime(data, false, 1, phases.toArray());
        return data;
    }


    @Chart(id = "phaseToDate", type = "pie", //
    titleKey = "Plan.Time_In_Phase_Title", smallFmt = "hideLegend=t")
    public ResultSet getTimeInPhaseToDate(ChartData chartData) {
        Map<String, DataPair> time = chartData.histData.getTotalTimeInPhase();
        double totalTime = time.remove(WorkflowHistDataHelper.TOTAL_PHASE_KEY).actual;
        List<String> phases = new ArrayList(time.keySet());
        ResultSet data = new ResultSet(phases.size(), 2);
        data.setColName(0, getRes("Phase"));
        data.setColName(1, getRes("Hours"));
        data.setColName(2, getRes("Percent_Units"));
        data.setFormat(2, "100%");

        for (int row = phases.size(); row > 0; row--) {
            String onePhase = phases.get(row - 1);
            data.setRowName(row, onePhase);
            data.setData(row, 1, num(time.get(onePhase).actual / 60));
            data.setData(row, 2, num(time.get(onePhase).actual / totalTime));
        }
        return data;
    }


}
