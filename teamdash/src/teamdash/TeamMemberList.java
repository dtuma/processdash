
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


/** Represents the list of team members assigned to a team project.
 *
 * This implements <code>TableModel</code> so the team can be displayed in
 * an editable table.
 */
public class TeamMemberList extends AbstractTableModel {

    /** A special interface for people wanting to be alerted to changes in
     * the initials of any existing team member. */
    public interface InitialsListener {
        public void initialsChanged(String oldInitials, String newInitials);
    }


    /** The list of team members */
    private ArrayList teamMembers = new ArrayList();
    /** True if we should always keep an empty team member at the end of
     * the list */
    private boolean autoNewRow = true;

    /** Creates an empty team member list. */
    public TeamMemberList() {
    }

    /** Create a team member list from the information in the given
     * XML element.  */
    public TeamMemberList(Element e) {
        NodeList nodes = e.getElementsByTagName(TeamMember.TAG_NAME);
        for (int i = 0;   i < nodes.getLength();   i++)
            teamMembers.add(new TeamMember((Element) nodes.item(i)));
    }

    /** Create a cloned copy of the given team member list */
    public TeamMemberList(TeamMemberList list) {
        this.teamMembers = copyTeamMemberList(list.teamMembers);
    }

    /** Add an empty team member to the bottom of the list if the last member
     * in the list is not currently empty.
     */
    public void maybeAddEmptyRow() {
        int rows = getRowCount();
        if (rows == 0 || hasValue(getValueAt(rows-1, NAME_COLUMN)))
            addNewRow();
    }

    /** Get a list of all the non-empty team members.
     */
    public List getTeamMembers() {
        ArrayList result = new ArrayList();
        Iterator i = teamMembers.iterator();
        while (i.hasNext()) {
            TeamMember m = (TeamMember) i.next();
            if (!m.isEmpty()) result.add(m);
        }
        return result;
    }

    /** Alter this team list so it contains the nonempty members
     * in the given list. */
    public void copyFrom(TeamMemberList newMembers) {
        this.teamMembers = copyTeamMemberList(newMembers.getTeamMembers());
        fireTableDataChanged();
    }



    /** Clone a List of team members. */
    private ArrayList copyTeamMemberList(List list) {
        ArrayList result = new ArrayList();
        Iterator i = list.iterator();
        while (i.hasNext())
            result.add(((TeamMember) i.next()).clone());
        return result;
    }




    /** Serialize this team member list to XML and write it to the given
     * Writer.
     */
    public void getAsXML(Writer out) throws IOException {
        out.write("<"+TAG_NAME+">\n");
        Iterator i = teamMembers.iterator();
        while (i.hasNext()) {
            TeamMember m = (TeamMember) i.next();
            if (!m.isEmpty())
                m.getAsXML(out);
        }
        out.write("</"+TAG_NAME+">\n");
    }
    // the XML tag name identifying the team member list.
    private static final String TAG_NAME = "teamList";



    ////////////////////////////////////////////////////////////////////////
    //// information and methods to implement the TableModel interface
    ////////////////////////////////////////////////////////////////////////

    public static final int NAME_COLUMN = 0;
    public static final int INITIALS_COLUMN = 1;
    public static final int COLOR_COLUMN = 2;
    public static final int HOURS_COLUMN = 3;

    private static final String[] columnNames = {
        "Name", "Initials", "Color", "Est Hours/Week" };
    private static final Class[] columnClass = {
        String.class, String.class, Color.class, Double.class };

    public int getRowCount()            { return teamMembers.size(); }
    public int getColumnCount()         { return columnNames.length; }
    public String getColumnName(int c)  { return columnNames[c];     }
    public Class getColumnClass(int c)  { return columnClass[c];     }
    public boolean isCellEditable(int row, int col) { return true; }

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
            if (autoNewRow) maybeAddEmptyRow();
            break;

        case INITIALS_COLUMN: m.setInitials((String) aValue); break;
        case COLOR_COLUMN: m.setColor((Color) aValue); break;
        case HOURS_COLUMN: m.setHoursPerWeek((Double) aValue); break;
        }
    }


    /** Convenience method to retrieve a particular team member. */
    private TeamMember get(int r) {
        return (TeamMember) teamMembers.get(r);
    }
    /** Convenience method to determine whether a string has a non-null,
     * non-whitespace, non-empty value. */
    private boolean hasValue(Object s) {
        return (s instanceof String && ((String)s).trim().length() > 0);
    }

    /** Add a new, empty team member to the end of the list.  Use the
     * first available unused color. */
    private void addNewRow() {
        int newRowNum = getRowCount();
        Color c = getFirstUnusedColor();
        teamMembers.add(new TeamMember(null, null, c));
        fireTableRowsInserted(newRowNum, newRowNum);
    }

    /** Find the first color in the DEFAULT_COLORS list which has not
     * been used by any team member in this list. */
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

    /** A list of 18 colors that look mutually unique. (This was really hard
     * to create!) */
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





    /** Check this team list for errors and invalid values */
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
                addError(errors,
                         "You must enter initials for each team member");
            else if (initials.contains(init))
                addError(errors,
                         "More than one team member has the initials '" +
                         init + "'");
            initials.add(init);
        }

        if (errors.size() == 0) return null;
        return errors.toArray();
    }

    /** Add a string to the list if it isn't already present */
    private void addError(ArrayList errList, String err) {
        if (!errList.contains(err)) errList.add(err);
    }




    /** An object describing a change in a team member list.
     *
     * If the <code>before</code> object is null, this describes the addition
     * of a team member to a list. If the <code>after</code> object is null,
     * this describes the deletion of a team member to a list.  Otherwise, it
     * describes a change to an existing team member.
     *
     * For deletions and changes, the description field contains a user-
     * readable description of the change.
     */
    public class Delta {
        TeamMember before, after;
        String description;
        public Delta(TeamMember bef, TeamMember aft, String d) {
            before = bef;   after = aft;   description = d;
        }
        public String toString() { return description; }
    }

    /** Compare this team member list to a new, altered team member list and
     * return a list of any significant changes that have been made. (A
     * significant change is either deletion of an existing team member, or a
     * change to the name or initials of an exising team member.) If no
     * significant changes have been made, returns null.
     */
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

    /** Try to find a team member in the given list which looks similar to
     * the one passed in.
     *
     * @param t a team member to find a match for
     * @param l a list of team members to look in
     * @param search if 0, two team members will be considered a "match" only
     * if their names are equal. if 1, two team members will "match" if their
     * initials are equal.
     * @return Delta null if no matching team member could be found.
     * Otherwise, a Delta object comparing two team members.
     */
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

    /** notify any registered InitialsListeners about the any changes to
     * initials found in the given list. */
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
