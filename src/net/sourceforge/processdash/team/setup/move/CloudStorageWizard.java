// Copyright (C) 2022 Tuma Solutions, LLC
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.net.http.TinyCGIException;
import net.sourceforge.processdash.team.setup.TeamMemberDataStatus;
import net.sourceforge.processdash.team.setup.move.MoveProjectException.OutOfDateTeamMembers;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

public class CloudStorageWizard extends TinyCGIBase {

    // constants for wizard flow and user interface
    private static final String ACTION = "action";
    private static final String WELCOME_URI = "cloudWelcome.shtm";
    private static final String START_ACTION = "start";
    private static final String MEMBER_VERSION_URI = "cloudMembers.shtm";
    private static final String MEMBER_CONF_ACTION = "memberConfirm";
    private static final String FOLDER_URI = "cloudFolder.shtm";
    private static final String SAVE_FOLDER_ACTION = "saveFolder";
    private static final String CONFIRM_URI = "cloudConfirm.shtm";
    private static final String CONFIRM_ACTION = "confirm";
    private static final String WAIT_URI = "cloudWait.shtm";
    private static final String MIGRATE_ACTION = "migrate";
    private static final String ERROR_URI = "cloudError.shtm";
    private static final String SUCCESS_URI = "cloudSuccess.shtm";


    protected boolean supportQueryFiles() { return false; }

    protected void doGet() {}

    protected void doPost() throws IOException {
        parseFormData();
    }

    public void service(InputStream in, OutputStream out, Map env)
            throws IOException {
        this._migrator = null;
        super.service(in, out, env);

        try {
            rejectCrossSiteRequests(env);
            handleRequest();

        } catch (TinyCGIException tcgie) {
            // if a cross site request is detected, show the welcome page
            showWelcomePage();

        } catch (MoveProjectException mpe) {
            // if an error occurs, show an appropriate page
            String page = (mpe.page == null ? ERROR_URI : mpe.page);
            printRedirect(page + "?" + mpe.query);
        }

        this.out.flush();
        this._migrator = null;
    }

    private void handleRequest() throws MoveProjectException {
        String action = getParameter(ACTION);
        if (START_ACTION.equals(action))              handleStartAction();
        else if (MEMBER_CONF_ACTION.equals(action))   handleMemberConfAction();
        else if (SAVE_FOLDER_ACTION.equals(action))   handleSaveFolder();
        else if (CONFIRM_ACTION.equals(action))       handleConfirmAction();
        else if (MIGRATE_ACTION.equals(action))       performMigration();
        else                                          showWelcomePage();
    }


    private void showWelcomePage() {
        saveMemberVersionData(MEMBER_CONF_ACTION, null);
        printRedirect(WELCOME_URI);
    }


    private void handleStartAction() {
        try {
            validateRuntimePreconditions();
            validateSourceData(true);
            maybeValidateTeamMemberVersions();
        } catch (MoveProjectException mpe) {
            throw mpe.append("start", "t");
        }
        printRedirect(FOLDER_URI);
    }

    private void validateRuntimePreconditions() {
        getMigrator().validateRuntimePreconditions();
    }

    private void validateSourceData(boolean quick) {
        getMigrator().validateSourceData(quick);
    }

    private void maybeValidateTeamMemberVersions() {
        if (getMemberVersionData(MEMBER_CONF_ACTION) == null)
            validateTeamMemberVersions();
    }

    private void validateTeamMemberVersions() {
        try {
            getMigrator().validateTeamMemberVersions();
        } catch (OutOfDateTeamMembers oodtm) {
            saveMemberVersionData(oodtm.teamMembers);
            throw new MoveProjectException(MEMBER_VERSION_URI, oodtm.query);
        }
    }

    private void saveMemberVersionData(List<TeamMemberDataStatus> teamMembers) {
        Collections.sort(teamMembers, TeamMemberDataStatus.EXPORT_TIME_DESC);
        StringBuilder list = new StringBuilder(",");
        for (int i = 0; i < teamMembers.size(); i++) {
            TeamMemberDataStatus t = teamMembers.get(i);
            saveMemberVersionData(i, "Name", t.ownerName);
            saveMemberVersionData(i, "Projects_HTML",
                formatProjectList(t.projectPath));
            saveMemberVersionData(i, "Export_Date",
                FormatUtil.formatDateTime(t.exportDate));
            saveMemberVersionData(i, "Version", t.dashVersion);
            list.append(i + ",");
        }
        saveMemberVersionData("List", list.toString());
    }

    private void saveMemberVersionData(int num, String name, String value) {
        saveMemberVersionData(num + "_" + name, value);
    }

    private void saveMemberVersionData(String name, String value) {
        String dataName = "Team_Members//" + name;
        StringData sd = (value == null ? null : StringData.create(value));
        getDataContext().putValue(dataName, sd);
    }

    private String getMemberVersionData(String name) {
        String dataName = "Team_Members//" + name;
        SimpleData sd = getDataContext().getSimpleValue(dataName);
        return (sd == null ? null : sd.format());
    }

    private String formatProjectList(String projects) {
        return StringUtils.findAndReplace(HTMLUtils.escapeEntities(projects),
            "\t", "<br/>");
    }


    private void handleMemberConfAction() {
        saveMemberVersionData(MEMBER_CONF_ACTION, "t");
        handleStartAction();
    }


    private void handleSaveFolder() {
        String folder = getParameter("folder");
        String folderConfirm = getParameter("folderConfirm");
        getMigrator().setDestDirectory(folder, folderConfirm);
        validateDestFolder();
        showConfirmPage();
    }

    private void validateDestFolder() {
        try {
            getMigrator().validateDestDirectory();
        } catch (MoveProjectException mpe) {
            throw new MoveProjectException(FOLDER_URI, mpe.query) //
                    .append("destDirErr", "t");
        }
    }


    private void showConfirmPage() {
        generatePostToken();
        printRedirect(CONFIRM_URI);
    }


    private void handleConfirmAction() {
        printRedirect(WAIT_URI);
    }


    private void performMigration() {
        if (!checkPostToken()) {
            showConfirmPage();
            return;
        }

        // perform all validations again before starting the migration
        validateRuntimePreconditions();
        validateSourceData(false);
        validateDestFolder();

        // perform the migration operation
        getMigrator().run();

        // display success message and shut down
        boolean relaunchSuccessful = getMigrator().launchNewDashboard(3000);
        showSuccessPage(relaunchSuccessful);
        initiateShutDown();
    }


    private void showSuccessPage(boolean relaunchSuccessful) {
        String query = (relaunchSuccessful ? "" : "?relaunchFailed");
        printRedirect(SUCCESS_URI + query);
    }


    private void initiateShutDown() {
        new Thread() {
            public void run() {
                waitAndShutDown();
            }
        }.start();
    }

    private void waitAndShutDown() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ie) {
        }
        System.exit(0);
    }


    /**
     * Get the dataset migration coordinator object
     */
    private CloudStorageDatasetMigrator getMigrator() {
        if (_migrator == null)
            _migrator = new CloudStorageDatasetMigrator(getDashboardContext());
        return _migrator;
    }

    private CloudStorageDatasetMigrator _migrator;

    /**
     * Send an HTTP redirect command to the browser, sending it to the relative
     * URI named by filename.
     */
    private void printRedirect(String filename) {
        out.print("Location: ");
        out.print(filename);
        out.print("\r\n\r\n");
    }

}
