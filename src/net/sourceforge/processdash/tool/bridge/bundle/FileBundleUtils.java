// Copyright (C) 2021-2023 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge.bundle;

import static net.sourceforge.processdash.team.TeamDataConstants.DISSEMINATION_DIRECTORY;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import net.sourceforge.processdash.util.RobustFileOutputStream;

public class FileBundleUtils {

    /**
     * Open a stream for writing to a file in a bundle directory.
     * 
     * (This method provides a single place for modifying our policy in the
     * future, if the RobustFileOutputStream is found to cause problems with a
     * sync client.)
     */
    public static OutputStream outputStream(File destFile) throws IOException {
        return new BufferedOutputStream(
                new RobustFileOutputStream(destFile, false));
    }

    /**
     * @return true if the given directory is a bundled directory
     */
    public static boolean isBundledDir(File dir) {
        File bundles = new File(dir, FileBundleConstants.BUNDLE_SUBDIR);
        File heads = new File(dir, FileBundleConstants.HEADS_SUBDIR);
        return dir != null && bundles.isDirectory() && heads.isDirectory();
    }

    /**
     * Return the bundle mode in use by a given directory
     * 
     * @param dir
     *            the directory to check
     * @return the {@link FileBundleMode} in use, or null if the directory is
     *         not using bundled file format
     * @throws FileNotFoundException
     *             if the directory could not be reached
     * @throws IOException
     *             if the data could not be read
     */
    public static FileBundleMode getBundleMode(File dir)
            throws FileNotFoundException, IOException {
        // get the props for this directory
        Properties props = getBundleProps(dir);

        // if this is not a bundled dir, the mode is null
        if (props == null)
            return null;

        // retrieve and parse the bundle mode
        String modeStr = props.getProperty(BUNDLE_MODE_PROP);
        if (modeStr == null)
            return null;
        else
            return FileBundleMode.parse(modeStr);
    }

    /**
     * Verify that the mode of a directory matches expectations.
     * 
     * @param dir
     *            the directory to check
     * @param expectedMode
     *            the bundle mode we expect the directory to have. This can be
     *            null to indicate expectations for an unbundled directory
     * @throws FileNotFoundException
     *             if the directory could not be reached
     * @throws IOException
     *             if the data could not be read
     * @throws FileBundleModeMismatch
     *             if the bundle modes did not match
     */
    public static void ensureBundleMode(File dir, FileBundleMode expectedMode)
            throws FileNotFoundException, IOException, FileBundleModeMismatch {
        FileBundleMode dirMode = getBundleMode(dir);
        if (dirMode != expectedMode)
            throw new FileBundleModeMismatch(dir, expectedMode, dirMode);
    }

    /**
     * Read the properties file for a bundled directory, and return the contents
     * 
     * @param dir
     *            a directory, which may or may not be bundled
     * @return the bundle properties for the directory. If the directory is not
     *         bundled, returns null
     * @throws FileNotFoundException
     *             if the directory could not be reached
     * @throws IOException
     *             if the properties file could not be read
     */
    public static Properties getBundleProps(File dir)
            throws FileNotFoundException, IOException {
        // if the directory is not accessible, throw an exception
        if (!dir.isDirectory())
            throw new FileNotFoundException(dir.getPath());

        // if this is not a bundled directory, return null
        if (!isBundledDir(dir))
            return null;

        // look for the bundle properties file. Different names are used for WBS
        // directories vs dashboard directories
        if ("disseminate".equalsIgnoreCase(dir.getName()))
            dir = dir.getParentFile();
        File propsFile = new File(dir, "user-settings.ini");
        if (!propsFile.isFile())
            propsFile = new File(dir, "pspdash.ini");
        if (!propsFile.isFile())
            throw new FileNotFoundException(dir.getPath() + " - bundle props");

        // read properties from the file and return the result
        Properties result = new Properties();
        InputStream in = new FileInputStream(propsFile);
        try {
            result.load(in);
        } finally {
            in.close();
        }
        return result;
    }


    static final String BUNDLE_MODE_PROP = "bundleMode";

    /**
     * Get a prefix that can be used in log messages to identify a bundle dir.
     * 
     * @param dir
     *            a target directory or bundle directory
     */
    public static String getLogPrefix(File dir) {
        while (true) {
            if (dir == null) {
                return "";
            } else if (KNOWN_DIR_NAMES.contains(dir.getName())) {
                dir = dir.getParentFile();
            } else {
                break;
            }
        }

        String result = dir.getName();
        if (DISSEMINATION_DIRECTORY.equalsIgnoreCase(result))
            result = dir.getParentFile().getName() + "/diss";

        return result + ": ";
    }

    private static final List<String> KNOWN_DIR_NAMES = Collections
            .unmodifiableList(Arrays.asList("metadata", //
                FileBundleConstants.BUNDLE_SUBDIR,
                FileBundleConstants.HEADS_SUBDIR));

}
