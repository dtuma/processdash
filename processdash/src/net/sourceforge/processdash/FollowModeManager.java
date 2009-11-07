// Copyright (C) 2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.log.time.DashboardTimeLog;
import net.sourceforge.processdash.tool.bridge.client.WorkingDirectory;

public class FollowModeManager extends Thread {

    private WorkingDirectory workingDirectory;

    private DashHierarchy hierarchy;

    private File hierarchyFile;

    private long hierarchyTimestamp;

    private DashHierarchy templates;

    private DataRepository dataRepository;

    private DashboardTimeLog timeLog;

    private Logger logger = Logger.getLogger(FollowModeManager.class.getName());

    public FollowModeManager(WorkingDirectory workingDirectory,
            DashHierarchy hierarchy, File hierarchyFile,
            DashHierarchy templates, DataRepository dataRepository,
            DashboardTimeLog timeLog) {
        super("FollowModeManager");
        setDaemon(true);
        this.workingDirectory = workingDirectory;
        this.hierarchy = hierarchy;
        this.hierarchyFile = hierarchyFile;
        this.hierarchyTimestamp = hierarchyFile.lastModified();
        this.templates = templates;
        this.dataRepository = dataRepository;
        this.timeLog = timeLog;
        start();
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(REFRESH_DELAY);
                update();
            } catch (Exception e) {
                logger.log(Level.WARNING,
                    "Encountered problem during follow-mode update", e);
            }

        }
    }

    private void update() throws Exception {
        // update the contents of the working directory, if it is in bridged
        // mode
        workingDirectory.update();
        // if the working directory has become temporarily unavailable (for
        // example, due to network connectivity problems), don't attempt to
        // perform any updates.
        if (workingDirectory.getDirectory().isDirectory() == false)
            return;

        updateHierarchy();
        dataRepository.reloadModifiedDatafiles();
        timeLog.reloadData();
        InternalSettings.maybeReload();
    }

    private void updateHierarchy() throws Exception {
        if (!hierarchyFile.isFile())
            return;

        long newTimestamp = hierarchyFile.lastModified();
        if (newTimestamp == 0 || newTimestamp == hierarchyTimestamp)
            return;

        // load the new data from the hierarchy file
        DashHierarchy newHier = new DashHierarchy(hierarchy.dataPath);
        newHier.loadXML(hierarchyFile.getPath(), templates);
        // alter our current hierarchy to match the new changes
        DashController.getHierarchyAlterer().mergeChangesFrom(newHier);
        // update the timestamp to indicate success
        hierarchyTimestamp = newTimestamp;
    }

    private static final long REFRESH_DELAY = 60000; // one minute

}
