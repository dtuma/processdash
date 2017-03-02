// Copyright (C) 2003-2017 Tuma Solutions, LLC
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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ev.EVSchedule;
import net.sourceforge.processdash.ev.EVTaskFilter;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.ui.EVSnippetEnvironment;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.http.TinyCGI;
import net.sourceforge.processdash.ui.snippet.SnippetWidget;
import net.sourceforge.processdash.ui.web.CGIChartBase;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.Disposable;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.XYSeriesLabelGenerator;
import org.jfree.chart.plot.Plot;
import org.jfree.data.general.Dataset;
import org.jfree.data.xy.XYDataset;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public abstract class AbstractEVChart<D extends Dataset, P extends Plot>
        implements SnippetWidget, HtmlEvChart, HelpAwareEvChart, Disposable {
    private static Resources resources = Resources.getDashBundle("EV.Chart");

    protected String helpId;
    protected String helpUri;
    protected Disposable disposableDataset;

    protected abstract D createDataset(Map env, Map params);

    public void setConfigElement(Element xml, String attrName) {
        NodeList helpNodes = xml.getElementsByTagName("help");
        if (helpNodes != null && helpNodes.getLength() == 1) {
            Element help = (Element) helpNodes.item(0);
            helpId = help.getAttribute("id");
            helpUri = help.getAttribute("uri");
        }
    }

    public String getHelpId() {
        return helpId;
    }

    public String getHelpUri() {
        return helpUri;
    }

    private ChartPanel buildChart(D data, Map environment, Map parameters) {
        data = getAdjustedData(data);
        JFreeChart chart = createChart(data);

        if (chart != null) {
            chart.getPlot().setNoDataMessage(resources.getString("No_Data_Message"));
            adjustPlot((P) chart.getPlot(), data, environment, parameters);
        }

        ChartPanel panel = getChartPanel(chart, data);
        panel.setInitialDelay(50);
        panel.setDismissDelay(60000);

        return panel;
    }

    /** This method has to be overridden to perform changes on the data
        before creating the chart */
    protected D getAdjustedData(D data) { return data; }

    protected void adjustPlot(P plot, D data, Map environment, Map parameters) { }

    protected ChartPanel getChartPanel(JFreeChart chart, D data) {
        return new ChartPanel(chart, false);
    }

    /** This method has to be overridden to return the appropriate JFreeChart
        chart */
    protected abstract JFreeChart createChart(D data);

    public Component getWidgetComponent(Map environment, Map parameters) {
        D dataset = createDataset(environment, parameters);
        if (dataset instanceof Disposable)
            disposableDataset = (Disposable) dataset;
        return buildChart(dataset, environment, parameters);
    }

    public void dispose() {
        if (disposableDataset != null)
            disposableDataset.dispose();
    }

    public void writeChartAsHtml(Writer out, Map environment, Map parameters)
            throws IOException {
        Map htmlEnv = new HashMap(environment);
        htmlEnv.put(EVSnippetEnvironment.HTML_OUTPUT_KEY, Boolean.TRUE);
        D dataset = createDataset(htmlEnv, parameters);
        ChartPanel chartPanel = buildChart(dataset, htmlEnv, parameters);
        JFreeChart chart = chartPanel.getChart();
        if (chart != null) {
            new HtmlWriter(out, parameters, chart).writeChartHtml();
        }
    }

    protected Resources getResources(Map environment) {
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

    protected DashboardContext getDashContext(Map environment) {
        return (DashboardContext) environment.get(TinyCGIBase.DASHBOARD_CONTEXT);
    }

    protected DashHierarchy getDashHierarchy(Map environment) {
        return (DashHierarchy) environment.get(TinyCGI.PSP_PROPERTIES);
    }

    protected DataRepository getDataRepository(Map environment) {
        return (DataRepository) environment.get(TinyCGIBase.DATA_REPOSITORY);
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
        SERIES_PAINTS.put("Replan", Color.pink);
        SERIES_PAINTS.put("Actual", Color.blue);
        SERIES_PAINTS.put("Total_Cost", Color.red);
        SERIES_PAINTS.put("Forecast", Color.green);
        SERIES_PAINTS.put("Optimized_Forecast", Color.orange);

        // The renderer used for those charts automatically chooses the right
        //  color for the series
        SERIES_PAINTS.put("Completed_Task", null);
        SERIES_PAINTS.put("Completed_Period", null);

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

    private List<ChangeListener> changeListeners;

    public void addChangeListener(ChangeListener l) {
        if (changeListeners == null)
            changeListeners = new Vector();
        changeListeners.add(l);
    }

    public void removeChangeListener(ChangeListener l) {
        if (changeListeners != null)
            changeListeners.remove(l);
    }

    protected void fireStateChanged() {
        if (changeListeners != null) {
            List<ChangeListener> listeners = new ArrayList<ChangeListener>(
                    changeListeners);
            ChangeEvent evt = new ChangeEvent(this);
            for (ChangeListener l : listeners)
                l.stateChanged(evt);
        }
    }

    protected void propertyChangeNotify(PropertyChangeEvent evt) {}

    protected PropertyChangeListener getPropertyChangeNotifier() {
        return new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                propertyChangeNotify(evt);
                fireStateChanged();
            }
        };
    }

    protected static class HtmlWriter extends CGIChartBase {
        private JFreeChart chart;
        public HtmlWriter(Writer out, Map params, JFreeChart chart) {
            if (out instanceof PrintWriter) {
                this.out = (PrintWriter) out;
            } else {
                this.out = new PrintWriter(out);
            }
            this.parameters = params;
            this.chart = chart;
        }
        @Override protected void buildData() {}
        @Override public JFreeChart createChart() { return chart; }
        @Override protected boolean isHtmlMode() { return true; }
        public void writeChartHtml() throws IOException { writeContents(); }
    }

}
