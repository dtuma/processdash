// Copyright (C) 2018 Tuma Solutions, LLC
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

package teamdash.wbs.columns;

import javax.swing.JLabel;
import javax.swing.table.TableCellRenderer;

import teamdash.wbs.CustomRenderedColumn;
import teamdash.wbs.ItalicNumericCellRenderer;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.TeamProcess;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;

public class WorkflowYieldColumn extends WorkflowQualityParameterColumn
        implements CustomRenderedColumn {

    public WorkflowYieldColumn(WBSModel workflowWbsModel,
            TeamProcess teamProcess) {
        super(workflowWbsModel, teamProcess, ATTR_NAME);
        this.columnName = resources.getString("Workflow.Yield.Name");
        this.columnID = COLUMN_ID;
    }

    @Override
    protected double getProcessDefaultForPhase(String phase) {
        return teamProcess.getPhaseEstYield(phase) * 100;
    }

    @Override
    protected void setValueForNode(double value, WBSNode node) {
        if (value > 100)
            ;
        else
            super.setValueForNode(value, node);
    }

    @Override
    public TableCellRenderer getCellRenderer() {
        return new CellRenderer();
    }

    private static class CellRenderer extends ItalicNumericCellRenderer {

        public CellRenderer() {
            super(DEFAULT_TOOLTIP);
            setHorizontalAlignment(JLabel.CENTER);
        }

        @Override
        protected String formatNumber(NumericDataValue number) {
            return number.toString() + "%";
        }

    }

    private static final String ATTR_NAME = "Yield";

    public static final String COLUMN_ID = ATTR_NAME;

}
