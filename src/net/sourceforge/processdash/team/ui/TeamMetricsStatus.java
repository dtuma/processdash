// Copyright (C) 2002-2022 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package net.sourceforge.processdash.team.ui;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import net.sourceforge.processdash.team.setup.TeamMemberDataStatus;
import net.sourceforge.processdash.templates.DashPackage;
import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;


public class TeamMetricsStatus extends TinyCGIBase {


    @Override
    protected void writeContents() throws IOException {
        String prefixListName = getParameter("for");
        List<TeamMemberDataStatus> importData = TeamMemberDataStatus
                .get(getDataRepository(), getPrefix(), prefixListName);

        if (parameters.get("xml") != null)
            writeXml(importData);
        else
            writeHtml(importData);
    }



    private void writeXml(List<TeamMemberDataStatus> importData) {
        out.println("<?xml version='1.0' encoding='UTF-8'?>");
        out.println("<members>");
        for (TeamMemberDataStatus d : importData) {
            out.print("  <member name='");
            out.print(XMLUtils.escapeAttribute(d.ownerName));
            if (d.exportDate != null) {
                out.print("' lastExport='");
                out.print(XMLUtils.saveDate(d.exportDate));
            }
            if (d.wbsLastSync != null) {
                out.print("' lastSync='");
                out.print(XMLUtils.saveDate(d.wbsLastSync));
            }
            out.print("' dashVersion='");
            out.print(XMLUtils.escapeAttribute(d.dashVersion));
            out.println("' />");
        }
        out.println("</members>");
    }



    protected void writeHtml(List<TeamMemberDataStatus> importData) {
        out.println(HTML_HEADER);

        out.print("<h2>For ");
        out.print(HTMLUtils.escapeEntities(getPrefix()));
        if (!isExporting())
            out.print(" <a href='../../control/importNow.class?redirectToReferrer="
                    + (RELOAD++) + "' class='nav doNotPrint'>Refresh Data...</a>");
        out.print("</h2>\n");

        if (importData == null || importData.size() == 0)
            showNoTeamMembersFound();
        else
            printTeamMemberData(importData);

        out.print("<p class='doNotPrint'><a href=\"/reports/excel.iqy\"><i>Export to Excel</i></a></p>\n");
        out.print("</body></html>");
    }


    private void showNoTeamMembersFound() {
        out.print("<p><i>No team member data found for this project.</i></p>");
    }


    private void printTeamMemberData(List<TeamMemberDataStatus> importData) {
        String teamDashVersion = TemplateLoader.getPackageVersion(DASH_PKG_ID);

        out.println("<table border class='sortable' id='data'><tr>");
        out.println("<th>Team Member Name</th>");
        out.println("<th>Metrics Data Last Exported</th>");
        out.println("<th>Last Sync to WBS</th>");
        out.println("<th>Process Dashboard Version Number</th></tr>");

        Collections.sort(importData);
        for (TeamMemberDataStatus d : importData) {
            printTeamMemberRow(d, teamDashVersion);
        }

        out.println("</table>");

        out.print("<p><i>You are currently running version ");
        out.print(HTMLUtils.escapeEntities(teamDashVersion));
        out.println(" of the Team Dashboard.</i></p>");
    }


    private void printTeamMemberRow(TeamMemberDataStatus d,
            String teamDashVersion) {
        out.print("<tr><td>");

        if (StringUtils.hasValue(d.ownerName))
            out.print(HTMLUtils.escapeEntities(d.ownerName));
        else
            out.print("<i>Name Unavailable</i>");

        if (d.hasDatasetIdCollision)
            out.print("&nbsp;<img src='/Images/warningRed.gif' "
                    + "title='Colliding value in datasetID.dat; "
                    + "project calculations may be incorrect'>");

        out.print("</td>");

        printDateCell(d.exportDate);
        printDateCell(d.wbsLastSync);

        if (!StringUtils.hasValue(d.dashVersion)) {
            out.print("<td></td>");
        } else {
            out.print("<td");
            if (DashPackage.compareVersions(d.dashVersion, teamDashVersion) < 0)
                out.print(OUT_OF_DATE_ATTRS);
            out.print(" sortkey='" + d.getVersionSortKey() + "'>");
            out.print(HTMLUtils.escapeEntities(d.dashVersion));
            out.print("</td>");
        }

        out.println("</tr>");
    }

    private void printDateCell(Date d) {
        if (d == null) {
            out.print("<td></td>");
        } else {
            out.print("<td sortkey='" + d.getTime() + "'>");
            out.print(HTMLUtils.escapeEntities(FormatUtil
                .formatDateTime(d)));
            out.print("</td>");
        }
    }



    private static int RELOAD = (int) ((System.currentTimeMillis() >> 10) & 255);

    private static final String HTML_HEADER = "<html><head>\n"
            + "<link rel=stylesheet type='text/css' href='/style.css'>\n"
            + "<link rel=stylesheet type='text/css' href='/lib/sorttable.css'>\n"
            + "<script type='text/javascript' src='/lib/sorttable.js'></script>\n"
            + "<title>Status of Team Metrics</title><style>\n"
            + "td.outOfDate { background-color: #ffcccc }\n"
            + "a.nav { font-style: italic; font-size: medium; " +
                        "font-weight: normal; margin-left: 1em }\n"
            + "</style></head><body>\n"
            + "<h1>Status of Team Metrics</h1>";


    private static final String DASH_PKG_ID = "pspdash";

    private static final String OUT_OF_DATE_ATTRS = " class='outOfDate' "
            + "title='This individual is running an older software version "
            + "than the Team Dashboard'";

}
