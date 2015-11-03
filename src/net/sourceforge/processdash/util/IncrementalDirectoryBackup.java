// Copyright (C) 2000-2015 Tuma Solutions, LLC
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


/**
 * Incrementally backup the files in a directory.
 * 
 * ZIP files will be placed into the backup directory over time. The last file
 * will always be a full backup. Earlier files will be incremental backups,
 * indicating the files that changed from one backup to the next.
 */
public class IncrementalDirectoryBackup extends DirectoryBackup {

    private static final String LOG_FILE_NAME = "log.txt";
    private static final String HIST_LOG_FILE_NAME = "histLog.txt";
    private static final String OLD_BACKUP_TEMP_FILENAME = "temp_old_zip.tmp";
    private static final String NEW_BACKUP_TEMP_FILENAME = "temp_new_zip.tmp";


    /** The compression level to use, 1-9 */
    private int compressionLevel = 1;

    /** The maximum amount of data to retain in the historical log */
    private int maxHistLogSize = 500000;

    /** A set of filename groupings that should always be backed up together */
    private String[][] atomicFileGroupings;

    /** A forced timestamp to use for the historical log file */
    private long histLogTimestamp;

    /** A list of backed-up files that start with null bytes (maybe corrupt) */
    private Set<File> filesWithNullBytes = new HashSet<File>();


    public int getCompressionLevel() {
        return compressionLevel;
    }

    public void setCompressionLevel(int c) {
        this.compressionLevel = Math.max(1, Math.min(9, c));
    }

    public int getMaxHistLogSize() {
        return maxHistLogSize;
    }

    public void setMaxHistLogSize(int maxHistLogSize) {
        this.maxHistLogSize = maxHistLogSize;
    }

    public String[][] getAtomicFileGroupings() {
        return atomicFileGroupings;
    }

    public void setAtomicFileGroupings(String[][] atomicFileGroupings) {
        this.atomicFileGroupings = atomicFileGroupings;
    }

    public long getHistLogTimestamp() {
        return histLogTimestamp;
    }

    public void setHistLogTimestamp(long timestamp) {
        this.histLogTimestamp = timestamp;
    }

    public Set<File> getFilesWithNullBytes() {
        return filesWithNullBytes;
    }


    @Override
    protected void doBackup(File destFile) throws IOException {
        try {
            backupFiles(destFile, false);
        } catch (Exception e1) {
            try {
                // It is possible that the most recent backup file was corrupt
                // or otherwise unreadable (for example, due to permissions
                // problems). We still want to make a new backup! In particular,
                // we CANNOT let a corrupt backup file prevent all future
                // backups from occurring.  So if we encountered any sort of
                // exception in our attempt to make a regular backup, try a
                // second time, and ignore the most recent backup this time.
                backupFiles(destFile, true);
                printError("Unexpected error in FileBackupManager; " +
                                "ignoring most recent backup", e1);
            } catch (IOException e2) {
                printError(e2);
                throw e2;
            }
        }
    }


    private boolean oldBackupIsEmpty;
    private List atomicFilesInOldBackup;


    // Find the most recent backup in the directory.  Open it for input.
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
    private void backupFiles(File destFile, boolean ignoreLastBackup)
            throws IOException
    {
        List dataFiles = getFilenamesToBackup();
        if (dataFiles == null || dataFiles.size() == 0)
            return;        // nothing to do

        Collections.sort(dataFiles);

        ProfTimer pt = new ProfTimer(IncrementalDirectoryBackup.class,
            "IncrementalDirectoryBackup.backupFiles");

        File dataDir = srcDirectory;
        File backupDir = destDirectory;
        File[] backupFiles = getBackupFiles(backupDir);
        File mostRecentBackupFile =
            (ignoreLastBackup ? null : findMostRecentBackupFile(backupFiles));
        File oldBackupTempFile = new File(backupDir, OLD_BACKUP_TEMP_FILENAME);
        File newBackupTempFile = new File(backupDir, NEW_BACKUP_TEMP_FILENAME);

        ZipOutputStream newBackupOut = new ZipOutputStream(
                new BufferedOutputStream(
                        new FileOutputStream(newBackupTempFile)));
        newBackupOut.setLevel(compressionLevel);

        boolean wroteHistLog = false;

        if (mostRecentBackupFile != null) {
            ZipInputStream oldBackupIn = new ZipInputStream(
                    new TimedInputStream(new BufferedInputStream(
                            new FileInputStream(mostRecentBackupFile)), 60000));
            ZipOutputStream oldBackupOut = new ZipOutputStream(
                    new BufferedOutputStream(new FileOutputStream(
                            oldBackupTempFile)));
            oldBackupOut.setLevel(compressionLevel);
            oldBackupIsEmpty = true;
            atomicFilesInOldBackup = new ArrayList();

            // iterate over all the entries in the old backup
            ZipEntry oldEntry;
            while ((oldEntry = oldBackupIn.getNextEntry()) != null) {
                String filename = oldEntry.getName();
                ThreadThrottler.tick();
                BufferedInputStream bufOldBackupIn = new BufferedInputStream(
                        oldBackupIn);

                if (HIST_LOG_FILE_NAME.equals(filename)) {
                    long histLogModTime = oldEntry.getTime();
                    if (histLogModTime < 1)
                        histLogModTime = mostRecentBackupFile.lastModified();
                    File logFile = new File(dataDir, LOG_FILE_NAME);
                    long currentLogModTime = logFile.lastModified();

                    if (currentLogModTime <= histLogModTime)
                        // at startup, just copy the file to the new backup.
                        copyZipEntry(bufOldBackupIn, newBackupOut, oldEntry);
                    else
                        // other times, append the old file and the new log.
                        writeHistLogFile(bufOldBackupIn, newBackupOut, dataDir);
                    wroteHistLog = true;
                    continue;
                }

                // exclude files in the old backup that don't match our filter.
                // (These were probably written by the extra content supplier.)
                if (!fileFilter.accept(srcDirectory, filename))
                    continue;

                File file = new File(dataDir, filename);

                if (dataFiles.remove(filename)) {
                    // this file is in the old backup zipfile AND in the backup
                    // directory.  Compare the two versions and back up the
                    // file appropriately.
                    backupFile(oldEntry, bufOldBackupIn, oldBackupOut,
                               newBackupOut, file, filename);
                } else {
                    // this file is in the old backup, but is no longer present
                    // in the backup directory.  Copy it over to the new version
                    // of the old backup
                    copyZipEntry(bufOldBackupIn, oldBackupOut, oldEntry);
                    wroteEntryToOldBackup(filename);
                }
            }

            // Some files must always be backed up and restored as an atomic
            // group - otherwise, Bad Things can happen. If one of these files
            // (but not the others) was written to the incremental old backup,
            // add its partners (which presumably must be identical to the file
            // in the dataDir).
            addAtomicFilesToBackup(oldBackupOut);

            oldBackupIn.close();
            mostRecentBackupFile.delete();

            if (oldBackupIsEmpty) {
                // ZipOutputStream refuses to create an empty archive.
                // Thus, we have to create a dummy entry to allow the
                // subsequent close() call to succeed.
                oldBackupOut.putNextEntry(new ZipEntry("foo"));
                oldBackupOut.close();
                oldBackupTempFile.delete();
            } else {
                oldBackupOut.close();
                FileUtils.renameFile(oldBackupTempFile, mostRecentBackupFile);
            }
        }

        // backup all the files that are present in the backup directory that
        // weren't in the old backup zipfile.
        for (Iterator iter = dataFiles.iterator(); iter.hasNext();) {
            ThreadThrottler.tick();
            String filename = (String) iter.next();
            File file = new File(dataDir, filename);
            backupFile(null, null, null, newBackupOut, file, filename);
        }

        // if the old backup didn't contain a historical log file, initialize
        // it with the current log file
        if (wroteHistLog == false)
            writeHistLogFile(null, newBackupOut, dataDir);

        pt.click("Backed up data files");

        // Allow the extra content supplier to save any items of interest.
        if (extraContentSupplier != null)
            extraContentSupplier.addExtraContentToBackup(newBackupOut);

        pt.click("Backed up extra content");

        // finalize the new backup, and give it its final name.
        newBackupOut.close();
        try {
            FileUtils.renameFile(newBackupTempFile, destFile);
        } catch (IOException ioe) {
            // On rare occasions, this rename operation fails. (Perhaps a
            // virus scanner is examining the ZIP?) In that case, try copying
            // the new backup to the final location.
            FileUtils.copyFile(newBackupTempFile, destFile);
            newBackupTempFile.delete();
        }
    }


    private File[] getBackupFiles(File backupDir) {
        File[] backupFiles = backupDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    try {
                        backupFilenameFormat.parse(name);
                        return true;
                    } catch (ParseException e) {
                        return false;
                    }
                }});
        Arrays.sort(backupFiles);
        return backupFiles;
    }

    private File findMostRecentBackupFile(File[] backupFiles) {
        if (backupFiles != null && backupFiles.length > 0)
            return backupFiles[backupFiles.length - 1];
        else
            return null;
    }


    private void copyZipEntry(BufferedInputStream oldBackupIn,
            ZipOutputStream oldBackupOut, ZipEntry e) throws IOException {
        copyZipEntry(oldBackupIn, oldBackupOut, e, null, 0, -1);
    }

    private void copyZipEntry(BufferedInputStream oldBackupIn,
            ZipOutputStream oldBackupOut, ZipEntry e, File prependFromFile,
            int prependFromFileLen, int prependExtraChar) throws IOException {

        // start a new ZIP entry in the old backup
        ZipEntry eOut = new ZipEntry(e.getName());
        eOut.setTime(e.getTime());
        oldBackupOut.putNextEntry(eOut);
        BufferedOutputStream bufOut = new BufferedOutputStream(oldBackupOut);

        // if we have data to prepend, write that data
        if (prependFromFile != null) {
            if (prependFromFileLen > 0) {
                InputStream prependIn = new BufferedInputStream(
                        new FileInputStream(prependFromFile));
                try {
                    while (prependFromFileLen-- > 0)
                        bufOut.write(prependIn.read());
                } finally {
                    prependIn.close();
                }
            }
            if (prependExtraChar != -1)
                bufOut.write(prependExtraChar);
        }

        // copy the remaining data from the old backup input stream
        int b;
        while ((b = oldBackupIn.read()) != -1)
            bufOut.write(b);

        // flush all data and finish the ZIP entry
        bufOut.flush();
        oldBackupOut.closeEntry();
    }


    private void backupFile(ZipEntry oldEntry, BufferedInputStream oldBackupIn,
            ZipOutputStream oldBackupOut, ZipOutputStream newBackupOut,
            File file, String filename) throws IOException
    {
        BufferedInputStream oldIn = null;

        // if the old backup file contains an entry for this file,
        if (oldEntry != null && oldBackupIn != null && oldBackupOut != null) {
            // compare the file sizes to see if they might be the same.
            long oldFileSize = oldEntry.getSize();
            if (oldFileSize != -1 && oldFileSize != file.length()) {
                // if the old entry in the ZIP is a different size than the
                // physical file, then we know they must be different. Just
                // copy the old entry to the old backup and don't bother
                // comparing it to the physical file.
                copyZipEntry(oldIn, oldBackupOut, oldEntry);
                wroteEntryToOldBackup(filename);
            } else {
                // if the files are the same size, or if we don't know the size
                // of the old ZIP entry, we will need to compare the two files
                // to ensure they are byte-for-byte identical.
                oldIn = oldBackupIn;
            }
        }

        // create an entry in the new backup archive for this file
        ZipEntry e = new ZipEntry(filename);
        e.setTime(file.lastModified());
        e.setSize(file.length());
        newBackupOut.putNextEntry(e);

        InputStream fileIn = new BufferedInputStream(new FileInputStream(file));
        OutputStream fileOut = new BufferedOutputStream(newBackupOut);
        int c, d;
        int matchLength = 0;
        boolean firstByte = true;
        try {
            while ((c = fileIn.read()) != -1) {
                fileOut.write(c);
                if (firstByte && c == 0)
                    filesWithNullBytes.add(file);
                firstByte = false;

                // if we are still comparing the two files for identity
                //  (they've matched so far)
                if (oldIn != null) {
                    // read the next byte from the old backup.
                    d = oldIn.read();
                    // if we've found a mismatch between the current file and its
                    // old backup,
                    if (c != d) {
                        // then eagerly copy the rest of the old backup.
                        copyZipEntry(oldIn, oldBackupOut, oldEntry, file,
                            matchLength, d);
                        oldIn = null;
                        oldBackupIn = null;
                        oldBackupOut = null;
                        wroteEntryToOldBackup(filename);
                    } else {
                        matchLength++;
                    }
                }
                ThreadThrottler.tick();
            }
        } finally {
            fileIn.close();
        }

        if (oldIn != null) {
            // read the next byte from the old backup.
            d = oldIn.read();
            if (d != -1) {
                // if the old backup is longer than the current file, write it
                // to the backup save archive.
                copyZipEntry(oldIn, oldBackupOut, oldEntry, file, matchLength, d);
                wroteEntryToOldBackup(filename);
            }
        }

        // finish writing the file to the new backup archive.
        fileOut.flush();
        newBackupOut.closeEntry();
    }

    private void wroteEntryToOldBackup(String filename) {
        oldBackupIsEmpty = false;

        if (atomicFileGroupings != null) {
            for (int g = 0; g < atomicFileGroupings.length; g++) {
                String[] group = atomicFileGroupings[g];
                for (int i = 0; i < group.length; i++) {
                    if (filename.equalsIgnoreCase(group[i])) {
                        atomicFilesInOldBackup.add(filename);
                        break;
                    }
                }
            }
        }
    }


    private void writeHistLogFile(InputStream oldBackupIn,
            ZipOutputStream newBackupOut, File dataDir)
            throws IOException {
        File currentLog = new File(dataDir, LOG_FILE_NAME);

        // start an entry in the zip file for the historical log.
        ZipEntry e = new ZipEntry(HIST_LOG_FILE_NAME);
        e.setTime(histLogTimestamp > 0 ? histLogTimestamp : System.currentTimeMillis());
        newBackupOut.putNextEntry(e);

        // read in the log data we have to work with.
        // read in the previous historical log, and copy appropriate portions
        // to the output ZIP.
        byte[] histLog = null;
        if (oldBackupIn != null) {
            histLog = FileUtils.slurpContents(oldBackupIn, false);

            long totalSize = histLog.length + currentLog.length();
            int skip = (int) Math.max(0, totalSize - maxHistLogSize);

            if (skip < histLog.length)
                newBackupOut.write(histLog, skip, histLog.length - skip);
            else
                histLog = null;
        }

        // if the current log exists, copy appropriate parts to the output ZIP
        if (currentLog.exists() && currentLog.length() > 0) {
            // read in the first few bytes of the current log.
            InputStream currentLogIn = new BufferedInputStream(
                    new FileInputStream(currentLog));
            try {
                byte[] currLogStart = new byte[100];
                int matchLen = currentLogIn.read(currLogStart);

                // see if the most recent entry in the historical log begins with
                // the same characters as the current log.  Since our logs always
                // begin with a full timestamp, a match will indicate that the
                // initial part of the current time log is already present in the
                // historical log file.
                int lastLogEntryPos = findLastLogEntryStart(histLog);
                if (matches(histLog, lastLogEntryPos, currLogStart, 0, matchLen)) {
                    // the two match! Skip over the portion of the current log
                    // that already appears in the historical log file.
                    int duplicateLen = histLog.length - lastLogEntryPos;
                    int skip = duplicateLen - matchLen;
                    if (skip > 0)
                        currentLogIn.skip(skip);
                } else {
                    // the two do not match.  Start a new entry in the historical
                    // log file, and write the first bytes of the current log
                    newBackupOut.write(HIST_SEPARATOR.getBytes());
                    newBackupOut.write(currLogStart, 0, matchLen);
                }

                // finally, copy any remaining portion of the current log file.
                FileUtils.copyFile(currentLogIn, newBackupOut);
            } finally {
                currentLogIn.close();
            }
        }

        newBackupOut.closeEntry();
    }

    private int findLastLogEntryStart(byte[] histLog) {
        if (histLog == null || histLog.length == 0)
            return -1;

        int dashcount = 0;
        for (int pos = histLog.length;  pos-- > 0; ) {
            if (histLog[pos] != '-') {
                dashcount = 0;
            } else if (++dashcount > 60) {
                int result = pos + dashcount;
                while (result < histLog.length) {
                    if (Character.isWhitespace(histLog[result]))
                        result++;
                    else
                        return result;
                }
                return -1;
            }
        }
        return -1;
    }

    private boolean matches(byte[] a, int aPos, byte[] b, int bPos, int len) {
        if (a == null || b == null)
            return false;
        if (aPos < 0 || aPos + len > a.length)
            return false;
        if (bPos < 0 || bPos + len > b.length)
            return false;
        while (len-- > 0) {
            if (a[aPos++] != b[bPos++])
                return false;
        }
        return true;
    }

    private static final String HIST_SEPARATOR = "--------------------"
            + "--------------------------------------------------"
            + System.getProperty("line.separator");

    private void addAtomicFilesToBackup(ZipOutputStream zipOut) throws IOException {
        if (atomicFileGroupings != null) {
            for (int g = 0; g < atomicFileGroupings.length; g++) {
                addAtomicFilesToBackup(zipOut, atomicFileGroupings[g]);
            }
        }
    }

    private void addAtomicFilesToBackup(ZipOutputStream zipOut,
            String[] filenames) throws IOException {
        List matchedFiles = new ArrayList();
        List unmatchedFiles = new ArrayList();
        for (int i = 0; i < filenames.length; i++) {
            String filename = filenames[i];
            if (containsIgnoreCase(atomicFilesInOldBackup, filename))
                matchedFiles.add(filename);
            else
                unmatchedFiles.add(filename);
        }
        if (!matchedFiles.isEmpty() && !unmatchedFiles.isEmpty()) {
            for (Iterator i = unmatchedFiles.iterator(); i.hasNext();) {
                String filename = (String) i.next();
                File file = new File(srcDirectory, filename);
                backupFile(null, null, null, zipOut, file, filename);
            }
        }
    }
    private boolean containsIgnoreCase(List list, String str) {
        for (Iterator i = list.iterator(); i.hasNext();) {
            String oneItem = (String) i.next();
            if (oneItem.equalsIgnoreCase(str))
                return true;
        }
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
