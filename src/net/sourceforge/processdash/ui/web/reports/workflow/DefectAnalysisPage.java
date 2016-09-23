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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.util.ResultSet;
import net.sourceforge.processdash.log.defects.Defect;
import net.sourceforge.processdash.tool.db.WorkflowHistDataHelper;
import net.sourceforge.processdash.tool.db.WorkflowHistDataHelper.Enactment;
import net.sourceforge.processdash.tool.db.WorkflowHistDataHelper.PhaseType;
import net.sourceforge.processdash.util.DataPair;

public class DefectAnalysisPage extends AnalysisPage {

    public DefectAnalysisPage() {
        super("Defects", "Defects.Title");
    }


    @Override
    protected void writeHtmlContent(HttpServletRequest req,
            HttpServletResponse resp, ChartData chartData)
            throws ServletException, IOException {

        writeChart(req, resp, chartData, "totalDefects");

        for (String phase : chartData.getPhases(PhaseType.Construction))
            writeChart(req, resp, chartData, "injDefects", phase);
        writeChart(req, resp, chartData, "injByPhase");
        writeChart(req, resp, chartData, "injToDate");

        for (String phase : chartData.getPhases(PhaseType.Appraisal,
            PhaseType.Failure))
            writeChart(req, resp, chartData, "remDefects", phase);
        if (SHOW_ESCAPE_CHARTS)
            writeChart(req, resp, chartData, "escapes");

        List<String> fail = chartData.getPhases(PhaseType.Failure);
        for (int i = 1; i < fail.size(); i++)
            writeChart(req, resp, chartData, "remScatter", fail.get(i - 1),
                fail.get(i));

        if (SHOW_ESCAPE_CHARTS && !fail.isEmpty())
            writeChart(req, resp, chartData, "escapeScatter",
                fail.get(fail.size() - 1));

        writeChart(req, resp, chartData, "remByPhase");
        writeChart(req, resp, chartData, "remToDate");

        writeChart(req, resp, chartData, "injRates");
        writeChart(req, resp, chartData, "remRates");
        writeChart(req, resp, chartData, "drl");
    }


    @Override
    protected Map invokeChart(Entry<Chart, Method> chart, ChartData chartData)
            throws ServletException {
        Map result = super.invokeChart(chart, chartData);

        if (result != null && result.remove("isDensity") != null
                && chartData.isTimeUnits())
            result.put("headerComment", resources.format(
                "Hours_Units_Density_Comment_FMT",
                chartData.sizeDensityMultiplier));

        return result;
    }


    @Chart(id = "totalDefects", type = "line", //
    titleKey = "Defects.Total_Title", format = "isDensity=t")
    public ResultSet getTotalDefectDensity(ChartData chartData) {
        ResultSet data = chartData.getEnactmentResultSet(1);
        setDensityColumnHeader(data, chartData);

        for (int row = data.numRows(); row > 0; row--) {
            Enactment e = (Enactment) data.getRowObj(row);
            int total = e.actualDefects(TOTAL, true);
            int unknown = e.actualDefects(UNKNOWN, true);
            double denom = getDenom(chartData, e, Denom.Density, null);
            data.setData(row, 1, num((total + unknown) / denom));
        }
        return data;
    }


    @Chart(id = "injDefects", type = "line", params = "phase", //
    titleKey = "Defects.Injected_Title_FMT", format = "isDensity=t")
    public ResultSet getInjectedDefectDensity(ChartData chartData) {
        return getSinglePhaseDefectDensity(chartData, false);
    }


    @Chart(id = "remDefects", type = "line", params = "phase", //
    titleKey = "Defects.Removed_Title_FMT", format = "isDensity=t")
    public ResultSet getRemovedDefectDensity(ChartData chartData) {
        return getSinglePhaseDefectDensity(chartData, true);
    }


    @Chart(id = "escapes", type = "line", //
    titleKey = "Defects.Escaped_Title", //
    format = "headerComment=${Defects.Escaped_Header_FMT}")
    public ResultSet getEscapedDefectDensity(ChartData chartData) {
        // request defect density for the "after development" pseudophase
        chartData.chartArgs = new String[] { Defect.AFTER_DEVELOPMENT };
        ResultSet data = getSinglePhaseDefectDensity(chartData, true);
        data.setColName(1, resources.format("Defects.Escape_Scatter.Axis_FMT", //
            chartData.getDensityStr()));

        // now store the name of the last workflow phase in the args, so it can
        // be used in the construction of chart headers/labels
        List<String> phases = chartData.getPhases();
        String lastPhase = phases.get(phases.size() - 1);
        chartData.chartArgs[0] = lastPhase;

        return data;
    }


    @Chart(id = "injByPhase", type = "area", //
    titleKey = "Defects.Phase_Injected_Title", //
    format = "stacked=pct\ncolorScheme=consistent", smallFmt = "hideLegend=t")
    public ResultSet getInjectedDefectsByPhase(ChartData chartData) {
        List<String> phases = chartData.getPhases();
        phases.add(0, Defect.BEFORE_DEVELOPMENT);
        ResultSet data = getDefectsByPhase(chartData, phases, false, Denom.None);
        data.setColName(1,
            resources.format("Workflow.Analysis.Before_FMT", phases.get(1)));
        return data;
    }


    @Chart(id = "remByPhase", type = "area", //
    titleKey = "Defects.Phase_Removed_Title", //
    format = "stacked=pct\ncolorScheme=consistent\nconsistentSkip=1", //
    smallFmt = "hideLegend=t")
    public ResultSet getRemovedDefectsByPhase(ChartData chartData) {
        List<String> phases = chartData.getPhases();
        phases.add(Defect.AFTER_DEVELOPMENT);
        ResultSet data = getDefectsByPhase(chartData, phases, true, Denom.None);
        String lastPhase = phases.get(phases.size() - 2);
        data.setColName(data.numCols(),
            resources.format("Workflow.Analysis.After_FMT", lastPhase));
        return data;
    }


    @Chart(id = "remScatter", type = "xy", params = { "phaseX", "phaseY" }, //
    titleKey = "Defects.Density_Scatter.Title_FMT", format = "isDensity=t")
    public ResultSet getDensityScatter(ChartData chartData) {
        List<String> phases = Arrays.asList(chartData.chartArgs);
        ResultSet data = getDefectsByPhase(chartData, phases, true,
            Denom.Density);
        String densityStr = chartData.getDensityStr();
        data.setColName(1, resources.format("Defects.Density_Scatter.Axis_FMT",
            phases.get(0), densityStr));
        data.setColName(2, resources.format("Defects.Density_Scatter.Axis_FMT",
            phases.get(1), densityStr));
        return data;
    }

    @Chart(id = "escapeScatter", type = "xy", params = "phase", //
    titleKey = "Defects.Escape_Scatter.Title_FMT", format = "isDensity=t")
    public ResultSet getEscapedDefectsScatter(ChartData chartData) {
        String lastFailurePhase = chartData.chartArgs[0];
        chartData.chartArgs = new String[] { lastFailurePhase,
                Defect.AFTER_DEVELOPMENT };
        ResultSet data = getDensityScatter(chartData);
        data.setColName(2, resources.format("Defects.Escape_Scatter.Axis_FMT", //
            chartData.getDensityStr()));
        return data;
    }


    @Chart(id = "injRates", type = "line", //
    titleKey = "Defects.Injection_Rate.Title", //
    format = "noSkipLegend=t\nunits=${Defects.Injection_Rate.Units}\n"
            + "headerComment=${Defects.Injection_Rate.Units}")
    public ResultSet getPhaseInjectionRates(ChartData chartData) {
        List<String> phases = chartData.getPhases(PhaseType.Construction);
        if (phases.isEmpty())
            return null;
        Collections.reverse(phases);
        ResultSet data = getDefectsByPhase(chartData, phases, false, Denom.Rate);
        return data;
    }


    @Chart(id = "remRates", type = "line", //
    titleKey = "Defects.Removal_Rate.Title", //
    format = "noSkipLegend=t\nunits=${Defects.Removal_Rate.Units}\n"
            + "headerComment=${Defects.Removal_Rate.Comment}")
    public ResultSet getPhaseRemovalRates(ChartData chartData) {
        List<String> phases = chartData.getPhases(PhaseType.Appraisal,
            PhaseType.Failure);
        if (phases.isEmpty())
            return null;
        Collections.reverse(phases);
        ResultSet data = getDefectsByPhase(chartData, phases, true, Denom.Rate);
        return data;
    }


    @Chart(id = "drl", type = "line", titleKey = "Defects.Leverage.Title", //
    format = "headerComment=${Defects.Leverage.Comment_FMT}")
    public ResultSet getDefectRemovalLeverage(ChartData chartData) {
        ResultSet data = getPhaseRemovalRates(chartData);
        if (data == null || data.numCols() < 2)
            return null;

        for (int row = data.numRows(); row > 0; row--) {
            double denom = ((DoubleData) data.getData(row, 1)).getDouble();
            for (int col = data.numCols(); col > 1; col--) {
                double val = ((DoubleData) data.getData(row, col)).getDouble();
                double drl = val / denom;
                data.setData(row, col, num(drl));
            }
            data.setData(row, 1, num(1));
        }

        // store the name of the last workflow phase in the args, so it can
        // be used in the construction of chart headers/labels
        chartData.chartArgs = new String[] { data.getColName(1) };

        return data;
    }


    @Chart(id = "injToDate", type = "pie", //
    titleKey = "Defects.Injected_By_Phase_Title", //
    format = "colorScheme=consistent", smallFmt = "hideLegend=t")
    public ResultSet getDefectsInjectedToDate(ChartData chartData) {
        ResultSet data = getTotalDefectsByPhaseToDate(chartData, false);
        data.setColName(1, getRes("Defects_Injected"));
        String firstPhase = data.getRowName(2);
        data.setRowName(1,
            resources.format("Workflow.Analysis.Before_FMT", firstPhase));
        return data;
    }


    @Chart(id = "remToDate", type = "pie", //
    titleKey = "Defects.Removed_By_Phase_Title", //
    format = "colorScheme=consistent\nconsistentSkip=1", //
    smallFmt = "hideLegend=t")
    public ResultSet getDefectsRemovedToDate(ChartData chartData) {
        ResultSet data = getTotalDefectsByPhaseToDate(chartData, true);
        data.setColName(1, getRes("Defects_Removed"));
        String lastPhase = data.getRowName(data.numRows() - 1);
        data.setRowName(data.numRows(),
            resources.format("Workflow.Analysis.After_FMT", lastPhase));
        return data;
    }


    private ResultSet getTotalDefectsByPhaseToDate(ChartData chartData,
            boolean removed) {
        Map<String, DataPair>[] raw = chartData.histData.getDefectsByPhase();
        Map<String, DataPair> counts = raw[removed ? WorkflowHistDataHelper.REM
                : WorkflowHistDataHelper.INJ];
        double total = counts.remove(TOTAL).actual;
        List<String> phases = new ArrayList<String>(counts.keySet());

        ResultSet data = new ResultSet(phases.size(), 2);
        data.setColName(0, getRes("Phase"));
        data.setColName(2, getRes("Percent_Units"));
        data.setFormat(2, "100%");

        for (int row = phases.size(); row > 0; row--) {
            String phase = (String) phases.get(row - 1);
            data.setRowName(row, phase);
            data.setData(row, 1, num(counts.get(phase).actual));
            data.setData(row, 2, num(counts.get(phase).actual / total));
        }

        return data;
    }

    private ResultSet getSinglePhaseDefectDensity(ChartData chartData,
            boolean removed) {
        List<String> phase = Collections.singletonList(chartData.chartArgs[0]);
        ResultSet data = getDefectsByPhase(chartData, phase, removed,
            Denom.Density);
        setDensityColumnHeader(data, chartData);
        return data;
    }

    private void setDensityColumnHeader(ResultSet data, ChartData chartData) {
        String label = resources.format("Defects.Density_Units_FMT",
            chartData.getDensityStr());
        data.setColName(1, label);
    }

    private ResultSet getDefectsByPhase(ChartData chartData,
            List<String> phases, boolean removed, Denom denomType) {
        ResultSet data = chartData.getEnactmentResultSet(phases.size());
        writeDefectsByPhase(chartData, phases, removed, denomType, data, 1);
        return data;
    }

    public static void writePhaseDefectDensity(ChartData chartData,
            ResultSet data, int col, String... phases) {
        writeDefectsByPhase(chartData, Arrays.asList(phases), true,
            Denom.Density, data, col);
    }

    private static void writeDefectsByPhase(ChartData chartData,
            List<String> phases, boolean removed, Denom denomType,
            ResultSet data, int firstCol) {
        for (int col = phases.size(); col-- > 0;) {
            String phase = phases.get(col);
            data.setColName(col + firstCol, phase);
            for (int row = data.numRows(); row > 0; row--) {
                Enactment e = (Enactment) data.getRowObj(row);
                int count = e.actualDefects(phase, removed);
                double denom = getDenom(chartData, e, denomType, phase);
                data.setData(row, col + firstCol, num(count / denom));
            }
        }
    }

    private static double getDenom(ChartData chartData, Enactment e,
            Denom type, String phase) {
        switch (type) {
        case Density:
            double size;
            if (chartData.isTimeUnits())
                size = e.actualTime() / 60;
            else
                size = e.actualSize(chartData.primarySizeUnits);
            return size / chartData.sizeDensityMultiplier;

        case Rate:
            return e.actualTime(phase) / 60;

        case None:
        default:
            return 1;
        }
    }

    private enum Denom {
        None, Density, Rate
    }

    private static final String TOTAL = WorkflowHistDataHelper.TOTAL_PHASE_KEY;

    private static final String UNKNOWN = WorkflowHistDataHelper.UNKNOWN_PHASE_KEY;

    private static final boolean SHOW_ESCAPE_CHARTS = false;

}
