package teamdash;

import java.awt.Color;
import java.awt.Component;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

public class ColorCellRenderer extends JLabel
implements TableCellRenderer {

    Border unselectedBorder = null;
    Border selectedBorder = null;
    boolean isBordered = true;

    public ColorCellRenderer(boolean isBordered) {
        super(); //"foo");
        this.isBordered = isBordered;
        setOpaque(true); //MUST do this for background to show up.
    }

    private Border getSelectedBorder(JTable table) {
        if (selectedBorder == null)
            selectedBorder = BorderFactory.createMatteBorder
                (2, 5, 2, 5, table.getSelectionBackground());
        return selectedBorder;
    }
    private Border getUnselectedBorder(JTable table) {
        if (unselectedBorder == null)
            unselectedBorder = BorderFactory.createMatteBorder
                (2,5,2,5, table.getBackground());
        return unselectedBorder;
    }

    public Component getTableCellRendererComponent
        (JTable table, Object color, boolean isSelected,
         boolean hasFocus, int row, int column)
    {
        setBackground((Color) color);
        setForeground(xorColor((Color) color));
        if (isBordered)
            setBorder(isSelected
                      ? getSelectedBorder(table)
                      : getUnselectedBorder(table));

        return this;
    }

    private static HashMap XOR_COLORS = new HashMap();
    private static Color xorColor(Color c) {
        Color result = (Color) XOR_COLORS.get(c);
        if (result == null) {
            result = new Color(c.getRed() ^ 255,
                               c.getGreen() ^ 255,
                               c.getBlue() ^ 255);
            XOR_COLORS.put(c, result);
        }
        return result;
    }

    static void setUpColorRenderer(JTable table) {
        table.setDefaultRenderer
            (Color.class, new ColorCellRenderer(true));
    }

}
