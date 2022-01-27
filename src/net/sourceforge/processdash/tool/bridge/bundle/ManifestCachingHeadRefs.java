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

package net.sourceforge.processdash.tool.bridge.bundle;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ManifestCachingHeadRefs implements HeadRefsManifestSource {

    private HeadRefs headRefs;

    private FileBundleManifestSource manifestSource;

    private File manifestCacheDir;

    private Map<String, FileBundleManifest> inMemoryCache;

    public ManifestCachingHeadRefs(HeadRefs headRefs,
            FileBundleManifestSource manifestSource, File manifestCacheDir) {
        this.headRefs = headRefs;
        this.manifestSource = manifestSource;
        this.manifestCacheDir = manifestCacheDir;
        this.inMemoryCache = Collections
                .synchronizedMap(new HashMap<String, FileBundleManifest>());
    }


    //
    // Implementation of HeadRefs
    //

    public FileBundleID getHeadRef(String bundleName) throws IOException {
        return headRefs.getHeadRef(bundleName);
    }


    public Map<String, FileBundleID> getHeadRefs() throws IOException {
        return headRefs.getHeadRefs();
    }


    public void storeHeadRef(FileBundleID bundleID) throws IOException {
        storeHeadRefs(Collections.singleton(bundleID));
    }


    public void storeHeadRefs(Collection<FileBundleID> newRefs)
            throws IOException {
        // pass the request along to the delegate
        headRefs.storeHeadRefs(newRefs);

        // cache the bundle manifests for each of the new refs
        for (FileBundleID bundleID : newRefs) {
            FileBundleManifest manifest = manifestSource.getManifest(bundleID);
            inMemoryCache.put(bundleID.getBundleName(), manifest);
            manifest.writeToFile(getCacheFile(bundleID.getBundleName()));
        }
    }


    public void deleteHeadRef(String bundleName) throws IOException {
        // pass the request along to the delegate
        headRefs.deleteHeadRef(bundleName);

        // delete the bundle manifest for the given ref
        getCacheFile(bundleName).delete();
        inMemoryCache.remove(bundleName);
    }


    //
    // Implementation of FileBundleManifestSource
    //

    public FileBundleManifest getManifest(FileBundleID bundleID)
            throws IOException {
        // get the locally cached manifest for the bundle with the same name
        FileBundleManifest result = getCachedManifest(bundleID.getBundleName());

        // if we found a manifest and its bundleID matches, return it
        if (result != null && result.getBundleID().equals(bundleID))
            return result;

        // if this bundle isn't in our manifest cache, pass the request along
        return manifestSource.getManifest(bundleID);
    }


    public FileBundleManifest getCachedManifest(String bundleName) {
        // Look in our memory-based cache first
        FileBundleManifest result = inMemoryCache.get(bundleName);

        // if that failed, try reading a manifest from our cache directory
        if (result == null) {
            try {
                result = new FileBundleManifest(getCacheFile(bundleName));
                inMemoryCache.put(bundleName, result);
            } catch (IOException ioe) {
            }
        }

        return result;
    }


    private File getCacheFile(String bundleName) {
        return new File(manifestCacheDir,
                FILENAME_PREFIX + bundleName + ".xml");
    }

    private static final String FILENAME_PREFIX = "bundle-manifest-";

}
