// Copyright (C) 2000-2014 Tuma Solutions, LLC
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

package net.sourceforge.processdash.util;

import java.util.Vector;

public class LinearRegression {

    public double beta0;
    public double beta1;
    public double stddev;
    public double x_avg;
    public double y_avg;

    public double projection, range, UPI, LPI;

    private static final int X=0; // provided to enhance readability.
    private static final int Y=1;

    private double x_var;
    private int n;

    public LinearRegression(Vector data) {
        int indx;
        double x_sum, y_sum, xx_sum, xy_sum;
        double[] dataPoint;

        x_sum = y_sum = xx_sum = xy_sum = 0.0;
        indx = n = data.size();
        while (indx-- != 0) {
            dataPoint = (double[])data.elementAt(indx);
            x_sum += dataPoint[X];
            y_sum += dataPoint[Y];
            xy_sum += dataPoint[X] * dataPoint[Y];
            xx_sum += dataPoint[X] * dataPoint[X];
        }

        x_avg = x_sum / n;
        y_avg = y_sum / n;

        beta1 = ((xy_sum - (n * x_avg * y_avg)) /
                 (xx_sum - (n * x_avg * x_avg)));
        beta0 = y_avg - (beta1 * x_avg);


        indx = n;
        double term, term2;
        double sum, sum2;
        sum = sum2 = 0.0;
        while (indx-- != 0) {
            dataPoint = (double[])data.elementAt(indx);
            term = dataPoint[Y] - beta0 - (beta1 * dataPoint[X]);
            sum += (term * term);

            term2 = dataPoint[X] - x_avg;
            sum2 += (term2 * term2);
        }

        stddev = Math.sqrt(sum / (n-2));
        x_var = sum2;
    }

    public void project(double newX, double p) {
        double term, stud_t;

        projection = beta0 + (beta1 * newX);

        if (Double.isNaN(p) ||
            (p < 0.0) ||
            (p >= 1.0) ||
            (n <= 2))
            range = Double.NaN;
        else {

            term = newX - x_avg;
            term = 1.0 + (1.0 / n) + (term * term) / x_var;

            stud_t = TDistribution.quantile(0.5 + p / 2.0, n - 2);

            range = stud_t * stddev * Math.sqrt(term);
        }

        UPI = projection + range;
        LPI = projection - range;
    }

    public double getXIntercept() {
        return -beta0 / beta1;
    }

}
