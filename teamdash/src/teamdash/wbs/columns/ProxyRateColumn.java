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

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.CustomRenderedColumn;
import teamdash.wbs.ItalicNumericCellRenderer;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.ProxyDataModel;
import teamdash.wbs.ProxyWBSModel;
import teamdash.wbs.WBSNode;

public class ProxyRateColumn extends AbstractNumericColumn implements
        CalculatedDataColumn, CustomRenderedColumn {

    private static final String ATTR_NAME = "Proxy Rate";

    private static final String INHERITED_ATTR_NAME = "_Inherited " + ATTR_NAME;

    private static final String CALC_ATTR_NAME = "_Calculated " + ATTR_NAME;

    private static final String COLUMN_ID = ATTR_NAME;


    private ProxyDataModel dataModel;

    private ProxyWBSModel proxyModel;

    private ProxySizeColumn sizeColumn;

    private ProxyTimeColumn timeColumn;

    public ProxyRateColumn(ProxyDataModel dataModel, ProxySizeColumn size) {
        this.dataModel = dataModel;
        this.proxyModel = dataModel.getWBSModel();
        this.sizeColumn = size;
        this.columnName = resources.getString("Proxy_Rate.Name");
        this.columnID = COLUMN_ID;
        this.dependentColumns = new String[] { ProxySizeColumn.COLUMN_ID };
        setConflictAttributeName(ATTR_NAME);
    }

    public void setTimeColumn(ProxyTimeColumn timeColumn) {
        this.timeColumn = timeColumn;
    }

    @Override
    public Object getValueAt(WBSNode node) {
        if (!ProxySizeColumn.hasSizeMetric(node))
            return null;

        double value = node.getNumericAttribute(ATTR_NAME);
        if (value > 0)
            return new NumericDataValue(value);

        value = node.getNumericAttribute(CALC_ATTR_NAME);
        if (value > 0) {
            NumericDataValue result = new NumericDataValue(value);
            result.errorMessage = CALCULATED_TOOLTIP;
            return result;
        }

        value = node.getNumericAttribute(INHERITED_ATTR_NAME);
        if (value > 0) {
            NumericDataValue result = new NumericDataValue(value);
            result.errorMessage = INHERITED_TOOLTIP;
            return result;
        }

        return null;
    }

    public boolean isCellEditable(WBSNode node) {
        return node.getIndentLevel() > 0;
    }

    @Override
    public void setValueAt(Object aValue, WBSNode node) {
        if (isNoOpEdit(aValue, node))
            return;

        double value = NumericDataValue.parse(aValue);
        if (value == 0) {
            // null and "" parse to zero; delete the underlying value.
            node.removeAttribute(ATTR_NAME);

        } else if (value > 0) {
            // if a real rate was provided, store it.
            node.setNumericAttribute(ATTR_NAME, value);
            // if a time was manually entered for this row, clear it so the
            // time can be recalculated based on the new rate.
            timeColumn.setValueAt(null, node);
            // make certain this proxy table has size metrics enabled.
            sizeColumn.ensureSizeMetric(node);
        }

        // changes to this column mean we should recalculate the time column.
        dataModel.columnChanged(timeColumn);
    }

    public void storeDependentColumn(String ID, int columnNumber) {}

    public boolean recalculate() {
        return true;
    }

    void recalcInheritedRates() {
        // scan the list of proxy tables.
        WBSNode[] proxies = proxyModel.getChildren(proxyModel.getRoot());
        for (WBSNode proxy : proxies) {
            // determine whether this table has a size and a default rate.
            boolean hasSize = ProxySizeColumn.hasSizeMetric(proxy);
            double rate = hasSize ? proxy.getNumericAttribute(ATTR_NAME)
                    : Double.NaN;

            // Now scan the relative size buckets in this proxy table.
            WBSNode[] buckets = proxyModel.getChildren(proxy);
            for (WBSNode bucket : buckets) {
                // set the inherited rate for this bucket.
                bucket.setNumericAttribute(INHERITED_ATTR_NAME, rate);
                // if the explicit rate is equal to the inherited rate, remove
                // the explicit entry.
                double explicit = bucket.getNumericAttribute(ATTR_NAME);
                if (equal(rate, explicit))
                    bucket.removeAttribute(ATTR_NAME);
                // clear the "calculated" rate for now (it will be set later
                // in the recalculation process).
                bucket.removeAttribute(CALC_ATTR_NAME);
            }
        }
    }

    void storeCalculatedRate(WBSNode bucket, double rate) {
        bucket.removeAttribute(ATTR_NAME);
        bucket.setNumericAttribute(CALC_ATTR_NAME, rate);
    }


    public TableCellRenderer getCellRenderer() {
        return new ProxyRateRenderer();
    }

    private class ProxyRateRenderer extends ItalicNumericCellRenderer {

        public ProxyRateRenderer() {
            super(INHERITED_TOOLTIP, CALCULATED_TOOLTIP);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {

            Component result = super.getTableCellRendererComponent(table,
                value, isSelected, hasFocus, row, column);

            NumericDataValue ndv = (NumericDataValue) value;
            if (ndv != null && CALCULATED_TOOLTIP.equals(ndv.errorMessage)) {
                setHorizontalAlignment(SwingConstants.RIGHT);
            } else {
                setHorizontalAlignment(SwingConstants.CENTER);
            }

            return result;
        }

    }

    private static final String INHERITED_TOOLTIP = resources
            .getString("Inherited_Tooltip");
    private static final String CALCULATED_TOOLTIP = resources
            .getString("Proxy_Rate.Calculated_Tooltip");

}
