// Copyright (C) 2008-2018 Tuma Solutions, LLC
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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.jfree.chart.PaintMap;
import org.jfree.chart.StrokeMap;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.event.PlotChangeEvent;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.data.general.PieDataset;

import net.sourceforge.processdash.ui.lib.PaintUtils;

public class StandardDiscItemRenderer implements DiscItemRenderer {

    public interface PaintKeyMapper {
        public Comparable getPaintKey(Comparable dataKey);
        public Paint getPreferredPaint(DrawingSupplier ds, Comparable paintKey);
    }

    /** The plot. */
    private DiscPlot plot;

    /** The disc paint map. */
    private PaintMap discPaintMap;

    /** An object that can lookup paint-specific keys for data elements */
    private PaintKeyMapper paintKeyMapper;

    /** The base disc paint (fallback). */
    private transient Paint baseDiscPaint;

    /** A paint key that should always map to the base disc paint. */
    private Comparable baseDiscPaintKey;

    /** The label paint map. */
    private PaintMap labelPaintMap;

    /** The outline paint map. */
    private PaintMap outlinePaintMap;

    /** The base outline paint (fallback). */
    private transient Paint baseOutlinePaint;

    /** The amount of padding to reserve between the edge of the disc and
     * the disc label. */
    private double labelPadding;

    /** The minimum number of characters to display in from the disc's label.
     * If this number of characters does not fit, no label will be displayed. */
    private int minLabelChars;

    /** The outline stroke map. */
    private StrokeMap outlineStrokeMap;

    /** The base outline stroke (fallback). */
    private transient Stroke baseOutlineStroke;

    public StandardDiscItemRenderer() {
        this.discPaintMap = new PaintMap();
        this.baseDiscPaint = Color.gray;
        this.labelPaintMap = new PaintMap();
        this.labelPadding = 5;
        this.minLabelChars = 4;
        this.outlinePaintMap = new PaintMap();
        this.baseOutlinePaint = null;
        this.outlineStrokeMap = new StrokeMap();
        this.baseOutlineStroke = Plot.DEFAULT_OUTLINE_STROKE;
    }

    /** Get the object that maps data keys to paint keys */
    public PaintKeyMapper getPaintKeyMapper() {
        return paintKeyMapper;
    }

    /**
     * Install an object that maps data keys to paint keys
     * @since 2.2.1
     */
    public void setPaintKeyMapper(PaintKeyMapper paintKeyMapper) {
        this.paintKeyMapper = paintKeyMapper;
        this.discPaintMap = new PaintMap();
    }

    public Comparable getBaseDiscPaintKey() {
        return baseDiscPaintKey;
    }

    public void setBaseDiscPaintKey(Comparable baseDiscPaintKey) {
        this.baseDiscPaintKey = baseDiscPaintKey;
    }

    /**
     * Returns the paint for the specified disc. This is equivalent to
     * <code>lookupDiscPaint(section, false)</code>.
     * 
     * @param key
     *                the disc key.
     * 
     * @return The paint for the specified disc.
     * 
     * @see #lookupDiscPaint(Comparable, boolean)
     */
    protected Paint lookupDiscPaint(Comparable key) {
        return lookupDiscPaint(key, false);
    }

    /**
     * Returns the paint for the specified disc.
     * 
     * @param key
     *                the disc key.
     * @param autoPopulate
     *                a flag that controls whether the drawing supplier is used
     *                to auto-populate the disc paint settings.
     * 
     * @return The paint.
     */
    protected Paint lookupDiscPaint(Comparable key, boolean autoPopulate) {

        // if a paint key mapper is in effect, use it to translate the key
        if (paintKeyMapper != null)
            key = paintKeyMapper.getPaintKey(key);

        // see if this is the key for the base color
        if (key.equals(baseDiscPaintKey))
            return baseDiscPaint;

        // check if there is a paint defined for the specified key
        Paint result = this.discPaintMap.getPaint(key);
        if (result != null) {
            return result;
        }

        // nothing defined - do we autoPopulate?
        if (autoPopulate) {
            DrawingSupplier ds = getDrawingSupplier();
            if (ds != null) {
                if (paintKeyMapper != null)
                    result = paintKeyMapper.getPreferredPaint(ds, key);
                if (result == null)
                    result = ds.getNextPaint();
                Paint bg = plot.getBackgroundPaint();
                if (result instanceof Color && bg instanceof Color) {
                    result = PaintUtils.adjustForContrast((Color) result,
                        (Color) bg);
                }
                this.discPaintMap.put(key, result);
            } else {
                result = this.baseDiscPaint;
            }
        } else {
            result = this.baseDiscPaint;
        }
        return result;
    }

    /**
     * Returns the label paint associated with the specified key, or
     * <code>null</code> if there is no paint associated with the key.
     * 
     * @param key  the key (<code>null</code> not permitted).
     * 
     * @return The paint associated with the specified key, or
     *     <code>null</code>.
     *
     * @throws IllegalArgumentException if <code>key</code> is
     *     <code>null</code>.
     * 
     * @see #setLabelPaint(Comparable, Paint)
     */
    public Paint getLabelPaint(Comparable key) {
        // null argument check delegated...
        return this.labelPaintMap.getPaint(key);
    }

    /**
     * Sets the label paint associated with the specified key, and sends a
     * {@link PlotChangeEvent} to all registered listeners.
     * 
     * @param key  the key (<code>null</code> not permitted).
     * @param paint  the paint.
     * 
     * @throws IllegalArgumentException if <code>key</code> is
     *     <code>null</code>.
     *
     * @see #getLabelPaint(Comparable)
     */
    public void setLabelPaint(Comparable key, Paint paint) {
        // null argument check delegated...
        this.labelPaintMap.put(key, paint);
    }

    protected Paint lookupLabelPaint(Comparable key, Paint discPaint) {
        Paint result = getLabelPaint(key);
        if (result == null) {
            result = plot.getLabelPaint();
        }

        if (result == DiscPlot.LABEL_PAINT_AUTO) {
            if (discPaint instanceof Color) {
                double gray = PaintUtils.toGray((Color) discPaint);
                if (gray > 128)
                    result = Color.black;
                else
                    result = Color.white;
            } else {
                result = Color.white;
            }
        }

        return result;
    }

    protected String lookupDiscLabel(PieDataset dataset, Comparable key) {
        PieSectionLabelGenerator labelGenerator = plot.getLabelGenerator();
        if (labelGenerator == null)
            return null;
        else
            return labelGenerator.generateSectionLabel(dataset, key);
    }

    /**
     * @return Returns the labelPadding.
     */
    public double getLabelPadding() {
        return labelPadding;
    }

    /**
     * @param labelPadding The labelPadding to set.
     */
    public void setLabelPadding(double labelPadding) {
        this.labelPadding = labelPadding;
    }

    /**
     * Returns the outline paint associated with the specified key, or
     * <code>null</code> if there is no paint associated with the key.
     * 
     * @param key  the key (<code>null</code> not permitted).
     * 
     * @return The paint associated with the specified key, or
     *     <code>null</code>.
     *
     * @throws IllegalArgumentException if <code>key</code> is
     *     <code>null</code>.
     * 
     * @see #setOutlinePaint(Comparable, Paint)
     */
    public Paint getOutlinePaint(Comparable key) {
        // null argument check delegated...
        return this.outlinePaintMap.getPaint(key);
    }

    /**
     * Sets the outline paint associated with the specified key, and sends a
     * {@link PlotChangeEvent} to all registered listeners.
     * 
     * @param key  the key (<code>null</code> not permitted).
     * @param paint  the paint.
     * 
     * @throws IllegalArgumentException if <code>key</code> is
     *     <code>null</code>.
     *
     * @see #getOutlinePaint(Comparable)
     */
    public void setOutlinePaint(Comparable key, Paint paint) {
        // null argument check delegated...
        this.outlinePaintMap.put(key, paint);
    }

    /**
     * Returns the base outline paint.  This is used when no other paint is
     * available.
     * 
     * @return The paint (possibly <code>null</code>).
     * 
     * @see #setBaseOutlinePaint(Paint)
     */
    public Paint getBaseOutlinePaint() {
        return this.baseOutlinePaint;
    }

    /**
     * Sets the base outline paint.
     * 
     * @param paint  the paint (<code>null</code> permitted).
     * 
     * @see #getBaseOutlinePaint()
     */
    public void setBaseOutlinePaint(Paint paint) {
        this.baseOutlinePaint = paint;
    }

    protected Paint lookupOutlinePaint(Comparable key) {
        Paint result = getOutlinePaint(key);
        if (result == null) {
            result = getBaseOutlinePaint();
        }
        return result;
    }


    /**
     * Returns the outline stroke associated with the specified key, or
     * <code>null</code> if there is no stroke associated with the key.
     * 
     * @param key  the key (<code>null</code> not permitted).
     * 
     * @return The stroke associated with the specified key, or
     *     <code>null</code>.
     *
     * @throws IllegalArgumentException if <code>key</code> is
     *     <code>null</code>.
     * 
     * @see #setOutlineStroke(Comparable, Stroke)
     */
    public Stroke getOutlineStroke(Comparable key) {
        // null argument check delegated...
        return this.outlineStrokeMap.getStroke(key);
    }

    /**
     * Sets the outline stroke associated with the specified key, and sends a
     * {@link PlotChangeEvent} to all registered listeners.
     * 
     * @param key  the key (<code>null</code> not permitted).
     * @param stroke  the stroke.
     * 
     * @throws IllegalArgumentException if <code>key</code> is
     *     <code>null</code>.
     *
     * @see #getOutlineStroke(Comparable)
     */
    public void setOutlineStroke(Comparable key, Stroke stroke) {
        // null argument check delegated...
        this.outlineStrokeMap.put(key, stroke);
    }

    /**
     * Returns the base outline stroke.  This is used when no other stroke is
     * available.
     * 
     * @return The stroke (possibly <code>null</code>).
     * 
     * @see #setBaseOutlineStroke(Stroke)
     */
    public Stroke getBaseOutlineStroke() {
        return this.baseOutlineStroke;
    }

    /**
     * Sets the base outline stroke.
     * 
     * @param stroke  the stroke (<code>null</code> permitted).
     * 
     * @see #getBaseOutlineStroke()
     */
    public void setBaseOutlineStroke(Stroke stroke) {
        this.baseOutlineStroke = stroke;
    }

    protected Stroke lookupOutlineStroke(Comparable key) {
        Stroke result = getOutlineStroke(key);
        if (result == null) {
            result = getBaseOutlineStroke();
        }
        return result;
    }

    /**
     * Returns the drawing supplier from the plot.
     * 
     * @return The drawing supplier (possibly <code>null</code>).
     */
    public DrawingSupplier getDrawingSupplier() {
        if (plot != null)
            return plot.getDrawingSupplier();
        else
            return null;
    }

    public DiscItemRendererState initialise(Graphics2D g2,
            Rectangle2D dataArea, DiscPlot plot, PieDataset dataset,
            PlotRenderingInfo info) {
        this.plot = plot;
        return new DiscItemRendererState(info);
    }

    public void drawItem(Graphics2D g2, DiscItemRendererState state,
            Rectangle2D dataArea, Ellipse2D discLocation,
            PlotRenderingInfo info, DiscPlot plot, PieDataset dataset, int item) {

        Ellipse2D shape = plot.getDiscDistributor().getDiscLocation(item);
        if (shape == null)
            return;

        Comparable key = dataset.getKey(item);
        Paint discPaint = lookupDiscPaint(key, true);

        drawDisc(g2, shape, discPaint, item);
        drawLabel(g2, plot, dataset, shape, key, discPaint);
        drawOutline(g2, shape, key);

        if (info != null) {
            EntityCollection entities = state.getEntityCollection();
            if (entities != null) {
                String tip = null;
                if (plot.getToolTipGenerator() != null) {
                    tip = plot.getToolTipGenerator().generateToolTip(
                            dataset, key);
                }
                String url = null;
                if (plot.getUrlGenerator() != null) {
                    url = plot.getUrlGenerator().generateURL(dataset,
                            key, item);
                }
                DiscItemEntity entity = new DiscItemEntity(shape, dataset,
                        item, key, tip, url);
                entities.add(entity);
            }
        }

    }

    protected void drawOutline(Graphics2D g2, Ellipse2D shape, Comparable key) {
        Paint outlinePaint = lookupOutlinePaint(key);
        Stroke outlineStroke = lookupOutlineStroke(key);
        if (outlinePaint != null && outlineStroke != null) {
            g2.setPaint(outlinePaint);
            g2.draw(shape);
        }
    }

    protected void drawLabel(Graphics2D g2, DiscPlot plot, PieDataset dataset,
            Ellipse2D shape, Comparable key, Paint discPaint) {
        Paint labelPaint = lookupLabelPaint(key, discPaint);
        Font labelFont = plot.getLabelFont();
        String label = lookupDiscLabel(dataset, key);
        if (labelPaint != null && labelFont != null && label != null) {
            drawDiscLabel(g2, shape, labelPaint, labelFont, label);
        }
    }

    protected void drawDisc(Graphics2D g2, Ellipse2D shape, Paint discPaint, int item) {
        g2.setPaint(discPaint);
        g2.fill(shape);
    }

    protected void drawDiscLabel(Graphics2D g2, Ellipse2D shape,
            Paint labelPaint, Font labelFont, String label) {
        g2.setFont(labelFont);
        g2.setPaint(labelPaint);
        FontMetrics m = g2.getFontMetrics();
        int height = m.getAscent();
        double halfHeight = height / 2.0;

        double radius = shape.getWidth() / 2;
        double availableRadius = radius - getLabelPadding();
        double halfWidth = Math.sqrt(availableRadius * availableRadius
                - halfHeight * halfHeight);

        int width = (int) Math.floor(halfWidth * 2 + 0.99);

        Rectangle viewR = new Rectangle(width, height);
        Rectangle iconR = new Rectangle();
        Rectangle textR = new Rectangle();
        String text = SwingUtilities.layoutCompoundLabel(m, label, null,
            SwingConstants.CENTER, SwingConstants.CENTER,
            SwingConstants.CENTER, SwingConstants.TRAILING, viewR, iconR,
            textR, 0);
        if (text.equals(label) || text.length() >= 3 + minLabelChars) {
            double x = shape.getCenterX() - halfWidth + textR.x;
            double y = shape.getCenterY() + halfHeight + textR.y;
            g2.drawString(text, (float) x, (float) y);
        }
    }

}
