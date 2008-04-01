// Copyright (C) 2006-2008 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC: processdash-devel@lists.sourceforge.net

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
import net.sourceforge.processdash.tool.bridge.client.AbstractWorkingDirectory;
import net.sourceforge.processdash.tool.bridge.client.DirectoryPreferences;
import net.sourceforge.processdash.tool.bridge.client.TeamServerSelector;
import net.sourceforge.processdash.tool.bridge.impl.DashboardInstanceStrategy;
import net.sourceforge.processdash.tool.export.mgr.ExternalResourceManager;
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

    private File compressedData;

    private String prefix;

    private long dataTimeStamp;

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
            vmArgs.add("-D" + Settings.SYS_PROP_PREFIX + "ev.effectiveDate="
                    + dataTimeStamp);

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

    static boolean isCompressedInstanceFilename(String basename) {
        basename = basename.toLowerCase();
        return basename.endsWith(".zip")
                || basename.endsWith(PDASH_BACKUP_EXTENSION);
    }

    static List getDataDirectoriesWithinZip(File zipfile) throws IOException {
        List result = new ArrayList();
        ZipInputStream in = openZipStream(zipfile);
        collectDataDirectoryPrefixes(result, "", in);
        return result;
    }

    private static void collectDataDirectoryPrefixes(List result,
            String prepend, ZipInputStream in) throws IOException {
        ZipEntry e;
        while ((e = in.getNextEntry()) != null) {
            String filename = e.getName().replace('\\', '/');
            if (filename.endsWith(DATA_DIR_FILE_ITEM)) {
                int prefixLen = filename.length() - DATA_DIR_FILE_ITEM.length();
                String prefix = filename.substring(0, prefixLen);
                result.add(prepend + prefix);
            } else if (isCompressedInstanceFilename(filename)
                    && filename.toLowerCase().indexOf("backup/") == -1) {
                ZipInputStream subIn = openZipStream(in, filename);
                collectDataDirectoryPrefixes(result, prepend + filename
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
        try {
            File tempDirectory = DirectoryPreferences
                    .getMasterWorkingDirectory();

            File[] files = tempDirectory.listFiles();
            if (files == null) return;
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()
                        && files[i].getName().startsWith(TEMP_DIR_PREFIX)) {
                    File lockFile = new File(files[i],
                            DashboardInstanceStrategy.LOCK_FILE_NAME);
                    if (!lockFile.exists())
                        FileUtils.deleteDirectory(files[i], true);
                }
            }

        } catch (IOException ioe) {}
    }

}
