// Copyright (C) 2008-2014 Tuma Solutions, LLC
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

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public class DashboardBackupFactory {

    private static int keepBackupsNumDays = 42;
    private static int compressionLevel = 1;
    private static int maxHistLogSize = 500000;

    public static int getKeepBackupsNumDays() {
        return keepBackupsNumDays;
    }

    public static void setKeepBackupsNumDays(int keepBackupsNumDays) {
        DashboardBackupFactory.keepBackupsNumDays = keepBackupsNumDays;
    }

    public static int getCompressionLevel() {
        return compressionLevel;
    }

    public static void setCompressionLevel(int compressionLevel) {
        DashboardBackupFactory.compressionLevel = compressionLevel;
    }

    public static int getMaxHistLogSize() {
        return maxHistLogSize;
    }

    public static void setMaxHistLogSize(int maxHistLogSize) {
        DashboardBackupFactory.maxHistLogSize = maxHistLogSize;
    }

    /**
     * Create a backup object which performs backups of a dashboard instance
     * directory
     * 
     * @param dataDir the directory to back up
     * @return a DirectoryBackup object
     */
    public static IncrementalDirectoryBackup getDashboardBackup(File dataDir) {
        IncrementalDirectoryBackup result = new IncrementalDirectoryBackup();
        result.setSrcDirectory(dataDir);
        result.setAtomicFileGroupings(DASH_ATOMIC_FILE_GROUPS);
        result.setBackupFilenameFormat(DASH_BACKUP_FILENAME_FMT);
        result.setFileFilter(DASH_FILE_FILTER);
        result.setMaxHistLogSize(maxHistLogSize);
        result.setAutoCleanupNumDays(keepBackupsNumDays);
        result.setCompressionLevel(compressionLevel);
        return result;
    }

    private static final String[][] DASH_ATOMIC_FILE_GROUPS = { {
            "timelog.xml", "timelog2.xml" } };

    private static final String DASH_BACKUP_FILENAME_FMT = "pdash-{0}-{1}.zip";

    private static class DashFileFilter implements FilenameFilter {

        public boolean accept(File dir, String name) {
            name = name.toLowerCase();

            // the only subdirectory we backup is the cms subdirectory, and we
            // only accept XML files found there.
            if (name.indexOf('/') != -1) {
                return name.startsWith("cms/")
                        && (name.endsWith("/") || name.endsWith(".xml"));
            }

            // don't back up invisible operating system files on Unix/Linux/Mac
            if (name.startsWith("."))
                return name.equals(".pspdash");

            // don't include the temporary files that are created by the
            // RobustFileOutputStream
            if (name.startsWith(RobustFileOutputStream.OUT_PREFIX))
                return false; // don't copy around temporary files

            if (name.equals("log.txt") || // backup the log file
                name.endsWith(".dat") || // backup data files
                name.endsWith(".def") || // backup defect logs
                name.equals("time.log") || // backup the time log
                name.equalsIgnoreCase("timelog.xml") ||
                name.equalsIgnoreCase("timelog2.xml") ||
                name.equals("state") || // backup the state file
                name.equals(".pspdash") || // backup the user settings
                name.equals("pspdash.ini") ||
                name.endsWith(".ico"))  // backup icon preferences
                return true;

            // don't back up any other files.
            return false;
        }

    }

    public static final FilenameFilter DASH_FILE_FILTER = new DashFileFilter();

    /**
     * Create a backup object which performs backups of a wbs data directory
     * 
     * @param wbsDir the directory to back up
     * @return a DirectoryBackup object
     */
    public static DirectoryBackup getWbsBackup(File wbsDir) {
        FullDirectoryBackup result = new FullDirectoryBackup();
        result.setSrcDirectory(wbsDir);
        result.setFileFilter(WBS_FILE_FILTER);
        result.setBackupFilenameFormat(WBS_BACKUP_FILENAME_FMT);
        result.setAutoCleanupNumDays(keepBackupsNumDays);
        return result;
    }

    private static final String WBS_BACKUP_FILENAME_FMT = "backup-{0}-{1}.zip";

    private static class WBSFileFilter implements FilenameFilter {

        Set includedNames;

        public WBSFileFilter() {
            Set m = new HashSet();
            m.add("team.xml");
            m.add("team2.xml");
            m.add("wbs.xml");
            m.add("workflow.xml");
            m.add("tabs.xml");
            m.add("changeHistory.xml");
            this.includedNames = Collections.unmodifiableSet(m);
        }

        public boolean accept(File dir, String name) {
            return includedNames.contains(name.toLowerCase());
        }

    }

    public static final FilenameFilter WBS_FILE_FILTER = new WBSFileFilter();
}
