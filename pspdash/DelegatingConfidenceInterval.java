// PSP Dashboard - Data Automation Tool for PSP-like processes
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net


package pspdash;

import DistLib.uniform;

public class DelegatingConfidenceInterval implements ConfidenceInterval,
     TargetedConfidenceInterval
{

    protected ConfidenceInterval delegate;

    public void setInput(double input) {
        if (delegate != null)
            delegate.setInput(input);
    }

    public double getPrediction() {
        return (delegate == null ? Double.NaN : delegate.getPrediction());
    }

    public double getLPI(double percentage) {
        return (delegate == null ? Double.NaN : delegate.getLPI(percentage));
    }

    public double getUPI(double percentage) {
        return (delegate == null ? Double.NaN : delegate.getUPI(percentage));
    }

    public double getRandomValue(uniform u) {
        return (delegate == null ? Double.NaN : delegate.getRandomValue(u));
    }

    public double getViability() {
        return (delegate == null ? CANNOT_CALCULATE : delegate.getViability());
    }

    public void calcViability(double target, double minimumProb) {
        if (delegate instanceof TargetedConfidenceInterval)
            ((TargetedConfidenceInterval) delegate).calcViability
                (target, minimumProb);
    }

}
