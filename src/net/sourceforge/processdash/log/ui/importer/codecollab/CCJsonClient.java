// Copyright (C) 2021 Tuma Solutions, LLC
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

package net.sourceforge.processdash.log.ui.importer.codecollab;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import net.sourceforge.processdash.util.HTTPUtils;
import net.sourceforge.processdash.util.HttpException;
import net.sourceforge.processdash.util.StringUtils;

public class CCJsonClient {

    private URL baseUrl, apiUrl;

    private JSONParser parser;

    private String username;

    private String loginTicket;

    public CCJsonClient(String url) throws MalformedURLException {
        this.baseUrl = new URL(getBaseUrl(url));
        this.apiUrl = new URL(baseUrl, "services/json/v1");
        this.parser = new JSONParser();
    }

    private String getBaseUrl(String url) {
        // clean whitespace
        url = url.trim();

        // remove query and anchor from URL, if present
        int pos = url.indexOf('?');
        if (pos != -1)
            url = url.substring(0, pos);
        pos = url.indexOf('#');
        if (pos != -1)
            url = url.substring(0, pos);

        // if the user has provided a URL within the app, strip to base
        if (url.endsWith("/go") || url.endsWith("/ui"))
            url = url.substring(0, url.length() - 2);

        // make sure our final URL ends with a slash
        if (!url.endsWith("/"))
            url = url + "/";

        // return the cleaned up URL
        return url;
    }


    /**
     * @return true if this server is reachable, and is a newer server that
     *         supports the Collaborator JSON API.
     * @throws IOException
     *             if the server is unreachable
     */
    public boolean isServerWithJsonSupport() throws IOException {
        // try connecting to the base URL of the server. If that fails, throw
        // an HttpException to indicate the server isn't reachable.
        URLConnection conn = baseUrl.openConnection();
        HttpException.checkValid(conn);
        conn.getInputStream().close();

        // try connecting to the JSON API. Return true on success
        try {
            execute(command("ServerInfoService.getVersion"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * Authenticate this object using a given username and password
     * 
     * @return true if the authentication succeeded, false otherwise
     */
    public boolean authenticate(String username, String password) {
        try {
            // make an API call to the SessionService to login
            List<JSONObject> response = execute(
                command("SessionService.getLoginTicket", //
                    "login", username, "password", password));

            // extract the login ticket from the JSON response
            JSONObject result = (JSONObject) response.get(0).get("result");
            this.loginTicket = (String) result.get("loginTicket");
            this.username = username;
            return StringUtils.hasValue(loginTicket);

        } catch (Exception ioe) {
            return false;
        }
    }


    /**
     * Send a command to the JSON API and return the result.
     * 
     * @param command
     *            the name of the command
     * @param args
     *            any arguments to send (can be empty)
     * @return the result returned by the JSON API
     * @throws IOException
     *             if an error is encountered
     */
    public JSONObject execute(String command, Object... args)
            throws IOException {
        return execute(authenticate(), command(command, args)).get(1);
    }

    private JSONObject authenticate() {
        return command("SessionService.authenticate", //
            "login", username, "ticket", loginTicket);
    }


    private List<JSONObject> execute(Object... commands) throws IOException {
        // build the JSON to post to the Collaborator API
        String jsonText = JSONArray.toJSONString(list(commands));
        byte[] jsonBytes = jsonText.getBytes("UTF-8");
        String contentLength = Integer.toString(jsonBytes.length);

        // make a connection to the API
        HttpURLConnection conn = (HttpURLConnection) apiUrl.openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", JSON_CONTENT_TYPE);
        conn.setRequestProperty("Content-Length", contentLength);

        // post the JSON request body
        OutputStream out = conn.getOutputStream();
        out.write(jsonBytes);
        out.close();

        // retrieve the response stream
        InputStream response = new BufferedInputStream(conn.getInputStream());
        String charset = HTTPUtils.getCharset(conn.getContentType(), "UTF-8");
        Reader reader = new InputStreamReader(response, charset);

        // read and parse the results returned by the API
        try {
            synchronized (parser) {
                return (List<JSONObject>) parser.parse(reader);
            }
        } catch (ParseException pe) {
            throw new IOException("Invalid JSON returned from " + apiUrl, pe);
        } finally {
            reader.close();
        }
    }

    private JSONObject command(String commandName, Object... args) {
        return command(commandName, map(args));
    }

    private JSONObject command(String commandName, JSONObject args) {
        JSONObject result = map("command", commandName);
        if (!args.isEmpty())
            result.put("args", args);
        return result;
    }

    private JSONObject map(Object... args) {
        JSONObject result = new JSONObject();
        for (int i = 0; i < args.length; i += 2)
            result.put(args[i], args[i + 1]);
        return result;
    }

    private List list(Object... args) {
        return Arrays.asList(args);
    }

    private static final String JSON_CONTENT_TYPE = "application/json;charset=utf-8";

}
