package teamdash.wbs.columns;

import teamdash.wbs.DataColumn;

public abstract class AbstractDataColumn implements DataColumn {

    protected String columnID;
    protected String columnName;
    protected String[] dependentColumns = null;

    public String getColumnID()   { return columnID;     }
    public String getColumnName() { return columnName;   }
    public Class getColumnClass() { return String.class; }
    public String[] getDependentColumnIDs() { return dependentColumns; }
    public String[] getAffectedColumnIDs() { return null; }
    public void resetDependentColumns() {}

}
