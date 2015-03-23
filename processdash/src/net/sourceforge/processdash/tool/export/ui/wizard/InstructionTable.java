// Copyright (C) 2005 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.export.ui.wizard;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.util.EventListener;
import java.util.EventObject;

import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import net.sourceforge.processdash.tool.export.mgr.AbstractInstruction;
import net.sourceforge.processdash.tool.export.mgr.AbstractManager;
import net.sourceforge.processdash.ui.lib.WrappedTextTableCellRenderer;

class InstructionTable extends JTable {

    public interface Listener extends EventListener {
        public void toggleEnabled(int row);

        public void edit(int row);
    }

    public InstructionTable(TableModel model) {
        super(model);
        setRowSelectionAllowed(true);
        setUpWrappedTextColumn();
        setUpColumnWidths(AbstractManager.COLUMN_WIDTHS);
        setUpPreferredViewportSize();
    }

    void setUpWrappedTextColumn() {
        TableColumn column = getColumnModel().getColumn(1);
        column.setCellRenderer(new WrappedTextTableCellRenderer(this));
    }

    private void setUpColumnWidths(int[] columnWidths) {
        for (int i = 0; i < columnWidths.length; i++) {
            TableColumn col = getColumnModel().getColumn(i);
            col.setMinWidth(50);
            col.setMaxWidth(9999);
            col.setPreferredWidth(columnWidths[i]);
        }
    }

    private void setUpPreferredViewportSize() {
        preferredViewportSize = new Dimension(500, 500);
    }

    public void addInstructionTableListener(Listener l) {
        listenerList.add(Listener.class, l);
    }

    public void removeInstructionTableListener(Listener l) {
        listenerList.remove(Listener.class, l);
    }

    public Component prepareEditor(TableCellEditor editor, int row, int column) {
        EventListener[] listeners = listenerList.getListeners(Listener.class);
        for (int i = 0; i < listeners.length; i++) {
            Listener l = (Listener) listeners[i];
            if (column == 0)
                l.toggleEnabled(row);
            else
                l.edit(row);
        }
        return null;
    }

}
