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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.jar.JarInputStream;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.eclipse.jetty.http.PathMap;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.eclipse.jetty.util.resource.FileResource;
import org.eclipse.jetty.util.resource.JarResource;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.jfree.util.Log;

import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.util.FileUtils;


class WebAppContextDashboard extends WebAppContext {

    static final String WEB_INF_URL_SUFFIX = "!/WEB-INF/";

    static final String DEFAULT_SERVLET = "org.eclipse.jetty.servlet.Default";

    static final Logger logger = Logger.getLogger(WebAppContextDashboard.class
            .getName());

    String templateUrl;

    WebAppContextDashboard(String templateUrl) {
        this.templateUrl = templateUrl;

        // do not allow browsing of anonymous directories
        setInitParameter(DEFAULT_SERVLET + ".dirAllowed", "false");

        // install a filter which can inject hierarchy prefixes back into URIs
        FilterHolder filt = new FilterHolder(new DashboardUriPrefixFilter());
        addFilter(filt, "/*", EnumSet.allOf(DispatcherType.class));
    }

    WebAppContextDashboard(URL webInfUrl) {
        this(webInfUrl.toString());

        // set the URL of the WAR file
        setWar(calcWarFileUrl());

        // servlets should be allowed to act as welcome files
        setInitParameter(DEFAULT_SERVLET + ".welcomeServlets", "true");

        // set an appropriate context path for this web application.
        setContextPath(TemplateLoader.getAddOnContextPath(templateUrl));
    }

    private String calcWarFileUrl() {
        int pos = templateUrl.indexOf(WEB_INF_URL_SUFFIX);
        if (pos == -1)
            throw new IllegalArgumentException();
        return templateUrl.substring(4, pos);
    }

    void initializeLegacyContentTypes() {
        initializeTextPreprocessorSupport();
        initializeTinyCgiSupport();
    }

    private void initializeTextPreprocessorSupport() {
        ServletHolder holder = new ServletHolder(
                new TextPreprocessingHandlerServlet());
        getServletHandler().addServlet(holder);

        ServletMapping map = new ServletMapping();
        map.setServletName(holder.getName());
        map.setPathSpecs(TextPreprocessingHandlerServlet.FILENAME_PATTERNS);
        getServletHandler().addServletMapping(map);
    }

    private void initializeTinyCgiSupport() {
        List<String> linkFiles = listFilesWithSuffixes(true,
            TinyCGIHandlerServlet.LINK_SUFFIX);
        if (linkFiles.isEmpty())
            return;

        ServletHolder holder = new ServletHolder(new TinyCGIHandlerServlet());
        getServletHandler().addServlet(holder);

        ServletMapping map = new ServletMapping();
        map.setServletName(holder.getName());
        map.setPathSpecs(linkFiles.toArray(new String[linkFiles.size()]));
        try {
            PathMap.setPathSpecSeparators("!");
            getServletHandler().addServletMapping(map);
        } finally {
            PathMap.setPathSpecSeparators(":,");
        }
    }


    protected List<String> listFilesWithSuffixes(boolean scanDirs,
            String... suffixes) {
        Resource baseResource = getBaseResource();
        if (baseResource instanceof JarResource) {
            return scanJarForFiles((JarResource) baseResource, suffixes);
        } else if (scanDirs && baseResource instanceof FileResource) {
            return scanDirForFiles((FileResource) baseResource, suffixes);
        } else {
            return Collections.EMPTY_LIST;
        }
    }


    private List<String> scanJarForFiles(JarResource jarResource,
            String[] suffixes) {
        String jarUrl = jarResource.toString();
        int pos = jarUrl.indexOf("!/");
        if (pos == -1)
            return Collections.EMPTY_LIST;

        List<String> result = new ArrayList<String>();
        String jarFileUrl = jarUrl.substring(4, pos);
        JarInputStream jarIn = null;
        try {
            jarIn = new JarInputStream(new URL(jarFileUrl).openStream());
            ZipEntry e;
            while ((e = jarIn.getNextEntry()) != null) {
                String name = e.getName();
                for (String suffix : suffixes) {
                    if (name.endsWith(suffix)) {
                        if (name.startsWith("Templates/"))
                            name = name.substring(9);
                        else
                            name = "/" + name;
                        result.add(name.substring(0,
                            name.length() - suffix.length()));
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Log.error("Unable to scan '" + jarFileUrl + "' for files", e);
        } finally {
            FileUtils.safelyClose(jarIn);
        }
        return result;
    }

    private List<String> scanDirForFiles(FileResource baseFile,
            String[] suffixes) {
        List<String> result = new ArrayList<String>();
        if (baseFile.isDirectory()) {
            File baseDir = baseFile.getFile();
            String basePath = baseDir.getPath();
            scanDirForFiles(result, baseDir, basePath, suffixes);
        }
        return result;
    }

    private void scanDirForFiles(List<String> result, File dir,
            String basePath, String[] suffixes) {
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                scanDirForFiles(result, f, basePath, suffixes);
            } else if (f.isFile()) {
                String filename = f.getName();
                for (String suffix : suffixes) {
                    if (filename.endsWith(suffix)) {
                        String path = f.getPath().replace('\\', '/');
                        path = path.substring(basePath.length(), //
                            path.length() - suffix.length());
                        result.add(path);
                        break;
                    }
                }
            }
        }
    }


    @Override
    public void doScope(String target, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        new DoScopeAction(target, baseRequest, request, response).execute();
    }


    /**
     * Perform doScope in a privileged block.
     */
    private class DoScopeAction implements PrivilegedExceptionAction<Object> {

        private String target;

        private Request baseRequest;

        private HttpServletRequest request;

        private HttpServletResponse response;

        protected DoScopeAction(String target, Request baseRequest,
                HttpServletRequest request, HttpServletResponse response) {
            this.target = target;
            this.baseRequest = baseRequest;
            this.request = request;
            this.response = response;
        }

        public Object run() throws Exception {
            WebAppContextDashboard.super.doScope(target, baseRequest, request,
                response);
            return null;
        }

        public void execute() throws IOException, ServletException {
            try {
                AccessController.doPrivileged(this);
            } catch (PrivilegedActionException pae) {
                maybeThrow(pae.getCause(), IOException.class);
                maybeThrow(pae.getCause(), ServletException.class);
                maybeThrow(pae.getCause(), RuntimeException.class);
                throw new ServletException(pae.getCause());
            }
        }

        private <T extends Throwable> void maybeThrow(Throwable t,
                Class<T> clazz) throws T {
            if (t != null && clazz.isInstance(t))
                throw (T) t;
        }

    }


    @Override
    public void doHandle(String target, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        IgnoreResponseErrors wrap = new IgnoreResponseErrors(response);
        super.doHandle(target, baseRequest, request, wrap);
        if (wrap.ignored)
            baseRequest.setHandled(false);
    }


    private class IgnoreResponseErrors extends HttpServletResponseWrapper {

        private boolean ignored = false;

        public IgnoreResponseErrors(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void sendError(int sc) throws IOException {
            if (shouldIgnore(sc) == false)
                super.sendError(sc);
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            if (shouldIgnore(sc) == false)
                super.sendError(sc, msg);
        }

        private boolean shouldIgnore(int sc) {
            for (int err : ERRORS_TO_IGNORE) {
                if (err == sc)
                    return (ignored = true);
            }

            return false;
        }

    }

    /**
     * The following errors should be ignored, so the web server can move
     * forward and try the next WAR in the search path.
     */
    private static final int[] ERRORS_TO_IGNORE = {
            // file is not present in this WAR
            HttpServletResponse.SC_NOT_FOUND,
            // user requested a directory listing, which is forbidden
            HttpServletResponse.SC_FORBIDDEN, };

}
