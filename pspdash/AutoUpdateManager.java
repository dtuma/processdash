// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 2003 Software Process Dashboard Initiative
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net


package pspdash;

import java.awt.Component;
import java.io.IOException;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent;

import org.w3c.dom.*;
import org.xml.sax.*;


/** Automatically determines whether newer versions of the dashboard (or any
 *  dashboard add-ons) are available for download, and alerts the user.
 */
public class AutoUpdateManager {

    ArrayList packages;
    long now;
    private String p_user = Settings.getVal(AUTO_UPDATE_SETTING + PROXY_USER);
    private String p_pass = Settings.getVal(AUTO_UPDATE_SETTING + PROXY_PASS);

    public AutoUpdateManager() {
        packages = new ArrayList();
        now = (new Date()).getTime();
        if (p_user != null && p_pass != null)
            Authenticator.setDefault(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication
                        (p_user, p_pass.toCharArray());
                } });
    }

    public static final String AUTO_UPDATE_SETTING = "autoUpdate";
    public static final String DISABLED = ".disabled";
    public static final String REMIND = ".remind";
    public static final String LAST_CHECK = ".lastCheckDate";
    public static final String PROXY_USER = ".proxyUsername";
    public static final String PROXY_PASS = ".proxyPassword";
    public static final long CHECK_INTERVAL = 30L /*days*/ * 24L /*hours*/ *
        60L /*minutes*/ * 60L /*seconds*/ * 1000L /*milliseconds*/;
    private static final String BULLET = "\u2022";

    /** Read package information from the given manifest file. */
    public void addPackage(String filename, Manifest manifest) {
        if (manifest != null) try {
            packages.add(new DashPackage(filename, manifest));
        } catch (InvalidDashPackage idp) {}
    }

    /** Return the version number of an installed package, or null if
     *  the package is not installed. */
    public String getPackageVersion(String packageID) {
        if (packageID == null) return null;

        Iterator i = packages.iterator();
        DashPackage pkg;
        while (i.hasNext()) {
            pkg = (DashPackage) i.next();
            if (packageID.equals(pkg.id))
                return pkg.version;
        }
        return null;
    }

    /** Possibly perform a check for updates */
    public void maybePerformCheck(Component parent) {
        // If the user has disabled auto updates, stop immediately.
        if ("true".equalsIgnoreCase
            (Settings.getVal(AUTO_UPDATE_SETTING + DISABLED))) {
            debug("update check is disabled.");
            return;
        }

        // Check to see if we've already checked for updates recently.
        String lastUpdate = Settings.getVal(AUTO_UPDATE_SETTING + LAST_CHECK);
        String remind = Settings.getVal(AUTO_UPDATE_SETTING + REMIND);
        if (remind == null && lastUpdate != null) try {
            long lastTime = Long.parseLong(lastUpdate);
            if (now < lastTime + CHECK_INTERVAL) {
                debug("update check is recent.");
                return;
            }
        } catch (NumberFormatException nfe) {}

        // If we don't appear to be connected to the internet, abort.
        try {
            byte[] addr = InetAddress.getLocalHost().getAddress();
            if (addr[0] == 127 &&
                addr[1] == 0   &&
                addr[2] == 0   &&
                addr[3] == 1) {
                debug("not connected to internet.");
                return;
            }
        } catch (IOException ioe) {}

        // okay, everything appears to be fine.  Try to check for the
        // update using a background thread.
        final Component parentComponent = parent;
        Thread t = new Thread() {
                public void run() { performCheck(parentComponent, false); }
            };
        t.start();
    }

    /** Perform an explicit check for updates, at the request of the user.
     *
     * Since the user requested this check, there is no need to run in the
     * background, and we should display some sort of result dialog whether
     * or not an update was found.
     */
    public void checkForUpdates(Component parent) {
        performCheck(parent, true);
    }


    /** Check to see if an update is available.
     *
     * If an update is found, alerts the user and returns true.
     * If msgAlways is true, also alerts the user if the check was
     *     unsuccessful or if no update was found.
     */
    protected void performCheck(Component parent, boolean msgAlways) {
        debug("performing check");
        boolean checkSuccessful = true;
        int updatesFound = 0;
        DashPackage pkg;

        for (int i = packages.size();  i-- > 0;  ) {
            pkg = (DashPackage) packages.get(i);
            pkg.getUpdateInfo();
            if (pkg.connectFailed   == true) checkSuccessful = false;
            if (pkg.updateAvailable == true) updatesFound++;
        }

        // if we were able to perform the check, save the current date.
        if (checkSuccessful) {
            InternalSettings.set(AUTO_UPDATE_SETTING + LAST_CHECK,
                                 Long.toString(now));
            InternalSettings.set(AUTO_UPDATE_SETTING + REMIND, null);
        }

        // If we found any updates, inform the user.
        if (updatesFound > 0)
            displayUpdateMessage(parent, updatesFound);
        else if (msgAlways) {
            if (!checkSuccessful)
                displayCheckFailed(parent);
            else
                displayUpToDate(parent);
        }
    }

    /** Display a message box, telling the user that various packages are
     *  now available for download.
     */
    protected void displayUpdateMessage(Component parent,
                                        int numUpdatesFound) {
        HashSet urlsSeen = new HashSet();
        DashPackage pkg;
        StringBuffer html = new StringBuffer();
        html.append("<html><head><style>"+
                    "UL { margin-top: 0pt; margin-bottom: 0pt }"+
                    "</style></head><body>");

        for (int i = 0;   i < packages.size();   i++) {
            pkg = (DashPackage) packages.get(i);
            if (!pkg.updateAvailable) continue;
            String userURL = pkg.userURL;
            if (userURL == null || urlsSeen.contains(userURL)) continue;
            urlsSeen.add(userURL);

            ArrayList updates = new ArrayList();
            updates.add(pkg.name);
            for (int j = i + 1;   j < packages.size();   j++) {
                pkg = (DashPackage) packages.get(j);
                if (pkg.updateAvailable && userURL.equals(pkg.userURL))
                    updates.add(pkg.name);
            }
            Collections.sort(updates, String.CASE_INSENSITIVE_ORDER);

            html.append("<p>");
            if (updates.size() == 1)
                html.append("A new version of the following package is");
            else
                html.append("New versions of the following packages are");
            html.append(" now available from<br><a href=\"")
                .append(userURL).append("\">")
                .append(HTMLUtils.escapeEntities(userURL))
                .append("</a>:<ul>");
            Iterator u = updates.iterator();
            while (u.hasNext())
                html.append("<li>")
                    .append(HTMLUtils.escapeEntities((String) u.next()));
            html.append("</ul>");
        }

        JEditorPane message = new JEditorPane();
        message.setContentType("text/html");
        message.setEditable(false);
        message.setBackground(null);
        message.setText(html.toString());
        message.addHyperlinkListener(new HyperlinkListener() {
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType() ==
                        HyperlinkEvent.EventType.ACTIVATED)
                        Browser.launch(e.getURL().toString());
                } } );


        JCheckBox disable = new JCheckBox
            ("Don't perform a monthly check for new releases.");
        Object[] messageDisplay = new Object[2];
        messageDisplay[0] = message;
        messageDisplay[1] = disable;

        int choice = JOptionPane.showOptionDialog
            (parent, messageDisplay, "Software Updates are Available",
             JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
             null, UPDATE_OPTIONS, UPDATE_OPTIONS[0]);
        if (choice == 0)
            InternalSettings.set(AUTO_UPDATE_SETTING + REMIND, "true");

        if (disable.isSelected())
            InternalSettings.set(AUTO_UPDATE_SETTING + DISABLED, "true");
    }
    private static final String [] UPDATE_OPTIONS = {
        "Remind me again", "Close this window" };
    private static final String DOWNLOAD_URL =
        "http://processdash.sourceforge.net/autoupdate.html";


    /** Display a dialog advising the user that the check failed. */
    public void displayCheckFailed(Component parent) {
        JOptionPane.showMessageDialog(parent, CHECK_FAILED_MSG,
                                      "Internet Connection Failure",
                                      JOptionPane.WARNING_MESSAGE);
    }
    private static final String[] CHECK_FAILED_MSG = {
        "The dashboard was unable to determine if any updates",
        "are available because it could not make a connection to",
        "the internet. This could either be because:",
        BULLET + " You are not currently connected to the internet, or",
        BULLET + " You connect to the internet via an HTTP proxy server",
        "    (which the update mechanism does not currently support).",
        "Sorry!" };


    /** Display a dialog advising the user that everything is up-to-date. */
    public void displayUpToDate(Component parent) {
        JOptionPane.showMessageDialog(parent, UP_TO_DATE_MSG, "Up To Date",
                                      JOptionPane.INFORMATION_MESSAGE);
    }
    private static final String[] UP_TO_DATE_MSG = {
        "The check is complete.  Your installation",
        "of the dashboard is up-to-date!" };


    private class InvalidDashPackage extends Exception {};

    /** Contains information about a package that is installed on the
     *  local system.
     */
    private class DashPackage {

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

        /** Create a package object based on information found in a
         *  manifest file.
         *  @param filename the name of the jar containing this manifest.
         *  @param in an InputStream pointing at the open manifest file
         */
        public DashPackage(String filename, Manifest manifest)
            throws InvalidDashPackage
        {
            Attributes attrs = manifest.getMainAttributes();
            name      = attrs.getValue(NAME_ATTRIBUTE);
            id        = attrs.getValue(ID_ATTRIBUTE);
            version   = attrs.getValue(VERSION_ATTRIBUTE);
            updateURL = attrs.getValue(URL_ATTRIBUTE);
            this.filename = filename;

            debug("File: " + filename);

            if (id==null || name==null || version==null || updateURL==null)
                throw new InvalidDashPackage();

            String lastUpdate = Settings.getVal
                (AUTO_UPDATE_SETTING + LAST_CHECK + "." + id);
            if (lastUpdate != null) try {
                lastUpdateCheckTime = Long.parseLong(lastUpdate);
            } catch (NumberFormatException nfe) {}

            debug("Found a package!" +
                  "\n\tname = " + name +
                  "\n\tid = " + id +
                  "\n\tversion = " + version +
                  "\n\tupdateURL = " + updateURL);
        }

        /** Try to download the update information for this package. */
        public void getUpdateInfo() {
            try {
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
                        (AUTO_UPDATE_SETTING + LAST_CHECK + "." + id,
                         Long.toString(lastUpdateCheckTime = now),
                         COMMENT_START + "\"" + name + "\"");

                debug("getUpdateInfo: for " + name +
                      "\n\tconnectFailed = " + connectFailed +
                      "\n\tupdateAvailable = " + updateAvailable);
            } catch (IOException ioe) {}
        }

        private static final String COMMENT_START =
            "The last date when the dashboard was able to successfully " +
            "check for an updated version of ";
    }

    public static int compareVersions(String version1, String version2) {
        if (version1.equals(version2)) return 0;

        StringTokenizer v1 = new StringTokenizer(version1, ".");
        StringTokenizer v2 = new StringTokenizer(version2, ".");

        while (true) {
            if (!v1.hasMoreTokens()) return -1;
            if (!v2.hasMoreTokens()) return 1;

            double result = vNum(v1.nextToken()) - vNum(v2.nextToken());
            if (result > 0) return 1;
            if (result < 0) return -1;
        }
    }
    private static double vNum(String num) {
        double result = 0;
        if (num.endsWith("b")) {
            num = num.substring(0, num.length()-1);
            result = -0.1;
        }
        try {
            result += Integer.parseInt(num);
        } catch (NumberFormatException nfe) {}
        return result;
    }

    private void debug(String msg) {
        // System.out.println("AutoUpdateManager: " + msg);
    }

    static final String XML_PKG_TAG = "package";
    static final String XML_PKG_ID_ATTR = "pkg-id";
    static final String XML_PKG_VERSION_ATTR = "current-version";
    static final String XML_PKG_USER_URL_ATTR = "user-url";


    public static final String ID_ATTRIBUTE      = "Dash-Pkg-ID";
    public static final String VERSION_ATTRIBUTE = "Dash-Pkg-Version";
    public static final String NAME_ATTRIBUTE    = "Dash-Pkg-Name";
    public static final String URL_ATTRIBUTE     = "Dash-Pkg-URL";
}
