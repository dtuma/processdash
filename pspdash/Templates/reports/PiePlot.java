/* =======================================
 * JFreeChart : a Java Chart Class Library
 * =======================================
 * Version:         0.5.6;
 * Project Lead:    David Gilbert (david.gilbert@bigfoot.com);
 *
 * File:            PiePlot.java
 * Author:          Andrzej Porebski;
 * Contributor(s):  David Gilbert;
 *
 * (C) Copyright 2000, by Andrzej Porebski;
 *
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation;
 * either version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307, USA.
 *
 * $Id$
 *
 */

package com.jrefinery.chart;

import java.awt.*;
import java.awt.geom.*;
import java.awt.font.*;
import java.util.*;
import com.jrefinery.chart.event.*;

/**
 * A plot that displays data in the form of a pie chart, using data from any class that implements
 * the CategoryDataSource interface.
 * @see Plot
 * @see CategoryDataSource
 */
public class PiePlot extends Plot {

    /** Flag determining whether to draw an ellipse or a perfect circle. */
    boolean drawCircle = true;

    /** Flag determining whether to draw wedge labels. */
    boolean drawWedgeLabels = true;

    /** Interior spacing - that is the room between the plot border and a rectangle that will be
        considered for drawing the pie. */
    int interiorSpacing = 30;

    Font wedgeLabelFont = new Font("Arial", Font.PLAIN, 12);
    Paint wedgePaint = Color.black;

    /**
     * Standard constructor: returns a PiePlot with attributes specified by the caller.
     * @param chart The chart that the plot belongs to;
     * @param insets The gaps between the plot area and the border of the chart;
     */
    public PiePlot(JFreeChart chart, Insets insets) throws AxisNotCompatibleException {
        super(chart, new BlankAxis(), new BlankAxis());
        setInsets(insets);
    }

    /**
     * Standard constructor - builds a PiePlot with mostly default attributes.
     * @param chart The chart that the plot belongs to;
     */
    public PiePlot(JFreeChart chart) throws AxisNotCompatibleException {
        this(chart, new Insets(0, 5, 5, 0));
    }

    /**
     * A convenience method that returns the data source for the plot, cast as a CategoryDataSource.
     */
    public CategoryDataSource getDataSource() {
        return (CategoryDataSource)chart.getDataSource();
    }

    /**
     * A convenience method that returns a lit of the categories in the data source.
     */
    public java.util.List getCategories() {
        return getDataSource().getCategories();
    }

    /**
     * Checks the compatibility of a horizontal axis, returning true if the axis is compatible with
     * the plot, and false otherwise.
     * @param axis The horizontal axis;
     */
    public boolean isCompatibleHorizontalAxis(Axis axis) {
        if (axis instanceof BlankAxis)
            return true;
        else
            return false;
    }

    /**
     * Checks the compatibility of a vertical axis, returning true if the axis is compatible with
     * the plot, and false otherwise.
     * @param axis The vertical axis;
     */
    public boolean isCompatibleVerticalAxis(Axis axis) {
        if (axis instanceof BlankAxis)
            return true;
        else
            return false;
    }

    public void setInteriorSpacing(int i) { interiorSpacing = i; }

    /**
     * Draws the plot on a Java 2D graphics device (such as the screen or a printer).
     * @param g2 The graphics device;
     * @param drawArea The area within which the plot should be drawn;
     */
    public void draw(Graphics2D g2, Rectangle2D drawArea) {
        // compute the plot area
        Rectangle2D plotArea = drawArea;

        if (insets!=null) {
            plotArea = new Rectangle2D.Double(drawArea.getX()+insets.left, drawArea.getY()+insets.top,
                                              drawArea.getWidth()-insets.left-insets.right,
                                              drawArea.getHeight()-insets.top-insets.bottom);
        }

        // draw the outline and background
        drawOutlineAndBackground(g2, plotArea);

        // adjust the plot area by the interior spacing value
        plotArea = new Rectangle2D.Double(plotArea.getX()+interiorSpacing,
                                          plotArea.getY()+interiorSpacing,
                                          plotArea.getWidth()-2*interiorSpacing,
                                          plotArea.getHeight()-2*interiorSpacing);

        // if we are drawing a perfect circle, we need to readjust the top left coordinates of the
        // drawing area for the arcs to arrive at this effect.
        if (drawCircle) {
            double min = Math.min(plotArea.getWidth(), plotArea.getHeight())/2;
            plotArea = new Rectangle2D.Double(plotArea.getCenterX() - min,
                                              plotArea.getCenterY() - min,
                                              2*min, 2*min);
        }

        // get the data source - return if null;
        CategoryDataSource data = (CategoryDataSource)chart.getDataSource();
        if (data == null)
            return;

        // get the first category from the datasource, return if null;
        Object category = data.getCategories().iterator().next();
        if (category == null)
            return;

        // establish the coordinates of the top left corner of the drawing area
        double arcX = plotArea.getX();
        double arcY = plotArea.getY();

        // compute the total value of the data series skipping over the negative values
        double totalValue = 0;
        int seriesCount = data.getSeriesCount();
        for (int seriesIndex = 0; seriesIndex<seriesCount; seriesIndex++) {
            Number dataValue = data.getValue(seriesIndex, category);
            double value = dataValue.doubleValue();
            if (value<=0)
                continue;
            totalValue += value;
        }

        // For each positive value in the dataseries, compute and draw the corresponding arc.
        double sumTotal = 0;
        for (int seriesIndex=0; seriesIndex<seriesCount; seriesIndex++) {
            Number dataValue = data.getValue(seriesIndex, category);
            double value = dataValue.doubleValue();
            if (value<=0)
                continue;
            double extent = value * 360 / totalValue;
            double startAngle = 90 - (sumTotal * 360 / totalValue) - extent;
            Arc2D.Double arc = new Arc2D.Double(arcX, arcY, plotArea.getWidth(), plotArea.getHeight(),
                                                startAngle, extent, Arc2D.PIE);
            sumTotal += value;

            Paint paint = chart.getSeriesPaint(seriesIndex);
            Paint outlinePaint = chart.getSeriesOutlinePaint(seriesIndex);

            g2.setPaint(paint);
            g2.fill(arc);
            g2.setStroke(new BasicStroke());
            g2.setPaint(outlinePaint);
            g2.draw(arc);

            if (drawCircle && drawWedgeLabels) {
                FontRenderContext frc = g2.getFontRenderContext();
                String seriesName = data.getSeriesName(seriesIndex);
                Rectangle2D seriesBounds = wedgeLabelFont.getStringBounds(seriesName, frc);
                LineMetrics lm = wedgeLabelFont.getLineMetrics(seriesName, frc);
                double ascent = lm.getAscent();

                Rectangle2D labelPlotArea = new Rectangle2D.Double(plotArea.getX() - ascent,
                                                                   plotArea.getY() - ascent,
                                                                   plotArea.getWidth()+2*ascent,
                                                                   plotArea.getHeight()+2*ascent);
                // we could use either width or height - they should be the same since the area is supposed
                // to be a square.  Extent is divided by 2 to get the middle of the arc.
                // use formula
                // x = r * cos(alpha)
                // y = r * sin(alpha)

                double radius = labelPlotArea.getWidth()/2;
                double labelLocationX = labelPlotArea.getCenterX() + Math.cos(startAngle * Math.PI/180. +
                                        extent/2. * Math.PI/180.) * labelPlotArea.getWidth()/2;
                double labelLocationY = labelPlotArea.getCenterY() - Math.sin(startAngle * Math.PI/180. +
                                        extent/2. * Math.PI/180.) * labelPlotArea.getHeight()/2;

                if (labelLocationX <= labelPlotArea.getCenterX())
                    labelLocationX -= seriesBounds.getWidth();

                if (labelLocationY > labelPlotArea.getCenterY())
                    labelLocationY +=ascent;

                g2.setPaint(wedgePaint);
                g2.setFont(wedgeLabelFont);
                g2.drawString(seriesName, (float)labelLocationX, (float)labelLocationY);
            }

        }

    }

    /**
     * Returns a short string describing the type of plot.
     */
    public String getPlotType() {
        return "Pie Plot";
    }

    /**
     * Returns true if this pie chart is configured to draw a perfect circle in the available chart
     * area, and false otherwise.
     */
    public boolean getDrawCircle() {
        return drawCircle;
    }

    /**
     * Sets this chart's property that causes it to draw the shape as either a perfect circle or an
     * ellipse.
     */
    public void setDrawCircle(boolean circle) {
        drawCircle = circle;
    }

    /**
     * Returns true if this pie chart is configured to draw wedge labels,
     * and false otherwise.
     */
    public boolean getDrawWedgeLabels() {
        return drawWedgeLabels;
    }

    /**
     * Sets this chart's property that causes it to draw wedge labels.
     */
    public void setDrawWedgeLabels(boolean wedge) {
        drawWedgeLabels = wedge;
    }

    public void setWedgeLabelFont(Font f) { wedgeLabelFont = f; }
    public Font getWedgeLabelFont() { return wedgeLabelFont; }

    public void setWedgeLabelPaint(Paint p) { wedgePaint = p; }
    public Paint getWedgeLabelPaint() { return wedgePaint; }
}
