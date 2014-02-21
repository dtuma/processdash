// Copyright (C) 2002-2014 Tuma Solutions, LLC
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

package teamdash.templates.tools;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.w3c.dom.Element;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.templates.ExtensionManager;
import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.tool.bridge.client.TeamServerSelector;
import net.sourceforge.processdash.tool.export.mgr.ExternalResourceManager;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.RuntimeUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

import teamdash.FilenameMapper;
import teamdash.wbs.WBSEditor;

public class OpenWBSEditor extends TinyCGIBase {

    private static final String JAR_PARAM = "jar";

    public OpenWBSEditor() {
        this.charset = "UTF-8";
    }

    protected void writeHeader() {
    }
    private void writeHtmlHeader() {
        super.writeHeader();
    }

    /** Generate CGI script output. */
    protected void writeContents() throws IOException {
        if (parameters.containsKey(JAR_PARAM)) {
            serveJar();
            return;
        }

        boolean useJNLP = Settings.getBool("wbsEditor.useJNLP", false);
        if (parameters.containsKey("useJNLP"))
            useJNLP = true;
        else if (parameters.containsKey("isTriggering")
                || parameters.containsKey("trigger"))
            useJNLP = false;
        try {
            DashController.checkIP(env.get("REMOTE_ADDR"));
        } catch (IOException ioe) {
            useJNLP = true;
        }

        parseFormData();
        String url = getStringParameter("directoryURL");
        url = FilenameMapper.remap(url);
        String directory = getStringParameter("directory");
        directory = FilenameMapper.remap(directory);

        if (url == null && directory == null) {
            writeHtmlHeader();
            out.print(LOCATION_MISSING_MSG);
            return;
        }

        if (useJNLP)
            writeJnlpFile(url, directory);
        else
            openInProcess(url, directory);
    }

    private String getStringParameter(String name) {
        Object o = parameters.get(name);
        if (o instanceof String) {
            String s = (String) o;
            return (s.length() > 0 ? s : null);
        } else {
            return null;
        }
    }

    private String getSyncURL() {
        return makeAbsoluteURL(getParameter("syncURL"));
    }

    private String getReverseSyncURL() {
        return makeAbsoluteURL(getParameter("reverseSyncURL"));
    }

    private String makeAbsoluteURL(String uri) {
        if (uri == null || uri.trim().length() == 0)
            return null;
        if (uri.startsWith("http"))
            return uri;
        return getRequestURLBase() + uri;
    }

    protected String getRequestURLBase() {
        try {
            return super.getRequestURLBase();
        } catch (Throwable t) {
            // this method will not be present in the superclass prior to
            // dashboard 1.14.3.1.  If the method is not found, fall back to
            // an earlier implementation of this logic.
            WebServer ws = getTinyWebServer();
            return "http://" + ws.getHostName(false) + ":" + ws.getPort();
        }
    }

    private void openInProcess(String url, String directory) {
        if (!checkEntryCriteria(url, directory))
            return;

        writeHtmlHeader();
        if (launchEditorProcess(url, directory)) {
            // if we successfully opened the WBS, write the null document.
            DashController.printNullDocument(out);
        } else {
            // if, for some reason, we weren't able to launch the WBS in a new
            // process, our best bet is to try JNLP instead.  Use an HTML
            // redirect to point the user to a forced JNLP page.
            out.print("<html><head>");
            out.print("<meta http-equiv='Refresh' CONTENT='0;URL=" +
                        "/team/tools/OpenWBSEditor.class?useJNLP&");
            out.print(env.get("QUERY_STRING"));
            out.print("'></head></html>");
        }
    }

    private boolean checkEntryCriteria(String url, String directory) {
        String javaVersion = System.getProperty("java.version");
        if (javaVersion.startsWith("1.3")) {
            writeHtmlHeader();
            out.print(JAVA_VERSION_MSG1 + javaVersion + JAVA_VERSION_MSG2);
            return false;
        }

        // If a URL was provided and it maps to a valid team server
        // collection, we'll be fine in terms of opening the WBS.
        if (url != null) {
            if (TeamServerSelector.isUrlFormat(url)) {
                if (TeamServerSelector.testServerURL(url) != null)
                    return true;

            } else {
                File dir = new File(url);
                if (dir.isDirectory())
                    return true;
            }
        }

        // If the URL was bad and no directory was given, display a "server
        // unavailable" error message.
        if (directory == null) {
            writeHtmlHeader();
            out.print(SERVER_UNAVAILABLE_MSG1 + HTMLUtils.escapeEntities(url)
                    + SERVER_UNAVAILABLE_MSG2);
            return false;
        }

        // If the directory supplied does not exist, display a "cannot find
        // directory" error message.
        File dir = new File(directory);
        if (!dir.isDirectory()) {
            writeHtmlHeader();
            out.print(DIR_MISSING_MSG1 + HTMLUtils.escapeEntities(directory) +
                      DIR_MISSING_MSG2);
            return false;
        }

        return true;
    }

    public Map<String, String> getLaunchProperties(String url) {
        Map<String,String> result = new HashMap<String,String>();

        if (parameters.containsKey("bottomUp"))
            result.put("teamdash.wbs.bottomUp", "true");

        if (parameters.containsKey("indiv")) {
            result.put("teamdash.wbs.indiv", "true");
            result.put("teamdash.wbs.indivInitials", getIndivInitials());
        }

        if (parameters.containsKey("team"))
            result.put("teamdash.wbs.showTeamMemberList", "true");

        if (Boolean.getBoolean("forceReadOnly")
                || "true".equalsIgnoreCase(getParameter("forceReadOnly")))
            result.put("teamdash.wbs.readOnly", "true");

        if (TeamServerSelector.isTeamServerUseDisabled()) {
            result.put(TeamServerSelector.DISABLE_TEAM_SERVER_PROPERTY, "true");
        } else if (TeamServerSelector.isUrlFormat(url)
                && url.indexOf('/') > 0) {
            int lastSlash = url.lastIndexOf('/');
            String baseUrl = url.substring(0, lastSlash);
            result.put(TeamServerSelector.DEFAULT_TEAM_SERVER_PROPERTY,
                    baseUrl);
        }

        if ("true".equals(getParameter("reverseSyncNewMembers")))
            result.put("teamdash.wbs.reverseSyncNewMembers", "true");

        result.put("teamdash.wbs.syncURL", getSyncURL());
        result.put("teamdash.wbs.reverseSyncURL", getReverseSyncURL());
        result.put("teamdash.wbs.owner", getOwner());
        result.put("teamdash.wbs.processSpecURL", getProcessURL());
        result.put(teamdash.wbs.columns.CustomColumnManager.SYS_PROP_NAME,
            getCustomColumnSpecURLs());
        result.put("teamdash.wbs.globalInitialsPolicy",
            getGlobalInitialsPolicy());
        if (parameters.containsKey("dumpAndExit"))
            result.put("teamdash.wbs.dumpAndExit", "true");

        return result;
    }

    private String getIndivInitials() {
        Object param = parameters.get("indivInitials");
        if (param instanceof String) {
            String result = ((String) param).trim();
            if (StringUtils.hasValue(result) && !"tttt".equals(result))
                return result;
        }
        return null;
    }

    private String getProcessURL() {
        Object processURL = parameters.get("processURL");
        if (processURL instanceof String)
            return (String) processURL;
        else
            return null;
    }

    private String getCustomColumnSpecURLs() {
        List<Element> configElements = ExtensionManager
                .getXmlConfigurationElements("customWbsColumns");
        if (configElements == null || configElements.isEmpty())
            return null;

        StringBuffer result = new StringBuffer();
        for (Element xml : configElements) {
            String uri = xml.getAttribute("specFile");
            URL url = TemplateLoader.resolveURL(uri);
            if (url != null)
                result.append(" ").append(url.toString());
        }
        if (result.length() > 0)
            return result.substring(1);
        else
            return null;
    }

    private String getGlobalInitialsPolicy() {
        Object param = parameters.get("globalInitialsPolicy");
        if (param instanceof String) {
            String result = (String) param;
            if (StringUtils.hasValue(result))
                return result;
        }
        return null;
    }

    private static Hashtable editors = new Hashtable();

    private boolean launchEditorProcess(String url, String directory) {
        String[] cmdLine = getProcessCmdLine(url, directory);
        if (cmdLine == null)
            return false;

        try {
            Process p = RuntimeUtils.execWithAdaptiveHeapSize(cmdLine, null,
                null, getUserChosenHeapMemoryValue(cmdLine));
            new OutputConsumer(p).start();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String[] getProcessCmdLine(String url, String directory) {
        String jreExecutable = RuntimeUtils.getJreExecutable();
        File classpath = RuntimeUtils.getClasspathFile(getClass());
        if (jreExecutable == null || classpath == null)
            return null;

        List cmd = new ArrayList();

        String extraArgs = Settings.getVal("wbs.jvmArgs", "");
        extraArgs = maybeDisableJvmMemoryArg(extraArgs).trim();
        if (extraArgs.length() > 0)
            cmd.addAll(Arrays.asList(extraArgs.split("\\s+")));

        // propagate security-related system properties
        cmd.addAll(Arrays.asList(RuntimeUtils.getPropagatedJvmArgs()));

        // set a reasonable application menu name on Mac OS X
        if ("Mac OS X".equalsIgnoreCase(System.getProperty("os.name")))
            cmd.add("-Xdock:name=WBS Editor");

        Map<String, String> props = getLaunchProperties(url);
        props.putAll(ExternalResourceManager.getInstance()
                .getJvmArgsForMapping());

        for (Map.Entry<String, String> e : props.entrySet()) {
            if (e.getValue() != null)
                cmd.add("-D" + e.getKey() + "=" + e.getValue());
        }

        cmd.add("-jar");
        cmd.add(classpath.getAbsolutePath());

        if (url != null)
            cmd.add(url);
        if (directory != null)
            cmd.add(directory);

        return (String[]) cmd.toArray(new String[cmd.size()]);
    }

    private static final String DISABLED_MEM_PREFIX = "-DdisabledXmx";

    private String maybeDisableJvmMemoryArg(String extraArgs) {
        return StringUtils.findAndReplace(extraArgs, "-Xmx", DISABLED_MEM_PREFIX);
    }

    private int getUserChosenHeapMemoryValue(String[] cmdLine) {
        for (String arg : cmdLine) {
            if (arg.startsWith(DISABLED_MEM_PREFIX))
                try {
                    String num = arg.substring(DISABLED_MEM_PREFIX.length(),
                        arg.length() - 1);
                    return Integer.parseInt(num);
                } catch (NumberFormatException nfe) {}
        }
        return 0;
    }

    private static class OutputConsumer extends Thread {
        Process p;
        public OutputConsumer(Process p) {
            this.p = p;
            setDaemon(true);
        }
        public void run() {
            RuntimeUtils.consumeOutput(p, System.out, System.err);
        }
    }


    protected void showEditorInternally(String directory, boolean bottomUp,
            boolean indiv, String indivInitials, boolean showTeam,
            boolean readOnly, String syncURL) {
        String key = directory;
        if (bottomUp)
            key = "bottomUp:" + key;

        WBSEditor editor = (WBSEditor) editors.get(key);
        if (editor != null && !editor.isDisposed()) {
            if (showTeam)
                editor.showTeamListEditorWithSaveButton();
            else
                editor.raiseWindow();

        } else {
            editor = WBSEditor.createAndShowEditor(new String[] { directory },
                bottomUp, indiv, indivInitials, showTeam, syncURL, false,
                readOnly, getOwner());
            if (editor != null)
                editors.put(key, editor);
            else
                editors.remove(key);
        }
    }

    private void writeJnlpFile(String url, String directory) {
        out.print("Content-type: application/x-java-jnlp-file\r\n\r\n");

        out.print("<?xml version='1.0' encoding='utf-8'?>\n");
        out.print("<jnlp spec='1.0+' codebase='");
        out.print(getRequestURLBase());
        out.print("/'>\n");

        out.print("<information>\n");
        out.print("<title>WBS Editor</title>\n");
        out.print("<vendor>Tuma Solutions, LLC</vendor>\n");
        out.print("<description>Work Breakdown Structure Editor</description>\n");
        out.print("</information>\n");

        out.print("<security><all-permissions/></security>\n");

        String path = (String) env.get("SCRIPT_NAME");
        int pos = path.lastIndexOf('/');
        String jarPath = path.substring(1, pos+1) + "TeamTools.jar";
        out.print("<resources>\n");
        out.print("<j2se version='1.5+' initial-heap-size='2M' max-heap-size='800M'/>\n");
        out.print("<jar href='");
        out.print(jarPath);
        out.print("'/>\n");

        Map<String, String> props = getLaunchProperties(url);
        for (Map.Entry<String, String> e : props.entrySet()) {
            if (e.getValue() != null) {
                out.print("<property name='");
                out.print(e.getKey());
                out.print("' value='");
                out.print(XMLUtils.escapeAttribute(e.getValue()));
                out.print("'/>\n");
            }
        }

        out.print("</resources>\n");

        out.print("<application-desc>\n");
        if (url != null) {
            out.print("<argument>");
            out.print(XMLUtils.escapeAttribute(url));
            out.print("</argument>\n");
        }
        if (directory != null) {
            out.print("<argument>");
            out.print(XMLUtils.escapeAttribute(directory));
            out.print("</argument>\n");
        }
        out.print("</application-desc>\n");

        out.print("</jnlp>\n");
        out.flush();
    }

    private void serveJar() throws IOException {
        URL myURL = getClass().getResource("OpenWBSEditor.class");
        String u = myURL.toString();
        int pos = u.indexOf("!/");
        if (!u.startsWith("jar:") || pos == -1)
            throw new IOException();

        URL jarURL = new URL(u.substring(4, pos));
        URLConnection conn = jarURL.openConnection();
        long modTime = conn.getLastModified();
        InputStream in = conn.getInputStream();

        out.print("Content-type: application/octet-stream\r\n");
        if (modTime > 0) {
            out.print("Last-Modified: ");
            out.print(dateFormat.format(new Date(modTime)));
            out.print("\r\n");
        }
        out.print("\r\n");
        out.flush();

        byte[] buf = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(buf)) != -1)
            outStream.write(buf, 0, bytesRead);
        outStream.flush();
    }

    private static final DateFormat dateFormat =
                           // Tue, 05 Dec 2000 17:28:07 GMT
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);

    private static final String LOCATION_MISSING_MSG =
        "<html><head><title>Team Directory Missing</title></head><body>" +
        "<h1>Team Directory Missing</h1>" +
        "<p>The Work Breakdown Structure Editor cannot be used until you " +
        "specify a team data directory on the Project Parameters and " +
        "Settings page.</p></body></html>";


    private static final String JAVA_VERSION_MSG1 =
        "<html><body><h1>Incorrect Java Version</h1>" +
        "Sorry, but the team planning tools require version 1.4 or higher " +
        "of the Java Runtime Environment (JRE).  You are currently running the " +
        "dashboard with version ";
    private static final String JAVA_VERSION_MSG2 =
        " of the JRE.  To use the team planning tools, please upgrade the " +
        "Java Runtime Environment on your computer, restart the dashboard, " +
        "and try again.</body></html>";


    private static final String SERVER_UNAVAILABLE_MSG1 =
        "<html><body><h1>Team Data Server Unavailable</h1>" +
        "The team planning tools need access to the team server that hosts " +
        "data for this project.  According to your current project settings, " +
        "that server is <pre>";
    private static final String SERVER_UNAVAILABLE_MSG2 =
        "</pre>  Unfortunately, this server is unavailable.  Please check " +
        "your network connection, or contact your system administrator to " +
        "see if the Team Server is running.</body></html>";


    private static final String DIR_MISSING_MSG1 =
        "<html><body><h1>Team Data Directory Missing</h1>" +
        "The team planning tools need access to the team data directory for " +
        "this project.  According to your current project settings, that " +
        "directory is <pre>";
    private static final String DIR_MISSING_MSG2 =
        "</pre>  Unfortunately, this directory does not appear to exist.  " +
        "Check to make certain that the directory is accessible and try " +
        "again.</body></html>";

}
