
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
        out.print("<h2><a target='contents' href=\"");
        out.print(WebServer.urlEncodePath(projectRoot));
        out.print("//");
        out.print(processID);
        out.print("/setup/selectWBSFrame.class\">");
        out.print("<img border=0 src='../hier.png' alt='Navigate Hierarchy' "+
                  "style='margin-right:2px' width=16 height=23></a>");
        if (prefix.equals(projectRoot))
            out.print("/");
        else
            out.print(prefix.substring(projectRoot.length()+1));
        out.println("</h2></body></html>");
    }

}
