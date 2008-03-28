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
        super(m, "Unassigned Time", COLUMN_ID);
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
