
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

    DataTableCellEditor() { super(new JTextField()); }

    public Component getTableCellEditorComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 int row,
                                                 int column) {
        if (value instanceof ErrorValue)
            value = ((ErrorValue) value).value;

        return super.getTableCellEditorComponent
            (table, value, isSelected, row, column);
    }
}
