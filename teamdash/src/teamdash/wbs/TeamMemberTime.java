package teamdash.wbs;

import java.awt.Color;

/** Contains the information needed to display the time for a team member.
 *
 * Basically, this is just a numeric value with an associated
 * background color.
 */
public class TeamMemberTime extends NumericDataValue {

    public Color color;

    public TeamMemberTime(NumericDataValue v, Color c) {
        super(v.value, v.isEditable, v.isInvisible,
              v.errorMessage, v.expectedValue);
        this.color = c;
    }

}
