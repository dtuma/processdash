// Copyright (C) 2001-2014 Tuma Solutions, LLC
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

package net.sourceforge.processdash.net.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.net.cache.ObjectCache;
import net.sourceforge.processdash.security.DashboardPermission;
import net.sourceforge.processdash.templates.DashPackage;
import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.HTTPUtils;

public class WebServer implements ContentSource {

    private Server server;
    private LocalConnector localConnector;
    private ContextHandlerCollection webApps;

    private static final int ALLOW_REMOTE_NEVER = 0;
    // private static final int ALLOW_REMOTE_MAYBE = 1; // No longer supported
    private static final int ALLOW_REMOTE_ALWAYS = 2;
    private int allowingRemoteConnections = ALLOW_REMOTE_NEVER;
    private int port;
    private String startupTimestamp;
    private Hashtable DEFAULT_ENV = new Hashtable();

    static final String DEFAULT_ENV_KEY = WebServer.class.getName()
            + ".defaultEnvironment";
    public static final String TIMESTAMP_HEADER = "Dash-Startup-Timestamp";
    public static final String PACKAGE_ENV_PREFIX = "Dash_Package_";
    public static final String DASHBOARD_PROTOCOL = "processdash";

    private static final DashboardPermission CREATE_PERMISSION =
        new DashboardPermission("webServer.create");
    private static final DashboardPermission SET_ROOTS_PERMISSION =
        new DashboardPermission("webServer.setRoots");
    private static final DashboardPermission SET_ENV_PERMISSION =
        new DashboardPermission("webServer.setEnvironment");
    private static final DashboardPermission ADD_PORT_PERMISSION =
        new DashboardPermission("webServer.addPort");
    private static final DashboardPermission QUIT_PERMISSION =
        new DashboardPermission("webServer.quit");

    public static final String HTTP_ALLOWREMOTE_SETTING = "http.allowRemote";

    private static final Properties mimeTypes = new Properties();
    private static final String DASH_CHARSET = HTTPUtils.DEFAULT_CHARSET;
    static final String HEADER_CHARSET = DASH_CHARSET;
    private static String OUTPUT_CHARSET = DASH_CHARSET;

    private static final Logger logger =
        Logger.getLogger(WebServer.class.getName());




    /**
     * Create a web server, not listening on any ports and not serving any
     * content.
     */
    public WebServer() throws IOException {
        init();
    }

    /**
     * Run a web server on the given port, not serving any content.
     */
    public WebServer(int port) throws IOException {
        init();
        addPort(port);
    }

    static {
        try {
            mimeTypes.load(WebServer.class
                           .getResourceAsStream("mime_types"));
        } catch (Exception e) { e.printStackTrace(); }
    }


    /** Encode HTML entities in the given string, and return the result.
     * @deprecated */
    public static String encodeHtmlEntities(String str) {
        return HTMLUtils.escapeEntities(str);
    }

    public static String urlEncodePath(String path) {
        return HTMLUtils.urlEncodePath(path);
    }

    /** 
     * @return the host name that an external computer might use to
     * communicate with this web server.
     */
    public String getHostName(boolean forRemoteUse) {
        // if the user is forbidding remote connections, then "localhost" is
        // the only address we'll respond to, period.
        if (allowingRemoteConnections == ALLOW_REMOTE_NEVER)
            return "localhost";

        // if the user has set a host they want to use, return that value.
        String result = Settings.getVal("http.hostname");
        if (result != null && result.length() > 0)
            return result;

        // if we need a name for use by a different computer, look it up.
        if (forRemoteUse) try {
            if (Settings.getBool("http.hostname.useIP", false))
                return InetAddress.getLocalHost().getHostAddress();
            else
                return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException uhe) {}

        // return the default value
        return "localhost";
    }

    public static Map getMimeTypeMap() {
        return Collections.unmodifiableMap(mimeTypes);
    }

    /** Utility routine: slurp an entire file from an InputStream.
     * 
     * @deprecated Use {@link FileUtils#slurpContents(InputStream, boolean)} instead.
     */
    public static byte[] slurpContents(InputStream in, boolean close)
        throws IOException
    {
        return FileUtils.slurpContents(in, close);
    }


    /** Perform an internal http request for the caller.
     *
     * @param uri the absolute uri of a resource on this server (e.g.
     *     <code>/0980/help/about.htm?foo=bar</code>)
     * @param skipHeaders if true, the generated response headers are discarded
     * @param extraEnvironment additional environment variables to add to the
     *     request environment.  (These variables cannot overwrite any values
     *     in the environment;  they only supplement it.)
     * @return the response generated by performing the http request.
     */
    public byte[] getRequest(final String uri, boolean skipHeaders,
            Map extraEnvironment) throws IOException {
        if (internalRequestNesting > 50)
            throw new IOException("Infinite recursion - aborting.");

        synchronized(this) { internalRequestNesting++; }
        byte [] result = null;
        try {
            result = localConnector.getResponse(uri, port, extraEnvironment)
                    .asArray();
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            synchronized(this) { internalRequestNesting--; }
        }

        if (!skipHeaders)
            return result;
        else {
            int headerLen = HTTPUtils.getHeaderLength(result);
            byte [] contents = new byte[result.length - headerLen];
            System.arraycopy(result, headerLen, contents, 0, contents.length);
            return contents;
        }
    }
    private volatile int internalRequestNesting = 0;

    public byte[] getRequest(String uri, boolean skipHeaders)
            throws IOException {
        return getRequest(uri, skipHeaders, null);
    }


    private String resolveUriInContext(String context, String uri) throws IOException {
        if (!uri.startsWith("/")) {
            URL contextURL = new URL("http://unimportant" + context);
            URL uriURL = new URL(contextURL, uri);
            uri = uriURL.getFile();
        }
        return uri;
    }

    public byte[] getContent(String context, String uri, boolean raw)
            throws IOException {
        uri = resolveUriInContext(context, uri);
        if (raw)
            throw new IOException(new UnsupportedOperationException());
        else
            return getRequest(uri, true);
    }

    public String getRequestAsString(String uri) throws IOException {
        return getRequestAsString(uri, null);
    }

    public String getRequestAsString(String uri, Map extraEnv) throws IOException {
        byte[] response = getRequest(uri, false, extraEnv);
        int headerLen = HTTPUtils.getHeaderLength(response);
        String header = new String(response, 0, headerLen, HEADER_CHARSET);

        String charset = HTTPUtils.DEFAULT_CHARSET;
        String contentType = HTTPUtils.getContentType(header);
        if (contentType != null)
            charset = HTTPUtils.getCharset(contentType);

        return new String
            (response, headerLen, response.length - headerLen, charset);
    }

    /** Return the number of the port this server is listening on. */
    public int getPort()         { return port; }

    /** Return the startup timestamp for this server. */
    public String getTimestamp() { return startupTimestamp; }

    public static String getOutputCharset() {
        return OUTPUT_CHARSET;
    }

    public static void setOutputCharset(String charsetName) {
        try {
            // do a quick check to make certain that the charset name is valid
            "test".getBytes(charsetName);
            OUTPUT_CHARSET = charsetName;
        } catch (UnsupportedEncodingException uee) {}
    }

    private void init() throws IOException
    {
        CREATE_PERMISSION.checkPermission();
        initAllowRemote();
        initDefaultEnvironment();
        createServer();

        try {
            DashboardURLStreamHandlerFactory.initialize(this);
        } catch (Exception e) {}
    }

    private void initAllowRemote() {
        String setting = Settings.getVal(HTTP_ALLOWREMOTE_SETTING);
        if ("true".equalsIgnoreCase(setting))
            allowingRemoteConnections = ALLOW_REMOTE_ALWAYS;
        else
            allowingRemoteConnections = ALLOW_REMOTE_NEVER;
    }

    private void initDefaultEnvironment() {
        startupTimestamp = Long.toString(System.currentTimeMillis());

        DEFAULT_ENV.put(TinyCGI.TINY_WEB_SERVER, this);
        DEFAULT_ENV.put("SERVER_SOFTWARE", "PROCESS_DASHBOARD");
        DEFAULT_ENV.put("SERVER_NAME", "localhost");
        DEFAULT_ENV.put("GATEWAY_INTERFACE", "CGI/1.1");
        DEFAULT_ENV.put("SERVER_ADDR", "127.0.0.1");
        DEFAULT_ENV.put("PATH_INFO", "");
        DEFAULT_ENV.put("PATH_TRANSLATED", "");
        DEFAULT_ENV.put("REMOTE_HOST", "localhost");
        DEFAULT_ENV.put("REMOTE_ADDR", "127.0.0.1");
    }

    private void writePackagesToDefaultEnv() {
        Iterator i = DEFAULT_ENV.keySet().iterator();
        while (i.hasNext())
            if (((String) i.next()).startsWith(PACKAGE_ENV_PREFIX))
                i.remove();
        i = TemplateLoader.getPackages().iterator();
        while (i.hasNext()) {
            DashPackage pkg = (DashPackage) i.next();
            DEFAULT_ENV.put(PACKAGE_ENV_PREFIX + pkg.id, pkg.version);
        }
    }

    public void setProps(DashHierarchy props) {
        setEnvironmentObject(TinyCGI.PSP_PROPERTIES, props);
    }
    public void setData(DataRepository data) {
        setEnvironmentObject(TinyCGI.DATA_REPOSITORY, data);
    }
    public void setCache(ObjectCache cache) {
        setEnvironmentObject(TinyCGI.OBJECT_CACHE, cache);
    }
    public void setDashboardContext(DashboardContext dashboardContext) {
        setEnvironmentObject(TinyCGI.DASHBOARD_CONTEXT, dashboardContext);
    }
    private void setEnvironmentObject(String key, Object value) {
        SET_ENV_PERMISSION.checkPermission();
        if (value == null)
            DEFAULT_ENV.remove(key);
        else
            DEFAULT_ENV.put(key, value);
    }


    private void createServer() {
        // compile JSPs using an internal compiler, not a JDK javac executable
        System.setProperty("org.apache.jasper.compiler.disablejsr199", "true");

        // create a new Jetty server
        server = new Server();
        server.setAttribute(DEFAULT_ENV_KEY,
            Collections.unmodifiableMap(DEFAULT_ENV));

        // the server thread pool should use daemon threads
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setName("WebServer");
        threadPool.setDaemon(true);
        server.setThreadPool(threadPool);

        // create a local connector which can be used for internal requests
        localConnector = new LocalConnector();
        server.addConnector(localConnector);

        // create a handler chain to serve requests
        webApps = new ContextHandlerCollection();
        webApps.setHandlers(new Handler[0]);
        HandlerWrapper interceptor = new DashboardHttpRequestInterceptor(
                startupTimestamp);
        interceptor.setHandler(webApps);
        server.setHandler(interceptor);

        // start the server
        try {
            server.start();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not start web server", e);
        }
    }



    /** Start listening for connections on an additional port */
    public void addPort(int port) throws IOException {
        ADD_PORT_PERMISSION.checkPermission();

        // look through the existing connectors.  If we are already listening
        // on the requested port, do nothing.
        for (Connector c : server.getConnectors()) {
            if (c instanceof SelectChannelConnector) {
                SelectChannelConnector conn = (SelectChannelConnector) c;
                if (conn.getPort() == port)
                    return;
            }
        }

        for (int retries = 50;  retries-- > 0; ) {
            try {
                // create a new listener on the given port
                SelectChannelConnector c = new SelectChannelConnector();
                if (allowingRemoteConnections != ALLOW_REMOTE_ALWAYS)
                    c.setHost("127.0.0.1");
                c.setPort(port);
                c.setServer(server);

                // attempt to start listening. This will throw an exception if
                // the given port is already in use.
                c.start();

                // if the connection started successfully, add it to the
                // server and record the port in our data structures.
                server.addConnector(c);
                this.port = port;
                DEFAULT_ENV.put("SERVER_PORT", Integer.toString(this.port));
                break;

            } catch (Exception e) {
                // if we were unable to listen on the given port, increment
                // the number and try listening on the next higher port.
                port++;
            }
        }
    }


    public void setRoots(URL [] roots) {
        SET_ROOTS_PERMISSION.checkPermission();

        // find or build web apps for each of the given URLs
        List<WebAppContextDashboard> newWebApps = new ArrayList();
        for (URL u : roots)
            newWebApps.add(getWebAppForUrl(u));
        webApps.setHandlers(newWebApps.toArray(new Handler[newWebApps.size()]));

        writePackagesToDefaultEnv();
    }

    private WebAppContextDashboard getWebAppForUrl(URL u) {
        String webAppBase = u.toString();

        // Check to see if we already have a web application for this URL
        for (Object handler : webApps.getHandlers()) {
            WebAppContextDashboard webApp = (WebAppContextDashboard) handler;
            if (webAppBase.equals(webApp.templateUrl) && !webApp.isOutOfDate())
                return webApp;
        }

        // Create a new web application
        WebAppContextDashboard result;
        if (webAppBase.endsWith(WebAppContextDashboard.WEB_INF_URL_SUFFIX))
            result = new WebAppContextDashboard(u);
        else
            result = new WebAppContextLegacy(u);
        result.setServer(server);
        try {
            result.start();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to start web application for '"
                    + u + "'", e);
        }
        return result;
    }

    public boolean reloadWars() {
        SET_ROOTS_PERMISSION.checkPermission();

        // find WAR files that are out of date
        boolean needsReload = false;
        for (Object handler : webApps.getHandlers()) {
            WebAppContextDashboard webApp = (WebAppContextDashboard) handler;
            if (webApp.checkNeedsReload())
                needsReload = true;
        }

        // if any out of date WARs were found, reset the template roots
        if (needsReload) {
            logger.info("Starting reload of changed WAR files");
            setRoots(TemplateLoader.getTemplateURLs());
            logger.info("Finished reload of changed WAR files");
        }

        // let the caller know whether a reload was performed
        return needsReload;
    }


    /** Stop the web server. */
    public void quit() {
        QUIT_PERMISSION.checkPermission();

        try {
            for (Connector c : server.getConnectors())
                c.stop();
            for (Handler h : webApps.getHandlers())
                h.stop();
            server.stop();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unable to stop web server", e);
        }
    }

}
