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
import java.util.List;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.team.setup.TeamProjectUtils;
import net.sourceforge.processdash.team.setup.TeamProjectUtils.ProjectType;
import net.sourceforge.processdash.tool.export.mgr.ExternalResourceManager;

public class CloudStorageDatasetMigrator {

    private DashboardContext ctx;

    CloudStorageDatasetMigrator(DashboardContext ctx) {
        this.ctx = ctx;
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

    public void setDestDirectory(String destDir) {
        String dd = (destDir == null ? "" : destDir.trim());
        if (dd.endsWith("/") || dd.endsWith("\\"))
            dd = dd.substring(0, dd.length() - 1);
        _destDirectory = dd;
        putValue(SETUP, DEST_DIR, _destDirectory);
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

}
