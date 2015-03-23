// Copyright (C) 2001-2003 Tuma Solutions, LLC
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

import java.awt.Component;
import java.io.IOException;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.i18n.Translator;
import net.sourceforge.processdash.ui.Browser;
import net.sourceforge.processdash.util.HTMLUtils;


/** Automatically determines whether newer versions of the dashboard (or any
 *  dashboard add-ons) are available for download, and alerts the user.
 */
public class AutoUpdateManager {

    public static final String AUTO_UPDATE_SETTING = "autoUpdate";
    public static final String DISABLED = ".disabled";
    public static final String REMIND = ".remind";
    public static final String LAST_CHECK = ".lastCheckDate";
    public static final String PROXY_USER = ".proxyUsername";
    public static final String PROXY_PASS = ".proxyPassword";
    public static final long CHECK_INTERVAL = 30L /*days*/ * 24L /*hours*/ *
        60L /*minutes*/ * 60L /*seconds*/ * 1000L /*milliseconds*/;


    private ArrayList packages;
    private String p_user;
    private String p_pass;
    private long now;
    private Resources resources = Resources.getDashBundle("AutoUpdateManager");

    public AutoUpdateManager(Collection packages) {
        this.packages = new ArrayList(packages);
        this.p_user = Settings.getVal(AUTO_UPDATE_SETTING + PROXY_USER);
        this.p_pass = Settings.getVal(AUTO_UPDATE_SETTING + PROXY_PASS);
        this.now = System.currentTimeMillis();

        if (p_user != null && p_pass != null)
            Authenticator.setDefault(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication
                        (p_user, p_pass.toCharArray());
                } });
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
            pkg.getUpdateInfo(now);
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
            String hyperlink = "<a href=\"" + userURL + "\">" +
                HTMLUtils.escapeEntities(userURL) + "</a>";
            html.append(resources.format("Updates_Available_Message_FMT",
                                         hyperlink,
                                         new Integer(updates.size())));
            html.append("<ul>");
            Iterator u = updates.iterator();
            while (u.hasNext()) {
                String updateName = Translator.translate((String) u.next());
                html.append("<li>")
                    .append(HTMLUtils.escapeEntities(updateName));
            }
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
            (resources.getString("Do_Not_Check_Label"));
        Object[] messageDisplay = new Object[2];
        messageDisplay[0] = message;
        messageDisplay[1] = disable;

        String [] updateOptions = { resources.getString("Remind_Label"),
                                    resources.getString("Close_Label") };

        int choice = JOptionPane.showOptionDialog
            (parent, messageDisplay,
             resources.getString("Updates_Available_Title"),
             JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
             null, updateOptions, updateOptions[0]);
        if (choice == 0)
            InternalSettings.set(AUTO_UPDATE_SETTING + REMIND, "true");

        if (disable.isSelected())
            InternalSettings.set(AUTO_UPDATE_SETTING + DISABLED, "true");
    }


    /** Display a dialog advising the user that the check failed. */
    public void displayCheckFailed(Component parent) {
        JOptionPane.showMessageDialog
            (parent,
             resources.getStrings("Check_Failed_Message"),
             resources.getString("Check_Failed_Title"),
             JOptionPane.WARNING_MESSAGE);
    }


    /** Display a dialog advising the user that everything is up-to-date. */
    public void displayUpToDate(Component parent) {
        JOptionPane.showMessageDialog
            (parent,
             resources.getStrings("Up_To_Date_Message"),
             resources.getString("Up_To_Date_Title"),
             JOptionPane.INFORMATION_MESSAGE);
    }




    private final void debug(String msg) {
        // System.out.println("AutoUpdateManager: " + msg);
    }

}
