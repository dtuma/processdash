
package teamdash.wbs;


/** A simple wrapper object which can be used to indicate that a value
 * is read only.
 */
public class ReadOnlyValue {

    public Object value;

    public ReadOnlyValue(Object value) {
        this.value = value;
    }

    public String toString() {
        return String.valueOf(value);
    }
}
