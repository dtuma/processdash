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

}
