
package teamdash;

import java.awt.Color;
import java.io.IOException;
import java.io.Writer;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;



/** Holds information about a individual member of a project team.
 */
public class TeamMember implements Cloneable {

    /** The full name of the team member (e.g. John Doe)*/
    private String name;
    /** The initials of the team member (e.g. jd) */
    private String initials;
    /** The date this individual will start working */
    private Date startDate;
    /** The approximate number of hours this team member will be able to spend
     * per week on project tasks */
    private Double hoursPerWeek;
    /** A color that will be used to represent this individual in various
     * charts, tables, and displays */
    private Color color;


    /** public TeamMember() { } */

    /** Create a new team member.
     *
     * A default time allocation of 20 hours per week will be used.
     * @param name the name of the individual
     * @param initials the person's initials
     * @param color the color to use for this person
     */
    public TeamMember(String name, String initials, Color color, Date start) {
        this.name = trim(name);
        this.initials = trim(initials);
        this.color = color;
        this.startDate = start;
        this.hoursPerWeek = new Double(20);
    }

    /** Create a team member object using data in the given XML element. */
    public TeamMember(Element e) {
        // extract the name from the element.
        this.name = trim(e.getAttribute(NAME_ATTR));
        // extract the initials from the element.
        this.initials = trim(e.getAttribute(INITIALS_ATTR));
        // extract hours per week information if it is present.
        String attr = e.getAttribute(HOURS_ATTR);
        if (XMLUtils.hasValue(attr)) try {
            hoursPerWeek = new Double(attr);
        } catch (NumberFormatException nfe) {}
        this.startDate = XMLUtils.getXMLDate(e, START_DATE_ATTR);
        // extract color information if it is present.
        attr = e.getAttribute(COLOR_ATTR);
        if (XMLUtils.hasValue(attr)) try {
            color = Color.decode(attr);
        } catch (NumberFormatException nfe) {}
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
    public Date getStartDate() { return startDate; }
    public void setStartDate(Date d) { startDate = d; }

    // getter/setter for the "hours per week" property.
    public Double getHoursPerWeek() { return hoursPerWeek; }
    public void setHoursPerWeek(Double d) { hoursPerWeek = d; }

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
        out.write("' "+HOURS_ATTR+"='");
        if (hoursPerWeek != null)
            out.write(hoursPerWeek.toString());
        out.write("' "+START_DATE_ATTR+"='");
        if (startDate != null)
            out.write(XMLUtils.saveDate(startDate));
        out.write("' "+COLOR_ATTR+"='");
        if (color != null) {
            out.write("#");
            out.write(Integer.toHexString(color.getRGB()).substring(2));
        }
        out.write("'/>\n");
    }

    /** Make a copy of this team member object. */
    public Object clone() {
        try {
            return super.clone();
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
    private static final String START_DATE_ATTR = "startDate";
    private static final String HOURS_ATTR = "hoursPerWeek";
    private static final String COLOR_ATTR = "color";

}
