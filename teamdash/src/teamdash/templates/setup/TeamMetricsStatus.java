// Copyright (C) 2002-2010 Tuma Solutions, LLC
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

package teamdash.templates.setup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
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
        ListData prefixList = ListData.asListData(getDataContext()
                .getSimpleValue(prefixListName));
        List<ImportData> importData = getImportData(prefixList);

        if (parameters.get("xml") != null)
            writeXml(importData);
        else
            writeHtml(importData);
    }



    private void writeXml(List<ImportData> importData) {
        out.println("<?xml version='1.0' encoding='UTF-8'?>");
        out.println("<members>");
        for (ImportData d : importData) {
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



    protected void writeHtml(List<ImportData> importData) {
        out.println(HTML_HEADER);

        out.print("<h2>For ");
        out.print(HTMLUtils.escapeEntities(getPrefix()));
        out.print(" <a href='../../control/importNow.class?redirectToReferrer="
                + (RELOAD++) + "' class='nav'>Refresh Data...</a></h2>");

        if (importData == null || importData.size() == 0)
            showNoTeamMembersFound();
        else
            printTeamMemberData(importData);

        out.print("</body></html>");
    }


    private List<ImportData> getImportData(ListData prefixList) {
        List<ImportData> result = new ArrayList<ImportData>();
        if (prefixList != null) {
            for (int i = prefixList.size(); i-- > 0;) {
                String onePrefix = StringUtils.asString(prefixList.get(i));
                String metadataPrefix = getMetadataPrefix(onePrefix);
                if (metadataPrefix != null)
                    result.add(new ImportData(onePrefix, metadataPrefix));
            }
        }
        return result;
    }

    private String getMetadataPrefix(String prefix) {
        if (prefix == null)
            return null;
        StringBuffer result = new StringBuffer(prefix);
        if (getDataRepository().getInheritableValue(result, METADATA) != null)
            return result.toString();
        else
            return null;
    }


    private void showNoTeamMembersFound() {
        out.print("<p><i>No team member data found for this project.</i></p>");
    }


    private void printTeamMemberData(List<ImportData> importData) {
        String teamDashVersion = TemplateLoader.getPackageVersion(DASH_PKG_ID);

        out.println("<table border><tr>");
        out.println("<th>Team Member Name</th>");
        out.println("<th>Metrics Data Last Exported</th>");
        out.println("<th>Last Sync to WBS</th>");
        out.println("<th>Process Dashboard Version Number</th></tr>");

        Collections.sort(importData);
        for (ImportData d : importData) {
            printTeamMemberRow(d, teamDashVersion);
        }

        out.println("</table>");

        out.print("<p><i>You are currently running version ");
        out.print(HTMLUtils.escapeEntities(teamDashVersion));
        out.println(" of the Team Dashboard.</i></p>");
    }


    private void printTeamMemberRow(ImportData d, String teamDashVersion) {
        out.print("<tr><td>");

        if (StringUtils.hasValue(d.ownerName))
            out.print(HTMLUtils.escapeEntities(d.ownerName));
        else
            out.print("<i>Name Unavailable</i>");

        out.print("</td><td>");

        if (d.exportDate != null)
            out.print(HTMLUtils.escapeEntities(FormatUtil
                    .formatDateTime(d.exportDate)));
        out.print("</td><td>");

        if (d.wbsLastSync != null)
            out.print(HTMLUtils.escapeEntities(FormatUtil
                .formatDateTime(d.wbsLastSync)));
        out.print("</td>");

        if (!StringUtils.hasValue(d.dashVersion)) {
            out.print("<td></td>");
        } else {
            out.print("<td");
            if (DashPackage.compareVersions(d.dashVersion, teamDashVersion) < 0)
                out.print(OUT_OF_DATE_ATTRS);
            out.print(">");
            out.print(HTMLUtils.escapeEntities(d.dashVersion));
            out.print("</td>");
        }

        out.println("</tr>");
    }

    private class ImportData implements Comparable<ImportData> {
        String prefix;

        String ownerName;

        Date exportDate;

        String dashVersion;

        Date wbsLastSync;

        public ImportData(String prefix, String metadataPrefix) {
            this.ownerName = getString(OWNER_VAR, metadataPrefix);
            this.exportDate = getDate(TIMESTAMP_VAR, metadataPrefix);
            this.dashVersion = getString(DASH_VERSION_VAR, metadataPrefix);
            this.wbsLastSync = getDate(TeamDataConstants.LAST_SYNC_TIMESTAMP, prefix);
        }

        private SimpleData get(String name, String prefix) {
            String dataName = DataRepository.createDataName(prefix, name);
            return getDataRepository().getSimpleValue(dataName);
        }

        private String getString(String name, String prefix) {
            SimpleData val = get(name, prefix);
            return (val == null ? "" : val.format());
        }

        private Date getDate(String name, String prefix) {
            SimpleData val = get(name, prefix);
            if (val instanceof DateData) {
                return ((DateData) val).getValue();
            } else {
                return null;
            }
        }

        public int compareTo(ImportData that) {
            return this.ownerName.compareTo(that.ownerName);
        }
    }

    private static int RELOAD = (int) ((System.currentTimeMillis() >> 10) & 255);

    private static final String HTML_HEADER = "<html><head>\n"
            + "<title>Status of Team Metrics</title><style>\n"
            + "td.outOfDate { background-color: #ffcccc }\n"
            + "a.nav { font-style: italic; font-size: medium; " +
                        "font-weight: normal; margin-left: 1em }\n"
            + "</style></head><body>\n"
            + "<h1>Status of Team Metrics</h1>";


    private static final String DASH_PKG_ID = "pspdash";

    private static final String METADATA = "Import_Metadata";

    private static final String OWNER_VAR = METADATA + "/exported.byOwner";

    private static final String TIMESTAMP_VAR = METADATA + "/exported.when";

    private static final String DASH_VERSION_VAR = METADATA
            + "/exported.withPackage/" + DASH_PKG_ID;

    private static final String OUT_OF_DATE_ATTRS = " class='outOfDate' "
            + "title='This individual is running an older software version "
            + "than the Team Dashboard'";

}
