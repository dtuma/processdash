package teamdash.wbs.columns;

import java.awt.Component;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import net.sourceforge.processdash.ui.lib.JDateTimeChooserCellEditor;
import teamdash.wbs.CustomEditedColumn;
import teamdash.wbs.MilestonesWBSModel;
import teamdash.wbs.WBSNode;

public class MilestoneCommitDateColumn extends AbstractDataColumn implements
        CustomEditedColumn {

    public static final String COLUMN_ID = "Commit Date";

    private static final String VALUE_ATTR = "Commit Date";
    private static final String CACHED_DATE_ATTR = "Commit_Date_Object";
    private static final Date NO_DATE_VALUE = new Date(0);


    public MilestoneCommitDateColumn() {
        this.columnName = this.columnID = COLUMN_ID;
        this.preferredWidth = 65;
    }

    public Class getColumnClass() {
        return Date.class;
    }

    public Object getValueAt(WBSNode node) {
        return getCommitDate(node);
    }

    public static Date getCommitDate(WBSNode node) {
        Date result = (Date) node.getAttribute(CACHED_DATE_ATTR);
        if (result == null) {
            result = NO_DATE_VALUE;
            String value = (String) node.getAttribute(VALUE_ATTR);
            if (value != null)
                try {
                    result = STORAGE_FMT.parse(value);
                } catch (Exception e) {
                }
            node.setAttribute(CACHED_DATE_ATTR, result);
        }

        if (result == NO_DATE_VALUE)
            return null;
        else
            return result;
    }

    public boolean isCellEditable(WBSNode node) {
        return MilestonesWBSModel.MILESTONE_TYPE.equals(node.getType());
    }

    public void setValueAt(Object value, WBSNode node) {
        String storageVal;
        Date dateVal;
        if (value instanceof Date) {
            dateVal = (Date) value;
            storageVal = STORAGE_FMT.format(dateVal);
        } else {
            dateVal = NO_DATE_VALUE;
            storageVal = null;
        }
        node.setAttribute(VALUE_ATTR, storageVal);
        node.setAttribute(CACHED_DATE_ATTR, dateVal);
    }

    private static final SimpleDateFormat STORAGE_FMT = new SimpleDateFormat(
            "yyyy-MM-dd");

    public TableCellEditor getCellEditor() {
        return CELL_EDITOR;
    }

    private static class CellEditor extends JDateTimeChooserCellEditor {

        public CellEditor() {
            super(getDateFormatString());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table,
                Object value, boolean isSelected, int row, int column) {

            if (value == null) {
                for (int r = row; r-- > 1;) {
                    Object oneVal = table.getValueAt(r, column);
                    if (oneVal instanceof Date) {
                        value = (Date) oneVal;
                        break;
                    }
                }
            }

            return super.getTableCellEditorComponent(table, value, isSelected,
                row, column);
        }

    }

    public static final TableCellEditor CELL_EDITOR = new CellEditor();

    private static String getDateFormatString() {
        DateFormat fmt = DateFormat.getDateInstance();
        if (fmt instanceof SimpleDateFormat) {
            return ((SimpleDateFormat) fmt).toPattern();
        }
        return STORAGE_FMT.toPattern();
    }


}
