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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sourceforge.processdash.tool.bridge.ResourceCollection;
import net.sourceforge.processdash.tool.bridge.ResourceCollectionInfo;
import net.sourceforge.processdash.tool.bridge.ResourceFilterFactory;
import net.sourceforge.processdash.tool.bridge.ResourceListing;
import net.sourceforge.processdash.tool.bridge.impl.FileResourceCollectionStrategy;
import net.sourceforge.processdash.tool.bridge.report.ResourceCollectionDiff;

public class ResourceBundleClient {

    private ResourceCollection workingDir;

    private HeadRefs workingHeads;

    private FileBundleDirectory bundleDir;

    private HeadRefs bundleHeads;

    private FileBundlePartitioner partitioner;

    private long logBundleTimestamp;

    public ResourceBundleClient(FileResourceCollectionStrategy strategy,
            ResourceCollection workingDirectory, HeadRefs workingHeads,
            File bundleDirectory, HeadRefs bundleHeads) throws IOException {
        this.workingDir = workingDirectory;
        this.workingHeads = workingHeads;
        this.bundleDir = new FileBundleDirectory(bundleDirectory);
        this.bundleHeads = bundleHeads;
        this.partitioner = new FileBundlePartitioner(strategy, workingDir,
                workingHeads, bundleDir);
        this.logBundleTimestamp = System.currentTimeMillis() - 1000;
    }

    public ResourceCollection getWorkingDir() {
        return workingDir;
    }

    public HeadRefs getWorkingHeads() {
        return workingHeads;
    }

    public FileBundleDirectory getBundleDir() {
        return bundleDir;
    }

    public HeadRefs getBundleHeads() {
        return bundleHeads;
    }



    /**
     * Compare the bundle directory to the previously checked out HEAD refs. If
     * any changes are found, copy them down.
     * 
     * Files that were unchanged in the bundle directory are not modified, so
     * local changes will still have a chance to be synced up in the future.
     * 
     * @return true if any changes were made, false if the working directory was
     *         already up to date with the bundle HEADS.
     */
    public boolean syncDown() throws IOException {
        // keep track of whether any changes were made
        boolean madeChange = false;

        // list the bundles that were present in the working dir in the past
        Map<String, FileBundleID> oldHeadRefs = workingHeads.getHeadRefs();
        Set<String> oldBundleNames = new HashSet<String>(oldHeadRefs.keySet());

        Set<String> obsoleteFilenames = new HashSet<String>();
        Set<String> currentFilenames = new HashSet<String>();
        for (FileBundleID bundleID : bundleHeads.getHeadRefs().values()) {
            // keep track of the bundles we encounter
            String bundleName = bundleID.getBundleName();
            oldBundleNames.remove(bundleName);

            // if the bundle HEAD has not changed, no extraction is needed
            FileBundleID oldBundleID = oldHeadRefs.get(bundleName);
            if (bundleID.equals(oldBundleID)) {
                currentFilenames.addAll(getBundleFilenames(bundleID));

            } else {
                // extract the new bundle into the directory
                ResourceCollectionDiff diff = extractBundle(bundleID,
                    oldBundleID, false);
                workingHeads.storeHeadRef(bundleID);
                madeChange = true;

                // keep track of the old and new files in the working directory
                obsoleteFilenames.addAll(diff.getOnlyInA());
                currentFilenames.addAll(diff.getB().listResourceNames());
            }
        }

        // if any bundles disappeared from the directory, find the names of
        // the files they contained
        for (String obsoleteBundleName : oldBundleNames) {
            FileBundleID obsoleteBundleID = oldHeadRefs.get(obsoleteBundleName);
            obsoleteFilenames.addAll(getBundleFilenames(obsoleteBundleID));
        }

        // if any files are obsolete, delete them
        for (String filename : obsoleteFilenames) {
            if (!containsIgnoreCase(currentFilenames, filename)) {
                workingDir.deleteResource(filename);
                madeChange = true;
            }
        }

        // let our caller know whether any changes were made
        return madeChange;
    }

    private List<String> getBundleFilenames(FileBundleID bundleID)
            throws IOException {
        FileBundleManifest manifest = bundleDir.getManifest(bundleID);
        return manifest.getFiles().listResourceNames();
    }

    private ResourceCollectionDiff extractBundle(FileBundleID bundleID,
            FileBundleID oldBundleID, boolean overwrite) throws IOException {
        // get a diff between the new and old bundles
        ResourceCollectionInfo oldFiles = (oldBundleID == null
                ? ResourceCollectionInfo.EMPTY_COLLECTION
                : bundleDir.getManifest(oldBundleID).getFiles());
        ResourceCollectionInfo newFiles = bundleDir.getManifest(bundleID)
                .getFiles();
        ResourceCollectionDiff diff = new ResourceCollectionDiff(oldFiles,
                newFiles);

        // extract files from the new bundle as requested
        List<String> filesToExtract = null;
        if (!overwrite) {
            filesToExtract = new ArrayList<String>();
            filesToExtract.addAll(diff.getDiffering());
            filesToExtract.addAll(diff.getOnlyInB());
        }
        bundleDir.extractBundle(bundleID, workingDir, filesToExtract);

        // return the diff
        return diff;
    }

    private boolean containsIgnoreCase(Collection<String> set, String item) {
        if (set.contains(item))
            return true;
        for (String oneItem : set) {
            if (item.equalsIgnoreCase(oneItem))
                return true;
        }
        return false;
    }



    /**
     * Overwrite files in the working directory with fresh copies of the files
     * as they looked when they were previously checked out.
     * 
     * @param filenames
     *            the names of files to restore
     * @return true if any changes were made
     */
    public boolean restoreFiles(List<String> filenames) throws IOException {
        // keep track of whether any changes were made
        boolean madeChange = false;

        // iterate over the bundles that are currently in the working dir
        for (FileBundleID bundleID : workingHeads.getHeadRefs().values()) {
            // ask each bundle to extract the named files. If this bundle
            // doesn't contain any of the named files, this will be a no-op
            ResourceCollectionInfo extracted = bundleDir.extractBundle(bundleID,
                workingDir, filenames);

            // make a note of whether any files were extracted
            if (!extracted.listResourceNames().isEmpty())
                madeChange = true;
        }

        // let our caller know whether any changes were made
        return madeChange;
    }



    /**
     * Compare the working directory to the previously checked out HEAD refs. If
     * any changes are found, publish them.
     * 
     * @return true if changes were published, false if none were needed
     */
    public boolean syncUp() throws IOException {
        return syncUp(null);
    }


    /**
     * Publish the bundle containing the given file, if it has changed.
     * 
     * @return true if changes were published, false if none were needed
     */
    public boolean syncUp(String singleFilename) throws IOException {
        Map<String, FileBundleID> workingHeadRefs = workingHeads.getHeadRefs();
        Set<FileBundleID> newRefs = new HashSet<FileBundleID>();

        // have the partitioner compute the bundles needed for the working dir
        for (FileBundleSpec spec : partitioner.partition()) {
            // if the files in this bundle have changed, publish it
            if (isBundleChange(spec, singleFilename, workingHeadRefs)) {
                FileBundleID newBundleID = bundleDir.storeBundle(spec);
                newRefs.add(newBundleID);
            }
        }

        // if any new bundles were stored, save their refs
        boolean madeChange = !newRefs.isEmpty();
        if (madeChange) {
            workingHeads.storeHeadRefs(newRefs);
            bundleHeads.storeHeadRefs(newRefs);
        }

        // let our caller know if any changes were made
        return madeChange;
    }

    private boolean isBundleChange(FileBundleSpec spec, String singleFilename,
            Map<String, FileBundleID> workingHeadRefs) throws IOException {
        // get the last bundleID that was extracted for this bundle. If this
        // bundle wasn't present in the directory before, it must be new
        FileBundleID previouslyExtractedBundleID = workingHeadRefs
                .get(spec.bundleName);
        if (previouslyExtractedBundleID == null)
            return matchesSingleFilename(singleFilename, spec.filenames);

        // get info for files that were previously extracted for this bundle
        FileBundleManifest manifest = bundleDir
                .getManifest(previouslyExtractedBundleID);
        ResourceCollectionInfo previouslyExtractedFiles = manifest.getFiles();

        // if we're syncing a single file which isn't in this bundle, abort
        if (!matchesSingleFilename(singleFilename, spec.filenames)
                && !matchesSingleFilename(singleFilename,
                        previouslyExtractedFiles.listResourceNames())) {
            return false;
        }

        // gather current info for the local files in the working directory
        ResourceCollectionInfo currentLocalFiles = new ResourceListing(
                workingDir, spec.filenames);

        // compare the two collections to see if they represent any changes
        ResourceCollectionDiff diff = new ResourceCollectionDiff(
                previouslyExtractedFiles, currentLocalFiles);
        return diff.noDifferencesFound() == false;
    }

    private boolean matchesSingleFilename(String singleFilename,
            List<String> filenames) {
        if (singleFilename == null)
            return true;
        else
            return containsIgnoreCase(filenames, singleFilename);
    }



    /**
     * Log files are excluded from sync up/down operations by default. This
     * method publishes those files to the bundle directory.
     */
    public void saveDefaultExcludedFiles() throws IOException {
        // scan the working directory for excluded files
        FileBundleSpec spec = new FileBundleSpec(FileBundleConstants.LOG_BUNDLE,
                workingDir);
        spec.timestamp = logBundleTimestamp;
        for (String oneFile : ResourceFilterFactory.DEFAULT_EXCLUDE_FILENAMES) {
            if (workingDir.getLastModified(oneFile) > logBundleTimestamp)
                spec.filenames.add(oneFile);
        }

        // if any excluded files were found, publish them
        if (!spec.filenames.isEmpty())
            bundleDir.storeBundle(spec);
    }

}
