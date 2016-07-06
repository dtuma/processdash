// Copyright (C) 2002-2016 Tuma Solutions, LLC
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


package net.sourceforge.processdash.team.setup;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.w3c.dom.Document;

import net.sourceforge.processdash.BackgroundTaskManager;
import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.ImmutableDoubleData;
import net.sourceforge.processdash.data.ImmutableStringData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.EVTaskListData;
import net.sourceforge.processdash.ev.EVTaskListRollup;
import net.sourceforge.processdash.ev.EVTaskListXML;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.Prop;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.team.mcf.MCFManager;
import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.tool.bridge.ResourceCollectionType;
import net.sourceforge.processdash.tool.bridge.client.ResourceBridgeClient;
import net.sourceforge.processdash.tool.bridge.client.TeamServerSelector;
import net.sourceforge.processdash.tool.bridge.client.WorkingDirectory;
import net.sourceforge.processdash.tool.bridge.client.WorkingDirectoryFactory;
import net.sourceforge.processdash.tool.export.DataImporter;
import net.sourceforge.processdash.tool.export.mgr.ExportManager;
import net.sourceforge.processdash.tool.quicklauncher.CompressedInstanceLauncher;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.NetworkDriveList;
import net.sourceforge.processdash.util.RobustFileOutputStream;
import net.sourceforge.processdash.util.RuntimeUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.VersionUtils;
import net.sourceforge.processdash.util.XMLUtils;
import net.sourceforge.processdash.util.lock.AlreadyLockedException;
import net.sourceforge.processdash.util.lock.LockFailureException;

/** This wizard sets up a team project.
 *
 * It is expected that the act of setting up a team project will be
 * bootstrapped by a CGI script called "dash/teamStart.class", which
 * is included in the main dashboard distribution.
 */
public class TeamProjectSetupWizard extends TinyCGIBase implements
        TeamDataConstants {

    // The following four constants are copies of constants in the
    // bootstrap script.  They must match the values in the bootstrap
    // script, so they should not be edited.

    // query parameter used to control logic flow.
    private static final String PAGE = "page";
    // session variable name used to save the selected process ID.
    private static final String TEAM_PID = "setup//Process_ID";
    // session variable name used to save the selected process name.
    private static final String TEAM_PROC_NAME = "setup//Process_Name";
    // session variable name used to save the team project url.
    private static final String TEAM_URL = "setup//Team_URL";

    // In the case of an unexpected failure, the following URLs are
    // used to redirect control back to the bootstrap script.  The
    // URLs must properly point to the bootstrap script, and the query
    // parameters must match the values the bootstrap script is
    // looking for.
    private static final String BOOTSTRAP_URL = "../../dash/teamStart.class";
    private static final String WELCOME_URL = BOOTSTRAP_URL + "?page=welcome";
    private static final String PROCESS_URL = BOOTSTRAP_URL + "?page=showProc";
    private static final String TEAM_URL_URL = BOOTSTRAP_URL + "?page=showURL";


    // value indicating we should set up a team project
    private static final String TEAM_START_PAGE = "team";
    // Information for the team directory selection page
    private static final String TEAM_MASTER_PAGE = "teamMaster";
    private static final String TEAM_MASTER_URL = "teamMaster.shtm";
    // Information for the team directory selection page
    private static final String TEAM_DIR_PAGE = "teamDir";
    private static final String TEAM_DIR_URL = "teamDirectory.shtm";
    private static final String TEAM_SERVER_URL = "teamServerUrl.shtm";
    // Information for the team schedule name selection page.
    private static final String TEAM_SCHEDULE_PAGE = "teamSchedule";
    private static final String TEAM_SCHEDULE_URL = "teamSchedule.shtm";
    // Information for the team confirmation page.
    private static final String TEAM_CONFIRM_PAGE = "teamConfirm";
    private static final String TEAM_CONFIRM_URL = "teamConfirm.shtm";
    // Information for the page which asks the user to close the
    // hierarchy editor, when creating a team project.
    private static final String TEAM_CLOSE_HIERARCHY_PAGE = "teamCloseHier";
    private static final String TEAM_CLOSE_HIERARCHY_URL =
        "teamCloseHierarchy.shtm";
    // URL of a page that chastises the user for attempting to run the
    // setup wizard on an invalid project
    private static final String INVALID_URL = "invalid.shtm";
    // URL of the page that is displayed when the wizard successfully
    // creates and sets up a team project
    private static final String TEAM_SUCCESS_URL = "teamSuccess.shtm";

    // information for the team project relaunch welcome page
    private static final String RELAUNCH_WELCOME_PAGE = "relaunch";
    private static final String RELAUNCH_WELCOME_URL = "relaunchWelcome.shtm";
    private static final String RELAUNCH_START_PAGE = "relaunchStart";
    // information for the team project relaunch node name page
    private static final String RELAUNCH_NODE_PAGE = "relaunchNode";
    private static final String RELAUNCH_NODE_URL = "relaunchNode.shtm";
    private static final String RELAUNCH_NODE_SELECTED_PAGE = "relaunchNodeSelected";
    // URL of a page that chastises the user for attempting to run the
    // relaunch wizard on an invalid path
    private static final String RELAUNCH_INVALID_URL = "relaunchInvalid.shtm";
    // URL of a page asking the user to close the WBS Editor
    private static final String RELAUNCH_CANNOT_COPY_URL = "relaunchCannotCopy.shtm";

    // value indicating we should add an individual to a team project
    private static final String IND_PAGE = "indiv";
    private static final String IND_START_PAGE = "indivStart";
    private static final String IND_START_URL = "indivJoin.shtm";
    // Values for the page which asks the user for joining information
    private static final String IND_DATA_PAGE = "indivEnterData";
    private static final String IND_DATA_URL = "indivEnterData.shtm";
    // URL for the page which asks the user to close the hierarchy
    // editor, when joining a team project.
    private static final String IND_CLOSE_HIERARCHY_URL =
        "indivCloseHierarchy.shtm";
    // information for the page indicating that the user wishes to override
    // the location of the team directory
    private static final String IND_OVERRIDE_PAGE = "indivDirOverride";
    // id of the page requesting to retry the joining process
    private static final String IND_JOIN_PAGE = "indivTryJoin";
    // URL of the page that is displayed when the wizard successfully
    // joins the individual to the team project
    private static final String IND_SUCCESS_URL = "indivSuccess.shtm";
    private static final String IND_BG_SYNC_URL = "sync.class?run&bg&noExport";
    // URLs for pages alerting an individual to various errors that could
    // occur when attempting to join a team project.
    private static final String IND_DUPL_PROJ_URL = "indivDuplicateProj.shtm";
    private static final String IND_CONNECT_ERR_URL = "indivConnectError.shtm";
    private static final String IND_DATADIR_ERR_URL = "indivDataDirError.shtm";

    // Flag indicating an individual wants to join the team schedule.
    private static final String JOIN_TEAM_SCHED_PAGE = "joinSchedule";


    // Names of session variables used to store user selections.
    private static final String TEAM_MASTER_FLAG = "setup//Is_Master";
    private static final String RELAUNCH_FLAG = "setup//Is_Relaunch";
    private static final String RELAUNCH_SOURCE_ID = "setup//Relaunch_Source_ID";
    private static final String RELAUNCH_SOURCE_PATH = "setup//Relaunch_Source_Path";
    private static final String RELAUNCH_SOURCE_DATA = "setup//Relaunch_Source_Data";
    private static final String TEAM_TEMPLATE_FLAG = "setup//Is_Importing_Template";
    private static final String SUGG_TEAM_DIR = "setup//Suggested_Team_Dir";
    private static final String TEAM_DIR = "setup//Team_Dir";
    private static final String TEAM_SCHEDULE = "setup//Team_Schedule";
    private static final String NODE_NAME = "setup//Node_Name";
    private static final String NODE_LOCATION = "setup//Node_Location";
    private static final String IND_INITIALS = "setup//Indiv_Initials";
    private static final String IND_FULLNAME = "/Owner";
    private static final String IND_SCHEDULE = "setup//Indiv_Schedule";
    private static final String DATA_DIR = "setup//Data_Directory";
    private static final String DATA_DIR_URL = "setup//Data_Directory_URL";
    private static final String IND_DIR_OVERRIDE = "setup//Indiv_Team_Dir_Override";
    private static final String IGNORE_DUPS = "setup//Ignore_Duplicate_Projects";
    private static final String INITIALS_POLICY = "initialsPolicy";
    private static final String INITIALS_POLICY_USERNAME = "username";
    private static final String INITIALS_LABEL = "setup//Initials_Label";
    private static final String INITIALS_LABEL_LC = "setup//initials_label";
    private static final String CSS_CLASS_SUFFIX = "//Class";
    private static final String JOINING_DATA_MAP = "setup//Joining_Data";
    private static final String IN_PROGRESS_URI = "setup//In_Progress_URI";
    private static final String[] JOIN_SESSION_VARIABLES = { NODE_NAME,
            NODE_LOCATION, IND_SCHEDULE, DATA_DIR, DATA_DIR_URL, IND_DIR_OVERRIDE };

    // the template ID of a "team project stub"
    private static final String TEAM_STUB_ID = "TeamProjectStub";

    private static final Resources resources = Resources
            .getDashBundle("ProcessDashboard");

    private static final Logger logger =
        Logger.getLogger(TeamProjectSetupWizard.class.getName());

    protected void writeHeader() {}
    protected void writeContents() {}
    public void service(InputStream in, OutputStream out, Map env)
        throws IOException
    {
        try {
            serviceImpl(in, out, env);
        } catch (WizardError we) {
            printRedirect(we.uri);
        }

        // finish and clean up
        this.out.flush();
        networkDriveList = null;
    }

    private void serviceImpl(InputStream in, OutputStream out, Map env)
            throws IOException {

        super.service(in, out, env);
        if ("POST".equalsIgnoreCase((String) env.get("REQUEST_METHOD")))
            parseFormData();

        String page = getParameter(PAGE);
        if (IND_PAGE.equals(page))                 startIndivJoinFromBootstrap();
        else if (IND_START_PAGE.equals(page))      startIndivJoin();
        else if (IND_OVERRIDE_PAGE.equals(page))   handleIndivDirOverridePage();
        else if (IND_DATA_PAGE.equals(page))       handleIndivDataPage();
        else if (IND_JOIN_PAGE.equals(page))       handleIndivJoinPage();

        else if (JOIN_TEAM_SCHED_PAGE.equals(page))
                                                   handleJoinTeamSchedPage();

        else if (RELAUNCH_WELCOME_PAGE.equals(page)) showRelaunchWelcomePage();
        else if (RELAUNCH_START_PAGE.equals(page)) handleRelaunchWelcomePage();
        else if (RELAUNCH_NODE_PAGE.equals(page))  handleRelaunchNodePage();
        else if (RELAUNCH_NODE_SELECTED_PAGE.equals(page)) handleRelaunchNodeSelected();

        else if (TEAM_CLOSE_HIERARCHY_PAGE.equals(page)) handleTeamCloseHierPage();
        else if (!prefixNamesTeamProjectStub())    showInvalidPage();
        else if (page == null)                     showWelcomePage();
        else if (TEAM_START_PAGE.equals(page))     showTeamMasterChoicePage();
        else if (TEAM_MASTER_PAGE.equals(page))    handleTeamMasterChoicePage();
        else if (TEAM_DIR_PAGE.equals(page))       handleTeamDirPage();
        else if (TEAM_SCHEDULE_PAGE.equals(page))  handleTeamSchedulePage();
        else if (TEAM_CONFIRM_PAGE.equals(page))   handleTeamConfirmPage();
        else                                       showWelcomePage();
    }

    /** Send an HTTP redirect command to the browser, sending it to the
     *  relative URI named by filename. */
    protected void printRedirect(String filename) {
        out.print("Location: ");
        out.print(filename);
        out.print("\r\n\r\n");
    }

    /** Save a value into the data repository. */
    protected void putValue(String name, String value) {
        putValue(name, value == null ? null : new ImmutableStringData(value));
    }

    protected void putValue(String name, SimpleData dataValue) {
        DataRepository data = getDataRepository();
        String prefix = getPrefix();
        if (prefix == null) prefix = "";
        String dataName = DataRepository.createDataName(prefix, name);
        data.putValue(dataName, dataValue);
    }

    /** Get a value from the data repository as a String. */
    protected String getValue(String name) {
        SimpleData d = getSimpleValue(name);
        return (d == null ? null : d.format());
    }

    /** Get a value from the data repository and test it for trueness. */
    protected boolean testValue(String name) {
        SimpleData d = getSimpleValue(name);
        return (d == null ? false : d.test());
    }

    /** Get a value from the data repository. */
    protected SimpleData getSimpleValue(String name) {
        DataRepository data = getDataRepository();
        String prefix = getPrefix();
        if (prefix == null) prefix = "";
        String dataName = DataRepository.createDataName(prefix, name);
        SimpleData d = data.getSimpleValue(dataName);
        return d;
    }

    /** Returns true if the current prefix names a "TeamProjectStub" node */
    protected boolean prefixNamesTeamProjectStub() {
        return pathNamesTeamProjectStub(getPrefix());
    }

    private boolean pathNamesTeamProjectStub(String path) {
        return TEAM_STUB_ID.equals(getTemplateID(path));
    }

    private String getTemplateID(String path) {
        DashHierarchy hierarchy = getPSPProperties();
        PropertyKey key = hierarchy.findExistingKey(path);
        return hierarchy.getID(key);
    }

    /** Display the invalid page */
    protected void showInvalidPage() {
        printRedirect(INVALID_URL);
    }

    /** Display the welcome page */
    protected void showWelcomePage() {
        printRedirect(WELCOME_URL);
    }

    /** Display the team process selection page */
    protected void showTeamProcessesPage() {
        printRedirect(PROCESS_URL);
    }

    /** Display the team/master project selection page */
    protected void showTeamMasterChoicePage() {
        printRedirect(TEAM_MASTER_URL);
    }

    protected void handleTeamMasterChoicePage() {
        if (parameters.get("createMasterProject") != null) {
            putValue(TEAM_MASTER_FLAG, ImmutableDoubleData.TRUE);
            maybeShowTeamDirPage();
        } else if (parameters.get("createTeamProject") != null) {
            putValue(TEAM_MASTER_FLAG, ImmutableDoubleData.FALSE);
            maybeShowTeamDirPage();
        } else {
            showTeamMasterChoicePage();
        }
    }

    protected void maybeShowTeamDirPage() {
        String teamServerUrl = TeamServerSelector.getDefaultTeamServerUrl();
        if (teamServerUrl != null) {
            putValue(TEAM_DIR, teamServerUrl);
            showTeamSchedulePage();
        } else {
            showTeamDirPage();
        }
    }

    /** Display the team directory selection page */
    protected void showTeamDirPage() {
        // possibly record a suggested team directory.
        String suggestedTeamDir = guessTeamDirectory();
        if (suggestedTeamDir != null)
            putValue(SUGG_TEAM_DIR, suggestedTeamDir);

        // display the team directory page.
        printRedirect(TEAM_DIR_URL);
    }

    /** Make an educated guess for an appropriate team directory. */
    private String guessTeamDirectory() {
        // a recommended best practice is to use a single directory to hold the
        // team dashboard data and the team project data. If the team
        // dashboard data appears to be on a shared network drive, suggest
        // reusing it for the team data directory.
        try {
            // Locate the directory where this team dashboard stores its data.
            String settingsFilename = DashController.getSettingsFileName();
            if (!StringUtils.hasValue(settingsFilename))
                return null;
            File settingsFile = new File(settingsFilename).getCanonicalFile();
            File dataDir = settingsFile.getParentFile();
            if (dataDir == null)
                return null;

            // In bridged mode, the data directory will be located in a parent
            // dir called "working." If that appears to be the case, do not
            // suggest the reuse of this temporary bridged directory.
            File parent = dataDir.getParentFile();
            if (parent != null && "working".equalsIgnoreCase(parent.getName()))
                return null;

            // On Unix systems, we don't have a way of testing for network
            // filesystems.  Give the user the benefit of the doubt and assume
            // that the Team Dashboard directory is located on the network.
            String dataDirName = dataDir.getPath();
            if (!isWindows())
                return dataDirName;

            // If the path is already in UNC format, return it.
            if (dataDirName.startsWith("\\\\"))
                return dataDirName;

            // Try to convert the path to UNC format.  If successful, return
            // the UNC formatted path.
            String uncPath = getNetworkDriveList().toUNCName(dataDirName);
            if (uncPath != null && uncPath.startsWith("\\\\"))
                return uncPath;

        } catch (Exception e) {
        }

        // We did our best.  Return null to indicate failure.
        return null;
    }

    /** Try to locate the jarfile containing the definition for the
     * given process, and return the path to the file.
     */
    public static String findTeamProcessJarfile(String templateID) {
        int slashPos = templateID.indexOf('/');
        String processID = templateID.substring(0, slashPos);
        URL u = MCFManager.getInstance().getMcfSourceFileUrl(processID,
            TemplateLoader.MCF_PROCESS_XML);
        File f = RuntimeUtils.getFileForUrl(u);
        if (f == null || f.getName().endsWith(".xml"))
            return null;
        else
            return f.getPath();
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

    private boolean isWindows() {
        return (System.getProperty("os.name").indexOf("Windows") != -1);
    }

    private NetworkDriveList networkDriveList = null;
    private NetworkDriveList getNetworkDriveList() {
        if (networkDriveList == null)
            networkDriveList = new NetworkDriveList();
        return networkDriveList;
    }

    /** Handle values posted from the team directory selection page */
    protected void handleTeamDirPage() {
        String teamDir = getParameter("teamDir");
        if (teamDir == null || teamDir.trim().length() == 0) {
            printRedirect(TEAM_DIR_URL + "?missing");
            return;
        }
        teamDir = teamDir.trim();
        putValue(TEAM_DIR, teamDir);

        String confirm = getParameter("confirm");
        if ((confirm != null && confirm.equals(teamDir)) ||
            TeamServerSelector.isUrlFormat(teamDir) ||
            ensureNetworkDrive(teamDir))

            showTeamSchedulePage();

        else
            printRedirect(TEAM_DIR_URL + "?confirm");
    }

    /** Display the team schedule name page */
    protected void showTeamSchedulePage() {
        if (getValue(TEAM_SCHEDULE) == null)
            putValue(TEAM_SCHEDULE, suggestTeamScheduleName());

        printRedirect(TEAM_SCHEDULE_URL);
    }

    private String suggestTeamScheduleName() {
        String prefix = getPrefix();
        if (prefix == null || prefix.length() < 2)
            return null;
        int slashPos = prefix.lastIndexOf('/');
        if (slashPos == -1)
            return null;
        String projectName = prefix.substring(slashPos + 1);
        return (StringUtils.hasValue(projectName) ? projectName : null);
    }

    /** Handle values posted from the team schedule name page */
    protected void handleTeamSchedulePage() {
        String scheduleName = getParameter("scheduleName");
        if (scheduleName == null || scheduleName.trim().length() == 0) {
            printRedirect(TEAM_SCHEDULE_URL + "?missing");
            return;
        }

        scheduleName = scheduleName.trim();
        putValue(TEAM_SCHEDULE, scheduleName);

        if (!EVTaskListRollup.validName(scheduleName)) {
            printRedirect(TEAM_SCHEDULE_URL + "?invalid");
            return;
        }

        DataRepository data = getDataRepository();
        if (EVTaskListData.exists(data, scheduleName) ||
            EVTaskListRollup.exists(data, scheduleName))
            printRedirect(TEAM_SCHEDULE_URL + "?duplicate");

        else
            showTeamConfirmPage();

    }

    /** Ensure that all the required team data has been entered.  If
     *  any data is missing, redirect to that page and return false.
     *  Otherwise return true
     */
    protected boolean ensureTeamValues() {
        if (getValue(TEAM_PID) == null|| getValue(TEAM_PROC_NAME) == null)
            showTeamProcessesPage();

        else if (getValue(TEAM_MASTER_FLAG) == null)
            showTeamMasterChoicePage();

        else if (getValue(TEAM_DIR) == null)
            showTeamDirPage();

        else if (getValue(TEAM_SCHEDULE) == null)
            showTeamSchedulePage();

        else if (DashController.isHierarchyEditorOpen())
            printRedirect(TEAM_CLOSE_HIERARCHY_URL);

        else
            return true;

        return false;
    }

    /** respond to the Next button on the team close hierarchy page */
    protected void handleTeamCloseHierPage() {
        if (testValue(RELAUNCH_FLAG) && !prefixNamesTeamProjectStub())
            showRelaunchNodePage();
        else
            showTeamConfirmPage();
    }

    /** Display the team confirmation page */
    protected void showTeamConfirmPage() {
        if (ensureTeamValues())
            printRedirect(TEAM_CONFIRM_URL);
    }

    /** Once the user has confirmed their settings, perform the setup
     *  process.
     */
    protected void handleTeamConfirmPage() {
        // make sure all the required data is present - otherwise abort.
        if (!ensureTeamValues()) return;

        String teamPID = getValue(TEAM_PID);
        String teamDirectory = getValue(TEAM_DIR);
        String teamDataDir = null;
        String teamDataDirUrl = null;
        String teamSchedule = getValue(TEAM_SCHEDULE);
        String processJarFile = findTeamProcessJarfile(teamPID);
        String projectID;

        boolean isMaster = testValue(TEAM_MASTER_FLAG);
        if (isMaster)
            teamPID = StringUtils.findAndReplace(teamPID,
                    "/TeamRoot", "/MasterRoot");

        // create the required team directories / collections. This involves
        // file or network IO which could fail for various reasons, so we
        // attempt to get it out of the way first. Failures in the methods
        // below will throw exceptions that halt logic and redirect to an
        // error page.

        if (TeamServerSelector.isUrlFormat(teamDirectory)) {
            projectID = createServerCollection(teamDirectory);
            teamDataDirUrl = teamDirectory + "/" + projectID;
            teamDirectory = teamDataDir = null;

        } else {
            projectID = generateID();
            teamDataDir = createTeamDirs(teamDirectory, projectID);
        }

        // if we are relaunching a project, mark the original WBS as closed.
        File relaunchSourceDir = maybeCloseRelaunchedProjectWbs();
        String relaunchSourcePath = getValue(RELAUNCH_SOURCE_PATH);
        String relaunchSourceID = getValue(RELAUNCH_SOURCE_ID);

        // attempt to write applicable files to the new WBS directory.
        writeTeamSettingsFile(teamPID, teamDataDir, teamDataDirUrl,
            teamSchedule, projectID, processJarFile);
        writeFilesToNewWbsDir(relaunchSourceDir, relaunchSourceID,
            teamDataDirUrl, teamDataDir);

        // perform lots of other setup tasks.  Unlike the operation
        // above, these tasks should succeed 99.999% of the time.
        alterTeamTemplateID(teamPID);
        maybeSetProjectRootNodeId(projectID);
        String scheduleID = createTeamSchedule (teamSchedule);
        saveTeamDataValues(teamDirectory, teamDataDirUrl, projectID,
            teamSchedule, scheduleID, relaunchSourceID, relaunchSourcePath);
        saveTeamSettings (teamDirectory, teamDataDir, projectID);
        tryToCopyProcessJarfile (processJarFile, teamDirectory);
        exportProjectData();
        if (StringUtils.hasValue(relaunchSourcePath))
            startAsyncExport(relaunchSourcePath);
        if (StringUtils.hasValue(relaunchSourceID))
            startAsyncCleanupOfRelaunchedWbs();

        // print a success message!
        printRedirect(TEAM_SUCCESS_URL);
    }

    private String createServerCollection(String teamServerUrl) {
        try {
            URL url = new URL(teamServerUrl);
            String collectionId = ResourceBridgeClient.createNewCollection(url,
                ResourceCollectionType.TeamProjectData);
            return collectionId;
        } catch (Exception e) {
            throw new WizardError(TEAM_SERVER_URL).param("cannotContact");
        }
    }

    private String generateID() {
        return Long.toString(System.currentTimeMillis(), Character.MAX_RADIX);
    }

    protected String createTeamDirs(String teamDirectory, String projectID) {
        File teamDir = new File(teamDirectory);
        createTeamDirectory(teamDir);

        File templateDir = new File(teamDir, "Templates");
        createTeamDirectory(templateDir);

        File dataDir = new File(teamDir, "data");
        createTeamDirectory(dataDir);

        File projDataDir = new File(dataDir, projectID);
        createTeamDirectory(projDataDir);

        File disseminationDir = new File(projDataDir, DISSEMINATION_DIRECTORY);
        createTeamDirectory(disseminationDir);

        return projDataDir.getPath();
    }

    private void createTeamDirectory(File directory) {
        if (!directory.isDirectory() && !directory.mkdirs())
            throw new WizardError(TEAM_DIR_URL) //
                    .param("errCantCreateDir", directory.getPath()) //
                    .param(isWindows() ? "isWindows" : null);
    }

    private void writeTeamSettingsFile(String teamPID,
                                          String teamDataDir,
                                          String teamDataDirUrl,
                                          String teamSchedule,
                                          String projectID,
                                          String processJarFile)
    {
        TeamSettingsFile tsf = new TeamSettingsFile(teamDataDir, teamDataDirUrl);

        try {
            // write the project name
            tsf.setProjectHierarchyPath(getPrefix());
            tsf.setProjectID(projectID);
            tsf.setScheduleName(teamSchedule);

            // set the process ID.
            int pos = teamPID.indexOf('/');
            if (pos != -1) teamPID = teamPID.substring(0, pos);
            tsf.setProcessID(teamPID);

            // write the relative path to the process jar file, if we know it
            if (processJarFile != null) {
                File jarFile = new File(processJarFile);
                tsf.setTemplatePath("../../Templates/" + jarFile.getName());
            }

            tsf.write();

        } catch (IOException ioe) {
            System.out.println("Could not write team settings file:");
            ioe.printStackTrace();
            throw new WizardError(TEAM_DIR_URL).param("errSettingsFile",
                tsf.getSettingsFileDescription());
        }
    }

    private File maybeCloseRelaunchedProjectWbs() {
        // if we are relaunching a project, get the locations where that
        // project stores its data
        ListData relaunchSourceLocations = ListData
                .asListData(getSimpleValue(RELAUNCH_SOURCE_DATA));
        if (relaunchSourceLocations == null || !relaunchSourceLocations.test())
            return null;

        // update settings on the relaunched project WBS to mark it as closed
        CloseRelaunchedProjectWbs crpw = new CloseRelaunchedProjectWbs();
        List l = relaunchSourceLocations.asList();
        try {
            writeFilesToWbsDir(Collections.singleton(crpw),
                (String[]) l.toArray(new String[l.size()]));
        } catch (Exception e) {
            // This error could theoretically occur for a number of reasons;
            // but since we've already validated network connectivity and
            // file writability a few steps earlier, this error is most
            // likely an indication that the WBS Editor is currently open.
            // Display a page advising the user to close the WBS Editor.
            System.out.println("Unable to close WBS of relaunched project:");
            e.printStackTrace();
            throw new WizardError(RELAUNCH_CANNOT_COPY_URL);
        }

        // return the local directory where the WBS data is cached
        return crpw.dir;
    }

    private class CloseRelaunchedProjectWbs implements WriteFileTask {
        private File dir;
        public void write(File dir) throws IOException {
            closeOldProjectWbs(dir);
            this.dir = dir;
        }
    }

    /** write files to the WBS directory for the newly created project */
    private void writeFilesToNewWbsDir(File relaunchSourceDir,
            String relaunchSourceID, String... locations) {
        List<WriteFileTask> files = new ArrayList();

        // write the userSettings.ini file
        Properties wbsUserSettings = getWbsUserSettings(relaunchSourceID);
        if (!wbsUserSettings.isEmpty())
            files.add(new UserSettingsWriter(wbsUserSettings));

        // if this is a relaunched project, copy other files needed to relaunch
        if (relaunchSourceDir != null)
            files.add(new CopyRelaunchFiles(relaunchSourceDir));

        // perform the write operations
        try {
            writeFilesToWbsDir(files, locations);
        } catch (Exception e) {
            // This error is quite unexpected. By the time we reach this point,
            // other code has successfully connected to and written data into
            // the new WBS dir. We display the "cannot copy" page, which will
            // (a) give them a chance to retry the operation, and (b) show a
            // link they can click to view the stack trace we print below.
            System.out.println("Unable to write files to project WBS dir");
            e.printStackTrace();
            if (relaunchSourceDir != null)
                throw new WizardError(RELAUNCH_CANNOT_COPY_URL);

            // if we are not in relaunch mode, just log the error and keep
            // going. we were only unable to write the settings file, and the
            // team can manually alter those settings later.
        }
    }

    private Properties getWbsUserSettings(String relaunchSourceID) {
        Properties result = new Properties();

        // record the global initials policy, if one is registered
        String initialsPolicy = getValue("/Team_Project_Policy/Initials_Policy");
        if (StringUtils.hasValue(initialsPolicy))
            result.put("initialsPolicy", initialsPolicy);

        // if this is a relaunched project, record applicable settings
        if (StringUtils.hasValue(relaunchSourceID)) {
            result.put("relaunchProject", "true");
            result.put("relaunchSourceID", relaunchSourceID);
        }

        return result;
    }

    private class UserSettingsWriter implements WriteFileTask {
        private Properties userSettings;
        protected UserSettingsWriter(Properties userSettings) {
            this.userSettings = userSettings;
        }
        public void write(File newWbsDir) throws IOException {
            File f = new File(newWbsDir, "user-settings.ini");
            RobustFileOutputStream out = new RobustFileOutputStream(f);
            userSettings.store(out, null);
            out.close();
        }
    }

    private class CopyRelaunchFiles implements WriteFileTask {
        private File sourceDir;
        public CopyRelaunchFiles(File sourceDir) {
            this.sourceDir = sourceDir;
        }
        public void write(File destDir) throws IOException {
            copyRelaunchFiles(sourceDir, destDir);
        }
    }


    private interface WriteFileTask {
        public void write(File dir) throws IOException;
    }

    private void writeFilesToWbsDir(Collection<? extends WriteFileTask> files,
            String... wbsDirLocations) throws LockFailureException, IOException {
        if (files.isEmpty())
            return;
        WorkingDirectory dir = null;
        try {
            // obtain a working directory object for this WBS directory
            dir = WorkingDirectoryFactory.getInstance().get(
                WorkingDirectoryFactory.PURPOSE_WBS, wbsDirLocations);
            dir.acquireProcessLock("", null);
            dir.prepare();
            acquireWriteLock(dir);

            for (WriteFileTask task : files)
                task.write(dir.getDirectory());

            dir.flushData();

        } finally {
            if (dir != null)
                dir.releaseLocks();
        }
    }

    private void acquireWriteLock(WorkingDirectory dir)
            throws LockFailureException {
        // try several times to acquire a write lock. This should succeed
        // immediately in a new project directory, but could require a
        // retry in the directory for an existing project if someone is saving
        // changes to the WBS at this exact moment.
        AlreadyLockedException locked = null;
        for (int retryCount = 0; retryCount < 5; retryCount++) {
            try {
                dir.acquireWriteLock(null, "Team Project Setup Wizard");
                return;
            } catch (AlreadyLockedException e) {
                locked = e;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException i) {}
            }
        }
        throw locked;
    }


    private void startAsyncCleanupOfRelaunchedWbs() {
        try {
            // start a headless WBS Editor process in the background to
            // perform the cleanup operations on the new WBS
            String wbsUri = resolveRelativeURI("openWBS.shtm?dumpAndExit");
            getRequest(wbsUri, false);
        } catch (Exception e) {
            // if this fails, continue; the cleanup will be performed the
            // first time the WBS Editor is opened for this project.
            System.out.println("Couldn't run background WBS cleanup task");
            e.printStackTrace();
        }
    }


    protected boolean tryToCopyProcessJarfile(String jarFilename,
                                              String teamDirectory) {
        // no jar to copy? abort.
        if (jarFilename == null) return false;
        if (!StringUtils.hasValue(teamDirectory)) return false;

        // get the source file
        File srcFile = new File(jarFilename);
        String jarName = srcFile.getName();
        // get the destination file
        File templateDir = new File(teamDirectory, "Templates");
        File destFile = new File(templateDir, jarName);

        // if the destination file already exists, then there is
        // nothing to do (note that we're making the implicit
        // assumption that people would never give two different
        // processes the exact same name.  If they do, then they
        // deserve every bad thing that happens to them.
        if (destFile.exists()) return true;

        // copy the file.
        try {
            InputStream in = new FileInputStream(srcFile);
            OutputStream out = new FileOutputStream(destFile);

            byte [] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1)
                out.write(buffer, 0, bytesRead);
            in.close();
            out.close();
        } catch (IOException ioe) {
            return false;
        }
        return true;
    }

    protected void saveTeamDataValues(String teamDirectory,
            String teamDataDirUrl, String projectID, String teamScheduleName,
            String teamScheduleID, String relaunchSourceID,
            String relaunchSourcePath) {
        putValue(TEAM_DIRECTORY, teamDirectory);
        String uncName = calcUNCName(teamDirectory);
        putValue(TEAM_DIRECTORY_UNC, uncName);
        putValue(TEAM_DATA_DIRECTORY_URL, teamDataDirUrl);

        putValue(PROJECT_ID, projectID);
        putValue(PROJECT_SCHEDULE_NAME, teamScheduleName);
        putValue(PROJECT_SCHEDULE_ID, teamScheduleID);
        putValue(RELAUNCH_SOURCE_PROJECT_ID, relaunchSourceID);

        if (StringUtils.hasValue(relaunchSourcePath)) {
            putRelaunchValue(RELAUNCHED_PROJECT_FLAG, relaunchSourcePath,
                ImmutableDoubleData.TRUE);
            copyRelaunchableSettingsFrom(relaunchSourcePath);
        }

        putValue("_Password_", ImmutableDoubleData.TRUE);
    }

    private void copyRelaunchableSettingsFrom(String relaunchSourcePath) {
        ListData settingNames = ListData
                .asListData(getSimpleValue(RELAUNCHABLE_SETTINGS));
        if (settingNames == null || !settingNames.test())
            return;

        List<String> phases = ListData.asListData(getSimpleValue("Phase_List"))
                .asList();
        for (String oneName : (List<String>) settingNames.asList()) {
            if (oneName.contains("*PHASE*")) {
                for (String phase : phases)
                    copyRelaunchableSetting(relaunchSourcePath,
                        StringUtils.findAndReplace(oneName, "*PHASE*", phase));
            } else {
                copyRelaunchableSetting(relaunchSourcePath, oneName);
            }
        }
    }

    private void copyRelaunchableSetting(String sourcePath, String name) {
        String srcDataName = DataRepository.createDataName(sourcePath, name);
        SimpleData valueToCopy = getSimpleValue(srcDataName);
        SimpleData currentValue = getSimpleValue(name);
        if (valueToCopy != null && !valueToCopy.equals(currentValue))
            putValue(name, valueToCopy);
    }


    protected String calcUNCName(String filename) {
        if (!StringUtils.hasValue(filename)) return null;
        String result = getNetworkDriveList().toUNCName(filename);
        if (result == null) return null;

        File testFile = new File(result);
        if (testFile.isDirectory())
            return result;
        else
            return null;
    }




    protected void saveTeamSettings(String teamDirectory, String teamDataDir,
            String projectID) {
        // set up an import instruction to retrieve team data
        RepairImportInstruction.maybeRepairForTeam(getDataContext());

        // enable other configuration settings that are appropriate for
        // team use.
        DashController.enableTeamSettings();

        // possibly wire up a URL for the team directory.
        if (teamDataDir != null) {
            URL url = TeamServerSelector.getServerURL(new File(teamDataDir));
            if (url != null) {
                putValue(TEAM_DATA_DIRECTORY_URL, url.toString());
                RepairImportInstruction.maybeRepairForTeam(getDataContext());
            }
        }

        // initiate the template directory adding task.
        String templatePathSetting = Settings.getVal(
            "teamJoin.templateSearchPathPhilosophy");
        if (teamDirectory != null
                && "alwaysAdd".equalsIgnoreCase(templatePathSetting)) {
            putValue(TEAM_TEMPLATE_FLAG, "t");
            new TemplateDirAdder(teamDirectory);
        }

        // make this project the actively selected task
        DashController.setPath(getPrefix());
    }

    private final class TemplateDirAdder extends Thread {
        private String teamDirectory;
        public TemplateDirAdder(String teamDirectory) {
            this.teamDirectory = teamDirectory;
            this.start();
        }
        public void run() {
            // calculate the new template directory, and add it to the
            // template path
            String templateDir = teamDirectory + "/Templates";
            DashController.addTemplateDirToPath(templateDir, true);
        }
    }


    protected String createTeamSchedule(String scheduleName) {
        DataRepository data = getDataRepository();
        EVTaskListRollup rollup = new EVTaskListRollup
            (scheduleName, data,
             getPSPProperties(), getObjectCache(), false);
        rollup.save();

        // publish the newly created schedule, with no password required.
        String passwordDataName = "/ev /" + scheduleName + "/_Password_";
        data.putValue(passwordDataName, ImmutableDoubleData.TRUE);
        passwordDataName = "/ev /" + scheduleName + "/PW_STOR";
        data.putValue(passwordDataName, StringData.create(" none "));

        return rollup.getID();
    }

    protected void alterTeamTemplateID(String teamPID) {
        DashController.alterTemplateID(getPrefix(), TEAM_STUB_ID, teamPID);
    }

    private void maybeSetProjectRootNodeId(String projectID) {
        DashHierarchy hier = getPSPProperties();
        if (hier.hasNodeIDs()) {
            PropertyKey rootKey = hier.findExistingKey(getPrefix());
            hier.pget(rootKey).setNodeID(projectID + ":root");
        }
    }

    protected void handleJoinTeamSchedPage() {
        String teamScheduleName = getValue(PROJECT_SCHEDULE_NAME);
        String indivExportedName = getParameter("scheduleName");
        String indivScheduleID = getParameter("scheduleID");
        String indivFileName = getParameter("fileName");
        if (addIndivScheduleToTeamSchedule
            (teamScheduleName, indivExportedName,
             indivScheduleID, indivFileName)) {
            // print out a null document and call it good.
            out.print("Content-type: text/plain\r\n\r\n ");
        } else {
            out.print("Status: 500 Join Failed\r\n\r\n");
        }
    }

    /** Add an individual's personal schedule to the team rollup. */
    protected boolean addIndivScheduleToTeamSchedule(String teamScheduleName,
                                                     String indivExportedName,
                                                     String indivScheduleID,
                                                     String indivFileName)
    {
        if (teamScheduleName == null || teamScheduleName.length() == 0 ||
            indivExportedName == null || indivExportedName.length() == 0 ||
            indivFileName == null || indivFileName.length() == 0) return false;
        logger.fine("addIndivScheduleToTeamSchedule:"
                + "  teamScheduleName=" + teamScheduleName
                + "  indivExportedName=" + indivExportedName
                + "  indivFileName=" + indivFileName);

        String indivSchedPath = getSchedulePath
            (indivExportedName, indivScheduleID, indivFileName);
        if (indivSchedPath == null)
            indivSchedPath = getFallbackSchedulePath
                (indivExportedName, indivScheduleID);
        logger.fine("indivSchedPath="+indivSchedPath);
        if (indivSchedPath == null) return false;

        //*debug*/ System.out.println("indivSchedPath="+indivSchedPath);
        //*debug*/ SimpleData sd =
        //*debug*/     getDataRepository().getSimpleValue(indivSchedPath);
        //*debug*/ String sdStr = (sd == null ? null : sd.format());
        //*debug*/ System.out.println("indivSchedPath.value="+sdStr);

        // add the imported schedule to the team schedule.
        return addScheduleToRollup(teamScheduleName, indivSchedPath);
    }

    /** Calculate the fully qualified name of an imported XML schedule */
    protected String getSchedulePath(String indivExportedName,
                                     String indivScheduleID,
                                     String indivFileName) {
        // calculate the import prefix
        String projectID = getValue(PROJECT_ID);
        if (projectID == null || projectID.length() == 0) return null;
        String importPrefix = "/Import_"+projectID;
        logger.finer("importPrefix="+importPrefix);

        // import all data.
        DataImporter.refreshPrefix(importPrefix);
        logger.finer("refreshPrefix done");

        // calculate the name of the import directory.
        String teamDirectory = getValue(TEAM_DIRECTORY);
        if (teamDirectory == null || teamDirectory.length() == 0) return null;
        teamDirectory = teamDirectory.replace('\\', '/');
        String importDir = teamDirectory+"/data/"+projectID;
        logger.finer("importDir="+importDir);

        // calculate the full name of the imported file.
        indivFileName = indivFileName.replace('\\', '/');
        int slashPos = indivFileName.lastIndexOf('/');
        if (slashPos != -1)
            indivFileName = indivFileName.substring(slashPos+1);
        String importedFile = importDir + "/" + indivFileName;
        logger.finer("importedFile="+importedFile);
        File f = new File(importedFile);

        // calculate the fully qualified name of the imported schedule.
        String dataPrefix = null;
        try {
            dataPrefix = DataImporter.getPrefix(importPrefix, f);
        } catch (IOException ioe) {
            return null;
        }
        logger.finer("dataPrefix="+dataPrefix);
        String indivSchedPath = dataPrefix + indivExportedName;

        // prepend the task list ID if it is available.
        if (indivScheduleID != null && indivScheduleID.length() != 0)
            indivSchedPath = indivScheduleID + EVTaskListXML.XMLID_FLAG +
                indivSchedPath;

        return indivSchedPath;
    }

    /** In cases where a fully qualified name can not be calculated for an
     * imported XML schedule, calculate a fallback name that will work. */
    private String getFallbackSchedulePath(String indivExportedName,
                                           String indivScheduleID) {
        if (indivExportedName == null || indivExportedName.length() == 0 ||
            indivScheduleID == null || indivScheduleID.length() == 0)
            return null;

        return indivScheduleID + EVTaskListXML.XMLID_FLAG +
            "/ignored-fallback" + indivExportedName;
    }


    protected boolean addScheduleToRollup(String teamScheduleName,
                                          String indivSchedPath) {
        if (Settings.isReadOnly()) {
            logger.fine("Cannot add schedule while in read-only mode.");
            return false;
        }

        EVTaskList rollup = EVTaskList.openExisting
            (teamScheduleName, getDataRepository(),
             getPSPProperties(), getObjectCache(), false);
        if (!(rollup instanceof EVTaskListRollup)) {
            logger.fine("rollup not an EVTaskListRollup");
            return false;
        }
        if (!rollup.addTask(indivSchedPath, getDataRepository(),
                            getPSPProperties(), getObjectCache(), false)) {
            logger.fine("addTask failed");
            return false;
        }

        rollup.save();
        logger.fine("saved changed task list");
        return true;
    }



    /** Begin the process of relaunching a team project */
    private void showRelaunchWelcomePage() {
        printRedirect(RELAUNCH_WELCOME_URL);
    }

    private void handleRelaunchWelcomePage() {
        // don't proceed if we are running in the quick launcher.
        if (CompressedInstanceLauncher.isRunningFromCompressedData())
            throw new WizardError(RELAUNCH_INVALID_URL).param("quickLaunch");

        // don't proceed if we are running in read-only mode.
        if (Settings.isReadOnly())
            throw new WizardError(RELAUNCH_INVALID_URL).param("readOnly");

        // look at the current path. It it doesn't name a team project, abort.
        String projectPath = getPrefix();
        String templateID = getTemplateID(projectPath);
        if (templateID == null || !templateID.endsWith("/TeamRoot"))
            throw new WizardError(RELAUNCH_INVALID_URL).param("notTeamRoot");

        // if this team project has been relaunched in the past, abort.
        if (testValue(RELAUNCHED_PROJECT_FLAG))
            throw new WizardError(RELAUNCH_INVALID_URL).param("isRelaunched");

        // if the user does not have a high enough version of team tools, abort
        String toolsVersion = TemplateLoader.getPackageVersion("teamTools");
        if (toolsVersion == null
                || VersionUtils.compareVersions(toolsVersion, "4.1") < 0)
            throw new WizardError(RELAUNCH_INVALID_URL).param("badTeamTools");

        // break apart the name and infer parent/project name
        int slashPos = projectPath.lastIndexOf('/');
        String projectParent = (slashPos < 2 ? "/" : projectPath.substring(0,
            slashPos));
        String oldProjectName = projectPath.substring(slashPos + 1);
        String newProjectName = getDefaultRelaunchedProjectName(oldProjectName);
        putValue(NODE_NAME, newProjectName);
        putValue(NODE_LOCATION, projectParent);
        putValue(RELAUNCH_FLAG, "true");

        showRelaunchNodePage();
    }

    private static String getDefaultRelaunchedProjectName(String oldName) {
        // look for an iteration string in the old name. If present, increment
        // the iteration number.
        for (Pattern p : ITER_PATTERNS) {
            Matcher m = p.matcher(oldName);
            if (m.find()) {
                int oldIterNum = Integer.parseInt(m.group(1));
                int newIterNum = oldIterNum + 1;
                return oldName.substring(0, m.start(1)) + newIterNum
                        + oldName.substring(m.end(1));
            }
        }

        // if no iteration number was found, use a standard pattern to produce
        // iteration 2 of the project
        return oldName + " Iter 2";
    }

    private static final Pattern[] ITER_PATTERNS = {
            Pattern.compile("\\bIter\\s*(\\d+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bI(\\d+)\\W*$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(\\d+)\\W*$", Pattern.CASE_INSENSITIVE),
    };

    // display the page asking for project name and location
    private void showRelaunchNodePage() {
        printRedirect(RELAUNCH_NODE_URL);
    }

    private void handleRelaunchNodePage() {
        putValue(NODE_NAME, trimParameter("Node_Name"));
        putValue(NODE_LOCATION, trimParameter("Node_Location"));

        String projectPath = getTargetNodePath();
        ensureTeamProjectStub(projectPath);
        copyRelaunchValues(projectPath);

        // now display the next page in the relaunch sequence
        String redirectUri = WebServer.urlEncodePath(projectPath) + "//"
                + getValue("Team_Process_PID") + "/setup/wizard.class?page="
                + RELAUNCH_NODE_SELECTED_PAGE;
        putRelaunchValue(IN_PROGRESS_URI, projectPath, redirectUri);
        printRedirect(redirectUri);
    }

    private String getTargetNodePath() {
        WizardError err = new WizardError(RELAUNCH_NODE_URL);
        err.param(getNodeNameError());
        err.param(getNodeLocationError());
        if (!RELAUNCH_NODE_URL.equals(err.uri))
            throw err;

        String nodeName = getValue(NODE_NAME);
        String nodeLocation = getValue(NODE_LOCATION);
        String projectPath;
        if (nodeLocation.endsWith("/"))
            projectPath = nodeLocation + nodeName;
        else
            projectPath = nodeLocation + "/" + nodeName;
        return projectPath;
    }

    private void ensureTeamProjectStub(String path) {
        if (pathNamesTeamProjectStub(path))
            return;
        else if (DashController.alterTemplateID(path, null, TEAM_STUB_ID))
            return;
        else
            throw new WizardError(TEAM_CLOSE_HIERARCHY_URL);
    }

    private void copyRelaunchValues(String path) {
        // record the fact that we are relaunching a team project
        putRelaunchValue(RELAUNCH_FLAG, path, "true");
        putRelaunchValue(TEAM_MASTER_FLAG, path, ImmutableDoubleData.FALSE);
        // record the template ID of the process we will use
        putRelaunchValue(TEAM_PID, path, getTemplateID(getPrefix()));
        putRelaunchValue(TEAM_PROC_NAME, path, "Inherited Process");
        // record the default location for team data
        copyRelaunchValue(TEAM_DIR, path, TeamDataConstants.TEAM_DIRECTORY);
        // record the ID and location of the project we are relaunching
        copyRelaunchValue(RELAUNCH_SOURCE_ID, path, TeamDataConstants.PROJECT_ID);
        putRelaunchValue(RELAUNCH_SOURCE_PATH, path, getPrefix());
        // gather the locations of relaunched source data and record those
        ListData sourceData = new ListData();
        sourceData.add(getValue(TeamDataConstants.TEAM_DATA_DIRECTORY_URL));
        sourceData.add(getValue(TeamDataConstants.TEAM_DATA_DIRECTORY));
        putRelaunchValue(RELAUNCH_SOURCE_DATA, path, sourceData);
    }

    private void copyRelaunchValue(String destName, String destPath,
            String srcName) {
        putRelaunchValue(destName, destPath, getValue(srcName));
    }

    private void putRelaunchValue(String destName, String destPath, Object value) {
        String dataName = DataRepository.createDataName(destPath, destName);
        if (value instanceof SimpleData) {
            putValue(dataName, (SimpleData) value);
        } else {
            putValue(dataName, (String) value);
        }
    }

    private void handleRelaunchNodeSelected() {
        maybeShowTeamDirPage();
    }

    private void closeOldProjectWbs(File oldProjectWbsDir) throws IOException {
        // read the current settings from the user settings file
        File f = new File(oldProjectWbsDir, "user-settings.ini");
        Properties p = new Properties();
        if (f.isFile()) {
            InputStream in = new FileInputStream(f);
            p.load(in);
            in.close();
        }

        // add a "project closed" setting, and resave
        p.put("projectClosed", "true");
        RobustFileOutputStream out = new RobustFileOutputStream(f);
        p.store(out, null);
        out.close();

        // add the "projectClosed" attr to the root project tag of projDump.xml
        f = new File(oldProjectWbsDir, "projDump.xml");
        if (f.isFile()) {
            InputStream in = new BufferedInputStream(new FileInputStream(f));
            out = new RobustFileOutputStream(f);
            // copy bytes up through the opening of the initial "project" tag
            copyBytesThrough(in, out, "<project", -1);
            // copy bytes until we see a projectClosed attribute, or encounter
            // the ">" char. If the latter, write the projectClosed attribute.
            if (copyBytesThrough(in, out, "projectClosed=", '>') == false)
                out.write(" projectClosed='true'>".getBytes("utf-8"));
            // copy the rest of the file verbatim
            FileUtils.copyFile(in, out);
            in.close();
            out.close();
        }
    }

    private void copyRelaunchFiles(File srcDir, File destDir)
            throws IOException {
        copyRelaunchFiles(srcDir, destDir, "wbs.xml", "team.xml", "team2.xml",
            "workflow.xml", "proxies.xml", "milestones.xml", "tabs.xml");
        writeMergedUserDump(srcDir, destDir);
    }

    private void copyRelaunchFiles(File srcDir, File destDir, String... names)
            throws IOException {
        for (String name : names) {
            File srcFile = new File(srcDir, name);
            File destFile = new File(destDir, name);
            if (srcFile.isFile())
                FileUtils.copyFile(srcFile, destFile);
        }
    }

    private void writeMergedUserDump(File srcDir, File destDir)
            throws IOException {
        File destFile = new File(destDir, "relaunchDump.xml");
        OutputStream out = new BufferedOutputStream(new RobustFileOutputStream(
                destFile));
        out.write(MERGED_DUMP_HEADER.getBytes("utf-8"));

        for (File f : srcDir.listFiles()) {
            if (f.getName().toLowerCase().endsWith("-data.pdash"))
                copyUserDumpDataFromPdash(f, out);
        }

        out.write(MERGED_DUMP_FOOTER.getBytes("utf-8"));
        out.close();
    }

    private void copyUserDumpDataFromPdash(File f, OutputStream out) {
        try {
            // open the userDump.xml file from the PDASH
            ZipFile zip = new ZipFile(f);
            ZipEntry entry = zip.getEntry("userDump.xml");
            if (entry == null)
                return;
            InputStream in = zip.getInputStream(entry);

            // skip past the XML prolog, then copy the remainder to the output
            copyBytesThrough(in, null, "?>", -1);
            FileUtils.copyFile(in, out);
            in.close();

        } catch (IOException ioe) {
            // if a single PDASH file is corrupt and unreadable as a ZIP file,
            // skip it and process others
            System.out.println("Could not read userDump from " + f);
            ioe.printStackTrace();
        }
    }

    private static final String MERGED_DUMP_HEADER = //
            "<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>\r\n"
            + "<relaunchData>\r\n";

    private static final String MERGED_DUMP_FOOTER = "\r\n</relaunchData>";

    private boolean copyBytesThrough(InputStream in, OutputStream out,
            String utf8Pat, int stopByte) throws IOException {
        return copyBytesThrough(in, out, utf8Pat.getBytes("utf-8"), stopByte);
    }

    private boolean copyBytesThrough(InputStream in, OutputStream out,
            byte[] pat, int stopByte) throws IOException {
        for (int i = 1; i < pat.length; i++) {
            if (pat[i] == pat[0])
                throw new IllegalArgumentException(
                        "repeating patterns not supported");
            else if (pat[i] == stopByte)
                throw new IllegalArgumentException("pattern contains stop byte");
        }

        int b, numBytesMatched = 0;
        while ((b = in.read()) != -1) {
            if (b == stopByte)
                return false;
            if (out != null)
                out.write(b);
            if (b != pat[numBytesMatched])
                numBytesMatched = 0;
            if (b == pat[numBytesMatched])
                numBytesMatched++;
            if (numBytesMatched == pat.length)
                return true;
        }
        return false;
    }



    /** Begin the process of helping an individual join a team process. */
    private void startIndivJoinFromBootstrap() {
        // clear any session variables that were saved during a previous
        // joining operation
        for (String var : JOIN_SESSION_VARIABLES)
            putValue(var, (SimpleData) null);

        // direct the user to a page that will frame the rest of the wizard
        // joining operation
        printRedirect(IND_START_URL);
    }

    private void startIndivJoin() {
        // get information needed by the joining process
        Map<String, String> joinInfo = getTeamProjectJoinInformation();

        // make sure the user hasn't joined this project before.
        checkForDuplicateProject(joinInfo);

        // ensure we can locate the team data directory
        URL teamDirUrl = resolveTeamDataDirectory();

        // retrieve additional information about the project
        retrieveInfoFromTeamProjectDir(joinInfo, teamDirUrl);

        // set default values for the user
        saveDefaultJoiningValues(joinInfo);
        checkValidityOfIndivDataValues(false);

        // show a page asking the user to enter the data
        printRedirect(IND_DATA_URL);
    }

    private Map<String, String> getTeamProjectJoinInformation() {
        // Get the URL of the team project we are joining
        String teamURL = getValue(TEAM_URL);
        if (teamURL == null)
            throw new WizardError(TEAM_URL_URL);

        // see if we already have a map of data relating to this join operation
        SimpleData sd = getSimpleValue(JOINING_DATA_MAP);
        if (sd instanceof ListData) {
            try {
                Map result = (Map) ((ListData) sd).get(0);
                String mapUrl = (String) result.get(TEAM_PROJECT_URL);
                if (mapUrl == null)
                    result.put(TEAM_PROJECT_URL, mapUrl = teamURL);
                if (mapUrl.equals(teamURL))
                    return result;
            } catch (Exception e) {};
        }

        // download an XML document containing joining information
        Document xml = downloadTeamProjectInformation(teamURL);
        if (xml == null)
            throw new WizardError(IND_CONNECT_ERR_URL);

        // cache this information in the data repository
        Map result = XMLUtils.getAttributesAsMap(xml.getDocumentElement());
        result.put(TEAM_PROJECT_URL, teamURL);
        ListData l = new ListData();
        l.add(result);
        putValue(JOINING_DATA_MAP, l);
        return result;
    }

    private void checkForDuplicateProject(Map<String, String> joinInfo) {
        // extract the project ID from the joining information.
        String projectID = joinInfo.get(PROJECT_ID);
        if (!StringUtils.hasValue(projectID))
            return;

        // Look for projects in the current hierarchy that have that ID.
        Map<String, String> matchingProjects = new HashMap();
        scanForMatchingProjects(getPSPProperties(), PropertyKey.ROOT,
            projectID, matchingProjects);

        // Look through the projects that were found.
        String personalRoot = null;
        for (Map.Entry<String, String> e : matchingProjects.entrySet()) {
            if (e.getValue().endsWith("/TeamRoot")) {
                // if a team project root was found, abort immediately.
                // we don't support that mode of operation anymore.
                throw new WizardError(IND_DUPL_PROJ_URL).param("teamRootPresent");
            } else {
                // if a personal root was found, make a record of its path.
                personalRoot = e.getKey();
            }
        }

        // if the user wants us to ignore duplicate personal projects, oblige.
        if (parameters.containsKey("ignoreDups")) {
            putValue(IGNORE_DUPS, projectID);
            return;
        } else if (projectID.equals(getValue(IGNORE_DUPS))) {
            return;
        }

        // otherwise, if a personal root is present, show a warning page.
        if (personalRoot != null) {
            throw new WizardError(IND_DUPL_PROJ_URL)
                    .param("personalRoot", personalRoot);
        }

        // everything seems fine. No duplicates found.
    }
    private void scanForMatchingProjects(DashHierarchy hier, PropertyKey node,
            String projectID, Map<String, String> matches) {
        String path = node.path();
        String dataName = DataRepository.createDataName(path, PROJECT_ID);
        SimpleData sd = getDataRepository().getSimpleValue(dataName);
        if (sd != null && projectID.equals(sd.format())) {
            matches.put(path, hier.getID(node));
        } else {
            for (int i = hier.getNumChildren(node); i-- > 0;)
                scanForMatchingProjects(hier, hier.getChildKey(node, i),
                    projectID, matches);
        }
    }

    private void retrieveInfoFromTeamProjectDir(Map<String, String> joinInfo,
            URL teamDirUrl) {
        Properties userSettings = retrieveWbsUserSettings(teamDirUrl);

        // check to see if the team has a special policy for initials
        String initialsPolicy = (String) userSettings.get(INITIALS_POLICY);
        joinInfo.put(INITIALS_POLICY, initialsPolicy);
        String initialsLabel = "Initials";
        if (INITIALS_POLICY_USERNAME.equals(initialsPolicy))
            initialsLabel = "Username";
        putValue(INITIALS_LABEL, initialsLabel);
        putValue(INITIALS_LABEL_LC, initialsLabel.toLowerCase());
    }

    private Properties retrieveWbsUserSettings(URL teamDirUrl) {
        Properties result = new Properties();
        try {
            URL userSettingsUrl = new URL(teamDirUrl, "user-settings.ini");
            InputStream in = userSettingsUrl.openStream();
            result.load(in);
            in.close();
        } catch (Exception e) {}
        return result;
    }

    /** Look at information about the team project, and use it to set up
     * default values for some of the data we expect the individual to
     * provide. */
    private void saveDefaultJoiningValues(Map<String, String> joinInfo) {
        if (!prefixNamesTeamProjectStub()) {
            saveDefaultProjectName(joinInfo);
            saveDefaultNodeLocation();
        }
        saveDefaultScheduleName(joinInfo);
        saveDefaultUserName(joinInfo);
    }

    private void saveDefaultProjectName(Map<String, String> joinInfo) {
        // retrieve the project name from the joining info
        String projectName = joinInfo.get("Project_Full_Name");

        // if the joining info didn't contain a name, try to infer one from
        // the team project URL
        if (!StringUtils.hasValue(projectName)) {
            String teamURL = getValue(TEAM_URL);
            int end = teamURL.lastIndexOf("//");
            if (end == -1)
                end = teamURL.lastIndexOf("/+/");
            if (end != -1)
                projectName = HTMLUtils.urlDecode(teamURL.substring(0, end));
        }

        // use this information to set the default project name
        if (StringUtils.hasValue(projectName)) {
            int slashPos = projectName.lastIndexOf('/');
            if (slashPos != -1)
                projectName = projectName.substring(slashPos + 1);
            if (StringUtils.hasValue(projectName)
                    && getValue(NODE_NAME) == null)
                putValue(NODE_NAME, projectName);
        }
    }

    private void saveDefaultNodeLocation() {
        if (getValue(NODE_LOCATION) != null)
            return;

        // use the common/default node location for projects. Try the localized
        // name first, then fall back to English
        String projBranch = findExistingHierarchyNode(
            resources.getString("Project"), "Project");
        putValue(NODE_LOCATION, projBranch);
    }

    private String findExistingHierarchyNode(String... names) {
        for (String name : names) {
            String path = "/" + name;
            if (getPSPProperties().findExistingKey(path) != null)
                return path;
        }
        return null;
    }

    private void saveDefaultScheduleName(Map<String, String> joinInfo) {
        String scheduleName = joinInfo.get("Schedule_Name");
        if (!StringUtils.hasValue(scheduleName))
            scheduleName = getValue(NODE_NAME);
        if (StringUtils.hasValue(scheduleName)) {
            if (getValue(IND_SCHEDULE) == null)
                putValue(IND_SCHEDULE, scheduleName);
        }
    }

    private void saveDefaultUserName(Map<String, String> joinInfo) {
        String ownerInitials = null;
        if (INITIALS_POLICY_USERNAME.equals(joinInfo.get(INITIALS_POLICY)))
            ownerInitials = System.getProperty(
                TeamDataConstants.DATASET_OWNER_USERNAME_SYSPROP);
        String previousInitials = getInitialsFromPreviousProject(joinInfo);

        maybeSavePersonalValue(IND_INITIALS, Collections.EMPTY_SET,
            joinInfo.get("Suggested_Team_Member_Initials"), ownerInitials,
            previousInitials);
        maybeSavePersonalValue(IND_FULLNAME, getPlaceholderFullNames(),
            joinInfo.get("Suggested_Team_Member_Name"),
            System.getProperty(DATASET_OWNER_FULLNAME_SYSPROP));
    }

    private String getInitialsFromPreviousProject(Map<String, String> joinInfo) {
        // See if this project was relaunched from a previous project.
        String previousProjectID = joinInfo.get("Relaunch_Source_Project_ID");
        if (!StringUtils.hasValue(previousProjectID))
            return null;

        // See if this individual has a project in their hierarchy from joining
        // that previous project
        Map<String, String> matchingProjects = new HashMap();
        scanForMatchingProjects(getPSPProperties(), PropertyKey.ROOT,
            previousProjectID, matchingProjects);
        for (String path : matchingProjects.keySet()) {
            // if we find a matching project, read and return the initials
            String dataName = DataRepository.createDataName(path,
                TeamDataConstants.INDIV_INITIALS);
            SimpleData sd = getDataContext().getSimpleValue(dataName);
            if (sd != null && sd.test())
                return sd.format();
        }

        // no previous projects found.
        return null;
    }

    private void maybeSavePersonalValue(String dataName,
            Set overwritableValues, String... values) {
        String value = getFirstValidPersonalValue(values);
        if (value != null) {
            String currVal = getValue(dataName);
            if (currVal == null || currVal.length() == 0
                    || overwritableValues.contains(currVal))
                putValue(dataName, value);
        }
    }
    private String getFirstValidPersonalValue(String... values) {
        for (String value : values)
            if (StringUtils.hasValue(value)
                    && !(value.startsWith("[") && value.endsWith("]")))
                return value;
        return null;
    }

    /**
     * Handle values posted from the indiv join data entry page
     */
    private void handleIndivDataPage() {
        saveIndivDataValues();
        performProjectJoin();
    }

    private void saveIndivDataValues() {
        putValue(NODE_NAME, trimParameter("Node_Name"));
        putValue(NODE_LOCATION, trimParameter("Node_Location"));
        putValue(IND_INITIALS, trimParameter("Initials"));
        putValue(IND_FULLNAME, trimParameter("Full_Name"));
        putValue(IND_SCHEDULE, trimParameter("Schedule_Name"));
    }

    private void checkValidityOfIndivDataValues(boolean abortOnError) {
        WizardError err = new WizardError(IND_DATA_URL);

        if (!prefixNamesTeamProjectStub()) {
            checkIndivValue(err, NODE_NAME, getNodeNameError());
            checkIndivValue(err, NODE_LOCATION, getNodeLocationError());
        }
        checkIndivValue(err, IND_INITIALS, getPersonInitialsError());
        checkIndivValue(err, IND_FULLNAME, getPersonNameError());
        checkIndivValue(err, IND_SCHEDULE, getScheduleNameErr());

        if (abortOnError && !IND_DATA_URL.equals(err.uri))
            throw err;
    }

    private void checkIndivValue(WizardError we, String dataName, String error) {
        // store a CSS class for this input field based on whether we have
        // a non-null error token
        String cssClassName = (StringUtils.hasValue(error) ? "edit" : "flat");
        putValue(dataName + CSS_CLASS_SUFFIX, cssClassName);

        // also record the error token in the WizardError object.
        we.param(error);
    }

    private String getNodeNameError() {
        String nodeName = getValue(NODE_NAME);
        String nodeLocation = getValue(NODE_LOCATION);

        if (nodeName == null)
            return "nodeNameMissing";
        else if (nodeName.indexOf('/') != -1)
            return "nodeNameSlash";

        if (nodeLocation != null) {
            // check for unique name.
            String fullName = null;
            if (nodeLocation.endsWith("/"))
                fullName = nodeLocation + nodeName;
            else
                fullName = nodeLocation + "/" + nodeName;

            if (getPSPProperties().findExistingKey(fullName) != null
                    && !pathNamesTeamProjectStub(fullName))
                return "nodeNameDuplicateProject";
        }

        return null;
    }

    private String getNodeLocationError() {
        String nodeLocation = getValue(NODE_LOCATION);

        if (nodeLocation == null)
            return "nodeLocationMissing";

        DashHierarchy hierarchy = getPSPProperties();
        PropertyKey key = hierarchy.findExistingKey(nodeLocation);
        if (key == null)
            return "nodeLocationNotFound";

        while (key != null) {
            String templateID = hierarchy.getID(key);
            if (templateID != null && templateID.length() > 0)
                return "nodeLocationBadParent";
            key = key.getParent();
        }

        return null;
    }

    private String getPersonInitialsError() {
        String initials = getValue(IND_INITIALS);

        if (initials == null) {
            return "initialsMissing";
        } else {
            for (int i = initials.length();   i-- > 0; ) {
                char c = initials.charAt(i);
                if (c >= 'A' && c <= 'Z') continue;
                if (c >= 'a' && c <= 'z') continue;
                return "initialsNonAlpha";
            }
        }

        return null;
    }

    private String getPersonNameError() {
        String fullname = getValue(IND_FULLNAME);

        if (fullname == null || getPlaceholderFullNames().contains(fullname))
            return "fullNameMissing";

        return null;
    }

    private Set<String> getPlaceholderFullNames() {
        return new HashSet<String>(Arrays.asList("Enter your name",
            resources.getString("Enter_your_name")));
    }

    private String getScheduleNameErr() {
        String scheduleName = getValue(IND_SCHEDULE);

        if (scheduleName == null)
            return "scheduleNameMissing";

        if (!EVTaskListData.validName(scheduleName))
            return "scheduleNameInvalid";

        DataRepository data = getDataRepository();
        if (EVTaskListData.exists(data, scheduleName) ||
            EVTaskListRollup.exists(data, scheduleName)) {
            // if a task list already exists with this name, check to see if
            // it is empty (i.e., contains no tasks or subschedules).  If it
            // is empty, then it's OK to delete and replace it.  If not, we
            // should display an error message indicating that this schedule
            // name is already taken.
            EVTaskList tl = EVTaskList.openExisting(scheduleName, data,
                getPSPProperties(), getObjectCache(), false);
            if (tl != null && !tl.getTaskRoot().isLeaf()) {
                return "scheduleNameDuplicate";
            }
        }

        return null;
    }

    private String trimParameter(String name) {
        String result = getParameter(name);
        if (result != null)
            result = result.trim();
        return (StringUtils.hasValue(result) ? result : null);
    }


    private URL buildTeamURLReference(String teamURL, String ref) {
        return buildTeamURLReference(teamURL, ref, ref);
    }
    private URL buildTeamURLReference(String teamURL, String ref, String doRef) {
        //System.out.println("buildTeamURLReference("+teamURL+","+ref+")");
        if (teamURL == null || teamURL.trim().length() == 0) return null;

        teamURL = teamURL.trim();
        if (!teamURL.startsWith("http://") && !teamURL.startsWith("https://"))
            return null;

        if (teamURL.endsWith(".do") || teamURL.indexOf(".do?") != -1) {
            int pos = teamURL.lastIndexOf('/');
            teamURL = teamURL.substring(0, pos+1) + doRef;
        } else {
            teamURL = StringUtils.findAndReplace(teamURL, "/+/", "//");
            int pos = teamURL.indexOf("//", 7);
            if (pos != -1) pos = teamURL.indexOf('/', pos+2);
            if (pos == -1) return null;
            teamURL = teamURL.substring(0, pos+1) + ref;
        }
        //System.out.println("teamURL="+teamURL);
        URL u = null;
        try {
            u = new URL(teamURL);
            //System.out.println("teamURL(urlform)="+u);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return null;
        }
        return u;
    }

    /** Handle data from the page that told the user that the team data
     * directory could not be found.  (They may have entered a manual
     * override value.)
     */
    protected void handleIndivDirOverridePage() {
        String indivDirOverride = getParameter("indivTeamDirOverride");

        if (StringUtils.hasValue(indivDirOverride)) {
            indivDirOverride = indivDirOverride.trim();
            // check to see if the user has given us a value all the way
            // down in the alphanumeric subdirectory itself.  If so, strip
            // off that information to leave the team data directory only.
            Matcher m = DATA_SUBDIR_PATTERN.matcher(indivDirOverride);
            if (m.find())
                indivDirOverride = indivDirOverride.substring(0, m.start());
        }

        // save the value into the session, then attempt to retry the
        // joining process.
        putValue(IND_DIR_OVERRIDE, indivDirOverride);
        startIndivJoin();
    }
    private static final Pattern DATA_SUBDIR_PATTERN = Pattern.compile(
        "(/|\\\\)data((/|\\\\)[a-z0-9]{8,9})$", Pattern.CASE_INSENSITIVE);


    private void handleIndivJoinPage() {
        performProjectJoin();
    }

    /**
     * Perform the join process.
     */
    private void performProjectJoin() {
        // make sure all the required data is present - otherwise abort.
        Map<String, String> joinInfo = getTeamProjectJoinInformation();
        resolveTeamDataDirectory();
        checkValidityOfIndivDataValues(true);

        if (DashController.isHierarchyEditorOpen())
            throw new WizardError(IND_CLOSE_HIERARCHY_URL);

        String localProjectName = null;
        if (prefixNamesTeamProjectStub())
            localProjectName = getPrefix();
        else {
            String location = getValue(NODE_LOCATION);
            if (location.endsWith("/"))
                localProjectName = location+getValue(NODE_NAME);
            else
                localProjectName = location+"/"+getValue(NODE_NAME);
        }
        String indivInitials = getValue(IND_INITIALS);
        String scheduleName = getValue(IND_SCHEDULE);

        String teamURL = joinInfo.get(TEAM_PROJECT_URL);
        String projectID = joinInfo.get(PROJECT_ID);
        String teamDirectory = joinInfo.get(TEAM_DIRECTORY);
        String teamDirectoryUNC = joinInfo.get(TEAM_DIRECTORY_UNC);
        String teamDataDirectoryURL = joinInfo.get(TEAM_DATA_DIRECTORY_URL);
        String indivTemplateID = joinInfo.get("Template_ID");
        boolean teamDashSupportsScheduleMessages = "true".equals(joinInfo
                .get("Schedule_Messages_Supported"));

        if (!XMLUtils.hasValue(projectID) ||
            (!XMLUtils.hasValue(teamDirectory)
                    && !XMLUtils.hasValue(teamDataDirectoryURL)) ||
            !XMLUtils.hasValue(indivTemplateID)) {
            throw new WizardError(IND_CONNECT_ERR_URL);
        }

        if (teamDirectory != null && teamDirectory.endsWith(File.separator))
            // remove any trailing file separator if present.
            teamDirectory = teamDirectory.substring(0, teamDirectory.length()-1);

        // perform lots of other setup tasks.  Unlike the operation
        // above, these tasks should succeed 99.999% of the time.
        setPrefix(localProjectName);
        createIndivProject(indivTemplateID);
        String scheduleID = createIndivSchedule(scheduleName);
        saveIndivDataValues(projectID, teamURL, indivInitials, scheduleName,
            scheduleID, teamDirectory, teamDirectoryUNC, teamDataDirectoryURL);
        maybeSetProjectRootNodeId(projectID);
        boolean joinSucceeded = teamDashSupportsScheduleMessages
                || joinTeamSchedule(teamURL, scheduleName, scheduleID);
        importDisseminatedTeamData();
        DashController.setPath(localProjectName);

        showIndivSuccessPage(joinSucceeded);
    }

    /** Contacts the team dashboard and downloads information about
     * the project. On failure, this will return null.
     */
    protected Document downloadTeamProjectInformation(String teamURL) {
        URL u = buildTeamURLReference(teamURL, "setup/join.class?xml",
            "joinXml.do");
        if (u == null) return null;
        Document result = null;
        try {
            URLConnection conn = u.openConnection();
            conn.setUseCaches(false);
            conn.connect();
            String serverTimeStamp = conn.getHeaderField
                (WebServer.TIMESTAMP_HEADER);
            result = XMLUtils.parse(conn.getInputStream());
            if (serverTimeStamp != null)
                result.getDocumentElement().setAttribute
                    (WebServer.TIMESTAMP_HEADER, serverTimeStamp);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return result;
    }

    /** Check to ensure that the team and data directories exist, and
     * resolve the team directory to something that will work for the
     * current user, if possible.  */
    protected URL resolveTeamDataDirectory() {
        try {
            return resolveTeamDataDirectoryImpl();
        } catch (IOException e) {
            throw new WizardError(IND_DATADIR_ERR_URL).causedBy(e);
        }
    }

    private URL resolveTeamDataDirectoryImpl() throws IOException {
        Map<String, String> joinInfo = getTeamProjectJoinInformation();
        String teamDirectory = joinInfo.get(TEAM_DIRECTORY);
        String teamDirectoryUNC = joinInfo.get(TEAM_DIRECTORY_UNC);
        String teamDataDirectoryURL = joinInfo.get(TEAM_DATA_DIRECTORY_URL);
        String indivOverrideDirectory = getValue(IND_DIR_OVERRIDE);
        String dataSubdir = "data/" + joinInfo.get(PROJECT_ID);

        File f = null;

        if (StringUtils.hasValue(indivOverrideDirectory)) {
            // If the user has specified a manual directory override, try that
            // directory only.
            f = new File(indivOverrideDirectory, dataSubdir);
            if (f.isDirectory()) {
                joinInfo.put(TEAM_DIRECTORY, indivOverrideDirectory);
                joinInfo.put(TEAM_DIRECTORY_UNC, "");
                return f.toURI().toURL();
            }

        } else if (StringUtils.hasValue(teamDirectory)) {
            // Otherwise, try the directory specified by the team project.
            f = new File(teamDirectory, dataSubdir);
            if (f.isDirectory()) return f.toURI().toURL();

            // Try to find the data directory using the UNC path.
            if (StringUtils.hasValue(teamDirectoryUNC)) {
                NetworkDriveList networkDriveList = new NetworkDriveList();
                String altTeamDirectory =
                    networkDriveList.fromUNCName(teamDirectoryUNC);
                if (altTeamDirectory != null) {
                    File f2 = new File(altTeamDirectory, dataSubdir);
                    if (f2.isDirectory()) {
                        joinInfo.put(TEAM_DIRECTORY, altTeamDirectory);
                        return f2.toURI().toURL();
                    }
                }
            }
        }

        // if a URL has been provided, try it to see if it is valid.
        if (TeamServerSelector.testServerURL(teamDataDirectoryURL) != null) {
            joinInfo.put(TEAM_DIRECTORY, "");
            joinInfo.put(TEAM_DIRECTORY_UNC, "");
            return new URL(teamDataDirectoryURL + "/");
        }

        putValue(DATA_DIR, f == null ? null : f.getPath());
        putValue(DATA_DIR_URL, teamDataDirectoryURL);
        throw new WizardError(IND_DATADIR_ERR_URL);
    }

    protected void setPrefix(String newPrefix) {
        parameters.put("hierarchyPath", newPrefix);
    }

    /** Create the individual project if it doesn't exist, or change
     * the id of the TeamProjectStub to reflect the correct template
     * ID.  */
    protected void createIndivProject(String indivTemplateID) {
        if (DashController.alterTemplateID
            (getPrefix(), TEAM_STUB_ID, indivTemplateID)) return;

        DashController.alterTemplateID(getPrefix(), null, indivTemplateID);
    }

    /** Save data values for an individual project. */
    protected void saveIndivDataValues(String projectID, String teamURL,
            String indivInitials, String scheduleName, String scheduleID,
            String teamDirectory, String teamDirectoryUNC,
            String teamDataDirectoryURL) {
        putValue(PROJECT_ID, projectID);
        putValue(TEAM_PROJECT_URL, teamURL);
        putValue(INDIV_INITIALS, indivInitials);
        putValue(PROJECT_SCHEDULE_NAME, scheduleName);
        putValue(PROJECT_SCHEDULE_ID, scheduleID);
        putValue(TEAM_DIRECTORY, teamDirectory);
        putValue(TEAM_DIRECTORY_UNC, teamDirectoryUNC);
        putValue(TEAM_DATA_DIRECTORY_URL, teamDataDirectoryURL);
    }

    protected String createIndivSchedule(String scheduleName) {
        // if an older schedule exists with this same name, delete it.
        // (Based on our checks above, the older task list should be empty
        // anyway.  But we don't want to reuse its task list ID.)
        EVTaskList oldList = EVTaskList.openExisting(scheduleName,
            getDataRepository(), getPSPProperties(), getObjectCache(), false);
        if (oldList != null)
            oldList.save(null);

        EVTaskListData schedule = new EVTaskListData
            (scheduleName, getDataRepository(), getPSPProperties(), false);
        schedule.addTask(getPrefix(), getDataRepository(),
                         getPSPProperties(), null, false);
        schedule.getSchedule().setDatesLocked(true);
        schedule.setMetadata(PROJECT_SCHEDULE_SYNC_SCHEDULE,
            schedule.getSchedule().getSaveList().formatClean());
        schedule.setMetadata(PROJECT_SCHEDULE_SYNC_PDT,
            Double.toString(schedule.getSchedule().get(1).planDirectTime()));
        schedule.save();

        return schedule.getID();
    }

    protected void exportProjectData() {
        DashController.exportData(getPrefix());
    }

    private void startAsyncExport(final String forProject) {
        new Thread() {
            public void run() {
                DashController.exportData(forProject);
            };
        }.start();
    }

    protected boolean joinTeamSchedule(String teamURL, String scheduleName,
                                       String scheduleID) {
        exportProjectData();
        String exportedScheduleName = ExportManager.exportedScheduleDataPrefix(
                getOwner(), scheduleName);
        String exportFileName = getValue("EXPORT_FILE");

        String urlStr = "setup/wizard.class?"+PAGE+"="+JOIN_TEAM_SCHED_PAGE+
            "&scheduleName=" + HTMLUtils.urlEncode(exportedScheduleName) +
            "&fileName=" + HTMLUtils.urlEncode(exportFileName);
        if (scheduleID != null && scheduleID.length() != 0)
            urlStr = urlStr + "&scheduleID=" + HTMLUtils.urlEncode(scheduleID);
        URL u = buildTeamURLReference(teamURL, urlStr);
        try {
            URLConnection conn = u.openConnection();
            conn.setUseCaches(false);
            conn.connect();
            int status = ((HttpURLConnection) conn).getResponseCode();
            FileUtils.slurpContents(conn.getInputStream(), true);
            return (status == 200);
        } catch (Exception e) {}
        return false;
    }

    protected void importDisseminatedTeamData() {
        RepairImportInstruction.maybeRepairForIndividual(getDataContext());
    }

    protected void showIndivSuccessPage(boolean joinSucceeded) {
        String prefix = WebServer.urlEncodePath(getPrefix());
        String selfUrl = prefix + "/" + env.get("SCRIPT_NAME");
        String url = StringUtils.findAndReplace(selfUrl, "wizard.class",
            IND_SUCCESS_URL);

        if (joinSucceeded)
            printRedirect(url);
        else
            printRedirect(url + "?schedProblem");

        // the logic above will send the user to a "success" page.  While
        // they are reading that page, we will kick off a "Sync to WBS"
        // operation in the background.
        final String syncUrl = StringUtils.findAndReplace(selfUrl,
            "wizard.class", IND_BG_SYNC_URL);
        BackgroundTaskManager.getInstance().addTask(new Runnable() {
            public void run() {
                try {
                    getRequest(syncUrl, false);
                } catch (Exception e) {}
                // we tell the sync operation to skip the export step, and we
                // perform it ourselves instead.  This is to ensure that
                // *something* gets exported, even if no sync was performed
                // (for example, because the WBS hasn't been edited yet).
                exportProjectData();
            }});
    }

    private class WizardError extends RuntimeException {
        private String uri;

        protected WizardError(String uri) {
            this.uri = uri;
        }

        protected WizardError param(String param) {
            if (param != null)
                this.uri = HTMLUtils.appendQuery(uri, param);
            return this;
        }

        protected WizardError param(String param, String value) {
            this.uri = HTMLUtils.appendQuery(uri, param, value);
            return this;
        }

        protected WizardError causedBy(Throwable t) {
            initCause(t);
            return this;
        }

    }


    public static boolean copyNodeIDsToHierarchy(DataRepository data,
            DashHierarchy hier) {
        return copyNodeIDsToHierarchy(data, hier, PropertyKey.ROOT);
    }

    private static boolean copyNodeIDsToHierarchy(DataContext data,
            DashHierarchy hier, PropertyKey key) {

        boolean madeChange = false;
        SimpleData projectID = data.getSimpleValue(DataRepository
                .createDataName(key.path(), TeamDataConstants.PROJECT_ID));

        if (projectID != null && projectID.test()) {
            String nodeID = projectID.format() + ":root";
            Prop p = hier.pget(key);
            if (!nodeID.equals(p.getNodeID())) {
                p.setNodeID(nodeID);
                madeChange = true;
            }

        } else {
            for (int i = hier.getNumChildren(key); i-- > 0;) {
                PropertyKey child = hier.getChildKey(key, i);
                if (copyNodeIDsToHierarchy(data, hier, child))
                    madeChange = true;
            }
        }

        return madeChange;
    }

}
