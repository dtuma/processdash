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

import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.ui.snippet.SnippetEnvironment;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;


public class summaryName extends selectWBS {

    private String processID = null;

    protected void writeContents() throws IOException {
        if (processID == null) {
            String scriptName = (String) env.get("SCRIPT_NAME");
            int slashPos = scriptName.indexOf('/', 1);
            processID = scriptName.substring(1, slashPos);
        }

        String prefix = getPrefix();
        PropertyKey projectRootKey = getStartingKey();
        String projectRoot = projectRootKey.path();
        String currentFilter = selectLabelFilter.getCurrentFilter(
                getDataRepository(), projectRoot);
        boolean isSnippet = (env.containsKey(SnippetEnvironment.SNIPPET_ID));
        boolean isIndiv = (getID(getPSPProperties(), projectRootKey).indexOf(
                "Indiv") != -1);

        out.println("<html><head>");
        out.println("<link rel=stylesheet type='text/css' href='/style.css'>");
        if (isSnippet && StringUtils.hasValue(currentFilter))
            out.println("<link rel=stylesheet type='text/css' href='/reports/filter-style.css'>");
        out.println("<style>");
        out.println(" body { margin: 0pt; padding: 2px }");
        out.println(" h1   { margin: 0pt; padding: 0pt }");
        out.println(" h2   { margin: 0pt; padding: 0pt }");
        out.println("</style></head><body>");
        out.print("<h1 style='margin-top:0pt'><!-- editLink -->");
        out.print(HTMLUtils.escapeEntities(projectRoot));
        out.println("</h1>");
        out.print("<h2>");

        writeFilterIcon(projectRoot, currentFilter);
        if (isIndiv)
            writeHierarchyIconIndiv(projectRoot);
        else
            writeHierarchyIcon(prefix, projectRoot);

        out.println("</h2>");
        String cmsPageTitle = (String) env.get("cmsPageTitle");
        if (cmsPageTitle != null) {
            out.print("<h2>");
            out.print(HTMLUtils.escapeEntities(cmsPageTitle));
            out.println("</h2>");
        }
        out.println("</body></html>");
    }

    /** Print the icon and text for choosing a filter
     */
    private void writeFilterIcon(String projectRoot, String currentFilter) {
        boolean exporting = parameters.containsKey("EXPORT");
        if (currentFilter == null
                || (exporting && currentFilter.length() == 0))
            // if filtering doesn't make sense, or if we're exporting and no
            // filter is in effect, print nothing.
            return;

        if (!exporting)
            writeHyperlink("selectLabelFilter", getSnippetParams(false, false));

        out.print("<img border=0 src='/Images/filter.png' "
                + "style='margin-right:2px' width='16' height='23' ");
        if (!exporting)
            out.print("title='Choose label filter'></a>");
        else
            out.print("title='Filter is in effect'>");

        out.print(HTMLUtils.escapeEntities(currentFilter));
        if (currentFilter.length() > 0)
            out.print("&nbsp;&nbsp;&nbsp;");
        else
            out.print(" ");
    }

    /** Print the icon and text for navigating the hierarchy for an individual
     */
    private void writeHierarchyIconIndiv(String projectRoot) {
        String pathDisplay = "/";
        SimpleData wbsData = getDataContext().getSimpleValue(
                "Project_WBS_ID_Filter");
        if (wbsData != null) {
            pathDisplay = wbsData.format();
            int slashPos = pathDisplay.indexOf('/');
            if (slashPos == -1)
                pathDisplay = "/";
            else
                pathDisplay = pathDisplay.substring(slashPos);
        }

        boolean exporting = parameters.containsKey("EXPORT");
        if (!exporting) {
            String href = WebServer.urlEncodePath(projectRoot) + "//"
                    + processID + "/setup/selectWBSIndiv";
            writeHyperlink(href, getSnippetParams(false, false));
        }

        out.print("<img border=0 src='../hier.png' "
                + "style='margin-right:2px' width='16' height='23' ");
        if (!exporting)
            out.print("title='Navigate Hierarchy'></a>");
        else if ("/".equals(pathDisplay))
            out.print("title='Showing data from entire project hierarchy'>");
        else
            out.print("title='Hierarchy Drill-down is in effect'>");

        out.print(HTMLUtils.escapeEntities(pathDisplay));
    }

    /** Print the icon and text for navigating the hierarchy on the team side
     */
    private void writeHierarchyIcon(String prefix, String projectRoot) {
        String href = WebServer.urlEncodePath(projectRoot) + "//" + processID
                + "/setup/selectWBSFrame.class";
        writeHyperlink(href, getSnippetParams(true, true));

        out.print("<img border=0 src='../hier.png' title='Navigate Hierarchy' "+
                  "style='margin-right:2px' width=16 height=23></a>");
        if (prefix.equals(projectRoot))
            out.print("/");
        else
            out.print(HTMLUtils.escapeEntities(prefix.substring(projectRoot
                    .length() + 1)));
    }


    private String getSnippetParams(boolean fullPage, boolean stripPrefix) {
        String uri = (String) env.get(fullPage ? "cmsFullPageUri"
                : "cmsSnippetCurrentFrameUri");
        if (uri == null)
            return null;

        if (stripPrefix) {
            int pos = uri.indexOf("//");
            if (pos == -1) pos = uri.indexOf("/+/") + 1;
            if (pos > 0)
                uri = uri.substring(pos+1);
        }
        String result = "?destUri=" + HTMLUtils.urlEncode(uri);

        if (fullPage) {
            String target = (String) env.get("cmsFullPageTarget");
            if (target == null) target = "_top";
            result = result + "&target=" + HTMLUtils.urlEncode(target);
        }

        return result;
    }

    private void writeHyperlink(String href, String snippetParams) {
        out.print("<a href=\"");
        out.print(href);
        if (snippetParams == null)
            out.print("\" target=\"contents\">");
        else {
            out.print(snippetParams);
            out.print("\">");
        }
    }
}