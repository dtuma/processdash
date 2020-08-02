// Copyright (C) 2017-2020 Tuma Solutions, LLC
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Element;

import net.sourceforge.processdash.tool.bridge.client.ImportDirectory;
import net.sourceforge.processdash.util.DateUtils;

import teamdash.sync.DaemonMetadata.State;
import teamdash.wbs.ChangeHistory;
import teamdash.wbs.TeamProject;

public class ExtSyncCoordinator {

    private TeamProjectDataTarget dataTarget;

    private String extSystemName;

    private String extSystemID;

    private SyncDataFile syncData;

    private int refreshDelay, fileScanDelay, stateDelay;

    private DaemonMetadata daemonMetadata;

    private ElapsedTimeMonitor inboundTime, outboundTime;

    private SyncMetadata metadata;

    private long maxFileTime;

    private static final Logger log = ExtSynchronizer.log;

    private static final Logger debug = ExtSynchronizer.debug;


    public ExtSyncCoordinator(TeamProjectDataTarget dataTarget,
            String extSystemName, String extSystemID, Properties config) {
        this.dataTarget = dataTarget;
        this.extSystemName = extSystemName;
        this.extSystemID = extSystemID;
        this.syncData = new SyncDataFile(dataTarget,
                extSystemID + "-sync.pdash");
        this.refreshDelay = ExtSyncUtil.getParamAsMillis(config,
            "loop.refreshInterval", (int) DateUtils.SECONDS);
        this.fileScanDelay = ExtSyncUtil.getParamAsMillis(config,
            "loop.fileScanInterval", 10 * (int) DateUtils.SECONDS);
        this.stateDelay = ExtSyncUtil.getParamAsMillis(config,
            "loop.stateInterval", 5 * (int) DateUtils.MINUTES);
        this.daemonMetadata = dataTarget.getDaemonMetadata(extSystemID);
        this.daemonMetadata.setRefreshInterval(refreshDelay);
        this.inboundTime = new ElapsedTimeMonitor(20, 5000);
        this.outboundTime = new ElapsedTimeMonitor(20, 1000);
    }

    /**
     * Perform one round-trip synchronization run between this WBS and the given
     * set of external nodes.
     */
    public void run(ExtNodeSet nodeSet) throws IOException {
        // synchronize external changes into the WBS
        List<ExtChange> changes = applyInboundExtChanges(nodeSet);

        // synchronize WBS changes back to the external system
        applyOutboundWbsChanges(nodeSet, changes);

        // save metadata to record the completed operation
        saveMetadata();
    }

    private List<ExtChange> applyInboundExtChanges(ExtNodeSet nodeSet)
            throws IOException {
        // start tracking the time required for inbound sync
        inboundTime.start();

        // get the most recent data and load the team project
        File dataDir = dataTarget.getDirectory();
        String logPrefix = "[" + extSystemID + "/" + dataDir.getName() + "] - ";
        log.fine(logPrefix + "Checking for changes");
        dataTarget.update();
        syncData.checkComodification();
        metadata = syncData.getMetadata();
        TeamProject teamProject = new QuickTeamProject(dataDir, "");

        // let clients know we're starting an inbound sync
        daemonMetadata.setState(State.Inbound, inboundTime.getMaxTime());
        daemonMetadata.setSyncRequestPending(false);

        // retrieve the list of nodes from the external system
        ExtSynchronizer sync = new ExtSynchronizer(teamProject, extSystemName,
                extSystemID, metadata);
        Set<String> priorIDs = sync.getIDsOfNodesUsedInWbs();
        List<ExtNode> extNodes = nodeSet.getExtNodes(priorIDs);

        // write the external nodes to the log if requested
        if (debug.isLoggable(Level.FINEST))
            debugWriteExtNodes(extNodes);

        // perform a trial sync operation
        sync.sync(extNodes);
        if (sync.wasWbsChanged() == false) {
            // no changes were needed to the WBS
            log.fine(logPrefix + "No WBS changes needed");

        } else {
            // changes are needed
            log.info(logPrefix + "Applying external changes to WBS");

            try {
                // lock the WBS dir so we can make the changes atomically
                log.finer(logPrefix + "Locking data");
                String agentName = extSystemName + " Synchronizer";
                dataTarget.lock(agentName);

                // refresh the team project with the latest data, in case it
                // changed after our retrieval above
                log.finer(logPrefix + "Refreshing data");
                syncData.dispose();
                dataTarget.update();
                teamProject.reload();
                metadata = syncData.getMetadata();

                // run the sync process again
                log.finer(logPrefix + "Computing changes");
                sync = new ExtSynchronizer(teamProject, extSystemName,
                        extSystemID, metadata);
                sync.sync(extNodes);

                // save the changes to the WBS
                log.finer(logPrefix + "Saving changes");
                teamProject.save();
                sync.updateProjDump();

                // add an entry to the change history
                ChangeHistory changeHistory = new ChangeHistory(dataDir);
                changeHistory.addEntry(extSystemName);
                changeHistory.write(dataDir);

                // flush changes to the working directory
                log.finer(logPrefix + "Publishing changes");
                dataTarget.saveChanges();

            } catch (Exception e) {
                log.log(Level.SEVERE, logPrefix + "Encountered error", e);
                return Collections.EMPTY_LIST;
            } finally {
                log.finer(logPrefix + "Unlocking data");
                dataTarget.unlock();
            }
        }

        // save the new metadata
        try {
            saveMetadata();
        } catch (IOException ioe) {
            log.log(Level.SEVERE, logPrefix + "Error saving metadata", ioe);
        }

        // stop measuring elapsed time for inbound sync
        inboundTime.finish();

        // return the list of reverse sync changes that are needed
        return sync.getExtChangesNeeded();
    }

    private void debugWriteExtNodes(List<ExtNode> extNodes) {
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            SyncXml.writeExtNodes(buf, extSystemID, extSystemName, extNodes);
            debug.finest(buf.toString(SyncXmlConstants.ENCODING));
        } catch (IOException ioe) {
            // can't happen
        }
    }

    private void applyOutboundWbsChanges(ExtNodeSet nodeSet,
            List<ExtChange> changes) throws IOException {
        if (!changes.isEmpty()) {
            // start tracking the time required for outbound sync
            outboundTime.start();

            // let clients know we're starting an outbound sync
            long expectedDuration = outboundTime.getMaxTime() * changes.size();
            daemonMetadata.setState(State.Outbound, expectedDuration);

            // ask our nodeSet to make the changes
            nodeSet.applyWbsChanges(changes, getMetadata());

            // stop measuring elapsed time for outbound sync
            outboundTime.finish(changes.size());
        }
    }


    public boolean isActiveSleepSupported() {
        return daemonMetadata.isSyncRequestSupported();
    }

    public void sleep(long duration) throws IOException {
        if (isActiveSleepSupported() == false)
            sleepSimply(duration);
        else
            sleepWithActivityChecking(duration);
    }

    private void sleepSimply(long duration) throws IOException {
        daemonMetadata.setState(State.Sleep, duration);
        try {
            Thread.sleep(duration);
        } catch (InterruptedException ie) {}
    }

    private void sleepWithActivityChecking(long duration) throws IOException {
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
                if (targetFilesHaveChanged())
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
            try {
                Thread.sleep(refreshDelay);
            } catch (InterruptedException ie) {}

            // recalc how much longer we should sleep after the operations above
            now = System.currentTimeMillis();
            remainingTime = finishTime - now;
        }
    }

    private boolean targetFilesHaveChanged() {
        long newFileTime = 0;
        File[] files = dataTarget.getDirectory().listFiles();
        if (files != null) {
            for (File f : files) {
                String name = f.getName().toLowerCase();
                if (name.endsWith("-data.pdash") || name.equals("projdump.xml"))
                    newFileTime = Math.max(newFileTime, f.lastModified());
            }
        }
        if (newFileTime <= maxFileTime) {
            return false;
        } else {
            maxFileTime = newFileTime;
            return true;
        }
    }


    public SyncMetadata getMetadata() {
        return metadata;
    }

    public void saveMetadata() throws IOException {
        syncData.saveChanges();
        maxFileTime = Math.max(maxFileTime, syncData.getFileTimestamp());
    }


    /**
     * In a master project/subproject environment, the TeamProject object
     * automatically creates ImportDirectory objects for all of the interrelated
     * projects. That is unnecessary for our purposes, so we create this
     * subclass which skips the ImportDirectory creation step.
     */
    static class QuickTeamProject extends TeamProject {

        public QuickTeamProject(File directory, String projectName) {
            super(directory, projectName);
        }

        @Override
        protected ImportDirectory getProjectDataDirectory(Element e,
                boolean checkExists) {
            return null;
        }

    }

}
