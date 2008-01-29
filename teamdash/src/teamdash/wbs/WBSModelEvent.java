package teamdash.wbs;

import javax.swing.event.TableModelEvent;
import javax.swing.table.TableModel;

public class WBSModelEvent extends TableModelEvent {

    private boolean isExpansionOnly;

    public WBSModelEvent(TableModel source, boolean isExpansionOnly) {
        super(source);
        this.isExpansionOnly = isExpansionOnly;
    }

    public WBSModelEvent(TableModel source, int firstRow, int lastRow,
            int column, int type, boolean isExpansionOnly) {
        super(source, firstRow, lastRow, column, type);
        this.isExpansionOnly = isExpansionOnly;
    }

    public boolean isExpansionOnly() {
        return isExpansionOnly;
    }

    public static boolean isStructuralChange(TableModelEvent e) {
        if (e instanceof WBSModelEvent) {
            WBSModelEvent wbsEvent = (WBSModelEvent) e;
            if (wbsEvent.isExpansionOnly())
                return false;
        }
        return true;
    }
}
