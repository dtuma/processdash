package teamdash.wbs;

import javax.swing.JTable;

/** Table to display WBS data.
 */
public class DataJTable extends JTable {

        public DataJTable(DataTableModel model) {
                super(model);
                setDefaultRenderer(Object.class, new DataTableCellRenderer());
        setDefaultEditor  (Object.class, new DataTableCellEditor());
        }

}
