
package teamdash.wbs.columns;

import teamdash.wbs.DataTableModel;
import teamdash.wbs.ReadOnlyValue;
import teamdash.wbs.WBSNode;


public class NewAndChangedLOCColumn extends TopDownBottomUpColumn {

    private static final ReadOnlyValue BLANK = new ReadOnlyValue("");


    public NewAndChangedLOCColumn(DataTableModel dataModel) {
        super(dataModel, "N&C LOC", "N&C LOC");
        setPruner(new Pruner() {
            public boolean shouldPrune(WBSNode node) {
                return ! wbsModel.isSoftwareComponent(node.getType());
            } });
        hideInheritedValues = true;
    }
}
