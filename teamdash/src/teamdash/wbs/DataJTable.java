package teamdash.wbs;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;

import teamdash.wbs.columns.SizeTypeColumn;
import teamdash.wbs.columns.TaskSizeUnitsColumn;

/** Table to display WBS data.
 */
public class DataJTable extends JTable {

    public DataJTable(DataTableModel model) {
        super(model);
        setDefaultRenderer(Object.class, new DataTableCellRenderer());
        setDefaultRenderer(NumericDataValue.class,
                           new DataTableCellNumericRenderer());
        setDefaultRenderer(TeamMemberTime.class,
                           new TeamMemberTimeCellRenderer());
        setDefaultEditor  (Object.class, new DataTableCellEditor());

        JComboBox sizeUnits = new JComboBox(SizeTypeColumn.SIZE_UNITS);
        sizeUnits.setEditable(true);
        setDefaultEditor(TaskSizeUnitsColumn.class,
                         new DefaultCellEditor(sizeUnits));

        // work around Sun Java bug 4709394
        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    }

}
