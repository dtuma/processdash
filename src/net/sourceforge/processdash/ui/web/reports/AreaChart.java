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

package net.sourceforge.processdash.ui.web.reports;

import java.text.DecimalFormat;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StackedAreaRenderer;

import net.sourceforge.processdash.ui.web.CGIChartBase;

public class AreaChart extends CGIChartBase {

    /** Create a line chart. */
    public JFreeChart createChart() {
        JFreeChart chart;

        Object stacked = parameters.get("stacked");
        if (stacked != null) {
            chart = ChartFactory.createStackedAreaChart(null, null, null,
                data.catDataSource(), PlotOrientation.VERTICAL, true, true,
                false);
            if ("pct".equals(stacked)) {
                ((StackedAreaRenderer) chart.getCategoryPlot().getRenderer())
                        .setRenderAsPercentages(true);
                DecimalFormat fmt = new DecimalFormat();
                fmt.setMultiplier(100);
                ((NumberAxis) chart.getCategoryPlot().getRangeAxis())
                        .setNumberFormatOverride(fmt);
                if (parameters.get("units") == null)
                    parameters.put("units", "%");
            }

        } else {
            chart = ChartFactory.createAreaChart(null, null, null,
                data.catDataSource(), PlotOrientation.VERTICAL, true, true,
                false);
        }

        setupCategoryChart(chart);

        return chart;
    }

}
