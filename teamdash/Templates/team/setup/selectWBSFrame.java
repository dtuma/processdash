
import net.sourceforge.processdash.net.http.WebServer;


public class selectWBSFrame extends selectWBS {

    private String processID = null;

    protected String getScript() { return ""; }

    protected void printLink(String rootPath, String relPath) {
        if (processID == null) {
            String scriptName = (String) env.get("SCRIPT_NAME");
            int slashPos = scriptName.indexOf('/', 1);
            processID = scriptName.substring(1, slashPos);
        }

        out.print("<a target='topFrame' href=\"");
        out.print(WebServer.urlEncodePath(rootPath));
        if (relPath != null && relPath.length() > 0) {
            out.print("/");
            out.print(WebServer.urlEncodePath(relPath));
        }
        out.print("//");
        out.print(processID);
        out.print("/summary_frame.shtm\">");
    }

}
