// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// E-Mail POC:  ken.raisor@hill.af.mil

/*
 * Copyright (c) 1995-1997 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Permission to use, copy, modify, and distribute this software
 * and its documentation for NON-COMMERCIAL purposes and without
 * fee is hereby granted provided that this copyright notice
 * appears in all copies. Please refer to the file "copyright.html"
 * for further important copyright and licensing information.
 *
 * SUN MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. SUN SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 */

package pspdash;

import java.awt.Component;
import java.io.IOException;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;


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
    public static final String LAST_CHECK = ".lastCheckDate";
    public static final String PROXY_USER = ".proxyUsername";
    public static final String PROXY_PASS = ".proxyPassword";
    public static final long CHECK_INTERVAL = 30L /*days*/ * 24L /*hours*/ *
        60L /*minutes*/ * 60L /*seconds*/ * 1000L /*milliseconds*/;

    /** Read package information from the given manifest file. */
    public void addPackage(String filename, Manifest manifest) {
        if (manifest != null) try {
            packages.add(new DashPackage(filename, manifest));
        } catch (InvalidDashPackage idp) {}
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
        if (lastUpdate != null) try {
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
        if (checkSuccessful)
            InternalSettings.set(AUTO_UPDATE_SETTING + LAST_CHECK,
                                 Long.toString(now));

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

        Object [] message = new Object[numUpdatesFound+3];
        message[0] = ((numUpdatesFound == 1
                       ? "A new version of the following package is"
                       : "New versions of the following packages are") +
                      " now available:");
        int pos = 1;
        DashPackage pkg;
        for (int i = packages.size();  i-- > 0; ) {
            pkg = (DashPackage) packages.get(i);
            if (pkg.updateAvailable)
                message[pos++] = "\u2022 " + pkg.name;
        }
        message[pos++] =
            "Please visit http://processdash.sourceforge.net to download!";
        JCheckBox disable = new JCheckBox
            ("Don't perform a monthly check for new releases.");
        message[pos] = disable;

        JOptionPane.showMessageDialog
            (parent, message, "Software Updates are Available",
             JOptionPane.INFORMATION_MESSAGE);

        if (disable.isSelected())
            InternalSettings.set(AUTO_UPDATE_SETTING + DISABLED, "true");
    }


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
        "\u2022 You are not currently connected to the internet, or",
        "\u2022 You connect to the internet via an HTTP proxy server",
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

        /** Did the connection to the updateURL fail? */
        public boolean connectFailed = false;

        /** Was an update available, based on what was found? */
        public boolean updateAvailable;

        /** the update document retrieved from the server */
        public String updateDocument;

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

            debug("Found a package!" +
                  "\n\tname = " + name +
                  "\n\tid = " + id +
                  "\n\tversion = " + version +
                  "\n\tupdateURL = " + updateURL);
        }

        /** Try to download the update information for this package. */
        public void getUpdateInfo() {
            try {
                URL url = new URL(updateURL + "?id="+id + "&ver="+version);
                URLConnection conn = url.openConnection();
                conn.setAllowUserInteraction(true);
                int cl = conn.getContentLength();

                // a content-length of -1 means that the connection failed.
                connectFailed = (cl < 0);

                // a content-length of -1 or 0 automatically implies
                // that no update is available.
                updateAvailable = (cl > 0);
                if (updateAvailable) {
                    // Download the update package, which is an XML
                    // document containing upgrade info.
                    updateDocument = new String
                        (TinyWebServer.slurpContents(conn.getInputStream(),
                                                     true));
                    // scan the update package (dumbly for now) to see if
                    // our version is still the current version.
                    String versionString = "current-version=\"" +version+ "\"";
                    if (updateDocument.toUpperCase().indexOf
                        (versionString.toUpperCase()) != -1)
                        updateAvailable = false;
                }

                debug("getUpdateInfo: for " + name +
                      "\n\tconnectFailed = " + connectFailed +
                      "\n\tupdateAvailable = " + updateAvailable +
                      "\n\tupdateDocument = " + updateDocument);
            } catch (IOException ioe) {}
        }

        private static final String ID_ATTRIBUTE      = "Dash-Pkg-ID";
        private static final String VERSION_ATTRIBUTE = "Dash-Pkg-Version";
        private static final String NAME_ATTRIBUTE    = "Dash-Pkg-Name";
        private static final String URL_ATTRIBUTE     = "Dash-Pkg-URL";
    }

    private void debug(String msg) {
        System.out.println("AutoUpdateManager: " + msg);
    }
}
