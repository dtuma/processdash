// Copyright (C) 2014-2016 Tuma Solutions, LLC
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

package teamdash.wbs.columns;

import static teamdash.wbs.WorkflowModel.WORKFLOW_SOURCE_IDS_ATTR;

import java.awt.Component;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import teamdash.wbs.AutocompletingDataTableCellEditor;
import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.CustomEditedColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.ErrorValue;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.ProxyWBSModel;
import teamdash.wbs.TeamProcess;
import teamdash.wbs.WBSNode;

public class ProxyEstBucketColumn extends AbstractDataColumn implements
        CustomEditedColumn, CalculatedDataColumn {

    public static final String ATTR_NAME = "Estimated Proxy Bucket";

    public static final String COLUMN_ID = ATTR_NAME;


    private DataTableModel dataModel;

    private ProxyWBSModel proxyModel;

    private String[] sizeMetrics;

    private Map<String, String> componentTypes;

    private int timeColumnPos;

    private int nodeTypeColumn;

    private Map<String, Integer> sizeColumns;


    public ProxyEstBucketColumn(DataTableModel dataModel,
            ProxyWBSModel proxyModel, TeamProcess process) {
        this.dataModel = dataModel;
        this.proxyModel = proxyModel;
        this.sizeMetrics = process.getSizeMetrics();
        this.componentTypes = process.getWorkProductSizeMap();
        this.columnID = COLUMN_ID;
        this.columnName = resources.getString("Proxy_Est_Bucket.Name");
        setConflictAttributeName(ATTR_NAME);

        this.dependentColumns = new String[] { ProxyEstTypeColumn.COLUMN_ID };
    }

    public Object getValueAt(WBSNode node) {
        return node.getAttribute(ATTR_NAME);
    }

    public boolean isCellEditable(WBSNode node) {
        return node.getIndentLevel() > 0;
    }


    public void setValueAt(Object aValue, WBSNode node) {
        // if the user asks to clear the current value, oblige.
        if (aValue == null || "".equals(aValue)) {
            clearValueAt(node);
            return;
        }

        // find the bucket whose name matches the given value
        WBSNode selectedBucket = null;
        for (WBSNode bucket : getBuckets(node)) {
            if (bucket.getName().equals(aValue)) {
                selectedBucket = bucket;
                break;
            }
        }

        if (selectedBucket != null) {
            // store the name of the selected bucket for display purposes
            node.setAttribute(ATTR_NAME, selectedBucket.getName());

            // look for size and time estimates and apply if present
            maybeStoreTimeEstimate(node, selectedBucket);
            maybeStoreSizeEstimate(node, selectedBucket);
        }
    }

    private void maybeStoreTimeEstimate(WBSNode node, WBSNode bucket) {
        NumericDataValue time = ProxyTimeColumn.getTimeValueAt(bucket);
        if (time != null) {
            dataModel.setValueAt(time.value, node, timeColumnPos);
        }
    }

    private void maybeStoreSizeEstimate(WBSNode node, WBSNode bucket) {
        // check for a valid size estimate on this bucket; abort if absent
        String sizeMetric = ProxySizeColumn.getSizeMetric(bucket);
        NumericDataValue size = ProxySizeColumn.getSizeValueAt(bucket);
        Integer sizeCol = sizeColumns.get(sizeMetric);
        if (sizeMetric == null || size == null || sizeCol == null)
            return;

        // only change size estimates for components, not for tasks.
        String currentType = (String) dataModel
                .getValueAt(node, nodeTypeColumn);
        String currentMetric = componentTypes.get(currentType);
        if (currentMetric == null)
            return;

        // clear out top-down size estimates of other types for this node
        for (Integer col : sizeColumns.values()) {
            if (col != sizeCol) {
                NumericDataValue oneSize = (NumericDataValue) dataModel
                        .getValueAt(node, col);
                if (oneSize != null && oneSize.value > 0)
                    dataModel.setValueAt("", node, col);
            }
        }

        // change the type of this node if necessary.
        if (!currentMetric.equals(sizeMetric)) {
            Object workflowSrcId = node.getAttribute(WORKFLOW_SOURCE_IDS_ATTR);
            dataModel.setValueAt(getTypeForSizeMetric(sizeMetric), node,
                nodeTypeColumn);
            node.setAttribute(WORKFLOW_SOURCE_IDS_ATTR, workflowSrcId);
        }

        // store the new size estimate for this node
        dataModel.setValueAt(size.value, node, sizeCol);
    }

    private String getTypeForSizeMetric(String sizeMetric) {
        if ("LOC".equals(sizeMetric))
            return TeamProcess.COMPONENT_TYPE;
        for (Entry<String, String> e : componentTypes.entrySet()) {
            if (sizeMetric.equals(e.getValue()))
                return e.getKey();
        }
        return null;
    }

    @Override
    public void storeConflictResolutionValue(Object value, WBSNode node) {
        node.setAttribute(ATTR_NAME, value);
    }

    static void clearValueAt(WBSNode node) {
        node.removeAttribute(ATTR_NAME);
    }

    public boolean recalculate() {
        return true;
    }

    public void storeDependentColumn(String ID, int columnNumber) {}

    @Override
    public void resetDependentColumns() {
        timeColumnPos = dataModel.findColumn(TeamTimeColumn.COLUMN_ID);

        nodeTypeColumn = dataModel.findColumn(PhaseColumn.COLUMN_ID);

        sizeColumns = new HashMap();
        for (String sizeMetric : sizeMetrics) {
            String columnId = SizeAccountingColumnSet.getAddedID(sizeMetric);
            Integer columnPos = dataModel.findColumn(columnId);
            if (columnPos != -1)
                sizeColumns.put(sizeMetric, columnPos);
            columnId = SizeAccountingColumnSet.getModifiedID(sizeMetric);
            columnPos = dataModel.findColumn(columnId);
            if (columnPos != -1)
                sizeColumns.put(sizeMetric + " (Modified)", columnPos);
        }
        sizeColumns = Collections.unmodifiableMap(sizeColumns);
    }

    private WBSNode[] getBuckets(WBSNode node) {
        Integer typeId = ProxyEstTypeColumn.getTypeIdAt(node);
        if (typeId == null)
            return NO_BUCKETS;

        WBSNode proxy = proxyModel.getNodeMap().get(typeId);
        if (proxy == null)
            return NO_BUCKETS;

        WBSNode[] buckets = proxyModel.getChildren(proxy);
        return buckets;
    }

    private static final WBSNode[] NO_BUCKETS = new WBSNode[0];

    public TableCellEditor getCellEditor() {
        return new ProxyBucketCellEditor();
    }

    private class ProxyBucketCellEditor extends
            AutocompletingDataTableCellEditor {

        private String noTypeMessage = resources
                .getString("Proxy_Est_Bucket.No_Type");

        @Override
        public Component getTableCellEditorComponent(JTable table,
                Object value, boolean isSelected, int row, int column) {

            // refresh the data model with the current set of known proxies.
            getComboBox().removeAllItems();
            WBSNode node = dataModel.getWBSModel().getNodeForRow(row);
            WBSNode[] buckets = getBuckets(node);
            if (buckets.length == 0) {
                getComboBox().addItem(noTypeMessage);
            } else {
                HashSet namesSeen = new HashSet();
                namesSeen.add("");
                for (WBSNode bucket : buckets) {
                    String bucketName = bucket.getName();
                    if (namesSeen.add(bucketName))
                        getComboBox().addItem(bucketName);
                }
            }

            // now defer to the parent for the rest of the work.
            return super.getTableCellEditorComponent(table,
                ErrorValue.unwrap(value), isSelected, row, column);
        }

    }

}
