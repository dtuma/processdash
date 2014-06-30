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

import java.io.IOException;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.server.Request;

/**
 * Dashboard URLs contain an initial project prefix which is atypical for web
 * applications. The {@link DashboardHttpRequestInterceptor} strips that initial
 * prefix so Jetty can route the request using standard servlet routing rules.
 * 
 * After Jetty's dispatch logic is done, but before any of the code in a web app
 * executes, this filter adds the prefix back (into the "context path"). This
 * allows the prefix to be included in redirects and relative URLs that might be
 * calculated by a Java web application.
 */
public class DashboardUriPrefixFilter implements Filter {

    public void init(FilterConfig cfg) throws ServletException {}

    public void destroy() {}

    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        String prefix = getPrefix(request);
        if (prefix == null) {
            chain.doFilter(request, response);

        } else {
            Request req = Request.getRequest((HttpServletRequest) request);
            String origRequestURI = req.getRequestURI();
            String origContextPath = req.getContextPath();

            String fullRequestURI = prefix + "/" + origRequestURI;
            String fullContextPath = prefix + "/" + origContextPath;
            try {
                req.setRequestURI(fullRequestURI);
                req.setContextPath(fullContextPath);
                chain.doFilter(request, response);
            } finally {
                req.setRequestURI(origRequestURI);
                req.setContextPath(origContextPath);
            }
        }
    }

    private String getPrefix(ServletRequest request) {
        if (!(request instanceof HttpServletRequest))
            return null;

        Map dash = (Map) request.getAttribute(PDashServletConstants.PDASH_ATTR);
        if (dash == null)
            return null;

        String prefix = (String) dash.get(PDashServletConstants.URI_PREFIX);
        if (prefix == null || prefix.length() < 2)
            return null;

        HttpServletRequest hreq = (HttpServletRequest) request;
        String uri = hreq.getRequestURI();
        if (uri.startsWith(prefix)
                && (uri.contains("//") || uri.contains("/+/")))
            return null;

        return prefix;
    }

}
