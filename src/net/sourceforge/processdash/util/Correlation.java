// Copyright (C) 2000-2007 Tuma Solutions, LLC
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

public class Correlation {

    /** The correlation between the two numbers. */
    public double r;

    /** The probabilistic significance of the correlation. */
    public double p;

    private static final int X=0; // provided to enhance readability.
    private static final int Y=1;

    /** compute various statistics for the relationship between two elements.
     * @param data is a Vector of data points.  Each element in data should
     * be an array of 2 double values (x and y).
     */
    public Correlation(Vector data) {
        int indx, n;
        double x_sum, y_sum, xx_sum, yy_sum, xy_sum;
        double[] dataPoint;

        x_sum = y_sum = xx_sum = yy_sum = xy_sum = 0.0;
        indx = n = data.size();
        while (indx-- != 0) {
            dataPoint = (double[])data.elementAt(indx);
            x_sum  += dataPoint[X];
            y_sum  += dataPoint[Y];
            xx_sum += dataPoint[X] * dataPoint[X];
            yy_sum += dataPoint[Y] * dataPoint[Y];
            xy_sum += dataPoint[X] * dataPoint[Y];
        }

        r = ((n * xy_sum - x_sum * y_sum) /
             (Math.sqrt((n * xx_sum - x_sum * x_sum) *
                        (n * yy_sum - y_sum * y_sum))));

        if (n <= 2)
            p = 1.0;
        else if (r >= 1.0)
            p = 0.0;
        else {
            double t = Math.abs(r) * Math.sqrt((n - 2) / (1.0 - r * r));

            p = 2.0 * (1 - TDistribution.cumulative(t, n-2));
        }
    }

}
