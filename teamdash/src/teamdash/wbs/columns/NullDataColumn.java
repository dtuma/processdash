package teamdash.wbs.columns;

import teamdash.wbs.WBSNode;

public class NullDataColumn extends AbstractDataColumn {

    public NullDataColumn() {
        this.columnID = this.columnName = "null";
    }

    public boolean isCellEditable(WBSNode node) { return false; }
    public Object getValueAt(WBSNode node)      { return null;  }
    public void setValueAt(Object aValue, WBSNode node) {}

}
