// Process Dashboard - Data Automation Tool for high-maturity processes
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
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net


package net.sourceforge.processdash;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;

import javax.swing.JOptionPane;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.http.WebServer;


public class ConcurrencyLock {

    public static final String LOCK_FILE_NAME = "dashlock.txt";
    public static final String RAISE_URL = "/control/raiseWindow.class";

    File lockFile = null;

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

        String currentHost = LOOPBACK_ADDR;
        try {
            // Look up the address of the local host (where our web server
            // is currently running).
            currentHost = InetAddress.getLocalHost().getHostAddress();
        } catch (IOException ioe) {}

        /* Check for the presence of a lock file in the given directory. */
        lockFile = new File(directory, LOCK_FILE_NAME);
        if (lockFile.exists()) try {

            /* The lock file exists!  Try to contact the dashboard that
             * created this lock file.
             */
            BufferedReader in = new BufferedReader(new FileReader(lockFile));
            String otherHost = in.readLine();
            int otherPort = Integer.parseInt(in.readLine());
            String otherTimeStamp = in.readLine();

            /** Have we already successfully bound our webserver to the
             * address and port listed in the lock file? If so, we can
             * safely ignore the lock file.  After all, the dashboard that
             * created it can't still be running if we were able to listen
             * on that socket.  And if we connect to this host/port, we'll
             * just be talking to ourselves!
             */
            if ((!otherHost.equals(currentHost) &&
                 !otherHost.equals(LOOPBACK_ADDR)) || otherPort != port) {

                URL testUrl = new URL("http", otherHost, otherPort, RAISE_URL);
                HttpURLConnection conn =
                    (HttpURLConnection) testUrl.openConnection();
                conn.connect();

                /* If we get to this point, it means we were able to
                 * successfully connect to a running web server.  But it
                 * could be a coincidence that a web server is listening on
                 * the same host/port as the dashboard that originally
                 * created this lockfile.  Check the timestamp returned by
                 * the web server, to see if it matches the timestamp in
                 * the lockfile.
                 */
                String comparedTimeStamp = conn.getHeaderField
                    (WebServer.TIMESTAMP_HEADER);
                if (otherTimeStamp.equals(comparedTimeStamp)) {

                    /* If we get to this point, it means that the dashboard
                     * which created the lockfile is still up and running.
                     * If it honored our request to raise its window, we
                     * can exit silently.  Otherwise, display a warning
                     * before exiting.
                     */
                    if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        System.out.println("response code is " +
                                           conn.getResponseCode());
                        showWarningDialog(getPath(lockFile));
                    }

                    System.exit(0);
                }
            }
        } catch (Exception exc) {
            /* If we reach this point, it means we were UNABLE to contact
             * the dashboard which created the lock file.  We will assume
             * that the dashboard which created the lock file must have
             * crashed before deleting its lock file.  We will simply
             * ignore the lock file and proceed normally. */
        }

        /* Stake our claim to this data by creating a lock file in the
         * current directory.
         */
        try {
            FileWriter out = new FileWriter(lockFile);
            out.write(currentHost + "\n" +
                      port + "\n" +
                      timeStamp + "\n");
            out.close();
        } catch (IOException ioe2) {
            /* If we were unable to create a lock file, display an error
             * message and then exit.
             */
            showFailureDialog(getPath(lockFile));
            System.exit(0);
        }
    }
    private static final String LOOPBACK_ADDR = "127.0.0.1";

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
     * data in the given directory.
     */
    private void showWarningDialog(String directory) {
        Resources r = Resources.getDashBundle("ProcessDashboard");
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
        Resources r = Resources.getDashBundle("ProcessDashboard");
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
        if (lockFile != null)
            lockFile.delete();
        lockFile = null;
    }
}
