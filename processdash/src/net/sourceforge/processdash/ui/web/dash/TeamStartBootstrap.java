// Copyright (C) 2002-2014 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.dash;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.ImmutableStringData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.process.ScriptID;
import net.sourceforge.processdash.templates.DashPackage;
import net.sourceforge.processdash.templates.DashPackage.InvalidDashPackage;
import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.NetworkDriveList;
import net.sourceforge.processdash.util.ObjectCounter;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;




/** This script bootstraps the startup of a team process setup wizard.
 *
 * It interacts with the user just long enough to determine which team
 * process definition will be used to perform a team project, then
 * hands control over to the startup wizard for that process.
 *
 * Including this bootstrap portion in the main dashboard distribution
 * allows individuals to join a team project, even if they have never
 * been a part of one before (and thus don't have a team project
 * definition in their template path).
 */
public class TeamStartBootstrap extends TinyCGIBase {

    public static final String TEAM_STUB_ID = "TeamProjectStub";
    public static final String TEAM_START_URI = "/dash/teamStart.class";
    public static final String TARGET_PARENT_PARAM = "parent";

    private static final String PAGE = "page";

    // Information for the wizard's "welcome" page.
    private static final String WELCOME_PAGE = "welcome";
    private static final String WELCOME_URL = "teamStartWelcome.shtm";
    // Information for the page which asks the user what type of
    // project they wish to create.
    private static final String TEST_TYPE_PAGE = "testType";
    private static final String TYPE_PAGE = "type";
    private static final String TYPE_URL = "teamStartType.shtm";
    // Information for the page which asks the team leader for the desired
    // name and location of the team project.
    private static final String NODE_PAGE = "teamSelectNode";
    private static final String NODE_URL = "teamStartTeamNode.shtm";
    private static final String NODE_BROWSE_PAGE = "teamBrowseFolder";
    // Information for the page to report an error in creating the project node
    private static final String NODE_ERR_PAGE = "nodeError";
    private static final String NODE_ERR_URL = "teamStartTeamNodeError.shtm";
    // Information for the page which asks the team leader which team
    // process they wish to use.
    private static final String PROCESS_PAGE = "process";
    private static final String SHOW_PROCESS_PAGE = "showProc";
    private static final String PROCESS_URL = "teamStartProcess.shtm";
    // Information for the page which asks an individual for the URL
    // of the team project.
    private static final String TEAM_URL_PAGE = "teamURL";
    private static final String SHOW_URL_PAGE = "showURL";
    private static final String TEAM_URL_URL = "teamStartTeamURL.shtm";
    // Information for the page which tells the user they are in read only mode
    private static final String READ_ONLY_URL = "teamStartReadOnly.shtm";



    private static final String NODE_LOCATION = "setup//Node_Location";
    private static final String NODE_NAME = "setup//Node_Name";
    private static final String TEAM_PID = "setup//Process_ID";
    private static final String TEAM_PID_LIST = "setup//Process_ID_List";
    private static final String TEAM_PROC_NAME = "setup//Process_Name";
    private static final String TEAM_URL = "setup//Team_URL";
    private static final String TEMPLATE_ID = "setup//Template_ID";
    private static final String PACKAGE_ID = "setup//Package_ID";
    private static final String PACKAGE_VERSION = "setup//Package_Version";
    private static final String TEMPLATE_PATH = "setup//Template_Path";
    private static final String TEMPLATE_UNC = "setup//Template_Path_UNC";
    private static final String CONTINUATION_URI = "setup//Continuation_URI";
    private static final String RELAX_PATH_REQ = "setup//Relax_Path_Reqt";
    private static final String PROJECT_ID = "Project_ID";
    private static final String ALL_JOINING_DATA = "setup//Joining_Data";
    private static final String IN_PROGRESS_URI = "setup//In_Progress_URI";

    // value indicating we should help an individual join a team project
    private static final String JOIN_PAGE = "join";
    private static final String JOIN_ERROR_URL = "teamStartJoinError.shtm";
    private static final String JOIN_TEAM_ERROR_URL = "teamStartJoinErrorTeam.shtm";
    private static final String JOIN_VERIFY_URL = "teamStartJoinVerify.shtm";

    private static Resources resources = Resources.getDashBundle("TeamStart");

    private static final String TEMPLATE_PATH_FORCE = "alwaysAdd";
    private static final String TEMPLATE_PATH_UPDATE = "addIfNewer";
    private static final String TEMPLATE_PATH_NEVER = "neverAdd";

    private static final Logger logger = Logger.getLogger(
            TeamStartBootstrap.class.getName());



    protected void writeHeader() {}
    protected void writeContents() {}
    public void service(InputStream in, OutputStream out, Map env)
        throws IOException
    {
        DashController.checkIP(env.get("REMOTE_ADDR"));

        super.service(in, out, env);
        if ("POST".equalsIgnoreCase((String) env.get("REQUEST_METHOD")))
            parseFormData();

        String page = getParameter(PAGE);
        if (page == null)                         showWelcomePage();
        else if (WELCOME_PAGE.equals(page))       showWelcomePage();
        else if (Settings.isReadOnly())           showReadOnlyPage();
        else if (TEST_TYPE_PAGE.equals(page))     testDatasetType();
        else if (TYPE_PAGE.equals(page))          handleTypePage();
        else if (NODE_PAGE.equals(page))          handleTeamNodePage();
        else if (NODE_BROWSE_PAGE.equals(page))   showBrowseFolderPage();
        else if (SHOW_PROCESS_PAGE.equals(page))  showTeamProcessesPage();
        else if (PROCESS_PAGE.equals(page))       handleProcessPage();
        else if (NODE_ERR_PAGE.equals(page))      handleNodeErrRetryPage();

        else if (SHOW_URL_PAGE.equals(page))      showTeamURLPage();
        else if (TEAM_URL_PAGE.equals(page))      handleTeamURLPage();
        else if (JOIN_PAGE.equals(page))          handleJoinPage();

        this.out.flush();
    }

    /** Send an HTTP redirect command to the browser, sending it to the
     *  relative URI named by filename. */
    protected void printRedirect(String filename) {
        out.print("Location: ");
        out.print(filename);
        out.print("\r\n\r\n");
    }

    protected void printRedirect(String prefix, String filename) {
        if (prefix != null)
            filename = WebServer.urlEncodePath(prefix) + "/" + filename;
        printRedirect(filename);
    }

    /** Save a value into the data repository. */
    protected void putValue(String name, String value) {
        if (value == null) value = "";
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

    /** Display the welcome page */
    protected void showWelcomePage() {
        String desiredLocation = getParameter(TARGET_PARENT_PARAM);
        if (StringUtils.hasValue(desiredLocation))
            putValue(NODE_LOCATION, desiredLocation);

        String inProgressUri = getValue(IN_PROGRESS_URI);
        if (StringUtils.hasValue(inProgressUri))
            printRedirect(inProgressUri);
        else
            printRedirect(WELCOME_URL);
    }

    /** Display the read-only error page */
    protected void showReadOnlyPage() {
        printRedirect(READ_ONLY_URL);
    }

    /** Test the type of this dataset and redirect accordingly */
    private void testDatasetType() {
        if (Settings.isHybridMode())
            // if this is a legacy-hybrid dashboard, display the type page to
            // ask the user whether they want to create or join a project.
            printRedirect(TYPE_URL);

        else if (Settings.isTeamMode())
            // in a team dashboard, start the process to create a team project
            maybeShowTeamNodePage();

        else
            // in a personal dashboard, prompt for the team project URL.
            showTeamURLPage();
    }

    /** Handle values posted from the setup type page */
    protected void handleTypePage() {
        if (parameters.get("createTeamProject") != null) {
            maybeShowTeamNodePage();

        } else if (parameters.get("joinTeamProject") != null) {
            showTeamURLPage();
        }
    }


    /** Check to see if we need to prompt for the name of this team project */
    protected void maybeShowTeamNodePage() {
        if (prefixNamesTeamProjectStub())
            showTeamProcessesPage();

        maybeSetDefaultProjectPath();
        printRedirect(NODE_URL);
    }

    private boolean prefixNamesTeamProjectStub() {
        return pathNamesTeamProjectStub(getPrefix());
    }

    private boolean pathNamesTeamProjectStub(String path) {
        DashHierarchy hierarchy = getPSPProperties();
        PropertyKey key = hierarchy.findExistingKey(path);
        String templateID = hierarchy.getID(key);
        return TEAM_STUB_ID.equals(templateID);
    }

    private void maybeSetDefaultProjectPath() {
        // Look for an existing, unused stub in the hierarchy.  If one is
        // found, use its name and location and exit.  Users will still be
        // able to overwrite these values; but this helps them to pick up
        // where they left off if they got confused.
        PropertyKey stub = findUnusedTeamProjectStub(getPSPProperties(),
            PropertyKey.ROOT);
        if (stub != null) {
            putValue(NODE_NAME, stub.name());
            putValue(NODE_LOCATION, stub.getParent().path());
            return;
        }

        // if the prefix is not set, set it to the localized default location.
        if (!StringUtils.hasValue(getValue(NODE_LOCATION))) {
            putValue(NODE_LOCATION, getDefaultProjectLocation());
        }

        // if the prefix/name points to a project that already exists (a common
        // pattern if the team leader is creating several projects in
        // succession), clear the name.
        String nodePath = getTeamNodePathOrErrToken();
        if (!nodePath.startsWith("/")) {
            putValue(NODE_NAME, (SimpleData) null);
        }
    }

    private PropertyKey findUnusedTeamProjectStub(DashHierarchy hier,
            PropertyKey node) {
        if (TEAM_STUB_ID.equals(hier.getID(node))) {
            return node;

        } else {
            for (int i = hier.getNumChildren(node);  i-- > 0; ) {
                PropertyKey result = findUnusedTeamProjectStub(hier,
                    hier.getChildKey(node, i));
                if (result != null)
                    return result;
            }
            return null;
        }
    }


    /** Display a page that allows the user to browse for the node where the
     * team project should be created. */
    protected void showBrowseFolderPage() {
        super.writeHeader();

        out.println("<html><head>");
        out.println("<title>Select Project Folder</title>");
        out.println(HTMLUtils.cssLinkHtml("teamStart.css"));
        out.println("<script>");
        out.println("    function save(path) {");
        out.println("        self.opener.save(path);");
        out.println("        self.close();");
        out.println("    }");
        out.println("</script>");
        out.println("</head><body>");
        out.println("<p><b>Select Project Folder:</b></p>");
        out.println("<form><table>");
        printFolders(getPSPProperties(), PropertyKey.ROOT, 0);
        out.println("</table></form></body></html>");
    }

    private void printFolders(DashHierarchy hier, PropertyKey node, int depth) {
        // we only want to display entries for plain nodes, not projects.
        // if this node has a template ID, prune this node from the tree.
        String templateId = hier.getID(node);
        if (StringUtils.hasValue(templateId))
            return;

        // calculate information about this node for display
        int indent = depth * 18;
        String prefix = node.path();
        prefix = StringUtils.findAndReplace(prefix, "\\", "\\\\");
        prefix = StringUtils.findAndReplace(prefix, "'", "\\'");
        prefix = StringUtils.findAndReplace(prefix, "\"", "\\\"");
        String name = (depth == 0 ? "No Folder" : node.name());

        // print a row with a link for this hierarchy node.
        out.print("<tr><td style='padding-left: " + indent + "px'>");
        out.print("<img src='/Images/node.png'>&nbsp;");
        out.print("<a class='plain' href='#' onclick='save(&quot;"
                + HTMLUtils.escapeEntities(prefix) + "&quot;); return false'>");
        out.print(HTMLUtils.escapeEntities(name));
        out.println("</a></td></tr>");

        // recurse over children
        for (int i = 0; i < hier.getNumChildren(node); i++)
            printFolders(hier, hier.getChildKey(node, i), depth + 1);
    }


    /** Handle the values posted from the team node selection page. */
    protected void handleTeamNodePage() {
        // retrieve and save values
        putValue(NODE_NAME, getParameter("Node_Name"));
        putValue(NODE_LOCATION, tweakLocation(getParameter("Node_Location")));

        // validate the values. If they are acceptable, proceed.  (If the
        // values are unacceptable, we will have already redirected to an
        // appropriate error page.)
        if (validateTeamNodeValues() != null)
            showTeamProcessesPage();
    }

    private String tweakLocation(String nodeLocation) {
        // make the node location canonical
        nodeLocation = DashHierarchy.scrubPath(nodeLocation);

        // if the node location is missing, use the locale-specific default.
        String defaultProjectLocation = getDefaultProjectLocation();
        if (!StringUtils.hasValue(nodeLocation))
            return defaultProjectLocation;

        // If the location is relative, make it absolute. Resolve the name
        // relative to the locale-specific default folder unless it already
        // appears to be there.
        if (!nodeLocation.startsWith("/")) {
            nodeLocation = "/" + nodeLocation;
            if (!nodeLocation.startsWith(defaultProjectLocation))
                nodeLocation = defaultProjectLocation + nodeLocation;
        }

        return nodeLocation;
    }

    private String getDefaultProjectLocation() {
        return "/" + resources.getString("Project");
    }

    private String validateTeamNodeValues() {
        String nodePath = getTeamNodePathOrErrToken();
        if (nodePath.startsWith("/")) {
            return nodePath;
        } else {
            printRedirect(NODE_URL + "?" + nodePath);
            return null;
        }
    }

    /**
     * Look at the node name and location, and make certain they refer to a
     * valid path. If so, the full path (which starts with a slash) will be
     * returned. If not, an error token (which does not start with a slash) will
     * be returned.
     */
    private String getTeamNodePathOrErrToken() {
        String nodeName = getValue(NODE_NAME);
        if (nodeName == null || nodeName.trim().length() == 0)
            return "nodeNameMissing";

        nodeName = nodeName.trim();
        if (nodeName.indexOf('/') != -1)
            return "nodeNameSlash";

        // compute the desired effective path.
        String nodeLocation = tweakLocation(getValue(NODE_LOCATION));
        String nodePath = nodeLocation + "/" + nodeName;

        // does a node already exist in the hierarchy with this path?
        DashHierarchy hier = getPSPProperties();
        PropertyKey key = hier.findExistingKey(nodePath);
        if (key != null) {
            // if the preexisting node is a team project stub, all is well.
            if (TEAM_STUB_ID.equals(hier.getID(key)))
                return nodePath;
            else
                return "duplicateName";
        } else {
            // look at the ancestors of the target path, and make certain that
            // they are all plain nodes
            key = hier.findClosestKey(nodePath);
            while (key != null) {
                if (StringUtils.hasValue(hier.getID(key)))
                    return "invalidParent";
                key = key.getParent();
            }
        }

        // path seems OK
        return nodePath;
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
            redirectToTeamSetupWizard((String) e.getKey());
            return;
        }

        if (processes.size() > 0) {
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

            if (getValue(TEAM_PID) == null) {
                // identify a suggested process, and write it into the repository.
                String suggestedPid = findMostCommonlyUsedTeamProcessId();
                if (suggestedPid == null)
                    suggestedPid = chooseMostCustomProcessId(processes.keySet());
                putValue(TEAM_PID, suggestedPid);
            }
        }

        // display the process selection page.
        printRedirect(PROCESS_URL);
    }

    /** Handle values posted from the process selection page */
    protected void handleProcessPage() {
        String selectedProcess = getParameter("processID");
        String selectedProcessName =
            getParameter(selectedProcess + "_Full_Name");
        putValue(TEAM_PID, selectedProcess);
        putValue(TEAM_PROC_NAME, selectedProcessName);
        redirectToTeamSetupWizard(selectedProcess);
    }

    /** Handle the click when the user asks to retry after a failed project
     * creation */
    private void handleNodeErrRetryPage() {
        String selectedProcess = getValue(TEAM_PID);
        if (!StringUtils.hasValue(selectedProcess))
            // if the process ID has disappeared, it would indicate that the
            // dashboard has been closed and reopened. Start the process over.
            maybeShowTeamNodePage();
        else
            redirectToTeamSetupWizard(selectedProcess);
    }

    /** The user is creating a team project for the given teamPID; redirect
     *  to its setup wizard.
     */
    protected void redirectToTeamSetupWizard(String teamPID) {
        String prefix = getTeamProjectPrefix();
        if (prefix != null)
            printRedirect(HTMLUtils.urlEncodePath(prefix) + "//" +
                      getTeamSetupWizardURL(teamPID) + "?page=team");
    }

    /**
     * Identify the prefix that should be used for the new team project. Verify
     * that a stub is in place at that location, or create one if necessary, and
     * return the path. If any problem is encountered, redirect to an
     * appropriate error page and return null.
     */
    private String getTeamProjectPrefix() {
        // if we have an active prefix and it points to a team project stub,
        // return that prefix.
        String projectPath = getPrefix();
        if (projectPath != null && pathNamesTeamProjectStub(projectPath))
            return projectPath;

        // check and see if the user has provided a valid name and location
        // for the project.
        projectPath = validateTeamNodeValues();
        if (projectPath == null)
            // if the name/location is bad, just return.  An error screen will
            // have already been displayed at this point.
            return null;

        else if (pathNamesTeamProjectStub(projectPath)
                || DashController.alterTemplateID(projectPath, null,
                    TEAM_STUB_ID)) {
            // if the name/location points to a stub, or if we were able to
            // create a stub, store values and return the path.
            String processId = getValue(TEAM_PID);
            String processName = getValue(TEAM_PROC_NAME);
            parameters.put("hierarchyPath", projectPath);
            putValue(TEAM_PID, processId);
            putValue(TEAM_PROC_NAME, processName);
            return projectPath;
        }

        // There is a problem - most likely the hierarchy editor is open.
        // Display an error message and exit.
        printRedirect(NODE_ERR_URL);
        return null;
    }

    /** Get a list of all the team processes installed in the dashboard.
     * @return a Map mapping process IDs to process names
     */
    protected Map getTeamProcesses() {
        // get a list of all the processes in the dashboard
        Map templates = DashController.getTemplates();
        Iterator i = templates.keySet().iterator();
        while (i.hasNext()) {
            String id = (String) i.next();
            // filter out process templates which are not "team roots"
            if (!id.endsWith("/TeamRoot"))
                i.remove();
            // filter out process templates which don't have a setup wizard.
            else if (getTeamSetupWizardURL(id) == null)
                i.remove();
        }
        return templates;
    }

    /** Look at past projects to see if a particular team process has been
     * used often.  If so, return that process ID.
     */
    private String findMostCommonlyUsedTeamProcessId() {
        ObjectCounter<String> counts = new ObjectCounter<String>();
        countProcessUsage(counts, getPSPProperties(), PropertyKey.ROOT);
        return chooseMostCustomProcessId(counts.getMostCommonObjects());
    }

    private void countProcessUsage(ObjectCounter<String> counts,
            DashHierarchy hier, PropertyKey node) {
        String templateId = hier.getID(node);
        if (templateId != null && templateId.endsWith("/TeamRoot")) {
            counts.add(templateId);
        } else {
            for (int i = hier.getNumChildren(node);  i-- > 0;)
                countProcessUsage(counts, hier, hier.getChildKey(node, i));
        }
    }

    private String chooseMostCustomProcessId(Iterable<String> pids) {
        String result = null;
        if (pids != null) {
            for (String onePid : pids) {
                int resultPref = getProcessCustomizationRating(result);
                int onePref = getProcessCustomizationRating(onePid);
                result = (resultPref > onePref ? result : onePid);
            }
        }
        return result;
    }
    private int getProcessCustomizationRating(String templateId) {
        if (templateId == null) return -1;
        if (templateId.startsWith("PDSSD/")) return 0; // open source process
        if (templateId.startsWith("TSP/")) return 1;  // the TSP process
        return 2;  // a custom process
    }


    /** Determine the URL of the setup wizard for a given team process.
     *  @return null if a setup wizard cannot be located for the given
     *  process.
     */
    protected String getTeamSetupWizardURL(String processID) {
        Vector scripts = TemplateLoader.getScriptIDs(processID, null);
        if (scripts == null) return null;
        scripts.add(new ScriptID(processID + "-template.xml", null, null));
        for (int i = scripts.size();   i-- > 0; ) {
            String scriptURL = ((ScriptID) scripts.get(i)).getScript();
            int pos = scriptURL.lastIndexOf('/');
            if (pos < 1) continue;
            String processDir = scriptURL.substring(0, pos);
            String wizardURL = processDir + "/setup/wizard.class";
            URL u = resolveURL(wizardURL);
            if (u != null)
                return wizardURL;
        }
        return null;
    }

    /** Display the page asking the individual for the URL of the team
     * project. */
    protected void showTeamURLPage() {
        printRedirect(TEAM_URL_URL);
    }

    /** Handle values posted from the "team project url" page */
    protected void handleTeamURLPage() {
        String teamURL = getParameter("Team_URL");
        if (teamURL != null) putValue(TEAM_URL, teamURL);

        String errMsg = downloadTeamTemplateInfo(teamURL);
        if (errMsg == null)
            joinProject();
        else
            printRedirect(TEAM_URL_URL +
                          "?errMsg="+HTMLUtils.urlEncode(errMsg));
    }

    /** Contact the team dashboard and download information about the
     * process template in use by the project.
     * @return null on success; else an error message describing the
     * problem encountered.
     */
    protected String downloadTeamTemplateInfo(String teamURL) {
        // Ensure they entered a team URL.
        if (teamURL == null || teamURL.trim().length() == 0)
            return resources.getHTML("Errors.Missing_URL");

        // Make certain the team URL is a valid URL.  Note that we
        // should be able to work with the URL to just about any page
        // in the team project! So whether the team leader gives
        // people the URL to the "join" page, or the URL to the "table
        // of contents" page, or even the URL to some other obscure
        // page for the project, we should be able to derive the URL
        // we need.
        teamURL = teamURL.trim();
        if (!teamURL.startsWith("http://") && !teamURL.startsWith("https://"))
            return resources.getHTML("Errors.Invalid_Team_URL");
        if (teamURL.endsWith(".do") || teamURL.indexOf(".do?") != -1) {
            int pos = teamURL.lastIndexOf('/');
            teamURL = teamURL.substring(0, pos+1) + "joinXml.do";
        } else {
            teamURL = StringUtils.findAndReplace(teamURL, "/+/", "//");
            int pos = teamURL.indexOf("//", 7);
            if (pos != -1) pos = teamURL.indexOf('/', pos+2);
            if (pos == -1)
                return resources.getHTML("Errors.Invalid_Team_URL");
            teamURL = teamURL.substring(0, pos+1) + "setup/join.class?xml";
        }
        URL u = null;
        try {
            u = new URL(teamURL);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Caught exception creating team URL "
                    + teamURL, ioe);
            return resources.getHTML("Errors.Invalid_URL");
        }

        // Download an XML document containing the template information.
        Document doc = null;
        try {
            URLConnection conn = u.openConnection();
            conn.connect();
            doc = XMLUtils.parse(conn.getInputStream());
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not connect to team URL "+u, e);
            return resources.getHTML("Errors.Cannot_Connect_To_URL");
        }

        // Extract the relevant information from the XML document we
        // downloaded.
        Element e = doc.getDocumentElement();
        putValue(TEMPLATE_ID, e.getAttribute("Template_ID"));
        putValue(PACKAGE_ID, e.getAttribute("Package_ID"));
        putValue(PACKAGE_VERSION, e.getAttribute("Package_Version"));
        putValue(TEMPLATE_PATH, e.getAttribute("Template_Path"));
        putValue(TEMPLATE_UNC, e.getAttribute("Template_Path_UNC"));
        putValue(CONTINUATION_URI, e.getAttribute("Continuation_URI"));
        putValue(RELAX_PATH_REQ, e.getAttribute("Relax_Path_Reqt"));
        saveAllJoiningData(XMLUtils.getAttributesAsMap(e));

        return null;
    }


    /** Handle values posted from the "join team project" page */
    protected void handleJoinPage() {
        putValue(TEAM_URL, getParameter("Team_URL"));
        putValue(TEMPLATE_ID, getParameter("Template_ID"));
        putValue(PACKAGE_ID, getParameter("Package_ID"));
        putValue(PACKAGE_VERSION, getParameter("Package_Version"));
        putValue(TEMPLATE_PATH, getParameter("Template_Path"));
        putValue(TEMPLATE_UNC, getParameter("Template_Path_UNC"));
        putValue(CONTINUATION_URI, getParameter("Continuation_URI"));
        putValue(RELAX_PATH_REQ, getParameter("Relax_Path_Reqt"));
        saveAllJoiningData(parameters);
        joinProject();
    }

    private void saveAllJoiningData(Map values) {
        ListData l = null;
        if (values.containsKey(PROJECT_ID)) {
            l = new ListData();
            l.add(values);
        }
        putValue(ALL_JOINING_DATA, l);
    }


    /** Attempt to join a team project. */
    protected void joinProject() {
        // String teamURL = getValue(TEAM_URL);
        String templateID = getValue(TEMPLATE_ID);
        String packageID = getValue(PACKAGE_ID);
        String packageVersion = getValue(PACKAGE_VERSION);
        String templatePath = getValue(TEMPLATE_PATH);
        String templatePathUNC = getValue(TEMPLATE_UNC);
        String continuationURI = getValue(CONTINUATION_URI);
        String relaxPathReq = getValue(RELAX_PATH_REQ);
        boolean requirePath = !StringUtils.hasValue(relaxPathReq);

        String templatePathSetting = Settings.getVal(
            "teamJoin.templateSearchPathPhilosophy", TEMPLATE_PATH_UPDATE);
        if (templatePath == null || templatePath.trim().length() == 0) {
            requirePath = false;
            templatePathSetting = TEMPLATE_PATH_NEVER;
        } else if (!TEMPLATE_PATH_FORCE.equals(templatePathSetting)) {
            requirePath = false;
        }

        String errorMessage = null;
        File templateFile = resolveTemplateLocation(templatePath,
                templatePathUNC);

        if (!Settings.isPersonalMode()) {
            // This is a Team Dashboard, but it just received a joining request.
            // This probably means that we are incorrectly listening on port
            // 2468.  Enable team settings to fix the problem for the future,
            // then display an error page asking the user to restart.
            DashController.enableTeamSettings();
            InternalSettings.set("http.port", "3000");
            printRedirect(JOIN_TEAM_ERROR_URL);

        } else if (templateIsLoaded(templateID, packageID, packageVersion,
            templateFile, continuationURI, requirePath)) {
            // the template is already present in the dashboard.  Test to
            // make certain our dashboard can understand that template.
            errorMessage = testContinuation(getPrefix(), continuationURI);

            // If that went OK, redirect to the continuation URI.
            if (errorMessage == null)
                printRedirect(getPrefix(), continuationURI);

        } else if (TEMPLATE_PATH_NEVER.equals(templatePathSetting)
                || templateFile == null) {
            errorMessage = TEMPLATE_PATH_NEVER;

        } else {
            errorMessage = initiateTemplateLoad
                (templateID, templateFile, continuationURI);
            if (errorMessage == null)
                printRedirect(JOIN_VERIFY_URL);
        }

        if (errorMessage != null) {
            errorMessage = HTMLUtils.escapeEntities(errorMessage);
            errorMessage = HTMLUtils.urlEncode(errorMessage);
            printRedirect(JOIN_ERROR_URL + "?errMsg=" + errorMessage);
        }
    }


    private File resolveTemplateLocation(String templatePath,
            String templatePathUNC) {
        if (templatePath == null || templatePath.trim().length() == 0)
            return null;

        File f = new File(templatePath);
        if (!f.exists()) {
            if (templatePathUNC != null && templatePathUNC.length() > 0) {
                // Try to find the template file using the UNC path.
                NetworkDriveList networkDriveList = new NetworkDriveList();
                String altTemplatePath = networkDriveList.fromUNCName(
                        templatePathUNC);
                if (altTemplatePath != null)
                    f = new File(altTemplatePath);
                else
                    f = new File(templatePathUNC);
            }
        }

        try {
            f = f.getCanonicalFile();
        } catch (Exception e) {
            logger.log(Level.WARNING,
                    "Caught exception getting canonical file", e);
        }
        logger.finer("resolveTemplateLocation('" + templatePath + "', '"
                + templatePathUNC + "') = '" + f + "'");
        return f;
    }


    private boolean templateIsLoaded(String templateID,
                                     String packageID,
                                     String packageVersion,
                                     File templateJarfile,
                                     String continuationURI,
                                     boolean requireDirInSearchPath)
    {
        // if we have no loaded template with the given ID, return false.
        if (DashController.getTemplates().get(templateID) == null) {
            logger.log(Level.FINER, "No template found with ID {0}",
                    templateID);
            return false;
        }

        // check to see if the continuation URI is a valid resource.  If not,
        // return false.
        int pos = continuationURI.indexOf('?');
        if (pos != -1) continuationURI = continuationURI.substring(0, pos);
        pos = continuationURI.indexOf('#');
        if (pos != -1) continuationURI = continuationURI.substring(0, pos);
        logger.log(Level.FINER, "continuationURI={0}", continuationURI);
        URL url = resolveURL(continuationURI);
        logger.log(Level.FINER, "url={0}", url);
        if (url == null) return false;

        // if we're allowed to use local templates, and the team project gave
        // us a version number to compare against, check if we're up-to-date.
        if (requireDirInSearchPath == false
                && StringUtils.hasValue(packageID)
                && StringUtils.hasValue(packageVersion)
                && upgradeIsNeeded(packageID, packageVersion) == false)
            return true;

        // If we reach point, then one of the following is true:
        //  * we're requiring the directory to be added to the search path, or
        //  * the team process didn't tell us which version we need, or
        //  * we might need to upgrade by adding the template to our path.
        // Either way, we'll need the location of the template JAR file.  If
        // that JAR could not be located or does not exist, return false.
        if (templateJarfile == null || !templateJarfile.exists())
            return false;

        // Check to see if the template search path already includes the
        // directory containing the template JAR file.
        if (requireDirInSearchPath) {
            File templateJarDir = templateJarfile.getParentFile();
            if (!TemplateLoader.templateSearchPathContainsDir(templateJarDir)) {
                logger.log(Level.FINER, "The template search path does not "
                        + "contain the directory {0}", templateJarDir);
                return false;
            }
        }

        // Check to see if we have already loaded an up-to-date version of
        // the process contained in the JAR file.
        try {
            DashPackage pkg = getDashPackageForFile(templateJarfile);
            if (upgradeIsNeeded(pkg)) {
                logger.log(Level.FINEST, "Our currently installed version of "
                        + "the process JAR file is out of date.");
                return false;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Encountered error comparing process "
                    + "version numbers", e);
            return false;
        }

        // the template appears to be loaded, and meets all our criteria.
        logger.finer("Template is loaded");
        return true;
    }


    private URL resolveURL(String uri) {
        URL result = TemplateLoader.resolveURL(uri);
        if (result == null)
            result = TemplateLoader.resolveURL(uri + ".link");
        return result;
    }


    private DashPackage getDashPackageForFile(File templateFile)
            throws MalformedURLException, InvalidDashPackage {
        String jarURL = templateFile.toURI().toURL().toString();
        URL url = new URL("jar:" + jarURL + "!/Templates/");
        DashPackage dashPackage = new DashPackage(url);
        return dashPackage;
    }


    private boolean upgradeIsNeeded(DashPackage pkg) {
        String pkgId = pkg.id;
        String pkgVersion = pkg.version;
        return upgradeIsNeeded(pkgId, pkgVersion);
    }

    private boolean upgradeIsNeeded(String pkgId, String pkgVersion) {
        String instVersion = TemplateLoader.getPackageVersion(pkgId);
        logger.log(Level.FINER, "template package ID={0}", pkgId);
        logger.log(Level.FINER, "template package version={0}", pkgVersion);
        logger.log(Level.FINER, "installed version={0}", instVersion);
        return (instVersion == null
                || DashPackage.compareVersions(pkgVersion, instVersion) > 0);
    }


    /** Attempt to open the continuation page, to make certain it will
     * succeed when the user visits it in their browser.
     * 
     * @return an error message if the page cannot be opened, null if
     *     all is well.
     */
    private String testContinuation(String prefix, String continuationURI) {
        if (prefix != null)
            continuationURI = WebServer.urlEncodePath(prefix) + "/"
                    + continuationURI;
        try {
            WebServer ws = getTinyWebServer();
            String continuationURL = "http://" + ws.getHostName(false) + ":"
                    + ws.getPort() + continuationURI;
            URL u = new URL(continuationURL);
            logger.finer("Testing continuation URL " + continuationURL);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.connect();
            int status = conn.getResponseCode();
            if (status == 200)
                return null;
            logger.warning("Received response code of " + status + " for "
                    + continuationURL);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Received exception when contacting "
                    + continuationURI, e);
        }
        return resources.getString("Errors.Cannot_Use_Process");
    }


    /** Ask the dashboard to begin the process of loading a new template.
     * 
     * @return null on success; else an error message describing the
     * problem encountered.
     */
    private String initiateTemplateLoad(String templateID,
                                        File templateFile,
                                        String continuationURI)
    {
        String templatePath = templateFile.getPath();
        String templateDir = templateFile.getParent();

        // Check to ensure that the template is contained in a 'jar'
        // or 'zip' file.
        String suffix =
            templatePath.substring(templatePath.length()-4).toLowerCase();
        if (!suffix.equals(".jar") && !suffix.equals(".zip"))
            return resources.getString("Errors.Only_Jar_Zip");

        // Make certain the file can be found.
        if (!templateFile.isFile())
            return resources.getString("Errors.Cannot_Find_File");

        // Make certain that access permissions allow us to read the file
        if (!templateFile.canRead())
            return resources.getString("Errors.Cannot_Read_File");

        // Check to make certain the file is a valid dashboard package, and
        // is compatible with this version of the dashboard.
        boolean upgradeNeeded = true;
        try {
            DashPackage dashPackage = getDashPackageForFile(templateFile);
            String currentDashVersion = (String) env.get(
                    WebServer.PACKAGE_ENV_PREFIX + "pspdash");
            if (dashPackage.isIncompatible(currentDashVersion)) {
                String requiredVersion = dashPackage.requiresDashVersion;
                if (requiredVersion.endsWith("+")) {
                    requiredVersion = requiredVersion.substring(0,
                            requiredVersion.length() - 1);
                    return resources.format(
                            "Errors.Version_Mismatch.Need_Upgrade_FMT",
                            requiredVersion, currentDashVersion);
                } else {
                    return resources.format("Errors.Version_Mismatch.Exact_FMT",
                            requiredVersion, currentDashVersion);
                }
            }
            upgradeNeeded = upgradeIsNeeded(dashPackage);

        } catch (InvalidDashPackage idp) {
            return resources.getString("Errors.Invalid_Dash_Package");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to read team process add-on", e);
            return resources.getString("Errors.Cannot_Read_File");
        }

        // Initiate the loading of the template definition.
        new TemplateLoadTask(templatePath, templateDir, upgradeNeeded);
        return null;
    }


    /**
     * The DashController.loadNewTemplate method will block, waiting for user
     * input, so we must run it in a thread so this CGI script can complete.
     */
    private class TemplateLoadTask extends Thread {
        private String templatePath, templateDir;
        private boolean upgradeNeeded;
        public TemplateLoadTask(String templatePath, String templateDir,
                boolean upgradeNeeded) {
            this.templatePath = templatePath;
            this.templateDir = templateDir;
            this.upgradeNeeded = upgradeNeeded;
            this.start();
        }
        public void run() {
            if (upgradeNeeded)
                DashController.loadNewTemplate(templatePath, templateDir, false);
            else
                DashController.addTemplateDirToPath(templateDir, false);
        }
    }
}
