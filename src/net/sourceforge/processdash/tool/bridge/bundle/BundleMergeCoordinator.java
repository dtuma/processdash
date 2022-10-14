// Copyright (C) 2022 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.merge;

import java.awt.Component;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.bridge.ReadableResourceCollection;
import net.sourceforge.processdash.tool.bridge.bundle.BundleMergeCoordinator;
import net.sourceforge.processdash.tool.bridge.bundle.BundledWorkingDirectorySync;
import net.sourceforge.processdash.tool.bridge.bundle.FileBundleDirectory;
import net.sourceforge.processdash.tool.bridge.bundle.FileBundleID;
import net.sourceforge.processdash.tool.bridge.bundle.FileBundleRetentionGranularity;
import net.sourceforge.processdash.tool.bridge.bundle.FileBundleSpec;
import net.sourceforge.processdash.tool.bridge.bundle.ForkTracker;
import net.sourceforge.processdash.tool.bridge.impl.DashboardInstanceStrategy;
import net.sourceforge.processdash.tool.bridge.impl.FileResourceCollection;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.TempFileFactory;

public class DashboardMergeCoordinator implements BundleMergeCoordinator {

    private BundledWorkingDirectorySync workingDir;

    private ForkTracker forkTracker;

    private FileBundleDirectory bundleDir;

    private DashboardBundleMerger bundleMerger;

    private static final Logger log = Logger
            .getLogger(DashboardMergeCoordinator.class.getName());

    public DashboardMergeCoordinator(BundledWorkingDirectorySync dir) {
        this.workingDir = dir;
    }


    /**
     * Merge any bundles in this dashboard that have forked.
     * 
     * @return true if any bundles were merged
     * @throws IOException
     *             if files could not be read or bundles were missing
     */
    public boolean doMerge() throws IOException {
        // on first invocation, retrieve resources we need
        maybeInitialize();

        boolean madeChange = false;

        // get a list of the bundles which have multiple distinct forks
        Map<String, List<FileBundleID>> forkedBundles = forkTracker
                .getForkedBundles();

        // iterate over the forked bundles
        for (List<FileBundleID> oneBundleForkSet : forkedBundles.values()) {
            // perform an n-way merge of the forks in the bundle
            FileBundleID mergedBundleID = mergeBundleForks(oneBundleForkSet);

            // adopt the merged bundle as our new HEAD
            forkTracker.getSelfHeadRefs().storeHeadRef(mergedBundleID);

            // make a note that we performed a merge
            madeChange = true;
        }

        return madeChange;
    }


    private void maybeInitialize() {
        if (forkTracker == null) {
            forkTracker = workingDir.getForkTracker();
            bundleDir = workingDir.getBundleDirectory();
        }
        if (bundleMerger == null) {
            // currently, we only support sync bundle mode for teams
            bundleMerger = new DashboardBundleMergerTeam();
        }
    }

    protected FileBundleID mergeBundleForks(List<FileBundleID> bundleIDs)
            throws IOException {
        // check arguments and abort if not enough bundles were provided
        if (bundleIDs.size() == 1)
            return bundleIDs.get(0);

        // create a temporary directory to hold the intermediate states of the
        // merge operation
        File tmpDir = TempFileFactory.get().createTempDirectory("td-merge",
            ".tmp");
        FileResourceCollection working = new FileResourceCollection(tmpDir,
                false, DashboardInstanceStrategy.INSTANCE);

        // merge all of the files and publish to the bundle directory
        try {
            FileBundleID mergedBundleID = mergeBundleForks(bundleIDs, working);

            // return the ID of the merged bundle we published
            return mergedBundleID;

        } finally {
            // delete the temporary directory when done
            FileUtils.safelyClose(working);
            FileUtils.deleteDirectory(tmpDir);
        }
    }

    protected FileBundleID mergeBundleForks(List<FileBundleID> bundleIDs,
            FileResourceCollection working) throws IOException {
        // sort the bundles to merge in chronological order
        Collections.sort(bundleIDs, FileBundleID.CHRONOLOGICAL_ORDER);

        // select the first bundle in the list and load its data
        FileBundleID leftBundleID = bundleIDs.get(0);
        ReadableResourceCollection left = getCollection(leftBundleID);
        String bundleName = leftBundleID.getBundleName();

        // iterate over the second and remaining bundles, merging each one into
        // our working result
        for (int i = 1; i < bundleIDs.size(); i++) {
            // retrieve the ID and data for the next bundle
            FileBundleID rightBundleID = bundleIDs.get(i);
            ReadableResourceCollection right = getCollection(rightBundleID);

            // find a shared ancestor of the two bundles we'll be merging
            FileBundleID parentBundleID = forkTracker
                    .findSharedAncestor(leftBundleID, rightBundleID, 500);
            ReadableResourceCollection parent = getCollection(parentBundleID);

            // perform a 3-way merge of the given bundles
            try {
                bundleMerger.mergeBundle(parent, left, right, working);
            } finally {
                closeAll(parent, left, right, working);
            }

            // for the next round, use the intermediate working data as the
            // left side of our merge
            left = working;

            // to pick the 3-way merge parent for the next round, we want
            // something that is a parent of both the bundles we just merged,
            // AND the next bundle in the list. We accomplish this by using
            // the parent of the just-completed merge operation on the left-hand
            // side of the ancestor search.
            leftBundleID = parentBundleID;
        }

        // the merges completed successfully. Publish the final data into a new
        // bundle in the shared bundle directory.
        FileBundleSpec spec = new FileBundleSpec(bundleName, working,
                FileBundleRetentionGranularity.All);
        spec.filenames = working.listResourceNames();
        spec.parents = bundleIDs;
        FileBundleID mergedBundleID = bundleDir.storeBundle(spec);

        // return the ID of the merged bundle we just published
        return mergedBundleID;
    }

    private ReadableResourceCollection getCollection(FileBundleID bundleID)
            throws IOException {
        if (bundleID == null)
            return ReadableResourceCollection.EMPTY_COLLECTION;
        else
            return bundleDir.getBundleCollection(bundleID);
    }

    private void closeAll(Object... closeables) {
        for (Object item : closeables) {
            if (item instanceof Closeable) {
                FileUtils.safelyClose((Closeable) item);
            }
        }
    }


    public void showMergeConflictWarnings(Component parent) {
        List<String> warningKeys = getMergeConflictWarningKeys();
        if (warningKeys.isEmpty())
            return;

        Resources res = Resources.getDashBundle("Bundler.Merge.Conflict");
        String title = res.getString("Title");
        String[] bullets = new String[warningKeys.size()];
        for (int i = bullets.length; i-- > 0;)
            bullets[i] = BULLET + res.getString(warningKeys.get(i));
        Object[] message = new Object[] { res.getStrings("Header"), bullets,
                " ", res.getStrings("Footer") };
        JOptionPane.showMessageDialog(parent, message, title,
            JOptionPane.ERROR_MESSAGE);
    }

    private List<String> getMergeConflictWarningKeys() {
        List<String> result = new ArrayList<String>();
        for (String filename : bundleMerger.getAndClearMergedFiles()) {
            String key = getMergeConflictWarningKey(filename);
            if (key == null) {
                log.warning("Overlapping edits made to file '" + filename
                        + "' - merge performed");
            } else {
                log.severe("Conflicting edits made to file '" + filename
                        + "' - data may have been lost");
                result.add(key);
            }
        }
        return result;
    }

    private String getMergeConflictWarningKey(String filename) {
        for (String[] item : MERGE_WARNING_KEYS) {
            if (filename.startsWith(item[0]))
                return item[1];
        }
        return null;
    }

    private static final String[][] MERGE_WARNING_KEYS = { //
            { "state", "Hierarchy" }, //
            { "groups.dat", "Groups" }, //
            { "roles.dat", "Roles" }, //
            { "users.dat", "Users" }, //
            { "cms/", "Reports" } //
    };

    private static final String BULLET = "    \u2022 ";

}
