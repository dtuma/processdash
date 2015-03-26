// Copyright (C) 2013-2014 Tuma Solutions, LLC
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

package net.sourceforge.processdash.log.ui.importer.reviewboard;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import net.sourceforge.processdash.ui.lib.binding.ErrorTokens;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.HTTPUtils;
import net.sourceforge.processdash.util.JSONUtils;
import net.sourceforge.processdash.util.StringUtils;

public class RBRestClient implements ErrorTokens {

    private JSONParser parser;

    private String rbSessionCookie;

    private Map<String, String> uriTemplates;

    private static final Logger log = Logger.getLogger(RBRestClient.class
            .getName());

    /**
     * Create an object which can make calls against the REST API of a
     * ReviewBoard server at a particular URL
     * 
     * @param baseUrl
     *            the URL of the Review Board server (should <b>not</b> include
     *            the "/api" suffix)
     * @throws IOException
     *             if the server could not be contacted
     */
    public RBRestClient(String baseUrl) throws IOException {
        this.parser = new JSONParser();
        this.uriTemplates = getUriTemplates(baseUrl);
    }

    /** Retrieve the list of API URI templates from the server */
    private Map<String, String> getUriTemplates(String baseUrlStr)
            throws IOException {
        baseUrlStr = baseUrlStr.trim();
        if (!baseUrlStr.startsWith("http"))
            throw new MalformedURLException("Bad URL: " + baseUrlStr);
        if (!baseUrlStr.endsWith("/"))
            baseUrlStr = baseUrlStr + "/";
        URL baseUrl = new URL(baseUrlStr);
        URL apiUrl = new URL(baseUrl, "api/");

        JSONObject response = makeRequest(apiUrl);
        return (Map<String, String>) response.get("uri_templates");
    }


    /**
     * Authenticate this object using a given username and password
     * 
     * @return true if the authentication succeeded, false otherwise
     */
    public boolean authenticate(String username, String password) {
        try {
            URL sessionUrl = getUrl("session");
            String credential = HTTPUtils.calcCredential(username, password);
            JSONObject response = makeRequest(sessionUrl, "Authorization",
                credential);

            Map session = (Map) response.get("session");
            Object isAuthenticated = session.get("authenticated");
            return (isAuthenticated == Boolean.TRUE);

        } catch (IOException ioe) {
            // bad credentials trigger an HTTP error code from the server,
            // and Java will translate those into an IOException when we try
            // to read the response body.
            return false;
        }
    }


    /**
     * Perform a GET request to the server
     * 
     * @param uriTemplateKey
     *            the string key of a particular URI API template, as returned
     *            by the Review Board "/api" method. To make a nonstandard call
     *            which is not included in that list, pass in a complete URL
     *            beginning with "http".
     * @param args
     *            a list of arguments to pass to the method. Arguments in curly
     *            braces will be interpolated into the URI template provided by
     *            Review Board. Other arguments will be added as query
     *            parameters. Arguments should be passed in pairs, with a name
     *            followed by a value. Optionally, a final string can be passed
     *            to extract a particular value from the JSON response; see the
     *            return value below for more details.
     * @return if an even number of args (or zero) were passed, this method will
     *         return a JSONObject containing the data parsed from the response.
     *         If an odd number of args was passed, the final arg will be used
     *         in a call to the {@link JSONUtils#lookup(Map, String, boolean)}
     *         method, and the result of that call will be returned.
     * @throws IOException
     *             if a problem is encountered when connecting to the server
     */
    public <T> T performGet(String uriTemplateKey, String... args)
            throws IOException {
        URL u = getUrl(uriTemplateKey, args);
        JSONObject response = makeRequest(u);

        // if an even number of args were passed, return the JSON result
        if ((args.length & 1) == 0)
            return (T) response;

        // if an odd number of args were passed, the final arg is a path to
        // an object within the response that we should look up for the caller.
        String path = args[args.length - 1];
        Object result = JSONUtils.lookup(response, path, false);
        return (T) result;
    }

    private URL getUrl(String uriTemplateKey, String... args)
            throws IOException {
        StringBuffer template = new StringBuffer(getUrlTemplate(uriTemplateKey));

        for (int i = 0; i < args.length - 1; i += 2) {
            String argName = args[i];
            String argValue = args[i + 1];
            if (argName.startsWith("{"))
                StringUtils.findAndReplace(template, argName,
                    HTMLUtils.urlEncode(argValue));
            else
                HTMLUtils.appendQuery(template, argName, argValue);
        }

        return new URL(template.toString());
    }

    private String getUrlTemplate(String key) {
        if (key.startsWith("http"))
            return key;
        else
            return uriTemplates.get(key);
    }

    private JSONObject makeRequest(URL u, String... properties)
            throws IOException {
        long start = System.currentTimeMillis();

        // open the connection and set the request properties
        URLConnection conn = u.openConnection();
        conn.setRequestProperty("Accept", "application/json");
        for (int i = 0; i < properties.length; i += 2)
            conn.setRequestProperty(properties[i], properties[i + 1]);
        if (rbSessionCookie != null)
            conn.addRequestProperty("Cookie", rbSessionCookie);
        conn.connect();

        // check for an authorization cookie, and save if present
        String newCookies = conn.getHeaderField("Set-Cookie");
        if (newCookies != null) {
            for (String oneCookie : newCookies.split(";")) {
                oneCookie = oneCookie.trim();
                if (oneCookie.startsWith("rbsessionid")) {
                    rbSessionCookie = oneCookie;
                    break;
                }
            }
        }

        // parse the response body as a JSONObject
        InputStream inStr = new BufferedInputStream(conn.getInputStream());
        Reader in = new InputStreamReader(inStr, "UTF-8");
        try {
            return (JSONObject) parser.parse(in);
        } catch (Exception e) {
            IOException ioe = new IOException("Could not parse JSON response");
            ioe.initCause(e);
            throw ioe;
        } finally {
            long end = System.currentTimeMillis();
            long delta = end - start;
            log.finer("Took " + delta + " ms to call " + u);
        }
    }

    /**
     * Parse a date value returned from the Review Board API.
     */
    public static Date parseDate(Object val) {

        // The Review Board API claims that all dates will be returned in
        // YYYY-MM-DD HH:MM:SS format, but that claim does not appear to hold
        // true. Different versions of the server might return dates in that
        // format or in an ISO format. Fortunately, the alternate format does
        // include substrings matching the date and time portions above. So to
        // remediate, we find and extract those portions, use them to build a
        // date in canonical format, then parse that reformatted date.

        try {
            if (val instanceof String && StringUtils.hasValue((String) val)) {
                String dateStr = reformatDate((String) val);
                if (dateStr != null)
                    return JSON_DATE_FMT.parse(dateStr);
            }
        } catch (ParseException e) {
        }

        return null;
    }

    private static String reformatDate(String val) {
        Matcher date = JSON_DATE_PAT.matcher(val);
        if (!date.find())
            return null;

        Matcher time = JSON_TIME_PAT.matcher(val);
        if (!time.find())
            return null;

        return date.group() + " " + time.group();
    }

    private static final Pattern JSON_DATE_PAT = Pattern
            .compile("\\d{4}-\\d{2}-\\d{2}");

    private static final Pattern JSON_TIME_PAT = Pattern
            .compile("\\d{2}:\\d{2}:\\d{2}");

    private static final DateFormat JSON_DATE_FMT = new SimpleDateFormat(
            "yyyy-MM-dd hh:mm:ss");

}
