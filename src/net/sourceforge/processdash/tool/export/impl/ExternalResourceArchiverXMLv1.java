// Copyright (C) 2007-2015 Tuma Solutions, LLC
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
import java.io.InputStream;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.Prop;
import net.sourceforge.processdash.team.mcf.MCFManager;
import net.sourceforge.processdash.templates.TemplateLoader;
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

    private DashboardContext dashboardContext;
    private List<ImportDirectoryInstruction> importInstructions =
        new ArrayList<ImportDirectoryInstruction>();
    private ExternalResourceManifestXMLv1 manifest =
        new ExternalResourceManifestXMLv1();
    private IOException exceptionEncountered;

    public void setDashboardContext(DashboardContext dashboardContext) {
        this.dashboardContext = dashboardContext;
    }

    public Object dispatch(ImportDirectoryInstruction instr) {
        importInstructions.add(instr);
        return null;
    }

    public void export(ZipOutputStream out) throws IOException {
        archiveDirectories(out);
        archiveFrameworkMetadata(out);
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
                || filename.endsWith(".ini")
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

    private void archiveFrameworkMetadata(ZipOutputStream out) {
        for (String processID : getTeamProcessIDsInUse())
            archiveFrameworkMetadata(out, processID);
    }

    /** Get the IDs of all team processes in use by this dashboard dataset */
    private Set<String> getTeamProcessIDsInUse() {
        Set<String> templateIDs = getTeamProcessIDs();

        Set<String> result = new HashSet<String>();
        DashHierarchy hier = dashboardContext.getHierarchy();
        for (Iterator i = hier.values().iterator(); i.hasNext();) {
            Prop prop = (Prop) i.next();
            String oneID = prop.getID();
            if (oneID != null && oneID.endsWith("Root")) {
                int slashPos = oneID.indexOf('/');
                if (slashPos != -1) {
                    String oneProcessID = oneID.substring(0, slashPos);
                    if (templateIDs.contains(oneProcessID))
                        result.add(oneProcessID);
                }
            }
        }

        return result;

    }

    /** Get the IDs of all team process add-ons known to the dashboard */
    private Set<String> getTeamProcessIDs() {
        Set<String> teamProcessIDs = new HashSet<String>();
        Set<String> templateIDs = DashController.getTemplates().keySet();
        for (String oneID : templateIDs) {
            if (oneID.endsWith(TEAM_ROOT_SUFFIX)) {
                String processID = oneID.substring(0, oneID.length()
                        - TEAM_ROOT_SUFFIX.length());
                teamProcessIDs.add(processID);
            }
        }
        return teamProcessIDs;
    }
    private static final String TEAM_ROOT_SUFFIX = "/TeamRoot";

    private void archiveFrameworkMetadata(ZipOutputStream out, String processID) {
        // get the URL of the process settings.xml file
        URL u = MCFManager.getInstance().getMcfSourceFileUrl(processID,
            TemplateLoader.MCF_PROCESS_XML);
        if (u == null)
            return;

        // get the base URL of the JAR file containing the settings.xml file
        String baseUrl = u.toString();
        int slashPos = baseUrl.lastIndexOf("/");
        if (slashPos == -1)
            return;
        baseUrl = baseUrl.substring(0, slashPos + 1);

        // add the settings.xml file to our output ZIP
        String newPath = MCF_SUBDIR + "/" + processID;
        boolean foundSettings = addFrameworkMetadataToArchive(out, baseUrl,
                newPath, "settings.xml");
        if (!foundSettings)
            return;

        // add other optional items to our output ZIP as well
        addFrameworkMetadataToArchive(out, baseUrl, newPath, "process_info.xml");
        String version = TemplateLoader.getPackageVersion(processID);
        manifest.addMetricsCollectionFramework(processID, version, newPath);
    }

    private boolean addFrameworkMetadataToArchive(ZipOutputStream out,
            String baseUrl, String newPath, String file) {
        try {
            String fullUrl = baseUrl + file;
            InputStream in = new URL(fullUrl).openStream();
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            FileUtils.copyFile(in, buf);
            in.close();

            synchronized (out) {
                String path = ARCHIVE_PATH + "/" + newPath + "/" + file;
                out.putNextEntry(new ZipEntry(path));
                buf.writeTo(out);
                out.closeEntry();
            }
            return true;
        } catch (IOException ioe) {}
        return false;
    }

    public static void cleanupBogusArchiveDirectory(File baseDir) {
        try {
            File archiveDir = new File(baseDir, ARCHIVE_PATH);
            FileUtils.deleteDirectory(archiveDir, true);
        } catch (IOException e) {}
    }
}
