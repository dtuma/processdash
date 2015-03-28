// Copyright (C) 2002-2014 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package net.sourceforge.processdash.team.setup.move;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import java.util.zip.Adler32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.TagData;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.team.setup.RepairImportInstruction;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;
import net.sourceforge.processdash.util.lock.AlreadyLockedException;
import net.sourceforge.processdash.util.lock.FileConcurrencyLock;
import net.sourceforge.processdash.util.lock.LockFailureException;


public class MoveProjectWorker {

    private DashboardContext ctx;
    private String projectPrefix;
    private String masterPrefix;
    private String projectID;
    private String processID;
    private boolean isMaster;
    private String oldTeamDir;
    private File oldTeamDataDir;
    private String newTeamDir;
    private File newTeamDataDir;
    private String newTeamDirUNC;
    private FileConcurrencyLock fileLock;
    private StringBuffer nonfatalProblems;


    MoveProjectWorker(DashboardContext ctx, String projectPrefix,
            String masterPrefix, String projectID, String processID,
            boolean isMaster, String oldTeamDir, String newTeamDir,
            String newTeamDirUNC) {
        this.ctx = ctx;
        this.projectPrefix = projectPrefix;
        this.masterPrefix = masterPrefix;
        this.projectID = projectID;
        this.processID = processID;
        this.isMaster = isMaster;
        this.oldTeamDir = oldTeamDir;
        this.newTeamDir = newTeamDir;
        this.newTeamDirUNC = newTeamDirUNC;

        this.oldTeamDataDir = getDataDir(this.oldTeamDir);
        this.newTeamDataDir = getDataDir(this.newTeamDir);
        this.nonfatalProblems = new StringBuffer("z");
    }

    private File getDataDir(String teamDir) {
        File data = new File(teamDir, "data");
        return new File(data, projectID);
    }

    /**
     * Perform the operations needed to move this project.
     * 
     * @throws MoveProjectException if a problem occurs during the operation.
     */
    public void run() throws MoveProjectException {
        try {
            lockOldDataDirectory();
            copyFilesToNewDirectory();
            saveNewDataValues();
            repairMasterProjectLinks();
            tweakOldProjectFiles();

            if (nonfatalProblems.length() > 2)
                throw new MoveProjectException(MoveProjectWizard.SUCCESS_URI,
                        nonfatalProblems.substring(2));

        } finally {
            unlockOldDataDirectory();
        }
    }



    /*
     * Methods to lock and unlock the old data directory
     */

    private void lockOldDataDirectory() throws MoveProjectException {
        File lockFile = new File(oldTeamDataDir, LOCK_FILE);
        fileLock = new FileConcurrencyLock(lockFile);
        try {
            fileLock.acquireLock(null);
        } catch (AlreadyLockedException ale) {
            throw new MoveProjectException("teamDirInUse").append("lockOwner",
                ale.getExtraInfo());
        } catch (LockFailureException e) {
            throw new MoveProjectException("teamDirInUse");
        }
    }

    private void unlockOldDataDirectory() {
        if (fileLock != null)
            fileLock.releaseLock();
    }



    /*
     * Methods to copy files from the old directory to the new directory
     */

    private void copyFilesToNewDirectory() throws MoveProjectException {
        try {
            createNewDirectories();
            copyFiles(oldTeamDataDir, newTeamDataDir);
        } catch (MoveProjectException mpe) {
            deleteNewDirectory();
            throw mpe;
        }
    }

    private void createNewDirectories() throws MoveProjectException {
        if (dataDirAlreadyExists(newTeamDataDir))
            throw new MoveProjectException(MoveProjectWizard.NEWDIR_URI,
                    "exists").append("path", newTeamDataDir.getPath());
        createDirectory(newTeamDataDir);
        createDirectory(new File(newTeamDataDir,
                TeamDataConstants.DISSEMINATION_DIRECTORY));
    }

    static boolean dataDirAlreadyExists(File dir) {
        if (dir.isDirectory())
            return dir.list().length > 0;
        else if (dir.isFile())
            return true;
        else
            return false;
    }

    private void copyFiles(File fromDir, File toDir)
            throws MoveProjectException {
        for (File oldFile : fromDir.listFiles()) {
            String name = oldFile.getName();
            if (LOCK_FILE.equals(name) || MARKER_FILE.equals(name)
                    || MOVE_FILE_NAME.equals(name))
                continue;
            File newFile = new File(toDir, name);
            if (oldFile.isDirectory()) {
                copyFiles(oldFile, createDirectory(newFile));
            } else {
                copyFile(oldFile, newFile);
            }
        }
    }

    private File createDirectory(File dir) throws MoveProjectException {
        if (!dir.exists() && !dir.mkdirs())
            throw new MoveProjectException(MoveProjectWizard.NEWDIR_URI,
                    "cannotCreateDir").append("path", dir.getPath());
        return dir;
    }

    private void copyFile(File oldFile, File newFile)
            throws MoveProjectException {
        try {
            FileUtils.copyFile(oldFile, newFile);

            long oldSum = FileUtils.computeChecksum(oldFile, new Adler32());
            long newSum = FileUtils.computeChecksum(newFile, new Adler32());
            if (oldSum == newSum)
                return;
        } catch (IOException e) {}

        throw new MoveProjectException(MoveProjectWizard.NEWDIR_URI,
                "cannotCreateFile").append("oldPath", oldFile.getPath())
                .append("path", newFile.getPath());
    }

    private void deleteNewDirectory() {
        try {
            FileUtils.deleteDirectory(newTeamDataDir, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    /*
     * Methods to update the values in the data repository
     */

    private void saveNewDataValues() {
        DataContext data = ctx.getData().getSubcontext(projectPrefix);
        saveDataValue(data, TeamDataConstants.TEAM_DIRECTORY, newTeamDir);
        saveDataValue(data, TeamDataConstants.TEAM_DIRECTORY_UNC, newTeamDirUNC);
        RepairImportInstruction.maybeRepairForTeam(data);
    }

    private void saveDataValue(DataContext data, String name, String value) {
        data.putValue(name, value == null ? null : StringData.create(value));
    }



    /*
     * Methods to repair connections between master projects and subprojects
     */

    private void repairMasterProjectLinks() {
        if (!StringUtils.hasValue(masterPrefix))
            return;

        String masterTagDataName = masterPrefix + "/" + processID
                + " Master Root Tag";
        Object tag = ctx.getData().getSimpleValue(masterTagDataName);
        if (!(tag instanceof TagData)) {
            couldNotRepairMasterLinks();
            return;
        }

        String url = WebServer.DASHBOARD_PROTOCOL + ":"
                + WebServer.urlEncodePath(masterPrefix) + "//" + processID
                + "/setup/subprojectEdit?do=update";
        try {
            InputStream in = new URL(url).openStream();
            while (in.read() != -1)
                ;
        } catch (Exception e) {
            couldNotRepairMasterLinks();
        }
    }

    private void couldNotRepairMasterLinks() {
        HTMLUtils.appendQuery(nonfatalProblems, "cannotUpdateMaster");
        HTMLUtils.appendQuery(nonfatalProblems, "masterPrefix",
            masterPrefix);
    }



    /*
     * Methods to alter the files in the old data directory
     */

    private void tweakOldProjectFiles() {
        writeMarkerFile();
        makeFilesReadOnly();
        if (!isMaster)
            writeMoveFile();
    }

    private void writeMarkerFile() {
        try {
            File marker = new File(oldTeamDataDir, MARKER_FILE);
            PrintWriter out = new PrintWriter(new FileWriter(marker));
            out.println("This directory is obsolete.  If was formerly used to");
            out.println("store data for the project '" + projectPrefix + "'.");
            out.println("However, that data has been moved to the directory");
            out.println(newTeamDataDir);
            out.println();
            out.println("The contents of this directory have been left intact");
            out.println("while individual team members transition to the new");
            out.println("directory.  After all team members have transitioned,");
            out.println("this directory can be safely deleted.");
            out.flush();
            out.close();
        } catch (IOException e) {}
    }

    private void makeFilesReadOnly() {
        // Make the files in the old team data directory read-only.
        // This will cause the WBS to open in read-only mode.
        // It will also prevent individuals from exporting their data
        // until they update their team project location.
        for (File f : oldTeamDataDir.listFiles())
            f.setReadOnly();
    }

    private void writeMoveFile() {
        File disseminate = new File(oldTeamDataDir,
                TeamDataConstants.DISSEMINATION_DIRECTORY);
        File moveFile = new File(disseminate, MOVE_FILE_NAME);
        try {
            writeMoveFileImpl(moveFile);
        } catch (IOException e) {
            HTMLUtils.appendQuery(nonfatalProblems, "cannotWriteMoveFile");
            HTMLUtils.appendQuery(nonfatalProblems, "path", moveFile.getPath());
        }
    }

    private void writeMoveFileImpl(File moveFile) throws IOException {
        ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(
                new FileOutputStream(moveFile)));
        Writer out = new OutputStreamWriter(zipOut, "UTF-8");

        zipOut.putNextEntry(new ZipEntry("manifest.xml"));
        out.write(XML_HEADER);
        out.write("<archive type='dashboardDataExport'>\n");
        out.write("  <exported when='@" + System.currentTimeMillis() + "' />\n");
        out.write("  <file name='move.xml' type='messages' version='1' />\n");
        out.write("</archive>\n");
        out.flush();
        zipOut.closeEntry();

        zipOut.putNextEntry(new ZipEntry("move.xml"));
        out.write(XML_HEADER);
        out.write("<messages>\n");
        out.write("  <message type='" + escXml(getMoveMsgType())
                + "' msgId='" + escXml(getMoveMsgId()) + "'>\n");
        writeMoveMsgVal(out, MoveTeamDirMessageHandler.PROJECT_ID_ATTR, projectID);
        writeMoveMsgVal(out, MoveTeamDirMessageHandler.DIR_ATTR, newTeamDir);
        writeMoveMsgVal(out, MoveTeamDirMessageHandler.DIR_UNC_ATTR, newTeamDirUNC);
        out.write("  </message>\n");
        out.write("</messages>\n");
        out.flush();
        zipOut.closeEntry();

        zipOut.close();
    }
    private String getMoveMsgType() {
        return processID + "/moveTeamDir";
    }
    private String getMoveMsgId() {
        return projectID + "/" + Math.abs(newTeamDataDir.hashCode());
    }
    private void writeMoveMsgVal(Writer out, String name, String value)
            throws IOException {
        out.write("    <" + name + ">");
        out.write(escXml(value));
        out.write("</" + name + ">\n");
    }
    private String escXml(String s) {
        return s == null ? "" : XMLUtils.escapeAttribute(s);
    }



    private static final String LOCK_FILE = "teamProject.lock";
    private static final String MARKER_FILE =
        TeamDataConstants.OBSOLETE_DIR_MARKER_FILENAME;
    private static final String MOVE_FILE_NAME = "moved-data.pdash";
    private static final String XML_HEADER =
        "<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>\n";

}
