// Copyright (C) 2010-2020 Tuma Solutions, LLC
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

import java.util.Map;

import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import net.sourceforge.processdash.util.StringUtils;

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.CustomEditedColumn;
import teamdash.wbs.CustomRenderedColumn;
import teamdash.wbs.DataTableCellRenderer;
import teamdash.wbs.ErrorValue;
import teamdash.wbs.SizeMetric;
import teamdash.wbs.SizeMetricsWBSModel;
import teamdash.wbs.TeamProcess;
import teamdash.wbs.WBSNode;
import teamdash.wbs.WorkflowDataModel;
import teamdash.wbs.WorkflowWBSModel;

public class WorkflowSizeUnitsColumn extends AbstractDataColumn
        implements CalculatedDataColumn, CustomRenderedColumn,
        CustomEditedColumn {

    private WorkflowDataModel dataModel;

    private SizeMetricsWBSModel sizeMetrics;

    private SizeMetricCellEditor sizeMetricsEditor;


    public WorkflowSizeUnitsColumn(WorkflowDataModel dataModel,
            SizeMetricsWBSModel sizeMetrics) {
        this.dataModel = dataModel;
        this.sizeMetrics = sizeMetrics;
        this.columnName = resources.getString("Workflow.Units.Name");
        this.columnID = COLUMN_ID;
        this.preferredWidth = 80;
        setConflictAttributeName(METRIC_ID_ATTR);
    }

    @Override
    public void storeDependentColumn(String ID, int columnNumber) {}

    @Override
    public boolean isCellEditable(WBSNode node) {
        return isProbeTask(node);
    }

    private static boolean isProbeTask(WBSNode node) {
        return node != null && node.getIndentLevel() > 1
                && TeamProcess.isProbeTask(node.getType());
    }

    @Override
    public Object getValueAt(WBSNode node) {
        if (!isProbeTask(node))
            return null;

        String metricName = (String) node.getAttribute(METRIC_NAME_ATTR);
        String metricID = (String) node.getAttribute(METRIC_ID_ATTR);
        SizeMetric metric = sizeMetrics.getIdToMetricMap().get(metricID);
        if (metric != null)
            return metric;
        else if (StringUtils.hasValue(metricName))
            return new ErrorValue(metricName + " ",
                    resources.getString("Workflow.Units.Bad_Metric_Tooltip"));
        else
            return new ErrorValue(
                    resources.getString("Workflow.Units.Metric_Missing.Text"),
                    resources.format("Workflow.Units.Metric_Missing.Tooltip_FMT",
                        node.getName()));
    }

    @Override
    public void setValueAt(Object aValue, WBSNode node) {
        // if the user started an editing session but ultimately made no
        // changes, do nothing
        if (aValue instanceof String) {
            String oldValue = String.valueOf(getValueAt(node));
            if (aValue.equals(oldValue))
                return;
        }

        SizeMetric metric = SizeMetricCellEditor.parseValue(sizeMetrics, aValue,
            true);
        if (metric != null) {
            node.setAttribute(METRIC_NAME_ATTR, metric.getName());
            node.setAttribute(METRIC_ID_ATTR, metric.getMetricID());
        } else {
            node.removeAttribute(METRIC_NAME_ATTR);
            node.removeAttribute(METRIC_ID_ATTR);
        }
    }

    @Override
    public void storeConflictResolutionValue(Object value, WBSNode node) {
        node.setAttribute(METRIC_ID_ATTR, value);
    }

    @Override
    public boolean recalculate() {
        for (WBSNode node : dataModel.getWBSModel().getWbsNodes())
            recalculate(node);
        return true;
    }

    private void recalculate(WBSNode node) {
        if (!isProbeTask(node))
            return;

        // read the values for this metric from the proxy node
        String metricName = (String) node.getAttribute(METRIC_NAME_ATTR);
        String metricID = (String) node.getAttribute(METRIC_ID_ATTR);
        SizeMetric metric = sizeMetrics.getIdToMetricMap().get(metricID);
        if (metric != null) {
            // if we found a metric with the given ID, make sure the correct
            // name is stored in the node (in case it changed elsewhere)
            node.setAttribute(METRIC_NAME_ATTR, metric.getName());

        } else if (metricID == null && StringUtils.hasValue(metricName)) {
            // this node has a metric name, but no ID. This is the pattern for
            // a node from an external/legacy source. Lookup or create the named
            // size metric and store its data on the node.
            setValueAt(metricName, node);
        }
    }

    @Override
    public TableCellRenderer getCellRenderer() {
        return new DataTableCellRenderer();
    }

    @Override
    public TableCellEditor getCellEditor() {
        if (sizeMetricsEditor == null)
            sizeMetricsEditor = new SizeMetricCellEditor(sizeMetrics, dataModel,
                    this);
        return sizeMetricsEditor;
    }

    public static SizeMetric getSizeMetricForProbeTask(WBSNode node,
            Map<String, SizeMetric> sizeMetrics) {
        String metricID = getSizeMetricIdForProbeTask(node);
        return sizeMetrics.get(metricID);
    }

    public static String getSizeMetricIdForProbeTask(WBSNode node) {
        return (String) node.getAttribute(METRIC_ID_ATTR);
    }

    public static void remapNodeIDs(WorkflowWBSModel model,
            Map<String, String> sizeMetricIDMappings) {
        SizeMetricsWBSModel.remapSizeMetricIdAttrValues(model,
            sizeMetricIDMappings, METRIC_ID_ATTR);
    }

    public static final String COLUMN_ID = "Task Size Units";

    public static final String METRIC_NAME_ATTR = COLUMN_ID;

    public static final String METRIC_ID_ATTR = COLUMN_ID + " ID";

}
