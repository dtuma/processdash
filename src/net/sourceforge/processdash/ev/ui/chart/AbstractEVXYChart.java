// Copyright (C) 2008 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ev.ui.chart;

import java.util.Map;
import java.util.MissingResourceException;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;


public abstract class AbstractEVXYChart extends AbstractEVChart<XYDataset, XYPlot> {
    // The properties used for the axis labels
    private static final String X_AXIS_LABEL = "X_Axis_Label";
    private static final String Y_AXIS_LABEL = "Y_Axis_Label";

    @Override
    protected JFreeChart createChart(XYDataset data) {
        JFreeChart chart = null;
        chart = getXYChartObject(data);
        chart.getXYPlot().setRenderer(createRenderer(chart));

        return chart;
    }

    /** This method has to be overridden to return the appropriate JFreeChart
        chart */
    protected abstract JFreeChart getXYChartObject(XYDataset data);

    protected XYItemRenderer createRenderer(JFreeChart chart) {
        return chart.getXYPlot().getRenderer();
    }

    /** Used to create a chart RangeXYItemRenderer with the appropriate
         TooltipGenerator */
    protected RangeXYItemRenderer createRangeXYItemRenderer() {
        RangeXYItemRenderer renderer = new RangeXYItemRenderer();
        renderer.putAllSeriesPaints(SERIES_PAINTS);
        renderer.putAllSeriesStrokes(SERIES_STROKES);
        renderer.setLegendItemLabelGenerator(new SeriesNameGenerator());
        renderer.setBaseToolTipGenerator(getTooltipGenerator());
        return renderer;
    }
    /** Used to create a chart XYLineAndShapeRenderer with the appropriate
        tooltipGenerator */
    public static final boolean LINES = true;
    public static final boolean NO_LINES = false;
    public static final boolean SHAPES = true;
    public static final boolean NO_SHAPES = false;
    protected XYItemRenderer createXYLineAndShapeRenderer(boolean lines, boolean shapes) {
        XYItemRenderer renderer = new XYLineAndShapeRenderer(lines, shapes);
        renderer.setLegendItemLabelGenerator(new SeriesNameGenerator());
        renderer.setBaseToolTipGenerator(getTooltipGenerator());
        return renderer;
    }

    protected abstract XYToolTipGenerator getTooltipGenerator();

    @Override
    protected void adjustPlot(XYPlot plot, XYDataset data, Map environment, Map parameters) {
        String xLabel = getAxisLabel(environment, parameters, X_AXIS_LABEL);
        String yLabel = getAxisLabel(environment, parameters, Y_AXIS_LABEL);

        setPlotAxisLabels(plot, xLabel, yLabel);
    }

    private String getAxisLabel(Map environment,
                                Map parameters,
                                String axis) {
        String axisLabel = null;

        try {
            axisLabel = getResources(environment).getString(axis);
        } catch (MissingResourceException e) { }

        return axisLabel;
    }

    protected void setPlotAxisLabels(XYPlot plot,
                                     String xLabel,
                                     String yLabel) {

        if (xLabel != null && xLabel.length() != 0)
            plot.getDomainAxis().setLabel(xLabel);

        if (yLabel != null && yLabel.length() != 0)
            plot.getRangeAxis().setLabel(yLabel);
    }

}
