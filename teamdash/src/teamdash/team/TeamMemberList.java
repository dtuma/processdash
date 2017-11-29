// Copyright (C) 2002-2017 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 3
// of the License, or (at your option) any later version.
//
// Additional permissions also apply; see the README-license.txt
// file in the project root directory for more information.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net


package teamdash.team;

import java.awt.Color;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.swing.table.AbstractTableModel;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.StringUtils;

import teamdash.XMLUtils;
import teamdash.wbs.WBSPermissionManager;



/** Represents the list of team members assigned to a team project.
 *
 * This implements <code>TableModel</code> so the team can be displayed in
 * an editable table.
 */
public class TeamMemberList extends AbstractTableModel implements EffortCalendar {

    /** A special interface for people wanting to be alerted to changes in
     * the initials of any existing team member. */
    public interface InitialsListener {
        public void initialsChanged(String oldInitials, String newInitials);
    }

    public enum InitialsPolicy {
        Initials, Username
    }

    private static final Resources resources = TeamMember.resources;

    /** The list of team members */
    private ArrayList<TeamMember> teamMembers = new ArrayList();

    /** A model that holds information about named subteams */
    private SubteamDataModel subteamModel;

    /** An optional filter to use for date calculations */
    private Set<Integer> subteamFilter;

    /** True if we should always keep an empty team member at the end of
     * the list */
    private boolean autoNewRow = true;

    /** Is this team member list read only? */
    private boolean readOnly = false;

    /** If set, only the team member with these initials can be edited */
    private String onlyEditableFor = null;

    /** The policy this team uses for assigning initials */
    private InitialsPolicy initialsPolicy = InitialsPolicy.Initials;

    /** What day of the week would the user like the schedule to start?
     * This should be a value recognized by the java.util.Calendar class
     * for the DAY_OF_WEEK field. */
    private int startOnDayOfWeek;

    /** A date/time in the middle of the "zero week" of the schedule.
     * 
     * The reference date is not used directly by calculations;  instead, those
     * use the zero date.  Instead, the reference date exists solely to help us
     * produce the zero date.  The reference date is read and written to XML
     * by the load/save logic.  Then by definition, the zero date always falls
     * on the user-selected day of the week, in the 7-day period immediately
     * preceding the reference date.
     */
    private Date referenceDate;

    /** A date/time on the first day of the "zero week" of the schedule.
     * Note that this won't necessarily be at midnight; see the
     * {@link #recalcZeroDay()} method for an explanation. */
    private Date zeroDay;

    /** The table model only shows a slice of the overall schedule. This
     * field controls where in the calendar this slice has scrolled to. */
    private int weekOffset;

    /** The table model only shows a slice of the overall schedule. This
     * field controls how many weeks are represented in that slice. */
    private int columnCount = FIRST_WEEK_COLUMN+1;

    /** This field is set to true when this TeamMemberList has pending
     * changes */
    private boolean isDirty;

    /** Creates an empty team member list. */
    public TeamMemberList() {
        subteamModel = new SubteamDataModel();
        isDirty = false;
        startOnDayOfWeek = Calendar.SUNDAY;
        referenceDate = new Date();
        weekOffset = getDefaultWeekOffset();
        recalcZeroDay();
    }

    /** Create a team member list from the information in the given
     * XML element.  */
    public TeamMemberList(Element e) {
        subteamModel = new SubteamDataModel();

        // read the "starting day of the week" from the XML, or choose a default
        if (e.hasAttribute(DAY_OF_WEEK_ATTR))
            startOnDayOfWeek = XMLUtils.getXMLInt(e, DAY_OF_WEEK_ATTR);
        else
            startOnDayOfWeek = Calendar.SUNDAY;

        // read the "reference date" from the XML, or choose a default
        referenceDate = null;
        if (e.hasAttribute(REFERENCE_DATE_ATTR))
            referenceDate = XMLUtils.getXMLDate(e, REFERENCE_DATE_ATTR);
        if (referenceDate == null)
            referenceDate = getDefaultReferenceDate(e);

        // calculate the "zero date" for extrapolating display dates
        recalcZeroDay();

        // construct the list of team members from the XML data
        NodeList nodes = e.getElementsByTagName(TeamMember.TAG_NAME);
        for (int i = 0;   i < nodes.getLength();   i++)
            teamMembers.add(new TeamMember((Element) nodes.item(i), zeroDay,
                    subteamModel));

        // determine what date to scroll to initially
        weekOffset = getDefaultWeekOffset();

        isDirty = assignMissingUniqueIDs(null);
    }

    /** Create a cloned copy of the given team member list */
    public TeamMemberList(TeamMemberList list) {
        this.subteamModel = new SubteamDataModel();
        this.teamMembers = copyTeamMemberList(list.teamMembers);
        this.readOnly = list.readOnly;
        this.onlyEditableFor = list.onlyEditableFor;
        this.startOnDayOfWeek = list.startOnDayOfWeek;
        this.referenceDate = list.referenceDate;
        this.weekOffset = getDefaultWeekOffset();
        this.isDirty = false;
        recalcZeroDay();
    }

    public SubteamDataModel getSubteamModel() {
        return subteamModel;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public String getOnlyEditableFor() {
        return onlyEditableFor;
    }

    public void setOnlyEditableFor(String onlyEditableFor) {
        this.onlyEditableFor = onlyEditableFor;
    }

    public InitialsPolicy getInitialsPolicy() {
        return initialsPolicy;
    }

    public void setInitialsPolicy(InitialsPolicy initialsPolicy) {
        this.initialsPolicy = initialsPolicy;
        columnNames[INITIALS_COLUMN] = resources
                .getString("Columns." + initialsPolicy);
    }

    public void setInitialsPolicyName(String policyName) {
        setInitialsPolicy(parseInitialsPolicy(policyName));
    }

    private InitialsPolicy parseInitialsPolicy(String policyName) {
        if (policyName != null)
            for (InitialsPolicy p : InitialsPolicy.values())
                if (policyName.toLowerCase().startsWith(p.name().toLowerCase()))
                    return p;
        return InitialsPolicy.Initials;
    }

    public int getStartOnDayOfWeek() {
        return startOnDayOfWeek;
    }

    public void setStartOnDayOfWeek(int startOnDayOfWeek) {
        this.startOnDayOfWeek = startOnDayOfWeek;
        isDirty = true;
        fireTableDataChanged();
        recalcZeroDay();
    }

    public Date getZeroDay() {
        return zeroDay;
    }

    public void setNumWeekColumns(int num) {
        this.columnCount = num + FIRST_WEEK_COLUMN + 1;
    }

    public int getNumWeekColumns() {
        return this.columnCount - 1 - FIRST_WEEK_COLUMN;
    }

    public int getWeekOffset() {
        return weekOffset;
    }

    public void setWeekOffset(int offset) {
        this.weekOffset = offset;
        fireTableRowsUpdated(0, getRowCount()-1);
    }

    public void resetWeekOffset() {
        setWeekOffset(getDefaultWeekOffset());
    }

    public void setDirty(boolean dirty) {
        isDirty = dirty;
    }

    public boolean isDirty() {
        return isDirty;
    }

    /**
     * Move all team member start dates to the week containing the given date
     */
    public void moveAllStartDates(Date date) {
        if (date == null)
            return;

        // find the week that contains the given date.
        int week = WeeklySchedule.dateToWeekValue(zeroDay, date);
        Date roundedDate = WeeklySchedule.weekValueToDate(zeroDay, week);
        if (roundedDate.after(date))
            week--;

        // reset all team member start dates to the given week.
        for (TeamMember m : teamMembers)
            m.getSchedule().setWeekData(week - 1, WeekData.WEEK_START);

        // scroll the view to align to the new start week.
        setWeekOffset(week - FIRST_WEEK_COLUMN - 1);
    }

    /**
     * Set schedule privacy attributes for all the team members in this list
     */
    public void setSchedulePrivacyFlags(Set<Integer> allowTeamMemberIDs) {
        String initials = System.getProperty("teamdash.wbs.indivInitials");
        String user = WBSPermissionManager.getCurrentUser();
        Set<String> datasets = WBSPermissionManager.getGroupPerm(
            "pdash.indivData.scheduleHours", "2.3.1.4");
        for (TeamMember m : teamMembers) {
            m.setSchedulePrivacy(getMemberType(m, user, initials,
                allowTeamMemberIDs, datasets));
        }
    }

    private PrivacyType getMemberType(TeamMember m, String currentUserName,
            String currentUserInitials, Set<Integer> allowTeamMemberIDs,
            Set<String> grantedDatasets) {
        // if this member has no initials, this is an empty row. allow editing
        String initials = m.getInitials();
        if (!StringUtils.hasValue(initials))
            return PrivacyType.Allowed;

        // see if this team member's initials match those of the current user
        if (initials.equalsIgnoreCase(currentUserInitials))
            return PrivacyType.Me;

        // see if this team member's username matches the current user
        if (currentUserName != null) {
            Map<String, String> serverInfo = m.getServerIdentityInfoMap();
            if (currentUserName.equalsIgnoreCase(serverInfo.get("username")))
                return PrivacyType.Me;
            if (initialsPolicy == InitialsPolicy.Username
                    && currentUserName.equalsIgnoreCase(initials))
                return PrivacyType.Me;
        }

        // is this team member a person who should always be allowed?
        if (allowTeamMemberIDs != null
                && allowTeamMemberIDs.contains(m.getId()))
            return PrivacyType.Allowed;

        // test the two most common permission grant cases
        if (grantedDatasets == null)
            return (onlyEditableFor == null ? PrivacyType.Allowed
                    : PrivacyType.Visible);
        else if (grantedDatasets.isEmpty())
            return PrivacyType.Censored;

        // identify the datasetID for this team member
        String datasetID = m.getDatasetID();

        // determine the privacy type based on the dataset ID
        if (!XMLUtils.hasValue(datasetID))
            return PrivacyType.Uncertain;
        else if (grantedDatasets.contains(datasetID))
            return (onlyEditableFor == null ? PrivacyType.Allowed
                    : PrivacyType.Visible);
        else
            return PrivacyType.Censored;
    }

    public TeamMember getTeamMemberForCurrentUser() {
        // find a user whose server identity matches the logged in user
        String username = WBSPermissionManager.getCurrentUser();
        for (TeamMember m : teamMembers) {
            String mu = (String) m.getServerIdentityInfoMap().get("username");
            if (mu != null && mu.equalsIgnoreCase(username))
                return m;
        }

        // if this team is using a "username" initials policy, try matching
        // the logged in user against a team member's initials
        if (initialsPolicy == InitialsPolicy.Username && username != null) {
            for (TeamMember m : teamMembers) {
                if (username.equalsIgnoreCase(m.getInitials()))
                    return m;
            }
        }

        // if the WBS Editor was launched from a personal dashboard, try
        // finding the individual whose initials match that personal dashboard
        String initials = System.getProperty("teamdash.wbs.indivInitials");
        if (initials != null) {
            for (TeamMember m : teamMembers) {
                if (initials.equalsIgnoreCase(m.getInitials()))
                    return m;
            }
        }

        // no matches were found, return null
        return null;
    }

    /** Add an empty team member to the bottom of the list if the last member
     * in the list is not currently empty.
     */
    public void maybeAddEmptyRow() {
        if (isReadOnly())
            return;
        int rows = getRowCount();
        if (rows == 0 || hasValue(getValueAt(rows-1, NAME_COLUMN)))
            addNewRow();
    }

    private int getEarliestStartWeek() {
        int result = Integer.MAX_VALUE;
        for (int i = getRowCount();  i-- > 0; )
            if (!subteamFilterExcludesRow(i))
                result = Math.min(result, getScheduleAt(i).getStartWeek());
        if (result == Integer.MAX_VALUE)
            return 0;
        else
            return result;
    }

    private int getDefaultWeekOffset() {
        return getEarliestStartWeek() - FIRST_WEEK_COLUMN - 1;
    }

    /** Get a list of all the non-empty team members.
     */
    public List<TeamMember> getTeamMembers() {
        ArrayList<TeamMember> result = new ArrayList();
        Iterator i = teamMembers.iterator();
        while (i.hasNext()) {
            TeamMember m = (TeamMember) i.next();
            if (!m.isEmpty()) result.add(m);
        }
        return result;
    }

    void setTeamMembers(List newMembers) {
        this.teamMembers = copyTeamMemberList(newMembers);
    }

    /** Find the team member with the given initials */
    public TeamMember findTeamMember(String initials) {
        if (initials != null) {
            Iterator i = teamMembers.iterator();
            while (i.hasNext()) {
                TeamMember m = (TeamMember) i.next();
                if (initials.equalsIgnoreCase(m.getInitials()))
                    return m;
            }
        }
        return null;
    }

    /** Find the team member with the given ID */
    public TeamMember findTeamMemberByID(int id) {
        for (TeamMember m : teamMembers) {
            if (m.getId() == id)
                return m;
        }
        return null;
    }

    public int findTeamMemberPosByID(int id) {
        TeamMember t = findTeamMemberByID(id);
        return (t == null ? -1 : teamMembers.indexOf(t));
    }

    /** Alter this team list so it contains the nonempty members
     * in the given list. */
    public void copyFrom(TeamMemberList newMembers) {
        this.teamMembers = copyTeamMemberList(newMembers.getTeamMembers());
        this.startOnDayOfWeek = newMembers.startOnDayOfWeek;
        this.referenceDate = newMembers.referenceDate;
        recalcZeroDay();
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


    /**
     * Insert a list of team members at a specified position in the list.
     * 
     * @param list
     *            a list of team members. If these objects are already in this
     *            team member list, they will be moved.
     * @param pos
     *            the position in the list where the items should be inserted
     * @return the actual position the items were inserted (which could be
     *         different than the original number). If the items could not be
     *         inserted for some reason, returns -1.
     */
    public int insertTeamMembers(List<TeamMember> list, int pos) {
        // check preconditions
        if (pos < 0 || pos >= getRowCount() || list == null || list.isEmpty())
            return -1;

        // if they are attempting to insert the rows before one of the items
        // that they are moving, that is a recipe for confusion.  Do nothing.
        TeamMember insertBefore = get(pos);
        if (list.contains(insertBefore))
            return -1;

        // OK, we're going to make a change. Set the dirty flag.
        setDirty(true);

        // if we are MOVING existing team members, remove them from the list.
        for (TeamMember t : list)
            teamMembers.remove(t);

        // insert the team members into the appropriate position in the list.
        int insertionPos = teamMembers.indexOf(insertBefore);
        if (insertionPos == -1)
            insertionPos = teamMembers.size();
        teamMembers.addAll(insertionPos, list);

        // ensure that all newly inserted team members have a color assigned.
        for (TeamMember t : list) {
            if (!t.hasColor())
                t.setColor(getFirstUnusedColor());
        }

        // fire table events and return.
        fireTableDataChanged();
        return insertionPos;
    }



    /** Serialize this team member list to XML and write it to the given
     * Writer.
     */
    public void getAsXML(Writer out) throws IOException {
        out.write("<"+TAG_NAME);
        out.write(" "+DAY_OF_WEEK_ATTR+"='");
        out.write(Integer.toString(startOnDayOfWeek));
        out.write("' "+REFERENCE_DATE_ATTR+"='");
        out.write(XMLUtils.saveDate(referenceDate));
        out.write("'>\n");

        Iterator i = teamMembers.iterator();
        while (i.hasNext()) {
            TeamMember m = (TeamMember) i.next();
            if (!m.isEmpty())
                m.getAsXML(out, false, subteamModel);
        }

        out.write("</"+TAG_NAME+">\n");
    }
    // the XML tag name identifying the team member list.
    private static final String TAG_NAME = "teamList";
    private static final String DAY_OF_WEEK_ATTR = "firstDayOfWeek";
    private static final String REFERENCE_DATE_ATTR = "referenceDate";
    private static final String START_DATE_ATTR = "startDate";



    ////////////////////////////////////////////////////////////////////////
    //// information and methods to implement the TableModel interface
    ////////////////////////////////////////////////////////////////////////

    public static final int PRIVACY_COLUMN = -1;
    public static final int NAME_COLUMN = 0;
    public static final int INITIALS_COLUMN = 1;
    public static final int COLOR_COLUMN = 2;
    public static final int HOURS_COLUMN = 3;
    public static final int FIRST_WEEK_COLUMN = 4;

    private static final String[] columnNames = {
            resources.getString("Columns.Name"),
            resources.getString("Columns.Initials"),
            resources.getString("Columns.Color"),
            resources.getString("Columns.Hours") };

    private static final Class[] columnClass = {
        String.class, String.class, Color.class, Double.class };

    public int getRowCount() {
        return teamMembers.size();
    }

    public int getColumnCount() {
        return columnCount;
    }

    public String getColumnName(int col) {
        if (col < columnNames.length)
            return columnNames[col];

        if (DATE_FORMAT == null)
            buildDateFormats();
        int week = col + weekOffset;
        Date d = new Date(zeroDay.getTime() + week * WEEK_MILLIS);
        if (col == FIRST_WEEK_COLUMN+1)
            return LONG_DATE_FORMAT.format(d);
        else
            return DATE_FORMAT.format(d);
    }
    private DateFormat DATE_FORMAT = null;
    private DateFormat LONG_DATE_FORMAT = null;
    private void buildDateFormats() {
        DateFormat fmt = DateFormat.getDateInstance(DateFormat.SHORT);
        DATE_FORMAT = LONG_DATE_FORMAT = fmt;
        if (fmt instanceof SimpleDateFormat) {
            SimpleDateFormat sfmt = (SimpleDateFormat) fmt;
            String pat = sfmt.toPattern();
            pat = StringUtils.findAndReplace(pat, "yyyy", "yy");
            if (pat.startsWith("yy"))
                pat = pat.substring(3);
            else if (pat.endsWith("yy"))
                pat = pat.substring(0, pat.length()-3);
            DATE_FORMAT = new SimpleDateFormat(pat);
        }
    }
    private static final long WEEK_MILLIS = 7l * 24 * 60 * 60 * 1000;

    public Class getColumnClass(int col) {
        if (col < columnClass.length)
            return columnClass[col];
        else
            return WeekData.class;
    }

    public boolean isCellEditable(int row, int col) {
        if (isReadOnly())
            return false;

        PrivacyType p = teamMembers.get(row).getSchedulePrivacy();
        if (p == PrivacyType.Visible || p == PrivacyType.Censored
                || p == PrivacyType.Uncertain)
            return false;

        if (onlyEditableFor != null) {
            if (col == INITIALS_COLUMN)
                return false;
            else
                return p == PrivacyType.Me;
        }

        if (col < FIRST_WEEK_COLUMN)
            return true;

        Object value = getValueAt(row, col);
        if (value instanceof WeekData)
            return ((WeekData) value).isInsideSchedule();
        else
            return false;
    }

    public Object getValueAt(int row, int column) {
        TeamMember m = get(row);
        switch (column) {
        case PRIVACY_COLUMN: return m.getSchedulePrivacy();
        case NAME_COLUMN: return m.getName();
        case INITIALS_COLUMN: return m.getInitials();
        case COLOR_COLUMN: return m.getColor();
        case HOURS_COLUMN: return m.getHoursPerWeek();
        }

        if (column == columnCount-1)
            return WeekData.WEEK_END;

        WeeklySchedule sched = getScheduleAt(row);
        int week = column + weekOffset;
        return sched.getWeekData(week);
    }

    public void setValueAt(Object aValue, int row, int column) {
        if (isReadOnly())
            return;

        isDirty = true;

        TeamMember m = get(row);
        switch (column) {
        case NAME_COLUMN:
            m.userSetName((String) aValue);
            fireTableCellUpdated(row, column);
            if (autoNewRow) maybeAddEmptyRow();
            return;

        case INITIALS_COLUMN:
            m.setInitials((String) aValue);
            fireTableCellUpdated(row, column);
            return;

        case COLOR_COLUMN:
            m.setColor((Color) aValue);
            fireTableCellUpdated(row, column);
            return;

        case HOURS_COLUMN:
            m.setHoursPerWeek((Double) aValue);
            fireTableRowsUpdated(row, row);
            return;
        }

        if (column == columnCount-1) {
            if (aValue instanceof WeekData
                    && ((WeekData)aValue).getType() == WeekData.TYPE_END) {
                getScheduleAt(row).setEndWeek(WeeklySchedule.NO_END);
                fireTableRowsUpdated(row, row);
            }
        } else {
            WeeklySchedule sched = getScheduleAt(row);
            int week = column + weekOffset;
            sched.setWeekData(week, aValue);
            fireTableRowsUpdated(row, row);
        }
    }

    /** Convenience method to retrieve a particular team member. */
    public TeamMember get(int r) {
        return (TeamMember) teamMembers.get(r);
    }

    /** Convenience method to retrieve the schedule for a team member. */
    private WeeklySchedule getScheduleAt(int row) {
        return get(row).getSchedule();
    }

    /** Convenience method to determine whether a string has a non-null,
     * non-whitespace, non-empty value. */
    private boolean hasValue(Object s) {
        return (s instanceof String && ((String)s).trim().length() > 0);
    }

    /** Add a new, empty team member to the end of the list.  Use the
     * first available unused color. */
    private void addNewRow() {
        if (isReadOnly())
            return;

        int newRowNum = getRowCount();
        Color c = getFirstUnusedColor();
        int sw = getEarliestStartWeek();
        teamMembers.add(new TeamMember(null, null, c, sw, zeroDay));
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

    /** When we open a legacy team project with no reference date information,
     * this method calculates an appropriate reference date for the project.
     */
    private Date getDefaultReferenceDate(Element xml) {
        long when = System.currentTimeMillis();
        NodeList nodes = xml.getElementsByTagName(TeamMember.TAG_NAME);
        for (int i = 0;   i < nodes.getLength();   i++) {
            Element e = (Element) nodes.item(i);
            Date d = XMLUtils.getXMLDate(e, START_DATE_ATTR);
            if (d != null && d.getTime() < when)
                when = d.getTime();
        }
        return new Date(when + WEEK_MILLIS/2);
    }

    /**
     * Calculate a zero day, by finding the day on the calendar that falls
     * in the week prior to the reference date, and that is on the day of the
     * week selected by the user.
     * 
     * Note: one might initially expect to place the zero date at midnight
     * on the chosen day;  however, if we do, we have to be wary of daylight
     * savings time changes as we do week arithmetic.  We will never be
     * displaying times - only dates.  So we place the zero date at 4:00 AM.
     * This allows us to blindly add the number of nominal milliseconds in a
     * week without worrying about daylight savings time.  Our resulting
     * timestamps may fall at 3:00 AM or 5:00 AM, but will still format as
     * the correct day on the calendar.
     * 
     * After determining the zero date, this method will also recalculate the
     * reference date to be 3.5 days later.  This accomplishes two things:
     * <ul>

     *     <li>When the user changes the preferred day of the week, the zero
     *     date will move to the nearest occurrence of that day (no more than
     *     3 days in either direction)</li>
     * 
     *     <li>When users from various time zones open the WBS, the reference
     *     date can be reliably used to calculate the appropriate zero date in
     *     their timezone.  Since the reference date is over 24 hours away from
     *     the zero date, there is no danger that a timezone difference will
     *     accidentally shift the zero date by a week.</li>
     *
     * </ul>
     */
    private void recalcZeroDay() {
        Calendar c = Calendar.getInstance();
        c.setTime(referenceDate);
        c.set(Calendar.DAY_OF_WEEK, startOnDayOfWeek);
        c.set(Calendar.HOUR_OF_DAY, 4); c.set(Calendar.MINUTE, 0);  // 4:00 AM
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        Date result = c.getTime();
        if (result.after(referenceDate)) {
            result = new Date(result.getTime() - WEEK_MILLIS);
        }
        this.zeroDay = result;
        this.referenceDate = new Date(result.getTime() + WEEK_MILLIS/2);

        for (int i = getRowCount();  i-- > 0; )
            getScheduleAt(i).setZeroDay(result);
    }

    public void setSubteamFilter(Set<Integer> filter) {
        this.subteamFilter = filter;
    }

    private boolean subteamFilterExcludesRow(int row) {
        return (subteamFilter != null
                && !subteamFilter.contains(get(row).getId()));
    }

    public Date getDateForEffort(double hours) {

        int week = getEarliestStartWeek();

        while (hours > 0 && week < 1000) {
            double hoursThisWeek = 0;
            boolean inMaintenancePeriod = true;

            for (int i = getRowCount();  i-- > 0; ) {
                if (subteamFilterExcludesRow(i))
                    continue;
                WeekData wd = getScheduleAt(i).getWeekData(week);
                if (wd.isInsideSchedule())
                    hoursThisWeek += wd.getHours();
                if (week < getScheduleAt(i).getMaintenanceStartWeek())
                    inMaintenancePeriod = false;
            }

            // if we can get done this week, calculate when.
            if (hours < hoursThisWeek) {
                double fractionalWeek = hours / hoursThisWeek;
                return weekValueToDate(week + fractionalWeek);
            }

            // if we've passed all the exceptions, and we're in the maintenance
            // range of the project, just extrapolate to get the end date
            if (inMaintenancePeriod) {
                if (hoursThisWeek > 0) {
                    double remainingWeeks = hours / hoursThisWeek;
                    return weekValueToDate(week + remainingWeeks);
                } else {
                    return null;
                }
            }

            // otherwise, take credit for this week's hours, and keep going.
            hours = hours - hoursThisWeek;
            week++;
        }

        return weekValueToDate(week);
    }

    private Date weekValueToDate(double week) {
        return WeeklySchedule.weekValueToDate(zeroDay, week);
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
                addError(errors, resources.getString("Errors.Name_Missing"));
            else if (names.contains(name))
                addError(errors,
                    resources.format("Errors.Name_Duplicate_FMT", name));
            names.add(name);

            String init = m.getInitials();
            if (init == null)
                addError(errors, resources.getString("Errors.Initials_Missing"));
            else if (initials.contains(init))
                addError(errors,
                    resources.format("Errors.Initials_Duplicate_FMT", init));
            initials.add(init);
        }

        if (errors.size() == 0) return null;
        return errors.toArray();
    }

    /** Add a string to the list if it isn't already present */
    private void addError(ArrayList errList, String err) {
        if (!errList.contains(err)) errList.add(err);
    }



    /**
     * Look through this list and assign unique IDs to any individuals who do
     * not already have them.
     * 
     * @param addedIDs
     *            if non-null, newly assigned IDs will be added to this set
     * @return true if any IDs were assigned, false if all team members already
     *         had IDs.
     */
    public boolean assignMissingUniqueIDs(Set<Integer> addedIDs) {
        Set<Integer> usedIDs = new HashSet<Integer>();
        for (TeamMember m : teamMembers)
            usedIDs.add(m.getId());
        if (!usedIDs.contains(-1))
            return false;

        boolean madeChange = false;
        Random rand = new Random();
        for (TeamMember m : teamMembers) {
            if (m.getId() == -1 && !m.isEmpty()) {
                int newID;
                do {
                    newID = Math.abs(rand.nextInt());
                } while (newID == 0 || usedIDs.contains(newID));
                usedIDs.add(newID);
                m.setId(newID);
                if (addedIDs != null)
                    addedIDs.add(newID);
                madeChange = true;
            }
        }
        return madeChange;
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
        for (Iterator i = origList.iterator(); i.hasNext();) {
            TeamMember origMember = (TeamMember) i.next();
            Delta d = findTeamMember(origMember, newList);
            if (d != null) {
                i.remove();
                newList.remove(d.after);
                if (d.description.length() > 0) deltas.add(d);
            }
        }
        if (origList.size() > 0) {
            Iterator i = origList.iterator();
            while (i.hasNext()) {
                TeamMember t = (TeamMember) i.next();
                String message = TeamMember.resources.format(
                    "Compare_Members.Delete_FMT", t.getName());
                deltas.add(new Delta(t, null, message));
            }
        }

        if (deltas.size() == 0) return null;
        return (Delta[]) deltas.toArray(new Delta[0]);
    }

    /** Try to find a team member in the given list whose ID matches
     * the one passed in.
     *
     * @param t a team member to find a match for
     * @param l a list of team members to look in
     * @return Delta null if no matching team member could be found.
     * Otherwise, a Delta object comparing two team members.
     */
    private Delta findTeamMember(TeamMember t, ArrayList l) {
        Iterator i = l.iterator();
        while (i.hasNext()) {
            TeamMember s = (TeamMember) i.next();
            String diff = t.compareToMember(s);
            if (diff != null)
                return new Delta(t, s, diff);
        }
        return null;
    }

    /** During an edit session, someone might reuse a row in the team member
     * list to delete an individual and replace them with someone else. When
     * that occurs, the new team member will (incorrectly) still have the
     * unique ID from the deleted team member.  Look for and clear out any
     * IDs that match this pattern.
     */
    protected void eraseDeletedTeamMemberIDs(Delta[] changes) {
        if (changes != null && changes.length > 0) {
            for (Delta d : changes) {
                if (d.before != null && d.after == null) {
                    TeamMember deleted = d.before;
                    for (TeamMember m : teamMembers) {
                        if (m.getId() == deleted.getId())
                            m.setId(-1);
                    }
                }
            }
        }
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
