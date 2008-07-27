// Copyright (C) 2003-2008 Tuma Solutions, LLC
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
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.ui.EVSnippetEnvironment;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.snippet.SnippetWidget;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.labels.XYSeriesLabelGenerator;
import org.jfree.data.xy.XYDataset;


public abstract class AbstractEVChart implements SnippetWidget {

    // The properties used for the axis labels
    private static final String X_AXIS_LABEL = "X_Axis_Label";
    private static final String Y_AXIS_LABEL = "Y_Axis_Label";

    private static Resources resources = Resources.getDashBundle("EV.Chart");

    protected abstract XYDataset createDataset(Map env, Map params);
    protected abstract ChartPanel buildChart(XYDataset data,
                                             String xLabel,
                                             String yLabel);

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

    protected String getUnits(Map environment, Map parameters) {
        String units = null;

        try{
            units = getResources(environment).getString("Chart_Units");
        } catch (MissingResourceException e) { }

        return units;
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
        SERIES_PAINTS.put("Completed_Task", Color.green);
        SERIES_PAINTS.put("Forecast", Color.green);
        SERIES_PAINTS.put("Optimized_Forecast", Color.orange);

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
