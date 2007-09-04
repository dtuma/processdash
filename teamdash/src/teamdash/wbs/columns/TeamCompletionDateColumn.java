package teamdash.wbs.columns;

import java.text.DateFormat;
import java.util.Date;

import teamdash.team.TeamMember;
import teamdash.wbs.ReadOnlyValue;
import teamdash.wbs.WBSNode;

public class TeamCompletionDateColumn extends AbstractPrecomputedColumn {

    public static final String COLUMN_ID = "Actual_Completion_Date";

    public static final String ATTR_NAME = "Actual_Completion_Date";

    public TeamCompletionDateColumn() {
        super(COLUMN_ID, "Completed", ATTR_NAME, TeamActualTimeColumn.COLUMN_ID);
        this.preferredWidth = 70;
    }

    @Override
    public Class getColumnClass() {
        return Object.class;
    }

    @Override
    public Object getValueAt(WBSNode node) {
        String display = "";
        Date date = (Date) node.getAttribute(ATTR_NAME);
        if (date != null)
            display = FORMATTER.format(date);
        return new ReadOnlyValue(display);
    }

    public static final String getMemberNodeDataAttrName(TeamMember m) {
        return m.getInitials() + "@Actual_Node_Completion_Date";
    }

    private static final DateFormat FORMATTER = DateFormat
            .getDateInstance(DateFormat.SHORT);
}
