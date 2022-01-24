// Copyright (C) 2008-2022 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge.impl;

import java.io.File;
import java.io.FilenameFilter;

import net.sourceforge.processdash.tool.bridge.bundle.FileBundleConstants;
import net.sourceforge.processdash.tool.bridge.bundle.FileBundleRetentionGranularity;
import net.sourceforge.processdash.util.DashboardBackupFactory;
import net.sourceforge.processdash.util.DirectoryBackup;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.PatternList;

public class DashboardInstanceStrategy implements
        FileResourceCollectionStrategy {

    public static final DashboardInstanceStrategy INSTANCE =
            new DashboardInstanceStrategy();

    public static final String LOCK_FILE_NAME = "dashlock.txt";

    private DashboardInstanceStrategy() {}

    public DirectoryBackup getBackupHandler(File directory) {
        return DashboardBackupFactory.getDashboardBackup(directory);
    }

    public FilenameFilter getFilenameFilter() {
        return DASHBOARD_FILE_FILTER;
    }

    public String getLockFilename() {
        return LOCK_FILE_NAME;
    }

    public FilenameFilter getUnlockedFilter() {
        return UNLOCKED_DASHBOARD_FILE_FILTER;
    }

    public boolean isFilePossiblyCorrupt(File file) {
        String name = file.getName().toLowerCase();

        if ((Character.isDigit(name.charAt(0)) && name.endsWith(".dat"))
                || name.equals("global.dat"))
            // our data files always start with a #include directive
            return !FileUtils.fileContentsStartWith(file, "UTF-8", "#include ");

        else if (name.equals("state") || name.endsWith(".xml"))
            // the timelog and state files start with an XML prolog
            return !FileUtils.fileContentsStartWith(file, "UTF-8", "<?xml ");

        else if (name.equals("pspdash.ini"))
            // the pspdash.ini file begins with a specific comment
            return !FileUtils.fileContentsStartWith(file, "UTF-8",
                "# User preferences for the PSP Dashboard tool");

        else
            // No checks at this time for corrupt defect logs or other files
            return false;
    }

    public Object[][] getBundlePartitions() {
        return new Object[][] {
                // metadata bundle for static and slowly changing files
                { "meta", "datasetid.dat", //
                        "groups.dat", "roles.dat", "users.dat", //
                        new PatternList() //
                                .addLiteralStartsWith("cms/")
                                .addLiteralStartsWith("import/")
                                .addLiteralEndsWith(".ico") },

                // core data files
                { "core", "state", "pspdash.ini", ".pspdash",
                        FileBundleConstants.CATCH_ALL_PARTITION },

                // time log files
                { "time", "timelog.xml", "timelog2.xml", "time.log" },

                // EV baseline files
                { FileBundleConstants.SINGLETON_PARTITION,
                        new PatternList("^ev-.+\\.dat$") },

                // project data & defect files
                { "data", FileBundleConstants.QUALIFIED_PARTITION,
                        new PatternList() //
                                .addLiteralEndsWith(".dat") //
                                .addLiteralEndsWith(".def") },
        };
    }

    public FileBundleRetentionGranularity getDefaultRetentionGranularity() {
        return FileBundleRetentionGranularity.Hourly;
    }

    private static class UnlockedDashboardFileFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            // accept the "import" subdirectory, along with any .PDASH and
            // messages.xml files it contains.
            return name.startsWith("import/")
                    && name.lastIndexOf('/') == 6
                    && (name.length() == 7
                            || name.endsWith("/messages.xml")
                            || name.toLowerCase().endsWith(".pdash"));
        }
    }
    private static final FilenameFilter UNLOCKED_DASHBOARD_FILE_FILTER = new UnlockedDashboardFileFilter();

    private static class DashboardFileFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return DashboardBackupFactory.DASH_FILE_FILTER.accept(dir, name)
                    || UNLOCKED_DASHBOARD_FILE_FILTER.accept(dir, name);
        }
    }
    private static final FilenameFilter DASHBOARD_FILE_FILTER = new DashboardFileFilter();

}
