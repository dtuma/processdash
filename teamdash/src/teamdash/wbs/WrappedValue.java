package teamdash.wbs;

public class WrappedValue {

    /** The real value */
    public Object value;

    public static Object unwrap(Object obj) {
        while (obj instanceof WrappedValue)
            obj = ((WrappedValue) obj).value;
        return obj;
    }

}
