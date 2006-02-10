package teamdash.wbs.columns;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import teamdash.wbs.DataTableModel;
import teamdash.wbs.ReadOnlyValue;
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

    /** A list of the defined size metrics.  (This is hardcoded for
     * now, and must agree with the list in teamdash.wbs.TeamProcess) */
    public static final String[] SIZE_UNITS = new String[] {
            "LOC", "Text Pages", "Reqts Pages", "HLD Pages", "DLD Lines" };

    /** Maps node types to related size units */
    static final Map SIZE_METRICS = buildMap();


    private DataTableModel dataModel;

    public SizeTypeColumn(DataTableModel m) {
        this.dataModel = m;
        this.columnID = COLUMN_ID;
        this.columnName = "Units";
    }


    public boolean isCellEditable(WBSNode node) {
        return SIZE_METRICS.get(node.getType()) == null;
    }


    public Object getValueAt(WBSNode node) {
        Object result = SIZE_METRICS.get(node.getType());
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



        private static Map buildMap() {
        Map sizeMetrics = new HashMap();
        sizeMetrics.put("Project", "LOC");
        sizeMetrics.put("Software Component", "LOC");
        sizeMetrics.put("PSP Task", "LOC");
        sizeMetrics.put("General Document", "Text Pages");
        sizeMetrics.put("Requirements Document", "Reqts Pages");
        sizeMetrics.put("High Level Design Document", "HLD Pages");
        sizeMetrics.put("Detailed Design Document", "DLD Lines");
        return Collections.unmodifiableMap(sizeMetrics);
    }



    /** Create all of the required columns for size metrics, and add them
     * to the given data model.
     */
    public static void createSizeColumns(DataTableModel dataModel) {
        // create the size type columns.
        dataModel.addDataColumn(new SizeTypeColumn(dataModel));
        dataModel.addDataColumn(new DirectSizeTypeColumn(dataModel));

        // create an editable size column.
        dataModel.addDataColumn(new EditableSizeColumn(dataModel));

        // create LOC accounting columns.
        SizeAccountingColumnSet.create
            (dataModel, "LOC", new LOCPruner(), null);

        // create size accounting columns for various document types.
        Iterator i = SIZE_METRICS.entrySet().iterator();
        Map.Entry e;
        while (i.hasNext()) {
            e = (Map.Entry) i.next();
            if ("LOC".equals(e.getValue())) continue;

            String docType = (String) e.getKey();
            String id = (String) e.getValue();

            SizeAccountingColumnSet.create
                (dataModel, id, new DocSizePruner(docType), docType); //null);
        }

        // create aliasing columns
        dataModel.addDataColumn(new SizeAliasColumn
            (dataModel, "Base", SizeAccountingColumnSet.getBaseID(""),
             SIZE_UNITS));
        dataModel.addDataColumn(new SizeAliasColumn
            (dataModel, "Deleted", SizeAccountingColumnSet.getDeletedID(""),
             SIZE_UNITS));
        dataModel.addDataColumn(new SizeAliasColumn
            (dataModel, "Modified", SizeAccountingColumnSet.getModifiedID(""),
             SIZE_UNITS));
        dataModel.addDataColumn(new SizeAliasColumn
            (dataModel, "Added", SizeAccountingColumnSet.getAddedID(""),
             SIZE_UNITS));
        dataModel.addDataColumn(new SizeAliasColumn
            (dataModel, "Reused", SizeAccountingColumnSet.getReusedID(""),
             SIZE_UNITS));
        dataModel.addDataColumn(new SizeAliasColumn
            (dataModel, "N&C", SizeAccountingColumnSet.getNCID(""),
             SIZE_UNITS));
        dataModel.addDataColumn(new SizeAliasColumn
            (dataModel, "Total", SizeAccountingColumnSet.getTotalID(""),
             SIZE_UNITS));
    }
}
