package teamdash.wbs.columns;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import teamdash.wbs.DataTableModel;
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
public class SizeTypeColumn extends AbstractDataColumn {

    /** The ID we use for this column in the data model */
    static final String COLUMN_ID = "Size-Units";

    /** The attribute this column uses to store its data on WBS nodes */
    private static final String ATTR_NAME = "Size Metric";

    /** Maps node types to related size units */
    private Map sizeMetrics;


    private DataTableModel dataModel;

    public SizeTypeColumn(DataTableModel m, Map sizeMetrics) {
        this.dataModel = m;
        this.sizeMetrics = sizeMetrics;
        this.columnID = COLUMN_ID;
        this.columnName = "Units";
    }


    public boolean isCellEditable(WBSNode node) {
        return sizeMetrics.get(node.getType()) == null;
    }


    public Object getValueAt(WBSNode node) {
        Object result = sizeMetrics.get(node.getType());
        if (result == null)
            result = node.getAttribute(ATTR_NAME);
        else
            result = new ReadOnlyValue(result);
        return result;
    }


    public void setValueAt(Object aValue, WBSNode node) {
        node.setAttribute(ATTR_NAME, aValue);
    }


    protected DataTableModel getDataTableModel() {
        return dataModel;
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
                        .singleton("LOC")), null);

        // create size accounting columns for various non-LOC size metrics.
        Iterator i = sizeMetrics.entrySet().iterator();
        Map.Entry e;
        while (i.hasNext()) {
            e = (Map.Entry) i.next();
            String objType = (String) e.getKey();
            String metric = (String) e.getValue();

            if ("LOC".equals(metric)) continue;

            Pruner pruner = new WorkProductSizePruner(teamProcess, Arrays
                    .asList(new Object[] { "LOC", metric }));
            SizeAccountingColumnSet.create(dataModel, metric, pruner, objType);
        }

        // create aliasing columns
        String[] sizeUnits = teamProcess.getSizeMetrics();
        dataModel.addDataColumn(new SizeAliasColumn
            (dataModel, "Base", SizeAccountingColumnSet.getBaseID(""),
             sizeUnits, sizeMetrics));
        dataModel.addDataColumn(new SizeAliasColumn
            (dataModel, "Deleted", SizeAccountingColumnSet.getDeletedID(""),
             sizeUnits, sizeMetrics));
        dataModel.addDataColumn(new SizeAliasColumn
            (dataModel, "Modified", SizeAccountingColumnSet.getModifiedID(""),
             sizeUnits, sizeMetrics));
        dataModel.addDataColumn(new SizeAliasColumn
            (dataModel, "Added", SizeAccountingColumnSet.getAddedID(""),
             sizeUnits, sizeMetrics));
        dataModel.addDataColumn(new SizeAliasColumn
            (dataModel, "Reused", SizeAccountingColumnSet.getReusedID(""),
             sizeUnits, sizeMetrics));
        dataModel.addDataColumn(new SizeAliasColumn
            (dataModel, "N&C", SizeAccountingColumnSet.getNCID(""),
             sizeUnits, sizeMetrics));
        dataModel.addDataColumn(new SizeAliasColumn
            (dataModel, "Total", SizeAccountingColumnSet.getTotalID(""),
             sizeUnits, sizeMetrics));
    }
}
