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


package net.sourceforge.processdash.ev.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;

import net.sourceforge.processdash.ev.EVSchedule;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.ToolTipTimingCustomizer;
import net.sourceforge.processdash.ui.lib.chart.XYDatasetFilter;
import net.sourceforge.processdash.ui.snippet.SnippetWidget;
import net.sourceforge.processdash.util.Disposable;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.labels.XYSeriesLabelGenerator;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.xy.XYDataset;


public abstract class AbstractEVChart implements SnippetWidget {

    protected static Resources resources = Resources.getDashBundle("EV.Chart");

    protected abstract XYDataset createDataset(Map env, Map params);

    public Component getWidgetComponent(Map environment, Map parameters) {
        XYDataset dataset = createDataset(environment, parameters);
        String units = getUnits(environment, parameters);
        return buildChart(dataset, units);
    }

    protected String getUnits(Map environment, Map parameters) {
        Resources res = (Resources) environment
                .get(EVSnippetEnvironment.RESOURCES);
        return res.getString("Chart_Units");
    }

    protected ChartPanel buildChart(XYDataset data, String units) {
        XYDatasetFilter filteredData = new XYDatasetFilter(data);
        JFreeChart chart = createChart(filteredData);
        if (units != null && units.length() != 0)
            chart.getXYPlot().getRangeAxis().setLabel(units);
        return new EVChartPanel(chart, filteredData, units);
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
        renderer.setBaseToolTipGenerator(new TooltipGenerator());
        return renderer;
    }

    protected EVSchedule getSchedule(Map environment) {
        return (EVSchedule) environment.get(EVSnippetEnvironment.SCHEDULE_KEY);
    }

    protected static class SeriesNameGenerator implements XYSeriesLabelGenerator {

        public String generateLabel(XYDataset dataset, int series) {
            return getNameForSeries(dataset, series);
        }

    }

    protected static class TooltipGenerator extends StandardXYToolTipGenerator {

        public TooltipGenerator() {
            super("{0}: ({1}, {2})",
                    DateFormat.getDateInstance(DateFormat.SHORT),
                    getNumberFormat());
        }

        static NumberFormat getNumberFormat() {
            NumberFormat result = NumberFormat.getInstance();
            result.setMaximumFractionDigits(1);
            return result;
        }

        @Override
        protected Object[] createItemArray(XYDataset dataset, int series,
                int item) {
            Object[] result = super.createItemArray(dataset, series, item);
            result[0] = getNameForSeries(dataset, series);
            return result;
        }

    }

    private static final Map<Comparable, Paint> SERIES_PAINTS = new HashMap();
    private static final Map<Comparable, Stroke> SERIES_STROKES = new HashMap();
    private static final Map<Comparable, String> SERIES_NAMES = new HashMap();
    static {
        SERIES_PAINTS.put("Baseline", new Color(159, 141, 114));
        SERIES_PAINTS.put("Plan", Color.red);
        SERIES_PAINTS.put("Replan", Color.red);
        SERIES_PAINTS.put("Actual", Color.blue);
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
            String seriesName = resources.getString("Schedule." + seriesKey
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

    private static final int FULL = 2;
    private static final int MED = 1;
    private static final int SHORT = 0;
    private static int MED_WINDOW_WIDTH = 325;
    private static int SHORT_WINDOW_WIDTH = 170;
    static {
        try {
            MED_WINDOW_WIDTH = Integer.parseInt
                (resources.getString("Window_Width_Med_Name"));
        } catch (NumberFormatException nfe) {}
        try {
            SHORT_WINDOW_WIDTH = Integer.parseInt
                (resources.getString("Window_Width_Short_Name"));
        } catch (NumberFormatException nfe) {}
    }


    private class EVChartPanel extends ChartPanel implements Disposable,
            DatasetChangeListener {

        private LegendTitle legend;
        private XYDatasetFilter filteredData;
        private String units;
        private int currentStyle;

        public EVChartPanel(JFreeChart chart, XYDatasetFilter filteredData,
                String units) {
            super(chart);
            setMouseZoomable(true, false);
            this.legend = getChart().getLegend();

            this.filteredData = filteredData;
            this.units = units;
            getPopupMenu().insert(new JPopupMenu.Separator(), 0);
            filteredData.getSourceDataset().addChangeListener(this);
            reloadSeriesMenus();
            ToolTipManager.sharedInstance().registerComponent(this);
            new ToolTipTimingCustomizer().install(this);
        }

        public void dispose() {
            filteredData.getSourceDataset().removeChangeListener(this);
            getChart().getXYPlot().setDataset(null);
        }

        public void datasetChanged(DatasetChangeEvent event) {
            reloadSeriesMenus();
        }

        private void reloadSeriesMenus() {
            JPopupMenu menu = getPopupMenu();
            while (menu.getComponent(0) instanceof ShowChartLineMenuItem)
                menu.remove(0);
            XYDataset data = filteredData.getSourceDataset();
            for (int i = data.getSeriesCount();   i-- > 0; )
                menu.insert(new ShowChartLineMenuItem(filteredData, i), 0);
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            adjustStyle(width);
            super.setBounds(x, y, width, height);
        }

        private void adjustStyle(int width) {
            int style;
            if (width > MED_WINDOW_WIDTH)         style = FULL;
            else if (width > SHORT_WINDOW_WIDTH)  style = MED;
            else                                  style = SHORT;

            if (style == currentStyle) return;
            currentStyle = style;

            JFreeChart chart = getChart();
            chart.removeLegend();
            if (style == FULL) chart.addLegend(legend);
            adjustAxis(chart.getXYPlot().getRangeAxis(), style != FULL, units);
            adjustAxis(chart.getXYPlot().getDomainAxis(), style == SHORT, null);
        }

        private void adjustAxis(Axis a, boolean chromeless, String units) {
            a.setTickLabelsVisible(!chromeless);
            a.setTickMarksVisible(!chromeless);
            a.setLabel(chromeless ? null : units);
        }

    }

    private class ShowChartLineMenuItem extends JCheckBoxMenuItem implements
            ActionListener {
        private XYDatasetFilter data;
        private int seriesNum;
        public ShowChartLineMenuItem(XYDatasetFilter data, int seriesNum) {
            this.data = data;
            this.seriesNum = seriesNum;
            String seriesName = getNameForSeries(data, seriesNum);
            setText(resources.format("Show_Line_FMT", seriesName));
            setSelected(data.isSeriesHidden(seriesNum) == false);
            addActionListener(this);
        }
        public void actionPerformed(ActionEvent e) {
            data.setSeriesHidden(seriesNum, isSelected() == false);
        }
    }

}
