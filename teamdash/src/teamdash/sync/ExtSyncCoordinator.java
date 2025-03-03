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

import static teamdash.wbs.WBSFilenameConstants.CHANGE_HISTORY_FILE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.tool.bridge.ResourceCollection;

import teamdash.sync.DaemonMetadata.State;
import teamdash.sync.ExtNodeSet.ExportCreationCapable;
import teamdash.sync.ExtNodeSet.LifecycleAware;
import teamdash.sync.ExtNodeSet.WithConfig;
import teamdash.wbs.ChangeHistory;
import teamdash.wbs.TeamProject;

public class ExtSyncCoordinator {

    public static final String NO_EXT_CHANGES = "noExternalChanges";


    private TeamProjectDataTarget dataTarget;

    private String extSystemName;

    private String extSystemID;

    protected SyncDataFile syncData;

    private Properties config;

    private String noExternalChanges;

    private DaemonMetadata daemonMetadata;

    private ElapsedTimeMonitor exportTime, inboundTime, outboundTime;

    private SyncMetadata metadata;

    private TeamProject teamProject;

    private boolean wbsNeedsSave;

    private long maxFileTime;

    private String logPrefix;

    private Logger log;

    private static final Logger debug = ExtSynchronizer.debug;


    public ExtSyncCoordinator(TeamProjectDataTarget dataTarget,
            String extSystemName, String extSystemID, Properties config,
            DaemonMetadata daemonMetadata) {
        this.dataTarget = dataTarget;
        this.extSystemName = extSystemName;
        this.extSystemID = extSystemID;
        this.syncData = new SyncDataFile(dataTarget,
                extSystemID + "-sync.pdash");
        this.syncData.setLogGlobal(config.getProperty(GLOBAL_LOG_SETTING));
        this.config = config;
        this.daemonMetadata = daemonMetadata;
        this.log = syncData.getLogger();
        this.exportTime = new ElapsedTimeMonitor(20, 5000);
        this.inboundTime = new ElapsedTimeMonitor(20, 5000);
        this.outboundTime = new ElapsedTimeMonitor(20, 1000);
    }

    public void setExtSystemName(String newName) {
        this.extSystemName = newName;
    }

    /**
     * Perform one round-trip synchronization run between this WBS and the given
     * set of external nodes.
     */
    public void run(ExtNodeSet nodeSet) throws IOException {
        // refresh the data target and create a team project for this run
        prepareForSyncRun();

        // closed projects require no synchronization
        if (teamProject.getBoolUserSetting("projectClosed"))
            return;

        // debug print configuration properties if requested
        if (log.isLoggable(Level.FINE) && nodeSet instanceof WithConfig)
            log.fine(ExtSyncUtil.dumpTargetProperties(logPrefix,
                ((WithConfig) nodeSet).getEffectiveConfig()));

        // inform the node set that a sync pass is starting
        daemonMetadata.setState(State.Start, 100);
        if (nodeSet instanceof LifecycleAware)
            ((LifecycleAware) nodeSet).syncStarting();

        // copy new WBS nodes into the external system if needed
        if (nodeSet instanceof ExportCreationCapable)
            createExportedWbsNodes((ExportCreationCapable) nodeSet);

        // synchronize external changes into the WBS
        List<ExtChange> changes = applyInboundExtChanges(nodeSet);

        // synchronize WBS changes back to the external system
        applyOutboundWbsChanges(nodeSet, changes);

        // inform the node set that a sync pass is finishing
        if (nodeSet instanceof LifecycleAware)
            ((LifecycleAware) nodeSet).syncFinishing();

        // save metadata to record the completed operation
        saveMetadata();
        daemonMetadata.setState(State.Finish, 100);
        teamProject = null;
    }

    private void prepareForSyncRun() throws IOException {
        // get the most recent data and load the team project
        syncData.setLogGlobal(config.getProperty(GLOBAL_LOG_SETTING));
        ResourceCollection collection = dataTarget.getCollection();
        String name = collection.getDescription();
        int pos = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1;
        logPrefix = "[" + extSystemID + "/" + name.substring(pos) + "] - ";
        log.fine(logPrefix + "Checking for changes");
        dataTarget.update();
        syncData.checkComodification();
        noExternalChanges = config.getProperty(NO_EXT_CHANGES);
        metadata = syncData.getMetadata();
        metadata.setStr(noExternalChanges, NO_EXT_CHANGES);
        teamProject = new ExtSyncTeamProject(collection);
        wbsNeedsSave = false;
    }

    private void createExportedWbsNodes(ExportCreationCapable nodeSet)
            throws IOException {
        // check to see if any exported nodes need creating. Abort if none
        ExtSynchronizer sync = makeSynchronizer();
        wbsNeedsSave = sync.wasWbsChanged();
        List<ExportedWbsNode> exportedNodes = sync.getWbsNodesToExport();
        int numExportedNodes = exportedNodes.size();
        if (numExportedNodes == 0) {
            return;
        } else if (noExternalChanges != null) {
            // abort if we're in "no changes" mode, log messages instead
            for (ExportedWbsNode expNode : exportedNodes) {
                log.fine("External changes disabled; skipping creation of "
                        + expNode.getType() + " '" + expNode.getName() + "'");
            }
            return;
        }

        // start tracking the time required for export creation
        exportTime.start();

        // let clients know we're starting an export creation step
        long expectedDuration = exportTime.getMaxTime() * numExportedNodes;
        daemonMetadata.setState(State.Export, expectedDuration);
        daemonMetadata.setSyncRequestPending(false);
        log.info(logPrefix + "Publishing new exported nodes");

        // ask the node set to create the exported nodes
        nodeSet.createExportedWbsNodes(exportedNodes);

        // save the metadata generated by the export operation
        trySaveMetadata();

        // stop measuring elapsed time for export creation
        exportTime.finish(numExportedNodes);
    }

    private List<ExtChange> applyInboundExtChanges(ExtNodeSet nodeSet)
            throws IOException {
        // start tracking the time required for inbound sync
        inboundTime.start();

        // let clients know we're starting an inbound sync
        daemonMetadata.setState(State.Inbound, inboundTime.getMaxTime());
        daemonMetadata.setSyncRequestPending(false);

        // retrieve the list of nodes from the external system
        ExtSynchronizer sync = makeSynchronizer();
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
                metadata.setStr(noExternalChanges, NO_EXT_CHANGES);

                // run the sync process again
                log.finer(logPrefix + "Computing changes");
                sync = makeSynchronizer();
                sync.sync(extNodes);

                // save the changes to the WBS
                log.finer(logPrefix + "Saving changes");
                teamProject.save();
                sync.updateProjDump();

                // add an entry to the change history
                ResourceCollection collection = dataTarget.getCollection();
                ChangeHistory changeHistory = new ChangeHistory(
                        collection.getInputStream(CHANGE_HISTORY_FILE));
                changeHistory.addEntry(extSystemName);
                changeHistory.write(
                    collection.getOutputStream(CHANGE_HISTORY_FILE, 0));

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
        trySaveMetadata();

        // stop measuring elapsed time for inbound sync
        inboundTime.finish();

        // return the list of reverse sync changes that are needed
        return sync.getExtChangesNeeded();
    }

    private ExtSynchronizer makeSynchronizer() {
        return new ExtSynchronizer(dataTarget, teamProject, extSystemName,
                extSystemID, metadata, wbsNeedsSave);
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
        if (changes.isEmpty()) {
            // nothing to do
        } else if (noExternalChanges != null) {
            // abort if we're in "no changes" mode, log messages instead
            for (ExtChange c : changes)
                log.fine("External changes disabled; skipping modification of "
                        + c.extNode.getName() + "' - " + c.attrValues);
        } else {
            // start tracking the time required for outbound sync
            outboundTime.start();

            // let clients know we're starting an outbound sync
            long expectedDuration = outboundTime.getMaxTime() * changes.size();
            daemonMetadata.setState(State.Outbound, expectedDuration);
            log.info(logPrefix + "Applying WBS changes to external system");

            // ask our nodeSet to make the changes
            nodeSet.applyWbsChanges(changes, getMetadata());

            // stop measuring elapsed time for outbound sync
            outboundTime.finish(changes.size());
        }
    }


    public boolean targetFilesHaveChanged() {
        long newFileTime = 0;
        ResourceCollection collection = dataTarget.getCollection();
        for (String resourceName : collection.listResourceNames()) {
            if (ExtSyncUtil.isExtSyncTrigger(resourceName))
                newFileTime = Math.max(newFileTime,
                    collection.getLastModified(resourceName));
        }
        if (newFileTime <= maxFileTime) {
            return syncData.isTimestampMismatch();
        } else {
            maxFileTime = newFileTime;
            return true;
        }
    }

    public SyncDataFile getSyncDataFile() {
        return syncData;
    }

    public SyncMetadata getMetadata() {
        return metadata;
    }

    private void trySaveMetadata() {
        try {
            saveMetadata();
        } catch (IOException ioe) {
            log.log(Level.SEVERE, logPrefix + "Error saving metadata", ioe);
        }
    }

    public void saveMetadata() throws IOException {
        syncData.saveChanges();
        maxFileTime = Math.max(maxFileTime, syncData.getFileTimestamp());
    }


    static final String GLOBAL_LOG_SETTING = "log.global";

}
