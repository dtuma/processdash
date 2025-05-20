// Copyright (C) 2022-2025 Tuma Solutions, LLC
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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.sourceforge.processdash.tool.bridge.ReadableResourceCollection;
import net.sourceforge.processdash.tool.bridge.impl.FileResourceCollection;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.TempFileFactory;

public abstract class BundleMergeCoordinator {

    protected BundledWorkingDirectorySync workingDir;

    protected ForkTracker forkTracker;

    protected FileBundleDirectory bundleDir;

    protected BundleMerger bundleMerger;

    protected String logPrefix;

    protected static final Logger log = Logger
            .getLogger(BundleMergeCoordinator.class.getName());

    public BundleMergeCoordinator(BundledWorkingDirectorySync dir) {
        this.workingDir = dir;
        this.logPrefix = dir.logPrefix;
    }


    /**
     * Merge any bundles in this working directory that have forked.
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
            // skip this bundle if not enough forks are present
            if (oneBundleForkSet.size() < 2)
                continue;

            // perform an n-way merge of the forks in the bundle
            FileBundleID mergedBundleID = mergeBundleForks(oneBundleForkSet);

            if (mergedBundleID != null) {
                // adopt the merged bundle as our new HEAD
                forkTracker.getSelfHeadRefs().storeHeadRef(mergedBundleID);

                // make a note that we performed a merge
                madeChange = true;
            }
        }

        return madeChange;
    }


    private void maybeInitialize() {
        if (forkTracker == null) {
            forkTracker = workingDir.getForkTracker();
            bundleDir = workingDir.getBundleDirectory();
        }
        if (bundleMerger == null) {
            bundleMerger = makeBundleMerger();
        }
    }

    protected abstract BundleMerger makeBundleMerger();


    protected FileBundleID mergeBundleForks(List<FileBundleID> bundleIDs)
            throws IOException {
        // create a temporary directory to hold the intermediate states of the
        // merge operation
        File tmpDir = TempFileFactory.get().createTempDirectory("td-merge",
            ".tmp");
        FileResourceCollection working = new FileResourceCollection(tmpDir,
                false, workingDir.getStrategy());

        // merge all of the files and publish to the bundle directory
        try {
            log.fine(logPrefix + "Merging forked bundles " + bundleIDs);
            FileBundleID mergedBundleID = mergeBundleForks(bundleIDs, working);

            // return the ID of the merged bundle we published
            log.info(logPrefix + "Published merge bundle " + mergedBundleID);
            return mergedBundleID;

        } finally {
            // delete the temporary directory when done
            FileUtils.safelyClose(working);
            FileUtils.deleteDirectory(tmpDir);
        }
    }

    protected FileBundleID mergeBundleForks(List<FileBundleID> bundleIDs,
            FileResourceCollection working) throws IOException {
        // select the first bundle in the list and load its data
        FileBundleID leftBundleID = bundleIDs.get(0);
        ReadableResourceCollection left = getCollection(leftBundleID);
        String bundleName = leftBundleID.getBundleName();
        log.finer(logPrefix + "Merging bundle " + bundleName
                + " beginning with " + leftBundleID);

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
                log.finer(logPrefix + "Merging in fork " + rightBundleID
                        + " using parent " + parentBundleID);
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
        tweakSpec(spec);
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

    protected void tweakSpec(FileBundleSpec spec) {}

}
