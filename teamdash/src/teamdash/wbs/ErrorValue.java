
package teamdash.wbs;

public class ErrorValue {

    public static final int ERROR = 0;
    public static final int WARNING = 1;

    public Object value;
    public String error;
    public int severity;

    public ErrorValue(Object value, String errorMsg) {
        this(value, errorMsg, ERROR);
    }

    public ErrorValue(Object value, String errorMsg, int severity) {
        this.value = value;
        this.error = errorMsg;
        this.severity = severity;
    }
}
