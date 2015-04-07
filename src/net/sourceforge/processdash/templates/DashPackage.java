// Copyright (C) 2003-2015 Tuma Solutions, LLC
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


package net.sourceforge.processdash.templates;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.VersionUtils;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;



/** Contains information about a package that is installed on the
 *  local system.
 */
public class DashPackage {

    public class InvalidDashPackage extends Exception {};


    /** a user-friendly name for this package */
    public String name;

    /** a unique ID for identifying this package */
    public String id;

    /** the version of the package that is locally installed */
    public String version;

    /** the URL where we can check for updates */
    public String updateURL;

    /** the local filename of the jarfile containing the package */
    public String filename;

    /** the version of the dashboard which this package requires */
    public String requiresDashVersion;

    /** True if this package contains localizable resource bundles */
    public boolean localizable;

    /** the time we last successfully checked for an updated
        version of this package */
    long lastUpdateCheckTime = -1;

    /** Did the connection to the updateURL fail? */
    public boolean connectFailed = false;

    /** Was an update available, based on what was found? */
    public boolean updateAvailable;

    /** the update document retrieved from the server */
    public Document updateDocument;

    /** A URL the user can visit to download the updates */
    public String userURL;


    /** Create a package object based on information found in the
     *  manifest of a jarfile.
     *  @param templateURL the url of the jar containing this manifest.
     */
    public DashPackage(URL templateURL) throws InvalidDashPackage {
        try {
            String jarURL = templateURL.toString();
            if (!jarURL.startsWith("jar:"))
                throw new InvalidDashPackage();

            // Strip "jar:" from the beginning and the "!/Templates/"
            // from the end of the URL.
            jarURL = jarURL.substring(4, jarURL.indexOf('!'));

            URL jarFileURL = new URL(jarURL);
            JarInputStream jarFile = new JarInputStream
                (jarFileURL.openStream());
            Manifest manifest = jarFile.getManifest();
            jarFile.close();
            init(jarURL, manifest);

        } catch (InvalidDashPackage i) {
            throw i;
        } catch (Exception e) {
            throw new InvalidDashPackage();
        }
    }


    /** Create a package object based on information found in a
     *  manifest file.
     *  @param filename the name of the jar containing this manifest.
     *  @param in an InputStream pointing at the open manifest file
     */
    public DashPackage(String filename, Manifest manifest)
        throws InvalidDashPackage
    {
        init(filename, manifest);
    }

    /** For package-internal use only, create a package for an unpackaged
     * (binary) Process Dashboard build of the specified version.
     */
    DashPackage(String versionNumber) {
        if (versionNumber == null || versionNumber.trim().length() == 0)
            throw new IllegalArgumentException("Version number required");

        name = "Process Dashboard";
        id = "pspdash";
        version = versionNumber;
    }

    /**
     * For internal use only: create a package for an MCF add-on that was
     * generated on-the-fly from an XML file in a data backup.
     */
    DashPackage(String mcfId, String mcfVersion) {
        this.id = mcfId;
        this.version = mcfVersion;
        this.filename = "< inferred from data backup >";

        String name = mcfId.replace('_', ' ');
        int vPos = name.indexOf("-v");
        if (vPos != -1) {
            String version = name.substring(vPos + 1).replace(',', '.');
            name = name.substring(0, vPos) + " (" + version + ")";
        }
        this.name = name;
    }

    private void init(String fileUrl, Manifest manifest)
        throws InvalidDashPackage
    {
        Attributes attrs = manifest.getMainAttributes();
        name      = attrs.getValue(NAME_ATTRIBUTE);
        id        = attrs.getValue(ID_ATTRIBUTE);
        version   = attrs.getValue(VERSION_ATTRIBUTE);
        updateURL = attrs.getValue(URL_ATTRIBUTE);
        requiresDashVersion = attrs.getValue(REQUIRE_ATTRIBUTE);
        localizable = "true".equals(attrs.getValue(L10N_ATTRIBUTE));
        this.filename = extractFilename(fileUrl);

        debug("File: " + filename);

        if (id == null) try {
            lookForDashManifest();
        } catch (IOException ioe) {}

        if (id==null)
            throw new InvalidDashPackage();
        if (name==null)
            name = id;
        if (version==null)
            version = "0";

        String lastUpdate = Settings.getVal
            (AutoUpdateManager.AUTO_UPDATE_SETTING +
             AutoUpdateManager.LAST_CHECK + "." + id);
        if (lastUpdate != null) try {
            lastUpdateCheckTime = Long.parseLong(lastUpdate);
        } catch (NumberFormatException nfe) {}

        debug("Found a package!" +
              "\n\tname = " + name +
              "\n\tid = " + id +
              "\n\tversion = " + version +
              "\n\tupdateURL = " + updateURL);
    }

    private String extractFilename(String fileUrl) {
        if (fileUrl == null || fileUrl.length() == 0) {
            return null;
        } else if (fileUrl.startsWith("file:")) {
            File f = new File(HTMLUtils.urlDecode(fileUrl.substring(5)));
            return f.getAbsolutePath();
        } else {
            return fileUrl;
        }
    }


    private void lookForDashManifest() throws IOException {
        if (filename == null) return;

        JarFile jarFile = new JarFile(filename, false);
        ZipEntry entry = jarFile.getEntry(DASHBOARD_MANIFEST_FILENAME);
        if (entry == null) return;

        Properties p = new Properties();
        p.load(jarFile.getInputStream(entry));
        name      = p.getProperty(NAME_ATTRIBUTE);
        id        = p.getProperty(ID_ATTRIBUTE);
        version   = p.getProperty(VERSION_ATTRIBUTE);
        updateURL = p.getProperty(URL_ATTRIBUTE);
        requiresDashVersion = p.getProperty(REQUIRE_ATTRIBUTE);
        localizable = "true".equals(p.getProperty(L10N_ATTRIBUTE));
    }


        /** Try to download the update information for this package. */
    public void getUpdateInfo(long now) {
        if (updateURL == null) {
            connectFailed = updateAvailable = false;

        } else try {
            long deltaTime =
                (lastUpdateCheckTime<0 ? -1 : now-lastUpdateCheckTime);
            URL url = new URL(updateURL + "?id="+id + "&ver="+version +
                              "&time=" + deltaTime);
            URLConnection conn = url.openConnection();
            conn.setAllowUserInteraction(true);
            int cl = conn.getContentLength();

            // a content-length of -1 means that the connection failed.
            connectFailed = (cl < 0);

            // a content-length of -1 or 0 automatically implies
            // that no update is available.
            updateAvailable = (cl > 0);
            if (updateAvailable) {
                try {
                    // Download the update package, which is an XML
                    // document containing upgrade info.
                    updateDocument = XMLUtils.parse(conn.getInputStream());
                    connectFailed = false;
                } catch (Exception e) {
                    connectFailed = true;
                    updateAvailable = false;
                }
            }

            if (updateAvailable) {
                updateAvailable = false;

                NodeList updatePackages = updateDocument
                    .getDocumentElement()
                    .getElementsByTagName(XML_PKG_TAG);
                int numPackages = updatePackages.getLength();
                Element pkg;
                for (int i=0;  i<numPackages;  i++) {
                    if (!(updatePackages.item(i) instanceof Element))
                        continue;

                    pkg = (Element) updatePackages.item(i);
                    String xmlPackageID =
                        pkg.getAttribute(XML_PKG_ID_ATTR);
                    if (!id.equals(xmlPackageID))
                        continue;

                    userURL = pkg.getAttribute(XML_PKG_USER_URL_ATTR);
                    String xmlVers =
                        pkg.getAttribute(XML_PKG_VERSION_ATTR);
                    debug("Retrieved XML for package " + id +
                          "\n\tcurrent-version = " + xmlVers +
                          "\n\tuser-url = " + userURL);

                    if (compareVersions(version, xmlVers) < 0)
                        updateAvailable = true;
                    break;
                }
            }

            if (!connectFailed)
                InternalSettings.set
                    (AutoUpdateManager.AUTO_UPDATE_SETTING +
                     AutoUpdateManager.LAST_CHECK + "." + id,
                     Long.toString(lastUpdateCheckTime = now),
                     COMMENT_START + "\"" + name + "\"");

            debug("getUpdateInfo: for " + name +
                  "\n\tconnectFailed = " + connectFailed +
                  "\n\tupdateAvailable = " + updateAvailable);
        } catch (IOException ioe) {}
    }

    public boolean isIncompatible(String dashVersion) {
        if (requiresDashVersion == null || dashVersion == null)
            // this package has no version requirement.
            return false;

        int cmp = compareVersions(dashVersion, requiresDashVersion);

        if (requiresDashVersion.endsWith("+"))
            return (cmp < 0); // incompatible if dash version is too small
        else
            return (cmp != 0); // incompatible if version numbers differ
    }


    private void debug(String msg) {
        // System.out.println("DashPackage: " + msg);
    }

    /**
     * Compare two version strings.
     * 
     * @return +1 if version1 is greater than version 2; -1 if version1 is less
     *         than version2; and 0 if the two version numbers are the same.
     */
    public static int compareVersions(String version1, String version2) {
        return VersionUtils.compareVersions(version1, version2);
    }

    private static final String COMMENT_START =
        "The last date when the dashboard was able to successfully " +
        "check for an updated version of ";


    private static final String XML_PKG_TAG = "package";
    private static final String XML_PKG_ID_ATTR = "pkg-id";
    private static final String XML_PKG_VERSION_ATTR = "current-version";
    private static final String XML_PKG_USER_URL_ATTR = "user-url";

    public static final String ID_ATTRIBUTE      = "Dash-Pkg-ID";
    public static final String VERSION_ATTRIBUTE = "Dash-Pkg-Version";
    public static final String NAME_ATTRIBUTE    = "Dash-Pkg-Name";
    public static final String URL_ATTRIBUTE     = "Dash-Pkg-URL";
    public static final String REQUIRE_ATTRIBUTE = "Dash-Pkg-Requires-Version";
    public static final String L10N_ATTRIBUTE    = "Dash-Pkg-Localizable";
    private static final String DASHBOARD_MANIFEST_FILENAME = "META-INF/PDASH.MF";

}
