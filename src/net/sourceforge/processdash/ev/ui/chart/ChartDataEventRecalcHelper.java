// Copyright (C) 2008-2014 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ev.ui.chart;

import java.util.ArrayList;

import org.jfree.data.general.AbstractDataset;
import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.general.DatasetChangeListener;

/**
 * Handles the event-based chart recalculation.
 */
public class ChartDataEventRecalcHelper extends AbstractDataset {
    /** Support DataSourceChangeListener notification */
    private ArrayList<DatasetChangeListener> listenerList = null;
    private ChartEventAdapter eventAdapter;

    /** The chart data that we want to recalculate */
    private RecalculableChartData recalculableChartData;

    private boolean needsRecalc = true;

    public ChartDataEventRecalcHelper(RecalculableChartData chartDataSpecialization,
                     ChartEventAdapter eventAdapter) {
        this.recalculableChartData = chartDataSpecialization;
        this.eventAdapter = eventAdapter;
        eventAdapter.setChartData(this);
    }

    protected void maybeRecalc() {
        if (needsRecalc) {
            recalc(); needsRecalc = false;
        }
    }

    private void recalc() { recalculableChartData.recalc(); }

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

    public void dispose() {
        listenerList.clear();
        eventAdapter.deregisterForUnderlyingDataEvents();
    }

}
