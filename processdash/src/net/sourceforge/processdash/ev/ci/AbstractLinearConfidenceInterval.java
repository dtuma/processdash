// Copyright (C) 2003-2008 Tuma Solutions, LLC
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

import net.sourceforge.processdash.util.TDistribution;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;



public abstract class AbstractLinearConfidenceInterval
    extends AbstractConfidenceInterval
    implements TargetedConfidenceInterval
{

    protected double beta0, beta1;
    protected double x_avg;
    protected double y_avg;
    protected double x_sum_sq_diff;
    protected double variance;
    protected double stddev;
    protected double projection = Double.NaN;

    double lastRangeProb = Double.NaN;
    double lastRange = Double.NaN;
    double rangeRadical = Double.NaN;


    public AbstractLinearConfidenceInterval() {}



    /** Return the forecast value produced by translating the input
     * value according to correction algorithm contained in this
     * confidence interval.
     */
    public double getPrediction() {
        if (viability > ACCEPTABLE)
            return projection;
        else
            return Double.NaN;
    }


    /** Return a value indicating how viable this confidence interval
     * seems.
     */
    public double getViability() {
        return viability;
    }



    protected double viability = -1;

    protected abstract void calcBetaParameters();
    protected abstract void calcViability();

    public void calcViability(double target, double minimumProb) {
        if (target < getLPI(minimumProb) || target > getUPI(minimumProb))
            viability = SERIOUS_PROBLEM;
    }



    public void dataPointsComplete() {
        super.dataPointsComplete();

        calcBetaParameters();
        calcStddev();

        if (Double.isNaN(stddev)  || Double.isInfinite(stddev)  ||
            numSamples < 3) {
            viability = CANNOT_CALCULATE;
            return;
        }

        calcViability();
    }


    private void calcStddev() {
        x_avg = y_avg = 0;
        for (int i = 0;   i < numSamples;   i++) {
            DataPoint dataPoint = getPoint(i);
            x_avg += dataPoint.x;
            y_avg += dataPoint.y;
        }
        x_avg = x_avg / numSamples;
        y_avg = y_avg / numSamples;

        x_sum_sq_diff = variance = 0;
        for (int i = 0;   i < numSamples;   i++) {
            DataPoint dataPoint = getPoint(i);
            double term = dataPoint.y - beta0 - (beta1 * dataPoint.x);
            variance += (term * term);

            double term2 = dataPoint.x - x_avg;
            x_sum_sq_diff += (term2 * term2);
        }

        variance = variance / (numSamples-2);
        stddev = Math.sqrt(variance);
    }


    public void setInput(double input) {
        super.setInput(input);
        lastRange = lastRangeProb = Double.NaN;

        projection = beta0 + (beta1 * input);
        double term = input - x_avg;
        term = 1.0 + (1.0 / numSamples) + (term * term) / x_sum_sq_diff;
        rangeRadical = Math.sqrt(term);
    }

    public double getQuantile(double p) {
        double range;

        double rangeProb = 2 * Math.abs(0.5 - p);

        if (rangeProb == lastRangeProb)
            range = lastRange;

        else if (Double.isNaN(rangeProb) ||
                 rangeProb >= 1.0 ||
                 (numSamples <= 2))
            range = Double.NaN;

        else {
            double stud_t = TDistribution.quantile(0.5 + rangeProb / 2.0,
                                               numSamples - 2);
            range = stud_t * stddev * rangeRadical;
            lastRange = range;
            lastRangeProb = rangeProb;
        }

        return (p > 0.5 ? projection + range : projection - range);
    }



    protected void saveXMLAttributes(StringBuffer result) {
        result.append(" b0='").append(beta0)
            .append("' b1='").append(beta1)
            .append("' n='").append(numSamples)
            .append("' xavg='").append(x_avg)
            .append("' xssd='").append(x_sum_sq_diff)
            .append("' var='").append(variance)
            .append("'");
    }


    protected AbstractLinearConfidenceInterval(Element xml) {
        beta0 = XMLUtils.getXMLNum(xml, "b0");
        beta1 = XMLUtils.getXMLNum(xml, "b1");
        numSamples = XMLUtils.getXMLInt(xml, "n");
        x_avg = XMLUtils.getXMLNum(xml, "xavg");
        x_sum_sq_diff = XMLUtils.getXMLNum(xml, "xssd");
        variance = XMLUtils.getXMLNum(xml, "var");
        stddev = Math.sqrt(variance);
    }
}
