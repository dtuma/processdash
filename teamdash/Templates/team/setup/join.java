
import pspdash.*;
import pspdash.data.DataRepository;
import pspdash.data.ImmutableStringData;
import pspdash.data.ImmutableDoubleData;
import pspdash.data.SimpleData;

import java.io.*;
import java.net.*;
import java.util.*;

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

        // TODO: If the prefix names an individual project, (or in any
        // other way does not name a team project), redirect to an
        // error message.

        // TODO: If an individual has already joined this project,
        // detect that and display an error message.

        // TODO: If the team leader visits the join page, what should
        // happen?  Team leaders may want to join from the same dashboard
        // where the data is located.  The team leader may very well:
        //  1) have created the team project in his own hierarchy
        //  2) restarted his dashboard, so it is no longer listening
        //     on 2468
        // In this case, he will get a "start up your dashboard" error
        // message.

        // We have to comment out the rerooting functionality for
        // now - the java URLConnection logic gets confused by the
        // resulting redirection instructions.
        //maybeReroot();

        storeValues();
        if (parameters.get("xml") != null)
            printRedirect(JOIN_XML);
        else
            printRedirect(JOIN_URL);

        this.out.flush();
    }

    /** Send an HTTP redirect command to the browser, sending it to the
     *  relative URI named by filename. */
    protected void printRedirect(String filename) {
        out.print("Location: ");
        /*
        out.print(TinyWebServer.urlEncodePath(getPrefix()));
        out.print("//");
        out.print(getProcessID());
        out.print("/setup/"); */
        out.print(filename);
        out.print("\r\n\r\n");
    }

    /** Save a value into the data repository. */
    protected void putValue(String name, String value) {
        putValue(name, new ImmutableStringData(value));
    }

    protected void putValue(String name, SimpleData dataValue) {
        DataRepository data = getDataRepository();
        String prefix = getPrefix();
        if (prefix == null) prefix = "";
        String dataName = data.createDataName(prefix, name);
        data.putValue(dataName, dataValue);
    }

    /** Get a value from the data repository. */
    protected String getValue(String name) {
        DataRepository data = getDataRepository();
        String prefix = getPrefix();
        if (prefix == null) prefix = "";
        String dataName = data.createDataName(prefix, name);
        SimpleData d = data.getSimpleValue(dataName);
        return (d == null ? null : d.format());
    }

    /** If the current prefix doesn't name the root of a team project,
     * search upward through the hierarchy to find the project root,
     * and change the active prefix to name that node. */
    protected void maybeReroot() {
        PSPProperties hierarchy = getPSPProperties();
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

        if (rerooted && projectRoot != null)
            parameters.put("hierarchyPath", projectRoot);
    }

    protected void storeValues() {
        putValue("setup//rand", Long.toString(System.currentTimeMillis()));
        /*
        String pid = getProcessID();
        putValue(PROCESS_ID, pid);
        String teamDir = getValue("Team_Directory");
        String teamDirUNC = getValue("Team_Directory_UNC");

        // save the path to the template jarfile, in regular and UNC formats.
        String file =
            File.separator + "Templates" + File.separator + pid + ".zip";
        putValue(TEMPLATE_PATH, teamDir + file);
        if (teamDirUNC != null) putValue(TEMPLATE_UNC, teamDirUNC + file);

        // save the path to the data directory, in regular and UNC formats.
        String projectID = getValue("Project_ID");
        file = File.separator + "data" + File.separator + projectID;
        putValue(DATA_PATH, teamDir + file);
        if (teamDirUNC != null) putValue(DATA_UNC, teamDirUNC + file);
        */
    }

    protected String getProcessID() {
        String path = (String) env.get("SCRIPT_NAME");
        if (path == null || !path.startsWith("/")) return null;
        path = URLDecoder.decode(path).substring(1);
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
