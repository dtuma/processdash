
package teamdash.wbs;

/** Interface for a column in a DataTableModel
 */
public interface DataColumn {

    /** Get the ID of this column. */
    public String getColumnID();

    /** Get the name that should be displayed for this column */
    public String getColumnName();

    /** Get the type of data displayed by this column. */
    public Class getColumnClass();

    /** Return true if this column is editable for the given WBSNode */
    public boolean isCellEditable(WBSNode node);

    /** Get the value of this column for the given WBSNode */
    public Object getValueAt(WBSNode node);

    /** Set a new value of this column for the given WBSNode */
    public void setValueAt(Object aValue, WBSNode node);

    /** Return the preferred width of this column, or -1 if this column
     * has no preferred width. */
    public int getPreferredWidth();

}
