package teamdash.wbs;

import java.awt.Color;
import java.text.NumberFormat;
import java.text.ParseException;

public class NumericDataValue {

    public double value;
    public boolean isEditable = true;
    public boolean isInvisible = false;

    public String errorMessage;
    public Color errorColor = Color.red;
    public double expectedValue;

    public NumericDataValue(double v) {
        this(v, true, false, null, v);
    }

    public NumericDataValue(double v, boolean editable) {
        this(v, editable, false, null, v);
    }

    public NumericDataValue(double v, boolean editable, boolean invisible,
                            String error) {
        this(v, editable, invisible, error, v);
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
    /*
    public NumericDataValue(String value) {
        System.out.println("In NumericDataValue(string)");
        this.value = Double.NaN;
        if (value != null && value.length() != 0) try {
            this.value = FORMATTER.parse(value).doubleValue();
        } catch (ParseException pe) {}
    }
    */

    public String toString() {
        return format(value);
    }


    public static String format(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value))
            return "";
        else
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
            return FORMATTER.parse(aValue.toString()).doubleValue();
        } catch (ParseException nfe) { }

        return Double.NaN;
    }


    protected static final NumberFormat FORMATTER =
        NumberFormat.getNumberInstance();
    //protected static final NumberFormat NO_COMMA_FORMATTER =
        //NumberFormat.getNumberInstance();
    static {
        FORMATTER.setMaximumFractionDigits(1);
        //NO_COMMA_FORMATTER.setMaximumFractionDigits(1);
        //NO_COMMA_FORMATTER.
    }

}
