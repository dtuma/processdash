// Copyright (C) 2015-2016 Tuma Solutions, LLC
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

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYBoxAnnotation;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleEdge;

import net.sourceforge.processdash.ui.web.reports.XYChart;

public class EstErrorScatterChart extends XYChart {

    @Override
    protected void buildData() throws IOException {
        // retrieve the result set, and extract the % error columns
        super.buildData();
        if (parameters.containsKey("skipUnitsCol"))
            data = data.pluckColumns(3, 6);
        else
            data = data.pluckColumns(4, 7);
    }

    @Override
    public JFreeChart createChart() {
        JFreeChart chart = super.createChart();

        // set minimum/maximum bounds on the two axes
        XYPlot xyPlot = chart.getXYPlot();
        double cutoff = getPercentParam("cut", 100, 200, 5000);
        xyPlot.setDomainAxis(truncAxis(xyPlot.getDomainAxis(), cutoff));
        xyPlot.setRangeAxis(truncAxis(xyPlot.getRangeAxis(), cutoff));
        xyPlot.setRenderer(new TruncatedItemRenderer(xyPlot.getRenderer()));

        // add a box illustrating the target range
        if (data.numRows() > 0) {
            double box = getPercentParam("pct", 0, 50, 100);
            xyPlot.addAnnotation(new XYBoxAnnotation(-box, -box, box, box));
            xyPlot.addAnnotation(new XYLineAnnotation(-box, 0, box, 0));
            xyPlot.addAnnotation(new XYLineAnnotation(0, -box, 0, box));
        }

        return chart;
    }

    private int getPercentParam(String paramName, int min, int def, int max) {
        try {
            int pct = Integer.parseInt(getParameter(paramName));
            return Math.max(min, Math.min(max, pct));
        } catch (Exception e) {
            return def;
        }
    }

    private TruncatedNumberAxis truncAxis(ValueAxis va, double maxValue) {
        NumberAxis axis = (NumberAxis) va;
        double upper = Math.max(100, Math.min(maxValue, axis.getUpperBound()));
        return new TruncatedNumberAxis(va.getLabel(), upper);
    }

    private class TruncatedNumberAxis extends NumberAxis {
        private double maxValue;

        TruncatedNumberAxis(String label, double upper) {
            super(label);
            setRange(-100, upper * 1.02);
            this.maxValue = upper;
        }

        @Override
        public double valueToJava2D(double value, Rectangle2D area,
                RectangleEdge edge) {
            if (value > maxValue)
                value = maxValue;
            return super.valueToJava2D(value, area, edge);
        }
    }

    private class TruncatedItemRenderer extends XYLineAndShapeRenderer {

        public TruncatedItemRenderer(XYItemRenderer r) {
            super(false, true);
            setBaseToolTipGenerator(r.getBaseToolTipGenerator());
            setURLGenerator(r.getURLGenerator());
        }

        @Override
        public XYItemRendererState initialise(Graphics2D g2,
                Rectangle2D dataArea, XYPlot plot, XYDataset data,
                PlotRenderingInfo info) {
            XYItemRendererState state = super.initialise(g2, dataArea, plot,
                data, info);
            state.setProcessVisibleItemsOnly(false);
            return state;
        }
    }

}
