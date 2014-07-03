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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.server.Request;

import net.sourceforge.processdash.api.PDashContext;

public class PDashServletUtils {

    /**
     * Retrieve the {@link PDashContext} object associated with this request
     */
    public static PDashContext getContext(ServletRequest req) {
        return (PDashContext) req.getAttribute(PDashContext.REQUEST_ATTR);
    }


    /**
     * Using the data in an HttpServletRequest, build the standard environment
     * that TinyCGI scripts have traditionally expected for operation.
     */
    public static Map buildEnvironment(HttpServletRequest req) {
        Map env = new HashMap();

        // if any extra environment was attached to the request, add it
        Map extraEnv = (Map) req
                .getAttribute(LocalConnector.EXTRA_ENVIRONMENT_KEY);
        if (extraEnv != null)
            env.putAll(extraEnv);

        // store all values from the server default environment.
        Request baseRequest = Request.getRequest(req);
        Map defaultEnv = (Map) baseRequest.getConnection().getServer()
                .getAttribute(WebServer.DEFAULT_ENV_KEY);
        env.putAll(defaultEnv);

        // store the method, and protocol from the original request
        env.put("REQUEST_METHOD", req.getMethod());
        env.put("SERVER_PROTOCOL", req.getProtocol());

        // store the portion of the request URI that names the target resource
        String scriptName = req.getServletPath();
        if (req.getPathInfo() != null)
            scriptName += req.getPathInfo();
        env.put("SCRIPT_NAME", scriptName);
        env.put("SCRIPT_PATH", req.getRequestURI());

        // store information about the hierarchy prefix used on the request
        PDashContext dash = getContext(req);
        String uriPrefix = dash.getUriPrefix();
        if (uriPrefix != null && uriPrefix.endsWith("/")) {
            env.put("PATH_INFO", uriPrefix.substring(0, uriPrefix.length() - 1));
        } else {
            env.put("PATH_INFO", "");
        }
        env.put("PATH_TRANSLATED", dash.getProjectPath());
        env.put("REQUEST_URI", uriPrefix + baseRequest.getUri());

        // store the query string, if one was present
        String queryString = req.getQueryString();
        if (queryString != null && queryString.length() > 0)
            env.put("QUERY_STRING", queryString);

        // Store information about the inbound TCP/IP connection
        if (req.getRemoteAddr() != null) {
            env.put("REMOTE_PORT", req.getRemotePort());
            env.put("REMOTE_HOST", new RemoteHostName(req));
            env.put("REMOTE_ADDR", req.getRemoteAddr());
            env.put("SERVER_NAME", new ServerName(req));
            env.put("SERVER_ADDR", req.getLocalAddr());
        }

        // Copy HTTP headers into the environment.
        Enumeration<String> headerNames = req.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String oneName = headerNames.nextElement();
            String oneValue = req.getHeader(oneName);

            String key = "HTTP_" + oneName.toUpperCase().replace('-', '_');
            env.put(key, oneValue);
        }
        env.put("CONTENT_TYPE", req.getContentType());
        env.put("CONTENT_LENGTH", Integer.toString(req.getContentLength()));

        return env;
    }

    private static class RemoteHostName {
        private HttpServletRequest req;

        private RemoteHostName(HttpServletRequest req) {
            this.req = req;
        }

        public String toString() {
            return req.getRemoteHost();
        }
    }

    private static class ServerName {
        private HttpServletRequest req;

        private ServerName(HttpServletRequest req) {
            this.req = req;
        }

        public String toString() {
            return req.getServerName();
        }
    }

}
