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

public class TeamMetricsStatus extends TinyCGIBase {


    @Override
    protected void writeContents() throws IOException {
        out.println(HTML_HEADER);

        out.print("<h2>For ");
        out.print(HTMLUtils.escapeEntities(getPrefix()));
        out.print(" <a href='../../control/importNow.class?redirectToReferrer="
                + (RELOAD++) + "' class='nav'>Refresh Data...</a></h2>");

        String prefixListName = getParameter("for");
        ListData prefixList = ListData.asListData(getDataContext()
                .getSimpleValue(prefixListName));
        List<ImportData> importData = getImportData(prefixList);

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
                    result.add(new ImportData(metadataPrefix));
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

        public ImportData(String prefix) {
            this.prefix = prefix;
            this.ownerName = getString(OWNER_VAR);
            this.exportDate = getDate(TIMESTAMP_VAR);
            this.dashVersion = getString(DASH_VERSION_VAR);

        }

        private SimpleData get(String name) {
            String dataName = DataRepository.createDataName(prefix, name);
            return getDataRepository().getSimpleValue(dataName);
        }

        private String getString(String name) {
            SimpleData val = get(name);
            return (val == null ? "" : val.format());
        }

        private Date getDate(String name) {
            SimpleData val = get(name);
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
