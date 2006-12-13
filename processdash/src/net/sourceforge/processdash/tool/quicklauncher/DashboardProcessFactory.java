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
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.LightweightSet;

class DashboardProcessFactory {

    private String jreExecutable;

    private String classpath;

    private List vmArgs;

    private static final Resources resources = QuickLauncher.resources;

    public DashboardProcessFactory() throws Exception {
        jreExecutable = getJreExecutable();
        if (jreExecutable == null)
            throw new Exception(resources.getString("Errors.Missing_JRE"));

        classpath = getSelfClasspath();
        if (classpath == null)
            throw new Exception(resources.getString("Errors.Missing_JAR"));
    }

    public void addVmArg(String arg) {
        if (vmArgs == null)
            vmArgs = new LightweightSet();
        if (arg != null)
            vmArgs.add(arg);
    }

    public void removeVmArg(String arg) {
        if (vmArgs != null && arg != null)
            vmArgs.remove(arg);
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
        cmd.add(ProcessDashboard.class.getName());
        if (extraArgs != null)
            cmd.addAll(extraArgs);

        String[] cmdLine = (String[]) cmd.toArray(new String[cmd.size()]);
        Process result = Runtime.getRuntime().exec(cmdLine, null, pspdataDir);
        return result;
    }

    private String getJreExecutable() {
        File javaHome = new File(System.getProperty("java.home"));

        boolean isWindows = System.getProperty("os.name").toLowerCase()
                .indexOf("windows") != -1;
        String baseName = (isWindows ? "java.exe" : "java");

        String result = getExistingFile(javaHome, "bin", baseName);
        if (result == null)
            result = getExistingFile(javaHome, "sh", baseName);
        if (result == null)
            result = baseName;
        return result;
    }

    private static String getExistingFile(File dir, String subdir,
            String baseName) {
        dir = new File(dir, subdir);
        File file = new File(dir, baseName);
        if (file.exists())
            return file.getAbsolutePath();
        return null;
    }

    private String getSelfClasspath() {
        String selfClassName = getClass().getName();
        selfClassName = selfClassName
                .substring(selfClassName.lastIndexOf(".") + 1);
        URL selfUrl = getClass().getResource(selfClassName + ".class");
        if (selfUrl == null)
            return null;

        String selfUrlStr = selfUrl.toString();
        if (selfUrlStr.startsWith("jar:file:"))
            selfUrlStr = selfUrlStr.substring(9, selfUrlStr.indexOf("!/net/"));
        else
            return null;

        String jarFileName;
        try {
            jarFileName = URLDecoder.decode(selfUrlStr, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // can't happen
            return null;
        }
        File classpathItem = new File(jarFileName);
        return classpathItem.getAbsolutePath();
    }

}
