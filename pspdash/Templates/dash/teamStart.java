// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil

import pspdash.*;

import pspdash.data.DataRepository;
import pspdash.data.ImmutableStringData;
import pspdash.data.ImmutableDoubleData;
import pspdash.data.SimpleData;

import java.io.*;
import java.net.URL;
import java.util.*;



/** This script bootstraps the startup of a team process setup wizard.
 *
 * It interacts with the uesr just long enough to determine which team
 * process definition will be used to perform a team project, then
 * hands control over to the startup wizard for that process.
 *
 * Including this bootstrap portion in the main dashboard distribution
 * allows individuals to join a team project, even if they have never
 * been a part of one before (and thus don't have a team project
 * definition in their template path).
 */
public class teamStart extends TinyCGIBase {

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
    private static final String PROCESS_URL = "teamStartProcess.shtm";

    private static final String TEAM_PID = "setup//Process_ID";
    private static final String TEAM_PID_LIST = "setup//Process_ID_List";
    private static final String TEAM_PROC_NAME = "setup//Process_Name";

    protected void writeHeader() {}
    protected void writeContents() {}
    public void service(InputStream in, OutputStream out, Map env)
        throws IOException
    {
        super.service(in, out, env);
        if ("POST".equalsIgnoreCase((String) env.get("REQUEST_METHOD")))
            parseFormData();

        String page = getParameter(PAGE);
        if (page == null)                         showWelcomePage();
        else if (WELCOME_PAGE.equals(page))       showWelcomePage();
        else if (TYPE_PAGE.equals(page))          handleTypePage();
        else if (PROCESS_PAGE.equals(page))       handleProcessPage();

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
            URL u = TemplateLoader.resolveURL(wizardURL);
            if (u != null)
                return wizardURL;
        }
        return null;
    }


}
