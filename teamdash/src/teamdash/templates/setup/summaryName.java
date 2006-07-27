
package teamdash.templates.setup;
import java.io.IOException;

import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.util.HTMLUtils;


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

        out.println("<html><head>");
        out.println("<link rel=stylesheet type='text/css' href='style.css'>");
        out.println("<style>");
        out.println(" body { margin: 0pt; padding: 2px }");
        out.println(" h1   { margin: 0pt; padding: 0pt }");
        out.println(" h2   { margin: 0pt; padding: 0pt }");
        out.println("</style></head><body>");
        out.print("<h1>");
        out.print(HTMLUtils.escapeEntities(projectRoot));
        out.println("</h1>");
        out.print("<h2>");

        writeFilterIcon(projectRoot);
        writeHierarchyIcon(prefix, projectRoot);

        out.println("</h2></body></html>");
    }

    /** Print the icon and text for choosing a filter
     */
    private void writeFilterIcon(String projectRoot) {
        boolean exporting = parameters.containsKey("EXPORT");
        String currentFilter = selectLabelFilter.getCurrentFilter(
                getDataRepository(), projectRoot);
        if (currentFilter == null
                || (exporting && currentFilter.length() == 0))
            // if filtering doesn't make sense, or if we're exporting and no
            // filter is in effect, print nothing.
            return;

        if (!exporting)
            out.print("<a target='contents' href=\"selectLabelFilter\">");

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

    /** Print the icon and text for navigating the hierarchy
     */
    private void writeHierarchyIcon(String prefix, String projectRoot) {
        out.print("<a target='contents' href=\"");
        out.print(WebServer.urlEncodePath(projectRoot));
        out.print("//");
        out.print(processID);
        out.print("/setup/selectWBSFrame.class");
        out.print("\"><img border=0 src='../hier.png' title='Navigate Hierarchy' "+
                  "style='margin-right:2px' width=16 height=23></a>");
        if (prefix.equals(projectRoot))
            out.print("/");
        else
            out.print(HTMLUtils.escapeEntities(prefix.substring(projectRoot
                    .length() + 1)));
    }
}
