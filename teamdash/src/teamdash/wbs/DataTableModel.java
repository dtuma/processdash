
package teamdash.wbs;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.Timer;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;

import teamdash.TeamMemberList;
import teamdash.TeamProcess;
import teamdash.wbs.columns.NullDataColumn;
import teamdash.wbs.columns.PhaseColumn;
import teamdash.wbs.columns.SizeTypeColumn;
import teamdash.wbs.columns.TaskSizeColumn;
import teamdash.wbs.columns.TaskSizeUnitsColumn;
import teamdash.wbs.columns.TeamMemberColumnManager;
import teamdash.wbs.columns.TeamTimeColumn;
import teamdash.wbs.columns.TopDownBottomUpColumn;


public class DataTableModel extends AbstractTableModel {

    /** The wbs model which this is displaying data for */
    protected WBSModel wbsModel;
    /** The team process in use
    protected TeamProcess teamProcess;
    /** The list of columns in this data model */
    private ArrayList columns;
    /** A list of the calculated columns in the model */
    private Set calculatedColumns;
    /** A square matrix of dependencies. The value <tt>true</tt> in cell
     * [x][y] means that column x depends upon column y. */
    private boolean[][] dependencies;
    /** A set of columns that need recalculating */
    private Set dirtyColumns;
    /** A timer for triggering recalculations */
    private Timer recalcJanitorTimer;
    /** Object which manages columns for team members */
    private TeamMemberColumnManager memberColumnManager;


    public DataTableModel(WBSModel wbsModel, TeamMemberList teamList,
                          TeamProcess teamProcess)
    {
        this.wbsModel = wbsModel;
        wbsModel.addTableModelListener(new TableModelEventRepeater());

        columns = new ArrayList();
        dirtyColumns = new HashSet();

        recalcJanitorTimer = new Timer(1000, new RecalcJanitor());
        recalcJanitorTimer.setRepeats(false);
        recalcJanitorTimer.setInitialDelay(3000);

        buildDataColumns(teamList, teamProcess);
        initializeColumnDependencies();
    }

    private void initializeColumnDependencies() {
        // create the dependency matrix and populate it with "false" values.
        int numColumns = columns.size();
        dependencies = new boolean[numColumns][numColumns];
        for (int x = 0;   x < numColumns;   x++)
            for (int y = 0;   y < numColumns;   y++)
                dependencies[x][y] = false;

        // find all the calculated columns in the model.
        calculatedColumns = new HashSet();
        Iterator i = columns.iterator();
        while (i.hasNext()) {
            Object column = i.next();
            if (column instanceof CalculatedDataColumn) {
                calculatedColumns.add(column);
                ((CalculatedDataColumn) column).resetDependentColumns();
            }
        }

        // initialize each calculated column.
        i = calculatedColumns.iterator();
        while (i.hasNext()) {
            // get information about the calculated column
            CalculatedDataColumn column = (CalculatedDataColumn) i.next();
            int columnPos = columns.indexOf(column);

            // find each dependent column and register it with the calculated
            // column.
            String dependentID;
            String[] dependsOn = column.getDependentColumnIDs();
            if (dependsOn != null)
                for (int j = 0;   j < dependsOn.length;   j++) {
                    dependentID = dependsOn[j];
                    int dependentPos = findColumn(dependentID);

                    if (dependentPos != -1)
                        dependencies[columnPos][dependentPos] = true;

                    column.storeDependentColumn(dependentID, dependentPos);
                }

            // find each affected column and register this column with it.
            String[] affects = column.getAffectedColumnIDs();
            if (affects != null) {
                dependentID = column.getColumnID();
                for (int j = 0;   j < affects.length;   j++) {
                    String affectedID = affects[j];
                    int affectedPos = findColumn(affectedID);
                    if (affectedPos == -1) continue;

                    DataColumn affectedColumn = getColumn(affectedPos);
                    if (affectedColumn instanceof CalculatedDataColumn) {
                        ((CalculatedDataColumn) affectedColumn)
                            .storeDependentColumn(dependentID, columnPos);
                        dependencies[affectedPos][columnPos] = true;
                    }
                }
            }
        }

        // recalculate all calculated columns.
        try {
            beginChange();
            dirtyColumns.addAll(calculatedColumns);
        } finally {
            endChange();
        }
    }

    /** Add a single data column to the data model */
    public void addDataColumn(DataColumn column) {
        columns.add(column);
        // if the dependencies are already computed, update them.
        if (dependencies != null)
            initializeColumnDependencies();
    }

    /** Remove a single data column from the data model */
    public void removeDataColumn(DataColumn column) {
        int pos = columns.indexOf(column);
        if (pos == -1) return;
        columns.set(pos, new NullDataColumn());
        // if the dependencies are already computed, update them.
        if (dependencies != null)
            initializeColumnDependencies();
    }

    /** Add a list of data columns and remove another list of data columns.
     * 
     * The changes are made as part of a batch operation, deferring
     * recalculations until all the changes are made.  As a result, this will
     * be much more efficient than a corresponding series of calls to
     * {@link #addDataColumn(DataColumn) addDataColumn} and
     * {@link #removeDataColumn(DataColumn) removeDataColumn}
     * @param columnsToAdd a list of columns to be added. <code>null</code> is
     * allowed.
     * @param columnsToRemove a list of columns to be removed. <code>null</code>
     * is allowed.
     */
    public void addRemoveDataColumns(List columnsToAdd, List columnsToRemove) {
        try {
            beginChange();
            dependencies = null;

            Iterator i;
            if (columnsToRemove != null) {
                i = columnsToRemove.iterator();
                while (i.hasNext())
                    removeDataColumn((DataColumn) i.next());
            }

            if (columnsToAdd != null) {
                i = columnsToAdd.iterator();
                while (i.hasNext())
                    addDataColumn((DataColumn) i.next());
            }

            initializeColumnDependencies();

        } finally {
            endChange();
        }
    }


    public void addTeamMemberTimes(TableColumnModel columnModel) {
        memberColumnManager.addToColumnModel(columnModel);
    }

    protected void buildDataColumns(TeamMemberList teamList,
                                    TeamProcess teamProcess)
    {

        SizeTypeColumn.createSizeColumns(this);
        addDataColumn(new PhaseColumn());
        addDataColumn(new TaskSizeColumn(this));
        addDataColumn(new TaskSizeUnitsColumn(this, teamProcess));
        addDataColumn(new TeamTimeColumn(this));
        memberColumnManager = new TeamMemberColumnManager(this, teamList);

        for (char c = 'B';   c <= 'Z';   c++)
            addDataColumn(new TopDownBottomUpColumn
                          (this, String.valueOf(c), String.valueOf(c)));
    }

    public WBSModel getWBSModel() { return wbsModel; }

    private class TableModelEventRepeater implements TableModelListener {
        public void tableChanged(TableModelEvent e) {
            // rebroadcast the event as our own.
            TableModelEvent newEvent = new TableModelEvent
                (DataTableModel.this, e.getFirstRow(),
                 e.getLastRow(), e.getColumn(), e.getType());
            fireTableChanged(newEvent);

            // all calculated column implicitly depend upon the structure
            // of the WBS model, so we'll mark all of our calculated columns
            // as dirty (this will schedule a deferred recalculation operation)
            try {
                beginChange();
                dirtyColumns.addAll(calculatedColumns);
            } finally {
                endChange();
            }
        }
    }

    // implementation of javax.swing.table.TableModel interface

    public int getRowCount() { return wbsModel.getRowCount(); }
    public int getColumnCount() { return columns.size(); }

    protected DataColumn getColumn(int columnIndex) {
        return (DataColumn) columns.get(columnIndex);
    }

    public String getColumnName(int columnIndex) {
        return getColumn(columnIndex).getColumnName();
    }

    public Class getColumnClass(int columnIndex) {
        return getColumn(columnIndex).getColumnClass();
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return isCellEditable(wbsModel.getNodeForRow(rowIndex), columnIndex);
    }
    public boolean isCellEditable(WBSNode node, int columnIndex) {
        DataColumn column = getColumn(columnIndex);
        if (node == null || column == null) return false;

        return column.isCellEditable(node);
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return getValueAt(wbsModel.getNodeForRow(rowIndex), columnIndex);
    }

    public Object getValueAt(WBSNode node, int columnIndex) {
        DataColumn column = getColumn(columnIndex);
        if (node == null || column == null) return null;

        return column.getValueAt(node);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        setValueAt(aValue, wbsModel.getNodeForRow(rowIndex), columnIndex);
    }

    public void setValueAt(Object aValue, WBSNode node, int columnIndex) {
        DataColumn column = getColumn(columnIndex);
        if (node == null || column == null) return;

        try {
            beginChange();
            columnChanged(column, columnIndex);
            column.setValueAt(aValue, node);
        } finally {
            endChange();
        }
    }

    /** overridden to search first by column id, then by column name */
    public int findColumn(String columnName) {
        for (int col = getColumnCount();   col-- > 0; )
            if (columnName.equals(getColumn(col).getColumnID()))
                return col;

        return super.findColumn(columnName);
    }


    public int getPreferredWidth(int columnIndex) {
        if (columnIndex < 0 || columnIndex >= columns.size()) return -1;
        return getColumn(columnIndex).getPreferredWidth();
    }

    /** Let the model know that data in a particular column has been changed.
     * 
     * The column (and all columns that depend upon it) will be marked as
     * needing recalculation.
     * 
     * This will be called automatically by setValueAt(), so normally columns
     * will not need to call this. Calling this method is only necessary when
     * the data in a column has been changed by some external event. */
    public void columnChanged(DataColumn column) {
        if (column != null) {
            try {
                beginChange();
                columnChanged(column, columns.indexOf(column));
            } finally {
                endChange();
            }
        }
    }
    protected void columnChanged(DataColumn column, int columnPos) {
        synchronized (dirtyColumns) {
            if (columnPos == -1 || dirtyColumns.contains(column)) return;

            // if the column is calculated, add it to the dirty list.
            if (column instanceof CalculatedDataColumn)
                dirtyColumns.add(column);

            // find any columns that depend upon this column, and add them
            // to the dirty list as well.
            for (int j = columns.size();   j-- > 0; )
                if (dependencies[j][columnPos])
                    columnChanged(getColumn(j), j);
        }
    }

    //////////////////////////////////////////////////////////////
    //  recalc support
    /////////////////////////////////////////////////////////////

    /** how many changes are currently underway? */
    private int changeDepth = 0;

    /** call this method before you make a change which could affect the
     * list of dirty columns */
    private void beginChange() {
        synchronized (dirtyColumns) {
            changeDepth++;
        }
        recalcJanitorTimer.restart();
    }

    /** call this method after you finish making a change which could
     * affect the list of dirty columns */
    private void endChange() {
        int finalDepth;
        synchronized (dirtyColumns) {
            finalDepth = --changeDepth;
            if (changeDepth < 0) changeDepth = 0;
        }
        if (finalDepth == 0) recalcColumns();
    }

    /** This class cleans up after people who neglect to call endChange() */
    private final class RecalcJanitor implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            System.out.println("Someone didn't call endChange - cleaning up");
            synchronized (dirtyColumns) {
                changeDepth = 0;
            }
            recalcColumns();
        }
    }


    private void recalcColumns() {
        recalcJanitorTimer.stop();
        synchronized (dirtyColumns) {
            if (dirtyColumns.isEmpty()) return;

            HashSet waitingColumns = new HashSet();

            try {
                beginChange();
                while (!dirtyColumns.isEmpty()) {
                    waitingColumns.clear();
                    CalculatedDataColumn c =
                        (CalculatedDataColumn) dirtyColumns.iterator().next();
                    recalcColumn(c, waitingColumns);
                }
            } catch (Exception e) {
                e.printStackTrace();
                dirtyColumns.clear();
            } finally {
                endChange();
            }
        }
    }

    private void recalcColumn(CalculatedDataColumn column,
                              Set waitingColumns)
    {
        if (dirtyColumns.contains(column) == false) return;

        if (waitingColumns.contains(column)) {
            System.out.println("Circular column dependency:"+waitingColumns);
            return;
        }

        int columnPos = columns.indexOf(column);
        if (columnPos != -1) try {
            waitingColumns.add(column);

            // find all columns which this column depends upon, and be sure
            // to calculate them first.
            for (int j = columns.size();   j-- > 0; )
                if (dependencies[columnPos][j]) {
                    DataColumn dependentColumn = getColumn(j);
                    if (dependentColumn instanceof CalculatedDataColumn)
                        recalcColumn((CalculatedDataColumn) dependentColumn,
                                     waitingColumns);
                }

        } finally {
            waitingColumns.remove(column);
        }

        // recalculate the column
        if (column.recalculate()) {
            // if data changed, fire an appropriate table model event.
            TableModelEvent e = new TableModelEvent
                (this, 0, getRowCount()-1, columnPos, TableModelEvent.UPDATE);
            fireTableChanged(e);
        }

        // remove this column from the "dirty" list.
        dirtyColumns.remove(column);
    }

}
