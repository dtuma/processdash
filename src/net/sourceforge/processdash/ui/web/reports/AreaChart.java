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

import java.awt.Color;
import java.awt.Paint;
import java.text.DecimalFormat;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.StackedAreaRenderer;
import org.jfree.data.category.CategoryDataset;

import net.sourceforge.processdash.ui.web.CGIChartBase;

public class AreaChart extends CGIChartBase {

    /** Create a line chart. */
    public JFreeChart createChart() {
        JFreeChart chart;
        CategoryDataset catData = data.catDataSource();

        Object stacked = parameters.get("stacked");
        if (stacked != null) {
            chart = ChartFactory.createStackedAreaChart(null, null, null,
                catData, PlotOrientation.VERTICAL, true, true, false);
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
            chart = ChartFactory.createAreaChart(null, null, null, catData,
                PlotOrientation.VERTICAL, true, true, false);
        }

        setupCategoryChart(chart);

        Object colorScheme = parameters.get("colorScheme");
        if ("consistent".equals(colorScheme))
            configureConsistentColors(chart.getCategoryPlot(), catData);
        else if (parameters.containsKey("c1"))
            configureIndividualColors(chart.getCategoryPlot(), catData);

        return chart;
    }

    private void configureConsistentColors(CategoryPlot plot,
            CategoryDataset catData) {
        DefaultDrawingSupplier s = new DefaultDrawingSupplier();

        String skip = getParameter("consistentSkip");
        if (skip != null)
            for (int i = Integer.parseInt(skip); i-- > 0;)
                s.getNextPaint();

        CategoryItemRenderer rend = plot.getRenderer();
        for (int i = 0; i < catData.getRowCount(); i++) {
            Paint paint = s.getNextPaint();
            rend.setSeriesPaint(i, paint);
        }
    }

    private void configureIndividualColors(CategoryPlot plot,
            CategoryDataset catData) {
        CategoryItemRenderer rend = plot.getRenderer();
        for (int i = 0; i < catData.getRowCount(); i++) {
            String colorKey = "c" + (i + 1);
            String color = getParameter(colorKey);
            if (color != null)
                rend.setSeriesPaint(i, Color.decode("#" + color));
        }
    }

}
