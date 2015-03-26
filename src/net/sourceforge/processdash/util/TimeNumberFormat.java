package net.sourceforge.processdash.util;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeNumberFormat extends NumberFormat {

    public Number parse(String source, ParsePosition parsePosition) {
        int pos = parsePosition.getIndex();
        source = source.substring(pos);

        Matcher m = HOUR_MINUTE_PATTERN.matcher(source);
        if (m.lookingAt() && (m.group(2) != null || m.group(3) != null)) {
            parsePosition.setIndex(pos + m.end());
            long result = 0;
            if (m.group(2) != null)
                result += 60 * Long.parseLong(m.group(2));
            if (m.group(3) != null)
                result += Long.parseLong(m.group(3));
            if (m.group(1) != null)
                result = -1 * result;
            return new Long(result);
        }

        m = MINUTE_ONLY_PATTERN.matcher(source);
        if (m.lookingAt()) {
            parsePosition.setIndex(pos + m.end());
            long result = Long.parseLong(m.group(2));
            if (m.group(1) != null)
                result = -1 * result;
            return new Long(result);
        }

        return null;
    }
    private static Pattern HOUR_MINUTE_PATTERN =
        Pattern.compile("\\s*(-)?\\s*(\\d+)?\\s*:\\s*(\\d+)?");
    private static Pattern MINUTE_ONLY_PATTERN =
        Pattern.compile("\\s*(-)?\\s*:?(\\d+)");


    public StringBuffer format(double number, StringBuffer toAppendTo,
            FieldPosition pos) {
        return format((long) Math.floor(number + 0.5), toAppendTo, pos);
    }

    public StringBuffer format(long time, StringBuffer toAppendTo,
            FieldPosition pos) {
        long hours = (long) (time / 60);
        long minutes = Math.abs(time - (hours * 60));
        if (time < 0)
            toAppendTo.append("-");
        toAppendTo.append(Math.abs(hours)).append(':');
        if (minutes < 10)
            toAppendTo.append('0');
        toAppendTo.append(minutes);

        return toAppendTo;
    }

}
