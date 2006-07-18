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

import java.io.IOException;

public class PhaseChart extends AbstractChartSnippet {

    protected void writeContents() throws IOException {
        StringBuffer args = getQueryParams();

        String chartType = getParameter("ChartType");
        chartType = (chartType == null ? "pie" : chartType.toLowerCase());

        String phases = getParameter("PhaseGroup");
        if (phases == null || "All".equals(phases))
            phases = "[Phase_List]";
        else
            phases = "[" + phases + "_Phase_List]";
        appendParam(args, "for", phases);
        appendParam(args, "h0", "Phase");
        appendParam(args, "units", getParameter("YAxisLabel"));

        String fullURL = (parameters.containsKey("EXPORT")
                ? "table.class" : "full.htm");

        out.write("<a href=\"../../reports/");
        out.write(fullURL);
        out.write("?chart=");
        out.write(chartType);
        out.write(args.toString());
        out.write("\" title=\"");
        out.write(resources.getHTML("More_Detail_Here_Instruction"));
        out.write("\"><img src=\"../../reports/");
        out.write(chartType);
        out.write(".class?qf=small.rpt");
        out.write(args.toString());
        out.write("\"/></a>\n");
    }

}
