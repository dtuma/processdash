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


package pspdash;

import java.io.*;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;


/** On Windows systems, this class compiles a list of drive letters
 * that are mapped to network drives, along with the UNC names they
 * are mapped to. It can then be used to translate filenames back and
 * forth between drive letter syntax and UNC syntax.
 */
public class NetworkDriveList {

    private volatile boolean successful = false;
    private Map networkDrives = new TreeMap();
    private volatile Process subprocess = null;


    /** Get a list of network drives.
     * Uses a default maximum delay of 2 seconds.
     */
    public NetworkDriveList() { this(2000); }


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
            if (successful == false)
                synchronized (this) {
                    subprocess.destroy();
                    subprocess = null;
                }
        }
    }

    private void getList() {
        try {
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
                    networkDrives.put(driveLetter, line);
                }
            }
            successful = true;
        } catch (Exception e) {}
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
     * If wasSuccessful() returns false, this will always return false.
     */
    public boolean isNetworkDrive(String driveLetter) {
        if (driveLetter == null) return false;
        driveLetter = driveLetter.toUpperCase();
        return networkDrives.containsKey(driveLetter);
    }


    /** Returns true if the given file is on a network drive.
     *
     * If wasSuccessful() returns false, this will always return false.
     */
    public boolean onNetworkDrive(String filename) {
        if (filename.startsWith("\\\\")) return true;
        return isNetworkDrive(getDriveLetter(filename));
    }


    /** Get the UNC name associated with a particular drive letter.
     * If isNetworkDrive() returns false for this drive letter,
     * returns null.
     */
    public String getUNCName(String driveLetter) {
        if (driveLetter == null) return null;
        driveLetter = driveLetter.toUpperCase();
        return (String) networkDrives.get(driveLetter);
    }


    /** Convert the given filename (in drive letter format) to an UNC name.
     * @return an UNC name, or null if the filename could not be translated.
     */
    public String toUNCName(String filename) {
        if (!successful) return null;

        String driveLetter = getDriveLetter(filename);
        if (driveLetter == null) return null;

        String uncPrefix = getUNCName(driveLetter);
        if (uncPrefix.endsWith("\\"))
            uncPrefix = uncPrefix.substring(0, uncPrefix.length()-1);
        filename = filename.substring(2);
        if (!filename.startsWith("\\"))
            filename = "\\" + filename;
        return uncPrefix + filename;
    }


    /** Convert the given filename (in UNC format) to a drive letter format
     * name.
     * @return a filename beginning with a drive letter, or null if
     * the filename could not be translated.
     */
    public String fromUNCName(String uncName) {
        if (!successful) return null;
        if (uncName == null || !uncName.startsWith("\\\\")) return null;

        Iterator i = networkDrives.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry e = (Map.Entry) i.next();
            String driveLetter = (String) e.getKey();
            String uncPrefix = (String) e.getValue();
            if (uncName.startsWith(uncPrefix))
                return driveLetter + uncName.substring(uncPrefix.length());
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
        if (filename.length() < 2) return null;
        if (filename.charAt(1) != ':') return null;
        return filename.substring(0, 1);
    }
}
