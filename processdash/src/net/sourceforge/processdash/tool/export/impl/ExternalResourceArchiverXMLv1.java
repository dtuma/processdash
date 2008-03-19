// Copyright (C) 2007-2008 Tuma Solutions, LLC
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.tool.bridge.client.ImportDirectory;
import net.sourceforge.processdash.tool.bridge.client.ImportDirectoryFactory;
import net.sourceforge.processdash.tool.bridge.impl.TeamServerPointerFile;
import net.sourceforge.processdash.tool.export.DataImporter;
import net.sourceforge.processdash.tool.export.mgr.ImportDirectoryInstruction;
import net.sourceforge.processdash.util.DrainableExecutor;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlSerializer;

public class ExternalResourceArchiverXMLv1 implements ExternalResourceArchiver,
        ExternalResourceMappingLoader, ExternalResourceXmlConstantsv1 {

    private boolean keepFileModificationTimes = Settings.getBool(
        "backup.keepExternalFileTimes", true);

    private Map<ImportDirectoryInstruction, String> importedDirs = Collections
            .synchronizedMap(new HashMap<ImportDirectoryInstruction, String>());
    private IOException exceptionEncountered;

    public Object dispatch(ImportDirectoryInstruction instr) {
        importedDirs.put(instr, null);
        return null;
    }

    public void export(ZipOutputStream out) throws IOException {
        archiveDirectories(out);
        writeManifest(out);
    }

    private void archiveDirectories(ZipOutputStream out) throws IOException {
        int numThreads = Settings.getInt("slowNetwork.numParallelReads", 10);
        ExecutorService service = Executors.newFixedThreadPool(
            Math.max(numThreads, 1));
        DrainableExecutor executor = new DrainableExecutor(service);
        exceptionEncountered = null;
        NumberFormat fmt = NumberFormat.getIntegerInstance();
        fmt.setMinimumIntegerDigits(3);

        List<ImportDirectoryInstruction> importInstructions =
            new ArrayList<ImportDirectoryInstruction>(importedDirs.keySet());
        for (int i = 0; i < importInstructions.size(); i++) {
            ImportDirectoryInstruction instr = importInstructions.get(i);
            String newPath = "extdir" + fmt.format(i + 1);
            executor.execute(new ArchiveDirectoryTask(executor, out, instr,
                    newPath));
        }

        executor.drain();

        if (exceptionEncountered != null)
            throw exceptionEncountered;
    }

    private class ArchiveDirectoryTask implements Runnable {

        Executor taskRunner;
        ZipOutputStream out;
        ImportDirectoryInstruction instr;
        String newPath;

        public ArchiveDirectoryTask(Executor taskRunner, ZipOutputStream out,
                ImportDirectoryInstruction instr, String newPath) {
            this.taskRunner = taskRunner;
            this.out = out;
            this.instr = instr;
            this.newPath = newPath;
        }

        public void run() {
            ImportDirectory dir = ImportDirectoryFactory.getInstance().get(instr);
            if (dir != null && dir.getDirectory().isDirectory()) {
                importedDirs.put(instr, newPath);
                archiveDirectory(taskRunner, out, dir.getDirectory(), newPath);
            } else {
                importedDirs.remove(instr);
            }
        }
    }

    private void archiveDirectory(Executor taskRunner, ZipOutputStream out,
            File sourceDir, String newPath) {
        File[] files = sourceDir.listFiles();
        for (int i = 0; i < files.length; i++) {
            String filename = files[i].getName();
            if (isFileToArchive(filename)) {
                String zipPath = ARCHIVE_PATH + "/" + newPath + "/" + filename;
                taskRunner.execute(new ArchiveFileTask(out, files[i], zipPath));
            }
        }
    }

    private boolean isFileToArchive(String filename) {
        filename = filename.toLowerCase();
        if (TeamServerPointerFile.FILE_NAME.equalsIgnoreCase(filename))
            return false;
        return filename.endsWith(DataImporter.EXPORT_FILE_OLD_SUFFIX)
                || filename.endsWith(DataImporter.EXPORT_FILE_SUFFIX)
                || filename.endsWith(".xml");
    }

    private class ArchiveFileTask implements Runnable {

        ZipOutputStream out;
        File sourceFile;
        String zipEntryPath;

        public ArchiveFileTask(ZipOutputStream out, File sourceFile,
                String zipEntryPath) {
            this.out = out;
            this.sourceFile = sourceFile;
            this.zipEntryPath = zipEntryPath;
        }

        public void run() {
            if (sourceFile.isFile()) {
                try {
                    archiveFile();
                } catch (IOException e) {
                    exceptionEncountered = e;
                }
            }
        }

        private void archiveFile() throws IOException {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            FileUtils.copyFile(sourceFile, buf);

            ZipEntry e = new ZipEntry(zipEntryPath);
            if (keepFileModificationTimes) {
                e.setTime(sourceFile.lastModified());
            }

            synchronized (out) {
                out.putNextEntry(e);
                buf.writeTo(out);
                out.closeEntry();
            }
        }
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

        for (Map.Entry<ImportDirectoryInstruction, String> e : importedDirs
                .entrySet()) {
            ImportDirectoryInstruction instr = e.getKey();
            String origURL = instr.getURL();
            String origPath = instr.getDirectory();
            String newPath = e.getValue();
            ser.startTag(null, IMPORT_DIR_ELEM);
            if (StringUtils.hasValue(origURL))
                ser.attribute(null, ORIGINAL_URL, origURL);
            if (StringUtils.hasValue(origPath))
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
            String origURL = e.getAttribute(ORIGINAL_URL);
            String newPath = e.getAttribute(NEW_PATH);
            File newDir = new File(archiveDir, newPath);
            result.put(origPath, newDir.getAbsolutePath());
            result.put(origURL, newDir.getAbsolutePath());
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
