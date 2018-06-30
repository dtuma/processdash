// Copyright (C) 2018 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.launcher;

import java.io.File;

import net.sourceforge.processdash.tool.launcher.jnlp.DistributionManager;
import net.sourceforge.processdash.util.Bootstrap;

public class Scanner {

    public static void main(String[] args) {
        if (scanAndRunMostRecentLauncher(args) == false) {
            // if the scan fails, use the launching logic bundled into this JAR
            Main.main(args);
        }
    }

    private static boolean scanAndRunMostRecentLauncher(String[] args) {
        // skip the scan if requested by the user
        if (System.getProperty("noScan") != null)
            return false;

        try {
            // look for the most recently used distribution on this computer.
            // don't consider distributions that are older than our internal
            // implementation of the launching logic.
            String selfVersion = Scanner.class.getPackage()
                    .getImplementationVersion();
            File distr = DistributionManager
                    .getMostRecentlyUsedDistribution(selfVersion);
            if (distr == null)
                return false;

            File targetJarFile = new File(distr,
                    DistributionManager.TARGET_JARFILE);
            Bootstrap.launchMain(targetJarFile, Main.class.getName(), args);
            return true;

        } catch (Exception e) {
            return false;
        }
    }

}
