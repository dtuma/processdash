// Copyright (C) 2020-2021 Tuma Solutions, LLC
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

package teamdash.sync;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.w3c.dom.Element;

import net.sourceforge.processdash.tool.bridge.client.BridgedWorkingDirectory;
import net.sourceforge.processdash.tool.bridge.client.LocalWorkingDirectory;
import net.sourceforge.processdash.tool.bridge.client.ResourceBridgeClient;
import net.sourceforge.processdash.tool.bridge.client.WorkingDirectory;
import net.sourceforge.processdash.util.DateUtils;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.XMLUtils;

import teamdash.sync.DaemonMetadata.State;
import teamdash.wbs.WBSFilenameConstants;

/**
 * A helper object that can request an external refresh on behalf of a dashboard
 * client.
 * 
 * When a project WBS is connected to one or more external systems, the
 * synchronization is currently performed by a background daemon. This class
 * allows a dashboard client to send a message to that daemon, requesting an
 * immediate sync.
 */
public class ExtRefreshCoordinator {


    /**
     * Request an external refresh for the WBS in the given working directory.
     * 
     * @param dir
     *            the working directory for a given WBS. If that WBS is not
     *            connected to any external systems, this will return true
     *            immediately.
     * @param zealous
     *            true if we wish to retrieve the very latest, up-to-the-second
     *            data; false if generally recent data is acceptable.
     * @param timeoutSeconds
     *            the maximum amount of time we should wait for the refresh to
     *            complete
     * @return true if data was refreshed successfully, false if there were any
     *         errors
     */
    public static boolean runExtRefresh(WorkingDirectory dir, boolean zealous,
            int timeoutSeconds) {
        if (dir instanceof BridgedWorkingDirectory)
            return runBridgedExtRefresh(dir, zealous, timeoutSeconds);
        else if (dir instanceof LocalWorkingDirectory)
            return runLocalExtRefresh(dir.getDirectory(), zealous,
                timeoutSeconds);
        else
            return false;
    }



    /**
     * Request an external refresh for the WBS at the given location.
     * 
     * @param wbsLocation
     *            a URL or filename pointing to the data for a given WBS. If
     *            that WBS is not connected to any external systems, this will
     *            return true immediately.
     * @param zealous
     *            true if we wish to retrieve the very latest, up-to-the-second
     *            data; false if generally recent data is acceptable.
     * @param timeoutSeconds
     *            the maximum amount of time we should wait for the refresh to
     *            complete
     * @return true if data was refreshed successfully, false if there were any
     *         errors
     */
    public static boolean runExtRefresh(String wbsLocation, boolean zealous,
            int timeoutSeconds) {
        if (wbsLocation.startsWith("http"))
            return runUrlExtRefresh(wbsLocation, zealous, timeoutSeconds);
        else
            return runLocalExtRefresh(new File(wbsLocation), zealous,
                timeoutSeconds);
    }



    /** Refresh a WorkingDirectory that is known to be bridged */
    private static boolean runBridgedExtRefresh(WorkingDirectory dir,
            boolean zealous, int timeout) {
        // check to see if the directory contains an ext spec file. This lets
        // us avoid an HTTP call if there are no external connections.
        File externalSpecFile = new File(dir.getDirectory(),
                ExtSyncUtil.EXT_SPEC_FILE);
        if (!externalSpecFile.isFile())
            return true;

        return runUrlExtRefresh(dir.getDescription(), zealous, timeout);
    }


    /** Refresh a WBS location that is known to be a URL */
    private static boolean runUrlExtRefresh(String wbsUrl, boolean zealous,
            int timeoutSeconds) {
        BufferedReader in = null;
        try {
            // construct the URL for requesting an ext sync
            String url = wbsUrl;
            url = HTMLUtils.appendQuery(url, ResourceBridgeClient.ACTION_PARAM,
                ResourceBridgeClient.EXT_SYNC_ACTION);
            if (zealous == false)
                url = HTMLUtils.appendQuery(url,
                    ResourceBridgeClient.EXT_SYNC_LAZY_PARAM, "true");
            url = HTMLUtils.appendQuery(url,
                ResourceBridgeClient.EXT_SYNC_WAIT_PARAM,
                Integer.toString(timeoutSeconds));
            URL extSyncUrl = new URL(url);

            // make a POST connection to the server to request the sync
            HttpURLConnection conn = (HttpURLConnection) extSyncUrl
                    .openConnection();
            conn.setRequestMethod("POST");
            conn.setInstanceFollowRedirects(true);
            ResourceBridgeClient.setRequestToken(conn);
            conn.connect();

            // read output from the response; check for error indication
            in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.endsWith("state=Error"))
                    return false;
            }

            // return true to indicate the request completed successfully
            return true;

        } catch (IOException ioe) {
            return false;
        } finally {
            FileUtils.safelyClose(in);
        }
    }


    /** Refresh a WorkingDirectory that is known to be local */
    private static boolean runLocalExtRefresh(File wbsDirectory,
            boolean zealous, int timeoutSeconds) {
        // find out which external systems this WBS is linked to. If there
        // are no external connections, return immediately
        List<String> extSystemIDs = getExtSystemIDs(wbsDirectory);
        if (extSystemIDs.isEmpty())
            return true;

        // if the project is closed, no sync is needed
        if (isProjectClosed(wbsDirectory))
            return true;

        // start worker threads to simultaneously refresh each external system
        CountDownLatch semaphore = new CountDownLatch(extSystemIDs.size());
        AtomicBoolean success = new AtomicBoolean(true);
        for (String oneSystemID : extSystemIDs) {
            RefreshExtSystemTask task = new RefreshExtSystemTask(wbsDirectory,
                    oneSystemID, zealous, timeoutSeconds, semaphore, success);
            Thread t = new Thread(task, "ExtRefreshCoordinator.RefreshTask");
            t.setDaemon(true);
            t.start();
        }

        // wait for some period or until all worker threads complete
        try {
            return semaphore.await(timeoutSeconds, TimeUnit.SECONDS)
                    && success.get();
        } catch (InterruptedException e) {
            return false;
        }
    }



    private static List<String> getExtSystemIDs(File wbsDir) {
        // find the externals.xml file. If it doesn't exist, abort.
        File externalSpecFile = new File(wbsDir, ExtSyncUtil.EXT_SPEC_FILE);
        if (!externalSpecFile.isFile())
            return Collections.EMPTY_LIST;

        // collect the IDs for all of the external systems listed in the file
        List<String> result = new ArrayList<String>();
        try {
            Element xml = XMLUtils.parse(new FileInputStream(externalSpecFile))
                    .getDocumentElement();
            for (Element ext : XMLUtils.getChildElements(xml)) {
                if ("extSync".equals(ext.getTagName())) {
                    String type = ext.getAttribute("type");
                    String systemID = XMLUtils.getAttribute(ext, "id", type);
                    result.add(systemID);
                }
            }
        } catch (Exception e) {
        }
        return result;
    }



    private static boolean isProjectClosed(File wbsDir) {
        InputStream in = null;
        try {
            // open the project settings file
            File settingsFile = new File(wbsDir,
                    WBSFilenameConstants.USER_SETTINGS_FILENAME);
            in = new BufferedInputStream(new FileInputStream(settingsFile));

            // read the settings file and check the projectClosed setting
            Properties p = new Properties();
            p.load(in);
            return "true".equals(p.getProperty("projectClosed"));

        } catch (IOException ioe) {
            return false;
        } finally {
            FileUtils.safelyClose(in);
        }
    }



    private static class RefreshExtSystemTask implements Runnable {

        private DaemonMetadata daemon;

        private boolean zealous;

        private long timeoutEnd;

        private CountDownLatch semaphore;

        private AtomicBoolean success;

        private long lastSyncTimeSeen;

        public RefreshExtSystemTask(File wbsDir, String extSystemID,
                boolean zealous, int timeoutSeconds, CountDownLatch semaphore,
                AtomicBoolean success) {
            this.daemon = new DaemonMetadataFile(extSystemID, wbsDir);
            this.zealous = zealous;
            this.timeoutEnd = System.currentTimeMillis()
                    + timeoutSeconds * DateUtils.SECONDS - 500;
            this.semaphore = semaphore;
            this.success = success;
            this.lastSyncTimeSeen = -1;
        }

        @Override
        public void run() {
            try {
                boolean result = performRefresh();
                if (result == false)
                    success.set(false);
            } catch (Exception e) {
                success.set(false);
                e.printStackTrace();
            } finally {
                semaphore.countDown();
            }
        }

        private boolean performRefresh() throws IOException {
            // read information about the state of the daemon
            reloadDaemonMetadata();

            // if the last inbound sync completed recently, return immediately
            if (syncIsRecent())
                return true;

            // check the "next transition" timestamp recorded by the daemon.
            // it should normally be a time in the future (but could be very
            // slightly in the past if the current action ran over). If the
            // timestamp is old, the daemon is not running.
            long nextTime = daemon.getNextTime();
            long gracePeriod = System.currentTimeMillis()
                    - 30 * DateUtils.SECONDS;
            if (nextTime < gracePeriod)
                return false;

            // If the daemon isn't currently doing an inbound sync, request one
            State state = daemon.getState();
            if (state != State.Inbound)
                daemon.setSyncRequestPending(true);

            // if the daemon is in an error state, there's no sense waiting
            if (state == State.Error)
                return false;

            // If the daemon was sleeping, our sync request should wake it up
            // quickly. If it doesn't, abort.
            if (state == State.Sleep) {
                sleep(daemon.getRefreshInterval() + 100);
                if (daemon.isSyncRequestPending())
                    return false;
            }

            // watch the daemon until it finishes its next inbound sync, or
            // until our timeout expires
            while (System.currentTimeMillis() < timeoutEnd) {
                reloadDaemonMetadata();
                if (syncIsRecent())
                    return true;
                sleep(250);
            }

            // we ran out of time with no success
            return false;
        }

        private void reloadDaemonMetadata() throws IOException {
            // we don't expect problems loading the daemon metadata file; but
            // they might occur if the daemon overwrites the file while we're
            // reading it. retry a few times just in case.
            for (int i = 3; i-- > 0;) {
                try {
                    daemon.load();
                    return;
                } catch (IOException ioe) {
                    if (i == 0)
                        throw ioe;
                    else
                        sleep(100);
                }
            }
        }

        private boolean syncIsRecent() {
            // get the time when the last inbound sync completed.
            long syncTime = daemon.getLastTime(State.Inbound);
            if (syncTime < 0)
                return false;

            // compare this to the last time we saw. If the time has changed,
            // assume that a sync has occurred. (This offers an allowance for
            // mismatched clocks on different computers.)
            if (lastSyncTimeSeen < 0)
                lastSyncTimeSeen = syncTime;
            else if (lastSyncTimeSeen != syncTime)
                return true;

            // compute the age of the last sync and compare to requirements
            long syncAge = System.currentTimeMillis() - syncTime;
            if (zealous)
                return syncAge < 5 * DateUtils.SECONDS;
            else
                return syncAge < 5 * DateUtils.MINUTES;
        }

        private void sleep(long time) {
            try {
                if (time > 0)
                    Thread.sleep(time);
            } catch (InterruptedException e) {
            }
        }
    }

}
