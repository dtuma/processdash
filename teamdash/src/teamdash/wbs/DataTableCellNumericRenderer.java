
package teamdash.wbs;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;

class DataTableCellNumericRenderer extends DefaultTableCellRenderer {

    public DataTableCellNumericRenderer() {
        // TODO: use a smaller font.
        //this.setFont()
        setHorizontalAlignment(JTextField.RIGHT);
    }

    private Font regular = null, bold = null;

    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
        NumericDataValue number = (NumericDataValue) value;
        String display = "";
        String errorMsg = null;
        boolean readOnly = false;
        Color errorColor = null;

        if (number != null) {
            display = number.isInvisible ? "" : number.toString();
            errorMsg = number.errorMessage;
            errorColor = number.errorColor;
            readOnly = (number.isEditable == false);
        }

        Component result = super.getTableCellRendererComponent
            (table, display, isSelected, hasFocus, row, column);

        result.setForeground
            (getForegroundColor(errorMsg, errorColor, readOnly));

        Font f = getFont(errorMsg != null, result);
        if (f != null) result.setFont(f);

        if (result instanceof JComponent)
            ((JComponent) result).setToolTipText(errorMsg);

        return result;
    }

    protected Color getForegroundColor(String errorMsg, Color errorColor,
                                       boolean readOnly) {
        if (errorMsg != null)
            return errorColor;
        else
            return (readOnly ? Color.gray : Color.black);
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
