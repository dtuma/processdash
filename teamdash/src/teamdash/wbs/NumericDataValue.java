package teamdash.wbs;

import java.text.NumberFormat;

public class NumericDataValue {

    public double value;
    public boolean isEditable = true;
    public boolean isInvisible = false;

    public String errorMessage;
    public double expectedValue;

    public NumericDataValue(double v) {
        this(v, true, false, null, Double.NaN);
    }

    public NumericDataValue(double v, boolean editable) {
        this(v, editable, false, null, Double.NaN);
    }

    public NumericDataValue(double v, boolean editable, boolean invisible,
                            String error) {
        this(v, editable, invisible, error, Double.NaN);
    }

    public NumericDataValue(double v, boolean editable, boolean invisible,
                            String error, double expected)
    {
        this.value = v;
        this.isEditable = editable;
        this.isInvisible = invisible;
        this.errorMessage = error;
        this.expectedValue = expected;
    }


    public String toString() {
        return format(value);
    }


    public static String format(double value) {
        return FORMATTER.format(value);
    }

    public static double parse(Object aValue) {

        if (aValue instanceof NumericDataValue)
            return ((NumericDataValue) aValue).value;
        if (aValue instanceof Number)
            return ((Number) aValue).doubleValue();
        if (aValue == null || "".equals(aValue))
            return 0;
        try {
            return Double.parseDouble(aValue.toString());
        } catch (NumberFormatException nfe) { }

        return Double.NaN;
    }


    protected static final NumberFormat FORMATTER =
        NumberFormat.getNumberInstance();
    static {
        FORMATTER.setMaximumFractionDigits(1);
    }

}
