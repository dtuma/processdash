package teamdash.wbs.columns;

import teamdash.wbs.DataTableModel;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.WBSNode;

/** A column capturing the time not yet assigned to any team member */
public class UnassignedTimeColumn extends TopDownBottomUpColumn {

    public static final String COLUMN_ID = "Unassigned-Time";

    public UnassignedTimeColumn(DataTableModel m) {
        super(m, "Unassigned Time", COLUMN_ID);
        this.topDownAttrName += "_calc";
        this.bottomUpAttrName += "_calc";
        this.dependentColumns = new String[] { TeamTimeColumn.COLUMN_ID };
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

    void clearUnassignedTime(WBSNode node) {
        node.setAttribute(topDownAttrName, null);
    }

    void setUnassignedTime(WBSNode node, double val) {
        node.setNumericAttribute(topDownAttrName, val);
    }

    private static final NumericDataValue ZERO = new NumericDataValue(0, false,
            true, null);

}
