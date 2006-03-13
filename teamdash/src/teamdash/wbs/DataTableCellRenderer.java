
package teamdash.wbs;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.Font;
import java.awt.Component;
import java.awt.Color;

/** Default renderer for displaying values in a {@link DataTableModel}.
 *
 * This renderer can display values in grey if they are read-only.  It can
 * also display erroneous values in a special color with a descriptive
 * tooltip.
 */

class DataTableCellRenderer extends DefaultTableCellRenderer {

    public DataTableCellRenderer() {
        //this.setFont()
    }

    private Font regular = null, bold = null;

    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
        ErrorValue errorValue = null;
        boolean readOnly = false;

        // unwrap ErrorValue objects and ReadOnlyValue objects
        while (value instanceof WrappedValue) {
            if (value instanceof ErrorValue)
                errorValue = (ErrorValue) value;
            else if (value instanceof ReadOnlyValue)
                readOnly = true;
            value = ((WrappedValue) value).value;
        }

        // ask our superclass for an appropriate renderer component.
        Component result = super.getTableCellRendererComponent
            (table, value, isSelected, hasFocus, row, column);

        // change the foreground color for read-only or erroneous values.
        result.setForeground(getForegroundColor(errorValue, readOnly));

        // use a bold font for erroneous values.
        Font f = getFont(errorValue != null, result);
        if (f != null) result.setFont(f);

        if (result instanceof JComponent)
            // set or remove a descriptive tooltip
            ((JComponent) result).setToolTipText
                (errorValue == null ? null : errorValue.error);

        return result;
    }

    /** Determine the appropriate foreground color based on the conditions
     * supplied */
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
