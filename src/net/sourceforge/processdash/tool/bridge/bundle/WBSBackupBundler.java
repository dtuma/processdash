// Copyright (C) 2023 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge.bundle;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Adler32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import net.sourceforge.processdash.tool.bridge.ResourceListing;
import net.sourceforge.processdash.tool.bridge.report.ResourceCollectionDiff;
import net.sourceforge.processdash.util.FileUtils;

public class WBSBackupBundler {

    public void run(File wbsDir) throws IOException {
        File backupDir = new File(wbsDir, "backup");
        File bundleDir = new File(wbsDir, FileBundleConstants.BUNDLE_SUBDIR);
        run(backupDir, bundleDir, true);
    }


    public void run(File backupSrcDir, File bundleDestDir,
            boolean deleteFromSrc) throws IOException {
        // if the directories do not exist, abort
        if (!backupSrcDir.isDirectory() || !bundleDestDir.isDirectory())
            return;

        // list the files in the backup source directory
        File[] srcFiles = backupSrcDir.listFiles();
        if (srcFiles == null)
            return;

        // iterate over the backup files in chronological order
        Arrays.sort(srcFiles);
        ResourceListing lastZip = new ResourceListing();
        for (File oneFile : srcFiles) {
            // see if this filename matches one of the naming conventions used
            // for WBS backups. If not, skip the file
            String filename = oneFile.getName();
            Matcher m = SAVED_WBS_FILENAME_PAT.matcher(filename);
            if (!m.matches()) {
                m = STARTUP_WBS_FILENAME_PAT.matcher(filename);
                if (!m.matches())
                    continue;
            }

            // copy the backup into the bundle directory
            try {
                lastZip = copyWbsBackupToBundle(oneFile, m.group(1), m.group(2),
                    bundleDestDir, lastZip);

                // delete the original backup file if requested
                if (deleteFromSrc)
                    oneFile.delete();

            } catch (IOException ioe) {
                // if this ZIP file is corrupt, skip it
            }
        }
    }

    private ResourceListing copyWbsBackupToBundle(File wbsZip,
            String wbsTimestamp, String wbsSavedBy, File bundleDir,
            ResourceListing previousZipContents) throws IOException {
        // calculate the resource info for this ZIP file
        ResourceListing zipContents = getZipContentListing(wbsZip);

        // if this ZIP is identical to the previous one (common with "startup"
        // ZIP files), we don't need to copy it.
        ResourceCollectionDiff diff = new ResourceCollectionDiff(zipContents,
                previousZipContents);
        if (diff.noDifferencesFound())
            return zipContents;

        // compute a bundle ID to use for this WBS ZIP file
        String bundleTimestamp = wbsTimestamp.substring(0, 8) // yyyymmdd
                + "-" + wbsTimestamp.substring(8); // hhmmss
        String devicePseudoID = DeviceID.createPseudoID(wbsSavedBy);
        FileBundleID bundleID = new FileBundleID(bundleTimestamp,
                devicePseudoID, "wbs");

        // copy the ZIP to a bundle in the dest directory
        File destZipFile = new File(bundleDir, bundleID.getToken() + ".zip");
        OutputStream out = FileBundleUtils.outputStream(destZipFile);
        FileUtils.copyFile(wbsZip, out);
        out.close();

        // write the manifest file for this bundle
        FileBundleManifest mf = new FileBundleManifest(bundleID, zipContents,
                Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        File mfXml = FileBundleManifest.getFileForManifest(bundleDir, bundleID);
        mf.writeToFile(mfXml);

        // return the listing of the contents of this ZIP
        return zipContents;
    }

    private ResourceListing getZipContentListing(File zipFile)
            throws IOException {
        InputStream in = null;
        try {
            // open a zip stream for the given file
            in = new FileInputStream(zipFile);
            ZipInputStream zipIn = new ZipInputStream(
                    new BufferedInputStream(in));

            // iterate over the ZIP contents and gather file info
            ResourceListing result = new ResourceListing();
            ZipEntry e;
            while ((e = zipIn.getNextEntry()) != null) {
                String filename = e.getName();
                long modTime = e.getTime();
                long checksum = FileUtils.computeChecksum(zipIn, new Adler32(),
                    false);
                result.addResource(filename, modTime, checksum);
            }

            // return the info we collected
            return result;

        } finally {
            FileUtils.safelyClose(in);
        }
    }


    private static final Pattern SAVED_WBS_FILENAME_PAT = Pattern.compile(
        "backup-(\\d{14})-saved_by_(.+)\\.zip", Pattern.CASE_INSENSITIVE);

    private static final Pattern STARTUP_WBS_FILENAME_PAT = Pattern.compile(
        "backup-(\\d{14})-(startup)\\.zip", Pattern.CASE_INSENSITIVE);

}
