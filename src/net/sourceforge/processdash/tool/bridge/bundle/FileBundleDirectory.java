// Copyright (C) 2021-2022 Tuma Solutions, LLC
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
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import net.sourceforge.processdash.tool.bridge.ReadableResourceCollection;
import net.sourceforge.processdash.tool.bridge.ResourceCollection;
import net.sourceforge.processdash.tool.bridge.ResourceCollectionInfo;
import net.sourceforge.processdash.tool.bridge.ResourceListing;
import net.sourceforge.processdash.util.DateUtils;
import net.sourceforge.processdash.util.FileUtils;

public class FileBundleDirectory implements FileBundleManifestSource {

    private File bundleDir;

    private String deviceID;

    private FileBundleTimeFormat timeFormat;

    private ManifestCache manifestCache;


    public FileBundleDirectory(File bundleDir) throws IOException {
        this.bundleDir = bundleDir;
        this.deviceID = DeviceID.get();
        this.timeFormat = new FileBundleTimeFormat(getDirTimeZone());
        this.manifestCache = new ManifestCache();
    }

    private String getDirTimeZone() throws IOException {
        // identify the file in the directory that contains the time zone ID
        File timezoneFile = new File(bundleDir, "timezone.txt");

        // try reading the time zone ID from the file
        String timezone = null;
        try {
            timezone = new String(FileUtils.slurpContents( //
                new FileInputStream(timezoneFile), true), "UTF-8").trim();
        } catch (IOException ioe) {
        }

        // if no time zone could be read, initialize it and write the file
        if (timezone == null) {
            timezone = TimeZone.getDefault().getID();
            Writer out = new OutputStreamWriter(
                    FileBundleUtils.outputStream(timezoneFile), "UTF-8");
            out.write(timezone);
            out.close();
        }

        return timezone;
    }



    /**
     * Return a bundle ID containing the current time and device ID.
     */
    public FileBundleID getCurrentTimeBundleID() {
        return createNewBundleID(-1, "currentTime");
    }



    /**
     * Create a new bundle based on the instructions in a
     * {@link FileBundleSpec}.
     * 
     * @param bundleSpec
     *            the spec describing the contents of the bundle
     * @return the FileBundleID assigned to the newly created bundle
     * @throws IOException
     *             if I/O errors are encountered
     */
    public FileBundleID storeBundle(FileBundleSpec bundleSpec)
            throws IOException {
        return storeBundle(bundleSpec.bundleName, bundleSpec.source,
            bundleSpec.filenames, bundleSpec.parents, bundleSpec.timestamp);
    }


    /**
     * Create a new bundle by gathering up files from a given directory.
     * 
     * @param bundleName
     *            the name of the bundle
     * @param source
     *            the resource collection where source files are located
     * @param filenames
     *            the names of the file in <tt>srcDir</tt> that should be placed
     *            in the bundle
     * @param parents
     *            the {@link FileBundleID}s of parent bundles
     * @param timestamp
     *            the timestamp to use for this bundle, or -1 for the current
     *            time
     * @return the FileBundleID assigned to the new bundle
     * @throws IOException
     *             if I/O errors are encountered
     */
    public FileBundleID storeBundle(String bundleName,
            ReadableResourceCollection source, List<String> filenames,
            List<FileBundleID> parents, long timestamp) throws IOException {
        // generate an ID for the new bundle
        FileBundleID bundleID = createNewBundleID(timestamp, bundleName);

        // write a ZIP file holding the data for the new bundle
        ResourceListing fileInfo = writeFilesToZip(bundleID, source, filenames);

        // write a manifest for the bundle
        FileBundleManifest manifest = new FileBundleManifest(bundleID, fileInfo,
                parents);
        manifest.write(bundleDir);

        // add the manifest to our in-memory cache
        manifestCache.put(bundleID, manifest);

        // return the ID of the newly created bundle
        return bundleID;
    }

    private FileBundleID createNewBundleID(long timestamp, String bundleName) {
        // if no timestamp was supplied, use the current time
        boolean implicitTimestamp = timestamp <= 0;
        if (implicitTimestamp)
            timestamp = System.currentTimeMillis();

        // create a bundle ID with the appropriate information
        FileBundleID bid = new FileBundleID(timestamp, timeFormat, deviceID,
                bundleName);

        // if a bundle with this ID already exists, tweak the timestamp to
        // deconflict
        while (implicitTimestamp && bundleExists(bid)) {
            timestamp += 1000;
            bid = new FileBundleID(timestamp, timeFormat, deviceID, bundleName);
        }

        // return the bundle ID we created
        return bid;
    }

    private boolean bundleExists(FileBundleID bundleID) {
        // if our manifest cache contains this bundle ID, we know it exists
        if (manifestCache.containsKey(bundleID))
            return true;

        // see if the bundle directory contains a manifest file for this bundle
        File mf = FileBundleManifest.getFileForManifest(bundleDir, bundleID);
        return mf.isFile();
    }

    private ResourceListing writeFilesToZip(FileBundleID bundleID,
            ReadableResourceCollection source, List<String> filenames)
            throws IOException {
        // if there are no files to write, abort without creating a ZIP file
        ResourceListing fileInfo = new ResourceListing();
        if (filenames.isEmpty())
            return fileInfo;

        // open an output stream to write ZIP data
        File zipFile = getZipFileForBundle(bundleID);
        ZipOutputStream zipOut = new ZipOutputStream(
                FileBundleUtils.outputStream(zipFile));

        // write each of the files into the ZIP
        for (String filename : filenames) {
            // retrieve the file modification time and checksum. Skip if missing
            long modTime = source.getLastModified(filename);
            Long cksum = source.getChecksum(filename);
            if (modTime == 0 || cksum == null)
                continue;

            // add a new entry to the ZIP file
            ZipEntry e = new ZipEntry(filename);
            if (isCompressedFile(filename)) {
                e.setMethod(ZipEntry.STORED);
                long[] checkData = FileUtils.computeChecksumAndSize(
                    source.getInputStream(filename), new CRC32(), true);
                e.setCrc(checkData[0]);
                e.setSize(checkData[1]);
                e.setCompressedSize(checkData[1]);
            }
            e.setTime(modTime);
            zipOut.putNextEntry(e);

            // copy the file into the ZIP
            InputStream in = source.getInputStream(filename);
            FileUtils.copyFile(in, zipOut);
            in.close();
            zipOut.closeEntry();

            // add the file to our resource listing
            fileInfo.addResource(filename, modTime, cksum);
        }

        // close the ZIP file
        zipOut.close();
        return fileInfo;
    }

    private boolean isCompressedFile(String filename) {
        return filename.endsWith(".pdash") || filename.endsWith(".zip");
    }



    /**
     * Extract files from a bundle into a target directory
     * 
     * @param bundleID
     *            the ID of the bundle to extract
     * @param target
     *            the resource collection where the files should be extracted
     * @return the list of resources in the extracted bundle
     * @throws IOException
     *             if any problems are encountered during the extraction
     */
    public ResourceCollectionInfo extractBundle(FileBundleID bundleID,
            ResourceCollection target) throws IOException {
        return extractBundle(bundleID, target, null);
    }


    /**
     * Extract files from a bundle into a target directory
     * 
     * @param bundleID
     *            the ID of the bundle to extract
     * @param target
     *            the resource collection where the files should be extracted
     * @param filesToExtract
     *            the list of files to be extracted, or null to extract all
     *            files in the bundle
     * @return the list of files that were extracted
     * @throws IOException
     *             if any problems are encountered during the extraction
     */
    public ResourceCollectionInfo extractBundle(FileBundleID bundleID,
            ResourceCollection target, List<String> filesToExtract)
            throws IOException {
        // retrieve details about the bundle files we are extracting
        ResourceCollectionInfo fileInfo = getManifest(bundleID).getFiles();
        if (filesToExtract != null)
            fileInfo = new ResourceListing(fileInfo, filesToExtract);

        // if there are no files to be extracted, abort
        if (fileInfo.listResourceNames().isEmpty())
            return fileInfo;

        // open the ZIP file for reading
        File zipFile = getZipFileForBundle(bundleID);
        ZipInputStream zipIn = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(zipFile)));
        ZipEntry e;

        // scan the contents of the ZIP and extract files
        while ((e = zipIn.getNextEntry()) != null) {
            // if a file in the ZIP was not mentioned in the manifest, or was
            // not included in the list of files to extract, ignore it.
            String filename = e.getName();
            long lastMod = fileInfo.getLastModified(filename);
            if (lastMod <= 0)
                continue;

            // copy the file to the target directory
            OutputStream out = target.getOutputStream(filename, lastMod);
            FileUtils.copyFile(zipIn, out);
            out.close();
        }

        // close the ZIP file and return the file info
        zipIn.close();
        return fileInfo;
    }

    private File getZipFileForBundle(FileBundleID bundleID) {
        return new File(bundleDir, bundleID.getToken() + ".zip");
    }



    /**
     * Permanently delete a set of bundles from the directory.
     * 
     * @param bundleIDs
     *            the set of bundles to delete
     */
    public void deleteBundles(Set<FileBundleID> bundleIDs) {
        for (FileBundleID bid : bundleIDs) {
            File mf = FileBundleManifest.getFileForManifest(bundleDir, bid);
            mf.delete();
            File zf = getZipFileForBundle(bid);
            zf.delete();
            manifestCache.remove(bid);
        }
    }



    public FileBundleManifest getManifest(FileBundleID bundleID)
            throws IOException {
        FileBundleManifest result = manifestCache.get(bundleID);
        if (result == null) {
            result = new FileBundleManifest(bundleDir, bundleID);
            manifestCache.put(bundleID, result);
        }
        result.accessTime = System.currentTimeMillis();
        return result;
    }


    private static class ManifestCache
            extends LinkedHashMap<FileBundleID, FileBundleManifest> {

        ManifestCache() {
            super(100, 0.75f, true);
        }

        @Override
        protected boolean removeEldestEntry(
                java.util.Map.Entry<FileBundleID, FileBundleManifest> eldest) {
            // don't bother cleaning up if the cache is small
            if (size() < 100)
                return false;

            // discard the oldest entry if it hasn't been accessed in a while
            long cutoff = System.currentTimeMillis() - 5 * DateUtils.MINUTES;
            return eldest.getValue().accessTime < cutoff;
        }
    }

}
