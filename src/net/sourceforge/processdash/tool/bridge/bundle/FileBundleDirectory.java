// Copyright (C) 2021-2023 Tuma Solutions, LLC
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
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

public class FileBundleDirectory
        implements FileBundleManifestSource, FileBundleValidator {

    private File bundleDir;

    private String deviceID;

    private FileBundleTimeFormat timeFormat;

    private Map<FileBundleID, FileBundleManifest> manifestCache;

    private Map<FileBundleID, FileBundleManifest> replacementCache;

    private Map<File, Long> packManifestFileSizes;


    public FileBundleDirectory(File bundleDir) throws IOException {
        this.bundleDir = bundleDir;
        this.deviceID = DeviceID.get();
        this.timeFormat = new FileBundleTimeFormat(getDirTimeZone());
        this.manifestCache = Collections.synchronizedMap(new HashMap());
        this.replacementCache = Collections.synchronizedMap(new ManifestCache());
        this.packManifestFileSizes = Collections.synchronizedMap(new HashMap());
    }

    private String getDirTimeZone() throws IOException {
        // abort if the directory doesn't exist
        if (!bundleDir.isDirectory())
            throw new FileNotFoundException(bundleDir.getPath());

        // identify the file in the directory that contains the time zone ID
        File timezoneFile = new File(bundleDir, "timezone.txt");

        // if the file exists, read the time zone ID from it. If the file cannot
        // be read, fail with the IOException that was encountered.
        if (timezoneFile.exists()) {
            FileInputStream in = new FileInputStream(timezoneFile);
            return new String(FileUtils.slurpContents(in, true), "UTF-8").trim();
        }

        // if the file does not exist, initialize it with a default timezone
        String timezone = TimeZone.getDefault().getID();
        Writer out = new OutputStreamWriter(
                FileBundleUtils.outputStream(timezoneFile), "UTF-8");
        out.write(timezone);
        out.close();

        // return the timezone we assigned
        return timezone;
    }



    /**
     * Return the time format used by this directory.
     */
    public FileBundleTimeFormat getTimeFormat() {
        return timeFormat;
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
            bundleSpec.filenames, bundleSpec.parents, bundleSpec.replaces,
            bundleSpec.timestamp);
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
     * @param replaces
     *            the {@link FileBundleID}s of bundles replaced by this bundle
     * @param timestamp
     *            the timestamp to use for this bundle, or -1 for the current
     *            time
     * @return the FileBundleID assigned to the new bundle
     * @throws IOException
     *             if I/O errors are encountered
     */
    public FileBundleID storeBundle(String bundleName,
            ReadableResourceCollection source, List<String> filenames,
            List<FileBundleID> parents, List<FileBundleID> replaces,
            long timestamp) throws IOException {
        // generate an ID for the new bundle
        FileBundleID bundleID = createNewBundleID(timestamp, bundleName,
            parents, replaces);

        // write a ZIP file holding the data for the new bundle
        ResourceListing fileInfo = writeFilesToZip(bundleID, source, filenames);

        // write a manifest for the bundle
        FileBundleManifest manifest = new FileBundleManifest(bundleID, fileInfo,
                parents, replaces);
        manifest.write(bundleDir);

        // add the manifest to our in-memory cache
        manifestCache.put(bundleID, manifest);

        // return the ID of the newly created bundle
        return bundleID;
    }

    private FileBundleID createNewBundleID(long timestamp, String bundleName,
            List<FileBundleID>... mustComeAfterBundles) {
        // if no timestamp was supplied, use the current time
        boolean implicitTimestamp = timestamp <= 0;
        if (implicitTimestamp)
            timestamp = getCurrentTimeOrAfter(mustComeAfterBundles);

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

    private long getCurrentTimeOrAfter(List<FileBundleID>... bundles) {
        // get the current time
        long time = System.currentTimeMillis();

        // make sure the time falls after the timestamps of any parents.
        // this guards against overlap during daylight savings "fall-back"
        String timeStr = timeFormat.format(time);
        for (List<FileBundleID> oneBundleList : bundles) {
            for (FileBundleID oneBundle : oneBundleList) {
                String bundleTimestamp = oneBundle.getTimestamp();
                if (timeStr.compareTo(bundleTimestamp) <= 0) {
                    try {
                        Date bundleTime = timeFormat.parse(bundleTimestamp);
                        time = bundleTime.getTime() + 10000;
                        timeStr = timeFormat.format(time);
                    } catch (Exception e) {
                    }
                }
            }
        }

        return time;
    }

    private boolean bundleExists(FileBundleID bundleID) {
        // if our manifest cache contains this bundle ID, we know it exists
        if (manifestCache.containsKey(bundleID))
            return true;

        // return true if we can load a manifest file for this bundle
        try {
            getManifest(bundleID);
            return true;
        } catch (IOException ioe) {
            return false;
        }
    }

    private ResourceListing writeFilesToZip(FileBundleID bundleID,
            ReadableResourceCollection source, List<String> filenames)
            throws IOException {
        // if there are no files to write, abort without creating a ZIP file
        ResourceListing fileInfo = new ResourceListing();
        if (filenames.isEmpty())
            return fileInfo;

        // open an output stream to write ZIP data
        File zipFile = getZipFileForBundleID(bundleID);
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
        FileBundleManifest manifest = getManifest(bundleID);
        ResourceCollectionInfo fileInfo = manifest.getFiles();
        if (filesToExtract != null)
            fileInfo = new ResourceListing(fileInfo, filesToExtract);

        // if there are no files to be extracted, abort
        int fileExtractCount = fileInfo.listResourceNames().size();
        if (fileExtractCount == 0)
            return fileInfo;

        // open the ZIP file for reading
        ZipSource zipSource = getZipSourceForBundle(manifest);
        ZipInputStream zipIn = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(zipSource.file)));
        ZipEntry e;

        // scan the contents of the ZIP and extract files
        while ((e = zipIn.getNextEntry()) != null) {
            // skip files in a pack ZIP that don't belong to this bundle
            String filename = e.getName();
            if (filename.startsWith(zipSource.prefix))
                filename = filename.substring(zipSource.prefix.length());
            else
                continue;

            // if a file in the ZIP was not mentioned in the manifest, or was
            // not included in the list of files to extract, ignore it.
            long lastMod = fileInfo.getLastModified(filename);
            if (lastMod <= 0)
                continue;

            // copy the file to the target directory
            OutputStream out = target.getOutputStream(filename, lastMod);
            FileUtils.copyFile(zipIn, out);
            out.close();

            // stop when we've extracted all the files we were looking for
            if (--fileExtractCount == 0)
                break;
        }

        // close the ZIP file and return the file info
        zipIn.close();
        return fileInfo;
    }

    private ZipSource getZipSourceForBundle(FileBundleManifest mf)
            throws FileBundleFileNotFoundException {
        if (mf.pack == null)
            return new ZipSource(getZipFileForBundleID(mf.getBundleID()), "");
        else
            return new ZipSource(getZipFileForBundleID(mf.pack),
                    mf.getBundleID().getToken() + "/");
    }

    private File getZipFileForBundleID(FileBundleID bundleID) {
        return new File(bundleDir, bundleID.getToken() + ".zip");
    }



    /**
     * Permanently delete a set of bundles from the directory.
     * 
     * This method will be a no-op for bundles that do not exist, and for
     * bundles that are part of a pack.
     * 
     * @param bundleIDs
     *            the set of bundles to delete
     */
    public void deleteBundles(Set<FileBundleID> bundleIDs) {
        for (FileBundleID bid : bundleIDs) {
            File mf = FileBundleManifest.getFileForManifest(bundleDir, bid);
            mf.delete();
            File zf = getZipFileForBundleID(bid);
            zf.delete();
            manifestCache.remove(bid);
        }
    }



    public FileBundleManifest getManifest(FileBundleID bundleID)
            throws IOException {
        // check to see if we have a cached manifest
        FileBundleManifest result = manifestCache.get(bundleID);

        // if the cached manifest's XML file no longer exists, discard it.
        // (It might have been moved into a pack by another process.)
        if (result != null && !result.isManifestFilePresent()) {
            manifestCache.remove(bundleID);
            result = null;
        }

        if (result == null) {
            try {
                // try reading the manifest from its canonical XML file
                result = new FileBundleManifest(bundleDir, bundleID);
                manifestCache.put(bundleID, result);

            } catch (FileBundleManifest.Missing m) {
                // if this bundle doesn't have a plain manifest file, look for
                // it in a pack. If not found, throw original error
                result = findManifestInPack(bundleID);
                if (result == null)
                    throw m;
            }
        }

        // remember when the manifest was accessed and return it
        result.accessTime = System.currentTimeMillis();
        return result;
    }

    private FileBundleManifest findManifestInPack(FileBundleID bundleID) {
        // get the timestamp for the pack that would contain this bundle
        String packTimestamp = FileBundlePack.getPackTimestamp(bundleID);

        // search the bundle directory for packs with this timestamp
        for (File file : bundleDir.listFiles()) {
            if (!FileBundlePack.isPackManifestFilename(file.getName(),
                packTimestamp))
                continue;

            // if we've successfully loaded data from this pack manifest, we do
            // not need to check it again
            Long lastLength = packManifestFileSizes.get(file);
            if (lastLength != null && lastLength == file.length())
                continue;

            try {
                // load the bundle data from the pack manifest
                FileBundlePack pack = new FileBundlePack(file);
                Map<FileBundleID, FileBundleManifest> manifests = pack
                        .readBundleManifests();
                addPackManifestsToCache(pack.getManifestFile(), manifests);

                // if this pack contains the bundle we're looking for, return
                // the bundle's manifest
                FileBundleManifest result = manifests.get(bundleID);
                if (result != null)
                    return result;

            } catch (Exception e) {
                // cannot read the pack manifest. (Error already logged)
            }
        }

        // the bundle was not found inside any pack. return null
        return null;
    }

    private void addPackManifestsToCache(File packManifestFile,
            Map<FileBundleID, FileBundleManifest> packManifests) {
        packManifestFileSizes.put(packManifestFile, packManifestFile.length());
        manifestCache.putAll(packManifests);
    }


    public FileBundleManifest getManifestReplacing(FileBundleID bundleID)
            throws IOException {
        // if the given manifest file actually exists, it wasn't replaced
        try {
            getManifest(bundleID);
            return null;
        } catch (IOException ioe) {
        }

        // look for a cached result, return if found
        FileBundleManifest result = replacementCache.get(bundleID);
        if (result != null) {
            result.accessTime = System.currentTimeMillis();
            return result;
        }

        // find all the bundle in the directory with the same bundle name and
        // device ID that come after the given bundle
        List<FileBundleID> bundles = listBundles(bundleID.getBundleName(),
            bundleID.getDeviceID(), bundleID.getTimestamp());

        // if no subsequent matching bundles were found, abort
        if (bundles.isEmpty())
            return null;

        // load the manifest for the potentially replacing bundle. If it isn't
        // a replacement for the given bundle, abort
        FileBundleID potentiallyReplacingBundle = bundles.get(0);
        result = getManifest(potentiallyReplacingBundle);
        if (!result.isReplacementFor(bundleID))
            return null;

        // cache the result we found and return it.
        replacementCache.put(bundleID, result);
        result.accessTime = System.currentTimeMillis();
        return result;
    }


    /**
     * Test whether the manifest and ZIP files for a bundle are present.
     */
    @Override
    public boolean isBundleValid(FileBundleID bundleID) {
        try {
            getBundleCollection(bundleID);
            return true;
        } catch (IOException ioe) {
            return false;
        }
    }


    /**
     * Retrieve an object that can read file data from a bundle without
     * extracting it.
     * 
     * @param bundleID
     *            the ID of the bundle to open
     * @return a {@link FileBundleCollection} for reading files from the
     *         compressed bundle
     * @throws IOException
     *             if the bundle does not exist or could not be read
     */
    public FileBundleCollection getBundleCollection(FileBundleID bundleID)
            throws IOException {
        FileBundleManifest manifest = getManifest(bundleID);
        ZipSource zip = getZipSourceForBundle(manifest);
        return new FileBundleCollection(manifest, zip.file, zip.prefix);
    }


    /**
     * Return a list of bundles in this directory
     * 
     * @param bundleName
     *            only return bundles with this name. Can be null to return
     *            bundles with any name
     * @param deviceID
     *            only return bundles with this device ID. Can be null to return
     *            bundles written by any device
     * @param after
     *            only return bundles whose timestamp is after this time. Can be
     *            0 to return all bundles
     * @return a list of bundles IDs for bundles in this directory meeting the
     *         given criteria, sorted in chronological order
     */
    public List<FileBundleID> listBundles(String bundleName, String deviceID,
            long after) throws IOException {
        return listBundles(bundleName, deviceID, timeFormat.format(after));
    }


    /**
     * Return a list of bundles in this directory
     * 
     * @param bundleName
     *            only return bundles with this name. Can be null to return
     *            bundles with any name
     * @param deviceID
     *            only return bundles with this device ID. Can be null to return
     *            bundles written by any device
     * @param afterTimestamp
     *            only return bundles whose timestamp is after this timestamp
     * @return a list of bundles IDs for bundles in this directory meeting the
     *         given criteria, sorted in chronological order
     */
    public List<FileBundleID> listBundles(String bundleName, String deviceID,
            String afterTimestamp) throws IOException {
        // get timestamp of the first pack that might contain a matching bundle
        String packTimeCutoff = FileBundlePack.getPackTimestamp(afterTimestamp);
        List<FileBundleID> result = new ArrayList<FileBundleID>();

        // iterate over the files in the bundle directory
        String[] allFiles = bundleDir.list();
        if (allFiles == null)
            throw new FileNotFoundException(bundleDir.getPath());
        for (String filename : allFiles) {
            // only examine manifest XML files
            if (!filename.endsWith(".xml"))
                continue;

            try {
                // get the bundleID for this filename. If parsing fails, skip it
                FileBundleID bid = new FileBundleID(filename);

                if (FileBundlePack.isPack(bid)) {
                    // if this file is a pack manifest, make sure its timestamp
                    // isn't before the pack time cutoff
                    if (bid.getTimestamp().compareTo(packTimeCutoff) < 0)
                        continue;

                    // read the bundles from the pack manifest
                    FileBundlePack pack = new FileBundlePack(
                            new File(bundleDir, filename));
                    Map<FileBundleID, FileBundleManifest> manifests = pack
                            .readBundleManifests();

                    // add bundle IDs from the pack that match our search
                    for (FileBundleID innerBid : manifests.keySet()) {
                        if (bundleIDMatchesFilter(innerBid, bundleName,
                            deviceID, afterTimestamp))
                            result.add(innerBid);
                    }

                } else {
                    // if this file is a regular bundle manifest, add it to
                    // our result if it matches our search filter
                    if (bundleIDMatchesFilter(bid, bundleName, deviceID,
                        afterTimestamp)) {
                        result.add(bid);
                    }
                }

            } catch (IllegalArgumentException iae) {
                // this file is not bundle-related; skip it
            }
        }

        // sort the results and return them
        Collections.sort(result, FileBundleID.CHRONOLOGICAL_ORDER);
        return result;
    }

    private boolean bundleIDMatchesFilter(FileBundleID bundleID,
            String bundleName, String deviceID, String afterTimestamp) {
        // return false if the bundle name does not match
        if (bundleName != null && !bundleName.equals(bundleID.getBundleName()))
            return false;

        // return false if the device ID does not match
        if (deviceID != null && !deviceID.equals(bundleID.getDeviceID()))
            return false;

        // return true if the timestamp falls after the cutoff
        return bundleID.getTimestamp().compareTo(afterTimestamp) > 0;
    }


    /**
     * Scan the directory for bundles that precede the timestamp. If any are
     * found, organize them into monthly pack files.
     * 
     * @param timestamp
     *            the cutoff time for pack operations. If a bundle older than
     *            this time is seen, the entire month containing that bundle
     *            will be packed (even if the month ends after this timestamp)
     * @throws IOException
     *             if the pack operation encountered an error
     */
    public void packBundlesThroughMonth(long timestamp) throws IOException {
        // identify the cutoff timestamp for bundles that should be packaged
        String cutoffTime = timeFormat.format(timestamp);

        // list the files in the directory
        String[] files = bundleDir.list();
        if (files == null)
            throw new FileNotFoundException(bundleDir.getPath());

        // look for bundles & packs in the directory that precede the timestamp
        Set<String> packTokens = new TreeSet<String>();
        for (String file : files) {
            if (file.endsWith(".xml") && file.compareTo(cutoffTime) < 0) {
                try {
                    FileBundleID bundleID = new FileBundleID(file);
                    String pt = FileBundlePack.getPackTimestamp(bundleID);
                    packTokens.add(pt);
                } catch (IllegalArgumentException iae) {
                    // this XML file is apparently not bundle-related
                }
            }
        }

        // package the bundles for the months we found
        for (String token : packTokens) {
            packBundlesForMonth(token);
        }
    }


    /**
     * Gather up all the bundles from a particular month, and put them in a
     * single pack file
     * 
     * @param targetTimestampOrToken
     *            a FileBundleID token or timestamp belonging to the month that
     *            should be packed
     * @return the FileBundleID of the pack that was created, or null if no pack
     *         operation was needed
     * @throws IOException
     *             if the pack operation encountered an error
     */
    public FileBundleID packBundlesForMonth(String targetTimestampOrToken)
            throws IOException {
        // create a pack utility object
        FileBundlePack pack = new FileBundlePack(bundleDir,
                targetTimestampOrToken, deviceID);

        // ask the object to perform a merge
        Map<FileBundleID, FileBundleManifest> pmf = pack.packBundles();
        if (pmf == null)
            return null;

        // record all of the manifests that were merged
        addPackManifestsToCache(pack.getManifestFile(), pmf);

        // return the bundle ID of the merged pack
        return pack.getPackID();
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

    private class ZipSource {

        File file;

        String prefix;

        public ZipSource(File zipFile, String prefix)
                throws FileBundleFileNotFoundException {
            if (!zipFile.isFile())
                throw new FileBundleFileNotFoundException(zipFile);

            this.file = zipFile;
            this.prefix = prefix;
        }
    }

}
