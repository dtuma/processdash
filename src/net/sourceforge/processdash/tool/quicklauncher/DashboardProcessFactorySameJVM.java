// Copyright (C) 2007-2020 Tuma Solutions, LLC
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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.processdash.ProcessDashboard;

public class DashboardProcessFactorySameJVM extends DashboardProcessFactory {

    private boolean hasBeenUsed = false;

    public Process launchDashboard(File pspdataDir, List extraVmArgs,
            List extraArgs) throws Exception {
        if (hasBeenUsed)
            throw new IllegalStateException(resources
                    .getString("Errors.Cannot_Reuse_JVM"));
        hasBeenUsed = true;

        processVmArgs(this.vmArgs);
        processVmArgs(extraVmArgs);
        ProcessDashboard.main(getArgv(pspdataDir, extraArgs));

        return null;
    }

    private void processVmArgs(Collection args) {
        if (args == null || args.isEmpty())
            return;

        for (Iterator i = args.iterator(); i.hasNext();) {
            String arg = (String) i.next();
            Matcher m = SYS_PROP_PATTERN.matcher(arg);
            if (m.matches()) {
                String key = m.group(1);
                String value = m.group(2);
                if (System.getProperty(key) == null)
                    System.setProperty(key, value);
            }
        }
    }

    private static final Pattern SYS_PROP_PATTERN = Pattern
            .compile("-D([^=]+)=(.*)");

    private String[] getArgv(File pspdataDir, List extraArgs) {
        String locationArg = "-location=" + pspdataDir;
        if (extraArgs == null || extraArgs.isEmpty())
            return new String[] { locationArg };

        int argLen = extraArgs.size();
        String[] result = new String[argLen + 1];
        result[0] = locationArg;
        for (int i = 0; i < argLen; i++)
            result[i + 1] = (String) extraArgs.get(i);
        return result;
    }

    @Override
    public Process launchWBS(File wbsFile, List extraVmArgs, List extraArgs)
            throws Exception {
        throw new IllegalStateException(
                "Same-JVM mode does not support the launching of WBS files");
    }

    @Override
    public Process launchJnlp(File jnlpFile, List extraVmArgs, List extraArgs)
            throws Exception {
        throw new IllegalStateException(
                "Same-JVM mode does not support the launching of JNLP files");
    }

}
