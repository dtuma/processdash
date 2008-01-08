package teamdash.wbs.columns;

import java.text.DateFormat;
import java.util.Date;

import javax.swing.table.TableCellRenderer;

import teamdash.team.TeamMember;
import teamdash.wbs.CustomRenderedColumn;
import teamdash.wbs.DataTableCellRenderer;
import teamdash.wbs.ReadOnlyValue;
import teamdash.wbs.WBSNode;

public class TeamCompletionDateColumn extends AbstractPrecomputedColumn
        implements CustomRenderedColumn {

    public static final String COLUMN_ID = "Actual_Completion_Date";

    public static final String ATTR_NAME = "Actual_Completion_Date";

    public TeamCompletionDateColumn() {
        super(COLUMN_ID, "Completed", ATTR_NAME, TeamActualTimeColumn.COLUMN_ID);
        this.preferredWidth = 70;
    }

    @Override
    public Class getColumnClass() {
        return Date.class;
    }

    @Override
    public Object getValueAt(WBSNode node) {
        return new ReadOnlyValue(node.getAttribute(ATTR_NAME));
    }

    public static final String getMemberNodeDataAttrName(TeamMember m) {
        return m.getInitials() + "@Actual_Node_Completion_Date";
    }

    public TableCellRenderer getCellRenderer() {
        return new DateCellRenderer();
    }

    private class DateCellRenderer extends DataTableCellRenderer {

        @Override
        protected Object format(Object value) {
            if (value instanceof Date) {
                return FORMATTER.format((Date) value);
            } else {
                return value;
            }
        }

    }

    private static final DateFormat FORMATTER = DateFormat
            .getDateInstance(DateFormat.SHORT);
}
