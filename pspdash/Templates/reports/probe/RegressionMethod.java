// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil

import pspdash.data.Correlation;
import pspdash.data.LinearRegression;

import java.io.PrintWriter;
import java.util.Vector;

class RegressionMethod extends Method {
    LinearRegression l;
    Correlation c;

    public RegressionMethod(HistData data, double estObjLOC,
                           int xCol, int yCol,
                           String letter, String purpose) {
        super(data, estObjLOC, letter, purpose);

        Vector dataPoints = data.getXYDataPoints(xCol, yCol);
        l = new LinearRegression(dataPoints);
        c = new Correlation(dataPoints);

        if (dataPoints.size() < 3 ||
            badDouble(l.beta0) || badDouble(l.beta1) ||
            badDouble(c.r) || badDouble(c.p)) {
            errorMessages.add("You do not have enough historical data.");
            rating = CANNOT_CALCULATE;
            return;
        }
        l.project(estObjLOC, 0.70);    // 70 percent prediction interval

        rateBetas(true, l.beta0, l.projection, l.beta1,
                  getExpectedBeta1(), getExpectedBeta1Text());
        if (rating > 0) {
            observations.add("Your regression parameters are within bounds ("+
                             BETA0 + " = " + formatNumber(l.beta0) + ", " +
                             BETA1 + " = " + formatBeta1(l.beta1) + ").");
            rateCorrelation();
        }
    }

    public void rateCorrelation() {
        double rSquared = c.r * c.r;
        String statement = "(" + RSQ + " = " + formatNumber(rSquared) + ").";
        if (rSquared > 0.9) {
            observations.add
                ("In addition, the historical data points have an excellent "+
                 "correlation "+ statement);
            rating += 2;
        } else if (rSquared > 0.7) {
            observations.add
                ("In addition, the historical data points have a strong "+
                 "correlation "+ statement);
            rating += 1;
        } else if (rSquared > 0.5) {
            observations.add
                ("The historical data points have an adequate correlation "+
                 statement);
        } else {
            rating = SERIOUS_PROBLEM;
            errorMessages.add
                ("The correlation between your historical data points is not "+
                 "reliable for planning purposes "+ statement);
            return;
        }

        statement = "(" + PROB + " = " + formatNumber(c.p*100) + "%)";
        if (c.p <= 0.05) {
            observations.add
                ("This correlation appears to be significant " + statement);
            rating += 0.5;
        } else if (c.p <= 0.20) {
            observations.add
                ("This correlation is probably significant " + statement);
            rating += 0.2;
        } else if (c.p <= 0.20) {
            observations.add
                ("This correlation is likely to be significant " + statement);
        } else {
            observations.add
                ("However, this correlation may not be significant " +
                 statement);
            rating -= 0.5;
        }
    }

    public static final String RSQ = "r<sup>2</sup>";
    public static final String PROB = "p";

    void printOption(PrintWriter out, boolean isBest) {
        printOption(out, l.projection, isBest, l.beta0, l.beta1, l.range,
                    0.70, (c.r * c.r));
    }
}
