// Copyright (C) 2021 Tuma Solutions, LLC
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.processdash.util.DashboardBackupFactory;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.PatternList;

public class FileBundlePartitioner {

    private File srcDir;

    private FilenameFilter filter;

    private String catchAllBundleName;

    private HeadRefs headRefs;

    private FileBundleManifestSource manifests;

    private long logFileTime = -1;

    public FileBundlePartitioner(File sourceDir, FilenameFilter inclusionFilter,
            String catchAllBundleName, HeadRefs localHeadRefs,
            FileBundleManifestSource manifestSource) {
        this.srcDir = sourceDir;
        this.filter = inclusionFilter;
        this.catchAllBundleName = catchAllBundleName;
        this.headRefs = localHeadRefs;
        this.manifests = manifestSource;
    }

    /**
     * Set the timestamp to be used when building the ID of the logfile bundle
     */
    public void setLogFileTime(long logFileTimestamp) {
        this.logFileTime = logFileTimestamp;
    }


    /**
     * Look at the files currently present in the source directory, and decide
     * which bundles should be built to store them.
     */
    public List<FileBundleSpec> partition() throws IOException {
        // if the source directory cannot be reached, abort
        if (!srcDir.isDirectory())
            throw new FileNotFoundException(srcDir.getPath());

        // list the files in the source directory that match the filter
        List<String> filenames = FileUtils.listRecursively(srcDir, filter);

        // scan the bundles that were previously extracted to this directory
        Map<String, FileBundleSpec> result = new HashMap();
        Map<String, String> oldFileBundles = new HashMap();
        for (FileBundleID bundleID : headRefs.getHeadRefs().values()) {
            // the set of bundles for a working directory never shrinks; we only
            // see that some bundles become empty. Reflect this by preloading
            // empty file lists for each currently known bundle.
            result.put(bundleID.getBundleName(), makeSpec(bundleID));

            // scan prior bundles for included files that our filter didn't
            // recognize. (This could happen if the bundles were written by a
            // newer version of the dashboard.)
            FileBundleManifest manifest = manifests.getManifest(bundleID);
            for (String oldFilename : manifest.getFiles().listResourceNames()) {
                // make a note of the bundle each prior file came from
                oldFileBundles.put(oldFilename, bundleID.getBundleName());

                // if this file was not picked up by the filter, but it still
                // exists in the source directory, add it to our list to process
                if (!filenames.contains(oldFilename)) {
                    File oldFile = new File(srcDir, oldFilename);
                    if (oldFile.isFile())
                        filenames.add(oldFilename);
                }
            }
        }

        // identify the correct bundle for each included file
        for (String oneFilename : filenames) {
            String destBundleName = getBundleNameForFilename(oneFilename,
                oldFileBundles);
            FileBundleSpec spec = result.get(destBundleName);
            if (spec == null) {
                spec = makeSpec(destBundleName);
                result.put(destBundleName, spec);
            }
            spec.filenames.add(oneFilename);
        }

        // return the list of partitioned bundles we've built
        return new ArrayList<FileBundleSpec>(result.values());
    }


    private FileBundleSpec makeSpec(FileBundleID bundleID) throws IOException {
        FileBundleSpec result = makeSpec(bundleID.getBundleName());
        result.parents = Collections.singletonList(bundleID);
        return result;
    }

    private FileBundleSpec makeSpec(String bundleName) {
        FileBundleSpec result = new FileBundleSpec(bundleName, srcDir);
        if (LOG_PARTITION.equals(bundleName))
            result.timestamp = logFileTime;
        return result;
    }


    private String getBundleNameForFilename(String filename,
            Map<String, String> oldFileBundles) throws IOException {
        // use case-insensitive logic to sort filenames into bundles
        String filenameLC = filename.toLowerCase();

        // iterate over the standard partition types to see if any match
        for (Object[] partitionSpec : STANDARD_PARTITIONS) {
            if (matches(filename, filenameLC, partitionSpec)) {
                String bundleName = (String) partitionSpec[0];
                if (SINGLETON_PARTITION.equals(bundleName)) {
                    // certain files go into bundles by themselves. If this file
                    // meets that criteria, its filename is its bundle name
                    return FileBundleID.filenameToBundleName(filenameLC);
                } else {
                    // return the bundle we found
                    return bundleName;
                }
            }
        }

        // if we weren't able to identify an appropriate target bundle,
        // determine the bundle this file came from and put it back there
        String sourceBundle = oldFileBundles.get(filename);
        if (sourceBundle != null)
            return sourceBundle;

        // if we weren't able to identify an appropriate target bundle, put
        // the file into the default bundle.
        return catchAllBundleName;
    }


    private boolean matches(String filename, String filenameLC,
            Object[] partitionSpec) {
        for (int i = 1; i < partitionSpec.length; i++) {
            Object filter = partitionSpec[i];

            // with a simple string, check for case-sensitive comparison
            if (filter instanceof String) {
                if (((String) filter).equals(filenameLC))
                    return true;
            }

            // with a PatternList, check for a case-insensitive match
            else if (filter instanceof PatternList) {
                if (((PatternList) filter).matches(filenameLC))
                    return true;
            }

            // ask a FilenameFilter whether it accepts the given file
            else if (filter instanceof FilenameFilter) {
                if (((FilenameFilter) filter).accept(srcDir, filename))
                    return true;
            }
        }

        // this filename did not match any of the listed filters.
        return false;
    }




    private static final String LOG_PARTITION = "log";

    private static final String SINGLETON_PARTITION = "*";

    private static final Object[][] STANDARD_PARTITIONS = {

            // log files are stored separately
            { LOG_PARTITION, "log.txt" },

            // Dashboard directory - metadata
            { "meta", "datasetid.dat", //
                    "groups.dat", "roles.dat", "users.dat", //
                    new PatternList() //
                            .addLiteralStartsWith("cms/")
                            .addLiteralStartsWith("import/")
                            .addLiteralEndsWith(".ico") },

            // Dashboard directory - core data files
            { "core", "state", "pspdash.ini", ".pspdash" },

            // Dashboard directory - time log files
            { "time", "timelog.xml", "timelog2.xml", "time.log" },

            // Dashboard directory - EV baseline files
            { SINGLETON_PARTITION, new PatternList("^ev-.+\\.dat$") },

            // Dashboard directory - project data files
            { "data", new PatternList() //
                            .addLiteralEndsWith(".dat") //
                            .addLiteralEndsWith(".def") },


            // WBS directory - metadata
            { "settings", "settings.xml" },

            // WBS directory - PDASH files
            { SINGLETON_PARTITION, new PatternList("^[^/]+\\.pdash$") },

            // WBS directory - standard files
            { "wbs", DashboardBackupFactory.WBS_FILE_FILTER, "projdump.xml",
                    "workflowdump.xml", "relaunchdump.xml" },

    };

}
