package teamdash.wbs.columns;

import javax.swing.table.TableCellRenderer;

import teamdash.wbs.CustomRenderedColumn;
import teamdash.wbs.DataTableCellPercentRenderer;

public class PercentSpentColumn extends AbstractPrecomputedColumn implements
        CustomRenderedColumn {

    public static final String COLUMN_ID = "Percent_Spent";

    public static final String RESULT_ATTR = "Percent_Spent";

    protected PercentSpentColumn() {
        super(COLUMN_ID, "%S", RESULT_ATTR, TeamActualTimeColumn.COLUMN_ID);
        this.preferredWidth = 40;
    }

    public TableCellRenderer getCellRenderer() {
        return DataTableCellPercentRenderer.INSTANCE;
    }

}
