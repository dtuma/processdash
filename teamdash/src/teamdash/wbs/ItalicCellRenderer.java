package teamdash.wbs;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JTable;

/** Special cell renderer for numbers that can italicize displayed values.
 * 
 * The presence of a certain error message is interpreted as a flag that the
 * value should be displayed in italics (rather than in a bold colored font,
 * like the regular {@link DataTableCellNumericRenderer} would do).
 */
public class ItalicCellRenderer extends DataTableCellNumericRenderer {


    private Font italic = null;
    private String messageToItalicize;

    public ItalicCellRenderer(String messageToItalicize) {
        this.messageToItalicize = messageToItalicize;
    }


    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {

        Component result = super.getTableCellRendererComponent
            (table, value, isSelected, hasFocus, row, column);

        NumericDataValue num = (NumericDataValue) value;
        if (num.errorMessage != null &&
            num.errorMessage.equals(messageToItalicize)) {
            result.setForeground(Color.black);
            result.setFont(getItalicFont(result));
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
