package teamdash.wbs.columns;

import java.util.Map;

import teamdash.wbs.DataTableModel;
import teamdash.wbs.WBSNode;

/** This class echoes the value of the SizeTypeColumn, but only if a top-down
 * size estimate has been entered for a particular node. If no top-down
 * estimate has been entered, it returns null.
 */
public class DirectSizeTypeColumn extends SizeTypeColumn {

    public static final String COLUMN_ID = "Direct Size Units";

    public DirectSizeTypeColumn(DataTableModel m, Map sizeMetrics) {
        super(m, sizeMetrics);
        this.columnID = this.columnName = COLUMN_ID;
    }

    public Object getValueAt(WBSNode node) {
        Object result = super.getValueAt(node);
        if (result == null) return null;
        String units = String.valueOf(result);
        String addedAttr = TopDownBottomUpColumn.getTopDownAttrName
            (SizeAccountingColumnSet.getAddedID(units));
        String modifiedAttr = TopDownBottomUpColumn.getTopDownAttrName
            (SizeAccountingColumnSet.getModifiedID(units));

        if (node.getAttribute(addedAttr) != null ||
            node.getAttribute(modifiedAttr) != null)
            return units;
        else
            return null;
    }

}
