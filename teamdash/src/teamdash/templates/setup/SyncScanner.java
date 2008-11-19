package teamdash.templates.setup;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.ui.Browser;
import net.sourceforge.processdash.ui.UserNotificationManager;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This class periodically scans the dashboard hierarchy for team projects that
 * need synchronizing.
 * 
 * @author Tuma
 */
public class SyncScanner implements Runnable {

    private DashboardContext context;

    private Map syncTasks;


    private static final Vector SCANNER_OBJECTS = new Vector();

    private static final Logger logger = Logger.getLogger(SyncScanner.class
            .getName());

    public SyncScanner() {
        SCANNER_OBJECTS.add(this);
    }

    public void setDashboardContext(DashboardContext context) {
        this.context = context;
    }

    public void setConfigElement(Element configElement, String attrName) {
        syncTasks = new HashMap();
        NodeList l = configElement.getElementsByTagName("sync");
        for (int i = 0; i < l.getLength(); i++) {
            Element e = (Element) l.item(i);
            String templateID = e.getAttribute("templateID");
            syncTasks.put(templateID, new SyncTask(e));
        }
    }

    public void run() {
        if (Settings.isReadWrite())
            lookForSyncOperations(PropertyKey.ROOT, true);
    }

    private void lookForSyncOperations(PropertyKey node, boolean recurse) {
        DashHierarchy hier = context.getHierarchy();
        String templateID = hier.getID(node);
        SyncTask task = (SyncTask) syncTasks.get(templateID);
        if (task != null)
            task.run(node.path());
        else if (recurse) {
            for (int i = hier.getNumChildren(node); i-- > 0;)
                lookForSyncOperations(hier.getChildKey(node, i), true);
        }
    }

    private void lookForSyncOperation(String path) {
        PropertyKey key = context.getHierarchy().findExistingKey(path);
        if (Settings.isReadWrite() && key != null)
            lookForSyncOperations(key, false);
    }


    private class SyncTask {
        public String uri;

        public String alertIfToken;

        public SyncTask(Element configElement) {
            uri = configElement.getAttribute("uri");
            if (!uri.startsWith("/"))
                uri = "/" + uri;
            alertIfToken = configElement.getAttribute("alertIfToken");
        }

        public void run(String path) {
            String disabledDataName = DataRepository.createDataName(path,
                    "Autosync_Disabled");
            SimpleData disabledData = context.getData().getSimpleValue(
                    disabledDataName);
            if (disabledData != null && disabledData.test()) {
                logger.log(Level.FINE, "Automatic sync is disabled for \"{0}\"",
                        path);
                return;
            }

            String fullUri = WebServer.urlEncodePath(path) + "/" + uri;
            try {
                logger.log(Level.FINE, "Checking for synch against {0}", path);
                String response = context.getWebServer().getRequestAsString(
                        fullUri);

                if (containsAlertToken(response)) {
                    String id = getScanTaskID(path);
                    String msg = "The project '" + path + "' needs to be "
                            + "synchronized to the team work breakdown "
                            + "structure.";
                    if (fullUri.endsWith("brief"))
                        fullUri = fullUri.substring(0, fullUri.length() - 5);
                    UserNotificationManager.getInstance().addNotification(id,
                            msg, new LaunchBrowserWindow(fullUri));
                }
            } catch (IOException e) {
                String msg = "Encountered exception when trying to see if "
                        + "sync is needed for path '" + path + "'";
                logger.log(Level.SEVERE, msg, e);
            }
        }

        private boolean containsAlertToken(String response) {
            if (alertIfToken == null || alertIfToken.trim().length() == 0)
                return false;
            else
                return response.indexOf(alertIfToken) != -1;
        }
    }

    private static class LaunchBrowserWindow implements Runnable {
        private String uri;

        public LaunchBrowserWindow(String uri) {
            this.uri = uri;
        }

        public void run() {
            Browser.launch(uri);
        }
    }

    public static void requestScan(String path) {
        logger.log(Level.FINE, "Requesting scan for {0}", path);
        for (Iterator i = SCANNER_OBJECTS.iterator(); i.hasNext();) {
            SyncScanner scanner = (SyncScanner) i.next();
            scanner.lookForSyncOperation(path);
        }
    }

    public static String getScanTaskID(String path) {
        return "teamdash.SyncScanner:" + path;
    }

}
