
package teamdash.templates.setup;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.Map;

import net.sourceforge.processdash.data.ImmutableStringData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.net.http.TinyCGIException;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;


/** This class helps an individual to join a team project.
 *
 * Normally, a team member would connect to this script from a remote
 * machine.  This script tests to see if they have the dashboard running,
 * and if so, prints a
 */
public class join extends TinyCGIBase {

    private static final String JOIN_URL = "join.shtm";
    private static final String JOIN_XML = "joinxml.shtm";

    private static final String PROCESS_ID = "setup//Process_ID";
    private static final String TEMPLATE_PATH = "setup//Template_Path";
    private static final String TEMPLATE_UNC = "setup//Template_Path_UNC";
    private static final String DATA_PATH = "setup//Data_Path";
    private static final String DATA_UNC = "setup//Data_Path_UNC";

    protected void writeHeader() {}
    protected void writeContents() {}
    public void service(InputStream in, OutputStream out, Map env)
        throws IOException
    {
        super.service(in, out, env);

        // TODO: If an individual has already joined this project,
        // detect that and display an error message.

        maybeReroot();

        if (parameters.get("xml") != null)
            internalRedirect(JOIN_XML);
        else
            printRedirect(JOIN_URL);
    }

    /** the HTTPURLConnection in JRE1.4.1 can't seem to handle the
     * redirection instructions sent by the dashboard. Since xml
     * output from this script is read by the JRE rather than a web
     * browser, this causes problems.  Work around the problems by
     * performing an "internal redirect" ourselves.
     */
    protected void internalRedirect(String filename) throws IOException {
        String uri = makeURI(filename);
        byte[] contents = getTinyWebServer().getRequest(uri, false);
        outStream.write(contents);
        outStream.flush();
    }

    protected void printRedirect(String filename) {
        out.print("Location: ");
        out.print(makeURI(filename));
        out.print("\r\n\r\n");
        out.flush();
    }

    private String makeURI(String filename) {
        return WebServer.urlEncodePath(getPrefix()) +
            "//" + getProcessID() + "/setup/" + filename;
    }

    /** Save a value into the data repository. */
    protected void putValue(String name, String value) {
        putValue(name, new ImmutableStringData(value));
    }

    protected void putValue(String name, SimpleData dataValue) {
        DataRepository data = getDataRepository();
        String prefix = getPrefix();
        if (prefix == null) prefix = "";
        String dataName = DataRepository.createDataName(prefix, name);
        data.putValue(dataName, dataValue);
    }

    /** Get a value from the data repository. */
    protected String getValue(String name) {
        DataRepository data = getDataRepository();
        String prefix = getPrefix();
        if (prefix == null) prefix = "";
        String dataName = DataRepository.createDataName(prefix, name);
        SimpleData d = data.getSimpleValue(dataName);
        return (d == null ? null : d.format());
    }

    /** If the current prefix doesn't name the root of a team project,
     * search upward through the hierarchy to find the project root,
     * and change the active prefix to name that node. */
    protected void maybeReroot() throws TinyCGIException {
        DashHierarchy hierarchy = getPSPProperties();
        PropertyKey key = hierarchy.findExistingKey(getPrefix());
        boolean rerooted = false;
        String projectRoot = null;
        do {
            String templateID = hierarchy.getID(key);
            if (templateID != null && templateID.endsWith("/TeamRoot")) {
                projectRoot = key.path();
                break;
            }
            rerooted = true;
            key = key.getParent();
        } while (key != null);

        if (rerooted) {
            if (projectRoot != null)
                parameters.put("hierarchyPath", projectRoot);
            else
                throw new TinyCGIException(404, "Not Fount");
        }
    }

    protected String getProcessID() {
        String path = (String) env.get("SCRIPT_NAME");
        if (path == null || !path.startsWith("/")) return null;
        path = HTMLUtils.urlDecode(path).substring(1);
        int slashPos = path.indexOf('/');
        return path.substring(0, slashPos);
    }

    protected String getRemoteAddress() {
        String remoteAddress = (String) env.get("REMOTE_ADDR");
        if (remoteAddress == null) return null;
        if (remoteAddress.length() == 0) return null;
        if (remoteAddress.equals("127.0.0.1")) return null;
        try {
            String host = InetAddress.getLocalHost().getHostAddress();
            if (remoteAddress.equals(host)) return null;
        } catch (IOException ioe) {}
        return remoteAddress;
    }

    /*
    protected boolean testForRemoteDashboard(String address) {
        try {
            URL url = new URL("http://"+address+":2468/Nonexistent_File");
            URLConnection conn = url.openConnection();
            conn.connect();
            int status = ((HttpURLConnection) conn).getResponseCode();
            return true;

        } catch (IOException ioe) { }
        return false;
    }
    */

}
