package teamdash.wbs.columns;

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.WBSNode;

public class SizeAccountingColumnSet {

    public static void create(DataTableModel m, String id, Pruner p)
    {
        // create "Base" column
        m.addDataColumn(new TopDownBottomUpColumn
                            (m, getBaseID(id), getBaseID(id), p));

        // create "Deleted" column
        m.addDataColumn(new TopDownBottomUpColumn
                            (m, getDeletedID(id), getDeletedID(id), p));

        // create "Modified" column
        m.addDataColumn(new TopDownBottomUpColumn
                            (m, getModifiedID(id), getModifiedID(id), p));

        // create "Added" column
        m.addDataColumn(new TopDownBottomUpColumn
                            (m, getAddedID(id), getAddedID(id), p));

        // create "Reused" column
        m.addDataColumn(new TopDownBottomUpColumn
                            (m, getReusedID(id), getReusedID(id), p));

        // create New & Changed
        m.addDataColumn(new NewChangedSizeColumn(m, id));

        // create Total
        m.addDataColumn(new TotalSizeColumn(m, id));
    }

    public static String getBaseID(String id)     { return "Base-"     + id; }
    public static String getDeletedID(String id)  { return "Deleted-"  + id; }
    public static String getModifiedID(String id) { return "Modified-" + id; }
    public static String getAddedID(String id)    { return "Added-"    + id; }
    public static String getReusedID(String id)   { return "Reused-"   + id; }
    public static String getNCID(String id)       { return "N&C-"      + id; }
    public static String getTotalID(String id)    { return "Total-"    + id; }



    private static class NewChangedSizeColumn extends AbstractNumericColumn
        implements CalculatedDataColumn
    {
        DataTableModel dataModel;
        String addID, modID;
        int addedColumn, modifiedColumn;

        public NewChangedSizeColumn(DataTableModel m, String id) {
            this.dataModel = m;
            this.columnName = this.columnID = getNCID(id);
            this.addID = getAddedID(id);
            this.modID = getModifiedID(id);
            this.dependentColumns = new String[] { addID, modID };
        }

        public boolean recalculate() { return true; }

        public void storeDependentColumn(String ID, int columnNumber) {
            if (addID.equals(ID)) addedColumn = columnNumber;
            else if (modID.equals(ID)) modifiedColumn = columnNumber;
        }

        public boolean isCellEditable(WBSNode node) {
            return dataModel.isCellEditable(node,addedColumn);
        }

        protected double getValueForNode(WBSNode node) {
            return parseValue(dataModel.getValueAt(node, addedColumn)) +
                parseValue(dataModel.getValueAt(node, modifiedColumn));
        }

        protected void setValueForNode(double value, WBSNode node) {
            if (Double.isNaN(value) || value < 0) return;

            double nc = value, added, modified;
            added = parseValue(dataModel.getValueAt(node, addedColumn));
            modified = parseValue(dataModel.getValueAt(node, modifiedColumn));

            if (nc > modified) {
                // when the user edit "new & changed", try to accomodate them
                // by altering the "added" value.
                added = nc - modified;
                dataModel.setValueAt(new Double(added), node, addedColumn);
            } else {
                // if the strategy above would result in a negative value for
                // "added", then zero out added and decrease the modified value
                dataModel.setValueAt(new Double(0),  node, addedColumn);
                dataModel.setValueAt(new Double(nc), node, modifiedColumn);
            }
        }
    }

    private static class TotalSizeColumn extends AbstractNumericColumn
        implements CalculatedDataColumn
    {
        DataTableModel dataModel;
        String baseID;    int baseColumn;
        String deletedID; int deletedColumn;
        String addedID;   int addedColumn;
        String reusedID;  int reusedColumn;

        public TotalSizeColumn(DataTableModel m, String id) {
            this.dataModel = m;
            this.columnName = this.columnID = getTotalID(id);
            baseID = getBaseID(id);
            deletedID = getDeletedID(id);
            addedID = getAddedID(id);
            reusedID = getReusedID(id);
            this.dependentColumns = new String[] {
                baseID, deletedID, addedID, reusedID };
        }

        public boolean recalculate() { return true; }

        public void storeDependentColumn(String ID, int columnNumber) {
            if (ID == null) return;
            if      (ID.equals(baseID))     baseColumn    = columnNumber;
            else if (ID.equals(deletedID))  deletedColumn = columnNumber;
            else if (ID.equals(addedID))    addedColumn   = columnNumber;
            else if (ID.equals(reusedID))   reusedColumn  = columnNumber;
        }

        public boolean isCellEditable(WBSNode node) { return false; }

        private double getVal(WBSNode node, int column) {
            return parseValue(dataModel.getValueAt(node, column));
        }

        protected double getValueForNode(WBSNode node) {
            double base    = getVal(node, baseColumn);
            double deleted = getVal(node, deletedColumn);
            double added   = getVal(node, addedColumn);
            double reused  = getVal(node, reusedColumn);
            return base - deleted + added + reused;
        }

        protected void setValueForNode(double value, WBSNode node) { }
    }
}
