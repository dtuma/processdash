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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import net.sourceforge.processdash.tool.bridge.ResourceCollectionInfo;
import net.sourceforge.processdash.tool.bridge.ResourceListing;

public class ForkTracker {

    /** An object for retrieving manifests for this bundle directory */
    private FileBundleManifestSource manifestSource;

    /** An object for testing whether bundle files are intact */
    private FileBundleValidator bundleValidator;

    /** The directory containing HEADs files for this bundle directory */
    private File bundleHeadsDir;

    /** The prefix used to build HEADs files for each device */
    private String filenamePrefix;

    /** The DeviceID of the local computer */
    private String selfDeviceID;

    /** The names of bundles that use an "overwrite" strategy for conflicts */
    private Set<String> overwriteBundleNames;

    /** The HEADs file used to store refs for the local computer */
    private File selfHeadsFile;

    /** HeadRefs object to manage refs for the local computer */
    private SelfHeadRefs selfHeadRefs;

    /**
     * HeadRefs objects for all devices that have accessed this directory, keyed
     * by the file holding the HEADs for that device.
     */
    private Map<File, HeadRefsPropertiesFile> deviceHeadRefs;

    private String logPrefix;

    private static final Logger logger = Logger
            .getLogger(ForkTracker.class.getName());


    public ForkTracker(File bundleHeadsDir, String filenamePrefix,
            String selfDeviceID, Set<String> overwriteBundleNames,
            File headStorageDir) {
        this.bundleHeadsDir = bundleHeadsDir;
        this.filenamePrefix = filenamePrefix;
        this.selfDeviceID = selfDeviceID;
        this.overwriteBundleNames = overwriteBundleNames;

        this.selfHeadsFile = getHeadsFileForDevice(selfDeviceID);
        File selfStorageFile = new File(headStorageDir, selfHeadsFile.getName());
        this.selfHeadRefs = new SelfHeadRefs(selfStorageFile, selfHeadsFile);

        this.deviceHeadRefs = Collections.synchronizedMap(new HashMap());
        this.deviceHeadRefs.put(selfHeadsFile, selfHeadRefs);

        this.logPrefix = FileBundleUtils.getLogPrefix(bundleHeadsDir);
    }


    public void setManifestSource(FileBundleManifestSource manifestSource) {
        this.manifestSource = manifestSource;
    }


    public void setBundleValidator(FileBundleValidator bundleValidator) {
        this.bundleValidator = bundleValidator;
    }


    public HeadRefs getSelfHeadRefs() {
        return selfHeadRefs;
    }


    public boolean fastForward() throws IOException {
        // find the distinct forks for each bundle
        BundleForks bundleForks = getBundleForks();

        // calculate fast-forward opportunities for each bundle
        List<FileBundleID> fastForwardRefs = new ArrayList<FileBundleID>();
        for (String bundleName : bundleForks.keySet())
            calcFastForward(bundleForks, bundleName, fastForwardRefs);

        if (fastForwardRefs.isEmpty()) {
            return false;
        } else {
            logger.finest(logPrefix + "Fast forwarding to: " + fastForwardRefs);
            selfHeadRefs.storeHeadRefs(fastForwardRefs);
            return true;
        }
    }

    private boolean calcFastForward(BundleForks bundleForks, String bundleName,
            List<FileBundleID> fastForwardRefs) {

        // get the list of known forks for the given bundle. It will be sorted
        // in preferred order
        ForkList forkList = bundleForks.get(bundleName);
        if (forkList == null || forkList.isEmpty())
            return false;

        // scan the list of forks for a fast-forward opportunity
        for (ForkInfo fork : forkList) {
            if (fork.directDevices.contains(selfDeviceID)) {
                // if this device is already pointing to the given fork, no
                // changes are needed.
                return false;

            } else if (fork.parentDevices.contains(selfDeviceID)) {
                // if this device is pointing to a parent of the given fork,
                // we should fast forward to adopt it.
                fastForwardRefs.add(fork.forkRef);
                return true;

            } else if (overwriteBundleNames.contains(bundleName)) {
                // if this bundle uses an "overwrite" strategy on conflicts,
                // abandon our fork and adopt the newly preferred HEAD.
                fastForwardRefs.add(fork.forkRef);
                return true;

            } else {
                // this fork represents a conflict, NOT a fast forward. Skip it
            }
        }

        // we will reach this line if our device doesn't have this bundle yet.
        // In that case, checking the bundle out represents a fast forward.
        fastForwardRefs.add(forkList.get(0).forkRef);
        return true;
    }


    /**
     * Find bundles which have forked into distinct heads, and return them.
     * 
     * @return a map whose keys are the names of bundles that have forked, and
     *         whose values are a list of the HEAD refs for the distinct forks.
     * @throws IOException
     *             if any files could not be read or any bundles are missing.
     */
    public Map<String, List<FileBundleID>> getForkedBundles()
            throws IOException {
        // find the distinct forks for each bundle
        BundleForks bundleForks = getBundleForks();

        // scan the bundles, looking for ones that have more than one fork
        Map<String, List<FileBundleID>> result = new HashMap();
        for (Entry<String, ForkList> e : bundleForks.entrySet()) {
            String bundleName = e.getKey();
            ForkList forks = e.getValue();
            if (forks.size() > 1 && !overwriteBundleNames.contains(bundleName)) {
                // if a bundle has more than one distinct fork, add to result
                List<FileBundleID> forkRefs = new ArrayList<FileBundleID>();
                for (ForkInfo info : forks)
                    forkRefs.add(info.forkRef);
                result.put(bundleName, forkRefs);
            }
        }

        return result;
    }


    /**
     * Find the newest bundle that is a common ancestor of two bundles.
     * 
     * If the bundles are equal, or if one of the bundles is already an ancestor
     * of the other, it will be returned. Otherwise, this method will search
     * backward through the bundle parentage until it finds a bundle that is an
     * ancestor of both.
     * 
     * @param a
     *            the first bundle ID to examine
     * @param b
     *            the second bundle ID to examine
     * @param maxDepth
     *            the maximum levels of parentage to consider. If the shared
     *            ancestor is further than this many generations back, returns
     *            null. Use -1 for no limit.
     * @param directOnly
     *            true if only direct parents should be followed. A direct
     *            parent is one that appears in the first (or only) position of
     *            a bundle's parents list. false if we should consider incoming
     *            merged parents as well.
     * @return the ID of a bundle that is either equal to, or a parent of, both
     *         specified bundles. returns null if no shared ancestor could be
     *         found.
     * @throws IOException
     *             if any necessary files could not be read, or if any bundles
     *             in the parentage are missing
     */
    public FileBundleID findSharedAncestor(FileBundleID a, FileBundleID b,
            int maxDepth, boolean directOnly) throws IOException {
        return findSharedAncestorImpl(a, b, maxDepth, directOnly, null);
    }

    /**
     * Track the lineage from a parent to a child, and return that lineage as a
     * list.
     * 
     * In the lineage list, the first element will be the parent, and the last
     * element will be the child. Within the list, each item(n) is a parent of
     * item(n+1).
     * 
     * @param parent
     *            a parent bundle
     * @param child
     *            a bundle which we expect to be a direct descendant of the
     *            given parent, through some arbitrary number of generations
     * @return a list containing the lineage, or null if parent is not an
     *         ancestor of child.
     * @throws IOException
     *             if any necessary files could not be read, or if any bundles
     *             in the parentage are missing
     * @since 2.7.4
     */
    public List<FileBundleID> getLineage(FileBundleID parent,
            FileBundleID child) throws IOException {
        Map<FileBundleID, FileBundleID> lineage = new HashMap();
        FileBundleID bid;
        // try to find a direct ancestor first
        bid = findSharedAncestorImpl(child, parent, -1, true, lineage);
        // if that fails, accept an ancestor that included an incoming merge
        if (bid == null)
            bid = findSharedAncestorImpl(child, parent, -1, false, lineage);
        if (bid == null || !bid.equals(parent))
            return null;

        List<FileBundleID> result = new ArrayList<FileBundleID>();
        while (bid != null) {
            result.add(bid);
            bid = lineage.get(bid);
        }
        return result;
    }

    private FileBundleID findSharedAncestorImpl(FileBundleID a, FileBundleID b,
            int maxDepth, boolean directOnly,
            Map<FileBundleID, FileBundleID> aLineage) throws IOException {
        // check for null and abort
        if (a == null || b == null)
            return null;

        // bundles can have multiple parents (in the case of a merge), so we
        // create a set to track the current parentage of each branch.
        SortedSet<FileBundleID> aa = new TreeSet(FileBundleID.REVERSE_ORDER);
        SortedSet<FileBundleID> bb = new TreeSet(FileBundleID.REVERSE_ORDER);
        aa.add(a);
        bb.add(b);

        for (int i = maxDepth; i-- != 0;) {
            // if we walked off the end of either lineage without finding a
            // common ancestor, return null
            if (aa.isEmpty() || bb.isEmpty())
                return null;

            // if the two branches contain a common item, that's the parent
            // we're looking for
            FileBundleID overlap = getCommonItem(aa, bb);
            if (overlap != null) {
                // check to see if the parent we found was replaced by another
                // bundle. If not, step back to get a real parent.
                FileBundleManifest mf = getManifestForAncestorTracing(overlap);
                if (mf.getFiles() == REPLACED_BUNDLE_FILES)
                    overlap = mf.getParents().get(0);
                return overlap;
            }

            // find the chronologically newest bundle ID among the two branches,
            // and make a note which branch it came from
            FileBundleID aNewest = aa.first();
            FileBundleID bNewest = bb.first();
            FileBundleID newest;
            SortedSet<FileBundleID> newestSet;
            if (FileBundleID.REVERSE_ORDER.compare(aNewest, bNewest) < 0) {
                newest = aNewest;
                newestSet = aa;
            } else if (aLineage != null) {
                // if the caller wants to track a direct lineage from A to B,
                // and we've already walked past that possibility, return null
                return null;
            } else {
                newest = bNewest;
                newestSet = bb;
            }

            // remove the newest bundle ID, and replace it with its list of
            // parents. This will allow us to march back in time until we find
            // a common ancestor.
            newestSet.remove(newest);
            FileBundleManifest mf = getManifestForAncestorTracing(newest);
            if (directOnly == false)
                newestSet.addAll(mf.getParents());
            else if (!mf.getParents().isEmpty())
                newestSet.add(mf.getParents().get(0));

            // keep track of the lineage of A if requested
            if (aLineage != null) {
                for (FileBundleID parent : mf.getParents())
                    aLineage.put(parent, newest);
            }
        }

        return null;
    }

    private FileBundleID getCommonItem(Set<FileBundleID> a,
            Set<FileBundleID> b) {
        for (FileBundleID item : a) {
            if (b.contains(item))
                return item;
        }
        return null;
    }


    private BundleForks cachedBundleForks;

    private BundleForks getCachedBundleForks() {
        synchronized (BundleForks.class) {
            return cachedBundleForks;
        }
    }

    private void setCachedBundleForks(BundleForks forks) {
        synchronized (BundleForks.class) {
            cachedBundleForks = forks;
        }
    }

    private BundleForks getBundleForks() throws IOException {
        // look for any changes in the HEADs directory
        scanDirectoryForChanges();

        // reload the bundle fork lists if needed
        BundleForks forks = getCachedBundleForks();
        if (forks == null) {
            forks = loadBundleForks();
            setCachedBundleForks(forks);
        }

        // return the forks for all bundles
        return forks;
    }

    private boolean scanDirectoryForChanges() throws FileNotFoundException {
        // list the files in the HEADs directory. Abort if dir is unreachable
        File[] files = bundleHeadsDir.listFiles();
        if (files == null)
            throw new FileNotFoundException(bundleHeadsDir.getPath());

        // scan the directory for new and changed HEADs files
        Set<File> obsoleteFiles;
        synchronized (deviceHeadRefs) {
            obsoleteFiles = new HashSet<File>(deviceHeadRefs.keySet());
        }
        obsoleteFiles.remove(selfHeadsFile);
        boolean sawChange = false;
        for (File oneFile : files) {
            // if this file is not a HEADs file we care about, skip it
            String deviceID = getDeviceIDForFile(oneFile);
            if (deviceID == null)
                continue;

            obsoleteFiles.remove(oneFile);
            HeadRefsPropertiesFile oneHeadRefs = deviceHeadRefs.get(oneFile);
            if (oneHeadRefs == null) {
                // this is a new file. Create an object to manage its head refs
                oneHeadRefs = new HeadRefsPropertiesFile(oneFile);
                deviceHeadRefs.put(oneFile, oneHeadRefs);
                sawChange = true;
                setCachedBundleForks(null);

            } else if (oneHeadRefs.needsUpdate()) {
                // this file was present before, and has changed
                sawChange = true;
                setCachedBundleForks(null);
            }
        }

        // if any files were deleted from the dir, remove them from our list
        if (!obsoleteFiles.isEmpty()) {
            sawChange = true;
            setCachedBundleForks(null);
            for (File oneFile : obsoleteFiles)
                deviceHeadRefs.remove(oneFile);
        }

        return sawChange;
    }

    private BundleForks loadBundleForks() throws IOException {
        // load current bundle heads for each device
        DeviceBundleHeads deviceBundleHeads = new DeviceBundleHeads();
        Set<String> bundleNames = new HashSet<String>();
        for (Entry<File, HeadRefsPropertiesFile> e : deviceHeadRefs
                .entrySet()) {
            String deviceID = getDeviceIDForFile(e.getKey());
            Map<String, FileBundleID> deviceHeads = e.getValue().getHeadRefs();
            deviceBundleHeads.put(deviceID, deviceHeads);
            bundleNames.addAll(deviceHeads.keySet());
        }

        // iterate over each bundle and get the known forks
        BundleForks result = new BundleForks();
        for (String oneBundleName : bundleNames) {
            ForkList oneBundleForks = loadForksForBundle(oneBundleName,
                deviceBundleHeads);
            result.put(oneBundleName, oneBundleForks);
        }

        // return the list of forks we found
        return result;
    }

    private ForkList loadForksForBundle(String bundleName,
            DeviceBundleHeads deviceRefs) throws IOException {
        // build a set of ForkInfo objects for each distinct head we see
        Map<FileBundleID, ForkInfo> headForkInfo = new HashMap();
        for (Entry<String, Map<String, FileBundleID>> e : deviceRefs
                .entrySet()) {
            // get the FileBundleID this device has for the given bundle
            String deviceID = e.getKey();
            FileBundleID bundleRef = e.getValue().get(bundleName);

            if (bundleRef != null) {
                ForkInfo forkInfo = headForkInfo.get(bundleRef);

                // if this bundle doesn't appear in the fork map, add a
                // new ForkInfo for it
                if (forkInfo == null) {
                    forkInfo = new ForkInfo(bundleRef);
                    headForkInfo.put(bundleRef, forkInfo);
                }

                // add this device ID to the ForkInfo
                forkInfo.directDevices.add(deviceID);
            }
        }

        // list the distinct heads we found, sorted from newest to oldest
        ForkList distinctHeads = new ForkList();
        distinctHeads.addAll(headForkInfo.values());
        Collections.sort(distinctHeads, NEWEST_SORTER);
        ForkList uniqueChildren = new ForkList();

        // iterate over the distinct heads from newest to oldest
        for (ForkInfo head : distinctHeads) {

            // discard invalid references
            if (head.isValid == false)
                continue;

            boolean headIsUniqueChild = true;

            for (ForkInfo oneChild : uniqueChildren) {
                if (isAncestor(head.forkRef, oneChild.forkRef)) {
                    oneChild.parentDevices.addAll(head.directDevices);
                    headIsUniqueChild = false;
                }
            }

            if (headIsUniqueChild)
                uniqueChildren.add(head);
        }

        // return a list of unique forks, and the votes each received.
        boolean isOverwriteBundle = overwriteBundleNames.contains(bundleName);
        uniqueChildren.commit(isOverwriteBundle);
        return uniqueChildren;
    }



    private boolean isAncestor(FileBundleID parent, FileBundleID child)
            throws IOException {
        String parentToken = parent.getToken();
        String childToken = child.getToken();
        int cmp = parentToken.compareTo(childToken);
        if (cmp >= 0)
            return false;

        // check the cache for a previously computed answer
        Map<String, Boolean> cachedResults = ancestorCache.get(parentToken);
        if (cachedResults == null) {
            cachedResults = new HashMap<String, Boolean>();
            ancestorCache.put(parentToken, cachedResults);
        } else {
            Boolean result = cachedResults.get(childToken);
            if (result != null)
                return result;
        }

        boolean result = false;
        FileBundleManifest manifest = getManifestForAncestorTracing(child);
        if (manifest.isReplacementFor(parent)) {
            result = true;
        } else {
            for (FileBundleID mid : manifest.getParents()) {
                if (mid.equals(parent) || isAncestor(parent, mid)) {
                    result = true;
                    break;
                }
            }
        }

        cachedResults.put(childToken, result);
        return result;
    }

    private FileBundleManifest getManifestForAncestorTracing(
            FileBundleID bundleID) throws IOException {
        try {
            // try loading the manifest directly
            return manifestSource.getManifest(bundleID);

        } catch (FileBundleManifest.Missing m) {
            // if the manifest couldn't be found, see if this is the ID of
            // a replaced bundle
            FileBundleManifest r = manifestSource
                    .getManifestReplacing(bundleID);

            // if we couldn't find a manifest (either directly, or as a
            // replacement), abort. It's possible that a sync client hasn't
            // finished syncing all the files we need into the bundle directory.
            if (r == null)
                throw m;

            // return a pseudo-manifest representing what the replaced bundle
            // would have looked like. This contains the accurate bundleID,
            // parents, and replacements. A placeholder file list is supplied
            // because we don't know the actual contents of the discarded
            // bundle. This is acceptable because our ancestry tracing logic
            // doesn't need to know file contents.
            FileBundleManifest missingManifest = new FileBundleManifest(
                    bundleID, REPLACED_BUNDLE_FILES, r.getParents(),
                    r.getReplaces());
            return missingManifest;
        }
    }

    private Map<String, Map<String, Boolean>> ancestorCache = Collections
            .synchronizedMap(new HashMap());



    private File getHeadsFileForDevice(String deviceID) {
        return new File(bundleHeadsDir, filenamePrefix + deviceID + SUFFIX);
    }

    private String getDeviceIDForFile(File headsFile) {
        String filename = headsFile.getName();
        if (filename.startsWith(filenamePrefix) && filename.endsWith(SUFFIX))
            return filename.substring(filenamePrefix.length(),
                filename.length() - SUFFIX.length());
        else
            return null;
    }

    private static final String SUFFIX = ".txt";



    /**
     * A HeadRefs implementation that discards cached fork info when refs change
     */
    private class SelfHeadRefs extends HeadRefsPropertiesFileLocking {

        public SelfHeadRefs(File storage, File published) {
            super(storage);
            setPublishedFile(published);
        }

        @Override
        protected void flush() throws IOException {
            setCachedBundleForks(null);
            super.flush();
        }

    }


    /**
     * A Map whose keys are device IDs and whose values are bundle ref maps (as
     * returned by {@link HeadRefs#getHeadRefs()})
     */
    private class DeviceBundleHeads
            extends HashMap<String, Map<String, FileBundleID>> {
    }


    /**
     * A Map whose keys are bundle names, and whose values are known forks for
     * those bundles
     */
    private class BundleForks extends HashMap<String, ForkList> {
    }


    /**
     * A list of forks for a particular bundle
     */
    private class ForkList extends ArrayList<ForkInfo> {

        private void commit(boolean isOverwrite) {
            // finalize each of the items in our list
            for (ForkInfo fork : this)
                fork.commit();

            // sort the items in preferred order
            Collections.sort(this, isOverwrite ? NEWEST_SORTER : VOTE_SORTER);
        }

    }


    /**
     * Information about a fork, including its HEAD ref, the devices that have
     * adopted it, and the devices that point to one of its ancestor refs
     */
    private class ForkInfo {

        private FileBundleID forkRef;

        private Set<String> directDevices;

        private Set<String> parentDevices;

        private boolean isValid;

        private int voteCount;

        private ForkInfo(FileBundleID forkRef) {
            this.forkRef = forkRef;
            this.directDevices = new HashSet<String>();
            this.parentDevices = new HashSet<String>();
            this.isValid = bundleValidator.isBundleValid(forkRef);
            this.voteCount = 0;
        }

        private void commit() {
            directDevices = makeImmutable(directDevices);
            parentDevices = makeImmutable(parentDevices);
            voteCount = directDevices.size() + parentDevices.size();
        }

        private Set<String> makeImmutable(Set<String> set) {
            if (set == null || set.isEmpty())
                return Collections.EMPTY_SET;
            else
                return Collections.unmodifiableSet(set);
        }

    }

    private static final Comparator<ForkInfo> VOTE_SORTER = new Comparator<ForkInfo>() {

        @Override
        public int compare(ForkInfo a, ForkInfo b) {
            // sort items with most votes first
            int voteDiff = b.voteCount - a.voteCount;
            if (voteDiff != 0)
                return voteDiff;

            // items with equal votes are sorted from oldest to newest
            return a.forkRef.getToken().compareTo(b.forkRef.getToken());
        }

    };

    private static final Comparator<ForkInfo> NEWEST_SORTER = new Comparator<ForkInfo>() {

        @Override
        public int compare(ForkInfo a, ForkInfo b) {
            return b.forkRef.getToken().compareTo(a.forkRef.getToken());
        }

    };

    private static final ResourceCollectionInfo REPLACED_BUNDLE_FILES = new ResourceListing();

}
