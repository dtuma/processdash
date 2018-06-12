// Copyright (C) 2017-2018 Tuma Solutions, LLC
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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Element;

import net.sourceforge.processdash.tool.bridge.client.ImportDirectory;

import teamdash.wbs.ChangeHistory;
import teamdash.wbs.TeamProject;

public class ExtSyncCoordinator {

    private TeamProjectDataTarget dataTarget;

    private String extSystemName;

    private String extSystemID;

    private SyncDataFile syncData;

    private SyncMetadata metadata;

    private static final Logger log = ExtSynchronizer.log;

    private static final Logger debug = ExtSynchronizer.debug;


    public ExtSyncCoordinator(TeamProjectDataTarget dataTarget,
            String extSystemName, String extSystemID) {
        this.dataTarget = dataTarget;
        this.extSystemName = extSystemName;
        this.extSystemID = extSystemID;
        this.syncData = new SyncDataFile(dataTarget,
                extSystemID + "-sync.pdash");
    }

    public List<ExtChange> run(List<ExtNode> extNodes) {
        // write the external nodes to the log if requested
        if (debug.isLoggable(Level.FINEST))
            debugWriteExtNodes(extNodes);

        // get the most recent data and load the team project
        File dataDir = dataTarget.getDirectory();
        String logPrefix = "[" + extSystemID + "/" + dataDir.getName() + "] - ";
        log.fine(logPrefix + "Checking for changes");
        try {
            dataTarget.update();
            metadata = syncData.getMetadata();
        } catch (IOException e1) {
            log.severe(logPrefix + "Could not retrieve current data");
            return Collections.EMPTY_LIST;
        }
        TeamProject teamProject = new QuickTeamProject(dataDir, "");

        // perform a trial sync operation
        ExtSynchronizer sync = new ExtSynchronizer(teamProject, extSystemName,
                extSystemID, metadata);
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


    public SyncMetadata getMetadata() {
        return metadata;
    }

    public void saveMetadata() throws IOException {
        syncData.saveChanges();
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
