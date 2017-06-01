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

package teamdash.wbs.columns;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.HtmlRenderedValue;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.ReadOnlyValue;
import teamdash.wbs.TeamProcess;
import teamdash.wbs.WBSNode;


/** This class performs two purposes:<ul>
 * <li>It displays the unit of size measurement appropriate for a node
 *     in a work breakdown structure.
 * <li>It contains static methods for creating all necessary
 *     size-related columns
 * </ul>
 */
public class SizeTypeColumn extends AbstractDataColumn implements
        CalculatedDataColumn {

    /** The ID we use for this column in the data model */
    public static final String COLUMN_ID = "Size-Units";

    /** The attribute this column uses to store its data on WBS nodes */
    private static final String ATTR_NAME = "Size Metric";

    /** Maps node types to related size units */
    private Map sizeMetrics;

    /** the column that is holding the new & changed LOC value */
    private int locSizeColumn = -1;

    private DataTableModel dataModel;

    public SizeTypeColumn(DataTableModel m, Map sizeMetrics) {
        this.dataModel = m;
        this.sizeMetrics = sizeMetrics;
        this.columnID = COLUMN_ID;
        this.columnName = resources.getString("Units.Name");
        this.dependentColumns = new String[] {
                SizeAccountingColumnSet.getNCID("LOC") };
    }


    @Override
    public void resetDependentColumns() {
        locSizeColumn = -1;
    }

    public void storeDependentColumn(String ID, int columnNumber) {
        locSizeColumn = columnNumber;
    }

    public boolean recalculate() {
        return false;
    }

    public boolean isCellEditable(WBSNode node) {
        return sizeMetrics.get(node.getType()) == null
                && !TeamProcess.isProbeTask(node.getType());
    }


    public Object getValueAt(WBSNode node) {
        Object result = getWorkProductSizeMetric(node, sizeMetrics);
        if (result == null)
            result = node.getAttribute(ATTR_NAME);
        else {
            if (shouldHide(node.getType(), result, node))
                result = new HtmlRenderedValue(result, "");
            result = new ReadOnlyValue(result);
        }
        return result;
    }


    private boolean shouldHide(String type, Object units, WBSNode node) {
        // only hide the "LOC" size units moniker
        if (!"LOC".equals(units))
            return false;

        // only hide LOC for the project and the generic "Component" type
        if (!"Project".equals(type) && !"Component".equals(type))
            return false;

        // if we don't know the position of the LOC size column, abort.
        if (locSizeColumn == -1)
            return false;

        // If a nonzero LOC value is present, don't hide the LOC moniker
        Object locSizeVal = dataModel.getValueAt(node, locSizeColumn);
        double locSize = NumericDataValue.parse(locSizeVal);
        if (locSize > 0)
            return false;

        // we apparently have a "Project" or "Component" type with no real
        // LOC attached. Hide the "LOC" moniker.  This reduces confusion for
        // non-software teams who don't understand LOC, but who constantly
        // make use of the generic "Component" type.
        return true;
    }


    public void setValueAt(Object aValue, WBSNode node) {
        node.setAttribute(ATTR_NAME, aValue);
    }


    protected DataTableModel getDataTableModel() {
        return dataModel;
    }



    public static String getWorkProductSizeMetric(WBSNode node,
            TeamProcess process) {
        return getWorkProductSizeMetric(node, process.getWorkProductSizeMap());
    }

    public static String getWorkProductSizeMetric(WBSNode node,
            Map workProductSizeMap) {
        String nodeType = node.getType();
        if (TeamProcess.isProbeTask(nodeType)) {
            return TaskSizeUnitsColumn.getSizeUnitsForProbeTask(node);
        } else {
            return (String) workProductSizeMap.get(nodeType);
        }
    }



    /** Create all of the required columns for size metrics, and add them
     * to the given data model.
     */
    public static void createSizeColumns(DataTableModel dataModel,
            TeamProcess teamProcess) {
        Map sizeMetrics = teamProcess.getWorkProductSizeMap();

        // create the size type columns.
        dataModel.addDataColumn(new SizeTypeColumn(dataModel, sizeMetrics));
        dataModel.addDataColumn(new DirectSizeTypeColumn(dataModel, sizeMetrics));

        // create an editable size column.
        dataModel.addDataColumn(new EditableSizeColumn(dataModel, teamProcess));

        // create LOC accounting columns.
        SizeAccountingColumnSet.create(dataModel, "LOC",
                new WorkProductSizePruner(teamProcess, Collections
                        .singleton("LOC")), null, null);

        // create size accounting columns for various non-LOC size metrics.
        Iterator i = sizeMetrics.entrySet().iterator();
        Map.Entry e;
        while (i.hasNext()) {
            e = (Map.Entry) i.next();
            String objType = (String) e.getKey();
            String metric = (String) e.getValue();

            // add columns for plan/actual size data
            dataModel.addDataColumn(new SizeActualDataColumn(dataModel, metric,
                    true));
            dataModel.addDataColumn(new SizeActualDataColumn(dataModel, metric,
                    false));

            if ("LOC".equals(metric)) continue;

            Pruner pruner = new WorkProductSizePruner(teamProcess, Arrays
                    .asList(new Object[] { "LOC", metric }));
            SizeAccountingColumnSet.create(dataModel, metric, pruner, objType,
                metric);
        }

        // create aliasing columns
        String[] sizeUnits = teamProcess.getSizeMetrics();
        dataModel.addDataColumn(new SizeAliasColumn( //
            dataModel, "Base", "Size_Accounting.Base.Name",
            SizeAccountingColumnSet.getBaseID(""), sizeUnits, sizeMetrics));
        dataModel.addDataColumn(new SizeAliasColumn( //
            dataModel, "Deleted", "Size_Accounting.Deleted.Name",
            SizeAccountingColumnSet.getDeletedID(""), sizeUnits, sizeMetrics));
        dataModel.addDataColumn(new SizeAliasColumn( //
            dataModel, "Modified", "Size_Accounting.Modified.Name",
            SizeAccountingColumnSet.getModifiedID(""), sizeUnits, sizeMetrics));
        dataModel.addDataColumn(new SizeAliasColumn( //
            dataModel, "Added", "Size_Accounting.Added.Name",
            SizeAccountingColumnSet.getAddedID(""), sizeUnits, sizeMetrics));
        dataModel.addDataColumn(new SizeAliasColumn( //
            dataModel, "Reused", "Size_Accounting.Reused.Name",
            SizeAccountingColumnSet.getReusedID(""), sizeUnits, sizeMetrics));
        dataModel.addDataColumn(new SizeAliasColumn( //
            dataModel, "N&C", "Size_Accounting.New_and_Changed.Name",
            SizeAccountingColumnSet.getNCID(""), sizeUnits, sizeMetrics));
        dataModel.addDataColumn(new SizeAliasColumn( //
            dataModel, "Total", "Size_Accounting.Total.Name",
            SizeAccountingColumnSet.getTotalID(""), sizeUnits, sizeMetrics));
    }
}
