package teamdash.wbs.columns;

import teamdash.wbs.ReadOnlyValue;

public class ReadOnlyInvisibleValue extends ReadOnlyValue {
    public Object invisibleValue;
    public ReadOnlyInvisibleValue(Object value) {
        super("");
        invisibleValue = value;
    }
}
