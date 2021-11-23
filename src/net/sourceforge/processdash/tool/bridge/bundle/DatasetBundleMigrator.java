// Copyright (C) 2021 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge.bundle;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.team.setup.TeamProjectUtils;
import net.sourceforge.processdash.team.setup.TeamProjectUtils.ProjectType;
import net.sourceforge.processdash.tool.bridge.client.LocalWorkingDirectory;
import net.sourceforge.processdash.tool.bridge.client.WorkingDirectory;
import net.sourceforge.processdash.tool.bridge.impl.DashboardInstanceStrategy;
import net.sourceforge.processdash.tool.bridge.impl.FileResourceCollectionStrategy;
import net.sourceforge.processdash.tool.bridge.impl.TeamDataDirStrategy;
import net.sourceforge.processdash.tool.bridge.impl.TeamServerPointerFile;
import net.sourceforge.processdash.tool.export.mgr.ExternalResourceManager;

public class DatasetBundleMigrator {

    private DashboardContext ctx;

    private Map<File, Throwable> errors;

    public DatasetBundleMigrator(DashboardContext ctx) {
        this.ctx = ctx;
        this.errors = new LinkedHashMap<File, Throwable>();
    }


    public Map<File, Throwable> getErrors() {
        return errors;
    }


    public Runnable getDatasetMigrationTask() {
        WorkingDirectory working = ctx.getWorkingDirectory();
        if (working instanceof LocalWorkingDirectory
                && !(working instanceof BundledWorkingDirectory)) {
            return new MigrationTask("Bundling dashboard data directory",
                    working.getDirectory(), DashboardInstanceStrategy.INSTANCE);
        } else {
            return null;
        }
    }


    public List<Runnable> getProjectMigrationTasks() {
        List<Runnable> tasks = new ArrayList<Runnable>();
        getProjectMigrationTasks(tasks, ctx.getHierarchy(), PropertyKey.ROOT);
        return tasks;
    }

    private void getProjectMigrationTasks(List<Runnable> tasks,
            DashHierarchy hier, PropertyKey key) {
        ProjectType type = TeamProjectUtils.getProjectType(ctx, key);
        if (type == ProjectType.Master //
                || type == ProjectType.Team //
                || type == ProjectType.Personal) {
            Runnable task = getProjectMigrationTask(key.path());
            if (task != null)
                tasks.add(task);

        } else if (type == null) {
            for (int i = 0; i < hier.getNumChildren(key); i++) {
                PropertyKey child = hier.getChildKey(key, i);
                getProjectMigrationTasks(tasks, hier, child);
            }
        }
    }

    private Runnable getProjectMigrationTask(String projectPath) {
        // if this project has been migrated to a team server, do nothing
        String projectUrl = getString(projectPath,
            TeamDataConstants.TEAM_DATA_DIRECTORY_URL);
        if (projectUrl != null)
            return null;

        // get the directory where this project stores data
        String projectDirPath = getString(projectPath,
            TeamDataConstants.TEAM_DATA_DIRECTORY);
        projectDirPath = ExternalResourceManager.getInstance()
                .remapFilename(projectDirPath);
        File projectDir = new File(projectDirPath);

        // if the directory contains a team server pointer file, do nothing
        File pointer = new File(projectDir, TeamServerPointerFile.FILE_NAME);
        if (pointer.isFile())
            return null;

        // if the directory is already bundled, do nothing
        if (FileBundleUtils.isBundledDir(projectDir))
            return null;

        return new MigrationTask(projectPath, projectDir,
                TeamDataDirStrategy.INSTANCE);
    }

    private String getString(String project, String dataName) {
        dataName = DataRepository.createDataName(project, dataName);
        SimpleData sd = ctx.getData().getSimpleValue(dataName);
        return (sd == null || !sd.test() ? null : sd.format());
    }



    private class MigrationTask implements Runnable {

        private String name;

        private File directory;

        private FileResourceCollectionStrategy strategy;

        protected MigrationTask(String name, File directory,
                FileResourceCollectionStrategy strategy) {
            this.name = name;
            this.directory = directory;
            this.strategy = strategy;
        }

        public void run() {
            try {
                FileBundleMigrator.migrate(directory, strategy);
            } catch (Exception e) {
                errors.put(directory, e);
            }
        }

        public String toString() {
            return name;
        }
    }

}
