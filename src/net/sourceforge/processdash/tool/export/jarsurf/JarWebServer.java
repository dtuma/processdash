// Copyright (C) 2003 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.export.jarsurf;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class JarWebServer extends TinyWebServer {

    private JarData jarData;

    JarWebServer(JarData jarData) throws IOException {
        super(0, false);
        this.jarData = jarData;
    }

    public String getURL(String jarEntryPath) {
        return "http://localhost:" + port + "/" + jarEntryPath;
    }

    protected TinyWebThread createThread(Socket clientSocket) {
        return new ZipWebServerThread(clientSocket);
    }

    public class ZipWebServerThread extends TinyWebThread {

        public ZipWebServerThread(Socket clientSocket) {
            super(clientSocket);
        }

        public void serviceRequest()
            throws TinyWebThreadException, IOException {

            String query = null;
            int questPos = path.indexOf('?');
            if (questPos != -1) {
                query = path.substring(questPos);
                path = path.substring(0, questPos);
            }

            String jarPath = path.substring(1);

            String contentType = jarData.getContentType(jarPath);
            URL u = JarWebServer.class.getResource(path);
            if (contentType == null || u == null)
                sendError(HttpURLConnection.HTTP_NOT_FOUND, "Not Found",
                          "File '" + path + "' not found.");

            URLConnection conn = u.openConnection();
            sendHeaders(200, "OK", contentType, conn.getContentLength(),
                        conn.getLastModified()-1);
            copyStream(conn.getInputStream());
        }

    }

}
