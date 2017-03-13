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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.team.group.UserGroupManagerWBS;
import net.sourceforge.processdash.team.ui.PersonLookupData;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.NullSafeObjectUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;





/** Holds information about a individual member of a project team.
 */
public class TeamMember implements PersonLookupData, Cloneable {

    /** An internal ID that is guaranteed to be unique within a team project */
    private int id;
    /** The full name of the team member (e.g. John Doe)*/
    private String name;
    /** A query string provided by the lookup service for future reference */
    private String serverIdentityInfo;
    /** A cached version of server info, as a map of parameters */
    private Map serverIdentityInfoMap;
    /** The initials of the team member (e.g. jd) */
    private String initials;
    /** A color that will be used to represent this individual in various
     * charts, tables, and displays */
    private Color color;
    /** A list of the XML attributes that we did not recognize while loading
     * this object */
    private Map<String, String> extraAttributes;
    /** The schedule for this team member */
    private WeeklySchedule schedule;
    /** The privacy policy for this person's schedule */
    private PrivacyType schedulePrivacy;


    /** public TeamMember() { } */

    /** Create a new team member.
     *
     * A default time allocation of 20 hours per week will be used.
     * @param name the name of the individual
     * @param initials the person's initials
     * @param color the color to use for this person
     */
    public TeamMember(String name, String initials, Color color, int startWeek,
            Date zeroDay) {
        this.id = -1;
        this.name = trim(name);
        this.initials = trim(initials);
        this.color = color;
        this.schedule = new WeeklySchedule(zeroDay, startWeek);
    }

    /** Create a team member object using data in the given XML element. */
    public TeamMember(Element e, Date teamZeroDay, SubteamDataModel subteams) {
        Map<String, String> attrs = XMLUtils.getAttributesAsMap(e);
        // extract the ID attribute from the element.
        this.id = XMLUtils.getXMLInt(e, ID_ATTR);
        attrs.remove(ID_ATTR);
        // extract the name from the element.
        this.name = trim(attrs.remove(NAME_ATTR));
        // extract server identify information from the element.
        this.serverIdentityInfo = trim(attrs.remove(SERVER_IDENTITY_ATTR));
        // extract the initials from the element.
        this.initials = trim(attrs.remove(INITIALS_ATTR));
        // extract color information if it is present.
        String attr = attrs.remove(COLOR_ATTR);
        if (XMLUtils.hasValue(attr)) try {
            color = Color.decode(attr);
        } catch (NumberFormatException nfe) {}
        // extract subteam information
        String subteamNames = attrs.remove(SUBTEAMS_ATTR);
        if (subteams != null && XMLUtils.hasValue(subteamNames))
            subteams.addSubteamsForIndividual(id,
                Arrays.asList(subteamNames.split(SUBTEAMS_DELIM)));
        // extract schedule information
        this.schedule = new WeeklySchedule(e, teamZeroDay);
        // save extra, unrecognized attributes
        WeeklySchedule.removeRecognizedAttributes(attrs);
        this.extraAttributes = attrs;
    }

    protected TeamMember(int id, Map map, Date zeroDay) {
        this.id = id;
        this.name = (String) map.get(NAME_ATTR);
        this.serverIdentityInfo = (String) map.get(SERVER_IDENTITY_ATTR);
        this.initials = (String) map.get(INITIALS_ATTR);
        this.color = (Color) map.get(COLOR_ATTR);
        this.extraAttributes = (Map) map.get(EXTRA_ATTRS);
        this.schedule = new WeeklySchedule(map, zeroDay);
    }

    /** Convenience method - trim whitespace from the ends of a string.
     * if the result would be an empty string, this will return null. */
    private static String trim(String str) {
        if (str == null) return null;
        str = str.trim();
        if (str.length() == 0) return null;
        return str;
    }

    // getter/setter for the ID property
    public int getId() { return id; }
    void setId(int id) { this.id = id; }

    // getter/setter for the name property.
    public String getName() { return name; }
    public void setName(String name) { this.name = trim(name); }

    public void userSetName(String newName) {
        newName = trim(newName);
        if (!NullSafeObjectUtils.EQ(this.name, newName)) {
            setName(newName);
            setServerIdentityInfo(null);
        }
    }

    // getter/setter for the server identity info property.
    public String getServerIdentityInfo() { return serverIdentityInfo; }
    public void setServerIdentityInfo(String i) {
        this.serverIdentityInfo = trim(i);
        this.serverIdentityInfoMap = null;
    }

    // retrieve the server info as a map
    public Map getServerIdentityInfoMap() {
        if (serverIdentityInfoMap == null)
            serverIdentityInfoMap = Collections
                    .unmodifiableMap(HTMLUtils.parseQuery(serverIdentityInfo));
        return serverIdentityInfoMap;
    }

    public String getDatasetID() {
        // the user group manager is the ultimate authority. Ask it first
        String result = UserGroupManagerWBS.getInstance().getDatasetIDMap()
                .get(id);
        if (result == null)
            result = (String) getServerIdentityInfoMap().get("datasetID");
        return result;
    }

    // getter/setter for the initials property.
    public String getInitials() { return initials; }
    public void setInitials(String initials) {
        this.initials = convertToInitials(initials);
    }

    public static String convertToInitials(String initials) {
        Matcher m = LETTERS.matcher(initials);
        StringBuffer result = new StringBuffer();
        while (m.find())
            result.append(m.group());
        String theAnswer = trim(result.toString());
        return theAnswer;
    }
    private static final Pattern LETTERS = Pattern.compile("[a-zA-Z]+");

    // getter/setter for the color property.
    public Color getColor() {
        return (color == null ? Color.white : color);
    }
    public boolean hasColor() { return color != null; }
    public void setColor(Color color) { this.color = color; }

    Map getExtraAttributes() { return extraAttributes; }

    // getter/setter for the "start date" property.
    public Date getStartDate() { return schedule.getStartDate(); }
    public void setStartDate(Date d) { schedule.setStartDate(d); }

    public Date getEndDate() { return schedule.getEndDate(); }

    // getter/setter for the "hours per week" property.
    public Double getHoursPerWeek() {
        return new Double(schedule.getHoursPerWeek());
    }
    public void setHoursPerWeek(Double d) {
        schedule.setHoursPerWeek(d == null ? 0 : d.doubleValue());
    }

    public WeeklySchedule getSchedule() { return schedule; }

    public PrivacyType getSchedulePrivacy() {
        return schedulePrivacy;
    }

    public void setSchedulePrivacy(PrivacyType schedulePrivacy) {
        this.schedulePrivacy = schedulePrivacy;
    }

    /** returns true if this individual has no name and no initials. */
    public boolean isEmpty() {
        if (name != null && name.trim().length() > 0) return false;
        if (initials != null && initials.trim().length() > 0) return false;
        return true;
    }

    /** Serialize this team member to XML and write it to the given Writer. */
    public void getAsXML(Writer out, boolean dumpMode, SubteamDataModel subteams)
            throws IOException {
        out.write("  <"+TAG_NAME+" "+NAME_ATTR+"='");
        if (name != null)
            out.write(XMLUtils.escapeAttribute(name));
        if (id != -1) {
            out.write("' "+ID_ATTR+"='");
            out.write(Integer.toString(id));
        }
        if (serverIdentityInfo != null) {
            out.write("' "+SERVER_IDENTITY_ATTR+"='");
            out.write(XMLUtils.escapeAttribute(serverIdentityInfo));
        }
        out.write("' "+INITIALS_ATTR+"='");
        if (initials != null)
            out.write(XMLUtils.escapeAttribute(initials));
        if (!dumpMode) {
            out.write("' "+COLOR_ATTR+"='");
            if (color != null)
                out.write(ColorCellEditor.encodeColor(color));
        }
        if (subteams != null) {
            out.write("' " + SUBTEAMS_ATTR + "='");
            out.write(XMLUtils.escapeAttribute(StringUtils.join(
                subteams.getSubteamsForIndividual(id), SUBTEAMS_DELIM)));
        }

        if (extraAttributes != null && !extraAttributes.isEmpty()) {
            for (Map.Entry<String, String> e : extraAttributes.entrySet()) {
                out.write("' "+e.getKey()+"='");
                out.write(XMLUtils.escapeAttribute(e.getValue()));
            }
        }

        out.write("'");

        schedule.writeAttributes(out, dumpMode);
        if (schedule.hasExceptions()) {
            out.write(">\n");
            schedule.writeExceptions(out, dumpMode);
            out.write("  </"+TAG_NAME+">\n");
        } else {
            out.write("/>\n");
        }
    }

    protected Map<String, Object> getAsMap() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put(NAME_ATTR, getName());
        result.put(SERVER_IDENTITY_ATTR, serverIdentityInfo);
        result.put(INITIALS_ATTR, getInitials());
        result.put(COLOR_ATTR, getColor());
        result.put(EXTRA_ATTRS, extraAttributes);
        schedule.getAsMap(result);
        return result;
    }

    /** Make a copy of this team member object. */
    public Object clone() {
        try {
            TeamMember result = (TeamMember) super.clone();
            result.schedule = new WeeklySchedule(schedule);
            return result;
        } catch (CloneNotSupportedException cnse) {
            // can't happen?
            return null;
        }
    }

    /** Look to see if this team member is similar to another team member
     * object.
     *
     * @param that another team member object to compare to.
     * @return One of the following:<ul>
     *         <li>null if the two objects don't appear to represent the same
     *         individual.</li>
     *         <li>the empty string if the two objects represent the same
     *         individual, and the name and intiials match.</li>
     *         <li>A user-readable string describing the changes that would
     *         need to be made to this object for it look like the object
     *         passed in.</li>
     *         </ul>
     */
    public String compareToMember(TeamMember that) {
        if (this.id == -1 || this.id != that.id)
            return null;

        boolean differentName = (this.name == null ||
                                 !this.name.equals(that.name));
        boolean differentInitials = (this.initials == null ||
                                     !this.initials.equals(that.initials));
        if (differentName && differentInitials) return null;

        String resKey;
        if (differentName)
            resKey = "Compare_Members.Name_Change_FMT";
        else if (differentInitials)
            resKey = "Compare_Members.Initials_Change_FMT";
        else
            return "";

        return resources.format(resKey, this.name, that.name, this.initials,
            that.initials);
    }

    static final Resources resources = Resources
            .getDashBundle("WBSEditor.Team");

    public static final String TAG_NAME = "teamMember";
    public static final String ID_ATTR = "tmid";
    public static final String NAME_ATTR = "name";
    static final String SERVER_IDENTITY_ATTR = "serverIdentityData";
    public static final String INITIALS_ATTR = "initials";
    static final String COLOR_ATTR = "color";
    static final String SUBTEAMS_ATTR = "subteams";
    static final String SUBTEAMS_DELIM = ",\t";
    static final String EXTRA_ATTRS = "extraAttributes";

}
