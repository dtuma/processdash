package teamdash.team;

import java.util.Date;

public interface EffortCalendar {

    /** Return the date a particular effort value could be reached. */
    public Date getDateForEffort(double hours);

}
