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

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.process.ProcessUtil;


public class TimeMethodPurpose extends MethodPurpose {

    static final String PURPOSE_KEY = "Time";

    private ProcessUtil processUtil;
    private ProbeData histData;

    TimeMethodPurpose(ProbeData data) {
        this.processUtil = data.getProcessUtil();
        this.histData = data;
    }

    public String getKey() {
        return PURPOSE_KEY;
    }

    public String getUnits() {
        return Wizard.resources.getHTML("Hours");
    }

    public String formatBeta1(double beta1) {
        return processUtil.formatProductivity(1.0 / beta1);
    }

    public double getExpectedBeta1() {
        return 1.0 / histData.getProductivity();
    }

    public double getBeta1MaxRatio() {
        return 1.5;
    }

    public int getYColumn() {
        return ProbeData.ACT_TIME;
    }

    public String getTargetDataElement() {
        return histData.getDataName(ProbeData.EST_TIME, true);
    }

    public int getTargetColumn() {
        return ProbeData.EST_TIME;
    }

    public String getTargetName() {
        return resources.getString("Method.Time.Label");
    }

    public int mapInputColumn(int xColumn) {
        boolean strictMethods = true;
        if ("false".equalsIgnoreCase
            (Settings.getVal("probeWizard.strictTimeMethods")))
            strictMethods = false;

        if (strictMethods)
            return ProbeData.EST_OBJ_LOC;
        else if (xColumn != ProbeData.EST_OBJ_LOC &&
                 xColumn != ProbeData.EST_NC_LOC)
            return ProbeData.EST_NC_LOC;
        else
            return xColumn;
    }

    public double getMult() {
        return 60;
    }

    public String getTargetDataElementMin() {
        return "Estimated Min Time";
    }

    public String getTargetDataElementMax() {
        return "Estimated Max Time";
    }

}
