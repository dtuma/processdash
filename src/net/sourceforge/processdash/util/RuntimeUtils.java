// Copyright (C) 2007-2015 Tuma Solutions, LLC
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
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.security.AccessController;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.management.OperatingSystemMXBean;

public class RuntimeUtils {

    public static final String AUTO_PROPAGATE_SETTING =
        RuntimeUtils.class.getName() + ".propagatedSystemProperties";

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

    /** Returns a list of args that should be passed to any JVM that we
     * spawn.
     */
    public static String[] getPropagatedJvmArgs() {
        List<String> result = new ArrayList<String>();
        List<String> propagating = new ArrayList<String>();
        for (Map.Entry<String, String> e : PROPS_TO_PROPAGATE.entrySet()) {
            String propName = e.getKey();
            String propValue = e.getValue();

            if (propValue == null)
                propValue = System.getProperty(propName);

            if (propValue != null) {
                result.add("-D" + propName + "=" + propValue);
                propagating.add(propName);
            }
        }
        if (!propagating.isEmpty())
            result.add("-D" + AUTO_PROPAGATE_SETTING + "="
                    + StringUtils.join(propagating, ","));
        return result.toArray(new String[result.size()]);
    }

    /** Register a particular system property as one that should be propagated
     * to child JVMs.
     * 
     * @param name the name of a property that should be propagated to
     *     child JVMs
     * @param value the value to propagate.  If null, the actual value will be
     *     retrieved on-the-fly via System.getProperty() at JVM creation time
     * @throws SecurityException if the caller does not have the appropriate
     *     permission to alter propagated system properties
     */
    public static void addPropagatedSystemProperty(String name, String value)
            throws SecurityException {
        if (!StringUtils.hasValue(name))
            throw new IllegalArgumentException("No property name was provided");

        checkJvmArgsPermission();
        PROPS_TO_PROPAGATE.put(name, value);
    }

    /** Look in a system property for a list of other properties that should
     * be propagated to child JVMs, and register all of those properties for
     * propagation.
     */
    public static void autoregisterPropagatedSystemProperties() {
        checkJvmArgsPermission();
        String autoPropSpec = System.getProperty(AUTO_PROPAGATE_SETTING);
        if (autoPropSpec != null && autoPropSpec.length() > 0) {
            for (String prop : autoPropSpec.split(",")) {
                prop = prop.trim();
                if (prop.length() > 0)
                    RuntimeUtils.addPropagatedSystemProperty(prop, null);
            }
        }
    }

    private static final Map<String,String> PROPS_TO_PROPAGATE =
        Collections.synchronizedMap(new HashMap<String,String>());
    static {
        addPropagatedSystemProperty("user.language", null);
        addPropagatedSystemProperty("java.util.logging.config.file", null);
    }

    /** Define the permission that is needed to alter propagated JVM args
     * 
     * @param p the permission to use
     * @throws SecurityException if a previous permission was set, and the
     *     caller does not have that permission.
     */
    public static void setJvmArgsPermission(Permission p)
            throws SecurityException {
        checkJvmArgsPermission();
        JVM_ARGS_PERMISSION = p;
    }

    private static Permission JVM_ARGS_PERMISSION = null;
    private static void checkJvmArgsPermission() {
        if (JVM_ARGS_PERMISSION != null)
            AccessController.checkPermission(JVM_ARGS_PERMISSION);
    }

    /**
     * JVMs are often started with a -Xmx argument to set the max heap size.
     * This method will return the argument that was most likely used when
     * starting the current JVM.
     * 
     * @since 1.14.2
     */
    public static String getJvmHeapArg() {
        long heapMaxSize = Runtime.getRuntime().maxMemory();
        heapMaxSize = heapMaxSize >> 20;
        return "-Xmx" + heapMaxSize + "m";
    }

    /**
     * JVMs are often started with a -Xmx argument to set the max heap size.
     * This method will return the suggested maximum amount of memory that
     * should be used for such an argument, based on the current operating
     * system and JVM environment.
     * 
     * @since 1.15.0.4
     */
    public static long getSuggestedMaxJvmHeapSize() {
        try {
            Object bean = ManagementFactory.getOperatingSystemMXBean();
            OperatingSystemMXBean osMxBean = (OperatingSystemMXBean) bean;
            long systemMemoryBytes = osMxBean.getTotalPhysicalMemorySize();
            long systemMemoryMegabytes = systemMemoryBytes >> 20;
            long halfOfMemory = systemMemoryMegabytes / 2;
            long result = halfOfMemory;

            // Systems today may have a lot of memory - 4 or 6GB - and half
            // of that value will still be excessive.  In addition, requesting
            // half of 4GB would result in a process that exceeds the memory
            // limitations of a 32-bit JVM.  Look at the type of system we are
            // running, and choose a more conservative limit as appropriate.
            if ("64".equals(System.getProperty("sun.arch.data.model")))
                result = Math.min(halfOfMemory, 2000);
            else
                result = Math.min(halfOfMemory, 1000);

            return result;
        } catch (Throwable t) {
            // If we are not running in a Sun JVM, the code above will fail.
            // In that case, use a conservative threshhold.
            return 800;
        }
    }

    /**
     * JVMs can be started with an -Xmx argument to set the max heap size.
     * Unfortunately, it can be difficult to set this argument properly: if you
     * choose a heap size that is too high, the java process will exit
     * immediately. This method runs a Java subprocess using a tentative value
     * for the heap size. If the subprocess fails immediately, it reduces the
     * heap size and tries again. If several attempts fail, the process will be
     * run without any -Xmx argument at all.
     * 
     * @param args
     *            the command-line arguments to pass to the Java application.
     *            Special notes:
     *            <ul>
     *            <li>These arguments should <b>not</b> begin with the java
     *            executable path, and should <b>not</b> include an -Xmx
     *            parameter.</li>
     *            <li>The given program is expected to run for at least 1.5
     *            seconds, or to return with an exit code of 0. If the program
     *            returns quickly with a nonzero exit code, this method will
     *            assume that the JRE aborted and will run the program again
     *            with a lower heap setting.</li>
     *            <li>This method will <b>not</b> automatically add the values
     *            from the {@link #getPropagatedJvmArgs()} method; that is the
     *            caller's responsibility.</li>
     *            </ul>
     * @param envp
     *            array of strings, each element of which has environment
     *            variable settings in the format <i>name</i>=<i>value</i>, or
     *            <tt>null</tt> if the subprocess should inherit the environment
     *            of the current process.
     * @param dir
     *            the working directory of the subprocess, or <tt>null</tt> if
     *            the subprocess should inherit the working directory of the
     *            current process.
     * @param heapSize
     *            an optional single value specifying the initial number of
     *            megabytes to attempt using for the heap size. If this value is
     *            not provided or is zero, the value from
     *            {@link #getSuggestedMaxJvmHeapSize()} will be used as the
     *            starting size. If this value is of type AtomicInteger, its
     *            value will be set() to the actual heap size that was
     *            eventually used. (The number -1 will be returned here if the
     *            method gave up and ran the program without a heap size
     *            argument.)
     * @return the Process that was launched
     * 
     * @since 2.0.4
     */
    public static Process execWithAdaptiveHeapSize(String[] args,
            String[] envp, File dir, Number... heapSize) throws IOException {
        String[] cmdLine = new String[args.length + 2];
        System.arraycopy(args, 0, cmdLine, 2, args.length);
        cmdLine[0] = getJreExecutable();

        int heapSizeUsed = 0;
        if (heapSize != null && heapSize.length > 0 && heapSize[0] != null)
            heapSizeUsed = heapSize[0].intValue();
        if (heapSizeUsed <= 0)
            heapSizeUsed = (int) getSuggestedMaxJvmHeapSize();

        // try launching the app with the suggested heap size. If that fails,
        // try successively lower heap sizes until one works.
        for (int retries = 10; retries-- > 0;) {
            cmdLine[1] = "-Xmx" + heapSizeUsed + "m";
            Process result = Runtime.getRuntime().exec(cmdLine, envp, dir);
            if (isRunningSuccessfully(result, 1500)) {
                maybeStoreNumber(heapSize, heapSizeUsed);
                return result;
            } else {
                heapSizeUsed = heapSizeUsed * 3 / 4;
            }
        }

        // If the process still failed, launch without any heap argument.
        maybeStoreNumber(heapSize, -1);
        cmdLine[1] = "-D" + RuntimeUtils.class.getName() + ".noHeapArgUsed=t";
        return Runtime.getRuntime().exec(cmdLine, envp, dir);
    }

    private static boolean isRunningSuccessfully(Process p, int maxWaitTime) {
        int numTimeSlices = 20;
        for (int i = numTimeSlices; i-- > 0;) {
            try {
                // pause for a moment before checking the exit status
                Thread.sleep(maxWaitTime / numTimeSlices);
                // check for the exit status. If the call returns successfully
                // without throwing an exception, the process has terminated.
                int exitValue = p.exitValue();
                return (exitValue == 0);
            } catch (InterruptedException ie) {
            } catch (IllegalThreadStateException e) {
                // If the process is still running, the "exitValue" method will
                // throw this exception.
            }
        }
        // we've checked multiple times (waiting up to maxWaitTime) for the
        // process to terminate. But it still seems to be running.
        return true;
    }

    private static void maybeStoreNumber(Number[] n, int value) {
        if (n != null && n.length > 0 && n[0] instanceof AtomicInteger)
            ((AtomicInteger) n[0]).set(value);
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
            return getFileForUrl(classUrlStr);
        else if (classUrlStr.startsWith("file:"))
            return getDirBasedClasspath(classUrlStr, className);
        else
            return null;
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


    /**
     * Return the File referenced by a "file:" or "jar:file:" URL.
     * 
     * @param url
     *            a URL; may be null
     * @return the File this URL points to if applicable; otherwise null
     * @since 2.1.8
     */
    public static File getFileForUrl(URL url) {
        return (url == null ? null : getFileForUrl(url.toString()));
    }

    /**
     * Return the File referenced by a "file:" or "jar:file:" URL.
     * 
     * @param url
     *            a URL in string form; may be null
     * @return the File this URL points to if applicable; otherwise null
     * @since 2.1.8
     */
    public static File getFileForUrl(String url) {
        if (url == null)
            return null;

        if (url.startsWith("jar:")) {
            int pos = url.indexOf('!');
            url = (pos == -1 ? url.substring(4) : url.substring(4, pos));
        }

        String jarFileName;
        try {
            if (url.startsWith("file:"))
                jarFileName = URLDecoder.decode(url.substring(5), "UTF-8");
            else
                return null;
        } catch (UnsupportedEncodingException e) {
            // can't happen
            return null;
        }
        File jarFile = new File(jarFileName).getAbsoluteFile();
        return jarFile;
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

    /**
     * Discard the output produced by another process.
     * 
     * This method will return immediately, but will continue asynchronously
     * reading and discarding the output from the given process.  This is
     * important to prevent that process from hanging.
     * 
     * @since 1.14.2
     */
    public static void discardOutput(final Process p) {
        Thread t = new Thread() {
            public void run() {
                consumeOutput(p, null, null);
            } };
        t.setDaemon(true);
        t.start();
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
        return consumeOutput(p, destOut, destErr, false);
    }

    /**
     * Collect the output from a process.
     * 
     * @param p the process to run; this method will wait until the process
     *       terminates.
     * @param stdOut true if data from stdout should be collected
     * @param stdErr true if data from stderr should be collected
     * @return the bytes generated by the process.
     */
    public static byte[] collectOutput(Process p, boolean stdOut,
            boolean stdErr) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        consumeOutput(p, stdOut ? buf : null, stdErr ? buf : null, true);
        return buf.toByteArray();
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
     * @param eager if true, output will be collected as quickly as possible.
     *    If false, the collection will take a lower priority.
     * @return the exit status of the process.
     */
    public static int consumeOutput(Process p, OutputStream destOut,
            OutputStream destErr, boolean eager) {

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
                    Thread.sleep(eager ? 10 : 500);
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


    /**
     * Utility routine to check that a given class provides a particular
     * method in the enclosing java runtime environment.
     * 
     * Note: this verifies the presence of a method by name only.  In the
     * future, if more stringent checks are required (for example, for specific
     * overloaded arguments), a new method can be introduced.
     * 
     * @param clazz the class to check
     * @param methodName a method to require
     * @throws UnsupportedOperationException if the method does not exist
     */
    public static void assertMethod(Class clazz, String methodName)
            throws UnsupportedOperationException {
        Method[] m = clazz.getMethods();
        for (Method method : m) {
            if (method.getName().equals(methodName))
                return;
        }
        throw new UnsupportedOperationException("Class " + clazz.getName()
                + " does not support method " + methodName);
    }

}
