// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net


package net.sourceforge.processdash.ev.ci;

import org.w3c.dom.Element;


public class LinearRatioConfidenceInterval
    extends AbstractLinearConfidenceInterval
{
    public LinearRatioConfidenceInterval() {}

    public LinearRatioConfidenceInterval(Element xml) {
        super(xml);
        calcViability();
    }

    protected void calcBetaParameters() {
        double x_sum, y_sum;

        x_sum = y_sum = 0.0;
        for (int i = 0;   i < numSamples;   i++) {
            DataPoint p = getPoint(i);
            x_sum += p.x;
            y_sum += p.y;
        }

        beta0 = 0;
        beta1 = y_sum / x_sum;
    }


    protected void calcViability() {
        if (Double.isNaN(stddev)  || Double.isInfinite(stddev)  ||
            numSamples < 3)
            viability = CANNOT_CALCULATE;
        else
            viability = NOMINAL;
    }

}
