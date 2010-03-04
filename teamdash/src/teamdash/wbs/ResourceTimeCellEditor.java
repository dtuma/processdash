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

package teamdash.wbs;

import java.awt.Component;
import java.util.regex.Pattern;

import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

public class ResourceTimeCellEditor extends DefaultCellEditor {

    private DataTableModel dataModel;
    private int timePerPersonColumn = -1;
    private int editingRow;
    private Component editor;

    public ResourceTimeCellEditor(DataTableModel dataModel) {
        super(new JTextField());
        this.dataModel = dataModel;
    }

    public Component getTableCellEditorComponent
        (JTable table, Object value, boolean isSelected, int row, int column) {

        this.editingRow = row;
        editor = super.getTableCellEditorComponent
            (table, value, isSelected, row, column);
        return editor;
    }


    public boolean stopCellEditing() {
        if (newValueIsBad()) {
            promptForTime();
            return false;
        } else
            return super.stopCellEditing();
    }

    private boolean newValueIsBad() {
        if (!(editor instanceof JTextComponent)) return false;

        String value = ((JTextComponent) editor).getText();
        if (value == null) return false;
        if (CHAR.matcher(value).find() == false) return false;
        if (DIGIT.matcher(value).find() == true) return false;

        if (timePerPersonColumn == -1)
            timePerPersonColumn = dataModel.findColumn("Time Per Person");
        if (timePerPersonColumn == -1) return false;

        double timePerPerson = NumericDataValue.parse
            (dataModel.getValueAt(editingRow, timePerPersonColumn));
        return !(timePerPerson > 0);
    }

    private static final Pattern CHAR = Pattern.compile("[a-zA-Z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");


    private void promptForTime() {
        JTextComponent field = (JTextComponent) editor;
        String prompt = field.getText();
        prompt = prompt + "(???)";
        field.setText(prompt);
        int len = prompt.length();
        field.select(len-4, len-1);
    }

}
