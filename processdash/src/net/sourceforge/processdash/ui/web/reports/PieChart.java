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

package net.sourceforge.processdash.ui.web.reports;


import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.CategoryDataset;
import org.jfree.data.DatasetUtilities;
import org.jfree.data.PieDataset;

import pspdash.CGIChartBase;


public class PieChart extends CGIChartBase {

    /** Create a  line chart. */
    public JFreeChart createChart() {
        CategoryDataset catData = data.catDataSource();
        PieDataset pieData = null;
        if (catData.getRowCount() == 1)
            pieData = DatasetUtilities.createPieDatasetForRow(catData, 0);
        else
            pieData = DatasetUtilities.createPieDatasetForColumn(catData, 0);

        JFreeChart chart = null;
        if (get3DSetting()) {
            chart = ChartFactory.createPie3DChart
                (null, pieData, true, false, false);
            chart.getPlot().setForegroundAlpha(ALPHA);
        } else {
            chart = ChartFactory.createPieChart
                (null, pieData, true, false, false);
        }

        PiePlot plot = (PiePlot) chart.getPlot();
        if (parameters.get("skipWedgeLabels") != null)
            plot.setSectionLabelType(PiePlot.NO_LABELS);
        else if (parameters.get("wedgeLabelFontSize") != null) try {
            float fontSize =
                Float.parseFloat(getParameter("wedgeLabelFontSize"));
            plot.setSeriesLabelFont
                (plot.getSeriesLabelFont().deriveFont(fontSize));
        } catch (Exception lfe) {}
        if (parameters.get("ellipse") != null)
            plot.setCircular(true);
        else
            plot.setCircular(false);

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
