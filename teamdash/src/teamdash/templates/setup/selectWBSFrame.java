
package teamdash.templates.setup;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.net.http.WebServer;


public class selectWBSFrame extends selectWBS {

    private String processID = null;

    private int maxDepth = -1;


    protected void initialize(DashHierarchy hierarchy, PropertyKey key,
            String id, int depth, String rootPath) {
        super.initialize(hierarchy, key, id, depth, rootPath);
        initMaxDepth(rootPath);
    }

    protected boolean prune(DashHierarchy hierarchy, PropertyKey key,
            String id, int depth, String rootPath) {
        if (maxDepth >= 0 && depth > maxDepth)
            return true;
        else
            return super.prune(hierarchy, key, id, depth, rootPath);
    }

    private void initMaxDepth(String prefix) {
        if (!parameters.containsKey("EXPORT")) {
            // if we aren't currently exporting, display nodes to any arbitrary
            // depth for drill-down.
            maxDepth = -1;
        } else {
            // when exporting, check to see if the user has configured
            // a desired depth for drill-down
            String dataName = DataRepository.createDataName(prefix,
                    MAX_EXPORT_DEPTH_DATA_NAME);
            SimpleData data = getDataRepository().getSimpleValue(dataName);
            if (data instanceof DoubleData) {
                DoubleData doubleVal = (DoubleData) data;
                maxDepth = doubleVal.getInteger();
            } else {
                maxDepth = DEFAULT_MAX_EXPORT_DEPTH;
            }
        }
    }

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

    private static final String MAX_EXPORT_DEPTH_DATA_NAME =
        "Export_Max_Node_Depth";
    private static final int DEFAULT_MAX_EXPORT_DEPTH = 2;
}
