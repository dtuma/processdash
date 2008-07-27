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

package net.sourceforge.processdash.ev.ui.chart;

import java.util.ArrayList;
import java.util.List;

import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.xy.AbstractXYDataset;

/**
 * Base class for implementing XYDataSource functionality.
 */
public abstract class XYChartData extends AbstractXYDataset {

    protected List<XYChartSeries> series = new ArrayList<XYChartSeries>();
    private boolean needsRecalc = true;
    private ChartEventAdapter eventAdapter;

    public XYChartData(ChartEventAdapter eventAdapter) {
        this.eventAdapter = eventAdapter;
        this.eventAdapter.setChartData(this);
    }

    protected void recalc() {}
    protected void maybeRecalc() {
        if (needsRecalc) { recalc(); needsRecalc = false; } }
    /** Returns the number of series in the data source. */
    @Override
    public int getSeriesCount() {
        maybeRecalc();
        return series.size();
    }
    /** Append a data series if it appears viable */
    protected void maybeAddSeries(XYChartSeries s) {
        if (s != null && s.getItemCount() > 0)
            series.add(s);
    }
    /** Returns the name of the specified series (zero-based). */
    @Override
    public Comparable getSeriesKey(int seriesIndex) { maybeRecalc();
        return series.get(seriesIndex).getSeriesKey(); }
    /** Returns the number of items in the specified series */
    public int getItemCount(int seriesIndex) { maybeRecalc();
        return series.get(seriesIndex).getItemCount(); }
    /** Returns the x-value for the specified series and item */
    public Number getX(int seriesIndex, int itemIndex) {
        maybeRecalc();
        return series.get(seriesIndex).getX(itemIndex); }
    /** Returns the y-value for the specified series and item */
    public Number getY(int seriesIndex, int itemIndex) {
        maybeRecalc();
        if (itemIndex == -1) return null;
        return series.get(seriesIndex).getY(itemIndex); }

    // support DataSourceChangeListener notification
    private ArrayList<DatasetChangeListener> listenerList = null;
    @Override
    public void addChangeListener(DatasetChangeListener l) {
        if (listenerList == null) listenerList = new ArrayList<DatasetChangeListener>();
        synchronized (listenerList) {
            if (listenerList.size() == 0) eventAdapter.registerForUnderlyingDataEvents();
            if (!listenerList.contains(l)) listenerList.add(l);
        }
    }
    @Override
    public void removeChangeListener(DatasetChangeListener l) {
        if (listenerList == null) return;
        synchronized (listenerList) {
            if (listenerList.remove(l) && listenerList.size() == 0)
                eventAdapter.deregisterForUnderlyingDataEvents();
        }
    }
    private void fireChangeEvent() {
        if (listenerList == null) return;
        DatasetChangeEvent e = null;
        Object [] listeners = listenerList.toArray();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length; i-- > 0; ) {
            if (e == null) e = new DatasetChangeEvent(this, this);
            ((DatasetChangeListener)listeners[i]).datasetChanged(e);
        }
    }

    public void dataChanged() {
        needsRecalc = true;
        fireChangeEvent();
    }
}
