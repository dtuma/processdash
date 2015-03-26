// Copyright (C) 2008 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.macosx;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


public class MacDataCleanup {

    public static void main(String[] args) {
        try {
            // the user's data directory will be passed on the command line.
            String newDataDirName = args[0];
            File newDataDir = new File(newDataDirName);

            // if the user's data directory already has data in it, stop
            // immediately. We don't want to bother that existing data.
            if (containsDashboardData(newDataDir))
                return;

            // check to see if the user has an old version of the dashboard
            // installed, and whether it contains metrics data. If so,
            // possibly move the data files to the new location, then rename
            // the old installation directory
            File oldInstallDir = new File(OLD_INSTALL_DIR);
            if (maybeCleanup(oldInstallDir, newDataDir)) {
                File renamedDir = new File(OLD_INSTALL_DIR_RENAME);
                oldInstallDir.renameTo(renamedDir);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Return true if a particular directory appears to contain Process
     * Dashboard metrics data.
     * 
     * @param dir the directory to examine
     * @return true if the directory exists, and contains metrics data files
     */
    private static boolean containsDashboardData(File dir) {
        // if the file provided doesn't exist or isn't a directory, then it
        // definitely doesn't contain any dashboard data.
        if (dir.isDirectory() == false)
            return false;

        // Every dashboard data directory will contain a "global.dat" file and
        // a "state" file. If either file is present, return true.
        File globalDatFile = new File(dir, "global.dat");
        File stateFile = new File(dir, "state");
        if (globalDatFile.isFile() || stateFile.isFile())
            return true;
        else
            return false;
    }


    /**
     * Possibly move dashboard data files from one directory to another.
     * 
     * @param oldDataDir a directory which might contain data files
     * @param newDataDir a directory where data files should be placed
     * @return true if data files were found and moved from the old directory
     *     to the new directory.
     */
    private static boolean maybeCleanup(File oldDataDir, File newDataDir) {
        // if there isn't any data in the oldDataDir, return false.
        if (containsDashboardData(oldDataDir) == false)
            return false;

        // if the new dir doesn't exist and cannot be created, return false.
        if (!newDataDir.isDirectory() && !newDataDir.mkdirs())
            return false;

        // Iterate over the files in the old directory. Move each data file
        // into the new directory.
        File[] origFiles = oldDataDir.listFiles();
        for (int i = 0; i < origFiles.length; i++) {
            File orig = origFiles[i];
            String name = orig.getName();
            if (isDashboardDataFile(name)) {
                if (".pspdash".equals(name))
                    name = "pspdash.ini";
                File dest = new File(newDataDir, name);
                orig.renameTo(dest);
            }
        }
        return true;
    }


    /**
     * Return true if a given filename is one created by the dashboard.
     */
    private static boolean isDashboardDataFile(String filename) {
        String name = filename.toLowerCase();
        return (name.endsWith(".dat") || // include data files
                name.endsWith(".def") || // include defect logs
                DASH_FILE_NAMES.contains(name)); // include other files
    }

    private static final String OLD_INSTALL_DIR =
            "/Applications/Process Dashboard";

    private static final String OLD_INSTALL_DIR_RENAME =
            "/Applications/Process Dashboard (old version)";

    private static final Set DASH_FILE_NAMES = new HashSet<String>( //
            Arrays.asList(new String[] { ".pspdash", "pspdash.ini", "state",
                    "time.log", "timelog.xml", "timelog2.xml",
                    "cms", "backup", "log.txt", "defns.ser" }));
}
