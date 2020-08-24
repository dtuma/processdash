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

import static net.sourceforge.processdash.util.VersionUtils.compareVersions;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.sourceforge.processdash.util.RuntimeUtils;


public class TeamToolsVersionManager {

    public static final String WBS_EDITOR_VERSION_REQUIREMENT = "minWbsEditorVersion";

    public static final String DATA_VERSION = "wbsDataVersion";

    private static final String PROJ_SETTINGS_FILENAME = "user-settings.ini";

    private static final String DATA_REQ_MANIFEST_ATTR = "Dash-WBS-Min-Data-Version";


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



    /**
     * Look through various WBS Editor JAR files that might be packaged along
     * with the current application, and choose the JAR that is best for opening
     * a particular project WBS.
     * 
     * Note: if the given project requires a minimum version of the WBS Editor
     * logic, and that version isn't installed, this method could return a JAR
     * that is incapable of displaying the project. But the returned JAR will at
     * least be capable of detecting the mismatch and displaying an error to the
     * user.
     * 
     * @param wbsFile
     *            a directory or a ZIP file containing WBS data for a project
     * @param jarDir
     *            the directory to search for WBS Editor JAR files, or null to
     *            search in the directory this class is hosted from
     * @return a JAR file containing WBS Editor logic that should be used to
     *         open the given project. Returns null if no file could be found.
     */
    public static File getBestTeamToolsJarFor(File wbsFile, File jarDir) {
        // retrieve the data version number from the WBS zip file
        String dataVersion = getProjectProperty(wbsFile, DATA_VERSION);
        if (dataVersion == null)
            dataVersion = "1";

        // find the directory where TeamTools JAR files should be found
        File executableJarDirectory = jarDir;
        if (executableJarDirectory == null)
            executableJarDirectory = getExecutableJarDirectory();
        if (executableJarDirectory == null)
            return null;
        File[] files = executableJarDirectory.listFiles();
        if (files == null)
            return null;

        // scan the files we found, looking for the best TeamTools.jar file
        File bestJarFile = null;
        String bestVersion = "0";
        for (File file : files) {
            String version = checkAcceptableTeamToolsJar(file, dataVersion);
            if (version != null && compareVersions(version, bestVersion) > 0) {
                bestJarFile = file;
                bestVersion = version;
            }
        }

        // return the team tools JAR file we found
        return bestJarFile;
    }

    private static File getExecutableJarDirectory() {
        // find the classpath location of the current class
        File selfClasspath = RuntimeUtils
                .getClasspathFile(TeamToolsVersionManager.class);

        // if we were unable to find the classpath, abort
        if (selfClasspath == null) {
            return null;

        } else if (selfClasspath.isDirectory()) {
            // if the classpath is a directory, we're in development mode.
            // Return the "dist" directory that appears nearby
            return new File(selfClasspath.getParentFile(), "dist");

        } else {
            // when the classpath is a JAR file, return the parent dir
            return selfClasspath.getParentFile();
        }
    }

    private static String checkAcceptableTeamToolsJar(File jarFile,
            String dataVersion) {
        try {
            // only scan files whose names match a WBS Editor JAR pattern
            String name = jarFile.getName().toLowerCase();
            if (!name.startsWith("teamtools") && !name.startsWith("wbseditor"))
                return null;
            if (!name.endsWith(".jar"))
                return null;

            // read the attributes from the JAR file
            JarFile jar = new JarFile(jarFile);
            Attributes attrs = jar.getManifest().getMainAttributes();
            jar.close();

            // make sure this is a WBS Editor JAR file
            String packageId = attrs.getValue("Dash-Pkg-ID");
            if (packageId == null || !packageId.startsWith("teamTools"))
                return null;

            // if this JAR file only handles project data after a certain min
            // version, ensure the current project satisfies that requirement
            String minDataVersion = attrs.getValue(DATA_REQ_MANIFEST_ATTR);
            if (minDataVersion != null
                    && compareVersions(dataVersion, minDataVersion) < 0)
                return null;

            // all requirements seem to be met. Return the JAR version number
            return attrs.getValue("Dash-Pkg-Version");

        } catch (IOException e) {
            return null;
        }
    }

}
