package teamdash.wbs;

import java.awt.Component;

import javax.swing.JTable;

/** This simple renderer appends a fixed string to the end of
 * displayed values.
 */
public class DataTableStringSuffixRenderer extends DataTableCellRenderer {

    private String suffix;

    /** Create a renderer to append the given suffix to the end of
     * displayed values */
    public DataTableStringSuffixRenderer(String suffix) {
        this.suffix = suffix;
    }

    public Component getTableCellRendererComponent
        (JTable table, Object value, boolean isSelected,
         boolean hasFocus, int row, int column)
    {
        if (value != null)
            value = String.valueOf(value) + suffix;

        return super.getTableCellRendererComponent
            (table, value, isSelected, hasFocus, row, column);
    }

}
