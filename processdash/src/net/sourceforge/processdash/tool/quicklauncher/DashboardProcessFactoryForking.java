// Copyright (C) 2006-2007 Tuma Solutions, LLC
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
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.ui.systray.SystemTrayManagement;
import net.sourceforge.processdash.util.RuntimeUtils;

class DashboardProcessFactoryForking extends DashboardProcessFactory {

    private String jreExecutable;

    private String classpath;

    private String mainClassName;

    public DashboardProcessFactoryForking() throws Exception {
        jreExecutable = RuntimeUtils.getJreExecutable();
        if (jreExecutable == null)
            throw new Exception(resources.getString("Errors.Missing_JRE"));

        classpath = getSelfClasspath();
        if (classpath == null)
            throw new Exception(resources.getString("Errors.Missing_JAR"));

        mainClassName = ProcessDashboard.class.getName();

        // do not display tray icons for launched instances; otherwise, there
        // could be an unhelpful proliferation of identical icons
        addVmArg("-D" + Settings.SYS_PROP_PREFIX
                + SystemTrayManagement.DISABLED_SETTING + "=true");
    }

    public void setClasspath(String classpath) {
        this.classpath = classpath;
    }

    public void setMainClassName(String mainClassName) {
        this.mainClassName = mainClassName;
    }

    public Process launchDashboard(File pspdataDir, List extraVmArgs, List extraArgs)
            throws Exception {
        List cmd = new ArrayList();
        cmd.add(jreExecutable);
        cmd.add("-cp");
        cmd.add(classpath);
        if (vmArgs != null)
            cmd.addAll(vmArgs);
        if (extraVmArgs != null)
            cmd.addAll(extraVmArgs);
        cmd.add(mainClassName);
        if (extraArgs != null)
            cmd.addAll(extraArgs);

        String[] cmdLine = (String[]) cmd.toArray(new String[cmd.size()]);
        Process result = Runtime.getRuntime().exec(cmdLine, null, pspdataDir);
        return result;
    }

    private String getSelfClasspath() {
        File f = RuntimeUtils.getClasspathFile(getClass());

        if (f.isFile())
            return f.getAbsolutePath();
        else if (f.isDirectory())
            return extendDashboardClasspath(f);
        else
            return null;
    }

    /** Return a classpath for use with the unpackaged class files in a
     * compiled dashboard project directory.
     * 
     * @param selfUrlStr the URL of the class file for this class; must be a
     *    file: URL pointing to a .class file in the "bin" directory of a
     *    process dashboard project directory
     * @return the classpath that can be used to launch a dashboard instance.
     *    This classpath will include the effective "bin" directory that
     *    contains this class, and will also include the JAR files in the
     *    "lib" directory of the process dashboard project directory.
     */
    private String extendDashboardClasspath(File binDir) {
        File parentDir = binDir.getParentFile();
        File libDir = new File(parentDir, "lib");
        File[] libFiles = libDir.listFiles();
        if (libFiles == null)
            return null;

        StringBuffer result = new StringBuffer();
        result.append(binDir.toString());
        String sep = System.getProperty("path.separator");
        for (int i = 0; i < libFiles.length; i++) {
            if (libFiles[i].getName().toLowerCase().endsWith(".jar"))
                result.append(sep).append(libFiles[i].toString());
        }

        return result.toString();
    }

}
