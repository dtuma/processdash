// Copyright (C) 2001-2016 Tuma Solutions, LLC
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


import java.awt.Color;
import java.awt.Paint;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.general.PieDataset;

import net.sourceforge.processdash.process.ProcessUtil;
import net.sourceforge.processdash.ui.lib.chart.PhaseChartColorer;
import net.sourceforge.processdash.ui.web.CGIChartBase;



public class PieChart extends CGIChartBase {

    /** Create a  line chart. */
    @Override
    public JFreeChart createChart() {
        CategoryDataset catData = data.catDataSource();
        PieDataset pieData = null;
        if (catData.getColumnCount() == 1)
            pieData = DatasetUtilities.createPieDatasetForColumn(catData, 0);
        else
            pieData = DatasetUtilities.createPieDatasetForRow(catData, 0);

        JFreeChart chart = null;
        if (get3DSetting()) {
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

        Object colorScheme = parameters.get("colorScheme");
        if ("byPhase".equals(colorScheme))
            maybeConfigurePhaseColors(plot, pieData);
        else if ("consistent".equals(colorScheme))
            // since 2.0.9
            configureConsistentColors(plot, pieData);
        else if (parameters.containsKey("c1"))
            configureIndividualColors(plot, pieData);

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

    private void maybeConfigurePhaseColors(final PiePlot plot,
            PieDataset pieData) {
        ProcessUtil procUtil = new ProcessUtil(getDataContext());
        new PhaseChartColorer(procUtil, pieData.getKeys()) {
            public void setItemColor(Object key, int pos, Color c) {
                plot.setSectionPaint((Comparable) key, c);
            }
        }.run();
    }

    private void configureConsistentColors(final PiePlot plot,
            PieDataset pieData) {
        DefaultDrawingSupplier s = new DefaultDrawingSupplier();

        String skip = getParameter("consistentSkip");
        if (skip != null)
            for (int i = Integer.parseInt(skip); i-- > 0;)
                s.getNextPaint();

        for (Object key : pieData.getKeys()) {
            Paint paint = s.getNextPaint();
            plot.setSectionPaint((Comparable) key, paint);
        }
    }

    private void configureIndividualColors(PiePlot plot, PieDataset pieData) {
        int num = 1;
        for (Object key : pieData.getKeys()) {
            String colorKey = "c" + num;
            String color = getParameter(colorKey);
            if (color != null)
                plot.setSectionPaint((Comparable) key,
                    Color.decode("#" + color));
            num++;
        }
    }

}
