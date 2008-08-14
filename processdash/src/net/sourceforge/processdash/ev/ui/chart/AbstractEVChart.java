// Copyright (C) 2003-2008 Tuma Solutions, LLC
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Paint;
import java.awt.Stroke;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.MissingResourceException;

import net.sourceforge.processdash.ev.EVSchedule;
import net.sourceforge.processdash.ev.EVTaskFilter;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.ui.EVSnippetEnvironment;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.snippet.SnippetWidget;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.XYSeriesLabelGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;


public abstract class AbstractEVChart implements SnippetWidget {

    // The properties used for the axis labels
    private static final String X_AXIS_LABEL = "X_Axis_Label";
    private static final String Y_AXIS_LABEL = "Y_Axis_Label";

    private static Resources resources = Resources.getDashBundle("EV.Chart");

    protected abstract XYDataset createDataset(Map env, Map params);

    private ChartPanel buildChart(XYDataset data,
                                  String xLabel,
                                  String yLabel) {
        data = getAdjustedData(data);
        JFreeChart chart = createChart(data);
        adjustPlot(chart.getXYPlot(), xLabel, yLabel);
        ChartPanel panel = getChartPanel(chart, data);

        return panel;
    }

    /** This method has to be overridden to perform changes on the data
        before creating the chart */
    protected XYDataset getAdjustedData(XYDataset data) { return data; }

    protected void adjustPlot(XYPlot plot, String xLabel, String yLabel) {
        setPlotAxisLabels(plot, xLabel, yLabel);
    }

    /** This method has to be overridden to return the appropriate chart panel */
    protected abstract ChartPanel getChartPanel(JFreeChart chart, XYDataset data);

    protected JFreeChart createChart(XYDataset data) {
        JFreeChart chart = getChartObject(data);
        chart.getXYPlot().setRenderer(createRenderer(chart));
        return chart;
    }

    /** This method has to be overridden to return the appropriate JFreeChart
        chart */
    protected abstract JFreeChart getChartObject(XYDataset data);
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
    protected abstract XYToolTipGenerator getTooltipGenerator();

    protected void setPlotAxisLabels(XYPlot plot,
                                     String xLabel,
                                     String yLabel) {

        if (xLabel != null && xLabel.length() != 0)
            plot.getDomainAxis().setLabel(xLabel);

        if (yLabel != null && yLabel.length() != 0)
            plot.getRangeAxis().setLabel(yLabel);
    }

    public Component getWidgetComponent(Map environment, Map parameters) {
        XYDataset dataset = createDataset(environment, parameters);
        String xLabel = getAxisLabel(environment, parameters, X_AXIS_LABEL);
        String yLabel = getAxisLabel(environment, parameters, Y_AXIS_LABEL);
        return buildChart(dataset, xLabel, yLabel);
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

    private Resources getResources(Map environment) {
        return (Resources) environment.get(EVSnippetEnvironment.RESOURCES);
    }

    protected EVSchedule getSchedule(Map environment) {
        return (EVSchedule) environment.get(EVSnippetEnvironment.SCHEDULE_KEY);
    }

    protected EVTaskList getTaskList(Map environment) {
        return (EVTaskList) environment.get(EVSnippetEnvironment.TASK_LIST_KEY);
    }

    protected EVTaskFilter getTaskFilter(Map environment) {
        return (EVTaskFilter) environment.get(EVSnippetEnvironment.TASK_FILTER_KEY);
    }

    public static class SeriesNameGenerator implements XYSeriesLabelGenerator {

        public String generateLabel(XYDataset dataset, int series) {
            return getNameForSeries(dataset, series);
        }

    }

    protected static final Map<Comparable, Paint> SERIES_PAINTS = new HashMap();
    protected static final Map<Comparable, Stroke> SERIES_STROKES = new HashMap();
    private static final Map<Comparable, String> SERIES_NAMES = new HashMap();
    static {
        SERIES_PAINTS.put("Baseline", new Color(159, 141, 114));
        SERIES_PAINTS.put("Plan", Color.red);
        SERIES_PAINTS.put("Replan", Color.red);
        SERIES_PAINTS.put("Actual", Color.blue);
        SERIES_PAINTS.put("Total_Cost", Color.red);
        SERIES_PAINTS.put("Completion_Date", Color.red);
        SERIES_PAINTS.put("Forecast", Color.green);
        SERIES_PAINTS.put("Optimized_Forecast", Color.orange);

        // The renderer used for this chart automatically chooses the right
        //  color for this series
        SERIES_PAINTS.put("Completed_Task", null);

        SERIES_PAINTS.put("Plan_Value", Color.red);
        SERIES_PAINTS.put("Actual_Value", Color.blue);
        SERIES_PAINTS.put("Actual_Cost", Color.green);
        SERIES_PAINTS.put("Actual_Time", Color.orange);

        BasicStroke dashed = new BasicStroke(1.0f, BasicStroke.CAP_SQUARE,
                BasicStroke.JOIN_MITER, 10.0f, new float[] { 10.0f, 5.0f },
                0.0f);
        SERIES_STROKES.put("Schedule.Replan_Label", dashed);

        for (Iterator i = SERIES_PAINTS.keySet().iterator(); i.hasNext();) {
            String seriesKey = (String) i.next();
            String seriesName = getResources().getString("Schedule." + seriesKey
                + "_Label");
            SERIES_NAMES.put(seriesKey, seriesName);
        }
    }

    public static String getNameForSeries(XYDataset d, int seriesNum) {
        String seriesKey = d.getSeriesKey(seriesNum).toString();
        String result = SERIES_NAMES.get(seriesKey);
        if (result != null)
            return result;
        else
            return seriesKey;
    }

    public static Resources getResources() {
        return resources;
    }
}
