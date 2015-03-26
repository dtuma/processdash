// Copyright (C) 2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.reports.snippets;

import java.util.Map;

import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.net.cms.SnippetDataEnumerator;
import net.sourceforge.processdash.util.StringUtils;

public class MetricsRangeAlert extends MetricsAlert {

    private static final String ITEM_TYPE = "Range";
    private static final String OPERATOR = "Operator";
    private static final String NUMBER = "Number";
    private static final String MSG_TYPE = "MessageType";
    private static final String MSG_TEXT = "Message";

    public enum Operator {
        LT { public boolean op(double a, double b) { return a < b; } },
        LE { public boolean op(double a, double b) { return a <= b; } },
        EQ { public boolean op(double a, double b) { return a == b; } },
        GE { public boolean op(double a, double b) { return a >= b; } },
        GT { public boolean op(double a, double b) { return a > b; } },
        ;

        public abstract boolean op(double a, double b);
    };

    @Override
    protected void writeContentsForValue(SimpleData value) {
        if (!(value instanceof DoubleData))
            return;

        double d = ((DoubleData) value).getDouble();
        if (Double.isNaN(d) || Double.isInfinite(d))
            return;

        Map[] ranges = SnippetDataEnumerator.getEnumeratedValues(parameters,
            ITEM_TYPE);
        if (ranges == null) {
            out.write("<!-- no ranges defined;  nothing to display -->\n\n");
            return;
        }

        for (int i = 0; i < ranges.length; i++) {
            if (maybePrintRange(ranges[i], d))
                return;
        }

        out.write("<!-- no ranges matched;  nothing to display -->\n\n");
    }

    private boolean maybePrintRange(Map range, double value) {
        Operator operator;
        double number;
        try {
            operator = Operator.valueOf((String) range.get(OPERATOR));
            number = Double.parseDouble((String) range.get(NUMBER));
        } catch (Exception e) {
            // unrecognized or missing operator or number
            return false;
        }

        // test the relational expression to see if we match.
        if (operator.op(value, number) == false)
            return false;

        // load the message type
        String messageType = (String) range.get(MSG_TYPE);
        if (!StringUtils.hasValue(messageType) || "Nothing".equals(messageType))
            return true;

        AlertType type;
        try {
            type = AlertType.valueOf(messageType);
        } catch (Exception e) {
            // invalid message type - treat like "Nothing".
            out.println("<!-- Unrecognized message type '" + messageType
                    + "' -->");
            return true;
        }

        String messageTemplate = (String) range.get(MSG_TEXT);
        printMessageFromTemplate(type, messageTemplate);
        return true;
    }

}
