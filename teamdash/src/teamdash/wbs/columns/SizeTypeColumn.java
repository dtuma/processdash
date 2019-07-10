// Copyright (C) 2002-2019 Tuma Solutions, LLC
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.processdash.util.VersionUtils;
import net.sourceforge.processdash.util.XMLUtils;

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.HtmlRenderedValue;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.ReadOnlyValue;
import teamdash.wbs.TeamProcess;
import teamdash.wbs.WBSModel;
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

    /** positions of columns that are holding planned size values */
    int[] sizeColumns;

    DataTableModel dataModel;

    public SizeTypeColumn(DataTableModel m, Map sizeMetrics) {
        this.dataModel = m;
        this.sizeMetrics = sizeMetrics;
        this.columnID = COLUMN_ID;
        this.columnName = resources.getString("Units.Name");
        this.dependentColumns = new String[] {
                SizeAccountingColumnSet.getNCID("LOC") };
        this.sizeColumns = new int[] { -1 };
    }


    @Override
    public void resetDependentColumns() {
        Arrays.fill(sizeColumns, -1);
    }

    public void storeDependentColumn(String ID, int columnNumber) {
        for (int i = sizeColumns.length; i-- > 0;) {
            if (ID.equals(dependentColumns[i]))
                sizeColumns[i] = columnNumber;
        }
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
        if (sizeColumns[0] == -1)
            return false;

        // If a nonzero LOC value is present, don't hide the LOC moniker
        Object locSizeVal = dataModel.getValueAt(node, sizeColumns[0]);
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



    private static class NewSizeTypeColumn extends SizeTypeColumn {

        /** names of known size metrics */
        private List<String> sizeMetricNames;

        private final Object BLANK = new ReadOnlyValue(
                new HtmlRenderedValue("LOC", ""));

        public NewSizeTypeColumn(DataTableModel m, Map sizeMetrics) {
            super(m, sizeMetrics);

            sizeMetricNames = new ArrayList(new HashSet(sizeMetrics.values()));
            sizeMetricNames.remove("LOC");
            sizeMetricNames.add(0, "LOC"); // make sure LOC is in position 0
            int numMetrics = sizeMetricNames.size();

            dependentColumns = new String[numMetrics];
            for (int i = numMetrics; i-- > 0;) {
                dependentColumns[i] = SizeDataColumn
                        .getColumnID(sizeMetricNames.get(i), true);
            }
            sizeColumns = new int[numMetrics];
            resetDependentColumns();
        }

        @Override
        public Object getValueAt(WBSNode node) {
            // don't display size units for the root node.
            String nodeType = node.getType();
            if ("Project".equals(nodeType))
                return BLANK;

            // if this isn't a 'generic component', defer to the old logic
            if (!"Component".equals(nodeType))
                return super.getValueAt(node);

            // special handling for 'generic components': if only one planned
            // size value has been entered, assume that metric; else null
            Object result = BLANK;
            for (int i = sizeColumns.length; i-- > 0;) {
                Object oneSize = dataModel.getValueAt(node, sizeColumns[i]);
                if (NumericDataValue.parse(oneSize) > 0) {
                    if (result == BLANK)
                        result = sizeMetricNames.get(i);
                    else
                        // more than one metric found: abort
                        return BLANK;
                }
            }
            return result;
        }

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



    /**
     * Return true if this WBS is using the new-style size columns (provided by
     * the SizeDataColumn class), false if it is using the old size accounting
     * columns.
     */
    public static boolean isUsingNewSizeDataColumns(WBSModel wbsModel) {
        WBSNode root = wbsModel.getRoot();
        String createdWithVersion = (String) root
                .getAttribute(WBSModel.CREATED_WITH_ATTR);
        if (XMLUtils.hasValue(createdWithVersion))
            return VersionUtils.compareVersions(MIN_NEW_SIZE_VERSION,
                createdWithVersion) <= 0;
        else
            return false;
    }

    private static final String MIN_NEW_SIZE_VERSION = "5.0.0";



    /** Create all of the required columns for size metrics, and add them
     * to the given data model.
     */
    public static void createSizeColumns(DataTableModel dataModel,
            TeamProcess teamProcess) {
        Map sizeMetrics = teamProcess.getWorkProductSizeMap();

        if (isUsingNewSizeDataColumns(dataModel.getWBSModel()))
            createNewSizeColumns(dataModel, teamProcess, sizeMetrics);
        else
            createOldSizeColumns(dataModel, teamProcess, sizeMetrics);
    }

    private static void createNewSizeColumns(DataTableModel dataModel,
            TeamProcess teamProcess, Map sizeMetrics) {

        // create the size type columns.
        dataModel.addDataColumn(new NewSizeTypeColumn(dataModel, sizeMetrics));
        dataModel.addDataColumn(new DirectSizeTypeColumn.Simple());

        Iterator i = new HashSet(sizeMetrics.values()).iterator();
        while (i.hasNext()) {
            String metric = (String) i.next();

            // add a planned size column for this metric
            dataModel.addDataColumn(
                new SizeDataColumn(dataModel, teamProcess, metric, true));

            // add an actual size column for this metric
            dataModel.addDataColumn(
                new SizeDataColumn(dataModel, teamProcess, metric, false));
        }
    }

    private static void createOldSizeColumns(DataTableModel dataModel,
            TeamProcess teamProcess, Map sizeMetrics) {

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
