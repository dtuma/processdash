
package teamdash.wbs;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.Font;
import java.awt.Component;
import java.awt.Color;

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
