// Copyright (C) 2008 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ev.ui.chart;

import java.text.DateFormat;

import net.sourceforge.processdash.ui.lib.chart.XYDatasetFilter;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYDataset;



public abstract class AbstractEVTimeSeriesChart extends AbstractEVChart {

    @Override
    protected ChartPanel buildChart(XYDataset data,
                                    String xLabel,
                                    String yLabel) {
        XYDatasetFilter filteredData = new XYDatasetFilter(data);
        JFreeChart chart = createChart(filteredData);

        if (xLabel != null && xLabel.length() != 0)
            chart.getXYPlot().getDomainAxis().setLabel(xLabel);

        if (yLabel != null && yLabel.length() != 0)
            chart.getXYPlot().getRangeAxis().setLabel(yLabel);

        return new EVHiddenOrShownSeriesXYChartPanel(chart, filteredData, yLabel);
    }

    public static JFreeChart createChart(XYDataset data) {
        JFreeChart chart = ChartFactory.createTimeSeriesChart
                    (null, null, null, data, true, true, false);
        chart.getXYPlot().setRenderer(createRenderer());
        return chart;
    }

    protected static RangeXYItemRenderer createRenderer() {
        RangeXYItemRenderer renderer = new RangeXYItemRenderer();
        renderer.putAllSeriesPaints(SERIES_PAINTS);
        renderer.putAllSeriesStrokes(SERIES_STROKES);
        renderer.setLegendItemLabelGenerator(new SeriesNameGenerator());
        renderer.setBaseToolTipGenerator(new EVTimeSeriesTooltipGenerator());
        return renderer;
    }

    public static class EVTimeSeriesTooltipGenerator extends EVXYToolTipGenerator {

        public EVTimeSeriesTooltipGenerator() {
            super(DateFormat.getDateInstance(DateFormat.SHORT),
                  getNumberFormat());
        }

    }
}
