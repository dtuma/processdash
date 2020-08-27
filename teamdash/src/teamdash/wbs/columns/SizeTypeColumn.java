// Copyright (C) 2002-2020 Tuma Solutions, LLC
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
import teamdash.wbs.HtmlRenderedValue;
import teamdash.wbs.NumericDataValue;
import teamdash.wbs.ReadOnlyValue;
import teamdash.wbs.SizeMetric;
import teamdash.wbs.TeamProcess;
import teamdash.wbs.TeamProject;
import teamdash.wbs.WBSDataModel;
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
    private Map<String, String> workProductSizeMap;

    Map<String, SizeMetric> sizeMetrics;

    WBSDataModel dataModel;

    public SizeTypeColumn(WBSDataModel m) {
        this.dataModel = m;
        this.workProductSizeMap = m.getTeamProcess().getWorkProductSizeMap();
        this.sizeMetrics = m.getTeamProcess().getSizeMetricMap();
        this.columnID = COLUMN_ID;
        this.columnName = resources.getString("Units.Name");
        this.dependentColumns = new String[] {
                SizeAccountingColumnSet.getNCID("LOC") };
    }


    public void storeDependentColumn(String ID, int columnNumber) {}

    public boolean recalculate() {
        return false;
    }

    public boolean isCellEditable(WBSNode node) {
        return workProductSizeMap.get(node.getType()) == null
                && !TeamProcess.isProbeTask(node.getType());
    }


    public Object getValueAt(WBSNode node) {
        String metricID = getWorkProductSizeMetric(node, workProductSizeMap);
        if (metricID == null) {
            metricID = (String) node.getAttribute(ATTR_NAME);
            return sizeMetrics.get(metricID);
        } else {
            Object result = sizeMetrics.get(metricID);
            if (shouldHide(node.getType(), metricID, node))
                result = new HtmlRenderedValue(result, "");
            return new ReadOnlyValue(result);
        }
    }


    private boolean shouldHide(String type, String metricID, WBSNode node) {
        // only hide the "LOC" size units moniker
        if (!"LOC".equals(metricID))
            return false;

        // only hide LOC for the project and the generic "Component" type
        if (!"Project".equals(type) && !"Component".equals(type))
            return false;

        // If a nonzero LOC value is present, don't hide the LOC moniker
        if (getPlannedSize(node, "LOC") > 0)
            return false;

        // we apparently have a "Project" or "Component" type with no real
        // LOC attached. Hide the "LOC" moniker.  This reduces confusion for
        // non-software teams who don't understand LOC, but who constantly
        // make use of the generic "Component" type.
        return true;
    }


    protected double getPlannedSize(WBSNode node, String metricID) {
        int planSizeColumn = dataModel.getSizeColumnIndexes(true).get(metricID);
        if (planSizeColumn == -1)
            return 0;
        Object planSizeVal = dataModel.getValueAt(node, planSizeColumn);
        return NumericDataValue.parse(planSizeVal);
    }


    public void setValueAt(Object aValue, WBSNode node) {
        String metricID = null;
        if (aValue instanceof String)
            metricID = (String) aValue;
        else if (aValue instanceof SizeMetric)
            metricID = ((SizeMetric) aValue).getMetricID();
        node.setAttribute(ATTR_NAME, metricID);
    }



    private static class NewSizeTypeColumn extends SizeTypeColumn {

        private final Object BLANK = new ReadOnlyValue(
                new HtmlRenderedValue(new SizeMetric("LOC", "LOC"), ""));

        public NewSizeTypeColumn(WBSDataModel m) {
            super(m);
            this.dependentColumns = new String[] { SizeColumnGroup.PLAN };
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
            for (SizeMetric metric : sizeMetrics.values()) {
                double oneSize = getPlannedSize(node, metric.getMetricID());
                if (oneSize > 0) {
                    if (result == BLANK)
                        result = metric;
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
            Map<String, String> workProductSizeMap) {
        String nodeType = node.getType();
        if (TeamProcess.isProbeTask(nodeType)) {
            return TaskSizeUnitsColumn.getSizeUnitsForProbeTask(node);
        } else {
            return workProductSizeMap.get(nodeType);
        }
    }



    /**
     * Set the new-style size flag on the given WBS, on behalf of older versions
     * of the WBS Editor that might inadvertently open this project.
     */
    public static void enableNewSizeDataColumns(WBSModel wbs) {
        wbs.getRoot().setAttribute(NEW_SIZE_ATTR_NAME, "true");
    }

    /**
     * @deprecated
     */
    public static boolean isUsingNewSizeDataColumns(WBSModel wbsModel) {
        return true;
    }

    private static final String NEW_SIZE_ATTR_NAME = "WBS Managed Size Data";



    /** Create all of the required columns for size metrics, and add them
     * to the given data model.
     */
    public static void createSizeColumns(WBSDataModel dataModel,
            TeamProcess teamProcess) {
        Map workProductSizeMap = teamProcess.getWorkProductSizeMap();

        if (isUsingNewSizeDataColumns(dataModel.getWBSModel()))
            createNewSizeColumns(dataModel);
        else
            createOldSizeColumns(dataModel, teamProcess, workProductSizeMap);

        dataModel.addDataColumn(new SizeColumnGroup(true));
        dataModel.addDataColumn(new SizeColumnGroup(false));
    }

    private static void createNewSizeColumns(WBSDataModel dataModel) {
        // create the size type columns.
        dataModel.addDataColumn(new NewSizeTypeColumn(dataModel));
        dataModel.addDataColumn(new DirectSizeTypeColumn.Simple(
                dataModel.getTeamProcess().getSizeMetricMap()));
        dataModel.addDataColumn(new SizeOwnerColumn(dataModel));
    }

    private static void createOldSizeColumns(WBSDataModel dataModel,
            TeamProcess teamProcess, Map sizeMetrics) {

        // create the size type columns.
        dataModel.addDataColumn(new SizeTypeColumn(dataModel));
        dataModel.addDataColumn(new DirectSizeTypeColumn(dataModel));

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
