package teamdash.wbs;

import java.text.NumberFormat;

public class DataTableCellPercentRenderer extends DataTableCellNumericRenderer {

    public static DataTableCellPercentRenderer INSTANCE =
        new DataTableCellPercentRenderer(0);
    public static final double ONE_HUNDRED_PCT_CUTOFF = 0.99999;

    private NumberFormat formatter;

    public DataTableCellPercentRenderer(int fractionDigits) {
        formatter = NumberFormat.getPercentInstance();
        formatter.setMaximumFractionDigits(fractionDigits);
    }

    @Override
    protected String formatNumber(NumericDataValue number) {
        if (number == null || Double.isNaN(number.value)
                || Double.isInfinite(number.value))
            return "";
        else if (number.value > 0.994 && number.value < ONE_HUNDRED_PCT_CUTOFF)
            // round down for values that aren't quite 100%
            return formatter.format(0.99);
        else
            return formatter.format(number.value);
    }
}
