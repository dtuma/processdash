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

import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.util.ResultSet;
import net.sourceforge.processdash.tool.db.WorkflowHistDataHelper;
import net.sourceforge.processdash.tool.db.WorkflowHistDataHelper.Enactment;

public class ChartData {

    public WorkflowHistDataHelper histData;

    public String primarySizeUnits;

    public String[] chartArgs;

    public String getRes(String resourceKey) {
        if (resourceKey.endsWith("_FMT"))
            return AnalysisPage.resources.format(resourceKey, chartArgs);
        else
            return AnalysisPage.resources.getString(resourceKey);
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

    public void writePhaseTimePct(ResultSet resultSet, int firstCol,
            Object... phases) {
        for (int i = phases.length; i-- > 0; ) {
            int col = firstCol + i;
            Object onePhase = phases[i];
            if (onePhase instanceof String)
                resultSet.setColName(col, (String) onePhase);
            resultSet.setFormat(col, "100%");

            for (int row = resultSet.numRows(); row > 0; row--) {
                Enactment e = (Enactment) resultSet.getRowObj(row);
                double phaseTime = e.actualTime(onePhase);
                double pctTime = phaseTime / e.actualTime();
                resultSet.setData(row, col, new DoubleData(pctTime));
            }
        }
    }

}
