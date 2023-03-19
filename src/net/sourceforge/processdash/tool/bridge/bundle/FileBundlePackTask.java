// Copyright (C) 2023 Tuma Solutions, LLC
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.team.setup.TeamProjectUtils;
import net.sourceforge.processdash.team.setup.TeamProjectUtils.ProjectType;
import net.sourceforge.processdash.tool.bridge.client.WorkingDirectory;
import net.sourceforge.processdash.tool.bridge.client.WorkingDirectoryFactory;
import net.sourceforge.processdash.util.DateUtils;

public class FileBundlePackTask {

    public enum Speed {
        ALL, NOW, DEFAULT, SLOW
    }


    private DashboardContext ctx;

    private Speed speed;

    private List<String> packedDirs;

    public FileBundlePackTask(DashboardContext ctx, Speed speed) {
        this.ctx = ctx;
        this.speed = speed;
        this.packedDirs = new ArrayList<String>();
    }

    public List<String> getPackedDirs() {
        return packedDirs;
    }


    public void run() throws IOException {
        // run pack logic against the main working directory
        run(ctx.getWorkingDirectory());

        // run pack logic against each of the projects owned by this dashboard
        scanForProjects(PropertyKey.ROOT);
    }

    private void scanForProjects(PropertyKey key) throws IOException {
        if (isOwnedProject(key)) {
            runForProject(key.path());
        } else {
            for (int i = ctx.getHierarchy().getNumChildren(key); i-- > 0;) {
                PropertyKey child = ctx.getHierarchy().getChildKey(key, i);
                scanForProjects(child);
            }
        }
    }

    private boolean isOwnedProject(PropertyKey key) {
        // return true if this key points to a project that is owned by this
        // team or personal dashboard
        ProjectType projectType = TeamProjectUtils.getProjectType(ctx, key);
        return projectType == ProjectType.Team //
                || projectType == ProjectType.Master //
                || projectType == ProjectType.Personal;
    }


    public void runForProject(String path) throws IOException {
        // get the filesystem path of the team data directory for this project
        String dataName = DataRepository.createDataName(path,
            TeamDataConstants.TEAM_DATA_DIRECTORY);
        SimpleData sd = ctx.getData().getSimpleValue(dataName);
        if (!sd.test())
            return;

        // run pack logic against the WorkingDirectory for this project. (The
        // disseminate subdir does not need packing because it does not retain
        // a large number of team-data.pdash files.)
        WorkingDirectory wd = WorkingDirectoryFactory.getInstance()
                .get(WorkingDirectoryFactory.PURPOSE_WBS, sd.format());
        run(wd);
    }


    public void run(WorkingDirectory wd) throws IOException {
        // do nothing unless this is a bundled working directory
        if (wd instanceof BundledWorkingDirectoryLocal)
            run((BundledWorkingDirectoryLocal) wd);
    }

    private void run(BundledWorkingDirectoryLocal bwd) throws IOException {
        // to reduce the chance that two different computers will pack bundles
        // simultaneously, we add randomness into the decision to pack. (This
        // randomness can be disabled by the use of "NOW" speed.)
        int rand = (speed == Speed.NOW ? 0 : new Random().nextInt(100));

        // decide how recently we should pack
        int oldBundleAge;
        int today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        if (speed == Speed.ALL)
            // we've been asked to pack everything through today
            oldBundleAge = 0;

        else if (speed != Speed.SLOW && today > 20 && rand < 20)
            // by default, wait until the 21st of the month, then use a 20%
            // probability to pack the bundles from the previous month. (Packing
            // of last month's bundles can be disabled via the "SLOW" speed.)
            oldBundleAge = today + 1;

        else if (rand < 20)
            // if it isn't time yet to pack last month's bundles, use a 20%
            // probability to to pack the bundles from the month before last.
            oldBundleAge = today + 32;

        else
            // if our probabilistic chance didn't occur, make no changes
            return;

        // ask our bundle directory to perform the pack operation
        long oldBundleTime = System.currentTimeMillis()
                - oldBundleAge * DateUtils.DAYS;
        bwd.packBundlesThroughMonth(oldBundleTime);
        packedDirs.add(bwd.getDescription());
    }



    public static Speed parseSpeedSetting(String setting, Speed defaultValue) {
        try {
            return Speed.valueOf(setting.toUpperCase());
        } catch (Exception e) {
            return defaultValue;
        }
    }


    public static class BG implements Runnable {

        private DashboardContext ctx;

        public void setDashboardContext(DashboardContext ctx) {
            this.ctx = ctx;
        }

        public void run() {
            try {
                // don't autopack in read-only mode
                if (Settings.isReadOnly())
                    return;

                // read the desired speed from a user setting
                Speed speed = parseSpeedSetting(
                    Settings.getVal("bundlePacker.speed"), Speed.DEFAULT);

                // run the pack operation
                new FileBundlePackTask(ctx, speed).run();

            } catch (IOException ioe) {
                System.err.println(
                    "Error performing background bundle packing operation:");
                ioe.printStackTrace();
            }
        }
    }

}
