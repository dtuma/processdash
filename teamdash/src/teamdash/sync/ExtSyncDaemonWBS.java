// Copyright (C) 2017-2025 Tuma Solutions, LLC
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

import static teamdash.sync.ExtSyncCoordinator.FIRST_RUN_FLAG;
import static teamdash.sync.ExtSyncCoordinator.NO_EXT_CHANGES;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import net.sourceforge.processdash.tool.bridge.ResourceCollection;
import net.sourceforge.processdash.util.DateUtils;
import net.sourceforge.processdash.util.XMLUtils;

import teamdash.sync.DaemonMetadata.State;


public class ExtSyncDaemonWBS {

    protected Logger log;

    protected Properties globalConfig;

    protected String systemName, systemID;

    protected ExtSystemConnection connection;

    protected String wbsLocation;

    private TeamProjectDataTarget dataTarget;

    private DaemonMetadata daemonMetadata;

    private int loopDelay, retryDelay, refreshDelay, fileScanDelay, stateDelay;


    public ExtSyncDaemonWBS(ExtSyncDaemon parent, String wbsLocation) {
        this.log = parent.log;
        this.globalConfig = new Properties(parent.globalConfig);
        this.systemName = parent.systemName;
        this.systemID = parent.systemID;
        this.connection = parent.connection;
        this.wbsLocation = wbsLocation;
    }


    public void run() throws Exception {
        // create a data target for synchronization
        this.dataTarget = TeamProjectDataTargetFactory
                .getBatchProcessTarget(wbsLocation);

        try {
            // run a loop to synchronize this WBS
            runImpl();
        } finally {
            // dispose of resources when finished
            dataTarget.dispose();
            dataTarget = null;
            daemonMetadata = null;
        }
    }

    protected void runImpl() throws Exception {
        // retrieve project-specific sync configuration
        Element targetConfig = getConfigXml();
        if (targetConfig == null) {
            log.warning("No " + systemName + " sync spec found, exiting");
            return;
        }
        globalConfig.put(FIRST_RUN_FLAG, "true");
        if ("true".equals(targetConfig.getAttribute(NO_EXT_CHANGES)))
            globalConfig.put(NO_EXT_CHANGES, "true");

        // create objects to perform the synchronization of this target
        daemonMetadata = TeamProjectDataTargetFactory
                .getDaemonMetadata(dataTarget, systemID);
        ExtSyncCoordinator coord = new ExtSyncCoordinator(dataTarget,
                systemName, systemID, globalConfig, daemonMetadata);
        ExtNodeSet nodeSet = connection.getNodeSet(targetConfig,
            coord.syncData);

        // run the sync operation, potentially multiple times
        long heartbeat = System.currentTimeMillis() + DateUtils.HOUR;
        int errCount = 0;
        loopDelay = getMillisParam("loop.syncInterval",
            isActiveSleepSupported() ? (int) DateUtils.HOUR : 5000);
        retryDelay = getMillisParam("loop.retryDelay", 5000);
        refreshDelay = getMillisParam("loop.refreshInterval", DateUtils.SECONDS);
        fileScanDelay = getMillisParam("loop.fileScanInterval", 10 * DateUtils.SECONDS);
        stateDelay = getMillisParam("loop.stateInterval", 5 * DateUtils.MINUTES);
        daemonMetadata.setRefreshInterval(refreshDelay);

        do {
            long start = System.currentTimeMillis();

            if (start > heartbeat) {
                log.info("Sync daemon is alive");
                heartbeat = start + DateUtils.HOUR;
            }

            try {
                // perform the synchronization operation
                coord.run(nodeSet);

                // record successful completion of sync operation
                if (errCount > 0)
                    log.info("Connection reestablished, "
                            + "synchronization resuming");
                errCount = 0;

                // display elapsed time
                long end = System.currentTimeMillis();
                long elapsed = end - start;
                log.fine("Synchronization took " + elapsed + " ms.");

            } catch (SyncDataFile.ComodificationException sdfce) {
                // another sync daemon process has started; we should exit
                log.info("A new sync daemon has started; old daemon will exit");
                return;
            } catch (Exception e) {
                log.log(errCount == 0 ? Level.SEVERE : Level.FINE,
                    "Encountered unexpected problem while synchronizing");
                connection.disconnect();
                errCount++;
            }

            // delay before repeating the loop again
            long nextStart = start
                    + loopDelayMillis(loopDelay, errCount, retryDelay);
            long wait = nextStart - System.currentTimeMillis();
            if (loopDelay == 0) {
                log.info("Press enter to repeat.");
                System.console().readLine();
            } else if (wait > 0) {
                try {
                    sleep(wait, coord, errCount == 0 ? 0 : 2 * retryDelay);
                } catch (IOException ioe) {
                    // if we've lost connectivity to the WBS directory, record
                    // an error, then wait a moment before trying again
                    if (errCount == 0) errCount++;
                    threadSleep(loopDelayMillis(1, errCount, retryDelay));
                }
            }

        } while (loopDelay >= 0);
    }

    private Element getConfigXml() throws IOException {
        // find the configuration file in the WBS data directory
        dataTarget.update();
        ResourceCollection collection = dataTarget.getCollection();
        if (collection.getLastModified(ExtSyncUtil.EXT_SPEC_FILE) == 0)
            return null;

        // parse the configuration file as an XML document
        Element xmlDoc;
        try {
            BufferedInputStream in = new BufferedInputStream(
                    collection.getInputStream(ExtSyncUtil.EXT_SPEC_FILE));
            xmlDoc = XMLUtils.parse(in).getDocumentElement();
        } catch (SAXException se) {
            throw new IOException(se);
        }

        // find the specification for this system in the document
        for (Element systemXml : XMLUtils.getChildElements(xmlDoc)) {
            String extSystemType = systemXml.getAttribute("type");
            extSystemType = XMLUtils.getAttribute(systemXml, "id", extSystemType);
            if (extSystemType.equalsIgnoreCase(systemID))
                return systemXml;
        }

        // no configuration was found
        return null;
    }

    private int getMillisParam(String propName, long defaultMillis) {
        return ExtSyncUtil.getParamAsMillis(globalConfig, propName,
            (int) defaultMillis);
    }

    private int loopDelayMillis(int loopDelay, int errCount, int retryDelay) {
        if (loopDelay <= 0 || errCount == 0)
            return loopDelay;
        else if (errCount < 2)
            return retryDelay;
        else if (errCount < 4)
            return 5 * retryDelay;
        else
            return 10 * retryDelay;
    }

    private boolean isActiveSleepSupported() {
        return daemonMetadata.isSyncRequestSupported();
    }

    private void sleep(long duration, ExtSyncCoordinator coord, int errDelay)
            throws IOException {
        if (isActiveSleepSupported() == false)
            sleepSimply(duration, errDelay > 0);
        else {
            if (errDelay > 0)
                sleepSimply(Math.min(duration, errDelay), true);
            if (duration > errDelay)
                sleepWithActivityChecking(duration - errDelay, coord);
        }
    }

    private void sleepSimply(long duration, boolean isErr) throws IOException {
        daemonMetadata.setState(isErr ? State.Error : State.Sleep, duration);
        threadSleep(duration);
    }

    private void sleepWithActivityChecking(long duration,
            ExtSyncCoordinator coord) throws IOException {
        // calculate the maximum amount of time our sleep should last
        long remainingTime = duration;
        long now = System.currentTimeMillis();
        long finishTime = now + remainingTime;
        long nextFileScan = 0, nextStatePublish = 0;

        // loop for up to the requested duration
        while (remainingTime > 0) {
            // if a client has requested a refresh, wake up
            if (daemonMetadata.isSyncRequestPending())
                return;

            // periodically check to see if any of the files in our target
            // directory have been externally modified. If so, wake up
            if (now > nextFileScan) {
                if (coord.targetFilesHaveChanged())
                    return;
                nextFileScan = now + fileScanDelay;
            }

            // periodically update the state file, to let clients know we're
            // still alive but sleeping
            if (now > nextStatePublish) {
                daemonMetadata.setState(State.Sleep,
                    Math.min(remainingTime, stateDelay));
                nextStatePublish = now + stateDelay;
            }

            // sleep for a moment before checking for activity again
            threadSleep(refreshDelay);

            // recalc how much longer we should sleep after the operations above
            now = System.currentTimeMillis();
            remainingTime = finishTime - now;
        }
    }

    private void threadSleep(long delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ie) {}
    }

}
