// Copyright (C) 2001-2018 Tuma Solutions, LLC
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.StringUtil;

import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.HTTPUtils;

public class TextPreprocessingHandlerServlet extends HttpServlet {

    private static final int SCAN_BUF_SIZE = 4096;

    private static final String SERVER_PARSE_OVERRIDE = "<!--#server-parsed";

    static final String[] FILENAME_PATTERNS = { "*.shtm", "*.shtml", "*.htm",
            "*.html", "*.txt", "*.js", "*.rpt" };

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        handleFile(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        handleFile(req, resp);
    }

    private void handleFile(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Look up the named resource. If it doesn't exist, return an error.
        String uri = req.getServletPath();
        URL url = getServletContext().getResource(uri);
        if (url == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // determine the content type of the resource
        String contentType;
        boolean isPreprocessedFileType;
        if (StringUtil.endsWithIgnoreCase(uri, ".shtm")
                || StringUtil.endsWithIgnoreCase(uri, ".shtml")) {
            contentType = "text/html; charset=utf-8";
            isPreprocessedFileType = true;
        } else {
            contentType = getContentType(uri);
            isPreprocessedFileType = false;
        }

        // open a connection to the resource, and read some initial data
        URLConnection conn = url.openConnection();
        InputStream in = conn.getInputStream();
        byte[] buffer = new byte[SCAN_BUF_SIZE];
        int numBytes = in.read(buffer);
        if (numBytes < 1) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Couldn't read file '" + uri + "'");
            FileUtils.safelyClose(in);
            return;
        }

        // if the file does not need preprocessing, serve it verbatim
        if (isPreprocessedFileType == false
                && containsServerParsedOverride(buffer, numBytes) == false) {
            if (contentType != null)
                resp.setContentType(contentType);

            int len = conn.getContentLength();
            if (len >= 0)
                resp.setContentLength(len);

            long mod = conn.getLastModified();
            if (mod > 0)
                resp.setDateHeader("Last-Modified", mod);

            resp.getOutputStream().write(buffer, 0, numBytes);
            FileUtils.copyFile(in, resp.getOutputStream());
            FileUtils.safelyClose(in);
            return;
        }

        // build the environment we will use for handling the request
        Map env = PDashServletUtils.buildEnvironment(req);

        // read the original resource as a string
        ByteArrayOutputStream rawBytes = new ByteArrayOutputStream(Math.max(
            numBytes, conn.getContentLength()));
        rawBytes.write(buffer, 0, numBytes);
        FileUtils.copyFile(in, rawBytes);
        FileUtils.safelyClose(in);
        String content = rawBytes.toString("utf-8");
        rawBytes = null;

        // invoke the preprocessor on the content
        WebServer webServer = (WebServer) env.get(TinyCGI.TINY_WEB_SERVER);
        DataRepository data = (DataRepository) env.get(TinyCGI.DATA_REPOSITORY);
        String prefix = (String) env.get("PATH_TRANSLATED");
        HTMLPreprocessor p = new HTMLPreprocessor(webServer,
                data.getSubcontext(prefix), env);
        p.setForeachParams(false);
        if (contentType != null && contentType.indexOf("html") != -1)
            p.setDefaultEchoEncoding("html");
        else
            p.setEchoBareParams(false);
        content = p.preprocess(content);
        byte[] resultBytes = content.getBytes("utf-8");

        if (contentType != null)
            resp.setContentType(HTTPUtils.setCharset(contentType, "utf-8"));
        resp.setContentLength(resultBytes.length);
        resp.getOutputStream().write(resultBytes);
    }

    private boolean containsServerParsedOverride(byte[] buf, int numBytes)
            throws IOException {
        String bufStr = new String(buf, 0, numBytes, "utf-8");
        return bufStr.contains(SERVER_PARSE_OVERRIDE);
    }

    private static final Map CONTENT_TYPES = WebServer.getMimeTypeMap();

    private static String getContentType(String uri) {
        if (uri == null)
            return null;

        int dotPos = uri.lastIndexOf('.');
        if (dotPos == -1)
            return null;

        String suffix = uri.substring(dotPos).toLowerCase();
        return (String) CONTENT_TYPES.get(suffix);
    }

}
