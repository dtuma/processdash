package teamdash.wbs;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.border.Border;

/** Special cell renderer for strings that can italicize displayed values.
 * 
 * The presence of a certain error message is interpreted as a flag that the
 * value should be displayed in italics (rather than in a bold colored font,
 * like the regular {@link DataTableCellRenderer} would do).
 */
public class ItalicCellRenderer extends DataTableCellRenderer {


    private Font italic = null;
    private String messageToItalicize;
    private Border inheritedBorder;

    public ItalicCellRenderer(String messageToItalicize) {
        this.messageToItalicize = messageToItalicize;
        inheritedBorder = BorderFactory.createEmptyBorder(0, 10, 0, 0);
    }


    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {

        Component result = super.getTableCellRendererComponent
            (table, value, isSelected, hasFocus, row, column);

        if (value instanceof ErrorValue) {
            ErrorValue err = (ErrorValue) value;
            if (err.error != null &&
                err.error.equals(messageToItalicize)) {
                result.setForeground(Color.black);
                result.setFont(getItalicFont(result));
                ((JComponent) result).setBorder(inheritedBorder);
            }
        } else {
            ((JComponent) result).setBorder(null);
        }

        return result;
    }


    /** Create and cache an appropriate italic font. */
    protected Font getItalicFont(Component c) {
        if (italic == null) {
            Font regular = super.getFont(false, c);
            if (regular != null)
                 italic = regular.deriveFont(Font.ITALIC);
        }

        return italic;
    }
}
