// Copyright (C) 2016-2018 Tuma Solutions, LLC
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

package net.sourceforge.processdash.team.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.team.group.UserFilter;
import net.sourceforge.processdash.team.group.UserGroup;
import net.sourceforge.processdash.team.group.UserGroupManager;
import net.sourceforge.processdash.team.group.UserGroupManagerDash;
import net.sourceforge.processdash.team.group.UserGroupMember;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

public class SelectGroupFilter extends TinyCGIBase {

    public static final String FILTER_PARAM = "groupFilter";

    private static final Resources resources = Resources.getDashBundle("Groups");

    @Override
    protected void doGet() throws IOException {
        String projectRoot = getPrefix();
        String selectedFilter = getParameter("filter");
        String destUri = getParameter("destUri");
        if (!StringUtils.hasValue(destUri))
            destUri = (String) env.get("HTTP_REFERER");

        if (StringUtils.hasValue(selectedFilter)) {
            rejectCrossSiteRequests(env);
            applyFilter(projectRoot, selectedFilter, destUri);
        } else {
            String args = "&amp;destUri=" + HTMLUtils.urlEncode(destUri)
                    + "&amp;rl=" + (System.currentTimeMillis() % 100000);
            displayFilterSelections(projectRoot, args);
        }
    }

    private void displayFilterSelections(String projectRoot,
            String extraLinkArgs) throws IOException {
        writeHeader();

        String title = resources.getHTML("Filter_To_Group");
        out.print("<html><head><title>" + title + "</title>\n");
        out.print(HTMLUtils.cssLinkHtml("/style.css"));
        out.print("<style>\n"
                + "  A:link    { color:black; text-decoration:none }\n"
                + "  A:visited { color:black; text-decoration:none }\n"
                + "  A:hover   { color:blue; text-decoration:underline }\n"
                + "</style><body>\n<h3>");
        out.print(title);
        out.print("</h3>\n<p>");
        out.print(resources.getHTML("Report_Filter_Prompt"));
        out.print("</p>\n");

        out.print("<table style='margin-left:1em'>");
        writeFilterOption(UserGroup.EVERYONE, extraLinkArgs, false);
        writeFolder("Groups");
        writeFilterOptions(UserGroupManager.getInstance().getGroups().values(),
            extraLinkArgs);
        if (UserGroupManager.getInstance().isIndivFilteringSupported()) {
            writeFolder("Individuals");
            writeFilterOptions(UserGroupManager.getInstance()
                    .getAllKnownPeople(), extraLinkArgs);
        }
        out.print("</table>\n</body>\n</html>");
    }

    private void writeFilterOptions(Collection filters, String extraLinkArgs)
            throws IOException {
        List<UserFilter> l = new ArrayList(filters);
        Collections.sort((List) l);
        for (UserFilter f : l)
            writeFilterOption(f, extraLinkArgs, true);
    }

    private void writeFilterOption(UserFilter f, String extraLinkArgs,
            boolean indent) throws IOException {
        out.print("<tr><td");
        if (indent)
            out.print(" style='padding-left:1cm'");
        out.print("><img src='/Images/userGroup");
        int width = 16;
        if (f instanceof UserGroupMember)
            out.print("Member");
        else if (UserGroup.isEveryone(f)) {
            out.print("Everyone");
            width = 19;
        }
        out.print(".png' style='margin-right: 2px; position:relative; top:2px; "
                + "width:" + width + "px; height:16px'>");
        out.print("<a href='selectGroupFilter?filter=");
        out.print(HTMLUtils.urlEncode(f.getId()));
        out.print(extraLinkArgs);
        out.print("'>");
        out.print(HTMLUtils.escapeEntities(f.toString()));
        out.print("</a></td></tr>\n");
    }

    private void writeFolder(String resKey) {
        out.print("<tr><td><img src='/Images/node.png' "
                + "style='margin-right: 2px; width:16px; height:13px'>");
        out.print(resources.getHTML(resKey));
        out.print("</td></tr>\n");
    }

    private void applyFilter(String projectRoot, String selectedFilter,
            String destUri) {
        UserFilter f = UserGroupManager.getInstance().getFilterById(
            selectedFilter);
        if (f == null)
            f = UserGroup.EVERYONE;

        int pos = destUri.indexOf(FILTER_PARAM + '=');
        if (pos > 0) {
            int beg = pos + FILTER_PARAM.length() + 1;
            int end = destUri.indexOf('&', beg);
            destUri = destUri.substring(0, beg)
                    + HTMLUtils.urlEncode(f.getId())
                    + (end == -1 ? "" : destUri.substring(end));

        } else {
            UserGroupManagerDash.getInstance().setLocalFilter(projectRoot, f);
            getDataRepository().waitForCalculations();
        }

        out.write("Location: " + destUri + "\r\n\r\n");
    }

}
