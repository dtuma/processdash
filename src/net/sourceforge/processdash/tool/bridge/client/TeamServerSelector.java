// Copyright (C) 2008-2020 Tuma Solutions, LLC
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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.tool.bridge.ResourceBridgeConstants;
import net.sourceforge.processdash.tool.bridge.ResourceBridgeConstants.Permission;
import net.sourceforge.processdash.tool.bridge.impl.TeamServerPointerFile;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.HttpException;

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
        return getDefaultTeamServerUrl() != null;
    }

    /**
     * Get the URL of the default team server, if one is in effect.
     * 
     * If Team Server usage is disabled, this will always return null.
     * 
     * @return a URL if a default Team Server has been configured for this
     *         application, otherwise null
     */
    public static String getDefaultTeamServerUrl() {
        if (isTeamServerUseDisabled())
            return null;

        String result = System.getProperty(DEFAULT_TEAM_SERVER_PROPERTY);
        return (isUrlFormat(result) ? result : null);
    }

    /**
     * If the working directory is a {@link BridgedWorkingDirectory} to a
     * server at version 2.0 or higher, and no default team server URL has
     * been set, set one.
     */
    public static void maybeSetDefaultTeamServerUrl(WorkingDirectory wd) {
        if (isTeamServerUseDisabled())
            return;  // team server use is disabled

        if (!wd.getClass().getName().endsWith(".BridgedWorkingDirectory"))
            return; // working directory is not bridged to a server

        if (isDefaultTeamServerConfigured())
            return; // a default team server is already configured.

        // get the remote URL of the bridged working directory
        String remoteCollectionUrl = wd.getDescription();
        if (!remoteCollectionUrl.startsWith("http"))
            return;

        // is the bridged working directory on a server running 2.0 or higher?
        if (testServerURL(remoteCollectionUrl, "2.0") != null) {
            // calculate the "base" url for data sharing, and set it as the
            // default team server URL.
            String serverBaseUrl = getServerBaseURL(remoteCollectionUrl);
            if (serverBaseUrl != null)
                System.setProperty(DEFAULT_TEAM_SERVER_PROPERTY, serverBaseUrl);
        }
    }

    /**
     * Test whether a particular string is in a format that looks like a team
     * server URL.
     *
     * @param location a String describing a location
     * @return true if the location should be treated as a URL, false if it
     *    should be treated as a File path.
     */
    public static boolean isUrlFormat(String location) {
        return (location != null && location.startsWith("http"));
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
        return getServerURL(dir, minVersion, false);
    }

    /**
     * Look in a particular real directory and see if its contents have been
     * migrated to a team server. If not, return null. Otherwise, try returning
     * the URL of the server which appears to be closest / most responsive. If
     * no server could be contacted but the offlineOK parameter is true, a URL
     * will be returned anyway.
     * 
     * @param dir
     *            a directory on the filesystem
     * @param offlineOK
     *            if true, return a URL if one is available, even if the
     *            corresponding server is not reachable
     * @return the URL of a team server data collection representing the data in
     *         that directory
     */
    public static URL getServerURL(File dir, boolean offlineOK) {
        return getServerURL(dir, null, offlineOK);
    }

    private static URL getServerURL(File dir, String minVersion,
            boolean offlineOK) {
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

        if (result == null && offlineOK) {
            List<String> instanceURLs = pointerFile.getInstanceURLs();
            if (!instanceURLs.isEmpty()) {
                try {
                    // select one of the URLs at random. (Of course, with
                    // the current PDES deployment scenario, we expect exactly
                    // one URL to be present anyway.)
                    result = new URL(instanceURLs.get(0));
                } catch (Exception ex) {}
            }
        }

        return result;
    }

    private static void addDefaultURL(Map<String, String> destMap, File dir,
            String minVersion) {
        String url = getDefaultURL(dir);
        if (url == null)
            return;

        String requiredVersion = minVersionForExploratoryTest(minVersion);

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
     * Validate that a given URL points to an existing data collection on
     * a team dashboard server, or return an equivalent functional URL on the
     * default team server.
     * 
     * @param serverURL
     *                the URL to validate, in string form
     * @return if validation was successful, returns a functional URL that
     *         can be used to contact the team server for a particular data
     *         collection.  This could be the original URL, or it could be
     *         an equivalent URL on the default team server.  If no functional
     *         team server was found, returns null.
     * @since 1.12.1.1b
     */
    public static URL resolveServerURL(String serverURL) {
        return resolveServerURL(serverURL, null);
    }

    /** 
     * Validate that a given URL points to an existing data collection on
     * a team dashboard server running a particular minimum version of the
     * software, or return an equivalent functional URL on the default team
     * server.
     * 
     * @param serverURL
     *                the URL to validate, in string form
     * @param minVersion
     *                the minimum acceptable version number of the server
     *                software
     * @return if validation was successful, returns a functional URL that
     *         can be used to contact the team server for a particular data
     *         collection.  This could be the original URL, or it could be
     *         an equivalent URL on the default team server.  If no functional
     *         team server was found, returns null.
     * @since 1.12.1.1b
     */
    public static URL resolveServerURL(String serverURL, String minVersion) {
        if (serverURL == null || serverURL.trim().length() == 0)
            return null;

        if (isTeamServerUseDisabled())
            return null;

        // test the URL that we were given. If it is valid, return it.
        URL result = testServerURL(serverURL, minVersion);
        if (result != null)
            return result;

        // produce an alternative URL using the default team server and test it
        String defaultURL = getDefaultURL(serverURL);
        if (defaultURL != null) {
            String requiredVersion = minVersionForExploratoryTest(minVersion);
            result = testServerURL(defaultURL, requiredVersion);
        }
        return result;
    }

    private static String getDefaultURL(String serverURL) {
        String baseURL = getDefaultTeamServerUrl();
        if (baseURL == null)
            return null;

        if (serverURL.startsWith(baseURL) || serverURL.contains(".."))
            return null;

        int slashPos = serverURL.lastIndexOf('/');
        if (slashPos == -1)
            return null;

        String collectionPath = serverURL.substring(slashPos);
        return baseURL + collectionPath;
    }

    private static String getServerBaseURL(String collectionURL) {
        int slashPos = collectionURL.lastIndexOf('/');
        if (slashPos == -1)
            return null;
        else
            return collectionURL.substring(0, slashPos);
    }


    /**
     * Validate that a given URL points to an existing data collection on
     * a team dashboard server
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
     * Validate that a given URL points to an existing data collection on
     * a team dashboard server running a particular minimum version of the
     * software
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
        try {
            return testServerURL(serverURL, minVersion, null);
        } catch (HttpException e) {
            return null;
        }
    }

    /**
     * Validate that a given URL points to an existing data collection on a team
     * dashboard server, and the current user has permission to interact with it
     * 
     * @param collectionURL
     *            the URL to validate, in string form
     * @param minPermission
     *            a permission we require the user to have
     * @return true if the collection exists and the user has the required
     *         permission
     * @since 2.5.6
     */
    public static boolean hasPermission(String collectionURL,
            Permission minPermission) {
        try {
            return testServerURL(collectionURL, null, minPermission) != null;
        } catch (HttpException e) {
            return false;
        }
    }

    /**
     * Validate that a given URL points to an existing data collection on a team
     * dashboard server. Optionally require a minimum version of the server
     * software, and a specific permission the current user must have on the
     * collection.
     * 
     * @param serverURL
     *            the URL to validate, in string form
     * @param minVersion
     *            the minimum acceptable version number of the server software,
     *            or null for any version
     * @param permissionToAssert
     *            a permission we expect the user to have, or null for no
     *            permission checks
     * @return if validation was successful, returns the original URL.
     *         Otherwise, returns null.
     * @throws HttpException.Unauthorized
     *             if the user did not provide valid login credentials
     * @throws HttpException.Forbidden
     *             if the current user does not have the desired permission
     * @since 2.5.6
     */
    public static URL testServerURL(String serverURL, String minVersion,
            Permission permissionToAssert)
            throws HttpException.Unauthorized, HttpException.Forbidden {
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
                ResourceBridgeConstants.VERSION_PARAM, CLIENT_VERSION);
            if (permissionToAssert != null)
                HTMLUtils.appendQuery(requestURL,
                    ResourceBridgeConstants.ASSERT_PERMISSION_PARAM,
                    permissionToAssert.toString());

            // make a connection to the server and verify that we get a valid
            // response back.
            URL u = new URL(requestURL.toString());
            URLConnection conn = u.openConnection();
            if (permissionToAssert != null)
                HttpException.checkValid(conn);
            int status = ((HttpURLConnection) conn).getResponseCode();
            if (status != 403) {
                InputStream in = new BufferedInputStream(conn.getInputStream());
                while ((in.read() != -1))
                    ;
                in.close();
            }

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
        } catch (HttpException.Unauthorized u) {
            throw u;
        } catch (HttpException.Forbidden f) {
            throw f;
        } catch (Exception ioe) {
            // if the server is not accepting new connections, it will send
            // back an HTTP error code, and we'll catch the IOException here.
            // However, we catch any other exception as well, because the URL
            // and URLConnection classes sometimes throw runtime exceptions.
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

    /**
     * Operations above that make use of the "default team server URL" must make
     * exploratory inquiries to a "guessed" URL, attempting to see if it is the
     * team server. When making these exploratory tests, we have to require
     * version 1.4 or higher of the team server. This is because earlier
     * versions of the server did not check to see if a collection actually
     * existed before returning a response to the "session start inquiry" action.
     */
    private static String minVersionForExploratoryTest(String desiredVersion) {
        String requiredVersion = "1.4";
        if (desiredVersion != null
                && compareVersions(desiredVersion, requiredVersion) > 0)
            requiredVersion = desiredVersion;
        return requiredVersion;
    }

    /**
     * This method was added in version 2.4.3, to return the real version of the
     * Process Dashboard software. Previous versions of the dashboard would
     * always use "1.0" as the version number for the client bridge.
     */
    private static String getClientVersionNumber() {
        String result = null;
        try {
            result = TeamServerSelector.class.getPackage()
                    .getImplementationVersion();
        } catch (Exception e) {
        }
        return (result == null ? "9999" : result);
    }

    static final String CLIENT_VERSION = getClientVersionNumber();

}
