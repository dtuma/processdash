// Copyright (C) 2002-2020 Tuma Solutions, LLC
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

import static net.sourceforge.processdash.tool.bridge.client.HistoricalMode.DATA_EFFECTIVE_DATE_PROPERTY;

import java.awt.Toolkit;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
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
import java.util.Properties;
import java.util.TreeSet;

import org.json.simple.JSONObject;
import org.w3c.dom.Element;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.SaveableData;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.templates.ExtensionManager;
import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.tool.bridge.client.TeamServerSelector;
import net.sourceforge.processdash.tool.export.mgr.ExternalResourceManager;
import net.sourceforge.processdash.ui.Browser;
import net.sourceforge.processdash.ui.macosx.MacGUIUtils;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.RuntimeUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

import teamdash.FilenameMapper;
import teamdash.wbs.ProxyLibraryEditor;
import teamdash.wbs.WBSEditor;
import teamdash.wbs.WBSTabPanel;
import teamdash.wbs.WorkflowLibraryEditor;

public class OpenWBSEditor extends TinyCGIBase {

    private static final String JAR_PARAM = "jar";

    private static final Resources resources = Resources
            .getDashBundle("WBSEditor.Errors");

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
        } else if (parameters.containsKey("err")) {
            printErrorPage();
            return;
        }

        boolean useJNLP = Settings.getBool("wbsEditor.useJNLP", false);
        if (parameters.containsKey("useJNLP"))
            useJNLP = true;
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
            printErrorPage("Location_Missing");
            return;
        }

        if (useJNLP)
            openViaJnlp(url, directory);
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

        if (launchEditorProcess(url, directory)) {
            // if we successfully opened the WBS, write the null document.
            writeNullDocument();
        } else {
            // if we weren't able to find the JRE executable or the JAR, our
            // best bet is to try JNLP instead. Use an HTML redirect to point
            // point the user to a forced JNLP page.
            writeHtmlHeader();
            out.print("<html><head>");
            out.print("<meta http-equiv='Refresh' CONTENT='0;URL=" +
                    getScriptUri() + "?useJNLP&");
            out.print(env.get("QUERY_STRING"));
            out.print("'></head></html>");
        }
    }

    protected void writeNullDocument() {
        writeHtmlHeader();
        if (isJsonRequest()) {
            out.print(jsonResponse("window", "pid", -1, "title", "WBS Editor"));
        } else {
            DashController.printNullDocument(out);
        }
    }

    private String jsonResponse(String key, Object... values) {
        JSONObject result = new JSONObject();
        if (values.length == 1) {
            result.put(key, values[0]);
        } else {
            JSONObject obj = new JSONObject();
            for (int i = 0; i < values.length; i += 2)
                obj.put(values[i], values[i + 1]);
            result.put(key, obj);
        }
        result.put("stat", "ok");
        return result.toJSONString();
    }

    private boolean checkEntryCriteria(String url, String directory) {
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
            printErrorPage("Server_Unavailable", url);
            return false;
        }

        // If the directory supplied does not exist, display a "cannot find
        // directory" error message.
        File dir = new File(directory);
        if (!dir.isDirectory()) {
            printErrorPage("Directory_Unavailable", directory);
            return false;
        }

        return true;
    }

    private void printErrorPage() {
        String resKey = getParameter("err");
        String[] locations = (String[]) parameters.get("location_ALL");
        printErrorPage(resKey, locations);
    }

    private void printErrorPage(String resKey, String... location) {
        writeHtmlHeader();

        if (isJsonRequest()) {
            StringBuffer errUri = new StringBuffer();
            errUri.append(getScriptUri());
            HTMLUtils.appendQuery(errUri, "err", resKey);
            for (String l : location)
                HTMLUtils.appendQuery(errUri, "location", l);

            out.print(jsonResponse("redirect", errUri.toString()));
            return;
        }

        String title = resources.getHTML(resKey + ".Title");
        out.print("<html>\n<head>\n<title>");
        out.print(title);
        out.print("</title>\n</head>\n<body>\n<h1>");
        out.print(title);
        out.print("</h1>\n<p>");
        if (location == null || location.length == 0) {
            out.print(resources.getHTML(resKey + ".Message"));
        } else {
            out.print(resources.getHTML(resKey + ".Message_1"));
            out.print("</p>\n<pre style='margin-left:1cm'>");
            out.print(HTMLUtils.escapeEntities(location[0]));
            out.print("</pre>\n<p>");
            out.print(resources.getHTML(resKey + ".Message_2"));
        }
        out.print("</p>\n</body>\n</html>\n");
    }

    public Map<String, String> getLaunchProperties(String url) {
        Map<String,String> result = new HashMap<String,String>();

        String itemHref = null;
        if (parameters.get("showItem") instanceof String)
            itemHref = getParameter("showItem");

        if (parameters.containsKey("bottomUp"))
            result.put("teamdash.wbs.bottomUp", "true");

        if (parameters.containsKey("indiv")) {
            result.put("teamdash.wbs.indiv", "true");
            result.put("teamdash.wbs.indivInitials", getIndivInitials());
            if (!StringUtils.hasValue(itemHref))
                itemHref = getItemHrefForIndivActiveTask();
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
            getTemplateResourceURLs("customWbsColumns", "specFile"));
        result.put(WBSTabPanel.CUSTOM_TABS_SYS_PROP,
            getTemplateResourceURLs("customWbsTabs", "specFile"));
        result.put(WorkflowLibraryEditor.ORG_WORKFLOWS_SYS_PROP,
            getTemplateResourceURLs("org-workflows", "uri"));
        result.put(ProxyLibraryEditor.ORG_PROXIES_SYS_PROP,
            getTemplateResourceURLs("org-proxies", "uri"));
        result.put("teamdash.wbs.globalInitialsPolicy",
            getGlobalInitialsPolicy());
        if (StringUtils.hasValue(itemHref))
            result.put("teamdash.wbs.showItem", itemHref);
        if (parameters.containsKey("dumpAndExit"))
            result.put("teamdash.wbs.dumpAndExit", "true");
        else if (isJsonRequest())
            result.put("teamdash.wbs.startupPause",
                Settings.getVal("userPref.wbsEditor.jsonPause", "500"));

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

    private String getItemHrefForIndivActiveTask() {
        String activeTask = ((ProcessDashboard) getDashboardContext())
                .getActiveTaskModel().getPath();
        if (activeTask == null || !activeTask.startsWith(getPrefix()))
            return null;

        SaveableData wbsID = getDataRepository().getInheritableValue(
            activeTask, "WBS_Unique_ID");
        if (wbsID == null)
            return null;
        else
            return "wbs/" + wbsID.getSimpleValue().format();
    }

    private String getProcessURL() {
        Object processURL = parameters.get("processURL");
        if (processURL instanceof String)
            return (String) processURL;
        else
            return null;
    }

    private String getTemplateResourceURLs(String tagName, String uriAttr) {
        List<Element> configElements = ExtensionManager
                .getXmlConfigurationElements(tagName);
        if (configElements == null || configElements.isEmpty())
            return null;

        // build a list of dashboard URIs to the named template resources
        TreeSet<String> uriList = new TreeSet<String>();
        for (Element xml : configElements) {
            String uri = ExtensionManager.getConfigUri(xml, uriAttr);
            uriList.add(uri);
        }

        // resolve the template URIs into absolute URLs, and add to a list
        StringBuffer result = new StringBuffer();
        for (String uri : uriList) {
            URL url = TemplateLoader.resolveURL(uri);
            if (url != null && !url.toString().startsWith("processdash"))
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
        final String[] cmdLine = getProcessCmdLine(url, directory);
        if (cmdLine == null)
            return false;
        final int heapSize = getUserChosenHeapMemoryValue(cmdLine);

        new Thread() {
            public void run() {
                try {
                    // the execWithAdaptiveHeapSize method waits up to 1500 ms
                    // to make sure the process is still running. We need to
                    // get a response back to our client faster than that, so
                    // we launch the process in an async thread
                    Process p = RuntimeUtils.execWithAdaptiveHeapSize(cmdLine,
                        null, null, heapSize);
                    new OutputConsumer(p).start();
                } catch (Exception e) {
                    Toolkit.getDefaultToolkit().beep();
                    e.printStackTrace();
                }
            };
        }.start();
        return true;
    }

    private String[] getProcessCmdLine(String url, String directory) {
        String jreExecutable = RuntimeUtils.getJreExecutable();
        File classpath = findWbsEditorJarFile();
        if (jreExecutable == null || classpath == null)
            return null;

        List cmd = new ArrayList();

        String extraArgs = Settings.getVal("wbs.jvmArgs", "");
        extraArgs = maybeDisableJvmMemoryArg(extraArgs).trim();
        if (extraArgs.length() > 0)
            cmd.addAll(Arrays.asList(extraArgs.split("\\s+")));

        // propagate security-related system properties
        cmd.addAll(Arrays.asList(RuntimeUtils.getPropagatedJvmArgs()));

        // pass along the historical effective date if it is set
        String dataEffDate = System.getProperty(DATA_EFFECTIVE_DATE_PROPERTY);
        if (dataEffDate != null)
            cmd.add("-D" + DATA_EFFECTIVE_DATE_PROPERTY + "=" + dataEffDate);

        // set a reasonable application menu name on Mac OS X
        if (MacGUIUtils.isMacOSX()) {
            cmd.add("-Xdock:name=" + resources.getString("Window.App_Name"));
            File icon = new File(classpath.getParentFile(), "wbs-editor.icns");
            if (icon.isFile())
                cmd.add("-Xdock:icon=" + icon.getAbsolutePath());
        }

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
                readOnly, null, getOwner());
            if (editor != null)
                editors.put(key, editor);
            else
                editors.remove(key);
        }
    }

    private void openViaJnlp(String url, String directory) throws IOException {
        if (parameters.containsKey("isTriggering")
                || parameters.containsKey("trigger"))
            openInWebStart(url, directory);
        else
            writeJnlpFile(url, directory);
    }

    private void openInWebStart(String url, String directory)
            throws IOException {
        // make certain the caller is local
        DashController.checkIP(env.get("REMOTE_ADDR"));

        // build a command line for launching web start
        String[] cmdLine = new String[] { getJavaWebStartExecutable(),
                "-Xnosplash", getJnlpUrl() };
        Runtime.getRuntime().exec(cmdLine);

        // if we successfully opened the WBS, write the null document.
        writeNullDocument();
    }

    private String getJavaWebStartExecutable() {
        // get the path to the Java Web Start executable.
        String javaApp = RuntimeUtils.getJreExecutable();
        int javaEndPos = javaApp.lastIndexOf("java") + 4;
        return javaApp.substring(0, javaEndPos) + "ws"
                + javaApp.substring(javaEndPos);
    }

    private String getJnlpUrl() {
        // construct a URL for the JNLP file that will open this WBS
        String query = (String) env.get("QUERY_STRING");
        query = StringUtils.findAndReplace(query, "isTriggering", "isTr");
        query = StringUtils.findAndReplace(query, "trigger", "tr");
        String jnlpUri = getScriptUri() + "?useJNLP&" + query;
        return Browser.mapURL(jnlpUri);
    }

    private void writeJnlpFile(String url, String directory) throws IOException {
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

        String path = getScriptUri();
        int pos = path.lastIndexOf('/');
        String jarPath = path.substring(1, pos+1) + "WBSEditor.jar";
        out.print("<resources>\n");
        out.print("<j2se version='1.6+' initial-heap-size='2M' max-heap-size='800M'/>\n");
        out.print("<jar href='");
        out.print(jarPath);
        out.print("'/>\n");
        out.print("</resources>\n");

        Properties props = new Properties();
        for (Map.Entry e : getLaunchProperties(url).entrySet()) {
            if (e.getValue() != null)
                props.put(e.getKey(), e.getValue());
        }
        for (String arg : RuntimeUtils.getPropagatedJvmArgs()) {
            if (arg.startsWith("-D")) {
                int eqPos = arg.indexOf('=');
                String key = arg.substring(2, eqPos);
                String value = arg.substring(eqPos + 1);
                props.put(key, value);
            }
        }
        ByteArrayOutputStream propsOut = new ByteArrayOutputStream();
        props.store(propsOut, null);
        String propsStr = new String(propsOut.toByteArray(), "ISO-8859-1");
        propsStr = propsStr.replaceAll("[\r\n]+", "////");

        out.print("<application-desc>\n");
        out.print("<argument>--jnlp</argument>\n");
        out.print("<argument>");
        out.print(XMLUtils.escapeAttribute(propsStr));
        out.print("</argument>\n");

        out.print("<argument>");
        if (url != null)
            out.print(XMLUtils.escapeAttribute(url));
        if (url != null && directory != null)
            out.print("////");
        if (directory != null)
            out.print(XMLUtils.escapeAttribute(directory));
        out.print("</argument>\n");
        out.print("</application-desc>\n");

        out.print("</jnlp>\n");
        out.flush();
    }

    private void serveJar() throws IOException {
        File jarFile = findWbsEditorJarFile();
        if (jarFile == null)
            throw new IOException("Cannot locate WBSEditor.jar file");
        
        long modTime = jarFile.lastModified();
        InputStream in = new FileInputStream(jarFile);

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
        in.close();
    }

    private static final DateFormat dateFormat =
                           // Tue, 05 Dec 2000 17:28:07 GMT
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);

    private File findWbsEditorJarFile() {
        // first, try to locate the JAR file that holds this class definition
        File classpath = RuntimeUtils.getClasspathFile(getClass());
        if (classpath != null && classpath.isFile())
            return classpath;

        // when running in development mode, the definition of this class will
        // be located in a "bin" directory instead of a JAR. In that case, find
        // WBSEditor.jar by searching for a "template" file we know it contains.
        URL myURL = TemplateLoader.resolveURL(getScriptUri() + ".link");
        if (myURL == null)
            return null;

        String u = myURL.toString();
        int pos = u.indexOf("!/");
        if (!u.startsWith("jar:file:") || pos == -1)
            return null;
        u = u.substring(9, pos);

        String jarFileName;
        try {
            jarFileName = URLDecoder.decode(u, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // can't happen
            return null;
        }

        File jarFile = new File(jarFileName).getAbsoluteFile();
        return (jarFile.isFile() ? jarFile : null);
    }

    private String getScriptUri() {
        return (String) env.get("SCRIPT_NAME");
    }

}
