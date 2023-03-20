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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import net.sourceforge.processdash.tool.bridge.ResourceCollection;
import net.sourceforge.processdash.tool.bridge.ResourceCollectionInfo;
import net.sourceforge.processdash.tool.bridge.ResourceFilterFactory;
import net.sourceforge.processdash.tool.bridge.ResourceListing;
import net.sourceforge.processdash.tool.bridge.impl.FileResourceCollectionStrategy;
import net.sourceforge.processdash.tool.bridge.report.ResourceCollectionDiff;
import net.sourceforge.processdash.util.NullSafeObjectUtils;
import net.sourceforge.processdash.util.StringUtils;

public class ResourceBundleClient {

    private ResourceCollection workingDir;

    private HeadRefsManifestSource workingHeads;

    private FileBundleDirectory bundleDir;

    private HeadRefs bundleHeads;

    private FilenameFilter syncDownOnlyFilter;

    private FileBundlePartitioner partitioner;

    private String retentionThresholdTimestamp;

    private long useModTimeForBundleTimestamp;

    protected String logPrefix;

    private static final Logger logger = Logger
            .getLogger(ResourceBundleClient.class.getName());

    public ResourceBundleClient(FileResourceCollectionStrategy strategy,
            ResourceCollection workingDirectory, HeadRefs workingHeads,
            File manifestCacheDirectory, File bundleDirectory,
            HeadRefs bundleHeads, FilenameFilter syncDownOnlyFilter)
            throws IOException {
        this.workingDir = workingDirectory;
        this.bundleDir = new FileBundleDirectory(bundleDirectory);
        this.workingHeads = new ManifestCachingHeadRefs(workingHeads, bundleDir,
                manifestCacheDirectory);
        this.bundleHeads = bundleHeads;
        this.syncDownOnlyFilter = syncDownOnlyFilter;
        this.partitioner = new FileBundlePartitioner(strategy, workingDir,
                workingHeads, this.workingHeads);
        setRetentionThresholdTimestamp();
        this.useModTimeForBundleTimestamp = -1;
        this.logPrefix = FileBundleUtils.getLogPrefix(bundleDirectory);

        logger.finest(logPrefix + "Locally checked out bundles: "
                + workingHeads.getHeadRefs().values());
    }

    public void setRetentionThresholdTimestamp() {
        retentionThresholdTimestamp = bundleDir.getCurrentTimeBundleID()
                .getTimestamp();
    }

    /**
     * Configure whether newly published bundles should receive a timestamp that
     * reflects the modification time of the files they contain.
     * 
     * @param modTimestamp
     *            <tt>-1</tt> to disable the use of file modification times for
     *            bundle timestamp generation. Any number greater than or equal
     *            to zero will arrange for bundles to receive a timestamp
     *            reflecting the modification time of the newest file they
     *            contain, or this value, whichever is higher.
     */
    public void setUseModTimeForBundleTimestamp(long modTimestamp) {
        this.useModTimeForBundleTimestamp = modTimestamp;
    }

    public ResourceCollection getWorkingDir() {
        return workingDir;
    }

    public HeadRefs getWorkingHeads() {
        return workingHeads;
    }

    public FileBundleManifestSource getManifests() {
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
    public synchronized boolean syncDown() throws IOException {
        // keep track of whether any changes were made
        boolean madeChange = false;

        // list the bundles that were present in the working dir in the past
        Map<String, FileBundleID> oldHeadRefs = workingHeads.getHeadRefs();
        Set<String> oldBundleNames = new HashSet<String>(oldHeadRefs.keySet());

        // get the new set of refs from the bundle directory. If it is
        // unexpectedly empty, a network issue is likely to blame. Abort
        Map<String, FileBundleID> newHeadRefs = bundleHeads.getHeadRefs();
        if (newHeadRefs.isEmpty() && !oldHeadRefs.isEmpty())
            throw new IOException("Could not read bundle heads");

        Set<String> obsoleteFilenames = new HashSet<String>();
        Set<String> currentFilenames = new HashSet<String>();
        for (FileBundleID bundleID : newHeadRefs.values()) {
            // keep track of the bundles we encounter
            String bundleName = bundleID.getBundleName();
            oldBundleNames.remove(bundleName);

            // if the bundle HEAD has not changed, no extraction is needed
            FileBundleID oldBundleID = oldHeadRefs.get(bundleName);
            if (bundleID.equals(oldBundleID)) {
                currentFilenames.addAll(getBundleFilenames(bundleID));
                if (restoreLocallyModifiedSyncDownOnlyFiles(bundleID))
                    madeChange = true;

            } else {
                // extract the new bundle into the directory
                logger.fine(logPrefix + "Syncing down " + bundleID);
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
            logger.finest(logPrefix + "Deleting bundle " + obsoleteBundleName);
            FileBundleID obsoleteBundleID = oldHeadRefs.get(obsoleteBundleName);
            obsoleteFilenames.addAll(getBundleFilenames(obsoleteBundleID));
            workingHeads.deleteHeadRef(obsoleteBundleName);
            madeChange = true;
        }

        // if any files are obsolete, delete them
        for (String filename : obsoleteFilenames) {
            if (!StringUtils.containsIgnoreCase(currentFilenames, filename)) {
                logger.finest(logPrefix + "Deleting file " + filename);
                workingDir.deleteResource(filename);
                madeChange = true;
            }
        }

        // let our caller know whether any changes were made
        return madeChange;
    }

    private List<String> getBundleFilenames(FileBundleID bundleID)
            throws IOException {
        FileBundleManifest manifest = workingHeads.getManifest(bundleID);
        return manifest.getFiles().listResourceNames();
    }

    private boolean restoreLocallyModifiedSyncDownOnlyFiles(
            FileBundleID bundleID) throws IOException {
        // if no "sync down only" filter is in effect, return false
        if (syncDownOnlyFilter == null)
            return false;

        // scan the files that were previously extracted from this bundle
        List<String> filesToRestore = new ArrayList<String>();
        FileBundleManifest manifest = workingHeads.getManifest(bundleID);
        ResourceCollectionInfo prevFiles = manifest.getFiles();
        for (String filename : prevFiles.listResourceNames()) {
            // if this is a sync down only file, see if it is locally changed
            if (syncDownOnlyFilter.accept(null, filename)) {
                Long prevSum = prevFiles.getChecksum(filename);
                Long currSum = workingDir.getChecksum(filename);
                if (!NullSafeObjectUtils.EQ(prevSum, currSum))
                    filesToRestore.add(filename);
            }
        }

        // if no changes are needed, return false
        if (filesToRestore.isEmpty())
            return false;

        // re-extract the files and return true to indicate a change was made
        bundleDir.extractBundle(bundleID, workingDir, filesToRestore);
        logger.finest(logPrefix + "Restored " + filesToRestore);
        return true;
    }

    private ResourceCollectionDiff extractBundle(FileBundleID bundleID,
            FileBundleID oldBundleID, boolean overwrite) throws IOException {
        // get a diff between the new and old bundles
        ResourceCollectionInfo oldFiles = (oldBundleID == null
                ? ResourceCollectionInfo.EMPTY_COLLECTION
                : workingHeads.getManifest(oldBundleID).getFiles());
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
        logger.finest(logPrefix + "Extracted " + filesToExtract);

        // return the diff
        return diff;
    }



    /**
     * Check to make sure all working heads reference valid bundles
     * 
     * In certain rare scenarios, bundles may disappear from a bundle storage
     * dir. (For example, this could happen after an end user deletes/renames a
     * bundled directory.) Afterward, previously created working directories
     * might reference no-longer-existent bundles. This method checks for that
     * scenario and returns false if it applies.
     */
    public synchronized boolean workingHeadsAreValid() throws IOException {
        for (FileBundleID bundleID : workingHeads.getHeadRefs().values()) {
            try {
                bundleDir.getManifest(bundleID);
            } catch (FileBundleManifest.Missing m) {
                // the locally checked out bundle is no longer present in the
                // remote bundle dir. It might have been replaced though. If it
                // wasn't replaced by another bundle, it is invalid.
                if (bundleDir.getManifestReplacing(bundleID) == null) {
                    logger.warning(logPrefix + "Invalid ref: " + bundleID);
                    return false;
                }
            }
        }

        // no invalid refs were found.
        return true;
    }



    /**
     * Overwrite files in the working directory with fresh copies of the files
     * as they looked when they were previously checked out.
     * 
     * @param filenames
     *            the names of files to restore
     * @return true if any changes were made
     */
    public synchronized boolean restoreFiles(List<String> filenames)
            throws IOException {
        // keep track of whether any changes were made
        boolean madeChange = false;

        // iterate over the bundles that are currently in the working dir
        for (FileBundleID bundleID : workingHeads.getHeadRefs().values()) {
            // ask each bundle to extract the named files. If this bundle
            // doesn't contain any of the named files, this will be a no-op
            ResourceCollectionInfo extracted = bundleDir.extractBundle(bundleID,
                workingDir, filenames);

            // make a note of whether any files were extracted
            List<String> extractedFiles = extracted.listResourceNames();
            if (!extractedFiles.isEmpty()) {
                logger.finest(logPrefix + "Restored " + extractedFiles);
                madeChange = true;
            }
        }

        // let our caller know whether any changes were made
        return madeChange;
    }



    /**
     * Compare the working directory to the previously checked out HEAD refs. If
     * any changes are found, return true.
     * 
     * @return true if the working directory contains pending changes that need
     *         to be synced up
     */
    public synchronized boolean isDirty() throws IOException {
        return syncUpImpl(null, true);
    }



    /**
     * Compare the working directory to the previously checked out HEAD refs. If
     * any changes are found, publish them.
     * 
     * @return true if changes were published, false if none were needed
     */
    public synchronized boolean syncUp() throws IOException {
        return syncUpImpl(null, false);
    }


    /**
     * Publish the bundle containing the given file, if it has changed.
     * 
     * @return true if changes were published, false if none were needed
     */
    public synchronized boolean syncUp(String singleFilename)
            throws IOException {
        return syncUpImpl(singleFilename, false);
    }


    private boolean syncUpImpl(String singleFilename, boolean whatIfMode)
            throws IOException {
        Map<String, FileBundleID> workingHeadRefs = workingHeads.getHeadRefs();
        Set<FileBundleID> obsoleteRefs = new HashSet<FileBundleID>();
        Set<FileBundleID> newRefs = new HashSet<FileBundleID>();
        FileBundleID currentTime = bundleDir.getCurrentTimeBundleID();

        // have the partitioner compute the bundles needed for the working dir
        for (FileBundleSpec spec : partitioner.partition()) {
            // see if the files in this bundle have changed
            if (isBundleChange(spec, singleFilename, workingHeadRefs)) {
                if (whatIfMode) {
                    // in what-if mode, return true to indicate a change
                    return true;
                } else {
                    // in regular mode, publish the new/changed bundle
                    if (useModTimeForBundleTimestamp >= 0)
                        assignModTimeForBundleTimestamp(spec);
                    else
                        checkBundleRetention(spec, currentTime, obsoleteRefs);
                    FileBundleID newBundleID = bundleDir.storeBundle(spec);
                    logger.fine(logPrefix + "Synced up " + newBundleID);
                    newRefs.add(newBundleID);
                }
            }
        }

        // if any new bundles were stored, save their refs
        boolean madeChange = !newRefs.isEmpty();
        if (madeChange) {
            workingHeads.storeHeadRefs(newRefs);
            bundleHeads.storeHeadRefs(newRefs);
            bundleDir.deleteBundles(obsoleteRefs);
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
        FileBundleManifest manifest = workingHeads
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
            return StringUtils.containsIgnoreCase(filenames, singleFilename);
    }

    private void assignModTimeForBundleTimestamp(FileBundleSpec spec) {
        // if this spec already has an explicit timestamp, don't change it
        if (spec.timestamp > 0)
            return;

        // begin with the mod timestamp we've been configured to use
        long modTime = useModTimeForBundleTimestamp;

        // if any files in this bundle are newer, adopt their mod time
        for (String filename : spec.filenames)
            modTime = Math.max(modTime, spec.source.getLastModified(filename));

        // if no file mod times were found, fallback to regular timestamp logic
        if (modTime == 0)
            return;

        // ensure the selected bundle timestamp is later than its parent bundle
        FileBundleTimeFormat fmt = bundleDir.getTimeFormat();
        for (FileBundleID parent : spec.parents) {
            try {
                long parentTime = fmt.parse(parent.getTimestamp()).getTime();
                modTime = Math.max(modTime, parentTime + 1000);
            } catch (Exception e) {
            }
        }

        // assign this modification timestamp to the bundle spec
        spec.timestamp = modTime;
    }

    private void checkBundleRetention(FileBundleSpec spec,
            FileBundleID currentTime, Set<FileBundleID> obsoleteRefs)
            throws IOException {
        // if the bundle has no parents, or has multiple, don't discard it
        if (spec.parents == null || spec.parents.size() != 1)
            return;

        // if the parent was written by a different user/computer, keep it
        FileBundleID parentID = spec.parents.get(0);
        if (!parentID.getDeviceID().equals(currentTime.getDeviceID()))
            return;

        // if the parent was written before our retention theshold, keep it
        if (parentID.getTimestamp().compareTo(retentionThresholdTimestamp) <= 0)
            return;

        // if the parent was a merge of multiple grandparents, don't discard it
        FileBundleManifest parentManifest = bundleDir.getManifest(parentID);
        if (parentManifest.getParents().size() > 1)
            return;

        // ask the retention granularity in the spec to decide if we keep it
        if (spec.retentionGranularity.shouldRetainPreviousBundle(parentID,
            currentTime))
            return;

        // the parent bundle is obsolete. Point our new bundle at its parents,
        // and make a note to mark it for deletion
        obsoleteRefs.add(parentID);
        spec.parents = parentManifest.getParents();
        spec.replaces = parentManifest.getReplaces();
        if (spec.replaces.isEmpty())
            spec.replaces = Collections.singletonList(parentID);
    }



    /**
     * Log files are excluded from sync up/down operations by default. This
     * method publishes those files to the bundle directory.
     * 
     * @param bundleTimestamp the publishing timestamp to use for the bundle
     * @param afterCrash true if this is a log file from a crashed session
     */
    public synchronized void saveLogBundle(long bundleTimestamp,
            boolean afterCrash) throws IOException {
        // create a bundle to use for saving log data
        String bundleName = FileBundleConstants.LOG_BUNDLE;
        if (afterCrash)
            bundleName += ",crashed";
        FileBundleSpec spec = new FileBundleSpec(bundleName, workingDir,
                FileBundleRetentionGranularity.None);
        spec.timestamp = bundleTimestamp;

        // scan the working directory for log/excluded files
        for (String oneFile : ResourceFilterFactory.DEFAULT_EXCLUDE_FILENAMES) {
            if (workingDir.getLastModified(oneFile) > 0)
                spec.filenames.add(oneFile);
        }

        // if any files were found, publish them
        if (!spec.filenames.isEmpty())
            bundleDir.storeBundle(spec);
    }

}
