// Copyright (C) 2001-2014 Tuma Solutions, LLC
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


import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.data.xy.XYDataset;

import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.i18n.Translator;
import net.sourceforge.processdash.ui.web.CGIChartBase;



public class XYChart extends CGIChartBase {

    /** Create a scatter plot. */
    @Override
    public JFreeChart createChart() {
        JFreeChart chart;
        String xLabel = null, yLabel = null;
        if (!chromeless) {
            xLabel = Translator.translate(data.getColName(1));

            yLabel = getSetting("yLabel");
            if (yLabel == null && data.numCols() == 2)
                yLabel = data.getColName(2);
            if (yLabel == null) yLabel = getSetting("units");
            if (yLabel == null) yLabel = "Value";
            yLabel = Translator.translate(yLabel);
        }

        Object autoZero = parameters.get("autoZero");

        if ((data.numRows() > 0 && data.numCols() > 0 &&
             data.getData(1,1) instanceof DateData) ||
            parameters.get("xDate") != null)
            chart = ChartFactory.createTimeSeriesChart
                (null, xLabel, yLabel, data.xyDataSource(), true, true, false);
        else {
            XYDataset src = data.xyDataSource();
            chart = ChartFactory.createScatterPlot
                (null, xLabel, yLabel, src, PlotOrientation.VERTICAL,
                 true, true, false);
            if (src instanceof XYToolTipGenerator) {
                chart.getXYPlot().getRenderer().setBaseToolTipGenerator(
                    (XYToolTipGenerator) src);
            }

            String trendLine = getParameter("trend");
            if ("none".equalsIgnoreCase(trendLine))
                ;
            else if ("average".equalsIgnoreCase(trendLine))
                addTrendLine(chart, XYDataSourceTrendLine.getAverageLine(src,
                    0, autoZero != null && !autoZero.equals("y")));
            else
                addTrendLine(chart, XYDataSourceTrendLine.getRegressionLine(
                    src, 0, autoZero != null && !autoZero.equals("y")));
        }

        if (autoZero != null) {
            if (!"x".equals(autoZero))
                ((NumberAxis) chart.getXYPlot().getRangeAxis())
                        .setAutoRangeIncludesZero(true);
            if (!"y".equals(autoZero))
                ((NumberAxis) chart.getXYPlot().getDomainAxis())
                        .setAutoRangeIncludesZero(true);
        }

        if (data.numCols() == 2)
            chart.removeLegend();

        return chart;
    }

    private void addTrendLine(JFreeChart chart, XYDataset dataset) {
        XYPlot plot = chart.getXYPlot();
        plot.setDataset(1, dataset);
        plot.setRenderer
            (1, new StandardXYItemRenderer(StandardXYItemRenderer.LINES));
    }

    @Override
    public void massageParameters() {
        //parameters.put("order", parameters.get("d1"));
    }

    @Override
    protected Axis getAxis(JFreeChart chart, PlotOrientation dir) {
        try {
            XYPlot p = chart.getXYPlot();
            if (dir.equals(p.getOrientation()))
                return p.getRangeAxis();
            else
                return p.getDomainAxis();
        } catch (Exception e) {
            return null;
        }
    }

}
