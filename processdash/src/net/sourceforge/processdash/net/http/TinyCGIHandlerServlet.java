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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.net.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.processdash.util.HTMLUtils;

public class TinyCGIHandlerServlet extends HttpServlet {

    private String hardcodedLinkContents;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        hardcodedLinkContents = config.getInitParameter("linkData");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        handleLink(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        handleLink(req, resp);
    }

    protected void handleLink(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // open the link file which describes this CGI script
        BufferedReader linkContents = getLinkContents(req);
        if (linkContents == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // build the CGI environment and parse the link file
        Map env = PDashServletUtils.buildEnvironment(req);
        TinyCGI script = getScript(req, linkContents);
        parseLinkParameters(env, linkContents);

        // run the script
        runScript(req, resp, env, script);
    }


    /**
     * Open the contents of the ".link" file that specifies this TinyCGI script.
     */
    private BufferedReader getLinkContents(HttpServletRequest req)
            throws IOException {

        // if this servlet instance was configured to point to a specific
        // TinyCGI script, return that link data.
        if (hardcodedLinkContents != null) {
            return new BufferedReader(new StringReader(hardcodedLinkContents));
        }

        // otherwise, load the link file from the template path.
        String uri = req.getServletPath();
        String linkUri = uri + TinyCGIHandlerServlet.LINK_SUFFIX;
        InputStream in = getServletContext().getResourceAsStream(linkUri);
        if (in == null)
            return null;
        else
            return new BufferedReader(new InputStreamReader(in, "UTF-8"));
    }


    /**
     * Retrieve a TinyCGI script object named by the ".link" file
     */
    private TinyCGI getScript(HttpServletRequest req, BufferedReader link) {
        try {
            // read the first line of the link file, which names the CGI class
            String linkTarget = link.readLine();
            if (linkTarget == null || !linkTarget.startsWith(CGI_LINK_PREFIX))
                return null;

            String className = linkTarget.substring(CGI_LINK_PREFIX.length())
                    .trim();
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Class clazz = Class.forName(className, true, cl);
            return (TinyCGI) clazz.newInstance();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Parse the parameters specified in the link file, and add them to the
     * QUERY_STRING in the environment
     */
    private void parseLinkParameters(Map env, BufferedReader linkContents)
            throws IOException {
        StringBuilder query = new StringBuilder();
        String param;

        // read each line of the file.
        while ((param = linkContents.readLine()) != null) {
            int equalsPos = param.indexOf('=');
            if (equalsPos == 0 || param.length() == 0) {
                // ignore empty lines and lines starting with '='
                continue;

            } else if (equalsPos == -1)
                // if there is no value, append the name only.
                query.append("&").append(HTMLUtils.urlEncode(param.trim()));

            else {
                // extract and append name and value.
                String name = param.substring(0, equalsPos);
                String val = param.substring(equalsPos + 1);
                if (val.startsWith("="))
                    val = val.substring(1);
                query.append("&").append(HTMLUtils.urlEncode(name))//
                        .append("=").append(HTMLUtils.urlEncode(val));
            }
        }

        if (query.length() != 0) {
            // merge the newly constructed query parameters with
            // the existing query string.
            String existingQuery = (String) env.get("QUERY_STRING");
            if (existingQuery != null)
                query.append("&").append(existingQuery);

            // save the resulting query string into the environment
            // for this thread.
            env.put("QUERY_STRING", query.substring(1));
        }

        linkContents.close();
    }


    /**
     * Run the TinyCGI script for the given request
     */
    private void runScript(HttpServletRequest req, HttpServletResponse resp,
            Map env, TinyCGI script) throws IOException, ServletException {
        if (script == null) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Couldn't load script for server shortcut.");
            return;
        }

        // Run the cgi script, and capture the results.
        CGIOutputStream cgiOut = null;
        try {
            cgiOut = new CGIOutputStream(resp, getOutputMode(script));
            script.service(req.getInputStream(), cgiOut, env);
            cgiOut.finish();

        } catch (Exception cgie) {
            if (cgiOut != null)
                cgiOut.cleanup();

            if (cgie instanceof TinyCGIException) {
                // If a CGI script throws a TinyCGIException, send back the
                // requested HTTP error code.
                TinyCGIException tce = (TinyCGIException) cgie;
                resp.sendError(tce.getStatus(), tce.getTitle());

            } else if (cgie instanceof IOException) {
                // if a CGI script throws an IOException, pass it along. This
                // will send an HTTP 500 "Server Error" response, and print an
                // HTML page with a stack trace
                throw (IOException) cgie;

            } else {
                // This will send an HTTP 500 "Server Error" response, and
                // print an HTML page with a stack trace
                throw new ServletException(cgie);
            }
        }
    }

    private int getOutputMode(Object script) {
        if (script instanceof TinyCGIHighVolume)
            return CGIOutputStream.LARGE;
        else if (script instanceof TinyCGIStreaming)
            return CGIOutputStream.STREAMING;
        else
            return CGIOutputStream.NORMAL;
    }

    static final String LINK_SUFFIX = ".link";

    private static final String CGI_LINK_PREFIX = "class:";

}
