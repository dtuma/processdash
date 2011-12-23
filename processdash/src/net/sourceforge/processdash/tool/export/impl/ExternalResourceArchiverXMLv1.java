// Copyright (C) 2007-2011 Tuma Solutions, LLC
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.tool.bridge.client.ImportDirectory;
import net.sourceforge.processdash.tool.bridge.client.ImportDirectoryFactory;
import net.sourceforge.processdash.tool.bridge.impl.DashboardInstanceStrategy;
import net.sourceforge.processdash.tool.bridge.impl.TeamServerPointerFile;
import net.sourceforge.processdash.tool.export.DataImporter;
import net.sourceforge.processdash.tool.export.mgr.ImportDirectoryInstruction;
import net.sourceforge.processdash.util.DrainableExecutor;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.StringUtils;

public class ExternalResourceArchiverXMLv1 implements ExternalResourceArchiver,
        ExternalResourceXmlConstantsv1 {

    private boolean keepFileModificationTimes = Settings.getBool(
        "backup.keepExternalFileTimes", true);

    private List<ImportDirectoryInstruction> importInstructions =
        new ArrayList<ImportDirectoryInstruction>();
    private ExternalResourceManifestXMLv1 manifest =
        new ExternalResourceManifestXMLv1();
    private IOException exceptionEncountered;

    public Object dispatch(ImportDirectoryInstruction instr) {
        importInstructions.add(instr);
        return null;
    }

    public void export(ZipOutputStream out) throws IOException {
        archiveDirectories(out);
        manifest.write(out);
    }

    private void archiveDirectories(ZipOutputStream out) throws IOException {
        int numThreads = Settings.getInt("slowNetwork.numParallelReads", 10);
        ExecutorService service = Executors.newFixedThreadPool(
            Math.max(numThreads, 1));
        DrainableExecutor executor = new DrainableExecutor(service);
        exceptionEncountered = null;
        NumberFormat fmt = NumberFormat.getIntegerInstance();
        fmt.setMinimumIntegerDigits(3);

        Set<String> extDirNames = new HashSet<String>();
        extDirNames.add(null); extDirNames.add("");
        for (int i = 0; i < importInstructions.size(); i++) {
            ImportDirectoryInstruction instr = importInstructions.get(i);
            String newPath = getExtDirName(instr);
            if (extDirNames.contains(newPath))
                newPath = "extdir" + fmt.format(i + 1);
            extDirNames.add(newPath);
            executor.execute(new ArchiveDirectoryTask(executor, out, instr,
                    newPath));
        }

        executor.drain();

        if (exceptionEncountered != null)
            throw exceptionEncountered;
    }

    private String getExtDirName(ImportDirectoryInstruction instr) {
        String path = instr.getDirectory();
        if (!StringUtils.hasValue(path)) path = instr.getURL();
        if (!StringUtils.hasValue(path)) return null;

        path = path.replace('\\', '/');
        path = StringUtils.findAndReplace(path, "/disseminate", "-disseminate");
        int pos = path.lastIndexOf('/');
        if (pos == -1)
            return null;
        else
            return path.substring(pos + 1);
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
            String origUrl = instr.getURL();
            String origPath = instr.getDirectory();
            ImportDirectory dir = ImportDirectoryFactory.getInstance().get(
                origUrl, origPath);
            if (dir != null && dir.getDirectory().isDirectory()) {
                manifest.addMapping(origPath, origUrl, newPath);
                archiveDirectory(taskRunner, out, dir.getDirectory(), newPath);
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
        if (DashboardInstanceStrategy.LOCK_FILE_NAME.equalsIgnoreCase(filename))
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

    public static void cleanupBogusArchiveDirectory(File baseDir) {
        try {
            File archiveDir = new File(baseDir, ARCHIVE_PATH);
            FileUtils.deleteDirectory(archiveDir, true);
        } catch (IOException e) {}
    }
}
