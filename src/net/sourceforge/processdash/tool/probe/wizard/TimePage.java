// Copyright (C) 2002-2014 Tuma Solutions, LLC
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

import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.SimpleData;


public class TimePage extends MethodsPage {

    @Override
    public void settingDone() {
        super.settingDone();
        purpose = new TimeMethodPurpose(histData);
    }

    @Override
    protected void buildExtraMethods(ProbeData histData) {
        ProbeMethod m = new AveragingMethod
            (histData, "C3", purpose, ProbeData.ACT_NC_LOC)
        {
            public void calc() {
                super.calc();
                observations.clear();
                if (rating > 0) rating = PROBE_METHOD_D + 0.00001;
            }
        };

        addMethod(m);
    }

    @Override
    protected boolean isInverseBeta1() {
        return true;
    }

    @Override
    protected void estimateWasSaved(SimpleData estimate) {
        if (histData.isDatabaseMode() && estimate instanceof DoubleData)
            new ProbeDatabaseUtil(data, prefix).spreadEstimatedTime(histData,
                ((DoubleData) estimate).getDouble());
    }

}
