// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil


package pspdash;

import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.*;
import java.text.DecimalFormatSymbols;
import java.util.EventObject;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import com.jrefinery.chart.*;
import com.jrefinery.chart.event.DataSourceChangeListener;
import com.jrefinery.chart.event.DataSourceChangeEvent;

public class TaskScheduleChart extends JFrame
    implements EVTaskList.RecalcListener, ComponentListener {

    EVTaskList taskList;
    EVSchedule schedule;
    JTabbedPane tabPane;

    public TaskScheduleChart(TaskScheduleDialog parent) {
        super("EV Chart - " + parent.taskListName);
        PCSH.enableHelpKey(this, "UsingTaskSchedule.chart");
        setIconImage(parent.frame.getIconImage());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        taskList = parent.model;
        taskList.addRecalcListener(this);
        schedule = taskList.getSchedule();

        tabPane = new JTabbedPane();
        tabPane.addTab("Earned Value", buildValueChart());
        tabPane.addTab("Direct Hours", buildTimeChart());
        tabPane.addTab("Combined",     buildCombinedChart());
        tabPane.addTab("Statistics",   buildStatsTable());
        tabPane.addComponentListener(this);

        getContentPane().add(tabPane);
        pack();
        show();
    }
    public void dispose() {
        super.dispose();
        taskList.removeRecalcListener(this);
    }
    public void evRecalculated(EventObject e) {}


    private JFreeChartPanel buildTimeChart() {
        return buildChart(schedule.getTimeChartData());
    }

    private JFreeChartPanel buildValueChart() {
        return buildChart(schedule.getValueChartData());
    }

    private JFreeChartPanel buildCombinedChart() {
        return buildChart(schedule.getCombinedChartData());
    }

    private JFreeChartPanel buildChart(XYDataSource data) {
        JFreeChart chart = JFreeChart.createTimeSeriesChart(data);
        chart.setTitle((Title) null);
        charts[numCharts]   = chart;
        legends[numCharts++] = chart.getLegend();
        data.addChangeListener(new DataChangeWrapper(chart));
        JFreeChartPanel panel = new JFreeChartPanel(chart);
        return panel;
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

    private static final String[][] tabNames = {
        { "%", (new DecimalFormatSymbols()).getCurrencySymbol(), "&", "#" },
        { "EV", "Hours", "Both", "Stats" },
        { "Earned Value", "Direct Hours", "Combined", "Statistics" }};
    private static final int FULL = 2;
    private static final int MED = 1;
    private static final int SHORT = 0;
    private int currentStyle = FULL;

    private void adjustTabNames(int width) {
        int style = (width > 325 ? FULL : (width > 170 ? MED : SHORT));
        synchronized (this) {
            if (style == currentStyle) return;
            currentStyle = style;
        }

        for (int i=tabNames[style].length;   i-- > 0; )
            tabPane.setTitleAt(i, tabNames[style][i]);
        for (int i=0;   i < charts.length;   i++) {
            charts[i].setLegend(style != FULL ? null : legends[i]);
            adjustAxis(charts[i].getPlot().getAxis(Plot.HORIZONTAL_AXIS),
                       style != FULL);
            adjustAxis(charts[i].getPlot().getAxis(Plot.VERTICAL_AXIS),
                       style == SHORT);
        }
    }
    private void adjustAxis(Axis a, boolean chromeless) {
        a.setShowTickLabels(!chromeless);
        a.setShowTickMarks(!chromeless);
    }


    private class DescriptionPane extends JTextArea
        implements ListSelectionListener, TableModelListener
    {
        EVMetrics metrics;
        ListSelectionModel selectionModel;
        public DescriptionPane(EVMetrics m, ListSelectionModel sm) {
            super("Select a metric above for more information...");
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



    // We shouldn't need to have this class, but the JFreeChart code
    // has a bug.  When the data source is modified, it fails to
    // correctly resize the chart axes.  It <b>will</b> resize if the
    // data source is replaced.  So rather than just registering for
    // change notification, we have to create this intermediary that
    // listens for data source changes and responds by resetting the
    // chart's data source.

    private class DataChangeWrapper implements DataSourceChangeListener {
        JFreeChart chart;
        public DataChangeWrapper(JFreeChart c) { chart = c; }
        public void dataSourceChanged(DataSourceChangeEvent event) {
            chart.setDataSource(chart.getDataSource());
        }
    }
}
