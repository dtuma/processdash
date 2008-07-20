// Copyright (C) 2001-2008 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.reports;


import java.util.Map;

import net.sourceforge.processdash.data.util.ResultSet;
import net.sourceforge.processdash.ui.web.CGIChartBase;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.general.PieDataset;



public class PieChart extends CGIChartBase {

    /** Create a  line chart. */
    @Override
    public JFreeChart createChart() {
        return createPieChart(data, parameters, get3DSetting());
    }

    public static JFreeChart createPieChart(ResultSet data, Map parameters,
            boolean threeDimensional) {
        CategoryDataset catData = data.catDataSource();
        PieDataset pieData = null;
        if (catData.getColumnCount() == 1)
            pieData = DatasetUtilities.createPieDatasetForColumn(catData, 0);
        else
            pieData = DatasetUtilities.createPieDatasetForRow(catData, 0);

        JFreeChart chart = null;
        if (threeDimensional) {
            chart = ChartFactory.createPieChart3D
                (null, pieData, true, true, false);
            chart.getPlot().setForegroundAlpha(ALPHA);
        } else {
            chart = ChartFactory.createPieChart
                (null, pieData, true, true, false);
        }

        PiePlot plot = (PiePlot) chart.getPlot();
        if (parameters.get("skipItemLabels") != null
                || parameters.get("skipWedgeLabels") != null)
            plot.setLabelGenerator(null);
        else if (parameters.get("wedgeLabelFontSize") != null) try {
            float fontSize =
                Float.parseFloat((String) parameters.get("wedgeLabelFontSize"));
            plot.setLabelFont(plot.getLabelFont().deriveFont(fontSize));
        } catch (Exception lfe) {}
        if (parameters.get("ellipse") != null)
            plot.setCircular(true);
        else
            plot.setCircular(false);

        String interiorGap = (String) parameters.get("interiorGap");
        if (interiorGap != null) try {
            plot.setInteriorGap(Integer.parseInt(interiorGap) / 100.0);
        } catch (NumberFormatException e) {}
        String interiorSpacing = (String) parameters.get("interiorSpacing");
        if (interiorSpacing != null) try {
            plot.setInteriorGap(Integer.parseInt(interiorSpacing) / 200.0);
        } catch (NumberFormatException e) {}

        if (!parameters.containsKey("showZeroValues")) {
            plot.setIgnoreZeroValues(true);
            plot.setIgnoreNullValues(true);
        }

        return chart;
    }

}
