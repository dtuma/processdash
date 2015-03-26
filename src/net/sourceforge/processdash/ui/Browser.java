//This class shared from the Giant Java Tree, http://www.gjt.org
//originally in package org.gjt.fredde.util.net;


/*  Browser.java - A browser launcher.
 *  Copyright (C) 1999, 2000 Fredrik Ehnbom
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package net.sourceforge.processdash.ui;

import java.io.IOException;

import javax.swing.JOptionPane;

import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.ui.lib.BrowserLauncher;

/**
 * This class is used for launching the current platforms browser.
 * @author Fredrik Ehnbom <fredde@gjt.org>
 * @version $Id$
 */
public class Browser {

    // static { try { maybeSetupForWindowsIE(); } catch (Exception e) {} }
    private static String defaultHost = "localhost";
    private static int defaultPort = 2468;

    public static final String BROWSER_LAUNCHER = "BrowserLauncher";

    public static void setDefaults(String host, int port) {
        defaultHost = host;
        defaultPort = port;
    }
    public static String mapURL(String url) {
        if (url.startsWith("http:/") || url.startsWith("https:/") ||
            url.startsWith("file:/") || url.startsWith("mailto:"))
            return url;

        if (!url.startsWith("/"))
            url = "/" + url;
        url = "http://" + defaultHost + ":" + defaultPort + url;
        return url;
    }

    /**
     * Starts the browser for the current platform.
     * @param url The link to point the browser to.
     */
    public static void launch(String url)
    {
        url = mapURL(url);
        String cmd = Settings.getFile("browser.command");

        try {
            if (cmd != null) {
                if (BROWSER_LAUNCHER.equalsIgnoreCase(cmd))
                    try {
                        BrowserLauncher.openURL(url);
                    } catch (IOException ble) {
                        System.err.println(ble);
                        throw ble;
                    }
                else {
                    cmd = cmd + " " + url;
                    Runtime.getRuntime().exec(cmd);
                }
            } else if (isWindows()) {
                cmd = ("rundll32 url.dll,FileProtocolHandler " +
                       maybeFixupURLForWindows(url));
                Runtime.getRuntime().exec(cmd);
            } else if (isMac()){
                Runtime.getRuntime().exec(new String[] { "open", url });
            } else {
                Runtime.getRuntime().exec(new String[] { "firefox", url });
            }
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog
                (null, errorMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    private static String[] errorMessage() {
        String cmd = Settings.getFile("browser.command");
        if (cmd == null)
            ERROR_MESSAGE[1] = "you need to specify the browser that you " +
                "want the dashboard to use.";
        else
            ERROR_MESSAGE[1] = "could not execute '" + cmd + "'";

        ERROR_MESSAGE[3] = "             " +
            InternalSettings.getSettingsFileName();
        return ERROR_MESSAGE;
    }
    private static String[] ERROR_MESSAGE = {
        "The Process Dashboard was unable to launch a web browser;",
        "",
        "To solve this problem, exit the dashboard. Then edit the file, ",
        "",
        "Add a line of the form, 'browser.command=command-to-run-browser',",
        "where command-to-run-browser is the complete path to a web browser",
        "executable such as Internet Explorer or Firefox.  Then restart",
        "the Process Dashboard." };

    /*
     * If the default browser is Internet Explorer 5.0 or greater,
     * the url.dll program fails if the url ends with .htm or .html
     * A workaround is to append a null query string onto the end.
     */
    private static String maybeFixupURLForWindows(String url) {
        // plain filenames (e.g. c:\some_file.html or \\server\filename) do
        // not need fixing.
        if (url == null || url.length() < 2 ||
            url.charAt(0) == '\\' || url.charAt(1) == ':')
            return url;

        if (url.startsWith("file:/"))
            return url.substring(6);

        String lower_url = url.toLowerCase();
        int i = badEndings.length;
        while (i-- > 0)
            if (lower_url.endsWith(badEndings[i]))
                return fixupURLForWindows(url);

        return url;
    }
    private static final String[] badEndings = {
        ".htm", ".html", ".htw", ".mht", ".cdf", ".mhtml", ".stm", ".shtm" };
    private static String fixupURLForWindows(String url) {
        if (url.indexOf('?') == -1)
            return url + "?";
        else
            return url + "&workaroundStupidWindowsBug";
    }

    /**
     * Checks if the OS is windows.
     * @return true if it is, false if it's not.
     */
    public static boolean isWindows() {
        if (System.getProperty("os.name").indexOf("Windows") != -1) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks if the OS is Macintosh.
     * @return true if it is, false if it's not.
     */
    private static boolean isMac() {
        return System.getProperty("os.name").contains("OS X");
    }

}
