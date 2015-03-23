// Copyright (C) 2008 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 3
// of the License, or (at your option) any later version.
//
// Additional permissions also apply; see the README-license.txt
// file in the project root directory for more information.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ev.ui.chart;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.FormatUtil;

public class ConfidenceIntervalToolTipGenerator extends EVXYToolTipGenerator {

    private static Resources resources =
        Resources.getDashBundle("EV.Snippet.Confidence_Interval_Chart");

    protected static final String TOOLTIP_FORMAT = "{0} {2} = {1}";

    public ConfidenceIntervalToolTipGenerator() {
        this(FormatUtil.getOneFractionDigitNumberFormat());
    }

    public ConfidenceIntervalToolTipGenerator(NumberFormat xFormat) {
        super(TOOLTIP_FORMAT, xFormat, PROBABILITY_FORMAT);
    }

    public ConfidenceIntervalToolTipGenerator(DateFormat xFormat) {
        super(TOOLTIP_FORMAT, xFormat, PROBABILITY_FORMAT);
    }


    private static class ProbabilityFormatter extends NumberFormat {

        @Override
        public StringBuffer format(double number, StringBuffer toAppendTo,
                FieldPosition notUsed) {
            if (Math.abs(number) < 0.01)
                toAppendTo.append(resources.getString("Most_Likely_Value"));
            else if (number < 0)
                toAppendTo.append(resources.format("LPI_FMT", -number/100.0));
            else
                toAppendTo.append(resources.format("UPI_FMT", number/100.0));

            return toAppendTo;
        }

        @Override
        public StringBuffer format(long number, StringBuffer toAppendTo,
                FieldPosition notUsed) {
            return format(number, toAppendTo, notUsed);
        }

        @Override
        public Number parse(String source, ParsePosition parsePosition) {
            throw new UnsupportedOperationException();
        }

    }

    private static final NumberFormat PROBABILITY_FORMAT = new ProbabilityFormatter();

}
