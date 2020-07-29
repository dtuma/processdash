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

package teamdash.wbs;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.text.JTextComponent;

import net.sourceforge.processdash.ui.macosx.MacGUIUtils;

import teamdash.ActionCategoryComparator;
import teamdash.hist.BlameData;
import teamdash.hist.BlameModelData;
import teamdash.hist.BlameModelDataListener;
import teamdash.hist.BlameNodeData;
import teamdash.hist.ui.BlameAnnotationBorder;
import teamdash.hist.ui.BlameComponentRepainter;
import teamdash.hist.ui.BlameSelectionListener;

/** Table to display data for a work breakdown structure.
 */
public class DataJTable extends JTable {

    /** Copy and Paste Actions' categories */
    public static final String DATA_ACTION_CATEGORY =
        ActionCategoryComparator.ACTION_CATEGORY;
    public static final String DATA_ACTION_CATEGORY_CLIPBOARD = "dataClipboard";

    /** A collection of blame data for drawing annotations */
    BlameModelData blameData;

    /** An object for updating the blame caret position */
    BlameSelectionListener blameSelectionListener;

    /** True if getValueAt() should unwrap values before returning them */
    boolean unwrapQueriedValues = false;

    /** When an editing session is in progress, this remembers the value of
     * the cell editor when editing began */
    private Object valueBeforeEditing;

    /** Allows data to be copied and pasted from this DataJTable to the system clipboard */
    private Action copyAction;
    private Action pasteAction;

    public DataJTable(DataTableModel model, int rowHeight) {
        super(model);

        setTableHeader(new DataTableHeader());

        setDefaultEditor  (Object.class, new DefaultCellEditor(new JTextField()));
        setDefaultRenderer(Object.class, new DataTableCellRenderer());

        setDefaultRenderer(NumericDataValue.class,
                           new DataTableCellNumericRenderer());
        setForeground(Color.black);
        setBackground(Color.white);
        setSelectionForeground(Color.black);
        setSelectionBackground(new Color(0xb8cfe5));

        setRowHeight(rowHeight);
        WBSZoom.get().manage(this, "font", "rowHeight");
        WBSZoom.get().manage(getTableHeader(), "font");

        ClipboardBridge clipboardBridge = new ClipboardBridge(this);

        copyAction = clipboardBridge.getCopyAction();
        copyAction.putValue(Action.NAME, WBSEditor.resources.getString("Edit.Data.Copy"));
        copyAction.putValue(DATA_ACTION_CATEGORY, DATA_ACTION_CATEGORY_CLIPBOARD);

        pasteAction = clipboardBridge.getPasteAction();
        pasteAction.putValue(Action.NAME, WBSEditor.resources.getString("Edit.Data.Paste"));
        pasteAction.putValue(DATA_ACTION_CATEGORY, DATA_ACTION_CATEGORY_CLIPBOARD);

        addFocusListener(new FocusWatcher());

        // work around Sun Java bug 4709394
        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        MacGUIUtils.tweakTable(this);
    }

    private void selectAllColumns() {
        getColumnModel().getSelectionModel().addSelectionInterval(0,
                getColumnCount() - 1);
    }

    public void selectColumn(int col) {
        timeOfLastColumnSelection = System.currentTimeMillis();
        getColumnModel().getSelectionModel().clearSelection();
        addColumnSelectionInterval(col, col);
    }
    long timeOfLastColumnSelection = -1;

    public void setColumnModel(TableColumnModel columnModel) {
        if (blameSelectionListener != null)
            blameSelectionListener.columnModelChanging();

        super.setColumnModel(columnModel);

        setCellSelectionEnabled(true);
        selectAllColumns();

        if (blameSelectionListener != null)
            blameSelectionListener.columnModelChanged();
    }

    public Action[] getEditingActions() {
        return new Action[] { this.copyAction,
                              this.pasteAction };
    }

    public void setEditingEnabled(boolean enabled) {
        ((DataTableModel) getModel()).setEditingEnabled(enabled);
        pasteAction.setEnabled(enabled);
    }

    @Override
    public Object getValueAt(int row, int column) {
        Object result = super.getValueAt(row, column);
        if (unwrapQueriedValues)
            result = ErrorValue.unwrap(result);
        return result;
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        Component editor = getEditorComponent();
        if (editor != null)
            editor.setFont(font);
    }

    @Override
    public Component prepareEditor(TableCellEditor editor, int row, int column) {
        unwrapQueriedValues = true;
        Component result = super.prepareEditor(editor, row, column);
        unwrapQueriedValues = false;

        if (result != null) {
            // set the font to match the table
            result.setFont(DataJTable.this.getFont());
            // save the value we are editing for undo purposes
            valueBeforeEditing = editor.getCellEditorValue();
            // register ourselves with the UndoList
            UndoList.addCellEditor(this, editor);
        }

        // select all the text in the component (users are used to this)
        if (result instanceof JTextComponent)
            ((JTextComponent) result).selectAll();

        return result;
    }

    @Override
    public void editingStopped(ChangeEvent e) {
        // check to see if the value in this cell actually changed.
        boolean valueChanged = false;
        String columnName = null;
        TableCellEditor editor = getCellEditor();
        if (editor != null) {
            Object valueAfterEditing = editor.getCellEditorValue();
            valueChanged = !equal(valueBeforeEditing, valueAfterEditing);
            columnName = getColumnName(getEditingColumn());
        }

        // stop the editing session
        super.editingStopped(e);

        // if the value was changed, notify the UndoList.
        if (valueChanged)
            UndoList.madeChange
                (DataJTable.this, "Editing value in '"+columnName+"' column");
    }


    public void setBlameData(BlameData blame) {
        BlameModelData blameData = BlameData.getModel(blame,
            ((DataTableModel) getModel()).getWBSModel().getModelType());
        if (this.blameData != blameData) {
            if (this.blameData != null)
                this.blameData.removeBlameModelDataListener(BLAME_LISTENER);
            if (this.blameSelectionListener != null) {
                this.blameSelectionListener.dispose();
                this.blameSelectionListener = null;
            }

            this.blameData = blameData;

            if (this.blameData != null) {
                this.blameData.addBlameModelDataListener(BLAME_LISTENER);
                this.blameSelectionListener = new BlameSelectionListener(this,
                        blame);
            }

            repaint();
        }
    }

    private BlameNodeData getBlameDataForRow(int row) {
        if (blameData == null)
            return null;

        WBSModel wbsModel = ((DataTableModel) getModel()).getWBSModel();
        WBSNode node = wbsModel.getNodeForRow(row);
        if (node == null)
            return null;
        else
            return blameData.get(node.getTreeNodeID());
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
        TableCellRenderer result = super.getCellRenderer(row, column);
        if (result instanceof Component)
            ((Component) result).setBackground(null);
        return result;
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row,
            int column) {
        Component result = super.prepareRenderer(renderer, row, column);

        BlameNodeData blame = getBlameDataForRow(row);
        if (blame != null) {
            DataTableModel dataModel = (DataTableModel) getModel();
            column = convertColumnIndexToModel(column);
            String columnID = dataModel.getColumn(column).getColumnID();
            if (blame.isColumnAffected(columnID))
                BlameAnnotationBorder.annotate(result);
        }

        return result;
    }

    public void setMinHeaderHeight(int h) {
        ((DataTableHeader) getTableHeader()).minHeight = h;
    }

    private final BlameModelDataListener BLAME_LISTENER = //
            new BlameComponentRepainter(this);

    
    /** Compare two (possibly null) values for equality */
    private boolean equal(Object a, Object b) {
        if (a == b) return true;
        return (a != null && a.equals(b));
    }

    private class FocusWatcher extends FocusAdapter {

        public void focusLost(FocusEvent e) {
            if (e.isTemporary() || e.getComponent() != DataJTable.this)
                return;
            if (System.currentTimeMillis() - timeOfLastColumnSelection < 300)
                return;
            Component opposite = e.getOppositeComponent();
            if (opposite == null
                    || opposite instanceof JButton
                    || SwingUtilities.isDescendingFrom(opposite,
                        DataJTable.this))
                return;
            selectAllColumns();
        }

    }

    private class DataTableHeader extends JTableHeader {

        int minHeight;

        @Override
        public Dimension getPreferredSize() {
            Dimension result = super.getPreferredSize();
            result.height = Math.max(result.height, minHeight);
            return result;
        }

    }

}
