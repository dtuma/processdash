// Copyright (C) 2006 Tuma Solutions, LLC
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

public class RadarChart extends AbstractChartSnippet {

    protected void writeContents() throws IOException {
        StringBuffer args = getQueryParams();
        appendParam(args, "for", ".");

        String query = args.toString();
        if (query.indexOf("&d3") == -1)
            out.print("<!-- at least three metrics must be selected -->\n\n");
        else
            writeSmallChart("radar", query, "&skipAxisLabels");
    }

    protected void appendExtraMetricParams(StringBuffer result, Map map, int i) {
        super.appendExtraMetricParams(result, map, i);
        appendParam(result, "t"+i, map.get("TargetValue"));
        appendParam(result, "r"+i, map.get("ReverseAxis"));
    }


}
