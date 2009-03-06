package teamdash.wbs.columns;

import teamdash.wbs.IndexAwareDataColumn;

/** Abstract implementation of DataColumn interface.
 */
public abstract class AbstractDataColumn implements IndexAwareDataColumn {

    /** The value of this field will be returned as the column ID */
    protected String columnID;

    /** The value of this field will be returned as the column name */
    protected String columnName;

    /** The index of this column in the data model */
    private int dataColumnIndex = -1;

    /** For {@link teamdash.wbs.CalculatedDataColumn CalculatedDataColumn}
     * objects, the value of this field will be returned as the list
     * of dependent columns */
    protected String[] dependentColumns = null;

    /** The value of this field will be returned as the preferred
     * column width */
    protected int preferredWidth = -1;

    public String getColumnID()   { return columnID;     }
    public String getColumnName() { return columnName;   }
    public Class getColumnClass() { return String.class; }
    public int getColumnIndex()   { return dataColumnIndex; }
    public void setColumnIndex(int index) { dataColumnIndex = index; }
    public String[] getDependentColumnIDs() { return dependentColumns; }
    public String[] getAffectedColumnIDs() { return null; }
    public void resetDependentColumns() {}
    public int getPreferredWidth() { return preferredWidth; }

}
