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

import java.util.Arrays;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;

import cern.jet.random.ChiSquare;
import cern.jet.random.Normal;
import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;

public class LognormalConfidenceInterval extends AbstractConfidenceInterval {

    public LognormalConfidenceInterval() {
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
    protected double getQuantile(double percentage) {
        if (samples == null) return Double.NaN;

        int pos = (int) ((1-percentage) * samples.length);
        if (pos < 0 || pos >= samples.length) return Double.NaN;

        double t = samples[pos];
        double logResult = base - t * rational;
        return Math.exp(logResult) * input;
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
    /** The samples generated by the bootstrap algorithm */
    protected double[] samples;
    /** A precalculated term in the calculation interval equation */
    protected double base;
    /** A precalculated term in the calculation interval equation */
    protected double rational;
    /** The calculated viability of the confidence interval */
    protected double viability = -1;


    public void dataPointsComplete() {
        super.dataPointsComplete();

        calcMeanStddev();
        calculateInterval(logmean, logstd);
    }

    protected void calculateInterval(double mean, double std) {
        logmean = mean;
        logstd = std;

        if (Double.isNaN(logmean) || Double.isInfinite(logmean) ||
            Double.isNaN(logstd)  || Double.isInfinite(logstd)  ||
            numSamples < 3) {
            viability = CANNOT_CALCULATE;
            return;
        }

        runParametricBootstrap();
        calcViability();
    }

    /** Calculate the ratio between actual and planned value for each data
     * point, then calculate the weighted lognormal mean and standard
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
        double sum = 0;

        for (int i = 0;   i < numSamples;   i++) {
            DataPoint p = getPoint(i);
            totalPlan += p.plan();
            totalActual += p.actual();
            logRatio[i] = Math.log(p.actual() / p.plan());
            sum += logRatio[i] * p.plan();
        }
        ratio = totalActual / totalPlan;
        logmean = sum / totalPlan;

        sum = 0;
        double diff;
        for (int i = 0;   i < numSamples;   i++) {
            DataPoint p = getPoint(i);
            diff = logRatio[i] - logmean;
            sum += diff * diff * p.plan();
        }
        logstd = Math.sqrt(sum * numSamples / ((numSamples - 1) * totalPlan));
    }

    private void runParametricBootstrap() {
        samples = generateBootstrapSamples();

        double s = logstd;
        double n = numSamples;
        rational = Math.sqrt((s*s*(1 + (s*s/2))) / n);
        base = logmean + s*s/2;
    }

    private double[] generateBootstrapSamples() {
        RandomEngine u = new MersenneTwister();
        Normal normal = new Normal(0, 1, u);
        ChiSquare chisquare = new ChiSquare(numSamples-1, u);
        int bootstrapSize = Settings.getInt("logCI.bootstrapSize", 2000);
        double[] samples = new double[bootstrapSize];
        for (int i = bootstrapSize;   i-- > 0; )
             samples[i] = generateBootstrapSample(normal, chisquare, numSamples,
                 logstd);
        Arrays.sort(samples);
        return samples;
    }


    /** Generate a single sample for the bootstrap algorithm.
     */
    private double generateBootstrapSample
        (Normal normal, ChiSquare chisquare, int n, double sigma)
    {
        double N = normal.nextDouble();
        double chi = chisquare.nextDouble();
        double chiRatio = chi / (n-1);

        double numerator = N + sigma * Math.sqrt(n) * (chiRatio - 1.0) / 2.0;
        double denominator =
            Math.sqrt(chiRatio * (1.0 + sigma*sigma * chiRatio / 2.0));
        return numerator / denominator;
    }


    /** Heuristically estimate how viable this confidence interval appears
     * to be.
     */
    private void calcViability() {
        // perform the quantile() calculation backward, to determine what
        // probability percentage would yield a confidence interval that
        // includes the overall linear ratio between plan and actual.
        double range = Math.abs(Math.log(ratio) - base);
        double normRng = - range / rational;
        int pos = Math.abs(Arrays.binarySearch(samples, normRng));
        double prob = 2 * Math.abs((((double) pos) / samples.length) - 0.5);

        // what percentage does the user find acceptable?
        double cutoff = Settings.getInt("logCI.cutoffProbability", 50) / 100.0;
        if (prob > cutoff) {
            // the current probability is not acceptable.
            viability = SERIOUS_PROBLEM;
        } else {
            // use the percentage to scale the nominal viability rating.
            viability = NOMINAL * (1 - prob);
        }
    }

    protected void saveXMLAttributes(StringBuffer result) {
        result.append(" mean='").append(logmean)
            .append("' std='").append(logstd)
            .append("' ratio='").append(ratio)
            .append("' n='").append(numSamples)
            .append("'");
    }

    public LognormalConfidenceInterval(Element xml) {
        logmean = XMLUtils.getXMLNum(xml, "mean");
        logstd = XMLUtils.getXMLNum(xml, "std");
        ratio = XMLUtils.getXMLNum(xml, "ratio");
        numSamples = XMLUtils.getXMLInt(xml, "n");
        calculateInterval(logmean, logstd);
        setInput(1.0);
    }

}
