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


package net.sourceforge.processdash.ui.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.StringTokenizer;
import java.util.UUID;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.cache.ObjectCache;
import net.sourceforge.processdash.net.http.TinyCGI;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.ui.Browser;
import net.sourceforge.processdash.util.DateUtils;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.MultipartRequest;
import net.sourceforge.processdash.util.StringMapper;
import net.sourceforge.processdash.util.StringUtils;

public class TinyCGIBase implements TinyCGI {

    static String DEFAULT_CHARSET = "ISO-8859-1";

    protected InputStream inStream = null;
    protected OutputStream outStream = null;
    protected PrintWriter out = null;
    protected Map env = null;
    protected Map parameters = new HashMap();
    protected Interpolator interpolator = null;
    protected String charset = getDefaultCharset();
    /** @since 1.13.0.6 */
    protected String requestCharset = null;

    public void service(InputStream in, OutputStream out, Map env)
        throws IOException
    {
        this.inStream = in;
        this.outStream = out;
        this.out = new PrintWriter(new OutputStreamWriter(outStream, charset));
        this.env = env;
        parameters.clear();
        parseInput((String) env.get("SCRIPT_PATH"),
                   (String) env.get("QUERY_STRING"));
        if ("POST".equalsIgnoreCase((String) env.get("REQUEST_METHOD")))
            doPost();
        else
            doGet();
        this.out.flush();
    }

    /** Parse CGI query parameters, and store them in the Map
     *  <code>parameters</code>.
     *
     * Single valued parameters can be fetched directly from the map.
     * Multivalued parameters are stored in the map as String arrays,
     * with "_ALL" appended to the name.  (So a query string
     * "name=foo&name=bar" would result in a 2-element string array
     * being placed in the map under the key "name_ALL".)
     */
    protected void parseInput(String context, String query) throws IOException {
        if (query == null || query.length() == 0) return;

        String delim = "&";
        boolean urlDecode = true;
        boolean interpolate = false;
        if (query.indexOf('\n') != -1) {
            delim = "\r\n";
            urlDecode = false;
            interpolate = true;
        }
        StringTokenizer params = new StringTokenizer(query, delim);
        String param, name, val;
        int equalsPos;
        while (params.hasMoreTokens()) {
            param = params.nextToken();
            equalsPos = param.indexOf('=');
            if (equalsPos == 0 || param.length() == 0)
                continue;
            else if (equalsPos == -1)
                parameters.put(HTMLUtils.urlDecode(param), Boolean.TRUE);
            else try {
                name = HTMLUtils.urlDecode(param.substring(0, equalsPos));
                val = param.substring(equalsPos+1);
                // skip URL decoding if the value begins with "=".  This
                // saves us from having to URL-encode complex expressions
                // in query files.
                if (val.startsWith("=")) val = val.substring(1);
                else if (urlDecode) val = HTMLUtils.urlDecode(val);

                if (supportQueryFiles() && QUERY_FILE_PARAM.equals(name))
                    parseInputFile(context, val);
                else if (supportQueryFiles() && RESOURCE_FILE_PARAM.equals(name))
                    parseResourceFile(context, val);
                else {
                    if (interpolate)
                        val = StringUtils.interpolate(getInterpolator(), val);
                    putParam(name, val);
                }
            } catch (Exception e) {
                System.err.println("Malformed query parameter: " + param);
            }
        }
    }
    public static final String QUERY_FILE_PARAM = "qf";
    public static final String RESOURCE_FILE_PARAM = "rf";

    private void putParam(String name, String val) {
        parameters.put(name, val);
        name = name + "_ALL";
        parameters.put
            (name, append((String[]) parameters.get(name), val));
    }

    /* Read name=value pairs from POSTed form data. */
    protected void parseFormData() throws IOException {
        int length = -1;
        try {
            length = Integer.parseInt((String) env.get("CONTENT_LENGTH"));
        } catch (Exception e) {}
        if (length <= 0)
            return;

        byte [] messageBody = new byte[length];
        FileUtils.readAndFillArray(inStream, messageBody);

        String charset = requestCharset;
        if (charset == null)
            charset = this.charset;
        parseInput((String) env.get("SCRIPT_PATH"),
                   new String(messageBody, 0, length, charset));
    }

    /* Read name=value pairs, and uploaded files, from multipart form data.
     *
     * For each uploaded file with parameter name "foo", the following
     * entries will appear in the parameter list:<ul>
     * <li>foo - the name of the uploaded file.
     * <li>foo_SIZE - the size of the uploaded file (a Long).
     * <li>foo_TYPE - the content type of the uploaded file
     * <li>foo_CONTENTS - the contents of the uploaded file (byte[]).
     * </ul>
     */
    protected void parseMultipartFormData() throws IOException {
        String contentType = (String) env.get("CONTENT_TYPE");
        int contentLength;
        try {
            contentLength =
                Integer.parseInt((String) env.get("CONTENT_LENGTH"));
        } catch (Exception e) { return; }

        try {
            // Parse the incoming multipart form data. This may throw
            // an IllegalArgumentException if the incoming data is not
            // multipart/form data.
            MultipartRequest req = new MultipartRequest
                (new PrintWriter(System.out), contentType, contentLength,
                 inStream, MultipartRequest.MAX_READ_BYTES);

            // copy the name/value pairs from the request into our
            // list of parameters.
            Enumeration parameterNames = req.getParameterNames();
            while (parameterNames.hasMoreElements()) {
                String name = (String) parameterNames.nextElement();
                Enumeration values = req.getURLParameters(name);
                while (values.hasMoreElements())
                    putParam(name, (String) values.nextElement());
            }

            // fetch all the files read, and store them into our
            // parameters map.
            parameterNames = req.getFileParameterNames();
            while (parameterNames.hasMoreElements()) {
                String name = (String) parameterNames.nextElement();
                parameters.put(name, req.getFileSystemName(name));
                parameters.put
                    (name + "_SIZE",
                     req.getFileParameter(name, MultipartRequest.SIZE));
                parameters.put
                    (name + "_TYPE",
                     req.getFileParameter(name, MultipartRequest.CONTENT_TYPE));
                parameters.put
                    (name + "_CONTENTS",
                     req.getFileParameter(name, MultipartRequest.CONTENTS));
            }

        } catch (IllegalArgumentException iae) {
            parseFormData();
        }
    }


    /* Read name=value pairs from the given URI. If the URI is not
     * absolute (e.g. "/reports/foo"), it is interpreted relative
     * to the current request. */
    protected void parseInputFile(String scriptPath, String filename) throws IOException {
        if (filename == null || filename.length() == 0) return;

        WebServer t = getTinyWebServer();
        String origFilename = filename;
        try {
            filename = resolveRelativeURI(scriptPath, filename);
            parseInput(filename, t.getRequestAsString(filename));

            // now try looking for a companion resource bundle, and load
            // values from it as well.
            try {
                String bundleName = filename;
                int pos = bundleName.indexOf("//");
                if (pos != -1)
                    bundleName = bundleName.substring(pos+1);
                pos = bundleName.lastIndexOf('.');
                if (pos != -1 && bundleName.indexOf('/', pos) == -1)
                    bundleName = bundleName.substring(0, pos);

                Resources bundle = Resources.getTemplateBundle(bundleName);
                parameters.putAll(bundle.asMap());
            } catch (Exception e) {
                // it is not an error if no companion bundle was found.
            }

        } catch (IOException ioe) {
            System.out.println("Couldn't read file: " + filename);
            System.out.println("(Specified as '" + origFilename + "' from '" +
                               scriptPath +"')");
        }
    }

    protected void parseResourceFile(String context, String filename)
            throws IOException
    {
        String bundleName = resolveRelativeURI(context, filename);

        int pos = bundleName.indexOf("//");
        if (pos != -1)
            bundleName = bundleName.substring(pos+1);
        pos = bundleName.lastIndexOf('.');
        if (pos != -1 && bundleName.indexOf('/', pos) == -1)
            bundleName = bundleName.substring(0, pos);

        try {
            Resources bundle = Resources.getTemplateBundle(bundleName);
            getInterpolator().addResources(bundle);
        } catch (MissingResourceException mre) {
            System.out.println("Couldn't find resource file: " + bundleName);
            System.out.println("(Specified as '" + filename + "' from '" +
                               context +"')");
        }
    }

    public String resolveRelativeURI(String context, String uri) {
        if (uri == null || uri.startsWith("/") || uri.startsWith("http:"))
            return uri;

        try {
            if (!context.startsWith("http:"))
                context = "http://unimportant" + context;
            URL cntxt = new URL(context);
            URL file = new URL(cntxt, uri);
            return file.getFile();
        } catch (MalformedURLException mue) {
            return uri;
        }
    }
    public String resolveRelativeURI(String uri) {
        return resolveRelativeURI((String) env.get("REQUEST_URI"), uri);
    }

    private String[] append(String [] array, String element) {
        String [] result;
        result = new String[array == null ? 1 : array.length + 1];
        if (array != null)
            System.arraycopy(array, 0, result, 0, array.length);
        result[result.length-1] = element;
        return result;
    }

    /**
     * Checks the HTTP Referer header to see whether it matches the default host
     * and port that this dashboard is using to serve documents.
     * 
     * @since 1.14.0.1
     */
    protected boolean checkReferer() {
        return checkReferer("/");
    }

    /**
     * Checks the HTTP Referer header to see whether it matches the default host
     * and port that this dashboard is using to serve documents, and that the
     * initial URI is also the same.
     * 
     * @since 1.14.0.1
     */
    protected boolean checkReferer(String uriPrefix) {
        String referer = (String) env.get("HTTP_REFERER");
        if (referer == null)
            return false;

        String expectedPrefix = Browser.mapURL(uriPrefix);
        return referer.startsWith(expectedPrefix);
    }


    /**
     * Generate a new, unique token and save it in the data repository.
     * 
     * @since 1.14.0.1
     */
    protected String generatePostToken() {
        return generatePostToken(getDefaultPostTokenDataNameSuffix());
    }

    /**
     * Generate a new, unique token and save it in the data repository.
     * 
     * @param dataNameSuffix
     *            a suffix to use when constructing the name of the data
     *            repository data element
     * @return a new, unique token.
     * @since 1.14.0.1
     */
    protected String generatePostToken(String dataNameSuffix) {
        UUID uuid = UUID.randomUUID();
        String result = uuid.toString();
        String dataName = getPostTokenDataName(dataNameSuffix);
        getDataRepository().putValue(dataName, StringData.create(result));
        getDataRepository().putValue(dataName + "/TS", new DateData());
        return result;
    }

    /**
     * Retrieve the token that was previously generated by
     * {@link #generatePostToken()}
     * 
     * @since 1.14.0.1
     */
    protected String getPostToken() {
        return getPostToken(getDefaultPostTokenDataNameSuffix());
    }

    /**
     * Retrieve the token that was previously generated by
     * {@link #generatePostToken(String)}
     *
     * @param dataNameSuffix the suffix that was used to generate the token
     * @since 1.14.0.1
     */
    protected String getPostToken(String dataNameSuffix) {
        String dataName = getPostTokenDataName(dataNameSuffix);
        SimpleData storedToken = getDataRepository().getSimpleValue(dataName);
        SimpleData storedDate = getDataRepository().getSimpleValue(dataName+"/TS");
        if (storedToken != null && storedDate instanceof DateData) {
            DateData date = (DateData) storedDate;
            long age = System.currentTimeMillis() - date.getValue().getTime();
            if (age > 0 && age < getPostTokenAgeTimeout())
                return storedToken.format();
        }

        return null;
    }

    /**
     * Write an HTML "input" tag of type "hidden" that records the value of the
     * post token in an HTML form.
     * 
     * @param forceGenerateNew
     *            if true, a new unique token will always be generated. If
     *            false, a token will only be generated if one is not already
     *            present.
     * @since 1.14.0.1
     */
    protected void writePostTokenFormElement(boolean forceGenerateNew) {
        writePostTokenFormElement(getDefaultPostTokenParamName(),
            getDefaultPostTokenDataNameSuffix(), forceGenerateNew);
    }

    /**
     * Write an HTML "input" tag of type "hidden" that records the value of the
     * post token in an HTML form.
     * 
     * @param postParamName
     *            the name to use for the hidden field
     * @param dataNameSuffix
     *            the name previously used in the call to
     *            {@link #generatePostToken(String)}
     * @param forceGenerateNew
     *            if true, a new unique token will be generated. If false, a
     *            token will only be generated if one is not already present.
     * @since 1.14.0.1
     */
    protected void writePostTokenFormElement(String postParamName,
            String dataNameSuffix, boolean forceGenerateNew) {
        String token = null;
        if (forceGenerateNew == false)
            token = getPostToken(dataNameSuffix);
        if (token == null)
            token = generatePostToken(dataNameSuffix);

        out.write("<input type=\"hidden\" name=\"");
        out.write(HTMLUtils.escapeEntities(postParamName));
        out.write("\" value=\"");
        out.write(HTMLUtils.escapeEntities(token));
        out.write("\">");
    }

    /**
     * Checks to see whether the post token was included with this POST request.
     * 
     * @return true if the current request was a POST request that included the
     *         previously stored value of the post token; false otherwise
     * @since 1.14.0.1
     */
    protected boolean checkPostToken() {
        return checkPostToken(getDefaultPostTokenParamName(),
            getDefaultPostTokenDataNameSuffix());
    }

    /**
     * Checks to see whether the post token was included with this POST request.
     * 
     * @param postParamName
     *            the name of the post parameter that should contain the token
     * @param dataNameSuffix
     *            the name previously used in the call to
     *            {@link #generatePostToken(String)}
     * @return true if the current request was a POST request that included the
     *         previously stored value of the post token; false otherwise
     * @since 1.14.0.1
     */
    protected boolean checkPostToken(String postParamName, String dataNameSuffix) {
        String method = (String) env.get("REQUEST_METHOD");
        if (! "POST".equalsIgnoreCase(method))
            return false;

        String storedToken = getPostToken(dataNameSuffix);
        String postedToken = getParameter(postParamName);
        return (storedToken != null && storedToken.equals(postedToken));
    }

    protected String getDefaultPostTokenParamName() {
        return "__POST_TOKEN";
    }

    protected String getDefaultPostTokenDataNameSuffix() {
        return "DEFAULT";
    }

    protected long getPostTokenAgeTimeout() {
        return 10 * DateUtils.MINUTES;
    }

    private String getPostTokenDataName(String suffix) {
        return getPrefix() + "//POST_TOKEN//" + suffix.replace('.', ' ');
    }


    /**
     * Some scripts may wish to display a page, then redirect to another page
     * after a short delay. For example, this is a common way to display a
     * "please wait" page, followed by a page that actually performs the work.
     * 
     * A meta-refresh tag would normally be used for this purpose, but some web
     * browsers (namely, newer versions of IE with High security enabled) ignore
     * the meta refresh tag. This method writes a block of HTML that can be
     * included within the &lt;head&gt; block of a webpage.
     * 
     * @param uri the uri to redirect to
     * @param delay the number of seconds to wait before redirecting
     * @since 1.14.3
     */
    protected void writeRedirectInstruction(String uri, int delay) {
        if (Settings.getBool("http.useMetaRefresh", false)) {
            out.write("<meta http-equiv=\"Refresh\" content=\"" + delay
                    + ";URL=" + uri + "\">");
        } else {
            out.write(HTMLUtils.redirectScriptHtml(uri, delay));
        }
    }

    protected String cssLinkHTML() {
        String style = (String) parameters.get("style");
        if (style == null)
            style = "/style.css";
        return "<LINK REL='stylesheet' TYPE='text/css' HREF='" + style + "'>";
    }

    /** Get a context for retrieving and setting data */
    protected DataContext getDataContext() {
        return getDataRepository().getSubcontext(getPrefix());
    }

    /** Get the data repository servicing this request. */
    protected DataRepository getDataRepository() {
        return getDashboardContext().getData();
    }
    /** Get the tiny web server that is running this request. */
    protected WebServer getTinyWebServer() {
        return getDashboardContext().getWebServer();
    }
    /** Get the PSPProperties object */
    protected DashHierarchy getPSPProperties() {
        return getDashboardContext().getHierarchy();
    }
    protected ObjectCache getObjectCache() {
        return getDashboardContext().getCache();
    }
    protected DashboardContext getDashboardContext() {
        return (DashboardContext) env.get(DASHBOARD_CONTEXT);
    }
    /** Perform an internal http request. */
    protected byte[] getRequest(String uri, boolean skipHeaders)
        throws IOException {
        return getTinyWebServer().getRequest(uri, skipHeaders);
    }
    protected String getRequestAsString(String uri) throws IOException {
        return getTinyWebServer().getRequestAsString(uri);
    }
    /** Fetch a named query parameter */
    protected String getParameter(String name) {
        return (String) parameters.get(name);
    }
    /**
     * Return the base URL of the dashboard web server, as it was accessed
     * during the HTTP request that invoked this script.  The result will
     * include the protocol, the hostname, and the port.  It will not include
     * a trailing slash.
     * 
     * Examples: http://localhost:2468 or http://somehostname:3000
     * 
     * @since 1.14.3.1
     */
    protected String getRequestURLBase() {
        WebServer ws = getTinyWebServer();

        // get the host that was used to make this request from the http headers
        String fullHostAndPort = (String) env.get("HTTP_HOST");
        if (StringUtils.hasValue(fullHostAndPort))
            return "http://" + fullHostAndPort;

        // if the http request did not contain a host header, reconstruct the
        // host and port from other values at our disposal.
        String host = (String) env.get("SERVER_ADDR");
        if (!StringUtils.hasValue(host))
            host = ws.getHostName(true);

        // get the port number that the server is listening on
        int port = ws.getPort();

        // return the appropriate value
        return "http://" + host + ":" + port;
    }

    /** get the effective prefix, set via the URL */
    protected String getPrefix() {
        String result = (String) parameters.get("hierarchyPath");
        if (result == null)
            result = (String) env.get("PATH_TRANSLATED");
        else if (!result.startsWith("/")) {
            String prefix = (String) env.get("PATH_TRANSLATED");
            if (prefix == null)
                prefix = "";
            result = prefix + "/" + result;
        }
        return result;
    }
    /** get the name of the person who owns the data in the repository */
    protected String getOwner() {
        return ProcessDashboard.getOwnerName(getDataRepository());
    }

    /** Does this CGI script want to support query parameter files?
     * child classes that DO NOT want query parameter support should
     * override this method to return false. */
    protected boolean supportQueryFiles() { return true; }

    /** Is the current request occurring on behalf of an export operation? */
    protected boolean isExporting() {
        return parameters.containsKey("EXPORT");
    }

    /** Is the current request occurring on behalf of an export-to-excel
     * operation? */
    protected boolean isExportingToExcel() {
        return "excel".equals(parameters.get("EXPORT"));
    }

    /** Handle an HTTP POST request */
    protected void doPost() throws IOException {
        writeHeader();
        writeContents();
    }


    /** Handle an HTTP GET request */
    protected void doGet() throws IOException {
        writeHeader();
        writeContents();
    }


    /** Write a standard CGI header.
     *
     * This method can be overridden by children that might need to generate
     * a special header, or might need to vary the header on the fly.
     */
    protected void writeHeader() {
        out.print("Content-type: text/html; charset="+charset+"\r\n\r\n");
        // flush in case writeContents wants to use outStream instead of out.
        out.flush();
    }


    /** Generate CGI script output.
     *
     * This method should be overridden by child classes to generate
     * the contents of the script.
     */
    protected void writeContents() throws IOException {
        out.println("<HTML><BODY>");
        out.println("This space intentionally left blank.");
        out.println("</BODY></HTML>");
    }

    /** Get the name of the default character set to be used for CGI output.
     */
    public static String getDefaultCharset() {
        return WebServer.getOutputCharset();
    }

    protected Interpolator getInterpolator() {
        if (interpolator == null)
            interpolator = new Interpolator();
        return interpolator;
    }

    protected class Interpolator implements StringMapper {

        LinkedList resources = new LinkedList();

        public String getString(String key) {
            if (parameters.containsKey(key))
                return (String) parameters.get(key);

            Iterator i = resources.iterator();
            while (i.hasNext()) {
                Resources r = (Resources) i.next();
                try {
                    return r.getString(key);
                } catch (Exception e) {}
            }

            return "";
        }

        public void addResources(Resources r) {
            resources.add(r);
        }
    }
}
