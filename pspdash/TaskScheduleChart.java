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

import javax.swing.*;
import com.jrefinery.chart.*;
import com.jrefinery.chart.event.DataSourceChangeListener;
import com.jrefinery.chart.event.DataSourceChangeEvent;

public class TaskScheduleChart extends JDialog {

    TaskScheduleDialog parent;

    public TaskScheduleChart(TaskScheduleDialog parent) {
        super(parent.frame, "EV Chart - " + parent.taskListName);
        this.parent = parent;

        JTabbedPane tabPane = new JTabbedPane();
        tabPane.addTab("Earned Value", buildValueChart());
        tabPane.addTab("Direct Hours", buildTimeChart());
        tabPane.addTab("Combined",     buildCombinedChart());

        getContentPane().add(tabPane);
        pack();
        show();
    }

    private JFreeChartPanel buildTimeChart() {
        return buildChart(parent.model.getSchedule().getTimeChartData());
    }

    private JFreeChartPanel buildValueChart() {
        return buildChart(parent.model.getSchedule().getValueChartData());
    }

    private JFreeChartPanel buildCombinedChart() {
        return buildChart(parent.model.getSchedule().getCombinedChartData());
    }

    private JFreeChartPanel buildChart(XYDataSource data) {
        JFreeChart chart = JFreeChart.createTimeSeriesChart(data);
        chart.setTitle((Title) null);
        data.addChangeListener(new DataChangeWrapper(chart));
        JFreeChartPanel panel = new JFreeChartPanel(chart);
        return panel;
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
