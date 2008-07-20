// Copyright (C) 2002-2007 Tuma Solutions, LLC
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

package net.sourceforge.processdash.util;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;


/** On Windows systems, this class compiles a list of shared
 * directories and drive letters that are mapped to network drives,
 * along with the UNC names they are mapped to. It can then be used to
 * translate filenames back and forth between drive letter syntax and
 * UNC syntax.
 */
public class NetworkDriveList {

    private volatile boolean successful = false;
    private Map networkDrives = new TreeMap();
    private volatile Process subprocess = null;

    private static final Logger logger = Logger.getLogger(
            NetworkDriveList.class.getName());


    /** Get a list of network drives.
     * Uses a default maximum delay of 3 seconds.
     */
    public NetworkDriveList() { this(3000); }


    /** Get a list of network drives.
     * @param maxDelay the maximum number of milliseconds to spend
     *    creating the drive list.  If the operation takes longer than
     *    this, the resulting drive list will be empty, and
     *    wasSuccessful() will return false.
     */
    public NetworkDriveList(int maxDelay) {
        if (isWindows()) {
            Thread t = new Thread() {
                    public void run() { getList(); }};
            t.setDaemon(true);
            t.start();
            try {
                t.join(maxDelay);
            } catch (InterruptedException ie) {}
            if (successful == false) {
                synchronized (this) {
                    if (subprocess != null)
                        subprocess.destroy();
                    subprocess = null;
                }
                logger.severe("Network drive list was not built");
            }
        }
    }

    private void getList() {
        try {
            listMappedDrives();
            listSharedDrives();
            successful = true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unable to build network drive list", e);
        }
    }


    /** Find the drives mapped on the current system, and add them to the
     * list.
     */
    private void listMappedDrives() throws Exception {
        synchronized (this) {
            subprocess = Runtime.getRuntime().exec("net use");
        }
        BufferedReader in = new BufferedReader
            (new InputStreamReader(subprocess.getInputStream()));
        String line;
        boolean sawHeader = false;
        while ((line = in.readLine()) != null) {
            if (!sawHeader && line.startsWith("-----"))
                sawHeader = true;
            else if (sawHeader) {
                int pos = line.indexOf(':');
                if (pos < 2) continue;
                if (" \t".indexOf(line.charAt(pos-2)) == -1) continue;
                String driveLetter = line.substring(pos-1, pos);
                driveLetter = driveLetter.toUpperCase();
                pos = line.indexOf('\\', pos);
                if (pos == -1) continue;
                line = line.substring(pos);
                pos = line.indexOf('\t');
                if (pos != -1) line = line.substring(0, pos);
                pos = line.indexOf(' ');
                if (pos != -1) line = line.substring(0, pos);
                String drivePath = getDrivePath(driveLetter);
                networkDrives.put(drivePath, line);
                logger.fine("Identified mapped network drive: '"
                        + drivePath + "' => '" + line + "'");
            }
        }
        subprocess.waitFor();
    }


    /** Find the local directories which are shared as networked drives,
     * and add them to the list.
     */
    private void listSharedDrives() throws Exception {
        synchronized (this) {
            subprocess = Runtime.getRuntime().exec("net config workstation");
        }
        BufferedReader in = new BufferedReader
            (new InputStreamReader(subprocess.getInputStream()));
        String line;
        String computerName = null;
        while ((line = in.readLine()) != null) {
            if (computerName != null) continue;
            int pos = line.indexOf("\\\\");
            if (pos != -1)
                computerName = line.substring(pos).trim();
        }
        if (computerName == null)
            return;


        synchronized (this) {
            subprocess = Runtime.getRuntime().exec("net share");
        }
        in = new BufferedReader
            (new InputStreamReader(subprocess.getInputStream()));
        boolean sawHeader = false;
        String lastLine = "";
        while ((line = in.readLine()) != null) {
            if (!sawHeader && line.startsWith("-----"))
                sawHeader = true;
            else if (sawHeader) {
                int pos = line.indexOf(':');
                if (pos < 2 || " \t".indexOf(line.charAt(pos-2)) == -1) {
                    lastLine = line;
                    continue;
                }
                String shareName = line.substring(0, pos-1).trim();
                if (shareName.length() == 0)
                    shareName = lastLine.trim();
                if (shareName.endsWith("$"))
                    continue;  // don't include the "default" shares

                String resourceName = line.substring(pos-1);
                pos = resourceName.indexOf('\t');
                if (pos != -1) resourceName = resourceName.substring(0, pos);
                pos = resourceName.indexOf("  ");
                if (pos != -1) resourceName = resourceName.substring(0, pos);
                resourceName = resourceName.trim();

                if (shareName.length() == 0 || resourceName.length() == 0) {
                    lastLine = line;
                    continue;
                } else {
                    shareName = computerName + "\\" + shareName;
                    if (!resourceName.endsWith("\\"))
                        resourceName = resourceName + "\\";
                    networkDrives.put(resourceName, shareName);
                    logger.fine("Identified network share: '"
                            + resourceName + "' => '" + shareName + "'");
                }
            }
        }
    }


    /** Return true if this object was able to successfully compile a list
     * of network drives.
     *
     * On Windows systems, this will return false if the operation took
     * too long and was abandoned.
     *
     * On non-Windows systems, this will always return false.
     */
    public boolean wasSuccessful() {
        return successful;
    }


    /** Returns true if the given drive letter names a network drive.
     *
     * If wasSuccessful() returns false, this will generally return false.
     */
    public boolean isNetworkDrive(String driveLetter) {
        if (driveLetter == null) return false;
        String drivePath = getDrivePath(driveLetter);
        boolean result = networkDrives.containsKey(drivePath);
        logger.finer("isNetworkDrive(" + driveLetter + ") = '" + result + "'");
        return result;
    }


    /** Returns true if the given file is on a network drive.
     *
     * If wasSuccessful() returns false, this will generally return false.
     */
    public boolean onNetworkDrive(String filename) {
        if (filename.startsWith("\\\\")) return true;
        boolean result = (toUNCName(filename) != null);
        logger.finer("onNetworkDrive(" + filename + ") = '" + result + "'");
        return result;
    }


    /** Get the UNC name associated with a particular drive letter.
     * If isNetworkDrive() returns false for this drive letter,
     * returns null.
     */
    public String getUNCName(String driveLetter) {
        driveLetter = getDriveLetter(driveLetter);
        if (driveLetter == null) return null;
        String drivePath = getDrivePath(driveLetter);
        String result = (String) networkDrives.get(drivePath);
        logger.finer("getUNCName(" + driveLetter + ") = '" + result + "'");
        return result;
    }


    /** Convert the given filename (in drive letter format) to an UNC name.
     * @return an UNC name, or null if the filename could not be translated.
     */
    public String toUNCName(String filename) {
        String result = toUNCNameImpl(filename);
        logger.finer("toUNCNameImpl(" + filename + ") = '" + result + "'");
        return result;
    }
    private String toUNCNameImpl(String filename) {
        if (!successful) return null;
        if (filename == null || filename.startsWith("\\\\")) return filename;

        Iterator i = networkDrives.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry e = (Map.Entry) i.next();
            String resPrefix = (String) e.getKey();
            String uncPrefix = (String) e.getValue();

            if (filename.regionMatches
                (true, 0, resPrefix, 0, resPrefix.length())) {
                if (uncPrefix.endsWith("\\"))
                    uncPrefix = uncPrefix.substring(0, uncPrefix.length()-1);
                filename = filename.substring(resPrefix.length() - 1);
                return uncPrefix + filename;
            } else if (resPrefix != null
                    && resPrefix.equalsIgnoreCase(filename+"\\")) {
                if (uncPrefix.endsWith("\\"))
                    uncPrefix = uncPrefix.substring(0, uncPrefix.length()-1);
                return uncPrefix;
            }
        }

        return null;
    }


    /** Convert the given filename (in UNC format) to a drive letter format
     * name.
     * @return a filename beginning with a drive letter, or null if
     * the filename could not be translated.
     */
    public String fromUNCName(String uncName) {
        String result = fromUNCNameImpl(uncName);
        logger.finer("fromUNCName(" + uncName + ") = '" + result + "'");
        return result;
    }
    private String fromUNCNameImpl(String uncName) {
        if (!successful) return null;
        if (uncName == null || !uncName.startsWith("\\\\")) return null;

        Iterator i = networkDrives.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry e = (Map.Entry) i.next();
            String resPrefix = (String) e.getKey();
            String uncPrefix = (String) e.getValue();

            if (uncName.equalsIgnoreCase(uncPrefix))
                return resPrefix;

            if (!uncPrefix.endsWith("\\"))
                uncPrefix = uncPrefix + "\\";

            if (uncName.regionMatches
                (true, 0, uncPrefix, 0, uncPrefix.length())) {
                uncName = uncName.substring(uncPrefix.length());
                return resPrefix + uncName;
            }
        }

        return null;
    }



    /** Return true if the operating system is a variant of Windows */
    private boolean isWindows() {
        return (System.getProperty("os.name").indexOf("Windows") != -1);
    }

    /** Extract the drive letter from a filename */
    private String getDriveLetter(String filename) {
        if (filename == null) return null;

        // is it already a drive letter?
        if (filename.length() == 1 &&
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ".indexOf(filename.charAt(0)) != -1)
            return filename;

        if (filename.length() < 2) return null;
        if (filename.charAt(1) != ':') return null;
        return filename.substring(0, 1).toUpperCase();
    }

    /** Get the filename path corresponding to a drive letter */
    private String getDrivePath(String driveLetter) {
        return driveLetter.toUpperCase() + ":\\";
    }

}
