
import pspdash.*;
import pspdash.data.DataRepository;
import pspdash.data.DataImporter;
import pspdash.data.ImmutableStringData;
import pspdash.data.ImmutableDoubleData;
import pspdash.data.SimpleData;

import java.io.*;
import java.net.*;
import java.util.*;
import org.w3c.dom.*;

/** This wizard sets up a team project.
 *
 * It is expected that the act of setting up a team project will be
 * bootstrapped by a CGI script called "dash/teamStart.class", which
 * is included in the main dashboard distribution.
 */
public class wizard extends TinyCGIBase {


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
    // URL of the page that is displayed when the wizard successfully
    // joins the individual to the team project
    private static final String IND_SUCCESS_URL = "indivSuccess.shtm";
    // URLs for pages alerting an individual to various errors that could
    // occur when attempting to join a team project.
    private static final String IND_CONNECT_ERR_URL = "indivConnectError.shtm";
    private static final String IND_DATADIR_ERR_URL = "indivDataDirError.shtm";

    // Flag indicating an individual wants to join the team schedule.
    private static final String JOIN_TEAM_SCHED_PAGE = "joinSchedule";


    // Names of session variables used to store user selections.
    private static final String SUGG_TEAM_DIR = "setup//Suggested_Team_Dir";
    private static final String TEAM_DIR = "setup//Team_Dir";
    private static final String TEAM_SCHEDULE = "setup//Team_Schedule";
    private static final String NODE_NAME = "setup//Node_Name";
    private static final String NODE_LOCATION = "setup//Node_Location";
    private static final String IND_INITIALS = "setup//Indiv_Initials";
    private static final String IND_SCHEDULE = "setup//Indiv_Schedule";
    private static final String DATA_DIR = "setup//Data_Directory";

    // the template ID of a "team project stub"
    private static final String TEAM_STUB_ID = "TeamProjectStub";


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

        else if (JOIN_TEAM_SCHED_PAGE.equals(page))
                                                   handleJoinTeamSchedPage();

        else if (!prefixNamesTeamProjectStub())    showInvalidPage();
        else if (page == null)                     showWelcomePage();
        else if (TEAM_START_PAGE.equals(page))     showTeamDirPage();
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
        putValue(name, new ImmutableStringData(value));
    }

    protected void putValue(String name, SimpleData dataValue) {
        DataRepository data = getDataRepository();
        String prefix = getPrefix();
        if (prefix == null) prefix = "";
        String dataName = DataRepository.createDataName(prefix, name);
        data.putValue(dataName, dataValue);
    }

    /** Get a value from the data repository. */
    protected String getValue(String name) {
        DataRepository data = getDataRepository();
        String prefix = getPrefix();
        if (prefix == null) prefix = "";
        String dataName = DataRepository.createDataName(prefix, name);
        SimpleData d = data.getSimpleValue(dataName);
        return (d == null ? null : d.format());
    }

    /** Returns true if the current prefix names a "TeamProjectStub" node */
    protected boolean prefixNamesTeamProjectStub() {
        PSPProperties hierarchy = getPSPProperties();
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
    private String findTeamProcessJarfile(String processID) {
        Vector scripts = TemplateLoader.getScriptIDs(processID, null);
        scripts.add(new ScriptID(processID + "-template.xml", null, null));
        for (int i = scripts.size();   i-- > 0; ) {
            String scriptURL = ((ScriptID) scripts.get(i)).getScript();
            URL u = TemplateLoader.resolveURL(scriptURL);
            if (u == null) continue;
            String url = u.toString();
            if (!url.startsWith("jar:file:")) continue;
            int pos = url.indexOf('!');
            if (pos == -1) continue;
            return URLDecoder.decode(url.substring(9, pos));
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
        saveTeamDataValues (teamDirectory, projectID, teamSchedule);
        createTeamSchedule (teamSchedule);
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
        errMsg = URLEncoder.encode(errMsg);
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
        File settingsFile = new File(projDataDir, "settings.xml");

        try {
            Writer out = new OutputStreamWriter
                (new FileOutputStream(settingsFile), "UTF-8");
            // write XML header
            out.write("<?xml version='1.0' encoding='UTF-8'?>\n");
            // open XML tag
            out.write("<project-settings\n");
            // write the project name
            String name = getPrefix();
            if (name != null) {
                int pos = name.lastIndexOf('/');
                if (pos != -1) name = name.substring(pos+1);
                out.write("    projectName='" +
                          XMLUtils.escapeAttribute(name) + "'\n");
            }
            // write the project ID.
            out.write("    projectID='" +
                      XMLUtils.escapeAttribute(projectID) + "'\n");
            // write the process ID.
            int pos = teamPID.indexOf('/');
            if (pos != -1) teamPID = teamPID.substring(0, pos);
            out.write("    processID='" +
                      XMLUtils.escapeAttribute(teamPID) + "'\n");
            // write the relative path to the process jar file, if we know it
            if (processJarFile != null) {
                File jarFile = new File(processJarFile);
                out.write("    templatePath='../../Templates/" +
                          XMLUtils.escapeAttribute(jarFile.getName()) + "'\n");
            }
            // write the schedule name
            out.write("    scheduleName='" +
                      XMLUtils.escapeAttribute(teamSchedule) + "'\n");
            // close the XML tag
            out.write("/>\n");
            out.close();
            return true;

        } catch (IOException ioe) {
            String errMsg = "The team project setup wizard was unable to "+
                "write the team project settings file '" + settingsFile +
                "'. Please ensure that you have adequate file permissions " +
                "to create this file, then click &quot;Next.&quot; " +
                "Otherwise, enter a different team directory below.";
            errMsg = URLEncoder.encode(errMsg);
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

    protected void saveTeamDataValues(String teamDirectory,
                                      String projectID,
                                      String teamSchedule) {
        putValue("Team_Directory", teamDirectory);
        String uncName = calcUNCName(teamDirectory);
        if (uncName != null) putValue("Team_Directory_UNC", uncName);

        putValue("Project_ID", projectID);
        putValue("Project_Schedule_Name", teamSchedule);

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
        new TemplateDirAdder(teamDirectory);
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


    protected void createTeamSchedule(String scheduleName) {
        EVTaskListRollup rollup = new EVTaskListRollup
            (scheduleName, getDataRepository(),
             getPSPProperties(), getObjectCache(), false);
        rollup.save();

        // todo: create a top-down schedule as well.
    }

    protected void alterTeamTemplateID(String teamPID) {
        DashController.alterTemplateID(getPrefix(), TEAM_STUB_ID, teamPID);
    }

    protected void handleJoinTeamSchedPage() {
        String teamScheduleName = getValue("Project_Schedule_Name");
        String indivExportedName = getParameter("scheduleName");
        String indivFileName = getParameter("fileName");
        if (addIndivScheduleToTeamSchedule
            (teamScheduleName, indivExportedName, indivFileName)) {
            // print out a null document and call it good.
            out.print("Content-type: text/plain\r\n\r\n ");
        } else {
            out.print("Status: 500 Join Failed\r\n\r\n");
        }
    }

    /** Add an individual's personal schedule to the team rollup. */
    protected boolean addIndivScheduleToTeamSchedule(String teamScheduleName,
                                                     String indivExportedName,
                                                     String indivFileName)
    {
        if (teamScheduleName == null || teamScheduleName.length() == 0 ||
            indivExportedName == null || indivExportedName.length() == 0 ||
            indivFileName == null || indivFileName.length() == 0) return false;
        //*debug*/ System.out.println("addIndivScheduleToTeamSchedule");
        //*debug*/ System.out.println("teamScheduleName="+teamScheduleName);
        //*debug*/ System.out.println("indivExportedName="+indivExportedName);
        //*debug*/ System.out.println("indivFileName="+indivFileName);

        // calculate the import prefix
        String projectID = getValue("Project_ID");
        if (projectID == null || projectID.length() == 0) return false;
        String importPrefix = "/Import_"+projectID;
        //*debug*/ System.out.println("importPrefix="+importPrefix);

        // import all data.
        DataImporter.refreshPrefix(importPrefix);
        //*debug*/ System.out.println("refreshPrefix done");

        // calculate the name of the import directory.
        String teamDirectory = getValue("Team_Directory");
        if (teamDirectory == null || teamDirectory.length() == 0) return false;
        teamDirectory = teamDirectory.replace('\\', '/');
        String importDir = teamDirectory+"/data/"+projectID;
        //*debug*/ System.out.println("importDir="+importDir);

        // calculate the full name of the imported file.
        indivFileName = indivFileName.replace('\\', '/');
        int slashPos = indivFileName.lastIndexOf('/');
        if (slashPos == -1) return false;
        String importedFile =
            importDir + "/" + indivFileName.substring(slashPos+1);
        //*debug*/ System.out.println("importedFile="+importedFile);
        File f = new File(importedFile);

        // calculate the fully qualified name of the imported schedule.
        String dataPrefix = null;
        try {
            dataPrefix = DataImporter.getPrefix(importPrefix, f);
        } catch (IOException ioe) {
            return false;
        }
        //*debug*/ System.out.println("dataPrefix="+dataPrefix);
        String indivSchedPath = dataPrefix + indivExportedName;
        //*debug*/ System.out.println("indivSchedPath="+indivSchedPath);
        //*debug*/ SimpleData sd =
        //*debug*/     getDataRepository().getSimpleValue(indivSchedPath);
        //*debug*/ String sdStr = (sd == null ? null : sd.format());
        //*debug*/ System.out.println("indivSchedPath.value="+sdStr);

        // add the imported schedule to the team schedule.
        return addScheduleToRollup(teamScheduleName, indivSchedPath);
    }

    protected boolean addScheduleToRollup(String teamScheduleName,
                                          String indivSchedPath) {

        EVTaskList rollup = EVTaskList.openExisting
            (teamScheduleName, getDataRepository(),
             getPSPProperties(), getObjectCache(), false);
        if (!(rollup instanceof EVTaskListRollup)) {
            //*debug*/ System.out.println("rollup not an EVTaskListRollup");
            return false;
        }
        if (!rollup.addTask(indivSchedPath, getDataRepository(),
                            getPSPProperties(), getObjectCache(), false)) {
            //*debug*/ System.out.println("addTask failed");
            return false;
        }

        rollup.save();
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
            PSPProperties props = getPSPProperties();
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
        printRedirect(IND_NODE_URL + "?errMsg=" + URLEncoder.encode(errMsg));
    }

    /** Possibly store a default name for the project node */
    protected void maybeSetDefaultNodeName() {
        if (getValue(NODE_NAME) != null) return;

        String teamURL = getValue(TEAM_URL);
        if (teamURL == null) return;

        teamURL = URLDecoder.decode(teamURL);
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
        PSPProperties hierarchy = getPSPProperties();
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
        String fullName = nodeLocation + "/" + nodeName;
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
        String initials = getParameter("initials");
        if (initials == null || initials.trim().length() == 0) {
            printRedirect(IND_INITIALS_URL + "?missing");
            return;
        }

        initials = initials.trim();
        putValue(IND_INITIALS, initials);

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
            EVTaskListRollup.exists(data, scheduleName))
            printRedirect(IND_SCHEDULE_URL + "?duplicate");

        else
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

        teamURL = teamURL.trim();
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

    /** Display the page for an individual to select a schedule name */
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
        else
            localProjectName = getValue(NODE_LOCATION)+"/"+getValue(NODE_NAME);
        String indivInitials = getValue(IND_INITIALS);
        String scheduleName = getValue(IND_SCHEDULE);

        // Download team project information from the team dashboard.
        Document d = downloadTeamProjectInformation(teamURL);
        if (d == null) {
            printRedirect(IND_CONNECT_ERR_URL); return;
        }
        teamURL = buildTeamURLReference(teamURL, "teamIndex.shtm").toString();
        Element e = d.getDocumentElement();

        String projectID = e.getAttribute("Project_ID");
        String teamDirectory = e.getAttribute("Team_Directory");
        String teamDirectoryUNC = e.getAttribute("Team_Directory_UNC");
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
        String dataSubdir = File.separator+"data"+File.separator+ projectID;
        teamDirectory = resolveTeamDirectory(teamDirectory, teamDirectoryUNC,
                                             dataSubdir);
        if (teamDirectory == null)
            // the error page will already have been displayed by now,
            // so just abort on failure.
            return;

        // Check to see if the "team project" is in the same dashboard
        // instance as the "individual project."
        String remoteTimeStamp =e.getAttribute(TinyWebServer.TIMESTAMP_HEADER);
        String localTimeStamp = getTinyWebServer().getTimestamp();
        boolean isLocal = localTimeStamp.equals(remoteTimeStamp);

        // perform lots of other setup tasks.  Unlike the operation
        // above, these tasks should succeed 99.999% of the time.
        setPrefix(localProjectName);
        createIndivProject(indivTemplateID);
        saveIndivDataValues(projectID, teamURL, indivInitials, scheduleName,
                            teamDirectory, teamDirectoryUNC, isLocal);
        createIndivSchedule(scheduleName);
        exportIndivData();
        boolean joinSucceeded = true;
        if (isLocal)
            joinSucceeded = joinLocalTeamSchedule(teamURL, scheduleName);
        else
            joinSucceeded = joinTeamSchedule(teamURL, scheduleName);

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
            conn.connect();
            String serverTimeStamp = conn.getHeaderField
                (TinyWebServer.TIMESTAMP_HEADER);
            result = XMLUtils.parse(conn.getInputStream());
            result.getDocumentElement().setAttribute
                (TinyWebServer.TIMESTAMP_HEADER, serverTimeStamp);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return result;
    }

    /** Check to ensure that the team and data directories exist, and
     * resolve the team directory to something that will work for the
     * current user, if possible.  */
    protected String resolveTeamDirectory(String teamDirectory,
                                          String teamDirectoryUNC,
                                          String dataSubdir) {
        File f = new File(teamDirectory + dataSubdir);
        if (f.isDirectory()) return teamDirectory;

        // Try to find the data directory using the UNC path.
        if (teamDirectoryUNC == null) return null;
        NetworkDriveList networkDriveList = new NetworkDriveList();
        String altTeamDirectory =
            networkDriveList.fromUNCName(teamDirectoryUNC);
        if (altTeamDirectory == null) return null;
        f = new File(altTeamDirectory + dataSubdir);
        if (f.isDirectory()) return altTeamDirectory;

        putValue(DATA_DIR, teamDirectory + dataSubdir);
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
    protected void saveIndivDataValues
        (String projectID, String teamURL, String indivInitials,
         String scheduleName, String teamDirectory, String teamDirectoryUNC,
         boolean isLocal) {
        putValue("Project_ID", projectID);
        putValue("Team_URL", teamURL);
        putValue("Indiv_Initials", indivInitials);
        putValue("Project_Schedule_Name", scheduleName);
        putValue("Team_Directory", teamDirectory);
        if (teamDirectoryUNC != null)
            putValue("Team_Directory_UNC", teamDirectoryUNC);
        if (isLocal)
            putValue("EXPORT_FILE", ImmutableStringData.EMPTY_STRING);
    }

    protected void createIndivSchedule(String scheduleName) {
        EVTaskListData schedule = new EVTaskListData
            (scheduleName, getDataRepository(), getPSPProperties(), false);
        schedule.addTask(getPrefix(), getDataRepository(),
                         getPSPProperties(), null, false);
        schedule.save();
    }

    protected void exportIndivData() {
        DashController.exportData(getPrefix());
    }

    protected boolean joinTeamSchedule(String teamURL, String scheduleName) {
        String exportedScheduleName = ImportExport.exportedScheduleName
            (getDataRepository(), scheduleName);
        String exportFileName = getValue("EXPORT_FILE");

        String urlStr = "setup/wizard.class?"+PAGE+"="+JOIN_TEAM_SCHED_PAGE+
            "&scheduleName=" + URLEncoder.encode(exportedScheduleName) +
            "&fileName=" + URLEncoder.encode(exportFileName);
        URL u = buildTeamURLReference(teamURL, urlStr);
        try {
            URLConnection conn = u.openConnection();
            conn.connect();
            int status = ((HttpURLConnection) conn).getResponseCode();
            TinyWebServer.slurpContents(conn.getInputStream(), true);
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
            URLDecoder.decode(teamURL.substring(slashPos, sslashPos));

        String dataName = DataRepository.createDataName
            (teamProjectPrefix, "Project_Schedule_Name");
        SimpleData schedNameData =
            getDataRepository().getSimpleValue(dataName);
        if (schedNameData == null) return false;
        String teamScheduleName = schedNameData.format();

        // add the individual's schedule to the team schedule.
        return addScheduleToRollup(teamScheduleName, indivScheduleName);
    }

    protected void showIndivSuccessPage(boolean joinSucceeded) {
        String prefix = URLEncoder.encode(getPrefix());
        if (joinSucceeded)
            printRedirect(IND_SUCCESS_URL + "?hierarchyPath=" + prefix);
        else
            printRedirect(IND_SUCCESS_URL + "?schedProblem&hierarchyPath="+
                          prefix);
    }

}
