
package teamdash.wbs;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.text.NumberFormat;

public class NewAndChangedLOCColumn extends TopDownBottomUpColumn {

    private static final ReadOnlyValue BLANK = new ReadOnlyValue("");


    public NewAndChangedLOCColumn(DataTableModel dataModel) {
        super(dataModel, "N&C LOC");
    }

    public Object getValueAt(int rowIndex) {
        Object result = super.getValueAt(rowIndex);
        if (result instanceof ReadOnlyValue)
            return BLANK;
        else
            return result;
    }

    protected boolean shouldPrune(WBSNode node) {
        return wbsModel.isDocument(node.getType());
    }

}
