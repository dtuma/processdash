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

import cern.jet.random.engine.RandomEngine;

public class DelegatingConfidenceInterval implements ConfidenceInterval,
     TargetedConfidenceInterval, XMLPersistableConfidenceInterval
{

    protected ConfidenceInterval delegate;

    protected ConfidenceInterval getDelegate() {
        if (delegate == null)
            return NULL_DELEGATE;
        else
            return delegate;
    }

    public void setInput(double input) {
        getDelegate().setInput(input);
    }

    public double getPrediction() {
        return getDelegate().getPrediction();
    }

    public double getQuantile(double percentage) {
        return getDelegate().getQuantile(percentage);
    }

    public double getLPI(double percentage) {
        return getDelegate().getLPI(percentage);
    }

    public double getUPI(double percentage) {
        return getDelegate().getUPI(percentage);
    }

    public double getRandomValue(RandomEngine u) {
        return getDelegate().getRandomValue(u);
    }

    public double getViability() {
        return getDelegate().getViability();
    }

    public void calcViability(double target, double minimumProb) {
        ConfidenceInterval delegate = getDelegate();
        if (delegate instanceof TargetedConfidenceInterval)
            ((TargetedConfidenceInterval) delegate).calcViability
                (target, minimumProb);
    }

    public void saveToXML(String tagName, StringBuffer result) {
        ConfidenceInterval delegate = getDelegate();
        if (delegate instanceof XMLPersistableConfidenceInterval) {
            XMLPersistableConfidenceInterval xpci =
                (XMLPersistableConfidenceInterval) delegate;
            xpci.saveToXML(tagName, result);
        }
    }

    private static final ConfidenceInterval NULL_DELEGATE =
        new SingleValueConfidenceInterval(Double.NaN, CANNOT_CALCULATE);
}
