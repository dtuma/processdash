// Copyright (C) 2021-2025 Tuma Solutions, LLC
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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlSerializer;

import net.sourceforge.processdash.tool.bridge.ResourceCollectionInfo;
import net.sourceforge.processdash.tool.bridge.report.XmlCollectionListing;
import net.sourceforge.processdash.util.XMLUtils;

public class FileBundleManifest {

    public static class Missing extends FileBundleFileNotFoundException {

        private FileBundleID bundleID;

        private Missing(File f, FileBundleID bundleID) {
            super(f);
            this.bundleID = bundleID;
        }

        public FileBundleID getBundleID() {
            return bundleID;
        }

    }

    private FileBundleID bundleID;

    private ResourceCollectionInfo files;

    private List<String> metadata;

    private List<FileBundleID> parents;

    private List<FileBundleID> replaces;

    File manifestFile;

    long accessTime;

    FileBundleID pack;


    public FileBundleID getBundleID() {
        return bundleID;
    }

    public ResourceCollectionInfo getFiles() {
        return files;
    }

    public List<String> getMetadata() {
        return metadata;
    }

    public List<FileBundleID> getParents() {
        return parents;
    }

    public List<FileBundleID> getReplaces() {
        return replaces;
    }

    public boolean isManifestFilePresent() {
        return manifestFile != null && manifestFile.isFile();
    }


    /**
     * Returns true if this manifest specifies a long-lived bundle that replaced
     * other temporary bundles, including the bundle with targetID.
     * 
     * The target bundle is considered replaced if:
     * <ul>
     * <li>This bundle's name and device ID match the target bundle; and</li>
     * <li>This bundle specifies a replaced bundle; and</li>
     * <li>replacedBundleTimestamp &lt;= targetBundleTimestamp &lt;
     * thisBundleTimestamp</li>
     * </ul>
     * 
     * @param targetID
     *            a bundle that might have been replaced
     * @return true if the target bundle was replaced by this one; false if they
     *         are equal or if this is not a replacement
     */
    public boolean isReplacementFor(FileBundleID targetID) {
        // if this bundle didn't replace any other bundles, return false
        if (replaces == null || replaces.isEmpty())
            return false;

        // if this bundle's name and device ID do not match the target bundle,
        // this can't be a replacement.
        if (!targetID.getDeviceID().equals(bundleID.getDeviceID())
                || !targetID.getBundleName().equals(bundleID.getBundleName()))
            return false;

        // if this bundle was finalized at the same time or before the target
        // bundle, it can't be a replacement for it
        String targetTimestamp = targetID.getTimestamp();
        if (bundleID.getTimestamp().compareTo(targetTimestamp) <= 0)
            return false;

        // if we replaced a bundle that precedes (or equals) the target bundle,
        // this is a replacement. (Note there should only ever be one replaced
        // bundle, but we iterate anyway for robustness.)
        for (FileBundleID replaced : replaces) {
            if (replaced.getTimestamp().compareTo(targetTimestamp) <= 0)
                return true;
        }

        // the target bundle doesn't fall in the replacement time frame.
        return false;
    }


    public FileBundleManifest(FileBundleID bundleID,
            ResourceCollectionInfo files, List<FileBundleID> parents,
            List<FileBundleID> replaces) {
        this(bundleID, files, Collections.EMPTY_LIST, parents, replaces);
    }

    public FileBundleManifest(FileBundleID bundleID,
            ResourceCollectionInfo files, List<String> metadata,
            List<FileBundleID> parents, List<FileBundleID> replaces) {
        if (bundleID == null || files == null || parents == null
                || replaces == null || metadata == null)
            throw new NullPointerException();

        this.bundleID = bundleID;
        this.files = files;
        this.metadata = metadata;
        this.parents = parents;
        this.replaces = replaces;
    }


    public FileBundleManifest(File dir, FileBundleID bundleID)
            throws IOException {
        this(getFileForManifest(dir, bundleID));
    }

    public FileBundleManifest(File src) throws IOException {
        this(readManifestXml(src));
        this.manifestFile = src;
    }

    protected static Element readManifestXml(File src) throws IOException {
        // open and parse the file as XML
        File dir = src.getParentFile();
        if (!dir.isDirectory())
            throw new FileNotFoundException(dir.getPath());
        if (!src.isFile()) {
            try {
                throw new Missing(src, new FileBundleID(src.getName()));
            } catch (IllegalArgumentException iae) {
                throw new FileNotFoundException(src.getPath());
            }
        }
        return parseXml(src).getDocumentElement();
    }

    public FileBundleManifest(Element xml) {
        // extract bundle data from the file
        this.bundleID = new FileBundleID(xml.getAttribute(ID_ATTR));
        this.files = XmlCollectionListing.parseListing(xml);
        this.metadata = extractMetadataList(xml);
        this.parents = extractBundleList(xml, PARENTS_TAG);
        this.replaces = extractBundleList(xml, REPLACES_TAG);
    }

    protected static Document parseXml(File src) throws IOException {
        try {
            return XMLUtils.parse(new BufferedInputStream( //
                    new FileInputStream(src)));
        } catch (SAXException e) {
            throw new IOException("Could not parse file bundle manifest " //
                    + src.getPath(), e);
        }
    }

    private List<String> extractMetadataList(Element xml) {
        NodeList nl = xml.getElementsByTagName(METADATA_TAG);
        if (nl == null || nl.getLength() == 0)
            return Collections.EMPTY_LIST;

        List<String> result = new ArrayList<String>();
        Element metaTag = (Element) nl.item(0);
        for (Element entry : XMLUtils.getChildElements(metaTag)) {
            String name = entry.getAttribute(ENTRY_NAME_ATTR);
            result.add(name);
        }
        return Collections.unmodifiableList(result);
    }

    private List<FileBundleID> extractBundleList(Element xml, String tagName) {
        List<FileBundleID> result = new ArrayList<FileBundleID>();
        for (Element child : XMLUtils.getChildElements(xml)) {
            if (tagName.equals(child.getTagName())) {
                NodeList nl = child.getElementsByTagName(BUNDLE_TAG);
                for (int i = 0; i < nl.getLength(); i++) {
                    Element tag = (Element) nl.item(i);
                    String bundleId = tag.getAttribute(ID_ATTR);
                    if (XMLUtils.hasValue(bundleId))
                        result.add(new FileBundleID(bundleId));
                }
            }
        }
        if (result.isEmpty())
            return Collections.EMPTY_LIST;
        else
            return Collections.unmodifiableList(result);
    }


    public void write(File directory) throws IOException {
        // create an XML file in the target directory
        File file = new File(directory, bundleID.getToken() + ".xml");
        writeToFile(file);
        if (manifestFile == null)
            manifestFile = file;
    }

    public void writeToFile(File file) throws IOException {
        OutputStream out = FileBundleUtils.outputStream(file);

        // start writing the document
        XmlSerializer xml = XMLUtils.getXmlSerializer(true);
        xml.setOutput(out, "UTF-8");
        xml.startDocument("UTF-8", null);
        xml.startTag(null, DOCUMENT_TAG);
        xml.attribute(null, ID_ATTR, bundleID.getToken());

        // write the list of files
        XmlCollectionListing.runReport(files, files.listResourceNames(), xml);

        // write the metadata entries
        writeMetadataList(xml);

        // write the list of parent bundles
        writeBundleList(xml, PARENTS_TAG, parents);

        // write the list of replaced bundles, if there are any
        if (!replaces.isEmpty())
            writeBundleList(xml, REPLACES_TAG, replaces);

        // finalize the document
        xml.endTag(null, DOCUMENT_TAG);
        xml.endDocument();
        out.close();
    }

    private void writeMetadataList(XmlSerializer xml) throws IOException {
        // do not write a metadata tag if this bundle contains no metadata
        if (metadata == null || metadata.isEmpty())
            return;

        // write a metadata block containing the given entries
        xml.startTag(null, METADATA_TAG);
        for (String name : metadata) {
            xml.startTag(null, ENTRY_TAG);
            xml.attribute(null, ENTRY_NAME_ATTR, name);
            xml.endTag(null, ENTRY_TAG);
        }
        xml.endTag(null, METADATA_TAG);
    }

    private void writeBundleList(XmlSerializer xml, String tagName,
            List<FileBundleID> bundleIDs) throws IOException {
        xml.startTag(null, tagName);
        if (bundleIDs != null) {
            for (FileBundleID oneBundleId : bundleIDs) {
                xml.startTag(null, BUNDLE_TAG);
                xml.attribute(null, ID_ATTR, oneBundleId.getToken());
                xml.endTag(null, BUNDLE_TAG);
            }
        }
        xml.endTag(null, tagName);
    }


    public static File getFileForManifest(File bundleDir,
            FileBundleID bundleID) {
        if (bundleDir == null || bundleID == null)
            throw new NullPointerException();
        return new File(bundleDir, bundleID.getToken() + ".xml");
    }


    static final String DOCUMENT_TAG = "fileBundle";

    private static final String METADATA_TAG = "metadata";

    private static final String ENTRY_TAG = "entry";

    private static final String ENTRY_NAME_ATTR = "name";

    private static final String PARENTS_TAG = "parents";

    private static final String REPLACES_TAG = "replaces";

    private static final String BUNDLE_TAG = "bundle";

    static final String ID_ATTR = "id";

}
