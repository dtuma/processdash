package teamdash.wbs;


public interface CalculatedDataColumn extends DataColumn {

    /** Recalculate the data in this column.
     * @return true if data values changed. */
    public boolean recalculate();

    /** Get the IDs of columns upon which this column depends */
    public String[] getDependentColumnIDs();

    /** Get the IDs of columns which this column knows it affects */
    public String[] getAffectedColumnIDs();

    /** The DataTableModel will call this method once for each dependent
     * column */
    public void storeDependentColumn(String ID, int columnNumber);

}
