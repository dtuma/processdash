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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlSerializer;

import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.TempFileFactory;
import net.sourceforge.processdash.util.XMLUtils;

public class FileBundlePack {

    private File bundleDir;

    private FileBundleID packID;

    private File manifestFile;

    private String logPrefix;

    private static final Logger logger = Logger
            .getLogger(FileBundlePack.class.getName());


    public FileBundlePack(File bundleDir, String targetTimestampOrToken,
            String deviceID) {
        this.bundleDir = bundleDir;
        this.packID = new FileBundleID(getPackTimestamp(targetTimestampOrToken),
                deviceID, PACK_BUNDLE);
        this.manifestFile = FileBundleManifest.getFileForManifest(bundleDir,
            packID);
        this.logPrefix = FileBundleUtils.getLogPrefix(bundleDir);
    }


    public FileBundleID getPackID() {
        return packID;
    }

    public File getManifestFile() {
        return manifestFile;
    }


    public Map<FileBundleID, FileBundleManifest> packBundles()
            throws IOException {
        // identify the files the merge would create
        File targetZip = new File(bundleDir, packID.getToken() + ".zip");
        File targetManifest = manifestFile;

        // if repacking is disabled and the target manifest exists, abort
        if (REPACK == false && targetManifest.exists()) {
            logger.fine(logPrefix //
                    + "Pack manifest already present " + targetManifest);
            return null;
        }

        // find the lists of manifest and ZIP files to merge
        List<File> sourceManifests = listMatchingFiles(".xml");
        List<File> sourceZips = listMatchingFiles(".zip");

        // if we didn't find any files to merge, do nothing
        if (sourceManifests.isEmpty()) {
            logger.fine(logPrefix + "No bundles found to pack into " + packID);
            return null;
        }

        // if we found a single pack manifest, the data is already merged
        if (sourceManifests.size() == 1
                && isPackFilename(sourceManifests.get(0).getName())) {
            logger.fine(logPrefix + "No repack needed for "
                    + sourceManifests.get(0).getName());
            return null;
        }

        // merge the ZIP and manifest data
        logger.info(logPrefix + "Packing bundles into " + packID);
        Map<FileBundleID, FileBundleManifest> result = new HashMap();
        File tmpZip = writeMergedZip(sourceZips);
        File tmpXml = writeMergedManifest(sourceManifests, targetManifest, result);
        publishFile(tmpZip, targetZip);
        publishFile(tmpXml, targetManifest);

        // delete the manifest and ZIP files that were merged into the target
        for (File mf : sourceManifests) {
            if (!mf.equals(targetManifest))
                mf.delete();
        }
        for (File z : sourceZips) {
            if (!z.equals(targetZip))
                z.delete();
        }

        // return the set of merged bundle manifests
        logger.info(logPrefix + "Packed " + result.size() //
                + " bundles into " + packID);
        return result;
    }


    private List<File> listMatchingFiles(String suffix) throws IOException {
        // get the timestamp for the pack we are building
        String packTime = packID.getTimestamp();

        // list the files in the directory
        File[] allFiles = bundleDir.listFiles();
        if (allFiles == null)
            throw new FileNotFoundException(bundleDir.getPath());

        // iterate over the files we found
        List<File> result = new ArrayList<File>();
        for (File oneFile : allFiles) {
            String oneFilename = oneFile.getName();

            // only accept files from the same month as this pack
            if (!oneFilename.regionMatches(0, packTime, 0, TIMESTAMP_MATCH_LEN))
                continue;

            // only accept files with the desired suffix (.zip or .xml)
            if (!oneFilename.endsWith(suffix))
                continue;

            // only accept valid bundle-related files
            FileBundleID bundleID;
            try {
                bundleID = new FileBundleID(oneFilename);
            } catch (IllegalArgumentException iae) {
                continue;
            }

            // if repack is disabled, don't accept pack.xml / pack.zip files
            if (REPACK == false && isPack(bundleID))
                continue;

            // this file matches our criteria; add it to the list
            result.add(oneFile);
        }

        // sort the results in reverse chronological order and return them
        Collections.sort(result);
        Collections.reverse(result);
        return result;
    }


    private File writeMergedZip(List<File> sourceZipFiles) throws IOException {
        // open a file to write the merged ZIP
        File tmpZipFile = makeTempFile(".zip");
        OutputStream out = new FileOutputStream(tmpZipFile);

        try {
            ZipOutputStream destZip = new ZipOutputStream(
                    new BufferedOutputStream(out));

            // copy all of the bundle ZIP data to the destination
            Set<String> destEntryNames = new HashSet();
            for (File oneZipFile : sourceZipFiles) {
                copyBundleZipData(oneZipFile, destZip, destEntryNames);
            }

            // finish the ZIP file and close the output stream
            destZip.finish();
            destZip.close();

            // return the merged zip file we created
            return tmpZipFile;

        } catch (IOException ioe) {
            FileUtils.safelyClose(out);
            tmpZipFile.delete();
            throw ioe;
        }
    }

    private void copyBundleZipData(File srcFile, ZipOutputStream destZip,
            Set<String> destEntryNames) throws IOException {
        // open the source ZIP
        logger.finer(logPrefix + "Packing file " + srcFile.getName());
        ZipInputStream srcZip = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(srcFile)));

        // if this is a ZIP for a single bundle, prepend its bundle ID to the
        // name of each ZIP entry. If it's a pack, copy the entries verbatim
        FileBundleID bid = new FileBundleID(srcFile.getName());
        String prefix = isPack(bid) ? "" : bid.getToken() + "/";

        // iterate over the entries in the src zip
        ZipEntry srcEntry;
        while ((srcEntry = srcZip.getNextEntry()) != null) {
            // calculate the name of the entry in the dest ZIP. If the dest
            // ZIP already has an entry by that name, skip this file.
            String destName = prefix + srcEntry.getName();
            if (destEntryNames.contains(destName))
                continue;

            // compute a ZipEntry for this file for the dest ZIP
            ZipEntry destEntry = new ZipEntry(destName);
            if (srcEntry.getMethod() == ZipEntry.STORED) {
                destEntry.setMethod(ZipEntry.STORED);
                destEntry.setCrc(srcEntry.getCrc());
                destEntry.setSize(srcEntry.getSize());
                destEntry.setCompressedSize(srcEntry.getCompressedSize());
            }
            destEntry.setTime(srcEntry.getTime());

            // copy the data from source to dest
            destZip.putNextEntry(destEntry);
            FileUtils.copyFile(srcZip, destZip);
            destZip.closeEntry();
            destEntryNames.add(destName);
        }

        // close the source ZIP file
        FileUtils.safelyClose(srcZip);
    }


    private File writeMergedManifest(List<File> sourceManifestFiles,
            File destManifestFile,
            Map<FileBundleID, FileBundleManifest> manifests)
            throws IOException {
        // open a file to write the merged manifest
        File tmpManifestFile = makeTempFile(".xml");
        OutputStream out = new FileOutputStream(tmpManifestFile);

        try {
            // write the XML header and opening document tag
            XmlSerializer dest = XMLUtils.getXmlSerializer(true);
            dest.setOutput(out, ENCODING);
            dest.startDocument(ENCODING, null);
            dest.startTag(null, PACK_TAG);
            dest.attribute(null, ID_ATTR, packID.getToken());

            // copy all of the bundle manifest data to the destination
            for (File oneManifestFile : sourceManifestFiles) {
                copyBundleManifestData(oneManifestFile, destManifestFile, dest,
                    manifests);
            }

            // finish the XML document and close the output stream
            dest.endTag(null, PACK_TAG);
            dest.endDocument();
            out.close();

            // return the merged manifest file we created
            return tmpManifestFile;

        } catch (IOException ioe) {
            FileUtils.safelyClose(out);
            tmpManifestFile.delete();
            throw ioe;
        }
    }

    private void copyBundleManifestData(File srcFile, File destFile,
            XmlSerializer dest, Map<FileBundleID, FileBundleManifest> manifests)
            throws IOException {
        // parse the XML manifest document
        logger.finer(logPrefix + "Packing file " + srcFile.getName());
        Document doc = FileBundleManifest.parseXml(srcFile);

        // iterate over the <fileBundle> tags in the file
        NodeList nl = doc.getElementsByTagName(BUNDLE_TAG);
        for (int i = 0; i < nl.getLength(); i++) {
            Element mfXml = (Element) nl.item(i);
            FileBundleManifest mf = new FileBundleManifest(mfXml);
            FileBundleID bid = mf.getBundleID();

            // if we haven't already seen this bundle, write it to the output
            // and add it to our result set
            if (!manifests.containsKey(bid)) {
                copyXml(mfXml, dest);
                mf.manifestFile = destFile;
                mf.pack = packID;
                manifests.put(bid, mf);
            }
        }
    }

    private void copyXml(Element src, XmlSerializer dest) throws IOException {
        dest.startTag(null, src.getTagName());
        Map<String, String> attrs = XMLUtils.getAttributesAsMap(src);
        for (Entry<String, String> e : attrs.entrySet())
            dest.attribute(null, e.getKey(), e.getValue());
        for (Element child : XMLUtils.getChildElements(src))
            copyXml(child, dest);
        dest.endTag(null, src.getTagName());
    }


    private File makeTempFile(String suffix) throws IOException {
        File f = TempFileFactory.get().createTempFile("FileBundlePack", suffix);
        f.deleteOnExit();
        return f;
    }

    private void publishFile(File tmpFile, File destFile) throws IOException {
        OutputStream dest = FileBundleUtils.outputStream(destFile);
        FileUtils.copyFile(tmpFile, dest);
        dest.close();
        tmpFile.delete();
    }


    public static String getPackTimestamp(FileBundleID bundleID) {
        return getPackTimestamp(bundleID.getTimestamp());
    }

    public static String getPackTimestamp(String timestampOrToken) {
        if (timestampOrToken.length() < TIMESTAMP_MATCH_LEN)
            throw new IllegalArgumentException(
                    "Not enough timestamp digits: " + timestampOrToken);

        return timestampOrToken.substring(0, TIMESTAMP_MATCH_LEN) + "01-000000";
    }

    public static boolean isPack(FileBundleID bundleID) {
        return PACK_BUNDLE.equals(bundleID.getBundleName());
    }

    public static boolean isPackFilename(String filename) {
        try {
            return isPack(new FileBundleID(filename));
        } catch (IllegalArgumentException iae) {
            return false;
        }
    }


    private static final boolean REPACK = true;

    private static final String PACK_BUNDLE = FileBundleConstants.PACK_BUNDLE;

    private static final int TIMESTAMP_MATCH_LEN = 6;

    private static final String ENCODING = "UTF-8";

    private static final String PACK_TAG = "fileBundlePack";

    private static final String BUNDLE_TAG = FileBundleManifest.DOCUMENT_TAG;

    private static final String ID_ATTR = FileBundleManifest.ID_ATTR;

}
