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
import javax.swing.table.TableCellRenderer;

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.CustomRenderedColumn;
import teamdash.wbs.ItalicNumericCellRenderer;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.ProxyDataModel;
import teamdash.wbs.ProxyWBSModel;
import teamdash.wbs.WBSNode;

public class ProxyTimeColumn extends AbstractNumericColumn implements
        CalculatedDataColumn, CustomRenderedColumn {

    private static final String ATTR_NAME = "Proxy Time";

    private static final String RATE_CALC_ATTR_NAME = "_Rate_Calc " + ATTR_NAME;

    private static final String COLUMN_ID = ATTR_NAME;


    private ProxyDataModel dataModel;

    private ProxyWBSModel proxyModel;

    private ProxySizeColumn sizeColumn;

    private ProxyRateColumn rateColumn;

    public ProxyTimeColumn(ProxyDataModel dataModel,
            ProxySizeColumn sizeColumn, ProxyRateColumn rateColumn) {
        this.dataModel = dataModel;
        this.proxyModel = dataModel.getWBSModel();
        this.sizeColumn = sizeColumn;
        this.rateColumn = rateColumn;
        this.columnName = resources.getString("Proxy_Time.Name");
        this.columnID = COLUMN_ID;
        this.dependentColumns = new String[] { ProxySizeColumn.COLUMN_ID };
        setConflictAttributeName(ATTR_NAME);
    }

    @Override
    public Object getValueAt(WBSNode node) {
        if (!ProxyWBSModel.isBucket(node))
            return null;

        double value = node.getNumericAttribute(ATTR_NAME);
        if (value > 0)
            return new NumericDataValue(value);

        value = node.getNumericAttribute(RATE_CALC_ATTR_NAME);
        if (value > 0)
            return new NumericDataValue(value);

        return null;
    }

    public boolean isCellEditable(WBSNode node) {
        return ProxyWBSModel.isBucket(node);
    }

    @Override
    public void setValueAt(Object aValue, WBSNode node) {
        if (isNoOpEdit(aValue, node))
            return;

        double value = NumericDataValue.parse(aValue);
        if (value == 0) {
            // if the value was null, "", or zero, clear this row's time.
            node.removeAttribute(ATTR_NAME);

        } else if (value > 0) {
            // if a real value was entered for this cell, save it.
            node.setNumericAttribute(ATTR_NAME, value);
            // clear any explicit rate that was previously entered for this row.
            rateColumn.setValueAt(null, node);
        }

        // changes to this column mean we should recalculate the rate column.
        dataModel.columnChanged(rateColumn);
    }

    public void storeDependentColumn(String ID, int columnNumber) {}

    public boolean recalculate() {
        // give the rate column a chance to record state of inherited values
        rateColumn.recalcInheritedRates();

        // recalculate data for each proxy category, one at a time.
        WBSNode[] proxies = proxyModel.getChildren(proxyModel.getRoot());
        for (WBSNode proxy : proxies)
            recalculateProxy(proxy);

        return true;
    }

    private void recalculateProxy(WBSNode proxy) {
        WBSNode[] buckets = proxyModel.getChildren(proxy);
        boolean proxyHasSizeMetric = ProxySizeColumn.hasSizeMetric(proxy);

        // scan the buckets and calculate rate-driven times where appropriate
        for (WBSNode bucket : buckets) {
            bucket.removeAttribute(RATE_CALC_ATTR_NAME);
            if (proxyHasSizeMetric) {
                double explicitTime = bucket.getNumericAttribute(ATTR_NAME);
                if (!(explicitTime > 0)) {
                    double size = parseNum(sizeColumn.getValueAt(bucket));
                    double rate = parseNum(rateColumn.getValueAt(bucket));
                    if (size > 0 && rate > 0) {
                        double time = size / rate;
                        bucket.setNumericAttribute(RATE_CALC_ATTR_NAME, time);
                    }
                }
            }
        }

        // TODO: fill in missing cells with an extrapolation

        // finally, calculate effective rates for applicable buckets
        if (proxyHasSizeMetric) {
            for (WBSNode bucket : buckets) {
                double time = bucket.getNumericAttribute(ATTR_NAME);
                double size = parseNum(sizeColumn.getValueAt(bucket));
                if (time > 0 && size > 0) {
                    double rate = size / time;
                    rateColumn.storeCalculatedRate(bucket, rate);
                }
            }
        }
    }

    private static double parseNum(Object value) {
        return NumericDataValue.parse(value);
    }

    public TableCellRenderer getCellRenderer() {
        return new ProxyTimeRenderer();
    }

    private class ProxyTimeRenderer extends ItalicNumericCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {

            Component result = super.getTableCellRendererComponent(table,
                value, isSelected, hasFocus, row, column);

            return result;
        }

    }

}
