

package teamdash;

import java.awt.Color;
import java.util.*;
import javax.swing.table.*;


public class TeamMemberList extends AbstractTableModel {

    private ArrayList teamMembers = new ArrayList();
    private boolean autoNewRow = true;

    public TeamMemberList() {
        addNewRow();
    }

    public static final int NAME_COLUMN = 0;
    public static final int INITIALS_COLUMN = 1;
    public static final int COLOR_COLUMN = 2;
    public static final int HOURS_COLUMN = 3;

    private static final String[] columnNames = {
        "Name", "Initials", "Color", "Est Hours/Week" };
    private static final Class[] columnClass = {
        String.class, String.class, Color.class, Double.class };


    public int getColumnCount()         { return columnNames.length; }
    public String getColumnName(int c)  { return columnNames[c];     }
    public Class getColumnClass(int c)  { return columnClass[c];     }

    private TeamMember get(int r) {
        return (TeamMember) teamMembers.get(r);
    }

    public int getRowCount() {
        return teamMembers.size();
    }

    public Object getValueAt(int row, int column) {
        TeamMember m = get(row);
        switch (column) {
        case NAME_COLUMN: return m.getName();
        case INITIALS_COLUMN: return m.getInitials();
        case COLOR_COLUMN: return m.getColor();
        case HOURS_COLUMN: return m.getHoursPerWeek();
        }
        return null;
    }

    public void setValueAt(Object aValue, int row, int column) {


        TeamMember m = get(row);
        switch (column) {
        case NAME_COLUMN:
            Object currentVal = getValueAt(row, NAME_COLUMN);
            // if both values are empty, there is nothing to do.
            if (!hasValue(currentVal) && !hasValue(aValue)) break;
            // if the value has not changed, there is nothing to do.
            if (hasValue(aValue) && aValue.equals(currentVal)) break;
            m.setName((String) aValue);
            if (autoNewRow && hasValue(aValue) && (row + 1 == getRowCount()))
                addNewRow();
            break;

        case INITIALS_COLUMN: m.setInitials((String) aValue); break;
        case COLOR_COLUMN: m.setColor((Color) aValue); break;
        case HOURS_COLUMN: m.setHoursPerWeek((Double) aValue); break;
        }
    }
    private boolean hasValue(Object s) {
        return (s instanceof String && ((String)s).trim().length() > 0);
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    private void addNewRow() {
        int newRowNum = getRowCount();
        teamMembers.add(new TeamMember(null, null, null));
        fireTableRowsInserted(newRowNum, newRowNum);
    }
}
