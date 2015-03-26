// Copyright (C) 2008-2012 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.lib.chart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;

import net.sourceforge.processdash.ev.EVTaskList.PlanVsActualCategoryChartSeries;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.PieDataset;

/**
 * A DiscItemrenderer that is used for disc charts where each disc represents a
 *  task. This renderer draws an "overspent" circle on each task for which the planned
 *  time is less than the actual direct time. The DiscPlot using this renderer
 *  must have been initiated with a CategoryToPieDataset where one of the column
 *  contains the planned time and another contains the actual direct time.
 */
public class OverspentDiscItemRenderer extends StandardDiscItemRenderer {

    /** The overspent circle stroke */
    private static final BasicStroke OVERSPENT_CIRCLE_STROKE = new BasicStroke(2.0f);

    /** The overspent circle color */
    private Color overspentColor;

    /** The alpha value of the most transparent part of the overspent section */
    private static final int ALPHA = 10;

    /** The color used to draw the overspent section */
    private Color overspentGradientFill;

    /** The number of ellipses drawn in the overspent section */
    private static final double NUM_CIRCLES_OVERSPENT_SECTION = 20;

    /** The exponent that is used when drawing the overspent section to make the
        gradient better looking */
    private static final double EXPONENT = 2;

    /** The paint to use when drawing the quantiles */
    private static final Paint QUANTILE_PAINT = Color.white;

    /** The stroke to use when drawing the quantiles */
    private static final Stroke QUANTILE_STROKE = new BasicStroke(1f);

    /** The width of the quantile arc, in degrees */
    private static final int QUANTILE_ARC_DEGREES = 30;

    /** The CategoryDataset that contains all columns we need to draw the ovserspent
        section on the disc chart */
    private CategoryDataset underlyingDataset;

    /** True if quantiles should be drawn on the disc */
    private boolean showQuantiles;

    public OverspentDiscItemRenderer(CategoryDataset underlyingDataset) {
        this(underlyingDataset, Color.red);
    }

    public OverspentDiscItemRenderer(CategoryDataset underlyingDataset,
            Color overspentColor) {
        this.underlyingDataset = underlyingDataset;

        setShowQuantiles(true);
        setOverspentColor(overspentColor);
    }

    public boolean isShowQuantiles() {
        return showQuantiles;
    }

    public void setShowQuantiles(boolean showQuantiles) {
        this.showQuantiles = showQuantiles;
    }

    public Color getOverspentColor() {
        return overspentColor;
    }

    public void setOverspentColor(Color c) {
        this.overspentColor = c;
        this.overspentGradientFill = new Color(c.getRed(), c.getGreen(),
                c.getBlue(), ALPHA);
    }

    @Override
    protected void drawDisc(Graphics2D g2, Ellipse2D discShape, Paint discPaint, int item) {
        super.drawDisc(g2, discShape, discPaint, item);

        Number actualDirectTime = underlyingDataset.getValue(item,
                PlanVsActualCategoryChartSeries.ACTUAL_DIRECT_TIME_COLUMN_POS);
        Number plannedTime = underlyingDataset.getValue(item,
                PlanVsActualCategoryChartSeries.PLANNED_TIME_COLUMN_POS);

        double scale = getScale(discShape, actualDirectTime);
        double discWidth = discShape.getWidth();


        if (plannedTime.doubleValue() < actualDirectTime.doubleValue()) {
            // The task is overspent.

            g2.setColor(overspentGradientFill);

            // We draw multiple red ellipses to represent the overspent section. The first
            //  one we draw is as big as the entire disc and the last one has the same
            //  width as the overspentCircle. The radius of the overspent circle is the
            //  square root of the task's planned time (multiplied by the scale). To get to
            //  width, we simply multiply by 2. Since all ellipses are semi-transparent and
            //  are drawn over each other, this makes a nice gradient effect. Since drawing
            //  multiple semi-transparent ellipses over each other creates a
            //  multiplicative alpha blending, we calculate the width of the ellipses in
            //  a exponential manner, so they are farther apart near the center.
            double overspentCircleWidth = (Math.sqrt(plannedTime.doubleValue()) * scale) * 2;
            Ellipse2D shape = null;
            double width = discWidth;
            double x = 0;
            double lastX = Math.pow(discWidth - overspentCircleWidth, 1/EXPONENT);
            double increment = lastX / NUM_CIRCLES_OVERSPENT_SECTION;

            while (x < lastX + (1 * increment)) {
                width = -Math.pow(x, EXPONENT) + discWidth;
                shape = getEllipseShape(discShape, width);
                g2.fill(shape);
                x += increment;
            }

            // We now draw a smaller disc that represents the planned time for the task
            g2.setPaint(discPaint);
            shape = getEllipseShape(discShape, overspentCircleWidth);
            g2.fill(shape);

            // We finally draw the overspent delimitation circle
            shape = getEllipseShape(discShape, overspentCircleWidth);
            g2.setColor(overspentColor);
            g2.setStroke(OVERSPENT_CIRCLE_STROKE);
            g2.draw(shape);
        }

        if (showQuantiles) {
            g2.setPaint(QUANTILE_PAINT);
            g2.setStroke(QUANTILE_STROKE);
            for (int i = 1;  i < 4;  i++) {
                double percent = i / 4.0;
                double quantileTime = plannedTime.doubleValue() * percent;
                double radius = Math.sqrt(quantileTime) * scale;
                double width = radius * 2;
                if (width > discWidth)
                    break;
                g2.draw(new Arc2D.Double(discShape.getCenterX() - radius,
                        discShape.getCenterY() - radius, width, width,
                        90 - QUANTILE_ARC_DEGREES / 2, QUANTILE_ARC_DEGREES,
                        Arc2D.OPEN));
            }
        }
    }

    private double getScale(Ellipse2D discShape, Number actualDirectTime) {
        double discRadius = discShape.getWidth() / 2;
        double disctTheoreticalRadius = Math.sqrt(actualDirectTime.doubleValue());
        return discRadius / disctTheoreticalRadius;
    }

    private Ellipse2D getEllipseShape(Ellipse2D discShape, double width) {
        return new Ellipse2D.Double(discShape.getCenterX() - width/2,
                                    discShape.getCenterY() - width/2,
                                    width,
                                    width);
    }

    @Override
    protected void drawLabel(Graphics2D g2, DiscPlot plot, PieDataset dataset,
            Ellipse2D shape, Comparable key, Paint discPaint) {
        // We don't want any labels
    }

}
