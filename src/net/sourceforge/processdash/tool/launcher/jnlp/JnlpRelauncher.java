// Copyright (C) 2012-2018 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.launcher.jnlp;

import java.io.File;
import java.util.List;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.tool.bridge.client.TeamServerSelector;
import net.sourceforge.processdash.tool.bridge.impl.DatasetAutoMigrator;
import net.sourceforge.processdash.tool.bridge.impl.TeamServerPointerFile;
import net.sourceforge.processdash.tool.launcher.pdes.PDESUtil;
import net.sourceforge.processdash.tool.quicklauncher.QuickLauncher;
import net.sourceforge.processdash.util.RuntimeUtils;

public class JnlpRelauncher {

    /**
     * Examine the given location to see if it represents a filesystem
     * directory that has been migrated into an enterprise server. If so, try
     * relaunching the dataset with the dashboard's JNLP handling logic.
     * 
     * @param location
     *            the location of a Process Dashboard dataset
     * @return true if the dataset was relaunched via JNLP, false otherwise
     */
    public static boolean maybeRelaunch(String location) {
        try {
            return maybeRelaunchImpl(location);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean maybeRelaunchImpl(String location)
            throws Exception {
        // if no location was provided, use the current directory.
        if (location == null)
            location = System.getProperty("user.dir");
        if (location == null)
            return false;

        // if the location is already a URL, there is no need to relaunch
        if (TeamServerSelector.isUrlFormat(location))
            return false;

        // check to see if the user has disabled relaunching.
        if (isDisabled())
            return false;

        // Only consider a JNLP restart if our target location is a local
        // filesystem directory that has been migrated into a server.
        File dir = new File(location);
        if (isMigratedTargetDirectory(dir) == false)
            return false;

        // Retrieve a URL to the appropriate JNLP file. If this working
        // directory is not available via JNLP, abort.
        String jnlpUrl = getJnlpURL(dir);
        if (jnlpUrl == null)
            return false;

        // Get the path to our JAR 
        File classpath = RuntimeUtils.getClasspathFile(JnlpDatasetLauncher.class);
        if (classpath == null)
            return false;

        // relaunch the app and return true if it completes without exception
        relaunch(classpath, jnlpUrl);
        return true;
    }


    /**
     * Should the logic in this class be disabled?
     */
    protected static boolean isDisabled() {
        // has the user explicitly added a command line arg to disable this?
        if (Boolean.getBoolean(DISABLE_SETTING))
            return true;

        // Has the user set a preference to disable this?
        if (Settings.getBool(Settings.PREFS_PREFIX + DISABLE_SETTING, false))
            return true;

        // are we running in the quick launcher? If so, don't relaunch via JNLP;
        // otherwise we could lose important instructions like "read-only mode,"
        // "disable auto export," or "same jvm"
        if (Boolean.getBoolean(QuickLauncher.QUICK_LAUNCH_MODE_PROP))
            return true;

        // The logic in this class doesn't appear to be disabled.
        return false;
    }

    private static final String DISABLE_SETTING = "noJnlpRelaunch";


    /**
     * Return true if the given directory is a local filesystem directory that
     * has been migrated into a server.
     */
    private static boolean isMigratedTargetDirectory(File targetDirectory) {
        // if the directory does not exist, abort
        if (targetDirectory == null || !targetDirectory.isDirectory())
            return false;

        // check to see if the target directory contains a marker file
        // indicating that it was migrated into a team server.
        File migrationMarker = new File(targetDirectory,
                DatasetAutoMigrator.MARKER_FILE);
        return migrationMarker.isFile();
    }


    /**
     * If the given directory was migrated into a server, return a JNLP URL to
     * that dataset. If not, return null.
     */
    private static String getJnlpURL(File dir) {
        // look for a teamServer.xml file in the given directory. A directory
        // that has been migrated should have a pointer file with one entry.
        TeamServerPointerFile pointerFile = new TeamServerPointerFile(dir);
        List<String> urls = pointerFile.getInstanceURLs();
        if (urls.size() != 1)
            return null;

        // compute the URL of the JNLP file, based on the data bridge URL
        String dataBridgeUrl = urls.get(0);
        return PDESUtil.getJnlpUrl(dataBridgeUrl, false);
    }


    /**
     * Launch a Java process for a given JNLP URL.
     */
    protected static void relaunch(File classpath, String jnlpUrl)
            throws Exception {
        System.out.println("Relaunching via Java Web Start URL " + jnlpUrl);
        long start = System.currentTimeMillis();

        // fork a child process to open the given URL
        String[] cmd = new String[] { RuntimeUtils.getJreExecutable(), //
                "-cp", classpath.getAbsolutePath(),
                JnlpDatasetLauncher.class.getName(), jnlpUrl };
        Process p = Runtime.getRuntime().exec(cmd, null, null);
        RuntimeUtils.discardOutput(p);

        // The process might take a moment to start. wait a few seconds so our
        // splash screen remains visible until the splash screen of the
        // replacement process can appear.
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed < 2900)
            Thread.sleep(2900 - elapsed);
    }

}
