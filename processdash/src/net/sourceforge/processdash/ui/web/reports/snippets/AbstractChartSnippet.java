// Copyright (C) 2006 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ui.web.reports.snippets;

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
        for (int i = 0; i < metrics.length; i++) {
            int n = i + 1;
            appendParam(result, "d" + n, metrics[i].get("DataName"));
            appendParam(result, "h" + n, metrics[i].get("DisplayName"));
        }
        return result;
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

}
