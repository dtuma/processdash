// Copyright (C) 2008 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.bridge.client;

import java.io.File;

public class DirectoryPreferences {

    private static final String APPLICATION_DIR_PROPERTY = "net.sourceforge.processdash.applicationDir";

    private static final String WORKING_DIR_PROPERTY = "net.sourceforge.processdash.workingDir";

    private static final String IMPORT_DIR_PROPERTY = "net.sourceforge.processdash.importDir";


    public static File getApplicationDirectory() {
        // check for a specific value, supplied by a system property
        String hardcodedResult = System.getProperty(APPLICATION_DIR_PROPERTY);
        if ((hardcodedResult != null && hardcodedResult.length() > 0))
            return new File(hardcodedResult);

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
        return new File(userHome, subdir);
    }


    public static File getMasterWorkingDirectory() {
        // check for a specific value, supplied by a system property
        String hardcodedResult = System.getProperty(WORKING_DIR_PROPERTY);
        if ((hardcodedResult != null && hardcodedResult.length() > 0))
            return new File(hardcodedResult);

        return new File(getApplicationDirectory(), "working");
    }


    public static File getMasterImportDirectory() {
        // check for a specific value, supplied by a system property
        String hardcodedResult = System.getProperty(IMPORT_DIR_PROPERTY);
        if ((hardcodedResult != null && hardcodedResult.length() > 0))
            return new File(hardcodedResult);

        return new File(getApplicationDirectory(), "import");
    }
}
