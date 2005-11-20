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


package net.sourceforge.processdash.ui.web;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.cache.ObjectCache;
import net.sourceforge.processdash.net.http.TinyCGI;
import net.sourceforge.processdash.net.http.WebServer;
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
        int length;
        try {
            length = Integer.parseInt((String) env.get("CONTENT_LENGTH"));
        } catch (Exception e) { return; }

        byte [] messageBody = new byte[length];
        int bytesRead = inStream.read(messageBody);
        parseInput((String) env.get("SCRIPT_PATH"),
                   // FIXME: does this handle character sets correctly?
                   new String(messageBody, 0, bytesRead));
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

    protected String cssLinkHTML() {
        String style = (String) parameters.get("style");
        if (style == null)
            style = "/style.css";
        return "<LINK REL='stylesheet' TYPE='text/css' HREF='" + style + "'>";
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
