package teamdash.wbs.columns;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import teamdash.wbs.DataTableModel;
import teamdash.wbs.ReadOnlyValue;
import teamdash.wbs.WBSNode;

public class SizeTypeColumn extends AbstractDataColumn {

    private static final String ATTR_NAME = "Size Metric";
    static final Map SIZE_METRICS = buildMap();


    private DataTableModel dataModel;

    public SizeTypeColumn(DataTableModel m) {
        this.dataModel = m;
        this.columnID = "Size-Units";
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

    public static void createSizeColumns(DataTableModel dataModel) {
        // create the size type column.
        dataModel.addDataColumn(new SizeTypeColumn(dataModel));

        // create an editable size column.
        dataModel.addDataColumn(new EditableSizeColumn(dataModel));

        // create LOC accounting columns.
        SizeAccountingColumnSet.create(dataModel, "LOC", new LOCPruner(), null);

        // create size accounting columns for various document types.
        Iterator i = SIZE_METRICS.entrySet().iterator();
        Map.Entry e;
        while (i.hasNext()) {
            e = (Map.Entry) i.next();
            if ("LOC".equals(e.getValue())) continue;

            String docType = (String) e.getKey();
            String id = (String) e.getValue();

            SizeAccountingColumnSet.create
                (dataModel, id, new DocSizePruner(docType), null); //docType);
        }

        // create aliasing columns
        String[] sizeUnits = new String[] {
            "LOC","Text Pages", "Reqts Pages", "HLD Pages", "DLD Lines" };
        dataModel.addDataColumn(new SizeAliasColumn
            (dataModel, "Base", SizeAccountingColumnSet.getBaseID(""),
             sizeUnits));
        dataModel.addDataColumn(new SizeAliasColumn
            (dataModel, "Deleted", SizeAccountingColumnSet.getDeletedID(""),
             sizeUnits));
        dataModel.addDataColumn(new SizeAliasColumn
            (dataModel, "Modified", SizeAccountingColumnSet.getModifiedID(""),
             sizeUnits));
        dataModel.addDataColumn(new SizeAliasColumn
            (dataModel, "Added", SizeAccountingColumnSet.getAddedID(""),
             sizeUnits));
        dataModel.addDataColumn(new SizeAliasColumn
            (dataModel, "Reused", SizeAccountingColumnSet.getReusedID(""),
             sizeUnits));
        dataModel.addDataColumn(new SizeAliasColumn
            (dataModel, "N&C", SizeAccountingColumnSet.getNCID(""),
             sizeUnits));
        dataModel.addDataColumn(new SizeAliasColumn
            (dataModel, "Total", SizeAccountingColumnSet.getTotalID(""),
             sizeUnits));
    }


}
