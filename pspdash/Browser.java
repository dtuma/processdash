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
package pspdash;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import javax.swing.JOptionPane;

/**
 * This class is used for launching the current platforms browser.
 * @author Fredrik Ehnbom <fredde@gjt.org>
 * @version $Id$
 */
public class Browser {

    // static { try { maybeSetupForWindowsIE(); } catch (Exception e) {} }

    /**
     * Starts the browser for the current platform.
     * @param url The link to point the browser to.
     */
    public static void launch(String url)
    //        throws InterruptedException, IOException
    {
        String cmd = Settings.getFile("browser.command");

        try {
            if (cmd != null) {
                cmd = cmd + " " + url;
                Runtime.getRuntime().exec(cmd);
            } else if (isWindows()) {
                cmd = ("rundll32 url.dll,FileProtocolHandler " +
                       maybeFixupURLForWindows(url));
                Runtime.getRuntime().exec(cmd);
            } else {
                String windowName = ",window" + System.currentTimeMillis();
                cmd = "netscape -remote openURL(" + url + windowName + ")";

                Process p = Runtime.getRuntime().exec(cmd);

                int exitcode = p.waitFor();

                if (exitcode != 0) {
                    cmd = "netscape " + url;

                    Runtime.getRuntime().exec(cmd);
                }
            }
        } catch (InterruptedException ie) {
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
        "To solve this problem, create or edit the file, ",
        "",
        "Add a line of the form, 'browser.command=command-to-run-browser',",
        "where command-to-run-browser is the complete path to a web browser",
        "executable such as Internet Explorer or Netscape.  Then restart",
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

        String lower_url = url.toLowerCase();
        int i = badEndings.length;
        while (i-- > 0)
            if (lower_url.endsWith(badEndings[i]))
                return fixupURLForWindows(url);

        return url;
    }
    private static final String[] badEndings = {
        ".htm", ".html", ".htw", ".mht", ".cdf", ".mhtml", ".stm" };
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

    private static File getTrustlibDir() {
        File result = null;

        /* It would be nice to do this, but JRE throws an error, stating that
         * deprecated method getenv() is no longer supported.
        String windir = System.getenv("windir");
        if (windir != null) {
            result = new File(windir);
            if (result.isDirectory()) return result;
        }
        */

        result = new File("c:\\windows\\java\\trustlib");
        if (result.isDirectory()) return result;

        result = new File("c:\\winnt\\java\\trustlib");
        if (result.isDirectory()) return result;

        return null;
    }

    private static void debug(String msg) {
        // System.out.println("Browser: " + msg);
    }
    private static void maybeSetupForWindowsIE() throws IOException {

        debug("maybeSetupForWindowsIE");
        if (!isWindows()) return;
        debug("isWindows.");

        File trustlibdir = getTrustlibDir();
        if (trustlibdir == null) return;
        debug("got trustlibdir");

        File pspdashdir = new File(trustlibdir, "pspdash");
        if (!(pspdashdir.isDirectory() || pspdashdir.mkdir())) return;
        debug("made pspdashdir");

        File datadir = new File(pspdashdir, "data");
        if (!(datadir.isDirectory() || datadir.mkdir())) return;
        debug("made datadir");

        copyClassFile(datadir, "OLEDBDSLWrapper.class");
        copyClassFile(datadir, "OLEDBListenerWrapper.class");
        debug("copied classes");

    }

    private static void copyClassFile(File destdir, String classFileName)
        throws IOException
    {
        File destFile = new File(destdir, classFileName);
        if (destFile.exists()) return;

        FileOutputStream out = new FileOutputStream(destFile);
        InputStream in = Browser.class.getResourceAsStream
            ("/pspdash/data/" + classFileName);

        byte [] buffer = new byte[3000];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) > -1)
            out.write(buffer, 0, bytesRead);
        out.close();
        in.close();
    }
}
/*
 * ChangeLog:
 * $Log$
 * Revision 1.6  2001/03/06 23:58:10  tuma
 * In anticipation of creating GUIs for editing user preferences, revamped
 * the Settings class so it automatically saves changes out to the user's
 * settings file, complete with embedded comments.
 *
 * Revision 1.5  2001/02/08 17:55:48  tuma
 * disable auto-copy of OLEDB*.class files into trustlib directory; we're
 * going to do this with an InstallShield script.
 *
 * Revision 1.4  2001/02/06 23:13:56  tuma
 * bug in prior fix: space before word "windows" was preventing netscape
 * from creating new windows.
 *
 * Revision 1.3  2001/02/06 22:36:51  tuma
 * When launching something in a netscape browser, always open it in a
 * new window - don't reuse existing windows.
 *
 * Revision 1.2  2001/02/06 19:21:52  tuma
 * on windows, copy needed classfiles into the Trustlib directory if
 * they aren't already there.
 *
 * Revision 1.1  2001/02/05 18:39:15  tuma
 * New code which can kick off the default browser on Windows
 *
 * Revision 1.3  2000/07/13 19:03:32  fredde
 * updated the command to startup browsers in windows
 *
 * Revision 1.2  2000/04/01 14:59:56  fredde
 * license for the package changed to LGPL
 *
 */
