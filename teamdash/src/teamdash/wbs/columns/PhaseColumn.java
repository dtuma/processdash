package teamdash.wbs.columns;

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.ReadOnlyValue;
import teamdash.wbs.WBSNode;

public class PhaseColumn extends AbstractDataColumn
    implements CalculatedDataColumn
{

    public PhaseColumn() {
        this.columnID = this.columnName = "Phase";
    }

    public Object getValueAt(WBSNode node) {
        String type = node.getType();
        if (type.endsWith(" Task"))
            return new ReadOnlyValue(type.substring(0, type.length() - 5));
        else
            return null;
    }


    public boolean isCellEditable(WBSNode node) { return false; }
    public boolean recalculate() { return true; }

    public void storeDependentColumn(String ID, int columnNumber) {}
    public void setValueAt(Object aValue, WBSNode node) {}
}
