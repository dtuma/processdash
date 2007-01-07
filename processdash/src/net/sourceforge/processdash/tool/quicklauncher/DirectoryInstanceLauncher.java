// Copyright (C) 2006 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC: processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.quicklauncher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class DirectoryInstanceLauncher extends DashboardInstance {

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
        launchApp(processFactory, Collections.EMPTY_LIST,
                pspdataDir);
    }

    public boolean equals(Object obj) {
        if (obj instanceof DirectoryInstanceLauncher) {
            DirectoryInstanceLauncher that = (DirectoryInstanceLauncher) obj;
            return eq(this.pspdataDir, that.pspdataDir);
        }

        return false;
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
