// Copyright (C) 2003-2007 Tuma Solutions, LLC
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
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.text.DecimalFormatSymbols;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import net.sourceforge.processdash.ev.EVMetrics;
import net.sourceforge.processdash.ev.EVSchedule;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.ui.lib.EVDatasetFilter;
import net.sourceforge.processdash.ui.lib.IDSeriesDataset;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.Legend;
import org.jfree.chart.axis.Axis;
import org.jfree.data.DatasetChangeEvent;
import org.jfree.data.DatasetChangeListener;
import org.jfree.data.XYDataset;


public class TaskScheduleChart extends JFrame
    implements EVTaskList.RecalcListener, ComponentListener {

    EVTaskList taskList;
    EVSchedule schedule;
    JTabbedPane tabPane;

    static Resources resources = Resources.getDashBundle("EV.Chart");

    public TaskScheduleChart(EVTaskList tl) {
        super(resources.format("Window_Title_FMT", tl.getDisplayName()));
        PCSH.enableHelpKey(this, "UsingTaskSchedule.chart");
        setIconImage(DashboardIconFactory.getWindowIconImage());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        taskList = tl;
        taskList.addRecalcListener(this);
        schedule = taskList.getSchedule();

        tabPane = new JTabbedPane();
        tabPane.addTab(TAB_NAMES[FULL][0], buildValueChart());
        tabPane.addTab(TAB_NAMES[FULL][1], buildTimeChart());
        tabPane.addTab(TAB_NAMES[FULL][2], buildCombinedChart());
        tabPane.addTab(TAB_NAMES[FULL][3], buildStatsTable());
        tabPane.addComponentListener(this);

        getContentPane().add(tabPane);
        pack();
        adjustTabNames(getWidth());
        show();
    }
    public void dispose() {
        super.dispose();
        taskList.removeRecalcListener(this);
    }
    public void evRecalculated(EventObject e) {}

    private ChartPanel buildTimeChart() {
        return buildChart(schedule.getTimeChartData(), UNITS[1]);
    }

    private ChartPanel buildValueChart() {
        return buildChart(schedule.getValueChartData(), UNITS[0]);
    }

    private ChartPanel buildCombinedChart() {
        return buildChart(schedule.getCombinedChartData(), UNITS[2]);
    }

    private ChartPanel buildChart(XYDataset data, String units) {
        EVDatasetFilter filteredData = new EVDatasetFilter(data);
        JFreeChart chart = createChart(filteredData);
        if (units != null && units.length() != 0)
            chart.getXYPlot().getRangeAxis().setLabel(units);
        charts[numCharts]    = chart;
        legends[numCharts++] = chart.getLegend();
        return new EVChartPanel(chart, filteredData);
    }

    public static JFreeChart createChart(XYDataset data) {
        JFreeChart chart = ChartFactory.createTimeSeriesChart
                    (null, null, null, data, true, true, false);
        RangeXYItemRenderer renderer = new RangeXYItemRenderer();
        configureRenderer(data, renderer);
        chart.getXYPlot().setRenderer(renderer);
        return chart;
    }
    private static void configureRenderer(XYDataset xyData,
            RangeXYItemRenderer renderer) {
        if (xyData instanceof IDSeriesDataset) {
            IDSeriesDataset data = (IDSeriesDataset) xyData;
            for (int i = 0;  i < data.getSeriesCount();  i++) {
                String seriesID = data.getSeriesID(i);
                Paint p = (Paint) getPrefForSeries(SERIES_PAINTS, seriesID);
                if (p != null) renderer.setSeriesPaint(i, p);
                Stroke s = (Stroke) getPrefForSeries(SERIES_STROKES, seriesID);
                if (s != null) renderer.setSeriesStroke(i, s);
            }
        }
    }

    private static final Map SERIES_PAINTS = new HashMap();
    private static final Map SERIES_STROKES = new HashMap();
    static {
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
    }
    private static Object getPrefForSeries(Map prefs, String seriesID) {
        for (Iterator i = prefs.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            String id = (String) e.getKey();
            if (id.equals(seriesID))
                return e.getValue();
        }
        return null;
    }

    JFreeChart charts[] = new JFreeChart[3];
    Legend legends[] = new Legend[3];
    int numCharts = 0;

    private Component buildStatsTable() {
        EVMetrics m = schedule.getMetrics();

        JTable table = new JTable(m);
        table.removeColumn(table.getColumnModel().getColumn(3));
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(100, 100));

        DescriptionPane descr =
            new DescriptionPane(m, table.getSelectionModel());

        Box result = Box.createVerticalBox();
        result.add(scrollPane);
        result.add(Box.createVerticalStrut(2));
        result.add(descr);
        return result;
    }





    public void componentMoved(ComponentEvent e) {}
    public void componentShown(ComponentEvent e) {}
    public void componentHidden(ComponentEvent e) {}
    public void componentResized(ComponentEvent e) {
        adjustTabNames(tabPane.getWidth());
    }


    private static final int FULL = 2;
    private static final int MED = 1;
    private static final int SHORT = 0;
    private int currentStyle = FULL;
    private static int MED_WINDOW_WIDTH = 325;
    private static int SHORT_WINDOW_WIDTH = 170;

    private static final String[][] TAB_NAMES;
    private static final String[] UNITS;
    static {
        String[] colKeys = new String[]
            { "Earned_Value_Chart", "Direct_Hours_Chart",
                  "Combined_Chart", "Statistics" };
        TAB_NAMES = new String[3][0];
        TAB_NAMES[FULL] = resources.getStrings("Tabs.", colKeys, ".Full_Name");
        TAB_NAMES[MED]  = resources.getStrings("Tabs.", colKeys, ".Med_Name");
        TAB_NAMES[SHORT]= resources.getStrings("Tabs.", colKeys, ".Short_Name");
        String[] chartColKeys = new String[3];
        System.arraycopy(colKeys, 0, chartColKeys, 0, 3);
        UNITS = resources.getStrings("Tabs.", chartColKeys, ".Units");

        if ("CURRENCY".equals(TAB_NAMES[SHORT][1]))
            TAB_NAMES[SHORT][1] =
                (new DecimalFormatSymbols()).getCurrencySymbol();
        try {
            MED_WINDOW_WIDTH = Integer.parseInt
                (resources.getString("Window_Width_Med_Name"));
        } catch (NumberFormatException nfe) {}
        try {
            SHORT_WINDOW_WIDTH = Integer.parseInt
                (resources.getString("Window_Width_Short_Name"));
        } catch (NumberFormatException nfe) {}
    }

    private void adjustTabNames(int width) {
        int style;
        if (width > MED_WINDOW_WIDTH)         style = FULL;
        else if (width > SHORT_WINDOW_WIDTH)  style = MED;
        else                                  style = SHORT;
        synchronized (this) {
            if (style == currentStyle) return;
            currentStyle = style;
        }

        for (int i=TAB_NAMES[style].length;   i-- > 0; )
            tabPane.setTitleAt(i, TAB_NAMES[style][i]);
        for (int i=0;   i < charts.length;   i++) {
            charts[i].setLegend(style != FULL ? null : legends[i]);
            adjustAxis(charts[i].getXYPlot().getRangeAxis(),
                       style != FULL, UNITS[i]);
            adjustAxis(charts[i].getXYPlot().getDomainAxis(),
                       style == SHORT, null);
        }
    }
    private void adjustAxis(Axis a, boolean chromeless, String units) {
        a.setTickLabelsVisible(!chromeless);
        a.setTickMarksVisible(!chromeless);
        a.setLabel(chromeless ? null : units);
    }


    private class EVChartPanel extends ChartPanel {
        private EVDatasetFilter filteredData;
        public EVChartPanel(JFreeChart chart, EVDatasetFilter filteredData) {
            super(chart);
            setMouseZoomable(true, false);

            this.filteredData = filteredData;
            getPopupMenu().insert(new JPopupMenu.Separator(), 0);
            filteredData.getSourceDataset().addChangeListener(
                    new DatasetChangeListener() {
                        public void datasetChanged(DatasetChangeEvent event) {
                            reloadSeriesMenus();
                        }});
            reloadSeriesMenus();
        }
        private void reloadSeriesMenus() {
            JPopupMenu menu = getPopupMenu();
            while (menu.getComponent(0) instanceof ShowChartLineMenuItem)
                menu.remove(0);
            XYDataset data = filteredData.getSourceDataset();
            RangeXYItemRenderer renderer = (RangeXYItemRenderer) getChart()
                    .getXYPlot().getRenderer();
            for (int i = data.getSeriesCount();   i-- > 0; )
                menu.insert(new ShowChartLineMenuItem(filteredData, renderer,
                        i), 0);
        }
    }
    private class ShowChartLineMenuItem extends JCheckBoxMenuItem implements
            ActionListener {
        private EVDatasetFilter data;
        private RangeXYItemRenderer renderer;
        private int seriesNum;
        public ShowChartLineMenuItem(EVDatasetFilter data,
                RangeXYItemRenderer renderer, int seriesNum) {
            this.data = data;
            this.renderer = renderer;
            this.seriesNum = seriesNum;
            String seriesName = data.getSourceDataset().getSeriesName(seriesNum);
            setText(resources.format("Show_Line_FMT", seriesName));
            setSelected(data.isSeriesHidden(seriesNum) == false);
            addActionListener(this);
        }
        public void actionPerformed(ActionEvent e) {
            data.setSeriesHidden(seriesNum, isSelected() == false);
            configureRenderer(data, renderer);
        }
    }



    private class DescriptionPane extends JTextArea
        implements ListSelectionListener, TableModelListener
    {
        EVMetrics metrics;
        ListSelectionModel selectionModel;
        public DescriptionPane(EVMetrics m, ListSelectionModel sm) {
            super(resources.getString("Choose_Metric_Instruction"));
            setBackground(null);
            setLineWrap(true); setWrapStyleWord(true); setEditable(false);
            doResize();
            metrics = m;
            metrics.addTableModelListener(this);
            selectionModel = sm;
            selectionModel.addListSelectionListener(this);
        }

        public void valueChanged(ListSelectionEvent e) { refreshText(); }
        public void tableChanged(TableModelEvent e)    { refreshText(); }
        public void refreshText() {
            String descr = (String) metrics.getValueAt
                (selectionModel.getMinSelectionIndex(), EVMetrics.FULL);
            setText(descr);
            doResize();
        }
        private void doResize() {
            Dimension d = getPreferredSize();
            setMinimumSize(d);
            d.width = 10000;
            setMaximumSize(d);
        }
    }

}
