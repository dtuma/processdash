package teamdash.wbs.columns;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.Timer;
import javax.swing.table.TableCellEditor;

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.CustomEditedColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.TeamProcess;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;

public class PhaseColumn extends AbstractDataColumn
    implements CalculatedDataColumn, CustomEditedColumn
{
    private WBSModel wbsModel;
    private Set allowedTypes;
    private TableCellEditor cellEditor;

    public PhaseColumn(DataTableModel dataModel, TeamProcess teamProcess) {
        this.wbsModel = dataModel.getWBSModel();
        this.columnID = this.columnName = "Phase";
        this.preferredWidth = 100;
        buildEditor(teamProcess);
    }

    public Object getValueAt(WBSNode node) {
        String type = node.getType();
        if (allowedTypes.contains(type))
            return type;
        else
            return null;
    }


    public boolean isCellEditable(WBSNode node) {
        return node != wbsModel.getRoot() && node.isReadOnly() == false;
    }

    public void setValueAt(Object aValue, WBSNode node) {
        if (aValue instanceof String) {
            String s = (String) aValue;
            if (allowedTypes.contains(s))
                changeNodeType(node, s);
            else if (allowedTypes.contains(s + " Task"))
                changeNodeType(node, s + " Task");
        }
    }

    private void changeNodeType(WBSNode node, String type) {
        // if the name of the node matches the old node type, update the
        // name of the task to keep it in sync.
        String oldType = node.getType();
        String nodeName = node.getName();
        nodeName = (nodeName == null ? "" : nodeName.trim());
        if (nodeName.length() == 0 || oldType.equals(nodeName) ||
            oldType.equals(nodeName + " Task")) {
            nodeName = type;
            if (nodeName.endsWith(" Task"))
                nodeName = nodeName.substring(0, nodeName.length() - 5);
            node.setName(nodeName);
        }

        // save the new type of the node.
        node.setType(type);
        EVENT_CONSOLIDATOR.needEvent(wbsModel.getRowForNode(node));
    }

    public boolean recalculate() { return true; }
    public void storeDependentColumn(String ID, int columnNumber) {}

    private void buildEditor(TeamProcess teamProcess) {
        List nodeTypes = teamProcess.getChoosableNodeTypes();
        allowedTypes = new HashSet(nodeTypes);

        JComboBox comboBox = new JComboBox(nodeTypes.toArray());
        DefaultCellEditor dce = new DefaultCellEditor(comboBox);
        dce.setClickCountToStart(2);
        this.cellEditor = dce;
    }

    public TableCellEditor getCellEditor() {
        return cellEditor;
    }

    /** If the user pastes a long string of values into this column, each
     * one will change the type of a different WBS node.  Rather than firing
     * change events on the WBS for each pasted value, send one event at the
     * end of the entire operation.  Since WBS change events trigger broad
     * recalculation operations, consolidating these events can save a lot
     * of churn downstream.
     */
    private class EventConsolidator implements ActionListener {
        private int row;
        private Timer timer;
        public EventConsolidator() {
            this.timer = new Timer(20, this);
            this.timer.setRepeats(false);
            this.row = Integer.MAX_VALUE;
        }
        public synchronized void needEvent(int row) {
            this.row = Math.min(this.row, row);
            timer.restart();
        }
        public void actionPerformed(ActionEvent e) {
            int rowNumber;
            synchronized (this) {
                rowNumber = row;
                row = Integer.MAX_VALUE;
            }

            // we must update not just the given row, but also the rows
            // below it in the table (since they may now be invalid
            // because of the change).  Although only descendants of
            // this node could be affected, just repaint all the
            // remaining rows (lazy).
            int rowCount = wbsModel.getRowCount();
            if (rowNumber < rowCount)
                wbsModel.fireTableRowsUpdated(rowNumber, rowCount-1);
        }
    }
    private EventConsolidator EVENT_CONSOLIDATOR = new EventConsolidator();
}
