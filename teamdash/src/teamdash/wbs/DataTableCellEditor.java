
package teamdash.wbs;

import java.awt.Component;

import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

/** Default cell editor for the data table.
 *
 * This class unwraps {@link WrappedValue} objects and passes them to
 * the DefaultCellEditor.  It also registers itself with the
 * appropriate UndoList, and sends change notifications to that
 * UndoList when the user edits a table cell value.
 */
public class DataTableCellEditor extends DefaultCellEditor {

    private Object startingValue;
    private String columnName;
    protected JTable table = null;

    public DataTableCellEditor() {
        super(new JTextField());
    }

    public Component getTableCellEditorComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 int row,
                                                 int column) {
        // unwrap error values.
        value = WrappedValue.unwrap(value);

        Component result = super.getTableCellEditorComponent
            (table, value, isSelected, row, column);

        // register with the appropriate UndoList
        this.startingValue = getCellEditorValue();
        this.columnName = table.getColumnName(column);
        this.table = table;
        UndoList.addCellEditor(table, this);

        // select all the text in the component (users are used to this)
        if (result instanceof JTextComponent)
            ((JTextComponent) result).selectAll();

        return result;
    }

    public boolean stopCellEditing() {
        // check to see if the value in this cell actually changed.
        boolean valueChanged = !equal(startingValue, getCellEditorValue());

        // call our superclass to actually stop editing.
        boolean result = super.stopCellEditing();

        // if editing was stopped AND the value was changed, notify
        // the UndoList about the change.
        if (result && valueChanged)
            UndoList.madeChange
                (table, "Editing value in '"+columnName+"' column");

        return result;
    }

    /** Compare two (possibly null) values for equality */
    private boolean equal(Object a, Object b) {
        if (a == null && b == null) return true;
        return (a != null && a.equals(b));
    }
}
