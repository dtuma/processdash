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


import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jfree.chart.event.PlotChangeEvent;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.PlotState;
import org.jfree.data.general.PieDataset;
import org.jfree.ui.RectangleInsets;

/**
 * A plot that displays data in the form of a radar chart, using data
 * from any class that implements the CategoryDataSource interface.
 * <P>
 * Notes:
 * (1) negative values in the dataset are ignored;
 * (2) vertical axis and horizontal axis are set to null;
 * (3) there are utility methods for creating a CategoryDataSource from a
 * CategoryDataset;
 * @see Plot
 * @see CategoryDataSource */
public class RadarPlot extends Plot {

    /** The default interior gap percent (currently 20%). */
    public static final double DEFAULT_INTERIOR_GAP = 0.20;

    /** The maximum interior gap (currently 40%). */
    public static final double MAX_INTERIOR_GAP = 0.40;

    /** The default radius percent (currently 100%). */
    public static final double DEFAULT_RADIUS = 1.00;

    /** The maximum radius (currently 100%). */
    public static final double MAX_RADIUS = 1.00;

    /** The default axis label font. */
    public static final Font DEFAULT_AXIS_LABEL_FONT =
        new Font("SansSerif", Font.PLAIN, 10);

    /** The default axis label paint. */
    public static final Paint DEFAULT_AXIS_LABEL_PAINT = Color.black;

    /** The default axis label gap (currently 10%). */
    public static final double DEFAULT_AXIS_LABEL_GAP = 0.10;

    /** The maximum interior gap (currently 30%). */
    public static final double MAX_AXIS_LABEL_GAP = 0.30;

    /** The default stroke for the series line */
    public static final Stroke DEFAULT_LINE_STROKE = new BasicStroke(3.0f);

    /** A magic color object used to designate adaptive coloring, based
     *  on the computed quality index */
    public static final Paint ADAPTIVE_COLORING = new Color(0);

    /** The dataset for the radar chart. */
    private PieDataset dataset;

    /** The amount of space left around the outside of the radar
        chart, expressed as a percentage. */
    protected double interiorGap;

//    /** Flag determining whether to draw an ellipse or a perfect circle. */
//    protected boolean circular;

    /** The radius as a percentage of the available drawing area. */
    protected double radius;

    /** The font used to display the axis labels. */
    protected Font axisLabelFont;

    /** The color used to draw the axis labels. */
    protected Paint axisLabelPaint;

    /** The gap between the labels and the radar axes, as a
        percentage of the radius. */
    protected double axisLabelGap;

    /** Whether or not axis labels should be drawn */
    protected boolean showAxisLabels;

    /** The color used to paint the axis lines (i.e. spokes) */
    protected Paint axisPaint;

    /** The stroke used to paint the axis lines (i.e. spokes) */
    protected Stroke axisStroke;

    /** The color used to paint the grid lines */
    protected Paint gridLinePaint;

    /** The stroke used to paint the grid lines */
    protected Stroke gridLineStroke;

    /** The color to use to draw the data polygon */
    protected Paint plotLinePaint;

    /** The stroke used to draw the data polygon */
    protected Stroke plotLineStroke;

    /**
     * Constructs a new radar chart, using default attributes as required.
     */
    public RadarPlot() {
        this(null);
    }

    public RadarPlot(PieDataset dataset) {
        super();
        this.dataset = dataset;
        initialise();
    }

    private void initialise() {
        this.interiorGap = DEFAULT_INTERIOR_GAP;
//        this.circular = true;
        this.radius = DEFAULT_RADIUS;
        this.showAxisLabels = true;
        this.axisLabelFont = DEFAULT_AXIS_LABEL_FONT;
        this.axisLabelPaint = DEFAULT_AXIS_LABEL_PAINT;
        this.axisLabelGap = DEFAULT_AXIS_LABEL_GAP;
//        this.itemLabelGenerator = null;
//        this.urlGenerator = null;

        this.plotLinePaint = ADAPTIVE_COLORING;
        this.axisPaint = Color.black;
        this.axisStroke = DEFAULT_OUTLINE_STROKE;
        this.gridLinePaint = Color.lightGray;
        this.gridLineStroke = DEFAULT_OUTLINE_STROKE;
        this.plotLineStroke = DEFAULT_LINE_STROKE;
        setForegroundAlpha(0.5f);
        setInsets(new RectangleInsets(0, 5, 5, 5));
    }

    /**
     * Returns the interior gap, measured as a percentage of the
     * available drawing space.
     *
     * @return The gap percentage.  */
    public double getInteriorGap() {
        return this.interiorGap;
    }

    /**
     * Sets the interior gap.
     *
     * @param percent The gap.
     */
    public void setInteriorGap(double percent) {

        // check arguments...
        if ((percent < 0.0) || (percent > MAX_INTERIOR_GAP)) {
            throw new IllegalArgumentException(
                "RadarPlot.setInteriorGap(double): percentage "+
                "outside valid range.");
        }

        // make the change...
        if (this.interiorGap != percent) {
            this.interiorGap = percent;
            notifyListeners(new PlotChangeEvent(this));
        }

    }

//    /**
//     * Returns a flag indicating whether the pie chart is circular, or
//     * stretched into an elliptical shape.
//     *
//     * @return a flag indicating whether the pie chart is circular.
//     */
//    public boolean isCircular() {
//        return circular;
//    }
//
//    /**
//     * A flag indicating whether the pie chart is circular, or stretched
//     * into an elliptical shape.
//     *
//     * @param flag  the new value.
//     */
//    public void setCircular(boolean flag) {
//
//        // no argument checking required...
//        // make the change...
//        if (circular != flag) {
//            circular = flag;
//            notifyListeners(new PlotChangeEvent(this));
//        }
//
//    }

    /**
     * Returns the radius (a percentage of the available space).
     *
     * @return The radius percentage.
     */
    public double getRadius() {
        return this.radius;
    }

    /**
     * Sets the radius.
     *
     * @param percent  the new value.
     */
    public void setRadius(double percent) {

        // check arguments...
        if ((percent <= 0.0) || (percent > MAX_RADIUS)) {
            throw new IllegalArgumentException(
                "RadarPlot.setRadius(double): percentage "+
                "outside valid range.");
        }

        // make the change (if necessary)...
        if (this.radius != percent) {
            this.radius = percent;
            notifyListeners(new PlotChangeEvent(this));
        }

    }

    /**
     * Returns the axis label font.
     * @return The axis label font.
     */
    public Font getAxisLabelFont() {
        return this.axisLabelFont;
    }

    /**
     * Sets the axis label font.
     * <P>
     * Notifies registered listeners that the plot has been changed.
     * @param font The new axis label font.
     */
    public void setAxisLabelFont(Font font) {

        // check arguments...
        if (font==null) {
            throw new IllegalArgumentException
                ("RadarPlot.setAxisLabelFont(...): "
                 +"null font not allowed.");
        }

        // make the change...
        if (!this.axisLabelFont.equals(font)) {
            this.axisLabelFont = font;
            notifyListeners(new PlotChangeEvent(this));
        }

    }

    /**
     * Returns the axis label paint.
     * @return The axis label paint.
     */
    public Paint getAxisLabelPaint() {
        return this.axisLabelPaint;
    }

    /**
     * Sets the axis label paint.
     * <P>
     * Notifies registered listeners that the plot has been changed.
     * @param paint The new axis label paint.
     */
    public void setAxisLabelPaint(Paint paint) {

        // check arguments...
        if (paint==null) {
            throw new IllegalArgumentException
                ("RadarPlot.setAxisLabelPaint(...): "
                 +"null paint not allowed.");
        }

        // make the change...
        if (!this.axisLabelPaint.equals(paint)) {
            this.axisLabelPaint = paint;
            notifyListeners(new PlotChangeEvent(this));
        }

    }

    /**
     * Returns the plot line paint.
     * @return The plot line paint.
     */
    public Paint getPlotLinePaint() {
        return this.plotLinePaint;
    }

    /**
     * Sets the plot line paint.
     * <P>
     * Notifies registered listeners that the plot has been changed.
     * @param paint The new plot line paint.
     */
    public void setPlotLinePaint(Paint paint) {

        // check arguments...
        if (paint==null) {
            throw new IllegalArgumentException
                ("RadarPlot.setPlotPaint(...): "
                 +"null paint not allowed.");
        }

        // make the change...
        if (!this.plotLinePaint.equals(paint)) {
            this.plotLinePaint = paint;
            notifyListeners(new PlotChangeEvent(this));
        }

    }

    /**
     * Returns the axis label gap, measures as a percentage of the radius.
     * @return The axis label gap, measures as a percentage of the radius.
     */
    public double getAxisLabelGap() {
        return this.axisLabelGap;
    }

    /**
     * Sets the axis label gap percent.
     */
    public void setAxisLabelGap(double percent) {

        // check arguments...
        if ((percent<0.0) || (percent>MAX_AXIS_LABEL_GAP)) {
            throw new IllegalArgumentException
                ("RadarPlot.setAxisLabelGap(double): "
                 +"percentage outside valid range.");
        }

        // make the change...
        if (this.axisLabelGap!=percent) {
            this.axisLabelGap = percent;
            notifyListeners(new PlotChangeEvent(this));
        }

    }

    /**
     * Returns the show axis labels flag.
     *
     * @return the show axis label flag.
     */
    public boolean getShowAxisLabels () {
        return (this.showAxisLabels);
    }

    /**
     * Sets the show axis labels flag.
     * <P>
     * Notifies registered listeners that the plot has been changed.
     *
     * @param flag  the new show axis labels flag.
     */
    public void setShowAxisLabels(boolean flag) {
        if (this.showAxisLabels != flag) {
            this.showAxisLabels = flag;
            notifyListeners(new PlotChangeEvent(this));
        }
    }

    /**
     * Returns the dataset for the plot, cast as a CategoryDataSource.
     * <P>
     * Provided for convenience.
     * @return The dataset for the plot, cast as a CategoryDataSource.
     */
    public PieDataset getPieDataset() {
        return dataset;
    }

    /**
     * Returns a collection of the section keys (or categories) in the dataset.
     *
     * @return the categories.
     */
    public Collection getKeys() {
        if (dataset != null)
            return Collections.unmodifiableCollection(dataset.getKeys());
        else
            return null;
    }

    /**
     * Draws the plot on a Java 2D graphics device (such as the screen
     * or a printer).
     * @param g2 The graphics device.
     * @param plotArea The area within which the plot should be drawn.
     */
    @Override
    public void draw(Graphics2D g2, Rectangle2D plotArea, Point2D anchor,
            PlotState state, PlotRenderingInfo info) {
        // adjust for insets...
        RectangleInsets insets = getInsets();
        if (insets!=null) {
            plotArea.setRect(plotArea.getX()+insets.getLeft(),
                             plotArea.getY()+insets.getTop(),
                             plotArea.getWidth()-insets.getLeft()-insets.getRight(),
                             plotArea.getHeight()-insets.getTop()-insets.getBottom());
        }

        if (info != null) {
            info.setPlotArea(plotArea);
            info.setDataArea(plotArea);
        }

        drawBackground(g2, plotArea);
        drawOutline(g2, plotArea);

        Shape savedClip = g2.getClip();
        g2.clip(plotArea);

        Composite originalComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance
                        (AlphaComposite.SRC_OVER, getForegroundAlpha()));

        if (this.dataset != null) {
            drawRadar(g2, plotArea, info, 0, this.dataset);
        } else {
            drawNoDataMessage(g2, plotArea);
        }

        g2.clip(savedClip);
        g2.setComposite(originalComposite);

        drawOutline(g2, plotArea);

    }



    protected void drawRadar(Graphics2D g2, Rectangle2D plotArea,
                             PlotRenderingInfo info, int pieIndex,
                             PieDataset data) {

        // adjust the plot area by the interior spacing value
        double gapHorizontal = plotArea.getWidth() * this.interiorGap;
        double gapVertical = plotArea.getHeight() * this.interiorGap;
        double radarX = plotArea.getX() + gapHorizontal / 2;
        double radarY = plotArea.getY() + gapVertical / 2;
        double radarW = plotArea.getWidth() - gapHorizontal;
        double radarH = plotArea.getHeight() - gapVertical;

        // make the radar area a square if the radar chart is to be circular...
        // NOTE that non-circular radar charts are not currently supported.
        if (true) { //circular) {
            double min = Math.min(radarW, radarH) / 2;
            radarX = (radarX + radarX + radarW) / 2 - min;
            radarY = (radarY + radarY + radarH) / 2 - min;
            radarW = 2 * min;
            radarH = 2 * min;
        }

        double radius = radarW / 2;
        double centerX = radarX + radarW / 2;
        double centerY = radarY + radarH / 2;

        Rectangle2D radarArea = new Rectangle2D.Double
            (radarX, radarY, radarW, radarH);

        // plot the data (unless the dataset is null)...
        if ((data != null) && (data.getKeys().size() > 0)) {

            // get a list of categories...
            List keys = data.getKeys();
            int numAxes = keys.size();

            // draw each of the axes on the radar chart, and register
            // the shape of the radar line.

            double multiplier = 1.0;
            GeneralPath lineShape =
                new GeneralPath(GeneralPath.WIND_NON_ZERO, numAxes+1);
            GeneralPath gridShape =
                new GeneralPath(GeneralPath.WIND_NON_ZERO, numAxes+1);

            int axisNumber = -1;
            Iterator iterator = keys.iterator();
            while (iterator.hasNext()) {
                Comparable currentKey = (Comparable) iterator.next();
                axisNumber++;
                Number dataValue = data.getValue(currentKey);

                double value =
                    (dataValue != null ? dataValue.doubleValue() : 0);
                if (value > 1 || Double.isNaN(value) ||
                    Double.isInfinite(value)) value = 1.0;
                if (value < 0) value = 0.0;
                multiplier *= value;

                double angle = 2 * Math.PI * axisNumber / numAxes;
                double deltaX = Math.sin(angle) * radius;
                double deltaY = - Math.cos(angle) * radius;

                // draw the spoke
                g2.setPaint(axisPaint);
                g2.setStroke(axisStroke);
                Line2D line = new Line2D.Double
                    (centerX, centerY, centerX + deltaX, centerY + deltaY);
                g2.draw(line);

                // register the grid line and the shape line
                if (axisNumber == 0) {
                    gridShape.moveTo((float)deltaX, (float)deltaY);
                    lineShape.moveTo((float)(deltaX*value),
                                     (float)(deltaY*value));
                } else {
                    gridShape.lineTo((float)deltaX, (float)deltaY);
                    lineShape.lineTo((float)(deltaX*value),
                                     (float)(deltaY*value));
                }

                if (showAxisLabels) {
                    // draw the label
                    double labelX = centerX + deltaX*(1+axisLabelGap);
                    double labelY = centerY + deltaY*(1+axisLabelGap);
                    String label = currentKey.toString();
                    drawLabel(g2, radarArea, label, axisNumber, labelX,
                              labelY);
                }

            }
            gridShape.closePath();
            lineShape.closePath();

            // draw five gray concentric gridlines
            g2.translate(centerX, centerY);
            g2.setPaint(gridLinePaint);
            g2.setStroke(gridLineStroke);
            for (int i = 5;   i > 0;   i--) {
                Shape scaledGrid = gridShape.createTransformedShape
                    (AffineTransform.getScaleInstance(i / 5.0, i/5.0));
                g2.draw(scaledGrid);
            }

            // get the color for the plot shape.
            Paint dataPaint = plotLinePaint;
            if (dataPaint == ADAPTIVE_COLORING) {
                //multiplier = Math.exp(Math.log(multiplier) * 2 / numAxes);
                dataPaint = getMultiplierColor((float)multiplier);
            }

            // compute a slightly transparent version of the plot color for
            // the fill.
            Paint dataFill = null;
            if (dataPaint instanceof Color &&
                getForegroundAlpha() != 1.0)
                dataFill = new Color(((Color)dataPaint).getRed() / 255f,
                                     ((Color)dataPaint).getGreen() / 255f,
                                     ((Color)dataPaint).getBlue() / 255f,
                                     getForegroundAlpha());
            else
                dataFill = dataPaint;

            // draw the plot shape.  First fill with a parially
            // transparent color, then stroke with the opaque color.
            g2.setPaint(dataFill);
            g2.fill(lineShape);
            g2.setPaint(dataPaint);
            g2.setStroke(plotLineStroke);
            g2.draw(lineShape);

            // cleanup the graphics context.
            g2.translate(-centerX, -centerY);
        }
    }

    /** Calculate an appropriate color for the quality chart.
     * if the multiplier is 0, use red; if it is 1, use green;
     * use yellow in between, and fade proportionately.
     */
    private Paint getMultiplierColor(float value) {
        if (value > 0.4)
            return Color.green;
        else if (value > 0.2)
            // at 0.4, red component should be 0.0; at 0.2, it should be 1.0
            return new Color(2 - 5*value, 1, 0, 1);
        else
            // at 0.0, green component should be 0.0; at 0.2, it should be 1.0
            return new Color(1, 5*value, 0, 1);
    }


    /**
     * Draws the label for one radar axis.
     *
     * @param g2 The graphics device.
     * @param chartArea The area for the radar chart.
     * @param data The data for the plot.
     * @param axis The axis (zero-based index).
     * @param startAngle The starting angle.
     */
    protected void drawLabel(Graphics2D g2, Rectangle2D chartArea,
                             String label, int axis,
                             double labelX, double labelY) {

        // handle label drawing...
        FontRenderContext frc = g2.getFontRenderContext();
        Rectangle2D labelBounds =
            this.axisLabelFont.getStringBounds(label, frc);
        LineMetrics lm = this.axisLabelFont.getLineMetrics(label, frc);
        double ascent = lm.getAscent();

        if (labelX == chartArea.getCenterX())
            labelX -= labelBounds.getWidth() / 2;
        else if (labelX < chartArea.getCenterX())
            labelX -= labelBounds.getWidth();
        if (labelY > chartArea.getCenterY())
                labelY += ascent;

        g2.setPaint(this.axisLabelPaint);
        g2.setFont(this.axisLabelFont);
        g2.drawString(label, (float)labelX, (float)labelY);
    }


    /**
     * Returns a short string describing the type of plot.
     */
    @Override
    public String getPlotType() {
        return "Radar Chart";
    }


    /**
     * A zoom method that does nothing.
     * <p>
     * Plots are required to support the zoom operation.  In the case
     * of a radar chart, it doesn't make sense to zoom in or out, so
     * the method is empty.
     *
     * @param percent The zoom percentage.
     */
    @Override
    public void zoom(double percent) {
    }

}
