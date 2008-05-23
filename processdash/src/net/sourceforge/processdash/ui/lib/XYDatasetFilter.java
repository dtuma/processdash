// Copyright (C) 2006-2008 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.lib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.general.DatasetGroup;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XYDataset;

public class XYDatasetFilter extends AbstractXYDataset {

    private XYDataset source;
    private boolean[] hideSeries;
    private List listeners;

    /** Create a dataset to filter one or more series out of another XYDataset.
     * 
     * @param source the XYDataset that should be filtered
     */
    public XYDatasetFilter(XYDataset source) {
        this.source = source;

        this.hideSeries = new boolean[source.getSeriesCount() + 5];
        Arrays.fill(hideSeries, false);

        this.listeners = new ArrayList();
    }

    @Override
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

    @Override
    public String getSeriesKey(int series) {
        return source.getSeriesKey(mapSeries(series)).toString();
    }

    public int getItemCount(int series) {
        return source.getItemCount(mapSeries(series));
    }

    public Number getX(int series, int item) {
        return source.getX(mapSeries(series), item);
    }

    @Override
    public double getXValue(int series, int item) {
        return source.getXValue(mapSeries(series), item);
    }

    public Number getY(int series, int item) {
        return source.getY(mapSeries(series), item);
    }

    @Override
    public double getYValue(int series, int item) {
        return source.getYValue(mapSeries(series), item);
    }

    public boolean isSeriesHidden(int series) {
        return hideSeries[series];
    }

    public XYDatasetFilter setSeriesHidden(int series, boolean hidden) {
        if (hideSeries[series] != hidden) {
            hideSeries[series] = hidden;
            fireDatasetChanged();
        }
        return this;
    }

    public XYDatasetFilter setSeriesHidden(String seriesID, boolean hidden) {
        int index = source.indexOf(seriesID);
        if (index != -1)
            setSeriesHidden(index, hidden);
        return this;
    }

    public XYDataset getSourceDataset() {
        return source;
    }

    @Override
    public void addChangeListener(DatasetChangeListener listener) {
        listeners.add(listener);
        source.addChangeListener(listener);
    }

    @Override
    public void removeChangeListener(DatasetChangeListener listener) {
        listeners.remove(listener);
        source.removeChangeListener(listener);
    }

    @Override
    protected void fireDatasetChanged() {
        DatasetChangeEvent e = new DatasetChangeEvent(this, this);
        for (Iterator i = listeners.iterator(); i.hasNext();) {
            DatasetChangeListener l = (DatasetChangeListener) i.next();
            l.datasetChanged(e);
        }
    }

    @Override
    public DatasetGroup getGroup() {
        return source.getGroup();
    }

    @Override
    public void setGroup(DatasetGroup group) {
        source.setGroup(group);
    }



}
