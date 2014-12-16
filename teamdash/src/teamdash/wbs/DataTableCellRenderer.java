// Copyright (C) 2002-2014 Tuma Solutions, LLC
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
public class DataTableCellRenderer extends DefaultTableCellRenderer {

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
        String html = null;
        String tooltip = null;

        // unwrap ErrorValue objects and ReadOnlyValue objects
        while (value instanceof WrappedValue) {
            if (value instanceof ErrorValue)
                errorValue = (ErrorValue) value;
            else if (value instanceof ReadOnlyValue)
                readOnly = true;
            else if (value instanceof HtmlRenderedValue) {
                HtmlRenderedValue hValue = (HtmlRenderedValue) value;
                html = hValue.html;
                tooltip = hValue.tooltip;
            }
            value = ((WrappedValue) value).value;
        }

        Object toDisplay = (html == null ? format(value) : html);

        // ask our superclass for an appropriate renderer component.
        Component result = super.getTableCellRendererComponent
            (table, toDisplay, isSelected, hasFocus, row, column);

        // change the foreground color for read-only or erroneous values.
        result.setForeground(getForegroundColor(errorValue, readOnly));

        // use a bold font for erroneous values.
        Font f = getFont(errorValue != null, result);
        if (f != null) result.setFont(f);

        if (result instanceof JComponent)
            // set or remove a descriptive tooltip
            ((JComponent) result).setToolTipText
                (errorValue == null ? tooltip : errorValue.error);

        return result;
    }

    protected Object format(Object value) {
        return value;
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
        case ErrorValue.INFO: return Color.blue;
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
