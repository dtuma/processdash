
package teamdash.wbs.columns;

import teamdash.wbs.DataColumn;
import teamdash.wbs.WBSNode;

public class BogusColumn implements DataColumn {

    private String name;

    public BogusColumn(String name) {
        this.name = name;
    }

    public String getColumnName() { return name; }
    public Class getColumnClass() { return String.class; }
    public boolean isCellEditable(WBSNode node) { return true; }
    public Object getValueAt(WBSNode node) { return name+":"+node.getName(); }
    public void setValueAt(Object aValue, WBSNode node) {}
    public String getColumnID() { return name; }
    public int getPreferredWidth() {
        return -1;
    }

}
