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

import org.jfree.data.DatasetChangeListener;
import org.jfree.data.DatasetGroup;
import org.jfree.data.XYDataset;

public class EVDatasetFilter implements XYDataset {

    private XYDataset source;
    private boolean hidePlan;
    private boolean hideForecast;
    private int forecastIndex;

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
        this.hidePlan = hidePlan;
        this.hideForecast = hideForecast;
        this.forecastIndex = forecastIndex;
    }

    public int getSeriesCount() {
        int seriesCount = source.getSeriesCount();
        if (hideForecast)
            seriesCount = Math.min(seriesCount, forecastIndex);
        if (hidePlan)
            seriesCount--;
        return seriesCount;
    }


    private int mapSeries(int series) {
        if (hidePlan)
            return series + 1;
        else
            return series;
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



    public void addChangeListener(DatasetChangeListener listener) {
        source.addChangeListener(listener);
    }

    public void removeChangeListener(DatasetChangeListener listener) {
        source.removeChangeListener(listener);
    }

    public DatasetGroup getGroup() {
        return source.getGroup();
    }

    public void setGroup(DatasetGroup group) {
        source.setGroup(group);
    }



}
