

package teamdash;

import java.awt.Color;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class TeamMemberList extends AbstractTableModel {

    private ArrayList teamMembers = new ArrayList();
    private boolean autoNewRow = true;

    public TeamMemberList() {
        addNewRow();
    }

    public TeamMemberList(Element e) {
        NodeList nodes = e.getElementsByTagName(TeamMember.TAG_NAME);
        for (int i = 0;   i < nodes.getLength();   i++)
            teamMembers.add(new TeamMember((Element) nodes.item(i)));
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
        Color c = Color.darkGray;
        if (newRowNum < DEFAULT_COLORS.length)
            c = Color.decode(DEFAULT_COLORS[newRowNum]);
        teamMembers.add(new TeamMember(null, null, c));
        fireTableRowsInserted(newRowNum, newRowNum);
    }

    public void getAsXML(Writer out) throws IOException {
        out.write("<"+ATTR_NAME+">\n");
        Iterator i = teamMembers.iterator();
        while (i.hasNext()) {
            TeamMember m = (TeamMember) i.next();
            if (!m.isEmpty())
                m.getAsXML(out);
        }
        out.write("</"+ATTR_NAME+">\n");
    }
    private static final String ATTR_NAME = "teamList";
    private static final String[] DEFAULT_COLORS = {
        "#9933ff", // lavender
        "#6666ff", // pale blue
        "#00ccff", // pale cyan
        "#66ff99", // pale green
        "#ffff66", // pale yellow
        "#ffcc66", // pale orange
        "#660066", // dark purple
        "#000066", // navy
        "#0000ff", // blue
        "#00ff00", // green
        "#cccc00", // mustard
        "#996633", // light brown
        "#993366", // purple
        "#003366", // slate blue
        "#006666", // green-blue
        "#339933", // medium green
        "#003300", // dark green
        "#663300"  // brown
    };
    public List getTeamMembers() {
        ArrayList result = new ArrayList();
        Iterator i = teamMembers.iterator();
        while (i.hasNext()) {
            TeamMember m = (TeamMember) i.next();
            if (!m.isEmpty()) result.add(m);
        }
        return result;
    }
}
