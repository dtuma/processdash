// Copyright (C) 2016 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.reports.workflow;

import java.util.List;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.util.ResultSet;
import net.sourceforge.processdash.tool.db.WorkflowHistDataHelper;
import net.sourceforge.processdash.tool.db.WorkflowHistDataHelper.Enactment;
import net.sourceforge.processdash.tool.db.WorkflowHistDataHelper.PhaseType;

public class ChartData {

    public WorkflowHistDataHelper histData;

    public String primarySizeUnits;

    public int sizeDensityMultiplier = 1;

    public String[] chartArgs;

    public String getRes(String resourceKey) {
        if (resourceKey.endsWith("_FMT"))
            return AnalysisPage.resources.format(resourceKey, chartArgs);
        else
            return AnalysisPage.resources.getString(resourceKey);
    }

    public List<String> getPhases(PhaseType... types) {
        return histData.getPhasesOfType(types);
    }

    public ResultSet getEnactmentResultSet(String... columnKeys) {
        ResultSet result = getEnactmentResultSet(columnKeys.length);
        for (int i = columnKeys.length; i-- > 0;)
            result.setColName(i + 1, getRes(columnKeys[i]));
        return result;
    }

    public ResultSet getEnactmentResultSet(int numColumns) {
        List<Enactment> enactments = histData.getEnactments();
        ResultSet result = new ResultSet(enactments.size(), numColumns);

        result.setColName(0, getRes("Project/Task"));
        for (int i = enactments.size(); i-- > 0;)
            result.setRowName(i + 1, enactments.get(i));

        return result;
    }

    public void setPrimarySizeUnits(String units) {
        primarySizeUnits = units;

        if ("LOC".equalsIgnoreCase(units))
            sizeDensityMultiplier = 1000; // KLOC

        else if (AnalysisPage.isTimeUnits(units))
            sizeDensityMultiplier = Settings.getInt(
                SIZE_MULT_SETTING + "hours", 10); // 10-Hours

        else if (units != null)
            sizeDensityMultiplier = Settings.getInt(SIZE_MULT_SETTING
                    + units.toLowerCase().replace(' ', '_'), 1);
    }

    public boolean isLegitSize() {
        return primarySizeUnits != null && !isTimeUnits();
    }

    public boolean isTimeUnits() {
        return AnalysisPage.isTimeUnits(primarySizeUnits);
    }

    public String getDensityStr() {
        if (isTimeUnits())
            return AnalysisPage.resources.format("Hours_Units_FMT",
                sizeDensityMultiplier);

        else if (sizeDensityMultiplier == 1)
            return primarySizeUnits;

        else if (sizeDensityMultiplier == 1000
                && "LOC".equalsIgnoreCase(primarySizeUnits))
            return "KLOC";

        else
            return sizeDensityMultiplier + " " + primarySizeUnits;
    }

    private static final String SIZE_MULT_SETTING = "workflow.sizeDensityFactor.";

}
