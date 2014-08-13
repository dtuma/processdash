// Copyright (C) 2014 Tuma Solutions, LLC
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

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;

import net.sourceforge.processdash.api.PDashContext;
import net.sourceforge.processdash.util.StringUtils;

/**
 * This object is the first handler in the HTTP handling sequence for the
 * dashboard web server. It performs these actions:
 * 
 * <ul>
 * <li>It parses dashboard URLs to separate the initial project prefix from the
 * path of the target resource.</li>
 * <li>It stores a <tt>pdash</tt> object in the HTTP request attributes, which
 * provides access to commonly used dashboard constructs</li>
 * </ul>
 */
public class DashboardHttpRequestInterceptor extends HandlerWrapper {

    private String startupTimestamp;

    public DashboardHttpRequestInterceptor(String startupTimestamp) {
        this.startupTimestamp = startupTimestamp;
    }

    public void handle(String target, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        // add the dashboard startup timestamp header to the response.
        response.addHeader(WebServer.TIMESTAMP_HEADER, startupTimestamp);

        // make the URI canonical
        HttpURI uri = baseRequest.getUri();
        String origUri = uri.toString();
        String origPath = uri.getPath();

        String path = canonicalizePath(origPath);
        if (path == null || !path.startsWith("/") || path.contains("/../")) {
            baseRequest.setHandled(true);
            response.sendError(SC_BAD_REQUEST, "Bad filename.");
            return;
        }

        // Separate out the hierarchy prefix if the URI contains one
        String[] pathSplit = splitPath(path, "//", "/+/");
        String prefix = "";
        if (pathSplit != null) {
            prefix = pathSplit[0];
            path = pathSplit[1];
        }

        if (path.contains("//")) {
            baseRequest.setHandled(true);
            response.sendError(SC_BAD_REQUEST, "Bad path/filename.");
            return;
        }

        if (!origPath.equals(path)) {
            String newUri = replaceUriPath(origUri, origPath, path);
            if (newUri == null) {
                baseRequest.setHandled(true);
                response.sendError(SC_BAD_REQUEST, "Bad uri/filename.");
            } else {
                uri.parse(newUri);
            }
        }

        // add the "pdash" object to the request.
        baseRequest.setAttribute(PDashContext.REQUEST_ATTR,
            new PdashContextImpl(baseRequest, prefix));

        // pass the request on to the rest of the processing chain
        super.handle(path, baseRequest, request, response);
    }


    /**
     * Canonicalize a path through the removal of directory changes made by
     * occurences of &quot;..&quot; and &quot;.&quot;.
     *
     * @return a canonical path, or null on error.
     */
    private String canonicalizePath(String path) {
        if (path == null)
            return null;

        path = path.trim();
        path = StringUtils.findAndReplace(path, "%2f", "/");
        path = StringUtils.findAndReplace(path, "%2F", "/");
        path = StringUtils.findAndReplace(path, "%2e", ".");
        path = StringUtils.findAndReplace(path, "%2E", ".");

        int pos, beg;
        while (true) {

            if (path.startsWith("../") || path.startsWith("/../"))
                return null;

            else if (path.startsWith("./"))
                path = path.substring(2);

            else if ((pos = path.indexOf("/./")) != -1)
                path = path.substring(0, pos) + path.substring(pos + 2);

            else if (path.endsWith("/."))
                path = path.substring(0, path.length() - 2);

            else if ((pos = path.indexOf("/../", 1)) != -1) {
                beg = path.lastIndexOf('/', pos - 1);
                if (beg == -1)
                    path = path.substring(pos + 4);
                else
                    path = path.substring(0, beg) + path.substring(pos + 3);

            } else if (path.endsWith("/..")) {
                beg = path.lastIndexOf('/', path.length() - 4);
                if (beg == -1)
                    return null;
                else
                    path = path.substring(0, beg + 1);

            } else
                return path;
        }
    }


    /**
     * Check to see if a path contains one of a list of delimiters. If so, split
     * the path on the delimiter and return the two parts. Each delimiter is
     * expected to begin and end with a slash; the first String returned from
     * this method will end with a slash, and the second String returned from
     * this method will begin with that slash.
     */
    private String[] splitPath(String path, String... delimeters) {
        for (String delim : delimeters) {
            int pos = path.indexOf(delim);
            if (pos != -1) {
                String prefix = path.substring(0, pos + 1);
                String suffix = path.substring(pos + delim.length() - 1);
                return new String[] { prefix, suffix };
            }
        }

        return null;
    }


    /**
     * Find the origPath within a full URI, and replace it with a new path.
     */
    private String replaceUriPath(String fullUri, String origPath,
            String newPath) {
        // if the full URI does not contain any query parameters, it will be the
        // same as the original path. In this case, just return the new path.
        if (fullUri.equals(origPath))
            return newPath;

        int origPathLen = origPath.length();
        int pos = fullUri.indexOf(origPath);
        if (pos == 0)
            // if the full URI begins with the original path (the expected
            // scenario), just append the remainder of the URI to the new path
            return newPath + fullUri.substring(origPathLen);
        else if (pos == -1)
            // this scenario should never happen; but guard for it just in case
            return null;
        else
            // if the original path was in the middle of the full URI, replace
            // that segment with the new path. (This scenario is also
            // unexpected, but we guard for it just in case.)
            return fullUri.substring(0, pos) + newPath
                    + fullUri.substring(pos + origPathLen);
    }

}
