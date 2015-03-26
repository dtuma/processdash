// Copyright (C) 2006-2012 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.quicklauncher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sourceforge.processdash.tool.export.mgr.ExternalResourceManager;

class DirectoryInstanceLauncher extends DashboardInstance {

    private static final String EXT_RES_MGR_ARG = "-D"
            + ExternalResourceManager.INITIALIZATION_MODE_PROPERTY_NAME + "="
            + ExternalResourceManager.INITIALIZATION_MODE_AUTO;

    protected File pspdataDir;

    public DirectoryInstanceLauncher() {}

    public DirectoryInstanceLauncher(File dir) {
        setPspdataDir(dir);
    }

    public void setPspdataDir(File pspdataDir) {
        this.pspdataDir = pspdataDir;
        if (display == null)
            setDisplay(pspdataDir.getAbsolutePath());
    }

    public void launch(DashboardProcessFactory processFactory) {
        launchApp(processFactory, Collections.singletonList(EXT_RES_MGR_ARG),
                pspdataDir);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DirectoryInstanceLauncher) {
            DirectoryInstanceLauncher that = (DirectoryInstanceLauncher) obj;
            return eq(this.pspdataDir, that.pspdataDir);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return pspdataDir == null ? -1 : pspdataDir.hashCode();
    }

    static List getDataDirectoriesWithinDir(File dir) throws IOException {
        List result = new ArrayList();
        getDataDirectoriesWithinDir(result, dir);
        return result;
    }

    private static void getDataDirectoriesWithinDir(List result, File dir) {
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()
                    && files[i].getName().equals(DATA_DIR_FILE_ITEM))
                result.add(dir);
            else if (files[i].isDirectory())
                getDataDirectoriesWithinDir(result, files[i]);
        }
    }
}
