/* =======================================
 * JFreeChart : a Java Chart Class Library
 * =======================================
 * Version:         0.5.6;
 * Project Lead:    David Gilbert (david.gilbert@bigfoot.com);
 *
 * File:            XYPlot.java
 * Author:          David Gilbert;
 * Contributor(s):  -;
 *
 * (C) Copyright 2000, Simba Management Limited;
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
 */

package com.jrefinery.chart;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;

/**
 * A Plot that displays data in the form of an XY plot, using data from any class that
 * implements the XYDataSource interface.
 * @see Plot
 * @see XYDataSource
 */
public class XYPlot extends Plot implements HorizontalValuePlot, VerticalValuePlot {

    /**
     * Standard constructor: returns an XYPlot with the specified axes (other attributes take
     * default values).
     * @param chart The chart that the plot belongs to;
     * @param horizontal The horizontal axis;
     * @param vertical The vertical axis.
     */
    public XYPlot(JFreeChart chart, Axis horizontal, Axis vertical)
             throws AxisNotCompatibleException {
        super(chart, horizontal, vertical);
    }

    /**
     * A convenience method that returns the data-source for the plot, cast as an XYDataSource.
     * @return The data-source for the plot, cast as an XYDataSource.
     */
    public XYDataSource getDataSource() {
        return (XYDataSource)chart.getDataSource();
    }

    /**
     * A convenience method that returns a reference to the horizontal axis cast as a
     * ValueAxis.
     * @return The horizontal axis cast as a ValueAxis.
     */
    public ValueAxis getHorizontalValueAxis() {
        return (ValueAxis)horizontalAxis;
    }

    /**
     * A convenience method that returns a reference to the vertical axis cast as a
     * ValueAxis.
     * @return The vertical axis cast as a ValueAxis.
     */
    public ValueAxis getVerticalValueAxis() {
        return (ValueAxis)verticalAxis;
    }

    /**
     * Returns a list of lines that will fit inside the specified area.
     * @param plotArea The area within which the plot will be drawn;
     * @return A list of lines for the XYPlot.
     */
    private java.util.List getLines(Rectangle2D plotArea) {

        java.util.List lines = new ArrayList();
        XYDataSource data = getDataSource();
        if (data!=null) {
            int seriesCount = data.getSeriesCount();

            for (int series=0; series<seriesCount; series++) {
                int itemCount = data.getItemCount(series);
                int itemIndex = 0;
                Point2D prev = null;
                boolean isScatter = false;
                try {
                    // we want to be able to determine whether each series should be
                    // drawn as a scatterplot or as a line.  The best way to do this
                    // is to ask the datasource.  We could creating a subinterface of
                    // XYDataSource that includes a method for us to call, but that
                    // would be painful.  Instead, we'll use this workable kludge:
                    // an XYDataSource can tell us whether each series is a scatter
                    // via the YValue with itemIndex -1.  If we ask for item -1, and
                    // an exception is thrown, or we get null, we'll presume the
                    // default line behavior.  If we get any nonull value back, we'll
                    // take that as our signal that the series is a scatter.
                    isScatter = (data.getYValue(series, -1) != null);
                } catch (Exception e) {}
                while (itemIndex<itemCount) {
                    Number x = data.getXValue(series, itemIndex);
                    Number y = data.getYValue(series, itemIndex);
                    if (y == null)
                        prev = null;
                    else {
                        double xx = getHorizontalValueAxis().translatedValue(x, plotArea);
                        double yy = getVerticalValueAxis().translatedValue(y, plotArea);
                        Paint p = chart.getSeriesPaint(series);
                        Stroke s = chart.getSeriesStroke(series);
                        Point2D current = new Point2D.Double(xx, yy);
                        if (isScatter) {
                            lines.add(new Bar(xx-3, yy-3, 6.0, 6.0, s, Color.black, p));
                        } else if (y instanceof RangeInfo) {
                            double yylow = getVerticalValueAxis().translatedValue
                                (((RangeInfo) y).getMinimumRangeValue(), plotArea);
                            double yyhigh = getVerticalValueAxis().translatedValue
                                (((RangeInfo) y).getMaximumRangeValue(), plotArea);
                            lines.add(new LineRange(prev.getX(), prev.getY(), xx, yy, xx, yylow, xx, yyhigh, s, p));
                        } else if (prev!=null) {
                            if (x instanceof RangeInfo) {
                                double xxlow = getHorizontalValueAxis().translatedValue
                                    (((RangeInfo) x).getMinimumRangeValue(), plotArea);
                                double xxhigh = getHorizontalValueAxis().translatedValue
                                    (((RangeInfo) x).getMaximumRangeValue(), plotArea);
                                lines.add(new LineRange(prev.getX(), prev.getY(), xx, yy, xxlow, yy, xxhigh, yy, s, p));
                            } else {
                                lines.add(new Line(prev.getX(), prev.getY(), current.getX(), current.getY(), s, p));
                            }
                        }
                        prev = current;
                    }
                    itemIndex++;
                }
            }
        }
        return lines;
    }

    /**
     * Checks the compatibility of a horizontal axis, returning true if the axis is compatible with
     * the plot, and false otherwise.
     * @param axis The horizontal axis;
     * @return True if the axis is compatible with the plot, and false otherwise.
     */
    public boolean isCompatibleHorizontalAxis(Axis axis) {
        if (axis instanceof HorizontalNumberAxis) {
            return true;
        }
        else if (axis instanceof HorizontalDateAxis) {
            return true;
        }
        else return false;
    }

    /**
     * Checks the compatibility of a vertical axis, returning true if the axis is compatible with
     * the plot, and false otherwise.
     * @param axis The vertical axis;
     * @return True if the axis is compatible with the plot, and false otherwise.
     */
    public boolean isCompatibleVerticalAxis(Axis axis) {
        if (axis instanceof VerticalNumberAxis) {
            return true;
        }
        else return false;
    }

    /**
     * Draws the plot on a Java 2D graphics device (such as the screen or a printer).
     * @param g2 The graphics device;
     * @param drawArea The area within which the plot should be drawn;
     */
    public void draw(Graphics2D g2, Rectangle2D drawArea) {

        if (insets!=null) {
            drawArea = new Rectangle2D.Double(drawArea.getX()+insets.left,
                                              drawArea.getY()+insets.top,
                                              drawArea.getWidth()-insets.left-insets.right,
                                              drawArea.getHeight()-insets.top-insets.bottom);
        }

        // we can cast the axes because XYPlot enforces support of these interfaces
        HorizontalAxis ha = getHorizontalAxis();
        VerticalAxis va = getVerticalAxis();

        double h = ha.reserveHeight(g2, this, drawArea);
        Rectangle2D vAxisArea = va.reserveAxisArea(g2, this, drawArea, h);

        // compute the plot area
        Rectangle2D plotArea = new Rectangle2D.Double(drawArea.getX()+vAxisArea.getWidth(),
                                                      drawArea.getY(),
                                                      drawArea.getWidth()-vAxisArea.getWidth(),
                                                      drawArea.getHeight()-h);

        drawOutlineAndBackground(g2, plotArea);

        // draw the axes
        this.horizontalAxis.draw(g2, drawArea, plotArea);
        this.verticalAxis.draw(g2, drawArea, plotArea);

        Shape originalClip = g2.getClip();
        g2.setClip(plotArea);

        java.util.List lines = getLines(plotArea);   // area should be remaining area only
        for (int i=0; i<lines.size(); i++) {
            Object o = lines.get(i);
            if (o instanceof Line) {
                Line l = (Line) o;
                g2.setPaint(l.getPaint());
                g2.setStroke(l.getStroke());
                g2.draw(l.getLine());
            } else if (o instanceof LineRange) {
                ((LineRange) o).draw(g2);
            } else {
                Bar b = (Bar) o;
                Rectangle2D barArea = b.getArea();
                g2.setPaint(b.getFillPaint());
                g2.fill(barArea);
                g2.setStroke(b.getOutlineStroke());
                g2.setPaint(b.getOutlinePaint());
                g2.draw(barArea);
            }
        }

        g2.setClip(originalClip);
    }

    /**
     * Returns the plot type as a string.
     * @return A short string describing the type of plot.
     */
    public String getPlotType() {
        return "XY Plot";
    }

    /**
     * Returns the minimum value in the domain, since this is plotted against the horizontal axis for
     * an XYPlot.
     * @return The minimum value to be plotted against the horizontal axis.
     */
    public Number getMinimumHorizontalDataValue() {

        DataSource data = this.getChart().getDataSource();
        if (data!=null) {
            return DataSources.getMinimumDomainValue(data);
        }
        else return null;

    }

    /**
     * Returns the maximum value in the domain, since this is plotted against the horizontal axis for
     * an XYPlot.
     * @return The maximum value to be plotted against the horizontal axis.
     */
    public Number getMaximumHorizontalDataValue() {

        DataSource data = this.getChart().getDataSource();
        if (data!=null) {
            return DataSources.getMaximumDomainValue(data);
        }
        else return null;
    }

    /**
     * Returns the minimum value in the range, since this is plotted against the vertical axis for
     * an XYPlot.
     * @return The minimum value to be plotted against the vertical axis.
     */
    public Number getMinimumVerticalDataValue() {

        DataSource data = this.getChart().getDataSource();
        if (data!=null) {
            return DataSources.getMinimumRangeValue(data);
        }
        else return null;

    }

    /**
     * Returns the maximum value in the range, since this is plotted against the vertical axis for
     * an XYPlot.
     * @return The maximum value to be plotted against the vertical axis.
     */
    public Number getMaximumVerticalDataValue() {

        DataSource data = this.getChart().getDataSource();
        if (data!=null) {
            return DataSources.getMaximumRangeValue(data);
        }
        else return null;
    }

    private static class LineRange {
        int[] x, y;
        Stroke mainLineStroke, edgeLineStroke;
        Paint mainLinePaint, edgeLinePaint, fillPaint;
        public LineRange(double x0, double y0,
                         double x1, double y1,
                         double x2, double y2,
                         double x3, double y3,
                         Stroke outlineStroke,
                         Paint mainLinePaint) {
            x = new int[4];   y = new int[4];
            x[0] = (int) x0;  x[1] = (int) x2;  x[2] = (int) x1;  x[3] = (int) x3;
            y[0] = (int) y0;  y[1] = (int) y2;  y[2] = (int) y1;  y[3] = (int) y3;
            this.mainLinePaint = mainLinePaint;
            if (mainLinePaint instanceof Color) {
                Color c = (Color) mainLinePaint;
                //        Color dark = transp(c, 0.8);
                //        Color light = transp(c, 0.01);
                Color dark = transp(c, calcAlpha(c));
                Color light = transp(c, 0.01);
                this.edgeLinePaint = this.fillPaint = c;
                try {
                    this.fillPaint = new GradientPaint
                        (gradientStart(x0, y0, x1, y1, x2, y2, x3, y3), light,
                         new Point2D.Double(x1, y1), dark, true);
                } catch (Exception e) { }

            } else {
                this.edgeLinePaint = this.fillPaint = mainLinePaint;
            }

            if (outlineStroke instanceof BasicStroke) {
                float lineWidth = ((BasicStroke) outlineStroke).getLineWidth();
                this.edgeLineStroke = new BasicStroke(lineWidth / 2);
                this.mainLineStroke = new BasicStroke(lineWidth * 2);
            } else {
                this.edgeLineStroke = outlineStroke;
            }

        }

        public void draw(Graphics2D g2) {
            g2.setPaint(fillPaint);
            g2.fillPolygon(x, y, 4);

            g2.setStroke(edgeLineStroke);
            g2.setPaint(edgeLinePaint);
            g2.drawLine(x[0], y[0], x[1], y[1]);
            g2.drawLine(x[0], y[0], x[3], y[3]);

            g2.setStroke(mainLineStroke);
            g2.setPaint(mainLinePaint);
            g2.drawLine(x[0], y[0], x[2], y[2]);
        }

        private double calcAlpha(Color c) {
            double gray = (0.30 * c.getRed() +
                           0.59 * c.getGreen() +
                           0.11 * c.getBlue()) / 255;
            // the brighter the color, the higher an alpha value we need.
            // the darker the color, the lower an alpha value we need.
            // green (0.59) should become 0.3;  yellow (0.89) should map to 0.8
            double result = 0.123 / (1 - gray);
            return (result > 0 && result < 1) ? result : 0.3;
        }

        private Color transp(Color c, double alpha) {
            return new Color
                (c.getRed(), c.getGreen(), c.getBlue(), (int) (255 * alpha));
        }

        private Point2D gradientStart(double x0, double y0,
                                      double x1, double y1,
                                      double x2, double y2,
                                      double x3, double y3) {
            double dy = x0 - x1;
            double dx = y1 - y0;
            double startLen = Math.sqrt(dy * dy + dx * dx);
            if (startLen == 0) throw new IllegalArgumentException();

            Line2D line = new Line2D.Double(x0, y0, x1, y1);
            double len2 = line.ptLineDist(x2, y2);
            double len3 = line.ptLineDist(x3, y3);
            double len = 10;
            len = Math.max(len, len2);
            len = Math.max(len, len3);

            double fraction = len / startLen;

            return new Point2D.Double(x1 + dx * fraction, y1 + dy * fraction);
        }

    }

}
