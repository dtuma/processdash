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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.team.setup.RepairImportInstruction;
import net.sourceforge.processdash.team.setup.TeamProjectUtils;
import net.sourceforge.processdash.team.setup.TeamProjectUtils.ProjectType;
import net.sourceforge.processdash.tool.bridge.bundle.BundledWorkingDirectory;

public class CloudStorageCleanupWorker {

    static final String SETTING = //
            CloudStorageCleanupWorker.class.getName() + ".enabled";


    public static void maybeRun(DashboardContext ctx) {
        // if this worker is enabled / armed, and we're running against the new
        // bundled working directory, perform a cleanup
        if (Settings.getBool(SETTING, false) && !Settings.isReadOnly()
                && ctx.getWorkingDirectory() instanceof BundledWorkingDirectory)
            new CloudStorageCleanupWorker(ctx).run();
    }



    private DashboardContext ctx;

    private File targetDir;

    private Logger logger;

    private Map<String, ProjectType> projects;

    private CloudStorageCleanupWorker(DashboardContext ctx) {
        this.ctx = ctx;
        this.targetDir = ctx.getWorkingDirectory().getTargetDirectory();
        this.logger = Logger.getLogger(getClass().getName());
    }



    private void run() {
        // log a message about our work
        logger.info("Starting post-cloud-migration cleanup");

        // find all team/master projects in this dashboard
        this.projects = new HashMap();
        scanForProjects(ctx.getHierarchy(), PropertyKey.ROOT);

        // update all projects to use "." as their team directory
        makeTeamDirectoriesRelative();

        // rewrite settings.xml files for master projects and their subprojects
        fixMasterProjectLinks();

        // delete data elements left over from the previous dataset
        cleanupDataElements();

        // clear the flag indicating that cleanup is needed
        InternalSettings.set(SETTING, null);

        // log a message about successful completion
        logger.info("Finished post-cloud-migration cleanup");
    }



    private void scanForProjects(DashHierarchy hier, PropertyKey node) {
        ProjectType type = TeamProjectUtils.getProjectType(ctx, node);
        if (type == ProjectType.Team || type == ProjectType.Master) {
            logger.info("Found " + type + " project " + node.path());
            projects.put(node.path(), type);

        } else if (type == null) {
            for (int i = hier.getNumChildren(node); i-- > 0;) {
                scanForProjects(hier, hier.getChildKey(node, i));
            }
        }
    }



    private void makeTeamDirectoriesRelative() {
        for (String projectPrefix : projects.keySet())
            makeTeamDirectoriesRelative(projectPrefix);
    }

    private void makeTeamDirectoriesRelative(String projectPrefix) {
        // If the team directory is already ".", no changes are needed
        DataContext data = ctx.getData().getSubcontext(projectPrefix);
        String teamDir = getDataValue(data, TDC.TEAM_DIRECTORY);
        String teamDirUNC = getDataValue(data, TDC.TEAM_DIRECTORY_UNC);
        if (".".equals(teamDir) && teamDirUNC == null)
            return;

        // get the ID of this project, and make sure its data directory is
        // present in the expected location
        String projectID = getDataValue(data, TDC.PROJECT_ID);
        File dataDir = new File(targetDir, "data/" + projectID);
        if (!dataDir.isDirectory()) {
            logger.warning("No relative team dir found for project " //
                    + projectPrefix);
            return;
        }

        // update the team data directory to be "."
        logger.info("Setting relative team dir for project " + projectPrefix);
        saveDataValue(data, TDC.TEAM_DIRECTORY, ".");
        saveDataValue(data, TDC.TEAM_DIRECTORY_UNC, null);
        saveDataValue(data, CloudStorageProjectWorker.CLOUD_DIRECTORY, null);
        ctx.getData().waitForCalculations();

        // fix the global imports to reflect the new relative directory
        RepairImportInstruction.maybeRepairForTeam(data);
    }



    private void fixMasterProjectLinks() {
        for (Entry<String, ProjectType> e : projects.entrySet()) {
            if (e.getValue() == ProjectType.Master)
                fixMasterProjectLinks(e.getKey());
        }
    }

    private void fixMasterProjectLinks(String projectPrefix) {
        DataContext data = ctx.getData().getSubcontext(projectPrefix);
        String processID = getDataValue(data, TDC.PROCESS_ID);
        try {
            MoveProjectWorker.repairMasterProjectLinks(projectPrefix,
                processID);
            logger.info("Repaired links for master project " + projectPrefix);
        } catch (Exception e) {
            logger.info("Could not repair master links for " + projectPrefix);
        }
    }



    private void cleanupDataElements() {
        // delete the temporary path value set by the migrator
        CloudStorageDatasetMigrator.clearDestDirectory(ctx.getData());

        // clear the list of historically exported PDASH files, so the export
        // janitor doesn't try to delete them
        ctx.getData().putValue("/Historically_Exported_Filenames", null);
    }




    private String getDataValue(DataContext data, String dataName) {
        SimpleData sd = data.getSimpleValue(dataName);
        return (sd != null && sd.test() ? sd.format() : null);
    }

    private void saveDataValue(DataContext data, String name, String value) {
        data.putValue(name, value == null ? null : StringData.create(value));
    }

    private interface TDC extends TeamDataConstants {
    }

}
