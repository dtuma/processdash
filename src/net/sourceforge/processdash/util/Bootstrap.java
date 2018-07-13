// Copyright (C) 2018 Tuma Solutions, LLC
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

package net.sourceforge.processdash.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class Bootstrap {

    public static Thread createMainThread(final File targetJarFile,
            final String mainClassName, final String[] args) throws Exception {
        return launchMain(targetJarFile, mainClassName, args, true);
    }

    public static void launchMain(File targetJarFile, String mainClassName,
            String[] args) throws Exception {
        launchMain(targetJarFile, mainClassName, args, false);
    }

    private static Thread launchMain(File targetJarFile, String mainClassName,
            String[] args, boolean createThread) throws Exception {
        // get a classloader to load files from the given file
        ClassLoader cl = getAppClassLoader(targetJarFile);
        Class clazz = Class.forName(mainClassName, true, cl);
        Method main = clazz.getMethod("main", new Class[] { String[].class });

        if (createThread) {
            // create a thread for starting the application
            return new BootstrapThread(main, args);
        } else {
            // run the main method on the current thread
            main.invoke(null, new Object[] { args });
            return null;
        }
    }

    private static ClassLoader getAppClassLoader(File targetJarFile)
            throws IOException {
        // see if we're already running from the indicated JAR. If so, there
        // is no need to create a new class loader.
        File selfClasspath = RuntimeUtils.getClasspathFile(Bootstrap.class);
        if (targetJarFile.equals(selfClasspath))
            return Bootstrap.class.getClassLoader();

        // make a new class loader to read data from the given JAR
        ClassLoader cl = new URLClassLoader(
                new URL[] { targetJarFile.toURI().toURL() },
                Bootstrap.class.getClassLoader().getParent());
        Thread.currentThread().setContextClassLoader(cl);
        return cl;
    }

}
