package teamdash.process;

import java.awt.Color;
import java.awt.Component;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import teamdash.process.CustomProcess.Item;

public abstract class ItemListTableModel extends AbstractTableModel {

    private CustomProcess process;
    private String itemType;
    private List itemList;
    private Set insertedItems;
    private boolean isDirty = false;
    private boolean structureChanged = false;

    public ItemListTableModel(CustomProcess p, String itemType) {
        this.itemType = itemType;
        setProcess(p);
    }

    public void setProcess(CustomProcess p) {
        this.process = p;
        this.itemList = p.getItemList(itemType);
        this.insertedItems = new HashSet();
        isDirty = false;
        fireTableDataChanged();
    }

    public CustomProcess getProcess() {
        return process;
    }

    public abstract void insertItem(int pos);

    protected void insertItem(Item newPhase, int pos) {
        insertedItems.add(newPhase);
        pos = Math.min(pos, itemList.size());
        itemList.add(pos, newPhase);
        isDirty = true;
        fireTableRowsInserted(pos, pos);
    }

    public void deleteItem(int pos) {
        if (pos >= 0 && pos < itemList.size()) {
            Item deletedItem = get(pos);
            itemList.remove(pos);
            isDirty = true;
            if (!insertedItems.remove(deletedItem))
                structureChanged = true;
            fireTableRowsDeleted(pos, pos);
        }
    }

    public void moveItemUp(int pos) {
        if (pos > 0 && pos < itemList.size()) {
            Object temp = itemList.get(pos);
            itemList.set(pos, itemList.get(pos-1));
            itemList.set(pos-1, temp);
            isDirty = true;
            fireTableRowsUpdated(pos-1, pos);
        }
    }
    public Item get(int pos) {
        return (Item) itemList.get(pos);
    }
    public boolean isDirty() {
        return isDirty;
    }
    public void clearDirty() {
        this.isDirty = false;
    }
    public boolean isStructureChanged() {
        return structureChanged;
    }
    public abstract JTable createJTable();


    // TableModel interface methods

    protected abstract String[] getColumnAttrs();
    protected abstract boolean isStructuralColumn(int column);

    public int getRowCount() { return itemList.size(); }
    public int getRealRowCount() { return itemList.size(); }
    public int getColumnCount() { return getColumnAttrs().length; }
    public Class getColumnClass(int columnIndex) { return String.class; }
    public Object getValueAt(int row, int column) {
        Item phase = get(row);
        String attrName = getColumnAttrs()[column];
        return phase.getAttr(attrName);
    }
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return !get(rowIndex).getAttributes().containsKey("readOnly");
    }
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (!isCellEditable(rowIndex, columnIndex)) return;

        Item item = get(rowIndex);

        String value = null;
        if (aValue != null) {
            value = CustomProcess.makeSafe(String.valueOf(aValue));
            if (value.indexOf('/') != -1) return;
        }

        if (isStructuralColumn(columnIndex) && !insertedItems.contains(item))
            structureChanged = true;

        String attrName = getColumnAttrs()[columnIndex];
        if (!eq(item.getAttr(attrName), value)) {
            item.putAttr(attrName, value);
            isDirty = true;
        }
    }
    private boolean eq(Object a, Object b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    protected boolean isStructuralCell(int row, int col) {
        return (isStructuralColumn(col) && !insertedItems.contains(get(row)));
    }

    protected Object getItemDisplay(int column, Object value) {
        return value;
    }



    protected class ItemTableCellRenderer extends DefaultTableCellRenderer {

        private Color structuralColor = new Color(255, 230, 200);

        public Component getTableCellRendererComponent
            (JTable table, Object value, boolean isSelected,
             boolean hasFocus, int row, int col)
        {
            setBackground(null);
            value = getItemDisplay(col, value);

            Component result = super.getTableCellRendererComponent
                (table, value, isSelected, hasFocus, row, col);

            String tooltip = null;
            if (table.getModel().isCellEditable(row, col)) {
                result.setForeground(Color.black);
                if (isStructuralCell(row, col)) {
                    result.setBackground(structuralColor);
                    tooltip = "<html><body>"
                            + "Changes to this value will result in a new<br>"
                            + "metrics collection framework that is not<br>"
                            + "backward-compatible with existing project<br>"
                            + "data.</body></html>";
                }
            } else {
                result.setForeground(Color.gray);
                tooltip = "This item is read-only";
            }


            if (result instanceof JComponent)
                ((JComponent) result).setToolTipText(tooltip);

            return result;
        }
    }
}
