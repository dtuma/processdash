
package teamdash.wbs;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;

/** Default renderer for displaying {@link NumericDataValue}s in a
 * {@link DataTableModel}.
 *
 * This renderer can optionally make numbers invisible.  It can
 * display numbers in grey if they are read-only.  Finally, it can
 * display erroneous values in a special color with a descriptive
 * tooltip.
 */
class DataTableCellNumericRenderer extends DefaultTableCellRenderer {

    public DataTableCellNumericRenderer() {
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
            // extract information from the NumericDataValue object.
            display = number.isInvisible ? "" : number.toString();
            errorMsg = number.errorMessage;
            errorColor = number.errorColor;
            readOnly = (number.isEditable == false);
        }

        // ask our superclass for an appropriate renderer component.
        Component result = super.getTableCellRendererComponent
            (table, display, isSelected, hasFocus, row, column);

        // change the foreground color for read-only or erroneous values.
        result.setForeground
            (getForegroundColor(errorMsg, errorColor, readOnly));

        // use a bold font for erroneous values.
        Font f = getFont(errorMsg != null, result);
        if (f != null) result.setFont(f);

        if (result instanceof JComponent)
            // set or remove a descriptive tooltip
            ((JComponent) result).setToolTipText(errorMsg);

        return result;
    }

    /** Determine the appropriate foreground color based on the conditions
     * supplied */
    protected Color getForegroundColor(String errorMsg, Color errorColor,
                                       boolean readOnly) {
        if (errorMsg != null)
            return errorColor;
        else
            return (readOnly ? Color.gray : Color.black);
    }


    /** construct, cache, and return bold and normal fonts */
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
