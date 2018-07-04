// Copyright (C) 2008-2018 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge.client;

import java.io.File;
import java.io.IOException;

import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.TempFileFactory;

public class DirectoryPreferences {

    private static final String APPLICATION_DIR_PROPERTY = "net.sourceforge.processdash.applicationDir";

    private static final String WORKING_DIR_PROPERTY = "net.sourceforge.processdash.workingDir";

    private static final String IMPORT_DIR_PROPERTY = "net.sourceforge.processdash.importDir";


    public static File getApplicationDirectory() {
        // legacy semantics for backwards compatibility
        return getApplicationDirectory(false);
    }


    /**
     * Get the base storage directory for the application
     * 
     * @param workstationLocal
     *            <tt>true</tt> if this method should favor a directory that is
     *            only cached on the current computer; <tt>false</tt> if it
     *            should favor a directory that follows the user when they log
     *            on to various workstations. (Some operating systems will not
     *            discriminate between the two, and will return the same value.)
     * @since 2.4.3
     */
    public static File getApplicationDirectory(boolean workstationLocal) {
        // check for a specific value, supplied by a system property
        String hardcodedResult = System.getProperty(APPLICATION_DIR_PROPERTY);
        if ((hardcodedResult != null && hardcodedResult.length() > 0))
            return new File(hardcodedResult);

        if (workstationLocal) {
            // check Windows environment variable
            String envDir = System.getenv("LOCALAPPDATA");
            if (envDir != null && envDir.length() > 0)
                return new File(envDir, "Process Dashboard");
        }

        // check for a value specified through the APPDATA environment variable.
        // This is useful on Windows
        try {
            String envDir = System.getenv("APPDATA");
            if (envDir != null && envDir.length() > 0)
                return new File(envDir, "Process Dashboard");
        } catch (Throwable t) {
            // In Java 1.4, getenv will throw an error
        }

        // construct default values based upon current operating system.
        String subdir;

        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.startsWith("mac"))
            subdir = "Library/Process Dashboard";
        else if (osName.startsWith("win"))
            subdir = "Application Data\\Process Dashboard";
        else
            subdir = ".processdash";

        File userHome = new File(System.getProperty("user.home"));
        File result = new File(userHome, subdir);
        int i = 2;
        while (result.isFile() && i < 10) {
            // in certain rare scenarios, a regular file may exist with
            // the name we desire.  (For example, if a Linux user has a plain
            // file called ~/.processdash)  The regular file will prevent us
            // from creating the application directory; so in such a scenario,
            // try choosing a different name.
            result = new File(userHome, subdir + i++);
        }
        return result;
    }


    public static File getMasterWorkingDirectory() {
        File result;

        // check for a specific value, supplied by a system property
        String hardcodedResult = System.getProperty(WORKING_DIR_PROPERTY);
        if ((hardcodedResult != null && hardcodedResult.length() > 0))
            result = new File(hardcodedResult);
        else
            result = new File(getDataDirectoryParent(false), "working");

        result.mkdirs();
        return result;
    }


    public static File getMasterImportDirectory() {
        File result;

        // check for a specific value, supplied by a system property
        String hardcodedResult = System.getProperty(IMPORT_DIR_PROPERTY);
        if ((hardcodedResult != null && hardcodedResult.length() > 0))
            result = new File(hardcodedResult);
        else
            result = new File(getDataDirectoryParent(true), "import");

        result.mkdirs();
        return result;
    }


    private static File getDataDirectoryParent(boolean workstationLocal) {
        if (HistoricalMode.isHistoricalModeEnabled()) {
            synchronized (DirectoryPreferences.class) {
                try {
                    if (HISTORICAL_DATA_DIR == null)
                        HISTORICAL_DATA_DIR = TempFileFactory.get()
                                .createTempDirectory("pdes-historical-data",
                                    ".tmp", true, true);
                    return HISTORICAL_DATA_DIR;
                } catch (IOException ioe) {}
            }
        }
        return getApplicationDirectory(workstationLocal);
    }

    private static File HISTORICAL_DATA_DIR = null;



    public static File getLocalCacheDir(String url) {
        return new File(getMasterWorkingDirectory(), getWorkingIdForUrl(url));
    }

    static String getWorkingIdForUrl(String url) {
        if (url.startsWith("https"))
            url = "http" + url.substring(5);
        return FileUtils.makeSafeIdentifier(url);
    }

}
