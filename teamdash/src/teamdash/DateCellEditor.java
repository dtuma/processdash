package teamdash;

import java.awt.Color;
import java.awt.Component;
import java.text.DateFormat;

import javax.swing.DefaultCellEditor;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;

public class DateCellEditor extends DefaultCellEditor {

    DateFormat formatter;

    Object value;

    public DateCellEditor() {
        super(new JTextField());
        formatter = DateFormat.getDateInstance();
    }

    public boolean stopCellEditing() {
        String str = (String) super.getCellEditorValue();

        if (str == null || str.trim().length() == 0)
            value = null;

        else
            try {
                value = formatter.parse(str);
            } catch (Exception e) {
                setCompBorder(Color.red);
                return false;
            }

        return super.stopCellEditing();

    }

    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int column) {
        this.value = null;
        setCompBorder(Color.black);
        String str = (value == null ? "" : formatter.format(value));
        return super.getTableCellEditorComponent(table, str, isSelected, row,
                column);
    }

    public Object getCellEditorValue() {
        return value;
    }

    private void setCompBorder(Color c) {
        ((JComponent) getComponent()).setBorder(new LineBorder(c));
    }
}
