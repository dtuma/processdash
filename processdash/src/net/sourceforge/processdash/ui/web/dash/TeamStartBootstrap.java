// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ui.web.dash;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.data.ImmutableStringData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.process.ScriptID;
import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.NetworkDriveList;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;




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

    private static final String PAGE = "page";

    // Information for the wizard's "welcome" page.
    private static final String WELCOME_PAGE = "welcome";
    private static final String WELCOME_URL = "teamStartWelcome.shtm";
    // Information for the page which asks the user what type of
    // project they wish to create.
    private static final String TYPE_PAGE = "type";
    private static final String TYPE_URL = "teamStartType.shtm";
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


    private static final String TEAM_PID = "setup//Process_ID";
    private static final String TEAM_PID_LIST = "setup//Process_ID_List";
    private static final String TEAM_PROC_NAME = "setup//Process_Name";
    private static final String TEAM_URL = "setup//Team_URL";
    private static final String TEMPLATE_ID = "setup//Template_ID";
    private static final String TEMPLATE_PATH = "setup//Template_Path";
    private static final String TEMPLATE_UNC = "setup//Template_Path_UNC";
    private static final String CONTINUATION_URI = "setup//Continuation_URI";

    // value indicating we should help an individual join a team project
    private static final String JOIN_PAGE = "join";
    private static final String JOIN_ERROR_URL = "teamStartJoinError.shtm";
    private static final String JOIN_VERIFY_URL = "teamStartJoinVerify.shtm";



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
        else if (TYPE_PAGE.equals(page))          handleTypePage();
        else if (SHOW_PROCESS_PAGE.equals(page))  showTeamProcessesPage();
        else if (PROCESS_PAGE.equals(page))       handleProcessPage();

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

    /** Display the welcome page */
    protected void showWelcomePage() {
        printRedirect(WELCOME_URL);
    }

    /** Handle values posted from the setup type page */
    protected void handleTypePage() {
        if (parameters.get("createTeamProject") != null) {
            showTeamProcessesPage();

        } else if (parameters.get("joinTeamProject") != null) {
            showTeamURLPage();
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

    /** The user is creating a team project for the given teamPID; redirect
     *  to its setup wizard.
     */
    protected void redirectToTeamSetupWizard(String teamPID) {
        printRedirect(getPrefix() + "//" +
                      getTeamSetupWizardURL(teamPID) + "?page=team");
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
            return "You must enter a team project URL!";

        // Make certain the team URL is a valid URL.  Note that we
        // should be able to work with the URL to just about any page
        // in the team project! So whether the team leader gives
        // people the URL to the "join" page, or the URL to the "table
        // of contents" page, or even the URL to some other obscure
        // page for the project, we should be able to derive the URL
        // we need.
        teamURL = teamURL.trim();
        if (!teamURL.startsWith("http://")) return INVALID_TEAM_URL_ERR;
        int pos = teamURL.indexOf("//", 7);
        if (pos != -1) pos = teamURL.indexOf('/', pos+2);
        if (pos == -1) return INVALID_TEAM_URL_ERR;
        teamURL = teamURL.substring(0, pos+1) + "setup/join.class?xml";
        URL u = null;
        try {
            u = new URL(teamURL);
        } catch (IOException ioe) {
            return "The URL you entered is not a valid URL.  Please "+
                "doublecheck and correct it.";
        }

        // Download an XML document containing the template information.
        Document doc = null;
        try {
            URLConnection conn = u.openConnection();
            conn.connect();
            doc = XMLUtils.parse(conn.getInputStream());
        } catch (Exception e) {
            return "The dashboard was unable to retrieve information about "+
                "the team project.  Please ensure that you have entered the "+
                "team project URL correctly, and contact your team leader "+
                "to ensure that the team dashboard is currently running.";
        }

        // Extract the relevant information from the XML document we
        // downloaded.
        Element e = doc.getDocumentElement();
        putValue(TEMPLATE_ID, e.getAttribute("Template_ID"));
        putValue(TEMPLATE_PATH, e.getAttribute("Template_Path"));
        putValue(TEMPLATE_UNC, e.getAttribute("Template_Path_UNC"));
        putValue(CONTINUATION_URI, e.getAttribute("Continuation_URI"));

        return null;
    }
    private static final String INVALID_TEAM_URL_ERR =
        "The URL you entered is not a valid team project URL. "+
        "Please doublecheck and correct it.";


    /** Handle values posted from the "join team project" page */
    protected void handleJoinPage() {
        putValue(TEAM_URL, getParameter("Team_URL"));
        putValue(TEMPLATE_ID, getParameter("Template_ID"));
        putValue(TEMPLATE_PATH, getParameter("Template_Path"));
        putValue(TEMPLATE_UNC, getParameter("Template_Path_UNC"));
        putValue(CONTINUATION_URI, getParameter("Continuation_URI"));
        joinProject();
    }


    /** Attempt to join a team project. */
    protected void joinProject() {
        String teamURL = getValue(TEAM_URL);
        String templateID = getValue(TEMPLATE_ID);
        String templatePath = getValue(TEMPLATE_PATH);
        String templatePathUNC = getValue(TEMPLATE_UNC);
        String continuationURI = getValue(CONTINUATION_URI);

        if (templateIsLoaded(templateID, templatePath,
                             templatePathUNC, continuationURI)) {

            // the template is already present in the dashboard, so simply
            // redirect to the continuation URI.
            String prefix = getPrefix();
            if (prefix == null)
                printRedirect(continuationURI);
            else
                printRedirect(prefix + "/" + continuationURI);

        } else {
            String errorMessage = initiateTemplateLoad
                (templateID, templatePath, templatePathUNC, continuationURI);
            if (errorMessage == null)
                printRedirect(JOIN_VERIFY_URL);
            else
                printRedirect(JOIN_ERROR_URL + "?errMsg=" +
                              HTMLUtils.urlEncode(errorMessage));
        }
    }

    private boolean templateIsLoaded(String templateID,
                                     String templatePath,
                                     String templatePathUNC,
                                     String continuationURI)
    {
        // if we have no loaded template with the given ID, return false.
        if (DashController.getTemplates().get(templateID) == null)
            return false;

        // check to see if the continuation URI is a valid resource.  If not,
        // return false.
        int pos = continuationURI.indexOf('?');
        if (pos != -1) continuationURI = continuationURI.substring(0, pos);
        pos = continuationURI.indexOf('#');
        if (pos != -1) continuationURI = continuationURI.substring(0, pos);
        URL url = resolveURL(continuationURI);
        if (url == null) return false;

        // check to see if the continuation URI appears to be provided by
        // the named templatePath
        String urlStr = url.toString();
        if (!urlStr.startsWith("jar:file:")) return false;
        pos = urlStr.indexOf("!/Templates");
        if (pos == -1) return false;
        String urlJarpath = HTMLUtils.urlDecode(urlStr.substring(9, pos));
        File urlJarfile = new File(urlJarpath);
        File templateJarfile = new File(templatePath);
        try {
            if (!urlJarfile.getCanonicalPath().equals
                 (templateJarfile.getCanonicalPath()))
                return false;
        } catch (IOException ioe) {
            return false;
        }

        // the template appears to be loaded, and meets all our criteria.
        return true;
    }


    private URL resolveURL(String uri) {
        URL result = TemplateLoader.resolveURL(uri);
        if (result == null)
            result = TemplateLoader.resolveURL(uri + ".link");
        return result;
    }

    /** Ask the dashboard to begin the process of loading a new template.
     * 
     * @return null on success; else an error message describing the
     * problem encountered.
     */
    private String initiateTemplateLoad(String templateID,
                                        String templatePath,
                                        String templatePathUNC,
                                        String continuationURI)
    {
        // Check to ensure that the template is contained in a 'jar'
        // or 'zip' file.
        String suffix =
            templatePath.substring(templatePath.length()-4).toLowerCase();
        if (!suffix.equals(".jar") && !suffix.equals(".zip"))
            return "The team project setup logic currently only supports "+
                "custom projects that are stored in 'jar' or 'zip' files.";

        // Check to see if the file actually exists.
        File f = new File(templatePath);
        if (!f.exists()) {
            if (templatePathUNC == null || templatePathUNC.length() == 0) {
                return "that file doesn't exist";

            } else {
                // Try to find the template file using the UNC path.
                NetworkDriveList networkDriveList = new NetworkDriveList();
                String altTemplatePath =
                    networkDriveList.fromUNCName(templatePathUNC);
                if (altTemplatePath != null &&
                    (f = new File(altTemplatePath)).exists())
                    templatePath = altTemplatePath;
                else
                    return "that file doesn't exist";
            }
        }

        // Initiate the loading of the template definition.
        String templateDir = f.getParent();
        new TemplateLoadTask(templatePath, templateDir);
        return null;
    }

    /** The DashController.loadNewTemplate method will block, waiting
     * for user input, so we must run it in a thread so this CGI script
     * can complete.
     */
    private class TemplateLoadTask extends Thread {
        private String templatePath, templateDir;
        public TemplateLoadTask(String templatePath, String templateDir) {
            this.templatePath = templatePath;
            this.templateDir = templateDir;
            this.start();
        }
        public void run() {
            DashController.loadNewTemplate(templatePath, templateDir, false);
        }
    }
}
