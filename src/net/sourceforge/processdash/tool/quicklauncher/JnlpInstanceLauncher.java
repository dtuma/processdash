// Copyright (C) 2020 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.quicklauncher;

import java.io.File;
import java.util.List;

public class JnlpInstanceLauncher extends DashboardInstance {

    private File jnlpFile;

    public JnlpInstanceLauncher(File jnlpFile) {
        this.jnlpFile = jnlpFile;
        setDisplay(jnlpFile.getAbsolutePath());
    }

    @Override
    protected String getLaunchedWindowTitle() {
        return null;
    }

    @Override
    public void launch(DashboardProcessFactory processFactory) {
        launchApp(processFactory, null, jnlpFile);
    }

    @Override
    protected Process createProcess(DashboardProcessFactory processFactory,
            File launchTarget, List extraVmArgs, List extraArgs)
            throws Exception {
        return processFactory.launchJnlp(launchTarget, extraVmArgs, extraArgs);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof JnlpInstanceLauncher) {
            JnlpInstanceLauncher that = (JnlpInstanceLauncher) obj;
            return this.jnlpFile.equals(that.jnlpFile);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return jnlpFile.hashCode();
    }

}
