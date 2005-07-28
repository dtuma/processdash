// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003,2005 Software Process Dashboard Initiative
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

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.*;

import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.ui.*;


/** Backup data and other files automatically.
 *
 * We want to back up data files (*.dat), defect logs (*.def), the time log
 * (time.log), the state file (state), user settings (pspdash.ini), and
 * the error log (log.txt).
 *
 * Do this each time the dashboard starts or shuts down.
 * Also do it at midnight.
 */
public class FileBackupManager {

    public static final int STARTUP = 0;
    public static final int RUNNING = 1;
    public static final int SHUTDOWN = 2;

    private static OutputStream logFile = null;
    private static final String LOG_FILE_NAME = "log.txt";
    private static final String OLD_BACKUP_TEMP_FILENAME = "temp_old.zip";
    private static final String NEW_BACKUP_TEMP_FILENAME = "temp_new.zip";
    private static final SimpleDateFormat DATE_FMT =
        new SimpleDateFormat("yyyyMMddHHmmss");
    private static final long DAY_MILLIS = 24L /*hours*/ * 60 /*minutes*/
        * 60 /*seconds*/ * 1000 /*millis*/;


    public static void maybeRun(String dataDirName, int when) {
        if (Settings.getBool("backup.enabled", false)) {
            try {
                run(dataDirName, when);
            } catch (Throwable t) {}
        } else if (when == STARTUP &&
                   Settings.getBool("logging.enabled", false)) {
            startLogging(new File(dataDirName));
        }
    }


    public synchronized static void run(String dataDirName, int when) {
        File dataDir = new File(dataDirName);
        File backupDir = new File(dataDir, "backup");
        if (!backupDir.exists()) backupDir.mkdir();
        if (!backupDir.exists()) return;
        boolean loggingEnabled = Settings.getBool("logging.enabled", false);

        if (loggingEnabled)
            stopLogging();

        try {
            backupFiles(dataDir, backupDir, when);
        } catch (Exception e) {
            printError(e);
        }

        if (loggingEnabled && when != SHUTDOWN)
            startLogging(dataDir);
    }


    private static boolean oldBackupIsEmpty;


    // Find the oldest backup in the directory.  Open it for input.
    // Open two zip output streams: one for the new backup, and one for
    // the old backup.
    // Retrieve all the files in the data directory. sort. iterate:
    //   - write the contents to the new backup zipfile.
    //   - compare the contents to the old backup zipfile.
    //      - If the contents are identical, do nothing.
    //      - If the file differ (or aren't present in both places), copy
    //        contents from the old backup input to the old backup output.
    // Close all files.
    // Rename the output files appropriately.
    // Delete old/outdated backup files.
    private static void backupFiles(File dataDir, File backupDir, int when)
        throws IOException
    {
        File[] backupFiles = getBackupFiles(backupDir);
        if (backupFiles == null || backupFiles.length == 0)
            return;        // nothing to do

        File oldestBackupFile = findMostRecentBackupFile(backupFiles);
        File oldBackupTempFile = new File(backupDir, OLD_BACKUP_TEMP_FILENAME);
        File newBackupTempFile = new File(backupDir, NEW_BACKUP_TEMP_FILENAME);

        ZipInputStream oldBackupIn = null;
        ZipOutputStream oldBackupOut = null;
        if (oldestBackupFile != null) {
            oldBackupIn = new ZipInputStream
                (new FileInputStream(oldestBackupFile));
            oldBackupOut = new ZipOutputStream
                (new FileOutputStream(oldBackupTempFile));
            oldBackupOut.setLevel(9);
        }

        ZipOutputStream newBackupOut = new ZipOutputStream
            (new FileOutputStream(newBackupTempFile));
        newBackupOut.setLevel(9);

        File[] dataFiles = getDataFiles(dataDir);
        oldBackupIsEmpty = true;

        for (int i = 0; i < dataFiles.length; i++) {
            File file = dataFiles[i];
            ZipEntry oldEntry = positionStreams(oldBackupIn, oldBackupOut,
                                                file);
            if (oldEntry == null) {
                oldBackupIn = null;
                oldBackupOut = null;
            }
            backupFile(oldEntry, oldBackupIn, oldBackupOut,
                       newBackupOut, file);
        }
        // copy any additional files that might remain in oldBackupIn
        positionStreams(oldBackupIn, oldBackupOut, null);

        // finalize the new backup, and give it its final name.
        newBackupOut.close();
        String outputFilename = getOutputFilename(when, new Date());
        File newBackupFile = new File(backupDir, outputFilename);
        newBackupTempFile.renameTo(newBackupFile);

        // finalize the old backup
        if (oldBackupIn != null)
            oldBackupIn.close();
        if (oldestBackupFile != null) {
            oldestBackupFile.delete();
            if (oldBackupIsEmpty) {
                // ZipOutputStream refuses to create an empty archive.
                // Thus, we have to create a dummy entry to allow the
                // subsequent close() call to succeed.
                oldBackupOut.putNextEntry(new ZipEntry("foo"));
                oldBackupOut.close();
                oldBackupTempFile.delete();
            } else {
                if (oldBackupOut != null)
                    oldBackupOut.close();
                oldBackupTempFile.renameTo(oldestBackupFile);
            }
        }

        cleanupOldBackupFiles(backupFiles);
    }


    private static File[] getBackupFiles(File backupDir) {
        File[] backupFiles = backupDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.startsWith("pdash-");
                }});
        Arrays.sort(backupFiles);
        return backupFiles;
    }


    private static File findMostRecentBackupFile(File[] backupFiles) {
        if (backupFiles != null && backupFiles.length > 0)
            return backupFiles[backupFiles.length - 1];
        else
            return null;
    }


    private static File[] getDataFiles(File dataDir) {
        File[] files = dataDir.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    return inBackupSet(file);
                }});
        Arrays.sort(files);
        return files;
    }


    private static ZipEntry positionStreams(ZipInputStream oldBackupIn,
                                            ZipOutputStream oldBackupOut,
                                            File file)
        throws IOException
    {
        if (oldBackupIn == null || oldBackupOut == null || file == null)
            return null;

        String fileName = null;
        if (file != null)
            fileName = file.getName();

        while (true) {
            ZipEntry e = oldBackupIn.getNextEntry();
            if (e == null) {
                oldBackupIn.close();
                oldBackupOut.close();
                return null;
            }
            if (fileName != null && fileName.equals(e.getName()))
                return e;
            else {
                oldBackupIsEmpty = false;
                copyZipEntry(oldBackupIn, oldBackupOut, e, null);
            }
        }
    }



    private static void copyZipEntry(InputStream oldBackupIn,
                                     ZipOutputStream oldBackupOut,
                                     ZipEntry e,
                                     byte[] prepend)
        throws IOException
    {
        ZipEntry eOut = new ZipEntry(e.getName());
        eOut.setTime(e.getTime());
        oldBackupOut.putNextEntry(eOut);

        if (prepend != null)
            oldBackupOut.write(prepend);

        int bytesRead;
        while ((bytesRead = oldBackupIn.read(copyBuf)) != -1)
            oldBackupOut.write(copyBuf, 0, bytesRead);
        oldBackupOut.closeEntry();
    }
    private static byte[] copyBuf = new byte[1024];


    private static void backupFile(ZipEntry oldEntry,
                                   ZipInputStream oldBackupIn,
                                   ZipOutputStream oldBackupOut,
                                   ZipOutputStream newBackupOut,
                                   File file)
        throws IOException
    {
        ByteArrayOutputStream bytesSeen = null;
        BufferedInputStream oldIn = null;

        // if the old backup file contains an entry for this file,
        if (oldEntry != null && oldBackupIn != null && oldBackupOut != null) {
            // do the prep to start comparing it with the new file.
            bytesSeen = new ByteArrayOutputStream();
            oldIn = new BufferedInputStream(oldBackupIn);
        }

        // create an entry in the new backup archive for this file
        ZipEntry e = new ZipEntry(file.getName());
        e.setTime(file.lastModified());
        e.setSize(file.length());
        newBackupOut.putNextEntry(e);

        BufferedInputStream fileIn =
            new BufferedInputStream(new FileInputStream(file));
        BufferedOutputStream fileOut = new BufferedOutputStream(newBackupOut);
        int c, d;
        while ((c = fileIn.read()) != -1) {
            fileOut.write(c);

            // if we are still comparing the two files for identity
            //  (they've matched so far)
            if (oldIn != null) {
                // read the next byte from the old backup.
                d = oldIn.read();
                if (d != -1)
                    bytesSeen.write(d);
                // if we've found a mismatch between the current file and its
                // old backup,
                if (c != d) {
                    // then eagerly copy the rest of the old backup.
                    copyZipEntry(oldIn, oldBackupOut, oldEntry,
                                 bytesSeen.toByteArray());
                    oldIn = null;
                    bytesSeen = null;
                    oldBackupIn = null;
                    oldBackupOut = null;
                    oldBackupIsEmpty = false;
                }
            }
        }

        if (oldIn != null) {
            // read the next byte from the old backup.
            d = oldIn.read();
            if (d != -1) {
                // if the old backup is longer than the current file, write it
                // to the backup save archive.
                bytesSeen.write(d);
                copyZipEntry(oldIn, oldBackupOut, oldEntry, bytesSeen.
                             toByteArray());
                oldBackupIsEmpty = false;
            }
        }

        // finish writing the file to the new backup archive.
        fileOut.flush();
        newBackupOut.closeEntry();
    }


    private static void cleanupOldBackupFiles(File[] backupFiles) {
        int maxBackupAge = Settings.getInt("backup.keepBackupsNumDays", -1);
        if (maxBackupAge > 0) {
            long delta = maxBackupAge * DAY_MILLIS;
            Date oldAge = new Date(System.currentTimeMillis() - delta);
            String filename = getOutputFilename(STARTUP, oldAge);
            for (int i = 0; i < backupFiles.length-10; i++) {
                File file = backupFiles[i];
                if (file.getName().compareTo(filename) < 0)
                    file.delete();
            }
        }
    }


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
            System.out.println("Process Dashboard - logging started at " +
                               new Date());
            System.out.println(System.getProperty("java.vendor") +
                               " JRE " + System.getProperty("java.version") +
                               "; " + System.getProperty("os.name"));
        } catch (IOException ioe) { printError(ioe); }
    }


    private static String getOutputFilename(int when, Date date) {
        return "pdash-" + DATE_FMT.format(date) + WHEN_STR[when] + ".zip";
    }

    private static final String[] WHEN_STR = {
        "-startup", "-checkpoint", "-shutdown"
    };

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

    private static void printError(Throwable t) {
        printError("Unexpected error in FileBackupManager", t);
    }

    private static void printError(String msg, Throwable t) {
        System.err.println(msg);
        t.printStackTrace();
    }

}
