// Copyright (C) 2006-2008 Tuma Solutions, LLC
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
import java.util.Map;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.cms.SnippetDataEnumerator;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

public class AbstractChartSnippet extends TinyCGIBase {

    protected static final Resources resources = Resources
            .getDashBundle("Analysis");

    protected StringBuffer getQueryParams() {
        StringBuffer result = new StringBuffer();
        copyParam(result, "Title", "title");
        Map[] metrics = SnippetDataEnumerator.getEnumeratedValues(parameters,
                "Metric");
        if (metrics != null)
            for (int i = 0; i < metrics.length; i++) {
                int n = i + 1;
                appendParam(result, "d" + n, metrics[i].get("DataName"));
                appendParam(result, "h" + n, metrics[i].get("DisplayName"));
                appendExtraMetricParams(result, metrics[i], n);
            }
        return result;
    }

    protected void appendExtraMetricParams(StringBuffer result, Map map, int i) {
        // subclasses may override
    }

    protected void copyParam(StringBuffer buf, String srcAttr, String destAttr) {
        String attrVal = getParameter(srcAttr);
        appendParam(buf, destAttr, attrVal);
    }

    protected void appendParam(StringBuffer buf, String name, Object val) {
        if (val instanceof String) {
            if (StringUtils.hasValue((String) val))
                buf.append('&').append(name).append('=').append(
                        HTMLUtils.urlEncode((String) val));
        } else if (val != null) {
            buf.append('&').append(name);
        }
    }

    protected void writeSmallChart(String chartType, String query)
            throws IOException {
        writeSmallChart(chartType, query, "");
    }
    protected void writeSmallChart(String chartType, String query,
            String extraQueryForSmallVersion) throws IOException {
        Object exporting = parameters.get("EXPORT");
        if ("excel".equals(exporting)) return;

        StringBuffer href = new StringBuffer("../../reports/");
        if (exporting != null)
            href.append("table.class");
        else
            href.append("full.htm");
        href.append("?chart=").append(chartType).append(query);

        String imageUri = "../../reports/" + chartType
                + ".class?html&qf=small.rpt" + query
                + extraQueryForSmallVersion
                + "&href=" + HTMLUtils.urlEncode(href.toString());
        String imageHtml = getRequestAsString(resolveRelativeURI(imageUri));
        out.write(OVERLIB_HEADER_HTML);
        out.write(imageHtml);
        out.write("\n</body></html>\n");
    }

    private static final String OVERLIB_HEADER_HTML = "<html><head>"
        + "<script type='text/javascript' src='/lib/overlib.js'></script>"
        + "</head><body>";

}
