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

import net.sourceforge.processdash.ev.ci.ConfidenceInterval;

/**
 * A class that wraps a ConfidenceInterval object to show estimates in a
 *  XYChartSeries format.
 */
public class ConfidenceIntervalChartSeries implements XYChartSeries {
    /** We want to get the ConfidenceInterval's quantile for all percentage that
         are a multiple of 0.025 (between 0 and 1) */
    private static final double QUANTILE_PERCENTAGE_INCREMENT = 0.025;

    /** The ConfidenceInterval for which we want to show data */
    private ConfidenceInterval confidenceInterval = null;

    /** The series key associated with the ConfidenceInterval */
    private String seriesKey = null;

    /** The minimum quantile value to display in the chart. Data points with
     * smaller values will be excluded from the resulting dataset. */
    private double minValue;

    /** The maximum quantile value to display in the chart. Data points with
     * larger values will be excluded from the resulting dataset. */
    private double maxValue;

    /** The percentages values */
    private double[] percentages;

    /** The quantiles values */
    private double[] quantiles;

    /** The number of quantiles contained in that series */
    private int numberOfQuantiles = 0;

    public ConfidenceIntervalChartSeries(ConfidenceInterval confidenceInterval,
            String seriesKey, double minValue, double maxValue) {
        this.confidenceInterval = confidenceInterval;
        this.seriesKey = seriesKey;
        this.minValue = minValue;
        this.maxValue = maxValue;

        recalc();
    }

    public void recalc() {
        if (this.confidenceInterval == null) {
            this.numberOfQuantiles = 0;
            return;
        }

        // The chart will display 39 quantile values (1/0.025 - 1 = 439)
        int maxNumPoints = (int) (1 / QUANTILE_PERCENTAGE_INCREMENT - 1);

        quantiles = new double[maxNumPoints];
        percentages = new double[maxNumPoints];

        int j = 0;
        for (int i = 0; i < maxNumPoints; ++i) {
            double percentage = (i+1) * QUANTILE_PERCENTAGE_INCREMENT;
            double value = confidenceInterval.getQuantile(percentage);
            if (Double.isNaN(value) || value < minValue || value > maxValue)
                continue;

            percentages[j] = 200 * (percentage - 0.5);
            quantiles[j] = value;
            j++;
        }

        this.numberOfQuantiles = j;
    }

    public int getItemCount() {
        return this.numberOfQuantiles;
    }

    public String getSeriesKey() {
        return this.seriesKey;
    }

    public Number getX(int itemIndex) {
        return quantiles[itemIndex];
    }

    public Number getY(int itemIndex) {
        return percentages[itemIndex];
    }

}
