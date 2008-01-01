package teamdash.wbs;

import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumnModel;
import javax.swing.text.JTextComponent;

/** Table to display data for a work breakdown structure.
 */
public class DataJTable extends JTable {

    /** True if getValueAt() should unwrap values before returning them */
    boolean unwrapQueriedValues = false;

    /** When an editing session is in progress, this remembers the value of
     * the cell editor when editing began */
    private Object valueBeforeEditing;


    public DataJTable(DataTableModel model) {
        super(model);

        setDefaultEditor  (Object.class, new DefaultCellEditor(new JTextField()));
        setDefaultRenderer(Object.class, new DataTableCellRenderer());

        setDefaultRenderer(NumericDataValue.class,
                           new DataTableCellNumericRenderer());

        new ClipboardBridge(this);
        addFocusListener(new FocusWatcher());

        // work around Sun Java bug 4709394
        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    }

    private void selectAllColumns() {
        getColumnModel().getSelectionModel().addSelectionInterval(0,
                getColumnCount() - 1);
    }

    public void setColumnModel(TableColumnModel columnModel) {
        super.setColumnModel(columnModel);

        setCellSelectionEnabled(true);
        selectAllColumns();
    }

    @Override
    public Object getValueAt(int row, int column) {
        Object result = super.getValueAt(row, column);
        if (unwrapQueriedValues)
            result = ErrorValue.unwrap(result);
        return result;
    }

    @Override
    public Component prepareEditor(TableCellEditor editor, int row, int column) {
        unwrapQueriedValues = true;
        Component result = super.prepareEditor(editor, row, column);
        unwrapQueriedValues = false;

        if (result != null) {
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

    /** Compare two (possibly null) values for equality */
    private boolean equal(Object a, Object b) {
        if (a == b) return true;
        return (a != null && a.equals(b));
    }

    private class FocusWatcher extends FocusAdapter {

        public void focusLost(FocusEvent e) {
            if (e.getComponent() != DataJTable.this)
                return;
            if (SwingUtilities.isDescendingFrom(e.getOppositeComponent(),
                DataJTable.this))
                return;
            selectAllColumns();
        }

    }

}
