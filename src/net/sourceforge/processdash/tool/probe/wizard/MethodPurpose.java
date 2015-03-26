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

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.FormatUtil;


public abstract class MethodPurpose {

    protected static Resources resources =
        Resources.getDashBundle("PROBE.Wizard");
    protected static final int NO_RANGE = -1;


    // expect "size" or "time"
    public abstract String getKey();

    public String formatValue(double value, double range) {
        StringBuffer result = new StringBuffer();
        result.append(FormatUtil.formatNumber(value));
        if (range >= 0)
            result.append("&nbsp;&plusmn;&nbsp;")
                .append(FormatUtil.formatNumber(range));

        result.append("&nbsp;").append(getUnits());
        return result.toString();
    }

    public abstract String formatBeta1(double beta1);
    public abstract double getExpectedBeta1();
    public abstract double getBeta1MaxRatio();
    public abstract String getUnits();
    public abstract int getYColumn();
    public abstract int getTargetColumn();
    public abstract String getTargetDataElement();
    public abstract String getTargetDataElementMin();
    public abstract String getTargetDataElementMax();
    public abstract String getTargetName();
    public abstract int mapInputColumn(int xColumn);
    public abstract double getMult();

}
