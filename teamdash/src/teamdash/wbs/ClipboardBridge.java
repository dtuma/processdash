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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JTable;
import javax.swing.KeyStroke;

import net.sourceforge.processdash.ui.macosx.MacGUIUtils;

/** Copies and pastes data from JTables to the system clipboard.
 */
public class ClipboardBridge {

    private static final int KEY_MODIFIER = (MacGUIUtils.isMacOSX()
            ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK);

    private JTable table;
    private Action copyAction;
    private Action pasteAction;

    public ClipboardBridge(JTable table) {
        this.table = table;
        this.copyAction = new CopyAction();
        this.pasteAction = new PasteAction();

        setupAction(KeyEvent.VK_C, "CopyToClipboard", this.copyAction);
        setupAction(KeyEvent.VK_V, "PasteFromClipboard", this.pasteAction);
    }

    private void setupAction(int keyChar, String token, Action action) {
        KeyStroke keyStroke = KeyStroke.getKeyStroke(keyChar,
                KEY_MODIFIER, false);
        table.getInputMap().put(keyStroke, token);
        table.getActionMap().put(token, action);
    }

    public JTable getJTable() {
        return table;
    }

    public void setJTable(JTable table) {
        this.table = table;
    }

    public Action getCopyAction() {
        return copyAction;
    }

    public Action getPasteAction() {
        return pasteAction;
    }

    private class CopyAction extends AbstractAction {

        public void actionPerformed(ActionEvent e) {
            // Ensure the selection represents a contiguous block of cells
            int numCols = table.getSelectedColumnCount();
            int numRows = table.getSelectedRowCount();
            int[] selRows = table.getSelectedRows();
            int[] selCols = table.getSelectedColumns();
            if (!isContiguous(numRows, selRows)
                    || !isContiguous(numCols, selCols)) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }

            StringBuffer buf = new StringBuffer();
            for (int row = 0; row < numRows; row++) {
                for (int col = 0; col < numCols; col++) {
                    if (col > 0)
                        buf.append("\t");
                    Object val = table.getValueAt(selRows[row], selCols[col]);
                    while (val instanceof WrappedValue || val instanceof Annotated) {
                        if (val instanceof Annotated)
                            val = ((Annotated) val).getAnnotation();
                        else
                            val = ((WrappedValue) val).value;
                    }
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

        private boolean isContiguous(int num, int[] sel) {
            return (num == sel.length)
                    && ((sel[sel.length - 1] - sel[0]) == (num - 1));
        }

    }

    private class PasteAction extends AbstractAction {

        public void actionPerformed(ActionEvent e) {
            try {
                boolean madeChange = false;

                int[] selRows = table.getSelectedRows();
                int[] selCols = table.getSelectedColumns();

                Clipboard clipboard = Toolkit.getDefaultToolkit()
                        .getSystemClipboard();
                String trstring = (String) (clipboard.getContents(this)
                        .getTransferData(DataFlavor.stringFlavor));
                String[][] cells = splitString(trstring);

                if (cells.length == 1 && cells[0].length == 1
                        && selCols.length == 1) {
                    // if there is only one piece of text on the clipboard,
                    // but several cells in a column are selected, try pasting
                    // the value into all of the selected cells.
                    String val = cells[0][0];
                    for (int r = 0; r < selRows.length; r++)
                        for (int c = 0; c < selCols.length; c++)
                            if (table.isCellEditable(selRows[r], selCols[c])) {
                                table.setValueAt(val, selRows[r], selCols[c]);
                                madeChange = true;
                            }

                } else {
                    // regular behavior: paste the text into the rectangular
                    // area starting with the top left selected cell
                    int startRow = selRows[0];
                    int startCol = selCols[0];
                    int endRow = startRow;
                    int endCol = startCol;
                    for (int r = 0; r < cells.length; r++) {
                        int row = startRow + r;
                        if (row >= table.getRowCount())
                            break;

                        for (int c = 0; c < cells[r].length; c++) {
                            int col = startCol + c;
                            if (col >= table.getColumnCount())
                                break;
                            if (table.isCellEditable(row, col)) {
                                table.setValueAt(cells[r][c], row, col);
                                madeChange = true;
                            }
                            endRow = row;
                            endCol = col;
                        }
                    }
                    table.getSelectionModel().setSelectionInterval(startRow,
                            endRow);
                    table.getColumnModel().getSelectionModel()
                            .setSelectionInterval(startCol, endCol);
                }

                if (madeChange)
                    UndoList.madeChange(table, "Paste");
            } catch (Exception ex) {
                Toolkit.getDefaultToolkit().beep();
                ex.printStackTrace();
            }

        }

        private String[][] splitString(String text) {
            String[] lines = text.split("\n");
            String[][] result = new String[lines.length][];
            for (int i = 0; i < lines.length; i++)
                result[i] = lines[i].split("\t");

            return result;
        }

    }
}
