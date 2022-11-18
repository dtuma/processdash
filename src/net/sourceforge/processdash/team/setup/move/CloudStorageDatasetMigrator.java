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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.Adler32;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.team.setup.TeamProjectUtils;
import net.sourceforge.processdash.team.setup.TeamProjectUtils.ProjectType;
import net.sourceforge.processdash.tool.bridge.bundle.BundledWorkingDirectory;
import net.sourceforge.processdash.tool.bridge.bundle.FileBundleUtils;
import net.sourceforge.processdash.tool.bridge.client.BridgedWorkingDirectory;
import net.sourceforge.processdash.tool.bridge.client.CompressedWorkingDirectory;
import net.sourceforge.processdash.tool.bridge.client.LocalWorkingDirectory;
import net.sourceforge.processdash.tool.bridge.client.WorkingDirectory;
import net.sourceforge.processdash.tool.bridge.impl.FileResourceCollectionStrategy;
import net.sourceforge.processdash.tool.bridge.impl.SyncClientMappings;
import net.sourceforge.processdash.tool.export.mgr.ExternalResourceManager;
import net.sourceforge.processdash.tool.export.mgr.FolderMappingManager;
import net.sourceforge.processdash.tool.quicklauncher.CompressedInstanceLauncher;
import net.sourceforge.processdash.tool.quicklauncher.SimpleInternalLauncher;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.StringUtils;

public class CloudStorageDatasetMigrator {

    private DashboardContext ctx;

    CloudStorageDatasetMigrator(DashboardContext ctx) {
        this.ctx = ctx;
    }



    /**
     * Make sure the runtime characteristics of the enclosing dashboard are
     * appropriate for a migration operation.
     * 
     * @throws MoveProjectException
     *             if any blocking problems are present
     */
    public void validateRuntimePreconditions() throws MoveProjectException {
        // cloud storage is only supported for team dashboards
        if (!Settings.isTeamMode())
            throw new MoveProjectException("notTeamMode").fatal();

        // cannot migrate in read-only mode
        if (Settings.isReadOnly())
            throw new MoveProjectException("readOnlyMode").fatal();

        // do not migrate if we're viewing a data backup
        WorkingDirectory wd = ctx.getWorkingDirectory();
        if (CompressedInstanceLauncher.isRunningFromCompressedData()
                || wd instanceof CompressedWorkingDirectory)
            throw new MoveProjectException("zipDataset").fatal();

        // only local working directories can be migrated
        if (!LocalWorkingDirectory.class.equals(wd.getClass())) {
            MoveProjectException mpe = new MoveProjectException("badWDirType")
                    .append("workingDirDescr", wd.getDescription()).fatal();
            if (wd instanceof BridgedWorkingDirectory)
                mpe.append("pdes", "t");
            else if (wd instanceof BundledWorkingDirectory)
                mpe.append("bundled", "t");
            throw mpe;
        }
    }



    /**
     * Perform advance checks on all source data to ensure it can be migrated
     */
    public void validateSourceData(boolean quick) throws MoveProjectException {
        getDashboardWorker().validateSourceData(quick);
        for (CloudStorageProjectWorker w : getProjectWorkers())
            w.validateSourceData(quick);
    }

    static void validateSourceData(File dir, boolean quick,
            FileResourceCollectionStrategy strategy, String... requiredFiles)
            throws MoveProjectException {
        // verify the directory is not currently bundled
        if (FileBundleUtils.isBundledDir(dir))
            throw new MoveProjectException("directoryBundled") //
                    .append("path", dir.getAbsolutePath()).fatal();

        // make sure the source directory is reachable. (It could be temporarily
        // unavailable due to network issues, for example.)
        if (!dir.isDirectory())
            throw new MoveProjectException("directoryUnreachable") //
                    .append("path", dir.getAbsolutePath());

        // verify the directory has the contents we expect. (If not, a network
        // problem could be preventing us from reading the dir contents.)
        for (String filename : requiredFiles) {
            File f = new File(dir, filename);
            if (!f.isFile())
                throw new MoveProjectException("directoryUnreachable") //
                        .append("path", dir.getAbsolutePath()) //
                        .append("filename", filename);
        }

        // ensure readability of all the files
        List<String> files = FileUtils.listRecursively(dir,
            strategy.getFilenameFilter());
        for (String filename : files) {
            File f = new File(dir, filename);
            boolean canRead = f.canRead();
            if (canRead && !quick) {
                try {
                    FileUtils.computeChecksum(f, new Adler32());
                } catch (Exception ioe) {
                    canRead = false;
                }
            }
            if (!canRead)
                throw new MoveProjectException("cannotReadFile") //
                        .append("path", f.getPath());
        }
    }



    //
    // Methods relating to the destination directory
    //

    private String _destDirectory;

    public String getDestDirectory() {
        if (_destDirectory == null)
            _destDirectory = getValue(SETUP, DEST_DIR);
        if (_destDirectory == null)
            _destDirectory = "";
        return _destDirectory;
    }

    public void setDestDirectory(String destDir, String destConfirm) {
        String dd = (destDir == null ? "" : destDir.trim());
        if (dd.endsWith("/") || dd.endsWith("\\"))
            dd = dd.substring(0, dd.length() - 1);
        _destDirectory = dd;
        putValue(SETUP, DEST_DIR, _destDirectory);
        putValue(SETUP, DEST_CONFIRM, destConfirm);
    }

    public static void clearDestDirectory(DataRepository data) {
        String dataName = DataRepository.createDataName(SETUP, DEST_DIR);
        data.putValue(dataName, null);
    }


    /** Check the suitability of the destination directory */
    public void validateDestDirectory() throws MoveProjectException {
        // make sure the user entered a directory
        String dirPath = getDestDirectory();
        if (!StringUtils.hasValue(dirPath))
            throw new MoveProjectException("destDirMissing");

        // make sure we were given an absolute path
        File dir = new File(dirPath);
        if (!dir.isAbsolute())
            throw new MoveProjectException("destDirNotAbsolute");

        // make sure the directory is under a [Shared Folder]
        SyncClientMappings.initialize(dir);
        String encoded = FolderMappingManager.getInstance().encodePath(dirPath);
        if (!FolderMappingManager.isEncodedPath(encoded))
            throw new MoveProjectException("destDirNotShared");

        // make sure the parent directory exists
        File dirParent = dir.getParentFile();
        if (dirParent == null || !dirParent.isDirectory())
            throw new MoveProjectException("destParentUnreachable");

        // ask the dashboard worker to validate the destination directory
        String confirmedPath = getValue(SETUP, DEST_CONFIRM);
        boolean contentsAllowed = dirPath.equals(confirmedPath);
        getDashboardWorker().validateDestDirectory(!contentsAllowed);
    }



    //
    // Methods relating to the migrator for core dashboard data
    //

    private CloudStorageDashboardWorker _dashboardWorker;

    private CloudStorageDashboardWorker getDashboardWorker() {
        if (_dashboardWorker == null)
            _dashboardWorker = buildDashboardWorker();
        return _dashboardWorker;
    }

    private CloudStorageDashboardWorker buildDashboardWorker() {
        File oldDir = ctx.getWorkingDirectory().getDirectory();
        File newDir = new File(getDestDirectory());
        return new CloudStorageDashboardWorker(oldDir, newDir);
    }



    //
    // Methods relating to the migrators for team projects
    //

    private List<CloudStorageProjectWorker> _projectWorkers;

    private List<CloudStorageProjectWorker> getProjectWorkers() {
        if (_projectWorkers == null)
            _projectWorkers = buildProjectWorkers();
        return _projectWorkers;
    }

    private List<CloudStorageProjectWorker> buildProjectWorkers() {
        List<CloudStorageProjectWorker> result = new ArrayList();
        buildProjectWorkers(result, PropertyKey.ROOT);
        return result;
    }

    private void buildProjectWorkers(List<CloudStorageProjectWorker> result,
            PropertyKey node) {
        ProjectType type = TeamProjectUtils.getProjectType(ctx, node);
        if (type != null) {
            CloudStorageProjectWorker pw = makeProjectWorker(node.path(), type);
            if (pw != null)
                result.add(pw);

        } else {
            for (int i = ctx.getHierarchy().getNumChildren(node); i-- > 0;) {
                PropertyKey child = ctx.getHierarchy().getChildKey(node, i);
                buildProjectWorkers(result, child);
            }
        }
    }

    private CloudStorageProjectWorker makeProjectWorker(String projectPath,
            ProjectType type) {
        // identify whether this is a team or master project. Abort if neither
        boolean isMaster;
        if (type == ProjectType.Team)
            isMaster = false;
        else if (type == ProjectType.Master)
            isMaster = true;
        else
            return null;

        // if this project is hosted on an enterprise server, it doesn't need
        // to be migrated to cloud storage
        String teamDirURL = getValue(projectPath, TDC.TEAM_DATA_DIRECTORY_URL);
        if (teamDirURL != null)
            return null;

        // get project ID and location
        String projectID = getValue(projectPath, TDC.PROJECT_ID);
        String projectDir = getValue(projectPath, TDC.TEAM_DIRECTORY_EFF);
        String remappedProjectDir;
        if (".".equals(projectDir))
            remappedProjectDir = ctx.getWorkingDirectory().getTargetDirectory()
                    .getPath();
        else
            remappedProjectDir = ExternalResourceManager.getInstance()
                    .remapFilename(projectDir);

        // create the worker and return it
        CloudStorageProjectWorker result = new CloudStorageProjectWorker(ctx,
                projectPath, projectID, isMaster, remappedProjectDir,
                getDestDirectory());
        return result;
    }



    /**
     * Perform the migration of this dashboard dataset, including core data and
     * all of the team projects inside.
     * 
     * @throws MoveProjectException
     *             if an error occurs during migration
     */
    public void run() throws MoveProjectException {
        CloudStorageDashboardWorker dashboardWorker = getDashboardWorker();
        List<CloudStorageProjectWorker> projectWorkers = getProjectWorkers();

        for (CloudStorageProjectWorker oneProject : projectWorkers) {
            oneProject.run();
        }

        dashboardWorker.run();

        for (CloudStorageProjectWorker oneProject : projectWorkers) {
            oneProject.finish();
        }
    }



    /**
     * Attempt to launch the dashboard, pointing at the destination directory
     */
    public boolean launchNewDashboard(int startupPause) {
        try {
            // launch the dashboard, pointing at the new directory. (We request
            // a startup pause so the current process has time to shut down and
            // relinquish its web server port before the new server starts.)
            File destDir = new File(getDestDirectory());
            SimpleInternalLauncher.launchDashboardForDir(destDir,
                Arrays.asList("-Dprocessdash.startupPause=" + startupPause),
                null);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }



    //
    // Utility methods
    //

    private String getValue(String prefix, String name) {
        String dataName = DataRepository.createDataName(prefix, name);
        SimpleData sd = ctx.getData().getSimpleValue(dataName);
        return (sd != null && sd.test() ? sd.format() : null);
    }

    private void putValue(String prefix, String name, String value) {
        String dataName = DataRepository.createDataName(prefix, name);
        SimpleData sd = (value == null ? null : StringData.create(value));
        ctx.getData().putValue(dataName, sd);
    }



    private interface TDC extends TeamDataConstants {
    }

    private static final String SETUP = "/Cloud_Storage";

    private static final String DEST_DIR = "Dest_Directory";

    private static final String DEST_CONFIRM = DEST_DIR + "//Confirm";

}
