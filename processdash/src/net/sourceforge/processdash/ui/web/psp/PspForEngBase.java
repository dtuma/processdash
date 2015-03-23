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

package net.sourceforge.processdash.ui.web.psp;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.snippet.SnippetEnvironment;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.ui.web.dash.ClipboardHelper;
import net.sourceforge.processdash.ui.web.reports.snippets.MetricsAlert;
import net.sourceforge.processdash.ui.web.reports.snippets.MetricsAlert.AlertType;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

public class PspForEngBase extends TinyCGIBase {

    protected static final String HEADER = "<html><head>"
            + ClipboardHelper.HEADER_ITEMS + MetricsAlert.HEADER_ITEMS
            + "</head><body>";

    protected static final String FOOTER = "</body></html>";

    protected void printStudentDataError(String text) throws IOException {
        out.println(MetricsAlert.getAlertHtml(AlertType.Error, text, true));
    }

    protected void printStudentDataOK(String text) throws IOException {
        out.println(MetricsAlert.getAlertHtml(AlertType.Check, text, true));
    }

    protected boolean getBoolParam(String name) {
        Object value = parameters.get(name);
        return (value != null && !"".equals(value));
    }

    protected double getDoubleData(String name) {
        SimpleData val = getDataContext().getSimpleValue(name);
        if (val instanceof DoubleData)
            return ((DoubleData) val).getDouble();
        else
            return 0;
    }

    protected double getDoubleParam(String name, double defaultVal) {
        try {
            return nf.parse(getParameter(name)).doubleValue();
        } catch (Exception e) {
            return defaultVal;
        }
    }

    protected NumberFormat nf = NumberFormat.getNumberInstance();

    protected boolean hasValue(String s) {
        return (s != null && s.trim().length() > 0);
    }

    protected String plural(List l, String singular, String plural) {
        return (l.size() == 1 ? singular : plural);
    }

    protected String joinList(List l, String conjunction) {
        int size = l.size();

        if (size == 0)
            return "";
        else if (size == 1)
            return String.valueOf(l.get(0));
        else if (size == 2)
            return l.get(0) + " " + conjunction + " " + l.get(1);
        else
            return StringUtils.join(l.subList(0, size - 1), ", ") + ", "
                    + conjunction + " " + l.get(size - 1);
    }

    private Resources resources = null;

    protected Resources res() {
        if (resources == null)
            resources = (Resources) env.get(SnippetEnvironment.RESOURCES);
        return resources;
    }

    protected static String esc(String s) {
        return HTMLUtils.escapeEntities(s);
    }

    protected static final List<String> PSP_LEVELS = Collections
            .unmodifiableList(Arrays.asList("PSP0", "PSP0.1", "PSP1", "PSP1.1",
                "PSP2", "PSP2.1", "PSP3"));

}
