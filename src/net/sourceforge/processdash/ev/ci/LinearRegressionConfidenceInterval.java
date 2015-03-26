// Copyright (C) 2003-2007 Tuma Solutions, LLC
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


package net.sourceforge.processdash.ev.ci;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.util.TDistribution;

public class LinearRegressionConfidenceInterval
    extends AbstractLinearConfidenceInterval
{

    protected void calcBetaParameters() {
        double x_sum, y_sum, xx_sum, xy_sum;

        x_sum = y_sum = xx_sum = xy_sum = 0.0;
        for (int i = 0;   i < numSamples;   i++) {
            DataPoint p = getPoint(i);
            x_sum += p.x;
            y_sum += p.y;
            xy_sum += p.x * p.y;
            xx_sum += p.x * p.x;
        }

        double x_avg = x_sum / numSamples;
        double y_avg = y_sum / numSamples;

        beta1 = ((xy_sum - (numSamples * x_avg * y_avg)) /
                 (xx_sum - (numSamples * x_avg * x_avg)));
        beta0 = y_avg - (beta1 * x_avg);
    }



    /** Heuristically estimate how viable this confidence interval appears
     * to be.
     */
    protected void calcViability() {
        // what would the forecast be if a simple ratio were used?
        double independentForecast = input * y_avg / x_avg;

        // perform the quantile() calculation backward, to determine what
        // probability percentage would yield a confidence interval that
        // includes the independent forecast.
        double range = Math.abs(projection - independentForecast);
        double stud_t = range / stddev / rangeRadical;
        double prob =
            2 * Math.abs(TDistribution.cumulative(stud_t, numSamples - 2) - 0.5);

        // what percentage does the user find acceptable?
        double cutoff = Settings.getInt("linCI.cutoffProbability", 30) / 100.0;
        if (prob > cutoff) {
            // the current probability is not acceptable.
            viability = SERIOUS_PROBLEM;
        } else {
            // use the percentage to scale the nominal viability rating.
            viability = NOMINAL * (1 - prob);
        }

        // TODO: need to examine correlation and significance
    }

}
