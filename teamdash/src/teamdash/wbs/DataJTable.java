package teamdash.wbs;

import javax.swing.JTable;

/** Table to display data for a work breakdown structure.
 */
public class DataJTable extends JTable {

    public DataJTable(DataTableModel model) {
        super(model);

        setDefaultEditor  (Object.class, new DataTableCellEditor());
        setDefaultRenderer(Object.class, new DataTableCellRenderer());

        setDefaultRenderer(NumericDataValue.class,
                           new DataTableCellNumericRenderer());

        // work around Sun Java bug 4709394
        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    }

}
