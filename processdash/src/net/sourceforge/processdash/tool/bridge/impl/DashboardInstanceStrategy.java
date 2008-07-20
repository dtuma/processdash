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

package net.sourceforge.processdash.tool.bridge.impl;

import java.io.File;
import java.io.FilenameFilter;

import net.sourceforge.processdash.util.DashboardBackupFactory;
import net.sourceforge.processdash.util.DirectoryBackup;

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
        return DashboardBackupFactory.DASH_FILE_FILTER;
    }

    public String getLockFilename() {
        return LOCK_FILE_NAME;
    }

    public FilenameFilter getUnlockedFilter() {
        return null;
    }

}
