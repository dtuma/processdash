package teamdash.wbs;

import javax.swing.table.TableCellEditor;


/** This interface allows DataColumns to specify a nonstandard cell editor.
 */
public interface CustomEditedColumn {
    public TableCellEditor getCellEditor();
}
