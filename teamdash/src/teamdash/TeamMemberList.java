

package teamdash;

import java.awt.Color;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
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

    public TeamMemberList(TeamMemberList list) {
        this.teamMembers = copyTeamMemberList(list.teamMembers);
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
            m.setName((String) aValue);
            if (autoNewRow) maybeAddEmptyRow(); /*
            Object currentVal = getValueAt(row, NAME_COLUMN);
            // if both values are empty, there is nothing to do.
            if (!hasValue(currentVal) && !hasValue(aValue)) break;
            // if the value has not changed, there is nothing to do.
            if (hasValue(aValue) && aValue.equals(currentVal)) break;
            m.setName((String) aValue);
            if (autoNewRow && hasValue(aValue) && (row + 1 == getRowCount()))
                addNewRow(); */
            break;

        case INITIALS_COLUMN: m.setInitials((String) aValue); break;
        case COLOR_COLUMN: m.setColor((Color) aValue); break;
        case HOURS_COLUMN: m.setHoursPerWeek((Double) aValue); break;
        }
    }
    private boolean hasValue(Object s) {
        return (s instanceof String && ((String)s).trim().length() > 0);
    }

    public void maybeAddEmptyRow() {
        int rows = getRowCount();
        if (hasValue(getValueAt(rows-1, NAME_COLUMN)))
            addNewRow();
    }


    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    private void addNewRow() {
        int newRowNum = getRowCount();
        Color c = getFirstUnusedColor();
        teamMembers.add(new TeamMember(null, null, c));
        fireTableRowsInserted(newRowNum, newRowNum);
    }

    private Color getFirstUnusedColor() {
        HashSet usedColors = new HashSet();
        Iterator i = teamMembers.iterator();
        while (i.hasNext())
            usedColors.add(((TeamMember) i.next()).getColor());

        for (int j = 0;   j < DEFAULT_COLORS.length;   j++) {
            Color c = Color.decode(DEFAULT_COLORS[j]);
            if (!usedColors.contains(c)) return c;
        }

         return Color.darkGray;
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

    private ArrayList copyTeamMemberList(List list) {
        ArrayList result = new ArrayList();
        Iterator i = list.iterator();
        while (i.hasNext())
            result.add(((TeamMember) i.next()).clone());
        return result;
    }

    public class Delta {
        TeamMember before, after;
        String description;
        public Delta(TeamMember bef, TeamMember aft, String d) {
            before = bef;   after = aft;   description = d;
        }
        public String toString() { return description; }
    }
    public Delta[] calculateDelta(TeamMemberList newMembers) {
        ArrayList origList =  new ArrayList(this.teamMembers);
        ArrayList newList =  new ArrayList(newMembers.teamMembers);
        ArrayList deltas = new ArrayList();
        for (int search = 0;   search < 2;   search++) {
            Iterator i = origList.iterator();
            while (i.hasNext()) {
                TeamMember origMember = (TeamMember) i.next();
                Delta d = findTeamMember(origMember, newList, search);
                if (d != null) {
                    i.remove();
                    newList.remove(d.after);
                    if (d.description.length() > 0) deltas.add(d);
                }
            }
        }
        if (origList.size() > 0) {
            Iterator i = origList.iterator();
            while (i.hasNext()) {
                TeamMember t = (TeamMember) i.next();
                deltas.add(new Delta(t, null, "Delete " + t.getName()));
            }
        }

        if (deltas.size() == 0) return null;
        return (Delta[]) deltas.toArray(new Delta[0]);
    }
    private Delta findTeamMember(TeamMember t, ArrayList l, int search) {
        Iterator i = l.iterator();
        while (i.hasNext()) {
            TeamMember s = (TeamMember) i.next();
            String diff = t.compareToMember(s, search == 1);
            if (diff != null)
                return new Delta(t, s, diff);
        }
        return null;
    }

    public void copyFrom(TeamMemberList newMembers) {
        this.teamMembers = copyTeamMemberList(newMembers.getTeamMembers());
        fireTableDataChanged();
    }

    private void addError(ArrayList errList, String err) {
        if (!errList.contains(err)) errList.add(err);
    }

    public Object[] getErrors() {
        HashSet names = new HashSet();
        HashSet initials = new HashSet();
        ArrayList errors = new ArrayList();
        Iterator i = teamMembers.iterator();
        while (i.hasNext()) {
            TeamMember m = (TeamMember) i.next();
            if (m.isEmpty()) continue;

            String name = m.getName();
            if (name == null)
                addError(errors, "You must enter a name for each team member");
            else if (names.contains(name))
                addError(errors, "More than one team member is named '" +
                         name + "'");
            names.add(name);

            String init = m.getInitials();
            if (init == null)
                addError(errors, "You must enter initials for each team member");
            else if (initials.contains(init))
                addError(errors, "More than one team member has the initials '"
                         + init + "'");
            initials.add(init);
        }

        if (errors.size() == 0) return null;
        return errors.toArray();
    }

    public void publishChanges(Delta[] changes) {
        if (changes == null || changes.length == 0)
            return;
        for (int i = changes.length;   i-- > 0; ) {
            if (changes[i].before == null) continue;
            if (changes[i].after == null) continue;
            String initBefore = changes[i].before.getInitials();
            String initAfter = changes[i].after.getInitials();
            if (initBefore != null && initAfter != null &&
                !initBefore.equals(initAfter))
                fireInitialsChanged(initBefore, initAfter);
        }
    }

    public interface InitialsListener {
        public void initialsChanged(String oldInitials, String newInitials);
    }

    private HashSet initialsListeners = null;
    public void addInitialsListener(InitialsListener l) {
        if (l == null) return;
        if (initialsListeners == null)
            initialsListeners = new HashSet();
        initialsListeners.add(l);
    }
    public void removeInitialsListener(InitialsListener l) {
        if (initialsListeners != null)
            initialsListeners.remove(l);
    }

    private void fireInitialsChanged(String initBefore, String initAfter) {
        Iterator i = initialsListeners.iterator();
        while (i.hasNext())
            ((InitialsListener) i.next()).initialsChanged
                (initBefore, initAfter);
    }
}
