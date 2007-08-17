
package teamdash.team;

import java.awt.Color;
import java.io.IOException;
import java.io.Writer;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import teamdash.XMLUtils;




/** Holds information about a individual member of a project team.
 */
public class TeamMember implements Cloneable {

    /** The full name of the team member (e.g. John Doe)*/
    private String name;
    /** The initials of the team member (e.g. jd) */
    private String initials;
    /** A color that will be used to represent this individual in various
     * charts, tables, and displays */
    private Color color;
    /** The schedule for this team member */
    private WeeklySchedule schedule;


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
        this.name = trim(name);
        this.initials = trim(initials);
        this.color = color;
        this.schedule = new WeeklySchedule(zeroDay, startWeek);
    }

    /** Create a team member object using data in the given XML element. */
    public TeamMember(Element e, Date teamZeroDay) {
        // extract the name from the element.
        this.name = trim(e.getAttribute(NAME_ATTR));
        // extract the initials from the element.
        this.initials = trim(e.getAttribute(INITIALS_ATTR));
        // extract color information if it is present.
        String attr = e.getAttribute(COLOR_ATTR);
        if (XMLUtils.hasValue(attr)) try {
            color = Color.decode(attr);
        } catch (NumberFormatException nfe) {}
        // extract schedule information
        this.schedule = new WeeklySchedule(e, teamZeroDay);
    }

    /** Convenience method - trim whitespace from the ends of a string.
     * if the result would be an empty string, this will return null. */
    private static String trim(String str) {
        if (str == null) return null;
        str = str.trim();
        if (str.length() == 0) return null;
        return str;
    }

    // getter/setter for the name property.
    public String getName() { return name; }
    public void setName(String name) { this.name = trim(name); }

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
    public void setColor(Color color) { this.color = color; }

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

    /** returns true if this individual has no name or initials. */
    public boolean isEmpty() {
        if (name != null && name.trim().length() > 0) return false;
        if (initials != null && initials.trim().length() > 0) return false;
        return true;
    }

    /** Serialize this team member to XML and write it to the given Writer. */
    public void getAsXML(Writer out) throws IOException {
        out.write("  <"+TAG_NAME+" "+NAME_ATTR+"='");
        if (name != null)
            out.write(XMLUtils.escapeAttribute(name));
        out.write("' "+INITIALS_ATTR+"='");
        if (initials != null)
            out.write(XMLUtils.escapeAttribute(initials));
        out.write("' "+COLOR_ATTR+"='");
        if (color != null) {
            out.write("#");
            out.write(Integer.toHexString(color.getRGB()).substring(2));
        }
        out.write("'");

        schedule.writeAttributes(out);
        if (schedule.hasExceptions()) {
            out.write(">\n");
            schedule.writeExceptions(out);
            out.write("  </"+TAG_NAME+">\n");
        } else {
            out.write("/>\n");
        }
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
     * @param considerInitials if true, this method will take the initials into
     * consideration during the comparison.
     * @return One of the following:<ul>
     * <li>the empty string if the two objects represent the same individual
     * (both name and initials match).
         * <li>null if the two objects don't appear to represent the same
         * individual (the names don't match, and if considerInitials is true,
         * neither do the initials)
     * <li>A user-readable string describing the change that would need to be
     * made to this object for it look like the object passed in.
     */
    public String compareToMember(TeamMember that, boolean considerInitials) {
        boolean differentName = (this.name == null ||
                                 !this.name.equals(that.name));
        if (differentName && !considerInitials) return null;
        boolean differentInitials = (this.initials == null ||
                                     !this.initials.equals(that.initials));
        if (differentName && differentInitials) return null;

        String result = "";

        if (differentName)
            result = "Rename " + this.name + " to '" + that.name + "'";
        else if (differentInitials)
            result = "Change initials for " + this.name + " to '" +
                that.initials + "'";

        return result;
    }

    static final String TAG_NAME = "teamMember";
    private static final String NAME_ATTR = "name";
    private static final String INITIALS_ATTR = "initials";
    private static final String COLOR_ATTR = "color";

}
