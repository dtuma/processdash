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

import java.io.IOException;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.applet.InterpreterFactory;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.ui.web.dash.ClipboardHelper;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

public class MetricsAlert extends TinyCGIBase {

    public static final String HEADER_ITEMS = "<link rel=\"stylesheet\" type=\"text/css\" href=\"/dash/snippets/alert.css\">";

    public enum AlertType { Error, Warning, Info, Check };

    protected static final Resources resources = Resources
            .getDashBundle("Analysis.TestExpression");

    public static String getAlertHtml(AlertType type, String alertText,
            boolean supportClipboard) {
        StringBuilder result = new StringBuilder();
        result.append("<div class=\"alert").append(type).append("\">");
        if (supportClipboard)
            result.append(ClipboardHelper.getHyperlinkTag(alertText));
        result.append(HTMLUtils.escapeEntities(alertText));
        if (supportClipboard)
            result.append("</a>");
        result.append("</div>");
        return result.toString();
    }

    @Override
    protected void writeContents() throws IOException {
        if (alertIsDisabled())
            return;

        String expression = getParameter("Expression");
        if (!StringUtils.hasValue(expression))
            return;

        try {
            SimpleData value = getDataRepository().evaluate(expression,
                getPrefix());
            writeContentsForValue(value);
        } catch (Exception e) {
            out.println("<html><head>");
            out.println(HEADER_ITEMS);
            out.println("</head><body><div class=\"alertMalformedExpression\">");
            out.print(resources.format("Expression_Error_HTML_FMT",
                    HTMLUtils.escapeEntities(expression)));
            out.println("</div></body></html");
        }
    }

    protected void writeContentsForValue(SimpleData value) {
        boolean result = (value != null && value.test());

        String which = (result ? "True" : "False");
        String messageType = getParameter(which + "MessageType");
        if (!StringUtils.hasValue(messageType) || "Nothing".equals(messageType))
            return;

        AlertType type;
        try {
            type = AlertType.valueOf(messageType);
        } catch (Exception e) {
            out.println("<!-- Unrecognized message type '" + messageType
                    + "' -->");
            return;
        }

        String messageTemplate = getParameter(which + "Message");
        if (messageTemplate != null && messageTemplate.startsWith("${"))
            messageTemplate = resources.interpolate(messageTemplate);
        printMessageFromTemplate(type, messageTemplate);
    }

    protected void printMessageFromTemplate(AlertType type,
            String messageTemplate) {
        if (type == null || !StringUtils.hasValue(messageTemplate))
            return;

        String message = expandVariables(messageTemplate);
        boolean supportClipboard = supportClipboard(type);
        out.println("<html><head>");
        out.println(HEADER_ITEMS);
        if (supportClipboard)
            out.println(ClipboardHelper.HEADER_ITEMS);
        out.println("</head><body>");
        out.println(getAlertHtml(type, message, supportClipboard));
        out.println("</body></html");
    }

    protected boolean alertIsDisabled() {
        String disabledBy = getParameter("disabledBy");
        if (disabledBy != null && Settings.getBool(disabledBy, false))
            return true;

        String enabledBy = getParameter("enabledBy");
        if (enabledBy != null && !Settings.getBool(enabledBy, false))
            return true;

        return false;
    }

    protected boolean supportClipboard(AlertType type) {
        return false;
    }

    protected String expandVariables(String message) {
        int startPos = 0;
        while (true) {
            int endBracket = message.indexOf(']', startPos);
            if (endBracket == -1)
                return message;
            int startBracket = message.lastIndexOf('[', endBracket);
            if (startBracket == -1) {
                startPos = endBracket+1;
            } else {
                String name = message.substring(startBracket+1, endBracket);
                SimpleData value = getDataContext().getSimpleValue(name);
                String replacement = formatData(name, value);
                message = message.substring(0, startBracket) + replacement
                        + message.substring(endBracket + 1);
                startPos = startBracket + replacement.length();
            }
        }
    }

    private String formatData(String dataName, SimpleData value) {
        if (value == null)
            return "null";

        if (value instanceof DoubleData) {
            double d = ((DoubleData) value).getDouble();
            if (InterpreterFactory.isPercentInputDataName(dataName) )
                return FormatUtil.formatPercent(d);
            else if (InterpreterFactory.isTimeInputDataName(dataName))
                return FormatUtil.formatTime(d);
            else
                return FormatUtil.formatNumber(d);
        }

        return value.format();
    }

}
