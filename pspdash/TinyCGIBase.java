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


package pspdash;

import pspdash.data.DataRepository;
import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;

public class TinyCGIBase implements TinyCGI {

    protected InputStream inStream = null;
    protected OutputStream outStream = null;
    protected PrintWriter out = null;
    protected Map env = null;
    protected Map parameters = new HashMap();

    public void service(InputStream in, OutputStream out, Map env)
        throws IOException
    {
        this.inStream = in;
        this.outStream = out;
        this.out = new PrintWriter(out);
        this.env = env;
        parameters.clear();
        parseInput((String) env.get("QUERY_STRING"));
        writeHeader();
        writeContents();
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
    protected void parseInput(String query) throws IOException {
        if (query == null || query.length() == 0) return;

        String delim = (query.indexOf('\n') == -1) ? "&" : "\r\n";
        StringTokenizer params = new StringTokenizer(query, delim);
        String param, name, val;
        int equalsPos;
        while (params.hasMoreTokens()) {
            param = params.nextToken();
            equalsPos = param.indexOf('=');
            if (equalsPos == 0 || param.length() == 0)
                continue;
            else if (equalsPos == -1)
                parameters.put(URLDecoder.decode(param), Boolean.TRUE);
            else try {
                name = URLDecoder.decode(param.substring(0, equalsPos));
                val = param.substring(equalsPos+1);
                // skip URL decoding if the value begins with "=".  This
                // saves us from having to URL-encode complex expressions
                // in query files.
                if (val.startsWith("=")) val = val.substring(1);
                else val = URLDecoder.decode(val);
                if (supportQueryFiles() && QUERY_FILE_PARAM.equals(name))
                    parseInputFile(val);
                else {
                    parameters.put(name, val);
                    name = name + "_ALL";
                    parameters.put
                        (name, append((String[]) parameters.get(name), val));
                }
            } catch (Exception e) {
                System.err.println("Malformed query parameter: " + param);
            }
        }
    }
    public static final String QUERY_FILE_PARAM = "qf";

    /* Read name=value pairs from POSTed form data. */
    protected void parseFormData() throws IOException {
        parseInput(new String(TinyWebServer.slurpContents(inStream, false)));
    }

    /* Read name=value pairs from the given URI. If the URI is not
     * absolute (e.g. "/reports/foo"), it is interpreted relative
     * to the current request. */
    protected void parseInputFile(String filename) throws IOException {
        if (filename == null || filename.length() == 0) return;

        TinyWebServer t = getTinyWebServer();
        String origFilename = filename;
        String scriptPath = (String) env.get("SCRIPT_PATH");
        try {
            if (!filename.startsWith("/")) {
                URL context = new URL("http://unimportant" + scriptPath);
                URL file = new URL(context, filename);
                filename = file.getFile();
            }
            env.put("SCRIPT_PATH", filename);
            parseInput(new String(t.getRequest(filename, true)));
        } catch (IOException ioe) {
            System.out.println("Couldn't read file: " + filename);
            System.out.println("(Specified as '" + origFilename + "' from '" +
                               scriptPath +"')");
        } finally {
            env.put("SCRIPT_PATH", scriptPath);
        }
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
        return (DataRepository) env.get(DATA_REPOSITORY);
    }
    /** Get the tiny web server that is running this request. */
    protected TinyWebServer getTinyWebServer() {
        return (TinyWebServer) env.get(TINY_WEB_SERVER);
    }
    /** Get the PSPProperties object */
    protected PSPProperties getPSPProperties() {
        return (PSPProperties) env.get(PSP_PROPERTIES);
    }
    /** Perform an internal http request. */
    protected byte[] getRequest(String uri, boolean skipHeaders)
        throws IOException {
        return getTinyWebServer().getRequest(uri, skipHeaders);
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
        DataRepository data = getDataRepository();
        String result = data.getSimpleValue("/Owner").format();
        if ("Enter your name".equals(result))
            return null;
        else
            return result;
    }

    /** Does this CGI script want to support query parameter files?
     * child classes that DO NOT want query parameter support should
     * override this method to return false. */
    protected boolean supportQueryFiles() { return true; }


    /** Write a standard CGI header.
     *
     * This method can be overridden by children that might need to generate
     * a special header, or might need to vary the header on the fly.
     */
    protected void writeHeader() {
        out.print("Content-type: text/html\r\n\r\n");
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
}
