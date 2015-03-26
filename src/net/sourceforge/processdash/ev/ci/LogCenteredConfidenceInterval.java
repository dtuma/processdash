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

import org.w3c.dom.Element;

import net.sourceforge.processdash.util.TDistribution;
import net.sourceforge.processdash.util.XMLUtils;


public class LogCenteredConfidenceInterval extends AbstractConfidenceInterval
        implements ConfidenceIntervalWithRatio {

    public LogCenteredConfidenceInterval() {
        // since this object is typically used to compare planned
        // values to actual values, the nominal input value of 1.0 is
        // a useful default.
        setInput(1.0);
    }


    /** Return the forecast value produced by translating the input
     * value according to correction algorithm contained in this
     * confidence interval.
     */
    public double getPrediction() {
        if (viability > ACCEPTABLE)
            return input * ratio;
        else
            return Double.NaN;
    }


    /** Obtain the given quantile of the distribution used to produce this
     * confidence interval, and translate the input value using that
     * number.
     */
    public double getQuantile(double percentage) {
        if (viability > ACCEPTABLE) {
            double logRange = TDistribution.quantile(percentage, numSamples-2) * logstd;
            double logRatio = logmean + logRange;
            return input * Math.exp(logRatio);
        } else
            return Double.NaN;
    }


    /** Return a value indicating how viable this confidence interval
     * seems.
     */
    public double getViability() {
        return viability;
    }



    /** Add a historical data point
     */
    public void addDataPoint(double x, double y) {
        DataPoint lastPoint = null;
        if (dataPointsList != null && !dataPointsList.isEmpty())
            lastPoint = (DataPoint) dataPointsList.getLast();

        if (lastPoint != null &&
            ((x == 0 || lastPoint.x == 0) || (y == 0 || lastPoint.y == 0))) {
            // lognormal confidence intervals cannot deal well with
            // zero values.  If such a data point is encountered,
            // consolidate it with an adjacent data point.
            lastPoint.x += x;
            lastPoint.y += y;
        } else {
            super.addDataPoint(x, y);
        }
    }

    /** The ratio of total actual to total plan for all data points. */
    protected double ratio;
    /** The weighted lognormal mean of the actual-to-plan ratios */
    protected double logmean;
    /** The weighted lognormal standard deviation of the actual-to-plan ratios */
    protected double logstd;
    /** The calculated viability of the confidence interval */
    protected double viability = -1;


    public void dataPointsComplete() {
        super.dataPointsComplete();

        calcMeanStddev();
        calcViability();
    }

    /** Calculate the ratio between actual and planned value for each data
     * point, then calculate the centered log mean and standard
     * deviation of these ratios.  (Planned values are used for weights.)
     *
     * This also calculates the overall linear ratio of total actual to
     * total plan.
     */
    private void calcMeanStddev() {
        numSamples = getNumPoints();
        double totalPlan = 0;
        double totalActual = 0;
        double[] logRatio = new double[numSamples];

        for (int i = 0;   i < numSamples;   i++) {
            DataPoint p = getPoint(i);
            totalPlan += p.plan();
            totalActual += p.actual();
            logRatio[i] = Math.log(p.actual() / p.plan());
        }
        ratio = totalActual / totalPlan;
        logmean = Math.log(ratio);

        double sum = 0;
        double diff;
        for (int i = 0;   i < numSamples;   i++) {
            DataPoint p = getPoint(i);
            diff = logRatio[i] - logmean;
            sum += diff * diff * p.plan();
        }
        logstd = Math.sqrt(sum * numSamples / ((numSamples - 1) * totalPlan));
    }

    /**
     * Move this interval, so it is centered around a different mean.
     * 
     * This could be used, for example, if we want to reuse the variability
     * from a historical dataset, but center it around a different ratio for
     * use in a new earned value schedule.
     * 
     * @param newRatio the desired ratio of "actual divided by plan" values.
     */
    public void recenter(double newRatio) {
        this.ratio = newRatio;
        this.logmean = Math.log(newRatio);
    }

    /**
     * Return the effective data ratio for this confidence interval
     * 
     * @return the ratio of total actual vs total planned data in the
     *     dataset that was used to construct this confidence interval.
     */
    public double getActualVsPlanRatio() {
        return ratio;
    }


    /** Heuristically estimate how viable this confidence interval appears
     * to be.
     */
    private void calcViability() {
        if (Double.isNaN(logmean) || Double.isInfinite(logmean) ||
            Double.isNaN(logstd)  || Double.isInfinite(logstd)  ||
            numSamples < 3) {
            viability = CANNOT_CALCULATE;
            return;
        }

        viability = NOMINAL;
    }


    @Override
    protected void saveXMLAttributes(StringBuffer result) {
        result.append(" mean='").append(logmean)
            .append("' std='").append(logstd)
            .append("' ratio='").append(ratio)
            .append("' n='").append(numSamples)
            .append("'");
    }

    public LogCenteredConfidenceInterval(Element xml) {
        logmean = XMLUtils.getXMLNum(xml, "mean");
        logstd = XMLUtils.getXMLNum(xml, "std");
        ratio = XMLUtils.getXMLNum(xml, "ratio");
        numSamples = XMLUtils.getXMLInt(xml, "n");

        setInput(1.0);
        calcViability();
    }


}
