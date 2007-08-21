// Copyright (C) 2007 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.export.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.tool.export.DataImporter;
import net.sourceforge.processdash.tool.export.mgr.ExternalResourceManager;
import net.sourceforge.processdash.tool.export.mgr.ImportDirectoryInstruction;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.ProfTimer;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlSerializer;

public class ExternalResourceArchiverXMLv1 implements ExternalResourceArchiver,
        ExternalResourceMappingLoader, ExternalResourceXmlConstantsv1 {

    private static final Logger logger = Logger
            .getLogger(ExternalResourceArchiverXMLv1.class.getName());

    private boolean keepFileModificationTimes = Settings.getBool(
        "backup.keepExternalFileTimes", true);

    private Map importedDirs = new HashMap();

    public Object dispatch(ImportDirectoryInstruction instr) {
        importedDirs.put(instr.getDirectory(), null);
        return null;
    }

    public void export(ZipOutputStream out) throws IOException {
        archiveDirectories(out);
        writeManifest(out);
    }

    private void archiveDirectories(ZipOutputStream out) throws IOException {
        ExternalResourceManager extMgr = ExternalResourceManager.getInstance();
        NumberFormat fmt = NumberFormat.getIntegerInstance();
        fmt.setMinimumIntegerDigits(3);
        int pos = 1;

        for (Iterator i = importedDirs.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            String origPath = (String) e.getKey();
            origPath = extMgr.remapFilename(origPath);
            File sourceDir = new File(origPath);
            ProfTimer pt = new ProfTimer(logger, origPath);
            if (sourceDir.isDirectory()) {
                pt.click("isDirectory");
                String newPath = "extdir" + fmt.format(pos++);
                e.setValue(newPath);
                archiveDirectory(out, sourceDir, newPath, pt);
                pt.click("finished archiving directory");
            } else {
                i.remove();
                pt.click("is not directory");
            }
        }
    }

    private void archiveDirectory(ZipOutputStream out, File sourceDir,
            String newPath, ProfTimer pt) throws IOException {
        File[] files = sourceDir.listFiles();
        pt.click("listed files");
        for (int i = 0; i < files.length; i++) {
            String filename = files[i].getName();
            if (isFileToArchive(filename) && files[i].isFile()) {
                pt.click("isFile("+filename+")");
                ZipEntry e = new ZipEntry(ARCHIVE_PATH + "/" + newPath
                        + "/" + filename);
                if (keepFileModificationTimes) {
                    e.setTime(files[i].lastModified());
                    pt.click("lastModified("+filename+")");
                }
                out.putNextEntry(e);
                FileUtils.copyFile(files[i], out);
                out.closeEntry();
                pt.click("copyFile("+filename+")");
            }
        }
    }

    private boolean isFileToArchive(String filename) {
        filename = filename.toLowerCase();
        return filename.endsWith(DataImporter.EXPORT_FILE_OLD_SUFFIX)
                || filename.endsWith(DataImporter.EXPORT_FILE_SUFFIX)
                || filename.endsWith(".xml");
    }

    private void writeManifest(ZipOutputStream out) throws IOException {
        if (importedDirs.isEmpty())
            // none of the directories listed in the import instruction setting
            // actually existed, so none of them were backed up into the ZIP
            // archive.  There is no need to write a manifest file, either.
            return;

        XmlSerializer ser = XMLUtils.getXmlSerializer(true);

        ZipEntry entry = new ZipEntry(ARCHIVE_PATH + "/" + MANIFEST_FILE_NAME);
        out.putNextEntry(entry);
        ser.setOutput(out, ENCODING);
        ser.startDocument(ENCODING, null);
        ser.startTag(null, DOC_ROOT_ELEM);

        for (Iterator i = importedDirs.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            String origPath = (String) e.getKey();
            String newPath = (String) e.getValue();
            ser.startTag(null, IMPORT_DIR_ELEM);
            ser.attribute(null, ORIGINAL_PATH, origPath);
            ser.attribute(null, NEW_PATH, newPath);
            ser.endTag(null, IMPORT_DIR_ELEM);
        }

        ser.endTag(null, DOC_ROOT_ELEM);
        ser.endDocument();

        out.closeEntry();
    }


    public Map load(File directory) {
        File archiveDir = new File(directory, ARCHIVE_PATH);
        if (!archiveDir.isDirectory())
            return Collections.EMPTY_MAP;

        File manifestFile = new File(archiveDir, MANIFEST_FILE_NAME);
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
            String origPath = e.getAttribute(ORIGINAL_PATH);
            String newPath = e.getAttribute(NEW_PATH);
            File newDir = new File(archiveDir, newPath);
            result.put(origPath, newDir.getAbsolutePath());
        }
        return result;
    }

    public static void cleanupBogusArchiveDirectory(File baseDir) {
        try {
            File archiveDir = new File(baseDir, ARCHIVE_PATH);
            FileUtils.deleteDirectory(archiveDir, true);
        } catch (IOException e) {}
    }
}
