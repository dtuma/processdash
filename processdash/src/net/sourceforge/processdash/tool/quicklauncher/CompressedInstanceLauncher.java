// Copyright (C) 2006-2013 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.quicklauncher;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.ev.EVCalculator;
import net.sourceforge.processdash.tool.bridge.client.AbstractWorkingDirectory;
import net.sourceforge.processdash.tool.bridge.client.DirectoryPreferences;
import net.sourceforge.processdash.tool.bridge.client.TeamServerSelector;
import net.sourceforge.processdash.tool.bridge.impl.DashboardInstanceStrategy;
import net.sourceforge.processdash.tool.export.mgr.ExternalResourceManager;
import net.sourceforge.processdash.util.DateUtils;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.XorInputStream;

public class CompressedInstanceLauncher extends DashboardInstance {

    public static final String PDASH_BACKUP_EXTENSION = "pdbk";
    public static final int PDASH_BACKUP_XOR_BITS = 0x55;

    private static final String TEMP_DIR_PREFIX = "pdash-quicklaunch-";

    private static final String EXT_RES_MGR_ARG = "-D"
            + ExternalResourceManager.INITIALIZATION_MODE_PROPERTY_NAME + "="
            + ExternalResourceManager.INITIALIZATION_MODE_ARCHIVE;
    private static final String DISABLE_BACKUP = "-D"
            + Settings.SYS_PROP_PREFIX + "backup.enabled=false";
    private static final String DISABLE_PROCESS_LOCK = "-D"
            + AbstractWorkingDirectory.NO_PROCESS_LOCK_PROPERTY + "=true";
    private static final String DISABLE_TEAM_SERVER = "-D"
            + TeamServerSelector.DISABLE_TEAM_SERVER_PROPERTY + "=true";
    private static final String DISABLE_DATABASE_PLUGIN = "-D"
            + Settings.SYS_PROP_PREFIX + "tpidw.enabled=false";
    private static final String READ_WRITE_ARG = "-D"
            + Settings.SYS_PROP_PREFIX + "readOnly=false";

    private File compressedData;

    private String prefix;

    private long dataTimeStamp;

    private boolean sawWbsXml, sawProjDump;

    public CompressedInstanceLauncher(File compressedData, String prefix) {
        this.compressedData = compressedData;
        this.prefix = prefix;
        setDisplay(compressedData.getAbsolutePath());
    }

    public void launch(DashboardProcessFactory processFactory) {
        File pspdataDir;

        try {
            pspdataDir = uncompressData();
        } catch (IOException e) {
            String message = resources.format(
                    "Errors.Zip.Read_Error_Simple_FMT",
                    compressedData .getAbsolutePath(),
                    e.getLocalizedMessage());
            throw new LaunchException(message, e);
        }

        List vmArgs = new ArrayList();
        vmArgs.add(EXT_RES_MGR_ARG);
        vmArgs.add(DISABLE_BACKUP);
        vmArgs.add(DISABLE_PROCESS_LOCK);
        vmArgs.add(DISABLE_TEAM_SERVER);
        if (dataTimeStamp > 0)
            vmArgs.add("-D" + Settings.SYS_PROP_PREFIX
                    + EVCalculator.FIXED_EFFECTIVE_DATE_SETTING + "="
                    + dataTimeStamp);
        if (sawWbsXml && !sawProjDump)
            vmArgs.add(DISABLE_DATABASE_PLUGIN);
        if (processFactory.hasVmArg("-DreadOnly=true") == false)
            vmArgs.add(READ_WRITE_ARG);

        launchApp(processFactory, vmArgs, pspdataDir);

        if (process != null) {
            waitForCompletion();
            cleanupDataDir(pspdataDir);
        }
    }

    private File uncompressData() throws IOException {
        File tempDir = File.createTempFile(TEMP_DIR_PREFIX, "",
                DirectoryPreferences.getMasterWorkingDirectory());
        tempDir.delete();
        tempDir.mkdir();
        dataTimeStamp = 0;
        sawWbsXml = sawProjDump = false;

        ZipInputStream in = openZipStream(compressedData);
        uncompressData(tempDir, in, prefix);
        in.close();

        return tempDir;
    }

    private void uncompressData(File tempDir, ZipInputStream in,
            String fullPrefix) throws IOException {
        String prefix = fullPrefix;
        String remainingPrefix = null;
        if (fullPrefix.indexOf(SUBZIP_SEPARATOR) != -1) {
            int pos = fullPrefix.indexOf(SUBZIP_SEPARATOR);
            prefix = fullPrefix.substring(0, pos);
            remainingPrefix = fullPrefix.substring(pos
                    + SUBZIP_SEPARATOR.length());
        }

        ZipEntry e;
        while ((e = in.getNextEntry()) != null) {
            String filename = e.getName().replace('\\', '/');
            if (remainingPrefix != null) {
                if (filename.equals(prefix)) {
                    ZipInputStream subZip = openZipStream(in, filename);
                    uncompressData(tempDir, subZip, remainingPrefix);
                }

            } else if (filename.startsWith(prefix) && !e.isDirectory()) {
                filename = filename.substring(prefix.length());
                File destFile = new File(tempDir, filename);
                if (filename.indexOf('/') != -1)
                    destFile.getParentFile().mkdirs();
                if ("wbs.xml".equals(destFile.getName()))
                    sawWbsXml = true;
                else if ("projDump.xml".equals(destFile.getName()))
                    sawProjDump = true;
                FileUtils.copyFile(in, destFile);
                if (e.getTime() != -1) {
                    destFile.setLastModified(e.getTime());
                    dataTimeStamp = Math.max(dataTimeStamp, e.getTime());
                }
            }
        }
    }

    private void cleanupDataDir(File pspdataDir) {
        try {
            FileUtils.deleteDirectory(pspdataDir, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean equals(Object obj) {
        if (obj instanceof CompressedInstanceLauncher) {
            CompressedInstanceLauncher that = (CompressedInstanceLauncher) obj;
            return (eq(this.prefix, that.prefix) && eq(this.compressedData,
                    that.compressedData));
        }

        return false;
    }

    @Override
    public int hashCode() {
        int result = 0;
        if (this.prefix != null)
            result = this.prefix.hashCode();
        if (this.compressedData != null)
            result ^= this.compressedData.hashCode();
        return result;
    }

    static boolean isCompressedInstanceFilename(String basename) {
        basename = basename.toLowerCase();
        return basename.endsWith(".zip")
                || basename.endsWith(PDASH_BACKUP_EXTENSION);
    }

    static List getLaunchTargetsWithinZip(File zipfile) throws IOException {
        List result = new ArrayList();
        ZipInputStream in = openZipStream(zipfile);
        collectLaunchTargetPrefixes(result, "", in);
        FileUtils.safelyClose(in);
        return result;
    }

    private static void collectLaunchTargetPrefixes(List result,
            String prepend, ZipInputStream in) throws IOException {
        ZipEntry e;
        while ((e = in.getNextEntry()) != null) {
            String filename = e.getName().replace('\\', '/');
            if (filename.endsWith(DATA_DIR_FILE_ITEM)) {
                int prefixLen = filename.length() - DATA_DIR_FILE_ITEM.length();
                String prefix = filename.substring(0, prefixLen);
                result.add(prepend + prefix);
                result.remove(WBS_DIR_FILE_ITEM);

            } else if (filename.equals(WBS_DIR_FILE_ITEM)
                    && "".equals(prepend) && result.isEmpty()) {
                result.add(WBS_DIR_FILE_ITEM);

            } else if (isCompressedInstanceFilename(filename)
                    && filename.toLowerCase().indexOf("backup/") == -1) {
                ZipInputStream subIn = openZipStream(in, filename);
                collectLaunchTargetPrefixes(result, prepend + filename
                        + SUBZIP_SEPARATOR, subIn);
            }
        }
    }

    private static ZipInputStream openZipStream(File f)
            throws FileNotFoundException {
        InputStream compressedIn = new BufferedInputStream(new FileInputStream(
                f));
        return openZipStream(compressedIn, f.getName());
    }

    private static ZipInputStream openZipStream(InputStream in, String filename) {
        if (filename.toLowerCase().endsWith(PDASH_BACKUP_EXTENSION))
            in = new XorInputStream(in, PDASH_BACKUP_XOR_BITS);

        return new ZipInputStream(in);
    }

    /** A string that will be used to separate the names of nested zip files */
    private static final String SUBZIP_SEPARATOR = " -> ";

    public static void cleanupOldDirectories() {
        File tempDirectory = DirectoryPreferences
                .getMasterWorkingDirectory();
        final File[] files = tempDirectory.listFiles();
        Thread t = new Thread("Quick Launch Cleanup Thread") {
            public void run() {
                cleanupOldDirectoriesImpl(files);
            }};
        t.setDaemon(true);
        t.start();
    }

    private static void cleanupOldDirectoriesImpl(File[] files) {
        try {
            if (files == null) return;
            for (int i = 0; i < files.length; i++) {
                if (isOldDirectoryToCleanup(files[i]))
                    FileUtils.deleteDirectory(files[i], true);
            }

        } catch (IOException ioe) {}
    }

    private static boolean isOldDirectoryToCleanup(File dir) {
        if (!dir.isDirectory())
            return false;
        if (!dir.getName().startsWith(TEMP_DIR_PREFIX))
            return false;

        File lockFile = new File(dir, DashboardInstanceStrategy.LOCK_FILE_NAME);
        if (!lockFile.exists())
            return true;

        File logFile = new File(dir, "log.txt");
        long logFileMod = logFile.lastModified();
        if (logFileMod > 0) {
            long age = System.currentTimeMillis() - logFileMod;
            if (age > 7 * DateUtils.DAYS)
                return true;
        }

        return false;
    }

    /**
     * Return true if the current dashboard process is working against a
     * temporary copy of data that was extracted from a ZIP file.
     * @since 1.12.1.1
     */
    public static boolean isRunningFromCompressedData() {
        String val = System.getProperty(
            ExternalResourceManager.INITIALIZATION_MODE_PROPERTY_NAME);
        return ExternalResourceManager.INITIALIZATION_MODE_ARCHIVE.equals(val);
    }

}
