
package teamdash.wbs;

public interface DataColumn {

    public String getColumnID();
    public String getColumnName();
    public Class getColumnClass();
    public boolean isCellEditable(WBSNode node);
    public Object getValueAt(WBSNode node);
    public void setValueAt(Object aValue, WBSNode node);

}
