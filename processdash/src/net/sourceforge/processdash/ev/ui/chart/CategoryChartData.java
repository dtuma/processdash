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

import java.util.List;

import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DatasetChangeListener;

import net.sourceforge.processdash.util.Disposable;

/**
 * Base class to use for charts that use a CategoryDataset
 */
public class CategoryChartData extends DefaultCategoryDataset
        implements RecalculableChartData, Disposable {

    /** Used to handle the event-based recalculation */
    private ChartDataEventRecalcHelper chartData;

    CategoryChartSeries series;

    public CategoryChartData(ChartEventAdapter eventAdapter, CategoryChartSeries series) {
        this.chartData = new ChartDataEventRecalcHelper(this, eventAdapter);
        this.series = series;
    }

    @Override
    public int getColumnCount() {
        maybeRecalc();
        return super.getColumnCount();
    }

    @Override
    public int getRowCount() {
        maybeRecalc();
        return super.getRowCount();
    }

    public void maybeRecalc() {
        chartData.maybeRecalc();
    }

    public void recalc() {
        clear();
        series.recalc();
        List<? extends Comparable> columnKeys = series.getColumnsKeys();
        List<? extends Comparable> rowKeys = series.getRowKeys();

        for (int r = 0; r < rowKeys.size(); ++r) {
            for (int c = 0; c < columnKeys.size(); c++) {
                addValue((Number)series.getValue(r, c), rowKeys.get(r), columnKeys.get(c));
            }
        }
    }

    @Override
    public void addChangeListener(DatasetChangeListener listener) {
        chartData.addChangeListener(listener);
    }

    @Override
    public void removeChangeListener(DatasetChangeListener listener) {
        chartData.removeChangeListener(listener);
    }

    public void dispose() {
        chartData.dispose();
    }

}
