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

import java.util.HashMap;
import java.util.Map;

import teamdash.wbs.DataTableModel;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.WBSNode;

/** A column capturing the time not yet assigned to any team member */
public class UnassignedTimeColumn extends TopDownBottomUpColumn {

    public static final String COLUMN_ID = "Unassigned-Time";

    public static final String MILESTONE_UNASSIGNED_TIME_ATTR =
        "Milestone_Unassigned_Time";

    public UnassignedTimeColumn(DataTableModel m) {
        super(m, resources.getString("Unassigned_Time.Name"), COLUMN_ID);
        this.topDownAttrName += "_calc";
        this.bottomUpAttrName += "_calc";
        this.dependentColumns = new String[] { TeamTimeColumn.COLUMN_ID,
                MilestoneColumn.COLUMN_ID };
        this.preferredWidth = 80;
    }

    public Object getValueAt(WBSNode node) {
        NumericDataValue value = (NumericDataValue) super.getValueAt(node);
        if (value == null || value.value == 0) {
            return ZERO;
        } else {
            value.isEditable = false;
            return value;
        }
    }

    private Map<Integer, Double> milestoneTimes;

    @Override
    public boolean recalculate() {
        milestoneTimes = new HashMap<Integer, Double>();
        wbsModel.getRoot().setAttribute(MILESTONE_UNASSIGNED_TIME_ATTR,
            milestoneTimes);

        return super.recalculate();
    }


    @Override
    protected double recalc(WBSNode node) {
        if (wbsModel.isLeaf(node)) {
            double unassignedTime = node.getNumericAttribute(topDownAttrName);
            if (unassignedTime > 0) {
                int milestone = MilestoneColumn.getMilestoneID(node);
                Double current = milestoneTimes.get(milestone);
                if (current == null)
                    milestoneTimes.put(milestone, unassignedTime);
                else
                    milestoneTimes.put(milestone, current + unassignedTime);
            }
        }

        return super.recalc(node);
    }

    void clearUnassignedTime(WBSNode node) {
        node.setAttribute(topDownAttrName, null);
    }

    void setUnassignedTime(WBSNode node, double val) {
        node.setNumericAttribute(topDownAttrName, val);
    }

    private static final NumericDataValue ZERO = new NumericDataValue(0, false,
            true, null);

}
