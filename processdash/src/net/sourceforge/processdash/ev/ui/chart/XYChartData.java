// Copyright (C) 2008-2015 Tuma Solutions, LLC
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
import java.util.List;

import org.jfree.data.Range;
import org.jfree.data.RangeInfo;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.AbstractXYDataset;

import net.sourceforge.processdash.util.Disposable;

/**
 * Base class for implementing XYDataSource functionality.
 */
public abstract class XYChartData extends AbstractXYDataset
            implements RecalculableChartData, RangeInfo, Disposable {

    /** Used to handle event-driven chart recalculation */
    private ChartDataEventRecalcHelper chartDataRecalcHelper;

    protected List<XYChartSeries> series = new ArrayList<XYChartSeries>();

    public XYChartData(ChartEventAdapter eventAdapter) {
        this.chartDataRecalcHelper = new ChartDataEventRecalcHelper(this, eventAdapter);
    }

    public void recalc() {}
    public void maybeRecalc() {
        chartDataRecalcHelper.maybeRecalc();
    }

    /** Returns the number of series in the data source. */
    @Override
    public int getSeriesCount() {
        maybeRecalc();
        return series.size();
    }

    public List<XYChartSeries> getSeries() {
        return series;
    }

    /** Append a data series if it appears viable */
    protected boolean maybeAddSeries(XYChartSeries s) {
        if (s != null && s.getItemCount() > 0)
            return series.add(s);
        return false;
    }

    public void clearSeries() {
        series.clear();
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

    public Range getRangeBounds(boolean includeInterval) {
        Range result = DatasetUtilities.iterateRangeBounds(this);
        for (XYChartSeries s : series) {
            if (s instanceof RangeInfo) {
                RangeInfo ri = (RangeInfo) s;
                Range oneRange = ri.getRangeBounds(includeInterval);
                result = Range.combine(result, oneRange);
            }
        }
        return result;
    }

    public double getRangeLowerBound(boolean includeInterval) {
        return getRangeBounds(includeInterval).getLowerBound();
    }

    public double getRangeUpperBound(boolean includeInterval) {
        return getRangeBounds(includeInterval).getUpperBound();
    }

    public void dataChanged() {
        chartDataRecalcHelper.dataChanged();
    }

    @Override
    public void addChangeListener(DatasetChangeListener l) {
        chartDataRecalcHelper.addChangeListener(l);
    }
    @Override
    public void removeChangeListener(DatasetChangeListener l) {
        chartDataRecalcHelper.removeChangeListener(l);
    }

    public void dispose() {
        chartDataRecalcHelper.dispose();
    }

}
