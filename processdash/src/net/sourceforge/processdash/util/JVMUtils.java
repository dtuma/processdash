// Copyright (C) 2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
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

package net.sourceforge.processdash.util;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

public class JVMUtils {

    /** Return the path to the executable that can launch a new JVM.
     */
    public static String getJreExecutable() {
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

    /** Return the file that forms the classpath for the given class.
     * 
     * If the class has been loaded from a local JAR file, this will return a
     * File object pointing to that JAR file.
     * 
     * If the class has been loaded from a local directory, this will return a
     * File object pointing to the directory that forms the base of the
     * classpath.
     * 
     * If a local file cannot be determined for the given class, returns null.
     */
    public static File getClasspathFile(Class clz) {
        String className = clz.getName();
        String baseName = className.substring(className.lastIndexOf(".") + 1);
        URL classUrl = clz.getResource(baseName + ".class");
        if (classUrl == null)
            return null;

        String classUrlStr = classUrl.toString();
        if (classUrlStr.startsWith("jar:file:"))
            return getJarBasedClasspath(classUrlStr);
        else if (classUrlStr.startsWith("file:"))
            return getDirBasedClasspath(classUrlStr, className);
        else
            return null;
    }

    /** Return a classpath for use with the packaged JAR file containing the
     * compiled classes used by the dashboard.
     * 
     * @param selfUrlStr the URL of the class file for this class; must be a
     *    jar:file: URL.
     * @return the JAR-based classpath in effect
     */
    private static File getJarBasedClasspath(String selfUrlStr) {
        // remove initial "jar:file:" and trailing "!/package/..." information
        selfUrlStr = selfUrlStr.substring(9, selfUrlStr.indexOf("!/"));

        String jarFileName;
        try {
            jarFileName = URLDecoder.decode(selfUrlStr, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // can't happen
            return null;
        }
        File jarFile = new File(jarFileName).getAbsoluteFile();
        return jarFile;
    }

    /** Return a classpath for use with an unpackaged class file
     * 
     * @param selfUrlStr the URL of the class file for this class; must be a
     *    file: URL pointing to a .class file in the "bin" directory of a
     *    process dashboard project directory
     * @return the classpath that can be used to launch a dashboard instance.
     *    This classpath will include the effective "bin" directory that
     *    contains this class, and will also include the JAR files in the
     *    "lib" directory of the process dashboard project directory.
     */
    private static File getDirBasedClasspath(String selfUrlStr, String className) {
        // remove initial "file:" and trailing "/net/..." information
        String classFilename = className.replace('.', '/');
        int packagePos = selfUrlStr.indexOf(classFilename);
        if (packagePos == -1)
            return null;
        selfUrlStr = selfUrlStr.substring(5, packagePos);

        File binDir;
        try {
            String path = URLDecoder.decode(selfUrlStr, "UTF-8");
            binDir = new File(path).getAbsoluteFile();
            return binDir;
        } catch (Exception e) {
            return null;
        }
    }
}
