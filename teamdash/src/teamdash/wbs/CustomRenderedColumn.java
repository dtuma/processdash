package teamdash.wbs;

import javax.swing.table.TableCellRenderer;

/** This interface allows DataColumns to specify a nonstandard cell renderer.
 */
public interface CustomRenderedColumn extends DataColumn {
    public TableCellRenderer getCellRenderer();
}
