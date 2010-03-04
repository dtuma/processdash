// Copyright (C) 2002-2010 Tuma Solutions, LLC
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

package teamdash.templates.setup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import net.sourceforge.processdash.data.ImmutableStringData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.templates.DashPackage;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.NetworkDriveList;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class MoveProjectWizard extends TinyCGIBase {

    // query parameter used to control logic flow.
    private static final String PAGE = "page";

    // information for the start page
    private static final String START_URI = "moveStart.shtm";
    private static final String START = "start";
    // information for the version warning page
    static final String VERSION_URI = "moveVersionWarning.shtm";
    // information for the new directory page
    static final String NEWDIR_URI = "moveNewDir.shtm";
    private static final String NEWDIR = "newDir";
    // information for the confirm page
    private static final String CONFIRM_URI = "moveConfirm.shtm";
    private static final String CONFIRM = "confirm";
    // information for the success page
    static final String SUCCESS_URI = "moveSuccess.shtm";

    private static final String MASTER_ROOT = "/MasterRoot";
    private static final String TEAM_ROOT = "/TeamRoot";
    private static final String TEAM_MASTER_FLAG = "move//Is_Master";
    private static final String TEAM_NAME = "move//Team";
    private static final String TEAM_NAME_LOWER = "move//team";
    private static final String FILE_SEP = "move//File_Sep";
    private static final String CURRENT_TEAM_DIR = "move//Current_Team_Dir";
    private static final String CURRENT_DATA_DIR = "move//Current_Data_Dir";
    private static final String OOD_MEMBER_LIST =
            "move//Out_Of_Date_Team_Member_List";
    private static final String TEAM_DIR = "move//Team_Dir";
    private static final String DATA_DIR = "move//Data_Dir";



    private String projectID;
    private String processID;
    private boolean isMaster;




    protected void doGet() {}

    protected void doPost() throws IOException {
        parseFormData();
    }

    public void service(InputStream in, OutputStream out, Map env)
            throws IOException {
        super.service(in, out, env);

        try {
            handleRequest();
        } catch (MoveProjectException mp) {
            String page = (mp.page != null ? mp.page : "moveError.shtm");
            printRedirect(page + "?" + mp.query);
        }
        this.out.flush();
    }

    private void handleRequest() throws MoveProjectException {
        checkProject();

        String page = getParameter(PAGE);
        if (START.equals(page))        handleStartPage();
        else if (NEWDIR.equals(page))  handleNewDirPage();
        else if (CONFIRM.equals(page)) handleConfirmPage();
        else                           showStartPage();
    }

    /**
     * Checks to ensure that the prefix points to a team or master project.
     * If so, sets the projectType and processID fields accordingly.
     * If not, throws a {@link MoveProjectException}.
     * @throws MoveProjectException
     */
    private void checkProject() throws MoveProjectException {
        DashHierarchy hierarchy = getPSPProperties();
        PropertyKey key = hierarchy.findExistingKey(getPrefix());
        String templateID = hierarchy.getID(key);

        if (templateIs(templateID, MASTER_ROOT)) {
            this.isMaster = true;
            putValue(TEAM_MASTER_FLAG, "t");
            putValue(TEAM_NAME, "Master");
            putValue(TEAM_NAME_LOWER, "master");
        } else if (templateIs(templateID, TEAM_ROOT)) {
            this.isMaster = false;
            putValue(TEAM_MASTER_FLAG, (SimpleData) null);
            putValue(TEAM_NAME, "Team");
            putValue(TEAM_NAME_LOWER, "team");
        } else {
            putValue(TEAM_NAME, "");
            putValue(TEAM_NAME_LOWER, "");
            throw new MoveProjectException("notTeamProject");
        }

        projectID = getStringValue(TeamDataConstants.PROJECT_ID);
        if (!hasValue(projectID))
            throw new MoveProjectException("noProjectID");
    }
    private boolean templateIs(String templateID, String suffix) {
        if (templateID != null && templateID.endsWith(suffix)) {
            int idLen = templateID.length() - suffix.length();
            this.processID = templateID.substring(0, idLen);
            return true;
        }
        return false;
    }



    private void showStartPage() {
        printRedirect(START_URI);
    }

    private void handleStartPage() throws MoveProjectException {
        File currentDataDir = getCurrentDataDirectory();
        File currentTeamDir = currentDataDir.getParentFile().getParentFile();
        putValue(CURRENT_TEAM_DIR, currentTeamDir.getPath());
        putValue(CURRENT_DATA_DIR, currentDataDir.getPath());

        File settingsFile = new File(currentDataDir, "settings.xml");
        if (!settingsFile.isFile())
            throw new MoveProjectException("emptyTeamDir")
                .append("path", currentDataDir.getPath());

        if (someTeamMembersAreOutOfDate())
            printRedirect(VERSION_URI);
        else
            showNewDirPage();
    }

    /**
     * Lookup the current data directory for this project, ensuring that it
     * exists and that it is not served by a Team Server.
     */
    private File getCurrentDataDirectory() throws MoveProjectException {
        if (testValue(TeamDataConstants.TEAM_DATA_DIRECTORY_URL))
            throw new MoveProjectException("hasURL");

        putValue(FILE_SEP, File.separator);
        File result = null;

        String uncDir = getStringValue(TeamDataConstants.TEAM_DIRECTORY_UNC);
        if (hasValue(uncDir)) {
            File teamDir = new File(uncDir);
            File data = new File(teamDir, "data");
            result = new File(data, projectID);
            if (result.isDirectory())
                return result;
        }

        String dir = getStringValue(TeamDataConstants.TEAM_DIRECTORY);
        if (hasValue(dir)) {
            File teamDir = new File(dir);
            File data = new File(teamDir, "data");
            result = new File(data, projectID);
            if (result.isDirectory())
                return result;
        }

        if (result == null)
            throw new MoveProjectException("noTeamDir");
        else
            throw new MoveProjectException("badTeamDir")
                    .append("path", result.getPath());
    }

    private boolean someTeamMembersAreOutOfDate() {
        if (isMaster)
            return false;

        try {
            ListData members = getOutOfDateTeamMembers();
            putValue(OOD_MEMBER_LIST, members);
            return (members.size() > 0);
        } catch (Exception e) {
            return false;
        }
    }

    private ListData getOutOfDateTeamMembers() throws Exception {
        ListData result = new ListData();
        String uri = resolveRelativeURI("teamMetricsStatus");
        uri = uri + "?for=Corresponding_Project_Nodes&xml";
        String xml = getRequestAsString(uri);
        NodeList nl = XMLUtils.parse(xml).getElementsByTagName("member");
        for (int i = 0;  i < nl.getLength();  i++) {
            Element m = (Element) nl.item(i);
            String version = m.getAttribute("dashVersion");
            if (DashPackage.compareVersions(version, "1.10.5") < 0)
                result.add(HTMLUtils.escapeEntities(m.getAttribute("name")));
        }
        return result;
    }



    private void showNewDirPage() {
        printRedirect(NEWDIR_URI);
    }

    private void handleNewDirPage() {
        String teamDir = getParameter("teamDir");
        if (teamDir == null || teamDir.trim().length() == 0) {
            printRedirect(NEWDIR_URI + "?missing");
            return;
        }
        File teamDirFile = new File(teamDir.trim()).getAbsoluteFile();
        putValue(TEAM_DIR, teamDirFile.getPath());

        File dataDirFile = new File(new File(teamDirFile, "data"), projectID);
        putValue(DATA_DIR, dataDirFile.getPath());

        if (MoveProjectWorker.dataDirAlreadyExists(dataDirFile)) {
            printRedirect(NEWDIR_URI + "?exists");
            return;
        }

        String confirm = getParameter("confirm");
        if ((confirm != null && confirm.equals(teamDir))
                || ensureNetworkDrive(teamDir)) {
            showConfirmPage();

        } else {
            printRedirect(NEWDIR_URI + "?confirm");
        }
    }

    /** Return true if the filename appears to be on a network drive.
     * @param filename a canonical filename
     */
    private boolean ensureNetworkDrive(String filename) {
        if (filename == null) return false;

        // if we weren't able to get a list of network drives, then we
        // have to give the user the benefit of the doubt.
        if (!getNetworkDriveList().wasSuccessful())
            return true;

        return getNetworkDriveList().onNetworkDrive(filename);
    }



    private void showConfirmPage() {
        printRedirect(CONFIRM_URI);
    }

    private void handleConfirmPage() throws MoveProjectException {
        String oldTeamDir = getStringValue(CURRENT_TEAM_DIR);
        String newTeamDir = getStringValue(TEAM_DIR);
        if (!hasValue(oldTeamDir) || !hasValue(newTeamDir)) {
            showNewDirPage();
            return;
        }
        String newTeamDirUNC = getNetworkDriveList().toUNCName(newTeamDir);
        String masterPrefix = (isMaster ? getPrefix()
                : getStringValue(TeamDataConstants.MASTER_PROJECT_PATH));

        new MoveProjectWorker(getDashboardContext(), getPrefix(), masterPrefix,
                projectID, processID, isMaster, oldTeamDir, newTeamDir,
                newTeamDirUNC).run();

        printRedirect(SUCCESS_URI);
    }




    // Utility methods

    private NetworkDriveList networkDriveList = null;
    private NetworkDriveList getNetworkDriveList() {
        if (networkDriveList == null)
            networkDriveList = new NetworkDriveList();
        return networkDriveList;
    }


    /**
     * Send an HTTP redirect command to the browser, sending it to the relative
     * URI named by filename.
     */
    private void printRedirect(String filename) {
        out.print("Location: ");
        out.print(filename);
        out.print("\r\n\r\n");
    }

    /** Save a value into the data repository. */
    private void putValue(String name, String value) {
        putValue(name, value == null ? null : new ImmutableStringData(value));
    }

    private void putValue(String name, SimpleData dataValue) {
        DataRepository data = getDataRepository();
        String prefix = getPrefix();
        if (prefix == null)
            prefix = "";
        String dataName = DataRepository.createDataName(prefix, name);
        data.putValue(dataName, dataValue);
    }

    /** Get a value from the data repository as a String. */
    private String getStringValue(String name) {
        SimpleData d = getSimpleValue(name);
        return (d == null ? null : d.format());
    }

    /** Get a value from the data repository as a String. */
    private boolean testValue(String name) {
        SimpleData d = getSimpleValue(name);
        return (d == null ? false : d.test());
    }

    /** Get a value from the data repository. */
    private SimpleData getSimpleValue(String name) {
        DataRepository data = getDataRepository();
        String prefix = getPrefix();
        if (prefix == null)
            prefix = "";
        String dataName = DataRepository.createDataName(prefix, name);
        SimpleData d = data.getSimpleValue(dataName);
        return d;
    }

    /** Return true if a string is non-null and not empty */
    private boolean hasValue(String s) {
        return StringUtils.hasValue(s);
    }

}
