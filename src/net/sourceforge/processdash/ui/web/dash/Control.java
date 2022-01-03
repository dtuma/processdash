// Copyright (C) 2001-2022 Tuma Solutions, LLC
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


import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

import org.json.simple.JSONObject;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.ev.EVTaskList;
import net.sourceforge.processdash.log.defects.RepairDefectCounts;
import net.sourceforge.processdash.net.http.TinyCGIException;
import net.sourceforge.processdash.process.ui.TriggerURI;
import net.sourceforge.processdash.ui.ConsoleWindow;
import net.sourceforge.processdash.ui.WindowTracker;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;



public class Control extends TinyCGIBase {

    private String taskName;
    private boolean printNullDocument;

    @Override
    protected void doPost() throws IOException {
        parseFormData();
        writeContents();
    }

    /** Write the CGI header. */
    protected void writeHeader() {}

    protected void writeHtmlHeader() {
        out.print("Expires: 0\r\n");
        super.writeHeader();
    }

    /** Generate CGI script output. */
    protected void writeContents() throws IOException {
        printNullDocument = true;
        taskName = (String) env.get("SCRIPT_NAME");

        if (shouldForbidRemote())
            DashController.checkIP(env.get("REMOTE_ADDR"));

        raiseWindow();
        hideWindow();
        showFindTask();
        showConsole();
        setPath();
        startTiming();
        stopTiming();
        showHelp();
        saveDataFiles();
        saveAllData();
        scrubDataDir();
        repairDefectCounts();
        migrateBaselines();
        reloadWARs();
        runTrigger();

        if (printNullDocument) {
            writeHtmlHeader();
            DashController.printNullDocument(out);
        }
    }

    private boolean shouldForbidRemote() {
        Matcher m = TASK_NAME_PATTERN.matcher(taskName);
        if (m.matches()) {
            String taskShortName = m.group(1);
            String settingName = "control.allowRemote." + taskShortName;
            boolean remoteAllowed = Settings.getBool(settingName, false);
            if (remoteAllowed)
                return false;
        }
        return true;
    }

    private static final Pattern TASK_NAME_PATTERN = Pattern
            .compile(".*/([^/.]+)(.class)?");

    private boolean isTask(String task) {
        return taskName.indexOf(task) != -1;
    }

    private void raiseWindow() {
        if (isTask("raiseWindow") || isTask("showMainWindow")) {
            DashController.raiseWindow();
            printWindowOpenedJson(getDashboardContext());
        }
    }

    private void hideWindow() {
        if (isTask("hideMainWindow")) {
            DashController.hideWindow();
            if (isJsonRequest())
                printJsonResponse("{\"stat\":\"ok\"}");
        }
    }

    private void showFindTask() {
        if (isTask("showFindTaskWindow")) {
            printWindowOpenedJson(DashController.showFindTaskWindow());
        }
    }

    private void showConsole() {
        if (isTask("showConsole")) ConsoleWindow.showInstalledConsole();
    }

    private void startTiming() {
        if (isTask("startTiming")) DashController.startTiming();
    }

    private void setPath() throws IOException {
        if (isTask("setPath")) {
            try {
                boolean startTiming = parameters.containsKey("start");
                String phase = getParameter("phase");
                SwingUtilities.invokeAndWait(
                    new SetPath(startTiming, getPrefix(), phase));
            } catch (InterruptedException ie) {
            } catch (InvocationTargetException ite) {
                if (ite.getTargetException() instanceof IOException)
                    throw (IOException) ite.getTargetException();
            }
        }
    }

    private void stopTiming() {
        if (isTask("stopTiming")) DashController.stopTiming();
    }

    private void showHelp() {
        if (isTask("showHelp"))
            PCSH.displayHelpTopic(getParameter("topicID"));
    }

    private void saveDataFiles() {
        if (isTask("saveDataFiles"))
            getDataRepository().saveAllDatafiles();
    }

    private void saveAllData() {
        if (isTask("saveAllData")) {
            boolean success = DashController.saveAllDataWithFeedback();

            if (isJsonRequest()) {
                printJsonResponse(success ? "{\"stat\":\"ok\"}"
                        : "{\"stat\":\"fail\"}");
            } else {
                writeHtmlHeader();
                out.println("<HTML><HEAD><TITLE>Data Saved</TITLE></HEAD>");
                out.println("<BODY><H1>Data Saved</H1>");
                if (success)
                    out.println("All data files were saved at " + new Date());
                else
                    out.print("Some files could not be saved. Please try again.");
                out.println("</BODY></HTML>");
            }

            printNullDocument = false;
        }
    }

    private void scrubDataDir() {
        if (isTask("scrubDataDir")) {
            DashController.scrubDataDirectory();

            writeHtmlHeader();
            out.println("<HTML><HEAD><TITLE>Data Directory Scrubbed</TITLE></HEAD>");
            out.println("<BODY><H1>Data Directory Scrubbed</H1>");
            out.println("The data directory was scrubbed at " + new Date());
            out.println("</BODY></HTML>");

            printNullDocument = false;
        }
    }

    private void repairDefectCounts() {
        if (isTask("repairDefectCounts")) {
            ProcessDashboard dash = (ProcessDashboard) getDashboardContext();
            RepairDefectCounts.run(dash, dash.getDirectory());

            writeHtmlHeader();
            out.println("<HTML><HEAD><TITLE>Defect Counts Repaired</TITLE></HEAD>");
            out.println("<BODY><H1>Defect Counts Repaired</H1>");
            out.println("The defect counts were repaired at " + new Date());
            out.println("</BODY></HTML>");

            printNullDocument = false;
        }
    }

    private void migrateBaselines() {
        if (isTask("migrateBaselines")) {
            EVTaskList.migrateSnapshotsToExternalData(getDataRepository(), true);

            writeHtmlHeader();
            out.println("<HTML><HEAD><TITLE>EV Baselines Migrated</TITLE></HEAD>");
            out.println("<BODY><H1>EV Baselines Migrated</H1>");
            out.println("Earned value baselines were migrated to external storage on " + new Date());
            out.println("</BODY></HTML>");

            printNullDocument = false;
        }
    }

    private void reloadWARs() {
        if (isTask("reloadWARs")) {
            boolean reloaded = getTinyWebServer().reloadWars();

            String title, message;
            if (reloaded) {
                title = "WAR Files Reloaded";
                message = "WAR files were reloaded at " + new Date();
            } else {
                title = "Reload Not Needed";
                message = "No out-of-date WAR files were found, so "
                        + "no reload was performed.";
            }

            writeHtmlHeader();
            out.println("<html><head><title>" + title + "</title></head>");
            out.println("<body><h1>" + title + "</h1>");
            out.println(message);
            out.println("</body></html>");

            printNullDocument = false;
        }
    }

    private void runTrigger() throws IOException {
        if (isTask("runTrigger")) {
            String uri = getParameter("uri");
            if (!TriggerURI.isTrigger(uri))
                throw new TinyCGIException(400, "Bad Request");

            // run the requested trigger script, and request JSON output
            String tUri = HTMLUtils.appendQuery(uri, TriggerURI.IS_TRIGGERING);
            env.put("HTTP_ACCEPT", "application/json");
            String response = getTinyWebServer().getRequestAsString(tUri, env)
                    .trim();

            // some legacy trigger scripts might not be fully JSON compliant.
            // In that case, we need to generate a JSON response for them.
            if (response.contains(TriggerURI.NULL_DOCUMENT_MARKER)) {
                // if the target returned an HTML null document, that means
                // it ran the trigger successfully, but it hasn't been upgraded
                // to return JSON. Return a do-nothing JSON trigger response.
                response = "{\"stat\":\"ok\"}";

            } else if (!response.startsWith("{")) {
                // if the target returned some other kind of content, we must
                // follow the trigger contract and open that content in a web
                // browser. Return a 'redirect' JSON trigger response.
                JSONObject json = new JSONObject();
                json.put("redirect", uri);
                json.put("stat", "ok");
                response = json.toJSONString();
            }

            // print the final response
            printJsonResponse(response);
        }
    }

    private void printWindowOpenedJson(Object window) {
        if (!isJsonRequest())
            return;

        String json = WindowTracker.getWindowOpenedJson(window);
        if (json != null) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
            printJsonResponse(json);
        }
    }

    protected void printJsonResponse(String json) {
        out.print("Content-type: application/json; charset="+charset+"\r\n");
        out.print("Expires: 0\r\n\r\n");
        out.print(json);
        out.flush();
        printNullDocument = false;
    }


    private class SetPath implements Runnable {

        private boolean start;
        private String prefix;
        private String phase;

        SetPath(boolean start, String prefix, String phase) {
            this.start = start;
            this.prefix = prefix;
            this.phase = phase;
        }

        public void run() {
            if (DashController.setPath(prefix) == false)
                start = false;
            else if (phase != null) {
                if (DashController.setPhase(phase) == false)
                    start = false;
            }
            if (start)
                DashController.startTiming();
        }

    }

}
