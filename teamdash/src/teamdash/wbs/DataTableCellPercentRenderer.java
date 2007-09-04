package teamdash.wbs;

import java.text.NumberFormat;

public class DataTableCellPercentRenderer extends DataTableCellNumericRenderer {

    public static DataTableCellPercentRenderer INSTANCE =
        new DataTableCellPercentRenderer(0);

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
        else
            return formatter.format(number.value);
    }
}
