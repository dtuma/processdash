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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.processdash.data.util.ResultSet;
import net.sourceforge.processdash.tool.db.WorkflowHistDataHelper.Enactment;

public class PlanAnalysisPage extends AnalysisPage {

    public PlanAnalysisPage() {
        super("workflowPlan", "Plan.Title");
    }


    @Override
    protected void writeHtmlContent(HttpServletRequest req,
            HttpServletResponse resp, ChartData chartData)
            throws ServletException, IOException {

        if (isLegitSize(chartData.primarySizeUnits))
            writeChart(req, resp, chartData, "estErrScatter",
                chartData.primarySizeUnits);

        writeChart(req, resp, chartData, "totalTime");
        writeChart(req, resp, chartData, "timeEstErr");
        writeChart(req, resp, chartData, "planVsActualTime");

        for (String units : chartData.histData.getSizeUnits()) {
            if (isLegitSize(units)) {
                writeChart(req, resp, chartData, "size", units);
                writeChart(req, resp, chartData, "sizeEstErr", units);
                writeChart(req, resp, chartData, "planVsActualSize", units);
                writeChart(req, resp, chartData, "sizeVsTime", units);
            }
        }
    }

    private boolean isLegitSize(String units) {
        return units != null && !units.equalsIgnoreCase("hours")
                && !units.equalsIgnoreCase(getRes("Hours"));
    }


    @Chart(id = "size", type = "line", params = "units", //
    titleKey = "Plan.Size_Title_FMT")
    public ResultSet getActualSize(ChartData chartData) {
        String units = chartData.chartArgs[0];
        ResultSet data = chartData.getEnactmentResultSet(units);
        for (int row = data.numRows(); row > 0; row--) {
            Enactment e = (Enactment) data.getRowObj(row);
            data.setData(row, 1, num(e.actualSize(units)));
        }
        return data;
    }


    @Chart(id = "sizeEstErr", type = "line", params = "units", //
    titleKey = "Plan.Size_Estimating_Error_Title_FMT")
    public ResultSet getSizeEstimatingError(ChartData chartData) {
        String units = chartData.chartArgs[0];
        ResultSet data = chartData.getEnactmentResultSet("Plan.Percent_Error");
        for (int row = data.numRows(); row > 0; row--) {
            Enactment e = (Enactment) data.getRowObj(row);
            double plan = chartData.histData.getSize(e, units, false);
            double actual = e.actualSize(units);
            double err = (actual - plan) / plan;
            data.setData(row, 1, num(err));
        }
        data.setFormat(1, "100%");
        return data;
    }


    @Chart(id = "planVsActualSize", type = "xy", params = "units", //
    titleKey = "Plan.Size_Scatter_Title_FMT", format = "autoZero=none")
    public ResultSet getPlanVsActualSize(ChartData chartData) {
        String units = chartData.chartArgs[0];
        ResultSet data = chartData.getEnactmentResultSet(
            "Plan.Size_Scatter_Plan_FMT", "Plan.Size_Scatter_Actual_FMT");
        for (int row = data.numRows(); row > 0; row--) {
            Enactment e = (Enactment) data.getRowObj(row);
            double plan = chartData.histData.getSize(e, units, false);
            double actual = e.actualSize(units);
            data.setData(row, 1, num(plan));
            data.setData(row, 2, num(actual));
        }
        return data;
    }


    @Chart(id = "sizeVsTime", type = "xy", params = "units", //
    titleKey = "Plan.Size_Vs_Time_FMT", format = "autoZero=none")
    public ResultSet getSizeVsTime(ChartData chartData) {
        String units = chartData.chartArgs[0];
        ResultSet data = chartData.getEnactmentResultSet(
            "Plan.Size_Scatter_Actual_FMT", "Plan.Time_Scatter_Actual");
        for (int row = data.numRows(); row > 0; row--) {
            Enactment e = (Enactment) data.getRowObj(row);
            data.setData(row, 1, num(e.actualSize(units)));
            data.setData(row, 2, num(e.actualTime() / 60));
        }
        return data;
    }


    @Chart(id = "totalTime", type = "line", titleKey = "Plan.Time_Title")
    public ResultSet getTotalActualTime(ChartData chartData) {
        ResultSet data = chartData.getEnactmentResultSet("Hours");
        for (int row = data.numRows(); row > 0; row--) {
            Enactment e = (Enactment) data.getRowObj(row);
            data.setData(row, 1, num(e.actualTime() / 60));
        }
        return data;
    }


    @Chart(id = "timeEstErr", type = "line", //
    titleKey = "Plan.Time_Estimating_Error_Title")
    public ResultSet getTimeEstimatingError(ChartData chartData) {
        ResultSet data = chartData.getEnactmentResultSet("Plan.Percent_Error");
        for (int row = data.numRows(); row > 0; row--) {
            Enactment e = (Enactment) data.getRowObj(row);
            double plan = chartData.histData.getTime(e, null, false);
            double actual = e.actualTime();
            double err = (actual - plan) / plan;
            data.setData(row, 1, num(err));
        }
        data.setFormat(1, "100%");
        return data;
    }


    @Chart(id = "planVsActualTime", type = "xy", //
    titleKey = "Plan.Time_Scatter_Title", format = "autoZero=none")
    public ResultSet getPlanVsActualTime(ChartData chartData) {
        ResultSet data = chartData.getEnactmentResultSet(
            "Plan.Time_Scatter_Plan", "Plan.Time_Scatter_Actual");
        for (int row = data.numRows(); row > 0; row--) {
            Enactment e = (Enactment) data.getRowObj(row);
            double plan = chartData.histData.getTime(e, null, false);
            double actual = e.actualTime();
            data.setData(row, 1, num(plan / 60));
            data.setData(row, 2, num(actual / 60));
        }
        return data;
    }


    @Chart(id = "estErrScatter", type = "estErrorScatter", params = "units", //
    titleKey = "EstErrorScatterChart.Title", format = "skipUnitsCol=t")
    public ResultSet getEstErrorScatterChart(ChartData chartData) {
        ResultSet data = chartData.getEnactmentResultSet(
            "Plan.Size_Scatter_Plan_FMT", "Plan.Size_Scatter_Actual_FMT", //
            "EstErrorScatterChart.Size_Est_Error", //
            "Plan.Time_Scatter_Plan", "Plan.Time_Scatter_Actual", //
            "EstErrorScatterChart.Time_Est_Error");
        data.setFormat(3, "100%");
        data.setFormat(6, "100%");
        String units = chartData.chartArgs[0];

        for (int row = data.numRows(); row > 0; row--) {
            Enactment e = (Enactment) data.getRowObj(row);

            double planSize = chartData.histData.getSize(e, units, false);
            double actualSize = e.actualSize(units);
            double sizeErr = (actualSize - planSize) / planSize;
            data.setData(row, 1, num(planSize));
            data.setData(row, 2, num(actualSize));
            data.setData(row, 3, num(sizeErr));

            double planTime = chartData.histData.getTime(e, null, false);
            double actualTime = e.actualTime();
            double timeErr = (actualTime - planTime) / planTime;
            data.setData(row, 4, num(planTime / 60));
            data.setData(row, 5, num(actualTime / 60));
            data.setData(row, 6, num(timeErr));
        }

        return data;
    }
}
