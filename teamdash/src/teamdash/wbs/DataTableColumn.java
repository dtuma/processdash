package teamdash.wbs;

import javax.swing.table.TableColumn;

public class DataTableColumn extends TableColumn {

    public DataTableColumn(DataTableModel model, String columnID) {
        super();

        // locate the named column
        int columnIndex = model.findColumn(columnID);
        if (columnIndex == -1)
            throw new IllegalArgumentException(
                "No column with ID " + columnID);

        init(model, model.getColumn(columnIndex), columnIndex);
    }

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

    private void init(DataTableModel model, DataColumn c, int columnIndex) {
        setModelIndex(columnIndex);
        setHeaderValue(c.getColumnName());
        setIdentifier(c.getColumnID());
        int width = c.getPreferredWidth();
        if (width > 0)
            setPreferredWidth(width);
    }

}
