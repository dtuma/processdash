package teamdash.wbs;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JTable;
import javax.swing.KeyStroke;

/** Copies and pastes data from JTables to the system clipboard.
 */
public class ClipboardBridge {

    private JTable table;

    public ClipboardBridge(JTable table) {
        this.table = table;
        setupAction(KeyEvent.VK_C, "CopyToClipboard", new CopyAction());
        setupAction(KeyEvent.VK_V, "PasteFromClipboard", new PasteAction());
    }

    private void setupAction(int keyChar, String token, Action action) {
        KeyStroke keyStroke = KeyStroke.getKeyStroke(keyChar,
                ActionEvent.CTRL_MASK, false);
        table.getInputMap().put(keyStroke, token);
        table.getActionMap().put(token, action);
    }

    public JTable getJTable() {
        return table;
    }

    public void setJTable(JTable table) {
        this.table = table;
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
