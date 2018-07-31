// Copyright (C) 2002-2018 Tuma Solutions, LLC
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

package teamdash.wbs;

import java.awt.Color;
import java.awt.Component;

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
                result.setFont(TableFontHandler.getItalic(table));
                ((JComponent) result).setBorder(inheritedBorder);
            }
        } else {
            ((JComponent) result).setBorder(null);
        }

        return result;
    }

}
