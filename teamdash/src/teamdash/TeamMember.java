

package teamdash;

import java.awt.Color;

public class TeamMember {

    private String name, initials;
    private Double hoursPerWeek;
    private Color color;


    public TeamMember() { }

    public TeamMember(String name, String initials, Color color) {
        this.name = name;
        this.initials = initials;
        this.color = color;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getInitials() { return initials; }
    public void setInitials(String initials) { this.initials = initials; }

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

}
