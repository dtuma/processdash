// Copyright (C) 2008 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.bridge.impl;

import java.io.File;
import java.io.FilenameFilter;

import net.sourceforge.processdash.util.DashboardBackupFactory;
import net.sourceforge.processdash.util.DirectoryBackup;

public class TeamDataDirStrategy implements FileResourceCollectionStrategy {

    public static TeamDataDirStrategy INSTANCE = new TeamDataDirStrategy();

    private TeamDataDirStrategy() {}

    public DirectoryBackup getBackupHandler(File directory) {
        return DashboardBackupFactory.getWbsBackup(directory);
    }

    public FilenameFilter getFilenameFilter() {
        return TEAM_DATA_DIR_FILE_FILTER;
    }

    public String getLockFilename() {
        return "teamProject.lock";
    }

    // The WBS_FILE_FILTER supplied by the dashboard backup factory is only
    // designed to pick up the *core* files that store WBS data (for backup
    // purposes).  Of course, we need to give our clients access to the other
    // files in the directory as well - like the pdash files and settings.xml.
    private static class TeamDataDirFileFilter implements FilenameFilter {

        public boolean accept(File dir, String name) {
            // we accept all of the core WBS files, of course!
            if (DashboardBackupFactory.WBS_FILE_FILTER.accept(dir, name))
                return true;

            name = name.toLowerCase();
            if (name.indexOf('/') != -1)
                return false; // don't support subdirectories!

            if (name.equalsIgnoreCase(TeamServerPointerFile.FILE_NAME))
                return false; // don't copy around any teamServer file!

            return (name.endsWith("-data.pdash")
                    || name.endsWith(".xml")
                    || name.equals("user-settings.ini"));
        }

    }
    private static final FilenameFilter TEAM_DATA_DIR_FILE_FILTER = new TeamDataDirFileFilter();

}
