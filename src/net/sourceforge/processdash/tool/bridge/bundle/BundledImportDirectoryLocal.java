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
import java.io.IOException;

import net.sourceforge.processdash.tool.bridge.client.DirectoryPreferences;
import net.sourceforge.processdash.tool.bridge.client.ImportDirectory;
import net.sourceforge.processdash.tool.bridge.impl.TeamDataDirStrategy;

public class BundledImportDirectoryLocal implements ImportDirectory {

    private BundledWorkingDirectoryLocal workingDir;

    protected long lastUpdateTime;

    public BundledImportDirectoryLocal(File dir) {
        this.workingDir = BundledWorkingDirectoryLocal.create(dir, STRATEGY,
            DirectoryPreferences.getMasterWorkingDirectory(), true);
        this.lastUpdateTime = -1;
    }

    public String getDescription() {
        return workingDir.getDescription();
    }

    public File getDirectory() {
        return workingDir.getDirectory();
    }

    public String getRemoteLocation() {
        return null;
    }

    public void update() throws IOException {
        // for the very first update, ensure the working directory has been
        // created and prepared
        if (lastUpdateTime < 0) {
            workingDir.getDirectory().mkdir();
            workingDir.prepare();
            lastUpdateTime = 0;
        }

        // this method may get called overzealously by code in different layers
        // of the application. If it is called more than once within a few
        // milliseconds, don't repeat the update.
        long now = System.currentTimeMillis();
        long lastUpdateAge = now - lastUpdateTime;
        if (lastUpdateAge > 1000 || lastUpdateAge < 0) {
            workingDir.update();
            lastUpdateTime = System.currentTimeMillis();
        }
    }

    private static final TeamDataDirStrategy STRATEGY = TeamDataDirStrategy.INSTANCE;

}
