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

package net.sourceforge.processdash.tool.bridge.client;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

import net.sourceforge.processdash.tool.bridge.impl.DashboardInstanceStrategy;
import net.sourceforge.processdash.tool.bridge.impl.FileResourceCollectionStrategy;
import net.sourceforge.processdash.tool.bridge.impl.TeamDataDirStrategy;

public class WorkingDirectoryFactory {

    public static final int PURPOSE_DASHBOARD = 1;

    public static final int PURPOSE_WBS = 2;

    static final Logger logger = Logger.getLogger(WorkingDirectory.class
            .getName());

    private static final WorkingDirectoryFactory INSTANCE =
            new WorkingDirectoryFactory();

    public static WorkingDirectoryFactory getInstance() {
        return INSTANCE;
    }

    private WorkingDirectoryFactory() {}

    public WorkingDirectory get(String location, int purpose) {
        if (location == null)
            return get(new File(System.getProperty("user.dir")), purpose);
// NOT YET SUPPORTED
//        else if (location.startsWith("http"))
//            return get(new URL(location), purpose);
        else
            return get(new File(location), purpose);
    }

    public WorkingDirectory get(File targetDirectory, int purpose) {
        URL serverURL = TeamServerSelector.getServerURL(targetDirectory);
        String serverUrlStr = (serverURL == null ? null : serverURL.toString());
        return get(targetDirectory, serverUrlStr, purpose);
    }

    public WorkingDirectory get(URL url, int purpose) {
        return get(null, url.toString(), purpose);
    }

    private WorkingDirectory get(File targetDirectory, String remoteURL,
            int purpose) {

        FileResourceCollectionStrategy strategy = getStrategy(purpose);

        if (targetDirectory != null) {
            try {
                targetDirectory = targetDirectory.getCanonicalFile();
            } catch (IOException e) {
                targetDirectory = targetDirectory.getAbsoluteFile();
            }
        }

        if (remoteURL != null) {
            logger.info("Using bridged working directory via URL " + remoteURL);
            return new BridgedWorkingDirectory(targetDirectory, remoteURL,
                    strategy, DirectoryPreferences.getMasterWorkingDirectory());

        } else if (targetDirectory != null) {
            logger.info("Using local working directory "
                    + targetDirectory.getPath());
            return new LocalWorkingDirectory(targetDirectory, strategy,
                    DirectoryPreferences.getMasterWorkingDirectory());

        } else {
            throw new NullPointerException();
        }
    }

    private FileResourceCollectionStrategy getStrategy(int purpose) {
        if (purpose == PURPOSE_DASHBOARD)
            return DashboardInstanceStrategy.INSTANCE;
        else if (purpose == PURPOSE_WBS)
            return TeamDataDirStrategy.INSTANCE;
        else
            throw new IllegalArgumentException(
                    "Unrecognized working directory strategy");
    }

}
