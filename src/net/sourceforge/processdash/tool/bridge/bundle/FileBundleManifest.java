// Copyright (C) 2021 Tuma Solutions, LLC
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
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlSerializer;

import net.sourceforge.processdash.tool.bridge.ResourceCollectionInfo;
import net.sourceforge.processdash.tool.bridge.report.XmlCollectionListing;
import net.sourceforge.processdash.util.XMLUtils;

public class FileBundleManifest {

    public static class Missing extends FileNotFoundException {

        private FileBundleID bundleID;

        private Missing(File f, FileBundleID bundleID) {
            super(f.getPath());
            this.bundleID = bundleID;
        }

        public FileBundleID getBundleID() {
            return bundleID;
        }

    }

    private FileBundleID bundleID;

    private ResourceCollectionInfo files;

    private List<FileBundleID> parents;

    long accessTime;


    public FileBundleID getBundleID() {
        return bundleID;
    }

    public ResourceCollectionInfo getFiles() {
        return files;
    }

    public List<FileBundleID> getParents() {
        return parents;
    }


    public FileBundleManifest(FileBundleID bundleID,
            ResourceCollectionInfo files, List<FileBundleID> parents) {
        if (bundleID == null || files == null || parents == null)
            throw new NullPointerException();

        this.bundleID = bundleID;
        this.files = files;
        this.parents = parents;
    }


    public FileBundleManifest(File src) throws IOException {
        this(src.getParentFile(), src.getName());
    }

    public FileBundleManifest(File dir, String token) throws IOException {
        this(dir, new FileBundleID(token));
    }

    public FileBundleManifest(File dir, FileBundleID bundleID)
            throws IOException {
        if (dir == null || bundleID == null)
            throw new NullPointerException();

        this.bundleID = bundleID;

        // open and parse the file as XML
        if (!dir.isDirectory())
            throw new FileNotFoundException(dir.getPath());
        File src = getFileForManifest(dir, bundleID);
        if (!src.isFile())
            throw new Missing(src, bundleID);
        Element xml = parseXml(src);

        // extract bundle data from the file
        this.files = XmlCollectionListing.parseListing(xml);
        this.parents = extractBundleList(xml, PARENTS_TAG);
    }

    private Element parseXml(File src) throws IOException {
        try {
            return XMLUtils.parse(new BufferedInputStream( //
                    new FileInputStream(src))).getDocumentElement();
        } catch (SAXException e) {
            throw new IOException("Could not parse file bundle manifest " //
                    + src.getPath(), e);
        }
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
        return result;
    }


    public void write(File directory) throws IOException {
        // create an XML file in the target directory
        File file = new File(directory, bundleID.getToken() + ".xml");
        OutputStream out = FileBundleUtils.outputStream(file);

        // start writing the document
        XmlSerializer xml = XMLUtils.getXmlSerializer(true);
        xml.setOutput(out, "UTF-8");
        xml.startDocument("UTF-8", null);
        xml.startTag(null, DOCUMENT_TAG);
        xml.attribute(null, ID_ATTR, bundleID.getToken());

        // write the list of files
        XmlCollectionListing.runReport(files, files.listResourceNames(), xml);

        // write the list of parent bundles
        writeBundleList(xml, PARENTS_TAG, parents);

        // finalize the document
        xml.endTag(null, DOCUMENT_TAG);
        xml.endDocument();
        out.close();
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
        return new File(bundleDir, bundleID.getToken() + ".xml");
    }


    private static final String DOCUMENT_TAG = "fileBundle";

    private static final String PARENTS_TAG = "parents";

    private static final String BUNDLE_TAG = "bundle";

    private static final String ID_ATTR = "id";

}
