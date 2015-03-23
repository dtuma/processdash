// Copyright (C) 2002-2008 Tuma Solutions, LLC
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


import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.NumberData;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.ui.web.CGIChartBase;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.StringUtils;

import org.jfree.chart.JFreeChart;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.general.PieDataset;




public class RadarChart extends CGIChartBase {

    /** Create a radar chart. */
    @Override
    public JFreeChart createChart() {
        maybeScaleDataAxes();
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

    private void maybeScaleDataAxes() {
        for (int i = 0;  i < data.numCols();  i++) {
            int n = i+1;

            String target = getParameter("t" + n);
            if (!StringUtils.hasValue(target))
                continue;

            double targetVal = 0;
            try {
                targetVal = FormatUtil.parseNumber(target);
            } catch (Exception e) {
                SaveableData val = getDataRepository().getInheritableValue(
                        getPrefix(), target);
                if (val != null) {
                    SimpleData sVal = val.getSimpleValue();
                    if (sVal instanceof NumberData)
                        targetVal = ((NumberData) sVal).getDouble();
                }
            }
            if (targetVal == 0)
                continue;

            boolean reverse = parameters.containsKey("r" + n);

            SimpleData d = data.getData(1, n);
            if (d instanceof NumberData) {
                NumberData num = (NumberData) d;
                double val = num.getDouble();
                if (Double.isInfinite(val) || Double.isNaN(val))
                    val = 1.0;
                else if (reverse)
                    val = 2.0 / (1.0 + (val / targetVal));
                else
                    val = val / targetVal;
                data.setData(1, n, new DoubleData(val));
            }
        }
    }

}
