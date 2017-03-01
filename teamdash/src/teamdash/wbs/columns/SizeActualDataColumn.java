// Copyright (C) 2010 Tuma Solutions, LLC
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

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.CustomNamedColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.TeamProcess;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;

public class SizeActualDataColumn extends AbstractNumericColumn implements
        CalculatedDataColumn, CustomNamedColumn {

    private WBSModel wbsModel;

    private String resultAttrName;

    private String nodeAttrName;

    public SizeActualDataColumn(DataTableModel dataModel, String units,
            boolean plan) {
        this.wbsModel = dataModel.getWBSModel();
        this.columnName = (plan ? "Plan " : "Actual ") + units;
        this.columnID = getColumnID(units, plan);
        this.resultAttrName = getResultAttrName(units, plan);
        this.nodeAttrName = getNodeAttrName(units, plan);
    }

    public String getCustomColumnName() {
        return getColumnName();
    }

    public boolean recalculate() {
        recalc(wbsModel.getRoot());
        return true;
    }

    protected double recalc(WBSNode node) {
        double result = safe(node.getNumericAttribute(nodeAttrName));

        for (WBSNode child : wbsModel.getChildren(node)) {
            double childVal = recalc(child);
            if (shouldFilterFromCalculations(child) == false)
                result += childVal;
        }

        Object value = null;
        if (result > 0)
            value = new NumericDataValue(result, false);
        node.setAttribute(resultAttrName, value);

        return result;
    }

    protected boolean shouldFilterFromCalculations(WBSNode node) {
        return (node.isHidden()
                && !node.getType().endsWith(TeamProcess.TASK_SUFFIX));
    }

    public void storeDependentColumn(String ID, int columnNumber) {}

    public Object getValueAt(WBSNode node) {
        return node.getAttribute(resultAttrName);
    }

    public boolean isCellEditable(WBSNode node) {
        return false;
    }

    public static void storeData(WBSNode node, String units, double plan,
            double actual) {
        if (node != null) {
            if (plan > 0)
                storeData(node, getNodeAttrName(units, true), plan);
            if (actual > 0)
                storeData(node, getNodeAttrName(units, false), actual);
        }
    }

    private static void storeData(WBSNode node, String attrName, double value) {
        double oldValue = safe(node.getNumericAttribute(attrName));
        double sum = oldValue + value;
        node.setNumericAttribute(attrName, sum);
    }

    private static double safe(double d) {
        return (d > 0 ? d : 0);
    }


    public static final String getColumnID(String units, boolean plan) {
        return (plan ? "Plan" : "Actual") + " Size " + units;
    }

    private static final String getNodeAttrName(String units, boolean plan) {
        return (plan ? "@Plan" : "@Actual") + "_Node_Size_" + units;
    }

    private static final String getResultAttrName(String units, boolean plan) {
        return (plan ? "Plan" : "Actual") + "-Size_" + units;
    }

}
