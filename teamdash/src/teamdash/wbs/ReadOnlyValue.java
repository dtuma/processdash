
package teamdash.wbs;

public class ReadOnlyValue {

    public Object value;

    public ReadOnlyValue(Object value) {
        this.value = value;
    }

    public String toString() {
        return String.valueOf(value);
    }
}
