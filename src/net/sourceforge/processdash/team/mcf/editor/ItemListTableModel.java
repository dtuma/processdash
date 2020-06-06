// Copyright (C) 2002-2020 Tuma Solutions, LLC
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

package net.sourceforge.processdash.team.mcf.editor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;

import net.sourceforge.processdash.team.mcf.CustomProcess;
import net.sourceforge.processdash.team.mcf.CustomProcess.Item;


public abstract class ItemListTableModel extends AbstractTableModel {

    private CustomProcess process;
    private String itemType;
    private List itemList;
    private Set insertedItems;
    private boolean isDirty = false;
    private boolean structureChanged = false;

    public ItemListTableModel(CustomProcess p, String itemType) {
        this.itemType = itemType;
        setProcess(p);
    }

    public void setProcess(CustomProcess p) {
        this.process = p;
        this.itemList = p.getItemList(itemType);
        this.insertedItems = new HashSet();
        isDirty = false;
        fireTableDataChanged();
    }

    public CustomProcess getProcess() {
        return process;
    }

    public abstract int insertItem(int pos);

    protected void markAllItemsInserted() {
        insertedItems.addAll(itemList);
    }

    protected int insertItem(Item newItem, int pos) {
        insertedItems.add(newItem);
        pos = Math.min(pos, itemList.size());
        itemList.add(pos, newItem);
        isDirty = true;
        fireTableRowsInserted(pos, pos);
        return pos;
    }

    public void deleteItem(int pos) {
        if (pos >= 0 && pos < itemList.size()) {
            Item deletedItem = get(pos);
            itemList.remove(pos);
            isDirty = true;
            if (!insertedItems.remove(deletedItem))
                structureChanged = true;
            fireTableRowsDeleted(pos, pos);
        }
    }

    public void moveItemUp(int pos) {
        if (pos > 0 && pos < itemList.size()) {
            Object temp = itemList.get(pos);
            itemList.set(pos, itemList.get(pos-1));
            itemList.set(pos-1, temp);
            isDirty = true;
            fireTableRowsUpdated(pos-1, pos);
        }
    }
    public Item get(int pos) {
        return (Item) itemList.get(pos);
    }
    public boolean isDirty() {
        return isDirty;
    }
    public void clearDirty() {
        this.isDirty = false;
        this.structureChanged = false;
    }
    public boolean isStructureChanged() {
        return structureChanged;
    }
    public abstract JTable createJTable();


    public void checkForErrors(Set errors) {

    }
    protected void checkForMissingField(Set errors, int col,
            Collection emptyNames, String errMsg) {
        for (int row = 0;  row < getRowCount();  row++) {
            Object val = getValueAt(row, col);
            if (emptyNames.contains(val)) {
                errors.add(errMsg);
                break;
            }
        }
    }

    protected void checkForDuplicateFields(Set errors, int[] cols,
            String errMsgFmt, Collection emptyNames) {
        Set valuesSeen = new HashSet();
        Set rowValues = new HashSet();

        for (int row = 0;  row < getRowCount();  row++) {
            rowValues.clear();
            for (int i = 0; i < cols.length; i++) {
                Object val = getValueAt(row, cols[i]);
                if (val == null || emptyNames.contains(val))
                    continue;
                rowValues.add(val);
                if (valuesSeen.contains(val)) {
                    String msg = MessageFormat.format(errMsgFmt,
                            new Object[] { val });
                    errors.add(msg);
                }
            }
            valuesSeen.addAll(rowValues);
        }
    }


    // TableModel interface methods

    protected abstract String[] getColumnAttrs();
    protected abstract boolean isStructuralColumn(int column);

    public int getRowCount() { return itemList.size(); }
    public int getRealRowCount() { return itemList.size(); }
    public int getColumnCount() { return getColumnAttrs().length; }
    public Class getColumnClass(int columnIndex) { return String.class; }
    public Object getValueAt(int row, int column) {
        Item phase = get(row);
        String attrName = getColumnAttrs()[column];
        return phase.getAttr(attrName);
    }
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return !get(rowIndex).getAttributes().containsKey(CustomProcess.READ_ONLY);
    }
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (!isCellEditable(rowIndex, columnIndex)) return;
        aValue = filterValidValue(aValue, rowIndex, columnIndex);
        if (aValue == INVALID_VALUE) return;

        Item item = get(rowIndex);

        String value = null;
        if (aValue != null) {
            value = CustomProcess.makeSafe(String.valueOf(aValue));
            if (value.indexOf('/') != -1) return;
        }

        if (isStructuralColumn(columnIndex) && !insertedItems.contains(item))
            structureChanged = true;

        String attrName = getColumnAttrs()[columnIndex];
        if (!eq(item.getAttr(attrName), value)) {
            item.putAttr(attrName, value);
            isDirty = true;
        }
    }
    private boolean eq(Object a, Object b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
    protected Object filterValidValue(Object aValue, int row, int col) {
        return aValue;
    }
    protected static final Object INVALID_VALUE = new Object();

    protected boolean isStructuralCell(int row, int col) {
        return (isStructuralColumn(col) && !insertedItems.contains(get(row)));
    }

    protected Object getItemDisplay(int column, Object value) {
        return value;
    }

    protected int[] pasteComparisonColumns = null;

    protected int getExistingRowForPastedData(String[] rowData) {
        if (pasteComparisonColumns == null)
            return -1;

        for (int row = 0;  row < getRowCount();  row++) {
            for (int j = 0; j < pasteComparisonColumns.length; j++) {
                int col = pasteComparisonColumns[j];
                if (eq(getValueAt(row, col), rowData[col]))
                    return row;
            }
        }

        return -1;
    }

    protected boolean rowIsDiscardable(int row) {
        return false;
    }





    protected class ItemTableCellRenderer extends DefaultTableCellRenderer {

        private Color structuralColor = new Color(255, 230, 200);
        private Color structuralWarning = new Color(255, 248, 200);

        public Component getTableCellRendererComponent
            (JTable table, Object value, boolean isSelected,
             boolean hasFocus, int row, int col)
        {
            setBackground(null);
            value = getItemDisplay(col, value);

            Component result = super.getTableCellRendererComponent
                (table, value, isSelected, hasFocus, row, col);

            String tooltip = null;
            if (table.getModel().isCellEditable(row, col)) {
                result.setForeground(Color.black);
                if (isStructuralColumn(col)) {
                    if (insertedItems.contains(get(row))) {
                        if (hasFocus || !isSelected)
                            result.setBackground(structuralWarning);
                        tooltip = "<html><body>"
                                + "Choose this value with thought and care.<br>"
                                + "After you begin collecting data using this<br>"
                                + "metrics collection framework, you will not<br>"
                                + "be able to alter this value.</body></html>";
                    } else {
                        if (hasFocus || !isSelected)
                            result.setBackground(structuralColor);
                        tooltip = "<html><body>"
                                + "Changes to this value will result in a new<br>"
                                + "metrics collection framework that is not<br>"
                                + "backward-compatible with existing project<br>"
                                + "data.</body></html>";
                    }
                }
            } else {
                result.setForeground(Color.gray);
                tooltip = "This item is read-only";
            }


            if (result instanceof JComponent)
                ((JComponent) result).setToolTipText(tooltip);

            return result;
        }
    }

    protected class ItemListJTable extends JTable {

        public ItemListJTable(TableModel dm) {
            super(dm);
            setSurrendersFocusOnKeystroke(true);

            setupAction(KeyEvent.VK_D, "Dupl_Cell", new DuplicateCellAction());
            setupAction(KeyEvent.VK_C, "CopyToClipboard", new CopyAction());
            setupAction(KeyEvent.VK_V, "PasteFromClipboard", new PasteAction());
        }

        private void setupAction(int keyChar, String token, Action action) {
            KeyStroke keyStroke = KeyStroke.getKeyStroke(keyChar,
                    ActionEvent.CTRL_MASK, false);
            getInputMap().put(keyStroke, token);
            getActionMap().put(token, action);
        }

        public Component prepareEditor(TableCellEditor editor, int row,
                int column) {
            Component result = super.prepareEditor(editor, row, column);
            if (result instanceof JTextField)
                ((JTextField) result).selectAll();
            return result;
        }

        private class DuplicateCellAction extends AbstractAction {

            public void actionPerformed(ActionEvent e) {
                int row = getSelectedRow();
                int col = getSelectedColumn();
                if (row < 1 || col < 0 || !isCellEditable(row, col)) {
                    Toolkit.getDefaultToolkit().beep();
                    return;
                }
                Object val = getValueAt(row-1, col);
                setValueAt(val, row, col);
                editCellAt(row, col);
            }

        }

        private class CopyAction extends AbstractAction {

            public void actionPerformed(ActionEvent e) {
                int numCols = getSelectedColumnCount();
                int numRows = getSelectedRowCount();
                if (numCols == 0 || numRows == 0) return;
                int[] selRows = getSelectedRows();
                int[] selCols = getSelectedColumns();

                StringBuffer buf = new StringBuffer();
                for (int row = 0; row < numRows; row++) {
                    for (int col = 0; col < numCols; col++) {
                        if (col > 0)
                            buf.append("\t");
                        Object val = getValueAt(selRows[row], selCols[col]);
                        if (val != null)
                            buf.append(val);
                    }
                    buf.append("\n");
                }

                Clipboard clipboard = Toolkit.getDefaultToolkit()
                        .getSystemClipboard();
                StringSelection selection = new StringSelection(buf.toString());
                clipboard.setContents(selection, selection);
            }

        }

        private class PasteAction extends AbstractAction {

            public void actionPerformed(ActionEvent e) {
                try {
                    int currRow = getSelectedRow();
                    int currCol = getSelectedColumn();
                    if (currRow == -1 || currCol == -1) return;

                    Clipboard clipboard = Toolkit.getDefaultToolkit()
                            .getSystemClipboard();
                    String trstring = (String) (clipboard.getContents(this)
                            .getTransferData(DataFlavor.stringFlavor));
                    String[][] cells = splitString(trstring);

                    if (cells.length == 1 && cells[0].length == 1) {
                        // there is only one cell on the clipboard.  Just paste
                        // the value into the active cell.
                        setValueAt(cells[0][0], currRow, currCol);
                        return;
                    }

                    int[] changedRows = new int[cells.length];
                    Arrays.fill(changedRows, -1);

                    for (int i = 0; i < cells.length; i++) {
                        // iterate over each pasted row of data.
                        String[] rowData = cells[i];
                        if (rowData == null) continue;

                        int destRow = getExistingRowForPastedData(rowData);
                        if (destRow == -1) {
                            if (rowIsDiscardable(currRow))
                                destRow = currRow;
                            else
                                destRow = insertItem(currRow);
                        }

                        int numCols = Math.min(rowData.length, getColumnCount());
                        for (int j = 0; j < numCols; j++)
                            setValueAt(rowData[j], destRow, j);

                        changedRows[i] = destRow;
                        currRow = Math.max(currRow, destRow + 1);
                    }

                    getSelectionModel().clearSelection();
                    for (int i = 0; i < changedRows.length; i++) {
                        int r = changedRows[i];
                        if (r != -1)
                            getSelectionModel().addSelectionInterval(r, r);
                    }

                } catch (Exception ex) {
                    Toolkit.getDefaultToolkit().beep();
                    ex.printStackTrace();
                }

            }

            private String[][] splitString(String text) {
                String[] lines = text.split("\n");
                String[][] result = new String[lines.length][];
                for (int i = 0; i < lines.length; i++) {
                    if (lines[i].trim().length() == 0)
                        result[i] = null;
                    else
                        result[i] = lines[i].split("\t");
                }
                return result;
            }

        }
    }
}
