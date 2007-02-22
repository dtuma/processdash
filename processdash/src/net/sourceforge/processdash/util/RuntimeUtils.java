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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

public class RuntimeUtils {

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

    /** Wait for a process to complete, and return the exit status.
     * 
     * This consumes output from the process, so it will not be blocked.  This
     * effectively works around limitations in <code>Process.waitFor()</code>.
     * 
     * @param p the process to wait for
     * @return the exit status of the process
     */
    public static int doWaitFor(Process p) {
        return consumeOutput(p, null, null);
    }

    /** Consume the output generated by a process until it completes, and
     * return its exit value.
     * 
     * The javadoc for the Runtime.exec() method casually mentions that if you
     * launch a process which generates output (to stdout or stderr), you must
     * consume that output, or the process will become blocked when its
     * OS-provided output buffers become full. This method consumes process
     * output, as required.
     * 
     * @param p the process to consume output for
     * @param destOut a stream to which stdout data should be copied.  If null,
     *    stdout data will be discarded
     * @param destErr a stream to which stderr data should be copied.  If null,
     *    stderr data will be discarded
     * @return the exit status of the process.
     */
    public static int consumeOutput(Process p, OutputStream destOut,
            OutputStream destErr) {

        int exitValue = -1; // returned to caller when p is finished

        try {

            InputStream in = p.getInputStream();
            InputStream err = p.getErrorStream();

            boolean finished = false; // Set to true when p is finished

            while (!finished) {
                try {
                    int c;

                    while (in.available() > 0 && (c = in.read()) != -1)
                        if (destOut != null)
                            destOut.write(c);

                    while (err.available() > 0 && (c = err.read()) != -1)
                        if (destErr != null)
                            destErr.write(c);

                    // Ask the process for its exitValue. If the process
                    // is not finished, an IllegalThreadStateException
                    // is thrown. If it is finished, we fall through and
                    // the variable finished is set to true.

                    exitValue = p.exitValue();
                    finished = true;

                } catch (IllegalThreadStateException e) {

                    // Process is not finished yet;
                    // Sleep a little to save on CPU cycles
                    Thread.sleep(500);
                }
            }


        } catch (Exception e) {
            // unexpected exception! print it out for debugging...
            System.err.println("doWaitFor(): unexpected exception - "
                    + e.getMessage());
        }

        // return completion status to caller
        return exitValue;
    }

}
