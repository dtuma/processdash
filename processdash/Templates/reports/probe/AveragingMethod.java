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

import pspdash.data.LinearRegression;

import java.io.PrintWriter;
import java.util.Vector;

class AveragingMethod extends Method {
    LinearRegression l;

    public AveragingMethod(HistData data, double estObjLOC,
                           int xCol, int yCol,
                           String letter, String purpose) {
        super(data, estObjLOC, letter, purpose);

        Vector dataPoints = data.getXYDataPoints(xCol, yCol);
        l = new LinearRegression(dataPoints);

        if (l.x_avg == 0 || badDouble(l.x_avg) || badDouble(l.y_avg)) {
            errorMessages.add("You do not have enough historical data.");
            rating = CANNOT_CALCULATE;
            return;
        }
        l.beta0 = 0;
        l.beta1 = l.y_avg / l.x_avg;
        l.project(estObjLOC, Double.NaN);

        rateBetas(false, l.beta0, l.projection, l.beta1,
                  getExpectedBeta1(), getExpectedBeta1Text());
        if (rating > 0) {
            observations.add("The slope of the line is within bounds ("+
                             BETA1 + " = " + formatBeta1(l.beta1) + ").");
        }
    }


    protected int xCol = 0;
    protected int getXCol() { return xCol; }
    protected int yCol = 0;
    protected int getYCol() { return yCol; }

    void printOption(PrintWriter out, boolean isSelected) {
        printOption(out, l.projection, isSelected, 0, l.beta1, -1, -1, -1);
    }

    void printTableRow(PrintWriter out, boolean isSelected) {
        printTableRow(out, l.projection, isSelected, 0, l.beta1,
                      Double.NaN, Double.NaN, Double.NaN);
    }
}
