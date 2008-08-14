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

package net.sourceforge.processdash.ev.ui.chart;

import java.util.ArrayList;

import net.sourceforge.processdash.ev.EVMetrics;
import net.sourceforge.processdash.ev.ci.ConfidenceInterval;

/** This XYChartData subclass contains one date series, for a
    particular confidenceInterval */
public abstract class ConfidenceIntervalChartData extends XYChartData {

    private EVMetrics[] seriesMetrics;

    /** Contains the EVMetrics for which the confidence interval has been added
        to the series list */
    protected ArrayList<EVMetrics> addedSeriesMetrics = new ArrayList<EVMetrics>();

    public ConfidenceIntervalChartData(ChartEventAdapter eventAdapter,
                                       EVMetrics... seriesMetrics) {
        super(eventAdapter);
        this.seriesMetrics = seriesMetrics;
    }

    @Override
    protected void recalc() {
        this.series.clear();

        for (EVMetrics metrics : seriesMetrics) {
            ConfidenceIntervalChartSeries series = getSeries(metrics);

            series.recalc();
            if (maybeAddSeries(series)) {
                addedSeriesMetrics.add(metrics);
            }
        }
    }

    protected abstract ConfidenceIntervalChartSeries getSeries(EVMetrics metrics);

}
