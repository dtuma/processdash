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

import net.sourceforge.processdash.i18n.Translator;
import net.sourceforge.processdash.process.ProcessUtil;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.HTMLUtils;


public class SizeMethodPurpose extends MethodPurpose {

    static final String PURPOSE_KEY = "Size";

    private ProbeData histData;
    private ProcessUtil processUtil;

    public SizeMethodPurpose(ProbeData data) {
        this.histData = data;
        this.processUtil = data.getProcessUtil();
    }

    public String getKey() {
        return PURPOSE_KEY;
    }

    public String getUnits() {
        return HTMLUtils.escapeEntities(processUtil.getSizeAbbrLabel());
    }

    public String formatBeta1(double beta1) {
        return FormatUtil.formatNumber(beta1);
    }

    public double getExpectedBeta1() {
        return 1.0;
    }

    public int getYColumn() {
        return ProbeData.ACT_NC_LOC;
    }

    public int mapInputColumn(int xColumn) {
        return ProbeData.EST_OBJ_LOC;
    }

    public String getTargetDataElement() {
        return histData.getDataName(ProbeData.EST_NC_LOC, true);
    }

    public int getTargetColumn() {
        return ProbeData.EST_NC_LOC;
    }

    public String getTargetName() {
        return Translator.translate(processUtil.getSizeMetric());
    }

    public double getMult() {
        return 1;
    }

    public String getTargetDataElementMin() {
        return "Estimated Min " + processUtil.getSizeAbbr();
    }

    public String getTargetDataElementMax() {
        return "Estimated Max " + processUtil.getSizeAbbr();
    }

}
