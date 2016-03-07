// Copyright (C) 2016 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.bridge.client;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import net.sourceforge.processdash.tool.bridge.impl.FileResourceCollection;
import net.sourceforge.processdash.tool.bridge.impl.TeamDataDirStrategy;
import net.sourceforge.processdash.tool.bridge.report.ResourceCollectionDiff;
import net.sourceforge.processdash.util.FileUtils;

/**
 * An {@link ImportDirectory} object that copies files from a network directory
 * to a cache directory on the local hard drive.
 */
public class CachingLocalImportDirectory implements ImportDirectory {

    private File srcDirectory;

    private File targetDirectory;

    private FileResourceCollection targetCollection;

    private File cacheDirectory;

    private FileResourceCollection cachedCollection;

    private long lastUpdateTime;


    protected CachingLocalImportDirectory(File targetDirectory) {
        this.targetDirectory = this.srcDirectory = targetDirectory;
        this.targetCollection = makeCollection(targetDirectory);

        this.cacheDirectory = BridgedImportDirectory
                .getCacheDirectoryForBridgedImport(getDescription());
        this.cacheDirectory.mkdirs();
        this.cachedCollection = makeCollection(cacheDirectory);

        this.lastUpdateTime = -1;
        try {
            update();
        } catch (IOException ioe) {
        }
    }

    private FileResourceCollection makeCollection(File dir) {
        FileResourceCollection result = new FileResourceCollection(dir, false);
        result.setStrategy(TeamDataDirStrategy.INSTANCE);
        return result;
    }

    public File getDirectory() {
        return srcDirectory;
    }

    public String getRemoteLocation() {
        return null;
    }

    public String getDescription() {
        return targetDirectory.getAbsolutePath();
    }

    public void update() throws IOException {
        // this method may get called overzealously by code in different layers
        // of the application. If it is called more than once within a few
        // milliseconds, don't repeat the update.
        long now = System.currentTimeMillis();
        long lastUpdateAge = now - lastUpdateTime;
        if (lastUpdateAge > 1000 || lastUpdateAge < 0) {
            syncDown();
            srcDirectory = cacheDirectory;
            lastUpdateTime = System.currentTimeMillis();
        }
    }

    private void syncDown() throws IOException {
        // ensure the directories exist
        targetCollection.validate();
        cachedCollection.validate();

        // compute a difference between the two directories
        ResourceCollectionDiff diff = new ResourceCollectionDiff(
                targetCollection, cachedCollection);

        // delete any files that are only present in the cache
        for (String deletedFile : diff.getOnlyInB())
            cachedCollection.deleteResource(deletedFile);

        // copy any files that are missing from the cache, or that differ
        syncFilesDown(diff.getOnlyInA(), diff.getDiffering());
    }

    private void syncFilesDown(List<String>... filesets) throws IOException {
        for (List<String> oneFileSet : filesets) {
            for (String filename : oneFileSet) {
                // copy the files from the target directory to the cache. We
                // route the modifications through the cache collection object
                // so it has a chance to update its internally cached state
                // (e.g. file modification times, checksums, etc)
                OutputStream out = cachedCollection.getOutputStream(filename);
                FileUtils.copyFile(new File(targetDirectory, filename), out);
                out.close();
            }
        }
    }

}
