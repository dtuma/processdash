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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;

import net.sourceforge.processdash.ui.lib.chart.XYDatasetFilter;

import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.xy.XYDataset;

public class EVHiddenOrShownSeriesXYChartPanel extends EVXYChartPanel
        implements DatasetChangeListener {

    private XYDatasetFilter filteredData;
    private String units;

    public EVHiddenOrShownSeriesXYChartPanel(JFreeChart chart,
                                    XYDatasetFilter filteredData,
                                    String units) {
        super(chart);
        this.filteredData = filteredData;
        this.units = units;

        filteredData.getSourceDataset().addChangeListener(this);
        getPopupMenu().insert(new JPopupMenu.Separator(), 0);
        reloadSeriesMenus();
    }

    @Override
    public void dispose() {
        this.filteredData.getSourceDataset().removeChangeListener(this);
        super.dispose();
    }

    private void reloadSeriesMenus() {
        JPopupMenu menu = getPopupMenu();
        while (menu.getComponent(0) instanceof ShowChartLineMenuItem)
            menu.remove(0);
        XYDataset data = filteredData.getSourceDataset();
        for (int i = data.getSeriesCount();   i-- > 0; )
            menu.insert(new ShowChartLineMenuItem(filteredData, i), 0);
    }

    public void datasetChanged(DatasetChangeEvent arg0) {
        reloadSeriesMenus();
    }

    private class ShowChartLineMenuItem extends JCheckBoxMenuItem
            implements ActionListener {
        private XYDatasetFilter data;
        private int seriesNum;
        public ShowChartLineMenuItem(XYDatasetFilter data, int seriesNum) {
            this.data = data;
            this.seriesNum = seriesNum;
            String seriesName = AbstractEVChart.getNameForSeries(data, seriesNum);
            setText(resources.format("Show_Line_FMT", seriesName));
            setSelected(data.isSeriesHidden(seriesNum) == false);
            addActionListener(this);
        }
        public void actionPerformed(ActionEvent e) {
            data.setSeriesHidden(seriesNum, isSelected() == false);
        }
    }

}
