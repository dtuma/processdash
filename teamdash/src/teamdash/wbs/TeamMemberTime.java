package teamdash.wbs;

import java.awt.Color;

public class TeamMemberTime extends NumericDataValue {

    public Color color;

    public TeamMemberTime(NumericDataValue v, Color c) {
        super(v.value, v.isEditable, v.isInvisible,
              v.errorMessage, v.expectedValue);
        this.color = c;
    }

}
