// Copyright (C) 2007-2012 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.export.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlSerializer;

public class ExternalResourceManifestXMLv1 implements
        ExternalResourceMappingLoader, ExternalResourceXmlConstantsv1 {


    private List<MappingEntry> mappingEntries;

    private List<MCFEntry> mcfEntries;

    public ExternalResourceManifestXMLv1() {
        this.mappingEntries = new ArrayList<MappingEntry>();
        this.mcfEntries = new ArrayList<MCFEntry>();
    }

    public boolean isEmpty() {
        return mappingEntries.isEmpty();
    }

    public synchronized void addMapping(String origPath, String origUrl,
            String newPath) {
        mappingEntries.add(new MappingEntry(origPath, origUrl, newPath));
    }

    public synchronized void addMetricsCollectionFramework(String frameworkID,
            String version, String newPath) {
        mcfEntries.add(new MCFEntry(frameworkID, version, newPath));
    }

    public void write(ZipOutputStream out) throws IOException {
        if (isEmpty())
            // none of the directories listed in the import instruction setting
            // actually existed, so none of them were backed up into the ZIP
            // archive.  There is no need to write a manifest file, either.
            return;

        ZipEntry entry = new ZipEntry(ARCHIVE_PATH + "/" + MANIFEST_FILE_NAME);
        out.putNextEntry(entry);
        write((OutputStream) out);
        out.closeEntry();
    }

    public void write(OutputStream out) throws IOException {
        XmlSerializer ser = XMLUtils.getXmlSerializer(true);
        ser.setOutput(out, ENCODING);
        ser.startDocument(ENCODING, null);
        ser.startTag(null, DOC_ROOT_ELEM);

        Collections.sort(mappingEntries);
        for (MappingEntry e : mappingEntries)
            e.write(ser);

        Collections.sort(mcfEntries);
        for (MCFEntry e : mcfEntries)
            e.write(ser);

        ser.endTag(null, DOC_ROOT_ELEM);
        ser.endDocument();
    }


    public Map load(File fileOrDirectory) {
        File archiveDir;
        File manifestFile;
        if (fileOrDirectory.getName().toLowerCase().endsWith(".xml")) {
            manifestFile = fileOrDirectory;
            archiveDir = manifestFile.getParentFile();
        }
        else {
            archiveDir = new File(fileOrDirectory, ARCHIVE_PATH);
            manifestFile = new File(archiveDir, MANIFEST_FILE_NAME);
        }

        if (!manifestFile.isFile())
            return Collections.EMPTY_MAP;

        Document doc;
        try {
            doc = XMLUtils.parse(new FileInputStream(manifestFile));
        } catch (Exception e) {
            return Collections.EMPTY_MAP;
        }

        Map result = new HashMap();
        NodeList directories = doc.getElementsByTagName(IMPORT_DIR_ELEM);
        for (int i=0;  i < directories.getLength();  i++) {
            Element e = (Element) directories.item(i);
            MappingEntry me = new MappingEntry(e);
            mappingEntries.add(me);

            File newDir = new File(archiveDir, me.newPath);
            result.put(me.originalPath, newDir.getAbsolutePath());
            result.put(me.originalURL, newDir.getAbsolutePath());
        }
        result.remove(null);

        NodeList frameworks = doc.getElementsByTagName(MCF_ELEM);
        for (int i=0;  i < frameworks.getLength();  i++) {
            Element e = (Element) frameworks.item(i);
            MCFEntry me = new MCFEntry(e);
            mcfEntries.add(me);
        }

        return result;
    }

    private class MappingEntry implements Comparable<MappingEntry> {

        private String originalPath;

        private String originalURL;

        private String newPath;

        protected MappingEntry(Element xml) {
            this(xml.getAttribute(ORIGINAL_PATH),
                 xml.getAttribute(ORIGINAL_URL),
                 xml.getAttribute(NEW_PATH));
        }

        protected MappingEntry(String originalPath, String originalURL,
                String newPath) {
            this.originalPath = nvl(originalPath);
            this.originalURL = nvl(originalURL);
            this.newPath = nvl(newPath);
        }

        public void write(XmlSerializer ser) throws IOException {
            ser.startTag(null, IMPORT_DIR_ELEM);
            if (StringUtils.hasValue(originalURL))
                ser.attribute(null, ORIGINAL_URL, originalURL);
            if (StringUtils.hasValue(originalPath))
                ser.attribute(null, ORIGINAL_PATH, originalPath);
            ser.attribute(null, NEW_PATH, newPath);
            ser.endTag(null, IMPORT_DIR_ELEM);
        }

        public int compareTo(MappingEntry that) {
            return this.newPath.compareTo(that.newPath);
        }

    }

    private class MCFEntry implements Comparable<MCFEntry> {

        private String frameworkID;

        private String frameworkVersion;

        private String newPath;

        protected MCFEntry(Element xml) {
            this(xml.getAttribute(MCF_ID),
                 xml.getAttribute(MCF_VERSION),
                 xml.getAttribute(NEW_PATH));
        }

        protected MCFEntry(String frameworkID, String frameworkVersion,
                String newPath) {
            this.frameworkID = nvl(frameworkID);
            this.frameworkVersion = nvl(frameworkVersion);
            this.newPath = nvl(newPath);
        }

        public void write(XmlSerializer ser) throws IOException {
            ser.startTag(null, MCF_ELEM);
            if (StringUtils.hasValue(frameworkID))
                ser.attribute(null, MCF_ID, frameworkID);
            if (StringUtils.hasValue(frameworkVersion))
                ser.attribute(null, MCF_VERSION, frameworkVersion);
            ser.attribute(null, NEW_PATH, newPath);
            ser.endTag(null, MCF_ELEM);
        }

        public int compareTo(MCFEntry that) {
            return this.newPath.compareTo(that.newPath);
        }

    }

    private static String nvl(String s) {
        return (StringUtils.hasValue(s) ? s : null);
    }

}
