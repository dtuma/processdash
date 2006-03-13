package teamdash.wbs;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JTable;
import javax.swing.table.TableColumnModel;

/** Table to display data for a work breakdown structure.
 */
public class DataJTable extends JTable {

    public DataJTable(DataTableModel model) {
        super(model);

        setDefaultEditor  (Object.class, new DataTableCellEditor());
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

    private class FocusWatcher extends FocusAdapter {

        public void focusLost(FocusEvent e) {
            if (e.getComponent() == DataJTable.this)
                selectAllColumns();
        }

    }

}
