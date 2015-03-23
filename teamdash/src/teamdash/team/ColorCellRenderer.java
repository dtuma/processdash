// Copyright (C) 2002-2010 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 3
// of the License, or (at your option) any later version.
//
// Additional permissions also apply; see the README-license.txt
// file in the project root directory for more information.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package teamdash.team;

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

    public static void setUpColorRenderer(JTable table) {
        table.setDefaultRenderer
            (Color.class, new ColorCellRenderer(true));
    }

}
