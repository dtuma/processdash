// Copyright (C) 2003-2008 Tuma Solutions, LLC
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
import net.sourceforge.processdash.ui.lib.chart.DiscPlot;
import net.sourceforge.processdash.ui.lib.chart.DrawingSupplierFactory;
import net.sourceforge.processdash.ui.lib.chart.StandardDiscItemDistributor;
import net.sourceforge.processdash.ui.web.CGIChartBase;

import org.jfree.chart.JFreeChart;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.general.PieDataset;
import org.jfree.ui.RectangleInsets;



public class DiscChart extends CGIChartBase {

    /** Create disc chart. */
    @Override
    public JFreeChart createChart() {
        return createDiscChart(data, parameters);
    }

    public static JFreeChart createDiscChart(ResultSet data, Map parameters) {
        // data.sortBy(1, true);
        CategoryDataset catData = data.catDataSource();
        PieDataset pieData = null;
        if (catData.getColumnCount() == 1)
            pieData = DatasetUtilities.createPieDatasetForColumn(catData, 0);
        else
            pieData = DatasetUtilities.createPieDatasetForRow(catData, 0);

        DiscPlot plot = new DiscPlot(pieData);
        plot.setInsets(new RectangleInsets(0.0, 5.0, 5.0, 5.0));
        plot.setDrawingSupplier(DRAWING_SUPPLIER_FACTORY.newDrawingSupplier());
        JFreeChart chart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT,
                plot, false);

        if (parameters.get("skipItemLabels") != null
                || parameters.get("skipDiscLabels") != null)
            plot.setLabelGenerator(null);
        else if (parameters.get("discLabelFontSize") != null) try {
            float fontSize =
                Float.parseFloat((String) parameters.get("discLabelFontSize"));
            plot.setLabelFont(plot.getLabelFont().deriveFont(fontSize));
        } catch (Exception lfe) {}
        if (parameters.get("ellipse") != null)
            ((StandardDiscItemDistributor) plot.getDiscDistributor())
                    .setCircular(false);

        String interiorGap = (String) parameters.get("interiorGap");
        if (interiorGap != null) try {
            plot.setInteriorGap(Integer.parseInt(interiorGap) / 100.0);
        } catch (NumberFormatException e) {}
        String interiorSpacing = (String) parameters.get("interiorSpacing");
        if (interiorSpacing != null) try {
            plot.setInteriorGap(Integer.parseInt(interiorSpacing) / 200.0);
        } catch (NumberFormatException e) {}

        return chart;
    }

    public static final DrawingSupplierFactory DRAWING_SUPPLIER_FACTORY =
        new DrawingSupplierFactory()
            .setPaintSequence(DrawingSupplierFactory.PALETTE1)
            .setFillPaintSequence(DrawingSupplierFactory.PALETTE1)
            .setOutlinePaintSequence(DrawingSupplierFactory.PALETTE1);
}
