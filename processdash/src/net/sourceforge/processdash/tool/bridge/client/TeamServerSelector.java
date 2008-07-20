// Copyright (C) 2008 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge.client;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.tool.bridge.ResourceBridgeConstants;
import net.sourceforge.processdash.tool.bridge.impl.TeamServerPointerFile;
import net.sourceforge.processdash.util.HTMLUtils;

public class TeamServerSelector {

    public static final String DISABLE_TEAM_SERVER_PROPERTY =
        TeamServerSelector.class.getName() + ".disabled";

    private static final Logger logger = Logger
            .getLogger(TeamServerSelector.class.getName());

    /**
     * Look in a particular real directory, and see if its contents are
     * available through one or more team servers. If so, return the URL of the
     * server which appears to be closest / most responsive.
     * 
     * @param dir
     *                a directory on the filesystem
     * @return the URL of a team server data collection representing the data in
     *         that directory
     */
    public static URL getServerURL(File dir) {
        if (Boolean.getBoolean(DISABLE_TEAM_SERVER_PROPERTY))
            return null;

        TeamServerPointerFile pointerFile = new TeamServerPointerFile(dir);

        URL result = null;
        long bestTimeSoFar = Integer.MAX_VALUE;

        for (String serverURL : pointerFile.getInstanceURLs()) {
            long start = System.currentTimeMillis();

            URL u = testServerURL(serverURL);
            if (u == null) {
                logger.log(Level.FINE,
                    "IOxception when contacting {0} - skipping", serverURL);
                continue;
            }

            long end = System.currentTimeMillis();
            long elapsed = end - start;

            logger.log(Level.FINE, "Successfully contacted {0} - took {1} ms",
                new Object[] { serverURL, elapsed });

            if (elapsed < bestTimeSoFar) {
                bestTimeSoFar = elapsed;
                result = u;
            }
        }

        return result;
    }

    /**
     * Validate that a given URL points to a team dashboard server
     * 
     * @param serverURL
     *                the URL to validate, in string form
     * @return if validation was successful, returns the original URL.
     *         Otherwise, returns null.
     */
    public static URL testServerURL(String serverURL) {
        if (Boolean.getBoolean(DISABLE_TEAM_SERVER_PROPERTY))
            return null;

        try {
            // construct a URL telling the server that we would like to
            // initiate a session, speaking a particular version of the
            // communications protocol.
            StringBuffer requestURL = new StringBuffer(serverURL);
            HTMLUtils.appendQuery(requestURL,
                ResourceBridgeConstants.ACTION_PARAM,
                ResourceBridgeConstants.SESSION_START_INQUIRY);
            HTMLUtils.appendQuery(requestURL,
                ResourceBridgeConstants.VERSION_PARAM,
                ResourceBridgeClient.CLIENT_VERSION);

            // make a connection to the server and verify that we get a valid
            // response back.
            URL u = new URL(requestURL.toString());
            URLConnection conn = u.openConnection();
            InputStream in = new BufferedInputStream(conn.getInputStream());
            while ((in.read() != -1))
                ;
            in.close();

            // make certain we're talking to a dashboard team server, and not
            // some other random web server.
            if (conn.getHeaderField(ResourceBridgeConstants.VERSION_HEADER) == null)
                return null;

            return new URL(serverURL);
        } catch (IOException ioe) {
            // if the server is not accepting new connections, it will send
            // back an HTTP error code, and we'll catch the exception here.
            return null;
        }
    }

}
