
import pspdash.*;
import pspdash.data.DataRepository;
import pspdash.data.ImmutableStringData;
import pspdash.data.ImmutableDoubleData;
import pspdash.data.SimpleData;

import java.io.*;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

public class wizard extends TinyCGIBase {

    private static final String PAGE = "page";
    private static final String WELCOME_PAGE = "welcome";
    private static final String TYPE_PAGE = "type";
    private static final String PROCESS_PAGE = "process";
    private static final String TEAM_DIR_PAGE = "teamDir";
    private static final String TEAM_SCHEDULE_PAGE = "teamSchedule";
    private static final String TEAM_CONFIRM_PAGE = "teamConfirm";
    private static final String TEAM_CLOSE_HIERARCHY_PAGE = "teamCloseHier";

    private static final String WELCOME_URL = "welcome.shtm";
    private static final String TYPE_URL = "selectType.shtm";
    private static final String PROCESS_URL = "selectProcess.shtm";
    private static final String TEAM_DIR_URL = "teamDirectory.shtm";
    private static final String TEAM_SCHEDULE_URL = "teamSchedule.shtm";
    private static final String TEAM_CONFIRM_URL = "teamConfirm.shtm";
    private static final String TEAM_CLOSE_HIERARCHY_URL =
        "teamCloseHierarchy.shtm";
    private static final String TEAM_SUCCESS_URL = "teamSuccess.shtm";


    private static final String TEAM_PID = "setup//Process_ID";
    private static final String TEAM_PID_LIST = "setup//Process_ID_List";
    private static final String TEAM_PROC_NAME = "setup//Process_Name";
    private static final String SUGG_TEAM_DIR = "setup//Suggested_Team_Dir";
    private static final String TEAM_DIR = "setup//Team_Dir";
    private static final String TEAM_SCHEDULE = "setup//Team_Schedule";

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
        if (page == null || WELCOME_PAGE.equals(page))
            showWelcomePage();
        else if (TYPE_PAGE.equals(page))          handleTypePage();
        else if (PROCESS_PAGE.equals(page))       handleProcessPage();
        else if (TEAM_DIR_PAGE.equals(page))      handleTeamDirPage();
        else if (TEAM_SCHEDULE_PAGE.equals(page)) handleTeamSchedulePage();
        else if (TEAM_CLOSE_HIERARCHY_PAGE.equals(page))
                                                  handleTeamCloseHierPage();
        else if (TEAM_CONFIRM_PAGE.equals(page))  handleTeamConfirmPage();
        else                                      showWelcomePage();

        this.out.flush();

        // clear out the networkDriveList in case we created it.
        networkDriveList = null;
    }

    /** Send an HTTP redirect command to the browser, sending it to
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
        String dataName = data.createDataName(prefix, name);
        data.putValue(dataName, dataValue);
    }

    /** Get a value from the data repository. */
    protected String getValue(String name) {
        DataRepository data = getDataRepository();
        String prefix = getPrefix();
        String dataName = data.createDataName(prefix, name);
        SimpleData d = data.getSimpleValue(dataName);
        return (d == null ? null : d.format());
    }

    /** Display the welcome page */
    protected void showWelcomePage() {
        printRedirect(WELCOME_URL);
    }

    /** Handle values posted from the setup type page */
    protected void handleTypePage() {
        if (parameters.get("createTeamProject") != null) {
            showTeamProcessesPage();

        } else if (parameters.get("joinTeamProject") != null) {
            // TODO
            printRedirect(TYPE_URL);
        }
    }

    /** Display the team process selection page */
    protected void showTeamProcessesPage() {
        // get a list of all the team processes
        Map processes = getTeamProcesses();
        Map.Entry e;

        // If there is only one process installed, skip directly
        // to the team directory page.
        if (processes.size() == 1) {
            e = (Map.Entry) processes.entrySet().iterator().next();
            putValue(TEAM_PID, (String) e.getKey());
            putValue(TEAM_PROC_NAME, (String) e.getValue());
            showTeamDirPage();
            return;
        }

        // Save information about the available processes into the
        // data repository.
        Iterator i = processes.entrySet().iterator();
        String pidList = ";";
        while (i.hasNext()) {
            e = (Map.Entry) i.next();
            String pid = (String) e.getKey();
            String processName = (String) e.getValue();
            pidList = pidList + pid + ";";
            putValue("setup//Process_Name{"+pid+"}", processName);
        }
        putValue(TEAM_PID_LIST, pidList);

        // display the process selection page.
        printRedirect(PROCESS_URL);
    }

    /** Get a list of all the team processes installed in the dashboard.
     * @return a Map mapping process IDs to process names
     */
    protected Map getTeamProcesses() {
        // get a list of all the processes in the dashboard
        Map templates = DashController.getTemplates();
        // filter out process templates which are not "team roots"
        Iterator i = templates.keySet().iterator();
        while (i.hasNext()) {
            String id = (String) i.next();
            if (!id.endsWith("/TeamRoot"))
                i.remove();
        }
        return templates;
    }

    /** Handle values posted from the process selection page */
    protected void handleProcessPage() {
        String selectedProcess = getParameter("processID");
        String selectedProcessName =
            getParameter(selectedProcess + "_Full_Name");
        putValue(TEAM_PID, selectedProcess);
        putValue(TEAM_PROC_NAME, selectedProcessName);
        showTeamDirPage();
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
        System.out.println("ensureNetworkDrive("+filename+")");

        if (filename == null) return false;

        String driveLetter = getDriveLetter(filename);
        //FIXME - uncomment the next line.
        //if ("C".equalsIgnoreCase(driveLetter)) return false;

        if (!getNetworkDriveList().wasSuccessful()) return true;

        return getNetworkDriveList().isNetworkDrive(driveLetter);
    }

    private boolean isWindows() {
        return (System.getProperty("os.name").indexOf("Windows") != -1);
    }
    private String getDriveLetter(String filename) {
        if (!isWindows())
            return null;

        if (filename.length() < 2) return null;
        if (filename.charAt(1) != ':') return null;
        return filename.substring(0, 1);
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
        if (!createTeamDirs(teamDirectory, projectID))
            return;             // abort on failure.

        // perform lots of other setup tasks.  Unlike the operation
        // above, these tasks should succeed 99.999% of the time.
        tryToCopyProcessJarfile (processJarFile, teamDirectory);
        saveTeamDataValues (teamDirectory, projectID, teamSchedule);
        saveTeamSettings (teamDirectory, projectID);
        createTeamSchedule (teamSchedule);

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
        String driveLetter = getDriveLetter(filename);
        if (driveLetter == null) return null;

        if (!getNetworkDriveList().wasSuccessful()) return null;
        String uncPrefix = getNetworkDriveList().getUNCName(driveLetter);
        String result = uncPrefix + filename.substring(2);
        File testFile = new File(result);
        if (testFile.isDirectory())
            return result;
        else
            return null;
    }

    private static final String TEMPLATE_PATH = "templates.directory";
    private static final String IMPORT_DIRS = "import.directories";
    private static final String EV_ROLLUP = "ev.enableRollup";
    private static final String HTTP_PORT = "http.port";
    protected void saveTeamSettings(String teamDirectory, String projectID) {
        // rewrite the team directory into "settings" filename form.
        teamDirectory = teamDirectory.replace('\\', '/');
        if (teamDirectory.endsWith("/"))
            teamDirectory = teamDirectory.substring
                (0, teamDirectory.length()-1);

        // calculate the new template directory, and add it to the
        // template path
        String templateDir = teamDirectory + "/Templates";
        String templatePath = Settings.getVal(TEMPLATE_PATH);
        if (templatePath == null)
            InternalSettings.set(TEMPLATE_PATH, templateDir);

        else if (!templateSettingContainsDir(templatePath, templateDir)) {
            templatePath = templateDir + ";" + templatePath;
            InternalSettings.set(TEMPLATE_PATH, templatePath);
        }

        // calculate the new import instruction, and add it to the
        // import list
        String importInstr = ("Import_"+projectID+ "=>"+
                              teamDirectory+"/data/"+projectID);
        String imports = Settings.getVal(IMPORT_DIRS);
        if (imports == null)
            InternalSettings.set(IMPORT_DIRS, importInstr);
        else
            InternalSettings.set(IMPORT_DIRS, imports + "|" + importInstr);

        // enable earned value rollups.
        InternalSettings.set(EV_ROLLUP, "true");

        // listen on a repeatable port.
        String port = Settings.getVal(HTTP_PORT);
        if (port == null) {
            int portNum = getAvailablePort();
            InternalSettings.set(HTTP_PORT, Integer.toString(portNum));
            try {
                // start listening on that port.
                getTinyWebServer().addExtraPort(portNum);
            } catch (IOException ioe) {}
        }

    }
    private boolean templateSettingContainsDir(String setting, String dir) {
        setting = ";" + setting + ";";
        dir     = ";" + dir     + ";";
        return setting.indexOf(dir) != -1;
    }

    private int getAvailablePort() {
        for (int i = 0;   i < PORT_PATTERNS.length;   i++)
            for (int j = 2;   j < 10;   j++)
                if (isPortAvailable(i*j))
                    return i*j;
        return 3000;
    }
    private int[] PORT_PATTERNS = {
        1000, 1111, 1001, 1010, 1100, 1011, 1101, 1110 };
    private boolean isPortAvailable(int port) {
        if (port < 1024) return false;
        boolean successful = false;
        try {
            ServerSocket a = new ServerSocket(port-1);
            ServerSocket b = new ServerSocket(port);
            successful = true;
            a.close();
            b.close();
        } catch (IOException ioe) {}
        return successful;
    }

    protected void createTeamSchedule(String scheduleName) {
        EVTaskListRollup rollup = new EVTaskListRollup
            (scheduleName, getDataRepository(),
             getPSPProperties(), getObjectCache(), false);
        rollup.save();

        // TODO: create a top-down schedule as well.
    }

    protected void alterTemplateID(String teamPID) {
        DashController.alterTemplateID(getPrefix(), TEAM_STUB_ID, teamPID);
    }


    /** On Windows systems, this class compiles a list of drive
     * letters that are mapped to network drives, along with the UNC
     * names they are mapped to.
     */
    private class NetworkDriveList implements Runnable {

        private volatile boolean successful = false;
        private Map networkDrives = new TreeMap();
        private volatile Process subprocess = null;

        public NetworkDriveList() {
            Thread t = new Thread(this);
            t.setDaemon(true);
            t.start();
            try {
                t.join(1000);
            } catch (InterruptedException ie) {}
            if (successful == false)
                subprocess.destroy();
            subprocess = null;
        }

        public void run() {
            try {
                subprocess = Runtime.getRuntime().exec("net use");
                BufferedReader in = new BufferedReader
                    (new InputStreamReader(subprocess.getInputStream()));
                String line;
                boolean sawHeader = false;
                while ((line = in.readLine()) != null) {
                    if (!sawHeader && line.startsWith("-----"))
                        sawHeader = true;
                    else if (sawHeader) {
                        int pos = line.indexOf(':');
                        if (pos < 2) continue;
                        if (" \t".indexOf(line.charAt(pos-2)) == -1) continue;
                        String driveLetter = line.substring(pos-1, pos);
                        driveLetter = driveLetter.toUpperCase();
                        pos = line.indexOf('\\', pos);
                        if (pos == -1) continue;
                        line = line.substring(pos);
                        pos = line.indexOf('\t');
                        if (pos != -1) line = line.substring(0, pos);
                        pos = line.indexOf(' ');
                        if (pos != -1) line = line.substring(0, pos);
                        networkDrives.put(driveLetter, line);
                    }
                }
                System.out.println("NetworkDriveList:");
                System.out.println(networkDrives);
                successful = true;
            } catch (Exception e) {}
        }

        public boolean wasSuccessful() { return successful; }
        public boolean isNetworkDrive(String driveLetter) {
            if (driveLetter == null) return false;
            driveLetter = driveLetter.toUpperCase();
            return networkDrives.containsKey(driveLetter);
        }
        public String getUNCName(String driveLetter) {
            if (driveLetter == null) return null;
            driveLetter = driveLetter.toUpperCase();
            return (String) networkDrives.get(driveLetter);
        }
    }

}
