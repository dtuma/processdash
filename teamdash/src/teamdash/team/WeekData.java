package teamdash.team;

public class WeekData {

    public static final int TYPE_DEFAULT = 0;

    public static final int TYPE_EXCEPTION = 1;

    public static final int TYPE_OUTSIDE_SCHEDULE = 2;

    public static final int TYPE_START = 3;

    public static final int TYPE_END = 4;


    public static final WeekData WEEK_START = new WeekData(0, TYPE_START);

    public static final WeekData WEEK_END = new WeekData(0, TYPE_END);

    public static final WeekData WEEK_OUTSIDE_SCHEDULE = new WeekData(0,
            TYPE_OUTSIDE_SCHEDULE);


    /** The number of hours in the given week */
    private double hours;

    private int type;


    public WeekData(double time, int type) {
        this.hours = time;
        this.type = type;
    }

    public double getHours() {
        return hours;
    }

    public int getType() {
        return type;
    }

    public boolean isInsideSchedule() {
        return isInsideSchedule(type);
    }

    public static boolean isInsideSchedule(int type) {
        return type == WeekData.TYPE_DEFAULT || type == WeekData.TYPE_EXCEPTION;
    }

}
