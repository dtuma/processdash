// Copyright (C) 2008 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ui.lib.chart;

import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.general.PieDataset;

public abstract class AbstractDiscItemDistributor implements
        DiscItemDistributor, DatasetChangeListener {

    PieDataset dataset;

    boolean skipNegativeValues;

    Rectangle2D discDataArea;

    DiscItemRecord[] discs;

    public AbstractDiscItemDistributor() {
        this(null);
    }

    public AbstractDiscItemDistributor(PieDataset dataset) {
        setDataset(dataset);
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
     * @param dataset
     *                the dataset (<code>null</code> permitted).
     * 
     * @see #getDataset()
     */
    public void setDataset(PieDataset dataset) {
        // if there is an existing dataset, remove the distributor from the
        // list of change listeners...
        PieDataset existing = this.dataset;
        if (existing != null) {
            existing.removeChangeListener(this);
        }

        // set the new dataset, and register the distributor as a change
        // listener...
        this.dataset = dataset;
        if (dataset != null) {
            dataset.addChangeListener(this);
        }

        // send a dataset change event to self...
        DatasetChangeEvent event = new DatasetChangeEvent(this, dataset);
        datasetChanged(event);
    }

    public boolean isSkipNegativeValues() {
        return skipNegativeValues;
    }

    public void setSkipNegativeValues(boolean skipNegativeValues) {
        this.skipNegativeValues = skipNegativeValues;
        invalidate();
    }

    public Rectangle2D getDiscDataArea() {
        return discDataArea;
    }

    public void setDiscDataArea(Rectangle2D discDataArea) {
        if (discDataArea == null) {
            throw new IllegalArgumentException("disc data area cannot be null");
        }

        if (!discDataArea.equals(this.discDataArea)) {
            this.discDataArea = discDataArea;
            invalidate();
        }
    }

    public Ellipse2D getDiscLocation(int item) {
        if (discs == null) {
            DiscItemRecord[] newDiscs = reloadDiscs();
            if (newDiscs == null)
                newDiscs = new DiscItemRecord[0];

            if (newDiscs.length > 0) {
                distributeDiscs(newDiscs);
                translateDiscs(newDiscs);
            }

            this.discs = newDiscs;
        }
        return discs[item].getLocation();
    }

    public boolean isDatasetEmpty() {
        for (int i = 0; i < dataset.getItemCount(); i++) {
            Number n = dataset.getValue(i);
            double val = interpretValue(n);
            if (val > 0)
                return false;
        }
        return true;
    }

    protected DiscItemRecord[] reloadDiscs() {
        int size = (dataset == null ? 0 : dataset.getItemCount());
        DiscItemRecord[] newDiscs = new DiscItemRecord[size];
        for (int i = 0; i < size; i++) {
            Comparable key = dataset.getKey(i);
            Number value = dataset.getValue(i);
            double dbl = interpretValue(value);
            newDiscs[i] = newDiscItemRecord(key, Math.sqrt(dbl));
        }
        return newDiscs;
    }

    protected DiscItemRecord newDiscItemRecord(Comparable key, double r) {
        return new DiscItemRecord(key, r);
    }

    protected double interpretValue(Number value) {
        if (value == null)
            return Double.NaN;
        double result = value.doubleValue();
        if (Double.isInfinite(result) || Double.isNaN(result))
            return Double.NaN;
        else if (result < 0)
            return (skipNegativeValues ? Double.NaN : -result);
        else
            return result;
    }

    protected abstract void distributeDiscs(DiscItemRecord[] discs);

    protected void translateDiscs(DiscItemRecord[] discs) {
        double left = 0;
        double right = 0;
        double top = 0;
        double bottom = 0;
        for (int i = 0; i < discs.length; i++) {
            DiscItemRecord disc = discs[i];
            left = Math.min(left, disc.getX() - disc.getR());
            right = Math.max(right, disc.getX() + disc.getR());
            top = Math.min(top, disc.getY() - disc.getR());
            bottom = Math.max(bottom, disc.getY() + disc.getR());
        }
        double xMid = (left + right) / 2;
        double yMid = (top + bottom) / 2;

        double xScale = discDataArea.getWidth() / (right - left);
        double yScale = discDataArea.getHeight() / (bottom - top);
        double scale = Math.min(xScale, yScale);

        double dx = discDataArea.getCenterX() - xMid * scale;
        double dy = discDataArea.getCenterY() - yMid * scale;

        for (int i = 0; i < discs.length; i++) {
            discs[i].setTranslation(dx, dy, scale);
        }
    }

    public void datasetChanged(DatasetChangeEvent event) {
        invalidate();
    }

    protected void invalidate() {
        discs = null;
    }

}
