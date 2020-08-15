// Copyright (C) 2020 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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

package teamdash.wbs.columns;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumnModel;

import teamdash.wbs.DataColumn;
import teamdash.wbs.DataTableColumn;
import teamdash.wbs.SizeMetricsWBSModel;
import teamdash.wbs.TeamProcess;
import teamdash.wbs.WBSDataModel;

public class SizeColumnManager {

    /** Constants for indexing the plan/actual arrays below */
    private static final int PLAN = 0, ACT = 1;

    /** The WBSDataModel to create columns within */
    private WBSDataModel wbsDataModel;

    /** The team process for this project */
    private TeamProcess teamProcess;

    /** Maps of the planned/actual DataColumn objects for each metric */
    private Map<String, DataColumn> sizeColumns[];

    /** Maps of the planned/actual column indexes for each metric */
    private Map<String, Integer> sizeColumnIndexes[];

    /** Table models holding the planned/actual size columns */
    private TableColumnModel tableColumns[];

    /** A list of objects that wish to be notified about column changes */
    private List<ChangeListener> listeners;


    public SizeColumnManager(WBSDataModel wbsDataModel, TeamProcess teamProcess,
            SizeMetricsWBSModel sizeMetrics) {
        this.wbsDataModel = wbsDataModel;
        this.teamProcess = teamProcess;
        this.sizeColumns = new Map[] { new LinkedHashMap<String, DataColumn>(),
                new LinkedHashMap<String, DataColumn>() };
        this.sizeColumnIndexes = new Map[] { new LinkedHashMap<String, Integer>(),
                new LinkedHashMap<String, Integer>() };
        this.tableColumns = new TableColumnModel[] {
                new DefaultTableColumnModel(), new DefaultTableColumnModel() };
        this.listeners = new ArrayList<ChangeListener>();

        if (SizeTypeColumn.isUsingNewSizeDataColumns(wbsDataModel.getWBSModel()))
            createSizeDataColumns();
        else
            lookupOldStyleSizeColumns();

        if (sizeMetrics != null) {
            sizeMetrics.addTableModelListener(new TableModelListener() {
                public void tableChanged(TableModelEvent e) {
                    createSizeDataColumns();
                }
            });
        }
    }

    /**
     * @param plan
     *            true to return plan columns, false to return actual
     * @return a map of data columns for each metric, keyed by their metric ID.
     *         The contents of the resulting map may change over time (as size
     *         metrics are modified), but the map reference itself will not.
     */
    public Map<String, DataColumn> getSizeColumns(boolean plan) {
        return sizeColumns[plan ? PLAN : ACT];
    }

    /**
     * @param plan
     *            true to return plan indexes, false to return actual
     * @return a map of column indexes for each metric, keyed by their metric
     *         ID. The contents of the resulting map may change over time (as
     *         size metrics are modified), but the map reference itself will
     *         not.
     */
    public Map<String, Integer> getSizeColumnIndexes(boolean plan) {
        return sizeColumnIndexes[plan ? PLAN : ACT];
    }

    /**
     * @param plan
     *            true to return a table of plan columns, false to return actual
     * @return a TableColumnModel containing columns for planned/actual size
     *         data. This model will be updated automatically if size metrics
     *         are modified.
     */
    public TableColumnModel getTableColumns(boolean plan) {
        return tableColumns[plan ? PLAN : ACT];
    }

    public void addSizeMetricsColumnListener(ChangeListener l) {
        listeners.add(l);
    }

    public void removeSizeMetricsColumnListener(ChangeListener l) {
        listeners.remove(l);
    }

    private void createSizeDataColumns() {
        Map<String, DataColumn> obsoleteColumns[] = new Map[2];
        ArrayList newColumns = new ArrayList();

        // Loop over plan/actual
        for (int i = 2; i-- > 0;) {
            boolean plan = (i == PLAN);
            obsoleteColumns[i] = new HashMap(sizeColumns[i]);
            sizeColumns[i].clear();

            // Loop through the list of size metrics
            for (String metricID : teamProcess.getSizeMetricMap().keySet()) {
                // try to find an existing size column for this metric
                DataColumn sizeCol = obsoleteColumns[i].remove(metricID);
                if (sizeCol == null) {
                    // create a new size data column if needed
                    sizeCol = new SizeDataColumn(wbsDataModel, teamProcess,
                            metricID, plan);
                    newColumns.add(sizeCol);
                }
                // add the size column to our master list
                sizeColumns[i].put(metricID, sizeCol);
            }
        }

        // make the changes to the columns in the data model.
        if (!newColumns.isEmpty() || !obsoleteColumns[PLAN].isEmpty()
                || !obsoleteColumns[ACT].isEmpty()) {
            List<DataColumn> columnsToRemove = new ArrayList();
            columnsToRemove.addAll(obsoleteColumns[PLAN].values());
            columnsToRemove.addAll(obsoleteColumns[ACT].values());
            wbsDataModel.addRemoveDataColumns(newColumns, columnsToRemove);
        }

        // update our maps of the column indexes
        lookupColumnIndexes(sizeColumnIndexes[PLAN], sizeColumns[PLAN]);
        lookupColumnIndexes(sizeColumnIndexes[ACT], sizeColumns[ACT]);

        // update our column models
        replaceColumns(tableColumns[PLAN], sizeColumns[PLAN].values());
        replaceColumns(tableColumns[ACT], sizeColumns[ACT].values());

        // alert any listeners that have registered interest
        ChangeEvent e = new ChangeEvent(this);
        for (ChangeListener l : listeners)
            l.stateChanged(e);
    }

    private void lookupColumnIndexes(Map<String, Integer> indexes,
            Map<String, DataColumn> columns) {
        indexes.clear();
        for (Entry<String, DataColumn> e : columns.entrySet()) {
            String metricID = e.getKey();
            DataColumn column = e.getValue();
            int columnIndex = wbsDataModel.findIndexOfColumn(column);
            indexes.put(metricID, columnIndex);
        }
    }

    private void replaceColumns(TableColumnModel table,
            Collection<DataColumn> columns) {
        // discard all of the columns in the model
        for (int i = table.getColumnCount(); i-- > 0;)
            table.removeColumn(table.getColumn(i));

        // create new columns for each metric
        for (DataColumn col : columns) {
            DataTableColumn dtc = new DataTableColumn(wbsDataModel, col);
            dtc.setHeaderValue(((SizeDataColumn) col).getMetricDisplayName());
            table.addColumn(dtc);
        }
    }

    /**
     * Look up the planned and actual size columns that were created for a
     * project that doesn't use WBS-managed size.
     */
    private void lookupOldStyleSizeColumns() {
        // Loop over plan/actual
        for (int i = 2; i-- > 0;) {
            boolean plan = (i == PLAN);

            // Loop through the list of size metrics
            for (String metric : teamProcess.getSizeMetricMap().keySet()) {
                // look up the index of this size column
                String columnID = SizeDataColumn.getColumnID(metric, plan);
                int columnIndex = wbsDataModel.findColumn(columnID);
                if (columnIndex != -1) {
                    // look up the column itself
                    DataColumn column = wbsDataModel.getColumn(columnIndex);
                    sizeColumns[i].put(metric, column);
                    sizeColumnIndexes[i].put(metric, columnIndex);
                }
            }
        }
    }

}
