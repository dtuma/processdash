
package teamdash.wbs;

public interface DataColumn {

    public String getColumnName();
    public Class getColumnClass();
    public boolean isCellEditable(int rowIndex);
    public Object getValueAt(int rowIndex);
    public void setValueAt(Object aValue, int rowIndex);

}
