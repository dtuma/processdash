

package teamdash;

import java.awt.Color;
import java.io.IOException;
import java.io.Writer;

import org.w3c.dom.Element;

import pspdash.XMLUtils;

public class TeamMember implements Cloneable {

    private String name, initials;
    private Double hoursPerWeek;
    private Color color;


    public TeamMember() { }

    public TeamMember(String name, String initials, Color color) {
        this.name = trim(name);
        this.initials = trim(initials);
        this.color = color;
        this.hoursPerWeek = new Double(20);
    }

    public TeamMember(Element e) {
        this.name = trim(e.getAttribute(NAME_ATTR));
        this.initials = trim(e.getAttribute(INITIALS_ATTR));
        String attr = e.getAttribute(HOURS_ATTR);
        if (XMLUtils.hasValue(attr)) try {
            hoursPerWeek = new Double(attr);
        } catch (NumberFormatException nfe) {}
        attr = e.getAttribute(COLOR_ATTR);
        if (XMLUtils.hasValue(attr)) try {
            color = Color.decode(attr);
        } catch (NumberFormatException nfe) {}
    }

    private String trim(String str) {
        if (str == null) return null;
        str = str.trim();
        if (str.length() == 0) return null;
        return str;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = trim(name); }

    public String getInitials() { return initials; }
    public void setInitials(String initials) { this.initials = trim(initials); }

    public Color getColor() {
        return (color == null ? Color.white : color);
    }
    public void setColor(Color color) { this.color = color; }

    public Double getHoursPerWeek() { return hoursPerWeek; }
    public void setHoursPerWeek(Double d) { hoursPerWeek = d; }

    public boolean isEmpty() {
        if (name != null && name.trim().length() > 0) return false;
        if (initials != null && initials.trim().length() > 0) return false;
        return true;
    }

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
        out.write("' "+COLOR_ATTR+"='");
        if (color != null) {
            out.write("#");
            out.write(Integer.toHexString(color.getRGB()).substring(2));
        }
        out.write("'/>\n");
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException cnse) {
            // can't happen?
            return null;
        }
    }

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
    private static final String HOURS_ATTR = "hoursPerWeek";
    private static final String COLOR_ATTR = "color";

}
