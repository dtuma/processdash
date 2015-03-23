// Copyright (C) 2014 Tuma Solutions, LLC
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

import java.awt.Component;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import teamdash.wbs.AutocompletingDataTableCellEditor;
import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.CustomRenderedColumn;
import teamdash.wbs.ItalicNumericCellRenderer;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.ProxyDataModel;
import teamdash.wbs.ProxyWBSModel;
import teamdash.wbs.TeamProcess;
import teamdash.wbs.WBSNode;

public class ProxySizeColumn extends AbstractNumericColumn implements
        CalculatedDataColumn, CustomRenderedColumn {

    /** Attribute to store the numeric size estimate for a bucket */
    private static final String ATTR_NAME = "Proxy Size";

    /** Attribute to store extrapolated size values for a bucket */
    private static final String EXTRAPOLATED_ATTR_NAME = "_Extrapolated "
            + ATTR_NAME;

    /** Attribute to store the size metric for a proxy category */
    private static final String METRIC_ATTR_NAME = "Proxy Size Metric";

    /** Attribute to hold a flag forcing the display of size-related values for
     * a proxy category, even when no size metric has been entered */
    public static final String FORCED_ATTR_NAME = "Force Enable Proxy Sizes";

    static final String COLUMN_ID = ATTR_NAME;

    /** Tooltip to display for extrapolated values */
    private static final String EXTRAPOLATED_TOOLTIP = resources
            .getString("Proxy_Size.Extrapolated_Tooltip");

    /** Text/tooltip to display on the proxy category row if no size metric has
     * been entered, but sizes have been "forcefully" enabled */
    private static final String MISSING_METRIC_MSG = resources
            .getString("Proxy_Size.Metric_Missing.Text");
    private static final String MISSING_METRIC_TOOLTIP = resources
            .getString("Proxy_Size.Metric_Missing.Tooltip");

    /** Tooltip to display for unrecognized size metrics */
    private static final String BAD_METRIC_TOOLTIP = resources
            .getString("Proxy_Size.Bad_Metric_Tooltip");


    private ProxyDataModel dataModel;

    private ProxyWBSModel proxyModel;

    private Set sizeMetrics;

    private TableCellEditor sizeMetricsEditor;


    public ProxySizeColumn(ProxyDataModel dataModel, TeamProcess process) {
        this.dataModel = dataModel;
        this.proxyModel = dataModel.getWBSModel();
        this.columnName = resources.getString("Proxy_Size.Name");
        this.columnID = COLUMN_ID;
        this.sizeMetrics = new HashSet(Arrays.asList(process.getSizeMetrics()));
        this.sizeMetricsEditor = new AutocompletingDataTableCellEditor(
                new JComboBox(process.getSizeMetrics()));
        setConflictAttributeName(ATTR_NAME);
        conflictAttributeNamePattern.addLiteralEquals(METRIC_ATTR_NAME);
    }

    public boolean isCellEditable(WBSNode node) {
        return node.getIndentLevel() > 0;
    }

    public Object getValueAt(WBSNode node) {
        if (ProxyWBSModel.isProxy(node)) {
            Object value = node.getAttribute(METRIC_ATTR_NAME);
            if (value == null && node.getAttribute(FORCED_ATTR_NAME) != null)
                value = MISSING_METRIC_MSG;
            return new SizeMetricValue(value);

        } else if (ProxyWBSModel.isBucket(node)) {
            return getSizeValueAt(node);

        } else {
            return null;
        }
    }

    public static NumericDataValue getSizeValueAt(WBSNode bucket) {
        if (!hasSizeMetric(bucket) || !ProxyWBSModel.isBucket(bucket))
            return null;

        double value = bucket.getNumericAttribute(ATTR_NAME);
        if (value > 0)
            return new NumericDataValue(value);

        value = bucket.getNumericAttribute(EXTRAPOLATED_ATTR_NAME);
        if (value > 0)
            return new NumericDataValue(value, true, false,
                    EXTRAPOLATED_TOOLTIP);

        return null;
    }

    public void setValueAt(Object aValue, WBSNode node) {
        if (isNoOpEdit(aValue, node)) {
            // if the user started an editing session but ultimately made no
            // changes, do nothing

        } else if (ProxyWBSModel.isProxy(node)) {
            // if this is a proxy category node, store the new size metric name
            storeProxySizeMetric(aValue, node);

        } else if (ProxyWBSModel.isBucket(node)) {
            // if this is a bucket, store the new size.
            storeBucketSize(aValue, node);
        }
    }

    private void storeProxySizeMetric(Object aValue, WBSNode proxy) {
        String metric = null;
        if (aValue != null)
            metric = aValue.toString().trim();
        if ("".equals(metric))
            metric = null;
        if (metric == null || sizeMetrics.contains(metric)) {
            proxy.setAttribute(METRIC_ATTR_NAME, metric);
            proxy.removeAttribute(FORCED_ATTR_NAME);
        }
    }

    private void storeBucketSize(Object aValue, WBSNode bucket) {
        double numValue = NumericDataValue.parse(aValue);
        if (numValue > 0) {
            bucket.setNumericAttribute(ATTR_NAME, numValue);
            ensureSizeMetric(bucket);
        } else if (numValue == 0) {
            bucket.setAttribute(ATTR_NAME, null);
        }
    }

    /**
     * Return true if a size metric has been entered on the proxy category that
     * the given node belongs to.
     * 
     * @param node
     *            a node in the proxy model. This can either be the node for a
     *            category row, or the node for one of the buckets within the
     *            category.
     * @return true if the specified category has a non-empty size metric.
     */
    public static boolean hasSizeMetric(WBSNode node) {
        if (node == null)
            return false;
        if (ProxyWBSModel.isBucket(node))
            node = node.getWbsModel().getParent(node);
        return node.getAttribute(METRIC_ATTR_NAME) != null
                || node.getAttribute(FORCED_ATTR_NAME) != null;
    }

    /**
     * Retrieve the size metric units that are set on a given proxy category.
     * 
     * @param node
     *            a node in the proxy model. This can either be the node for a
     *            category row, or the node for one of the buckets within the
     *            category.
     * @return the size units set on this proxy category, or null if no size
     *         metric has been set.
     */
    public static String getSizeMetric(WBSNode node) {
        if (node == null)
            return null;
        if (ProxyWBSModel.isBucket(node))
            node = node.getWbsModel().getParent(node);
        return (String) node.getAttribute(METRIC_ATTR_NAME);
    }

    /**
     * Ensure that <b>some</b> size metric has been entered on the proxy
     * category that the given node belongs to. If not, write an error flag on
     * the node. The flag will trigger the display of "?????" as the size
     * metric, which will be displayed as an error needing correction.
     * 
     * @param node
     *            a node in the proxy model. This can either be the node for a
     *            category row, or the node for one of the buckets within the
     *            category.
     */
    public void ensureSizeMetric(WBSNode node) {
        if (node == null)
            return;
        if (ProxyWBSModel.isBucket(node))
            node = node.getWbsModel().getParent(node);
        if (node.getAttribute(METRIC_ATTR_NAME) == null
                && node.getAttribute(FORCED_ATTR_NAME) == null) {
            node.setAttribute(FORCED_ATTR_NAME, "t");
            dataModel.columnChanged(this);
        }
    }

    public void storeDependentColumn(String ID, int columnNumber) {}

    public boolean recalculate() {
        WBSNode[] proxies = proxyModel.getChildren(proxyModel.getRoot());
        for (WBSNode proxy : proxies) {
            if (hasSizeMetric(proxy))
                recalcProxy(proxy);
        }
        return true;
    }


    private void recalcProxy(WBSNode proxy) {
        WBSNode[] buckets = proxyModel.getChildren(proxy);
        extrapolateMissingValues(this, buckets, EXTRAPOLATED_ATTR_NAME);
    }

    public TableCellEditor getCellEditorForRow(int row) {
        WBSNode node = proxyModel.getNodeForRow(row);
        return (ProxyWBSModel.isProxy(node) ? sizeMetricsEditor : null);
    }

    public TableCellRenderer getCellRenderer() {
        return new ProxySizeRenderer();
    }


    /**
     * Class to display data in the proxy size column.
     * 
     * This displays numbers regularly, but left-formats the size metric cell,
     * which is textual instead of numeric.
     */
    private class ProxySizeRenderer extends ItalicNumericCellRenderer {

        public ProxySizeRenderer() {
            super(EXTRAPOLATED_TOOLTIP);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            Component result = super.getTableCellRendererComponent(table,
                value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(getAlignment((NumericDataValue) value));
            return result;
        }

        private int getAlignment(NumericDataValue value) {
            if (value instanceof SizeMetricValue)
                return SwingConstants.LEFT;
            else if (value != null
                    && EXTRAPOLATED_TOOLTIP.equals(value.errorMessage))
                return SwingConstants.RIGHT;
            else
                return SwingConstants.CENTER;
        }
    }

    /**
     * This column primarily displays numeric data, so the renderer expects to
     * process NumericDataValue objects. The size metric on the proxy category
     * row is not a number, so this class wraps it as a pseudo-number so it can
     * be displayed anyway.
     */
    private class SizeMetricValue extends NumericDataValue {

        private String display;

        protected SizeMetricValue(Object value) {
            super(0);
            this.display = (value == null ? "" : value.toString());
            if (MISSING_METRIC_MSG.equals(value))
                this.errorMessage = MISSING_METRIC_TOOLTIP;
            else if (!sizeMetrics.contains(this.display))
                this.errorMessage = BAD_METRIC_TOOLTIP;
        }

        @Override
        public String toString() {
            return display;
        }

    }


    /**
     * Look at the values for a set of buckets, and fill in missing values with
     * a log-normal extrapolation.
     */
    static void extrapolateMissingValues(AbstractNumericColumn col,
            WBSNode[] buckets, String extrapolatedAttr) {
        // retrieve the current value for each bucket.
        int numValuesFound = 0;
        double[] values = new double[buckets.length];
        for (int i = buckets.length; i-- > 0;) {
            WBSNode b = buckets[i];
            b.setAttribute(extrapolatedAttr, null);
            NumericDataValue v = (NumericDataValue) col.getValueAt(b);
            if (v != null && v.value > 0) {
                values[i] = v.value;
                numValuesFound++;
            }
        }

        // if no buckets have values, or if all buckets have values, exit.
        if (numValuesFound == 0 || numValuesFound == buckets.length) {
            return;
        }

        // if we have only one value, extrapolate around it using a standard
        // scaling factor
        else if (numValuesFound == 1) {
            int pos = findFirstValue(values, 0);
            extrapolateDown(buckets, values, extrapolatedAttr, pos,
                DEFAULT_SCALING_FACTOR);
            extrapolateUp(buckets, values, extrapolatedAttr, pos,
                DEFAULT_SCALING_FACTOR);
        }

        // if we have two or more values, extrapolate between and around them
        else if (numValuesFound > 1) {
            // find the first pair of values, and extrapolate between them
            int low = findFirstValue(values, 0);
            int high = findFirstValue(values, low + 1);
            double factor = extrapolateBetween(buckets, values,
                extrapolatedAttr, low, high);

            // use the same factor to extrapolate down below that first pair
            extrapolateDown(buckets, values, extrapolatedAttr, low, factor);

            // find additional pairs and extrapolate between them too
            while (true) {
                low = high;
                high = findFirstValue(values, low + 1);
                if (high == -1)
                    break;
                factor = extrapolateBetween(buckets, values, extrapolatedAttr,
                    low, high);
            }

            // extrapolate up from the last pair
            extrapolateUp(buckets, values, extrapolatedAttr, low, factor);
        }
    }

    private static int findFirstValue(double[] values, int pos) {
        while (pos < values.length) {
            if (values[pos] > 0)
                return pos;
            else
                pos++;
        }
        return -1;
    }

    private static void extrapolateDown(WBSNode[] buckets, double[] values,
            String attrName, int pos, double factor) {
        double value = values[pos];
        while (pos-- > 0) {
            value /= factor;
            buckets[pos].setNumericAttribute(attrName, value);
        }
    }

    private static void extrapolateUp(WBSNode[] buckets, double[] values,
            String attrName, int pos, double factor) {
        double value = values[pos];
        while (++pos < buckets.length) {
            value *= factor;
            buckets[pos].setNumericAttribute(attrName, value);
        }
    }

    private static double extrapolateBetween(WBSNode[] buckets,
            double[] values, String attrName, int low, int high) {
        int width = high - low;
        double ratio = values[high] / values[low];
        double factor = Math.exp(Math.log(ratio) / width);
        int pos = low;
        double value = values[low];
        while (++pos < high) {
            value *= factor;
            buckets[pos].setNumericAttribute(attrName, value);
        }
        return factor;
    }

    private static final double DEFAULT_SCALING_FACTOR = 2.5;

}
