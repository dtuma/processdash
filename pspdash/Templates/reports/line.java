// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil

import com.jrefinery.chart.*;


public class line extends pspdash.CGIChartBase {

    /** Create a  line chart. */
    public JFreeChart createChart() {
        JFreeChart chart = JFreeChart.createLineChart(data.catDataSource());

        if (!chromeless) {
            String label = getSetting("xLabel");
            if (label == null) label = getSetting("h0");
            if (label == null) label = "Project/Task";
            HorizontalCategoryAxis hAxis = (HorizontalCategoryAxis)
                chart.getPlot().getAxis(Plot.HORIZONTAL_AXIS);
            hAxis.setLabel(label);
            String catLabels = getParameter("categoryLabels");
            if ("vertical".equalsIgnoreCase(catLabels))
                hAxis.setVerticalCategoryLabels(true);
            else if ("none".equalsIgnoreCase(catLabels))
                hAxis.setShowTickLabels(false);

            label = getSetting("yLabel");
            if (label == null && data.numCols() == 1)
                label = data.getColName(1);
            if (label == null) label = getSetting("units");
            if (label == null) label = "Value";
            chart.getPlot().getAxis(Plot.VERTICAL_AXIS).setLabel(label);
        }

        if (data.numCols() == 1) chart.setLegend(null);

        return chart;
    }

}
