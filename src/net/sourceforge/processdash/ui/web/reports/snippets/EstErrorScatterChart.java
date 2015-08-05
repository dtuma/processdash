// Copyright (C) 2015 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.reports.snippets;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYBoxAnnotation;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;

import net.sourceforge.processdash.ui.web.reports.XYChart;

public class EstErrorScatterChart extends XYChart {

    @Override
    protected void buildData() {
        // retrieve the result set, and extract the % error columns
        super.buildData();
        data = data.pluckColumns(3, 6);
    }

    @Override
    public JFreeChart createChart() {
        JFreeChart chart = super.createChart();

        // set minimum bounds on the two axes
        XYPlot xyPlot = chart.getXYPlot();
        tweakAxis(xyPlot.getDomainAxis());
        tweakAxis(xyPlot.getRangeAxis());

        // add a box illustrating the target range
        double box = getTargetPercent();
        xyPlot.addAnnotation(new XYBoxAnnotation(-box, -box, box, box));
        xyPlot.addAnnotation(new XYLineAnnotation(-box, 0, box, 0));
        xyPlot.addAnnotation(new XYLineAnnotation(0, -box, 0, box));

        return chart;
    }

    private void tweakAxis(ValueAxis va) {
        NumberAxis axis = (NumberAxis) va;
        Range range = axis.getRange();
        range = Range.expandToInclude(range, -100);
        range = Range.expandToInclude(range, +100);
        axis.setRange(range);
    }

    private int getTargetPercent() {
        try {
            int pct = Integer.parseInt(getParameter("pct"));
            return Math.min(100, Math.max(0, pct));
        } catch (Exception e) {
            return 50;
        }
    }

}
