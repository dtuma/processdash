
package teamdash.wbs;

import java.awt.Component;

import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;

/** Default cell editor for the data table.
 *
 * This class simply unwraps {@link ErrorValue} objects and passes them to the
 * DefaultCellEditor.
 */
class DataTableCellEditor extends DefaultCellEditor {

    private Object startingValue;
    private String columnName;
    private JTable table = null;

    DataTableCellEditor() {
        super(new JTextField());
    }

    public Component getTableCellEditorComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 int row,
                                                 int column) {
        if (value instanceof ErrorValue)
            value = ((ErrorValue) value).value;

        Component result = super.getTableCellEditorComponent
            (table, value, isSelected, row, column);

        this.startingValue = getCellEditorValue();
        this.columnName = table.getColumnName(column);
        this.table = table;
        UndoList.addCellEditor(table, this);
        System.out.println("Editing column " + columnName);

        return result;
    }

    public boolean stopCellEditing() {
        boolean valueChanged = !equal(startingValue, getCellEditorValue());
        boolean result = super.stopCellEditing();
        if (result && valueChanged)
            UndoList.madeChange
                (table, "Editing value in '"+columnName+"'column");

        return result;
    }

    private boolean equal(Object a, Object b) {
        if (a == null && b == null) return true;
        return (a != null && a.equals(b));
    }
}
