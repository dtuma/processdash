// PSP Dashboard - Data Automation Tool for PSP-like processes
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ui.web.reports;


import org.jfree.chart.JFreeChart;
import org.jfree.data.CategoryDataset;
import org.jfree.data.DatasetUtilities;
import org.jfree.data.PieDataset;

import pspdash.CGIChartBase;



public class RadarChart extends CGIChartBase {

    /** Create a radar chart. */
    public JFreeChart createChart() {
        CategoryDataset catData = data.catDataSource();
        PieDataset pieData = null;
        if (catData.getRowCount() == 1)
            pieData = DatasetUtilities.createPieDatasetForRow(catData, 0);
        else
            pieData = DatasetUtilities.createPieDatasetForColumn(catData, 0);

        RadarPlot plot = new RadarPlot(pieData);
        JFreeChart chart = new JFreeChart
            (null, JFreeChart.DEFAULT_TITLE_FONT, plot, false);

        if (parameters.get("skipAxisLabels") != null)
            plot.setShowAxisLabels(false);
        String interiorGap = getParameter("interiorGap");
        if (interiorGap != null) try {
            plot.setInteriorGap(Integer.parseInt(interiorGap) / 100.0);
        } catch (NumberFormatException e) {}
        String interiorSpacing = getParameter("interiorSpacing");
        if (interiorSpacing != null) try {
            plot.setInteriorGap(Integer.parseInt(interiorSpacing) / 200.0);
        } catch (NumberFormatException e) {}

        return chart;
    }

}
