// Copyright (C) 2002-2010 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

//import java.text.NumberFormat;
//import java.text.ParseException;

import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;

public class WorkflowPercentageColumn extends AbstractNumericColumn {

    private WBSModel wbsModel;

    public WorkflowPercentageColumn(WBSModel wbsModel) {
        this.wbsModel = wbsModel;
        this.columnName = "%";
        this.columnID = COLUMN_ID;
    }

    public boolean isCellEditable(WBSNode node) {
        return TeamTimeColumn.isLeafTask(wbsModel, node);
    }

    public Object getValueAt(WBSNode node) {
        return (isCellEditable(node) ? super.getValueAt(node) : null);
    }

    protected double getValueForNode(WBSNode node) {
        double d = node.getNumericAttribute(ATTR_NAME);
        if (Double.isNaN(d)) d = 100;
            return d;
    }

    protected void setValueForNode(double value, WBSNode node) {
        if (value > 0 && value <= 100)
            node.setNumericAttribute(ATTR_NAME, value);
    }


    private static final String ATTR_NAME = "Workflow Percentage";
    static final String COLUMN_ID = ATTR_NAME;

}
