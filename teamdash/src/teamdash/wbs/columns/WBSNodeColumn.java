package teamdash.wbs.columns;

import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;

public class WBSNodeColumn extends AbstractDataColumn {

    private WBSModel wbsModel;

    public WBSNodeColumn(WBSModel wbsModel) {
        this.wbsModel = wbsModel;
        this.columnName = "Name";
        this.columnID = "wbsNode";
    }

    public Class getColumnClass() { return WBSNode.class; }

    public boolean isCellEditable(WBSNode node) {
        return node != wbsModel.getRoot();
    }

    public Object getValueAt(WBSNode node) {
        return node;
    }

    public void setValueAt(Object aValue, WBSNode node) {}

}
