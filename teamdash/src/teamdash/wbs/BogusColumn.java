
package teamdash.wbs;

public class BogusColumn implements DataColumn {

    private String name;

    public BogusColumn(String name) {
        this.name = name;
    }

    public String getColumnName() { return name; }
    public Class getColumnClass() { return String.class; }
    public boolean isCellEditable(int rowIndex) { return true; }
    public Object getValueAt(int rowIndex) { return name + rowIndex; }
    public void setValueAt(Object aValue, int rowIndex) {}

}
