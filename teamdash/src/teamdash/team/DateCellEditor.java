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
import java.text.DateFormat;

import javax.swing.DefaultCellEditor;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;

public class DateCellEditor extends DefaultCellEditor {

    DateFormat formatter;

    Object value;

    public DateCellEditor() {
        super(new JTextField());
        formatter = DateFormat.getDateInstance();
    }

    public boolean stopCellEditing() {
        String str = (String) super.getCellEditorValue();

        if (str == null || str.trim().length() == 0)
            value = null;

        else
            try {
                value = formatter.parse(str);
            } catch (Exception e) {
                setCompBorder(Color.red);
                return false;
            }

        return super.stopCellEditing();

    }

    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int column) {
        this.value = null;
        setCompBorder(Color.black);
        String str = (value == null ? "" : formatter.format(value));
        return super.getTableCellEditorComponent(table, str, isSelected, row,
                column);
    }

    public Object getCellEditorValue() {
        return value;
    }

    private void setCompBorder(Color c) {
        ((JComponent) getComponent()).setBorder(new LineBorder(c));
    }
}
