
package teamdash.wbs;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.Font;
import java.awt.Component;
import java.awt.Color;

class DataTableCellRenderer extends DefaultTableCellRenderer {

    public DataTableCellRenderer() {}

    private Font regular = null, bold = null;

    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
        ErrorValue errorValue = null;
        boolean readOnly = false;

        if (value instanceof ErrorValue) {
            errorValue = (ErrorValue) value;
            value = errorValue.value;
        }
        if (value instanceof ReadOnlyValue) {
            readOnly = true;
            value = ((ReadOnlyValue) value).value;
        }

        Component result = super.getTableCellRendererComponent
            (table, value, isSelected, hasFocus, row, column);

        result.setForeground(getForegroundColor(errorValue, readOnly));

        Font f = getFont(errorValue != null, result);
        if (f != null) result.setFont(f);

        if (result instanceof JComponent)
            ((JComponent) result).setToolTipText
                (errorValue == null ? null : errorValue.error);

        return result;
    }

    protected Color getForegroundColor(ErrorValue errorValue,
                                       boolean readOnly) {
        if (errorValue == null)
            return (readOnly ? Color.gray : Color.black);
        switch (errorValue.severity) {
        case ErrorValue.ERROR: return Color.red;
        case ErrorValue.WARNING: return Color.orange;
        }
        return Color.black;
    }


    protected Font getFont(boolean bold, Component c) {
        if (this.regular == null) {
            Font base = c.getFont();
            if (base == null) return null;
            this.regular = base.deriveFont(Font.PLAIN);
            this.bold    = base.deriveFont(Font.BOLD);
        }
        return (bold ? this.bold : this.regular);
    }

}
