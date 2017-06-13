// Copyright (C) 2002-2017 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 3
// of the License, or (at your option) any later version.
//
// Additional permissions also apply; see the README-license.txt
// file in the project root directory for more information.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net


package teamdash.wbs;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import teamdash.team.TeamMemberList;
import teamdash.wbs.columns.CustomColumnManager;
import teamdash.wbs.columns.CustomColumnSpecs;
import teamdash.wbs.columns.ErrorNotesColumn;
import teamdash.wbs.columns.LabelSource;
import teamdash.wbs.columns.MilestoneColumn;
import teamdash.wbs.columns.NotesColumn;
import teamdash.wbs.columns.NullDataColumn;
import teamdash.wbs.columns.PhaseColumn;
import teamdash.wbs.columns.PlanTimeWatcher;
import teamdash.wbs.columns.ProxyEstBucketColumn;
import teamdash.wbs.columns.ProxyEstTypeColumn;
import teamdash.wbs.columns.SizeTypeColumn;
import teamdash.wbs.columns.TaskDependencyColumn;
import teamdash.wbs.columns.TaskLabelColumn;
import teamdash.wbs.columns.TaskSizeColumn;
import teamdash.wbs.columns.TaskSizeUnitsColumn;
import teamdash.wbs.columns.TeamActualTimeColumn;
import teamdash.wbs.columns.TeamMemberColumnManager;
import teamdash.wbs.columns.TeamTimeColumn;
import teamdash.wbs.columns.WBSNodeColumn;
import teamdash.wbs.columns.WbsNodeAttributeSource;


public class DataTableModel extends AbstractTableModel {

    public static final int WBS_NODE_COLUMN = 0;

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
    /** A list of the columns which are sources of label data */
    private Set<Integer> labelSources;
    /** A list of the columns which are sources of attribute data */
    private Set<Integer> attrSources;
    /** A timer for triggering recalculations */
    private Timer recalcJanitorTimer;
    /** Object which manages columns for team members */
    private TeamMemberColumnManager memberColumnManager;
    /** Object which manages custom columns */
    private CustomColumnManager customColumnManager;

    /** Should editing be disabled? */
    private boolean disableEditing = false;

    public DataTableModel(WBSModel wbsModel, TeamMemberList teamList,
                          TeamProcess teamProcess,
                          WorkflowWBSModel workflows,
                          ProxyWBSModel proxies,
                          MilestonesWBSModel milestones,
                          CustomColumnSpecs customColumns,
                          TaskDependencySource dependencySource,
                          String currentUser)
    {
        this.wbsModel = wbsModel;
        wbsModel.addTableModelListener(new TableModelEventRepeater());

        columns = new ArrayList();
        dirtyColumns = new HashSet();
        labelSources = new HashSet<Integer>();
        attrSources = new HashSet<Integer>();

        recalcJanitorTimer = new Timer(1000, new RecalcJanitor());
        recalcJanitorTimer.setRepeats(false);
        recalcJanitorTimer.setInitialDelay(3000);

        buildDataColumns(teamList, teamProcess, workflows, proxies, milestones,
            customColumns, dependencySource, currentUser);
        initializeColumnDependencies();
    }

    public void setEditingEnabled(boolean enabled) {
        disableEditing = !enabled;
    }

    public boolean isEditingEnabled() {
        return !disableEditing;
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
            int columnPos = findIndexOfColumn(column);

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
        //dumpColumnDependencies();

        // recalculate all calculated columns.
        try {
            beginChange();
            dirtyColumns.addAll(calculatedColumns);
        } finally {
            endChange();
        }
    }

    protected void dumpColumnDependencies() {
        System.out.println("===============================================");
        System.out.println("<html><head><title>Column dependencies</title></head>"+
        "<body><h1>Column dependencies</h1>");
        String indent="&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
        Iterator i = calculatedColumns.iterator();
        while (i.hasNext()) {
            DataColumn c = (DataColumn) i.next();
            String id1 = c.getColumnID();
            System.out.println("<p><a name='"+id1+"'></a>"+id1);
            System.out.println("<br>"+indent+"Name:" + c.getColumnName());
            System.out.println("<br>"+indent+"Class: {@link " + c.getClass().getName() + "}");
            System.out.println("<br>"+indent+"Depends on:");
            int id = findIndexOfColumn(c);
            for (int j=0;   j < columns.size();   j++)
                if (dependencies[id][j]) {
                    String id2 = getColumn(j).getColumnID();
                    System.out.println("<br>"+indent+indent+"<a href='#"+id2+"'>"+id2+"</a>");
                }
            System.out.println("    Affects:");
            id = findIndexOfColumn(c);
            for (int j=0;   j < columns.size();   j++)
                if (dependencies[j][id]) {
                    String id2 = getColumn(j).getColumnID();
                    System.out.println("<br>"+indent+indent+"<a href='#"+id2+"'>"+id2+"</a>");
                }
            System.out.println();
        }
        System.out.println("</body></html>");
    }

    /** Add a single data column to the data model */
    public void addDataColumn(DataColumn column) {
        int newColumnIndex = columns.size();
        columns.add(column);
        if (column instanceof IndexAwareDataColumn)
            ((IndexAwareDataColumn) column).setColumnIndex(newColumnIndex);
        if (column instanceof LabelSource)
            labelSources.add(newColumnIndex);
        if (column instanceof WbsNodeAttributeSource)
            attrSources.add(newColumnIndex);

        // if the dependencies are already computed, update them.
        if (dependencies != null)
            initializeColumnDependencies();
    }

    /** Remove a single data column from the data model */
    public void removeDataColumn(DataColumn column) {
        int pos = findIndexOfColumn(column);
        if (pos == -1) return;
        if (column instanceof IndexAwareDataColumn)
            ((IndexAwareDataColumn) column).setColumnIndex(-1);
        columns.set(pos, new NullDataColumn());
        labelSources.remove(pos);
        attrSources.remove(pos);
        // if the dependencies are already computed, update them.
        if (dependencies != null)
            initializeColumnDependencies();
    }

    public int findIndexOfColumn(Object column) {
        if (column instanceof IndexAwareDataColumn)
            return ((IndexAwareDataColumn) column).getColumnIndex();
        else
            return columns.indexOf(column);
    }

    public Integer[] getLabelSourceColumns() {
        return labelSources.toArray(new Integer[labelSources.size()]);
    }

    public Integer[] getAttributeSourceColumns() {
        return attrSources.toArray(new Integer[attrSources.size()]);
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
            boolean needToReinitialize = (dependencies != null);
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

            if (needToReinitialize)
                initializeColumnDependencies();

        } finally {
            endChange();
        }
    }


    /** Add time columns for each team member to the given column model. */
    public void addTeamMemberPlanTimes(TableColumnModel columnModel) {
        memberColumnManager.addPlanTimesToColumnModel(columnModel);
    }

    /** Add time columns for each team member to the given column model. */
    public void addTeamMemberActualTimes(TableColumnModel columnModel) {
        memberColumnManager.addActualTimesToColumnModel(columnModel);
    }

    /** Get a list of the column numbers for each team member column. */
    public IntList getTeamMemberColumnIDs() {
        IntList result = new IntList();
        Iterator i = memberColumnManager.getPlanTimeColumns().iterator();
        while (i.hasNext())
            result.add(findIndexOfColumn(i.next()));

        return result;
    }

    /** Register a listener that should be notified about team member column
     * changes */
    public void addTeamMemberColumnListener(ChangeListener l) {
        memberColumnManager.addTeamMemberColumnListener(l);
    }

    /** Add all custom columns to the given column model. */
    public void addCustomColumns(TableColumnModel columnModel) {
        customColumnManager.addColumnsToColumnModel(columnModel);
    }

    public CustomColumnManager getCustomColumnManager() {
        return customColumnManager;
    }

    /** Create a set of data columns for this data model. */
    protected void buildDataColumns(TeamMemberList teamList,
            TeamProcess teamProcess, WorkflowWBSModel workflows,
            ProxyWBSModel proxies, MilestonesWBSModel milestones,
            CustomColumnSpecs projectColumns,
            TaskDependencySource dependencySource, String currentUser)
    {
        addDataColumn(new WBSNodeColumn(wbsModel));
        SizeTypeColumn.createSizeColumns(this, teamProcess);
        addDataColumn(new PhaseColumn(this, teamProcess, workflows));
        memberColumnManager = new TeamMemberColumnManager(this, workflows,
                teamList);
        addDataColumn(new TaskSizeColumn(this, teamProcess));
        addDataColumn(new TaskSizeUnitsColumn(this, teamProcess));
        addDataColumn(new TeamTimeColumn(this, milestones));
        addDataColumn(new TeamActualTimeColumn(this, milestones, teamList));
        addDataColumn(new TaskLabelColumn(this));
        addDataColumn(new MilestoneColumn(this, milestones));
        addDataColumn(new ProxyEstTypeColumn(this, proxies));
        addDataColumn(new ProxyEstBucketColumn(this, proxies, teamProcess));
        addDataColumn(new TaskDependencyColumn(this, dependencySource,
                teamProcess.getIconMap()));
        addDataColumn(new NotesColumn(currentUser));
        addDataColumn(new ErrorNotesColumn(currentUser));
        addDataColumn(new PlanTimeWatcher(this));
        customColumnManager = new CustomColumnManager(this, projectColumns,
                teamProcess.getProcessID());
    }

    /** Return the work breakdown structure model that this data model
     * represents. */
    public WBSModel getWBSModel() { return wbsModel; }


    /** This class listens for table model events fired by the work
     * breakdown structure, and sends them to our listeners as well.
     */
    private class TableModelEventRepeater implements TableModelListener {
        public void tableChanged(TableModelEvent e) {
            // rebroadcast the event as our own.
            TableModelEvent newEvent = new TableModelEvent
                (DataTableModel.this, e.getFirstRow(),
                 e.getLastRow(), e.getColumn(), e.getType());
            fireTableChanged(newEvent);

            if (WBSModelEvent.isStructuralChange(e)) {
                // all calculated columns implicitly depend upon the structure
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
        if (disableEditing && columnIndex != 0) return false;

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
        setValueAtImpl(aValue, wbsModel.getNodeForRow(rowIndex), rowIndex,
            columnIndex);
    }

    public void setValueAt(Object aValue, WBSNode node, int columnIndex) {
        setValueAtImpl(aValue, node, -2, columnIndex);
    }

    private void setValueAtImpl(Object aValue, WBSNode node, int rowIndex,
            int columnIndex) {
        DataColumn column = getColumn(columnIndex);
        if (node == null || column == null) return;

        try {
            beginChange();
            columnChanged(column, columnIndex);
            column.setValueAt(aValue, node);

            if (!(column instanceof CalculatedDataColumn)) {
                if (rowIndex == -2)
                    rowIndex = wbsModel.getRowForNode(node);
                if (rowIndex != -1)
                    fireTableCellUpdated(rowIndex, columnIndex);
            }
        } finally {
            endChange();
        }
    }

    public void setValuesAt(WBSNode node, Map<String, Object> values) {
        try {
            beginChange();
            for (Entry<String, Object> e : values.entrySet()) {
                String columnName = e.getKey();
                int columnIndex = findColumn(columnName);
                if (columnIndex >= 0)
                    setValueAt(e.getValue(), node, columnIndex);
            }
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
                columnChanged(column, findIndexOfColumn(column));
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
        //When all in-progress data changes complete, trigger a recalculation.
        if (finalDepth == 0) recalcColumns();
    }

    /** This class cleans up after people who neglect to call endChange().
     */
    private final class RecalcJanitor implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            System.out.println("Someone didn't call endChange - cleaning up");
            synchronized (dirtyColumns) {
                changeDepth = 0;
            }
            recalcColumns();
        }
    }


    /** Recalculate data in all dirty columns.
     */
    private void recalcColumns() {
        recalcJanitorTimer.stop();
        synchronized (dirtyColumns) {
            // nothing to do?
            if (dirtyColumns.isEmpty()) return;

            HashSet waitingColumns = new HashSet();

            try {
                // start a pseudo-change; this way, if recalculating
                // any column causes it to alter other data, we can be
                // efficient about our recalculation process and not
                // have two recalcColumns() methods running simultaneously.
                beginChange();

                while (!dirtyColumns.isEmpty()) {
                    // recalculate a single column.
                    waitingColumns.clear();
                    CalculatedDataColumn c =
                        (CalculatedDataColumn) dirtyColumns.iterator().next();
                    recalcColumn(c, waitingColumns);
                }

            } catch (Exception e) {
                e.printStackTrace();
                // if we had difficulty recalculating, cancel the recalc by
                // clearing out the dirtyColumns list. (Otherwise, the
                // endChange() call two lines below could start an infinite
                // loop of recalculating and retriggering the exception)
                dirtyColumns.clear();
            } finally {
                endChange();
            }
        }
    }

    /** Recalculate a column.
     *
     * If the column depends upon any other dirty columns, they will be
     * recalculated first.  The waitingColumns parameter is used to detect
     * circular dependencies and avoid infinite recursion.
     *
     * @param column the column to recalculate.
     * @param waitingColumns a list of columns that are waiting on this
     * column before they can recalculate.
     */
    private void recalcColumn(CalculatedDataColumn column,
                              Set waitingColumns)
    {
        // if this column somehow isn't dirty anymore, do nothing.
        if (dirtyColumns.contains(column) == false) return;

        // detect circular dependencies and abort.
        if (waitingColumns.contains(column)) {
            System.out.println("Circular column dependency:"+waitingColumns);
            return;
        }

        int columnPos = findIndexOfColumn(column);
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


    /**
     * Customize the behavior and appearance of the columns in a table which is
     * using a DataTableModel, based upon the preferences described by the
     * DataColumn objects in that model.
     */
    public static void installColumnCustomizations(JTable table) {
        DataTableModel dataModel = (DataTableModel) table.getModel();
        for (int i = 0; i < table.getColumnCount(); i++) {
            TableColumn tc = table.getColumnModel().getColumn(i);
            DataColumn dc = dataModel.getColumn(tc.getModelIndex());
            DataTableColumn.init(tc, dc);
        }
    }

}
