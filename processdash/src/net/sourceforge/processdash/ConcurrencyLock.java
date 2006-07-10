// Copyright (C) 2003-2006 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net


package net.sourceforge.processdash;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import javax.swing.JOptionPane;

import net.sourceforge.processdash.i18n.Resources;


public class ConcurrencyLock {

    public static final String LOCK_FILE_NAME = "dashlock.txt";
    public static final String INFO_FILE_NAME = "dashlock2.txt";
    public static final String RAISE_URL = "/control/raiseWindow.class";

    File infoFile = null;
    File lockFile = null;
    FileChannel lockChannel = null;
    FileLock lock = null;
    Thread shutdownHook = null;


    /** Obtain a concurrency lock for the data in the given directory.
     *
     * If a lock cannot be obtained, this method will unequivocally
     * cause the current dashboard instance to exit.  (It may first
     * display a warning message to the user, if applicable).
     *
     * @param directory the directory containing the data which we want
     *     exclusive access to.
     * @param port the port of the webserver for the dashboard
     *     requesting the lock.
     * @param timeStamp the startup timestamp of the webserver for the
     *     dashboard requesting the lock.
     */
    public ConcurrencyLock(String directory, int port, String timeStamp) {

        /* Try to acquire a lock on a lock file in the given directory. */
        lockFile = new File(directory, LOCK_FILE_NAME);
        infoFile = new File(directory, INFO_FILE_NAME);
        try {
            lockChannel = new FileOutputStream(lockFile).getChannel();
            lock = lockChannel.tryLock();

            if (lock != null) {
                writeLockInfo(port, timeStamp);
                registerShutdownHook();
            } else {
                if (notifyOtherDashboard())
                    System.exit(0);
                else
                    showReadOnlyOptionDialog(getPath(lockFile));
            }

        } catch (IOException e) {
            /* If we were unable to create a lock file, display an error
             * message and then exit.
             */
            if (lockChannel == null || lock != null)
                showFailureDialog(getPath(lockFile));
            else
                showWarningDialog(getPath(lockFile));
            unlock();
            System.exit(0);
        }
    }


    /** Write information about the current dashboard into the lock info file.
     */
    private void writeLockInfo(int port, String timeStamp) throws IOException {
        Writer out = new OutputStreamWriter
            (new FileOutputStream(infoFile), "UTF-8");
        out.write(getCurrentHost() + "\n" +
                  port + "\n" +
                  timeStamp + "\n");
        out.close();
    }


    /** Register a JVM shutdown hook to clean up the files created by this lock.
     */
    private void registerShutdownHook() {
        // destroy the lock when the JVM is closing
        Runtime.getRuntime().addShutdownHook(shutdownHook = new Thread() {
            public void run() {
                unlock();
            }});
    }


    /** Attempt to contact the dashboard that wrote the lock info file, and
     * ask it to bring itself to the front of other windows.
     *
     * @return true if the other dashboard was successfully contacted, and
     *    successfully brought itself to the front of other windows.
     */
    private boolean notifyOtherDashboard() {
        try {
            /* Read the information written in the lock info file by the other
             * running dashboard instance.
             */
            BufferedReader in = new BufferedReader
                (new InputStreamReader(new FileInputStream(infoFile), "UTF-8"));
            String otherHost = in.readLine();
            int otherPort = Integer.parseInt(in.readLine());
            String otherTimeStamp = in.readLine();

            // keep Eclipse from complaining about unused variable
            otherTimeStamp = otherTimeStamp + "";

            /* Check to see if the other dashboard is running on the same
             * computer that we're running on.  If not, don't try to contact it.
             */
            if (!getCurrentHost().equals(otherHost))
                return false;

            /* Try to contact the dashboard that created this lock file, and
             * ask it to bring itself to the front of other windows.
             */
            URL testUrl = new URL("http", LOOPBACK_ADDR, otherPort, RAISE_URL);
            HttpURLConnection conn =
                (HttpURLConnection) testUrl.openConnection();
            conn.connect();

            /* If the response code is HTTP_OK, that means the dashboard
             * heard and complied with our request.
             */
            return (conn.getResponseCode() == HttpURLConnection.HTTP_OK);
        } catch (Exception exc) {
            /* If we reach this point, it means we were UNABLE to contact
             * the dashboard which created the lock file. */
        }

        return false;
    }


    /** Get the IP address of the current host.
     */
    private String getCurrentHost() {
        String currentHost = LOOPBACK_ADDR;
        try {
            // Look up the address of the local host (where our web server
            // is currently running).
            currentHost = InetAddress.getLocalHost().getHostAddress();
        } catch (IOException ioe) {}
        return currentHost;
    }
    private static final String LOOPBACK_ADDR = "127.0.0.1";


    /** Return a user-readable description of the directory containing
     * the given file.
     */
    private String getPath(File file) {
        File parent = file.getParentFile();
        if (parent != null)
            file = parent;
        try {
            return file.getCanonicalPath();
        } catch (IOException ioe) {
            return file.getAbsolutePath();
        }
    }

    /** Display a dialog to the user indicating that someone on
     * another machine is already running the dashboard for the
     * data in the given directory.  Ask if they would like to continue
     * in read-only mode, or abort.
     */
    private void showReadOnlyOptionDialog(String directory) {
        // We must manually use ResourceBundles until after the
        // TemplateLoader is initialized.  If we ask for a Resources object,
        // we will mess up its initialization of the global bundle, and the
        // rest of the running dashboard process will suffer. (Other methods
        // in this class can do that, because they are just about to call
        // System.exit anyway).
        ResourceBundle r = ResourceBundle.getBundle(
                "Templates.resources.ProcessDashboard");

        String title = r.getString("Errors.Concurrent_Use_Title");
        String message = MessageFormat.format(
                r.getString("Errors.Concurrent_Use_Message_FMT"),
                new Object[] { directory });

        if (JOptionPane.showConfirmDialog(null, message.split("\n"), title,
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            InternalSettings.setReadOnly(true);
        } else {
            showWarningDialog(directory);
            System.exit(0);
        }

    }

    /** Display a dialog to the user indicating that someone on
     * another machine is already running the dashboard for the
     * data in the given directory, and that the dashboard must exit.
     */
    private void showWarningDialog(String directory) {
        Resources r = Resources.getDashBundle("ProcessDashboard.Errors");
        JOptionPane.showMessageDialog
            (null,
             r.formatStrings("Data_Sharing_Violation_Message_FMT", directory),
             r.getString("Data_Sharing_Violation_Title"),
             JOptionPane.ERROR_MESSAGE);
    }


    /** Display a dialog to the user indicating failure to
     * create a lock file in the given directory.
     */
    private void showFailureDialog(String directory) {
        Resources r = Resources.getDashBundle("ProcessDashboard.Errors");
        JOptionPane.showMessageDialog
            (null,
             r.formatStrings("Lock_Failure_Message_FMT", directory),
             r.getString("Lock_Failure_Title"),
             JOptionPane.ERROR_MESSAGE);
    }


    /** Release this concurrency lock.
     *
     * This method should always be called by a dashboard instance
     * before it exits.
     */
    public synchronized void unlock() {
        if (lock != null) try {
            lock.release();
            lock = null;
        } catch (Exception e) {}

        if (lockChannel != null) try {
            lockChannel.close();
            lockChannel = null;
        } catch (Exception e) {}

        if (lockFile != null) try {
            lockFile.delete();
            lockFile = null;
        } catch (Exception e) {}

        if (infoFile != null) try {
            infoFile.delete();
            infoFile = null;
        } catch (Exception e) {}

        if (shutdownHook != null) try {
            if (Runtime.getRuntime().removeShutdownHook(shutdownHook))
                shutdownHook = null;
        } catch (Exception e) {}
    }
}
