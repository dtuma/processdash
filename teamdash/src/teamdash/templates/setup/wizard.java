
package teamdash.templates.setup;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.processdash.BackgroundTaskManager;
import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.ImmutableDoubleData;
import net.sourceforge.processdash.data.ImmutableStringData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.ev.EVTaskListData;
import net.sourceforge.processdash.ev.EVTaskListRollup;
import net.sourceforge.processdash.ev.EVTaskListXML;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.process.ScriptID;
import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.tool.export.DataImporter;
import net.sourceforge.processdash.tool.export.mgr.ExportManager;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.NetworkDriveList;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** This wizard sets up a team project.
 *
 * It is expected that the act of setting up a team project will be
 * bootstrapped by a CGI script called "dash/teamStart.class", which
 * is included in the main dashboard distribution.
 */
public class wizard extends TinyCGIBase implements TeamDataConstants {

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

    // value indicating we should add an individual to a team project
    private static final String IND_PAGE = "indiv";
    // Information for the page which asks the user to select a
    // location in their hierarchy to create the project.
    private static final String IND_NODE_PAGE = "indivSelectNode";
    private static final String IND_NODE_URL = "indivSelectNode.shtm";
    // Information for the page which asks an individual to enter
    // their initials
    private static final String IND_INITIALS_PAGE = "indivInitials";
    private static final String IND_INITIALS_URL = "indivInitials.shtm";
    // Information for the page which asks an individual to select a
    // name for their project schedule.
    private static final String IND_SCHEDULE_PAGE = "indivSchedule";
    private static final String IND_SCHEDULE_URL = "indivSchedule.shtm";
    // URL for the page which asks the user to close the hierarchy
    // editor, when joining a team project.
    private static final String IND_CLOSE_HIERARCHY_URL =
        "indivCloseHierarchy.shtm";
    // Information for the page which asks an individual to confirm
    // their choices.
    private static final String IND_CONFIRM_PAGE = "indivConfirm";
    private static final String IND_SHOW_CONFIRM_PAGE = "indivShowConfirm";
    private static final String IND_CONFIRM_URL = "indivConfirm.shtm";
    // information for the page indicating that the user wishes to override
    // the location of the team directory
    private static final String IND_OVERRIDE_PAGE = "indivDirOverride";
    // URL of the page that is displayed when the wizard successfully
    // joins the individual to the team project
    private static final String IND_SUCCESS_URL = "indivSuccess.shtm";
    private static final String IND_BG_SYNC_URL = "sync.class?run&bg";
    // URLs for pages alerting an individual to various errors that could
    // occur when attempting to join a team project.
    private static final String IND_CONNECT_ERR_URL = "indivConnectError.shtm";
    private static final String IND_DATADIR_ERR_URL = "indivDataDirError.shtm";

    // Flag indicating an individual wants to join the team schedule.
    private static final String JOIN_TEAM_SCHED_PAGE = "joinSchedule";


    // Names of session variables used to store user selections.
    private static final String TEAM_MASTER_FLAG = "setup//Is_Master";
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
    private static final String IND_DIR_OVERRIDE = "setup//Indiv_Team_Dir_Override";

    // the template ID of a "team project stub"
    private static final String TEAM_STUB_ID = "TeamProjectStub";

    private static final Logger logger =
        Logger.getLogger(wizard.class.getName());

    protected void writeHeader() {}
    protected void writeContents() {}
    public void service(InputStream in, OutputStream out, Map env)
        throws IOException
    {
        super.service(in, out, env);
        if ("POST".equalsIgnoreCase((String) env.get("REQUEST_METHOD")))
            parseFormData();

        String page = getParameter(PAGE);
        if (IND_PAGE.equals(page))                 handleIndivPage();
        else if (IND_NODE_PAGE.equals(page))       handleIndivNodePage();
        else if (IND_INITIALS_PAGE.equals(page))   handleIndivInitialsPage();
        else if (IND_SCHEDULE_PAGE.equals(page))   handleIndivSchedulePage();
        else if (IND_SHOW_CONFIRM_PAGE.equals(page))
                                                   showIndivConfirmPage();
        else if (IND_CONFIRM_PAGE.equals(page))    handleIndivConfirmPage();
        else if (IND_OVERRIDE_PAGE.equals(page))   handleIndivDirOverridePage();

        else if (JOIN_TEAM_SCHED_PAGE.equals(page))
                                                   handleJoinTeamSchedPage();

        else if (!prefixNamesTeamProjectStub())    showInvalidPage();
        else if (page == null)                     showWelcomePage();
        else if (TEAM_START_PAGE.equals(page))     showTeamMasterChoicePage();
        else if (TEAM_MASTER_PAGE.equals(page))    handleTeamMasterChoicePage();
        else if (TEAM_DIR_PAGE.equals(page))       handleTeamDirPage();
        else if (TEAM_SCHEDULE_PAGE.equals(page))  handleTeamSchedulePage();
        else if (TEAM_CLOSE_HIERARCHY_PAGE.equals(page))
                                                   handleTeamCloseHierPage();
        else if (TEAM_CONFIRM_PAGE.equals(page))   handleTeamConfirmPage();
        else                                       showWelcomePage();

        this.out.flush();

        // clear out the networkDriveList in case we created it.
        networkDriveList = null;
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

    /** Get a value from the data repository as a String. */
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
        DashHierarchy hierarchy = getPSPProperties();
        PropertyKey key = hierarchy.findExistingKey(getPrefix());
        String templateID = hierarchy.getID(key);
        return TEAM_STUB_ID.equals(templateID);
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
            showTeamDirPage();
        } else if (parameters.get("createTeamProject") != null) {
            putValue(TEAM_MASTER_FLAG, ImmutableDoubleData.FALSE);
            showTeamDirPage();
        } else {
            showTeamMasterChoicePage();
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
        String processID = getValue(TEAM_PID);
        if (processID == null) return null; // shouldn't happen!

        String teamJar = findTeamProcessJarfile(processID);
        String teamDir = checkNetworkDrive(extractTeamDir(teamJar));
        return teamDir;
    }

    /** Try to locate the jarfile containing the definition for the
     * given process, and return the path to the file.
     */
    static String findTeamProcessJarfile(String processID) {
        Vector scripts = TemplateLoader.getScriptIDs(processID, null);
        if (scripts == null) scripts = new Vector();
        scripts.add(new ScriptID(processID + "-template.xml", null, null));
        for (int i = scripts.size();   i-- > 0; ) {
            String scriptURL = ((ScriptID) scripts.get(i)).getScript();
            URL u = TemplateLoader.resolveURL(scriptURL);
            if (u == null) continue;
            String url = u.toString();
            if (!url.startsWith("jar:file:")) continue;
            int pos = url.indexOf('!');
            if (pos == -1) continue;
            return HTMLUtils.urlDecode(url.substring(9, pos));
        }
        return null;
    }

    /** Given the name of a jarfile containing a team process,
     * ascertain the corresponding team directory.
     */
    private String extractTeamDir(String filename) {
        if (filename == null) return null;
        int pos = filename.toUpperCase().indexOf("/TEMPLATES");
        if (pos != -1)
            return filename.substring(0, pos);
        else
            return null;
    }

    /** if the filename appears to be on a network drive, return the
     *  canonicalized version of the filename. Otherwise returns null.
     */
    private String checkNetworkDrive(String filename) {
        if (filename == null) return null;
        try {
            File directory = new File(filename);
            String result = directory.getCanonicalPath();
            if (ensureNetworkDrive(result))
                return result;
            else
                System.out.println("not a network drive: "+result);
        } catch (IOException ioe) {}
        return null;
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
            ensureNetworkDrive(teamDir))

            showTeamSchedulePage();

        else
            printRedirect(TEAM_DIR_URL + "?confirm");
    }

    /** Display the team schedule name page */
    protected void showTeamSchedulePage() {
        printRedirect(TEAM_SCHEDULE_URL);
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
        String teamSchedule = getValue(TEAM_SCHEDULE);
        String processJarFile = findTeamProcessJarfile(teamPID);
        String projectID = generateID();

        boolean isMaster = testValue(TEAM_MASTER_FLAG);
        if (isMaster)
            teamPID = StringUtils.findAndReplace(teamPID,
                    "/TeamRoot", "/MasterRoot");

        // create the required team directories. This involves file IO
        // which could fail for various reasons, so we attempt to get
        // it out of the way first.
        if (!createTeamDirs(teamDirectory, projectID) ||
            !writeTeamSettingsFile(teamPID, teamDirectory, teamSchedule,
                                   projectID, processJarFile))
            // the error page will already have been displayed by now,
            // so just abort on failure.
            return;

        // perform lots of other setup tasks.  Unlike the operation
        // above, these tasks should succeed 99.999% of the time.
        alterTeamTemplateID(teamPID);
        String scheduleID = createTeamSchedule (teamSchedule);
        saveTeamDataValues (teamDirectory, projectID, teamSchedule, scheduleID);
        saveTeamSettings (teamDirectory, projectID);
        tryToCopyProcessJarfile (processJarFile, teamDirectory);

        // print a success message!
        printRedirect(TEAM_SUCCESS_URL);
    }

    private String generateID() {
        return Long.toString(System.currentTimeMillis(), Character.MAX_RADIX);
    }

    protected boolean createTeamDirs(String teamDirectory, String projectID) {
        File teamDir = new File(teamDirectory);
        if (!createTeamDirectory(teamDir)) return false;

        File templateDir = new File(teamDir, "Templates");
        if (!createTeamDirectory(templateDir)) return false;

        File dataDir = new File(teamDir, "data");
        if (!createTeamDirectory(dataDir)) return false;

        File projDataDir = new File(dataDir, projectID);
        if (!createTeamDirectory(projDataDir)) return false;

        File disseminationDir = new File(projDataDir, DISSEMINATION_DIRECTORY);
        if (!createTeamDirectory(disseminationDir)) return false;

        return true;
    }

    private boolean createTeamDirectory(File directory) {
        if (directory.isDirectory()) return true;
        if (directory.mkdirs()) return true;
        String errMsg = "The team project setup wizard was unable to "+
            "create the directory '" + directory + "'. Please ensure that ";
        if (isWindows())
            errMsg = errMsg + "the network drive is mapped and that ";
        errMsg = errMsg + "you have adequate file permissions to create " +
            "this directory, then click &quot;Next.&quot;  Otherwise, " +
            "enter a different team directory below.";
        errMsg = HTMLUtils.urlEncode(errMsg);
        printRedirect(TEAM_DIR_URL + "?errMsg=" + errMsg);
        return false;
    }

    private boolean writeTeamSettingsFile(String teamPID,
                                          String teamDirectory,
                                          String teamSchedule,
                                          String projectID,
                                          String processJarFile)
    {
        File teamDir = new File(teamDirectory);
        File dataDir = new File(teamDir, "data");
        File projDataDir = new File(dataDir, projectID);
        TeamSettingsFile tsf = new TeamSettingsFile(projDataDir);

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
            return true;

        } catch (IOException ioe) {
            String errMsg = "The team project setup wizard was unable to "+
                "write the team project settings file '"+tsf.getSettingsFile()+
                "'. Please ensure that you have adequate file permissions " +
                "to create this file, then click &quot;Next.&quot; " +
                "Otherwise, enter a different team directory below.";
            errMsg = HTMLUtils.urlEncode(errMsg);
            printRedirect(TEAM_DIR_URL + "?errMsg=" + errMsg);
            return false;
        }
    }


    protected boolean tryToCopyProcessJarfile(String jarFilename,
                                              String teamDirectory) {
        // no jar to copy? abort.
        if (jarFilename == null) return false;

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

    protected void saveTeamDataValues(String teamDirectory, String projectID,
            String teamScheduleName, String teamScheduleID) {
        putValue(TEAM_DIRECTORY, teamDirectory);
        String uncName = calcUNCName(teamDirectory);
        if (uncName != null) putValue(TEAM_DIRECTORY_UNC, uncName);

        putValue(PROJECT_ID, projectID);
        putValue(PROJECT_SCHEDULE_NAME, teamScheduleName);
        putValue(PROJECT_SCHEDULE_ID, teamScheduleID);

        // FIXME: need to really give the user an opportunity to set a
        // password.
        putValue("_Password_", ImmutableDoubleData.TRUE);
    }


    protected String calcUNCName(String filename) {
        String result = getNetworkDriveList().toUNCName(filename);
        if (result == null) return null;

        File testFile = new File(result);
        if (testFile.isDirectory())
            return result;
        else
            return null;
    }




    protected void saveTeamSettings(String teamDirectory, String projectID) {
        // rewrite the team directory into "settings" filename form.
        teamDirectory = teamDirectory.replace('\\', '/');
        if (teamDirectory.endsWith("/"))
            teamDirectory = teamDirectory.substring
                (0, teamDirectory.length()-1);

        // calculate the new import instruction, and add it to the
        // import list
        String prefix = "Import_" + projectID;
        String importDir = teamDirectory + "/data/" + projectID;
        DashController.addImportSetting(prefix, importDir);

        // enable other configuration settings that are appropriate for
        // team use.
        DashController.enableTeamSettings();

        // initiate the template directory adding task.
        String templatePathSetting = Settings.getVal(
            "teamJoin.templateSearchPathPhilosophy");
        if ("alwaysAdd".equalsIgnoreCase(templatePathSetting)) {
            putValue(TEAM_TEMPLATE_FLAG, "t");
            new TemplateDirAdder(teamDirectory);
        }
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
        if (slashPos == -1) return null;
        String importedFile =
            importDir + "/" + indivFileName.substring(slashPos+1);
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


    /** Begin the process of helping an individual join a team process. */
    protected void handleIndivPage() {
        // if there is a current prefix, and it names a team planning
        // stub, then this is the location where the user wants to
        // create the team project.
        if (prefixNamesTeamProjectStub())
            showIndivInitialsPage();

        else
            // if the current prefix is null, or if it doesn't name a
            // team stub, then we must determine where the user wants
            // to create the team project.
            showIndivNodePage();
    }

    /** Redirect back to the bootstrap script to get the URL of the
     * team project. */
    protected void showTeamURLPage() {
        printRedirect(TEAM_URL_URL);
    }

    /** Return true if the URL prefix of this page names an existing
     * team project stub in the hierarchy.
    protected boolean prefixNamesTeamProjectStub() {
        String templateID = null;
        String prefix = getPrefix();
        if (prefix != null) {
            DashHierarchy props = getPSPProperties();
            templateID = props.getID(props.findExistingKey(prefix));
        }
        return (templateID != null && templateID.equals(TEAM_STUB_ID));
    }
     */

    /** Display a page allowing the user to select a place in their
     * hierarchy to create this project. */
    protected void showIndivNodePage() {
        maybeSetDefaultNodeName();
        printRedirect(IND_NODE_URL);
    }
    protected void showIndivNodePage(String errMsg) {
        printRedirect(IND_NODE_URL + "?errMsg=" + HTMLUtils.urlEncode(errMsg));
    }

    /** Possibly store a default name for the project node */
    protected void maybeSetDefaultNodeName() {
        if (getValue(NODE_NAME) != null) return;

        String teamURL = getValue(TEAM_URL);
        if (teamURL == null) return;

        teamURL = HTMLUtils.urlDecode(teamURL);
        int end = teamURL.lastIndexOf("//");
        if (end == -1) return;

        int beg = teamURL.lastIndexOf('/', end-1);
        if (beg == -1) return;

        String defaultName = teamURL.substring(beg+1, end);
        putValue(NODE_NAME, defaultName);
    }

    /** Handle values posted from the node selection page */
    protected void handleIndivNodePage() {
        String nodeName = getParameter("Node_Name");
        String nodeLocation = getParameter("Node_Location");
        putValue(NODE_NAME, nodeName);
        putValue(NODE_LOCATION, nodeLocation);

        if (nodeName == null || nodeName.trim().length() == 0) {
            showIndivNodePage("You must enter a name for the project.");
            return;
        }
        nodeName = nodeName.trim();
        if (nodeName.indexOf('/') != -1) {
            showIndivNodePage
                ("The project name cannot contain the '/' character.");
            return;
        }
        if (!nodeName.equals(new String(nodeName.getBytes()))) {
            showIndivNodePage
                ("Sorry, the dashboard currently does not support the "+
                 "use of extended unicode characters in project names.");
            return;
        }

        if (nodeLocation == null || nodeLocation.trim().length() == 0) {
            showIndivNodePage
                ("You must choose a location in the hierarchy where the "+
                 "project should be created.");
            return;
        }
        nodeLocation = nodeLocation.trim();
        DashHierarchy hierarchy = getPSPProperties();
        PropertyKey key = hierarchy.findExistingKey(nodeLocation);
        if (key == null) {
            showIndivNodePage
                ("The parent node you selected doesn't currently exist in "+
                 "your dashboard hierarchy.");
            return;
        }
        while (key != null) {
            String templateID = hierarchy.getID(key);
            if (templateID != null && templateID.length() > 0) {
                showIndivNodePage
                    ("The dashboard cannot create the project at the "+
                     "location you selected in your dashboard hierarchy. "+
                     "Please select a different location.");
                return;
            }
            key = key.getParent();
        }
        // check for unique name.
        String fullName = null;
        if (nodeLocation.endsWith("/"))
            fullName = nodeLocation + nodeName;
        else
            fullName = nodeLocation + "/" + nodeName;

        key = hierarchy.findExistingKey(fullName);
        if (key != null) {
            showIndivNodePage
                ("There is already a project in your hierarchy with the "+
                 "name and parent you selected. Please change either the "+
                 "project name or the project parent.");
            return;
        }

        showIndivInitialsPage();
    }

    /** Display the page for an individual to enter their initials */
    protected void showIndivInitialsPage() {
        printRedirect(IND_INITIALS_URL);
    }

    /** Handle values posted from the individual initials page */
    protected void handleIndivInitialsPage() {
        String error = "";

        String initials = getParameter("initials");
        if (initials == null || initials.trim().length() == 0) {
            error += "&missing";
        } else {
            initials = initials.trim();
            for (int i = initials.length();   i-- > 0; ) {
                char c = initials.charAt(i);
                if (c >= 'A' && c <= 'Z') continue;
                if (c >= 'a' && c <= 'z') continue;
                error += "&non_alpha";
                break;
            }
        }

        String fullname = getParameter("fullname");
        if (fullname == null || fullname.trim().length() == 0 ||
            fullname.equals("Enter your name")) {
            error += "&name_missing";
        }

        if (error.length() > 0) {
            printRedirect(IND_INITIALS_URL + "?err" + error);
            return;
        }

        putValue(IND_INITIALS, initials);
        fullname = fullname.trim();
        putValue(IND_FULLNAME, fullname);

        showIndivSchedulePage();
    }

    /** Display the page for an individual to select a schedule name */
    protected void showIndivSchedulePage() {
        printRedirect(IND_SCHEDULE_URL);
    }

    /** Handle values posted from the individual schedule name page */
    protected void handleIndivSchedulePage() {
        String scheduleName = getParameter("scheduleName");
        if (scheduleName == null || scheduleName.trim().length() == 0) {
            printRedirect(IND_SCHEDULE_URL + "?missing");
            return;
        }

        scheduleName = scheduleName.trim();
        putValue(IND_SCHEDULE, scheduleName);

        if (!EVTaskListData.validName(scheduleName)) {
            printRedirect(IND_SCHEDULE_URL + "?invalid");
            return;
        }

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
                printRedirect(IND_SCHEDULE_URL + "?duplicate");
                return;
            }
        }

        showIndivConfirmPage();
    }

    /** Ensure that all the required individual data has been entered.
     *  If any data is missing, redirect to that page and return
     *  false.  Otherwise return true
     */
    protected boolean ensureIndivValues() {
        if (!teamURLIsValid(getValue(TEAM_URL)))
            showTeamURLPage();

        else if (prefixNamesTeamProjectStub() == false &&
                 (getValue(NODE_NAME) == null ||
                  getValue(NODE_LOCATION) == null))
            showIndivNodePage();

        else if (getValue(IND_INITIALS) == null)
            showIndivInitialsPage();

        else if (getValue(IND_SCHEDULE) == null)
            showIndivSchedulePage();

        else if (DashController.isHierarchyEditorOpen())
            printRedirect(IND_CLOSE_HIERARCHY_URL);

        else
            return true;

        return false;
    }

    private boolean teamURLIsValid(String teamURL) {
        return buildTeamURLReference(teamURL, "index.htm") != null;
    }
    private URL buildTeamURLReference(String teamURL, String ref) {
        //System.out.println("buildTeamURLReference("+teamURL+","+ref+")");
        if (teamURL == null || teamURL.trim().length() == 0) return null;

        teamURL = StringUtils.findAndReplace(teamURL.trim(), "/+/", "//");
        if (!teamURL.startsWith("http://")) return null;
        int pos = teamURL.indexOf("//", 7);
        if (pos != -1) pos = teamURL.indexOf('/', pos+2);
        if (pos == -1) return null;
        teamURL = teamURL.substring(0, pos+1) + ref;
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
        handleIndivConfirmPage();
    }
    private static final Pattern DATA_SUBDIR_PATTERN = Pattern.compile(
        "(/|\\\\)data((/|\\\\)[a-z0-9]{8,9})$", Pattern.CASE_INSENSITIVE);

    /** Display the page for an individual to confirm their choices */
    protected void showIndivConfirmPage() {
        if (ensureIndivValues())
            printRedirect(IND_CONFIRM_URL);
    }

    /** Once the individual has confirmed their settings, perform the join
     *  process.
     */
    protected void handleIndivConfirmPage() {
        // make sure all the required data is present - otherwise abort.
        if (!ensureIndivValues()) return;

        String teamURL = getValue(TEAM_URL);
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

        // Download team project information from the team dashboard.
        Document d = downloadTeamProjectInformation(teamURL);
        if (d == null) {
            printRedirect(IND_CONNECT_ERR_URL); return;
        }
        teamURL = buildTeamURLReference(teamURL, "teamIndex.shtm").toString();
        Element e = d.getDocumentElement();

        String projectID = e.getAttribute(PROJECT_ID);
        String teamDirectory = e.getAttribute(TEAM_DIRECTORY);
        String teamDirectoryUNC = e.getAttribute(TEAM_DIRECTORY_UNC);
        String indivTemplateID = e.getAttribute("Template_ID");

        if (!XMLUtils.hasValue(projectID) ||
            !XMLUtils.hasValue(teamDirectory) ||
            !XMLUtils.hasValue(indivTemplateID)) {
            printRedirect(IND_CONNECT_ERR_URL); return;
        }

        // Check on the existence of the team data directory, and
        // resolve it to something that works for the current
        // user. This involves file IO which could fail for various
        // reasons, so we attempt to get it out of the way first.
        String dataSubdir = "data"+File.separator+ projectID;
        String indivDirOverride = getValue(IND_DIR_OVERRIDE);
        teamDirectory = resolveTeamDirectory(indivDirOverride, teamDirectory,
            teamDirectoryUNC, dataSubdir);
        if (teamDirectory == null)
            // the error page will already have been displayed by now,
            // so just abort on failure.
            return;
        else if (teamDirectory.equals(indivDirOverride))
            // if the user overrode the team directory, we should ignore any
            // unreachable UNC path associated with the original team dir.
            teamDirectoryUNC = null;

        if (teamDirectory.endsWith(File.separator))
            // remove any trailing file separator if present.
            teamDirectory = teamDirectory.substring(0, teamDirectory.length()-1);

        // Check to see if the "team project" is in the same dashboard
        // instance as the "individual project."
        String remoteTimeStamp =e.getAttribute(WebServer.TIMESTAMP_HEADER);
        String localTimeStamp = getTinyWebServer().getTimestamp();
        boolean isLocal = localTimeStamp.equals(remoteTimeStamp);

        // perform lots of other setup tasks.  Unlike the operation
        // above, these tasks should succeed 99.999% of the time.
        setPrefix(localProjectName);
        createIndivProject(indivTemplateID);
        String scheduleID = createIndivSchedule(scheduleName);
        saveIndivDataValues(projectID, teamURL, indivInitials, scheduleName,
            scheduleID, teamDirectory, teamDirectoryUNC, isLocal);
        exportIndivData();
        boolean joinSucceeded = true;
        if (isLocal)
            joinSucceeded = joinLocalTeamSchedule(teamURL, scheduleName);
        else {
            joinSucceeded = joinTeamSchedule
                (teamURL, scheduleName, scheduleID);
            importDisseminatedTeamData(teamDirectory, projectID);
        }

        showIndivSuccessPage(joinSucceeded);
    }

    /** Contacts the team dashboard and downloads information about
     * the project. On failure, this will return null.
     */
    protected Document downloadTeamProjectInformation(String teamURL) {
        URL u = buildTeamURLReference(teamURL, "setup/join.class?xml");
        Document result = null;
        try {
            URLConnection conn = u.openConnection();
            conn.setUseCaches(false);
            conn.connect();
            String serverTimeStamp = conn.getHeaderField
                (WebServer.TIMESTAMP_HEADER);
            result = XMLUtils.parse(conn.getInputStream());
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
    protected String resolveTeamDirectory(String indivOverrideDirectory,
                                          String teamDirectory,
                                          String teamDirectoryUNC,
                                          String dataSubdir) {
        File f;

        if (StringUtils.hasValue(indivOverrideDirectory)) {
            // If the user has specified a manual directory override, try that
            // directory only.
            f = new File(indivOverrideDirectory, dataSubdir);
            if (f.isDirectory()) return indivOverrideDirectory;

        } else {
            // Otherwise, try the directory specified by the team project.
            f = new File(teamDirectory, dataSubdir);
            if (f.isDirectory()) return teamDirectory;

            // Try to find the data directory using the UNC path.
            if (teamDirectoryUNC != null) {
                NetworkDriveList networkDriveList = new NetworkDriveList();
                String altTeamDirectory =
                    networkDriveList.fromUNCName(teamDirectoryUNC);
                if (altTeamDirectory != null) {
                    File f2 = new File(altTeamDirectory, dataSubdir);
                    if (f2.isDirectory()) return altTeamDirectory;
                }
            }
        }

        putValue(DATA_DIR, f.getPath());
        printRedirect(IND_DATADIR_ERR_URL);
        return null;
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
            String teamDirectory, String teamDirectoryUNC, boolean isLocal) {
        putValue(PROJECT_ID, projectID);
        putValue(TEAM_PROJECT_URL, teamURL);
        putValue(INDIV_INITIALS, indivInitials);
        putValue(PROJECT_SCHEDULE_NAME, scheduleName);
        putValue(PROJECT_SCHEDULE_ID, scheduleID);
        putValue(TEAM_DIRECTORY, teamDirectory);
        if (teamDirectoryUNC != null)
            putValue(TEAM_DIRECTORY_UNC, teamDirectoryUNC);
        if (isLocal)
            putValue("EXPORT_FILE", ImmutableStringData.EMPTY_STRING);
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

    protected void exportIndivData() {
        DashController.exportData(getPrefix());
    }

    protected boolean joinTeamSchedule(String teamURL, String scheduleName,
                                       String scheduleID) {
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

    protected boolean joinLocalTeamSchedule(String teamURL,
                                            String indivScheduleName) {

        // find the first slash after the "http://" prefix.
        int slashPos = teamURL.indexOf('/', 7);
        // find the "//" that ends the prefix.
        int sslashPos = teamURL.indexOf("//", slashPos);
        // extract the prefix of the team project from the URL.
        String teamProjectPrefix =
            HTMLUtils.urlDecode(teamURL.substring(slashPos, sslashPos));

        String dataName = DataRepository.createDataName
            (teamProjectPrefix, PROJECT_SCHEDULE_NAME);
        SimpleData schedNameData =
            getDataRepository().getSimpleValue(dataName);
        if (schedNameData == null) return false;
        String teamScheduleName = schedNameData.format();

        // add the individual's schedule to the team schedule.
        return addScheduleToRollup(teamScheduleName, indivScheduleName);
    }

    protected void importDisseminatedTeamData(String teamDirectory,
            String projectID) {
        // rewrite the team directory into "settings" filename form.
        teamDirectory = teamDirectory.replace('\\', '/');
        if (teamDirectory.endsWith("/"))
            teamDirectory = teamDirectory.substring
                (0, teamDirectory.length()-1);

        // calculate the new import instruction, and add it to the
        // import list
        String prefix = "Disseminated_" + projectID;
        String importDir = teamDirectory + "/data/" + projectID + "/"
                + DISSEMINATION_DIRECTORY;
        DashController.addImportSetting(prefix, importDir);
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
            }});
    }

}
