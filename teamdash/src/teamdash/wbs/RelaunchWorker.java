// Copyright (C) 2014 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package teamdash.wbs;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import net.sourceforge.processdash.util.XMLUtils;

import teamdash.wbs.columns.PercentCompleteColumn;
import teamdash.wbs.columns.TeamTimeColumn;

public class RelaunchWorker {

    public static final String RELAUNCH_PROJECT_SETTING = "relaunchProject";

    public static final String RELAUNCH_SOURCE_ID = "relaunchSourceID";


    private TeamProject teamProject;

    private DataTableModel dataModel;

    private WBSModel wbs;

    private static final String DUMP_DATA_FILE = "relaunchDump.xml";


    public RelaunchWorker(TeamProject teamProject, DataTableModel data) {
        this.teamProject = teamProject;
        this.dataModel = data;
        this.wbs = teamProject.getWBS();
    }

    public void run() {
        try {
            runImpl();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void runImpl() throws Exception {
        File historicalDumpFile = loadHistoricalTeamMemberData();
        updateProjectName();
        moveScheduleStartDates();
        deleteCompletedItems();
        adjustInProgressItems();
        writeRelaunchSourceIDs();
        discardHistoricalDataAttributes();
        cleanUp(historicalDumpFile);
    }


    /**
     * Load reverse-sync data containing information about individual
     * performance during the previous project cycle
     */
    private File loadHistoricalTeamMemberData() throws IOException,
            SAXException {
        // load the file of user dump data from the team project directory
        File historicalDumpFile = new File(teamProject.getStorageDirectory(),
                DUMP_DATA_FILE);
        InputStream in = new FileInputStream(historicalDumpFile);
        Element dumpData = XMLUtils.parse(new BufferedInputStream(in))
                .getDocumentElement();

        // perform a reverse-sync operation using that dump data
        WBSSynchronizer sync = new WBSSynchronizer(teamProject, dataModel,
                dumpData);
        sync.run();
        return historicalDumpFile;
    }


    /**
     * Change the name of the WBS root node to match the new project name.
     */
    private void updateProjectName() {
        String projectName = teamProject.getProjectSettings().getAttribute(
            "projectName");
        if (XMLUtils.hasValue(projectName))
            teamProject.setProjectName(projectName);
    }


    /**
     * Move the start dates for all team members
     */
    private void moveScheduleStartDates() {
        teamProject.getTeamMemberList().moveAllStartDates(new Date());
    }


    /**
     * Find WBS items that are 100% complete, and delete them from the WBS.
     */
    private void deleteCompletedItems() {
        List nodesToDelete = new ArrayList();
        listCompletedItems(nodesToDelete, wbs.getRoot());
        wbs.deleteNodes(nodesToDelete);
    }

    private void listCompletedItems(List dest, WBSNode wbsNode) {
        for (WBSNode child : wbs.getChildren(wbsNode)) {
            if (PercentCompleteColumn.isComplete(child)) {
                dest.add(child);
                dest.addAll(Arrays.asList(wbs.getDescendants(child)));
            } else {
                listCompletedItems(dest, child);
            }
        }
    }


    /**
     * Find leaf tasks that are in progress, and reduce the planned time based
     * on the amount of actual time that has been spent so far.
     */
    private void adjustInProgressItems() {
        int col = dataModel.findColumn(TeamTimeColumn.COLUMN_ID);
        TeamTimeColumn teamTimeCol = (TeamTimeColumn) dataModel.getColumn(col);
        teamTimeCol.replanInProgressTime();
    }


    /**
     * The data structures in this project are partial copies of data from an
     * earlier project. Record attributes that help us to keep track of where
     * the nodes came from.
     */
    private void writeRelaunchSourceIDs() {
        String projectSourceID = teamProject.getUserSetting(RELAUNCH_SOURCE_ID);
        if (!XMLUtils.hasValue(projectSourceID))
            return;

        writeRelaunchSourceIDs(projectSourceID, teamProject.getWBS());
        writeRelaunchSourceIDs(projectSourceID, teamProject.getWorkflows());
        writeRelaunchSourceIDs(projectSourceID, teamProject.getMilestones());
    }

    private void writeRelaunchSourceIDs(String projectSourceID, WBSModel model) {
        for (WBSNode node : model.getDescendants(model.getRoot())) {
            String existingID = (String) node.getAttribute(RELAUNCH_SOURCE_ID);
            if (!XMLUtils.hasValue(existingID)) {
                String sourceID = projectSourceID + ":" + node.getUniqueID();
                node.setAttribute(RELAUNCH_SOURCE_ID, sourceID);
            }
        }
    }


    /**
     * Our operations required us to load historical data from a past project.
     * Discard that data from in-memory data structures.
     */
    private void discardHistoricalDataAttributes() {
        // discard all transient attributes (including actual data and
        // calculated values)
        for (WBSNode node : wbs.getDescendants(wbs.getRoot()))
            node.discardTransientAttributes(true);

        // fire a WBS event to force the recalculation of all data columns
        wbs.fireTableRowsUpdated(0, 0);
    }


    /**
     * Delete files from disk that triggered the project relaunch operation.
     */
    private void cleanUp(File historicalDumpFile) {
        teamProject.getUserSettings().remove(RELAUNCH_PROJECT_SETTING);
        historicalDumpFile.delete();
    }

}
