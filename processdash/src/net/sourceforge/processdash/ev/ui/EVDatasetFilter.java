// Copyright (C) 2006 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ev.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.jfree.data.DatasetChangeEvent;
import org.jfree.data.DatasetChangeListener;
import org.jfree.data.DatasetGroup;
import org.jfree.data.XYDataset;

public class EVDatasetFilter implements XYDataset {

    private XYDataset source;
    private boolean[] hideSeries;
    private List listeners;

    /** Create a dataset to filter one or more series out of another earned
     * value dataset.
     * 
     * Note: this is not a general purpose filter - it relies upon the known
     * positions of certain series within an earned value dataset.
     * 
     * @param source the XYDataset that should be filtered
     * @param hidePlan should the plan line be hidden?
     * @param hideForecast should the forecast line be hidden?
     * @param forecastIndex the series number of the first forecast series
     */
    public EVDatasetFilter(XYDataset source, boolean hidePlan,
            boolean hideForecast, int forecastIndex) {
        this.source = source;

        this.hideSeries = new boolean[source.getSeriesCount() + 5];
        Arrays.fill(hideSeries, false);
        if (hidePlan)
            hideSeries[0] = true;
        if (hideForecast)
            Arrays.fill(hideSeries, forecastIndex, hideSeries.length, true);

        this.listeners = new ArrayList();
    }

    public int getSeriesCount() {
        int seriesCount = source.getSeriesCount();
        for (int i = seriesCount;   i-- > 0;  )
            if (hideSeries[i])
                seriesCount--;
        return seriesCount;
    }


    private int mapSeries(int series) {
        for (int i = 0; i < hideSeries.length; i++) {
            if (hideSeries[i] == false)
                if (series-- == 0)
                    return i;
        }
        // shouldn't happen
        return -1;
    }

    public String getSeriesName(int series) {
        return source.getSeriesName(mapSeries(series));
    }

    public int getItemCount(int series) {
        return source.getItemCount(mapSeries(series));
    }

    public Number getXValue(int series, int item) {
        return source.getXValue(mapSeries(series), item);
    }

    public Number getYValue(int series, int item) {
        return source.getYValue(mapSeries(series), item);
    }

    public boolean isSeriesHidden(int series) {
        return hideSeries[series];
    }

    public void setSeriesHidden(int series, boolean hidden) {
        if (hideSeries[series] != hidden) {
            hideSeries[series] = hidden;
            fireDatasetChanged();
        }
    }

    public XYDataset getSourceDataset() {
        return source;
    }

    public void addChangeListener(DatasetChangeListener listener) {
        listeners.add(listener);
        source.addChangeListener(listener);
    }

    public void removeChangeListener(DatasetChangeListener listener) {
        listeners.remove(listener);
        source.removeChangeListener(listener);
    }

    protected void fireDatasetChanged() {
        DatasetChangeEvent e = new DatasetChangeEvent(this, this);
        for (Iterator i = listeners.iterator(); i.hasNext();) {
            DatasetChangeListener l = (DatasetChangeListener) i.next();
            l.datasetChanged(e);
        }
    }

    public DatasetGroup getGroup() {
        return source.getGroup();
    }

    public void setGroup(DatasetGroup group) {
        source.setGroup(group);
    }



}
