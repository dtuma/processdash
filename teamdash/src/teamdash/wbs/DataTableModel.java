
package teamdash.wbs;

import java.awt.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import teamdash.*;


public class DataTableModel extends AbstractTableModel {

    private WBSModel wbsModel;
    private ArrayList columns;

    public DataTableModel(WBSModel wbsModel) {
        this.wbsModel = wbsModel;
        wbsModel.addTableModelListener(new TableModelEventRepeater());

        columns = new ArrayList();
        columns.add(new NewAndChangedLOCColumn(this));

        for (char c = 'B';   c <= 'Z';   c++)
            columns.add(new TopDownBottomUpColumn(this, String.valueOf(c)));
    }

    public WBSModel getWBSModel() { return wbsModel; }

    private class TableModelEventRepeater implements TableModelListener {
        public void tableChanged(TableModelEvent e) {
            TableModelEvent newEvent = new TableModelEvent
                (DataTableModel.this, e.getFirstRow(),
                 e.getLastRow(), e.getColumn(), e.getType());
            fireTableChanged(newEvent);
        }
    }

    // implementation of javax.swing.table.TableModel interface

    public int getRowCount() { return wbsModel.getRowCount(); }
    public int getColumnCount() { return columns.size(); }

    protected DataColumn getColumn(int columnIndex) {
        return (DataColumn) columns.get(columnIndex);
    }

    public String getColumnName(int columnIndex) {
        return getColumn(columnIndex).getColumnName();
    }

    public Class getColumnClass(int columnIndex) {
        return getColumn(columnIndex).getColumnClass();
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return getColumn(columnIndex).isCellEditable(rowIndex);
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return getColumn(columnIndex).getValueAt(rowIndex);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        getColumn(columnIndex).setValueAt(aValue, rowIndex);
    }

}
