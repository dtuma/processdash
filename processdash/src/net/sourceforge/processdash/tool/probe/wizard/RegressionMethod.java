// Copyright (C) 2002-2011 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.probe.wizard;

import java.util.List;
import java.util.Vector;

import net.sourceforge.processdash.data.util.ResultSet;
import net.sourceforge.processdash.util.Correlation;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.LinearRegression;


public class RegressionMethod extends AveragingMethod {

    public RegressionMethod(ProbeData data, String letter,
                            MethodPurpose purpose, int xColumn) {
        super(data, letter, purpose, xColumn);
    }

    public void calc() {
        Vector dataPoints = histData.getXYDataPoints(xColumn, yColumn);

        LinearRegression l = new LinearRegression(dataPoints);
        this.rangePercent = RANGE_PERCENT;
        l.project(this.inputValue, this.rangePercent);

        this.beta0 = l.beta0;
        this.beta1 = l.beta1;
        this.outputValue = l.projection;
        this.outputRange = l.range;
        this.standardDeviation = l.stddev;

        Correlation c = new Correlation(dataPoints);
        this.correlation = c.r * c.r;
        this.significance = c.p;

        rateCalculation(dataPoints);

        if (rating > 0) {
            rateBeta1();
            rateBeta0();
        }

        if (rating > 0) {
            String resKey = "Method." + methodPurpose.getKey() + ".Betas_OK_FMT";
            observations.add
                (resources.format(resKey,
                                  methodPurpose.formatValue(beta0, -1),
                                  methodPurpose.formatBeta1(beta1)));
            rateCorrelation();
        }
    }

    private void rateCalculation(Vector dataPoints) {
        if (dataPoints.size() < 3 ||
            badDouble(beta0) || badDouble(beta1) ||
            badDouble(correlation) || badDouble(significance))
        {
            errorMessages.add(resources.getString("Method.Not_Enough_Data"));
            rating = CANNOT_CALCULATE;
        }
    }

    private void rateBeta0() {
        // Beta0 should be close to zero (substantially
        // smaller than the projected value).
        double percent = Math.abs(beta0 / outputValue);

        if (percent > 0.5) {
            rating = SERIOUS_PROBLEM;
            String resKey =
                "Method." + methodPurpose.getKey() + ".Beta0_Problem_FMT";
            errorMessages.add
                (resources.format(resKey, methodPurpose.formatValue(beta0, -1)));

        } else {
            // We'll be ambivalent if Beta0 is one-quarter the size of
            // the projection. If it is any larger, we'll start taking
            // off rating points; if it is smaller, we'll add rating
            // points back in (up to a quarter of a point)
            rating += 0.25 - percent;
        }
    }

    private void rateCorrelation() {
        List messageDest = observations;
        String resKey;
        if (correlation > 0.9) {
            resKey = "Method.Correlation.Excellent_FMT";
            rating += 2;
        } else if (correlation > 0.7) {
            resKey = "Method.Correlation.Strong_FMT";
            rating += 1;
        } else if (correlation > 0.5) {
            resKey = "Method.Correlation.Adequate_FMT";
        } else {
            messageDest = errorMessages;
            resKey = "Method.Correlation.Inadequate_FMT";
            rating = SERIOUS_PROBLEM;
        }
        messageDest.add(resources.format(resKey, formatNumber(correlation)));

        if (significance <= 0.05) {
            resKey = "Method.Significance.Good_FMT";
            rating += 0.5;
        } else if (significance <= 0.12) {
            resKey = "Method.Significance.Probable_FMT";
            rating += 0.2;
        } else if (significance <= 0.20) {
            resKey = "Method.Significance.Likely_FMT";
        } else {
            resKey = "Method.Significance.Questionable_FMT";
            rating -= 0.5;
        }
        observations.add(resources.format
                         (resKey, FormatUtil.formatPercent(significance)));
    }

    protected void addTrendParam(StringBuffer url) {
        url.append("&trend=regress");
    }

    protected String getTutorialLink() {
        ResultSet rs = histData.getResultSet();
        return Tutorial.getRegressLink
            (getPurposeLabel(), getMethodLetter(),
             rs.getColName(xColumn),
             rs.getColName(methodPurpose.getYColumn()),
             rs.getColName(inputColumn),
             rs.getColName(methodPurpose.getTargetColumn()),
             RANGE_PERCENT);
    }

}
