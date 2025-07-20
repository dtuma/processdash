// Copyright (C) 2025 Tuma Solutions, LLC
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

package net.sourceforge.processdash.team.setup.move;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.SwingUtilities;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.team.setup.RepairImportInstruction;
import net.sourceforge.processdash.team.setup.TeamDirPermissionSettingsWriter;
import net.sourceforge.processdash.team.setup.TeamProjectUtils;
import net.sourceforge.processdash.team.setup.TeamProjectUtils.ProjectType;
import net.sourceforge.processdash.team.setup.TeamSettingsRepublisher;
import net.sourceforge.processdash.tool.bridge.client.TeamServerSelector;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;

/**
 * After a dataset has been imported into the PDES manually, this class can
 * clean up the project data directory pointers so they point to the server.
 */
public class TeamServerManualImportCleanup implements TeamDataConstants {

    private DashboardContext ctx;

    public TeamServerManualImportCleanup(DashboardContext ctx) {
        this.ctx = ctx;
    }

    private int numFixed;

    private boolean overwriteUrls;

    private List<String> masterProjectUpdateUris;

    public int run(String path) {
        masterProjectUpdateUris = new ArrayList<String>();
        if (TeamServerSelector.isDefaultTeamServerConfigured())
            scanProjects(ctx.getHierarchy().findClosestKey(path));

        if (numFixed > 0 && !Settings.isReadOnly()) {
            TeamSettingsRepublisher.getInstance().republish(true);
            if (!masterProjectUpdateUris.isEmpty()) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        updateMasterProjectLinks();
                    }});
            }
        }

        return numFixed;
    }

    private void scanProjects(PropertyKey node) {
        ProjectType type = TeamProjectUtils.getProjectType(ctx, node);
        if (type != null) {
            fixProject(node.path());
            if (type == ProjectType.Master)
                addMasterProjectUpdateUri(node);
        } else {
            DashHierarchy hier = ctx.getHierarchy();
            for (int i = hier.getNumChildren(node); i-- > 0;)
                scanProjects(hier.getChildKey(node, i));
        }
    }

    private void fixProject(String path) {
        DataContext data = ctx.getData().getSubcontext(path);
        URL u = getProjectDataNewUrl(data);
        if (u == null)
            return;

        // store the new URL into the project root
        data.putValue(TEAM_DATA_DIRECTORY_URL, StringData.create(u.toString()));
        data.putValue(TEAM_DIRECTORY, null);
        data.putValue(TEAM_DIRECTORY_UNC, null);

        // fix the import instruction for the project
        if (Settings.isPersonalMode()) {
            RepairImportInstruction.maybeRepairForIndividual(data);
        } else {
            TeamDirPermissionSettingsWriter.setExtraUsers(data, "*");
            RepairImportInstruction.maybeRepairForTeam(data);
        }

        // write a debug message about the change
        System.out.println("Set project URL to " + u);
        numFixed++;
    }

    private URL getProjectDataNewUrl(DataContext data) {
        // if a file-based location is configured, remap to a URL
        SimpleData sd = data.getSimpleValue(TEAM_DATA_DIRECTORY);
        if (sd != null && sd.test()) {
            File f = new File(sd.format());
            return TeamServerSelector.getServerURL(f);
        }

        // if a URL is configured, see if it should be changed
        sd = data.getSimpleValue(TEAM_DATA_DIRECTORY_URL);
        if (sd != null && sd.test() && !overwriteUrls) {
            URL u = TeamServerSelector.resolveServerURL(sd.format());
            if (u == null)
                // if the current project URL could not be resolved, fall
                // through and try the projectID-based approach below
                ;
            else if (u.toString().equals(sd.format()))
                // if the existing URL is correct, return null to indicate no
                // changes are needed
                return null;
            else
                // return the new URL we resolved
                return u;
        }

        // otherwise, try building a URL from the project ID
        sd = data.getSimpleValue(PROJECT_ID);
        File f = new File("./data/" + sd);
        return TeamServerSelector.getServerURL(f);
    }

    private void addMasterProjectUpdateUri(PropertyKey node) {
        String tid = ctx.getHierarchy().getID(node);
        int slashPos = (tid == null ? -1 : tid.lastIndexOf('/'));
        if (slashPos == -1)
            return;

        String processID = tid.substring(0, slashPos);
        String uri = HTMLUtils.urlEncodePath(node.path()) + "//" + processID
                + "/setup/subprojectEdit?do=update";
        masterProjectUpdateUris.add(uri);
    }

    private void updateMasterProjectLinks() {
        for (String uri : masterProjectUpdateUris) {
            try {
                ctx.getWebServer().getRequest(uri, false);
            } catch (Exception e) {
                System.err.println("Problem updating master links:");
                e.printStackTrace();
            }
        }
    }



    public static class Script extends TinyCGIBase {

        @Override
        protected void writeContents() throws IOException {
            // run the cleanup logic
            String path = getPrefix();
            TeamServerManualImportCleanup tsmic = //
                    new TeamServerManualImportCleanup(getDashboardContext());
            if (parameters.containsKey("force"))
                tsmic.overwriteUrls = true;
            int numFixed = tsmic.run(path);

            // print an HTML doc indicating success
            out.println("<html><head>");
            out.println("<title>Server Import Cleanup</title>");
            out.println("</head><body><h1>Server Import Cleanup</h1>");
            if (path.length() > 0)
                out.println("<h2>" + HTMLUtils.escapeEntities(path) + "</h2>");
            out.println("Server import cleanup ran at " + new Date() + ".");
            if (numFixed == 0)
                out.println("No changes were needed.");
            else
                out.println("Repaired " + numFixed + " project pointers.");
            out.println("</body></html>");
        }
    }



    public static class BGTask implements Runnable {

        private DashboardContext ctx;

        public void setDashboardContext(DashboardContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void run() {
            // if this cleanup task has already run in the past, do nothing
            if (Settings.getBool(AUTO_RUN_FLAG, false))
                return;

            // if no default team server is in effect, do nothing
            if (!TeamServerSelector.isDefaultTeamServerConfigured())
                return;

            // run the cleanup operation
            new TeamServerManualImportCleanup(ctx).run("");

            // set the flag so we do not run automatically again
            InternalSettings.set(AUTO_RUN_FLAG, "true");
            DashController.flushDirtyData();
        }
    }

    private static final String AUTO_RUN_FLAG = //
            "teamServerManualImportCleanup.hasRun";

}
