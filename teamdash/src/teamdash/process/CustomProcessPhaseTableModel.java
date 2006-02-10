package teamdash.process;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.table.AbstractTableModel;

import teamdash.process.CustomProcess.Item;

public class CustomProcessPhaseTableModel extends AbstractTableModel {

        private static final String PHASE_ITEM = CustomProcess.PHASE_ITEM;
        private static final String LONG_NAME = CustomProcess.LONG_NAME;
        private static final String NAME = CustomProcess.NAME;
        private static final String TYPE = CustomProcess.TYPE;

        private CustomProcess customProcess;
        private List phaseList;
        private Set insertedPhases;
        private boolean isDirty = false;
        private boolean structureChanged = false;

        public CustomProcessPhaseTableModel(CustomProcess p) {
                this.customProcess = p;
                this.phaseList = p.getItemList(PHASE_ITEM);
                this.insertedPhases = new HashSet();
        }


    public void insertPhase(int pos) {
        Item newPhase = customProcess.new Item(PHASE_ITEM);
        newPhase.putAttr(LONG_NAME, "Enter Phase Name");
        newPhase.putAttr(NAME, "Short Name");
        newPhase.putAttr(TYPE, "MGMT");
        insertedPhases.add(newPhase);
        phaseList.add(pos, newPhase);
        isDirty = true;
        fireTableRowsInserted(pos, pos);
    }

    public void deletePhase(int pos) {
        if (pos >= 0 && pos < phaseList.size()) {
            Item deletedPhase = get(pos);
            phaseList.remove(pos);
            isDirty = true;
            if (!insertedPhases.remove(deletedPhase))
                structureChanged = true;
            fireTableRowsDeleted(pos, pos);
        }
    }

    public void movePhaseUp(int pos) {
        if (pos > 0 && pos < phaseList.size()) {
            Object temp = phaseList.get(pos);
            phaseList.set(pos, phaseList.get(pos-1));
            phaseList.set(pos-1, temp);
            isDirty = true;
            fireTableRowsUpdated(pos-1, pos);
        }
    }
    public Item get(int pos) {
        return (Item) phaseList.get(pos);
    }
    public boolean isDirty() {
        return isDirty;
    }
    public boolean isStructureChanged() {
        return structureChanged;
    }

    // TableModel interface methods

    private static final String[] columnNames = {
        "Descriptive Name", "Short Name", "Type" };

    public int getRowCount() { return phaseList.size(); }
    public int getColumnCount() { return columnNames.length; }
    public Class getColumnClass(int columnIndex) { return String.class; }
    public String getColumnName(int col) { return columnNames[col]; }
    public Object getValueAt(int row, int column) {
        Item phase = get(row);
        switch (column) {
        case 0: return phase.getAttr(LONG_NAME);
        case 1: return phase.getAttr(NAME);
        case 2: default: return phase.getAttr(TYPE);
        }
    }
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return !get(rowIndex).getAttributes().containsKey("readOnly");
    }
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        Item phase = get(rowIndex);
        if (phase.getAttributes().containsKey("readOnly")) return;

        String value = CustomProcess.makeSafe(String.valueOf(aValue));
        if (value.indexOf('/') != -1) return;

        switch (columnIndex) {
        case 0: phase.putAttr(LONG_NAME, value); break;
        case 1: phase.putAttr(NAME, value); structureChanged = true; break;
        case 2: phase.putAttr(TYPE, value); break;
        }
        isDirty = true;
    }
}
