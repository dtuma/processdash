package teamdash.wbs.columns;

import javax.swing.table.TableCellRenderer;

import teamdash.wbs.CustomRenderedColumn;
import teamdash.wbs.DataTableCellPercentRenderer;
import teamdash.wbs.WBSNode;

public class PercentCompleteColumn extends AbstractPrecomputedColumn implements
        CustomRenderedColumn {

    public static final String COLUMN_ID = "Percent_Complete";

    public static final String RESULT_ATTR = "Percent_Complete";

    protected PercentCompleteColumn() {
        super(COLUMN_ID, "%C", RESULT_ATTR, TeamActualTimeColumn.COLUMN_ID);
        this.preferredWidth = 40;
    }

    public TableCellRenderer getCellRenderer() {
        return DataTableCellPercentRenderer.INSTANCE;
    }

    public static boolean isComplete(WBSNode node) {
        double percent = node.getNumericAttribute(RESULT_ATTR);
        return percent > DataTableCellPercentRenderer.ONE_HUNDRED_PCT_CUTOFF;
    }

}
