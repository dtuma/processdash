// Copyright (C) 2008 Tuma Solutions, LLC
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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.PlotChangeEvent;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.labels.PieToolTipGenerator;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.labels.StandardPieToolTipGenerator;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.PlotState;
import org.jfree.chart.plot.ValueAxisPlot;
import org.jfree.chart.urls.PieURLGenerator;
import org.jfree.data.Range;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.general.PieDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;

public class DiscPlot extends Plot implements ValueAxisPlot {

    /** The default interior gap. */
    public static final double DEFAULT_INTERIOR_GAP = 0.03;

    /** The maximum interior gap (currently 40%). */
    public static final double MAX_INTERIOR_GAP = 0.40;

    /** The default section label font. */
    public static final Font DEFAULT_LABEL_FONT = new Font("SansSerif",
            Font.PLAIN, 10);

    /** A "magic" color value that means, use either white or black depending
     * upon the background color the label is being drawn over */
    public static final Paint LABEL_PAINT_AUTO = new Color(255, 255, 255, 0);

    /** The default section label paint. */
    public static final Paint DEFAULT_LABEL_PAINT = LABEL_PAINT_AUTO;

    /** We want the legend axis to be drawn at 1/15 of the chart data area
     *   total width */
    private static final double LEGEND_DATA_AREA_PROPORTION = 1.0/15.0;

    /** The dataset for the pie chart. */
    private PieDataset dataset;

    /** 
     * The amount of space left around the outside of the pie plot, expressed
     * as a percentage of the plot area width and height.
     */
    private double interiorGap;

    private AbstractDiscItemDistributor discDistributor;

    private DiscItemRenderer discRenderer;

    /** The section label generator. */
    private PieSectionLabelGenerator labelGenerator;

    /** The font used to display the section labels. */
    private Font labelFont;

    /** The color used to draw the section labels. */
    private transient Paint labelPaint;

    /** The tooltip generator. */
    private PieToolTipGenerator toolTipGenerator;

    /** The URL generator. */
    private PieURLGenerator urlGenerator;

    /** The legend axis */
    private DiscLegendAxis legendAxis;

    public DiscPlot() {
        this(null);
    }

    public DiscPlot(PieDataset dataset) {
        super();
        this.dataset = dataset;
        if (dataset != null) {
            dataset.addChangeListener(this);
        }
        this.interiorGap = DEFAULT_INTERIOR_GAP;
        this.discDistributor = new StandardDiscItemDistributor(dataset);
        this.discRenderer = new StandardDiscItemRenderer();

        this.labelGenerator = new StandardPieSectionLabelGenerator();
        this.labelFont = DEFAULT_LABEL_FONT;
        this.labelPaint = DEFAULT_LABEL_PAINT;

        this.toolTipGenerator = new StandardPieToolTipGenerator();
        this.urlGenerator = null;
    }

    /**
     * Returns the dataset.
     *
     * @return The dataset (possibly <code>null</code>).
     * 
     * @see #setDataset(PieDataset)
     */
    public PieDataset getDataset() {
        return this.dataset;
    }

    /**
     * Sets the dataset and sends a {@link DatasetChangeEvent} to 'this'.
     *
     * @param dataset  the dataset (<code>null</code> permitted).
     * 
     * @see #getDataset()
     */
    public void setDataset(PieDataset dataset) {
        // if there is an existing dataset, remove the plot from the list of
        // change listeners...
        PieDataset existing = this.dataset;
        if (existing != null) {
            existing.removeChangeListener(this);
        }

        // set the new dataset, and register the chart as a change listener...
        this.dataset = dataset;
        if (dataset != null) {
            setDatasetGroup(dataset.getGroup());
            dataset.addChangeListener(this);
        }

        // tell the item distributor about the change
        if (discDistributor != null) {
            discDistributor.setDataset(dataset);
        }

        // send a dataset change event to self...
        DatasetChangeEvent event = new DatasetChangeEvent(this, dataset);
        datasetChanged(event);
    }

    /**
     * @return Returns the discDistributor.
     */
    public AbstractDiscItemDistributor getDiscDistributor() {
        return discDistributor;
    }

    /**
     * @param discDistributor The discDistributor to set.
     */
    public void setDiscDistributor(AbstractDiscItemDistributor discDistributor) {
        if (discDistributor == null)
            throw new IllegalArgumentException("Disc distributor cannot be null");

        this.discDistributor = discDistributor;
        this.discDistributor.setDataset(this.dataset);
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * @return Returns the discRenderer.
     */
    public DiscItemRenderer getDiscRenderer() {
        return discRenderer;
    }

    /**
     * @param discRenderer The discRenderer to set.
     */
    public void setDiscRenderer(DiscItemRenderer discRenderer) {
        if (discRenderer == null)
            throw new IllegalArgumentException("Disc renderer cannot be null");

        this.discRenderer = discRenderer;
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * @return Returns the labelGenerator.
     */
    public PieSectionLabelGenerator getLabelGenerator() {
        return labelGenerator;
    }

    /**
     * @param labelGenerator The labelGenerator to set.
     */
    public void setLabelGenerator(PieSectionLabelGenerator labelGenerator) {
        this.labelGenerator = labelGenerator;
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * @return Returns the labelFont.
     */
    public Font getLabelFont() {
        return labelFont;
    }

    /**
     * @param labelFont The labelFont to set.
     */
    public void setLabelFont(Font labelFont) {
        this.labelFont = labelFont;
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * @return Returns the labelPaint.
     */
    public Paint getLabelPaint() {
        return labelPaint;
    }

    /**
     * @param labelPaint The labelPaint to set.
     */
    public void setLabelPaint(Paint labelPaint) {
        this.labelPaint = labelPaint;
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * @return Returns the toolTipGenerator.
     */
    public PieToolTipGenerator getToolTipGenerator() {
        return toolTipGenerator;
    }

    /**
     * @param toolTipGenerator The toolTipGenerator to set.
     */
    public void setToolTipGenerator(PieToolTipGenerator toolTipGenerator) {
        this.toolTipGenerator = toolTipGenerator;
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * @return Returns the urlGenerator.
     */
    public PieURLGenerator getUrlGenerator() {
        return urlGenerator;
    }

    /**
     * @param urlGenerator The urlGenerator to set.
     */
    public void setUrlGenerator(PieURLGenerator urlGenerator) {
        this.urlGenerator = urlGenerator;
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Returns the interior gap, measured as a percentage of the available
     * drawing space.
     *
     * @return The gap (as a percentage of the available drawing space).
     * 
     * @see #setInteriorGap(double)
     */
    public double getInteriorGap() {
        return this.interiorGap;
    }

    /**
     * Sets the interior gap and sends a {@link PlotChangeEvent} to all
     * registered listeners.  This controls the space between the edges of the
     * pie plot and the plot area itself (the region where the section labels
     * appear).
     *
     * @param percent  the gap (as a percentage of the available drawing space).
     * 
     * @see #getInteriorGap()
     */
    public void setInteriorGap(double percent) {

        if ((percent < 0.0) || (percent > MAX_INTERIOR_GAP)) {
            throw new IllegalArgumentException(
                "Invalid 'percent' (" + percent + ") argument.");
        }

        if (this.interiorGap != percent) {
            this.interiorGap = percent;
            notifyListeners(new PlotChangeEvent(this));
        }

    }

    public DiscLegendAxis getLegendAxis() {
        return this.legendAxis;
    }

    public void setLegendAxis(DiscLegendAxis legendAxis) {
        this.legendAxis = legendAxis;

        if (this.legendAxis != null) {
            this.legendAxis.setPlot(this);
            this.legendAxis.addChangeListener(this);
            this.legendAxis.setDiscItemDistributor(getDiscDistributor());
        }
    }

    @Override
    public void draw(Graphics2D g2, Rectangle2D area, Point2D anchor,
            PlotState parentState, PlotRenderingInfo info) {

        // adjust for insets...
        RectangleInsets insets = getInsets();
        insets.trim(area);

        if (info != null) {
            info.setPlotArea(area);
            info.setDataArea(area);
        }

        drawBackground(g2, area);
        drawOutline(g2, area);

        Shape savedClip = g2.getClip();
        g2.clip(area);

        Composite originalComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                getForegroundAlpha()));

        if (!getDiscDistributor().isDatasetEmpty()) {
            Rectangle2D dataArea = getDataArea(area);
            drawDiscs(g2, dataArea, info);
            drawLegendAxis(g2, dataArea, info);
        }
        else {
            drawNoDataMessage(g2, area);
        }

        g2.setClip(savedClip);
        g2.setComposite(originalComposite);

        drawOutline(g2, area);
    }

    private void drawLegendAxis(Graphics2D g2,
                                Rectangle2D dataArea,
                                PlotRenderingInfo info) {
        if (legendAxis != null) {
            double cursor = dataArea.getMinX();
            legendAxis.draw(g2, cursor, dataArea, dataArea,
                            RectangleEdge.RIGHT, info);
        }
    }

    private void drawDiscs(Graphics2D g2, Rectangle2D dataArea,
                           PlotRenderingInfo info) {

        getDiscDistributor().setDiscDataArea(dataArea);

        DiscItemRendererState state = getDiscRenderer().initialise(g2,
            dataArea, this, dataset, info);

        for (int item = 0;  item < dataset.getItemCount();  item++) {
            Ellipse2D discLocation = getDiscDistributor().getDiscLocation(item);
            getDiscRenderer().drawItem(g2, state, dataArea, discLocation, info,
                this, dataset, item);
        }

    }

    private Rectangle2D getDataArea(Rectangle2D plotArea) {
        double hGap = plotArea.getWidth() * this.interiorGap;
        double vGap = plotArea.getHeight() * this.interiorGap;

        return new Rectangle2D.Double(plotArea.getX() + hGap,
                                      plotArea.getY() + vGap,
                                      plotArea.getWidth() - 2 * hGap,
                                      plotArea.getHeight() - 2 * vGap);
    }

    @Override
    public String getPlotType() {
        return "Disc Plot";
    }

    public Range getDataRange(ValueAxis axis) {
        CategoryDataset categoryDataset =
            DatasetUtilities.createCategoryDataset("data", dataset);
        return DatasetUtilities.findRangeBounds(categoryDataset);
    }

    @Override
    public void datasetChanged(DatasetChangeEvent event) {
        super.datasetChanged(event);

        if (this.legendAxis != null)
            this.legendAxis.configure();
    }



}
