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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class TeamToolsVersionManager {

    public static final String WBS_EDITOR_VERSION_REQUIREMENT = "minWbsEditorVersion";

    private static final String PROJ_SETTINGS_FILENAME = "user-settings.ini";


    /**
     * Look in a WBS directory and return the minimum required version of the
     * WBS Editor that is required by the given project
     * 
     * @param wbsFile
     *            a directory or a ZIP file containing WBS data for a project
     * @return the minimum version of the WBS Editor the project requires.
     *         Returns null if the project does not list a minimum requirement,
     *         or if project settings could not be found/read.
     */
    public static String getRequiredWBSEditorVersion(File wbsFile) {
        return getProjectProperty(wbsFile, WBS_EDITOR_VERSION_REQUIREMENT);
    }

    private static String getProjectProperty(File wbsFile, String propName) {
        try {
            // load the project settings from the given directory
            Map props = loadProjectProperties(wbsFile);

            // retrieve and return the version requirement setting
            return (String) props.get(propName);

        } catch (IOException ioe) {
            return null;
        }
    }

    private static Map loadProjectProperties(File wbsFile) throws IOException {
        if (wbsFile.isDirectory())
            return loadProjectPropertiesFromDir(wbsFile);
        else
            return loadProjectPropertiesFromZip(wbsFile);
    }

    private static Map loadProjectPropertiesFromDir(File wbsDir)
            throws IOException {
        File f = new File(wbsDir, PROJ_SETTINGS_FILENAME);
        return loadPropertiesFromStream(new FileInputStream(f));
    }

    private static Map loadProjectPropertiesFromZip(File wbsZipFile)
            throws IOException {
        if (!wbsZipFile.isFile())
            throw new FileNotFoundException(wbsZipFile.getPath());

        ZipFile zip = new ZipFile(wbsZipFile);
        try {
            ZipEntry entry = zip.getEntry(PROJ_SETTINGS_FILENAME);
            if (entry != null)
                return loadPropertiesFromStream(zip.getInputStream(entry));
            else
                return Collections.EMPTY_MAP;
        } finally {
            zip.close();
        }
    }

    private static Properties loadPropertiesFromStream(InputStream in)
            throws IOException {
        Properties result = new Properties();
        result.load(new BufferedInputStream(in));
        in.close();
        return result;
    }

}
