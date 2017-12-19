// Copyright (C) 2017 Tuma Solutions, LLC
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

import java.io.File;
import java.io.IOException;
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

    private List<ExtNode> extNodes;

    private static final Logger log = ExtSynchronizer.log;


    public ExtSyncCoordinator(TeamProjectDataTarget dataTarget,
            String extSystemName, String extSystemID, List<ExtNode> extNodes) {
        this.dataTarget = dataTarget;
        this.extSystemName = extSystemName;
        this.extSystemID = extSystemID;
        this.extNodes = extNodes;
    }

    public void run() {
        // get the most recent data and load the team project
        File dataDir = dataTarget.getDirectory();
        String logPrefix = "[" + extSystemID + "/" + dataDir.getName() + "] - ";
        log.fine(logPrefix + "Checking for changes");
        try {
            dataTarget.update();
        } catch (IOException e1) {
            log.severe(logPrefix + "Could not retrieve current data");
            return;
        }
        TeamProject teamProject = new QuickTeamProject(dataDir, "");

        // perform a trial sync operation
        ExtSynchronizer sync = new ExtSynchronizer(teamProject, extSystemName,
                extSystemID);
        sync.sync(extNodes);
        if (sync.wasChangeMade() == false) {
            // no changes were needed to the WBS
            log.fine(logPrefix + "No WBS changes needed");

        } else {
            // changes are needed
            log.info(logPrefix + "Applying external changes to WBS");

            try {
                // lock the WBS dir so we can make the changes atomically
                log.finest(logPrefix + "Locking data");
                String agentName = extSystemName + " Synchronizer";
                dataTarget.lock(agentName);

                // refresh the team project with the latest data, in case it
                // changed after our retrieval above
                log.finest(logPrefix + "Refreshing data");
                dataTarget.update();
                teamProject.reload();

                // run the sync process again
                log.finest(logPrefix + "Computing changes");
                sync = new ExtSynchronizer(teamProject, extSystemName,
                        extSystemID);
                sync.sync(extNodes);

                // save the changes to the WBS
                log.finest(logPrefix + "Saving changes");
                teamProject.save();
                sync.updateProjDump();

                // add an entry to the change history
                ChangeHistory changeHistory = new ChangeHistory(dataDir);
                changeHistory.addEntry(extSystemName);
                changeHistory.write(dataDir);

                // flush changes to the working directory
                log.finest(logPrefix + "Publishing changes");
                dataTarget.saveChanges();

            } catch (Exception e) {
                log.log(Level.SEVERE, logPrefix + "Encountered error", e);
                return;
            } finally {
                log.finest(logPrefix + "Unlocking data");
                dataTarget.unlock();
            }
        }

        // TODO: reverse sync logic
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
