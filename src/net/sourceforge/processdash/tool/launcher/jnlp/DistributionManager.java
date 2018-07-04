// Copyright (C) 2009-2018 Tuma Solutions, LLC
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import net.sourceforge.processdash.tool.bridge.client.DirectoryPreferences;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.VersionUtils;

import com.tuma_solutions.teamserver.jnlp.client.JarVerifier;
import com.tuma_solutions.teamserver.jnlp.client.JnlpPackagingConstants;

public class DistributionManager implements JnlpPackagingConstants {

    private static final String DISTRIB_DIR_FILENAME = "distrib";

    public static final String TARGET_JARFILE = "pspdash.jar";

    static final String USAGE_LOG_FILENAME = "lastUse.txt";

    private static final boolean VERIFY_DISTRIBUTION = true;

    private static final boolean VERIFY_EXISTING_TARGET_JAR = false;

    private static final boolean VERIFY_INSTALLED_TARGET_JAR = true;

    private static final String USER_VALUES_PREFS_NODE =
        "/net/sourceforge/processdash/installer";
    private static final String INSTALLATION_DIR_KEY = "INSTALL_PATH";


    /**
     * Look for a version of the Process Dashboard that has already been
     * installed on the curent computer.
     *
     * @return return the directory where the executable JAR files are
     *         installed, or null if no preexisting installation is found.
     */
    static File getInstalledDistributionDir() {
        String installPath = getInstallationPath();
        if (installPath == null)
            return null;

        File result = new File(installPath);
        if (distributionExists(result, VERIFY_INSTALLED_TARGET_JAR))
            return result;
        else
            return null;
    }

    private static String getInstallationPath() {
        // check to see if the current user installed the dashboard at some
        // time in the past.
        Preferences prefs = Preferences.userRoot().node(USER_VALUES_PREFS_NODE);
        String userInstallPath = prefs.get(INSTALLATION_DIR_KEY, null);
        if (userInstallPath != null && userInstallPath.length() > 0)
            return userInstallPath;

        // the current user did not install the dashboard - but perhaps some
        // other user on this computer did. Try looking in the typical places,
        // based on the current operating system.
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.indexOf("windows") != -1) {
            return "C:\\Program Files\\Process Dashboard";
        } else if (osName.startsWith("mac os x")) {
            return "/Library/Process Dashboard";
        } else {
            return null;
        }
    }


    /**
     * Get the directory for the most recently used distribution on the computer
     * 
     * @param minVersion
     *            the minimum version of a profile required, or null if any
     *            version is acceptable
     * @return the most recently used distribution that meets the version
     *         requirement, or null if none was found
     */
    public static File getMostRecentlyUsedDistribution(String minVersion) {
        List<File> distributions = getExistingDistributions(minVersion);
        if (distributions.isEmpty())
            return null;

        File result = distributions.get(0);
        touchDirectory(result);
        return result;
    }


    /**
     * Get a list of the directories holding locally cached distributions,
     * ordered with most recently used distributions first
     * 
     * @param minVersion
     *            the minimum version of a profile required, or null if any
     *            version is acceptable
     */
    public static List<File> getExistingDistributions(String minVersion) {
        // gather up the profiles in a map, sorted by usage date
        TreeMap<Long, File> profiles = new TreeMap<Long, File>();
        for (boolean local : LOCAL) {
            File[] files = getDistribDirectory(local).listFiles();
            if (files == null || files.length == 0)
                continue;

            for (File f : files) {
                // ignore plain files in the distrib directory
                if (!f.isDirectory())
                    continue;

                // only examine directories that contain a pspdash.jar file.
                // (Other directories are process assets.)
                File targetJar = new File(f, TARGET_JARFILE);
                if (!targetJar.isFile())
                    continue;

                // if a min version was requested, check it
                if (minVersion != null) {
                    Matcher m = LAUNCH_PROFILE_DIR_NAME_PAT.matcher(f.getName());
                    if (!m.matches())
                        continue;
                    String version = m.group(2);
                    if (VersionUtils.compareVersions(version, minVersion) < 0)
                        continue;
                }

                // get the timestamp and add it to our result
                File usageFile = new File(f, USAGE_LOG_FILENAME);
                profiles.put(usageFile.lastModified(), f);
            }
        }

        // build a list of the results, and reverse it to put the most recently
        // used profiles first
        List<File> result = new ArrayList<File>(profiles.values());
        Collections.reverse(result);
        return result;
    }

    private static final Pattern LAUNCH_PROFILE_DIR_NAME_PAT = Pattern
            .compile("(.+)-([0-9\\.ab]+)-([a-z0-9]+)");


    /**
     * Look for a dashboard distribution, previously extracted by this class,
     * that matches the content hash in the given launch profile URL.
     * 
     * This presumes that two unrelated launch profiles will not have the same
     * content hash. Since the content hash is a 32-bit checksum of the launch
     * profile data, such a collision would be extremely unlikely. In contrast,
     * the exact same launch profile published by two different enterprise
     * servers will be considered a match, allowing the user to avoid the second
     * download.
     * 
     * As a side effect, this method will also delete any previously extracted
     * distributions which have not been used recently.
     * 
     * @param launchProfileUrl
     *            the URL of a launch profile, as it appeared in the dataset
     *            JNLP file
     * @return a directory containing a previously extracted distribution with
     *         the same content hash as the one named by the URL
     * @throws IllegalArgumentException
     *             if the URL is not a launch profile URL
     */
    static File findExistingDistribution(String launchProfileUrl)
            throws IllegalArgumentException {
        // extract the content hash from the launch profile URL
        Matcher m = LAUNCH_PROFILE_URL_PAT.matcher(launchProfileUrl);
        if (!m.find())
            throw new IllegalArgumentException(
                    "Not a launch profile URL: " + launchProfileUrl);
        String contentHashSuffix = m.group(2);

        // scan the distributions on this computer, looking for one with the
        // correct content hash. Clean up unused distributions in the process
        long cutoffAge = System.currentTimeMillis() - 40 * DAY_MILLIS;
        File result = null;
        for (boolean local : LOCAL) {
            File[] files = getDistribDirectory(local).listFiles();
            if (files != null) {
                for (File f : files) {
                    if (!f.isDirectory()) {
                        // ignore plain files in the distrib directory

                    } else if (f.getName().endsWith(contentHashSuffix)
                            && result == null
                            && distributionExists(f, VERIFY_EXISTING_TARGET_JAR)) {
                        // if we find the desired profile, save it
                        result = f;
                        touchDirectory(result);

                    } else {
                        // consider other profiles for cleanup
                        maybeCleanupDirectory(f, cutoffAge);
                    }
                }
            }
        }

        return result;
    }

    private static final Pattern LAUNCH_PROFILE_URL_PAT = Pattern.compile(
        "/pub/Launch(Profile|Resource)/\\d+(-[a-z0-9]+)\\.(jnlp|jar)$");


    /**
     * Extract the files needed to launch the dashboard, from a launch profile
     * that has been downloaded locally.
     * 
     * @return the directory where the distribution was extracted.
     * @throws IOException
     *             if an error occurred during the extraction process.
     */
    static File extractDistribution(File launchProfileFile) throws IOException {
        URL resourceUrl = new URL("jar:" + launchProfileFile.toURI().toURL()
                + "!/" + RESOURCE_JAR_FILENAME);
        JarInputStream in = new JarInputStream(new BufferedInputStream(
                resourceUrl.openStream()));

        Manifest manifest = in.getManifest();
        checkPackageFormat(manifest);
        String distrDirectoryName = getDistrDirName(manifest);

        File distribDir = getDistribDirectory(true);
        File destDir = new File(distribDir, distrDirectoryName);
        extractDistribution(resourceUrl, destDir, in);

        in.close();

        return destDir;
    }

    static File getDistribDirectory(boolean local) {
        File appDir = DirectoryPreferences.getApplicationDirectory(local);
        checkApplicationDir(appDir);
        File distribDir = new File(appDir, DISTRIB_DIR_FILENAME);
        return distribDir;
    }


    // if the incoming distribution is a higher version number than we are
    // programmed to handle, abort. (This should not happen if the server
    // has been configured correctly.)
    private static void checkPackageFormat(Manifest manifest)
            throws IOException {
        String distFormatVersion = manifest.getMainAttributes()
                .getValue(DISTR_FORMAT_ATTR);
        if (distFormatVersion == null || distFormatVersion.length() == 0)
            throw new IOException("Invalid launch profile: archive format "
                    + "version number is missing.");

        if (!distFormatVersion.startsWith("1."))
            throw new IOException("Launch Profile version mismatch; "
                    + distFormatVersion + " > 1.x");
    }


    // The DirectoryPreferences class allows external configuration of the
    // application directory - for example, through a system property.
    // Here, we will enforce some limitations on the amount of configuration
    // that is allowed: the path must contain either "Process Dashboard"
    // or ".processdash". (The intent is to prevent malicious reconfiguration
    // of the application directory as a means of overwriting system files.)
    private static void checkApplicationDir(File dir) {
        String path = dir.getAbsolutePath().toLowerCase();
        if (path.contains("process dashboard") || path.contains(".processdash"))
            return;
        else
            throw new RuntimeException("Incorrect configuration for Process"
                    + " Dashboard application directory");
    }


    private static String getDistrDirName(Manifest manifest) throws IOException {
        Attributes attrs = manifest.getMainAttributes();
        String distrId = attrs.getValue(DISTR_ID_ATTR);
        String distrVersion = attrs.getValue(DISTR_VERSION_ATTR);
        String contentToken = attrs.getValue(DISTR_TOKEN_ATTR);

        if (!hasValue(distrId, distrVersion, contentToken))
            throw new IOException("Invalid launch distribution file");

        String distrDirectoryName = FileUtils.makeSafeIdentifier(
            distrId + "-" + distrVersion + "-" + contentToken);
        return distrDirectoryName;
    }


    private static boolean distributionExists(File destDir,
            boolean verifyTargetJar) {
        String[] files = destDir.list();

        // if the directory doesn't exist, the distribution doesn't either.
        if (files == null)
            return false;

        // ensure that the target jar file is present in the directory, and is
        // properly signed by the dashboard team
        for (String name : files) {
            if (TARGET_JARFILE.equalsIgnoreCase(name)) {
                try {
                    if (verifyTargetJar)
                        return JarVerifier.verify(new File(destDir, name));
                    else
                        return true;
                } catch (IOException e) {
                    return false;
                }
            }
        }

        return false;
    }


    private static void extractDistribution(URL resourceUrl, File destDir,
            JarInputStream in) throws IOException {

        if (VERIFY_DISTRIBUTION)
            if (JarVerifier.verify(resourceUrl.openStream(), true) == false)
                throw new IOException("Invalid digital signature for "
                        + RESOURCE_JAR_FILENAME);

        extractFiles(destDir, in);
    }

    private static void extractFiles(File destDir, JarInputStream in)
            throws IOException {

        try {
            // begin by deleting and recreating the distribution directory, to
            // ensure that it does not contain extraneous files.
            FileUtils.deleteDirectory(destDir, true);
            destDir.mkdirs();

            // now, extract the files from the distribution JAR.
            extractFilesImpl(destDir, in);
            touchDirectory(destDir);
        } catch (IOException ioe) {
            FileUtils.deleteDirectory(destDir, true);
            throw ioe;
        }
    }

    private static void extractFilesImpl(File destDir, JarInputStream in)
            throws IOException {
        boolean sawTargetJar = false;

        ZipEntry e;
        while ((e = in.getNextEntry()) != null) {
            String name = e.getName();

            if (name.toUpperCase().startsWith("META-INF") || e.isDirectory())
                continue;

            if (TARGET_JARFILE.equalsIgnoreCase(name))
                sawTargetJar = true;

            // check for possible maliciously formed ZIP?
            if (name.startsWith("/") || name.startsWith("\\")
                    || name.contains("//") || name.contains("\\\\")
                    || name.contains(".."))
                throw new IOException("Invalid launch distribution file");

            File destFile = new File(destDir, name);
            if (!isChildOf(destFile, destDir))
                throw new IOException("Invalid launch distribution file");

            destFile.getParentFile().mkdirs();
            FileUtils.copyFile(in, destFile);
        }

        if (sawTargetJar == false)
            throw new IOException("No " + TARGET_JARFILE
                    + " found in distribution.");
    }

    private static boolean isChildOf(File f, File dir) {
        for (int i = 10; i-- > 0;) {
            f = f.getParentFile();
            if (f == null)
                return false;
            else if (f.equals(dir))
                return true;
        }
        return false;
    }


    private static void maybeCleanupDirectory(File dir, long cutoffAge) {
        File usageLogFile = new File(dir, USAGE_LOG_FILENAME);
        if (!usageLogFile.isFile() || usageLogFile.lastModified() < cutoffAge) {
            try {
                FileUtils.deleteDirectory(dir, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static void touchDirectory(File dir) {
        try {
            File usageLogFile = new File(dir, USAGE_LOG_FILENAME);
            FileWriter out = new FileWriter(usageLogFile);
            out.write(Long.toString(System.currentTimeMillis()));
            out.close();
        } catch (IOException e) {
        }
    }

    private static boolean hasValue(String... strings) {
        for (String s : strings)
            if (s == null || s.trim().length() == 0)
                return false;
        return true;
    }

    private static final boolean LOCAL[] = { true, false };

    private static final long DAY_MILLIS = 24L /*hours*/ * 60 /*minutes*/
            * 60 /*seconds*/ * 1000; /*millis*/

}
