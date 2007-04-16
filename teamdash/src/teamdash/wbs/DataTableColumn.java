package teamdash.wbs;

import javax.swing.table.TableColumn;


/** Encapsulates the logic necessary to create a TableColumn object for a
 * DataColumn
 */
public class DataTableColumn extends TableColumn {

    /** Create a DataTableColumn if we know the String ID of the column */
    public DataTableColumn(DataTableModel model, String columnID) {
        super();

        // locate the named column
        int columnIndex = model.findColumn(columnID);
        if (columnIndex == -1)
            throw new IllegalArgumentException(
                "No column with ID " + columnID);

        init(model, model.getColumn(columnIndex), columnIndex);
    }

    /** Create a DataTableColumn for a given DataColumn */
    public DataTableColumn(DataTableModel model, DataColumn c) {
        super();

        // locate the named column
        int columnIndex = model.findColumn(c.getColumnID());
        if (columnIndex == -1)
            columnIndex = model.findColumn(c.getColumnName());
        if (columnIndex == -1)
            throw new IllegalArgumentException(
                "No column with ID " + c.getColumnID());

        init(model, c, columnIndex);
    }

    /** 
     * Create a copy of an existing DataTableColumn
     * @param orig
     */
    public DataTableColumn(DataTableColumn orig) {
        setModelIndex(orig.getModelIndex());
        setHeaderValue(orig.getHeaderValue());
        setIdentifier(orig.getIdentifier());
        setPreferredWidth(orig.getPreferredWidth ());
        setCellRenderer(orig.getCellRenderer());
        setCellEditor(orig.getCellEditor());
    }


    private void init(DataTableModel model, DataColumn c, int columnIndex) {
        // set the index, header value, and identifier.
        setModelIndex(columnIndex);
        setHeaderValue(c.getColumnName());
        setIdentifier(c.getColumnID());

        // install the column's preferred width
        int width = c.getPreferredWidth();
        if (width > 0)
            setPreferredWidth(width);

        if (c instanceof CustomRenderedColumn)
            // install the column's preferred renderer.
            setCellRenderer(((CustomRenderedColumn) c).getCellRenderer());

        if (c instanceof CustomEditedColumn)
            // install the column's preferred editor.
            setCellEditor(((CustomEditedColumn) c).getCellEditor());
    }

    public String toString() {
        return getHeaderValue().toString();
    }
}
