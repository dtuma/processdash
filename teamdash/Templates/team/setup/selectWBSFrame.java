
import pspdash.TinyWebServer;

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
        out.print(TinyWebServer.urlEncodePath(rootPath));
        if (relPath != null && relPath.length() > 0) {
            out.print("/");
            out.print(TinyWebServer.urlEncodePath(relPath));
        }
        out.print("//");
        out.print(processID);
        out.print("/summary_frame.shtm\">");
    }

}
