// Copyright (C) 2018-2020 Tuma Solutions, LLC
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

package teamdash.sync;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlSerializer;

import net.sourceforge.processdash.tool.export.impl.ArchiveMetricsXmlConstants;
import net.sourceforge.processdash.tool.export.mgr.ExportFileEntry;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.RobustFileOutputStream;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

public class SyncDataFile implements ArchiveMetricsXmlConstants {

    public class ComodificationException extends IOException {}


    private TeamProjectDataTarget target;

    private File file;

    private String processToken;

    private long fileTimestamp;

    private ZipFile zip;

    private SyncMetadata syncMetadata;

    private Map<ExportFileEntry, byte[]> explicitEntries;


    public SyncDataFile(TeamProjectDataTarget target, String filename) {
        this(target.getDirectory(), filename);
        this.target = target;
    }

    public SyncDataFile(File directory, String filename) {
        this.file = new File(directory, filename);
        this.processToken = Long.toString(System.currentTimeMillis());
        this.fileTimestamp = 0;
    }

    public void dispose() throws IOException {
        fileTimestamp = 0;
        closeZip();
        syncMetadata = null;
    }

    public void saveChanges() throws IOException {
        if (target == null) {
            throw new IllegalStateException();
        } else if (syncMetadata.isChanged() || explicitEntries != null) {
            saveFile();
            target.saveSyncData(file.getName());
            syncMetadata.clearChanged();
            explicitEntries = null;
        } else {
            closeZip();
        }
    }

    public void checkComodification() throws IOException {
        if (fileTimestamp == 0) {
            // we haven't saved the file for the first time yet
        } else if (fileTimestamp == file.lastModified()) {
            // the filesystem time agrees with our records
        } else {
            // timestamp doesn't match. Look inside the file to be sure
            SyncMetadata currentMetadata = loadMetadata();
            String currentToken = currentMetadata.getStr(DAEMON_PROCESS_TOKEN);
            if (!processToken.equals(currentToken)) {
                closeZip();
                throw new ComodificationException();
            } else {
                fileTimestamp = file.lastModified();
            }
        }
    }

    public long getFileTimestamp() {
        return fileTimestamp;
    }

    public InputStream openEntry(String entryName) throws IOException {
        // if the file does not exist, it doesn't have any entries
        if (!file.isFile())
            return null;

        // create a ZipFile object as needed
        if (zip == null)
            zip = new ZipFile(file);

        // find and return the named entry
        ZipEntry entry = zip.getEntry(entryName);
        return (entry == null ? null : zip.getInputStream(entry));
    }

    public void addEntry(ExportFileEntry entry, byte[] data) {
        if (explicitEntries == null)
            explicitEntries = new LinkedHashMap<ExportFileEntry, byte[]>();
        else
            discardEntryWithName(explicitEntries.keySet(), entry.getFilename());
        explicitEntries.put(entry, data);
    }

    private void closeZip() throws IOException {
        if (zip != null) {
            zip.close();
            zip = null;
        }
    }

    public SyncMetadata getMetadata() throws IOException {
        // if the metadata hasn't been loaded yet, do so now
        if (syncMetadata == null) {
            syncMetadata = loadMetadata();
            syncMetadata.setStr(processToken, DAEMON_PROCESS_TOKEN);
        }
        return syncMetadata;
    }

    private SyncMetadata loadMetadata() throws IOException {
        SyncMetadata result = new SyncMetadata();
        InputStream in = openEntry(METADATA_ENTRY_NAME);
        if (in != null) {
            result.loadFromXML(in);
            in.close();
        }
        return result;
    }

    private void saveFile() throws IOException {
        // load information from the existing file, as applicable
        List<ExportFileEntry> fileEntries = readExistingManifestEntries();
        getMetadata();

        // open a stream to write ZIP data to the file
        RobustFileOutputStream out = new RobustFileOutputStream(file);
        ZipOutputStream zipOut = new ZipOutputStream(
                new BufferedOutputStream(out));

        // write the sync metadata file
        writeSyncMetadataFile(zipOut);

        // save any newly added entries
        writeExplicitEntries(fileEntries, zipOut);

        // copy other file entries from src to dest
        copyExistingFileEntries(fileEntries, zipOut);

        // write the manifest file
        writeManifest(zipOut, fileEntries);

        // finalize the ZIP file
        closeZip();
        zipOut.finish();
        zipOut.close();
        fileTimestamp = file.lastModified();
    }


    public List<ExportFileEntry> readExistingManifestEntries()
            throws IOException {
        if (!file.isFile())
            return new ArrayList<ExportFileEntry>();

        // read the manifest from the existing file
        Element manifest;
        try {
            manifest = XMLUtils.parse(openEntry(MANIFEST_FILE_NAME))
                    .getDocumentElement();
        } catch (SAXException e) {
            throw new IOException(e);
        }

        // read the file entries from the manifest
        List<ExportFileEntry> entries = new ArrayList<ExportFileEntry>();
        for (Element xml : XMLUtils.getChildElements(manifest)) {
            if (FILE_ELEM.equals(xml.getTagName())) {
                ExportFileEntry efe = new ExportFileEntry(xml);
                if (!METADATA_ENTRY_NAME.equals(efe.getFilename()))
                    entries.add(efe);
            }
        }
        return entries;
    }

    private void writeSyncMetadataFile(ZipOutputStream zipOut)
            throws IOException {
        // save the sync metadata file
        zipOut.putNextEntry(new ZipEntry(METADATA_ENTRY_NAME));
        syncMetadata.storeToXML(zipOut, null);
        zipOut.closeEntry();
    }

    private void writeExplicitEntries(List<ExportFileEntry> fileEntries,
            ZipOutputStream zipOut) throws IOException {
        if (explicitEntries == null)
            return;

        // iterate over the explicit entries
        for (Entry<ExportFileEntry, byte[]> e : explicitEntries.entrySet()) {
            ExportFileEntry entry = e.getKey();
            String entryName = entry.getFilename();
            byte[] data = e.getValue();

            // add the file to the output ZIP
            if (data != null) {
                zipOut.putNextEntry(new ZipEntry(entryName));
                zipOut.write(data);
                zipOut.closeEntry();
            }

            // if this entry replaces one that was already in the data file,
            // remove that existing entry from the fileEntries list
            discardEntryWithName(fileEntries, entryName);
        }
    }

    private void discardEntryWithName(Collection<ExportFileEntry> fileEntries,
            String entryName) {
        for (Iterator i = fileEntries.iterator(); i.hasNext();) {
            ExportFileEntry efe = (ExportFileEntry) i.next();
            if (entryName.equals(efe.getFilename()))
                i.remove();
        }
    }

    private void copyExistingFileEntries(List<ExportFileEntry> fileEntries,
            ZipOutputStream zipOut) throws IOException {
        // copy other data files from the old ZIP into the new ZIP
        for (ExportFileEntry efe : fileEntries) {
            String entryName = efe.getFilename();
            zipOut.putNextEntry(new ZipEntry(entryName));
            InputStream src = openEntry(entryName);
            FileUtils.copyFile(src, zipOut);
            src.close();
            zipOut.closeEntry();
        }
    }

    private void writeManifest(ZipOutputStream zipOut,
            List<ExportFileEntry> additionalEntries) throws IOException {
        // start an entry for the manifest file
        zipOut.putNextEntry(new ZipEntry(MANIFEST_FILE_NAME));

        // begin the XML document
        XmlSerializer xml = XMLUtils.getXmlSerializer(true);
        xml.setOutput(zipOut, ENCODING);
        xml.startDocument(ENCODING, Boolean.TRUE);

        // write the root <archive> tag
        xml.startTag(null, ARCHIVE_ELEM);
        xml.attribute(null, TYPE_ATTR, FILE_TYPE_ARCHIVE);

        // write an <exported> tag
        xml.startTag(null, EXPORTED_TAG);
        xml.attribute(null, WHEN_ATTR, XMLUtils.saveDate(new Date()));
        xml.endTag(null, EXPORTED_TAG);

        // write a file entry for the sync metadata
        writeManifestFileTag(xml, METADATA_ENTRY_NAME, METADATA_FILE_TYPE, "1");

        // write entries for each other file in the archive
        if (explicitEntries != null)
            additionalEntries.addAll(explicitEntries.keySet());
        for (ExportFileEntry e : additionalEntries) {
            writeManifestFileTag(xml, e.getFilename(), e.getType(),
                e.getVersion());
        }

        // end the document
        xml.endTag(null, ARCHIVE_ELEM);
        xml.endDocument();

        zipOut.closeEntry();
    }

    private void writeManifestFileTag(XmlSerializer xml, String filename,
            String type, String version) throws IOException {
        xml.startTag(null, FILE_ELEM);
        xml.attribute(null, FILE_NAME_ATTR, filename);
        if (StringUtils.hasValue(type))
            xml.attribute(null, TYPE_ATTR, type);
        if (StringUtils.hasValue(version))
            xml.attribute(null, VERSION_ATTR, version);
        xml.endTag(null, FILE_ELEM);
    }

    private static final String DAEMON_PROCESS_TOKEN = "daemon.processToken";

    private static final String METADATA_FILE_TYPE = "syncMetadata";

    private static final String METADATA_ENTRY_NAME = METADATA_FILE_TYPE
            + ".xml";

}
