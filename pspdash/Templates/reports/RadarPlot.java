/*
 * Radar chart plotter, designed for drawing quality profiles
 */

package com.jrefinery.chart;


import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Color;
import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.Insets;
import java.awt.Image;
import java.awt.Shape;
import java.awt.Composite;
import java.awt.AlphaComposite;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.GeneralPath;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Iterator;
import com.jrefinery.chart.*;
import com.jrefinery.chart.event.*;

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

    /** A magic color object used to designate adaptive coloring, based
     *  on the computed quality index */
    public static final Paint ADAPTIVE_COLORING = new Color(0);

    /** The amount of space left around the outside of the radar
        chart, expressed as a percentage. */
    protected double interiorGapPercent;

    /** Flag determining whether to draw an ellipse or a perfect circle. */
    protected boolean circular;

    /** The radius as a percentage of the available drawing area. */
    protected double radiusPercent;

    /** The font used to display the axis labels. */
    protected Font axisLabelFont;

    /** The color used to draw the axis labels. */
    protected Paint axisLabelPaint;

    /** The gap between the labels and the radar axes, as a
        percentage of the radius. */
    protected double axisLabelGapPercent;

    /** The color to use to draw the data polygon */
    protected Paint radarSeriesPaint;

    /** Whether or not axis labels should be drawn */
    protected boolean drawAxisLabels = true;

    /**
     * Constructs a new radar chart, using default attributes as required.
     */
    public RadarPlot(JFreeChart chart) throws AxisNotCompatibleException {
        this(chart,
             new Insets(0, 5, 5, 5),
             Color.white,
             null,
             1.0f,
             new BasicStroke(1),
             Color.gray,
             1.0f,
             RadarPlot.DEFAULT_INTERIOR_GAP,
             true, // circular
             DEFAULT_RADIUS,
             DEFAULT_AXIS_LABEL_FONT,
             DEFAULT_AXIS_LABEL_PAINT,
             DEFAULT_AXIS_LABEL_GAP,
             ADAPTIVE_COLORING);
    }


    /**
     * Constructs a radar chart.
     * @param chart The chart that the plot belongs to;
     * @param insets Amount of blank space around the plot area.
     * @param backgroundPaint An optional color for the plot's background.
     * @param backgroundImage An optional image for the plot's background.
     * @param backgroundAlpha Alpha-transparency for the plot's background.
     * @param outlineStroke The Stroke used to draw an outline around the plot.
     * @param outlinePaint The color used to draw an outline around the plot.
     * @param foregroundAlpha The alpha-transparency for the plot.
     * @param interiorGapPercent The interior gap (space for labels) as a
     *        percentage of the available space.
     * @param circular Flag indicating whether the radar chart is circular or
     *        elliptical.
     * @param radiusPercent The radius of the radar chart, as a percentage of
     *        the available space (after accounting for interior gap).
     * @param axisLabelFont The font for the axis labels.
     * @param axisLabelPaint The color for the axis labels.
     * @param axisLabelGapPercent The space between the radar axes and
     *        the labels.
     */
    public RadarPlot(JFreeChart chart, Insets insets, Paint backgroundPaint,
                     Image backgroundImage, float backgroundAlpha,
                     Stroke outlineStroke, Paint outlinePaint,
                     float foregroundAlpha, double interiorGapPercent,
                     boolean circular, double radiusPercent,
                     Font axisLabelFont, Paint axisLabelPaint,
                     double axisLabelGapPercent, Paint dataPaint)
        throws AxisNotCompatibleException {

        super(chart, new BlankAxis(), new BlankAxis(), insets,
              backgroundPaint, outlineStroke, outlinePaint);

        this.interiorGapPercent = interiorGapPercent;
        this.circular = circular;
        this.radiusPercent = radiusPercent;
        this.axisLabelFont = axisLabelFont;
        this.axisLabelPaint = axisLabelPaint;
        this.axisLabelGapPercent = axisLabelGapPercent;
        this.radarSeriesPaint = dataPaint;
        setInsets(insets);

    }

    /**
     * Returns the interior gap, measures as a percentage of the
     * available drawing space.
     * @return The interior gap, measured as a percentage of the
     * available drawing space.
     */
    public double getInteriorGapPercent() {
        return this.interiorGapPercent;
    }

    /**
     * Sets the interior gap percent.
     */
    public void setInteriorGapPercent(double percent) {

        // check arguments...
        if ((percent<0.0) || (percent>MAX_INTERIOR_GAP)) {
            throw new IllegalArgumentException
                ("RadarPlot.setInteriorGapPercent(double): "
                 +"percentage outside valid range.");
        }

        // make the change...
        if (this.interiorGapPercent!=percent) {
            this.interiorGapPercent = percent;
            notifyListeners(new PlotChangeEvent(this));
        }

    }

    /**
     * Returns a flag indicating whether the radar chart is circular,
     * or stretched into an elliptical shape.
     * @return A flag indicating whether the radar chart is circular.
     */
    public boolean isCircular() {
        return circular;
    }

    /**
     * A flag indicating whether the radar chart is circular, or
     * stretched into an elliptical shape.
     * @param flag The new value.
     */
    public void setCircular(boolean flag) {

        // no argument checking required...
        // make the change...
        if (circular!=flag) {
            circular = flag;
            this.notifyListeners(new PlotChangeEvent(this));
        }

    }

    /**
     * Returns the radius percentage.
     * @return The radius percentage.
     */
    public double getRadiusPercent() {
        return this.radiusPercent;
    }

    /**
     * Sets the radius percentage.
     * @param percent The new value.
     */
    public void setRadiusPercent(double percent) {

        // check arguments...
        if ((percent<=0.0) || (percent>MAX_RADIUS)) {
            throw new IllegalArgumentException
                ("RadarPlot.setRadiusPercent(double): "
                 +"percentage outside valid range.");
        }

        // make the change (if necessary)...
        if (this.radiusPercent!=percent) {
            this.radiusPercent = percent;
            this.notifyListeners(new PlotChangeEvent(this));
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
        return this.radarSeriesPaint;
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
        if (!this.radarSeriesPaint.equals(paint)) {
            this.radarSeriesPaint = paint;
            notifyListeners(new PlotChangeEvent(this));
        }

    }

    /**
     * Returns the axis label gap, measures as a percentage of the radius.
     * @return The axis label gap, measures as a percentage of the radius.
     */
    public double getAxisLabelGapPercent() {
        return this.axisLabelGapPercent;
    }

    /**
     * Sets the axis label gap percent.
     */
    public void setAxisLabelGapPercent(double percent) {

        // check arguments...
        if ((percent<0.0) || (percent>MAX_AXIS_LABEL_GAP)) {
            throw new IllegalArgumentException
                ("RadarPlot.setAxisLabelGapPercent(double): "
                 +"percentage outside valid range.");
        }

        // make the change...
        if (this.axisLabelGapPercent!=percent) {
            this.axisLabelGapPercent = percent;
            notifyListeners(new PlotChangeEvent(this));
        }

    }

    public void setDrawAxisLabels(boolean draw) {
        drawAxisLabels = draw;
    }

    /**
     * Returns the dataset for the plot, cast as a CategoryDataSource.
     * <P>
     * Provided for convenience.
     * @return The dataset for the plot, cast as a CategoryDataSource.
     */
    public CategoryDataSource getDataset() {
        return (CategoryDataSource)chart.getDataSource();
    }
    public CategoryDataSource getDataSource() { return getDataset(); }

    /**
     * Returns a collection of the categories in the dataset.
     * @return A collection of the categories in the dataset.
     */
    public Collection getCategories() {
        return getDataset().getCategories();
    }

    /**
     * Draws the plot on a Java 2D graphics device (such as the screen
     * or a printer).
     * @param g2 The graphics device.
     * @param plotArea The area within which the plot should be drawn.
     */
    public void draw(Graphics2D g2, Rectangle2D plotArea) {

        // adjust for insets...
        if (insets!=null) {
            plotArea.setRect(plotArea.getX()+insets.left,
                             plotArea.getY()+insets.top,
                             plotArea.getWidth()-insets.left-insets.right,
                             plotArea.getHeight()-insets.top-insets.bottom);
        }

        // draw the outline and background
        drawOutlineAndBackground(g2, plotArea);

        // adjust the plot area by the interior spacing value
        double gapHorizontal = plotArea.getWidth()*this.interiorGapPercent;
        double gapVertical = plotArea.getHeight()*this.interiorGapPercent;
        double radarX = plotArea.getX()+gapHorizontal/2;
        double radarY = plotArea.getY()+gapVertical/2;
        double radarW = plotArea.getWidth()-gapHorizontal;
        double radarH = plotArea.getHeight()-gapVertical;

        // make the radar area a square if the radar chart is to be circular...
        // NOTE that non-circular radar charts are not currently supported.
        if (true) { //circular) {
            double min = Math.min(radarW, radarH)/2;
            radarX = (radarX+radarX+radarW)/2 - min;
            radarY = (radarY+radarY+radarH)/2 - min;
            radarW = 2*min;
            radarH = 2*min;
        }

        double radius = radarW/2;
        double centerX = radarX + radarW/2;
        double centerY = radarY + radarH/2;

        Rectangle2D radarArea = new Rectangle2D.Double
            (radarX, radarY, radarW, radarH);

        // plot the data (unless the dataset is null)...
        CategoryDataSource data = (CategoryDataSource)chart.getDataSource();
        if (data != null) {

            Shape savedClip = g2.getClip();
            g2.clip(plotArea);

            // get a sorted collection of categories...
            Object category = data.getCategories().iterator().next();
            int numAxes = data.getSeriesCount();

            // draw each of the axes on the radar chart, and register
            // the shape of the radar line.

            double multiplier = 1.0;
            GeneralPath lineShape =
                new GeneralPath(GeneralPath.WIND_NON_ZERO, numAxes+1);
            GeneralPath gridShape =
                new GeneralPath(GeneralPath.WIND_NON_ZERO, numAxes+1);

            Paint axisPaint = Color.black;
            Paint gridPaint = Color.lightGray;
            g2.setStroke(new BasicStroke());

            for (int axisNumber = 0;   axisNumber < numAxes;   axisNumber++) {
                Number dataValue = data.getValue(axisNumber, category);
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

                if (drawAxisLabels) {
                    // draw the label
                    double labelX = centerX + deltaX*(1+axisLabelGapPercent);
                    double labelY = centerY + deltaY*(1+axisLabelGapPercent);
                    String label = data.getSeriesName(axisNumber);
                    drawLabel(g2, radarArea, label, axisNumber, labelX,
                              labelY);
                }

            }
            gridShape.closePath();
            lineShape.closePath();

            // draw five gray concentric gridlines
            g2.translate(centerX, centerY);
            g2.setPaint(gridPaint);
            for (int i = 5;   i > 0;   i--) {
                Shape scaledGrid = gridShape.createTransformedShape
                    (AffineTransform.getScaleInstance(i / 5.0, i/5.0));
                g2.draw(scaledGrid);
            }

            // get the color for the plot shape.
            Paint dataPaint = radarSeriesPaint;
            if (radarSeriesPaint == ADAPTIVE_COLORING) {
                //multiplier = Math.exp(Math.log(multiplier) * 2 / numAxes);
                dataPaint = getMultiplierColor((float)multiplier);
            }

            // compute a slightly transparent version of the plot color for
            // the fill.
            Paint dataFill = null;
            if (dataPaint instanceof Color)
                dataFill = new Color(((Color)dataPaint).getRed(),
                                     ((Color)dataPaint).getGreen(),
                                     ((Color)dataPaint).getBlue(), 100);
            else
                dataFill = dataPaint;

            // draw the plot shape.  First fill with a parially
            // transparent color, then stroke with the opaque color.
            g2.setPaint(dataFill);
            g2.fill(lineShape);
            g2.setPaint(dataPaint);
            g2.setStroke(new BasicStroke(3.0f));
            g2.draw(lineShape);

            // cleanup the graphics context.
            g2.translate(-centerX, -centerY);
            g2.setClip(savedClip);
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
    public String getPlotType() {
        return "Radar Chart";
    }

    /**
     * Returns true if the axis is compatible with the radar chart,
     * and false otherwise.  Since a radar plot requires no axes, only
     * a null axis is compatible.
     * @param axis The axis.
     */
    public boolean isCompatibleHorizontalAxis(Axis axis) {
        if (axis==null) return true;
        else return false;
    }

    /**
     * Returns true if the axis is compatible with the radar chart,
     * and false otherwise.  Since a radar plot requires no axes, only
     * a null axis is compatible.
     * @param axis The axis.
     */
    public boolean isCompatibleVerticalAxis(Axis axis) {
        if (axis==null) return true;
        else return false;
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
    public void zoom(double percent) {
    }

}
