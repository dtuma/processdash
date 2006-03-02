
package teamdash.wbs;

/** Simple class to describe a value with an associated error
 */
public class ErrorValue extends WrappedValue {

    public static final int ERROR = 0;
    public static final int WARNING = 1;

    /** The error message */
    public String error;
    /** The severity of the error */
    public int severity;

    /** Create an ErrorValue with a severity of {@link #ERROR} */
    public ErrorValue(Object value, String errorMsg) {
        this(value, errorMsg, ERROR);
    }

    /** Create an ErrorValue */
    public ErrorValue(Object value, String errorMsg, int severity) {
        this.value = value;
        this.error = errorMsg;
        this.severity = severity;
    }

    public String toString() {
        return String.valueOf(value);
    }
}
