package teamdash.wbs;

import java.awt.Component;

import javax.swing.JTable;

public class DataTableStringSuffixRenderer extends DataTableCellRenderer {

    private String suffix;

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
