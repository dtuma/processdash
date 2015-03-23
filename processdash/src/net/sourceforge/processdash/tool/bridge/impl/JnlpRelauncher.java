// Copyright (C) 2012 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge.impl;

import java.io.File;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.tool.bridge.client.BridgedWorkingDirectory;
import net.sourceforge.processdash.tool.bridge.client.WorkingDirectory;
import net.sourceforge.processdash.tool.quicklauncher.QuickLauncher;
import net.sourceforge.processdash.util.RuntimeUtils;
import net.sourceforge.processdash.util.XMLUtils;

public class JnlpRelauncher {

    /**
     * Examine the working directory to see if it represents a filesystem
     * directory that has been migrated into an enterprise server. If so, try
     * relaunching the dataset via Java Web Start.
     * 
     * @param workingDir
     *            the working directory for a Process Dashboard dataset
     * @return true if the dataset was relaunched via JNLP, false otherwise
     */
    public static boolean maybeRelaunch(WorkingDirectory workingDir) {
        if (workingDir instanceof BridgedWorkingDirectory) {
            try {
                return maybeRelaunchImpl((BridgedWorkingDirectory) workingDir);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private static boolean maybeRelaunchImpl(BridgedWorkingDirectory bwd)
            throws Exception {
        // check to see if the user has disabled relaunching.
        if (isDisabled())
            return false;

        // Only consider a JNLP restart if our BridgedWorkingDirectory was
        // constructed for a local filesystem directory that has been migrated
        // into the server.
        if (hasMigratedTargetDirectory(bwd) == false)
            return false;

        // Retrieve a URL to the appropriate JNLP file. If this working
        // directory is not available via JNLP, abort.
        String jnlpUrl = getJnlpURL(bwd);
        if (jnlpUrl == null)
            return false;

        // Get the path to the java web start executable
        String javawsExecutable = getWebStartExecutable();
        if (javawsExecutable == null)
            return false;

        // launch Java Web Start and check to ensure that it was successful.
        return relaunch(javawsExecutable, jnlpUrl);
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
     * Return true if the given working directory was constructed for a local
     * filesystem directory that has been migrated into a server.
     */
    private static boolean hasMigratedTargetDirectory(
            BridgedWorkingDirectory bwd) {

        // When the dashboard is launched from a legacy shortcut, the
        // targetDirectory property will point to that legacy shortcut's data
        // directory. (If the target directory is null, there is a high
        // probability that we were already launched via JNLP.)
        File targetDirectory = bwd.getTargetDirectory();
        if (targetDirectory == null)
            return false;

        // check to see if the target directory contains a marker file
        // indicating that it was migrated into a team server.
        File migrationMarker = new File(targetDirectory,
                DatasetAutoMigrator.MARKER_FILE);
        return migrationMarker.isFile();
    }


    /**
     * Construct a URL that points to a valid JNLP file for this working
     * directory. If this working directory is not accessible via a JNLP URL,
     * returns null.
     */
    private static String getJnlpURL(BridgedWorkingDirectory bwd) {
        // Look at the description of the working directory. For bridged
        // directories, this should be the URL of the data collection. Match
        // this against the typical pattern we expect for data collection URLs.
        // if it doesn't match, do nothing.
        String datasetUrl = bwd.getDescription();
        Matcher m = DATA_BRIDGE_PAT.matcher(datasetUrl);
        if (!m.matches())
            return null;

        // Use PDES naming conventions to construct a URL where we think the
        // JNLP file should be published. Try retrieving that URL, and make
        // certain it represents a JNLP XML file.
        //
        // Of course, this implies that we will not relaunch over JNLP if we
        // are offline and the server is unreachable. Although it could be
        // desirable to relaunch over JNLP in that situation if the dataset is
        // marked for offline use, that can't be guaranteed to work reliably.
        String jnlpUrl = m.group(1) + "pub/LaunchDataset" + m.group(3)
                + ".jnlp";
        try {
            URL testUrl = new URL(jnlpUrl);
            Document xml = XMLUtils.parse(testUrl.openStream());
            if ("jnlp".equals(xml.getDocumentElement().getTagName()))
                return jnlpUrl;
        } catch (Exception e) {
        }
        return null;
    }

    private static final Pattern DATA_BRIDGE_PAT = Pattern
            .compile("(http.*/)(DataBridge)(/INST-.*)");


    /**
     * Return the path the the java web start executable, or null if it cannot
     * be found.
     */
    private static final String getWebStartExecutable() {
        // get the path the to the main java executable.
        String java = RuntimeUtils.getJreExecutable();
        if (java == null)
            return null;

        // replace the word "java" with the word "javaws"
        int pos = java.lastIndexOf("java") + 4;
        if (pos < 4)
            return null;
        String javaws = java.substring(0, pos) + "ws" + java.substring(pos);

        // check to see if the file exists and is executable.
        File f = new File(javaws);
        if (f.isFile())
            return javaws;
        else
            return null;
    }


    /**
     * Launch a Java Web Start process for a given URL.
     */
    protected static boolean relaunch(String javawsExecutable, String jnlpUrl)
            throws Exception {
        System.out.println("Relaunching via Java Web Start URL " + jnlpUrl);
        long start = System.currentTimeMillis();

        // launch the web start process and wait for it to finish.
        String[] cmd = new String[] { javawsExecutable, "-Xnosplash", jnlpUrl };
        Process process = Runtime.getRuntime().exec(cmd);
        int exitStatus = RuntimeUtils.doWaitFor(process);

        // The process might return immediately; if it does, wait a few seconds
        // so our splash screen remains visible until the splash screen of the
        // replacement process can appear.
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed < 2900)
            Thread.sleep(2900 - elapsed);

        // check the exit status of the web start process.
        return (exitStatus == 0);
    }

}
