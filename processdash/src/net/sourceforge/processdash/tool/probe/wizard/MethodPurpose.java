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
