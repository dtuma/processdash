// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.text.DecimalFormatSymbols;
import java.util.EventObject;

import javax.swing.Box;
import javax.swing.JFrame;
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

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.Legend;
import org.jfree.chart.axis.Axis;
import org.jfree.data.XYDataset;

import pspdash.DashboardIconFactory;
import pspdash.PCSH;
import pspdash.RangeXYItemRenderer;
import pspdash.Resources;

public class TaskScheduleChart extends JFrame
    implements EVTaskList.RecalcListener, ComponentListener {

    EVTaskList taskList;
    EVSchedule schedule;
    JTabbedPane tabPane;

    static Resources resources =
        Resources.getDashBundle("pspdash.TaskScheduleChart");

    public TaskScheduleChart(TaskScheduleDialog parent) {
        super(resources.format("Window_Title_FMT", parent.taskListName));
        PCSH.enableHelpKey(this, "UsingTaskSchedule.chart");
        setIconImage(DashboardIconFactory.getWindowIconImage());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        taskList = parent.model;
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
        return buildChart(schedule.getTimeChartData());
    }

    private ChartPanel buildValueChart() {
        return buildChart(schedule.getValueChartData());
    }

    private ChartPanel buildCombinedChart() {
        return buildChart(schedule.getCombinedChartData());
    }

    private ChartPanel buildChart(XYDataset data) {
        JFreeChart chart = createChart(data);
        charts[numCharts]    = chart;
        legends[numCharts++] = chart.getLegend();
        ChartPanel panel = new ChartPanel(chart);
        return panel;
    }

    public static JFreeChart createChart(XYDataset data) {
        JFreeChart chart = ChartFactory.createTimeSeriesChart
                    (null, null, null, data, true, true, false);
        RangeXYItemRenderer renderer = new RangeXYItemRenderer();
        renderer.setSeriesPaint(3, Color.orange);
        chart.getXYPlot().setRenderer(renderer);
        return chart;
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
    static {
        String[] colKeys = new String[]
            { "Earned_Value_Chart", "Direct_Hours_Chart",
                  "Combined_Chart", "Statistics" };
        TAB_NAMES = new String[3][0];
        TAB_NAMES[FULL] = resources.getStrings("Tab_Full_Name_", colKeys);
        TAB_NAMES[MED] = resources.getStrings("Tab_Med_Name_", colKeys);
        TAB_NAMES[SHORT] = resources.getStrings("Tab_Short_Name_", colKeys);
        if ("CURRENCY".equals(TAB_NAMES[SHORT][1]))
            TAB_NAMES[SHORT][1] =
                (new DecimalFormatSymbols()).getCurrencySymbol();
        try {
            MED_WINDOW_WIDTH = Integer.parseInt
                (resources.getString("Med_Name_Window_Width"));
        } catch (NumberFormatException nfe) {}
        try {
            SHORT_WINDOW_WIDTH = Integer.parseInt
                (resources.getString("Short_Name_Window_Width"));
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
                       style != FULL);
            adjustAxis(charts[i].getXYPlot().getDomainAxis(),
                       style == SHORT);
        }
    }
    private void adjustAxis(Axis a, boolean chromeless) {
        a.setTickLabelsVisible(!chromeless);
        a.setTickMarksVisible(!chromeless);
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
