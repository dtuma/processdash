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
import java.util.*;
import java.util.zip.*;


/** Backup data and other files automatically.
 *
 * We want to back up data files (*.dat), defect logs (*.def), the time log
 * (time.log), the state file (state), user settings (pspdash.ini), and
 * the error log (log.txt).
 *
 * Do this each time the dashboard starts.  Also do it at midnight.
 */
public class FileBackupManager {

    public static final int STARTUP = 0;
    public static final int RUNNING = 1;
    public static final int SHUTDOWN = 2;

    private static OutputStream logFile = null;
    private static final String LOG_FILE_NAME = "log.txt";

    public static void maybeRun(String dataDirName, int when) {
        if ("true".equalsIgnoreCase(Settings.getVal("backup.enabled"))) try {
            run(dataDirName, when);
        } catch (Throwable t) {}
    }

    public synchronized static void run(String dataDirName, int when) {
        File dataDir = new File(dataDirName);
        File backupDir = new File(dataDir, "backup");
        if (!backupDir.exists()) backupDir.mkdir();
        if (!backupDir.exists()) return;

        File fileList = new File(backupDir, "list.txt");
        HashMap origFileTimes = new HashMap();
        HashMap origFileSizes = new HashMap();
        HashMap fileTimes = new HashMap();
        HashMap fileSizes = new HashMap();

        stopLogging();

        loadFileInfo(fileList, origFileTimes, origFileSizes);

        backupFiles(dataDir, backupDir,
                    origFileTimes, origFileSizes,
                    fileTimes, fileSizes, when);

        saveFileInfo(fileList, fileTimes, fileSizes);

        if (when != SHUTDOWN)
            startLogging(dataDir);
    }

    private static void loadFileInfo(File list, Map fileTimes, Map fileSizes) {
        if (!list.exists()) return;
        try {
            BufferedReader in = new BufferedReader(new FileReader(list));
            String line, name, time, size;
            while ((line = in.readLine()) != null) {
                StringTokenizer tok = new StringTokenizer(line, DELIM);
                name = tok.nextToken();
                time = tok.nextToken();
                size = tok.nextToken();

                fileTimes.put(name, new Long(time));
                fileSizes.put(name, new Long(size));
            }
            in.close();
        } catch (Exception e) { printError(e); }
    }

    private static void saveFileInfo(File list, Map fileTimes, Map fileSizes) {
        try {
            Writer out = new FileWriter(list);
            Iterator i = fileTimes.keySet().iterator();
            String name;
            while (i.hasNext()) {
                name = (String) i.next();
                out.write(name);
                out.write(DELIM);
                out.write(fileTimes.get(name).toString());
                out.write(DELIM);
                out.write(fileSizes.get(name).toString());
                out.write("\n");
            }
            out.flush();
            out.close();
        } catch (Exception e) { printError(e); }
    }
    private static final String DELIM = "";

    private static void stopLogging() {
        if (logFile != null) try {
            ConsoleWindow.getInstalledConsole().setCopyOutputStream(null);
            logFile.flush();
            logFile.close();
        } catch (IOException ioe) { printError(ioe); }
    }

    private static void startLogging(File dataDir) {
        try {
            File out = new File(dataDir, LOG_FILE_NAME);
            logFile = new FileOutputStream(out);
            ConsoleWindow.getInstalledConsole().setCopyOutputStream(logFile);
            System.out.println("Version 1.5.0.3 - logging started at " +
                               new Date());
            System.out.println(System.getProperty("java.vendor") +
                               " JRE " + System.getProperty("java.version") +
                               "; " + System.getProperty("os.name"));
        } catch (IOException ioe) { printError(ioe); }
    }

    private static void backupFiles(File dataDir, File backupDir,
                                    Map origFileTimes, Map origFileSizes,
                                    Map fileTimes, Map fileSizes, int when) {

        long now = System.currentTimeMillis();
        File zipFile = new File(backupDir, now + WHEN_STR[when] + ".zip");
        FileOutputStream out = null;
        ZipOutputStream zip = null;
        try {
            out = new FileOutputStream(zipFile);
            zip = new ZipOutputStream(out);
            zip.setLevel(9);
        } catch (IOException ioe) {
            printError("Error while creating backup file " + zipFile, ioe);
        }

        File[] srcFiles = dataDir.listFiles();
        String name;
        boolean backedSomethingUp = false;
        for (int i = srcFiles.length;   i-- > 0; )
            if (srcFiles[i].isFile() &&
                inBackupSet(srcFiles[i]) &&
                maybeBackupFile(srcFiles[i], zip,
                                origFileTimes, origFileSizes,
                                fileTimes, fileSizes))
                backedSomethingUp = true;

        try {
            if (backedSomethingUp)
                zip.close();
            else {
                zip = null;
                out.close();
                zipFile.delete();
            }
        } catch (Exception ioe) {
            printError("Error while closing or deleting " + zipFile, ioe);
        }
    }
    private static final String[] WHEN_STR = { "-a", "-m", "-z" };

    private static boolean inBackupSet(File f) {
        String name = f.getName();
        if (name.endsWith(".dat") ||    // backup data files
            name.endsWith(".def") ||    // backup defect logs
            name.equals("time.log") ||  // backup the time log
            name.equals("state") ||     // backup the state file
            name.equals("pspdash.ini")) // backup the user settings
            return true;
        if (name.equals(LOG_FILE_NAME) && f.length() > 0)
            // backup the log file if it contains anything.
            return true;
        return false;
    }

    private static boolean maybeBackupFile(File f, ZipOutputStream zip,
                                           Map origFileTimes,
                                           Map origFileSizes,
                                           Map fileTimes, Map fileSizes) {
        String name = f.getName();

        Long fileTime = new Long(f.lastModified());
        Long fileSize = new Long(f.length());
        fileTimes.put(name, fileTime);
        fileSizes.put(name, fileSize);

        Long prevTime = (Long) origFileTimes.get(name);
        Long prevSize = (Long) origFileSizes.get(name);

        if (prevTime != null && prevTime.equals(fileTime) &&
            prevSize != null && prevSize.equals(fileSize))
            return false;

        try {
            ZipEntry e = new ZipEntry(name);
            e.setTime(fileTime.longValue());
            zip.putNextEntry(e);

            byte[] buffer = new byte[4096];
            FileInputStream in = new FileInputStream(f);
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1)
                zip.write(buffer, 0, bytesRead);
            in.close();

        } catch (IOException ioe) {
            printError("Error while backing up " + name, ioe);
        }

        return true;
    }

    private static void printError(Throwable t) {
        printError("Unexpected error in FileBackupManager", t);
    }
    private static void printError(String msg, Throwable t) {
        System.err.println(msg);
        t.printStackTrace();
    }

}
