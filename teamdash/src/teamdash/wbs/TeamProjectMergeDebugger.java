// Copyright (C) 2012 Tuma Solutions, LLC
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

package teamdash.wbs;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.TempFileFactory;

public class TeamProjectMergeDebugger implements TeamProjectMergeListener {

    File debugDir;

    int debugId;


    public TeamProjectMergeDebugger() throws IOException {
        createDebugDir();
        this.debugId = 0;
    }

    protected TeamProjectMergeDebugger(boolean ignored) {
        this.debugId = 0;
    }

    protected void createDebugDir() throws IOException {
        if (debugDir != null)
            return;

        TempFileFactory tempFileFactory = new TempFileFactory("wbs-tmp-");
        tempFileFactory.useTempSubdirectory("pdash-tmp");
        this.debugDir = tempFileFactory.createTempDirectory("debugDir", ".tmp",
            true, true);
    }

    public boolean supportsZipOfAllMerges() {
        return true;
    }

    public File makeZipOfAllMerges() {
        return zipUpDirectory(debugDir);
    }

    public File makeZipOfCurrentMerge() {
        return zipUpDirectory(getCurrentMergeDumpDirectory());
    }

    public void mergeStarting() {
        debugId++;
    }

    public void mergeFinished() {}

    public void mergeDataNotify(String type, File srcDir) {
        File destDir = getMergeSubdir(type);
        File[] files = srcDir.listFiles();
        if (files != null) {
            for (File srcFile : files) {
                String filename = srcFile.getName();
                if (!filename.equals("teamProject.lock")) {
                    try {
                        File destFile = new File(destDir, filename);
                        FileUtils.copyFile(srcFile, destFile);
                    } catch (IOException ioe) {
                    }
                }
            }
        }
    }

    public void mergeDataNotify(String type, TeamProject teamProject) {
        if (teamProject != null) {
            File destDir = getMergeSubdir(type);
            teamProject.saveCopy(destDir);
        }
    }

    public void mergeException(Throwable t) {
        if (t != null) {
            try {
                File mergeDir = getCurrentMergeDumpDirectory();
                File file = new File(mergeDir, "00-exception.txt");
                FileWriter out = new FileWriter(file, true);
                PrintWriter pw = new PrintWriter(out);
                t.printStackTrace(pw);
                pw.println("------------------------------------------------");
                out.close();
            } catch (IOException e) {
            }
        }
    }

    private File getCurrentMergeDumpDirectory() {
        String identifier = Integer.toString(1000 + debugId).substring(1);
        return new File(debugDir, "merge-" + identifier);
    }

    private File getMergeSubdir(String type) {
        File dumpDir = getCurrentMergeDumpDirectory();
        File subdir = new File(dumpDir, type);
        subdir.mkdirs();
        return subdir;
    }

    private File zipUpDirectory(File baseDir) {
        try {
            return zipUpDirectoryImpl(baseDir);
        } catch (IOException ioe) {
            return null;
        }
    }

    private File zipUpDirectoryImpl(File baseDir) throws IOException {
        String filename = "pdash-WBS-dump-" + DATE_FMT.format(new Date()) + "-"
                + debugId + ".zip";
        File destZipFile = new File(System.getProperty("user.home"), filename);
        ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(
                new FileOutputStream(destZipFile)));

        for (String oneFile : FileUtils.listRecursively(baseDir, null)) {
            File srcFile = new File(baseDir, oneFile);
            zipOut.putNextEntry(new ZipEntry(oneFile));
            FileUtils.copyFile(srcFile, zipOut);
            zipOut.closeEntry();
        }

        zipOut.close();
        return destZipFile;
    }

    private static final DateFormat DATE_FMT = new SimpleDateFormat(
            "yyyy-MM-dd-HHmm");

}
