// Copyright (C) 2022 Tuma Solutions, LLC
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

package net.sourceforge.processdash.team.setup.move;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.templates.DataVersionChecker;
import net.sourceforge.processdash.tool.bridge.bundle.FileBundleMigrator;
import net.sourceforge.processdash.tool.bridge.bundle.FileBundleMode;
import net.sourceforge.processdash.tool.bridge.impl.DashboardInstanceStrategy;
import net.sourceforge.processdash.tool.bridge.impl.SyncClientMappings;
import net.sourceforge.processdash.tool.bridge.impl.TeamServerPointerFile;
import net.sourceforge.processdash.tool.export.mgr.FolderMappingManager;

public class CloudStorageDashboardWorker {

    private File oldDirectory, newDirectory;

    CloudStorageDashboardWorker(File oldDirectory, File newDirectory) {
        this.oldDirectory = oldDirectory;
        this.newDirectory = newDirectory;
    }


    /**
     * Perform advance checks on the source dir to ensure it can be migrated
     */
    public void validateSourceData(boolean quick) throws MoveProjectException {
        CloudStorageDatasetMigrator.validateSourceData(oldDirectory, quick,
            DashboardInstanceStrategy.INSTANCE, "state", "pspdash.ini",
            "global.dat");
    }


    /**
     * Perform advance checks on the destination dir to ensure it is acceptable
     */
    public void validateDestDirectory(boolean requireEmpty)
            throws MoveProjectException {
        // if the new directory already exists, make sure it doesn't contain
        // any of the files/directories we'll be writing, or other dashboard
        // related content
        if (newDirectory.isDirectory() && !isDataAlreadyMigrated()) {
            validateDestFilesNotPresent("bundles", "heads", "global.dat",
                "pspdash.ini", "datasetID.dat", "state", "timelog.xml",
                TeamServerPointerFile.FILE_NAME,
                ProcessDashboard.DATA_MOVED_FILENAME,
                TeamDataConstants.OBSOLETE_DIR_MARKER_FILENAME);
            if (requireEmpty && newDirectory.list().length > 0)
                throw new MoveProjectException("destDirNotEmpty");
        }
    }

    private void validateDestFilesNotPresent(String... filenames) {
        for (String name : filenames) {
            File f = new File(newDirectory, name);
            if (f.exists())
                throw new MoveProjectException("destFileExists") //
                        .append("path", f.getPath());
        }
    }


    /**
     * Prepare and migrate data to bundled storage.
     * 
     * @throws MoveProjectException
     *             if an error is encountered
     */
    public void run() throws MoveProjectException {
        // migrate the data to the new directory
        migrateData();

        // write a file to indicate the data has been moved
        writeMoveFile();

        // write a file indicating the directory is obsolete
        writeMarkerFile();
    }


    private void migrateData() {
        // if the data was already migrated successfully, abort
        if (isDataAlreadyMigrated())
            return;

        // make a note that post-cloud-migration cleanup is needed, so this
        // flag will be written into the bundles we publish
        InternalSettings.set(CloudStorageCleanupWorker.SETTING, "true");

        // ensure all data is saved to disk
        DashController.saveAllData();

        // copy the files to the new directory, bundling them at the same time
        try {
            newDirectory.mkdirs();
            FileBundleMigrator.migrate(oldDirectory, newDirectory,
                DashboardInstanceStrategy.INSTANCE, FileBundleMode.Sync,
                SOURCE_DIR_IS_ALREADY_LOCKED_FLAG);

            // write a setting to the source dir noting that the copy succeeded
            InternalSettings.set(CLOUD_DIR_COMPLETE_SETTING,
                newDirectory.getAbsolutePath());

        } catch (Exception e) {
            logError("performing cloud storage migration", e);
            throw new MoveProjectException("bundleError") //
                    .append("path", newDirectory.getPath());

        } finally {
            // undo the post-cloud-migration cleanup flag we set before
            // bundling, so it is no longer present on the source directory
            InternalSettings.set(CloudStorageCleanupWorker.SETTING, null);
        }
    }

    private boolean isDataAlreadyMigrated() {
        return Settings.getVal(CLOUD_DIR_COMPLETE_SETTING, "")
                .equalsIgnoreCase(newDirectory.getAbsolutePath());
    }


    private void writeMoveFile() {
        File movedFile = getTargetFile(ProcessDashboard.DATA_MOVED_FILENAME);
        try {
            // convert the target into [Shared Folder] encoding if possible
            SyncClientMappings.initialize(newDirectory);
            String newLocation = FolderMappingManager.getInstance()
                    .encodePath(newDirectory.getAbsolutePath());
            String knownFileSearchQuery = getKnownFileSearchQuery();

            // write a data moved file with the new location
            Writer out = new FileWriter(movedFile);
            out.write(newLocation);
            out.write(knownFileSearchQuery);
            out.write(System.getProperty("line.separator"));
            out.close();

            // register the version needed for clients to understand this
            // "moved-data.txt" redirection file. If older clients try to open
            // this dir, they will be rejected with an "upgrade needed" message
            DataVersionChecker.registerDataRequirement("pspdash",
                ProcessDashboard.DATA_MOVED_MIN_VERSION);

        } catch (IOException ioe) {
            logError("writing move file", ioe);
            throw new MoveProjectException("cannotCreateFile") //
                    .append("path", movedFile.getPath());
        }
    }

    private String getKnownFileSearchQuery() {
        try {
            File bundleDir = new File(newDirectory, "bundles");
            for (File f : bundleDir.listFiles()) {
                if (f.getName().endsWith("-core.xml"))
                    return "?bundles/" + f.getName();
            }
        } catch (Exception e) {
        }
        return "";
    }


    private void writeMarkerFile() {
        try {
            File marker = getTargetFile(
                TeamDataConstants.OBSOLETE_DIR_MARKER_FILENAME);
            PrintWriter p = new PrintWriter(new FileWriter(marker));
            p.println("This directory is obsolete.  The data it contains");
            p.println("has been moved to the cloud storage directory");
            p.println(newDirectory.getAbsolutePath());
            p.println();
            p.println("The contents of this directory have been left intact");
            p.println("while affected users transition to the new directory.");
            p.println("After all users have transitioned, this directory");
            p.println("can be safely deleted.");
            p.flush();
            p.close();
        } catch (IOException e) {
            logError("writing marker file", e);
        }
    }


    private File getTargetFile(String name) {
        File result = new File(oldDirectory, name);
        if (result.exists())
            result.setWritable(true, false);
        return result;
    }


    protected void logError(String activity, Throwable t) {
        System.out.println("Error while " + activity);
        t.printStackTrace();
    }



    // name of a setting stating that files were successfully copied to a
    // specific cloud storage directory
    private static final String CLOUD_DIR_COMPLETE_SETTING = //
            CloudStorageDashboardWorker.class.getSimpleName() + ".migratedTo";

    // we don't need the FileBundleMigrator to lock the source directory,
    // because it's already locked by the running dashboard
    private static boolean SOURCE_DIR_IS_ALREADY_LOCKED_FLAG = false;

}
