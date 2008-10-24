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
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.tool.bridge.ResourceBridgeConstants;
import net.sourceforge.processdash.tool.bridge.impl.TeamServerPointerFile;
import net.sourceforge.processdash.util.HTMLUtils;

public class TeamServerSelector {

    public static final String DISABLE_TEAM_SERVER_PROPERTY =
        TeamServerSelector.class.getName() + ".disabled";

    public static final String DEFAULT_TEAM_SERVER_PROPERTY =
        TeamServerSelector.class.getName() + ".defaultURL";

    private static final Logger logger = Logger
            .getLogger(TeamServerSelector.class.getName());

    /**
     * Test whether Team Server usage has been disabled.
     * 
     * As an example, Team Server usage is typically disabled when a data
     * backup is opened in the Quick Launcher; in that case, the data from
     * inside the backup should be used, and no Team Server should be contacted.
     * 
     * @return true if Team Servers should not be contacted by this application.
     */
    public static boolean isTeamServerUseDisabled() {
        return Boolean.getBoolean(DISABLE_TEAM_SERVER_PROPERTY);
    }

    /**
     * Test whether a default team server is in effect.
     * 
     * If Team Server usage is disabled, this will always return false.
     * 
     * @return true if a default Team Server has been configured for this
     *         application.
     */
    public static boolean isDefaultTeamServerConfigured() {
        if (isTeamServerUseDisabled())
            return false;

        String baseUrl = System.getProperty(DEFAULT_TEAM_SERVER_PROPERTY);
        return (baseUrl != null && baseUrl.length() > 0);
    }

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
        return getServerURL(dir, null);
    }

    /**
     * Look in a particular real directory, and see if its contents are
     * available through one or more team servers. If so, return the URL of the
     * server which appears to be closest / most responsive, and which is
     * running a particular minimum version of the software.
     * 
     * @param dir
     *                a directory on the filesystem
     * @param minVersion
     *                the minimum acceptable version number of the server
     *                software
     * @return the URL of a team server data collection representing the data in
     *         that directory
     */
    public static URL getServerURL(File dir, String minVersion) {
        if (isTeamServerUseDisabled() || dir == null)
            return null;

        Map<String, String> urlsToTry = new HashMap<String, String>();
        addDefaultURL(urlsToTry, dir, minVersion);

        TeamServerPointerFile pointerFile = new TeamServerPointerFile(dir);
        for (String url : pointerFile.getInstanceURLs())
            urlsToTry.put(url, minVersion);

        URL result = null;
        long bestTimeSoFar = Integer.MAX_VALUE;

        for (Map.Entry<String, String> e : urlsToTry.entrySet()) {
            String serverURL = e.getKey();
            String requiredVersion = e.getValue();

            long start = System.currentTimeMillis();

            URL u = testServerURL(serverURL, requiredVersion);
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

    private static void addDefaultURL(Map<String, String> destMap, File dir,
            String minVersion) {
        String url = getDefaultURL(dir);
        if (url == null)
            return;

        String requiredVersion = "1.4";
        if (minVersion != null
                && compareVersions(minVersion, requiredVersion) > 0)
            requiredVersion = minVersion;

        destMap.put(url, requiredVersion);
    }

    /**
     * If a default Team Server has been configured, return the hypothetical URL
     * that would be used to look up that directory in the default Team Server.
     * 
     * @param dir
     *                the directory of a resource collection
     * @return the URL that might be used to look up that resource collection
     *         through the default team server, or null if no default team
     *         server has been configured
     */
    static String getDefaultURL(File dir) {
        if (dir == null)
            return null;

        String baseUrl = System.getProperty(DEFAULT_TEAM_SERVER_PROPERTY);
        if (baseUrl == null || baseUrl.length() == 0)
            return null;

        String dirName = dir.getName();
        if ("disseminate".equalsIgnoreCase(dirName)
                && dir.getParentFile() != null)
            dirName = dir.getParentFile().getName() + "-disseminate";

        try {
            return baseUrl + "/" + URLEncoder.encode(dirName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // "can't happen"
            return null;
        }
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
        return testServerURL(serverURL, null);
    }

    /**
     * Validate that a given URL points to a team dashboard server running
     * a particular minimum version of the software
     * 
     * @param serverURL
     *                the URL to validate, in string form
     * @param minVersion
     *                the minimum acceptable version number of the server
     *                software
     * @return if validation was successful, returns the original URL.
     *         Otherwise, returns null.
     */
    public static URL testServerURL(String serverURL, String minVersion) {
        if (serverURL == null || serverURL.trim().length() == 0)
            return null;

        if (isTeamServerUseDisabled())
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
            String version = conn.getHeaderField(
                    ResourceBridgeConstants.VERSION_HEADER);
            if (version == null)
                return null;

            // if a minimum version number was stated, check that constraint
            if (minVersion != null && compareVersions(version, minVersion) < 0)
                return null;

            return new URL(serverURL);
        } catch (IOException ioe) {
            // if the server is not accepting new connections, it will send
            // back an HTTP error code, and we'll catch the exception here.
            return null;
        }
    }

    private static int compareVersions(String a, String b) {
        return normalizeVersion(a).compareTo(normalizeVersion(b));
    }

    private static String normalizeVersion(String version) {
        StringBuffer result = new StringBuffer();
        for (String component : version.split("\\.")) {
            for (int i = component.length();  i < 5;  i++)
                result.append('0');
            result.append(component).append('.');
        }
        return result.toString();
    }
}
